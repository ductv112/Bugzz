package com.bugzz.filter.camera.render

import android.graphics.Canvas
import androidx.camera.effects.Frame
import com.bugzz.filter.camera.detector.SmoothedFace
import com.bugzz.filter.camera.filter.AssetLoader
import com.bugzz.filter.camera.filter.FilterDefinition
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Production filter renderer — draws animated bug sprites onto the [OverlayEffect] Canvas (D-27/28).
 *
 * STUB — Plan 03-03 implements:
 *   - [setFilter]: writes to AtomicReference<FilterDefinition?>; calls assetLoader.preload() on cameraExecutor
 *   - [onDraw]: resolves anchor via FaceLandmarkMapper; advances flipbook frame index from frame.timestamp;
 *     calls BugBehavior.tick(); draws sprite bitmap at computed position; early-returns if face==null or
 *     no active filter or bitmap not yet cached (D-11 no-ghost contract)
 *
 * Logging policy (T-03-05): only log filterId + frameIndex, NEVER PointF landmark coords.
 */
@Singleton
class FilterEngine @Inject constructor(
    private val assetLoader: AssetLoader,
) {

    /**
     * Set the active filter. Pass null to disable filter rendering.
     * STUB: no-op. Plan 03-03 implements AtomicReference write + preload trigger.
     */
    fun setFilter(definition: FilterDefinition?): Unit = TODO("Plan 03-03")

    /**
     * Draw the current active filter sprite onto [canvas] for the given [face].
     * Early-returns silently if [face] is null, no filter is set, or bitmap not yet cached.
     *
     * Caller must have already called `canvas.setMatrix(frame.sensorToBufferTransform)`.
     * STUB: no-op. Plan 03-03 implements the full draw path.
     */
    fun onDraw(canvas: Canvas, frame: Frame, face: SmoothedFace?): Unit =
        TODO("Plan 03-03")
}
