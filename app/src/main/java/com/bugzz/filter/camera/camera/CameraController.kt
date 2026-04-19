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
import androidx.camera.core.ImageCaptureException
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
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
 * Provider-factory seam: the primary `@Inject` constructor hard-codes the production factory
 * (`ProcessCameraProvider.getInstance(ctx).await()`) so the Hilt graph has no "how do I provide
 * a suspend lambda?" ambiguity. Tests use the secondary constructor to inject a mock factory.
 * Plan 02-05 Rule 3 auto-fix: Hilt cannot satisfy a `Function2` injection even when it has a
 * Kotlin default value — the default exists in bytecode only for direct Kotlin callers, not
 * for Dagger's generated factory. Splitting the constructor keeps the seam testable without
 * requiring a Hilt `@Provides` binding for the lambda.
 */
@Singleton
class CameraController internal constructor(
    @ApplicationContext private val appContext: Context,
    @Named("cameraExecutor") private val cameraExecutor: Executor,
    private val faceDetector: FaceDetectorClient,
    private val overlayEffectBuilder: OverlayEffectBuilder,
    private val providerFactory: suspend (Context) -> ProcessCameraProvider,
    /**
     * Test seam (Plan 03-04 D-14 constructor-split pattern): allows tests to inject a mock
     * ImageCapture instead of constructing a real one via ImageCapture.Builder. Production
     * factory builds the real use case.
     */
    private val imageCaptureFactory: () -> ImageCapture = {
        ImageCapture.Builder().setTargetRotation(Surface.ROTATION_0).build()
    },
) {

    /**
     * Production constructor — Hilt-visible. Uses the static `ProcessCameraProvider.getInstance`
     * ListenableFuture adapted via the `await()` extension at the bottom of this file.
     */
    @Inject
    constructor(
        @ApplicationContext appContext: Context,
        @Named("cameraExecutor") cameraExecutor: Executor,
        faceDetector: FaceDetectorClient,
        overlayEffectBuilder: OverlayEffectBuilder,
    ) : this(
        appContext = appContext,
        cameraExecutor = cameraExecutor,
        faceDetector = faceDetector,
        overlayEffectBuilder = overlayEffectBuilder,
        providerFactory = { ctx -> ProcessCameraProvider.getInstance(ctx).await() },
        imageCaptureFactory = { ImageCapture.Builder().setTargetRotation(Surface.ROTATION_0).build() },
    )

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

        val imageCaptureUc = imageCaptureFactory().also { it.targetRotation = initialRotation }

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

    /**
     * CAP-01/02/03 — capture photo via ImageCapture.takePicture + MediaStoreOutputFileOptions.
     *
     * CameraX 1.6 handles IS_PENDING transaction automatically per
     * developer.android.com/media/camera/camerax/take-photo.
     *
     * On success: onResult(Result.success(uri)) — Uri is MediaStore content URI.
     * On error:   onResult(Result.failure(ImageCaptureException)) — storage full,
     *             access denied, or other CameraX error path.
     *
     * Thread: takePicture callback runs on cameraExecutor. Caller (ViewModel) forwards to
     * main via viewModelScope.launch. No allocation or I/O on the main thread.
     *
     * T-03-04: callback lives inside @Singleton CameraController; onResult lambda is invoked
     * once and released — no long-lived reference back to ViewModel.
     */
    fun capturePhoto(onResult: (Result<Uri>) -> Unit) {
        val ic = imageCapture ?: run {
            onResult(Result.failure(IllegalStateException("Camera not bound")))
            return
        }
        val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
        val filename = "Bugzz_${sdf.format(Date())}.jpg"
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, filename)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, "DCIM/Bugzz")
            // Do NOT set IS_PENDING — CameraX 1.6 handles the transaction.
        }
        val options = ImageCapture.OutputFileOptions.Builder(
            appContext.contentResolver,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            values,
        ).build()

        ic.takePicture(
            options,
            cameraExecutor,
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(results: ImageCapture.OutputFileResults) {
                    val uri = results.savedUri ?: run {
                        onResult(Result.failure(IllegalStateException("savedUri null on success")))
                        return
                    }
                    Timber.tag("CameraController").i("Photo saved %s", uri)
                    onResult(Result.success(uri))
                }
                override fun onError(exc: ImageCaptureException) {
                    Timber.tag("CameraController").e(exc, "Photo capture failed")
                    onResult(Result.failure(exc))
                }
            },
        )
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

