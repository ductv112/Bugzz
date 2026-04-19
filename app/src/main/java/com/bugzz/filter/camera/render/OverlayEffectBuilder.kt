package com.bugzz.filter.camera.render

import android.os.Handler
import android.os.HandlerThread
import androidx.camera.core.CameraEffect
import androidx.camera.effects.OverlayEffect
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
            // CRITICAL: match COORDINATE_SYSTEM_SENSOR from MlKitAnalyzer (D-17 / CAM-07).
            // setMatrix MUST precede any drawRect / drawCircle (PITFALLS #5).
            canvas.setMatrix(frame.sensorToBufferTransform)
            val snapshot = faceDetector.latestSnapshot.get()
            renderer.draw(canvas, snapshot, frame.timestampNanos)
            true  // = "I drew something; present this frame"
        }

        return effect
    }

    /** Release the render HandlerThread — callable from ViewModel onCleared(). */
    fun release() {
        if (::renderThread.isInitialized) renderThread.quitSafely()
    }

    companion object {
        /** Exposed for OverlayEffectBuilderTest (Plan 01) — asserts the exact target mask (CAM-06). */
        internal val TARGETS: Int =
            CameraEffect.PREVIEW or CameraEffect.VIDEO_CAPTURE or CameraEffect.IMAGE_CAPTURE

        /** Exposed for OverlayEffectBuilderTest (Plan 01) — asserts queueDepth == 0 (draw every frame). */
        internal const val QUEUE_DEPTH: Int = 0
    }
}
