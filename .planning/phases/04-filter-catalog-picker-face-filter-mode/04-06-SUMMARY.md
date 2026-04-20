---
phase: 04-filter-catalog-picker-face-filter-mode
plan: "06"
subsystem: ui/camera-picker
tags: [compose, lazyrow, coil, filter-picker, animation, cat-03]
dependency_graph:
  requires:
    - 04-05 (uiState.filters + uiState.selectedFilterId + vm.onSelectFilter — all consumed here)
    - 04-01 (coil-compose dep + sprite assets in assets/sprites/)
  provides:
    - FilterPicker: stateless LazyRow composable (filters, selectedId, onSelect, modifier)
    - CameraScreen: FilterPicker integrated at BottomCenter 104dp offset above shutter
  affects:
    - Plan 04-07 (HomeScreen redesign — no CameraScreen dependency; FilterPicker standalone)
    - Plan 04-08 (Xiaomi 13T device runbook — picker strip visible, rapid-tap CAT-03 verified)
tech_stack:
  added: []
  patterns:
    - "FilterPicker stateless composable: receives (filters, selectedId, onSelect) — no ViewModel coupling; caller owns state"
    - "animateFloatAsState tween(200ms, FastOutSlowInEasing) for 1.0f↔1.15f scale on selected thumbnail"
    - "LaunchedEffect(selectedId, filters.size) with instant scrollToItem on entry; tap handler uses animateScrollToItem for animated snap"
    - "file:///android_asset/ URI scheme for Coil AsyncImage — built-in AssetUriFetcher, no custom loader needed"
    - "LocalContext.current (not LocalView.current.context) for Coil ImageRequest.Builder"
    - "ColorPainter import from androidx.compose.ui.graphics.painter (not androidx.compose.ui.graphics)"
key_files:
  created:
    - app/src/main/java/com/bugzz/filter/camera/ui/camera/FilterPicker.kt
  modified:
    - app/src/main/java/com/bugzz/filter/camera/ui/camera/CameraScreen.kt
decisions:
  - "Picker bottom padding: 104dp (not 124dp) — Impl Notes #2 authoritative math: shutter top at 96dp from bottom (72dp + 24dp padding) + 8dp gap = 104dp. The 124dp value in 04-UI-SPEC §2 was an earlier draft; Impl Notes #2 supersedes."
  - "Coil file:///android_asset/ URI: worked first-try with coil-compose 2.7.0 built-in AssetUriFetcher — no custom ImageLoader config needed."
  - "LocalContext.current used for Coil ImageRequest.Builder (per 04-UI-SPEC Asset Contract example and 04-PLAN Interfaces note); equivalent to LocalView.current.context but idiomatic Compose."
  - "ColorPainter import path: androidx.compose.ui.graphics.painter.ColorPainter (not .graphics.ColorPainter — that class does not exist; Rule 1 auto-fix during Task 1 compile)."
  - "Cycle Filter button removed from CameraScreen code; KDoc comment updated to note Phase 4 removal. vm.onCycleFilter() method retained in ViewModel (Phase 3 test still exercises it)."
metrics:
  duration: "~178 seconds"
  completed_date: "2026-04-20"
  tasks_completed: 2
  files_changed: 2
---

# Phase 4 Plan 06: FilterPicker Composable + CameraScreen Integration — Summary

**One-liner:** Stateless `FilterPicker` LazyRow composable with Coil `file:///android_asset/` thumbnails, 1.15x scale + 2dp white border selection animation (200ms FastOutSlowIn), VIRTUAL_KEY haptic + animateScrollToItem center-snap, integrated into CameraScreen at 104dp above shutter with Cycle Filter debug button removed — CAT-03 picker UI complete.

## Tasks Completed

| # | Name | Commit | Key outputs |
|---|------|--------|-------------|
| 1 | Create FilterPicker composable | b856325 | FilterPicker.kt NEW (176 lines); LazyRow + Coil AsyncImage + scale/border animation + haptic |
| 2 | Integrate FilterPicker into CameraScreen + remove Cycle Filter | a15f113 | CameraScreen.kt: FilterPicker added at 104dp offset, Cycle button block deleted |

## Implementation Details

### Picker Bottom Offset: 104dp chosen (not 124dp)

Per 04-UI-SPEC §Implementation Notes #2:
- Shutter is `Alignment.BottomCenter` with `padding(bottom=24dp)` and `size(72dp)`
- Shutter top = 24dp + 72dp = 96dp from screen bottom
- Gap between strip bottom and shutter top = 8dp
- Picker strip `padding(bottom = 104dp)` → strip bottom sits at 104dp, strip top at 204dp

The 124dp value in the earlier §2 draft was superseded by Impl Notes #2 math. 104dp used.

### Coil AsyncImage: first-try success

`file:///android_asset/${filter.assetDir}/frame_00.png` URI loaded without any custom `ImageLoader` configuration. Coil 2.7.0 ships with a built-in `AssetUriFetcher` that handles this scheme. `LocalContext.current` passed to `ImageRequest.Builder` (idiomatic; equivalent to `LocalView.current.context`).

### ColorPainter import fix (Rule 1 auto-fix)

The plan template used `import androidx.compose.ui.graphics.ColorPainter`. That class path does not exist in Compose — it lives at `androidx.compose.ui.graphics.painter.ColorPainter`. Compile caught it immediately; fixed in the same task before commit.

### Phase 3 fix preservation verified

| Contract | Grep result | Status |
|----------|-------------|--------|
| `vm.onShutterTapped()` | 2 matches (KDoc + code) | PRESERVED |
| `HapticFeedbackConstants.LONG_PRESS` | 1 match | PRESERVED |
| `OneShotEvent.FilterLoadError` | 1 match | PRESERVED |
| `Text(if (uiState.isRecording) "REC..." else "TEST 5s")` | 1 match | PRESERVED |
| Flip button `OutlinedButton(` at TopEnd | 1 match (code only) | PRESERVED |
| Capture flash `AnimatedVisibility` | 1 match | PRESERVED |
| Permission gate `Column` | 1 match | PRESERVED |
| `DisposableEffect` orientation listener | 1 match | PRESERVED |
| `vm.events.collect` Toast handler | 1 match | PRESERVED |

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] ColorPainter wrong import path**
- **Found during:** Task 1 compile (`:app:compileDebugKotlin`)
- **Issue:** Plan template used `import androidx.compose.ui.graphics.ColorPainter` — no such class; `ColorPainter` lives in `androidx.compose.ui.graphics.painter.ColorPainter`
- **Fix:** Changed import to `androidx.compose.ui.graphics.painter.ColorPainter`
- **Files modified:** `FilterPicker.kt`
- **Commit:** b856325 (fixed before commit; no separate fix commit)

No other deviations — plan executed as written.

## Known Stubs

None. All picker data flows from `uiState.filters` (populated by `CameraViewModel.bind()` reading `FilterCatalog.all` — 15 entries). The Coil `placeholder` / `error` painters show a dark gray square while thumbnails load — this is intentional design per 04-UI-SPEC §3 "Loading" state, not a stub.

## Threat Surface Scan

No new network endpoints, auth paths, or external file access. `file:///android_asset/` reads are on-device, read-only, sandboxed to app assets. T-04-05 (rapid-tap race) mitigated by `vm.onSelectFilter(id)` dedupe guard in ViewModel (Plan 04-05). No new threat flags.

## Self-Check: PASSED

| Check | Result |
|-------|--------|
| `FilterPicker.kt` exists | FOUND |
| `FilterPicker.kt` has `fun FilterPicker` | FOUND |
| `FilterPicker.kt` has `file:///android_asset/` URI | FOUND |
| `FilterPicker.kt` has `animateFloatAsState` + `FastOutSlowInEasing` | FOUND |
| `FilterPicker.kt` has `1.15f` scale | FOUND |
| `FilterPicker.kt` has `2.dp` border on selected | FOUND |
| `FilterPicker.kt` has `VIRTUAL_KEY` haptic | FOUND |
| `FilterPicker.kt` has `Role.Button` semantics | FOUND |
| `CameraScreen.kt` has `FilterPicker(` | FOUND |
| `CameraScreen.kt` has `vm.onSelectFilter` | FOUND |
| `CameraScreen.kt` has `104.dp` bottom padding | FOUND |
| `CameraScreen.kt` Cycle button code removed | VERIFIED (grep shows comment only, no OutlinedButton) |
| `CameraScreen.kt` Flip OutlinedButton preserved | FOUND (1 code occurrence) |
| `CameraScreen.kt` shutter + LONG_PRESS haptic | FOUND |
| `CameraScreen.kt` TEST 5s button preserved | FOUND |
| `CameraScreen.kt` FilterLoadError Toast branch | FOUND |
| `CameraScreen.kt` capture flash AnimatedVisibility | FOUND |
| Commits b856325 + a15f113 exist | FOUND |
| `:app:testDebugUnitTest` exits 0 | PASSED |
| `:app:assembleDebug` exits 0 | PASSED |
