---
phase: 05
plan: 05
subsystem: insect-filter-ui
tags: [insect-filter-screen, sticker-gestures, detect-transform-gestures, record-button, recording-indicator, alert-dialog, back-handler, permission-launcher, lock-during-record, compose-screen]
dependency_graph:
  requires: [05-02-SUMMARY, 05-03-SUMMARY, 05-04-SUMMARY]
  provides: [InsectFilterScreen]
  affects: [BugzzApp nav graph (Plan 05-06 wires route), InsectFilterViewModel (onShutterTapped added), InsectFilterUiState (filters + isCapturing added)]
tech_stack:
  added: []
  patterns:
    - InsectFilterScreen 9-layer Box (viewfinder, sticker OverlayEffect comment, pointerInput gesture, FilterPicker, shutter, RecordButton, flip, RecordingIndicator, capture-flash)
    - pointerInput(isRecording) key restarts block on state change — correctly detaches/reattaches detectTransformGestures during recording (D-23 / T-05-04)
    - Reuses extracted RecordButton + RecordingIndicator from ui/camera/components/ (Plan 05-04 WARNING 9 option A closure)
    - ShutterButton inlined 72dp CircleShape white fill + gray border (option B per WARNING 9 — matches CameraScreen)
    - BackHandler(enabled = isRecording) + rememberSaveable showDiscardDialog (D-24 / VID-09)
    - Lazy RECORD_AUDIO via rememberLauncherForActivityResult + ContextCompat.checkSelfPermission (D-12 / VID-10)
    - SnackbarHostState for RECORD_AUDIO denial rationale + "Open Settings" CTA (D-13 reuse)
    - FilterPicker alpha(0.5f) lock treatment during recording (D-23)
    - Capture-flash AnimatedVisibility 75ms fadeIn / 150ms fadeOut (Phase 3 D-16 carry)
    - onShutterTapped() added to InsectFilterViewModel with isCapturing guard (WR-02) + captureFlash on success (WR-04)
    - filters: List<FilterSummary> added to InsectFilterUiState + populated in VM.init (D-01)
key_files:
  created:
    - app/src/main/java/com/bugzz/filter/camera/ui/insect/InsectFilterScreen.kt
  modified:
    - app/src/main/java/com/bugzz/filter/camera/ui/insect/InsectFilterUiState.kt
    - app/src/main/java/com/bugzz/filter/camera/ui/insect/InsectFilterViewModel.kt
decisions:
  - "ShutterButton inlined in InsectFilterScreen (option B per WARNING 9) — matches CameraScreen inlined pattern; only RecordButton + RecordingIndicator extracted per Plan 05-04"
  - "bitmapSize passed as IntSize(200, 200) placeholder from onGloballyPositioned — StickerRenderer overrides actual bitmap dims via setActiveFilter(); acceptable Phase 5 path per plan spec"
  - "onShutterTapped() added to InsectFilterViewModel (Rule 2) rather than calling controller.capturePhoto directly from screen — keeps VM as single owner of capture lifecycle, consistent with CameraViewModel pattern"
  - "filters: List<FilterSummary> added to InsectFilterUiState and populated in VM.init — eliminates need for screen-side FilterCatalog.all access; consistent with CameraUiState.filters pattern"
metrics:
  duration_seconds: 420
  completed_date: "2026-05-04"
  tasks_completed: 1
  tasks_total: 1
  files_created: 1
  files_modified: 2
---

# Phase 05 Plan 05: InsectFilterScreen Summary

**One-liner:** InsectFilterScreen production screen — 9-layer Box with detectTransformGestures sticker control, RecordButton + RecordingIndicator consumed from shared components, full CameraScreen-parity AlertDialog/BackHandler/RECORD_AUDIO, and Rule 2 onShutterTapped()+filters additions to ViewModel and UiState.

---

## What Was Built

### Task 1 — Create InsectFilterScreen + Rule 2 VM/UiState additions

| Component | What landed |
|-----------|-------------|
| `InsectFilterScreen.kt` | `@Composable fun InsectFilterScreen(viewModel: InsectFilterViewModel)`. 9-layer Box per UI-SPEC §5. Full-screen `Color.Black` background. `LaunchedEffect(lifecycleOwner)` calls `viewModel.bind()`. All 6 VM handlers wired: `onStickerGesture`, `onFilterSelected`, `onFlipLens`, `onPreviewSizeChanged`, `onShutterTapped`, `onRecordTapped`. One-shot Toast collector for PhotoSaved/PhotoError/FilterLoadError/VideoSaved/VideoError. |
| Layer 1 — CameraXViewfinder | `ImplementationMode.EXTERNAL` (Phase 2 STATE #15). `onGloballyPositioned` calls `viewModel.onPreviewSizeChanged(IntSize, IntSize(200,200))` for D-02 sticker centering on first measurement. |
| Layer 2 — Sticker (OverlayEffect) | Comment-only layer: sticker rendered by StickerRenderer inside OverlayEffect canvas, baked into preview/video/photo automatically (D-06/D-20). No Compose composable. |
| Layer 3 — Gesture handler | Full-screen transparent `Box` with `Modifier.pointerInput(isRecording)`. Early-return `if (isRecording) return@pointerInput` before `detectTransformGestures`. Calls `viewModel.onStickerGesture(pan, zoom, rotation)`. `Modifier.semantics { invisibleToUser() }` suppresses from TalkBack. |
| Layer 4 — FilterPicker | Reuses Phase 4 `FilterPicker` composable. `alpha(if (isRecording) 0.5f else 1f)`. Tap handler early-returns when `isRecording`. `uiState.filters` (now `List<FilterSummary>` in UiState) passed directly. |
| Layer 5 — Shutter button | Inlined 72dp `CircleShape` white fill + `BorderStroke(2.dp, Color.Gray)`. `HapticFeedbackConstants.LONG_PRESS`. Calls `viewModel.onShutterTapped()`. NOT locked during recording (D-23). |
| Layer 6 — RecordButton | Imported from `ui/camera/components/RecordButton` (Plan 05-04). `Alignment.BottomStart`, `padding(start=24.dp, bottom=24.dp)`. Lazy RECORD_AUDIO: `ContextCompat.checkSelfPermission` + `audioPermissionLauncher` on first tap. `HapticFeedbackConstants.VIRTUAL_KEY`. |
| Layer 7 — Flip button | `OutlinedButton(enabled = !isRecording)`. `Alignment.TopEnd`, `padding(top=16.dp, end=16.dp)`. Material3 disabled alpha applies automatically. |
| Layer 8 — RecordingIndicator | Imported from `ui/camera/components/RecordingIndicator` (Plan 05-04). `Alignment.TopCenter`, `padding(top=24.dp)`. |
| Layer 9 — Capture flash | `AnimatedVisibility(visible = uiState.captureFlashVisible)`. 75ms fadeIn / 150ms fadeOut full-screen white overlay (Phase 3 D-16 carry). |
| AlertDialog | `if (showDiscardDialog)` Material3 AlertDialog outside inner Box. Title "Recording in progress" `TextStyle(16.sp, FontWeight.Medium)` — Phase 5 2-weight system override (no SemiBold). Body "Are you sure you want to discard this recording?" `bodyMedium`. Cancel (right/confirmButton). Discard (left/dismissButton, `colorScheme.error`). |
| BackHandler | `BackHandler(enabled = isRecording) { showDiscardDialog = true }`. Declared early in composable body per UI-SPEC §Implementation Note 4. |
| RECORD_AUDIO | `rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission)`. Denied → `SnackbarHostState.showSnackbar("Microphone needed for video sound.", actionLabel="Open Settings")` → `Settings.ACTION_APPLICATION_DETAILS_SETTINGS`. |
| `InsectFilterUiState.kt` (modified) | Added `isCapturing: Boolean = false` + `filters: List<FilterSummary> = emptyList()`. Imports `FilterSummary` from `ui/camera/`. |
| `InsectFilterViewModel.kt` (modified) | Added `FilterSummary` import. Added `filters` population in `init` block from `FilterCatalog.all`. Added `onShutterTapped()`: mirrors CameraViewModel — `isCapturing` guard (WR-02), calls `controller.capturePhoto`, sets `captureFlashVisible=true` on success (WR-04), emits `OneShotEvent.PhotoSaved/PhotoError`. |

---

## Acceptance Criteria Verification

| Check | Result |
|-------|--------|
| `grep -c "@Composable" InsectFilterScreen.kt` | 1 ✓ |
| `grep -c "detectTransformGestures" InsectFilterScreen.kt` | 3 (import + comment + call) ✓ |
| `grep -c "pointerInput(isRecording)" InsectFilterScreen.kt` | 2 (import-style + actual usage) ✓ |
| `grep -c "BackHandler(enabled = isRecording)" InsectFilterScreen.kt` | 2 (comment + usage) ✓ |
| `grep -c "Recording in progress" InsectFilterScreen.kt` | 1 ✓ |
| `grep -c "Are you sure you want to discard this recording?" InsectFilterScreen.kt` | 1 ✓ |
| `grep -c "CameraXViewfinder" InsectFilterScreen.kt` | 4 (import + comment + usage) ✓ |
| `grep -c "ImplementationMode.EXTERNAL" InsectFilterScreen.kt` | 2 ✓ |
| `grep -c "RecordButton(" InsectFilterScreen.kt` | 1 ✓ |
| `grep -c "RecordingIndicator(" InsectFilterScreen.kt` | 1 ✓ |
| `import com.bugzz.filter.camera.ui.camera.components.RecordButton` | 1 ✓ |
| `import com.bugzz.filter.camera.ui.camera.components.RecordingIndicator` | 1 ✓ |
| `grep -c "captureFlashVisible" InsectFilterScreen.kt` | 2 ✓ |
| All 6 VM handlers wired | 7 call sites ✓ |
| `FontWeight.SemiBold` in insect/ | 0 ✓ |
| `vertical = 6.dp` in insect/ | 0 ✓ |
| `OneShotEvent.FilterLoadError` in InsectFilterScreen | 1 ✓ |
| `./gradlew :app:testDebugUnitTest :app:assembleDebug` | BUILD SUCCESSFUL ✓ |

---

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 2 - Missing critical functionality] `onShutterTapped()` absent from InsectFilterViewModel**

- **Found during:** Task 1 — InsectFilterScreen references `viewModel.onShutterTapped()` per plan spec (interface block lists `fun onShutterTapped(): Unit`) but Plan 05-02 did not implement it in InsectFilterViewModel
- **Issue:** Shutter button in InsectFilterScreen would call a non-existent method → compile error
- **Fix:** Added `onShutterTapped()` to InsectFilterViewModel mirroring CameraViewModel pattern: `isCapturing` guard (WR-02), `controller.capturePhoto { result -> ... }`, `captureFlashVisible=true` on success (WR-04), emits `OneShotEvent.PhotoSaved/PhotoError`
- **Files modified:** `InsectFilterViewModel.kt`, `InsectFilterUiState.kt` (added `isCapturing` field)
- **Commit:** `842c5ef`

**2. [Rule 2 - Missing critical functionality] `filters: List<FilterSummary>` absent from InsectFilterUiState**

- **Found during:** Task 1 — FilterPicker requires `List<FilterSummary>` but InsectFilterUiState only had `selectedFilterId`; CameraUiState has `filters` for the same purpose
- **Issue:** Screen needed to pass filters to FilterPicker; would require screen-side FilterCatalog.all access (anti-pattern — UI layer accesses catalog directly)
- **Fix:** Added `filters: List<FilterSummary> = emptyList()` to InsectFilterUiState; added `FilterCatalog.all.map { FilterSummary(...) }` population in InsectFilterViewModel.init block (mirrors CameraViewModel.bind() pattern)
- **Files modified:** `InsectFilterUiState.kt`, `InsectFilterViewModel.kt`
- **Commit:** `842c5ef`

---

## Phase 3+4+5 Fix Preservation

| Fix | Grep result |
|-----|-------------|
| `captureFlashVisible` in InsectFilterScreen | 2 occurrences ✓ |
| `OneShotEvent.FilterLoadError` in InsectFilterScreen | 1 occurrence ✓ |
| `FontWeight.SemiBold` in insect/ | 0 — no SemiBold introduced ✓ |
| `vertical = 6.dp` in insect/ | 0 — no Phase 4 padding anti-pattern ✓ |

---

## bitmapSize Tracking Approach

**Decision (Phase 5 simplest path):** `onGloballyPositioned` passes `bitmapSize = IntSize(200, 200)` as a placeholder. The actual sprite bitmap dimensions are managed by `StickerRenderer.setActiveFilter()` which loads frames from `AssetLoader` — the renderer has the real dimensions internally. `StickerState.applyGesture()` uses `bitmapSize` for clamp math; the 200×200 placeholder causes slightly loose clamping on first gesture before the renderer loads the filter.

**Phase 6 refinement path:** `StickerRenderer` can expose a `StateFlow<IntSize>` of `currentFrameSize` collected by `InsectFilterViewModel`; then `onPreviewSizeChanged` would receive the actual bitmap size. Not implemented in Phase 5 — clamping correctness is acceptable for personal-use MVP.

---

## Known Stubs

None. InsectFilterScreen is a complete production screen. Filter list is populated from the real catalog. Gesture, record, shutter, flip, picker, and capture-flash are all wired. Plan 05-06 wires the nav graph route (`CameraRoute(InsectFilter)` → `InsectFilterScreen`) — that's a nav-layer task, not a stub in this screen.

---

## Threat Surface Scan

T-05-04 (Tampering — gesture during background): `pointerInput(isRecording)` key + early-return applied. `collectAsStateWithLifecycle` pauses collection when screen is backgrounded.

T-05-06 (DoS — heavy gesture frequency): `onStickerGesture` is constant-time math (InsectFilterViewModel early-return when recording; StickerState.applyGesture is pure arithmetic with no allocation cascade).

No new network endpoints, auth paths, file access patterns, or schema changes beyond plan's threat model.

---

## Self-Check: PASSED

| Check | Result |
|-------|--------|
| `InsectFilterScreen.kt` exists | FOUND |
| `InsectFilterUiState.kt` modified | FOUND |
| `InsectFilterViewModel.kt` modified | FOUND |
| commit `842c5ef` | FOUND |
| `./gradlew :app:testDebugUnitTest :app:assembleDebug` | BUILD SUCCESSFUL |
| `FontWeight.SemiBold` in insect/ | 0 — PASSED |
| `vertical = 6.dp` in insect/ | 0 — PASSED |
| `captureFlashVisible` in InsectFilterScreen | 2 — PASSED |
| `OneShotEvent.FilterLoadError` in InsectFilterScreen | 1 — PASSED |
