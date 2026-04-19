package com.bugzz.filter.camera.render

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PointF
import com.bugzz.filter.camera.BuildConfig
import com.bugzz.filter.camera.detector.FaceSnapshot
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Debug-only overlay renderer (D-02 gate; replaced by FilterEngine in Phase 3).
 *
 * Invoked from OverlayEffect.onDrawListener on the renderExecutor / HandlerThread inside
 * OverlayEffectBuilder. Canvas has already been matrix-transformed by the caller
 * (`canvas.setMatrix(frame.sensorToBufferTransform)` — CAM-07 pairing); this function
 * draws in SENSOR coordinate space (where MlKitAnalyzer produced the face data via
 * COORDINATE_SYSTEM_SENSOR, D-17).
 *
 * D-23 / PITFALLS #13: multi-face behavior — ML Kit contour mode only populates `contours`
 * for the primary face; secondary faces have only `landmarks`. Draw both — secondary faces
 * gracefully degrade to boundingBox + subset of landmark dots.
 *
 * T-02-02 (information disclosure) mitigation: FIRST statement is `if (!BuildConfig.DEBUG) return`.
 * Release builds draw nothing, so saved MP4s / JPEGs contain no biometric overlay data.
 *
 * **Amended 2026-04-19 per GAP-02-B:**
 * - **Matrix scale compensation:** Paint strokeWidth + drawCircle radius are computed per
 *   draw by extracting the current canvas matrix's MSCALE_X. The renderer-wide intent is
 *   `DEVICE_PX_STROKE` device pixels regardless of the sensor-to-buffer transform scale.
 *   See the D-01 amendment in `02-CONTEXT.md` for rationale.
 * - **Dot density reduction:** instead of drawing a dot at every contour point
 *   (~97 dots per face when flattening all FaceContour lists), draw one centroid dot per
 *   populated FaceContour TYPE (≤ 15 dots per primary face). This preserves
 *   contour-mode-ingestion visual validation (D-01 intent) while keeping the overlay
 *   tractable for CAM-07 pixel-alignment verification.
 */
@Singleton
class DebugOverlayRenderer @Inject constructor() {

    private val boxPaint = Paint().apply {
        color = Color.RED
        style = Paint.Style.STROKE
        isAntiAlias = true
        // strokeWidth set per-frame in draw() — depends on canvas matrix scale (GAP-02-B).
    }
    private val dotPaint = Paint().apply {
        color = Color.argb(255, 255, 80, 0)  // orange-red — visible on both skin tones
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    // Scratch buffer reused each draw — avoid allocation in the render hot path.
    private val matrixValues = FloatArray(9)

    /** Invoked from OverlayEffect onDrawListener on the renderHandler thread. */
    fun draw(canvas: Canvas, snapshot: FaceSnapshot, timestampNanos: Long) {
        if (!BuildConfig.DEBUG) return                    // D-02 gate (T-02-02)
        if (snapshot.faces.isEmpty()) return              // CAM-06: no-face → draw nothing

        // Extract the current matrix scale so stroke + dot radii render at the
        // intended DEVICE_PX size regardless of sensorToBufferTransform scale factor
        // (GAP-02-B / H2 fix — without this, a 2x matrix scale doubles apparent
        // stroke width and saturates the preview with thick lines + huge dots).
        canvas.matrix.getValues(matrixValues)
        val scaleX = matrixValues[Matrix.MSCALE_X]
        val strokeInSensorSpace = computeSensorSpaceStroke(DEVICE_PX_STROKE, scaleX)
        val dotRadiusInSensorSpace = computeSensorSpaceStroke(DEVICE_PX_DOT_RADIUS, scaleX)
        boxPaint.strokeWidth = strokeInSensorSpace

        for (face in snapshot.faces) {
            // Bounding box — ALWAYS draw (D-23: primary + secondary faces)
            canvas.drawRect(
                face.boundingBox.left.toFloat(),
                face.boundingBox.top.toFloat(),
                face.boundingBox.right.toFloat(),
                face.boundingBox.bottom.toFloat(),
                boxPaint,
            )
            // Contour centroids — one dot per FaceContour TYPE (≤ 15 per primary face),
            // GAP-02-B dot-density reduction. Preserves D-01 contour-mode-ingestion
            // validation intent while keeping overlay tractable for CAM-07 alignment checks.
            face.contours.forEach { (_, points) ->
                val c = centroidOf(points) ?: return@forEach
                canvas.drawCircle(c.x, c.y, dotRadiusInSensorSpace, dotPaint)
            }
            // Fallback landmarks — populated for all faces (D-23 secondary-face path)
            face.landmarks.values.forEach { p ->
                canvas.drawCircle(p.x, p.y, dotRadiusInSensorSpace, dotPaint)
            }
        }
    }

    companion object {
        /** Intended stroke width in device pixels (matrix-compensated per-frame). */
        internal const val DEVICE_PX_STROKE: Float = 4f

        /** Intended dot radius in device pixels (matrix-compensated per-frame). */
        internal const val DEVICE_PX_DOT_RADIUS: Float = 5f

        /** Floor to prevent division-by-zero / NaN when canvas matrix is singular. */
        internal const val MIN_SAFE_SCALE: Float = 0.0001f

        /**
         * Compute the centroid (mean x, mean y) of a list of points. Returns null if the
         * list is empty. Exposed internal for DebugOverlayRendererTest Nyquist pin.
         */
        internal fun centroidOf(points: List<PointF>): PointF? {
            if (points.isEmpty()) return null
            var sx = 0f
            var sy = 0f
            for (p in points) {
                sx += p.x
                sy += p.y
            }
            return PointF(sx / points.size, sy / points.size)
        }

        /**
         * Compute the stroke width (in sensor-space, ready to assign to Paint.strokeWidth)
         * required to render as [deviceStrokePx] device pixels under a canvas matrix whose
         * MSCALE_X equals [matrixScaleX]. Clamps pathological scale factors
         * (NaN, 0, negative, < MIN_SAFE_SCALE) to MIN_SAFE_SCALE to guarantee a finite,
         * non-infinite return value. Exposed internal for DebugOverlayRendererTest.
         */
        internal fun computeSensorSpaceStroke(deviceStrokePx: Float, matrixScaleX: Float): Float {
            val safeScale =
                if (matrixScaleX.isNaN() || matrixScaleX < MIN_SAFE_SCALE) MIN_SAFE_SCALE
                else matrixScaleX
            return deviceStrokePx / safeScale
        }
    }
}
