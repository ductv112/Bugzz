package com.bugzz.filter.camera.render

import android.graphics.Color
import android.graphics.Matrix
import android.graphics.PorterDuff
import android.graphics.Rect
import android.graphics.RectF
import android.os.Handler
import android.os.HandlerThread
import androidx.camera.core.CameraEffect
import androidx.camera.effects.OverlayEffect
import com.bugzz.filter.camera.BuildConfig
import com.bugzz.filter.camera.detector.FaceDetectorClient
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Constructs the single OverlayEffect instance reused across lens flips (D-25).
 *
 * The effect:
 *   - Targets PREVIEW + VIDEO_CAPTURE + IMAGE_CAPTURE (CAM-06; bakes overlay into all 3 streams)
 *   - Draws on a dedicated HandlerThread ("BugzzRenderThread") to avoid main-thread contention
 *     during Compose recomposition (research §Open Questions #2)
 *   - Applies `canvas.setMatrix(frame.sensorToBufferTransform)` BEFORE every draw call
 *     (CAM-07 / PITFALLS #5 — setMatrix must precede drawRect; pairs with COORDINATE_SYSTEM_SENSOR
 *     configured on the MlKitAnalyzer in FaceDetectorClient)
 *
 * Companion constants are `internal` so OverlayEffectBuilderTest (Plan 01) can assert them
 * without instantiating OverlayEffect itself (OverlayEffect requires an Android Handler and
 * is not unit-testable in a plain JVM harness).
 */
@Singleton
class OverlayEffectBuilder @Inject constructor(
    private val faceDetector: FaceDetectorClient,
    private val renderer: DebugOverlayRenderer,
    private val filterEngine: FilterEngine,
) {
    private lateinit var renderThread: HandlerThread
    private lateinit var renderHandler: Handler

    /** Construct ONCE per app session. D-25: reuse across lens flips. */
    fun build(): OverlayEffect {
        renderThread = HandlerThread("BugzzRenderThread").apply { start() }
        renderHandler = Handler(renderThread.looper)

        val effect = OverlayEffect(
            /* targets       = */ TARGETS,
            /* queueDepth    = */ QUEUE_DEPTH,
            /* handler       = */ renderHandler,
            /* errorListener = */ { throwable -> Timber.e(throwable, "OverlayEffect internal error") },
        )

        effect.setOnDrawListener { frame ->
            val canvas = frame.overlayCanvas
            // GAP-02-B root cause fix: OverlayEffect does NOT clear the overlay canvas
            // between frames — prior-frame drawings persist and accumulate across frames
            // (especially visible across lens flips where bbox jumps position, producing
            // nested "ghost" rectangles). Clear to full transparency BEFORE setMatrix so
            // the clear covers the entire buffer regardless of the sensor transform.
            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
            // CRITICAL: match COORDINATE_SYSTEM_SENSOR from MlKitAnalyzer (D-17 / CAM-07).
            // setMatrix MUST precede any drawRect / drawCircle (PITFALLS #5).
            canvas.setMatrix(frame.sensorToBufferTransform)
            val snapshot = faceDetector.latestSnapshot.get()

            // GAP-02-B diagnostic — DEBUG-only, rate-limited to first face's bbox + matrix.
            // Retained after gap-closure as observability aid (T-02-02: aggregate-only, no landmarks).
            if (BuildConfig.DEBUG && snapshot.faces.isNotEmpty()) {
                logDiagnostic(frame.sensorToBufferTransform, snapshot.faces.first().boundingBox)
            }

            // D-27 draw order (Claude's Discretion): FilterEngine draws the bug sprite FIRST so
            // DebugOverlayRenderer's diagnostic overlay (bbox, contours) appears on top during
            // development. In release builds the debug renderer is a no-op, so draw order only
            // matters for correctness — sprites under the diagnostic grid is the right layering.
            filterEngine.onDraw(canvas, frame, snapshot.faces.firstOrNull())
            renderer.draw(canvas, snapshot, frame.timestampNanos)
            true  // = "I drew something; present this frame"
        }

        return effect
    }

    /** Release the render HandlerThread — callable from ViewModel onCleared(). */
    fun release() {
        if (::renderThread.isInitialized) renderThread.quitSafely()
    }

    private val diagValues = FloatArray(9)
    private var diagFrameCounter = 0

    /**
     * GAP-02-B diagnostic — logs every 60th frame (~once per 2s at 30fps) to avoid
     * log spam. DEBUG-only; strips in release. Aggregate coords + matrix values only
     * (no per-landmark data) — T-02-02 biometric-data-logging mitigation.
     */
    private fun logDiagnostic(transform: Matrix, bbox: Rect) {
        if (diagFrameCounter++ % 60 != 0) return
        transform.getValues(diagValues)
        val scaleX = diagValues[Matrix.MSCALE_X]
        val scaleY = diagValues[Matrix.MSCALE_Y]
        val transX = diagValues[Matrix.MTRANS_X]
        val transY = diagValues[Matrix.MTRANS_Y]
        // Post-matrix bbox — simulate what drawRect will produce after setMatrix
        val postBbox = RectF(bbox)
        transform.mapRect(postBbox)
        Timber.tag("OverlayDiag").v(
            "scaleX=%.3f scaleY=%.3f trans=%.1f,%.1f preBB=%d,%d-%dx%d postBB=%.1f,%.1f-%.1fx%.1f",
            scaleX, scaleY, transX, transY,
            bbox.left, bbox.top, bbox.width(), bbox.height(),
            postBbox.left, postBbox.top, postBbox.width(), postBbox.height(),
        )
    }

    companion object {
        /** Exposed for OverlayEffectBuilderTest (Plan 01) — asserts the exact target mask (CAM-06). */
        internal val TARGETS: Int =
            CameraEffect.PREVIEW or CameraEffect.VIDEO_CAPTURE or CameraEffect.IMAGE_CAPTURE

        /** Exposed for OverlayEffectBuilderTest (Plan 01) — asserts queueDepth == 0 (draw every frame). */
        internal const val QUEUE_DEPTH: Int = 0
    }
}
