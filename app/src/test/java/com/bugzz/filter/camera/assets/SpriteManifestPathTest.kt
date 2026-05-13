package com.bugzz.filter.camera.assets

import org.junit.Assert.fail
import org.junit.Ignore
import org.junit.Test

/**
 * Nyquist Wave 0 RED scaffolds — Plan 07-02 lands SpriteManifest.frameExtension field + un-Ignores.
 *
 * Pins PRF-04 manifest correctness post-WebP conversion (D-07 + RESEARCH Pattern 5):
 *  - Default frame extension is "png" (backcompat — existing manifests without the field).
 *  - When manifest sets frameExtension="webp", AssetLoader resolves frame_NN.webp path.
 *
 * RESEARCH §Anti-pattern: "Mutating manifest.json paths and forgetting to update tests" —
 * this is the test that prevents that drift.
 */
class SpriteManifestPathTest {

    @Test @Ignore("Plan 07-02 — SpriteManifest.frameExtension default field pending")
    fun default_frame_extension_is_png_for_backcompat() {
        fail("Plan 07-02 adds val frameExtension: String = \"png\" — Wave 0 RED")
    }

    @Test @Ignore("Plan 07-02 — SpriteManifest.frameExtension default field pending")
    fun webp_frame_extension_overrides_when_set_in_manifest_json() {
        fail("Plan 07-02 wires AssetLoader to use manifest.frameExtension — Wave 0 RED")
    }
}
