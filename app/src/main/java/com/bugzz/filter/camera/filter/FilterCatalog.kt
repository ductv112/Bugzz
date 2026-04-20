package com.bugzz.filter.camera.filter

import com.bugzz.filter.camera.detector.FaceLandmarkMapper.Anchor
import com.bugzz.filter.camera.render.BugBehavior
import com.bugzz.filter.camera.render.CrawlDirection

/**
 * Phase 4 catalog — exactly 15 [FilterDefinition] entries per 04-CONTEXT D-02 amended roster.
 *
 * Ordering is curated for picker UX flow — STATIC + behavior variety mixed for visual range.
 *
 * assetDir values reference the 4 shared sprite groups extracted by Plan 04-01:
 *   sprites/sprite_spider  — 23 frames (spider_prankfilter.json)
 *   sprites/sprite_bugA    — 7 frames  (home_lottie.json imgSeq 0..6)
 *   sprites/sprite_bugB    — 12 frames (home_lottie.json imgSeq 14..25)
 *   sprites/sprite_bugC    — 16 frames (home_lottie.json imgSeq 38..53)
 *
 * Frame counts confirmed from Plan 04-01 SUMMARY actuals (match research estimates exactly):
 *   sprite_spider=23, sprite_bugA=7, sprite_bugB=12, sprite_bugC=16.
 *
 * Behavior distribution: 6 STATIC + 3 CRAWL + 3 SWARM + 3 FALL = 15 (D-02 compliant).
 * FALL filters note: anchorType=NOSE_TIP is a sentinel — falling bugs spawn from y=0 across
 * full preview width; anchorType satisfies the non-nullable contract but is not used for spawn.
 */
object FilterCatalog {

    // Frame counts — confirmed from Plan 04-01 SUMMARY.md extracted actuals.
    private const val SPIDER_FRAMES = 23
    private const val BUG_A_FRAMES  = 7
    private const val BUG_B_FRAMES  = 12
    private const val BUG_C_FRAMES  = 16

    // ~24fps playback (1000/24 ≈ 42ms). manifest.json values match.
    private const val DEFAULT_FRAME_DURATION_MS = 42L

    // Scale factors per behavior class — STATIC larger (single sprite), SWARM/FALL smaller (density).
    private const val SCALE_STATIC = 0.20f
    private const val SCALE_CRAWL  = 0.18f
    private const val SCALE_SWARM  = 0.12f
    private const val SCALE_FALL   = 0.12f

    val all: List<FilterDefinition> = listOf(

        // ---- SPIDER (sprite_spider, 23 frames) ----

        // 1 STATIC — spider pinned to nose tip
        FilterDefinition(
            id = "spider_nose_static",
            displayName = "Spider Nose",
            anchorType = Anchor.NOSE_TIP,
            behavior = BugBehavior.Static,
            frameCount = SPIDER_FRAMES,
            frameDurationMs = DEFAULT_FRAME_DURATION_MS,
            scaleFactor = SCALE_STATIC,
            assetDir = "sprites/sprite_spider",
        ),

        // 2 STATIC — spider pinned to forehead
        FilterDefinition(
            id = "spider_forehead_static",
            displayName = "Spider Forehead",
            anchorType = Anchor.FOREHEAD,
            behavior = BugBehavior.Static,
            frameCount = SPIDER_FRAMES,
            frameDurationMs = DEFAULT_FRAME_DURATION_MS,
            scaleFactor = SCALE_STATIC,
            assetDir = "sprites/sprite_spider",
        ),

        // 3 CRAWL — spider traverses face contour clockwise
        FilterDefinition(
            id = "spider_jawline_crawl",
            displayName = "Spider Crawl",
            anchorType = Anchor.NOSE_TIP,
            behavior = BugBehavior.Crawl,
            frameCount = SPIDER_FRAMES,
            frameDurationMs = DEFAULT_FRAME_DURATION_MS,
            scaleFactor = SCALE_CRAWL,
            assetDir = "sprites/sprite_spider",
            behaviorConfig = BehaviorConfig.Crawl(direction = CrawlDirection.CW),
        ),

        // 4 SWARM — 6 spiders swarm toward nose
        FilterDefinition(
            id = "spider_swarm",
            displayName = "Spider Swarm",
            anchorType = Anchor.NOSE_TIP,
            behavior = BugBehavior.Swarm,
            frameCount = SPIDER_FRAMES,
            frameDurationMs = DEFAULT_FRAME_DURATION_MS,
            scaleFactor = SCALE_SWARM,
            assetDir = "sprites/sprite_spider",
            behaviorConfig = BehaviorConfig.Swarm(instanceCount = 6),
        ),

        // ---- BUG A (sprite_bugA, 7 frames) ----

        // 5 STATIC — bugA pinned to forehead
        FilterDefinition(
            id = "bugA_forehead_static",
            displayName = "Bug A Forehead",
            anchorType = Anchor.FOREHEAD,
            behavior = BugBehavior.Static,
            frameCount = BUG_A_FRAMES,
            frameDurationMs = DEFAULT_FRAME_DURATION_MS,
            scaleFactor = SCALE_STATIC,
            assetDir = "sprites/sprite_bugA",
        ),

        // 6 STATIC — bugA pinned to left cheek
        FilterDefinition(
            id = "bugA_cheek_static",
            displayName = "Bug A Cheek",
            anchorType = Anchor.LEFT_CHEEK,
            behavior = BugBehavior.Static,
            frameCount = BUG_A_FRAMES,
            frameDurationMs = DEFAULT_FRAME_DURATION_MS,
            scaleFactor = SCALE_STATIC,
            assetDir = "sprites/sprite_bugA",
        ),

        // 7 SWARM — 7 bugA instances swarm toward nose
        FilterDefinition(
            id = "bugA_swarm",
            displayName = "Bug A Swarm",
            anchorType = Anchor.NOSE_TIP,
            behavior = BugBehavior.Swarm,
            frameCount = BUG_A_FRAMES,
            frameDurationMs = DEFAULT_FRAME_DURATION_MS,
            scaleFactor = SCALE_SWARM,
            assetDir = "sprites/sprite_bugA",
            behaviorConfig = BehaviorConfig.Swarm(instanceCount = 7),
        ),

        // 8 FALL — bugA rain from top (sentinel anchor NOSE_TIP)
        FilterDefinition(
            id = "bugA_fall",
            displayName = "Bug A Rain",
            anchorType = Anchor.NOSE_TIP,
            behavior = BugBehavior.Fall,
            frameCount = BUG_A_FRAMES,
            frameDurationMs = DEFAULT_FRAME_DURATION_MS,
            scaleFactor = SCALE_FALL,
            assetDir = "sprites/sprite_bugA",
            behaviorConfig = BehaviorConfig.Fall(maxInstances = 8),
        ),

        // ---- BUG B (sprite_bugB, 12 frames) ----

        // 9 STATIC — bugB pinned to nose tip
        FilterDefinition(
            id = "bugB_nose_static",
            displayName = "Bug B Nose",
            anchorType = Anchor.NOSE_TIP,
            behavior = BugBehavior.Static,
            frameCount = BUG_B_FRAMES,
            frameDurationMs = DEFAULT_FRAME_DURATION_MS,
            scaleFactor = SCALE_STATIC,
            assetDir = "sprites/sprite_bugB",
        ),

        // 10 CRAWL — bugB traverses face contour counter-clockwise
        FilterDefinition(
            id = "bugB_crawl",
            displayName = "Bug B Crawl",
            anchorType = Anchor.NOSE_TIP,
            behavior = BugBehavior.Crawl,
            frameCount = BUG_B_FRAMES,
            frameDurationMs = DEFAULT_FRAME_DURATION_MS,
            scaleFactor = SCALE_CRAWL,
            assetDir = "sprites/sprite_bugB",
            behaviorConfig = BehaviorConfig.Crawl(direction = CrawlDirection.CCW),
        ),

        // 11 SWARM — 8 bugB instances swarm toward forehead
        FilterDefinition(
            id = "bugB_swarm",
            displayName = "Bug B Swarm",
            anchorType = Anchor.FOREHEAD,
            behavior = BugBehavior.Swarm,
            frameCount = BUG_B_FRAMES,
            frameDurationMs = DEFAULT_FRAME_DURATION_MS,
            scaleFactor = SCALE_SWARM,
            assetDir = "sprites/sprite_bugB",
            behaviorConfig = BehaviorConfig.Swarm(instanceCount = 8),
        ),

        // 12 FALL — bugB rain, faster gravity (sentinel anchor NOSE_TIP)
        FilterDefinition(
            id = "bugB_fall",
            displayName = "Bug B Rain",
            anchorType = Anchor.NOSE_TIP,
            behavior = BugBehavior.Fall,
            frameCount = BUG_B_FRAMES,
            frameDurationMs = DEFAULT_FRAME_DURATION_MS,
            scaleFactor = SCALE_FALL,
            assetDir = "sprites/sprite_bugB",
            behaviorConfig = BehaviorConfig.Fall(maxInstances = 6, gravityFactor = 0.4f),
        ),

        // ---- BUG C (sprite_bugC, 16 frames) ----

        // 13 STATIC — bugC pinned to chin
        FilterDefinition(
            id = "bugC_chin_static",
            displayName = "Bug C Chin",
            anchorType = Anchor.CHIN,
            behavior = BugBehavior.Static,
            frameCount = BUG_C_FRAMES,
            frameDurationMs = DEFAULT_FRAME_DURATION_MS,
            scaleFactor = SCALE_STATIC,
            assetDir = "sprites/sprite_bugC",
        ),

        // 14 CRAWL — bugC traverses face contour clockwise
        FilterDefinition(
            id = "bugC_crawl",
            displayName = "Bug C Crawl",
            anchorType = Anchor.NOSE_TIP,
            behavior = BugBehavior.Crawl,
            frameCount = BUG_C_FRAMES,
            frameDurationMs = DEFAULT_FRAME_DURATION_MS,
            scaleFactor = SCALE_CRAWL,
            assetDir = "sprites/sprite_bugC",
            behaviorConfig = BehaviorConfig.Crawl(direction = CrawlDirection.CW),
        ),

        // 15 FALL — bugC rain, heavier gravity (sentinel anchor NOSE_TIP)
        FilterDefinition(
            id = "bugC_fall",
            displayName = "Bug C Rain",
            anchorType = Anchor.NOSE_TIP,
            behavior = BugBehavior.Fall,
            frameCount = BUG_C_FRAMES,
            frameDurationMs = DEFAULT_FRAME_DURATION_MS,
            scaleFactor = SCALE_FALL,
            assetDir = "sprites/sprite_bugC",
            behaviorConfig = BehaviorConfig.Fall(maxInstances = 8, gravityFactor = 0.6f),
        ),
    )

    fun byId(id: String): FilterDefinition? = all.firstOrNull { it.id == id }
}
