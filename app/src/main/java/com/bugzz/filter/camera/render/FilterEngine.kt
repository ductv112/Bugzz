package com.bugzz.filter.camera.render

import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import androidx.camera.effects.Frame
import com.bugzz.filter.camera.detector.FaceLandmarkMapper
import com.bugzz.filter.camera.detector.SmoothedFace
import com.bugzz.filter.camera.filter.AssetLoader
import com.bugzz.filter.camera.filter.FilterDefinition
import timber.log.Timber
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Production filter renderer — draws animated bug sprites onto the [OverlayEffect] Canvas (D-27/28).
 *
 * Resolves anchor via [FaceLandmarkMapper]; advances flipbook frame index from
 * [Frame.timestampNanos]; dispatches [BugBehavior.tick]; draws the sprite bitmap.
 *
 * Early-return rules (D-11 no-ghost contract):
 *   - [face] is null (no detection on this frame)
 *   - no active filter ([setFilter] never called or called with null)
 *   - [AssetLoader.get] returns null (preload not yet complete)
 *
 * Logging policy (T-03-05): only logs filterId + frameIndex — NEVER PointF landmark coords.
 */
@Singleton
class FilterEngine @Inject constructor(
    private val assetLoader: AssetLoader,
) {
    private val activeFilter = AtomicReference<FilterDefinition?>(null)

    /** Single BugState instance — re-used per frame to avoid allocation in draw loop. */
    private val bugState = BugState()

    private val spritePaint = Paint(Paint.ANTI_ALIAS_FLAG)

    /**
     * Set the active filter. Pass null to disable filter rendering.
     *
     * Resets per-bug state so the new filter always starts at frame 0 (REN-07).
     * Triggers [AssetLoader.preload] asynchronously on the caller's thread if not already cached.
     */
    fun setFilter(definition: FilterDefinition?) {
        activeFilter.set(definition)
        // Reset flipbook state so the new filter starts at frame 0 (REN-07).
        bugState.lastFrameIndex = -1
        bugState.lastFrameAdvanceTimestampNanos = 0L
    }

    /**
     * Draw the current active filter sprite onto [canvas] for the given [face].
     *
     * Early-returns silently if [face] is null, no filter is set, or bitmap not yet cached.
     * Applies [Frame.sensorToBufferTransform] via [Canvas.setMatrix] before drawing (CAM-07).
     *
     * Logging policy (T-03-05): only logs filterId + frameIndex, NEVER PointF coords.
     */
    fun onDraw(canvas: Canvas, frame: Frame, face: SmoothedFace?) {
        val filter = activeFilter.get() ?: return
        if (face == null) return

        // CAM-07: apply sensor→buffer transform so sprite is correctly oriented/scaled.
        // Fall back to identity Matrix if transform is null (should never happen on a real device).
        canvas.setMatrix(frame.sensorToBufferTransform ?: Matrix())

        // Resolve anchor point from face contours.
        val anchor = FaceLandmarkMapper.anchorPoint(face, filter.anchorType) ?: return

        // Flipbook frame index: absolute timestamp / duration per frame, wrapped to frameCount.
        // Using absolute timestampNanos (not relative to setFilter time) so the animation
        // phase is stable and deterministic given the sensor clock.
        val tsNanos = frame.timestampNanos
        val frameDurationNanos = filter.frameDurationMs * 1_000_000L
        val frameIdx = if (frameDurationNanos > 0L) {
            ((tsNanos / frameDurationNanos) % filter.frameCount).toInt()
        } else {
            0
        }

        // D-11 no-ghost: skip draw if bitmap not yet cached (preload incomplete).
        val bitmap = assetLoader.get(filter.id, frameIdx) ?: return

        // Advance BugBehavior state (Static pins position = anchor, velocity = 0).
        val dtMs = if (bugState.lastFrameAdvanceTimestampNanos == 0L) 0L
                   else (tsNanos - bugState.lastFrameAdvanceTimestampNanos) / 1_000_000L
        filter.behavior.tick(bugState, face, anchor, dtMs)
        bugState.lastFrameIndex = frameIdx
        bugState.lastFrameAdvanceTimestampNanos = tsNanos

        // Compute sprite dimensions: scaleFactor * face bbox width, maintaining aspect ratio.
        val spriteW = face.boundingBox.width() * filter.scaleFactor
        val spriteH = spriteW * bitmap.height.toFloat() / bitmap.width.toFloat()

        // Draw sprite centred on the resolved anchor/position.
        val left = bugState.position.x - spriteW / 2f
        val top  = bugState.position.y - spriteH / 2f

        canvas.save()
        canvas.translate(left, top)
        canvas.drawBitmap(bitmap, 0f, 0f, spritePaint)
        canvas.restore()

        Timber.tag("FilterEngine").v("filter=%s frame=%d", filter.id, frameIdx)
    }
}
