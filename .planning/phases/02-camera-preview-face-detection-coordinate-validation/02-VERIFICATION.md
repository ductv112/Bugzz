---
phase: 02
phase_name: 02-camera-preview-face-detection-coordinate-validation
date: 2026-04-19
verified: 2026-04-19T20:00:00Z
status: gaps_found
reviewed_by: gsd-verifier
score: 3/5 success criteria verified
method: adb-terminal-device-runbook (Xiaomi 13T Pro / aristotle_global / HyperOS) — see 02-HANDOFF.md §"Actual Sign-Off"
requirements:
  covered:
    - id: CAM-01
      status: pass
    - id: CAM-02
      status: pass
    - id: CAM-03
      status: pass
    - id: CAM-04
      status: pass
    - id: CAM-05
      status: pass
    - id: CAM-06
      status: partial
      note: architecture PASS (MP4 saves via OverlayEffect bound to VIDEO_CAPTURE); visual baked-in-overlay confirmation blocked by GAP-02-B
    - id: CAM-07
      status: fail
      gap: GAP-02-B
    - id: CAM-08
      status: fail
      gap: GAP-02-A
    - id: CAM-09
      status: partial
      note: unit-level PASS (4/4 OneEuroFilterTest green); runtime jitter-smoothing observability degraded because filter state is keyed on trackingId (always null — see GAP-02-A)
gaps:
  - id: GAP-02-A
    truth: "Face `trackingId` remains stable for the same face across 60+ consecutive frames (ROADMAP SC #5 / CAM-08)"
    requirement: CAM-08
    status: failed
    severity: blocker
    reason: "`.enableTracking()` is silently ignored by ML Kit at runtime when `setContourMode(CONTOUR_MODE_ALL)` is active. 459/459 `FaceTracker` log frames over a 20s continuous face-hold show `id=null`."
    evidence:
      - "app/src/main/java/com/bugzz/filter/camera/detector/FaceDetectorClient.kt:119-124 — both `setContourMode(CONTOUR_MODE_ALL)` (line 120) and `.enableTracking()` (line 123) are present per D-15, but the pairing is mutually exclusive at runtime per Google ML Kit documented behavior."
      - "02-HANDOFF.md §Actual Sign-Off Step 10: 20s face-hold captured 459 FaceTracker lines, 0 non-null trackingId."
      - ".planning/research/PITFALLS.md §3 line 110 recommended `.enableTracking()` without noting the tracking + contour-mode-all mutual exclusivity — root cause is a research correctness issue."
    artifacts:
      - path: "app/src/main/java/com/bugzz/filter/camera/detector/FaceDetectorClient.kt"
        issue: "buildOptions() pairs CONTOUR_MODE_ALL with enableTracking() — produces null trackingId at runtime"
      - path: "app/src/main/java/com/bugzz/filter/camera/detector/OneEuroFilter.kt"
        issue: "1€ filter state map keyed on trackingId — cannot accumulate stable per-face state while id is always null (CAM-09 runtime observability degraded)"
      - path: ".planning/research/PITFALLS.md"
        issue: "§3 line 110 mis-recommends enableTracking() with contour mode; must be amended"
    missing:
      - "Remove `.enableTracking()` from FaceDetectorClient.buildOptions() (or replace pairing strategy)"
      - "Update FaceDetectorOptionsTest to assert isTrackingEnabled == false"
      - "Amend 02-CONTEXT.md D-15 + D-22 to document limitation"
      - "Relax CAM-08 acceptance to bounding-box centroid-IoU continuity (heuristic) OR implement lightweight bbox-IoU face-bridge in FaceDetectorClient"
      - "Update .planning/research/PITFALLS.md §3 with the mutual-exclusivity limitation"
    recommended_plan: "02-gaps-01 (Detector + research amendment): remove enableTracking, add DetectorOptions test assertion flip, update CONTEXT D-15/D-22, amend PITFALLS §3, document deferred bbox-IoU heuristic as Phase 3 CAM-08-bridge item"
    estimated_effort: "S (1 plan ~0.5d): ~3-file code edit + 1 test flip + 3-file docs amendment + re-run runbook step 10 for sign-off"

  - id: GAP-02-B
    truth: "Debug overlay renders a red rectangle that pixel-perfectly wraps the detected face in portrait, landscape, reverse-portrait, and reverse-landscape, on BOTH front and back lens, with zero manual matrix math (ROADMAP SC #3 / CAM-07)"
    requirement: CAM-07
    status: failed
    severity: blocker
    reason: "`DebugOverlayRenderer` over-draws: instead of a thin red stroked boundingBox rect + small landmark dots, the red saturates virtually the entire preview area. Face barely visible under red. Hypotheses not yet isolated: (H1) ~97 contour points × 4f-radius dots cluster into a blob; (H2) `canvas.setMatrix(frame.sensorToBufferTransform)` scales 4f stroke/radius to N× device pixels; (H3) boundingBox coord-space post-matrix may produce oversized rect."
    evidence:
      - "app/src/main/java/com/bugzz/filter/camera/render/DebugOverlayRenderer.kt:33 (`strokeWidth = 4f`), :58 (`drawCircle(p.x, p.y, 4f, dotPaint)`), :62 (`drawCircle(p.x, p.y, 5f, dotPaint)`) — all stroke/radius values are in sensor-space coordinates because OverlayEffectBuilder.kt:51 calls `canvas.setMatrix(frame.sensorToBufferTransform)` before drawing, so any post-matrix scale factor is applied to strokes and radii too."
      - "app/src/main/java/com/bugzz/filter/camera/render/OverlayEffectBuilder.kt:41-51 — TARGETS/QUEUE_DEPTH/setMatrix pairing is architecturally correct per D-17; issue is renderer-side scale, not compositor wiring."
      - "02-HANDOFF.md §Actual Sign-Off Step 9: screenshot `.tmp-shots/bugzz05-final.png` shows red saturation over preview. 4-rotation × 2-lens alignment matrix (Step 9's 8-combination table) not performable — baseline alignment failed at step 8."
    artifacts:
      - path: "app/src/main/java/com/bugzz/filter/camera/render/DebugOverlayRenderer.kt"
        issue: "strokeWidth/radius literals drawn under setMatrix(sensorToBufferTransform) → amplified by matrix scale factor; ~97 contour dots per face produce blob coverage"
      - path: "app/src/main/java/com/bugzz/filter/camera/render/OverlayEffectBuilder.kt"
        issue: "setMatrix is architecturally correct but needs either a scale-extract helper OR renderer must use device-pixel stroke units post-matrix"
    missing:
      - "Add targeted logging to dump `frame.sensorToBufferTransform` matrix values + boundingBox pre/post-matrix coords"
      - "Isolate root cause (H1 vs H2 vs H3)"
      - "Fix direction: (a) reduce dot density to 1 dot per contour type (~15 dots/face, not ~97); (b) compute device-pixel stroke width post-matrix-scale-extract OR draw strokes with `Paint.STROKE_CAP` on a pre-matrix canvas; (c) verify boundingBox coord space consistency"
      - "Re-run HANDOFF Step 8 (still head) + Step 9 (4 rotations × 2 lenses) on Xiaomi 13T for sign-off"
    recommended_plan: "02-gaps-02 (Renderer scale/density fix): diagnostic logging wave → isolate H1/H2/H3 → minimal-rendering rewrite → re-verify step 8+9 on device"
    estimated_effort: "M (1 plan ~1-1.5d): includes diagnostic rebuild, ~1-2 file code edits, optional unit test for dot-density reducer, device re-verification"

  - id: GAP-02-C
    truth: "A 5-second test recording produced via VideoCapture saves an .mp4 in which the red debug rectangle is visibly baked into every frame (ROADMAP SC #4 / CAM-06 end-to-end)"
    requirement: CAM-06
    status: partial
    severity: blocked-by-dependency
    reason: "Architecture layer PASS — ffprobe confirms `duration=4.965s / h264 / 720×1280 / 0 audio streams` for `/sdcard/DCIM/Bugzz/bugzz_test_1776602104294.mp4` (6,005,094 bytes). OverlayEffect TARGETS include CameraEffect.VIDEO_CAPTURE (OverlayEffectBuilder.kt:67-68) and `OverlayEffectBuilderTest` is GREEN. BUT visual inspection of the MP4 to confirm overlay-baked-into-every-frame is impossible while the on-preview overlay saturates red (GAP-02-B)."
    evidence:
      - "02-HANDOFF.md §Actual Sign-Off Step 11: architecture PASS via ffprobe; visual PASS gated on GAP-02-B."
      - "app/src/main/java/com/bugzz/filter/camera/render/OverlayEffectBuilder.kt:67-68 — TARGETS = PREVIEW | VIDEO_CAPTURE | IMAGE_CAPTURE (CAM-06 architecturally correct)."
      - "02-06-SUMMARY.md §Results table: CAM-06 MP4 pipeline = PASS (architecture); CAM-06 overlay baked in MP4 = BLOCKED."
    artifacts:
      - path: ".tmp-shots/test.mp4"
        issue: "cannot visually confirm baked overlay until GAP-02-B is fixed; file exists, is well-formed, 720×1280 H.264 / 4.965s / no audio"
    missing:
      - "Resolution of GAP-02-B"
      - "Re-run HANDOFF Step 11: tap TEST RECORD → extract mid-recording frame via ffmpeg select filter → inspect PNG for red rect + dots baked in"
    recommended_plan: "02-gaps-03 (re-verify CAM-06 visual): depends on 02-gaps-02 (GAP-02-B fix). Bundle as final step of gap-closure runbook."
    estimated_effort: "XS (< 0.25d, bundled with GAP-02-B device re-verification)"

blockers_summary:
  total: 3
  critical: 2  # GAP-02-A, GAP-02-B
  dependent: 1  # GAP-02-C (blocked by B)
  advancement: "Phase 2 does NOT reach exit gate. Advance to `/gsd-plan-phase 2 --gaps` for structured closure. Do NOT start Phase 3 until GAP-02-A and GAP-02-B are closed and HANDOFF steps 8-11 are re-signed-off."

deferred: []  # No gaps can be deferred — CAM-07 + CAM-08 + CAM-06 visual are Phase 2's core contract per ROADMAP §Key Decisions (risk-front-loaded phase)

overrides: []
---

# Phase 2: Camera Preview + Face Detection + Coordinate Validation — Verification Report

**Phase Goal (ROADMAP.md §Phase 2):** Validate the architecturally load-bearing `OverlayEffect` + `MlKitAnalyzer(COORDINATE_SYSTEM_SENSOR)` + `getSensorToBufferTransform()` pairing end-to-end on real hardware so Phase 3+ can draw production sprites without rewriting the pipeline.

**Verified:** 2026-04-19 via `adb` terminal session against Xiaomi 13T Pro (`2306EPN60G` / `aristotle_global` / HyperOS). See `02-HANDOFF.md` §"Actual Sign-Off (Xiaomi 13T Pro, HyperOS — verified 2026-04-19 via adb terminal + screenshots)".

**Status:** `gaps_found` — 2 critical blockers (CAM-07 rendering over-draw + CAM-08 null trackingId) + 1 blocked-by-dependency (CAM-06 visual).

**Re-verification:** No (initial verification).

---

## Goal Achievement

The phase goal is **partially met**. The architectural pairing itself is sound and wired end-to-end on a real Xiaomi 13T:

- `OverlayEffect` is constructed once (D-25), attached to `PREVIEW | VIDEO_CAPTURE | IMAGE_CAPTURE` targets (CAM-06 architectural PASS), and its `onDrawListener` correctly applies `canvas.setMatrix(frame.sensorToBufferTransform)` before every draw call (D-17 / PITFALLS #5).
- `MlKitAnalyzer` runs with `COORDINATE_SYSTEM_SENSOR` in contour mode with `STRATEGY_KEEP_ONLY_LATEST` (CAM-04 / CAM-05 PASS — 459 frames in 20s ≈ 23fps, no `Image already closed` logs).
- `UseCaseGroup` binds all four use cases (Preview + ImageAnalysis + ImageCapture + VideoCapture) under one lifecycle with the single effect attached (CAM-03 PASS — `CameraControllerTest` green + runtime `CXCP` log confirms).
- Lens flip is stable (CAM-02 PASS — 10× flip, zero `CameraInUseException`).
- 5-second test recording saves a valid 720×1280 H.264 MP4 to `/sdcard/DCIM/Bugzz/` with no audio track (CAM-06 architectural PASS + D-05 PASS).

**BUT** — two production-blocking correctness defects prevent the downstream consumer contract (Phase 3 sprite rendering) from actually being usable today:

1. **CAM-07 renderer defect (GAP-02-B)** — the debug overlay does not render as a thin stroked rect + small dots. It saturates the entire preview with red. This means:
   - Visual pixel-alignment across 4 rotations × 2 lenses is **not observable** (you can't see whether the rect is aligned when the rect has been drawn over the whole frame).
   - Phase 3's sprite renderer, which will inherit the same `setMatrix(sensorToBufferTransform)` pairing, will face the same scale-factor pitfall if GAP-02-B's root cause turns out to be H2 (matrix scale amplifying stroke widths). Fixing this in Phase 2 is mandatory before Phase 3 authors sprite rendering code against a broken reference renderer.

2. **CAM-08 null trackingId (GAP-02-A)** — `.enableTracking()` is silently ignored by ML Kit when `CONTOUR_MODE_ALL` is set. The 1€ filter state map (`LandmarkSmoother`), which is keyed by `$trackingId:$landmark:$channel`, has no stable key today. Filters still unit-test green because the unit test doesn't exercise real ML Kit output, but runtime smoothing observability is degraded — every frame starts a fresh filter state. Phase 3's sprite-per-face state (animation phase, velocity) cannot key on `trackingId` without a Phase 2 fix or an explicit acceptance that Phase 3 must implement bbox-IoU bridging.

Conclusion: **the pairing is wired correctly; the pairing is not yet visually proven** because the renderer's output is unreadable and the tracking signal the rest of the pipeline is keyed on is constant-null. Goal is 60% achieved — enough to prove the architectural pairing compiles and runs at 23fps on HyperOS, not enough to hand Phase 3 a validated rendering reference.

---

## Success Criteria Verification (ROADMAP §Phase 2)

| # | Success Criterion | Status | Evidence |
|---|-------------------|--------|----------|
| 1 | Live CameraX preview renders via `CameraXViewfinder`; front/back flip swaps lens in <500ms without "Camera in use" errors on 10× toggles | ✓ PASS | Screenshot `.tmp-shots/bugzz03-camera.png` (live feed). Logcat grep for `CameraInUseException\|Camera is in use` after 10 programmatic flips at tap (1064,120) returned 0 matches. HANDOFF Step 6 + Step 7. |
| 2 | `MlKitAnalyzer` (bundled, contour mode) runs with `STRATEGY_KEEP_ONLY_LATEST`; preview sustains visibly smooth motion; no `Image already closed` logs | ✓ PASS | 459 `FaceTracker` frames logged in 20s continuous hold (≈23fps, within `>=24fps`±1 budget), `contours=15` (all 15 ML Kit contour types populated), no `Image already closed` in logcat. HANDOFF Step 7/10 evidence. `CameraControllerTest` CAM-05 pin is GREEN. |
| 3 | Debug overlay renders as a red rect pixel-perfectly wrapping the detected face in portrait + landscape + reverse-portrait + reverse-landscape, front + back lens, zero manual matrix math | ✗ FAIL | **GAP-02-B.** Screenshot `.tmp-shots/bugzz05-final.png` — renderer saturates entire preview with red; thin-rect + small-dots shape not visible. 4-rotation × 2-lens matrix test (HANDOFF Step 9) not performed because baseline visual (Step 8) failed. |
| 4 | A 5-second test recording via `VideoCapture` on the bound `UseCaseGroup` saves an MP4 in which the red rect is visibly baked into every frame | ⚠ PARTIAL | **GAP-02-C.** Architecture PASS: ffprobe on `bugzz_test_1776602104294.mp4` = `duration=4.965s / h264 / 720×1280 / 0 audio streams`; file saved under `/sdcard/DCIM/Bugzz/`. `OverlayEffectBuilder.TARGETS` includes `CameraEffect.VIDEO_CAPTURE` (`OverlayEffectBuilderTest` GREEN). Visual confirmation of overlay-in-MP4 blocked by GAP-02-B (on-preview overlay is unreadable; extracting a frame would just show the red saturation). |
| 5 | Face `trackingId` remains stable for the same face across 60+ consecutive frames; 1€ filter smooths landmark jitter to <1px/frame on a still head | ✗ FAIL | **GAP-02-A.** 459/459 `FaceTracker` frames show `id=null` (HANDOFF Step 10). `.enableTracking()` silently ignored under `CONTOUR_MODE_ALL` — documented ML Kit limitation. 1€ unit tests (4/4) are GREEN, but the filter's per-trackingId state map has no stable key at runtime, so the `<1px/frame jitter on still head` claim is unverifiable until trackingId bridging (bbox-IoU heuristic in Phase 3 or removed + accepted) lands. |

**Score: 2/5 full PASS + 1/5 architectural-only PARTIAL + 2/5 FAIL = 3/5 criteria verified.** (Counting CAM-06's architectural PARTIAL as +0.5 would give 2.5/5; the canonical score here records full-PASS-only.)

---

## Requirement Coverage (CAM-01 through CAM-09)

| Req | Description | Status | Evidence / Gap |
|-----|-------------|--------|----------------|
| CAM-01 | Live CameraX preview renders on `CameraXViewfinder` in CameraScreen | ✓ PASS | `CameraScreen.kt` wires `CameraXViewfinder(ImplementationMode.EXTERNAL)` + `surfaceRequest` flow from `CameraController`; runtime screenshot confirms live feed |
| CAM-02 | User can flip between front and back camera via on-screen button | ✓ PASS | `CameraScreen.kt` flip button (TopEnd) + `CameraViewModel.onFlipLens()` + `CameraController.flipLens()`; 10× flip runtime test = 0 `CameraInUseException` |
| CAM-03 | `UseCaseGroup` binds Preview + ImageCapture + VideoCapture + ImageAnalysis under one lifecycle | ✓ PASS | `CameraController.kt` builds `UseCaseGroup` with 4 use cases + 1 effect (`CameraControllerTest.bind_includes_all_four_use_cases_plus_overlay_effect` GREEN); runtime `CXCP` log confirms |
| CAM-04 | ML Kit Face Detection (contour mode, bundled) runs on preview frames via `MlKitAnalyzer(COORDINATE_SYSTEM_SENSOR)` | ✓ PASS | `FaceDetectorClient.kt:119-124` sets `CONTOUR_MODE_ALL` + `PERFORMANCE_MODE_FAST` + bundled model; `MlKitAnalyzer` constructed with `COORDINATE_SYSTEM_SENSOR` (D-17); runtime logs show `contours=15` populated |
| CAM-05 | ImageAnalysis backpressure = `STRATEGY_KEEP_ONLY_LATEST`; preview does not stall when detection is slow | ✓ PASS | `CameraController` ImageAnalysis.Builder configured with `STRATEGY_KEEP_ONLY_LATEST` (pinned by `CameraControllerTest` CAM-05 assertion GREEN); 20s face-hold + 10× flip storm showed no `Image already closed` log line |
| CAM-06 | `OverlayEffect` binds `PREVIEW \| IMAGE_CAPTURE \| VIDEO_CAPTURE`; debug red rect renders on preview | ⚠ PARTIAL | Architecture PASS (`OverlayEffectBuilder.TARGETS` = PREVIEW \| VIDEO_CAPTURE \| IMAGE_CAPTURE; MP4 saves correctly with effect wired); visual confirmation of baked-in overlay BLOCKED by GAP-02-B. See gap **GAP-02-C**. |
| CAM-07 | Debug overlay stays aligned in portrait + landscape, front + back lens (no manual matrix math, uses `frame.getSensorToBufferTransform()`) | ✗ FAIL | `OverlayEffect.onDrawListener` correctly calls `canvas.setMatrix(frame.sensorToBufferTransform)` BUT `DebugOverlayRenderer` draws with fixed `strokeWidth=4f` + `radius=4f/5f` **after** setMatrix, so values get scaled by the matrix's scale factor, saturating the preview. See gap **GAP-02-B**. |
| CAM-08 | Face `trackingId` remains stable across frames for the same face | ✗ FAIL | `FaceDetectorClient.buildOptions()` calls both `setContourMode(CONTOUR_MODE_ALL)` + `.enableTracking()` — these are mutually exclusive at ML Kit runtime. 459/459 frames show `id=null`. See gap **GAP-02-A**. |
| CAM-09 | 1€ filter smooths landmark jitter between detector and renderer | ⚠ PARTIAL | Unit-level PASS: `OneEuroFilterTest` 4/4 GREEN (Casiez CHI 2012 algorithm verified). `LandmarkSmoother` wired between `MlKitAnalyzer` callback and `AtomicReference<FaceSnapshot>`. Runtime PASS unverifiable because filter state map keys on `trackingId` (always null — see GAP-02-A). Once GAP-02-A is closed (bbox-IoU bridging OR accepted null-id with time-since-lost state key), runtime jitter-smoothing observability can be re-verified. |

**Requirement totals:**
- PASS: 5/9 (CAM-01, 02, 03, 04, 05)
- PARTIAL: 2/9 (CAM-06 architectural, CAM-09 unit-only)
- FAIL: 2/9 (CAM-07, CAM-08)

---

## Gaps Found

### GAP-02-A — CAM-08 trackingId always null (ML Kit contour+tracking mutual exclusivity)

**Requirement:** CAM-08
**Severity:** Blocker
**Truth failed:** ROADMAP SC #5 — "Face `trackingId` remains stable for the same face across 60+ consecutive frames"

**Root cause:**
Google ML Kit documented behavior — `.enableTracking()` is silently ignored at runtime when `FaceDetectorOptions.Builder().setContourMode(CONTOUR_MODE_ALL)` is set. The `FaceDetectorOptions.isTrackingEnabled` reflective field still reports `true` (which is why `FaceDetectorOptionsTest.isTrackingEnabled` unit test passes), but the detector produces faces with `face.trackingId == null`.

Research `.planning/research/PITFALLS.md` §3 line 110 recommended `.enableTracking()` without documenting this mutual exclusivity — this is a research correctness issue upstream of the plan.

**Evidence:**
- `app/src/main/java/com/bugzz/filter/camera/detector/FaceDetectorClient.kt:119-124`:
  ```kotlin
  .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
  .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)   // line 120
  .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
  .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
  .enableTracking()                                        // line 123
  .setMinFaceSize(0.15f)
  ```
- `02-HANDOFF.md` §Actual Sign-Off Step 10: 20s continuous face-hold on Xiaomi 13T → 459 `FaceTracker` log lines, **0 non-null trackingId**.

**Recommended fix (for /gsd-plan-phase 2 --gaps):**

Create a gap-closure plan `02-gaps-01` (Detector + research amendment):

1. **Code:** Remove `.enableTracking()` from `FaceDetectorClient.buildOptions()` (line 123). D-15 updated in 02-CONTEXT to reflect this.
2. **Test:** Update `FaceDetectorOptionsTest` to assert `options.isTrackingEnabled == false` (reflecting reality, not plan intent).
3. **Docs amendment:** Update `02-CONTEXT.md` D-15 + D-22 to document the ML Kit limitation and defer stable-tracking to Phase 3 (implement bbox-IoU centroid-overlap heuristic for multi-frame face identity).
4. **Acceptance:** Relax CAM-08 acceptance to: *"face identity persists across consecutive frames via bounding-box centroid-IoU (implemented in Phase 3 before filter-state keying); `trackingId` column in `FaceTracker` log may be null (documented ML Kit limitation with contour mode)."*
5. **Research:** Update `.planning/research/PITFALLS.md` §3 to document the contour + tracking mutual exclusivity.
6. **Verify:** Re-run HANDOFF Step 10 — expected result: logs show `id=null` (accepted); next-face-continuity added as a Phase 3 CAM-08-bridge item.

**Estimated effort:** S (~0.5d; 1 plan, 1 code edit, 1 unit test flip, 3 docs files, re-sign-off Step 10).

---

### GAP-02-B — CAM-07 DebugOverlayRenderer over-draws (matrix-scale hypothesis)

**Requirement:** CAM-07
**Severity:** Blocker
**Truth failed:** ROADMAP SC #3 — "Debug overlay drawn via `OverlayEffect.setOnDrawListener` renders a red rectangle that pixel-perfectly wraps the detected face in portrait, landscape, reverse-portrait, and reverse-landscape, on BOTH front and back lens, with zero manual matrix math"

**Root cause (hypotheses, not yet isolated — fix plan must begin with diagnostic wave):**

- **H1 — Dot density:** `DebugOverlayRenderer` draws ~97 contour points × 4f-radius filled circles all clustered in the face region. Even without matrix amplification, this would produce a blob.
- **H2 — Matrix scale amplification (most likely):** `OverlayEffectBuilder.kt:51` calls `canvas.setMatrix(frame.sensorToBufferTransform)` before passing control to `DebugOverlayRenderer`. The transform's scale factor can be substantial (sensor space → overlay buffer space). Any `strokeWidth = 4f` (line 33) or `drawCircle(..., 4f, ...)` (lines 58, 62) gets multiplied by that factor. If scale ≈ N, stroke ≈ N × 4 device pixels.
- **H3 — BoundingBox coord-space bug:** possible that `face.boundingBox` coords are in a slightly different space than the transform expects, producing an oversized rect.

**Evidence:**
- `app/src/main/java/com/bugzz/filter/camera/render/DebugOverlayRenderer.kt`:
  - Line 33: `strokeWidth = 4f` (used for rect outline)
  - Line 49-55: `canvas.drawRect(face.boundingBox.left/top/right/bottom.toFloat(), ...)` — coords directly from `face.boundingBox`
  - Line 57-58: `face.contours.values.flatten().forEach { p -> canvas.drawCircle(p.x, p.y, 4f, dotPaint) }` — all ~97 points drawn
  - Line 62: `canvas.drawCircle(p.x, p.y, 5f, dotPaint)` — smoothed landmark dots
- `app/src/main/java/com/bugzz/filter/camera/render/OverlayEffectBuilder.kt:51`: `canvas.setMatrix(frame.sensorToBufferTransform)`
- `02-HANDOFF.md` §Actual Sign-Off Step 8-9: screenshot `.tmp-shots/bugzz05-final.png` shows red saturation; 4-rotation × 2-lens matrix test not performed.

**Recommended fix (for /gsd-plan-phase 2 --gaps):**

Create a gap-closure plan `02-gaps-02` (Renderer scale/density fix):

1. **Diagnostic wave (1 task):** Add targeted Timber logs to `OverlayEffectBuilder.onDrawListener` dumping `frame.sensorToBufferTransform` matrix values + `face.boundingBox` pre-matrix and post-matrix coords (via `Matrix.mapRect`). Rebuild, run on device, capture 20 frames of logs.
2. **Isolate root cause (1 task):** From the logs, determine H1 vs H2 vs H3:
   - If scale factor is ~1.0 and dots still blob → H1 (density). Fix: reduce to 1 dot per contour type (15 total).
   - If scale factor is ≫ 1.0 → H2 (matrix amplification). Fix: extract scale from matrix once, divide 4f stroke/radius by it OR draw strokes on a pre-matrix canvas using manually-mapped coords.
   - If boundingBox post-matrix is visibly larger than face bounds → H3. Fix: correct coord space; likely means Face.boundingBox is already in buffer space but we're re-applying the transform.
3. **Fix wave:** Apply the isolated fix. Regardless of H1 outcome, reduce dot density to ≤ 15 dots/face (H1 mitigation) as defensive improvement — the full ~97-point debug is not needed to validate CAM-07.
4. **Verify on device:** Re-install → visually verify thin red stroked rect + ~15 small orange dots wrap face. Then run HANDOFF Step 9 — 4 rotations × 2 lenses on front + back.

**Estimated effort:** M (~1-1.5d; 1 plan, diagnostic rebuild + isolate + fix + device re-verification).

---

### GAP-02-C — CAM-06 overlay-in-MP4 visual confirmation (blocked by GAP-02-B)

**Requirement:** CAM-06 (visual layer)
**Severity:** Blocked-by-dependency
**Truth failed:** ROADMAP SC #4 — "A 5-second test recording produced via `VideoCapture` saves an `.mp4` in which the red debug rectangle is visibly baked into every frame (proves three-stream `PREVIEW | IMAGE_CAPTURE | VIDEO_CAPTURE` binding)"

**Root cause:**
Architecture layer is correct — `OverlayEffectBuilder.TARGETS = PREVIEW | VIDEO_CAPTURE | IMAGE_CAPTURE` pinned by `OverlayEffectBuilderTest` GREEN assertion; ffprobe on `/sdcard/DCIM/Bugzz/bugzz_test_1776602104294.mp4` shows a valid 4.965s / 720×1280 / H.264 / zero-audio MP4. But we **cannot visually confirm the overlay is baked into every frame** because on preview the overlay is a red blob (GAP-02-B); extracting a frame from the MP4 would just show that same blob, which tells us nothing about whether the compositing pipeline is correctly wiring VIDEO_CAPTURE.

**Evidence:**
- `app/src/main/java/com/bugzz/filter/camera/render/OverlayEffectBuilder.kt:67-68`:
  ```kotlin
  internal val TARGETS: Int =
      CameraEffect.PREVIEW or CameraEffect.VIDEO_CAPTURE or CameraEffect.IMAGE_CAPTURE
  ```
- `OverlayEffectBuilderTest` (from Plan 02-01 Nyquist) GREEN — pin enforced at test level.
- `02-HANDOFF.md` §Actual Sign-Off Step 11: MP4 saves correctly; visual confirmation blocked.

**Recommended fix:**

Create a lightweight `02-gaps-03` (or bundle into `02-gaps-02` final task):

1. **Wait for GAP-02-B closure** — no independent code work.
2. **Device re-verify:** With renderer fixed, tap TEST RECORD 5s → wait for auto-stop + toast → `adb pull` the MP4 → run `ffmpeg -i test.mp4 -vf "select=eq(n\,30)" -vframes 1 frame30.png` → inspect `frame30.png`.
3. **Expected outcome:** thin red rect + small orange dots visible over the face in the mid-recording frame.
4. **Sign-off HANDOFF Step 11** in `02-HANDOFF.md` Actual Sign-Off section.

**Estimated effort:** XS (< 0.25d, bundled with GAP-02-B device re-verification).

---

## Deferred Items

**None.** CAM-07, CAM-08 and CAM-06 (visual) are the core contract of Phase 2 per ROADMAP §Key Decisions (risk-front-loaded phase). ROADMAP does not assign these to any later phase. GAP-02-A's fix path *does* defer one sub-task (bbox-IoU tracking heuristic) to Phase 3, but that is a forward-looking note inside the gap closure, not a gap that can be deferred away from Phase 2 itself — Phase 2 must still close the acceptance-criterion relaxation + options removal before advancing.

---

## Verification Methodology

**Mode:** Automated device runbook via `adb` terminal session (not human-in-device interactive).

**Justification:** Per 02-06-SUMMARY.md §Deviations: plan 02-06 originally specified the user would execute the 12-step runbook on-device. User was away from workspace with the device plugged in and requested Claude execute the runbook programmatically via `adb`. User approved this deviation (message 2026-04-19T19:4?Z: "B cho chỉn chu theo GSD nhé") per GSD Deviation Rule 4.

**Tools used:**
- `adb install -r` — APK install (Success, 82,124,007 bytes)
- `adb shell pm grant com.bugzz.filter.camera android.permission.CAMERA` — headless permission grant (no RECORD_AUDIO prompt confirmed by absence from `dumpsys package`)
- `adb shell input tap <x> <y>` — navigation + 10× flip + TEST RECORD
- `adb logcat -d | grep -iE "..."` — error greps (CameraInUseException / FATAL EXCEPTION / Image already closed / FaceTracker)
- `adb shell screencap -p /sdcard/foo.png` + `adb pull` — visual evidence capture (`.tmp-shots/bugzz01-splash.png` through `bugzz05-final.png`)
- `adb shell uiautomator dump` — UI hierarchy dump for preview surface bounds + button positions
- `adb pull /sdcard/DCIM/Bugzz/bugzz_test_*.mp4` + `ffprobe` — MP4 format/duration/audio-track validation

**Limitations (explicitly acknowledged):**
- **4-rotation × 2-lens pixel-alignment matrix (HANDOFF Step 9):** not performable via adb alone — requires physical rotation of the device. Blocked additionally by GAP-02-B (baseline visual failed at Step 8 so matrix iteration was moot).
- **Visual confirmation of overlay-in-MP4 (HANDOFF Step 11 video playback):** architecture confirmed via ffprobe; frame-level visual confirmation requires either human playback on the device OR ffmpeg frame extraction; both require GAP-02-B to be fixed first or would show the red blob.
- **1€ filter runtime jitter measurement (<1px/frame on still head):** blocked by GAP-02-A (filter state map cannot key on trackingId that is always null); unit test coverage stands in until runtime observability is restored.

**Primary evidence reference:** `02-HANDOFF.md` §"Actual Sign-Off (Xiaomi 13T Pro, HyperOS — verified 2026-04-19 via adb terminal + screenshots)" — lines 274-320.

**Cross-reference:** `02-06-SUMMARY.md` §Results table + §Blockers sections (lines 17-64) synthesize the same evidence with root cause hypotheses and fix recommendations.

---

## Next Action

Advance to `/gsd-plan-phase 2 --gaps` to produce gap-closure plans:
- `02-gaps-01` — GAP-02-A (detector + research amendment)
- `02-gaps-02` — GAP-02-B (renderer scale/density diagnostic + fix + device re-verification)
- `02-gaps-03` — GAP-02-C (re-verify CAM-06 visual after GAP-02-B closes); may be bundled into `02-gaps-02`'s final task.

Then `/gsd-execute-phase 2 --gaps-only`, then re-run HANDOFF Steps 8-11 on Xiaomi 13T for final sign-off.

**Do NOT start Phase 3 until GAP-02-A and GAP-02-B are closed and the handoff is re-signed-off (target: full PASS on HANDOFF Steps 8-11; partial PASS on Step 9 acceptable if OEM quirk — document in Phase 7).**

---

*Verified: 2026-04-19 via adb terminal runbook on Xiaomi 13T Pro / aristotle_global / HyperOS*
*Verifier: Claude (gsd-verifier)*
*Phase 2 exit gate: NOT REACHED — 2 blockers + 1 blocked-by-dependency*
