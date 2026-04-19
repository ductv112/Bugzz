package com.bugzz.filter.camera.detector

import android.graphics.PointF
import android.graphics.Rect

/**
 * Immutable smoothed face record — produced by FaceDetectorClient (cameraExecutor writes),
 * consumed by DebugOverlayRenderer (renderExecutor reads). D-19 handoff contract.
 *
 * @property trackingId ML Kit face trackingId (-1 if unavailable — primary face may still populate
 *   contours even when trackingId missing, but smoothed state requires a stable id).
 * @property boundingBox Sensor-coord rect around the face — always populated.
 * @property contours Key = FaceContour.Type (int); Value = smoothed points along that contour.
 *   Populated ONLY for primary face (ML Kit contour limitation — PITFALLS #13 / D-23).
 * @property landmarks Key = FaceLandmark.Type (int); Value = landmark point. Populated for all
 *   faces even when contours are empty — used as fallback for secondary faces.
 */
data class SmoothedFace(
    val trackingId: Int,
    val boundingBox: Rect,
    val contours: Map<Int, List<PointF>>,
    val landmarks: Map<Int, PointF>,
)

/**
 * Atomic snapshot of all detected faces at one analyzer callback. Written by cameraExecutor
 * (single-writer), read by renderExecutor on each OverlayEffect.onDrawListener tick (multi-reader).
 * Wrapped in AtomicReference for lock-free handoff (D-19).
 */
data class FaceSnapshot(
    val faces: List<SmoothedFace>,
    val timestampNanos: Long,
) {
    companion object {
        val EMPTY: FaceSnapshot = FaceSnapshot(emptyList(), 0L)
    }
}
