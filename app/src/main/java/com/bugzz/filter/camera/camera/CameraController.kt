package com.bugzz.filter.camera.camera

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.util.Size
import android.view.Surface
import androidx.camera.core.CameraEffect
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.MirrorMode
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceRequest
import androidx.camera.core.UseCaseGroup
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.bugzz.filter.camera.detector.FaceDetectorClient
import com.bugzz.filter.camera.render.OverlayEffectBuilder
import com.google.common.util.concurrent.ListenableFuture
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import timber.log.Timber
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executor
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Owns CameraX lifecycle binding. Single source of truth for use-case construction + lens flip
 * + device rotation + debug test recording (Phase 2).
 *
 * Contract:
 *   - `bind()` produces a UseCaseGroup with EXACTLY 4 use cases (Preview + ImageAnalysis +
 *     ImageCapture + VideoCapture) and EXACTLY 1 effect (OverlayEffect). (CAM-03 / CAM-06)
 *   - OverlayEffect is constructed ONCE via overlayEffectBuilder.build() (D-25) — same instance
 *     is re-added to every new UseCaseGroup on lens flip.
 *   - ImageAnalysis backpressure is STRATEGY_KEEP_ONLY_LATEST (CAM-05 / PITFALLS #4).
 *   - VideoCapture is ALWAYS bound (D-06 — even though only the debug test button triggers it).
 *   - No audio on test recording (D-05 — no `.withAudioEnabled()`; T-02-06).
 *
 * @param providerFactory Test seam — defaults to `ProcessCameraProvider.awaitInstance(context)`
 *   in production. CameraControllerTest (Plan 01) passes a mock provider factory.
 */
@Singleton
class CameraController @Inject constructor(
    @ApplicationContext private val appContext: Context,
    @Named("cameraExecutor") private val cameraExecutor: Executor,
    private val faceDetector: FaceDetectorClient,
    private val overlayEffectBuilder: OverlayEffectBuilder,
    private val providerFactory: suspend (Context) -> ProcessCameraProvider =
        { ctx -> ProcessCameraProvider.getInstance(ctx).await() },
) {
    // Compose-native: emit latest SurfaceRequest to the composable for CameraXViewfinder.
    private val _surfaceRequest = MutableStateFlow<SurfaceRequest?>(null)
    val surfaceRequest: StateFlow<SurfaceRequest?> = _surfaceRequest.asStateFlow()

    // Reused across lens flips (D-25 — OverlayEffect is singleton-ish per session).
    private val overlayEffect: CameraEffect = overlayEffectBuilder.build()

    // Use-case refs — recreated on lens flip; retained for setTargetRotation() to touch targetRotation.
    private var preview: Preview? = null
    private var imageAnalysis: ImageAnalysis? = null
    private var imageCapture: ImageCapture? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private var activeRecording: Recording? = null

    /** CAM-03 — bind 4 use cases + 1 effect under one lifecycle. */
    suspend fun bind(
        lifecycleOwner: LifecycleOwner,
        lens: CameraLens,
        initialRotation: Int = Surface.ROTATION_0,
    ) {
        val provider = providerFactory(appContext)
        provider.unbindAll()

        val previewUc = Preview.Builder()
            .setTargetRotation(initialRotation)
            .build()
            .also { p ->
                p.setSurfaceProvider { request -> _surfaceRequest.value = request }
            }

        val analyzer = faceDetector.createAnalyzer()
        val imageAnalysisUc = ImageAnalysis.Builder()
            .setResolutionSelector(
                ResolutionSelector.Builder()
                    .setResolutionStrategy(
                        ResolutionStrategy(
                            Size(720, 1280),
                            ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER,
                        )
                    ).build()
            )
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)  // CAM-05 / PITFALLS #4
            .setTargetRotation(initialRotation)
            .build()
            .also { it.setAnalyzer(cameraExecutor, analyzer) }

        val imageCaptureUc = ImageCapture.Builder()
            .setTargetRotation(initialRotation)
            .build()

        val videoCaptureUc = VideoCapture.Builder(
            Recorder.Builder()
                .setQualitySelector(QualitySelector.from(Quality.HD))
                .build()
        )
            .setMirrorMode(MirrorMode.MIRROR_MODE_ON_FRONT_ONLY)
            .setTargetRotation(initialRotation)
            .build()

        val useCaseGroup = UseCaseGroup.Builder()
            .addUseCase(previewUc)
            .addUseCase(imageAnalysisUc)
            .addUseCase(imageCaptureUc)
            .addUseCase(videoCaptureUc)
            .addEffect(overlayEffect)   // CAM-06 — effect bakes into Preview + Image + Video
            .build()

        val selector = when (lens) {
            CameraLens.FRONT -> CameraSelector.DEFAULT_FRONT_CAMERA
            CameraLens.BACK  -> CameraSelector.DEFAULT_BACK_CAMERA
        }

        provider.bindToLifecycle(lifecycleOwner, selector, useCaseGroup)

        // Retain refs for setTargetRotation() on device rotation.
        this.preview = previewUc
        this.imageAnalysis = imageAnalysisUc
        this.imageCapture = imageCaptureUc
        this.videoCapture = videoCaptureUc
    }

    /** D-24 + PITFALLS #6 — clear 1€ state BEFORE rebind to avoid stale trackingId leak. */
    suspend fun flipLens(lifecycleOwner: LifecycleOwner, newLens: CameraLens, rotation: Int) {
        faceDetector.onLensFlipped()
        bind(lifecycleOwner, newLens, rotation)
    }

    /** D-08 — invoked by OrientationEventListener on device rotation (under portrait lock). */
    fun setTargetRotation(rotation: Int) {
        preview?.targetRotation = rotation
        imageAnalysis?.targetRotation = rotation
        imageCapture?.targetRotation = rotation
        videoCapture?.targetRotation = rotation
    }

    /** D-04 / D-05 — debug-only, NO AUDIO (no .withAudioEnabled()), saves to DCIM/Bugzz/. */
    fun startTestRecording(onFinalized: (Result<Uri>) -> Unit) {
        val vc = videoCapture
        if (vc == null) {
            onFinalized(Result.failure(IllegalStateException("Camera not bound")))
            return
        }
        if (activeRecording != null) {
            onFinalized(Result.failure(IllegalStateException("Already recording")))
            return
        }
        val filename = "bugzz_test_${System.currentTimeMillis()}.mp4"
        val values = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, filename)
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            put(MediaStore.Video.Media.RELATIVE_PATH, "DCIM/Bugzz")
        }
        val options = MediaStoreOutputOptions.Builder(
            appContext.contentResolver,
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
        ).setContentValues(values).build()

        activeRecording = vc.output
            .prepareRecording(appContext, options)
            // NOTE: D-05 — do NOT call .withAudioEnabled(); RECORD_AUDIO is deferred to Phase 5.
            .start(ContextCompat.getMainExecutor(appContext)) { event ->
                when (event) {
                    is VideoRecordEvent.Start -> {
                        Timber.tag("CameraController").i("TEST RECORD start")
                    }
                    is VideoRecordEvent.Finalize -> {
                        activeRecording = null
                        if (event.hasError()) {
                            Timber.tag("CameraController")
                                .e(event.cause, "TEST RECORD finalize error=%d", event.error)
                            onFinalized(
                                Result.failure(
                                    event.cause ?: RuntimeException("record error ${event.error}")
                                )
                            )
                        } else {
                            val uri = event.outputResults.outputUri
                            Timber.tag("CameraController").i("TEST RECORD saved %s", uri)
                            onFinalized(Result.success(uri))
                        }
                    }
                    else -> Unit
                }
            }
    }

    fun stopTestRecording() {
        activeRecording?.stop()
        activeRecording = null
    }
}

/**
 * Minimal ListenableFuture → suspend bridge — avoids pulling `kotlinx-coroutines-guava` into
 * the classpath just for one call site. `ProcessCameraProvider.getInstance()` returns a
 * ListenableFuture (CameraX 1.6 does not expose a suspend `awaitInstance` on this AAR despite
 * research §A5 — Rule 3 auto-fix 1).
 *
 * Pattern: `addListener` on direct executor; on completion pull via `get()` which returns
 * the result or throws ExecutionException. Cancellation propagates via `cancel(true)`.
 */
private suspend fun <T> ListenableFuture<T>.await(): T =
    suspendCancellableCoroutine { cont ->
        addListener(
            {
                try {
                    cont.resume(get())
                } catch (e: ExecutionException) {
                    cont.resumeWithException(e.cause ?: e)
                } catch (t: Throwable) {
                    cont.resumeWithException(t)
                }
            },
            Runnable::run,
        )
        cont.invokeOnCancellation { cancel(true) }
    }

