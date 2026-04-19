package com.bugzz.filter.camera.filter

import android.graphics.Bitmap
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

/**
 * Nyquist Wave 0 tests for [AssetLoader] (REN-04 / D-08/09).
 *
 * Covers: sizeOf allocationByteCount contract, cache capacity formula, get-before-preload null,
 * preload populates cache, idempotency, malformed PNG error, manifest JSON parse.
 *
 * Robolectric needed for Bitmap operations (BitmapFactory / allocationByteCount).
 *
 * Wave 0 state: [AssetLoader.preload] is a TODO stub. Tests that call preload() are @Ignore'd.
 * Tests that only touch companion / sizeOfForTest are GREEN in Wave 0. Plan 03-03 un-Ignores.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@LooperMode(LooperMode.Mode.PAUSED)
class AssetLoaderTest {

    // -------------------------------------------------------------------------
    // sizeOf — test seam (GREEN in Wave 0; no TODO involved)
    // -------------------------------------------------------------------------

    @Test
    fun sizeOf_returnsAllocationByteCount_notByteCount() {
        // Create a small ARGB_8888 bitmap and verify that sizeOfForTest uses allocationByteCount
        val bmp = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888)
        // allocationByteCount = rowBytes * height (may be >= byteCount due to padding)
        val expected = bmp.allocationByteCount
        assertTrue("allocationByteCount must be > 0 for 10x10 ARGB_8888 bitmap", expected > 0)

        // sizeOfForTest is the test seam exposed by AssetLoader stub — no executor/context needed
        val loader = buildStubLoader()
        val result = loader.sizeOfForTest(bmp)
        assertEquals(
            "sizeOfForTest must return bitmap.allocationByteCount (not byteCount)",
            expected, result,
        )
    }

    // -------------------------------------------------------------------------
    // Cache capacity — companion test seam (GREEN in Wave 0)
    // -------------------------------------------------------------------------

    @Test
    fun cacheSize_capsAt32MBOrMaxMemoryDivEight() {
        val result = AssetLoader.computeCacheSize()
        val expected = minOf(32 * 1024 * 1024, (Runtime.getRuntime().maxMemory() / 8L).toInt())
        assertEquals(
            "computeCacheSize() must return min(32MB, maxMemory/8)",
            expected, result,
        )
        assertTrue(
            "Cache size must be at least 8 MB (floor guard for very constrained JVM)",
            result >= 8 * 1024 * 1024,
        )
    }

    // -------------------------------------------------------------------------
    // get() before preload — GREEN in Wave 0 (get() already returns null in stub)
    // -------------------------------------------------------------------------

    @Test
    fun get_beforePreload_returnsNull() {
        val loader = buildStubLoader()
        assertNull(
            "get() must return null before preload() is called",
            loader.get("test_filter", 0),
        )
    }

    // -------------------------------------------------------------------------
    // preload() — @Ignore'd until Plan 03-03
    // -------------------------------------------------------------------------

    /**
     * TODO Plan 03-03: un-Ignore when AssetLoader.preload() is implemented.
     */
    @org.junit.Ignore("Plan 03-03 — flip to GREEN when AssetLoader.preload() is implemented")
    @Test
    fun preload_populatesCache_getReturnsBitmap() {
        val loader = buildStubLoader()
        runBlocking { loader.preload("test_filter") }
        val bitmap = loader.get("test_filter", 0)
        assertNotNull("get() must return non-null Bitmap after preload()", bitmap)
        assertTrue("Decoded bitmap must have positive width", bitmap!!.width > 0)
        assertTrue("Decoded bitmap must have positive height", bitmap.height > 0)
    }

    /**
     * TODO Plan 03-03: un-Ignore when AssetLoader.preload() is implemented.
     */
    @org.junit.Ignore("Plan 03-03 — flip to GREEN when AssetLoader.preload() is implemented")
    @Test
    fun preload_idempotent_multipleCallsDoNotReDecode() {
        val loader = buildStubLoader()
        runBlocking {
            loader.preload("test_filter")
            loader.preload("test_filter")  // second call must be a no-op (cache hit)
        }
        // If preload is idempotent, the second call should not throw or corrupt cache
        val bitmap = loader.get("test_filter", 0)
        assertNotNull("Cache must still contain bitmap after idempotent double-preload", bitmap)
    }

    /**
     * TODO Plan 03-03: un-Ignore when AssetLoader.preload() propagates decode errors correctly.
     */
    @org.junit.Ignore("Plan 03-03 — flip to GREEN when AssetLoader.preload() handles malformed PNG")
    @Test
    fun preload_malformedPng_throwsIllegalArgument() {
        // A zero-byte or corrupted PNG should surface as IllegalArgumentException (T-03-02)
        val loader = buildStubLoader()
        try {
            runBlocking { loader.preload("bad_filter") }
            throw AssertionError("Expected IllegalArgumentException for malformed PNG, but no exception thrown")
        } catch (e: IllegalArgumentException) {
            // Expected — T-03-02 mitigation: decode failure must not silently produce null
        }
    }

    // -------------------------------------------------------------------------
    // SpriteManifest JSON parse — GREEN in Wave 0 (SpriteManifest is fully implemented)
    // -------------------------------------------------------------------------

    @Test
    fun manifestJson_deserializesViaKotlinxSerialization() {
        // Read the canonical test manifest from test resources
        val stream = javaClass.classLoader!!
            .getResourceAsStream("sprites/test_filter/manifest.json")
            ?: throw AssertionError("manifest.json not found in test resources at sprites/test_filter/")
        val text = stream.bufferedReader().readText()

        val manifest = Json.decodeFromString<SpriteManifest>(text)

        assertEquals("Manifest id must match", "test_filter", manifest.id)
        assertEquals("Manifest displayName", "Test Filter", manifest.displayName)
        assertEquals("Manifest frameCount", 1, manifest.frameCount)
        assertEquals("Manifest frameDurationMs", 42L, manifest.frameDurationMs)
        assertEquals("Manifest anchorType string", "NOSE_TIP", manifest.anchorType)
        assertEquals("Manifest behavior string", "STATIC", manifest.behavior)
        assertEquals("Manifest scaleFactor", 0.20f, manifest.scaleFactor, 1e-4f)
        assertTrue("Manifest mirrorable default true", manifest.mirrorable)
        assertNull("Manifest bitmapConfig default null", manifest.bitmapConfig)
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Build an AssetLoader with a synchronous inline executor for test simplicity.
     * The stub constructor accepts @ApplicationContext and @Named("cameraExecutor") but
     * in tests we use Robolectric's ApplicationProvider context + a direct executor.
     */
    private fun buildStubLoader(): AssetLoader {
        val ctx = org.robolectric.RuntimeEnvironment.getApplication()
        val executor = java.util.concurrent.Executor { it.run() }
        return AssetLoader(ctx, executor)
    }
}
