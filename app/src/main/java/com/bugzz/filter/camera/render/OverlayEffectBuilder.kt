package com.bugzz.filter.camera.render

import androidx.camera.core.CameraEffect
import androidx.camera.effects.OverlayEffect

/**
 * Builder for the three-stream [OverlayEffect] (D-25 / CAM-06).
 *
 * **Plan 02-03 placeholder stub.** The real implementation lands in Plan 02-04 —
 * see `.planning/phases/02-camera-preview-face-detection-coordinate-validation/02-04-PLAN.md`.
 * This stub exists only to let the test sourceset compile so Plan 02-03's GREEN-gate tests
 * (`OneEuroFilterTest`, `FaceDetectorOptionsTest`) can run. `OverlayEffectBuilderTest` stays
 * RED at runtime because [TARGETS] and [QUEUE_DEPTH] here are deliberately wrong placeholders.
 *
 * Plan 02-04 will:
 *  - Populate [TARGETS] to `PREVIEW or VIDEO_CAPTURE or IMAGE_CAPTURE` (CAM-06 three-stream).
 *  - Populate [QUEUE_DEPTH] to `0` (per-frame compositing, no latency buffer).
 *  - Implement [build] to construct a real OverlayEffect with Handler + onDrawListener.
 */
class OverlayEffectBuilder {

    /** Plan 02-04 stub — real instance not built in 02-03. */
    fun build(): OverlayEffect =
        throw NotImplementedError("OverlayEffectBuilder.build() lands in Plan 02-04")

    companion object {
        /**
         * Placeholder — Plan 02-04 sets this to the real target mask.
         * [OverlayEffectBuilderTest.target_mask_covers_preview_image_video] intentionally
         * fails against this wrong value (Nyquist RED until Plan 02-04).
         */
        const val TARGETS: Int = 0

        /**
         * Placeholder — Plan 02-04 sets this to 0 for per-frame compositing.
         * [OverlayEffectBuilderTest.queue_depth_is_zero] intentionally fails against this
         * wrong value (Nyquist RED until Plan 02-04).
         */
        const val QUEUE_DEPTH: Int = -1
    }
}
