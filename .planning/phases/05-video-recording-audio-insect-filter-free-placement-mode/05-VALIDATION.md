---
phase: 05
slug: video-recording-audio-insect-filter-free-placement-mode
status: draft
nyquist_compliant: false
wave_0_complete: false
created: 2026-05-04
---

# Phase 05 — Validation Strategy

> Per-phase validation contract. Extends Phase 4 JUnit 4 + Robolectric 4.13 + Mockito-Kotlin + Turbine harness with 5 new test files + 2 extensions. Phase 5 is the LAST production feature phase before Phase 6 UX polish.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit 4.13.2 + Mockito 5.11 + Mockito-Kotlin + Robolectric 4.13 + Turbine + DataStore test factory (all from Phase 4) |
| **Config file** | `gradle/libs.versions.toml` + `app/build.gradle.kts` (no new deps Phase 5) |
| **Quick run command** | `./gradlew :app:testDebugUnitTest` (with `JAVA_HOME="/c/Program Files/Android/Android Studio/jbr"` in bash env) |
| **Full suite command** | `./gradlew :app:testDebugUnitTest :app:assembleDebug` |
| **Estimated runtime** | ~75 seconds (Phase 4 baseline 60s + 5 new test classes) |

---

## Sampling Rate

- **After every task commit:** Run `./gradlew :app:testDebugUnitTest`
- **After every wave merge:** Run `./gradlew :app:testDebugUnitTest :app:assembleDebug`
- **Before `/gsd-verify-work`:** Full suite green AND clean debug APK builds AND manual device acceptance per 05-HANDOFF.md on Xiaomi 13T
- **Max feedback latency:** 90 seconds

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Threat Ref | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|------------|-----------|-------------------|-------------|--------|
| 05-01-NN | 01 (Wave 0 scaffolds) | 0 | VID-01..10, MOD-03..07 (test scaffolds) | T-05-01..07 | unit scaffolds RED | `./gradlew :app:testDebugUnitTest` | ❌ W0 creates | ⬜ |
| 05-02-NN | 02 (ThermalMonitor + VideoRecorder infra) | 1 | VID-08 + VID-04 architectural | T-05-05 (listener leak), T-05-02 (storage) | unit + Robolectric | `./gradlew :app:testDebugUnitTest --tests "*ThermalMonitor* *VideoRecorder*"` | ❌ W0 → ✅ W1 | ⬜ |
| 05-03-NN | 03 (StickerState + InsectFilterViewModel + StickerRenderer) | 1 | MOD-03..07 | T-05-04 (gesture lifecycle) | unit (pure JVM) + Robolectric | `./gradlew :app:testDebugUnitTest --tests "*StickerStateTest* *InsectFilterViewModelTest* *StickerRendererTest*"` | ❌ W0 → ✅ W1 | ⬜ |
| 05-04-NN | 04 (CameraController.startRecording + CameraViewModel record lifecycle) | 2 | VID-01..04, VID-09 | T-05-03 (concurrent record), T-05-07 (orphan file) | unit + Robolectric | `./gradlew :app:testDebugUnitTest --tests "*CameraControllerTest*recording* *CameraViewModelTest*onRecord*"` | ❌ W0 → ✅ W2 | ⬜ |
| 05-05-NN | 05 (CameraScreen Record button + indicator + AlertDialog + lock-during-record) | 3 | VID-07, VID-09, VID-11 (lock UI) | T-05-04 | unit + manual device | 05-HANDOFF + VM tests | ❌ W0 → ✅ W3 | ⬜ |
| 05-06-NN | 06 (InsectFilterScreen + nav rewire) | 3 | MOD-03..07 | T-05-04 | unit + manual device | 05-HANDOFF + InsectFilterViewModelTest | ❌ W0 → ✅ W3 | ⬜ |
| 05-07-NN | 07 (RECORD_AUDIO permission flow) | 3 | VID-10, VID-03 | T-05-01 | unit + manual device | 05-HANDOFF + VM tests | ❌ W0 → ✅ W3 | ⬜ |
| 05-08-NN | 08 (Clean build + 05-HANDOFF + device runbook) | 4 | All Phase 5 reqs device-verified | T-05-01..07 | manual (device) | `./gradlew :app:assembleDebug` + 05-HANDOFF.md sign-off | ❌ W0 → ✅ W4 | ⬜ |

*Task IDs (05-NN-MM) finalize when PLAN.md files land — planner may split. This map pins plan↔requirement↔test mapping.*

### Per-Requirement Test Specification

| Req ID | Behavior | Test Type | Automated Command | File |
|--------|----------|-----------|-------------------|------|
| VID-01 | Record button onRecordTapped → CameraController.startRecording invoked; isRecording guard prevents concurrent | unit | `./gradlew :app:testDebugUnitTest --tests "*CameraViewModelTest*onRecordTapped*"` | ❌ W0 — extend `CameraViewModelTest.kt` |
| VID-02 | Overlay baked into MP4 (filter + DebugOverlayRenderer if BuildConfig.DEBUG) | **manual (device)** | 05-HANDOFF: record 5s → pull MP4 → ffmpeg frame extract → confirm overlay visible | 05-HANDOFF Step |
| VID-03 | Audio synced in output (drift target <50ms over 60s — Phase 7 measures formally) | **manual (device)** | 05-HANDOFF: subjective sync check via playback | 05-HANDOFF Step |
| VID-04 | `Recorder.PendingRecording.withDurationLimit(60_000_000_000L)` set; finalize event with cause=DURATION_REACHED at 60s | unit (Robolectric) + manual | `./gradlew :app:testDebugUnitTest --tests "*VideoRecorderTest*durationLimit*"` + 05-HANDOFF | ❌ W0 — `VideoRecorderTest.kt` |
| VID-05 | `MIRROR_MODE_ON_FRONT_ONLY` set on VideoCapture.Builder | unit (Robolectric) | `./gradlew :app:testDebugUnitTest --tests "*CameraControllerTest*videoCaptureHasMirrorMode*"` | Extend existing `CameraControllerTest.kt` |
| VID-06 | MP4 saved to `DCIM/Bugzz/` with filename pattern `Bugzz_YYYYMMDD_HHmmss.mp4` | **manual (device)** | 05-HANDOFF: `adb shell ls /sdcard/DCIM/Bugzz/*.mp4` | 05-HANDOFF Step |
| VID-07 | Recording indicator (red dot blink + timer "MM:SS") visible during isRecording=true | unit (state logic only — Compose visual deferred) | `./gradlew :app:testDebugUnitTest --tests "*CameraViewModelTest*recordingState*"` | ❌ W0 — extend |
| VID-08 | `ThermalMonitor.status >= MODERATE` → FaceDetectorClient skips every other frame | unit (mock thermal status) | `./gradlew :app:testDebugUnitTest --tests "*ThermalMonitorTest*"` | ❌ W0 — `ThermalMonitorTest.kt` |
| VID-09 | BackHandler intercepts back press during recording; AlertDialog shown; Discard → recording.stop() + delete pending file | unit (state) + manual device | `./gradlew :app:testDebugUnitTest --tests "*CameraViewModelTest*onDiscardRecording*"` + 05-HANDOFF | ❌ W0 — extend |
| VID-10 | RECORD_AUDIO permission requested lazily on first record tap; denial shows rationale; subsequent record tap retries | unit (mock permission state) + **manual (device)** | `./gradlew :app:testDebugUnitTest --tests "*CameraViewModelTest*permission*"` + 05-HANDOFF | ❌ W0 — extend + 05-HANDOFF Step |
| MOD-03 | Insect sticker spawns at preview center on entering InsectFilter mode | unit (pure JVM) | `./gradlew :app:testDebugUnitTest --tests "*StickerStateTest*initialPosition*"` | ❌ W0 — `StickerStateTest.kt` |
| MOD-04 | Drag gesture mutates StickerState.offset proportionally to pan amount | unit | `./gradlew :app:testDebugUnitTest --tests "*StickerStateTest*drag*"` | ❌ W0 |
| MOD-05 | Pinch-to-zoom mutates StickerState.scale; clamped to [0.3..3.0] | unit | `./gradlew :app:testDebugUnitTest --tests "*StickerStateTest*pinch*"` | ❌ W0 |
| MOD-06 | Two-finger rotation mutates StickerState.rotation (mod 360°) | unit | `./gradlew :app:testDebugUnitTest --tests "*StickerStateTest*rotate*"` | ❌ W0 |
| MOD-07 | StickerState survives camera flip (CameraController.flipLens preserves VM state) and orientation change (Modifier.graphicsLayer transforms preview-space) | unit + **manual (device)** | `./gradlew :app:testDebugUnitTest --tests "*InsectFilterViewModelTest*stickerState_survivesFlip*"` + 05-HANDOFF | ❌ W0 — `InsectFilterViewModelTest.kt` + 05-HANDOFF Step |

---

## Wave 0 Requirements

New test files required (all pure JVM unless Robolectric noted):

- [ ] `app/src/test/java/com/bugzz/filter/camera/recording/VideoRecorderTest.kt` — Robolectric (Recorder construction); covers durationLimit, MediaStoreOutputOptions builder shape, audioEnabled toggle behavior (VID-04, VID-05)
- [ ] `app/src/test/java/com/bugzz/filter/camera/thermal/ThermalMonitorTest.kt` — pure JVM; covers `addThermalStatusListener` registration, `removeThermalStatusListener` on cleanup (T-05-05), frame-skip logic with mock ThermalStatus values (VID-08)
- [ ] `app/src/test/java/com/bugzz/filter/camera/ui/insect/StickerStateTest.kt` — pure JVM; covers initial-center spawn, drag offset accumulation, pinch scale clamp [0.3, 3.0], rotation mod 360, offset clamp with 50% overflow (MOD-03..06)
- [ ] `app/src/test/java/com/bugzz/filter/camera/ui/insect/InsectFilterViewModelTest.kt` — pure JVM (mock CameraController + FilterEngine + DataStore); covers gesture event → state mutation (MOD-04..06), state survival across flipLens (MOD-07), DataStore restore last-used filter on bind, face detection bypass (Insect mode does NOT attach MlKitAnalyzer)
- [ ] `app/src/test/java/com/bugzz/filter/camera/render/StickerRendererTest.kt` — Robolectric (Canvas mock); covers `setStickerState` mutation, `setActiveFilter` filter source switch, `onDraw` applies translate + scale + rotate transforms in correct order (per RESEARCH §Architecture Pattern 2)
- [ ] **EXTEND** `app/src/test/java/com/bugzz/filter/camera/camera/CameraControllerTest.kt` with `bind_videoCaptureHasMirrorMode` + `startRecording_durationLimitSet` + `startRecording_audioFlagToggle` + `stopRecording_invokesRecordingStop` (VID-04, VID-05 architectural)
- [ ] **EXTEND** `app/src/test/java/com/bugzz/filter/camera/ui/camera/CameraViewModelTest.kt` with `onRecordTapped_startsRecording_emitsActiveState` + `onRecordTapped_isRecording_returnEarly` + `onDiscardRecording_stopsAndDeletesPending` + `recordingState_timerIncrementsBy1Sec` + `lockUI_duringRecording_disablesPickerAndFlip` + `permissionDenied_showsRationale` (VID-01, VID-07, VID-09, VID-10, VID-11 lock UI)

**Deps additions:** none (Phase 4 already catalogued Coil, DataStore, Turbine; CameraX 1.6 + camera-video already from Phase 2).

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| Filter overlay baked into MP4 | VID-02 | Real CameraX VideoCapture pipeline + ffmpeg frame extraction can't reproduce in unit harness | 05-HANDOFF: record 5s of any filter → `adb pull` MP4 → `ffmpeg -i video.mp4 -vf "select=eq(n\,15)" frame.png` → confirm overlay visible at extracted frame |
| Audio sync drift | VID-03 | Real microphone + real Recorder mux are required; subjective playback is acceptable for Phase 5; Phase 7 PRF-03 measures formally | 05-HANDOFF: record 60s with talking face → playback in Google Photos → confirm audio not visibly out-of-sync from lips/movement |
| MP4 saved to DCIM/Bugzz/ + Google Photos indexing | VID-06 | MediaStore IS_PENDING transaction + Google Photos scanner are device-side | 05-HANDOFF: record → wait 5s → `adb shell ls /sdcard/DCIM/Bugzz/Bugzz_*.mp4` → also open Google Photos → confirm thumbnail appears |
| Lazy RECORD_AUDIO permission | VID-10 | Real Android runtime permission dialog can't be reliably automated | 05-HANDOFF: fresh install → tap Record → permission dialog appears → tap Deny → tap Record again → rationale shown → tap Open Settings → grant in settings → return → tap Record → recording starts with audio |
| Multi-touch sticker gestures | MOD-04..06 | Multi-touch pinch + rotate require physical touchscreen with 2 simultaneous pointers | 05-HANDOFF: enter Insect Filter mode → drag sticker around (1 finger) → pinch to zoom (2 fingers spread/squeeze) → rotate (2 fingers twist) → confirm sticker responds to each gesture independently |
| Sticker state survives flip + orientation | MOD-07 | Real camera flip + sensor rotation events on device | 05-HANDOFF: place sticker top-left at scale 1.5x rotation 45° → flip front/back camera → confirm sticker stays at same screen position with same scale/rotation → rotate device to landscape → confirm sticker preserved |
| ThermalStatusListener throttle behavior | VID-08 | Real device thermal stress can't be simulated reliably | 05-HANDOFF: record 60s + run intensive task in background → use logcat to observe ThermalMonitor.status changes → if device hits MODERATE+, observe FilterEngine logcat draws frequency drop ~50% |
| End-to-end Insect Filter video record | integration | Combines all features | 05-HANDOFF: enter Insect mode → place sticker → tap Record → record 10s with sticker visible + audio → stop → playback MP4 → confirm sticker in video at user-placed position with audio |

---

## Validation Sign-Off

- [ ] All tasks have `<automated>` verify or Wave 0 dependencies
- [ ] Sampling continuity: no 3 consecutive tasks without automated verify
- [ ] Wave 0 covers all MISSING test references (5 new + 2 extensions)
- [ ] No watch-mode flags
- [ ] Feedback latency < 90s (75s Gradle baseline)
- [ ] `nyquist_compliant: true` set in frontmatter — flipped from `false` to `true` in plan-phase step 13 after planner emits `<automated>` blocks for every task

**Approval:** pending
