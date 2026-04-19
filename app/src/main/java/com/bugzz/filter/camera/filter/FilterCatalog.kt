package com.bugzz.filter.camera.filter

import com.bugzz.filter.camera.detector.FaceLandmarkMapper
import com.bugzz.filter.camera.render.BugBehavior

/**
 * Phase 3 catalog — exactly 2 filters per D-01/D-02 (ant + spider). Phase 4 scales to 15-25.
 *
 * @see .planning/phases/03-first-filter-end-to-end-photo-capture/03-CONTEXT.md D-01 / D-02
 */
object FilterCatalog {
    private val ANT_ON_NOSE = FilterDefinition(
        id = "ant_on_nose_v1",
        displayName = "Ant on nose",
        anchorType = FaceLandmarkMapper.Anchor.NOSE_TIP,
        behavior = BugBehavior.Static,
        frameCount = 35,
        frameDurationMs = 66L,
        scaleFactor = 0.20f,
        assetDir = "sprites/ant_on_nose_v1",
        mirrorable = true,
    )

    private val SPIDER_ON_FOREHEAD = FilterDefinition(
        id = "spider_on_forehead_v1",
        displayName = "Spider on forehead",
        anchorType = FaceLandmarkMapper.Anchor.FOREHEAD,
        behavior = BugBehavior.Static,
        frameCount = 23,
        frameDurationMs = 66L,
        scaleFactor = 0.22f,
        assetDir = "sprites/spider_on_forehead_v1",
        mirrorable = true,
    )

    val all: List<FilterDefinition> = listOf(ANT_ON_NOSE, SPIDER_ON_FOREHEAD)

    fun byId(id: String): FilterDefinition? = all.firstOrNull { it.id == id }
}
