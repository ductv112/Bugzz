# Phase 02 Plan 06 — Summary

**Plan:** 02-06 — Clean debug APK + 02-HANDOFF.md Xiaomi 13T runbook + device sign-off
**Status:** COMPLETE with partial sign-off → 2 blockers surfaced → gap closure required
**Completed:** 2026-04-19

---

## What Was Built

- Clean debug APK rebuilt from master: `app/build/outputs/apk/debug/app-debug.apk` (82,124,007 bytes / ~78 MiB)
- 12-step Xiaomi 13T device-verification runbook at `.planning/phases/02-camera-preview-face-detection-coordinate-validation/02-HANDOFF.md`
- Runbook executed via `adb` terminal from developer PC (user plugged in Xiaomi 13T Pro `2306EPN60G` / aristotle_global / HyperOS). Actions automated: `adb install`, `shell pm grant CAMERA`, `shell input tap` for navigation + flip (10×) + TEST RECORD, `logcat` grep for errors/FaceTracker/CameraInUseException, `screencap` + `pull` for visual state, `ffprobe` on saved MP4.

## Results (Evidence-Backed, See 02-HANDOFF.md Actual Sign-Off)

| Requirement | Status | Evidence |
|-------------|--------|----------|
| CAM-01 live preview | PASS | Screenshot `bugzz03-camera.png` shows CameraX preview rendering room; later shots show face in frame |
| CAM-02 flip 10× no error | PASS | `logcat grep CameraInUseException` = 0 matches after 10 programmatic taps at (1064,120) |
| CAM-03 UseCaseGroup × 4 | PASS | `CameraControllerTest` green; runtime `CXCP` log shows 4 use cases bound + 1 effect |
| CAM-04 contour populate | PASS | `FaceTracker` log shows `contours=15` (all 15 ML Kit contour types populated); 459 frames in 20s ≈ 23fps |
| CAM-05 KEEP_ONLY_LATEST | PASS | Preview remained responsive through 20s face hold + flip storm; no `Image already closed` |
| CAM-06 MP4 pipeline | PASS (architecture) | `ffprobe` on `bugzz_test_1776602104294.mp4`: `duration=4.965s / h264 / 720×1280 / 0 audio streams`; file saved to `/sdcard/DCIM/Bugzz/` via MediaStore |
| CAM-06 overlay baked in MP4 | **BLOCKED** | Depends on CAM-07 rendering fix — cannot visually confirm until DebugOverlayRenderer over-draw is resolved |
| **CAM-07** overlay aligned 4 rot × 2 lens | **FAIL** | Screenshot `bugzz05-final.png` shows overlay saturating entire preview with red instead of thin stroked rect + small landmark dots |
| **CAM-08** trackingId stable | **FAIL** | 459/459 `FaceTracker` frames show `id=null`; `.enableTracking()` silently ignored by ML Kit when `CONTOUR_MODE_ALL` active |
| CAM-09 1€ filter smooth | PASS (unit) | 4 unit tests in `OneEuroFilterTest` green; runtime jitter-smoothing observability degraded because trackingId-keyed state never gets stable id (see CAM-08) |
| D-05 no audio in MP4 | PASS | ffprobe: zero audio streams |
| D-10 FIT_CENTER letterbox | PASS | UI hierarchy dump: preview surface bounds `[0,0][927,1920]` within screen `[0,0][1220,2712]` → correct letterbox |
| D-04 TEST RECORD debug-only button | PASS | Button visible in CameraScreen at bounds `[373,2472][848,2616]` with text `TEST RECORD 5s`; BuildConfig.DEBUG gate working |
| D-26/27 no RECORD_AUDIO prompt | PASS | Runbook Step 5 confirmed; audio permission never requested during any action |

**Overall:** 11/13 PASS, 2 BLOCKERS, 1 BLOCKED-BY-DEPENDENCY.

## Blockers (For Gap Closure)

### GAP-02-A — CAM-08 trackingId always null
**Evidence:** 20-second continuous face hold; 459 `FaceTracker` log lines; every single one shows `id=null`.
**Root cause:** Google ML Kit documented behavior — `.enableTracking()` is silently ignored at runtime when `FaceDetectorOptions.Builder().setContourMode(CONTOUR_MODE_ALL)` is set. `isTrackingEnabled` on the options object still reports `true` (which is why `FaceDetectorOptionsTest` passes), but the detector produces faces with `face.trackingId == null`.
**Research gap:** `.planning/research/PITFALLS.md` §3 line 110 recommended `.enableTracking()` without noting this mutual exclusivity. This is a research correctness issue that must be updated.
**Recommended fix (for gap-closure plan):**
1. Remove `.enableTracking()` from `FaceDetectorClient.buildOptions()` (D-15 update).
2. Update `FaceDetectorOptionsTest` to assert `isTrackingEnabled == false` (reflecting reality).
3. Amend `02-CONTEXT.md` D-15 + D-22 to document limitation and defer stable-tracking to Phase 3 (implement bbox-IoU heuristic — MediaPipe-style — to bridge frames by spatial proximity rather than ML Kit trackingId).
4. Relax CAM-08 acceptance in gap-closure to: "face identity persists across consecutive frames via bounding-box centroid-overlap; trackingId column in `FaceTracker` log may be null (documented ML Kit limitation with contour mode); next-face-continuity test added via bbox-IoU assertion."
5. Update `.planning/research/PITFALLS.md` §3 to document the limitation.

### GAP-02-B — CAM-07 DebugOverlayRenderer over-draws
**Evidence:** Screenshot `bugzz05-final.png` (`.tmp-shots/` directory). Front camera points at user's face. Expected: thin red stroked boundingBox rect + ~97 orange 4f-radius dots on landmark points. Actual: virtually the entire preview area saturated with red. Face barely visible underneath.
**Root cause hypotheses (not yet isolated — investigate during gap-closure):**
- H1 — Density: ~97 contour points × 4f-radius filled circles clustered in face region produce dense coverage that appears as blob.
- H2 — Matrix scale: `canvas.setMatrix(frame.sensorToBufferTransform)` may scale sensor→buffer with factor >> 1; under that matrix, a 4f strokeWidth / 4f radius renders at N× device pixels, saturating the frame.
- H3 — BoundingBox coord bug: face boundingBox coords may be in a different space than expected after matrix, producing oversized rect.
**Recommended fix:**
1. Add targeted logging to DebugOverlayRenderer and OverlayEffectBuilder: dump `frame.sensorToBufferTransform` matrix values; log boundingBox coords pre- and post-matrix transform.
2. If H2 confirmed: either bypass matrix for stroke widths (compute device-pixel stroke after matrix extract) OR render in buffer space directly with manual coord mapping.
3. Regardless of H1/H2/H3: reduce dot density to 1 dot per contour *type* (center-of-mass of each of 15 contour types) instead of all ~97 individual points. Produces ~15 dots per face, visually clear and minimum validates contour ingestion for Phase 3.
4. Verify on Xiaomi 13T: re-install → re-observe → confirm thin stroked rect + 15 small dots on face.

### GAP-02-C — CAM-06 overlay-in-MP4 visual confirmation
**Blocked by:** GAP-02-B resolution.
**Fix plan:** after B is fixed and device shows correct thin overlay, tap TEST RECORD, pull MP4 to PC, open in a media player or run `ffmpeg -i test.mp4 -vf "select=eq(n\,30)" -vframes 1 frame30.png` to extract a mid-recording frame, inspect frame PNG for red rect + dots baked in.

## Files

### Modified
- `.planning/phases/02-camera-preview-face-detection-coordinate-validation/02-HANDOFF.md` — appended "Actual Sign-Off" section with 11/13 PASS + blocker detail

### Created by this plan
- `app/build/outputs/apk/debug/app-debug.apk` (clean rebuild, 82 MB) — intermediate artifact, gitignored
- `.tmp-shots/bugzz01-splash.png` through `bugzz05-final.png` — evidence screenshots (gitignored, referenced here for diagnostic only)
- `.tmp-shots/test.mp4` — pulled test recording (gitignored)
- `.tmp-shots/ui.xml` — UI hierarchy dump (gitignored)

## Commits

- `375d437` — chore(02-06-01): clean debug build + tests + lint gate
- `b0fe515` — docs(02-06-02): write 02-HANDOFF.md runbook (12 steps)
- *(this SUMMARY + HANDOFF sign-off commit pending)*

## Deviations

**From plan 02-06 task 3:** the plan specified that Task 3 checkpoint awaits explicit user "approved" after 12/12 PASS runbook execution. Actual flow deviated: user plugged in device but was away from workspace, requested Claude to execute the runbook via `adb` terminal programmatically. Device testing completed via automated adb/screencap/ffprobe; VISUAL steps (overlay alignment across 4 rotations × 2 lenses, MP4 overlay-baked confirmation) were blocked by GAP-02-B rendering bug discovered at Step 8. User directed gap closure via GSD `--gaps` workflow (message 2026-04-19T19:4?Z: "B cho chỉn chu theo GSD nhé"). Deviation documented per Deviation Rule 4 (scope change requires user approval — obtained).

## Next Expected Action

1. Spawn `gsd-verifier` to produce `02-VERIFICATION.md` with `status: gaps_found`
2. `/gsd-plan-phase 2 --gaps` to create gap-closure plans for GAP-02-A + GAP-02-B
3. `/gsd-execute-phase 2 --gaps-only` to fix
4. Re-run `02-HANDOFF.md` verification → 12/12 PASS
5. Phase 2 complete → advance to Phase 3

## Self-Check

- [x] Runbook executed end-to-end where automatable
- [x] All automated gates (unit tests, build, logcat greps) have evidence
- [x] Blockers identified with root cause hypotheses and concrete fix plans
- [x] Scope of deviations documented + user approval captured
- [x] Files referenced with absolute or project-relative paths
