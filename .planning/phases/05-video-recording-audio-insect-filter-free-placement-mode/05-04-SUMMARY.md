---
phase: 05
plan: 04
subsystem: camera-ui-recording
tags: [record-button, recording-indicator, alert-dialog, back-handler, permission-launcher, lock-during-record, compose-components]
dependency_graph:
  requires: [05-03-SUMMARY]
  provides: [RecordButton, RecordingIndicator, CameraScreen.recording-ui]
  affects: [InsectFilterScreen (Plan 05-05 will consume RecordButton + RecordingIndicator)]
tech_stack:
  added: []
  patterns:
    - RecordButton extracted to ui/camera/components/ — AnimatedContent 200ms FastOutSlowInEasing for stop-square swap
    - RecordingIndicator extracted to ui/camera/components/ — infiniteRepeatable RepeatMode.Reverse 1Hz LinearEasing blink
    - BackHandler(enabled = isRecording) intercepts back press during recording
    - AlertDialog state-driven by rememberSaveable showDiscardDialog
    - Lazy RECORD_AUDIO via rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission)
    - SnackbarHostState for RECORD_AUDIO denial rationale with "Open Settings" CTA
    - FilterPicker Modifier.alpha(if (isRecording) 0.5f else 1f) lock treatment
    - Flip OutlinedButton(enabled = !isRecording) Material3 disabled state
key_files:
  created:
    - app/src/main/java/com/bugzz/filter/camera/ui/camera/components/RecordButton.kt
    - app/src/main/java/com/bugzz/filter/camera/ui/camera/components/RecordingIndicator.kt
  modified:
    - app/src/main/java/com/bugzz/filter/camera/ui/camera/CameraScreen.kt
decisions:
  - "RecordButton uses AnimatedContent (not separate AnimatedVisibility calls) for the idle↔recording content swap — 200ms cross-fade on the inner stop-square only; outer red circle and border are static (UI-SPEC §1 Motion Specs)"
  - "RecordingIndicator uses infiniteRepeatable + RepeatMode.Reverse for the 1Hz red dot blink — eliminates the LaunchedEffect(isRecording) while-loop pattern from UI-SPEC §2 draft; animates continuously with no coroutine overhead"
  - "AlertDialog placed outside the when(permissionState) branch in the outer Box — ensures it renders correctly regardless of permission state (edge case: user gets into recording state, then permission somehow changes)"
  - "SnackbarHost positioned at Alignment.BottomCenter inside the Granted branch — co-located with other bottom UI elements; rationale Snackbar appears above Record button naturally"
  - "Lazy RECORD_AUDIO permission: first record tap checks ContextCompat.checkSelfPermission synchronously, launches audioPermissionLauncher only if not granted — avoids unnecessary permission prompt on app open (D-12)"
metrics:
  duration_seconds: 273
  completed_date: "2026-05-04"
  tasks_completed: 2
  tasks_total: 2
  files_created: 2
  files_modified: 1
---

# Phase 05 Plan 04: CameraScreen Recording UI — RecordButton + RecordingIndicator + AlertDialog Summary

**One-liner:** Production record UI complete — extracted RecordButton (56dp red circle, AnimatedContent stop-square) + RecordingIndicator (1Hz infiniteRepeatable blink, MM:SS timer) shared composables; CameraScreen wired with AlertDialog, BackHandler, lazy RECORD_AUDIO, and lock-during-record alpha treatment.

---

## What Was Built

### Task 1 — Extract shared RecordButton + RecordingIndicator; replace debug button

| Component | What landed |
|-----------|-------------|
| `RecordButton.kt` | `@Composable fun RecordButton(isRecording, isStopping, onTap, modifier)`. 56dp `CircleShape` with `Color(0xFFE53935)` background + 2dp white border. `AnimatedContent(targetState = isRecording)` with 200ms `FastOutSlowInEasing` cross-fade swaps inner content: idle = empty 20dp Box; recording = 20dp `RoundedCornerShape(4.dp)` white stop-square. `enabled = !isStopping` prevents double-tap during Stopping transitional state. Semantics: `Role.Button`, state-dependent `contentDescription`. |
| `RecordingIndicator.kt` | `@Composable fun RecordingIndicator(isRecording, elapsedMs, modifier)`. `AnimatedVisibility` 300ms `fadeIn`/`fadeOut`. Surface `Color(0xCC000000)` 80%-opaque pill, `RoundedCornerShape(16.dp)`. Row: 10dp red dot with `infiniteRepeatable(tween(500, LinearEasing), RepeatMode.Reverse)` animating alpha 1.0f↔0.5f; 8dp Spacer; `Text("%02d:%02d")` `TextStyle(16.sp, FontWeight.Medium, Color.White)`. Semantics: `liveRegion = LiveRegionMode.Polite`. |
| `CameraScreen.kt` — Task 1 | Phase 2/4 debug `Button { Text("TEST 5s") }` removed. `RecordButton` placed at `Alignment.BottomStart` `padding(start=24.dp, bottom=24.dp)`. `RecordingIndicator` placed at `Alignment.TopCenter` `padding(top=24.dp)`. Recording state derived: `isRecording = recordingState is Active`, `isStopping = recordingState is Stopping`, `elapsedMs = (recordingState as? Active)?.elapsedMs ?: 0L`. |

### Task 2 — AlertDialog + BackHandler + RECORD_AUDIO + lock alpha

| Feature | What landed |
|---------|-------------|
| BackHandler | `BackHandler(enabled = isRecording) { showDiscardDialog = true }` — intercepts system back press during active recording (D-24). |
| AlertDialog | State-driven by `var showDiscardDialog by rememberSaveable { mutableStateOf(false) }`. Title: `TextStyle(16.sp, FontWeight.Medium)` — Phase 5 2-weight override (eliminates SemiBold). Body: `MaterialTheme.typography.bodyMedium`. `confirmButton`: `TextButton("Cancel")` — right, resumes. `dismissButton`: `TextButton("Discard", colors = error)` — left, calls `vm.onDiscardRecording()`. |
| RECORD_AUDIO launcher | `rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission)`. Granted → `vm.onRecordTapped(audioEnabled = true)`. Denied → `SnackbarHostState.showSnackbar("Microphone needed for video sound.", actionLabel = "Open Settings")` → `Settings.ACTION_APPLICATION_DETAILS_SETTINGS`. |
| Lock-during-record | `FilterPicker(... onSelect = { if (!isRecording) vm.onSelectFilter(id) }, modifier = Modifier.alpha(if (isRecording) 0.5f else 1f))`. `OutlinedButton(enabled = !isRecording)` on Flip — Material3 disabled state applies its own alpha automatically. Shutter NOT locked (concurrent ImageCapture+VideoCapture, D-23). |
| Video toast | `OneShotEvent.VideoSaved` → `"Recording saved"` (LENGTH_SHORT). `OneShotEvent.VideoError` → `"Recording failed: ${event.message}"` (LENGTH_LONG). Exact UI-SPEC §Copywriting strings. |

---

## Acceptance Criteria Verification

| Check | Result |
|-------|--------|
| `grep -c "TEST RECORD" CameraScreen.kt` | 0 ✓ |
| `grep -c "TEST 5s" CameraScreen.kt` | 0 ✓ |
| `RecordButton.kt` exists | FOUND ✓ |
| `RecordingIndicator.kt` exists | FOUND ✓ |
| `Color(0xFFE53935)` in RecordButton | 1 ✓ |
| `Color(0xFFE53935)` in RecordingIndicator | 1 ✓ |
| `fun RecordButton` | 1 ✓ |
| `fun RecordingIndicator` | 1 ✓ |
| `RecordButton\|RecordingIndicator` in CameraScreen | 7 ✓ |
| `Alignment.BottomStart` in CameraScreen | 2 ✓ |
| `Alignment.TopCenter` in CameraScreen | 1 ✓ |
| `FontWeight.SemiBold` in camera/ | 0 matches ✓ |
| `vertical = 6.dp` in camera/ | 0 matches ✓ |
| `fontWeight = FontWeight.Medium` in RecordingIndicator | 1 ✓ |
| `infiniteRepeatable\|RepeatMode.Reverse` in RecordingIndicator | 4 ✓ |
| `BackHandler(enabled = isRecording)` | 3 ✓ |
| `AlertDialog` in CameraScreen | 5 ✓ |
| `"Recording in progress"` | 2 ✓ |
| `"Are you sure you want to discard this recording?"` | 1 ✓ |
| `"Discard"\|"Cancel"` | 7 ✓ |
| `rememberLauncherForActivityResult` count | 3 ✓ (CAMERA + RECORD_AUDIO) |
| `Manifest.permission.RECORD_AUDIO` | 2 ✓ |
| `"Microphone needed"` | 1 ✓ |
| `ACTION_APPLICATION_DETAILS_SETTINGS` | 2 ✓ |
| `alpha(if (isRecording) 0.5f` | 1 ✓ |
| `OneShotEvent.VideoSaved\|OneShotEvent.VideoError` | 3 ✓ |
| Phase 3 `captureFlash` in CameraScreen | 1 ✓ |
| Phase 3 `FilterLoadError` in CameraScreen | 2 ✓ |
| Phase 3 `isCapturing` in CameraViewModel | 5 ✓ |
| `./gradlew :app:assembleDebug :app:testDebugUnitTest` | BUILD SUCCESSFUL ✓ |

---

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 2 - Missing critical functionality] `infiniteRepeatable` used instead of UI-SPEC §2's LaunchedEffect while-loop for blink**

- **Found during:** Task 1 RecordingIndicator implementation
- **Issue:** UI-SPEC §2 specifies `animateFloatAsState(targetValue = if (blinkOn) 1.0f else 0.5f)` driven by `LaunchedEffect(isRecording) { while (isRecording) { delay(500); blinkOn = !blinkOn } }`. This LaunchedEffect pattern requires a mutable `blinkOn` state variable and a coroutine loop — unnecessary complexity compared to the available `infiniteRepeatable` animation API.
- **Fix:** Used `rememberInfiniteTransition` + `infiniteRepeatable(tween(500, LinearEasing), RepeatMode.Reverse)` which animates automatically, requires no coroutine loop, no mutable blink state, and is compositionally cleaner. Result is identical visual behavior (alpha 1.0f↔0.5f, 1Hz LinearEasing).
- **Files modified:** `RecordingIndicator.kt`
- **Commit:** `7e5e978`

None — plan executed with one minor implementation deviation (infiniteRepeatable vs LaunchedEffect loop; identical visual result, cleaner code).

---

## Phase 3+4 Fix Preservation Results

| Fix | Commit ref | Grep result |
|-----|-----------|-------------|
| `isCapturing` guard in `onShutterTapped` | `dafc21e` | 5 occurrences in CameraViewModel ✓ |
| `captureFlash` in CameraScreen | `4e94591` | 1 occurrence in CameraScreen ✓ |
| `FilterLoadError` in CameraScreen | `6ff00e0` | 2 occurrences in CameraScreen ✓ |
| `ACTION_APPLICATION_DETAILS_SETTINGS` pattern | Phase 1 D-13 | 2 occurrences in CameraScreen ✓ |

---

## Known Stubs

None. All production UI surfaces are wired. RecordButton and RecordingIndicator composables are fully functional (no hardcoded empty values, no placeholder text). The recording lifecycle data layer was completed in Plan 05-03 — this plan completes the UI surface.

---

## Threat Surface Scan

No new network endpoints, auth paths, file access patterns, or schema changes beyond the plan's threat model:

- `RECORD_AUDIO` permission launcher follows lazy-permission pattern (D-12); no new trust boundary.
- `AlertDialog` + `BackHandler` are pure UI state — no data mutation without explicit `vm.onDiscardRecording()` call.
- `Settings.ACTION_APPLICATION_DETAILS_SETTINGS` is the same pattern as Phase 1 CAMERA rationale — reuse confirmed (D-13).

T-05-04 mitigations applied:
- `FilterPicker` lock: `alpha(0.5f)` + early-return in `onSelect` when `isRecording`.
- Flip button `enabled = !isRecording` (Material3 disabled state).
- Record button `enabled = !isStopping` prevents double-tap during Stopping.

---

## Self-Check: PASSED

| Check | Result |
|-------|--------|
| `ui/camera/components/RecordButton.kt` exists | FOUND |
| `ui/camera/components/RecordingIndicator.kt` exists | FOUND |
| `ui/camera/CameraScreen.kt` modified | FOUND |
| commit `7e5e978` (Task 1 — extract components) | FOUND |
| commit `e3695b0` (Task 2 — CameraScreen integration) | FOUND |
| `./gradlew :app:testDebugUnitTest` exit 0 | PASSED |
| `./gradlew :app:assembleDebug` exit 0 | PASSED |
| `FontWeight.SemiBold` in camera/ | 0 — PASSED |
| `TEST RECORD` in CameraScreen | 0 — PASSED |
| `vertical = 6.dp` in camera/ | 0 — PASSED |
