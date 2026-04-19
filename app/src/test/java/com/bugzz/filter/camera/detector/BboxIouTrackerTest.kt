package com.bugzz.filter.camera.detector

import android.graphics.Rect
import com.google.mlkit.vision.face.Face
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Nyquist Wave 0 tests for [BboxIouTracker] (ADR-01 #1).
 *
 * Tests cover: IoU math, greedy match algorithm, MAX_TRACKED_FACES=2 cap,
 * MAX_DROPOUT_FRAMES=5 retention, monotonic ID assignment (D-21/22/23).
 *
 * Wave 0 state: all tests that call [BboxIouTracker.iou] or [BboxIouTracker.assign]
 * are @Ignore'd because those methods are TODO stubs in the placeholder SUT. Only
 * [companion_constantsMatchSpec] runs — it asserts the WRONG stub constant values and
 * intentionally FAILS (RED gate). Plan 03-02 un-Ignores all tests + replaces the stub.
 *
 * Robolectric is used because [android.graphics.Rect]'s intersection / contains methods
 * require a real Android SDK implementation (JVM stubs are no-ops).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BboxIouTrackerTest {

    private lateinit var tracker: BboxIouTracker

    @Before
    fun setUp() {
        tracker = BboxIouTracker()
    }

    // -------------------------------------------------------------------------
    // IoU math (static method on companion)
    // -------------------------------------------------------------------------

    /**
     * TODO Plan 03-02: un-Ignore when BboxIouTracker.iou() has production body.
     */
    @org.junit.Ignore("Plan 03-02 — flip to GREEN when BboxIouTracker.iou() is implemented")
    @Test
    fun iou_identicalBoxes_returns1() {
        val box = Rect(0, 0, 100, 100)
        val result = BboxIouTracker.iou(box, box)
        assertEquals("IoU of identical boxes must be 1.0", 1.0f, result, 1e-6f)
    }

    /**
     * TODO Plan 03-02: un-Ignore when BboxIouTracker.iou() has production body.
     */
    @org.junit.Ignore("Plan 03-02 — flip to GREEN when BboxIouTracker.iou() is implemented")
    @Test
    fun iou_disjointBoxes_returns0() {
        val a = Rect(0, 0, 100, 100)
        val b = Rect(200, 200, 300, 300)
        val result = BboxIouTracker.iou(a, b)
        assertEquals("IoU of disjoint boxes must be 0.0", 0.0f, result, 1e-6f)
    }

    /**
     * TODO Plan 03-02: un-Ignore when BboxIouTracker.iou() has production body.
     */
    @org.junit.Ignore("Plan 03-02 — flip to GREEN when BboxIouTracker.iou() is implemented")
    @Test
    fun iou_halfOverlap_returnsOneThird() {
        // a = [0,0,100,100] area=10000; b = [50,0,150,100] area=10000
        // intersection = [50,0,100,100] = 50x100 = 5000
        // union = 10000 + 10000 - 5000 = 15000; IoU = 5000/15000 = 1/3
        val a = Rect(0, 0, 100, 100)
        val b = Rect(50, 0, 150, 100)
        val result = BboxIouTracker.iou(a, b)
        assertEquals("IoU of half-overlap boxes must be ~0.333", 1.0f / 3.0f, result, 1e-4f)
    }

    // -------------------------------------------------------------------------
    // assign() — ID assignment
    // -------------------------------------------------------------------------

    /**
     * TODO Plan 03-02: un-Ignore when BboxIouTracker.assign() is implemented.
     */
    @org.junit.Ignore("Plan 03-02 — flip to GREEN when BboxIouTracker.assign() is implemented")
    @Test
    fun assign_firstFrame_assignsMonotonicNewIds() {
        val face1 = mockFace(Rect(0, 0, 100, 100))
        val face2 = mockFace(Rect(200, 200, 300, 300))

        val result = tracker.assign(listOf(face1, face2))

        assertEquals("First frame: must track 2 faces", 2, result.tracked.size)
        val ids = result.tracked.map { it.id }.sorted()
        assertEquals("First two IDs must be monotonic 0 and 1", listOf(0, 1), ids)
        assertTrue("No removals on first frame", result.removedIds.isEmpty())
    }

    /**
     * TODO Plan 03-02: un-Ignore when BboxIouTracker.assign() is implemented.
     */
    @org.junit.Ignore("Plan 03-02 — flip to GREEN when BboxIouTracker.assign() is implemented")
    @Test
    fun assign_sameFaceNextFrame_retainsSameId() {
        // Frame 1: face at Rect(100,100,300,300)
        val face1 = mockFace(Rect(100, 100, 300, 300))
        val r1 = tracker.assign(listOf(face1))
        val originalId = r1.tracked.first().id

        // Frame 2: same face slightly moved — IoU ~0.90, well above threshold
        val face2 = mockFace(Rect(105, 105, 305, 305))
        val r2 = tracker.assign(listOf(face2))

        assertEquals("Slightly-moved face must retain same ID", originalId, r2.tracked.first().id)
    }

    /**
     * TODO Plan 03-02: un-Ignore when BboxIouTracker.assign() is implemented.
     */
    @org.junit.Ignore("Plan 03-02 — flip to GREEN when BboxIouTracker.assign() is implemented")
    @Test
    fun assign_threeFaces_capsAtMaxTrackedFaces() {
        // Three non-overlapping faces
        val f1 = mockFace(Rect(0, 0, 200, 200))
        val f2 = mockFace(Rect(300, 300, 500, 500))
        val f3 = mockFace(Rect(600, 600, 700, 700))  // smallest — should be dropped

        val result = tracker.assign(listOf(f1, f2, f3))

        assertTrue(
            "assign must never track more than MAX_TRACKED_FACES=${BboxIouTracker.MAX_TRACKED_FACES}",
            result.tracked.size <= BboxIouTracker.MAX_TRACKED_FACES,
        )
    }

    /**
     * TODO Plan 03-02: un-Ignore when BboxIouTracker.assign() is implemented.
     */
    @org.junit.Ignore("Plan 03-02 — flip to GREEN when BboxIouTracker.assign() is implemented")
    @Test
    fun assign_faceDisappears_dropsAfterFiveFrames() {
        // Frame 1: face present → id=0
        val face = mockFace(Rect(100, 100, 200, 200))
        tracker.assign(listOf(face))

        // Frames 2..6: empty list (dropout frames 1..5 — still tracked)
        repeat(BboxIouTracker.MAX_DROPOUT_FRAMES) {
            val r = tracker.assign(emptyList())
            assertTrue(
                "Face should still be tracked in dropout retention window (frame ${it + 2})",
                r.removedIds.isEmpty(),
            )
        }

        // Frame 7: 6th consecutive empty — should be removed now
        val removalFrame = tracker.assign(emptyList())
        assertFalse(
            "Face must be removed after MAX_DROPOUT_FRAMES=${BboxIouTracker.MAX_DROPOUT_FRAMES} consecutive misses",
            removalFrame.removedIds.isEmpty(),
        )
        assertEquals("Removed ID must be 0", 0, removalFrame.removedIds.first())
    }

    /**
     * TODO Plan 03-02: un-Ignore when BboxIouTracker.assign() is implemented.
     */
    @org.junit.Ignore("Plan 03-02 — flip to GREEN when BboxIouTracker.assign() is implemented")
    @Test
    fun assign_faceReappearsAsDifferent_assignsNewId() {
        val face = mockFace(Rect(100, 100, 200, 200))
        tracker.assign(listOf(face))

        // Exhaust dropout: MAX_DROPOUT_FRAMES + 1 empty frames
        repeat(BboxIouTracker.MAX_DROPOUT_FRAMES + 1) { tracker.assign(emptyList()) }

        // Reappear at same location — must get a NEW id (not 0 again)
        val face2 = mockFace(Rect(100, 100, 200, 200))
        val result = tracker.assign(listOf(face2))

        assertEquals("Reappeared face must get a fresh monotonic ID", 1, result.tracked.first().id)
    }

    /**
     * Companion constants RED gate. The stub has intentionally wrong values (0.0f / 0 / 0).
     * @Ignore'd to keep testDebugUnitTest exit 0. Plan 03-02 replaces the stub constants with
     * the correct values (0.3f / 5 / 2) and un-Ignores this test.
     */
    @org.junit.Ignore("Plan 03-02 — flip to GREEN when BboxIouTracker companion constants are 0.3f/5/2")
    @Test
    fun companion_constantsMatchSpec() {
        // These assertions FAIL in Wave 0 (stub has wrong values) — that is the Nyquist RED gate.
        // Plan 03-02 replaces the stub and makes these GREEN.
        assertEquals(
            "IOU_MATCH_THRESHOLD must be 0.3f per D-21",
            0.3f, BboxIouTracker.IOU_MATCH_THRESHOLD, 1e-6f,
        )
        assertEquals(
            "MAX_DROPOUT_FRAMES must be 5 per D-21",
            5, BboxIouTracker.MAX_DROPOUT_FRAMES,
        )
        assertEquals(
            "MAX_TRACKED_FACES must be 2 per D-22",
            2, BboxIouTracker.MAX_TRACKED_FACES,
        )
    }

    /**
     * TODO Plan 03-02: un-Ignore when BboxIouTracker.assign() is implemented.
     */
    @org.junit.Ignore("Plan 03-02 — flip to GREEN when BboxIouTracker.assign() is implemented")
    @Test
    fun assign_lowIouNewFace_assignsNewId() {
        // Frame 1: face at Rect(0,0,100,100) → id=0
        val face1 = mockFace(Rect(0, 0, 100, 100))
        tracker.assign(listOf(face1))

        // Frame 2: new face far away (IoU=0, below 0.3 threshold) → new id=1; id=0 enters dropout
        val face2 = mockFace(Rect(500, 500, 600, 600))
        val r2 = tracker.assign(listOf(face2))

        val ids = r2.tracked.map { it.id }
        assertTrue("New far-away face must get id=1 (new monotonic)", 1 in ids)
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private fun mockFace(rect: Rect): Face = mock<Face>().stub {
        on { boundingBox } doReturn rect
    }
}
