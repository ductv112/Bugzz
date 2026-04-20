package com.bugzz.filter.camera.render

import android.graphics.PointF
import android.graphics.Rect
import com.bugzz.filter.camera.detector.SmoothedFace
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Nyquist Wave 0 tests for the [FilterEngine.perFaceState] ConcurrentHashMap lifecycle (D-13).
 *
 * Tests pin the invariants for:
 *   1. setFilter() → clears entire perFaceState map (filter swap resets per-face state)
 *   2. onFaceLost(trackingId) → removes only that entry, leaving others intact
 *   3. getOrPut pattern → creates a fresh BehaviorState entry for a new trackingId
 *
 * **All tests are @Ignore'd** — Plan 04-04 Task 1 lands the extended FilterEngine with
 * perFaceState ConcurrentHashMap + onFaceLost + updated onDraw signature and un-Ignores these
 * tests (RED → GREEN transition).
 *
 * Types referenced in comments below land in Plan 04-03/04-04:
 *   - BehaviorState sealed interface (04-03)
 *   - BehaviorState.Static(pos: PointF) (04-03)
 *   - FilterEngine.perFaceState: ConcurrentHashMap<Int, BehaviorState> (04-04, internal-visible)
 *   - FilterEngine.onFaceLost(trackingId: Int) (04-04)
 *   - FilterEngine.onDraw(canvas, frame, faces: List<SmoothedFace>) (04-04 — new list signature)
 *
 * Robolectric needed for PointF + Bitmap + Canvas real implementations used by FilterEngine
 * and its helpers.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BehaviorStateMapTest {

    // -------------------------------------------------------------------------
    // D-13: setFilter clears perFaceState map
    // -------------------------------------------------------------------------

    /**
     * D-13: calling setFilter(filterB) after the map has been populated (via previous onDraw
     * calls) must clear the entire perFaceState map.
     *
     * Rationale: BehaviorState is filter-specific (a CRAWL progress value for filter A is
     * meaningless for filter B). On swap, all per-face state resets.
     */
    @Test
    @Ignore("TODO Plan 04-04 Task 1 — un-Ignore when FilterEngine.perFaceState exposed + setFilter clears it")
    fun setFilter_clearsPerFaceStateMap() {
        // val mockAssetLoader: AssetLoader = mock()
        // val engine = FilterEngine(mockAssetLoader)
        // val filterA = buildStaticFilter("ant_on_nose_v1", "sprites/ant_on_nose_v1")
        // val filterB = buildStaticFilter("spider_nose_static", "sprites/sprite_spider")
        //
        // // Populate map via 2 onDraw calls with distinct trackingIds
        // val face1 = buildFace(trackingId = 1)
        // val face2 = buildFace(trackingId = 2)
        // val mockCanvas: Canvas = mock()
        // val mockFrame: Frame = mock<Frame>().stub {
        //     on { timestampNanos } doReturn 0L
        //     on { overlayCanvas } doReturn mockCanvas
        // }
        // mockAssetLoader.stub { on { get(any(), any()) } doReturn null }
        // engine.setFilter(filterA)
        // engine.onDraw(mockCanvas, mockFrame, faces = listOf(face1, face2))
        // // Map should have 2 entries now
        // assertEquals(2, engine.perFaceState.size)
        //
        // // Swap to filterB — must clear map
        // engine.setFilter(filterB)
        // assertEquals("perFaceState must be empty after setFilter swap",
        //     0, engine.perFaceState.size)
    }

    // -------------------------------------------------------------------------
    // D-13: onFaceLost removes only the specified trackingId
    // -------------------------------------------------------------------------

    /**
     * D-13: onFaceLost(id=1) when map contains {1 → stateA, 2 → stateB} must:
     *   - Remove entry with key=1
     *   - Leave entry with key=2 intact
     * Other entries must not be cleared.
     */
    @Test
    @Ignore("TODO Plan 04-04 Task 1 — un-Ignore when FilterEngine.onFaceLost + perFaceState lands")
    fun onFaceLost_removesOnlyThatTrackingId() {
        // val mockAssetLoader: AssetLoader = mock()
        // val engine = FilterEngine(mockAssetLoader)
        // val filter = buildStaticFilter("spider_nose_static", "sprites/sprite_spider")
        // mockAssetLoader.stub { on { get(any(), any()) } doReturn null }
        // engine.setFilter(filter)
        //
        // // Populate map with 2 entries
        // val mockCanvas: Canvas = mock()
        // val mockFrame: Frame = mock<Frame>().stub {
        //     on { timestampNanos } doReturn 0L
        //     on { overlayCanvas } doReturn mockCanvas
        // }
        // engine.onDraw(mockCanvas, mockFrame, faces = listOf(buildFace(1), buildFace(2)))
        // assertEquals("map must have 2 entries before onFaceLost", 2, engine.perFaceState.size)
        //
        // engine.onFaceLost(trackingId = 1)
        //
        // assertEquals("perFaceState must have 1 entry after onFaceLost(1)",
        //     1, engine.perFaceState.size)
        // assertFalse("entry for id=1 must be removed", engine.perFaceState.containsKey(1))
        // assertTrue("entry for id=2 must still exist", engine.perFaceState.containsKey(2))
    }

    // -------------------------------------------------------------------------
    // D-13: getOrPut creates fresh BehaviorState for new trackingId
    // -------------------------------------------------------------------------

    /**
     * D-13: when onDraw encounters a face with a trackingId not yet in perFaceState,
     * a fresh BehaviorState must be created and inserted.
     *
     * For a STATIC filter, the initial state must be BehaviorState.Static(pos = PointF(0,0))
     * or equivalent. The state must exist in the map after the draw call.
     */
    @Test
    @Ignore("TODO Plan 04-04 Task 1 — un-Ignore when FilterEngine.onDraw getOrPut path lands")
    fun getOrPut_createsFreshStateForNewTrackingId() {
        // val mockAssetLoader: AssetLoader = mock()
        // val engine = FilterEngine(mockAssetLoader)
        // val filter = buildStaticFilter("spider_nose_static", "sprites/sprite_spider")
        // mockAssetLoader.stub { on { get(any(), any()) } doReturn null }
        // engine.setFilter(filter)
        //
        // // Map is empty before first onDraw
        // assertEquals("perFaceState must be empty before first onDraw", 0, engine.perFaceState.size)
        //
        // val mockCanvas: Canvas = mock()
        // val mockFrame: Frame = mock<Frame>().stub {
        //     on { timestampNanos } doReturn 0L
        //     on { overlayCanvas } doReturn mockCanvas
        // }
        // engine.onDraw(mockCanvas, mockFrame, faces = listOf(buildFace(trackingId = 5)))
        //
        // assertEquals("perFaceState must have 1 entry after first onDraw",
        //     1, engine.perFaceState.size)
        // assertTrue("entry for trackingId=5 must be created",
        //     engine.perFaceState.containsKey(5))
        // assertTrue("created state must be BehaviorState.Static for a STATIC filter",
        //     engine.perFaceState[5] is BehaviorState.Static)
    }

    // -------------------------------------------------------------------------
    // D-13: perFaceState survives multiple ticks for same trackingId (state continuity)
    // -------------------------------------------------------------------------

    /**
     * D-13: the SAME BehaviorState instance must be reused across consecutive onDraw calls
     * for the same trackingId (not re-created each frame).
     *
     * This ensures Crawl progress and Swarm positions accumulate over time rather than
     * resetting to initial values every frame.
     */
    @Test
    @Ignore("TODO Plan 04-04 Task 1 — un-Ignore when FilterEngine.onDraw state-continuity confirmed")
    fun perFaceState_sameInstanceReusedAcrossFrames() {
        // val mockAssetLoader: AssetLoader = mock()
        // val engine = FilterEngine(mockAssetLoader)
        // val filter = buildStaticFilter("spider_nose_static", "sprites/sprite_spider")
        // mockAssetLoader.stub { on { get(any(), any()) } doReturn null }
        // engine.setFilter(filter)
        //
        // val mockCanvas: Canvas = mock()
        // val frame1: Frame = mock<Frame>().stub {
        //     on { timestampNanos } doReturn 0L
        //     on { overlayCanvas } doReturn mockCanvas
        // }
        // val frame2: Frame = mock<Frame>().stub {
        //     on { timestampNanos } doReturn 16_000_000L
        //     on { overlayCanvas } doReturn mockCanvas
        // }
        // val face = buildFace(trackingId = 3)
        //
        // engine.onDraw(mockCanvas, frame1, faces = listOf(face))
        // val stateAfterFrame1 = engine.perFaceState[3]
        //
        // engine.onDraw(mockCanvas, frame2, faces = listOf(face))
        // val stateAfterFrame2 = engine.perFaceState[3]
        //
        // assertSame("same BehaviorState instance must be used across consecutive frames",
        //     stateAfterFrame1, stateAfterFrame2)
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private fun buildFace(
        trackingId: Int = 1,
        boundingBox: Rect = Rect(100, 100, 300, 400),
        contours: Map<Int, List<PointF>> = emptyMap(),
    ): SmoothedFace = SmoothedFace(trackingId, boundingBox, contours, emptyMap())

    // Note: buildStaticFilter() references FilterDefinition + BugBehavior.Static — both
    // exist in Phase 3 production source. Method is commented out here to avoid import
    // chain that might break compilation in a future refactor; un-Ignore removes the comment.
    //
    // private fun buildStaticFilter(id: String, assetDir: String) = FilterDefinition(
    //     id = id, displayName = id, anchorType = FaceLandmarkMapper.Anchor.NOSE_TIP,
    //     behavior = BugBehavior.Static, frameCount = 1, frameDurationMs = 100L,
    //     scaleFactor = 0.2f, assetDir = assetDir
    // )
}
