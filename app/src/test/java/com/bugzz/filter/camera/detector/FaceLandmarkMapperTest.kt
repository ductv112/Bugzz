package com.bugzz.filter.camera.detector

import android.graphics.PointF
import android.graphics.Rect
import com.google.mlkit.vision.face.FaceContour
import com.bugzz.filter.camera.detector.FaceLandmarkMapper.Anchor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Nyquist Wave 0 tests for [FaceLandmarkMapper.anchorPoint] (REN-03 / D-30).
 *
 * Covers all 7 [Anchor] enum values: primary contour path, fallback contour path,
 * and ultimate bbox-center fallback when all contours are empty.
 *
 * Robolectric needed because [android.graphics.PointF] and [android.graphics.Rect]
 * require real Android SDK shadows (same reason as DebugOverlayRendererTest).
 *
 * Wave 0 state: [FaceLandmarkMapper.anchorPoint] is a Phase 2 stub that always returns null.
 * All tests are @Ignore'd to keep the test run exit 0. Plan 03-03 implements the production
 * body and un-Ignores these tests.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FaceLandmarkMapperTest {

    // -------------------------------------------------------------------------
    // NOSE_TIP
    // -------------------------------------------------------------------------

    /**
     * TODO Plan 03-03: un-Ignore when FaceLandmarkMapper.anchorPoint() production body lands.
     */
    @Test
    fun noseTip_fromNoseBridge_returnsLastPoint() {
        // NOSE_TIP = last point of NOSE_BRIDGE contour (closest to the actual nose tip)
        val face = buildFace(
            bbox = Rect(0, 0, 200, 200),
            contours = mapOf(
                FaceContour.NOSE_BRIDGE to listOf(
                    PointF(10f, 10f),
                    PointF(20f, 20f),
                    PointF(30f, 40f),  // last point = tip
                ),
            ),
        )
        val result = FaceLandmarkMapper.anchorPoint(face, Anchor.NOSE_TIP)
        assertNotNull("NOSE_TIP from NOSE_BRIDGE must not be null", result)
        assertEquals("NOSE_TIP x must be last NOSE_BRIDGE point x", 30f, result!!.x, 0.01f)
        assertEquals("NOSE_TIP y must be last NOSE_BRIDGE point y", 40f, result.y, 0.01f)
    }

    /**
     * TODO Plan 03-03: un-Ignore when FaceLandmarkMapper.anchorPoint() production body lands.
     */
    @Test
    fun noseTip_fallbackToNoseBottom() {
        // NOSE_BRIDGE empty — fall back to NOSE_BOTTOM first point
        val face = buildFace(
            bbox = Rect(0, 0, 200, 200),
            contours = mapOf(
                FaceContour.NOSE_BOTTOM to listOf(
                    PointF(55f, 80f),
                    PointF(65f, 82f),
                ),
            ),
        )
        val result = FaceLandmarkMapper.anchorPoint(face, Anchor.NOSE_TIP)
        assertNotNull("NOSE_TIP fallback to NOSE_BOTTOM must not be null", result)
        assertEquals("NOSE_TIP fallback x = first NOSE_BOTTOM point", 55f, result!!.x, 0.01f)
        assertEquals("NOSE_TIP fallback y = first NOSE_BOTTOM point", 80f, result.y, 0.01f)
    }

    /**
     * TODO Plan 03-03: un-Ignore when FaceLandmarkMapper.anchorPoint() production body lands.
     */
    @Test
    fun noseTip_ultimateFallback_bboxCenter() {
        // All contours empty — ultimate fallback to bbox center
        val face = buildFace(
            bbox = Rect(0, 0, 100, 100),
            contours = emptyMap(),
        )
        val result = FaceLandmarkMapper.anchorPoint(face, Anchor.NOSE_TIP)
        assertNotNull("NOSE_TIP ultimate fallback must not be null", result)
        assertEquals("NOSE_TIP bbox-center fallback x", 50f, result!!.x, 0.01f)
        assertEquals("NOSE_TIP bbox-center fallback y", 50f, result.y, 0.01f)
    }

    // -------------------------------------------------------------------------
    // FOREHEAD
    // -------------------------------------------------------------------------

    /**
     * TODO Plan 03-03: un-Ignore when FaceLandmarkMapper.anchorPoint() production body lands.
     */
    @Test
    fun forehead_fromEyebrowTops_withUpwardOffset() {
        // FOREHEAD = mean of LEFT_EYEBROW_TOP[0] + RIGHT_EYEBROW_TOP[last], offset up 15% bbox height
        // mid.x = (20+80)/2 = 50; mid.y = (30+30)/2 = 30; offset = 200*0.15 = 30; result.y = 30-30 = 0
        val face = buildFace(
            bbox = Rect(0, 0, 100, 200),
            contours = mapOf(
                FaceContour.LEFT_EYEBROW_TOP to listOf(PointF(20f, 30f)),
                FaceContour.RIGHT_EYEBROW_TOP to listOf(PointF(80f, 30f)),
            ),
        )
        val result = FaceLandmarkMapper.anchorPoint(face, Anchor.FOREHEAD)
        assertNotNull("FOREHEAD from eyebrow tops must not be null", result)
        assertEquals("FOREHEAD x = midpoint of eyebrow tops", 50f, result!!.x, 0.5f)
        assertEquals("FOREHEAD y = eyebrow midpoint - 15% bbox height = 30 - 30 = 0", 0f, result.y, 0.5f)
    }

    // -------------------------------------------------------------------------
    // LEFT_CHEEK / RIGHT_CHEEK / CHIN
    // -------------------------------------------------------------------------

    /**
     * TODO Plan 03-03: un-Ignore when FaceLandmarkMapper.anchorPoint() production body lands.
     */
    @Test
    fun leftCheek_fromFaceContour_atFortyPercent() {
        // LEFT_CHEEK = FaceContour.FACE at index (size * 0.40).toInt() = (10 * 0.40) = 4
        val pts = (0 until 10).map { i -> PointF(i.toFloat() * 10, i.toFloat() * 5) }
        val face = buildFace(
            bbox = Rect(0, 0, 200, 200),
            contours = mapOf(FaceContour.FACE to pts),
        )
        val result = FaceLandmarkMapper.anchorPoint(face, Anchor.LEFT_CHEEK)
        assertNotNull("LEFT_CHEEK from FACE contour must not be null", result)
        // index 4 = PointF(40f, 20f)
        assertEquals("LEFT_CHEEK x = FACE[4].x", 40f, result!!.x, 0.01f)
        assertEquals("LEFT_CHEEK y = FACE[4].y", 20f, result.y, 0.01f)
    }

    /**
     * TODO Plan 03-03: un-Ignore when FaceLandmarkMapper.anchorPoint() production body lands.
     */
    @Test
    fun rightCheek_fromFaceContour_atSixtyPercent() {
        // RIGHT_CHEEK = FaceContour.FACE at index (size * 0.60).toInt() = (10 * 0.60) = 6
        val pts = (0 until 10).map { i -> PointF(i.toFloat() * 10, i.toFloat() * 5) }
        val face = buildFace(
            bbox = Rect(0, 0, 200, 200),
            contours = mapOf(FaceContour.FACE to pts),
        )
        val result = FaceLandmarkMapper.anchorPoint(face, Anchor.RIGHT_CHEEK)
        assertNotNull("RIGHT_CHEEK from FACE contour must not be null", result)
        // index 6 = PointF(60f, 30f)
        assertEquals("RIGHT_CHEEK x = FACE[6].x", 60f, result!!.x, 0.01f)
        assertEquals("RIGHT_CHEEK y = FACE[6].y", 30f, result.y, 0.01f)
    }

    /**
     * TODO Plan 03-03: un-Ignore when FaceLandmarkMapper.anchorPoint() production body lands.
     */
    @Test
    fun chin_fromFaceContour_atFiftyPercent() {
        // CHIN = FaceContour.FACE at index (size * 0.50).toInt() = (10 * 0.50) = 5
        val pts = (0 until 10).map { i -> PointF(i.toFloat() * 10, i.toFloat() * 5) }
        val face = buildFace(
            bbox = Rect(0, 0, 200, 200),
            contours = mapOf(FaceContour.FACE to pts),
        )
        val result = FaceLandmarkMapper.anchorPoint(face, Anchor.CHIN)
        assertNotNull("CHIN from FACE contour must not be null", result)
        // index 5 = PointF(50f, 25f)
        assertEquals("CHIN x = FACE[5].x", 50f, result!!.x, 0.01f)
        assertEquals("CHIN y = FACE[5].y", 25f, result.y, 0.01f)
    }

    // -------------------------------------------------------------------------
    // LEFT_EYE / RIGHT_EYE
    // -------------------------------------------------------------------------

    /**
     * TODO Plan 03-03: un-Ignore when FaceLandmarkMapper.anchorPoint() production body lands.
     */
    @Test
    fun leftEye_fromContourCentroid() {
        // LEFT_EYE = centroid (mean) of LEFT_EYE contour points
        val leftPts = listOf(
            PointF(10f, 20f),
            PointF(20f, 30f),
            PointF(30f, 20f),
            PointF(20f, 10f),
        )
        // mean x = (10+20+30+20)/4 = 20; mean y = (20+30+20+10)/4 = 20
        val face = buildFace(
            bbox = Rect(0, 0, 200, 200),
            contours = mapOf(FaceContour.LEFT_EYE to leftPts),
        )
        val result = FaceLandmarkMapper.anchorPoint(face, Anchor.LEFT_EYE)
        assertNotNull("LEFT_EYE centroid must not be null", result)
        assertEquals("LEFT_EYE centroid x = 20f", 20f, result!!.x, 0.01f)
        assertEquals("LEFT_EYE centroid y = 20f", 20f, result.y, 0.01f)
    }

    /**
     * TODO Plan 03-03: un-Ignore when FaceLandmarkMapper.anchorPoint() production body lands.
     */
    @Test
    fun rightEye_fromContourCentroid() {
        val rightPts = listOf(
            PointF(100f, 20f),
            PointF(120f, 30f),
            PointF(140f, 20f),
            PointF(120f, 10f),
        )
        // mean x = (100+120+140+120)/4 = 120; mean y = (20+30+20+10)/4 = 20
        val face = buildFace(
            bbox = Rect(0, 0, 200, 200),
            contours = mapOf(FaceContour.RIGHT_EYE to rightPts),
        )
        val result = FaceLandmarkMapper.anchorPoint(face, Anchor.RIGHT_EYE)
        assertNotNull("RIGHT_EYE centroid must not be null", result)
        assertEquals("RIGHT_EYE centroid x = 120f", 120f, result!!.x, 0.01f)
        assertEquals("RIGHT_EYE centroid y = 20f", 20f, result.y, 0.01f)
    }

    // -------------------------------------------------------------------------
    // All anchors — never null when bbox present
    // -------------------------------------------------------------------------

    /**
     * TODO Plan 03-03: un-Ignore when FaceLandmarkMapper.anchorPoint() production body lands.
     */
    @Test
    fun allAnchors_neverReturnsNull_whenBboxPresent() {
        // Parameterized: all 7 Anchor values with empty contours + non-null bbox
        // All must fall back to bbox center and return non-null.
        val face = buildFace(
            bbox = Rect(10, 20, 110, 220),
            contours = emptyMap(),
        )
        for (anchor in Anchor.values()) {
            val result = FaceLandmarkMapper.anchorPoint(face, anchor)
            assertNotNull(
                "anchorPoint(face, $anchor) must never return null when bbox is present",
                result,
            )
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private fun buildFace(
        bbox: Rect,
        contours: Map<Int, List<PointF>>,
        landmarks: Map<Int, PointF> = emptyMap(),
    ): SmoothedFace = SmoothedFace(
        trackingId = 0,
        boundingBox = bbox,
        contours = contours,
        landmarks = landmarks,
    )
}
