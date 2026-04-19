package com.bugzz.filter.camera.camera

import android.content.Context
import android.view.Surface
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.UseCaseGroup
import androidx.camera.effects.OverlayEffect
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.mlkit.vision.MlKitAnalyzer
import androidx.lifecycle.LifecycleOwner
import com.bugzz.filter.camera.camera.CameraLens
import com.bugzz.filter.camera.detector.FaceDetectorClient
import com.bugzz.filter.camera.render.OverlayEffectBuilder
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Ignore
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.concurrent.Executor
import java.util.concurrent.Executors

/**
 * Nyquist unit test for [CameraController] (CAM-03 + CAM-05).
 *
 * Pins two architectural contracts Plan 02-04 must honour:
 *  1. `UseCaseGroup` bound to the lifecycle contains exactly 4 use cases
 *     (Preview + ImageAnalysis + ImageCapture + VideoCapture) and 1 effect
 *     (the OverlayEffect). This is the three-stream + single-effect
 *     architecture mandated by CAM-03 / D-06.
 *  2. `ImageAnalysis` is configured with
 *     [ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST] backpressure (CAM-05 /
 *     PITFALLS #4 — raising to STRATEGY_BLOCK_PRODUCER backs up the frame
 *     queue on ML Kit analyzer slow frames).
 *
 * Both tests are `@Ignore`d until Plan 02-04 factors out a provider-injection
 * seam on [CameraController] — the 02-RESEARCH.md sketch uses
 * `ProcessCameraProvider.awaitInstance(appContext)` which is a static call
 * that cannot be mocked without Robolectric. Plan 02-04 MUST add a
 * constructor-injected default parameter
 * `providerFactory: suspend (Context) -> ProcessCameraProvider =
 *     { ProcessCameraProvider.awaitInstance(it) }` so the test can pass in a
 * mock provider. This is flagged as a Plan 02-04 acceptance criterion.
 *
 * TODO (Plan 04 seam): Remove `@Ignore` once CameraController exposes:
 *   - constructor parameter `providerFactory: suspend (Context) -> ProcessCameraProvider`
 *   - an accessor to retrieve the bound `ImageAnalysis` (or inspect via
 *     UseCaseGroup returned from bind()).
 *
 * Intentional Nyquist RED: [CameraController] does not yet exist; mockito-kotlin
 * is not yet on the testImplementation classpath (Plan 02-02 adds it);
 * compilation fails with "Unresolved reference" until both land.
 */
class CameraControllerTest {

    private val cameraExecutor: Executor = Executors.newSingleThreadExecutor()

    @Test
    @Ignore("Activated in Plan 04 when CameraController exposes provider factory seam")
    fun bind_produces_4_use_cases_plus_1_effect() = runBlocking {
        val mockContext: Context = mock()
        val mockProvider: ProcessCameraProvider = mock()
        val mockOwner: LifecycleOwner = mock()
        val mockAnalyzer: MlKitAnalyzer = mock()
        val mockDetector: FaceDetectorClient = mock()
        whenever(mockDetector.createAnalyzer()).thenReturn(mockAnalyzer)
        val mockEffect: OverlayEffect = mock()
        val mockBuilder: OverlayEffectBuilder = mock()
        whenever(mockBuilder.build()).thenReturn(mockEffect)

        val controller = CameraController(
            appContext = mockContext,
            cameraExecutor = cameraExecutor,
            faceDetector = mockDetector,
            overlayEffectBuilder = mockBuilder,
        )

        // TODO (Plan 04 seam): swap direct bind() for provider-injection variant:
        //   controller.bindWithProvider(mockOwner, CameraLens.FRONT, Surface.ROTATION_0, mockProvider)
        controller.bind(mockOwner, CameraLens.FRONT, Surface.ROTATION_0)

        val captor = ArgumentCaptor.forClass(UseCaseGroup::class.java)
        verify(mockProvider).bindToLifecycle(any(), any(), captor.capture())
        val group = captor.value
        assertEquals(
            "UseCaseGroup must contain exactly 4 use cases (Preview+IA+IC+VC)",
            4,
            group.useCases.size,
        )
        assertEquals(
            "UseCaseGroup must have exactly 1 effect (OverlayEffect)",
            1,
            group.effects.size,
        )
    }

    @Test
    @Ignore("Activated in Plan 04 when CameraController exposes provider factory seam + ImageAnalysis accessor")
    fun image_analysis_uses_keep_only_latest_strategy() = runBlocking {
        val mockContext: Context = mock()
        val mockProvider: ProcessCameraProvider = mock()
        val mockOwner: LifecycleOwner = mock()
        val mockAnalyzer: MlKitAnalyzer = mock()
        val mockDetector: FaceDetectorClient = mock()
        whenever(mockDetector.createAnalyzer()).thenReturn(mockAnalyzer)
        val mockEffect: OverlayEffect = mock()
        val mockBuilder: OverlayEffectBuilder = mock()
        whenever(mockBuilder.build()).thenReturn(mockEffect)

        val controller = CameraController(
            appContext = mockContext,
            cameraExecutor = cameraExecutor,
            faceDetector = mockDetector,
            overlayEffectBuilder = mockBuilder,
        )

        // TODO (Plan 04 seam): provider-injection variant, then
        //   val group = captor.value
        //   val imageAnalysis = group.useCases.filterIsInstance<ImageAnalysis>().first()
        //   assertEquals(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST, imageAnalysis.backpressureStrategy)
        controller.bind(mockOwner, CameraLens.FRONT, Surface.ROTATION_0)

        val captor = ArgumentCaptor.forClass(UseCaseGroup::class.java)
        verify(mockProvider).bindToLifecycle(any(), any(), captor.capture())
        val group = captor.value
        val imageAnalysis = group.useCases.filterIsInstance<ImageAnalysis>().first()
        assertEquals(
            "ImageAnalysis must use STRATEGY_KEEP_ONLY_LATEST (CAM-05 / PITFALLS #4)",
            ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST,
            imageAnalysis.backpressureStrategy,
        )
    }
}
