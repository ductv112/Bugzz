package com.bugzz.filter.camera.render

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.PointF
import android.graphics.Rect
import android.util.Size
import androidx.camera.effects.Frame
import com.bugzz.filter.camera.detector.FaceLandmarkMapper
import com.bugzz.filter.camera.detector.SmoothedFace
import com.bugzz.filter.camera.filter.AssetLoader
import com.bugzz.filter.camera.filter.FilterDefinition
import com.google.mlkit.vision.face.FaceContour
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Tests for [FilterEngine.perFaceState] ConcurrentHashMap lifecycle (D-13).
 *
 * Plan 04-04 Task 1 un-Ignores these — RED → GREEN transition.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BehaviorStateMapTest {

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private fun buildEngine(): Pair<FilterEngine, AssetLoader> {
        val mockAssetLoader: AssetLoader = mock()
        return FilterEngine(mockAssetLoader) to mockAssetLoader
    }

    private fun buildStaticFilter(id: String = "spider_nose_static", assetDir: String = "sprites/sprite_spider") =
        FilterDefinition(
            id = id,
            displayName = id,
            anchorType = FaceLandmarkMapper.Anchor.NOSE_TIP,
            behavior = BugBehavior.Static,
            frameCount = 1,
            frameDurationMs = 100L,
            scaleFactor = 0.2f,
            assetDir = assetDir,
        )

    private fun buildFrame(): Frame = mock<Frame>().stub {
        on { timestampNanos } doReturn 0L
        on { size } doReturn Size(640, 480)
    }

    private fun buildFace(
        trackingId: Int = 1,
        boundingBox: Rect = Rect(100, 100, 300, 400),
        contours: Map<Int, List<PointF>> = mapOf(
            FaceContour.NOSE_BRIDGE to listOf(PointF(200f, 200f))
        ),
    ): SmoothedFace = SmoothedFace(trackingId, boundingBox, contours, emptyMap())

    private fun buildCanvas(): Canvas = mock()

    // -------------------------------------------------------------------------
    // D-13: setFilter clears perFaceState map
    // -------------------------------------------------------------------------

    @Test
    fun setFilter_clearsPerFaceStateMap() {
        val (engine, mockAssetLoader) = buildEngine()
        val filterA = buildStaticFilter("ant_on_nose_v1", "sprites/ant_on_nose_v1")
        val filterB = buildStaticFilter("spider_nose_static", "sprites/sprite_spider")

        val fakeBitmap = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888)
        mockAssetLoader.stub { on { get(any(), any()) } doReturn fakeBitmap }

        val mockCanvas = buildCanvas()
        val mockFrame = buildFrame()

        engine.setFilter(filterA)
        engine.onDraw(mockCanvas, mockFrame, listOf(buildFace(1), buildFace(2)))
        assertEquals("map must have 2 entries before swap", 2, engine.perFaceState.size)

        // Swap to filterB — must clear map
        engine.setFilter(filterB)
        assertEquals("perFaceState must be empty after setFilter swap",
            0, engine.perFaceState.size)
    }

    // -------------------------------------------------------------------------
    // D-13: onFaceLost removes only the specified trackingId
    // -------------------------------------------------------------------------

    @Test
    fun onFaceLost_removesOnlyThatTrackingId() {
        val (engine, mockAssetLoader) = buildEngine()
        val filter = buildStaticFilter()
        val fakeBitmap = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888)
        mockAssetLoader.stub { on { get(any(), any()) } doReturn fakeBitmap }
        engine.setFilter(filter)

        val mockCanvas = buildCanvas()
        val mockFrame = buildFrame()
        engine.onDraw(mockCanvas, mockFrame, listOf(buildFace(1), buildFace(2)))
        assertEquals("map must have 2 entries before onFaceLost", 2, engine.perFaceState.size)

        engine.onFaceLost(trackingId = 1)

        assertEquals("perFaceState must have 1 entry after onFaceLost(1)",
            1, engine.perFaceState.size)
        assertFalse("entry for id=1 must be removed", engine.perFaceState.containsKey(1))
        assertTrue("entry for id=2 must still exist", engine.perFaceState.containsKey(2))
    }

    // -------------------------------------------------------------------------
    // D-13: getOrPut creates fresh BehaviorState for new trackingId
    // -------------------------------------------------------------------------

    @Test
    fun getOrPut_createsFreshStateForNewTrackingId() {
        val (engine, mockAssetLoader) = buildEngine()
        val filter = buildStaticFilter()
        // Return null bitmap — state still gets created even if draw is skipped
        mockAssetLoader.stub { on { get(any(), any()) } doReturn null }
        engine.setFilter(filter)

        assertEquals("perFaceState must be empty before first onDraw", 0, engine.perFaceState.size)

        val mockCanvas = buildCanvas()
        val mockFrame = buildFrame()
        engine.onDraw(mockCanvas, mockFrame, listOf(buildFace(trackingId = 5)))

        assertEquals("perFaceState must have 1 entry after first onDraw",
            1, engine.perFaceState.size)
        assertTrue("entry for trackingId=5 must be created",
            engine.perFaceState.containsKey(5))
        assertTrue("created state must be BehaviorState.Static for a STATIC filter",
            engine.perFaceState[5] is BehaviorState.Static)
    }

    // -------------------------------------------------------------------------
    // D-13: perFaceState survives multiple ticks for same trackingId (state continuity)
    // -------------------------------------------------------------------------

    @Test
    fun perFaceState_sameInstanceReusedAcrossFrames() {
        val (engine, mockAssetLoader) = buildEngine()
        val filter = buildStaticFilter()
        mockAssetLoader.stub { on { get(any(), any()) } doReturn null }
        engine.setFilter(filter)

        val mockCanvas = buildCanvas()
        val frame1 = mock<Frame>().stub {
            on { timestampNanos } doReturn 0L
            on { size } doReturn Size(640, 480)
        }
        val frame2 = mock<Frame>().stub {
            on { timestampNanos } doReturn 16_000_000L
            on { size } doReturn Size(640, 480)
        }
        val face = buildFace(trackingId = 3)

        engine.onDraw(mockCanvas, frame1, listOf(face))
        val stateAfterFrame1 = engine.perFaceState[3]

        engine.onDraw(mockCanvas, frame2, listOf(face))
        val stateAfterFrame2 = engine.perFaceState[3]

        assertSame("same BehaviorState instance must be used across consecutive frames",
            stateAfterFrame1, stateAfterFrame2)
    }
}
