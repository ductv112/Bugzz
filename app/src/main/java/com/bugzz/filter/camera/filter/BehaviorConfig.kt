package com.bugzz.filter.camera.filter

import com.bugzz.filter.camera.render.BugBehavior
import com.bugzz.filter.camera.render.CrawlDirection

/**
 * Optional per-filter behavior tuning (D-29). Overrides the defaults in [BugBehavior.Companion].
 *
 * Plan 04-04 wires these through [FilterEngine.createBehaviorState] into [BehaviorState] fields,
 * which [BugBehavior] tick loops read from state (not from BugBehavior companion constants directly).
 *
 * Sensible defaults mirror [BugBehavior] companion constants so a [FilterDefinition] with
 * `behaviorConfig = null` behaves identically to one with a [BehaviorConfig] of default values.
 *
 * SpriteManifest carries a raw [kotlinx.serialization.json.JsonElement] `behaviorConfig` field
 * (Phase 04-04 shape extension) but Phase 04-04 does NOT parse manifest JSON into these types —
 * the 15 catalog entries hardcode their configs here. Future phases that parse manifest.json must
 * wrap the decode in try/catch(SerializationException) per T-04-02.
 */
sealed interface BehaviorConfig {

    data class Crawl(
        val direction: CrawlDirection = CrawlDirection.CW,
        val speedFactor: Float = BugBehavior.CRAWL_SPEED_FACTOR_DEFAULT,
    ) : BehaviorConfig

    data class Swarm(
        val instanceCount: Int = BugBehavior.SWARM_INSTANCE_COUNT_DEFAULT,
        val driftSpeedMin: Float = BugBehavior.SWARM_DRIFT_SPEED_MIN,
        val driftSpeedMax: Float = BugBehavior.SWARM_DRIFT_SPEED_MAX,
    ) : BehaviorConfig

    data class Fall(
        val spawnIntervalMinMs: Int = BugBehavior.FALL_SPAWN_INTERVAL_MIN_MS,
        val spawnIntervalMaxMs: Int = BugBehavior.FALL_SPAWN_INTERVAL_MAX_MS,
        val gravityFactor: Float = BugBehavior.FALL_GRAVITY_FACTOR_DEFAULT,
        val maxInstances: Int = BugBehavior.FALL_MAX_INSTANCES_DEFAULT,
    ) : BehaviorConfig
}
