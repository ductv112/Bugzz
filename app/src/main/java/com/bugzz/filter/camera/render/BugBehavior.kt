package com.bugzz.filter.camera.render

import android.graphics.PointF
import com.bugzz.filter.camera.detector.SmoothedFace

/**
 * Per-bug animation state machine — sealed interface with 4 variants (D-04 / REN-02).
 *
 * Only [Static] has an implemented body in Phase 3. [Crawl], [Swarm], and [Fall] throw
 * [NotImplementedError] per plan — Phase 4 implements them.
 *
 * Each variant's [tick] method advances [state] by [dtMs] milliseconds, reading the
 * current [anchor] position resolved from [face].
 */
sealed interface BugBehavior {

    /**
     * Advance [state] by one frame.
     *
     * @param state   Mutable per-bug state to update in-place.
     * @param face    Current smoothed face — used by dynamic behaviors to read contours.
     * @param anchor  Pre-resolved anchor point in sensor-space coordinates.
     * @param dtMs    Elapsed milliseconds since the previous frame.
     */
    fun tick(state: BugState, face: SmoothedFace, anchor: PointF, dtMs: Long)

    /**
     * Bug stays pinned to the anchor point. Velocity is always zero (D-04 / REN-02).
     * This is the only implemented behavior in Phase 3.
     */
    object Static : BugBehavior {
        override fun tick(state: BugState, face: SmoothedFace, anchor: PointF, dtMs: Long) {
            state.position.set(anchor.x, anchor.y)
            state.velocity.set(0f, 0f)
        }
    }

    /**
     * Bug crawls along the face contour path. TODO Phase 4.
     */
    object Crawl : BugBehavior {
        override fun tick(state: BugState, face: SmoothedFace, anchor: PointF, dtMs: Long) {
            TODO("Phase 4 — Crawl behavior not yet implemented")
        }
    }

    /**
     * Multiple bugs swarm around the face. TODO Phase 4.
     */
    object Swarm : BugBehavior {
        override fun tick(state: BugState, face: SmoothedFace, anchor: PointF, dtMs: Long) {
            TODO("Phase 4 — Swarm behavior not yet implemented")
        }
    }

    /**
     * Bug falls off the face. TODO Phase 4.
     */
    object Fall : BugBehavior {
        override fun tick(state: BugState, face: SmoothedFace, anchor: PointF, dtMs: Long) {
            TODO("Phase 4 — Fall behavior not yet implemented")
        }
    }
}
