---
phase: "03"
plan: "04"
subsystem: camera-capture
tags: [photo-capture, media-store, viewmodel, compose-ui, tdd, cap-01, cap-03]
dependency_graph:
  requires: [03-01, 03-02, 03-03]
  provides: [CAP-01, CAP-02-arch, CAP-03, D-13, D-14, D-15, D-16]
  affects: [CameraController, CameraViewModel, CameraScreen, OneShotEvent, CameraUiState]
tech_stack:
  added: []
  patterns:
    - imageCaptureFactory constructor-split seam (mirrors Phase 2 providerFactory pattern)
    - mock<Uri>() for Android Uri in plain JVM tests (Uri.parse() returns null without Android runtime)
    - Dispatchers.setMain(testDispatcher) + advanceUntilIdle() for viewModelScope coroutine testing
    - async { flow.first() } concurrent with action + advanceUntilIdle() for Channel event testing
key_files:
  created: []
  modified:
    - app/src/main/java/com/bugzz/filter/camera/camera/CameraController.kt
    - app/src/main/java/com/bugzz/filter/camera/ui/camera/OneShotEvent.kt
    - app/src/main/java/com/bugzz/filter/camera/ui/camera/CameraUiState.kt
    - app/src/main/java/com/bugzz/filter/camera/ui/camera/CameraViewModel.kt
    - app/src/main/java/com/bugzz/filter/camera/ui/camera/CameraScreen.kt
    - app/src/test/java/com/bugzz/filter/camera/camera/CameraControllerTest.kt
    - app/src/test/java/com/bugzz/filter/camera/ui/camera/CameraViewModelTest.kt
decisions:
  - "imageCaptureFactory constructor-split seam added to CameraController (mirrors providerFactory Phase 2 pattern — D-14 STATE #14 canonical)"
  - "mock<Uri>() used in CameraViewModelTest — Uri.parse() returns null under plain JVM (no Robolectric needed for ViewModel layer)"
  - "Dispatchers.setMain(StandardTestDispatcher) + async{events.first()} pattern for testing viewModelScope Channel emissions"
  - "CameraScreen written as combined Task 2+3 completion due to Rule 3 auto-fix (exhaustive when-branch for new PhotoSaved/PhotoError variants)"
metrics:
  duration_seconds: 862
  completed_date: "2026-04-20"
  tasks_completed: 3
  tasks_total: 3
  files_modified: 7
---

# Phase 03 Plan 04: Photo Capture End-to-End Summary

**One-liner:** CameraController.capturePhoto via ImageCapture.OutputFileOptions MediaStore writer (DCIM/Bugzz) + ViewModel shutter/cycle-filter handlers + CameraScreen three-button layout with 72dp shutter, haptic, capture-flash overlay, and Toast events.

## Completed Tasks

| # | Task | Commit | Key Output |
|---|------|--------|------------|
| 1 | CameraController.capturePhoto + CameraControllerTest un-Ignore | e8da77d | capturePhoto(onResult) method, imageCaptureFactory seam, 3 tests GREEN |
| 2 | OneShotEvent + CameraUiState + CameraViewModel + CameraViewModelTest | b914915 | PhotoSaved/PhotoError variants, activeFilterId/captureFlashVisible fields, onShutterTapped/onCycleFilter/bind-preload, 3 VM tests un-Ignored and GREEN |
| 3 | CameraScreen three-button layout + capture-flash + Toast handlers | a316631 | Shutter BottomCenter 72dp, TEST RECORD BottomStart, Cycle BottomEnd (DEBUG), AnimatedVisibility flash, Toast events |

## Implementation Notes

### CAP-01: CameraController.capturePhoto
- Uses `ImageCapture.OutputFileOptions.Builder(contentResolver, EXTERNAL_CONTENT_URI, contentValues).build()`
- ContentValues: DISPLAY_NAME=`Bugzz_yyyyMMdd_HHmmss.jpg`, MIME_TYPE=`image/jpeg`, RELATIVE_PATH=`DCIM/Bugzz`
- IS_PENDING not set — CameraX 1.6 handles the MediaStore transaction internally
- `imageCaptureFactory: () -> ImageCapture` test seam added to internal primary constructor; `@Inject` secondary constructor delegates with production factory

### CAP-02: Overlay bake (architectural)
- OverlayEffect was bound with `PREVIEW | IMAGE_CAPTURE | VIDEO_CAPTURE` targets in Phase 2 (Plan 02-04)
- FilterEngine was wired into OverlayEffectBuilder.setOnDrawListener in Phase 3 Plan 03-03
- No new wiring needed in this plan — CAP-02 is architecturally satisfied; Wave 4 device handoff (Plan 03-05) confirms on Xiaomi 13T

### CAP-03: Content-values shape
- `capturePhoto_outputFileOptions_hasDCIMBugzzRelativePath` test verifies takePicture is called (options assembled without exception); D-32 field values validated structurally via code review (OutputFileOptions.contentValues not publicly accessible via API)

### Initial filter preload on bind
- `bind(owner)` extended: after `controller.bind()` succeeds, if `activeFilterId == null`, calls `assetLoader.preload(initial.id)` on `cameraExecutor.asCoroutineDispatcher()` then `filterEngine.setFilter(initial)` — no main-thread StrictMode trip

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Exhaustive when-branch for new OneShotEvent variants in CameraScreen**
- **Found during:** Task 2 (compile error when adding PhotoSaved/PhotoError to OneShotEvent sealed interface)
- **Issue:** CameraScreen.kt `when (event)` expression is exhaustive for sealed interface — Kotlin compile error `'when' expression must be exhaustive`
- **Fix:** CameraScreen edited as part of Task 3 (already planned); PhotoSaved + PhotoError branches added to LaunchedEffect when-block
- **Files modified:** `app/src/main/java/com/bugzz/filter/camera/ui/camera/CameraScreen.kt`
- **Commit:** a316631

**2. [Rule 1 - Bug] doNothing not available for suspend functions in mockito-kotlin**
- **Found during:** Task 2 test compilation
- **Issue:** `doNothing {}` syntax is not valid for suspend functions in mockito-kotlin; compile error `Unresolved reference 'doNothing'`
- **Fix:** Changed to `onBlocking { preload(any()) } doReturn Unit` — functionally equivalent no-op for suspend Unit function
- **Files modified:** `app/src/test/java/com/bugzz/filter/camera/ui/camera/CameraViewModelTest.kt`
- **Commit:** b914915

**3. [Rule 1 - Bug] Uri.parse() returns null under plain JVM — NullPointerException in PhotoSaved constructor**
- **Found during:** Task 2 test execution (suppressed exception in UncompletedCoroutinesError)
- **Issue:** `Uri.parse("content://test/1")` returns null without Android runtime (no Robolectric). `OneShotEvent.PhotoSaved(uri)` has non-null `val uri: Uri` — NPE thrown inside viewModelScope coroutine, swallowed, causing test timeout
- **Fix:** Changed `Uri.parse(...)` to `mock<Uri>()` — a Mockito mock object that is non-null and satisfies the type constraint
- **Files modified:** `app/src/test/java/com/bugzz/filter/camera/ui/camera/CameraViewModelTest.kt`
- **Commit:** b914915

**4. [Rule 1 - Bug] Dispatchers.Main not set for viewModelScope.launch in unit tests**
- **Found during:** Task 2 test execution (`UncompletedCoroutinesError: After waiting for 1m, the test body did not run to completion`)
- **Issue:** `viewModelScope` uses `Dispatchers.Main` which is not available in plain JVM tests. The coroutine launched inside `onShutterTapped` callback runs on the default empty dispatcher and never reaches `_events.send()`
- **Fix:** Added `Dispatchers.setMain(StandardTestDispatcher())` in `@Before`; `@After` resets. Used `async { vm.events.first() }` started before the action so the deferred collector is ready when the event is produced; `advanceUntilIdle()` drives all pending coroutines to completion
- **Files modified:** `app/src/test/java/com/bugzz/filter/camera/ui/camera/CameraViewModelTest.kt`
- **Commit:** b914915

## Threat Mitigations Applied

| Threat ID | Mitigation | Location |
|-----------|------------|----------|
| T-03-01 | onError path emits `Result.failure(exc)` → PhotoError Toast — no silent failure, no crash | CameraController.capturePhoto onError; CameraViewModelTest.onShutterTapped_capturePhotoFails_emitsPhotoError |
| T-03-04 | capturePhoto callback is one-shot; viewModelScope is ViewModel-scoped (auto-cancelled onCleared); no retained Activity refs | CameraViewModel.onShutterTapped + onCleared |

## Known Stubs

None — all capture paths are fully wired. CAP-02 overlay bake requires device verification (Plan 03-05 Wave 4 handoff) but the architecture is complete.

## Threat Flags

None — no new network endpoints, auth paths, or schema changes introduced beyond the MediaStore write path already covered by T-03-01.

## Test Summary

- **Total tests:** 74 (all GREEN)
- **New tests un-Ignored this plan:** 6
  - CameraControllerTest: `capturePhoto_invokesTakePicture_onBoundImageCapture`, `capturePhoto_outputFileOptions_hasDCIMBugzzRelativePath`, `capturePhoto_controllerNotBound_emitsFailure`
  - CameraViewModelTest: `onShutterTapped_capturePhotoSucceeds_emitsPhotoSaved`, `onShutterTapped_capturePhotoFails_emitsPhotoError`, `onCycleFilter_togglesFilterEngineActive`
- **Remaining @Ignore annotations for Plan 03-04:** 0

## Self-Check: PASSED

| Check | Result |
|-------|--------|
| CameraController.kt | FOUND |
| OneShotEvent.kt | FOUND |
| CameraUiState.kt | FOUND |
| CameraViewModel.kt | FOUND |
| CameraScreen.kt | FOUND |
| 03-04-SUMMARY.md | FOUND |
| commit e8da77d | FOUND |
| commit b914915 | FOUND |
| commit a316631 | FOUND |
