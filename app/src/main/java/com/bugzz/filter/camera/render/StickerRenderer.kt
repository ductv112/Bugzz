package com.bugzz.filter.camera.render

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.annotation.GuardedBy
import androidx.camera.effects.Frame
import com.bugzz.filter.camera.filter.AssetLoader
import com.bugzz.filter.camera.filter.FilterDefinition
import com.bugzz.filter.camera.ui.insect.StickerState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.concurrent.Executor
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * Draws the active filter's sprite at [StickerState] transform into the OverlayEffect canvas.
 *
 * Draw order (D-03 / UI-SPEC §6):
 *   canvas.save()
 *   canvas.translate(state.offset.x, state.offset.y)
 *   canvas.rotate(state.rotation)
 *   canvas.scale(state.scale, state.scale)
 *   canvas.drawBitmap(bitmap, -bitmap.width/2f, -bitmap.height/2f, null)  // centered at origin
 *   canvas.restore()
 *
 * Thread-safety: [setStickerState] and [setActiveFilter] are called from the main thread
 * (via ViewModel); [onDraw] runs on BugzzRenderThread (HandlerThread in OverlayEffectBuilder).
 * A simple `synchronized(lock)` guards the two mutable fields — no contention at 30fps.
 *
 * Phase 5.
 */
@Singleton
class StickerRenderer @Inject constructor(
    private val assetLoader: AssetLoader,
    @Named("cameraExecutor") private val cameraExecutor: Executor,
) {
    private val lock = Any()
    private val preloadScope = CoroutineScope(cameraExecutor.asCoroutineDispatcher())

    @GuardedBy("lock") private var stickerState: StickerState = StickerState()
    @GuardedBy("lock") private var activeDef: FilterDefinition? = null

    /** Called from main thread on gesture event. */
    fun setStickerState(state: StickerState) = synchronized(lock) {
        stickerState = state
    }

    /**
     * Called from main thread on filter selection. Stores the active filter definition and
     * triggers asset preload on cameraExecutor so frames are ready before the next onDraw.
     *
     * assetLoader.preload(def.assetDir) — Phase 4 fix (04-gaps-01): assetDir not filterId.
     */
    fun setActiveFilter(def: FilterDefinition) {
        synchronized(lock) { activeDef = def }
        preloadScope.launch { assetLoader.preload(def.assetDir) }
    }

    /**
     * Called from BugzzRenderThread on every OverlayEffect frame.
     * Early-returns when no active filter or bitmap not yet loaded.
     */
    fun onDraw(canvas: Canvas, frame: Frame) {
        val (state, def) = synchronized(lock) { stickerState to activeDef }
        val definition = def ?: return
        val frameIdx = computeFrameIdx(frame.timestampNanos, definition)
        val bitmap: Bitmap = assetLoader.get(definition.assetDir, frameIdx) ?: return

        canvas.save()
        canvas.translate(state.offset.x, state.offset.y)
        canvas.rotate(state.rotation)
        canvas.scale(state.scale, state.scale)
        canvas.drawBitmap(bitmap, -bitmap.width / 2f, -bitmap.height / 2f, null)
        canvas.restore()

        Timber.tag("StickerRenderer").v(
            "draw filter=%s frame=%d off=%.0f,%.0f scale=%.2f rot=%.0f",
            definition.id, frameIdx, state.offset.x, state.offset.y, state.scale, state.rotation,
        )
    }

    private fun computeFrameIdx(tsNanos: Long, def: FilterDefinition): Int {
        // Phase 3 fix b7f74cf: require frameCount > 0 (FilterDefinition.init already enforces
        // this, but we guard here too for belt-and-suspenders contract clarity).
        require(def.frameCount > 0) { "frameCount must be > 0 (Phase 3 fix b7f74cf)" }
        val frameDurationNanos = def.frameDurationMs * 1_000_000L
        return ((tsNanos / frameDurationNanos) % def.frameCount).toInt()
    }
}
