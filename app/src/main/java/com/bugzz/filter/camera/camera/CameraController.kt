package com.bugzz.filter.camera.camera

import android.content.Context
import androidx.lifecycle.LifecycleOwner
import com.bugzz.filter.camera.detector.FaceDetectorClient
import com.bugzz.filter.camera.render.OverlayEffectBuilder
import java.util.concurrent.Executor

/**
 * Camera binding controller (Plan 02-04).
 *
 * **Plan 02-03 placeholder stub.** The real implementation lands in Plan 02-04 —
 * see `.planning/phases/02-camera-preview-face-detection-coordinate-validation/02-04-PLAN.md`.
 * This stub exists only to let the test sourceset compile so Plan 02-03's GREEN-gate tests
 * (`OneEuroFilterTest`, `FaceDetectorOptionsTest`) can run. `CameraControllerTest`'s two tests
 * are `@Ignore`d — they will be un-Ignored by Plan 02-04 once the provider-factory seam is added.
 *
 * Plan 02-04 will:
 *  - Add the `providerFactory: suspend (Context) -> ProcessCameraProvider` constructor
 *    default parameter (per 02-01-SUMMARY.md provider-factory seam pattern).
 *  - Implement [bind] to build a `UseCaseGroup` of 4 use cases (Preview + ImageAnalysis +
 *    ImageCapture + VideoCapture) with the OverlayEffect + `STRATEGY_KEEP_ONLY_LATEST`.
 *  - Add `flipLens`, `capturePhoto`, `recordVideo` methods per CAM-01..CAM-06.
 */
class CameraController(
    private val appContext: Context,
    private val cameraExecutor: Executor,
    private val faceDetector: FaceDetectorClient,
    private val overlayEffectBuilder: OverlayEffectBuilder,
) {
    /** Plan 02-04 stub — real bind logic not implemented in 02-03. */
    suspend fun bind(owner: LifecycleOwner, lens: CameraLens, rotation: Int) {
        throw NotImplementedError("CameraController.bind() lands in Plan 02-04")
    }
}
