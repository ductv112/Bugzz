package com.bugzz.filter.camera.render

import android.graphics.PointF
import android.graphics.Rect
import com.bugzz.filter.camera.detector.SmoothedFace
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Tests for the CRAWL behavior (D-08 / REN-02).
 *
 * Un-Ignored Plan 04-03 — BehaviorState.Crawl + CRAWL tick impl landed.
 *
 * Robolectric needed for PointF real implementation.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CrawlBehaviorTest {

    // -------------------------------------------------------------------------
    // D-08: progress advances per dt + faceBoxWidth / previewWidth formula
    // -------------------------------------------------------------------------

    /**
     * D-08: progress += dtSeconds * 0.5f * faceBoxWidth / previewWidth
     *
     * With dtMs=1000 (1 second), faceBoxWidth=200 (bbox.right - bbox.left),
     * previewWidth=400 → progressDelta = 1.0 * 0.5 * (200/400) = 0.25.
     * Starting progress=0f → after tick, progress ≈ 0.25f.
     */
    @Test
    fun crawl_progressAdvancesPerDtMs() {
        val state = BehaviorState.Crawl(progress = 0f, direction = CrawlDirection.CW)
        val face = buildFace(boundingBox = Rect(100, 100, 300, 400)) // width=200
        val anchor = PointF(200f, 250f)
        BugBehavior.Crawl.tick(
            state, face, anchor,
            previewWidth = 400f, previewHeight = 800f,
            tsNanos = 1_000_000_000L, dtMs = 1000L,
        )
        // D-08: delta = 1.0 * 0.5 * 200 / 400 = 0.25
        assertEquals(
            "progress after 1s with 200px bbox on 400px preview must be ~0.25f",
            0.25f, state.progress, 0.01f,
        )
    }

    /**
     * D-08: progress wraps at 1.0 via modulo — never exceeds or equals 1.0.
     *
     * Starting progress=0.95f; dtMs=1000 → delta=0.25 → 0.95+0.25=1.20 wraps to 0.20.
     */
    @Test
    fun crawl_progressWrapsAt1_0() {
        val state = BehaviorState.Crawl(progress = 0.95f, direction = CrawlDirection.CW)
        val face = buildFace(boundingBox = Rect(100, 100, 300, 400)) // width=200
        BugBehavior.Crawl.tick(
            state, face, PointF(200f, 250f),
            previewWidth = 400f, previewHeight = 800f,
            tsNanos = 1L, dtMs = 1000L,
        )
        assertTrue("progress must be >= 0f after wrap", state.progress >= 0f)
        assertTrue("progress must be < 1.0f after wrap", state.progress < 1.0f)
        // Wrapped value: (0.95 + 0.25) % 1.0 = 0.20 → must not have accumulated without wrap
        assertTrue("progress must not have accumulated without wrap", state.progress < 0.95f)
    }

    // -------------------------------------------------------------------------
    // D-08: contour linear interpolation — BugBehavior.crawlPosition() helper
    // -------------------------------------------------------------------------

    /**
     * D-08: crawlPosition interpolates linearly between adjacent contour vertices.
     *
     * Test fixture: 4-vertex axis-aligned square contour.
     *   Vertices: (0,0) → (100,0) → (100,100) → (0,100)
     *   progress=0.125 → scaled = 0.125 * 4 = 0.5 → vertex index 0, t=0.5
     *   → midpoint of (0,0)→(100,0) = (50, 0).
     */
    @Test
    fun crawlPosition_interpolatesLinearlyBetweenVertices() {
        val contour = listOf(
            PointF(0f, 0f),
            PointF(100f, 0f),
            PointF(100f, 100f),
            PointF(0f, 100f),
        )
        val result = BugBehavior.crawlPosition(contour, progress = 0.125f)
        assertEquals("crawlPosition x must interpolate to midpoint of first edge",
            50f, result.x, 0.01f)
        assertEquals("crawlPosition y must be 0 (on first horizontal edge)",
            0f, result.y, 0.01f)
    }

    /**
     * D-08: crawlPosition must throw IllegalArgumentException on empty contour (not IOOBE).
     */
    @Test
    fun crawlPosition_handlesEmptyContourGracefully() {
        val emptyContour = emptyList<PointF>()
        try {
            BugBehavior.crawlPosition(emptyContour, progress = 0.5f)
            org.junit.Assert.fail("Expected require() to throw on empty contour")
        } catch (e: IllegalArgumentException) {
            // OK — require() in crawlPosition throws IAE not IOOBE
        }
    }

    // -------------------------------------------------------------------------
    // D-08: CCW direction reverses traversal order
    // -------------------------------------------------------------------------

    /**
     * D-08: CrawlDirection.CCW should traverse in reverse, producing different progress
     * advancement than CW for the same dt (negative delta wraps differently).
     */
    @Test
    fun crawl_ccwDirection_producesOppositeTraversal() {
        val stateCw  = BehaviorState.Crawl(progress = 0f, direction = CrawlDirection.CW)
        val stateCcw = BehaviorState.Crawl(progress = 0f, direction = CrawlDirection.CCW)
        val face = buildFace(boundingBox = Rect(100, 100, 300, 400))
        val anchor = PointF(200f, 250f)

        BugBehavior.Crawl.tick(stateCw,  face, anchor,
            previewWidth = 400f, previewHeight = 800f, tsNanos = 1L, dtMs = 1000L)
        BugBehavior.Crawl.tick(stateCcw, face, anchor,
            previewWidth = 400f, previewHeight = 800f, tsNanos = 1L, dtMs = 1000L)

        // CW advances progress forward (0 → 0.25); CCW wraps backward (0 → 0.75)
        assertNotEquals("CW and CCW must produce different progress for same dt",
            stateCw.progress, stateCcw.progress, 0.001f)
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private fun buildFace(
        trackingId: Int = 1,
        boundingBox: Rect = Rect(100, 100, 300, 400),
        contours: Map<Int, List<PointF>> = emptyMap(),
    ): SmoothedFace = SmoothedFace(trackingId, boundingBox, contours, emptyMap())
}
