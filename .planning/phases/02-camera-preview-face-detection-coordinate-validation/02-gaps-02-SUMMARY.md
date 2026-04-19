# Phase 02 Gap Plan 02 — Summary

**Plan:** 02-gaps-02 — CAM-07 DebugOverlayRenderer fix (GAP-02-B closure)
**Status:** COMPLETE — all verification gates pass on Xiaomi 13T
**Completed:** 2026-04-19

---

## What Was Built

### Task 1 — Diagnostic logging (Wave A)
- `OverlayEffectBuilder.kt`: added `logDiagnostic(Matrix, Rect)` rate-limited to every 60th frame, DEBUG-gated (T-02-02). Logs `scaleX / scaleY / trans / preBB / postBB` on `OverlayDiag` Timber tag.
- On-device capture via adb on Xiaomi 13T yielded: `scaleX=0.741 scaleY=0.741 trans=-0.0,-180.0 preBB=500,400-1500x1150 postBB=370,300-1100x850` (13 frames over 25s face hold).

### Task 1b — Root cause fix (canvas clear)
Diagnostic disproved H2 (matrix down-scales 0.741×, not up-scales) and ruled out H3. **New hypothesis H4 identified and proven:** `OverlayEffect.overlayCanvas` is NOT cleared between frames — prior drawings persist. Accumulation over lens flips + natural face micro-motion produced the saturation.

Fix: `canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)` at start of `setOnDrawListener` before `setMatrix`. Single line, covers full buffer.

### Task 2 — Matrix compensation + centroid reduction (Wave B — defensive correctness layers)
- `DebugOverlayRenderer.kt` rewrite:
  - `computeSensorSpaceStroke(deviceStrokePx, matrixScaleX): Float` — divides device-pixel width by matrix scale so stroke renders at intended device size regardless of future hardware where sensor > buffer.
  - `centroidOf(points: List<PointF>): PointF?` — returns mean (x,y) or null for empty list.
  - `draw()` now iterates `face.contours.forEach { (_, points) -> centroidOf(points) }` → ≤15 dots per face instead of ~97.
  - Zero-scale safety: `MIN_SAFE_SCALE = 0.0001f` floor.
- New `DebugOverlayRendererTest.kt` — 7 TDD cases covering centroid math (3) + stroke compensation (4).
- `02-CONTEXT.md` D-01 amended (dot-density reduction + matrix compensation cross-ref).

### Task 3 — Device re-verify
- APK reinstalled on Xiaomi 13T.
- Screenshots captured via adb: `bf6.png` shows clean overlay; `bf7-after5flips.png` shows zero ghost accumulation after 5 consecutive flips; `test-frame60.png` (ffmpeg-extracted from TEST RECORD MP4) shows red rect baked into video.

## Results

| Requirement | Before | After |
|-------------|--------|-------|
| CAM-07 overlay alignment | FAIL (red saturation) | **PASS** — clean stroked rect + ~9 orange centroid dots on face |
| CAM-02 no-ghost on flip | accumulates rects per flip | **PASS** — canvas clears each frame; 5 flips show zero residue |
| CAM-06 MP4 overlay baked | architecture PASS / visual unverified | **PASS** — frame 60 of new test MP4 shows red rect composited into video |

### On-device evidence (Xiaomi 13T Pro, HyperOS, aristotle_global)

- `bf6.png` (post-fix, front cam, face hold): single red bounding box + 9 orange dots at contour centroids (nose, eyes, cheeks, forehead, mouth region).
- `bf7-after5flips.png` (back cam after 5× flip): no overlay (correct, no face in scene) + no leftover ghost rects.
- `test-final.mp4` ffprobe: `duration=4.965s / h264 / 720×1280 / 0 audio streams`. Extracted `test-frame60.png` shows overlay baked in.
- Full unit test suite: `./gradlew :app:testDebugUnitTest` → **17/17 GREEN** (10 Phase 2 original + 7 new DebugOverlayRendererTest).
- `./gradlew :app:assembleDebug` → BUILD SUCCESSFUL.

## Commits

- `9924141` — feat(02-gaps-02-01a): diagnostic logging for GAP-02-B investigation
- `697a074` — test(02-gaps-02-02): add DebugOverlayRendererTest (7 cases, RED)
- `ade6753` — feat(02-gaps-02-02): rewrite DebugOverlayRenderer — centroid dots + MSCALE_X compensation
- `1e49d8c` — docs(02-gaps-02-02): amend CONTEXT.md D-01
- `1ec0a0d` — **fix(02-gaps-02-01b): clear OverlayEffect canvas between frames** (THE root-cause fix)
- `9082fff` — docs(02-gaps-02): diagnostic findings — H4 canvas-persistence root cause

## Deviations

- **Task 1 split** — Task 1 was specified as a single `auto` task but included an interactive device-capture step. In practice it executed as 1a (automatable code change, spawned executor) + 1b (inline device capture + analysis by orchestrator with user coordinating phone position). Plan-checker W-01 flagged this mismatch; deviation documented here.
- **Task 2 Rule-3 auto-fix** — DebugOverlayRendererTest PointF assertions needed Robolectric (matched CameraControllerTest precedent) because `android.jar` stubs under `testOptions.unitTests.isReturnDefaultValues=true` returned 0f for PointF fields. Fix: `@RunWith(RobolectricTestRunner::class) @Config(sdk=[34])`. No other test changes.
- **Diagnostic conclusion inverted plan hypothesis** — plan treated H2 (matrix amplification) as most likely; on-device data showed matrix DOWN-scales 0.741×, disproving H2. H4 (canvas persistence) — not in original hypothesis set — was the actual cause. Task 2's centroid + MSCALE fixes remain valid as defensive layers but Task 1b's canvas-clear line is the root-cause fix.

## Follow-up — Research Updates Pending (Phase 7 maintenance backlog)

- `.planning/research/ARCHITECTURE.md §3` should document that `OverlayEffect` overlay canvas must be manually cleared each frame via `canvas.drawColor(0, PorterDuff.Mode.CLEAR)`. Current docs do not note this.
- `.planning/research/PITFALLS.md` could add a new Pitfall #15 "OverlayEffect canvas persistence between frames" for future readers. Deferred to Phase 7 research cleanup.

## Self-Check

- [x] All 3 tasks completed
- [x] 17/17 unit tests GREEN
- [x] Debug APK builds clean
- [x] Device re-verify PASSED on Xiaomi 13T
- [x] VERIFICATION.md §GAP-02-B "Diagnostic findings" sub-section written with actual measurements
- [x] CONTEXT.md D-01 amended
- [x] Root cause identified and fixed with a 1-line change
- [x] Defensive improvements (centroid, matrix compensation) retained as hardening
