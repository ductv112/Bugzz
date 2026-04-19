package com.bugzz.filter.camera.render

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
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
 */
@Singleton
class DebugOverlayRenderer @Inject constructor() {

    private val boxPaint = Paint().apply {
        color = Color.RED
        style = Paint.Style.STROKE
        strokeWidth = 4f
        isAntiAlias = true
    }
    private val dotPaint = Paint().apply {
        color = Color.argb(255, 255, 80, 0)  // orange-red — visible on both skin tones
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    /** Invoked from OverlayEffect onDrawListener on the renderHandler thread. */
    fun draw(canvas: Canvas, snapshot: FaceSnapshot, timestampNanos: Long) {
        if (!BuildConfig.DEBUG) return                    // D-02 gate (T-02-02)
        if (snapshot.faces.isEmpty()) return              // CAM-06: no-face → draw nothing

        for (face in snapshot.faces) {
            // Bounding box — ALWAYS draw (D-23: primary + secondary faces)
            canvas.drawRect(
                face.boundingBox.left.toFloat(),
                face.boundingBox.top.toFloat(),
                face.boundingBox.right.toFloat(),
                face.boundingBox.bottom.toFloat(),
                boxPaint,
            )
            // Contour landmark dots — populated ONLY for primary face (PITFALLS #13)
            face.contours.values.flatten().forEach { p ->
                canvas.drawCircle(p.x, p.y, 4f, dotPaint)
            }
            // Fallback landmarks — populated for all faces (D-23 secondary-face path)
            face.landmarks.values.forEach { p ->
                canvas.drawCircle(p.x, p.y, 5f, dotPaint)
            }
        }
    }
}
