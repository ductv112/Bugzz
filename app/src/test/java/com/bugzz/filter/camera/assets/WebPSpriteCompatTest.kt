package com.bugzz.filter.camera.assets

import org.junit.Assert.fail
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Nyquist Wave 0 RED scaffolds — Plan 07-02 lands WebP fixture sprite + un-Ignores.
 *
 * Pins PRF-04 (D-07 WebP conversion preserves bitmap fidelity per RESEARCH Pattern 5):
 *  - BitmapFactory.decodeStream of a WebP frame yields Bitmap.Config.ARGB_8888 (not RGB_565).
 *  - Alpha channel survives lossless WebP encode/decode round-trip.
 *
 * Robolectric required — Bitmap.Config and BitmapFactory.decodeStream need the
 * shadow impl per existing AssetLoaderTest convention.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class WebPSpriteCompatTest {

    @Test @Ignore("Plan 07-02 — WebP fixture + AssetLoader extension support pending")
    fun webp_frame_decodes_to_argb_8888_bitmap() {
        fail("Plan 07-02 ships sample webp fixture + AssetLoader path — Wave 0 RED")
    }

    @Test @Ignore("Plan 07-02 — WebP fixture + AssetLoader extension support pending")
    fun webp_alpha_channel_preserved_through_decode() {
        fail("Plan 07-02 verifies alpha != 255 pixels round-trip — Wave 0 RED")
    }
}
