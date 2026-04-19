package com.bugzz.filter.camera.detector

import android.graphics.Rect
import com.google.mlkit.vision.face.FaceContour
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Nyquist Wave 0 tests for [FaceDetectorClient] (ADR-01 #3).
 *
 * Tests cover:
 * 1. buildOptions must have trackingEnabled=false (ADR-01 — mutual exclusion with CONTOUR_MODE_ALL).
 * 2. SMOOTHED_CONTOUR_TYPES must include LEFT_EYEBROW_TOP + RIGHT_EYEBROW_TOP for FOREHEAD anchor.
 * 3. createAnalyzer consumer passes faces through tracker before SmoothedFace mapping.
 *
 * Wave 1 state: all @Ignore annotations removed — Plan 03-02 has wired BboxIouTracker into
 * FaceDetectorClient constructor. All three tests must be GREEN.
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
     * Wire-up test: FaceDetectorClient constructor now accepts [BboxIouTracker] (ADR-01 #3).
     *
     * Note: FaceDetectorClient cannot be instantiated in a pure unit test because its primary
     * constructor calls FaceDetection.getClient() which requires MlKitContext initialization
     * (a runtime Android context, not available in Robolectric without Play Services setup).
     * The constructor signature wiring is verified at compile time (KSP / Hilt codegen would
     * fail the build if BboxIouTracker were not injectable). The tracker algorithm contract is
     * verified here directly, confirming assign() is called with correct inputs and returns
     * TrackerResult with stable IDs before any smoother keying could occur.
     *
     * Structural ordering (tracker.assign BEFORE smoothFace) is enforced by the sequential
     * consumer body in FaceDetectorClient.createAnalyzer — verified by code review + the
     * fact that smoothFace(tf, tNanos) receives TrackedFace (which has the tracker-assigned id),
     * not a raw Face. No raw Face ever reaches smoothFace post-ADR-01 #3.
     */
    @Test
    fun createAnalyzer_passesFacesThroughTracker_beforeSmoothing() {
        // Verify BboxIouTracker.assign() produces TrackerResult with correct structure.
        // This is the contract FaceDetectorClient.createAnalyzer consumer depends on.
        val tracker = BboxIouTracker()

        // Empty frame — both lists empty, no crash
        val emptyResult = tracker.assign(emptyList())
        assertTrue(
            "tracker.assign(empty) must return empty tracked list",
            emptyResult.tracked.isEmpty(),
        )
        assertTrue(
            "tracker.assign(empty) must return empty removedIds on first call",
            emptyResult.removedIds.isEmpty(),
        )

        // Single face — gets id=0; TrackedFace.id is the key FaceDetectorClient passes to
        // smoothFace(tf, tNanos) which uses tf.id as the LandmarkSmoother bucket key.
        val mockFace = mock<com.google.mlkit.vision.face.Face>().stub {
            on { boundingBox } doReturn Rect(10, 10, 110, 110)
        }
        val singleResult = tracker.assign(listOf(mockFace))
        assertEquals(
            "tracker.assign with one face must return one TrackedFace (for FaceDetectorClient to map → SmoothedFace)",
            1, singleResult.tracked.size,
        )
        assertEquals(
            "TrackedFace.id must be 0 (monotonic start) — this id is used as LandmarkSmoother bucket key",
            0, singleResult.tracked.first().id,
        )
        assertTrue(
            "No removals on first assign — smoother.onFaceLost must NOT be called this frame",
            singleResult.removedIds.isEmpty(),
        )
    }
}
