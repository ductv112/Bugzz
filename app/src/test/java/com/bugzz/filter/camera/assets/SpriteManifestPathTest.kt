package com.bugzz.filter.camera.assets

import com.bugzz.filter.camera.filter.SpriteManifest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Plan 07-02 — SpriteManifest.frameExtension field validation (un-Ignored from Wave 0 RED).
 *
 * Pins PRF-04 manifest correctness post-WebP conversion (D-07 + RESEARCH Pattern 5):
 *  - Default frame extension is "png" (backcompat — existing manifests without the field).
 *  - When manifest sets frameExtension="webp", deserializer returns "webp".
 *
 * RESEARCH §Anti-pattern: "Mutating manifest.json paths and forgetting to update tests" —
 * this is the test that prevents that drift.
 */
class SpriteManifestPathTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun default_frame_extension_is_png_for_backcompat() {
        val payload = """
            {
              "id": "test",
              "displayName": "Test",
              "frameCount": 1,
              "frameDurationMs": 33,
              "anchorType": "NOSE_TIP",
              "behavior": "STATIC",
              "scaleFactor": 0.3
            }
        """.trimIndent()
        val manifest = json.decodeFromString(SpriteManifest.serializer(), payload)
        // kotlinx-serialization assigns the default when the field is missing from JSON.
        assertEquals("png", manifest.frameExtension)
    }

    @Test
    fun webp_frame_extension_overrides_when_set_in_manifest_json() {
        val payload = """
            {
              "id": "test_webp",
              "displayName": "Test WebP",
              "frameCount": 1,
              "frameDurationMs": 33,
              "anchorType": "NOSE_TIP",
              "behavior": "STATIC",
              "scaleFactor": 0.3,
              "frameExtension": "webp"
            }
        """.trimIndent()
        val manifest = json.decodeFromString(SpriteManifest.serializer(), payload)
        assertEquals("webp", manifest.frameExtension)
    }
}
