package com.bugzz.filter.camera.detector

import android.graphics.Rect
import com.google.mlkit.vision.face.Face
import javax.inject.Inject
import javax.inject.Singleton

/**
 * STUB — Plan 03-02 REPLACES body + companion constants per D-21/22/23.
 *
 * Bbox-IoU greedy tracker that assigns stable integer IDs to detected faces across frames.
 * Replaces ML Kit's (broken) face.trackingId in contour mode — see ADR-01.
 *
 * Companion constants are intentionally WRONG (0.0f / 0 / 0) in this stub so that
 * [BboxIouTrackerTest.companion_constantsMatchSpec] stays intentionally RED until Plan 03-02
 * replaces this file with the production implementation.
 *
 * @see .planning/phases/02-camera-preview-face-detection-coordinate-validation/02-ADR-01-no-ml-kit-tracking-with-contour.md
 * @see .planning/phases/03-first-filter-end-to-end-photo-capture/03-CONTEXT.md D-20..D-26
 */
@Singleton
class BboxIouTracker @Inject constructor() {

    /** A face with a stable tracker-assigned ID. */
    data class TrackedFace(val id: Int, val face: Face)

    /** Result of one assign() call. */
    data class TrackerResult(val tracked: List<TrackedFace>, val removedIds: List<Int>)

    /**
     * Assign stable IDs to [faces] by matching against known tracked bounding boxes.
     * STUB: always returns TODO. Plan 03-02 implements greedy IoU matching per D-23.
     */
    fun assign(faces: List<Face>): TrackerResult = TODO("Plan 03-02")

    companion object {
        /** Minimum IoU overlap required to assign an existing ID. STUB value: wrong on purpose. */
        const val IOU_MATCH_THRESHOLD = 0.0f  // STUB — Plan 03-02 sets 0.3f

        /** Max frames a face can be absent before its ID is retired. STUB value: wrong on purpose. */
        const val MAX_DROPOUT_FRAMES = 0  // STUB — Plan 03-02 sets 5

        /** Maximum number of faces tracked simultaneously. STUB value: wrong on purpose. */
        const val MAX_TRACKED_FACES = 0  // STUB — Plan 03-02 sets 2

        /**
         * Intersection-over-Union of two axis-aligned bounding boxes.
         * STUB: throws TODO. Plan 03-02 implements the IoU formula.
         */
        internal fun iou(a: Rect, b: Rect): Float = TODO("Plan 03-02")
    }
}
