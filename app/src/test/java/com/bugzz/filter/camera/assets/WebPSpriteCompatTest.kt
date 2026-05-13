package com.bugzz.filter.camera.assets

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Plan 07-02 — WebP sprite decode validation (un-Ignored from Wave 0 RED).
 *
 * Pins PRF-04 (D-07 WebP conversion preserves bitmap fidelity per RESEARCH Pattern 5):
 *  - BitmapFactory.decodeStream of a WebP frame yields Bitmap.Config.ARGB_8888 (not RGB_565).
 *  - Alpha channel survives lossless WebP encode/decode round-trip.
 *
 * Fixture: `app/src/main/assets/sprites/test_filter_webp/frame_00.webp` (lossless-encoded
 * from sprite_spider/frame_00.png via Pillow 12 in Plan 07-02; alpha-rich).
 *
 * Robolectric required — Bitmap.Config and BitmapFactory.decodeStream need the
 * shadow impl per existing AssetLoaderTest convention.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class WebPSpriteCompatTest {

    // Per existing AssetLoaderTest convention (line 166): RuntimeEnvironment is the
    // Robolectric-supported entry point for getting an Android Context inside JVM unit tests
    // without pulling in androidx.test.core.
    private val context: Context get() = RuntimeEnvironment.getApplication()

    @Test
    fun webp_frame_decodes_to_argb_8888_bitmap() {
        val opts = BitmapFactory.Options().apply { inPreferredConfig = Bitmap.Config.ARGB_8888 }
        val bitmap = context.assets.open("sprites/test_filter_webp/frame_00.webp").use { stream ->
            BitmapFactory.decodeStream(stream, null, opts)
        }
        assertNotNull("WebP must decode to non-null Bitmap", bitmap)
        assertEquals(Bitmap.Config.ARGB_8888, bitmap!!.config)
    }

    @Test
    fun webp_alpha_channel_preserved_through_decode() {
        val bitmap = context.assets.open("sprites/test_filter_webp/frame_00.webp").use { stream ->
            BitmapFactory.decodeStream(stream)
        }
        assertNotNull(bitmap)
        // Lossless WebP must round-trip the alpha channel. The fixture was encoded from a
        // sprite_spider frame which has substantial transparent regions; scan all pixels and
        // assert AT LEAST ONE has alpha < 255.
        val pixels = IntArray(bitmap!!.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        val hasTransparent = pixels.any { (it ushr 24) and 0xFF < 255 }
        assertTrue("WebP alpha channel must round-trip through decode", hasTransparent)
    }
}
