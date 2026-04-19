package com.bugzz.filter.camera.filter

import android.content.Context
import android.graphics.Bitmap
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import java.util.concurrent.Executor

/**
 * Sprite asset loader — decodes PNG frames from `assets/sprites/<filterId>/` and
 * caches them in an LruCache (D-08/09).
 *
 * STUB — Plan 03-03 implements:
 *   - [preload]: reads manifest.json + decodes all frame_NN.png files on [cameraExecutor]
 *   - [get]: returns the cached Bitmap for (filterId, frameIdx), or null if not preloaded
 *   - Cache: `LruCache<String, Bitmap>` keyed on "$filterId/frame_$idx", size ≤ 32 MB
 *
 * Test seam: [sizeOfForTest] exposes the cache's sizeOf calculation for [AssetLoaderTest]
 * without reflection. KDoc: "Test seam — do not invoke from prod".
 */
@Singleton
class AssetLoader @Inject constructor(
    @ApplicationContext private val context: Context,
    @Named("cameraExecutor") private val cameraExecutor: Executor,
) {

    /**
     * Pre-decode all frames for [filterId] into the cache.
     * STUB: no-op. Plan 03-03 implements frame decode + cache population.
     */
    suspend fun preload(filterId: String): Unit = TODO("Plan 03-03")

    /**
     * Return the cached Bitmap for frame [frameIdx] of [filterId], or null if not yet preloaded.
     * STUB: always returns null. Plan 03-03 returns the cached bitmap.
     */
    fun get(filterId: String, frameIdx: Int): Bitmap? = null

    /**
     * Test seam — exposes the sizeOf calculation used by the internal LruCache.
     * Returns [bitmap.allocationByteCount] (not byteCount — see RESEARCH Pitfall #2).
     *
     * **Test seam — do not invoke from prod.**
     */
    internal fun sizeOfForTest(bitmap: Bitmap): Int = bitmap.allocationByteCount

    companion object {
        /**
         * Compute the LruCache capacity in bytes: min(32 MB, maxMemory/8).
         * Exposed as a test seam so [AssetLoaderTest.cacheSize_capsAt32MBOrMaxMemoryDivEight]
         * can verify the formula without reflection.
         *
         * **Test seam — do not invoke from prod.**
         */
        internal fun computeCacheSize(): Int =
            minOf(32 * 1024 * 1024, (Runtime.getRuntime().maxMemory() / 8L).toInt())
    }
}
