package com.bugzz.filter.camera.filter

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.util.concurrent.Executor
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * Decode sprite frames from assets/ into LruCache<String, Bitmap>. REN-04 / D-08 / D-09.
 *
 * Cache size = min(32 MB, maxMemory/8 in bytes). sizeOf returns allocationByteCount (not byteCount)
 * per Android 19+ recommendation (§Pitfall 2).
 *
 * Do NOT override entryRemoved to recycle bitmaps — GC handles it (§Pitfall 3).
 */
@Singleton
class AssetLoader @Inject constructor(
    @ApplicationContext private val appContext: Context,
    @Named("cameraExecutor") private val cameraExecutor: Executor,
) {
    private val decodeDispatcher = cameraExecutor.asCoroutineDispatcher()

    private val cache: LruCache<String, Bitmap> = object : LruCache<String, Bitmap>(computeCacheSize()) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.allocationByteCount
        // No entryRemoved override — DO NOT recycle (§Pitfall 3).
    }

    /** D-11 — preload all frames for filterId. Suspends until complete or throws. */
    suspend fun preload(filterId: String) = withContext(decodeDispatcher) {
        val manifest = loadManifest(filterId)
        for (idx in 0 until manifest.frameCount) {
            val key = "$filterId/frame_$idx"
            if (cache.get(key) != null) continue   // already cached
            val path = "sprites/$filterId/frame_${idx.toString().padStart(2, '0')}.png"
            val opts = BitmapFactory.Options().apply {
                inPreferredConfig = when (manifest.bitmapConfig) {
                    "RGB_565" -> Bitmap.Config.RGB_565
                    else      -> Bitmap.Config.ARGB_8888   // D-08 default
                }
            }
            val bitmap = try {
                appContext.assets.open(path).use { stream ->
                    BitmapFactory.decodeStream(stream, null, opts)
                        ?: throw IllegalArgumentException("decode failed: $path")
                }
            } catch (e: IllegalArgumentException) {
                throw e   // re-throw our own contract exception as-is
            } catch (e: Exception) {
                // T-03-02: BitmapFactory may throw RuntimeException on malformed PNG (e.g.
                // Robolectric ShadowBitmapFactory wraps IIOException as RuntimeException).
                // Normalise all decode failures to IllegalArgumentException for a uniform
                // caller contract — Wave 3 ViewModel layer catches this and emits OneShotEvent.Error.
                throw IllegalArgumentException("decode failed: $path", e)
            }
            cache.put(key, bitmap)
        }
    }

    /** Non-blocking lookup. Returns null if preload incomplete (D-11 semantics). */
    fun get(filterId: String, frameIdx: Int): Bitmap? =
        cache.get("$filterId/frame_$frameIdx")

    private fun loadManifest(filterId: String): SpriteManifest {
        val path = "sprites/$filterId/manifest.json"
        val text = appContext.assets.open(path).bufferedReader().use { it.readText() }
        return Json.decodeFromString(SpriteManifest.serializer(), text)
    }

    /**
     * Test seam — exposes the sizeOf calculation used by the internal LruCache.
     * Returns bitmap.allocationByteCount (not byteCount — see RESEARCH Pitfall #2).
     *
     * **Test seam — do not invoke from prod.**
     */
    internal fun sizeOfForTest(bitmap: Bitmap): Int = bitmap.allocationByteCount

    companion object {
        private const val MAX_BYTES: Int = 32 * 1024 * 1024   // 32 MB cap per D-09

        /** D-09 — min(32 MB, maxMemory/8). Exposed for AssetLoaderTest. */
        internal fun computeCacheSize(): Int {
            val oneEighthMaxMemory = (Runtime.getRuntime().maxMemory() / 8L).toInt()
            return minOf(MAX_BYTES, oneEighthMaxMemory.coerceAtLeast(8 * 1024 * 1024))
        }
    }
}
