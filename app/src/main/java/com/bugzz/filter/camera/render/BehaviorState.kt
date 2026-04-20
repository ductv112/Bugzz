package com.bugzz.filter.camera.render

import android.graphics.PointF

/**
 * Per-face animation state (D-12 / REN-02). One variant per [BugBehavior] kind.
 *
 * Phase 4 supersedes the Phase 3 flat [BugState] data class. Static replaces BugState's
 * position/velocity fields; Crawl/Swarm/Fall add their behavior-specific state.
 *
 * All mutable fields use `var` with explicit type (no `val` lazy init) — the tick loop
 * mutates state in place to avoid per-frame allocation in the render hot path.
 */
sealed interface BehaviorState {

    /** Position-only state; used by STATIC behavior (anchor-pinned sprites). */
    data class Static(val pos: PointF = PointF(0f, 0f)) : BehaviorState

    /** Progress along FaceContour.FACE polygon, 0..1 wrapping. D-08. */
    data class Crawl(
        var progress: Float = 0f,
        val direction: CrawlDirection = CrawlDirection.CW,
    ) : BehaviorState

    /** 5-8 drifting bugs per D-09. instances mutated per frame. */
    data class Swarm(
        val instances: MutableList<BugInstance> = mutableListOf(),
    ) : BehaviorState

    /** Rain of bugs per D-10. instances add/remove per frame. */
    data class Fall(
        val instances: MutableList<FallingBug> = mutableListOf(),
        var nextSpawnNanos: Long = 0L,
    ) : BehaviorState
}

/** D-08 crawl direction (clockwise or counter-clockwise along FACE contour). */
enum class CrawlDirection { CW, CCW }

/** One SWARM drift instance (D-09). Mutable — reused per frame. */
data class BugInstance(
    var position: PointF,
    var velocity: PointF,
    var frameIndex: Int = 0,
)

/** One FALL gravity instance (D-10). spawnNanos immutable (assigned at spawn). */
data class FallingBug(
    var position: PointF,
    var velocity: PointF,
    val spawnNanos: Long,
)
