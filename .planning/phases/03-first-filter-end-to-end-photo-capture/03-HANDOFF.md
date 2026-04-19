# Phase 3 Handoff — First Filter End-to-End + Photo Capture Device Runbook

**Device:** Xiaomi 13T (HyperOS / MIUI)
**APK:** `app/build/outputs/apk/debug/app-debug.apk` (produced by Plan 03-05 Task 1 — 79.1 MB)
**Reference APK:** `reference/com.insect.filters.funny.prank.bug.filter.face.camera.apk`
**Reference APK SHA-256 (T-03-06 asset provenance):** `616c6990331ea59f36f135221f15aff2cdb5c290dfde0cd6c91f898fa26e7859`
(Hash recorded in `reference/APK_SHA256.txt` during Plan 03-03 Wave 0 extraction — verify it matches before proceeding)

---

## Prerequisites (user-side)

1. Phase 2 prerequisites still met:
   - Settings → Additional settings → Developer options → USB debugging: ON
   - "Install via USB": ON (MIUI-specific)
   - "USB debugging (Security settings)": ON (MIUI-specific)
2. USB cable connected between phone and this PC (data-capable, not charge-only).
3. When prompted on phone, tap "Allow" for the RSA fingerprint dialog.
4. `adb` at `C:/Users/Admin/AppData/Local/Android/Sdk/platform-tools/adb.exe` (Phase 1 + 2 validated path).
5. Google Photos installed + signed into an account on Xiaomi 13T (preinstalled — confirm in app drawer).
6. Phone NOT in battery-saver or thermal-throttle mode (cool device preferred for REN-08 subjective smoothness).
7. ~200 MB storage free on device (30 JPEGs at ~3 MB each = ~90 MB; reference APK install + Bugzz APK install ~100 MB).

---

## What you are verifying

| Req | Description | Hard gate? |
|-----|-------------|------------|
| **REN-07** | Live filter swap via Cycle Filter button (ant on nose ↔ spider on forehead) with no visible freeze, no black flash, no CameraX rebind | Yes |
| **REN-08** | Subjective ≥24fps smoothness during ant-on-nose filter playback, 30s hold — no visible stutter or jank | No (soft gate — document as Phase 7 perf if fails) |
| **CAP-02** | JPEG captured by shutter shows bug sprite baked in at same position as live preview | Yes |
| **CAP-04** | Front-cam JPEG mirror convention matches reference app behavior | No (soft gate — document as gap if mismatch) |
| **CAP-05** | Saved photo appears in Google Photos within 1s of capture | No (soft gate — document as gap if fails) |
| **CAP-06** | No LeakCanary notification after 30 consecutive captures + kill + relaunch | Yes |
| **ADR-01 #4** | BboxIouTracker-assigned ID stable across 60+ consecutive frames (face held still) | Yes |

**Hard gates** (Steps 3, 5, 8, 10): failure here blocks Phase 3 sign-off — create `03-gaps-0N-PLAN.md` before advancing.

**Soft gates** (Steps 1-mirror, 9, 11): document the finding; Phase 3 may still close if you agree to a follow-up plan.

---

## Known expected findings (NOT bugs — flag only if different)

- Front-cam JPEG may differ in mirror orientation from preview depending on CameraX 1.6 ImageCapture default; Step 1 establishes the convention before testing Bugzz.
- Black letterbox bars on top/bottom of preview: expected (FIT_CENTER scale type from Phase 2 D-10; FILL_CENTER is Phase 6).
- Debug overlay (red bounding box + orange contour centroids) is visible BEHIND the bug sprite in the DEBUG build — both renderers draw per D-27 (FilterEngine first, DebugOverlayRenderer on top). This is correct and expected.
- Phase 2 PITFALLS #13: secondary faces show no contour dots (ML Kit contour-primary-face-only limitation). Phase 3 renders bug only on primary face — no bug on secondary face is correct.
- Xiaomi MIUI / HyperOS OEM quirks: document any unexpected behavior under the OEM Quirks section at the end; do NOT attempt to fix; Phase 7 cross-OEM matrix handles them.

---

## Step 1 — Verify APK and device connectivity

```bash
ls -la app/build/outputs/apk/debug/app-debug.apk
"C:/Users/Admin/AppData/Local/Android/Sdk/platform-tools/adb.exe" devices
```

Expected:
- APK file listed, size ~79 MB (debug unminified — Phase 3 sprites add ~3 MB vs Phase 2 82 MB baseline; size fluctuates with frame compression).
- Device output: `<xiaomi-13t-serial>    device` (not `unauthorized` or `offline`).

If `unauthorized`: tap "Allow" on RSA fingerprint dialog on phone, re-run.
If `offline`: unplug/replug USB; toggle USB debugging OFF then ON.

PASS / FAIL: ________

---

## Step 2 — Reference APK install + front-cam mirror inspection (CAP-04 lock gate)

This step establishes the mirror convention BEFORE testing Bugzz. Execute it first.

**2a. Verify reference APK SHA-256 (T-03-06 asset provenance):**
```bash
sha256sum reference/com.insect.filters.funny.prank.bug.filter.face.camera.apk
```

Expected: `616c6990331ea59f36f135221f15aff2cdb5c290dfde0cd6c91f898fa26e7859`
(Must match `reference/APK_SHA256.txt` entry — same APK used for sprite extraction in Plan 03-03.)

PASS / FAIL: ________

**2b. Install reference app:**
```bash
"C:/Users/Admin/AppData/Local/Android/Sdk/platform-tools/adb.exe" install -r reference/com.insect.filters.funny.prank.bug.filter.face.camera.apk
```
Expected: `Performing Streamed Install ... Success`

**2c. Launch reference app + capture front-cam selfie:**

Launch `com.insect.filters.funny.prank.bug.filter.face.camera` on Xiaomi 13T. Tap through splash / onboarding. Enter the camera view. Switch to front camera (selfie mode). Do NOT apply any filter — use the plain camera. Capture a photo. Open Google Photos → find the newest image.

Inspect the saved image:
- **Mirror orientation:** Is the photo LEFT-RIGHT MIRRORED (matches what you saw in preview — same as standard selfie apps like Snapchat) or NOT MIRRORED (flipped back to "camera POV")?
- **Save path:** Note the folder name (e.g., `DCIM/Camera/` or `DCIM/<packagedir>/` or similar). Compare to Bugzz's `DCIM/Bugzz/`.

Record result here:
- Reference mirror orientation: `[ ] MIRRORED` / `[ ] NOT MIRRORED`
- Reference save path: _______________

Decision for Bugzz (record in 03-SUMMARY.md post-handoff):
- If reference is MIRRORED → CameraX 1.6 ImageCapture front-cam default is expected to match; if Bugzz Step 6 also produces a mirrored JPEG, CAP-04 passes with no code change.
- If reference is NOT MIRRORED → document as a potential gap; check if Bugzz produces the same convention; if Bugzz is mirrored, create `03-gaps-01-PLAN.md` with ImageCapture mirror-mode fix or post-capture bitmap flip.
- If CameraX 1.6 ImageCapture.Builder has no setMirrorMode for ImageCapture (CONTEXT D-18 risk): fall back to post-capture bitmap horizontal flip; document in 03-SUMMARY.md.

**2d. Uninstall reference app (keep device clean):**
```bash
"C:/Users/Admin/AppData/Local/Android/Sdk/platform-tools/adb.exe" uninstall com.insect.filters.funny.prank.bug.filter.face.camera
```
Expected: `Success`

PASS / FAIL: ________

---

## Step 3 — Install + launch Bugzz debug APK

```bash
"C:/Users/Admin/AppData/Local/Android/Sdk/platform-tools/adb.exe" install -r app/build/outputs/apk/debug/app-debug.apk
"C:/Users/Admin/AppData/Local/Android/Sdk/platform-tools/adb.exe" shell am start -n com.bugzz.filter.camera/.ui.MainActivity
```

Expected: `Performing Streamed Install ... Success`; app opens on Splash screen → Home.

If `INSTALL_FAILED_UPDATE_INCOMPATIBLE` (existing version incompatible):
```bash
"C:/Users/Admin/AppData/Local/Android/Sdk/platform-tools/adb.exe" uninstall com.bugzz.filter.camera
```
Then re-install.

If MIUI shows "Install unknown apps" blocker: enable for the install source, retry.

Launch logcat in a second terminal (keep running through Steps 4-10):
```bash
"C:/Users/Admin/AppData/Local/Android/Sdk/platform-tools/adb.exe" logcat -c
"C:/Users/Admin/AppData/Local/Android/Sdk/platform-tools/adb.exe" logcat > phase3-run.log
```

PASS / FAIL: ________

---

## Step 4 — Grant CAMERA permission + initial filter loads (REN-01, REN-03, REN-05 visual)

On the phone:
1. Tap "Open Camera" on the Home screen. CAMERA permission dialog appears.
2. Tap "While using the app" / "Allow". Camera screen activates.

Expected within ~1 second after permission grant:
- Live preview renders (front camera, FIT_CENTER — black bars top/bottom are expected).
- **An ANIMATED ANT SPRITE appears on your nose**, tracking your head movement smoothly. The ant is a small cartoon ant (~20% of face width) anchored to your nose tip.
- Ant animation advances frame-by-frame (flipbook at ~15fps) — the ant appears to crawl in place.
- Debug overlay (red stroked bounding box + orange centroid dots) is visible BEHIND the ant sprite (correct — D-27 draw order).
- Three buttons visible:
  - Top-end: "Flip" button (flip lens)
  - Bottom-center: white 72dp circle = shutter button
  - Bottom-end (DEBUG only): "Cycle" button (cycle filter)

VERIFY:
- [ ] Ant visible on nose (REN-01, REN-03)
- [ ] Ant animation advances — not a frozen single frame (REN-05)
- [ ] Debug overlay present behind ant in debug build (D-27)

If ant is NOT visible: check logcat for `FATAL EXCEPTION`, `AssetLoader`, or `FilterEngine` error lines. Likely cause: asset preload failure or Hilt graph wiring issue. Report to planner — hard blocker.

If ant is visible but NOT animating (frozen single frame): likely `frameCount=1` fallback triggered or `frameDurationNanos` is 0. Report as Phase 3 gap.

PASS / FAIL: ________

---

## Step 5 — Filter swap via Cycle Filter button (REN-07)

**This is a HARD GATE step. Failure blocks Phase 3 sign-off.**

Tap the `Cycle` button (bottom-end). Observe:

Expected:
- Bug sprite changes from **ant on nose** to **spider on forehead** within 1 frame — no visible freeze, no preview black flash (CameraX rebind would cause black flash + "Camera in use" error).
- Spider appears on your forehead area (midpoint above eyebrows, ~20% face width).
- Spider has its own multi-frame animation (23 frames from reference APK spider sprite).

Tap Cycle again — switches back to ant on nose. Tap 5 times rapidly — swaps should keep pace with no queue backup or stutter.

VERIFY:
- [ ] Swap is instantaneous — no visible freeze between ant and spider (REN-07 proves `AtomicReference` swap, NOT CameraX rebind)
- [ ] No preview black flash during any swap
- [ ] Face tracking is uninterrupted through all swaps
- [ ] No "last frame ghost" of previous filter (D-11 no-ghost — early return when preload pending)

Check logcat after rapid 5× taps:
```bash
grep -i "CameraInUseException\|Camera in use" phase3-run.log
```
Expected: **zero matches**.

PASS / FAIL: ________

---

## Step 6 — Front-cam shutter capture (CAP-01, CAP-02, CAP-05)

**CAP-02 is a HARD GATE requirement. Failure blocks Phase 3 sign-off.**

Select ant filter (Cycle until ant visible on nose). Hold your face in frame. Tap the white shutter circle (bottom-center).

Expected immediately:
- Haptic buzz (D-15)
- Brief white flash overlay (D-16 — 150ms fade)
- Toast "Saved to gallery" within ~1 second of tap

Start stopwatch from the Toast moment. Open Google Photos immediately.

VERIFY:
- [ ] Toast "Saved to gallery" appears (CAP-01 success path)
- [ ] Newest image in Google Photos (DCIM/Bugzz/) is the just-captured photo (CAP-05 — ≤1s from Toast to thumbnail visible)
- [ ] JPEG shows **bug sprite (ant) baked into the image** at the same position as the live preview (CAP-02 — OverlayEffect IMAGE_CAPTURE target compositing proven end-to-end)
- [ ] File path in photo info: `DCIM/Bugzz/Bugzz_YYYYMMDD_HHmmss.jpg` (CAP-03 / D-32)

Cross-check mirror convention (CAP-04):
- [ ] Compare JPEG mirror orientation to reference app finding from Step 2
  - Both MIRRORED → CAP-04 PASS
  - Both NOT MIRRORED → CAP-04 PASS
  - They differ → CAP-04 FAIL (soft gate — document as `03-gaps-01-PLAN.md` mirror-mode fix; not a hard blocker for Phase 3 close but must be tracked)

If no ant in JPEG (sprite NOT baked in): this is a CAP-02 FAILURE. Check logcat for `FilterEngine`, `OverlayEffect`, or `ImageCapture` errors. Possible causes: OverlayEffect IMAGE_CAPTURE target not wired, or face detection latency missed the capture frame. Hard blocker — create `03-gaps-0N-PLAN.md`.

PASS / FAIL: ________

---

## Step 7 — Back-cam capture (CAP-02 back lens)

Tap `Flip` (top-end) to switch to back camera. Point camera at a face (second person, or hold up a printed photo of a face in front of the camera). Bug filter should attach to the detected face.

Tap shutter. VERIFY:
- [ ] JPEG saved to DCIM/Bugzz/ with bug sprite baked in at same position as preview
- [ ] No mirror concern for back-cam — photo is "camera POV" (un-mirrored), same as standard photography

If no face detectable for back-cam test, skip this step and note "no second face available — back-cam CAP-02 not verified" in sign-off.

PASS / FAIL / SKIPPED: ________

---

## Step 8 — Filter swap after capture (REN-07 continuity)

Flip back to front camera. Cycle filter to spider. Hold face steady (spider should appear on forehead). Tap shutter.

VERIFY:
- [ ] Capture toast fires
- [ ] Newest JPEG in Google Photos shows **spider on forehead** baked in (correct filter ID captured — not the previous ant)

PASS / FAIL: ________

---

## Step 9 — BboxIouTracker ID stability (CAM-08 re-verification — ADR-01 follow-up #4)

**This is a HARD GATE step. Failure blocks ADR-01 closure.**

Clear logcat and restart capture:
```bash
"C:/Users/Admin/AppData/Local/Android/Sdk/platform-tools/adb.exe" logcat -c
"C:/Users/Admin/AppData/Local/Android/Sdk/platform-tools/adb.exe" logcat -s FaceTracker:V > phase3-cam08.log
```

Hold head still in front-camera frame for **60 seconds** (face clearly visible, minimal movement). After 60 seconds, stop logcat (Ctrl-C).

Inspect the log:
```bash
grep "FaceTracker" phase3-cam08.log | tail -120
```

Expected log line format (from `FaceDetectorClient` Timber.tag):
```
V/FaceTracker( ...): t=<nanos> id=<N> bb=<x>,<y> contours=<M>
```

VERIFY:
- [ ] `id=` value is an INTEGER (not "null" — ADR-01 replaced the null ML Kit trackingId with BboxIouTracker-assigned integer ID)
- [ ] `id=` value stays CONSTANT across all 60+ seconds of frames logged (BboxIouTracker retained same identity — D-23 monotonic non-recycling assignment)
- [ ] `bb=` values remain stable within natural jitter (expected ±5-10 pixels on a still head — 1€ smoother in effect)

Example PASS output (id=0 stable across all frames):
```
V/FaceTracker: t=1234567890 id=0 bb=362,640 contours=15
V/FaceTracker: t=1234601223 id=0 bb=363,641 contours=15
V/FaceTracker: t=1234634556 id=0 bb=362,640 contours=15
...
```

Additional test — move head OUT of frame for 5+ frames then return:
```
V/FaceTracker: t=1234... id=0 bb=362,640 ...  (last frame before leaving)
[gap while face absent]
V/FaceTracker: t=1235... id=1 bb=365,643 ...  (new id assigned on re-entry — EXPECTED per D-23)
```
ID incrementing after full dropout is CORRECT behavior per D-23 (monotonic, non-recycling). Only a problem if the ID changes while the face is continuously held still.

If `id=null` appears in any frame: ADR-01 follow-up #1 (BboxIouTracker) is not wired. Hard blocker — check `FaceDetectorClient.createAnalyzer()` consumer body.

PASS / FAIL: ________

---

## Step 10 — REN-08 subjective smoothness (30-second hold)

**Soft gate — document as Phase 7 perf gap if fails, but does NOT hard-block Phase 3 sign-off.**

Set ant filter. Hold phone steady showing your face in portrait orientation for **30 seconds**. Observe:
- Bug sprite tracks face smoothly — no jank, stutter, or visible freeze frames
- Head tilts and small movements: bug stays anchored without visible lag (>1 frame drift is a concern)
- Flipbook animation progresses at a visually smooth cadence (~15fps animation, but 30fps preview)

If running Android Studio with device connected: optionally capture a 10-second CPU Profiler trace during this window for Phase 7 PRF-01 baseline reference (File → Profile → attach to com.bugzz.filter.camera).

VERIFY:
- [ ] No visible jank to naked eye over 30 seconds ("gut check" acceptance per D-37)
- [ ] Ant sprite animation does not stutter, freeze, or show repeated frames at a rate noticeable to user

If visible jank: raise as Phase 3 gap per `02-gaps` precedent; investigate whether Canvas `onDrawListener` loop is being pre-empted by GC or heavy ML Kit inference on the same executor thread. Phase 7 formal measurement (PRF-01) will confirm.

PASS / FAIL: ________

---

## Step 11 — CAP-06 LeakCanary 30-capture runbook

**This is a HARD GATE step. Failure blocks Phase 3 sign-off (T-03-04 mitigation insufficient).**

From camera screen with ant filter active on front cam:

**TAP SHUTTER 30 TIMES CONSECUTIVELY.**

Pacing: wait for the "Saved to gallery" Toast after each capture before tapping again (~1s between taps). Count aloud if helpful. If any tap produces an error Toast instead of "Saved to gallery", note the capture number and continue.

After the 30th capture:
1. Wait 5 seconds (let last callback complete)
2. Close app: tap the system Back button until the app closes, then swipe it from Recents
3. Wait 10 seconds (LeakCanary parses heap dumps on relaunch)
4. Relaunch app: `adb shell am start -n com.bugzz.filter.camera/.ui.MainActivity`
5. Wait 10 seconds on the Splash/Home screen
6. Pull down the notification drawer on the phone

VERIFY:
- [ ] **LeakCanary notification is ABSENT from the notification drawer** (no "LeakCanary: 1 retained objects" or similar) — T-03-04 mitigated
- [ ] In Google Photos DCIM/Bugzz/: 30+ photos present from this run (scroll to confirm count)
- [ ] If any Toast was "Error" during the 30 captures: note which numbers; check logcat for `ImageCapture onError` — storage-full condition (T-03-01) vs callback leak

If LeakCanary notification IS PRESENT:
1. Expand it, screenshot the leak trace
2. Attach to `03-SUMMARY.md` under "LeakCanary Findings"
3. Create `03-gaps-0N-PLAN.md` for callback leak investigation before Phase 4 begins

PASS / FAIL: ________

---

## Step 12 — MediaStore Google Photos indexing durability

With app backgrounded for 60 seconds: open Google Photos → DCIM/Bugzz/ album.

VERIFY:
- [ ] All 30+ photos from Step 11 are present and sorted by capture time
- [ ] Each photo is previewable (tap to open — not a broken thumbnail)
- [ ] Tap share on one photo — share sheet populates with available apps (confirms MediaStore URI grant is valid)
- [ ] Photos are correctly dated in Google Photos metadata (not epoch 0 or today-midnight)

PASS / FAIL: ________

---

## Step 13 — Rotation and lens matrix (regression of Phase 2 CAM-07)

Rotate device through 4 orientations with front camera (portrait, landscape-left, reverse-portrait, landscape-right). For each: verify bug sprite stays anchored to face. Then flip to back camera and repeat the orientation test.

| # | Lens | Orientation | Bug sprite aligned? |
|---|------|-------------|---------------------|
| 1 | FRONT | Portrait | PASS / FAIL |
| 2 | FRONT | Landscape-left | PASS / FAIL |
| 3 | FRONT | Reverse-portrait | PASS / FAIL |
| 4 | FRONT | Landscape-right | PASS / FAIL |
| 5 | BACK | Portrait | PASS / FAIL |
| 6 | BACK | Landscape-left | PASS / FAIL |
| 7 | BACK | Reverse-portrait | PASS / FAIL |
| 8 | BACK | Landscape-right | PASS / FAIL |

If any orientation causes bug sprite misalignment: regression from Phase 2 CAM-07 fix — raise as gap. Note the failing combination in sign-off.

PASS / FAIL (N/8): ________

---

## Sign-off

Reply with one of:

**Full pass:** `PASS: 13/13`

**Partial pass:** `PASS: N/13 — failed steps X, Y, Z with <brief description>`

Example: `PASS: 11/13 — failed step 11 (LeakCanary notification appeared: ImageCapture.OnImageSavedCallback retained by CameraController after 30th capture) + failed step 13 orientation 6 (FRONT landscape-left: ant sprite shifted 30px left of nose)`

---

## Phase 3 Gap-Closure Path

If any **hard-gate step** (Steps 5, 6-CAP-02, 9, 11) FAILS:
1. Claude will create `03-gaps-0N-PLAN.md` per Phase 2 gap-closure precedent (02-gaps-01/02/03).
2. Phase 3 sign-off is blocked until gap plan executes + passes re-verification.
3. Do NOT advance to Phase 4 until all hard gates PASS.

If any **soft-gate step** FAILS (Steps 2-mirror, 10-REN-08, 12-durability, 13-individual-orientation):
1. Document finding in 03-SUMMARY.md under "Known Gaps".
2. Create follow-up gap plan if user agrees; otherwise defer to Phase 7 cross-OEM matrix.
3. Phase 3 may still close if user explicitly signs off on the soft-gate failure as deferred.

If ALL 13 steps PASS → Phase 3 is fully signed off. Task 4 (02-VERIFICATION.md CAM-08 row update) runs next.

---

## ADR-01 Follow-up #4 Gate

After user reports PASS on Step 9 (BboxIouTracker ID stability), the orchestrator spawns a continuation agent to execute Plan 03-05 Task 4:

- Update `.planning/phases/02-camera-preview-face-detection-coordinate-validation/02-VERIFICATION.md` CAM-08 row
- Replace Phase 2 relaxed acceptance wording with: "BboxIouTracker-assigned ID stable across 60+ consecutive frames (verified on Xiaomi 13T HyperOS via logcat FaceTracker filter — see 03-HANDOFF.md Step 9). ML Kit native trackingId remains null under CONTOUR_MODE_ALL per ADR-01 — this is the documented baseline, not a bug."
- Add cross-reference: `(Re-verified 2026-04-20 in 03-05-PLAN.md Task 3 — ADR-01 follow-up #4 closed.)`
- Status: ✅ Complete

All 4 ADR-01 follow-up items then close:
- #1 BboxIouTracker implemented: Phase 03-02 (DONE)
- #2 LandmarkSmoother.onFaceLost re-keyed: Phase 03-02 (DONE)
- #3 FaceDetectorClient.createAnalyzer tracker wire: Phase 03-02 (DONE)
- #4 02-VERIFICATION.md CAM-08 doc close: Phase 03-05 Task 4 (PENDING user PASS)

---

*Phase 3 handoff runbook — 13 steps covering REN-07/08, CAP-01/02/04/05/06, ADR-01 #4 (CAM-08), T-03-06 asset provenance.*
*Analogous to `02-HANDOFF.md` format. Phase 3 acceptance gate: 13/13 PASS on Xiaomi 13T (or all hard gates PASS + soft gates documented).*
