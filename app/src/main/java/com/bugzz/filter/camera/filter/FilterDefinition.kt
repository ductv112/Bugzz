package com.bugzz.filter.camera.filter

import com.bugzz.filter.camera.detector.FaceLandmarkMapper
import com.bugzz.filter.camera.render.BugBehavior

/**
 * Immutable specification for a single bug filter (D-29).
 *
 * @property id             Unique identifier, e.g. "spider_nose_static"
 * @property displayName    Human-readable label shown in the filter picker.
 * @property anchorType     Which face landmark the sprite is pinned to.
 * @property behavior       State-machine variant driving per-frame sprite movement.
 * @property frameCount     Number of PNG frames in the flipbook (≥1; 1 = still image).
 * @property frameDurationMs Duration per frame in milliseconds (e.g. 1000/24 ≈ 42ms for 24fps).
 * @property scaleFactor    Sprite width as a fraction of the face bounding-box width (e.g. 0.20f).
 * @property assetDir       Path relative to `assets/` containing frame_00.png … manifest.json.
 * @property mirrorable     If true, sprite is horizontally flipped when the front camera is active.
 * @property behaviorConfig Optional per-filter behavior tuning (D-29). Overrides BugBehavior
 *   companion defaults for CRAWL direction, SWARM instance count, FALL gravity/cap, etc.
 *   Null = use BugBehavior companion defaults (backward-compatible default).
 */
data class FilterDefinition(
    val id: String,
    val displayName: String,
    val anchorType: FaceLandmarkMapper.Anchor,
    val behavior: BugBehavior,
    val frameCount: Int,
    val frameDurationMs: Long,
    val scaleFactor: Float,
    val assetDir: String,
    val mirrorable: Boolean = true,
    val behaviorConfig: BehaviorConfig? = null,   // D-29 Phase 4 addition
) {
    init {
        require(frameCount > 0) { "frameCount must be > 0, got $frameCount for filter '$id'" }
        require(scaleFactor > 0f && scaleFactor <= 1f) {
            "scaleFactor must be in (0,1], got $scaleFactor for '$id'"
        }
    }
}
