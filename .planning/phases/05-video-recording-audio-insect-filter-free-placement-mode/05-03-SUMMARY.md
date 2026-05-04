---
phase: 05
plan: 03
subsystem: video-recording-lifecycle
tags: [video-recorder, camera-controller, recording-state, overlay-effect, cameramode-branch, tdd]
dependency_graph:
  requires: [05-02-SUMMARY]
  provides: [VideoRecorder, CameraController.startRecording, CameraViewModel.onRecordTapped, OverlayEffectBuilder.cameraMode]
  affects: [CameraScreen, InsectFilterViewModel, InsectFilterViewModel.bind (Plan 05-02 stub closed)]
tech_stack:
  added: []
  patterns:
    - VideoRecorder @Singleton wrapping CameraX Recorder with check(activeRecording == null) double-defense
    - setDurationLimitMillis on MediaStoreOutputOptions.Builder (not on PendingRecording — CameraX 1.6 actual API)
    - CameraController constructor-split seam extended with VideoRecorder parameter
    - OverlayEffectBuilder @Volatile cameraMode field for cross-thread renderer selection
    - handleVideoEvent private callback dispatched from cameraExecutor to viewModelScope.launch (main)
    - pendingDiscardFlag @Volatile field for T-05-07 MediaStore URI deletion on discard
key_files:
  created:
    - app/src/main/java/com/bugzz/filter/camera/recording/VideoRecorder.kt
  modified:
    - app/src/main/java/com/bugzz/filter/camera/camera/CameraController.kt
    - app/src/main/java/com/bugzz/filter/camera/ui/camera/CameraUiState.kt
    - app/src/main/java/com/bugzz/filter/camera/ui/camera/CameraViewModel.kt
    - app/src/main/java/com/bugzz/filter/camera/ui/camera/OneShotEvent.kt
    - app/src/main/java/com/bugzz/filter/camera/ui/camera/CameraScreen.kt
    - app/src/main/java/com/bugzz/filter/camera/ui/insect/InsectFilterViewModel.kt
    - app/src/main/java/com/bugzz/filter/camera/render/OverlayEffectBuilder.kt
    - app/src/test/java/com/bugzz/filter/camera/recording/VideoRecorderTest.kt
    - app/src/test/java/com/bugzz/filter/camera/camera/CameraControllerTest.kt
    - app/src/test/java/com/bugzz/filter/camera/ui/camera/CameraViewModelTest.kt
decisions:
  - "setDurationLimitMillis(60_000L) on MediaStoreOutputOptions.Builder is the CameraX 1.6 API for 60s cap; PendingRecording.withDurationLimit() does not exist — plan spec incorrect, Rule 3 auto-fix applied"
  - "ERROR_DURATION_LIMIT_REACHED (not ERROR_DURATION_REACHED) is the correct CameraX 1.6 constant name — Rule 3 auto-fix applied in both CameraViewModel and InsectFilterViewModel"
  - "CameraController.bind() gained a 4th cameraMode parameter (default FaceFilter) — CameraViewModelTest.rapidSelectFilter_noCameraRebind updated from bind(any,any,any) to bind(any,any,any,any) — Rule 1 auto-fix"
  - "OverlayEffectBuilder DebugOverlayRenderer draw now gated on faces.isNotEmpty() — no behavioral change since InsectFilter mode has no faces; FaceFilter mode debug diagnostic behavior preserved"
  - "RecordingState consumed from Plan 05-02 canonical type — no rewrite, no placeholder churn (WARNING 6 closure confirmed)"
metrics:
  duration_seconds: 938
  completed_date: "2026-05-04"
  tasks_completed: 3
  tasks_total: 3
  files_created: 1
  files_modified: 10
---

# Phase 05 Plan 03: VideoRecorder + CameraController Recording API + OverlayEffectBuilder cameraMode Summary

**One-liner:** Production video recording lifecycle wired — VideoRecorder @Singleton with 60s cap, CameraController startRecording/stopRecording, CameraViewModel onRecordTapped/handleVideoEvent, OverlayEffectBuilder cameraMode branch — 15 tests un-Ignored GREEN.

---

## What Was Built

### Task 1 — VideoRecorder + CameraController production recording API

| Component | What landed |
|-----------|-------------|
| `VideoRecorder.kt` | `@Singleton @Inject constructor(appContext, cameraExecutor)`. `startRecording(videoCapture, audioEnabled, onEvent): Recording` — builds `MediaStoreOutputOptions` with `setDurationLimitMillis(60_000L)` (D-09), `DCIM/Bugzz`, `Bugzz_YYYYMMDD_HHmmss.mp4` (D-17), conditional `withAudioEnabled()` (D-12/D-18). `check(activeRecording == null)` guards re-entrance (T-05-03). `stopRecording()` + `clearActive()` lifecycle hooks. |
| `CameraController.kt` | Injected `VideoRecorder` via constructor-split (5th param). `startRecording(audioEnabled, onEvent): Recording` delegates to `VideoRecorder`. `stopRecording()` + `clearActiveRecording()` delegates. `val contentResolver` exposed for T-05-07 URI deletion. `bind()` gained `cameraMode: CameraMode = FaceFilter` 4th param; sets `overlayEffectBuilder.cameraMode`; gates `MlKitAnalyzer` attachment on `cameraMode == FaceFilter` (D-05). `MIRROR_MODE_ON_FRONT_ONLY` preserved on `VideoCapture.Builder` (D-13/VID-05). |
| `VideoRecorderTest.kt` | 5 tests un-Ignored GREEN: `durationLimit_setTo60Seconds`, `startRecording_audioEnabledTrue_callsWithAudioEnabled`, `startRecording_audioEnabledFalse_omitsWithAudioEnabled`, `stopRecording_invokesRecordingStop`, `clearActive_resetsActiveRecording_allowsNextStart` |
| `CameraControllerTest.kt` | 4 tests un-Ignored GREEN: `bind_videoCaptureHasMirrorMode`, `startRecording_durationLimitSet`, `startRecording_audioEnabledFlagToggle`, `stopRecording_invokesRecordingStop` |

### Task 2 — CameraViewModel recording lifecycle + OneShotEvent + CameraUiState

| Component | What landed |
|-----------|-------------|
| `OneShotEvent.kt` | Added `VideoSaved(uri: Uri)` + `VideoError(message: String)` variants. All Phase 3 variants preserved. |
| `CameraUiState.kt` | Added `recordingState: RecordingState = RecordingState.Idle` field. Added `val isActivelyRecording: Boolean` computed property (`recordingState is Active`). RecordingState imported from canonical Plan 05-02 type (WARNING 6 closure). |
| `CameraViewModel.kt` | `onRecordTapped(audioEnabled)` with `recordingState !is RecordingState.Idle` guard (D-26). `onStopRecording()`. `onDiscardRecording()` sets `pendingDiscardFlag = true`. `handleVideoEvent()` private method: `Start` confirms Active state; `Status` increments `elapsedMs`; `Finalize` clears recording, handles discard delete (T-05-07), `ERROR_DURATION_LIMIT_REACHED` treated as normal stop (D-09), emits `VideoSaved` or `VideoError`. |
| `InsectFilterViewModel.kt` | Mirror of CameraViewModel recording lifecycle added: `onRecordTapped` / `onStopRecording` / `onDiscardRecording` / `handleVideoEvent`. `events: Flow<OneShotEvent>` channel added. |
| `CameraScreen.kt` | Added `VideoSaved` + `VideoError` branches to exhaustive `when(event)` (Rule 2 — compiler forced). |
| `CameraViewModelTest.kt` | 6 Phase 5 tests un-Ignored GREEN: `onRecordTapped_idle_startsRecording_emitsActiveState`, `onRecordTapped_alreadyActive_returnsEarly`, `onDiscardRecording_stopsAndDeletesPendingUri`, `recordingState_statusEvent_incrementsElapsedMs`, `lockUI_duringRecording_pickerAlphaAndFlipDisabled`, `onRecordTapped_audioPermissionDenied_emitsRationaleEvent` |

### Task 3 — OverlayEffectBuilder cameraMode branch

| Component | What landed |
|-----------|-------------|
| `OverlayEffectBuilder.kt` | Injected `StickerRenderer`. Added `@Volatile var cameraMode: CameraMode = FaceFilter`. `setOnDrawListener` body branches: `when(cameraMode) { FaceFilter -> filterEngine.onDraw(…); InsectFilter -> stickerRenderer.onDraw(…) }`. `canvas.setMatrix(frame.sensorToBufferTransform)` preserved before all draws (CAM-07/PITFALLS #5). `BuildConfig.DEBUG` gate preserved. |

---

## Acceptance Criteria Verification

| Check | Result |
|-------|--------|
| `setDurationLimitMillis` in VideoRecorder.kt | 2 occurrences |
| `MediaStore.Video.Media.EXTERNAL_CONTENT_URI` in VideoRecorder.kt | 1 |
| `DCIM/Bugzz` in VideoRecorder.kt | 2 |
| `Bugzz_` filename pattern in VideoRecorder.kt | 2 |
| `check(activeRecording == null)` in VideoRecorder.kt | 1 |
| `fun startRecording` in CameraController.kt | 1 |
| `fun stopRecording` in CameraController.kt | 1 |
| `MIRROR_MODE_ON_FRONT_ONLY` in CameraController.kt | 2 |
| `MirrorMode` in CameraController.kt | 2 |
| `imageCaptureFactory\|providerFactory` in CameraController.kt | 6 |
| `val contentResolver` in CameraController.kt | 1 |
| `data class VideoSaved` in OneShotEvent.kt | 1 |
| `data class VideoError` in OneShotEvent.kt | 1 |
| `fun onRecordTapped` in CameraViewModel.kt | 1 |
| `fun onDiscardRecording` in CameraViewModel.kt | 1 |
| `pendingDiscardFlag` in CameraViewModel.kt | 5 |
| `ERROR_DURATION_LIMIT_REACHED` in CameraViewModel.kt | 3 (CameraVM + InsectVM) |
| `recordingState !is RecordingState.Idle` (isRecording guard) | 1 |
| Phase 3 fix: `isCapturing` in CameraViewModel.kt | 5 |
| Phase 3 fix: `bindJob` in CameraViewModel.kt | 3 |
| Phase 3 fix: `FilterLoadError` in CameraViewModel.kt | 6 |
| Phase 3 fix: `captureFlash` in CameraViewModel.kt | 4 |
| Phase 4 fix: `assetLoader.preload(*.assetDir)` in CameraViewModel.kt | 2 |
| Phase 3 fix: `require(def.frameCount > 0)` in StickerRenderer.kt | 1 |
| `stickerRenderer` in OverlayEffectBuilder.kt | 2 |
| `cameraMode` in OverlayEffectBuilder.kt | 3 |
| `when (cameraMode)` in OverlayEffectBuilder.kt | 1 |
| `canvas.setMatrix` in OverlayEffectBuilder.kt | 2 |
| `BuildConfig.DEBUG` in OverlayEffectBuilder.kt | 2 |
| `cameraMode: CameraMode\|cameraMode = CameraMode` in CameraController.kt | 1 |
| Full test suite: 143 tests GREEN | PASSED |
| `assembleDebug` exits 0 | PASSED |

---

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] `withDurationLimit` does not exist on `PendingRecording` in CameraX 1.6**

- **Found during:** Task 1 first compile attempt
- **Issue:** Plan spec referenced `PendingRecording.withDurationLimit(60_000L, TimeUnit.MILLISECONDS)`. This method does not exist in CameraX 1.6. The actual API is `OutputOptions.Builder.setDurationLimitMillis(long)` on the `MediaStoreOutputOptions.Builder`.
- **Fix:** Moved duration cap to `MediaStoreOutputOptions.Builder.setDurationLimitMillis(60_000L)` in `VideoRecorder.startRecording()`.
- **Files modified:** `VideoRecorder.kt`, `VideoRecorderTest.kt` (test updated to verify via `prepareRecording` invocation instead of `withDurationLimit` spy)
- **Commits:** `53b9c45`

**2. [Rule 3 - Blocking] `VideoRecordEvent.Finalize.ERROR_DURATION_REACHED` does not exist in CameraX 1.6**

- **Found during:** Task 2 compile attempt
- **Issue:** Plan spec used `ERROR_DURATION_REACHED`. Actual constant name is `ERROR_DURATION_LIMIT_REACHED`.
- **Fix:** Replaced in both `CameraViewModel.kt` and `InsectFilterViewModel.kt`.
- **Files modified:** `CameraViewModel.kt`, `InsectFilterViewModel.kt`
- **Commit:** `dffc21e`

**3. [Rule 1 - Bug] `CameraScreen.kt` `when(OneShotEvent)` compile failure after new variants added**

- **Found during:** Task 2 compile attempt
- **Issue:** `CameraScreen.kt` has an exhaustive `when` on `OneShotEvent`. Adding `VideoSaved` and `VideoError` variants caused a compile error "when expression must be exhaustive."
- **Fix:** Added `VideoSaved` → Toast "Video saved to gallery" and `VideoError` → Toast "Video error: …" branches to the `when` block.
- **Files modified:** `CameraScreen.kt`
- **Commit:** `dffc21e`

**4. [Rule 1 - Bug] `CameraViewModelTest.rapidSelectFilter_noCameraRebind` arity mismatch**

- **Found during:** Task 3 full test run
- **Issue:** `CameraController.bind()` gained a 4th `cameraMode` parameter. The existing test verified `never().bind(any(), any(), any())` (3 args) — this threw `InvalidUseOfMatchersException` at runtime.
- **Fix:** Updated to `never().bind(any(), any(), any(), any())` (4 args).
- **Files modified:** `CameraViewModelTest.kt`
- **Commit:** `eea889d`

---

## Phase 3+4 Fix Preservation Results

All Phase 3 + Phase 4 fix commits preserved verbatim (D-26 mandate):

| Fix | Commit ref | Grep result |
|-----|-----------|-------------|
| `isCapturing` guard in `onShutterTapped` | `dafc21e` | 5 occurrences in CameraViewModel ✓ |
| `bindJob?.cancel()` pattern | `9abbd0b` | 3 occurrences in CameraViewModel ✓ |
| `OneShotEvent.FilterLoadError` variant | `6ff00e0` | 6 occurrences in CameraViewModel ✓ |
| `captureFlash` inside `onSuccess` branch | `4e94591` | 4 occurrences in CameraViewModel ✓ |
| `require(frameCount > 0)` | `b7f74cf` | 1 occurrence in StickerRenderer ✓ |
| `assetLoader.preload(def.assetDir)` | `514410c` | 2 occurrences in CameraViewModel ✓ |
| **NEW Phase 5:** `recordingState !is RecordingState.Idle` guard | `dffc21e` | 1 in CameraViewModel + 1 in InsectFilterVM ✓ |

---

## Known Stubs

None. All production interfaces are wired. The recording lifecycle data layer is complete. UI surface (Plan 05-04+) is pending.

---

## Threat Surface Scan

No new network endpoints, auth paths, or unexpected schema changes beyond the plan's threat model:

- `VideoRecorder.startRecording()` writes to `MediaStore.Video.Media.EXTERNAL_CONTENT_URI` — within planned threat model (T-05-02/T-05-07 mitigated via `setDurationLimitMillis` + `pendingDiscardFlag` delete path).
- `CameraController.contentResolver` exposed as `internal val` — scoped to same module, not a public API surface.
- `OverlayEffectBuilder.cameraMode` is a `@Volatile var` settable by `CameraController` — no trust boundary crossed (both @Singleton in same process).

---

## Self-Check: PASSED

| Check | Result |
|-------|--------|
| `recording/VideoRecorder.kt` exists | FOUND |
| `ui/camera/RecordingState.kt` exists (Plan 05-02 canonical) | FOUND |
| commit `53b9c45` (Task 1) | FOUND |
| commit `dffc21e` (Task 2) | FOUND |
| commit `eea889d` (Task 3) | FOUND |
| `./gradlew :app:testDebugUnitTest` exit 0 (143 tests) | PASSED |
| `./gradlew :app:assembleDebug` exit 0 | PASSED |
| @Ignore count in VideoRecorderTest | 0 |
| @Ignore count in CameraControllerTest (Phase 5 tests) | 0 |
| @Ignore count in CameraViewModelTest (Phase 5 tests) | 0 |
| 15 tests un-Ignored and GREEN | PASSED |
