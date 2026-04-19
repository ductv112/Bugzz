package com.bugzz.filter.camera.detector

import android.graphics.Rect
import com.google.mlkit.vision.face.Face
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max
import kotlin.math.min

/**
 * Spatial-IoU face-identity tracker. Replaces ML Kit's trackingId (null under CONTOUR_MODE_ALL —
 * see 02-ADR-01-no-ml-kit-tracking-with-contour.md). Per-frame: greedy best-IoU match against
 * retained entries within dropout retention window, then new-id assignment for unmatched
 * detections, then dropout increment.
 *
 * Algorithm contract (D-23):
 *   1. For each detected face, compute IoU against every TrackedEntry.lastBoundingBox still
 *      within dropout retention (framesSinceLastSeen <= MAX_DROPOUT_FRAMES).
 *   2. Greedy best-match: pick pair with highest IoU >= threshold; assign existing ID;
 *      remove from both pools; repeat.
 *   3. Unmatched detected faces -> assign nextId++. Skip if exceeds MAX_TRACKED_FACES
 *      (lowest-IoU unmatched face dropped silently per D-23).
 *   4. Unmatched tracked entries -> framesSinceLastSeen++; if > MAX_DROPOUT_FRAMES, remove
 *      after frame callback completes (don't mutate during iteration).
 *
 * IDs are monotonic — never recycled — so LandmarkSmoother state cannot accidentally carry
 * over to a different face (D-25).
 *
 * @see .planning/phases/02-camera-preview-face-detection-coordinate-validation/02-ADR-01-no-ml-kit-tracking-with-contour.md
 * @see .planning/phases/03-first-filter-end-to-end-photo-capture/03-CONTEXT.md D-20..D-26
 */
@Singleton
class BboxIouTracker @Inject constructor() {

    /** A face with a stable tracker-assigned ID. */
    data class TrackedFace(val id: Int, val face: Face)

    /**
     * Result of one [assign] call.
     *
     * @property tracked List of tracked faces (at most [MAX_TRACKED_FACES]).
     * @property removedIds IDs that were removed this frame due to dropout expiry. Caller
     *   (FaceDetectorClient) must invoke [LandmarkSmoother.onFaceLost] for each removed ID
     *   to free per-id 1€ filter state (ADR-01 #2).
     */
    data class TrackerResult(val tracked: List<TrackedFace>, val removedIds: List<Int>)

    private data class TrackedEntry(
        val id: Int,
        var lastBoundingBox: Rect,
        var framesSinceLastSeen: Int,
    )

    private val tracked = mutableMapOf<Int, TrackedEntry>()
    private var nextId: Int = 0

    /**
     * D-20 — per-frame assignment. Returns [TrackerResult] with tracked faces (at most
     * [MAX_TRACKED_FACES]) and IDs removed this frame due to dropout expiry.
     */
    fun assign(faces: List<Face>): TrackerResult {
        // D-22 — retain 2 largest-bbox detected faces only
        val sortedFaces = faces
            .sortedByDescending { it.boundingBox.width().toLong() * it.boundingBox.height() }
            .take(MAX_TRACKED_FACES)

        val out = mutableListOf<TrackedFace>()
        val unmatchedDetectedIdx = sortedFaces.indices.toMutableSet()
        val unmatchedTrackedIds = tracked.keys.toMutableSet()

        // Step 1-2: greedy IoU-best-first match
        while (unmatchedDetectedIdx.isNotEmpty() && unmatchedTrackedIds.isNotEmpty()) {
            var bestIou = IOU_MATCH_THRESHOLD
            var bestDetectedIdx = -1
            var bestTrackedId = -1
            for (dIdx in unmatchedDetectedIdx) {
                for (tId in unmatchedTrackedIds) {
                    val iouVal = iou(sortedFaces[dIdx].boundingBox, tracked[tId]!!.lastBoundingBox)
                    if (iouVal > bestIou) {
                        bestIou = iouVal
                        bestDetectedIdx = dIdx
                        bestTrackedId = tId
                    }
                }
            }
            if (bestDetectedIdx < 0) break   // no remaining pair exceeds threshold

            // Match: update entry, emit result
            val face = sortedFaces[bestDetectedIdx]
            val entry = tracked[bestTrackedId]!!
            entry.lastBoundingBox = face.boundingBox
            entry.framesSinceLastSeen = 0
            out.add(TrackedFace(bestTrackedId, face))
            unmatchedDetectedIdx.remove(bestDetectedIdx)
            unmatchedTrackedIds.remove(bestTrackedId)
        }

        // Step 3: assign new IDs to unmatched detections (respecting MAX cap)
        for (dIdx in unmatchedDetectedIdx) {
            if (tracked.size >= MAX_TRACKED_FACES) break   // cap reached — drop silently per D-23
            val face = sortedFaces[dIdx]
            val newId = nextId++
            tracked[newId] = TrackedEntry(newId, face.boundingBox, framesSinceLastSeen = 0)
            out.add(TrackedFace(newId, face))
        }

        // Step 4: increment dropout, collect removals (collect first, then mutate — avoid
        // ConcurrentModificationException when iterating tracked.keys)
        val removedIds = mutableListOf<Int>()
        for (tId in unmatchedTrackedIds) {
            val entry = tracked[tId]!!
            entry.framesSinceLastSeen++
            if (entry.framesSinceLastSeen > MAX_DROPOUT_FRAMES) removedIds.add(tId)
        }
        for (id in removedIds) tracked.remove(id)

        return TrackerResult(out, removedIds)
    }

    companion object {
        /** Minimum IoU overlap required to assign an existing ID to a detected face (D-21). */
        const val IOU_MATCH_THRESHOLD: Float = 0.3f

        /** Max frames a face can be absent before its ID is retired (D-21). */
        const val MAX_DROPOUT_FRAMES: Int = 5

        /** Maximum number of faces tracked simultaneously (D-22). */
        const val MAX_TRACKED_FACES: Int = 2

        /**
         * Axis-aligned Intersection-over-Union for [android.graphics.Rect].
         * Returns a value in [0.0, 1.0]: identical boxes → 1.0; disjoint → 0.0.
         */
        internal fun iou(a: Rect, b: Rect): Float {
            val interLeft = max(a.left, b.left)
            val interTop = max(a.top, b.top)
            val interRight = min(a.right, b.right)
            val interBottom = min(a.bottom, b.bottom)
            val interW = (interRight - interLeft).coerceAtLeast(0)
            val interH = (interBottom - interTop).coerceAtLeast(0)
            val interArea = interW.toLong() * interH
            if (interArea == 0L) return 0f
            val areaA = a.width().toLong() * a.height()
            val areaB = b.width().toLong() * b.height()
            val unionArea = areaA + areaB - interArea
            if (unionArea <= 0L) return 0f
            return interArea.toFloat() / unionArea.toFloat()
        }
    }
}
