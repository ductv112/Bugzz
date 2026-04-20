---
phase: 03-first-filter-end-to-end-photo-capture
fixed_at: 2026-04-20T00:00:00Z
review_path: .planning/phases/03-first-filter-end-to-end-photo-capture/03-REVIEW.md
iteration: 1
findings_in_scope: 5
fixed: 5
skipped: 0
status: all_fixed
---

# Phase 3: Code Review Fix Report

**Fixed at:** 2026-04-20
**Source review:** .planning/phases/03-first-filter-end-to-end-photo-capture/03-REVIEW.md
**Iteration:** 1

**Summary:**
- Findings in scope: 5
- Fixed: 5
- Skipped: 0

## Fixed Issues

### WR-01: FilterEngine.onDraw has no guard against `frameCount == 0`

**Files modified:** `app/src/main/java/com/bugzz/filter/camera/render/FilterEngine.kt`, `app/src/main/java/com/bugzz/filter/camera/filter/FilterDefinition.kt`
**Commit:** b7f74cf
**Applied fix:**
- In `FilterEngine.onDraw`: extended the `frameDurationNanos > 0L` guard to also check `filter.frameCount > 0`, so a zero-frameCount manifest yields `frameIdx = 0` instead of throwing `ArithmeticException` on the render thread.
- In `FilterDefinition.init`: added `require(frameCount > 0)` so malformed manifests fail loudly at construction time (preload) rather than mid-render.

### WR-02: `onShutterTapped` has no in-flight-capture guard

**Files modified:** `app/src/main/java/com/bugzz/filter/camera/ui/camera/CameraUiState.kt`, `app/src/main/java/com/bugzz/filter/camera/ui/camera/CameraViewModel.kt`
**Commit:** dafc21e
**Applied fix:**
- Added `isCapturing: Boolean = false` field to `CameraUiState` as a dedicated business-state flag distinct from the UX-timing `captureFlashVisible`.
- In `onShutterTapped`: early-return `if (_uiState.value.isCapturing) return`, then set `isCapturing = true` on entry and `isCapturing = false` in both success and failure branches of the callback. Prevents concurrent `capturePhoto` calls on rapid double-tap.

### WR-03: `bind()` can race on concurrent lens-flip — no cancellation of prior bind coroutine

**Files modified:** `app/src/main/java/com/bugzz/filter/camera/ui/camera/CameraViewModel.kt`
**Commit:** 9abbd0b
**Applied fix:**
- Added `private var bindJob: Job? = null` field to `CameraViewModel`.
- At the top of `bind()`: `bindJob?.cancel()` before `bindJob = viewModelScope.launch { ... }`, ensuring any in-flight bind coroutine (including its concurrent `assetLoader.preload`) is cancelled before a new lens-flip bind begins. Also added `import kotlinx.coroutines.Job`.

### WR-04: `captureFlashVisible` set before bound-check — flash shown on synchronous failure

**Files modified:** `app/src/main/java/com/bugzz/filter/camera/ui/camera/CameraViewModel.kt`
**Commit:** 4e94591
**Applied fix:**
- Moved `captureFlashVisible = true` from the unconditional pre-call site into the `onSuccess` branch of the `capturePhoto` callback.
- On synchronous failure (camera not bound), only `isCapturing = false` is set and a `PhotoError` event is emitted — no flash occurs.
- The success path sets `captureFlashVisible = true` then immediately `false` in the same coroutine dispatch, giving `AnimatedVisibility` the true→false transition needed to trigger its 150ms fade-out exit animation.

### WR-05: Filter-load failures emit `OneShotEvent.PhotoError` — semantic mis-categorization

**Files modified:** `app/src/main/java/com/bugzz/filter/camera/ui/camera/OneShotEvent.kt`, `app/src/main/java/com/bugzz/filter/camera/ui/camera/CameraViewModel.kt`, `app/src/main/java/com/bugzz/filter/camera/ui/camera/CameraScreen.kt`
**Commit:** 6ff00e0
**Applied fix:**
- Added `data class FilterLoadError(val message: String) : OneShotEvent` to `OneShotEvent`.
- Both filter-preload failure sites in `CameraViewModel` (`bind()` line 101 and `onCycleFilter()` line 189) now emit `FilterLoadError` instead of `PhotoError`.
- `CameraScreen` event collector has a new `is OneShotEvent.FilterLoadError` branch that shows `"Filter error: ${event.message}"` Toast, keeping `PhotoError` reserved for actual capture failures.

## Skipped Issues

None — all findings were fixed.

---

_Fixed: 2026-04-20_
_Fixer: Claude (gsd-code-fixer)_
_Iteration: 1_
