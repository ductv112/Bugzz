package com.bugzz.filter.camera.detector

import android.graphics.PointF
import android.graphics.Rect

/**
 * Immutable smoothed face record — produced by FaceDetectorClient (cameraExecutor writes),
 * consumed by DebugOverlayRenderer / FilterEngine (renderExecutor reads). D-19 handoff contract.
 *
 * @property trackingId Tracker-assigned stable integer ID (non-negative, monotonic) from
 *   [BboxIouTracker]. Replaces ML Kit's native trackingId which is always null under
 *   CONTOUR_MODE_ALL — see 02-ADR-01-no-ml-kit-tracking-with-contour.md. Same face across
 *   consecutive frames retains the same ID as long as bbox IoU >= 0.3 and gap <= 5 frames.
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
