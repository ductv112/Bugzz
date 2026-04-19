package com.bugzz.filter.camera.camera

import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.view.Surface
import androidx.camera.core.CameraEffect
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.UseCaseGroup
import androidx.camera.effects.OverlayEffect
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.mlkit.vision.MlKitAnalyzer
import androidx.lifecycle.LifecycleOwner
import com.bugzz.filter.camera.detector.FaceDetectorClient
import com.bugzz.filter.camera.render.OverlayEffectBuilder
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.concurrent.Executor
import java.util.concurrent.Executors

/**
 * Nyquist unit test for [CameraController] (CAM-03 + CAM-05).
 *
 * Pins two architectural contracts from Plan 02-04:
 *  1. `UseCaseGroup` bound to the lifecycle contains exactly 4 use cases
 *     (Preview + ImageAnalysis + ImageCapture + VideoCapture) and 1 effect
 *     (the OverlayEffect) — CAM-03 / CAM-06 / D-06.
 *  2. `ImageAnalysis` is configured with [ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST]
 *     backpressure (CAM-05 / PITFALLS #4).
 *
 * Enabled in Plan 02-04 via the constructor-default `providerFactory` seam on
 * CameraController: `suspend (Context) -> ProcessCameraProvider` defaulting to
 * `{ ProcessCameraProvider.getInstance(it).await() }`. The test injects a Mockito
 * mock to sidestep the Camera2 HAL — we're only verifying how CameraController
 * wires up the UseCaseGroup, not that CameraX itself binds to a real camera.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])  // CameraX Preview.Builder internals exercise android.util.ArrayMap — needs Android SDK shadow
class CameraControllerTest {

    private val cameraExecutor: Executor = Executors.newSingleThreadExecutor()

    private fun buildController(mockProvider: ProcessCameraProvider): CameraController {
        val mockContext: Context = mock()
        val mockAnalyzer: MlKitAnalyzer = mock()
        val mockDetector: FaceDetectorClient = mock<FaceDetectorClient>().stub {
            on { createAnalyzer() } doReturn mockAnalyzer
        }
        // UseCaseGroup.Builder.build() validates effect.getTargets() against a whitelist —
        // mock default 0 is rejected. Stub to the real PREVIEW|VIDEO|IMAGE mask (CAM-06).
        val mockEffect: OverlayEffect = mock<OverlayEffect>().stub {
            on { targets } doReturn (
                CameraEffect.PREVIEW or CameraEffect.VIDEO_CAPTURE or CameraEffect.IMAGE_CAPTURE
            )
        }
        val mockBuilder: OverlayEffectBuilder = mock<OverlayEffectBuilder>().stub {
            on { build() } doReturn mockEffect
        }
        return CameraController(
            appContext = mockContext,
            cameraExecutor = cameraExecutor,
            faceDetector = mockDetector,
            overlayEffectBuilder = mockBuilder,
            providerFactory = { mockProvider },
        )
    }

    @Test
    fun bind_produces_4_use_cases_plus_1_effect() = runBlocking {
        val mockProvider: ProcessCameraProvider = mock()
        val mockOwner: LifecycleOwner = mock()
        val controller = buildController(mockProvider)

        controller.bind(mockOwner, CameraLens.FRONT, Surface.ROTATION_0)

        val captor = argumentCaptor<UseCaseGroup>()
        verify(mockProvider).bindToLifecycle(any(), any(), captor.capture())
        val group = captor.firstValue
        assertEquals(
            "UseCaseGroup must contain exactly 4 use cases (Preview+IA+IC+VC) — CAM-03",
            4, group.useCases.size
        )
        assertEquals(
            "UseCaseGroup must have exactly 1 effect (OverlayEffect) — CAM-06",
            1, group.effects.size
        )
    }

    @Test
    fun image_analysis_uses_keep_only_latest_strategy() = runBlocking {
        val mockProvider: ProcessCameraProvider = mock()
        val mockOwner: LifecycleOwner = mock()
        val controller = buildController(mockProvider)

        controller.bind(mockOwner, CameraLens.FRONT, Surface.ROTATION_0)

        val captor = argumentCaptor<UseCaseGroup>()
        verify(mockProvider).bindToLifecycle(any(), any(), captor.capture())
        val ia = captor.firstValue.useCases.filterIsInstance<ImageAnalysis>().firstOrNull()
        assertEquals(
            "ImageAnalysis.backpressureStrategy must be STRATEGY_KEEP_ONLY_LATEST — CAM-05 / PITFALLS #4",
            ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST, ia?.backpressureStrategy
        )
    }

    // -------------------------------------------------------------------------
    // Phase 3 Wave 0: capturePhoto tests (CAP-01 / CAP-03) — @Ignore'd until Plan 03-04
    // -------------------------------------------------------------------------

    /**
     * Verifies [CameraController.capturePhoto] calls ImageCapture.takePicture when camera is bound.
     *
     * @Ignore'd because [CameraController.capturePhoto] does not yet exist — Plan 03-04 Task 1 adds it.
     * The test body is fully written against the Plan 03-04 final contract.
     *
     * Test seam: Plan 03-04 must add an `internal` primary constructor param
     * `imageCaptureFactory: () -> ImageCapture = { ImageCapture.Builder().build() }`
     * (following the Phase 2 providerFactory split pattern — D-14 Phase 2 STATE).
     */
    @org.junit.Ignore("Plan 03-04 — flip to GREEN when capturePhoto method lands")
    @Test
    fun capturePhoto_invokesTakePicture_onBoundImageCapture() {
        // TODO Plan 03-04: construct CameraController with a mock imageCaptureFactory that returns
        // a mock ImageCapture; call bind() then capturePhoto {}; verify mockImageCapture.takePicture()
        // was invoked with non-null OutputFileOptions, non-null Executor, non-null callback.
    }

    /**
     * Verifies [CameraController.capturePhoto] builds ContentValues with correct MediaStore fields.
     *
     * Acceptance criteria (D-32):
     *   - RELATIVE_PATH == "DCIM/Bugzz"
     *   - MIME_TYPE == "image/jpeg"
     *   - DISPLAY_NAME matches regex Bugzz_\d{8}_\d{6}\.jpg
     *
     * @Ignore'd because [CameraController.capturePhoto] does not yet exist — Plan 03-04 Task 1 adds it.
     */
    @org.junit.Ignore("Plan 03-04 — flip to GREEN when capturePhoto method lands")
    @Test
    fun capturePhoto_outputFileOptions_hasDCIMBugzzRelativePath() {
        // TODO Plan 03-04: capture ContentValues passed to ImageCapture.OutputFileOptions.Builder
        // via the imageCaptureFactory seam; assert the 3 D-32 fields.
        //
        // Inline assertion template (to be activated when seam exists):
        // assertEquals("DCIM/Bugzz", contentValues.getAsString(MediaStore.Images.Media.RELATIVE_PATH))
        // assertEquals("image/jpeg",  contentValues.getAsString(MediaStore.Images.Media.MIME_TYPE))
        // assertTrue(contentValues.getAsString(MediaStore.Images.Media.DISPLAY_NAME)
        //     .matches(Regex("Bugzz_\\d{8}_\\d{6}\\.jpg")))
        assertTrue("placeholder — test body wired in Plan 03-04", true)
    }

    /**
     * Verifies [CameraController.capturePhoto] emits failure when camera is not bound.
     *
     * @Ignore'd because [CameraController.capturePhoto] does not yet exist — Plan 03-04 Task 1 adds it.
     */
    @org.junit.Ignore("Plan 03-04 — flip to GREEN when capturePhoto method lands")
    @Test
    fun capturePhoto_controllerNotBound_emitsFailure() {
        // TODO Plan 03-04: construct CameraController WITHOUT calling bind();
        // call capturePhoto { result -> ... }; assert result.isFailure and
        // result.exceptionOrNull() is IllegalStateException.
        //
        // Inline assertion template:
        // var capturedResult: Result<Uri>? = null
        // controller.capturePhoto { capturedResult = it }
        // assertNotNull(capturedResult)
        // assertTrue(capturedResult!!.isFailure)
        // assertTrue(capturedResult!!.exceptionOrNull() is IllegalStateException)
    }
}
