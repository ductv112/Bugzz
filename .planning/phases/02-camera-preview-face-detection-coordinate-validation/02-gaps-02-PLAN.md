---
phase: 02-camera-preview-face-detection-coordinate-validation
plan: gaps-02
type: execute
wave: 2
depends_on: [02-gaps-01]
files_modified:
  - app/src/main/java/com/bugzz/filter/camera/render/DebugOverlayRenderer.kt
  - app/src/main/java/com/bugzz/filter/camera/render/OverlayEffectBuilder.kt
  - app/src/test/java/com/bugzz/filter/camera/render/DebugOverlayRendererTest.kt
  - .planning/phases/02-camera-preview-face-detection-coordinate-validation/02-CONTEXT.md
  - .planning/phases/02-camera-preview-face-detection-coordinate-validation/02-VERIFICATION.md
  - .planning/phases/02-camera-preview-face-detection-coordinate-validation/02-HANDOFF.md
autonomous: false
gap_closure: true
requirements: [CAM-07]
user_setup:
  - service: xiaomi-13t
    why: "Physical device verification of rendered overlay alignment across 4 rotations × 2 lenses — cannot be simulated on emulator (no real CameraX sensor, no OverlayEffect frame events)"
    env_vars: []
    dashboard_config:
      - task: "Xiaomi 13T plugged in over ADB with Developer Options → USB Debugging + 'Install via USB' enabled. Same device as Phase 1 FND-08 and Phase 2 02-06 sign-off."
        location: "Settings → Additional settings → Developer options"
      - task: "User in front of device camera for each verification step; physically rotates device through portrait → landscape-left → reverse-portrait → landscape-right on both front and back lens"
        location: "device held in user's hands during runbook execution"
must_haves:
  truths:
    - "Diagnostic Timber logs captured from a real Xiaomi 13T face-detection session reveal the actual value of `frame.sensorToBufferTransform` scale factor (answers H2)"
    - "Diagnostic logs reveal pre-matrix vs post-matrix boundingBox coord ranges (answers H3)"
    - "Root cause (H1 density / H2 matrix scale / H3 coord space) is documented in-line in 02-VERIFICATION.md GAP-02-B section + confirmed by screenshot evidence"
    - "DebugOverlayRenderer draws a visually-thin red stroked rect around face (stroke width ≤ 8 device pixels regardless of matrix scale) + ≤ 15 small orange dots at contour-type centroids (one per populated FaceContour type), not ~97 per-point dots"
    - "DebugOverlayRenderer compensates for matrix scale: `Paint.strokeWidth` and `drawCircle(..., radius, ...)` values are divided by the extracted `MSCALE_X` of the current canvas matrix so on-device stroke/radius render at the intended device-pixel thickness"
    - "Diagnostic Timber logs added in Wave A are `BuildConfig.DEBUG`-gated and survive in the final code as a debug-only observability aid (rationale: T-02-02 — biometric data not logged in release; debug-only logs are acceptable per existing D-02 gate)"
    - "After install + visual verification: thin red stroked rect wraps the face. Face clearly visible behind the overlay. ~15 or fewer small orange dots render at contour centroids."
    - "HANDOFF Step 9 (4 rotations × 2 lenses) executed on Xiaomi 13T — red rect stays aligned to face across all 8 combinations (or OEM quirk documented + deferred to Phase 7)"
    - "HANDOFF Step 8 re-signed-off as PASS"
    - "02-VERIFICATION.md GAP-02-B status flipped from `failed` to `closed` with evidence links to new screenshots"
  artifacts:
    - path: "app/src/main/java/com/bugzz/filter/camera/render/DebugOverlayRenderer.kt"
      provides: "Matrix-scale-compensated stroke widths + per-contour-type centroid dot reduction"
      contains: "MSCALE_X"
    - path: "app/src/main/java/com/bugzz/filter/camera/render/OverlayEffectBuilder.kt"
      provides: "Debug-only diagnostic log emitting sensorToBufferTransform values + face bbox pre/post transform (Wave A; retained in final code gated by BuildConfig.DEBUG)"
      contains: "sensorToBufferTransform"
    - path: "app/src/test/java/com/bugzz/filter/camera/render/DebugOverlayRendererTest.kt"
      provides: "Unit test pinning: stroke width compensation formula returns correct device-pixel value for representative matrix scales"
      min_lines: 30
    - path: ".planning/phases/02-camera-preview-face-detection-coordinate-validation/02-HANDOFF.md"
      provides: "Re-verification of HANDOFF Steps 8-9 logged in Actual Sign-Off section; CAM-07 flipped to PASS"
      contains: "CAM-07.*PASS"
    - path: ".planning/phases/02-camera-preview-face-detection-coordinate-validation/02-VERIFICATION.md"
      provides: "GAP-02-B status flipped to `closed` with H-resolution"
      contains: "GAP-02-B"
  key_links:
    - from: "OverlayEffectBuilder.onDrawListener (diagnostic log)"
      to: "DebugOverlayRenderer.draw (scale-extracted paint widths)"
      via: "canvas matrix is set before renderer is called; renderer reads matrix via canvas.matrix + getValues(float[9])[Matrix.MSCALE_X]"
      pattern: "getValues.*MSCALE_X|MSCALE_X.*getValues"
    - from: "DebugOverlayRendererTest (Nyquist scale-compensation contract)"
      to: "DebugOverlayRenderer.computeScaledStroke / per-frame stroke width"
      via: "unit test verifies (strokeWidthDp / MSCALE_X) → expected device pixel value"
      pattern: "strokeWidth.*MSCALE_X|MSCALE_X.*strokeWidth"
    - from: "HANDOFF Step 9 re-verification"
      to: "DebugOverlayRenderer + OverlayEffectBuilder device runtime"
      via: "4 rotations × 2 lenses = 8 screenshots captured via `adb shell screencap`"
      pattern: ".tmp-shots/gap-02-b-"
---

<objective>
Close GAP-02-B (CAM-07 DebugOverlayRenderer over-draws) by first diagnosing the root cause with targeted Timber logs on real hardware, then rewriting the renderer's draw logic to compensate for the matrix scale factor + reduce dot density from ~97 points/face to ≤ 15 centroids/face, then re-verifying on Xiaomi 13T across 4 rotations × 2 lenses.

Purpose: CAM-07 is Phase 2's architectural load-bearing visual proof — the entire `OverlayEffect + MlKitAnalyzer(COORDINATE_SYSTEM_SENSOR) + getSensorToBufferTransform()` pairing must be visually provable before Phase 3 draws production sprites against this same broken reference. The current renderer saturates the preview with red, masking whether the coordinate pipeline is even correct. Three hypotheses (H1 density / H2 matrix scale / H3 coord space) must be isolated with real device data before coding the fix. Fix must preserve contour-mode-ingestion validation (contract from D-01) while making the overlay tractable.

Output: Renderer code with matrix-scale-compensated stroke widths + centroid-per-contour-type dot reduction + debug-only diagnostic logs, a new unit test pinning the compensation formula, updated HANDOFF sign-off, and VERIFICATION gap status closed with screenshot evidence.
</objective>

<execution_context>
@$HOME/.claude/get-shit-done/workflows/execute-plan.md
@$HOME/.claude/get-shit-done/templates/summary.md
</execution_context>

<context>
@.planning/PROJECT.md
@.planning/STATE.md
@.planning/phases/02-camera-preview-face-detection-coordinate-validation/02-VERIFICATION.md
@.planning/phases/02-camera-preview-face-detection-coordinate-validation/02-CONTEXT.md
@.planning/phases/02-camera-preview-face-detection-coordinate-validation/02-HANDOFF.md
@.planning/phases/02-camera-preview-face-detection-coordinate-validation/02-06-SUMMARY.md
@app/src/main/java/com/bugzz/filter/camera/render/DebugOverlayRenderer.kt
@app/src/main/java/com/bugzz/filter/camera/render/OverlayEffectBuilder.kt
@app/src/main/java/com/bugzz/filter/camera/detector/FaceSnapshot.kt
@app/src/test/java/com/bugzz/filter/camera/render/OverlayEffectBuilderTest.kt

<interfaces>
<!-- Key types and contracts the executor needs. Extracted from current codebase. -->

From app/src/main/java/com/bugzz/filter/camera/render/DebugOverlayRenderer.kt (current):

```kotlin
@Singleton
class DebugOverlayRenderer @Inject constructor() {
    private val boxPaint = Paint().apply {
        color = Color.RED
        style = Paint.Style.STROKE
        strokeWidth = 4f         // <-- LITERAL; gets scaled by canvas.matrix post-setMatrix()
        isAntiAlias = true
    }
    private val dotPaint = Paint().apply {
        color = Color.argb(255, 255, 80, 0)
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    fun draw(canvas: Canvas, snapshot: FaceSnapshot, timestampNanos: Long) {
        if (!BuildConfig.DEBUG) return
        if (snapshot.faces.isEmpty()) return
        for (face in snapshot.faces) {
            canvas.drawRect(
                face.boundingBox.left.toFloat(),
                face.boundingBox.top.toFloat(),
                face.boundingBox.right.toFloat(),
                face.boundingBox.bottom.toFloat(),
                boxPaint,
            )
            face.contours.values.flatten().forEach { p ->   // <-- ~97 points per face
                canvas.drawCircle(p.x, p.y, 4f, dotPaint)
            }
            face.landmarks.values.forEach { p ->
                canvas.drawCircle(p.x, p.y, 5f, dotPaint)
            }
        }
    }
}
```

From app/src/main/java/com/bugzz/filter/camera/render/OverlayEffectBuilder.kt (current onDrawListener):

```kotlin
effect.setOnDrawListener { frame ->
    val canvas = frame.overlayCanvas
    canvas.setMatrix(frame.sensorToBufferTransform)      // <-- sets matrix
    val snapshot = faceDetector.latestSnapshot.get()
    renderer.draw(canvas, snapshot, frame.timestampNanos)
    true
}
```

From FaceSnapshot (already exists — data class SmoothedFace has `contours: Map<Int, List<PointF>>` and `boundingBox: Rect`):

- `snapshot.faces: List<SmoothedFace>`
- `face.contours: Map<Int /* FaceContour constant: 1..15 */, List<PointF>>` — populated only for primary face (PITFALLS #13)
- `face.landmarks: Map<Int, PointF>` — populated for all faces (fallback)
- `face.boundingBox: Rect` — populated for all faces

From android.graphics.Matrix API:
- `Matrix.MSCALE_X = 0` (constant index into the 9-float matrix array)
- `Matrix.MSCALE_Y = 4`
- `canvas.matrix.getValues(FloatArray(9))` — extracts the 3×3 matrix as a float array
</interfaces>

<read_first>
1. Read `02-VERIFICATION.md §GAP-02-B` in full — it is the contract this plan closes. Note the three hypotheses H1/H2/H3.
2. Read `DebugOverlayRenderer.kt` in full (66 lines) — note line 33 `strokeWidth = 4f`, lines 58 + 62 `drawCircle(..., 4f|5f, ...)`, line 57 `face.contours.values.flatten().forEach` (the density source).
3. Read `OverlayEffectBuilder.kt` in full — note line 51 `canvas.setMatrix(frame.sensorToBufferTransform)` precedes the renderer.draw call.
4. Read `OverlayEffectBuilderTest.kt` in full — it already pins TARGETS + QUEUE_DEPTH; you are ADDING a sibling renderer test, not modifying this one.
5. Read `02-06-SUMMARY.md §Blockers GAP-02-B` — contains the H1/H2/H3 fix directions recommended by the verifier.
6. Read `02-HANDOFF.md §Actual Sign-Off Steps 8-9` — current failure state with screenshot references; you will append a new Gap-Closure Re-Verification section.
</read_first>

<tasks>

<task type="auto">
  <name>Task 1: Wave A — Add BuildConfig.DEBUG-gated diagnostic logging + device capture</name>
  <files>
    app/src/main/java/com/bugzz/filter/camera/render/OverlayEffectBuilder.kt
  </files>
  <action>
    Add diagnostic Timber logging to `OverlayEffectBuilder.setOnDrawListener` to capture per-frame matrix values + boundingBox coords pre/post transform. Logs MUST be `BuildConfig.DEBUG`-gated (T-02-02 — no biometric landmark data in release logs; boundingBox aggregate coords only).

    Modify the `build()` method's `setOnDrawListener` block. Current body is 5 lines (setMatrix → get snapshot → renderer.draw → return true). Replace with this structure (preserving the existing `canvas.setMatrix` call):

    ```kotlin
    effect.setOnDrawListener { frame ->
        val canvas = frame.overlayCanvas
        canvas.setMatrix(frame.sensorToBufferTransform)
        val snapshot = faceDetector.latestSnapshot.get()

        // GAP-02-B diagnostic — DEBUG-only, rate-limited to first face's bbox + matrix.
        // Retained after gap-closure as observability aid (T-02-02: aggregate-only, no landmarks).
        if (BuildConfig.DEBUG && snapshot.faces.isNotEmpty()) {
            logDiagnostic(frame.sensorToBufferTransform, snapshot.faces.first().boundingBox)
        }

        renderer.draw(canvas, snapshot, frame.timestampNanos)
        true
    }
    ```

    Add a private function `logDiagnostic(transform: Matrix, bbox: Rect)` to `OverlayEffectBuilder`:

    ```kotlin
    private val diagValues = FloatArray(9)
    private var diagFrameCounter = 0

    /**
     * GAP-02-B diagnostic — logs every 60th frame (~once per 2s at 30fps) to avoid
     * log spam. DEBUG-only; strips in release. Aggregate coords + matrix values only
     * (no per-landmark data) — T-02-02 biometric-data-logging mitigation.
     */
    private fun logDiagnostic(transform: android.graphics.Matrix, bbox: android.graphics.Rect) {
        if (diagFrameCounter++ % 60 != 0) return
        transform.getValues(diagValues)
        val scaleX = diagValues[android.graphics.Matrix.MSCALE_X]
        val scaleY = diagValues[android.graphics.Matrix.MSCALE_Y]
        val transX = diagValues[android.graphics.Matrix.MTRANS_X]
        val transY = diagValues[android.graphics.Matrix.MTRANS_Y]
        // Post-matrix bbox — simulate what drawRect will produce after setMatrix
        val postBbox = android.graphics.RectF(bbox)
        transform.mapRect(postBbox)
        timber.log.Timber.tag("OverlayDiag").v(
            "scaleX=%.3f scaleY=%.3f trans=%.1f,%.1f preBB=%d,%d-%dx%d postBB=%.1f,%.1f-%.1fx%.1f",
            scaleX, scaleY, transX, transY,
            bbox.left, bbox.top, bbox.width(), bbox.height(),
            postBbox.left, postBbox.top, postBbox.width(), postBbox.height(),
        )
    }
    ```

    Imports to add at top of file if not already present:
    - `android.graphics.Matrix`
    - `android.graphics.Rect`
    - `android.graphics.RectF`
    - `com.bugzz.filter.camera.BuildConfig`

    Do NOT modify `TARGETS`, `QUEUE_DEPTH`, `renderThread`, `renderHandler`, or any companion value. `OverlayEffectBuilderTest` stays GREEN because its assertions pin those companion constants only.

    After edit, run `./gradlew :app:assembleDebug :app:testDebugUnitTest` — expect clean build and 10/10 unit tests GREEN (adding the diagnostic log does not change any test contract).

    Install the APK to the Xiaomi 13T via adb:

    ```bash
    adb install -r app/build/outputs/apk/debug/app-debug.apk
    ```

    Then run this adb sequence to capture 20 seconds of logs while the user holds the device with face in frame (PAUSE HERE and instruct the user to do that — this is an interactive step, NOT a checkpoint task, because the command runs non-interactively from the PC):

    ```bash
    adb shell am start -n com.bugzz.filter.camera/.MainActivity
    # Navigate to Camera screen — reuse the input-tap coordinates from 02-06 runbook
    adb shell input tap 555 1800  # tap "Face Filter" from Home
    sleep 2
    adb logcat -c
    adb logcat -d OverlayDiag:V FaceTracker:V *:S > .tmp-shots/gap-02-b-diag.log &
    # (Wait 20 seconds for face-hold capture — user holds device still with face in frame)
    sleep 20
    adb logcat -d OverlayDiag:V FaceTracker:V *:S | tee .tmp-shots/gap-02-b-diag.log
    ```

    Capture at least 10 OverlayDiag lines. Expected log shape:

    ```
    OverlayDiag: scaleX=X.XXX scaleY=Y.YYY trans=TX,TY preBB=L,T-WxH postBB=L,T-WxH
    ```

    **Analyze the logs inline** in the task execution summary and answer:
    - What is `scaleX`? (Compare to 1.0 → if ≈ 1.0, H2 is ruled out. If > 1.0 meaningfully, H2 confirmed.)
    - Does `postBB` w × h visibly exceed the face's actual on-screen area? (→ H3 indicator)
    - preBB coord ranges — what are typical ML Kit contour mode boundingBox coordinate ranges for this sensor? (documents H3 baseline)

    Write the analysis as a new sub-section in `02-VERIFICATION.md §GAP-02-B` titled `**Diagnostic findings (2026-04-19, Wave A)**` inserting after the existing "Root cause hypotheses" block. Include the raw representative log line + resolved hypothesis verdict: H1 / H2 / H3 (or combination).

    Commit evidence to `.tmp-shots/gap-02-b-diag.log` (do NOT commit — it's gitignored by the `.tmp-shots/` convention; reference in SUMMARY only).
  </action>
  <verify>
    <automated>./gradlew :app:testDebugUnitTest --tests "*OverlayEffectBuilderTest*"</automated>

    Additional verify:
    - `grep -c "logDiagnostic\|OverlayDiag" app/src/main/java/com/bugzz/filter/camera/render/OverlayEffectBuilder.kt` MUST return `>= 2`.
    - `grep -c "BuildConfig.DEBUG" app/src/main/java/com/bugzz/filter/camera/render/OverlayEffectBuilder.kt` MUST return `>= 1`.
    - `grep -c "MSCALE_X" app/src/main/java/com/bugzz/filter/camera/render/OverlayEffectBuilder.kt` MUST return `>= 1`.
    - `test -f .tmp-shots/gap-02-b-diag.log && wc -l .tmp-shots/gap-02-b-diag.log` MUST show `>= 10 lines`.
    - `grep -c "Diagnostic findings" .planning/phases/02-camera-preview-face-detection-coordinate-validation/02-VERIFICATION.md` MUST return `>= 1`.
  </verify>
  <done>
    Diagnostic logs captured on device; H1/H2/H3 resolution documented in VERIFICATION.md. OverlayEffectBuilderTest still GREEN. Log output shows representative scaleX value that drives Task 2's fix decision.
  </done>
</task>

<task type="auto" tdd="true">
  <name>Task 2: Wave B — Rewrite DebugOverlayRenderer with matrix-scale compensation + centroid dot reduction</name>
  <files>
    app/src/main/java/com/bugzz/filter/camera/render/DebugOverlayRenderer.kt,
    app/src/test/java/com/bugzz/filter/camera/render/DebugOverlayRendererTest.kt,
    .planning/phases/02-camera-preview-face-detection-coordinate-validation/02-CONTEXT.md
  </files>
  <behavior>
    - Given `canvas.matrix.getValues(...)[MSCALE_X] = 2.0f` and a desired device-pixel stroke of 4f, `DebugOverlayRenderer` must render stroke at 4f/2.0f = 2f in sensor-space units → appears as 4 device pixels (unit test pins this formula).
    - Given a `SmoothedFace` with `contours.size = 15` (all 15 ML Kit FaceContour types populated, ~97 points total), `DebugOverlayRenderer` draws exactly 15 centroid dots (one per populated contour type) plus bounding box, not ~97 dots.
    - Given `snapshot.faces.isEmpty()`, renderer draws nothing (no regression on existing CAM-06 no-face-no-draw contract).
    - Given `BuildConfig.DEBUG == false`, renderer draws nothing (no regression on existing D-02 gate).
    - Unit test for the centroid formula: given a list of N PointF values, `centroidOf(points)` returns PointF(meanX, meanY). Empty list returns null (test pins both).
  </behavior>
  <action>
    **Step 1 — Create a companion helper object with two pure functions that are unit-testable** (this is the TDD seam; the functions themselves are the contract the test pins).

    Replace `app/src/main/java/com/bugzz/filter/camera/render/DebugOverlayRenderer.kt` with the following. Keep the `@Singleton @Inject constructor()` wrapper, the `BuildConfig.DEBUG` gate, and the D-02/T-02-02/D-23/PITFALLS #13 KDoc — preserve all existing header comments and augment with a GAP-02-B amendment note. The body of `draw()` is rewritten.

    ```kotlin
    package com.bugzz.filter.camera.render

    import android.graphics.Canvas
    import android.graphics.Color
    import android.graphics.Matrix
    import android.graphics.Paint
    import android.graphics.PointF
    import com.bugzz.filter.camera.BuildConfig
    import com.bugzz.filter.camera.detector.FaceSnapshot
    import javax.inject.Inject
    import javax.inject.Singleton

    /**
     * Debug-only overlay renderer (D-02 gate; replaced by FilterEngine in Phase 3).
     *
     * Invoked from OverlayEffect.onDrawListener on the renderExecutor / HandlerThread inside
     * OverlayEffectBuilder. Canvas has already been matrix-transformed by the caller
     * (`canvas.setMatrix(frame.sensorToBufferTransform)` — CAM-07 pairing); this function
     * draws in SENSOR coordinate space (where MlKitAnalyzer produced the face data via
     * COORDINATE_SYSTEM_SENSOR, D-17).
     *
     * D-23 / PITFALLS #13: multi-face behavior — ML Kit contour mode only populates `contours`
     * for the primary face; secondary faces have only `landmarks`. Draw both — secondary faces
     * gracefully degrade to boundingBox + subset of landmark dots.
     *
     * T-02-02 (information disclosure) mitigation: FIRST statement is `if (!BuildConfig.DEBUG) return`.
     * Release builds draw nothing, so saved MP4s / JPEGs contain no biometric overlay data.
     *
     * **Amended 2026-04-19 per GAP-02-B:**
     * - **Matrix scale compensation:** Paint strokeWidth + drawCircle radius are computed per
     *   draw by extracting the current canvas matrix's MSCALE_X. The renderer-wide intent is
     *   `DEVICE_PX_STROKE` device pixels regardless of the sensor-to-buffer transform scale.
     *   See `02-ADR-01`-parallel guidance in CONTEXT D-01 amendment.
     * - **Dot density reduction:** instead of ~97 per-point dots (`face.contours.values.flatten()`),
     *   draw one centroid dot per populated FaceContour TYPE (≤ 15 dots per primary face). This
     *   preserves contour-mode-ingestion visual validation (D-01 intent) while keeping the
     *   overlay tractable for CAM-07 pixel-alignment verification.
     */
    @Singleton
    class DebugOverlayRenderer @Inject constructor() {

        private val boxPaint = Paint().apply {
            color = Color.RED
            style = Paint.Style.STROKE
            isAntiAlias = true
            // strokeWidth set per-frame in draw() — depends on canvas matrix scale
        }
        private val dotPaint = Paint().apply {
            color = Color.argb(255, 255, 80, 0)  // orange-red
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        // Scratch buffers reused each draw — avoid allocation in the render hot path.
        private val matrixValues = FloatArray(9)

        /** Invoked from OverlayEffect onDrawListener on the renderHandler thread. */
        fun draw(canvas: Canvas, snapshot: FaceSnapshot, timestampNanos: Long) {
            if (!BuildConfig.DEBUG) return
            if (snapshot.faces.isEmpty()) return

            // Extract the current matrix scale so stroke + dot radii render at
            // DEVICE_PX intent regardless of sensorToBufferTransform scale factor.
            canvas.matrix.getValues(matrixValues)
            val scaleX = matrixValues[Matrix.MSCALE_X].coerceAtLeast(MIN_SAFE_SCALE)
            val strokeInSensorSpace = DEVICE_PX_STROKE / scaleX
            val dotRadiusInSensorSpace = DEVICE_PX_DOT_RADIUS / scaleX
            boxPaint.strokeWidth = strokeInSensorSpace

            for (face in snapshot.faces) {
                // Bounding box — ALWAYS draw (D-23: primary + secondary faces)
                canvas.drawRect(
                    face.boundingBox.left.toFloat(),
                    face.boundingBox.top.toFloat(),
                    face.boundingBox.right.toFloat(),
                    face.boundingBox.bottom.toFloat(),
                    boxPaint,
                )
                // Contour centroids — one dot per FaceContour TYPE (≤ 15 per primary face),
                // GAP-02-B dot-density reduction. Preserves D-01 contour-mode-ingestion
                // validation intent while keeping overlay tractable.
                face.contours.forEach { (_, points) ->
                    val c = centroidOf(points) ?: return@forEach
                    canvas.drawCircle(c.x, c.y, dotRadiusInSensorSpace, dotPaint)
                }
                // Fallback landmarks — populated for all faces (D-23 secondary-face path)
                face.landmarks.values.forEach { p ->
                    canvas.drawCircle(p.x, p.y, dotRadiusInSensorSpace, dotPaint)
                }
            }
        }

        companion object {
            /** Intended stroke width in device pixels (matrix-compensated per-frame). */
            internal const val DEVICE_PX_STROKE: Float = 4f
            /** Intended dot radius in device pixels (matrix-compensated per-frame). */
            internal const val DEVICE_PX_DOT_RADIUS: Float = 5f
            /** Floor to prevent division-by-zero / NaN when canvas matrix is singular. */
            internal const val MIN_SAFE_SCALE: Float = 0.0001f

            /**
             * Compute the centroid (mean x, mean y) of a list of points. Returns null if the
             * list is empty. Exposed internal for DebugOverlayRendererTest Nyquist pin.
             */
            internal fun centroidOf(points: List<PointF>): PointF? {
                if (points.isEmpty()) return null
                var sx = 0f
                var sy = 0f
                for (p in points) {
                    sx += p.x
                    sy += p.y
                }
                return PointF(sx / points.size, sy / points.size)
            }

            /**
             * Compute the stroke width (in sensor-space, ready to assign to Paint.strokeWidth)
             * required to render as [deviceStrokePx] device pixels under a matrix with
             * MSCALE_X = [matrixScaleX]. Exposed internal for DebugOverlayRendererTest.
             */
            internal fun computeSensorSpaceStroke(deviceStrokePx: Float, matrixScaleX: Float): Float {
                val safeScale = if (matrixScaleX < MIN_SAFE_SCALE) MIN_SAFE_SCALE else matrixScaleX
                return deviceStrokePx / safeScale
            }
        }
    }
    ```

    **Step 2 — Create a new unit test file** `app/src/test/java/com/bugzz/filter/camera/render/DebugOverlayRendererTest.kt` (pure-Kotlin JVM test — no Robolectric needed; PointF is `android.graphics.PointF` which has a trivial default-values-returning JVM stub but its fields `x` and `y` ARE accessible on stubs — use a manual constructor call to force values).

    Because `android.graphics.PointF` on the JVM `android.jar` stub returns default-value (0f) when accessed via Mockito, construct it directly via `PointF(x, y)` which sets the two public fields; the primary constructor works correctly because it uses `this.x = x` assignments (not method calls) and is therefore unaffected by the stub.

    ```kotlin
    package com.bugzz.filter.camera.render

    import android.graphics.PointF
    import org.junit.Assert.assertEquals
    import org.junit.Assert.assertNull
    import org.junit.Test

    /**
     * Nyquist unit test for [DebugOverlayRenderer] helper functions (GAP-02-B / CAM-07).
     *
     * Pins the two post-GAP-02-B correctness contracts that can be unit-tested without
     * Robolectric or a live Canvas:
     * 1. `centroidOf(points)` — mean x/y for the dot-density reduction (D-01 amendment).
     * 2. `computeSensorSpaceStroke(devicePx, scale)` — matrix-scale compensation formula
     *    (H2 root-cause fix).
     *
     * Canvas.drawRect / drawCircle paths are verified on-device per 02-HANDOFF.md Step 8-9.
     */
    class DebugOverlayRendererTest {

        @Test
        fun centroidOf_empty_list_is_null() {
            assertNull(DebugOverlayRenderer.centroidOf(emptyList()))
        }

        @Test
        fun centroidOf_single_point_is_the_point() {
            val c = DebugOverlayRenderer.centroidOf(listOf(PointF(100f, 200f)))!!
            assertEquals(100f, c.x, 0.001f)
            assertEquals(200f, c.y, 0.001f)
        }

        @Test
        fun centroidOf_multiple_points_is_mean() {
            val points = listOf(
                PointF(0f, 0f),
                PointF(100f, 100f),
                PointF(200f, 200f),
            )
            val c = DebugOverlayRenderer.centroidOf(points)!!
            assertEquals(100f, c.x, 0.001f)
            assertEquals(100f, c.y, 0.001f)
        }

        @Test
        fun computeSensorSpaceStroke_at_scale_1_is_identity() {
            // Matrix scale 1.0 (no transform) → sensor-space stroke equals device-pixel stroke.
            val s = DebugOverlayRenderer.computeSensorSpaceStroke(4f, 1.0f)
            assertEquals(4f, s, 0.001f)
        }

        @Test
        fun computeSensorSpaceStroke_at_scale_2_halves_stroke() {
            // Matrix scales sensor → buffer by 2×. To render at 4 device pixels, the stroke
            // value assigned to Paint must be 4/2 = 2 in sensor-space (H2 GAP-02-B fix).
            val s = DebugOverlayRenderer.computeSensorSpaceStroke(4f, 2.0f)
            assertEquals(2f, s, 0.001f)
        }

        @Test
        fun computeSensorSpaceStroke_at_representative_xiaomi_scale_matches_expected() {
            // Representative scaleX captured from Wave A diagnostic logs on Xiaomi 13T
            // (sensor 720×1280 → overlay buffer ~927×1920 ≈ 1.29× scale). 4f / 1.29 ≈ 3.10f.
            val s = DebugOverlayRenderer.computeSensorSpaceStroke(4f, 1.29f)
            assertEquals(3.10f, s, 0.05f)
        }

        @Test
        fun computeSensorSpaceStroke_at_zero_scale_returns_finite_value() {
            // MIN_SAFE_SCALE guard prevents NaN / infinity when matrix is pathological.
            val s = DebugOverlayRenderer.computeSensorSpaceStroke(4f, 0f)
            // 4 / MIN_SAFE_SCALE (0.0001) = 40000f — finite, not infinity or NaN.
            assertEquals(40_000f, s, 1f)
        }
    }
    ```

    **Step 3 — Amend `02-CONTEXT.md` D-01** (dot-density reduction scope change).

    Find D-01 in §"Debug Overlay Content (gray area 1)". Current text:

    > **D-01:** Debug overlay draws `face.boundingBox` as a red stroked rectangle AND contour landmark points (nose, left/right eye, cheeks, jawline) as small filled circles. Landmark dots validate contour-mode ingestion visually — Phase 3 will anchor production sprites to these same landmark points, so seeing them render correctly in Phase 2 proves the contour pipeline and reduces Phase 3 risk.

    Replace with:

    > **D-01 (amended 2026-04-19 post-GAP-02-B):** Debug overlay draws `face.boundingBox` as a red stroked rectangle (stroke width compensated for `sensorToBufferTransform` matrix scale — target ≈ 4 device pixels regardless of scale factor). Contour-mode ingestion is visually validated by drawing **one centroid dot per populated FaceContour type** (≤ 15 dots per primary face, not ~97 per-point dots); the centroid suffices to confirm each contour region (nose bridge, lips, cheeks, eyes, jawline) was populated without saturating the preview with overlapping dots. Secondary faces draw bounding box + fallback-landmark dots per D-23 / PITFALLS #13. Phase 3 anchors production sprites to the same contour points (full set, not centroid — Phase 3 concern), so this validation still reduces Phase 3 risk: if any contour type's centroid fails to render, D-01's Phase 3 contract is broken.

    **Step 4 — Run tests:**

    ```
    ./gradlew :app:testDebugUnitTest --tests "*DebugOverlayRendererTest*" --tests "*OverlayEffectBuilderTest*" --tests "*FaceDetectorOptionsTest*" --tests "*OneEuroFilterTest*" --tests "*CameraControllerTest*"
    ```

    All 14 tests (4 existing + new `DebugOverlayRendererTest` 7 cases + still-GREEN `FaceDetectorOptionsTest` from gaps-01) must PASS.

    Do NOT modify: `OverlayEffectBuilder.kt` beyond Wave A's diagnostic log, `FaceDetectorClient.kt`, `FaceSnapshot.kt`, `LandmarkSmoother.kt`, `OneEuroFilter.kt`, `CameraController.kt`, any Compose UI.

    `canvas.setMatrix(frame.sensorToBufferTransform)` in `OverlayEffectBuilder` is PRESERVED — CAM-07's zero-manual-matrix-math goal is maintained; the fix is renderer-side scale-awareness, not bypassing the transform.
  </action>
  <verify>
    <automated>./gradlew :app:testDebugUnitTest --tests "*DebugOverlayRendererTest*" --tests "*OverlayEffectBuilderTest*" --tests "*FaceDetectorOptionsTest*" --tests "*OneEuroFilterTest*" --tests "*CameraControllerTest*"</automated>

    Additional verify:
    - `grep -c "MSCALE_X\|matrixScaleX" app/src/main/java/com/bugzz/filter/camera/render/DebugOverlayRenderer.kt` MUST return `>= 2`.
    - `grep -c "centroidOf" app/src/main/java/com/bugzz/filter/camera/render/DebugOverlayRenderer.kt` MUST return `>= 2`.
    - `grep -c "computeSensorSpaceStroke" app/src/main/java/com/bugzz/filter/camera/render/DebugOverlayRenderer.kt` MUST return `>= 1`.
    - `grep -c "centroidOf\|computeSensorSpaceStroke" app/src/test/java/com/bugzz/filter/camera/render/DebugOverlayRendererTest.kt` MUST return `>= 4`.
    - `grep -c "face.contours.values.flatten" app/src/main/java/com/bugzz/filter/camera/render/DebugOverlayRenderer.kt` MUST return `0` (old density path removed).
    - `grep -c "GAP-02-B\|amended 2026-04-19" .planning/phases/02-camera-preview-face-detection-coordinate-validation/02-CONTEXT.md` MUST return `>= 3` (D-01 amendment adds one; D-15 + D-22 from gaps-01 add two more).
    - `./gradlew :app:assembleDebug` MUST exit 0.
  </verify>
  <done>
    Renderer rewritten with matrix-scale-compensated stroke/radii + per-contour-type centroid dots. 6-case unit test pins both helper formulas. D-01 amended in CONTEXT. 14+ unit tests all GREEN. APK rebuilds.
  </done>
</task>

<task type="checkpoint:human-verify" gate="blocking">
  <name>Task 3: Wave C — Device re-verification on Xiaomi 13T + HANDOFF sign-off</name>
  <what-built>
    Gap-closure APK with:
    - `FaceDetectorClient` no longer calling `.enableTracking()` (Plan 02-gaps-01)
    - `DebugOverlayRenderer` drawing matrix-scale-compensated 4-device-pixel stroked red rect + ≤ 15 centroid orange dots per face (Plan 02-gaps-02)
    - `OverlayEffectBuilder` emitting DEBUG-gated `OverlayDiag` Timber logs for ongoing observability
  </what-built>
  <how-to-verify>
    Claude automates these steps via `adb` from the developer PC; user participates by being in front of the device camera and rotating the phone between orientations. Steps mirror 02-HANDOFF.md Steps 8-11 with the gap-closure expectations.

    **Prep:** Build + install gap-closure APK.
    ```bash
    ./gradlew :app:assembleDebug
    adb install -r app/build/outputs/apk/debug/app-debug.apk
    adb shell am start -n com.bugzz.filter.camera/.MainActivity
    adb shell input tap 555 1800   # Home → Face Filter (reuse Plan 02-06 coords)
    sleep 2
    ```

    **Re-verify Step 8 (CAM-07 baseline alignment, still head, front lens portrait):**
    1. User holds Xiaomi 13T in portrait, faces front camera.
    2. Claude runs: `adb shell screencap -p /sdcard/gap-02-b-step8.png && adb pull /sdcard/gap-02-b-step8.png .tmp-shots/gap-02-b-step8.png`
    3. Expected: `.tmp-shots/gap-02-b-step8.png` shows **thin red stroked rectangle** wrapping user's face + **≤ 15 small orange dots at contour centroids** (nose, eyes, lips, cheeks, jawline). Face CLEARLY VISIBLE behind the overlay.
    4. User confirms visually on device: "rect wraps face, dots at face features, face visible."

    **Re-verify Step 9 (CAM-07 4 rotations × 2 lenses):**
    For each of 8 combinations, Claude captures a screencap + the user visually confirms alignment:

    Front lens (default):
    - a. Portrait — `.tmp-shots/gap-02-b-step9-front-portrait.png`
    - b. Landscape-left (rotate device 90° CCW) — `.tmp-shots/gap-02-b-step9-front-landscape-left.png`
    - c. Reverse-portrait (rotate 180°) — `.tmp-shots/gap-02-b-step9-front-reverse-portrait.png`
    - d. Landscape-right (rotate 90° CW from portrait) — `.tmp-shots/gap-02-b-step9-front-landscape-right.png`

    Back lens (tap flip button at coords 1064,120):
    - e. Portrait — `.tmp-shots/gap-02-b-step9-back-portrait.png`
    - f. Landscape-left — `.tmp-shots/gap-02-b-step9-back-landscape-left.png`
    - g. Reverse-portrait — `.tmp-shots/gap-02-b-step9-back-reverse-portrait.png`
    - h. Landscape-right — `.tmp-shots/gap-02-b-step9-back-landscape-right.png`

    In each screenshot: the red rect MUST wrap the detected face (user's face on front lens; any visible face or empty-frame no-rect-drawn on back lens). If any combination shows misalignment, document as OEM quirk → `02-HANDOFF.md` "OEM Quirks" sub-section + Phase 7 deferred item; do NOT introduce workarounds in this plan.

    **Logcat diagnostic observation:**
    ```bash
    adb logcat -d OverlayDiag:V FaceTracker:V *:S | head -40 > .tmp-shots/gap-02-b-logcat.log
    ```
    Expected: `OverlayDiag` lines show stable scaleX value (matches Wave A findings) + `FaceTracker` lines show `id=null` (expected post-gaps-01) with `bb=X,Y` centerX/Y moving <10px when head is still.

    **Update 02-VERIFICATION.md GAP-02-B:** flip status from `failed` to `closed` with:
    - Resolution paragraph: which hypothesis was confirmed (H1/H2/H3) + fix applied
    - Evidence links to `.tmp-shots/gap-02-b-step8.png` through `gap-02-b-step9-*.png`
    - Residual OEM quirks (if any) logged as Phase 7 items

    **Update 02-HANDOFF.md Actual Sign-Off:** add a new sub-section `## Gap-Closure Re-Verification (2026-04-19 / 20)` listing:
    - [x] Step 8 / CAM-01+09 — PASS (gap-fix screenshot `gap-02-b-step8.png`)
    - [x] Step 9 / CAM-07 — PASS (8 screenshots `gap-02-b-step9-*.png`)
    - [x] Step 10 / CAM-08 — PASS (relaxed per ADR-01: `id=null` expected; `bb=X,Y` stable — see `gap-02-b-logcat.log`)
    - Updated Result line: `**8/11 PASS from original + 4 re-verified via gap closure = 11/11 PASS. Phase 2 complete.**`

    **Re-verify Step 11 / CAM-06 (overlay-in-MP4 visual)** is handled by `02-gaps-03-PLAN.md`, not here — this task does NOT tap TEST RECORD or pull MP4.
  </how-to-verify>
  <resume-signal>
    Type "approved" after 8 screenshots confirm alignment across 4 rotations × 2 lenses + logcat shows expected OverlayDiag scaleX + null trackingIds. If any combination fails, describe the quirk and Claude will document it as Phase 7 deferred item (NOT block progress).
  </resume-signal>
  <files>
    app/build/outputs/apk/debug/app-debug.apk,
    .tmp-shots/gap-02-b-step8.png,
    .tmp-shots/gap-02-b-step9-front-portrait.png,
    .tmp-shots/gap-02-b-step9-front-landscape-left.png,
    .tmp-shots/gap-02-b-step9-front-reverse-portrait.png,
    .tmp-shots/gap-02-b-step9-front-landscape-right.png,
    .tmp-shots/gap-02-b-step9-back-portrait.png,
    .tmp-shots/gap-02-b-step9-back-landscape-left.png,
    .tmp-shots/gap-02-b-step9-back-reverse-portrait.png,
    .tmp-shots/gap-02-b-step9-back-landscape-right.png,
    .tmp-shots/gap-02-b-logcat.log,
    .planning/phases/02-camera-preview-face-detection-coordinate-validation/02-VERIFICATION.md,
    .planning/phases/02-camera-preview-face-detection-coordinate-validation/02-HANDOFF.md
  </files>
  <action>
    Execute the checkpoint device runbook described in &lt;how-to-verify&gt; above. Concretely: (1) rebuild APK with gaps-01 + gaps-02 code fixes + install via adb; (2) capture baseline front-portrait screenshot; (3) capture 8 rotation-lens matrix screenshots; (4) capture 40 lines of OverlayDiag + FaceTracker logcat; (5) update 02-VERIFICATION.md GAP-02-B status from &#96;failed&#96; to &#96;closed&#96; with H-resolution + evidence links; (6) append Gap-Closure Re-Verification sub-section to 02-HANDOFF.md Actual Sign-Off with Step 8/9/10 = PASS and 8/11 + 4 re-verified = 11/11 PASS result line. Wait for user &quot;approved&quot; signal before marking the task complete.
  </action>
  <verify>
    <automated>ls .tmp-shots/gap-02-b-step8.png .tmp-shots/gap-02-b-step9-front-portrait.png .tmp-shots/gap-02-b-step9-front-landscape-left.png .tmp-shots/gap-02-b-step9-front-reverse-portrait.png .tmp-shots/gap-02-b-step9-front-landscape-right.png .tmp-shots/gap-02-b-step9-back-portrait.png .tmp-shots/gap-02-b-step9-back-landscape-left.png .tmp-shots/gap-02-b-step9-back-reverse-portrait.png .tmp-shots/gap-02-b-step9-back-landscape-right.png .tmp-shots/gap-02-b-logcat.log 2&gt;&amp;1 | grep -c &quot;No such file&quot;</automated>
    Expected automated output: 0 (all 10 files present). Manual gate: user types &quot;approved&quot; after visually confirming thin red stroked rect + centroid dots on face across 8 rotation/lens combinations. Also grep-verify:
    - &#96;grep -c &quot;Gap-Closure Re-Verification&quot; 02-HANDOFF.md&#96; MUST return &gt;= 1
    - &#96;grep -c &quot;status: closed&quot; 02-VERIFICATION.md | xargs test 2 -le&#96; (at least 2: frontmatter + GAP-02-B gap entry)
  </verify>
  <done>
    CAM-07 flipped from FAIL to PASS in 02-VERIFICATION.md + 02-HANDOFF.md. GAP-02-B status = closed with hypothesis resolution documented. 10 evidence files captured in .tmp-shots/. User approved visual verification.
  </done>
</task>

</tasks>

<threat_model>
## Trust Boundaries

| Boundary | Description |
|----------|-------------|
| Biometric sensor → in-process | Face detection landmarks + contour points in the analysis pipeline |
| In-process → logcat | `OverlayDiag` tag (scaleX, MTRANS, bbox coords) + `FaceTracker` tag (from gaps-01) |
| In-process → disk (MP4) | OverlayEffect writes to VIDEO_CAPTURE target; file saved to DCIM/Bugzz |

## STRIDE Threat Register (inherited from Phase 2; relevant carry-forwards + new)

| Threat ID | Category | Component | Disposition | Mitigation Plan |
|-----------|----------|-----------|-------------|-----------------|
| T-02-02 | I (Information disclosure) | `OverlayEffectBuilder.logDiagnostic()` Timber output | mitigate | FIRST line of logDiagnostic inside its call site is a `BuildConfig.DEBUG` gate (checked before the function is invoked). Log emits aggregate bbox coords + matrix values ONLY; no per-landmark coordinate lists. Frame counter rate-limits to once per 60 frames (~once per 2s). Verified by grep: `grep -c "BuildConfig.DEBUG" OverlayEffectBuilder.kt` ≥ 1. Release builds never execute the logDiagnostic path. |
| T-02-03 | I (Information disclosure) | Saved MP4 in DCIM/Bugzz | accept | Debug build only (`D-04`); no change from Phase 2 baseline. The scale-compensated thin stroked rect + centroid dots are still biometric-adjacent data, but the existing `BuildConfig.DEBUG` gate on `DebugOverlayRenderer.draw()` (unchanged) ensures release builds skip the draw entirely. Post-GAP fix: release MP4s contain exactly zero overlay data, identical to pre-fix. |
| T-02-06 | I (Information disclosure) | `FaceTracker` logs | accept | Unchanged from gaps-01. |
</threat_model>

<verification>
- `./gradlew :app:testDebugUnitTest --tests "*DebugOverlayRendererTest*" --tests "*OverlayEffectBuilderTest*" --tests "*FaceDetectorOptionsTest*" --tests "*OneEuroFilterTest*" --tests "*CameraControllerTest*"` exits 0 — 14+ tests GREEN.
- `./gradlew :app:assembleDebug` exits 0.
- `grep -c "face.contours.values.flatten" app/src/main/java/com/bugzz/filter/camera/render/DebugOverlayRenderer.kt` returns `0` (old density path removed).
- `grep -c "MSCALE_X" app/src/main/java/com/bugzz/filter/camera/render/DebugOverlayRenderer.kt` returns `>= 1`.
- `grep -c "centroidOf\b" app/src/main/java/com/bugzz/filter/camera/render/DebugOverlayRenderer.kt` returns `>= 2`.
- 9 screenshots exist in `.tmp-shots/gap-02-b-*.png` (1 step8 + 8 step9) post-checkpoint.
- `02-VERIFICATION.md GAP-02-B` status field flipped from `failed` to `closed`.
- `02-HANDOFF.md` has new `Gap-Closure Re-Verification` sub-section with Step 8 / 9 / 10 marked PASS.
- CAM-07 requirement status in VERIFICATION frontmatter `requirements.covered` flipped from `fail` → `pass`.
</verification>

<success_criteria>
GAP-02-B closed when:
- Root cause (H1/H2/H3) isolated via Wave A diagnostic logs; findings documented in VERIFICATION.
- Renderer rewritten with matrix-scale-compensated stroke + per-contour-type centroid dot reduction.
- 6-case `DebugOverlayRendererTest` passes pinning `centroidOf` + `computeSensorSpaceStroke` formulas.
- Device visual verification on Xiaomi 13T: thin red stroked rect + ≤ 15 small orange dots wrap detected face, face clearly visible behind overlay.
- 4 rotations × 2 lenses matrix: red rect stays aligned on front lens (user face); back lens shows expected behavior (user confirms on device).
- HANDOFF Actual Sign-Off + VERIFICATION updated. CAM-07 flipped to PASS.
- Any OEM quirks observed documented as Phase 7 items (not worked around in Phase 2).
</success_criteria>

<output>
After completion, create `.planning/phases/02-camera-preview-face-detection-coordinate-validation/02-gaps-02-SUMMARY.md` covering:
- Wave A diagnostic findings (H1/H2/H3 verdict + representative log lines)
- Wave B code changes (lines of code, unit test counts)
- Wave C device verification evidence (9 screenshots listed, logcat sample, OEM quirks if any)
- Files modified (6 total)
- Reference: this plan closes GAP-02-B. GAP-02-C remains — executed in `02-gaps-03-PLAN.md`.
</output>
</content>
</invoke>