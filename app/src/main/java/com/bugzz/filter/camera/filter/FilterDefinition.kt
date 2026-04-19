package com.bugzz.filter.camera.filter

import com.bugzz.filter.camera.detector.FaceLandmarkMapper
import com.bugzz.filter.camera.render.BugBehavior

/**
 * Immutable specification for a single bug filter (D-29).
 *
 * @property id             Unique identifier, e.g. "ant_on_nose_v1"
 * @property displayName    Human-readable label shown in the filter picker.
 * @property anchorType     Which face landmark the sprite is pinned to.
 * @property behavior       State-machine variant driving per-frame sprite movement.
 * @property frameCount     Number of PNG frames in the flipbook (≥1; 1 = still image).
 * @property frameDurationMs Duration per frame in milliseconds (e.g. 1000/24 ≈ 42ms for 24fps).
 * @property scaleFactor    Sprite width as a fraction of the face bounding-box width (e.g. 0.20f).
 * @property assetDir       Path relative to `assets/sprites/` containing frame_00.png … manifest.json.
 * @property mirrorable     If true, sprite is horizontally flipped when the front camera is active.
 *
 * STUB — Plan 03-03 populates FilterCatalog with real instances of this class.
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
)
