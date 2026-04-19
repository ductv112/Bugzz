package com.bugzz.filter.camera.detector

import com.google.mlkit.vision.face.FaceDetectorOptions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Nyquist unit test for [FaceDetectorClient.buildOptions] (CAM-04 + D-15).
 *
 * Pins the exact ML Kit FaceDetectorOptions contract the face detection
 * pipeline must honour. Any drift from D-15 (e.g. enabling landmark mode
 * alongside contour mode, or raising minFaceSize) is caught here — see
 * PITFALLS.md #3 (contour + landmarks + classification simultaneously
 * degrades accuracy).
 *
 * **Verification strategy note (Plan 02-03 discovery):** `FaceDetectorOptions` is
 * R8-minified in the published ML Kit AAR — its public field accessors are obfuscated
 * to `zza()`..`zzg()` with no stable Kotlin-property-style names like
 * `opts.performanceMode`. Plan 01's original per-field assertion pattern did not
 * compile. Workaround: build an "expected" options instance with the exact D-15
 * values, compare via `equals()` (ML Kit overrides it — all 6 fields are included),
 * and fall back to `toString()` substring checks for per-field diagnostic messages
 * on failure. Contract coverage is identical; failure diagnostics are downgraded
 * from "which field drifted" to "drift present, inspect toString diff".
 *
 * Intentional Nyquist RED: [FaceDetectorClient] does not yet exist;
 * Plan 02-03 lands the companion `buildOptions()` factory.
 *
 * Updated 2026-04-19 per GAP-02-A: assertion flipped from trackingEnabled=true to
 * trackingEnabled=false — see 02-ADR-01-no-ml-kit-tracking-with-contour.md.
 */
class FaceDetectorOptionsTest {

    @Test
    fun options_configured_per_D15() {
        val opts = FaceDetectorClient.buildOptions()
        val expected = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
            .setMinFaceSize(0.15f)
            .build()

        // Per-field diagnostic via toString() substring — ML Kit's MoreObjects-style
        // toString emits `key=value` pairs. These assertions surface which D-15 field
        // drifted before the composite equals check fails, improving debuggability.
        val actualStr = opts.toString()
        assertTrue(
            "performanceMode must be PERFORMANCE_MODE_FAST (D-15); toString=$actualStr",
            actualStr.contains("performanceMode=${FaceDetectorOptions.PERFORMANCE_MODE_FAST}"),
        )
        assertTrue(
            "contourMode must be CONTOUR_MODE_ALL (D-15); toString=$actualStr",
            actualStr.contains("contourMode=${FaceDetectorOptions.CONTOUR_MODE_ALL}"),
        )
        assertTrue(
            "landmarkMode must be NONE (D-15 — contour + landmarks together degrades); toString=$actualStr",
            actualStr.contains("landmarkMode=${FaceDetectorOptions.LANDMARK_MODE_NONE}"),
        )
        assertTrue(
            "classificationMode must be NONE (D-15); toString=$actualStr",
            actualStr.contains("classificationMode=${FaceDetectorOptions.CLASSIFICATION_MODE_NONE}"),
        )
        assertTrue(
            "minFaceSize must be 0.15 (D-15); toString=$actualStr",
            actualStr.contains("minFaceSize=0.15"),
        )
        assertTrue(
            "tracking must be DISABLED — ML Kit silently ignores .enableTracking() under " +
                "CONTOUR_MODE_ALL (GAP-02-A / ADR-01); toString=$actualStr",
            actualStr.contains("trackingEnabled=false"),
        )

        // Composite equals as the definitive D-15 gate — ML Kit FaceDetectorOptions
        // overrides equals() across all 6 configuration fields. Any drift (including
        // ones the toString format might hide) fails here.
        assertEquals(
            "options must equal D-15 expected (performanceMode=FAST + contourMode=ALL + " +
                "landmarkMode=NONE + classificationMode=NONE + tracking=false + minFaceSize=0.15) " +
                "— per GAP-02-A / ADR-01, tracking is intentionally NOT enabled under contour mode",
            expected,
            opts,
        )
    }
}
