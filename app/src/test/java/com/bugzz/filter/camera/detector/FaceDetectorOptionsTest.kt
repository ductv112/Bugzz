package com.bugzz.filter.camera.detector

import com.google.mlkit.vision.face.FaceDetectorOptions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Nyquist unit test for [FaceDetectorClient.buildOptions] (CAM-04 + D-15).
 *
 * Pins the exact ML Kit FaceDetectorOptions contract the face detection
 * pipeline must honour. Any drift from D-15 (e.g. enabling landmark mode
 * alongside contour mode, or raising minFaceSize) is caught here — see
 * PITFALLS.md #3 (contour + landmarks + classification simultaneously
 * degrades accuracy).
 *
 * Intentional Nyquist RED: [FaceDetectorClient] does not yet exist;
 * Plan 02-03 lands the companion `buildOptions()` factory.
 */
class FaceDetectorOptionsTest {

    @Test
    fun options_configured_per_D15() {
        val opts = FaceDetectorClient.buildOptions()

        assertEquals(
            "performanceMode must be PERFORMANCE_MODE_FAST (D-15)",
            FaceDetectorOptions.PERFORMANCE_MODE_FAST,
            opts.performanceMode,
        )
        assertEquals(
            "contourMode must be CONTOUR_MODE_ALL (D-15)",
            FaceDetectorOptions.CONTOUR_MODE_ALL,
            opts.contourMode,
        )
        assertTrue(
            "tracking must be enabled (D-15 / CAM-08 — trackingId stability)",
            opts.isTrackingEnabled,
        )
        assertEquals(
            "minFaceSize must be 0.15f (D-15)",
            0.15f,
            opts.minFaceSize,
            1e-6f,
        )
        assertEquals(
            "landmarkMode must be NONE (D-15 — contour + landmarks together degrades)",
            FaceDetectorOptions.LANDMARK_MODE_NONE,
            opts.landmarkMode,
        )
        assertEquals(
            "classificationMode must be NONE (D-15)",
            FaceDetectorOptions.CLASSIFICATION_MODE_NONE,
            opts.classificationMode,
        )
    }
}
