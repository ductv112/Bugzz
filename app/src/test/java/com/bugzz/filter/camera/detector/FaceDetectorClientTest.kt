package com.bugzz.filter.camera.detector

import com.google.mlkit.vision.face.FaceContour
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Nyquist Wave 0 tests for [FaceDetectorClient] (ADR-01 #3).
 *
 * Tests cover:
 * 1. buildOptions must have trackingEnabled=false (ADR-01 — mutual exclusion with CONTOUR_MODE_ALL).
 * 2. SMOOTHED_CONTOUR_TYPES must include LEFT_EYEBROW_TOP + RIGHT_EYEBROW_TOP for FOREHEAD anchor.
 * 3. @Ignore'd: createAnalyzer consumer passes faces through tracker before SmoothedFace mapping.
 *
 * Robolectric required because ML Kit FaceDetection.getClient() exercises Android internals.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FaceDetectorClientTest {

    @Test
    fun buildOptions_excludesEnableTracking_contourModeAll() {
        val opts = FaceDetectorClient.buildOptions()
        // ADR-01: ML Kit silently ignores .enableTracking() when CONTOUR_MODE_ALL is active.
        // We explicitly do NOT call .enableTracking(); the toString() must show trackingEnabled=false.
        val str = opts.toString()
        assertTrue(
            "FaceDetectorOptions must have trackingEnabled=false (ADR-01 — mutual exclusion with CONTOUR_MODE_ALL). Got: $str",
            str.contains("trackingEnabled=false"),
        )
    }

    @Test
    fun smoothedContourTypes_includesForeheadEyebrows() {
        // FOREHEAD anchor (D-02 / D-30) requires LEFT_EYEBROW_TOP (=2) and RIGHT_EYEBROW_TOP (=4)
        // to be enrolled in SMOOTHED_CONTOUR_TYPES, else anchorPoint(FOREHEAD) always hits bbox fallback.
        //
        // Access via reflection since SMOOTHED_CONTOUR_TYPES is a private companion val.
        val field = FaceDetectorClient::class.java
            .getDeclaredField("SMOOTHED_CONTOUR_TYPES")
            .also { it.isAccessible = true }

        // The field belongs to the companion object — get from companion instance
        val companionField = FaceDetectorClient::class.java
            .getDeclaredField("Companion")
            .also { it.isAccessible = true }
        val companion = companionField.get(null)

        @Suppress("UNCHECKED_CAST")
        val types = field.get(companion) as? List<Int>
            ?: throw AssertionError("SMOOTHED_CONTOUR_TYPES not found via reflection")

        assertTrue(
            "SMOOTHED_CONTOUR_TYPES must include FaceContour.LEFT_EYEBROW_TOP=${FaceContour.LEFT_EYEBROW_TOP} for FOREHEAD anchor (D-30 / 03-RESEARCH §Finding 3)",
            FaceContour.LEFT_EYEBROW_TOP in types,
        )
        assertEquals(
            "FaceContour.LEFT_EYEBROW_TOP constant value must be 2 per ML Kit API",
            2, FaceContour.LEFT_EYEBROW_TOP,
        )
        assertTrue(
            "SMOOTHED_CONTOUR_TYPES must include FaceContour.RIGHT_EYEBROW_TOP=${FaceContour.RIGHT_EYEBROW_TOP} for FOREHEAD anchor (D-30 / 03-RESEARCH §Finding 3)",
            FaceContour.RIGHT_EYEBROW_TOP in types,
        )
        assertEquals(
            "FaceContour.RIGHT_EYEBROW_TOP constant value must be 4 per ML Kit API",
            4, FaceContour.RIGHT_EYEBROW_TOP,
        )
    }

    /**
     * Wire-up test: FaceDetectorClient.createAnalyzer() consumer must call tracker.assign(faces)
     * BEFORE mapping to SmoothedFace (ADR-01 #3 — tracker IDs flow into LandmarkSmoother keying).
     *
     * @Ignore'd in Wave 0 because FaceDetectorClient's constructor does not yet accept a
     * BboxIouTracker parameter — that additive change lands in Plan 03-02 Task 1.
     * Un-Ignore when Plan 03-02 adds `@Inject constructor(..., tracker: BboxIouTracker)`.
     */
    @org.junit.Ignore("Plan 03-02 — flip to GREEN when tracker constructor param lands")
    @Test
    fun createAnalyzer_passesFacesThroughTracker_beforeSmoothing() {
        // TODO Plan 03-02: construct FaceDetectorClient with a mock BboxIouTracker;
        // simulate MlKitAnalyzer consumer via reflection or test seam;
        // verify tracker.assign(faces) called BEFORE smoother.smoothPoint invocations.
        // Ordering is critical: smoother keys on tracker-assigned ID, not face.trackingId.
    }
}
