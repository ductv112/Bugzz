package com.bugzz.filter.camera.filter

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * JSON manifest for a sprite asset directory (D-06).
 *
 * Layout: `app/src/main/assets/sprites/<groupId>/manifest.json`
 *
 * Parsed via `kotlinx.serialization.json.Json.decodeFromString<SpriteManifest>(text)`.
 *
 * @property id              Sprite group identifier, e.g. "sprite_spider".
 * @property displayName     Human-readable label.
 * @property frameCount      Number of PNG frames (frame_00.png … frame_NN.png).
 * @property frameDurationMs Duration per frame in milliseconds.
 * @property anchorType      Anchor enum name string, e.g. "NOSE_TIP".
 * @property behavior        Behavior enum name string, e.g. "STATIC".
 * @property scaleFactor     Sprite width as fraction of face bbox width.
 * @property mirrorable      Whether the sprite flips horizontally on the front camera.
 * @property bitmapConfig    Optional bitmap decode config override ("RGB_565"). Null = ARGB_8888 (D-08).
 * @property behaviorConfig  Optional raw JSON for per-filter behavior tuning (D-29 / T-04-02).
 *   Phase 04-04 does NOT parse this into [BehaviorConfig] — the 15 catalog entries hardcode their
 *   configs in [FilterCatalog]. Future phases that parse this field MUST wrap in
 *   try/catch(SerializationException) per T-04-02 (untrusted JSON boundary).
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
    val behaviorConfig: JsonElement? = null,   // D-29 shape extension; NOT parsed by Phase 04-04
)
