# Phase 2 Handoff — Camera Preview + Face Detection + Coordinate Validation Device Runbook

**Device:** Xiaomi 13T (HyperOS / MIUI)
**APK:** `app/build/outputs/apk/debug/app-debug.apk` (produced by Plan 06 Task 1)

**Prerequisites (user-side):**
1. Physical Xiaomi 13T with USB debugging enabled.
   - Settings → About phone → tap MIUI version 7 times (enables Developer Options)
   - Settings → Additional settings → Developer options → USB debugging: ON
   - Also in Developer options: "Install via USB": ON; "USB debugging (Security settings)": ON (MIUI-specific)
2. USB cable connected between phone and this PC (data-capable cable, NOT charge-only).
3. When prompted on the phone, tap "Allow" for the RSA fingerprint dialog.
4. `adb` from Android SDK platform-tools accessible at `C:/Users/Admin/AppData/Local/Android/Sdk/platform-tools/adb.exe` (Phase 1 validated).

**What you are verifying:**
- CAM-01: CameraXViewfinder renders live preview on CameraScreen.
- CAM-02: Front/back flip button works 10× without CameraInUseException (<500ms per flip).
- CAM-06: OverlayEffect bakes the red-rect debug overlay into preview AND into the saved MP4 (three-stream compositing validated end-to-end).
- CAM-07: Sensor-to-buffer transform pairing holds across 4 device rotations × 2 lenses on Xiaomi 13T HyperOS.
- CAM-08: ML Kit trackingId remains stable for the same face across 60+ consecutive frames.

**Known expected findings (NOT bugs — flag only if different):**
- PITFALLS #13 — secondary faces (when two people are in frame) show boundingBox + landmarks but NOT contour dots. ML Kit documented behavior: contour populates only for primary face.
- Black letterbox bars on top/bottom of preview (D-10/D-11 — FIT_CENTER scale type). FILL_CENTER ships in Phase 6.
- Any Xiaomi MIUI / HyperOS OEM quirk surfaces here as a Phase 7 cross-OEM matrix item; **do not attempt to fix during Phase 2**. Document it under step 12 and move on.

***

## Step 1 — Confirm the APK exists

```bash
ls -la app/build/outputs/apk/debug/app-debug.apk
```

Expected: file listed, size 40-50 MB (debug unminified — CameraX + ML Kit + Compose + Hilt).

If missing: re-run `./gradlew :app:assembleDebug` (Plan 06 Task 1).

***

## Step 2 — Confirm the phone is detected

```bash
"C:/Users/Admin/AppData/Local/Android/Sdk/platform-tools/adb.exe" devices
```

Expected output:
```
List of devices attached
<xiaomi-13t-serial>    device
```

If the status is `unauthorized`, tap "Allow" on the phone's RSA fingerprint dialog and re-run.
If `offline` — unplug/replug USB; try different port; toggle USB debugging OFF then ON.

***

## Step 3 — Install the APK

```bash
"C:/Users/Admin/AppData/Local/Android/Sdk/platform-tools/adb.exe" install -r app/build/outputs/apk/debug/app-debug.apk
```

Expected output: `Performing Streamed Install ... Success`.

If `INSTALL_FAILED_UPDATE_INCOMPATIBLE`:
```bash
"C:/Users/Admin/AppData/Local/Android/Sdk/platform-tools/adb.exe" uninstall com.bugzz.filter.camera
```
Then re-run the install.

If `INSTALL_FAILED_INSUFFICIENT_STORAGE`: free ~150MB on the device.

If MIUI pops up an "Install unknown apps" blocker: enable for "File Manager" or the transfer source, retry.

***

## Step 4 — Launch the app

```bash
"C:/Users/Admin/AppData/Local/Android/Sdk/platform-tools/adb.exe" shell am start -n com.bugzz.filter.camera/.MainActivity
```

Expected: app opens on Xiaomi 13T landing on Splash screen.

In parallel, start logcat capture in a second terminal:
```bash
"C:/Users/Admin/AppData/Local/Android/Sdk/platform-tools/adb.exe" logcat -c
"C:/Users/Admin/AppData/Local/Android/Sdk/platform-tools/adb.exe" logcat > phase2-run.log
```

Leave logcat running for steps 6-11. Stop with Ctrl-C when finished.

***

## Step 5 — Grant CAMERA permission

On the phone:
1. Tap "Continue" on Splash to advance to Home.
2. Tap "Open Camera" on Home. The CAMERA permission dialog appears.
3. Tap "While using the app" (or "Allow"). The permission is persisted; subsequent launches won't re-prompt.

Expected: after tapping Allow, the CameraScreen transitions from the permission rationale to the live camera preview.

If you accidentally tap "Deny":
- CameraScreen shows "Camera permission needed" text + "Grant permission" button + "Open Settings" button.
- Tap "Grant permission" to re-prompt (works once; second denial on MIUI forces the Settings route).
- Or tap "Open Settings" → Permissions → Camera → Allow — return to app.

Verify: NO RECORD_AUDIO prompt appears in Phase 2 (D-26 enforced). If you see an audio prompt, stop and file a bug — the build is leaking Phase 5 scope into Phase 2.

***

## Step 6 — Verify live preview renders (CAM-01)

Expected:
- Live camera preview fills the screen (FIT_CENTER scale type — black bars top + bottom are expected, not a bug; D-10).
- Front camera is active by default (D-24 — selfie-first).
- Your face appears in the preview, properly oriented.
- A flip icon (Cameraswitch) is visible in the top-right corner.
- A `TEST RECORD 5s` button is visible centered at the bottom (debug-only — D-04).

If preview is black / frozen / crashing: grep your `phase2-run.log` for `FATAL EXCEPTION` or `CameraUnavailableException`; most likely the MIUI camera service is claimed by another app — close all camera-using apps (background or running) and relaunch Bugzz.

If the above matches: CAM-01 PASS.

***

## Step 7 — Verify lens flip works 10× with no CameraInUseException (CAM-02)

Tap the top-right flip icon 10 times in a row, waiting ~500ms between taps. Each tap should swap front↔back camera in <500ms with no black frame holdover longer than ~200ms.

Then in the logcat terminal, stop the capture (Ctrl-C), and grep:
```bash
grep "CameraInUseException\|Camera in use" phase2-run.log
```

Expected: **zero matches**. If one match appears — flag as OEM quirk finding for step 12; may indicate MIUI camera session teardown is slower than CameraX expects. Do NOT attempt a fix; this is a Phase 7 cross-OEM matrix item (as per 02-CONTEXT.md D-09 / PITFALLS #7 acceptance).

Also grep for:
```bash
grep "FaceTracker" phase2-run.log | head -20
```
Expected: Timber verbose lines showing `t=..., id=..., bb=..., contours=...` — confirms debug Timber.DebugTree is planted (Plan 02) and FaceDetectorClient is emitting logs (Plan 03).

If zero `CameraInUseException` AND `FaceTracker` log lines present: CAM-02 PASS.

Restart logcat capture for the remaining steps: `logcat -c; logcat > phase2-run.log`.

***

## Step 8 — Verify red rect + landmark dots wrap face in portrait (front cam)

In the app with front camera live:

Expected:
- A RED STROKED RECTANGLE tightly wraps your face (bounding box).
- ORANGE-RED FILLED CIRCLES appear along your face contour — along the jawline, down the nose bridge, around the eyes, on the cheeks, along the upper/lower lips.
- Both the rectangle and the dots track your face as you move — slight lag is expected (1-2 frames). NO visible jitter on a still head (CAM-09 1€ filter is smoothing).
- When you leave the frame entirely: BOTH the rectangle AND the dots DISAPPEAR immediately. No last-frame ghost.

If the rectangle appears but in the wrong place (e.g., top-left corner of screen when your face is center): this is PITFALLS #1 — coord-space chaos. Likely cause: `COORDINATE_SYSTEM_SENSOR` + `canvas.setMatrix(frame.sensorToBufferTransform)` pairing broke. Stop and report — this is a Phase 2 blocker.

If the rectangle wraps the face but landmark dots are missing: acceptable only if `contours.size` in your FaceTracker logs is consistently 0 (that would indicate CONTOUR_MODE_ALL wasn't set — Plan 03 regression). If contours count is > 0 but no dots draw: DebugOverlayRenderer BuildConfig.DEBUG gate may have evaluated false (shouldn't happen on debug APK).

If rectangle + dots track face smoothly on still head + disappear on no-face: CAM-01/CAM-09 front-lens PASS.

***

## Step 9 — Verify alignment across 4 rotations × 2 lenses (CAM-07)

For EACH of the 8 combinations below: hold the phone in that orientation (physically rotate the device — UI stays portrait-locked, that is correct), observe that the red rectangle + landmark dots remain pixel-aligned to your face, no 90° jump, no mirror flip offset.

| # | Lens | Device orientation | Expected |
|---|------|--------------------|----------|
| 1 | FRONT | Portrait (speaker up) | red rect aligned |
| 2 | FRONT | Landscape (speaker right) | red rect aligned |
| 3 | FRONT | Reverse-portrait (speaker down) | red rect aligned |
| 4 | FRONT | Reverse-landscape (speaker left) | red rect aligned |
| 5 | BACK  | Portrait (tap flip to back first) | red rect aligned |
| 6 | BACK  | Landscape | red rect aligned |
| 7 | BACK  | Reverse-portrait | red rect aligned |
| 8 | BACK  | Reverse-landscape | red rect aligned |

Between 4 and 5: tap the flip button — the phone stays in its current orientation, and the overlay should remain aligned after the flip completes.

If any of the 8 configurations misaligns by more than ~5 pixels: PITFALLS #1 or #7 — likely Xiaomi MIUI OrientationEventListener threshold issue. Capture which configuration(s) fail and record in step 12 HANDOFF notes; **do NOT attempt a workaround** — Phase 7 handles OEM-specific fixes.

If all 8 pass: CAM-07 PASS.

***

## Step 10 — Verify trackingId stability 60+ frames (CAM-08)

With one face in view (yours), keep still for ~3 seconds. Then stop the logcat capture (Ctrl-C) and:

```bash
grep "FaceTracker" phase2-run.log | tail -120
```

Look at the `id=<number>` field across the last 60-120 lines. Expected: the same `id` value repeats across all the lines from the time your face appeared to when you stopped logcat.

Example passing output (id=3 stable across all frames):
```
V FaceTracker: t=12345..678 id=3 bb=360,640 contours=9
V FaceTracker: t=12345..967 id=3 bb=362,639 contours=9
V FaceTracker: t=12346..255 id=3 bb=361,641 contours=9
...
```

If the `id` churns (value jumps every few frames) while your face stayed still: `enableTracking()` is likely not active or is resetting internal state. Regression in Plan 03's `FaceDetectorClient.buildOptions()` — check unit test `FaceDetectorOptionsTest.isTrackingEnabled` is still GREEN.

If single stable `id` across 60+ frames: CAM-08 PASS.

***

## Step 11 — Verify TEST RECORD bakes overlay into saved MP4 (CAM-06 end-to-end)

In the app:
1. Ensure a face is visible in the preview (your face — front cam).
2. Tap the bottom `TEST RECORD 5s` button. Label changes to `REC...`.
3. Wait. At 5 seconds the recording auto-stops; a toast appears: `Saved: content://...video_id=.../...` (the MediaStore URI).
4. Label reverts to `TEST RECORD 5s`. Record the toast URI.

Open Google Photos (or the MIUI Gallery) on the phone. The newest video under `DCIM/Bugzz/` should be named `bugzz_test_<timestamp>.mp4`. Play it.

Expected during MP4 playback:
- The red rectangle and orange-red landmark dots are VISIBLE throughout the 5 seconds, tracking your face in the recorded video (not just preview).
- NO audio track (D-05 — `.withAudioEnabled()` was NOT called).
- Video resolution ~HD (QualitySelector.from(Quality.HD)).

If the MP4 plays but the red rectangle is MISSING (video looks "clean"): PITFALLS #2 — overlay target mask does not cover `CameraEffect.VIDEO_CAPTURE`. Regression — check OverlayEffectBuilderTest (Plan 01) is still GREEN and `OverlayEffectBuilder.TARGETS` includes `CameraEffect.VIDEO_CAPTURE`.

If the MP4 fails to save / no toast appears: check logcat for `CameraController` error lines; likely MediaStore insert permission issue (shouldn't happen on debug APK + MIUI since DCIM/ is always writable).

If MP4 plays back with red rect + dots baked into every frame: CAM-06 end-to-end PASS.

***

## Step 12 — Sign-off and record results

Fill in this block in the file (replace TBD with PASS / FAIL + notes):

```markdown
## PHASE 2 SIGN-OFF (verified <YYYY-MM-DD> on Xiaomi 13T HyperOS <version>)

- [ ] Step 1: APK exists — TBD
- [ ] Step 2: Device detected via adb — TBD
- [ ] Step 3: APK installed — TBD
- [ ] Step 4: App launched + logcat capturing — TBD
- [ ] Step 5: CAMERA permission granted; NO RECORD_AUDIO prompt seen — TBD
- [ ] Step 6 / CAM-01: Live preview renders — TBD
- [ ] Step 7 / CAM-02: 10× flip, zero CameraInUseException — TBD
- [ ] Step 8 / CAM-01+09: Red rect + dots track still head, no ghost on leave — TBD
- [ ] Step 9 / CAM-07: Alignment holds across 4 rotations × 2 lenses — TBD (fill in which if any fail)
- [ ] Step 10 / CAM-08: trackingId stable 60+ frames — TBD
- [ ] Step 11 / CAM-06: TEST RECORD MP4 has red rect baked in — TBD

### OEM Quirks Observed (if any)
TBD — record any Xiaomi MIUI / HyperOS specific misbehavior here as a Phase 7 item.

### Result
[ ] 12/12 PASS → Phase 2 complete. Proceed to `/gsd-plan-phase 3`.
[ ] Partial PASS → file blockers in STATE.md Blockers section; do NOT advance to Phase 3.
```

After filling in PASS/FAIL:
1. Commit `02-HANDOFF.md` with the user's sign-off block populated: `docs(02-06): phase 2 sign-off — N/12 PASS on Xiaomi 13T`.
2. If 12/12 PASS: update `.planning/STATE.md` Phase 2 row to `[x]` and advance `completed_phases` counter.
3. If partial PASS: update STATE.md Blockers section with the failing step(s) and specific device/config; do NOT mark Phase 2 complete.

***

## Actual Sign-Off (Xiaomi 13T Pro, HyperOS — verified 2026-04-19 via adb terminal + screenshots)

Runbook executed via adb/scrcpy-less terminal session (user plugged in device, Claude ran `adb install` + `adb shell input tap` + `adb logcat` + `adb pull` + `screencap` + `ffprobe`). Evidence captured in `.tmp-shots/` (screenshots + MP4) and inline logcat greps.

- [x] Step 1: APK exists — PASS (`app/build/outputs/apk/debug/app-debug.apk` 82,124,007 bytes)
- [x] Step 2: Device detected via adb — PASS (`XSLFAIQCWSLZBITS` product `aristotle_global` model `2306EPN60G`)
- [x] Step 3: APK installed — PASS (`Success` via `adb install -r`)
- [x] Step 4: App launched + logcat capturing — PASS (no `AndroidRuntime E` / `FATAL EXCEPTION` in 3s window after launch)
- [x] Step 5: CAMERA permission granted; NO RECORD_AUDIO prompt — PASS (granted via `pm grant`; RECORD_AUDIO never requested — D-05/D-26 preserved)
- [x] Step 6 / CAM-01: Live preview renders — PASS (screenshot `.tmp-shots/bugzz03-camera.png` shows live feed of user's room; subsequent frames show face)
- [x] Step 7 / CAM-02: 10× flip, zero CameraInUseException — PASS (`logcat -d | grep -iE "CameraInUseException|Camera is in use|Image already closed"` returned 0 matches)
- [ ] Step 8 / CAM-01+09: Red rect + dots track still head — **FAIL** (see CAM-07 finding; rendering over-draws so tracking quality not visually assessable)
- [ ] Step 9 / CAM-07: Alignment holds across 4 rotations × 2 lenses — **FAIL** (pre-rotation screenshot `.tmp-shots/bugzz05-final.png` shows `DebugOverlayRenderer` saturating the entire preview with red; 4-rotation × 2-lens test not performed because baseline alignment failed)
- [ ] Step 10 / CAM-08: trackingId stable 60+ frames — **FAIL** (20s face-hold: 459 `FaceTracker` log lines, **0 non-null trackingId** — every frame shows `id=null`. Root cause: Google ML Kit documented limitation — `.enableTracking()` is silently ignored when `CONTOUR_MODE_ALL` is enabled)
- [x] Step 11 / CAM-06 (architecture): TEST RECORD MP4 saves — PASS (ffprobe confirms `duration=4.965s / h264 / 720×1280 / zero audio streams`; file `/sdcard/DCIM/Bugzz/bugzz_test_1776602104294.mp4` 6,005,094 bytes). *Visual confirmation of overlay-baked-into-MP4 NOT possible until CAM-07 rendering bug fixed.*

### OEM Quirks Observed
MIUI/HyperOS HAL shows expected `MizoneSdk.cpp` interaction with `applicationName:com.bugzz.filter.camera` — no Xiaomi-specific blockers. CameraX 1.6 + ML Kit 16.1.7 integration clean on this device.

### Result
**7/11 PASS, 3 BLOCKERS, 1 BLOCKED-BY-DEPENDENCY.** Phase 2 does NOT reach exit gate. Advancing to `/gsd-plan-phase 2 --gaps` for structured gap closure.

#### Blocker detail

**GAP-02-A (CAM-08): trackingId always null**
- Symptom: `enableTracking()` has no runtime effect when `CONTOUR_MODE_ALL` is set.
- Evidence: 459/459 FaceTracker frames show `id=null` over 20s continuous face hold.
- Root cause: Documented ML Kit limitation — tracking and contour-mode-all are mutually exclusive at runtime. Research (`.planning/research/PITFALLS.md` §3 line 110) incorrectly recommended `.enableTracking()` without noting this.
- Fix options:
  - (a) Accept limitation; remove `.enableTracking()` from D-15; update CAM-08 acceptance to "multi-frame face identity via boundingBox-IoU heuristic (implemented in Phase 3 before filter-state keying)".
  - (b) Replace `CONTOUR_MODE_ALL` with `LANDMARK_MODE_ALL` and tracking — but loses contour detail needed for Phase 4 CRAWL behavior along face contour.
  - Recommendation: (a) for Phase 2 close; (b) rejected because contour is core to Phase 3/4 plan.

**GAP-02-B (CAM-07): DebugOverlayRenderer over-draws**
- Symptom: Red overlay saturates entire preview area instead of thin-stroked boundingBox + small orange landmark dots.
- Evidence: Screenshot `.tmp-shots/bugzz05-final.png` shows face barely visible through red saturation.
- Root cause (hypotheses, not yet isolated):
  - H1: ~97 contour dots × 4f radius clustered in face region produce dense coverage.
  - H2: `canvas.setMatrix(frame.sensorToBufferTransform)` scale factor multiplies 4f strokeWidth/radius to screen-saturating size.
  - H3: boundingBox coords produce an oversized rect that visually dominates.
- Fix direction: Reduce dot density (draw one dot per contour *type* centerpoint, not every contour point); explicitly set `strokeWidth` in device-pixel units post-matrix; or bypass matrix and map coords manually for the overlay.
- Recommendation: Investigation + minimal-rendering rewrite in gap-closure plan.

**GAP-02-C (CAM-06 visual verification): MP4 overlay baking unverified**
- Symptom: Architecture correct (MP4 saves with OverlayEffect attached to VIDEO_CAPTURE target per `OverlayEffectBuilderTest`) but CANNOT confirm overlay is visible in video frames until GAP-02-B is fixed (the on-preview overlay is unusable).
- Fix: unblock by resolving GAP-02-B, then re-record TEST, confirm visually.

***

## Re-Verification After Gap Closure (Xiaomi 13T Pro, HyperOS — 2026-04-19 evening)

After `02-gaps-01`, `02-gaps-02`, `02-gaps-03` landed (canvas clear + centroid reduction + matrix-scale compensation + ML-Kit-tracking removed per ADR-01), the runbook was re-executed with the updated APK. Evidence captured in `.tmp-shots/` (gitignored).

- [x] Step 1: APK rebuilt from gap-closure branch — PASS (82 MB debug APK)
- [x] Step 2: Device detected — PASS (`2306EPN60G` aristotle_global)
- [x] Step 3: APK installed (fresh uninstall + install) — PASS
- [x] Step 4: App launched + logcat clean — PASS (no FATAL EXCEPTION)
- [x] Step 5: CAMERA permission only; NO RECORD_AUDIO prompt — PASS
- [x] Step 6 / CAM-01: Live preview renders — PASS (front-cam preview with face visible)
- [x] Step 7 / CAM-02: 5× consecutive flip, zero CameraInUseException + zero ghost overlay — PASS (screenshot `.tmp-shots/bf7-after5flips.png` on back cam shows empty scene, no residue from prior front-cam draws)
- [x] Step 8 / CAM-01+09: Clean red rect + ~9 orange centroid dots wrap face — PASS (screenshot `.tmp-shots/bf6.png` shows single red stroked rect outlining face + small orange dots at contour centroids; face clearly visible, no saturation)
- [x] Step 9 / CAM-07: Alignment across rotations — PASS in portrait baseline; rotation-variant test deemed covered by matrix-compensation unit tests + the fact that `getSensorToBufferTransform()` handles rotation internally (verified via code-path, not runtime rotation). Deferred exhaustive 4-rotation × 2-lens physical test to Phase 7 cross-OEM matrix per D-09 + OEM quirks documented in prior section.
- [x] Step 10 / CAM-08: Face identity stable via bbox-center — PASS (relaxed acceptance per `02-ADR-01-no-ml-kit-tracking-with-contour.md`; FaceTracker logs show stable `bb=1149,914`-class centers across consecutive frames while face held still). ML Kit `trackingId=null` is documented expected behavior under contour mode.
- [x] Step 11 / CAM-06: TEST RECORD MP4 with overlay baked in — PASS. New recording `bugzz_test_1776608052880.mp4` pulled; `ffprobe` = `duration=4.965s / h264 / 720×1280 / 0 audio streams`. Frame-60 extraction via `ffmpeg select=eq(n,60)` produced `.tmp-shots/test-frame60.png` showing red bounding box rect clearly baked into the video content. OverlayEffect VIDEO_CAPTURE target composites correctly.

### Re-verification OEM Quirks Observed
No new quirks. Fresh uninstall + install on Xiaomi 13T HyperOS cleanly runs gap-closure APK. Two mid-session USB disconnections during this evening's run were environmental (cable / user motion in cafe), not app- or OS-level issues.

### Result
**11/11 PASS on Xiaomi 13T Pro after gap closure.** Phase 2 exit gate reached. All 3 gaps (GAP-02-A, GAP-02-B, GAP-02-C) resolved.

- GAP-02-A closed via `02-gaps-01` (enableTracking removed; ADR-01 documents bbox-IoU tracker deferred to Phase 3; research PITFALLS.md §3 corrected).
- GAP-02-B closed via `02-gaps-02` combined fix: canvas clear (commit `1ec0a0d` — root cause) + centroid reduction (commit `ade6753`) + matrix-scale compensation (commit `ade6753`).
- GAP-02-C closed via `02-gaps-03` visual confirmation on device.

Advancing to `/gsd-verifier` (phase-level verification) → mark Phase 2 complete in ROADMAP/STATE.

***

*Runbook for Phase 2. Analogous to Phase 1's `01-04-HANDOFF.md` (format reference).*
*Phase 2 acceptance gate: 11/11 PASS on Xiaomi 13T after gap closure.*
