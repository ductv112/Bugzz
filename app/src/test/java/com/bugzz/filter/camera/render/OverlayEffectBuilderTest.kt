package com.bugzz.filter.camera.render

import androidx.camera.core.CameraEffect
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Nyquist unit test for [OverlayEffectBuilder] target mask + queueDepth (CAM-06 / D-25).
 *
 * Pins the three-stream compositing contract: the OverlayEffect MUST target
 * Preview + VideoCapture + ImageCapture so a single draw loop composites into
 * all three outputs. This is the architectural load-bearing decision of Phase 2
 * (proven by recording a 5-second MP4 and verifying the debug red rect is baked
 * into every frame).
 *
 * Plan 02-04 must expose [OverlayEffectBuilder.TARGETS] and
 * [OverlayEffectBuilder.QUEUE_DEPTH] as companion-object constants so this
 * test can pin the configuration without instantiating OverlayEffect itself
 * (OverlayEffect requires an Android Handler/HandlerThread and is not
 * unit-testable in a plain JVM harness).
 *
 * Intentional Nyquist RED: [OverlayEffectBuilder] does not yet exist;
 * Plan 02-04 lands it.
 */
class OverlayEffectBuilderTest {

    @Test
    fun target_mask_covers_preview_image_video() {
        val expected = CameraEffect.PREVIEW or CameraEffect.VIDEO_CAPTURE or CameraEffect.IMAGE_CAPTURE
        assertEquals(
            "OverlayEffect targets must cover Preview+Video+Image (CAM-06 / D-25)",
            expected,
            OverlayEffectBuilder.TARGETS,
        )
    }

    @Test
    fun queue_depth_is_zero() {
        assertEquals(
            "OverlayEffect queueDepth must be 0 for per-frame drawing (CAM-06)",
            0,
            OverlayEffectBuilder.QUEUE_DEPTH,
        )
    }
}
