package com.bugzz.filter.camera.filter

import kotlinx.serialization.Serializable

/**
 * JSON manifest for a sprite asset directory (D-06).
 *
 * Layout: `app/src/main/assets/sprites/<filterId>/manifest.json`
 *
 * Parsed via `kotlinx.serialization.json.Json.decodeFromString<SpriteManifest>(text)`.
 * This is the stable contract — Plan 03-03 uses it directly, no TODO body needed.
 *
 * @property id              Filter identifier, e.g. "ant_on_nose_v1".
 * @property displayName     Human-readable label.
 * @property frameCount      Number of PNG frames (frame_00.png … frame_NN.png).
 * @property frameDurationMs Duration per frame in milliseconds.
 * @property anchorType      Anchor enum name string, e.g. "NOSE_TIP".
 * @property behavior        Behavior enum name string, e.g. "STATIC".
 * @property scaleFactor     Sprite width as fraction of face bbox width.
 * @property mirrorable      Whether the sprite flips horizontally on the front camera.
 * @property bitmapConfig    Optional bitmap decode config override ("RGB_565"). Null = ARGB_8888 (D-08).
 */
@Serializable
data class SpriteManifest(
    val id: String,
    val displayName: String,
    val frameCount: Int,
    val frameDurationMs: Long,
    val anchorType: String,
    val behavior: String,
    val scaleFactor: Float,
    val mirrorable: Boolean = true,
    val bitmapConfig: String? = null,
)
