---
phase: 03-first-filter-end-to-end-photo-capture
reviewed: 2026-04-20T00:00:00Z
depth: standard
files_reviewed: 30
files_reviewed_list:
  - app/build.gradle.kts
  - gradle/libs.versions.toml
  - app/src/main/java/com/bugzz/filter/camera/camera/CameraController.kt
  - app/src/main/java/com/bugzz/filter/camera/detector/BboxIouTracker.kt
  - app/src/main/java/com/bugzz/filter/camera/detector/FaceDetectorClient.kt
  - app/src/main/java/com/bugzz/filter/camera/detector/FaceLandmarkMapper.kt
  - app/src/main/java/com/bugzz/filter/camera/detector/FaceSnapshot.kt
  - app/src/main/java/com/bugzz/filter/camera/detector/OneEuroFilter.kt
  - app/src/main/java/com/bugzz/filter/camera/filter/AssetLoader.kt
  - app/src/main/java/com/bugzz/filter/camera/filter/FilterCatalog.kt
  - app/src/main/java/com/bugzz/filter/camera/filter/FilterDefinition.kt
  - app/src/main/java/com/bugzz/filter/camera/filter/SpriteManifest.kt
  - app/src/main/java/com/bugzz/filter/camera/render/BugBehavior.kt
  - app/src/main/java/com/bugzz/filter/camera/render/BugState.kt
  - app/src/main/java/com/bugzz/filter/camera/render/FilterEngine.kt
  - app/src/main/java/com/bugzz/filter/camera/render/OverlayEffectBuilder.kt
  - app/src/main/java/com/bugzz/filter/camera/ui/camera/CameraScreen.kt
  - app/src/main/java/com/bugzz/filter/camera/ui/camera/CameraUiState.kt
  - app/src/main/java/com/bugzz/filter/camera/ui/camera/CameraViewModel.kt
  - app/src/main/java/com/bugzz/filter/camera/ui/camera/OneShotEvent.kt
  - app/src/test/java/com/bugzz/filter/camera/camera/CameraControllerTest.kt
  - app/src/test/java/com/bugzz/filter/camera/detector/BboxIouTrackerTest.kt
  - app/src/test/java/com/bugzz/filter/camera/detector/FaceDetectorClientTest.kt
  - app/src/test/java/com/bugzz/filter/camera/detector/FaceLandmarkMapperTest.kt
  - app/src/test/java/com/bugzz/filter/camera/detector/LandmarkSmootherTest.kt
  - app/src/test/java/com/bugzz/filter/camera/filter/AssetLoaderTest.kt
  - app/src/test/java/com/bugzz/filter/camera/filter/FilterCatalogTest.kt
  - app/src/test/java/com/bugzz/filter/camera/render/BugBehaviorTest.kt
  - app/src/test/java/com/bugzz/filter/camera/render/FilterEngineTest.kt
  - app/src/test/java/com/bugzz/filter/camera/ui/camera/CameraViewModelTest.kt
findings:
  critical: 0
  warning: 5
  info: 7
  total: 12
status: issues-found
---

# Phase 3: Code Review Report

**Reviewed:** 2026-04-20
**Depth:** standard
**Files Reviewed:** 30
**Status:** issues-found

## Summary

Phase 03 lands the first end-to-end filter pipeline (ML Kit face detection → BboxIouTracker identity → 1€ smoothing → FilterEngine draw → photo capture via ImageCapture + MediaStore). The code is generally well-architected and well-documented: clear separation of concerns (CameraController / FaceDetectorClient / FilterEngine / ViewModel), pervasive design-decision anchors (D-xx references), and thoughtful test seams (constructor-split for Hilt, factory parameter for ImageCapture).

No critical security vulnerabilities were found. The project's privacy posture is strong — `BuildConfig.DEBUG` gates debug UI, T-03-05 biometric-logging policy is visibly applied, and `MediaStore + scoped storage` is used correctly.

The 12 findings cluster into three themes:

1. **Missing re-entrance / input-validation guards in the capture + filter pipeline** (WR-01, WR-02, WR-03) — these are real bug risks on a production device under rapid UI input or malformed sprite manifests.
2. **UX / state-consistency edges** (WR-04, WR-05) — synchronous-failure flash flicker and semantic misuse of `OneShotEvent.PhotoError` for non-photo errors.
3. **Code-quality / contract-comment drift** (IN-01…IN-07) — mostly doc fixes and small cleanups.

## Warnings

### WR-01: FilterEngine.onDraw has no guard against `frameCount == 0` (division-by-zero / modulo-by-zero)

**File:** `app/src/main/java/com/bugzz/filter/camera/render/FilterEngine.kt:77-81`
**Issue:** The flipbook-index calculation does `(tsNanos / frameDurationNanos) % filter.frameCount`. If a future `FilterDefinition` or `SpriteManifest` declares `frameCount = 0`, this throws `ArithmeticException` on the render thread every frame until the filter is swapped — killing the preview. `frameDurationNanos > 0L` is guarded (line 77) but `frameCount` is not. The current hard-coded catalog (35 / 23 frames) avoids the issue by construction, but `SpriteManifest` is loaded from JSON with no validator — a typo in a future manifest would crash the render thread.
**Fix:**
```kotlin
val frameIdx = if (frameDurationNanos > 0L && filter.frameCount > 0) {
    ((tsNanos / frameDurationNanos) % filter.frameCount).toInt()
} else {
    0
}
```
Additionally, add a `require(frameCount > 0)` check in `FilterDefinition.init` and/or in `SpriteManifest` loader (`AssetLoader.loadManifest`) so malformed manifests fail loudly at preload time, not mid-render.

### WR-02: CameraViewModel.onShutterTapped has no in-flight-capture guard — rapid double-taps fire concurrent takePicture + desynchronize captureFlashVisible

**File:** `app/src/main/java/com/bugzz/filter/camera/ui/camera/CameraViewModel.kt:141-152`
**Issue:** A rapid double-tap on the shutter (the flash is only 75ms enter + 150ms exit) calls `onShutterTapped` twice. The second call:
1. Re-sets `captureFlashVisible = true` (second flash) while the first callback is still pending.
2. Triggers a second `controller.capturePhoto(...)`. CameraX serialises these internally but this pattern is the classic source of "shutter sounds fired twice; only one photo saved; flash stuck on".
3. When the two callbacks return in arbitrary order, `captureFlashVisible` gets set to `false` by the first, then the second launch re-sets it to `false` — but during that gap a third tap could flip it to `true` again, leaving the UI in an inconsistent state.
Compare with `onTestRecord` (line 108-109) which correctly guards with `if (_uiState.value.isRecording) return`.
**Fix:** Add an in-flight flag:
```kotlin
fun onShutterTapped() {
    if (_uiState.value.captureFlashVisible) return  // re-entrance guard
    _uiState.value = _uiState.value.copy(captureFlashVisible = true)
    controller.capturePhoto { result -> ... }
}
```
Or, more robust, a dedicated `isCapturing: Boolean` field in `CameraUiState` — `captureFlashVisible` is a UX-timing flag, not a business-state flag; conflating the two is fragile.

### WR-03: CameraViewModel.bind re-binds and re-preloads filter on every lens-change recomposition — race + double-preload

**File:** `app/src/main/java/com/bugzz/filter/camera/ui/camera/CameraViewModel.kt:77-99`, `CameraScreen.kt:91-95`
**Issue:** `CameraScreen` has `LaunchedEffect(uiState.permissionState, uiState.lens, uiState.isDetectorReady) { ... vm.bind(lifecycleOwner) }`. Any change to `lens` re-launches the effect → calls `vm.bind(owner)` → launches a new `viewModelScope` coroutine that calls `controller.bind()`. The `activeFilterId` guard (line 86 `if (_uiState.value.activeFilterId == null)`) prevents a re-preload on the flip path, but:
1. If the user flips lens while the initial `assetLoader.preload(initial.id)` coroutine is still running, a second bind() launches concurrently. The second coroutine sees `activeFilterId == null` still (first preload hasn't set it yet) and also enters the preload block — `AssetLoader.preload` is idempotent (line 39-41 `if (cache.get(key) != null) continue`) so data integrity is safe, but there are two concurrent coroutines walking through the frame list redundantly on the single-threaded cameraExecutor. Not a crash, but wasted CPU during the most latency-sensitive moment.
2. `controller.bind()` itself doesn't guard against concurrent binds. Two concurrent `bindToLifecycle` calls with the same owner will race on `provider.unbindAll()` → `bind...` sequence. CameraX typically handles this but it is an undocumented path.
**Fix:** Track a `bindJob: Job?` in the ViewModel and cancel/await the previous one before starting a new bind. Or gate with `Mutex`. Minimal patch:
```kotlin
private var bindJob: Job? = null
fun bind(owner: LifecycleOwner) {
    bindJob?.cancel()
    bindJob = viewModelScope.launch { /* existing body */ }
}
```

### WR-04: CameraViewModel.onShutterTapped synchronous-failure path shows a 150ms flash even when no photo was taken

**File:** `app/src/main/java/com/bugzz/filter/camera/ui/camera/CameraViewModel.kt:141-152`, `CameraController.kt:273-276`
**Issue:** When the camera is not bound, `CameraController.capturePhoto` invokes the callback synchronously with `Result.failure(IllegalStateException("Camera not bound"))`. But in the VM, `captureFlashVisible = true` is set unconditionally before `controller.capturePhoto(...)` is called. The callback then does `viewModelScope.launch { _uiState.value = ... captureFlashVisible = false }` which suspends to the next main-thread dispatch — so the user briefly sees a white flash even when nothing was captured. UX-wise this wrongly signals a successful capture before the error toast fires.
**Fix:** Defer setting `captureFlashVisible = true` until after we confirm the capture started — or, simpler, only set it inside the success branch, or set it after verifying the camera is bound by having the VM check `uiState.permissionState.isGranted && controller has bound` before flipping the flag.

### WR-05: `OneShotEvent.PhotoError` is emitted for filter-load failures — semantic mis-categorization leaks through to the Toast

**File:** `app/src/main/java/com/bugzz/filter/camera/ui/camera/CameraViewModel.kt:95`, `CameraViewModel.kt:173`, `CameraScreen.kt:117-118`
**Issue:** In both `bind()` (line 95) and `onCycleFilter()` (line 173), a filter-preload failure emits `OneShotEvent.PhotoError("Filter load failed: ...")`. `CameraScreen.kt` displays PhotoError as `"Photo error: ${event.message}"` — the user sees `Photo error: Filter load failed: ...`, which is confusing and makes debugging harder (a filter-asset bug surfaces as a photo-capture bug in logs and bug reports).
**Fix:** Introduce `OneShotEvent.FilterLoadError(val message: String)` and render its Toast with "Filter error: ..." text. Keep `PhotoError` reserved for actual `capturePhoto` failures.

## Info

### IN-01: BboxIouTracker cap-drop contract doesn't match implementation — "lowest-IoU dropped" but code drops based on set iteration order

**File:** `app/src/main/java/com/bugzz/filter/camera/detector/BboxIouTracker.kt:20-23, 98-105`
**Issue:** The class-level docstring (line 20-23) says step 3 drops "lowest-IoU unmatched face" and the comment on line 100 says "drop silently per D-23". In practice, `sortedFaces` is already clamped to `MAX_TRACKED_FACES=2` by `.take(MAX_TRACKED_FACES)` on line 65, so at most 2 sorted-by-size faces ever enter the algorithm; step 3 iterates `unmatchedDetectedIdx` which is a `mutableSetOf` with unspecified iteration order. The actual dropped face is not "lowest-IoU" — it's "first in sort order after the cap is hit", which by construction is the smallest-bbox face already pre-filtered. The docs and implementation don't match the stated contract — but the current behavior (drop smallest bbox) is arguably better than the documented "drop lowest-IoU". Fix is a comment update, not code.
**Fix:** Update the class doc and inline comment to describe the actual behavior ("faces are pre-filtered to top-2 by bbox area, so the 'drop' path at step 3 is unreachable during normal operation; kept as a defensive cap").

### IN-02: FaceDetectorClient logs bounding-box centerX/centerY per frame per tracked face — borderline with T-03-05 aggregate-only policy

**File:** `app/src/main/java/com/bugzz/filter/camera/detector/FaceDetectorClient.kt:67-78`
**Issue:** The Timber.v log emits `bb=centerX,centerY` for every tracked face every frame. The docstring on line 66 says "aggregate-only Timber; never land per-landmark coord lists" — bounding-box center is aggregate, so this is compliant with the letter of T-03-05. However, at 30fps this emits ~60 lines/sec of face-position data for two faces, which is essentially a face-trajectory trace. Verbose-level logs are stripped in release by Timber's production tree (noted in comment `BugzzApplication` DEBUG check), so the privacy impact is debug-only. The reviewer flags this as information disclosure risk only if a release build ever accidentally plants a verbose tree.
**Fix:** Gate the block with `if (BuildConfig.DEBUG)` for belt-and-suspenders safety, or drop the per-face loop and log only `facesTracked=N removed=M` aggregate counts.

### IN-03: CameraController._surfaceRequest can accumulate superseded SurfaceRequest instances on rapid re-bind

**File:** `app/src/main/java/com/bugzz/filter/camera/camera/CameraController.kt:108-109, 130-135`
**Issue:** `setSurfaceProvider { request -> _surfaceRequest.value = request }` replaces the StateFlow value. If a new SurfaceRequest arrives before the old one was consumed by `CameraXViewfinder`, the old one is never told `willNotProvideSurface()` — CameraX documents that a SurfaceRequest must be completed or explicitly declined. Missing this can leak the underlying Surface across rapid lens flips. Composables typically handle this correctly via `CameraXViewfinder` internals, but if the Composable is not in composition when the request arrives (e.g., during backgrounding), the request goes to `_surfaceRequest` and may never be consumed.
**Fix:** When replacing the flow value, check the previous value and call `willNotProvideSurface()`:
```kotlin
.setSurfaceProvider { request ->
    _surfaceRequest.getAndUpdate { prev -> prev?.willNotProvideSurface(); request }
}
```

### IN-04: OverlayEffectBuilder.release() is never called — render HandlerThread leaks for app lifetime

**File:** `app/src/main/java/com/bugzz/filter/camera/render/OverlayEffectBuilder.kt:86-88`
**Issue:** `release()` quits the render HandlerThread, but no caller invokes it. `OverlayEffectBuilder` is `@Singleton` so the thread lives for the process lifetime — which is the intended design for Phase 3 (single camera session per app foreground). However, if Phase 4+ ever makes the engine configurable per-session (e.g., different render threads for different effect stacks) the missing tie-off will leak. Adding a `ViewModel.onCleared()` hook is wrong (Singleton lifetime ≠ ViewModel lifetime); a correct tie-off would be an `Application.onTerminate()` or `@PreDestroy` via a custom scope. Flag as future-proofing only.
**Fix:** Document in the class that `release()` is intentionally not called in Phase 3, referencing the Singleton lifetime; or wire it to `BugzzApplication.onTerminate` if that hook is used.

### IN-05: CameraViewModelTest.onCycleFilter_togglesFilterEngineActive mixes single-mode verify() with inOrder verify() — potential false positive on Mockito call-count semantics

**File:** `app/src/test/java/com/bugzz/filter/camera/ui/camera/CameraViewModelTest.kt:147-180`
**Issue:** The test calls `verify(mockFilterEngine).setFilter(FilterCatalog.all[0])` (line 159) and `verify(mockFilterEngine).setFilter(FilterCatalog.all[1])` (line 166) which assert "called at least once", then after the third cycle calls `inOrder(mockFilterEngine)` to re-verify the three invocations in order (line 174-178). Mockito's inOrder maintains its own pointer and doesn't consume the prior `verify()` bookmarks, so the test can pass even if extra spurious calls happened between the verified ones. Low-risk (the ViewModel code is deterministic) but it's a sharp edge for future test maintenance.
**Fix:** Replace the two `verify(mock).setFilter(...)` calls with a single `inOrder` block that covers the full sequence:
```kotlin
// Remove lines 159, 166 and only use the final inOrder block with times(N) checks.
inOrder(mockFilterEngine).verify(mockFilterEngine, times(3)).setFilter(any())
```

### IN-06: CameraController.startTestRecording does not set `MediaStore.Video.Media.IS_PENDING` — aborted recordings may leave 0-byte files visible in gallery

**File:** `app/src/main/java/com/bugzz/filter/camera/camera/CameraController.kt:211-220`
**Issue:** `ContentValues` for the video file omits `IS_PENDING = 1` at insert and the matching `IS_PENDING = 0` at finalize. `photo` capture (line 283) correctly notes CameraX handles this automatically — but the `CameraX Recorder` path does not, per developer.android.com scoped-storage docs. If the app is killed mid-recording, the 0-byte file is visible in the gallery until the next MediaStore scan. This is debug-only (D-05 TEST RECORD), so impact is low.
**Fix:** Add `put(MediaStore.Video.Media.IS_PENDING, 1)` at insert, and on `VideoRecordEvent.Finalize` success flip it to 0 via `contentResolver.update`. Code sample in the Android Recorder sample. Deferred to Phase 5 (full recording UX) is acceptable.

### IN-07: FilterEngine.onDraw has a single shared `bugState` but OverlayEffectBuilder only draws snapshot.faces.firstOrNull() — cannot support multi-face without rework

**File:** `app/src/main/java/com/bugzz/filter/camera/render/FilterEngine.kt:36, 86-91`, `OverlayEffectBuilder.kt:77`
**Issue:** `FilterEngine` holds a single `bugState = BugState()` member. `OverlayEffectBuilder` passes only `snapshot.faces.firstOrNull()` to `filterEngine.onDraw`, so Phase 3 explicitly supports only the primary face. Consistent with the scope (MAX_TRACKED_FACES=2 in tracker but single-sprite in render), but when Phase 4 wants a bug on each of 2 faces, the shared `bugState` will prevent it — the `.position`/`.velocity`/`.lastFrameIndex` would be overwritten every tick. Flag for awareness so the Phase 4 design accounts for it.
**Fix:** Replace the single `bugState` with `Map<Int, BugState>` keyed by `SmoothedFace.trackingId`, and update `onDraw` to accept a face list (or call it per-face). Not a Phase 3 concern but worth planning.

---

_Reviewed: 2026-04-20_
_Reviewer: Claude (gsd-code-reviewer)_
_Depth: standard_
