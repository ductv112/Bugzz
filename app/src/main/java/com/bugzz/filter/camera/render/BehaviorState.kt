package com.bugzz.filter.camera.render

import android.graphics.PointF

/**
 * Per-face animation state (D-12 / REN-02). One variant per [BugBehavior] kind.
 *
 * Phase 04-04 adds per-filter config fields to [Swarm] and [Fall] so that
 * [FilterDefinition.behaviorConfig] values are threaded through tick loops without
 * changing the [BugBehavior] sealed-interface tick signature (D-29).
 *
 * All mutable fields use `var` with explicit type — the tick loop mutates state
 * in place to avoid per-frame allocation in the render hot path.
 */
sealed interface BehaviorState {

    /** Position-only state; used by STATIC behavior (anchor-pinned sprites). */
    data class Static(val pos: PointF = PointF(0f, 0f)) : BehaviorState

    /** Progress along FaceContour.FACE polygon, 0..1 wrapping. D-08. */
    data class Crawl(
        var progress: Float = 0f,
        val direction: CrawlDirection = CrawlDirection.CW,
    ) : BehaviorState

    /**
     * N drifting bugs per D-09.
     *
     * @param targetCount desired instance count (from [FilterDefinition.behaviorConfig] or
     *   [BugBehavior.SWARM_INSTANCE_COUNT_DEFAULT]). Set once at state creation by
     *   [FilterEngine.createBehaviorState]; read by [BugBehavior.Swarm.tick] for initial spawn.
     */
    data class Swarm(
        val instances: MutableList<BugInstance> = mutableListOf(),
        val targetCount: Int = BugBehavior.SWARM_INSTANCE_COUNT_DEFAULT,
    ) : BehaviorState

    /**
     * Rain of bugs per D-10.
     *
     * Config fields are pre-populated from [FilterDefinition.behaviorConfig] at state creation
     * by [FilterEngine.createBehaviorState], so [BugBehavior.Fall.tick] reads them from state
     * instead of BugBehavior companion constants — enabling per-filter override (D-29).
     */
    data class Fall(
        val instances: MutableList<FallingBug> = mutableListOf(),
        var nextSpawnNanos: Long = 0L,
        val maxInstances: Int = BugBehavior.FALL_MAX_INSTANCES_DEFAULT,
        val spawnIntervalMinMs: Int = BugBehavior.FALL_SPAWN_INTERVAL_MIN_MS,
        val spawnIntervalMaxMs: Int = BugBehavior.FALL_SPAWN_INTERVAL_MAX_MS,
        val gravityFactor: Float = BugBehavior.FALL_GRAVITY_FACTOR_DEFAULT,
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
