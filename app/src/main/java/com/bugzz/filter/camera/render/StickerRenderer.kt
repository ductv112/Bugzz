package com.bugzz.filter.camera.render

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
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
    @GuardedBy("lock") private var previewWidth: Int = 0
    @GuardedBy("lock") private var previewHeight: Int = 0

    /** Called from main thread on gesture event. */
    fun setStickerState(state: StickerState) = synchronized(lock) {
        stickerState = state
    }

    /**
     * 05-gaps-02 fix: set preview Compose-px dimensions so onDraw can scale
     * Compose-space [StickerState.offset] → buffer-space coords for the
     * OverlayEffect canvas (which runs in buffer dims, not Compose px).
     */
    fun setPreviewSize(width: Int, height: Int) = synchronized(lock) {
        previewWidth = width
        previewHeight = height
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
        val snapshot = synchronized(lock) {
            Quadruple(stickerState, activeDef, previewWidth, previewHeight)
        }
        val state = snapshot.first
        val definition = snapshot.second ?: return
        val pvW = snapshot.third
        val pvH = snapshot.fourth
        val frameIdx = computeFrameIdx(frame.timestampNanos, definition)
        val bitmap: Bitmap = assetLoader.get(definition.assetDir, frameIdx) ?: return

        // 05-gaps-02: OverlayEffectBuilder applied canvas.setMatrix(sensorToBufferTransform)
        // before calling us — that maps SENSOR coords → buffer coords for face-anchored render.
        // StickerState.offset is in COMPOSE-PREVIEW px (touch input space), not sensor coords.
        // Reset matrix to identity then scale offset to BUFFER canvas dimensions.
        canvas.save()
        canvas.setMatrix(Matrix())  // reset OverlayEffectBuilder's sensorToBufferTransform

        val canvasW = canvas.width.toFloat()
        val canvasH = canvas.height.toFloat()
        // 05-gaps-02: Compose preview PORTRAIT (1220×2712) → OverlayEffect canvas LANDSCAPE buffer (1920×1080).
        // PreviewView rotates buffer 90° CW for display + mirrors X for front camera.
        // To make sticker render at display position matching user's gesture, invert both axes:
        //   - Compose Y (long axis) → buffer X (long), inverted: bufferX = canvasW - normY
        //   - Compose X (short axis) → buffer Y (short), inverted: bufferY = canvasH - normX
        // Empirically verified on Xiaomi 13T front camera. Back-cam uses same path; CCW rotation
        // is symmetric for centered placement, drift may show on edge cases (Phase 7 cross-OEM matrix).
        val bufferX = if (pvH > 0) canvasW - state.offset.y * canvasW / pvH.toFloat() else state.offset.x
        val bufferY = if (pvW > 0) canvasH - state.offset.x * canvasH / pvW.toFloat() else state.offset.y

        canvas.translate(bufferX, bufferY)
        canvas.rotate(state.rotation)
        canvas.scale(state.scale, state.scale)
        canvas.drawBitmap(bitmap, -bitmap.width / 2f, -bitmap.height / 2f, null)
        canvas.restore()

        Timber.tag("StickerRenderer").v(
            "draw filter=%s frame=%d off=%.0f,%.0f → buffer=%.0f,%.0f canvas=%dx%d preview=%dx%d scale=%.2f rot=%.0f",
            definition.id, frameIdx, state.offset.x, state.offset.y, bufferX, bufferY,
            canvas.width, canvas.height, pvW, pvH, state.scale, state.rotation,
        )
    }

    private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

    private fun computeFrameIdx(tsNanos: Long, def: FilterDefinition): Int {
        // Phase 3 fix b7f74cf: require frameCount > 0 (FilterDefinition.init already enforces
        // this, but we guard here too for belt-and-suspenders contract clarity).
        require(def.frameCount > 0) { "frameCount must be > 0 (Phase 3 fix b7f74cf)" }
        val frameDurationNanos = def.frameDurationMs * 1_000_000L
        return ((tsNanos / frameDurationNanos) % def.frameCount).toInt()
    }
}
