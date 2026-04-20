package com.bugzz.filter.camera.render

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.RectF
import androidx.camera.effects.Frame
import com.bugzz.filter.camera.detector.FaceLandmarkMapper
import com.bugzz.filter.camera.detector.SmoothedFace
import com.bugzz.filter.camera.filter.AssetLoader
import com.bugzz.filter.camera.filter.BehaviorConfig
import com.bugzz.filter.camera.filter.FilterDefinition
import com.google.mlkit.vision.face.FaceContour
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Production filter renderer — draws animated bug sprites onto the [OverlayEffect] Canvas (D-27/28).
 *
 * Phase 04-04: upgraded to multi-face rendering (D-13/D-14/D-22).
 *   - [perFaceState]: ConcurrentHashMap<Int, BehaviorState> keyed by SmoothedFace.trackingId (D-13).
 *   - Primary face (largest bbox area) → full contour anchor via [FaceLandmarkMapper] (D-22).
 *   - Secondary face (second-largest) → bbox-centre fallback at 40% height (D-22).
 *   - Soft cap: [MAX_DRAWS_PER_FRAME] = 20 total draw calls across all faces (D-14).
 *   - [onFaceLost]: removes perFaceState entry when tracker drops a trackingId (D-23).
 *
 * Early-return rules (D-11 no-ghost contract):
 *   - faces list is empty or null active filter
 *   - [AssetLoader.get] returns null (preload not yet complete)
 *   - primary face anchor cannot be resolved (null contours + no bbox fallback)
 *
 * Logging policy (T-03-05): only logs filterId + frameIndex — NEVER PointF landmark coords.
 */
@Singleton
class FilterEngine @Inject constructor(
    private val assetLoader: AssetLoader,
) {
    companion object {
        /** D-14: maximum total sprite draw calls per frame across all faces. */
        const val MAX_DRAWS_PER_FRAME = 20
    }

    private val activeFilter = AtomicReference<FilterDefinition?>(null)

    /**
     * Per-face animation state, keyed by [SmoothedFace.trackingId] (D-13).
     * Exposed as `internal` for [BehaviorStateMapTest] visibility — @VisibleForTesting.
     * Clear on [setFilter]; entries removed by [onFaceLost] when BboxIouTracker drops a face.
     */
    internal val perFaceState = ConcurrentHashMap<Int, BehaviorState>()

    private var lastFrameAdvanceTsNanos: Long = 0L
    private val spritePaint = Paint(Paint.ANTI_ALIAS_FLAG)

    /**
     * Set the active filter. Pass null to disable filter rendering.
     * Clears all per-face state so the new filter always starts fresh (REN-07 / D-13).
     */
    fun setFilter(definition: FilterDefinition?) {
        activeFilter.set(definition)
        perFaceState.clear()
        lastFrameAdvanceTsNanos = 0L
    }

    /**
     * Notify engine that the face with [trackingId] has been lost by the tracker (D-23).
     * Removes the corresponding [BehaviorState] entry so stale state doesn't accumulate.
     * Called by FaceDetectorClient when BboxIouTracker.assign() returns removedIds.
     */
    fun onFaceLost(trackingId: Int) {
        perFaceState.remove(trackingId)
    }

    /**
     * Phase 3 single-face overload — retained for backward compatibility.
     * Delegates to the Phase 4 list overload.
     */
    fun onDraw(canvas: Canvas, frame: Frame, face: SmoothedFace?) =
        onDraw(canvas, frame, listOfNotNull(face))

    /**
     * Phase 4 list overload — D-22 multi-face + D-14 soft cap.
     *
     * Sort faces by bbox area descending: index 0 = primary, index 1 = secondary (cap at 2).
     * Primary gets full contour anchor; secondary gets bbox-centre fallback at 40% height.
     */
    fun onDraw(canvas: Canvas, frame: Frame, faces: List<SmoothedFace>) {
        val filter = activeFilter.get() ?: return
        if (faces.isEmpty()) return

        // CAM-07: apply sensor→buffer transform so sprite is correctly oriented/scaled.
        canvas.setMatrix(frame.sensorToBufferTransform ?: Matrix())

        // D-22: sort by bbox area descending; primary = index 0, secondary = index 1.
        val sorted = faces.sortedByDescending {
            it.boundingBox.width().toLong() * it.boundingBox.height()
        }

        // D-13: eagerly seed perFaceState for every tracked face so state persists even
        // when the bitmap is not yet cached (preload incomplete). This ensures Crawl progress
        // and Swarm positions accumulate correctly once assets are ready.
        sorted.take(2).forEach { face ->
            perFaceState.getOrPut(face.trackingId) { createBehaviorState(filter) }
        }

        val tsNanos = frame.timestampNanos
        val frameDurationNanos = filter.frameDurationMs * 1_000_000L
        val frameIdx = if (frameDurationNanos > 0L && filter.frameCount > 0) {
            ((tsNanos / frameDurationNanos) % filter.frameCount).toInt()
        } else {
            0
        }

        // D-11 no-ghost: skip draw if bitmap not yet cached (preload incomplete).
        val bitmap = assetLoader.get(filter.id, frameIdx) ?: return

        val dtMs = if (lastFrameAdvanceTsNanos == 0L) 0L
                   else (tsNanos - lastFrameAdvanceTsNanos) / 1_000_000L

        val previewWidth  = frame.size.width.toFloat()
        val previewHeight = frame.size.height.toFloat()

        var totalDraws = 0

        val primary = sorted.getOrNull(0)
        val secondary = sorted.getOrNull(1)

        if (primary != null) {
            totalDraws += drawFace(
                canvas, frame, primary, filter, bitmap,
                previewWidth, previewHeight, tsNanos, dtMs,
                isPrimary = true,
                budgetRemaining = MAX_DRAWS_PER_FRAME - totalDraws,
            )
        }
        if (secondary != null && totalDraws < MAX_DRAWS_PER_FRAME) {
            totalDraws += drawFace(
                canvas, frame, secondary, filter, bitmap,
                previewWidth, previewHeight, tsNanos, dtMs,
                isPrimary = false,
                budgetRemaining = MAX_DRAWS_PER_FRAME - totalDraws,
            )
        }

        lastFrameAdvanceTsNanos = tsNanos
        Timber.tag("FilterEngine").v("filter=%s frame=%d faces=%d draws=%d",
            filter.id, frameIdx, faces.size, totalDraws)
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private fun drawFace(
        canvas: Canvas,
        @Suppress("UNUSED_PARAMETER") frame: Frame,
        face: SmoothedFace,
        filter: FilterDefinition,
        bitmap: Bitmap,
        previewWidth: Float,
        previewHeight: Float,
        tsNanos: Long,
        dtMs: Long,
        isPrimary: Boolean,
        budgetRemaining: Int,
    ): Int {
        if (budgetRemaining <= 0) return 0

        // D-22: primary uses full FaceLandmarkMapper ladder; secondary uses bbox-centre @40% height.
        val anchor: PointF = if (isPrimary) {
            FaceLandmarkMapper.anchorPoint(face, filter.anchorType) ?: return 0
        } else {
            val bbox = face.boundingBox
            PointF(
                bbox.left + bbox.width() * 0.5f,
                bbox.top  + bbox.height() * 0.4f,
            )
        }

        // getOrPut is safe under single-thread render (renderExecutor D-18).
        val state = perFaceState.getOrPut(face.trackingId) { createBehaviorState(filter) }

        filter.behavior.tick(state, face, anchor, previewWidth, previewHeight, tsNanos, dtMs)

        val spriteW = face.boundingBox.width() * filter.scaleFactor
        val spriteH = spriteW * bitmap.height.toFloat() / bitmap.width.toFloat()

        // D-14 soft cap: count draws per behavior, clamp to budgetRemaining.
        return when (val s = state) {
            is BehaviorState.Static -> {
                drawBitmapCentered(canvas, bitmap, s.pos, spriteW, spriteH)
                1
            }
            is BehaviorState.Crawl -> {
                val contour = face.contours[FaceContour.FACE]
                val pos = if (!contour.isNullOrEmpty()) {
                    BugBehavior.crawlPosition(contour, s.progress)
                } else {
                    anchor  // bbox-centre fallback if FACE contour missing
                }
                drawBitmapCentered(canvas, bitmap, pos, spriteW, spriteH)
                1
            }
            is BehaviorState.Swarm -> {
                val cap = s.instances.size.coerceAtMost(budgetRemaining)
                for (i in 0 until cap) {
                    drawBitmapCentered(canvas, bitmap, s.instances[i].position, spriteW, spriteH)
                }
                cap
            }
            is BehaviorState.Fall -> {
                val cap = s.instances.size.coerceAtMost(budgetRemaining)
                for (i in 0 until cap) {
                    drawBitmapCentered(canvas, bitmap, s.instances[i].position, spriteW, spriteH)
                }
                cap
            }
        }
    }

    private fun drawBitmapCentered(canvas: Canvas, bitmap: Bitmap, pos: PointF, w: Float, h: Float) {
        val left = pos.x - w / 2f
        val top  = pos.y - h / 2f
        canvas.save()
        canvas.translate(left, top)
        canvas.drawBitmap(bitmap, 0f, 0f, spritePaint)
        canvas.restore()
    }

    /**
     * Create a fresh [BehaviorState] matching [filter.behavior], pre-populated with any
     * per-filter tuning from [FilterDefinition.behaviorConfig] (D-29).
     */
    private fun createBehaviorState(filter: FilterDefinition): BehaviorState = when (filter.behavior) {
        BugBehavior.Crawl -> BehaviorState.Crawl(
            progress = 0f,
            direction = (filter.behaviorConfig as? BehaviorConfig.Crawl)?.direction
                ?: CrawlDirection.CW,
        )
        BugBehavior.Swarm -> BehaviorState.Swarm(
            targetCount = (filter.behaviorConfig as? BehaviorConfig.Swarm)?.instanceCount
                ?: BugBehavior.SWARM_INSTANCE_COUNT_DEFAULT,
        )
        BugBehavior.Fall  -> (filter.behaviorConfig as? BehaviorConfig.Fall).let { cfg ->
            BehaviorState.Fall(
                maxInstances       = cfg?.maxInstances       ?: BugBehavior.FALL_MAX_INSTANCES_DEFAULT,
                spawnIntervalMinMs = cfg?.spawnIntervalMinMs ?: BugBehavior.FALL_SPAWN_INTERVAL_MIN_MS,
                spawnIntervalMaxMs = cfg?.spawnIntervalMaxMs ?: BugBehavior.FALL_SPAWN_INTERVAL_MAX_MS,
                gravityFactor      = cfg?.gravityFactor      ?: BugBehavior.FALL_GRAVITY_FACTOR_DEFAULT,
            )
        }
        else -> BehaviorState.Static()
    }
}
