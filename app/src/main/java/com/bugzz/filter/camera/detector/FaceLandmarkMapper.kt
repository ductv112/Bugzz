package com.bugzz.filter.camera.detector

import android.graphics.PointF
import com.google.mlkit.vision.face.FaceContour

/**
 * Face → anchor-point helper. Production body per CONTEXT D-30.
 *
 * Resolves each [Anchor] from [SmoothedFace.contours] primarily, with fallback ladders
 * ensuring a non-null result whenever [SmoothedFace.boundingBox] is present (always true
 * for a detected face).
 *
 * Fallback ladder per anchor:
 *   NOSE_TIP:    NOSE_BRIDGE[last] → NOSE_BOTTOM[0] → bbox center
 *   FOREHEAD:    mean(LEFT_EYEBROW_TOP[0], RIGHT_EYEBROW_TOP[last]) offset -15% bbox.height
 *                → bbox top-center
 *   LEFT_CHEEK:  FACE contour at 40% progress → bbox left-center
 *   RIGHT_CHEEK: FACE contour at 60% progress → bbox right-center
 *   CHIN:        FACE contour at 50% progress → bbox bottom-center
 *   LEFT_EYE:    LEFT_EYE contour centroid → bbox quadrant-center
 *   RIGHT_EYE:   RIGHT_EYE contour centroid → bbox quadrant-center
 *
 * Returns null ONLY if face.boundingBox is null — should never happen for a detected face.
 */
object FaceLandmarkMapper {

    /** Anchor keys FilterEngine consumes to place bug sprites on landmark points. */
    enum class Anchor { NOSE_TIP, FOREHEAD, LEFT_CHEEK, RIGHT_CHEEK, CHIN, LEFT_EYE, RIGHT_EYE }

    /**
     * Resolve [anchor] on [face] to a sensor-coord point.
     *
     * Returns null only if face.boundingBox is null (never occurs for a detected face).
     */
    fun anchorPoint(face: SmoothedFace, anchor: Anchor): PointF? {
        val bbox = face.boundingBox
        return when (anchor) {
            Anchor.NOSE_TIP -> {
                val bridge = face.contours[FaceContour.NOSE_BRIDGE]
                if (!bridge.isNullOrEmpty()) return bridge.last()
                val bottom = face.contours[FaceContour.NOSE_BOTTOM]
                if (!bottom.isNullOrEmpty()) return bottom.first()
                PointF(bbox.centerX().toFloat(), bbox.centerY().toFloat())
            }

            Anchor.FOREHEAD -> {
                val leb = face.contours[FaceContour.LEFT_EYEBROW_TOP]
                val reb = face.contours[FaceContour.RIGHT_EYEBROW_TOP]
                if (!leb.isNullOrEmpty() && !reb.isNullOrEmpty()) {
                    val lp = leb.first()
                    val rp = reb.last()
                    val mid = PointF((lp.x + rp.x) / 2f, (lp.y + rp.y) / 2f)
                    // Offset upward by 15% bbox height (D-30). In sensor coords "up" is -Y.
                    mid.y -= bbox.height() * 0.15f
                    return mid
                }
                PointF(bbox.centerX().toFloat(), bbox.top.toFloat())
            }

            Anchor.LEFT_CHEEK -> {
                val face2 = face.contours[FaceContour.FACE]
                if (!face2.isNullOrEmpty()) {
                    // FACE contour is ordered clockwise; left cheek ≈ index N * 0.40
                    val idx = (face2.size * 0.40f).toInt().coerceIn(0, face2.size - 1)
                    return face2[idx]
                }
                PointF(bbox.left.toFloat(), bbox.centerY().toFloat())
            }

            Anchor.RIGHT_CHEEK -> {
                val face2 = face.contours[FaceContour.FACE]
                if (!face2.isNullOrEmpty()) {
                    // Mirror of LEFT_CHEEK: 60% progress
                    val idx = (face2.size * 0.60f).toInt().coerceIn(0, face2.size - 1)
                    return face2[idx]
                }
                PointF(bbox.right.toFloat(), bbox.centerY().toFloat())
            }

            Anchor.CHIN -> {
                val face2 = face.contours[FaceContour.FACE]
                if (!face2.isNullOrEmpty()) {
                    // Midpoint of back-half (index ≈ 50% of contour)
                    val idx = (face2.size * 0.50f).toInt().coerceIn(0, face2.size - 1)
                    return face2[idx]
                }
                PointF(bbox.centerX().toFloat(), bbox.bottom.toFloat())
            }

            Anchor.LEFT_EYE -> {
                val eye = face.contours[FaceContour.LEFT_EYE]
                if (!eye.isNullOrEmpty()) return centroid(eye)
                PointF(bbox.left + bbox.width() * 0.3f, bbox.top + bbox.height() * 0.35f)
            }

            Anchor.RIGHT_EYE -> {
                val eye = face.contours[FaceContour.RIGHT_EYE]
                if (!eye.isNullOrEmpty()) return centroid(eye)
                PointF(bbox.left + bbox.width() * 0.7f, bbox.top + bbox.height() * 0.35f)
            }
        }
    }

    private fun centroid(points: List<PointF>): PointF {
        var sx = 0f; var sy = 0f
        for (p in points) { sx += p.x; sy += p.y }
        return PointF(sx / points.size, sy / points.size)
    }
}
