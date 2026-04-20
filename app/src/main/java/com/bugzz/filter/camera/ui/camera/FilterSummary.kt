package com.bugzz.filter.camera.ui.camera

/**
 * Immutable DTO for filter picker rendering (Plan 04-05 / 04-06).
 *
 * Does NOT hold a [android.graphics.Bitmap] — the picker uses Coil [AsyncImage] to load
 * thumbnails from `file:///android_asset/$assetDir/frame_00.png` (D-06).
 *
 * @param id         Stable filter identifier — matches [FilterDefinition.id] and is used as
 *                   the Compose key in LazyRow (no recomposition on list reorder).
 * @param displayName Human-readable label shown below the thumbnail (10sp, maxLines=1).
 * @param assetDir   Asset path prefix (e.g., "sprites/sprite_spider"). Coil loads the
 *                   thumbnail as "file:///android_asset/$assetDir/frame_00.png".
 */
data class FilterSummary(
    val id: String,
    val displayName: String,
    val assetDir: String,
)
