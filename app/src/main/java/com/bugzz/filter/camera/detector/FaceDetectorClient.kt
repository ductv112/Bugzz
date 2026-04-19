package com.bugzz.filter.camera.detector

import android.graphics.PointF
import androidx.camera.core.ImageAnalysis.COORDINATE_SYSTEM_SENSOR
import androidx.camera.mlkit.vision.MlKitAnalyzer
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceContour
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import timber.log.Timber
import java.util.concurrent.Executor
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * ML Kit face detector + 1€ smoothing + AtomicReference<FaceSnapshot> producer.
 *
 * Contract (D-18/19/20/22):
 *   - Single-writer: the MlKitAnalyzer callback (running on cameraExecutor) writes latestSnapshot.
 *   - Multi-reader: DebugOverlayRenderer / FilterEngine reads latestSnapshot on renderExecutor
 *     each draw.
 *   - Lock-free: AtomicReference provides visibility; reader may see 1-2-frame stale snapshot.
 *
 * Runtime face identity comes from [BboxIouTracker] (Plan 03-02). ML Kit's own trackingId is
 * always null under CONTOUR_MODE_ALL — see ADR-01.
 *
 * @see .planning/phases/02-camera-preview-face-detection-coordinate-validation/02-CONTEXT.md D-15..D-22
 * @see .planning/phases/02-camera-preview-face-detection-coordinate-validation/02-ADR-01-no-ml-kit-tracking-with-contour.md
 */
@Singleton
class FaceDetectorClient @Inject constructor(
    @Named("cameraExecutor") private val cameraExecutor: Executor,
    private val tracker: BboxIouTracker,   // ADR-01 #3 — Phase 3 bbox-IoU tracker
) {
    private val options = buildOptions()
    private val detector: FaceDetector = FaceDetection.getClient(options)
    private val smoother = LandmarkSmoother(minCutoff = 1.0, beta = 0.007, dCutoff = 1.0)

    /** Latest face snapshot — single-writer (cameraExecutor), multi-reader (renderExecutor). */
    val latestSnapshot: AtomicReference<FaceSnapshot> = AtomicReference(FaceSnapshot.EMPTY)

    /** Construct the MlKitAnalyzer — attach with ImageAnalysis.setAnalyzer(). */
    fun createAnalyzer(): MlKitAnalyzer =
        MlKitAnalyzer(
            /* detectors = */ listOf(detector),
            /* targetCoordinateSystem = */ COORDINATE_SYSTEM_SENSOR,
            /* executor = */ cameraExecutor,
        ) { result ->
            val faces: List<Face> = result.getValue(detector) ?: emptyList()
            val tNanos = System.nanoTime()

            // ADR-01 #3 — tracker assigns stable IDs (ML Kit trackingId always null under
            // CONTOUR_MODE_ALL per ADR-01). Assignment MUST precede smoothing so each 1€
            // filter's state key matches the face identity we want to persist across frames.
            val trackerResult = tracker.assign(faces)

            // ADR-01 #2 — drop smoother state for faces the tracker just lost.
            for (id in trackerResult.removedIds) smoother.onFaceLost(id)

            val smoothedFaces = trackerResult.tracked.map { tf -> smoothFace(tf, tNanos) }
            latestSnapshot.set(FaceSnapshot(smoothedFaces, tNanos))

            // T-02-06 / T-03-05 — aggregate-only Timber; never land per-landmark coord lists.
            if (trackerResult.tracked.isNotEmpty()) {
                for (tf in trackerResult.tracked) {
                    Timber.tag("FaceTracker").v(
                        "t=%d id=%d bb=%d,%d contours=%d",
                        tNanos,
                        tf.id,
                        tf.face.boundingBox.centerX(),
                        tf.face.boundingBox.centerY(),
                        tf.face.allContours.size,
                    )
                }
            }
        }

    /** Release detector on teardown. */
    fun close() = detector.close()

    /** D-25 — clear 1€ state on lens flip (PITFALLS #6 — stale trackingId state leak). */
    fun onLensFlipped() = smoother.clear()

    private fun smoothFace(tf: BboxIouTracker.TrackedFace, tNanos: Long): SmoothedFace {
        val id = tf.id   // non-negative tracker-assigned ID (ADR-01 #3)
        val face = tf.face
        val smoothedContours: Map<Int, List<PointF>> = buildMap {
            for (type in SMOOTHED_CONTOUR_TYPES) {
                val contour = face.getContour(type) ?: continue
                val pts = contour.points.map { p ->
                    val (sx, sy) = smoother.smoothPoint(id, "c$type", p.x, p.y, tNanos)
                    PointF(sx, sy)
                }
                put(type, pts)
            }
        }
        return SmoothedFace(
            trackingId = id,
            boundingBox = face.boundingBox,
            contours = smoothedContours,
            landmarks = face.allLandmarks.associate {
                it.landmarkType to PointF(it.position.x, it.position.y)
            },
        )
    }

    companion object {
        private val SMOOTHED_CONTOUR_TYPES: List<Int> = listOf(
            FaceContour.FACE,
            FaceContour.LEFT_EYEBROW_TOP,      // Phase 3 — FOREHEAD anchor
            FaceContour.RIGHT_EYEBROW_TOP,     // Phase 3 — FOREHEAD anchor
            FaceContour.NOSE_BRIDGE,
            FaceContour.NOSE_BOTTOM,
            FaceContour.LEFT_EYE,
            FaceContour.RIGHT_EYE,
            FaceContour.LEFT_CHEEK,
            FaceContour.RIGHT_CHEEK,
            FaceContour.UPPER_LIP_TOP,
            FaceContour.LOWER_LIP_BOTTOM,
        )

        /**
         * Exposed for FaceDetectorOptionsTest (Plan 01) — asserts D-15 values exactly. Do NOT
         * inline into `options` field initializer; the test calls this static method directly.
         *
         * **Tracking NOT enabled.** Google ML Kit silently ignores `.enableTracking()` at runtime
         * when `CONTOUR_MODE_ALL` is active — `face.trackingId` is always null. This was verified
         * on Xiaomi 13T / HyperOS (GAP-02-A, 459/459 frames, 2026-04-19). Face identity across
         * frames is provided by [BboxIouTracker] (Plan 03-02) — see
         * `02-ADR-01-no-ml-kit-tracking-with-contour.md`. Do not re-add `.enableTracking()` here
         * without also switching away from `CONTOUR_MODE_ALL`; the two are mutually exclusive.
         */
        fun buildOptions(): FaceDetectorOptions =
            FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
                .setMinFaceSize(0.15f)
                .build()
    }
}
