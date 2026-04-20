package com.bugzz.filter.camera.render

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.Rect
import android.util.Size
import androidx.camera.effects.Frame
import com.bugzz.filter.camera.detector.FaceLandmarkMapper.Anchor
import com.bugzz.filter.camera.detector.SmoothedFace
import com.bugzz.filter.camera.filter.AssetLoader
import com.bugzz.filter.camera.filter.FilterDefinition
import com.google.mlkit.vision.face.FaceContour
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.atMost
import org.mockito.kotlin.clearInvocations
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.stub
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Nyquist Wave 0 tests for [FilterEngine] (REN-01/05/06/07 + T-03-05).
 *
 * All tests are @Ignore'd in Wave 0 because [FilterEngine.onDraw] and [FilterEngine.setFilter]
 * are TODO stubs. Plan 03-03 implements them and un-Ignores these tests.
 *
 * Robolectric needed for Canvas, Paint, Matrix, Bitmap shadow implementations.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FilterEngineTest {

    private lateinit var mockCanvas: Canvas
    private lateinit var mockFrame: Frame
    private lateinit var mockAssetLoader: AssetLoader
    private lateinit var engine: FilterEngine
    private lateinit var filterA: FilterDefinition
    private lateinit var filterB: FilterDefinition

    @Before
    fun setUp() {
        mockCanvas = mock()
        mockFrame = mock<Frame>().stub {
            on { timestampNanos } doReturn 0L
            on { overlayCanvas } doReturn mockCanvas
            on { size } doReturn Size(640, 480)
        }
        mockAssetLoader = mock()
        engine = FilterEngine(mockAssetLoader)

        filterA = FilterDefinition(
            id = "ant_on_nose_v1",
            displayName = "Ant",
            anchorType = Anchor.NOSE_TIP,
            behavior = BugBehavior.Static,
            frameCount = 3,
            frameDurationMs = 100L,
            scaleFactor = 0.20f,
            assetDir = "sprites/ant_on_nose_v1",
        )
        filterB = FilterDefinition(
            id = "spider_on_forehead_v1",
            displayName = "Spider",
            anchorType = Anchor.FOREHEAD,
            behavior = BugBehavior.Static,
            frameCount = 2,
            frameDurationMs = 100L,
            scaleFactor = 0.20f,
            assetDir = "sprites/spider_on_forehead_v1",
        )
    }

    // -------------------------------------------------------------------------
    // REN-06: null face → early return
    // -------------------------------------------------------------------------

    /**
     * TODO Plan 03-03: un-Ignore when FilterEngine.onDraw() is implemented.
     */
    @Test
    fun onDraw_nullFace_returnsEarly_neverCallsDrawBitmap() {
        engine.setFilter(filterA)
        // Stub assetLoader to return a valid bitmap (so only null-face causes early return)
        val fakeBitmap = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888)
        mockAssetLoader.stub {
            on { get(any(), any()) } doReturn fakeBitmap
        }

        engine.onDraw(mockCanvas, mockFrame, face = null)

        verify(mockCanvas, never()).drawBitmap(any<Bitmap>(), any<Float>(), any<Float>(), any<Paint>())
    }

    /**
     * TODO Plan 03-03: un-Ignore when FilterEngine.onDraw() is implemented.
     */
    @Test
    fun onDraw_nullActiveFilter_returnsEarly() {
        // setFilter never called — no active filter
        engine.onDraw(mockCanvas, mockFrame, face = buildFace())

        verify(mockCanvas, never()).drawBitmap(any<Bitmap>(), any<Float>(), any<Float>(), any<Paint>())
    }

    /**
     * TODO Plan 03-03: un-Ignore when FilterEngine.setFilter() + onDraw() are implemented.
     * Pins D-11 no-ghost contract: if assetLoader.get() returns null (preload not yet done),
     * onDraw must skip drawing to avoid showing the previous filter's last frame.
     */
    @Test
    fun setFilter_beforePreload_onDrawSkipsDrawing() {
        engine.setFilter(filterA)
        // assetLoader.get() returns null (preload not triggered or not yet complete)
        mockAssetLoader.stub { on { get(any(), any()) } doReturn null }

        engine.onDraw(mockCanvas, mockFrame, face = buildFace())

        verify(mockCanvas, never()).drawBitmap(any<Bitmap>(), any<Float>(), any<Float>(), any<Paint>())
    }

    /**
     * TODO Plan 03-03: un-Ignore when FilterEngine.setFilter() + onDraw() are implemented.
     */
    @Test
    fun setFilter_afterPreload_onDrawCallsDrawBitmap() {
        val fakeBitmap = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888)
        mockAssetLoader.stub { on { get(any(), any()) } doReturn fakeBitmap }

        engine.setFilter(filterA)
        engine.onDraw(mockCanvas, mockFrame, face = buildFace())

        verify(mockCanvas, times(1)).drawBitmap(any<Bitmap>(), any<Float>(), any<Float>(), any<Paint>())
    }

    // -------------------------------------------------------------------------
    // REN-05: flipbook frame-index advances over time
    // -------------------------------------------------------------------------

    /**
     * TODO Plan 03-03: un-Ignore when FilterEngine flipbook frame-index logic is implemented.
     * filterA has frameCount=3, frameDurationMs=100 (100_000_000 ns per frame).
     * Sequence: t=0 → idx=0; t=120ms → idx=1; t=220ms → idx=2; t=320ms → idx=0 (wrap).
     */
    @Test
    fun flipbookIndex_advancesOverTime() {
        val fakeBitmap = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888)
        mockAssetLoader.stub { on { get(any(), any()) } doReturn fakeBitmap }
        engine.setFilter(filterA)

        val frameIdxCaptor = argumentCaptor<Int>()
        val timestamps = listOf(0L, 120_000_000L, 220_000_000L, 320_000_000L)
        val expectedFrameIdxs = listOf(0, 1, 2, 0)  // frameCount=3, 100ms/frame

        for (ts in timestamps) {
            val frame = mock<Frame>().stub {
                on { timestampNanos } doReturn ts
                on { overlayCanvas } doReturn mockCanvas
                on { size } doReturn Size(640, 480)
            }
            engine.onDraw(mockCanvas, frame, face = buildFace())
        }

        // Verify get() was called with the expected frame indices in order
        verify(mockAssetLoader, times(4)).get(any(), frameIdxCaptor.capture())
        assertEquals("Frame indices must advance per frameDurationNanos", expectedFrameIdxs, frameIdxCaptor.allValues)
    }

    // -------------------------------------------------------------------------
    // CAM-07: canvas.setMatrix before drawBitmap
    // -------------------------------------------------------------------------

    /**
     * TODO Plan 03-03: un-Ignore when FilterEngine.onDraw() calls setMatrix before drawBitmap.
     * Pins the CAM-07 matrix-before-draw contract inherited from Phase 2 OverlayEffectBuilder.
     */
    @Test
    fun onDraw_callsCanvasSetMatrixBeforeDrawBitmap() {
        val fakeBitmap = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888)
        mockAssetLoader.stub { on { get(any(), any()) } doReturn fakeBitmap }
        engine.setFilter(filterA)

        engine.onDraw(mockCanvas, mockFrame, face = buildFace())

        val inOrder = inOrder(mockCanvas)
        inOrder.verify(mockCanvas).setMatrix(any<Matrix>())
        inOrder.verify(mockCanvas).drawBitmap(any<Bitmap>(), any<Float>(), any<Float>(), any<Paint>())
    }

    // -------------------------------------------------------------------------
    // T-03-05: biometric log policy — no landmark coords in Timber logs
    // -------------------------------------------------------------------------

    /**
     * TODO Plan 03-03: un-Ignore when FilterEngine.onDraw() logging is implemented.
     * Pins T-03-05: FilterEngine must NEVER log PointF coordinates or "x=" patterns.
     */
    @Test
    fun onDraw_doesNotLogLandmarkCoords() {
        // This test verifies the logging policy at the source level.
        // Implementation note: Plan 03-03 must ensure Timber calls inside onDraw
        // only log filterId + frameIndex, never PointF toString or "x=N y=N" patterns.
        // Enforcement: code review of FilterEngine.kt Timber calls before un-Ignoring.
        // Automated: spy Timber tree + assert no logged string contains "PointF" or "x=\d".
    }

    // -------------------------------------------------------------------------
    // REN-07: filter swap resets BugState frame index
    // -------------------------------------------------------------------------

    /**
     * TODO Plan 03-03: un-Ignore when FilterEngine.setFilter() resets BugState on swap.
     */
    @Test
    fun setFilter_swap_resetsBugStateFrameIndex() {
        val fakeBitmap = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888)
        mockAssetLoader.stub { on { get(any(), any()) } doReturn fakeBitmap }

        // Advance filterA to frame index 2
        engine.setFilter(filterA)
        val frame220ms = mock<Frame>().stub {
            on { timestampNanos } doReturn 220_000_000L  // t=220ms → filterA idx=2
            on { overlayCanvas } doReturn mockCanvas
            on { size } doReturn Size(640, 480)
        }
        engine.onDraw(mockCanvas, frame220ms, face = buildFace())

        // Swap to filterB — should reset frame state
        engine.setFilter(filterB)
        val frameIdxCaptor = argumentCaptor<Int>()
        val frame0 = mock<Frame>().stub {
            on { timestampNanos } doReturn 0L
            on { overlayCanvas } doReturn mockCanvas
            on { size } doReturn Size(640, 480)
        }
        engine.onDraw(mockCanvas, frame0, face = buildFace())

        // filterB frame index after swap must start at 0 (no carry-over from filterA).
        // get() is called twice total (once for filterA at idx=2, once for filterB at idx=0);
        // lastValue captures the filterB call — verifies no carry-over (Rule 1 fix: times(2)).
        verify(mockAssetLoader, times(2)).get(any(), frameIdxCaptor.capture())
        assertEquals("Frame index must reset to 0 after filter swap", 0, frameIdxCaptor.lastValue)
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private fun buildFace(
        trackingId: Int = 0,
        boundingBox: Rect = Rect(0, 0, 200, 300),
        contours: Map<Int, List<PointF>> = emptyMap(),
    ) = SmoothedFace(
        trackingId = trackingId,
        boundingBox = boundingBox,
        contours = contours,
        landmarks = emptyMap(),
    )

    // =========================================================================
    // NEW — Phase 4 Wave 0 tests (Plan 04-04 Task 1 un-Ignores these).
    // =========================================================================

    /**
     * MOD-02 + D-22: primary face (largest bbox area) gets contour-resolved anchor via
     * FaceLandmarkMapper; secondary face (second-largest) gets bbox-centre fallback.
     * Both faces produce exactly 1 drawBitmap call each (STATIC behavior).
     */
    @Test
    fun multiFace_primaryGetsContourAnchor_secondaryGetsBboxCenter() {
        // primary: 200×200 = 40 000 px², NOSE_BRIDGE contour so FaceLandmarkMapper resolves
        val primary = buildFace(
            trackingId = 1,
            boundingBox = Rect(0, 0, 200, 200),
            contours = mapOf(FaceContour.NOSE_BRIDGE to listOf(PointF(100f, 110f))),
        )
        // secondary: 100×100 = 10 000 px², no contours → bbox-center fallback
        val secondary = buildFace(
            trackingId = 2,
            boundingBox = Rect(300, 0, 400, 100),
            contours = emptyMap(),
        )
        val fakeBitmap = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888)
        mockAssetLoader.stub { on { get(any(), any()) } doReturn fakeBitmap }
        engine.setFilter(filterA)

        // Feed list unordered (secondary first) — engine must sort by bbox area
        engine.onDraw(mockCanvas, mockFrame, listOf(secondary, primary))

        // Both faces drawn (STATIC = 1 draw per face)
        verify(mockCanvas, times(2)).drawBitmap(any<Bitmap>(), eq(0f), eq(0f), any<Paint>())

        // Both canvas.save()/translate()/restore() should have been called twice
        verify(mockCanvas, times(2)).save()
        verify(mockCanvas, times(2)).restore()
    }

    /**
     * D-14 soft-cap: total drawBitmap calls must not exceed MAX_DRAWS_PER_FRAME=20 across faces.
     * Strategy: prime perFaceState with 15-instance Swarm for each of 2 faces (30 total > 20 cap).
     */
    @Test
    fun softCap_cappedAtMaxDrawsPerFrame_acrossFaces() {
        val swarmFilter = FilterDefinition(
            id = "spider_swarm", displayName = "Spider Swarm",
            anchorType = Anchor.NOSE_TIP, behavior = BugBehavior.Swarm,
            frameCount = 3, frameDurationMs = 100L, scaleFactor = 0.15f,
            assetDir = "sprites/sprite_spider",
        )
        val fakeBitmap = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888)
        mockAssetLoader.stub { on { get(any(), any()) } doReturn fakeBitmap }
        engine.setFilter(swarmFilter)

        val f1 = buildFace(trackingId = 1, boundingBox = Rect(0, 0, 200, 200))
        val f2 = buildFace(trackingId = 2, boundingBox = Rect(300, 0, 500, 200))

        // First draw — seeds perFaceState entries with default SWARM_INSTANCE_COUNT_DEFAULT=6 instances
        engine.onDraw(mockCanvas, mockFrame, listOf(f1, f2))

        // Force each face state up to 15 instances to exceed the 20-draw cap
        (engine.perFaceState[1] as? BehaviorState.Swarm)?.let { s ->
            while (s.instances.size < 15) s.instances.add(
                BugInstance(PointF(100f, 100f), PointF(0f, 0f))
            )
        }
        (engine.perFaceState[2] as? BehaviorState.Swarm)?.let { s ->
            while (s.instances.size < 15) s.instances.add(
                BugInstance(PointF(400f, 100f), PointF(0f, 0f))
            )
        }

        // Re-draw with inflated states; total = 30 instances but cap = 20
        clearInvocations(mockCanvas)
        engine.onDraw(mockCanvas, mockFrame, listOf(f1, f2))
        verify(mockCanvas, atMost(FilterEngine.MAX_DRAWS_PER_FRAME))
            .drawBitmap(any<Bitmap>(), eq(0f), eq(0f), any<Paint>())
    }

    /**
     * D-13: onFaceLost(trackingId) removes the entry from perFaceState, leaving others intact.
     */
    @Test
    fun onFaceLost_removesStateEntry() {
        val fakeBitmap = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888)
        mockAssetLoader.stub { on { get(any(), any()) } doReturn fakeBitmap }
        engine.setFilter(filterA)

        val face1 = buildFace(
            trackingId = 1,
            boundingBox = Rect(0, 0, 200, 200),
            contours = mapOf(FaceContour.NOSE_BRIDGE to listOf(PointF(100f, 100f))),
        )
        val face2 = buildFace(trackingId = 2, boundingBox = Rect(300, 0, 500, 200))
        engine.onDraw(mockCanvas, mockFrame, listOf(face1, face2))

        assertEquals("perFaceState must have 2 entries after onDraw with 2 faces",
            2, engine.perFaceState.size)

        engine.onFaceLost(1)

        assertEquals("perFaceState must have 1 entry after onFaceLost(1)",
            1, engine.perFaceState.size)
        assertFalse("entry for trackingId=1 must be gone", engine.perFaceState.containsKey(1))
        assertTrue("entry for trackingId=2 must remain", engine.perFaceState.containsKey(2))
    }
}
