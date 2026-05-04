# Phase 5 Handoff — Video Recording + Audio + Insect Filter Free-Placement Mode Device Runbook

**Device:** Xiaomi 13T (HyperOS / MIUI)
**APK:** `app/build/outputs/apk/debug/app-debug.apk` (Plan 05-07 Task 1 produced — 84 MB, 143 unit tests GREEN)
**Debug APK SHA-256:** `b02894742c3ae120a836f5e2108e6a6ef95ab10faba51080a8bdb652fa415afc`
**Reference APK SHA-256 (T-04-02 asset provenance):** `616c6990331ea59f36f135221f15aff2cdb5c290dfde0cd6c91f898fa26e7859`
(Same reference APK used for Phase 3 and Phase 4 sprite extraction — unchanged. Verify it matches `reference/APK_SHA256.txt`.)

---

## Prerequisites (user-side)

1. Phase 4 prerequisites still met:
   - Settings → Additional settings → Developer options → USB debugging: ON
   - "Install via USB": ON (MIUI-specific)
   - "USB debugging (Security settings)": ON (MIUI-specific)
2. USB cable connected between phone and this PC (data-capable, not charge-only).
3. When prompted on phone, tap "Allow" for the RSA fingerprint dialog.
4. `adb` at `C:/Users/Admin/AppData/Local/Android/Sdk/platform-tools/adb.exe` (Phase 1-4 validated path).
5. Google Photos installed + signed in on Xiaomi 13T (for video playback verification in Step 9).
6. `ffmpeg` available in PATH for MP4 frame extraction (Step 9). If absent: play video in Google Photos and skip frame PNG check.
7. Phone NOT in battery-saver or power-saving mode (thermal throttle affects recording; cool device preferred).
8. ~250 MB storage free on device (84 MB APK + video files for test recordings).
9. Previous Bugzz debug build already on device from Phase 4 — new install replaces it automatically with `-r` flag.

Keep an ADB USB connection alive. Enable keep-awake during USB:
```bash
"C:/Users/Admin/AppData/Local/Android/Sdk/platform-tools/adb.exe" shell svc power stayon usb
```

---

## What you are verifying

| Criterion | Description | Hard gate? |
|-----------|-------------|------------|
| **VID-01** | Record button visible + tap starts recording; isRecording guard prevents concurrent double-starts | Yes — Steps 2 + 5 |
| **VID-02** | Filter overlay baked into MP4 (OverlayEffect VIDEO_CAPTURE target works end-to-end) | Yes — Step 9 (ffmpeg frame extract) |
| **VID-03** | Audio synced in output MP4 (subjective playback; formal PRF-03 drift <50ms is Phase 7) | Soft — Step 9 (Google Photos playback) |
| **VID-04** | Manual stop works (tap Record while recording → Finalize → file saved); 60s auto-stop fires DURATION_LIMIT_REACHED | Yes — Steps 7 + 10 |
| **VID-05** | Front-camera recording shows mirrored view in playback (MIRROR_MODE_ON_FRONT_ONLY) | Yes — Step 12 |
| **VID-06** | MP4 saved to `DCIM/Bugzz/` with filename `Bugzz_YYYYMMDD_HHmmss.mp4` | Yes — Step 8 |
| **VID-07** | Recording indicator (red dot blink + MM:SS timer) visible at TopCenter during isRecording=true | Yes — Step 6 |
| **VID-08** | ThermalMonitor logs status changes; if MODERATE+ reached, FilterEngine logcat shows draw frequency drop | Soft — Step 15 (logcat observation) |
| **VID-09** | BackHandler intercepts back press during recording; AlertDialog "Recording in progress" shown; Discard → file deleted | Yes — Step 11 |
| **VID-10** | RECORD_AUDIO lazy permission: first record tap triggers system dialog; deny → rationale Snackbar with "Open Settings" | Yes — Steps 3 + 4 + 5 |
| **MOD-03** | Insect Filter sticker spawns at preview center when entering InsectFilter mode | Yes — Step 13 |
| **MOD-04** | Single-finger drag translates sticker position | Yes — Step 13 |
| **MOD-05** | Two-finger pinch changes sticker scale; clamped [0.3x .. 3.0x] | Yes — Step 13 |
| **MOD-06** | Two-finger rotation changes sticker rotation (free, unbounded) | Yes — Step 13 |
| **MOD-07** | Sticker position/scale/rotation preserved across camera flip + device orientation change | Yes — Step 14 |
| **Bonus 1 (Phase 4 04-HUMAN-UAT #1)** | Multi-face 2-person scene in FaceFilter mode — no crash; primary face gets full filter; secondary gets bbox-center bug | Soft — Step 14 |
| **Bonus 2 (Phase 4 04-HUMAN-UAT #2)** | 30s subjective FPS smoothness during Insect Filter recording | Soft — Step 15 |
| **Regression** | Phase 4 FaceFilter mode + picker still works; Phase 3 photo capture still works | Yes — Step 1 |

**Hard gates** (Steps 1, 2, 3, 4, 5, 6, 7, 8, 9-overlay, 10, 11, 12, 13, 14-MOD-07): failure blocks Phase 5 sign-off — create `05-gaps-0N-PLAN.md` before advancing to Phase 6.

**Soft gates** (Steps 9-audio, 14-bonus, 15): document finding; Phase 5 may still close with user sign-off.

---

## Known expected findings (NOT bugs — flag only if different)

- Debug bounding-box (red rect) + orange contour dots visible BEHIND bug sprite in FaceFilter mode — expected (DebugOverlayRenderer draws on top of FilterEngine per D-27 Phase 3; release build omits debug overlay).
- Recording button is a 56dp red circle at BottomStart (not the Phase 2/4 "TEST RECORD 5s" debug button at BottomCenter) — expected (D-07 production button replaces debug button; shutter circle stays at BottomCenter).
- Picker thumbnails at 50% alpha during recording — expected (D-23 lock-during-record; picker taps ignored while recording).
- Flip button greyed + disabled during recording — expected (D-11).
- InsectFilter sticker gestures unresponsive during recording — expected (D-23 lock sticker drag/pinch/rotate while isRecording=true).
- Shutter button STILL responds during recording — expected (CameraX 1.6 concurrent ImageCapture + VideoCapture; photo gets baked overlay at shutter time; video continues uninterrupted).
- Debug overlay baked into the MP4 frames — expected (OverlayEffect VIDEO_CAPTURE target includes DebugOverlayRenderer output in DEBUG builds; distinguishable from filter overlay because it is red-stroked rect + orange dots).
- Face detection DISABLED in InsectFilter mode — expected (D-05; no red bbox or orange contour dots visible in InsectFilter preview; only sticker visible).
- Black letterbox bars on top/bottom of camera preview — expected (Phase 2 D-10 FIT_CENTER; FILL_CENTER is Phase 6).
- MIUI/HyperOS install blocker "Install unknown apps" — expected first-time; enable for install source and retry.

---

## Step 0 — Verify APK and device connectivity

```bash
ls -la app/build/outputs/apk/debug/app-debug.apk
"C:/Users/Admin/AppData/Local/Android/Sdk/platform-tools/adb.exe" devices
```

Expected:
- APK file listed, size ~84 MB.
- Device output: `<xiaomi-13t-serial>    device` (not `unauthorized` or `offline`).

If `unauthorized`: tap "Allow" on the RSA fingerprint dialog on the phone, re-run.
If `offline`: unplug/replug USB; toggle USB debugging OFF then ON.

---

## Step 1 — Install APK + launch + Phase 4 regression smoke (MOD-01 regression)

```bash
"C:/Users/Admin/AppData/Local/Android/Sdk/platform-tools/adb.exe" install -r app/build/outputs/apk/debug/app-debug.apk
"C:/Users/Admin/AppData/Local/Android/Sdk/platform-tools/adb.exe" shell am start -n com.bugzz.filter.camera/.MainActivity
```

Start logcat in a second terminal (keep running through all steps):
```bash
"C:/Users/Admin/AppData/Local/Android/Sdk/platform-tools/adb.exe" logcat -c
"C:/Users/Admin/AppData/Local/Android/Sdk/platform-tools/adb.exe" logcat > phase5-run.log
```

Expected:
- `Performing Streamed Install ... Success`
- App opens on Splash → Home screen
- Home screen shows: Face Filter (filled/enabled) + Insect Filter (outlined/enabled — Phase 5 enables this button) + Settings gear + My Collection

**Phase 4 regression check:**
- Tap Face Filter → CameraScreen opens with front camera live
- Picker strip visible at bottom with 15 thumbnails
- Spider sprite renders on face (default `spider_nose_static`)
- Tap shutter → JPEG saved → Toast "Saved to gallery"

If CameraScreen crashes: check logcat for Hilt binding errors or `CameraRoute` serialization regression.
If picker is missing: regression in `CameraScreen.kt` Phase 5 edits. Hard blocker.

PASS / FAIL: ________

---

## Step 2 — Record button visible at BottomStart (VID-01)

**This is a HARD GATE step. Failure blocks Phase 5 sign-off.**

In FaceFilter CameraScreen, verify:
- [ ] 56dp red circular Record button visible at BottomStart (24dp from edge), vertically aligned with shutter button
- [ ] Button appearance: filled red circle `#FFE53935` with white 2dp border (idle state)
- [ ] Shutter white circle still at BottomCenter (not replaced)
- [ ] No "TEST RECORD 5s" debug button visible (Phase 5 removes it — if still visible, document but non-blocking)

PASS / FAIL: ________

---

## Step 3 — VID-10 lazy RECORD_AUDIO permission — first tap triggers dialog

**This is a HARD GATE step. Failure blocks Phase 5 sign-off.**

On a FRESH INSTALL (first launch after install, or RECORD_AUDIO never granted):
- Tap the red Record button
- Expected: system permission dialog appears immediately for `RECORD_AUDIO` ("Allow Bugzz to record audio?")

If no dialog appears and recording starts silently: RECORD_AUDIO was pre-granted from a prior install. Test by clearing app data:
```bash
"C:/Users/Admin/AppData/Local/Android/Sdk/platform-tools/adb.exe" shell pm clear com.bugzz.filter.camera
```
Then re-install, launch, enter Face Filter, tap Record again.

PASS / FAIL: ________

---

## Step 4 — VID-10 deny permission flow — rationale Snackbar shown

**This is a HARD GATE step. Failure blocks Phase 5 sign-off.**

After the permission dialog appears (Step 3):
- Tap **Deny**
- Expected: Snackbar "Microphone needed for video sound." with action button "Open Settings"
- Tap "Open Settings" — expected: opens Android App Info settings for Bugzz with Permissions section visible

If no Snackbar appears: `CameraViewModel.onRecordAudioPermissionDenied()` handler missing. Hard blocker.
If "Open Settings" does not open App Info: `Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)` regression.

PASS / FAIL: ________

---

## Step 5 — VID-10 grant permission + VID-01 record starts (VID-01 + VID-10 combined)

**This is a HARD GATE step. Failure blocks Phase 5 sign-off.**

- Tap the Record button again
- Expected: RECORD_AUDIO dialog appears again (second tap after denial re-triggers)
- Tap **Allow**
- Expected: recording starts immediately

Observe:
- [ ] Record button icon changes from filled red circle → white square inside red circle (universal "stop" symbol)
- [ ] Recording indicator appears at TopCenter (see Step 6 for detail)
- [ ] No Toast error; no crash

If tapping Record after denial does NOT re-show the dialog: re-trigger logic missing in `CameraViewModel`. Hard blocker.
If recording starts but no visual change on Record button: `isRecording` state not flowing to UI. Hard blocker.

PASS / FAIL: ________

---

## Step 6 — VID-07 recording indicator — red dot blink + timer counts up

**This is a HARD GATE step. Failure blocks Phase 5 sign-off.**

While recording is active (Step 5 succeeded):
- [ ] Red dot (16dp) visible at TopCenter of the screen, ~24dp from top edge
- [ ] Red dot blinks at approximately 1Hz (alpha oscillates ~1.0 ↔ ~0.5, visible to naked eye)
- [ ] Timer text next to dot shows elapsed time: "00:01" → "00:02" → "00:03" ... updating every second
- [ ] Both dot and timer disappear when recording stops

If timer is frozen at "00:00": `LaunchedEffect(isRecording)` timer loop is not advancing `elapsedMs`. Check `CameraViewModel.recordingState`.

PASS / FAIL: ________

---

## Step 7 — VID-04 manual stop — tap Record → file saved (VID-04 + VID-06 setup)

**This is a HARD GATE step. Failure blocks Phase 5 sign-off.**

While recording is active with an active filter (e.g., "Spider Nose" or any filter):
- At approximately "00:05" on the timer, tap the Record button (now showing white-square-in-red-circle)
- Expected:
  - [ ] Recording indicator (red dot + timer) disappears
  - [ ] Record button returns to idle state (filled red circle)
  - [ ] Toast "Recording saved" appears at the bottom of the screen (brief)
  - [ ] No error Toast; no crash

If recording does not stop on tap: `recording.stop()` not called; check `CameraViewModel.onRecordTapped()` when `isRecording == true`. Hard blocker.
If no Toast: `OneShotEvent.VideoSaved` not emitted or not collected. Hard blocker.

PASS / FAIL: ________

---

## Step 8 — VID-06 MP4 saved to DCIM/Bugzz with correct filename

**This is a HARD GATE step. Failure blocks Phase 5 sign-off.**

After Step 7 (recording saved):
```bash
"C:/Users/Admin/AppData/Local/Android/Sdk/platform-tools/adb.exe" shell ls -la /sdcard/DCIM/Bugzz/*.mp4
```

Expected:
- At least one file matching `Bugzz_YYYYMMDD_HHmmss.mp4` (e.g., `Bugzz_20260504_221600.mp4`)
- File size > 0 (non-empty; a ~5s 720p H264 recording is typically 3–8 MB)

If file is absent from `DCIM/Bugzz/`: check `RELATIVE_PATH` in MediaStore `ContentValues` — must be `"DCIM/Bugzz"` not `"DCIM/Bugzz/"`.
If file has size 0: Recorder finalized with error — check logcat for `VideoRecordEvent.Finalize.error`.

PASS / FAIL: ________

---

## Step 9 — VID-02 + VID-03 overlay and audio baked into MP4

**VID-02 is a HARD GATE. VID-03 (audio sync) is soft — subjective.**

Pull the MP4 from Step 8 and verify:
```bash
"C:/Users/Admin/AppData/Local/Android/Sdk/platform-tools/adb.exe" pull /sdcard/DCIM/Bugzz/Bugzz_<timestamp>.mp4 .
```

**VID-02 — Filter overlay visible in frames:**
If `ffmpeg` is available:
```bash
ffmpeg -i Bugzz_<timestamp>.mp4 -vf "select=eq(n\,15)" -frames:v 1 -vsync vfr frame_15.png
```
Open `frame_15.png` — the bug sprite filter overlay should be visible at frame 15 (approximately 0.5s into a 30fps recording).

If `ffmpeg` is not available: open the MP4 in Google Photos on the phone → pause on any frame → screenshot and verify bug sprite is visible in the paused frame.

Expected:
- [ ] Bug sprite (and debug red-rect bounding box in DEBUG build) visible in the extracted frame
- [ ] Overlay is at the correct position (not offset, not missing)

**VID-03 — Audio sync (subjective):**
Play `Bugzz_<timestamp>.mp4` in Google Photos or via `ffplay Bugzz_<timestamp>.mp4`:
- [ ] Audio is present (not silent)
- [ ] Audio approximately synced with video — no audible 1s+ lead/lag

PASS / FAIL (VID-02): ________
SOFT (VID-03 audio): ________

---

## Step 10 — VID-04 auto-stop at 60s cap (DURATION_LIMIT_REACHED)

**This is a HARD GATE step. Failure blocks Phase 5 sign-off.**

- Tap Record to start a new recording
- Leave the phone running; do NOT tap Record manually
- Wait the full 60 seconds

Expected:
- [ ] Recording indicator timer counts from "00:00" to "01:00"
- [ ] At "01:00" the indicator disappears automatically (no user tap required)
- [ ] Toast "Recording saved" appears
- [ ] New `Bugzz_*.mp4` file appears in `DCIM/Bugzz/` (verify with `adb shell ls /sdcard/DCIM/Bugzz/*.mp4`)

Check logcat for the auto-stop event:
```bash
grep -i "DURATION_REACHED\|durationLimit\|VideoRecordEvent" phase5-run.log | tail -10
```
Expected: a `VideoRecordEvent.Finalize` log line with `cause=ERROR_DURATION_LIMIT_REACHED` or equivalent.

If recording does NOT stop at 60s: `withDurationLimit(60_000_000_000L)` (nanoseconds) may not be set. Hard blocker.

PASS / FAIL: ________

---

## Step 11 — VID-09 BackHandler discard flow — back press during record → AlertDialog → Discard

**This is a HARD GATE step. Failure blocks Phase 5 sign-off.**

- Tap Record → wait ~5 seconds (confirm recording active: indicator shows "00:05")
- Press the **device back button**
- Expected: `AlertDialog` appears with:
  - Title: "Recording in progress"
  - Body: "Are you sure you want to discard this recording?"
  - Two buttons: **Cancel** and **Discard**
- Tap **Cancel** — expected: dialog dismisses; recording CONTINUES (indicator still running)
- Tap Record button again to re-trigger another ~5s
- Press device back again → dialog appears again
- Tap **Discard** — expected:
  - [ ] Dialog dismisses
  - [ ] Recording indicator disappears (recording stopped)
  - [ ] No "Recording saved" Toast (discard means no save)
  - [ ] Pending MP4 file deleted from MediaStore (run `adb shell ls /sdcard/DCIM/Bugzz/*.mp4` — only previously saved files remain; no new zero-byte or partial file)

If back press navigates away without showing dialog: `BackHandler(enabled = isRecording)` not intercepting. Hard blocker.
If Discard does not delete the file: `MediaStore.Files.delete(pendingUri)` missing in `onDiscardRecording`. Hard blocker.

PASS / FAIL: ________

---

## Step 12 — VID-05 front-camera mirror in recording

**This is a HARD GATE step. Failure blocks Phase 5 sign-off.**

**Front camera (default — should already be active):**
- Hold a sheet of paper with text ("LEFT" written on it) in frame
- Tap Record → record for ~5s → stop
- Pull and play the MP4: `adb pull /sdcard/DCIM/Bugzz/Bugzz_<latest>.mp4`
- Expected: the text appears **mirrored** in the video (reads as a mirror image) — matching what you see in the preview

**Back camera (optional but recommended for full verification):**
- Tap the Flip button to switch to back camera
- Tap Record → record for ~5s with the text in frame → stop
- Play the MP4
- Expected: text appears **non-mirrored** (reads normally) — back camera is not mirrored

Expected logcat confirmation:
```bash
grep -i "MIRROR_MODE\|mirrorMode\|MirrorMode" phase5-run.log | head -5
```

PASS / FAIL: ________

---

## Step 13 — MOD-03..06 Insect Filter mode — sticker spawn + drag + pinch + rotate

**This is a HARD GATE step for MOD-03..06. Failure blocks Phase 5 sign-off.**

Navigate to Insect Filter mode:
- Press the device back button (or Home button → navigate back to Home screen)
- HomeScreen → tap **"Insect Filter"** button (should now be ENABLED — filled style — Phase 5 re-wired nav)
- `InsectFilterScreen` opens

**MOD-03 — Sticker spawns at preview center:**
- [ ] Immediately upon entering InsectFilter mode, a bug sticker is visible at the center of the camera preview
- [ ] Sticker uses the last-used filter from Phase 4/5 sessions (DataStore restore)
- [ ] No bounding-box debug overlay (face detection is disabled in InsectFilter mode per D-05)

**MOD-04 — Single-finger drag:**
- Touch and drag the sticker with one finger to the top-left of the preview
- [ ] Sticker translates smoothly with your finger — offset follows pan gesture
- [ ] Sticker stops moving when finger is lifted

**MOD-05 — Two-finger pinch-to-zoom:**
- Place two fingers on the screen and spread them apart
- [ ] Sticker grows larger (scale increases)
- [ ] Pinch inward → sticker shrinks
- [ ] Scale is clamped: cannot shrink past approximately 0.3x original size; cannot grow past approximately 3x
- [ ] Sticker does not jump or snap when gesture starts

**MOD-06 — Two-finger rotation:**
- Place two fingers on the screen and twist (one clockwise, one counter-clockwise)
- [ ] Sticker rotates following the two-finger twist gesture
- [ ] Rotation is smooth; no snap or teleport
- [ ] Rotation is unbounded (can rotate full 360°+ without stopping)

PASS / FAIL (MOD-03): ________
PASS / FAIL (MOD-04): ________
PASS / FAIL (MOD-05): ________
PASS / FAIL (MOD-06): ________

---

## Step 14 — MOD-07 sticker survives flip + orientation change (+ Phase 4 04-HUMAN-UAT Bonus #1: multi-face)

**MOD-07 is a HARD GATE. Multi-face bonus is soft.**

**Setup — place sticker with distinct position/scale/rotation:**
- In InsectFilterScreen, drag sticker to the top-left corner of the preview
- Pinch to approximately 1.5x scale
- Rotate to approximately 45° (diagonal)
- Note the visual position/size/angle

**Flip camera (front ↔ back):**
- Tap the Flip button
- [ ] Camera preview switches (front→back or back→front)
- [ ] Sticker is still visible at the SAME screen position, SAME scale (~1.5x), SAME rotation (~45°)
- [ ] No reset to center or scale 1.0 or rotation 0° after flip

**Orientation change (if app permits — Xiaomi 13T landscape):**
- Rotate the device to landscape mode
- [ ] Sticker remains visible at equivalent position (the transforms are applied in preview-space; some positional shift is expected — verify sticker has not DISAPPEARED or RESET to center/zero)
- Rotate back to portrait
- [ ] Sticker position/scale/rotation preserved again

**Bonus (Phase 4 04-HUMAN-UAT #1) — multi-face 2-person scene in FaceFilter mode:**
- Navigate back to FaceFilter mode (Home → Face Filter)
- Select the SWARM filter (e.g., "Bug B Swarm")
- Bring a second face into frame (printed photo ~8×8 cm, or a second person)
- [ ] No crash when 2 faces detected
- [ ] Primary face (larger bbox) gets full FaceLandmarkMapper anchor bug sprites
- [ ] Secondary face gets bug at bbox-center (mid-forehead area) — visible but without precise landmark positioning
- Document actual outcome in Notes (secondary face rendering is best-effort — soft gate)

PASS / FAIL (MOD-07 flip): ________
PASS / FAIL (MOD-07 orientation): ________
SOFT (multi-face bonus): ________

---

## Step 15 — End-to-end InsectFilter recording + Phase 4 04-HUMAN-UAT Bonus #2 (FPS subjective 30s + VID-08 thermal)

**End-to-end recording and FPS subjective are SOFT gates. VID-08 thermal observation is soft.**

**Start logcat thermal monitor in a second terminal:**
```bash
"C:/Users/Admin/AppData/Local/Android/Sdk/platform-tools/adb.exe" logcat -s ThermalMonitor:V VideoRecorder:V FaceTracker:V OverlayEffect:V CameraVM:I
```

**In InsectFilterScreen:**
- Place the sticker somewhere visible
- Tap the Record button (same recording UI as FaceFilter mode — it shares the same CameraViewModel production record path)
- With recording active:
  - Drag the sticker around for ~30 seconds (gestures DISABLED during record per D-23 — sticker should NOT move)
  - Observe the live preview during this time

**FPS subjective smoothness (Phase 4 04-HUMAN-UAT Bonus #2):**
- [ ] Preview does NOT visibly stutter or freeze during the 30s observation
- [ ] Sticker remains frozen at last gesture position (correct — gestures locked during recording)

After 30s, stop the recording:
- Tap Record → Toast "Recording saved" → pull MP4 and verify:
  ```bash
  "C:/Users/Admin/AppData/Local/Android/Sdk/platform-tools/adb.exe" pull /sdcard/DCIM/Bugzz/Bugzz_<latest>.mp4 .
  ffmpeg -i Bugzz_<latest>.mp4 -vf "select=eq(n\,15)" -frames:v 1 -vsync vfr insect_frame_15.png
  ```
- [ ] `insect_frame_15.png` shows the sticker at the user-placed position (baked into video)
- [ ] Audio present in the MP4 (play in Google Photos)

**VID-08 — ThermalMonitor observation:**
Check the thermal logcat output during the 30s+ recording:
- [ ] `ThermalMonitor` logcat lines present — confirms listener registered
- [ ] If device stays LIGHT/NONE: normal; note "no thermal throttle observed"
- [ ] If `MODERATE` or above reached: logcat should show `FaceTracker` logcat draw frequency dropping (~50% skip rate); note the status level
- Document thermal status observed in the Notes column

SOFT (FPS smoothness): ________
SOFT (InsectFilter record + baked sticker): ________
SOFT (VID-08 thermal): ________

---

## Logcat filter cheatsheet

Use during any step to filter relevant tags:
```bash
"C:/Users/Admin/AppData/Local/Android/Sdk/platform-tools/adb.exe" logcat -s ThermalMonitor:V VideoRecorder:V FaceTracker:V OverlayEffect:V CameraVM:I InsectFilterVM:I
```

For crash diagnosis:
```bash
grep -E "FATAL|AndroidRuntime|Exception" phase5-run.log | tail -30
```

For recording lifecycle events:
```bash
grep -i "VideoRecordEvent\|DURATION_REACHED\|startRecording\|stopRecording\|pendingUri" phase5-run.log | tail -20
```

For filter overlay render health:
```bash
grep -i "OverlayEffect\|filter=\|faces=\|draws=" phase5-run.log | tail -20
```

---

## OEM Quirks (Xiaomi 13T / HyperOS / MIUI)

Document anything unexpected here. Do NOT fix — record for Phase 7 cross-OEM matrix.

- 
- 

---

## Final sign-off table

| Step | Requirement | Outcome | Notes |
|------|-------------|---------|-------|
| 0 | APK + device connectivity | PASS / FAIL | |
| 1 | Install + launch + Phase 4 regression (FaceFilter + picker + shutter) | PASS / FAIL | |
| 2 | VID-01: Record button visible at BottomStart, idle state red circle | PASS / FAIL | |
| 3 | VID-10: First tap → RECORD_AUDIO system permission dialog | PASS / FAIL | |
| 4 | VID-10: Deny → Snackbar + "Open Settings" action | PASS / FAIL | |
| 5 | VID-01 + VID-10: Grant → recording starts; button changes to stop icon | PASS / FAIL | |
| 6 | VID-07: Red dot blinks + timer counts up at TopCenter | PASS / FAIL | |
| 7 | VID-04: Manual stop → recording indicator disappears + "Recording saved" Toast | PASS / FAIL | |
| 8 | VID-06: MP4 at `DCIM/Bugzz/Bugzz_*.mp4`, size > 0 | PASS / FAIL | |
| 9 | VID-02: Filter overlay visible in extracted frame (hard) + VID-03: audio present + synced (soft) | PASS / FAIL | |
| 10 | VID-04: 60s auto-stop → timer reaches "01:00" → indicator disappears → Toast | PASS / FAIL | |
| 11 | VID-09: Back during record → AlertDialog → Cancel resumes → Discard stops + deletes pending file | PASS / FAIL | |
| 12 | VID-05: Front-cam MP4 mirrored; back-cam non-mirrored | PASS / FAIL | |
| 13 | MOD-03..06: Sticker spawns center + drag + pinch + rotate all respond | PASS / FAIL | |
| 14 | MOD-07: Sticker survives flip + orientation (hard) + multi-face no-crash bonus (soft) | PASS / FAIL | |
| 15 | InsectFilter end-to-end record + FPS smooth 30s (soft) + VID-08 thermal logcat (soft) | SOFT | |

**Hard gate summary:**
- Hard gates: Steps 1–14 (excluding soft annotations in Step 9 audio, Step 14 multi-face bonus, Step 15 all soft)
- All hard gates PASS: **Yes / No**

---

## Post-PASS instructions

When sign-off table shows all hard gates PASS (Steps 1–14):

1. Reply `PASS — proceed to close-out` with:
   - How many steps passed (e.g., "15/15 PASS" or "14/15 — Step 15 soft")
   - Any OEM quirk notes
   - Logcat snippet for VID-08 thermal observation (if triggered)

2. The continuation executor agent will then:
   a. Flip `05-VALIDATION.md` frontmatter `nyquist_compliant: false` → `true` and `wave_0_complete: false` → `true`
   b. Add approval line: `**Approval:** approved YYYY-MM-DD per Xiaomi 13T 15/15 sign-off`
   c. Write `05-07-SUMMARY.md` covering all 7 Phase 5 plans
   d. Run `gsd-tools roadmap update-plan-progress 05`
   e. Update `STATE.md` Current Position: Phase 5 complete → Phase 6 UX Polish next
   f. Update `REQUIREMENTS.md` — mark VID-01..10 + MOD-03..07 complete
   g. Commit all state docs + SUMMARY

**If any hard gate FAILS:** Do NOT close Phase 5. File `05-gaps-0N-PLAN.md` per Phase 2/3/4 gap-closure precedent. Describe failing step + root cause. Re-run 05-HANDOFF after gap-closure.

---

## Phase 5 Gap-Closure Path

If any **hard-gate step** (Steps 1–14, excluding soft annotations) FAILS:
1. Claude creates `05-gaps-0N-PLAN.md` per Phase 2/3/4 gap-closure precedent.
2. Phase 5 sign-off blocked until gap plan executes + passes re-verification.
3. Do NOT advance to Phase 6 until all hard gates PASS.

If any **soft-gate** FAILS (Step 9 audio, Step 14 multi-face, Step 15):
1. Document finding in sign-off table Notes column.
2. Phase 5 may still close if user agrees to defer to Phase 7 cross-OEM matrix.

If ALL hard gates PASS + user signs off → Phase 5 fully signed off. Task 4 (VALIDATION flip + SUMMARY + STATE/ROADMAP/REQUIREMENTS updates) runs next.

---

*Phase 5 handoff runbook — 15 steps covering VID-01..10 + MOD-03..07 (15 REQs) + Phase 4 04-HUMAN-UAT 2 deferred items as bonus checks.*
*Follows `04-HANDOFF.md` format. Phase 5 acceptance gate: all hard gates PASS on Xiaomi 13T (or hard gates PASS + soft gates documented with user agreement).*
*APK: `app/build/outputs/apk/debug/app-debug.apk` (84 MB, Plan 05-07 Task 1, 143 unit tests GREEN).*
