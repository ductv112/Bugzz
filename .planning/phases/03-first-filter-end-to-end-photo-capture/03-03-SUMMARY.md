---
phase: "03"
plan: "03"
subsystem: filter-engine
tags: [filter, sprite, animation, asset-loader, face-landmark, overlay-effect]
dependency_graph:
  requires: [03-01, 03-02]
  provides: [live-bug-sprite-rendering, asset-preload, landmark-anchor-resolution]
  affects: [OverlayEffectBuilder, FilterEngine, FaceLandmarkMapper, AssetLoader, FilterCatalog]
tech_stack:
  added:
    - LruCache<String, Bitmap> for sprite frame cache (min(32MB, maxMemory/8))
    - AtomicReference<FilterDefinition?> for lock-free filter swap
    - BitmapFactory.decodeStream with ARGB_8888 / RGB_565 config from manifest
    - Node.js base64 decode script to extract Lottie-embedded PNG frames from reference APK
  patterns:
    - Flipbook index via absolute timestampNanos / frameDurationNanos % frameCount
    - D-11 no-ghost: early-return from onDraw when assetLoader.get() returns null
    - CAM-07: canvas.setMatrix(sensorToBufferTransform) before every drawBitmap
    - T-03-05: Timber logs filterId+frameIdx only, never PointF landmark coords
    - Rule 1 bug fix: FilterEngineTest setFilter_swap verify corrected to times(2)
key_files:
  created:
    - app/src/main/assets/sprites/ant_on_nose_v1/manifest.json
    - app/src/main/assets/sprites/ant_on_nose_v1/frame_00.png … frame_34.png (35 frames)
    - app/src/main/assets/sprites/spider_on_forehead_v1/manifest.json
    - app/src/main/assets/sprites/spider_on_forehead_v1/frame_00.png … frame_22.png (23 frames)
    - app/src/main/assets/sprites/test_filter/ (Robolectric test fixture)
    - app/src/main/assets/sprites/bad_filter/ (malformed PNG fixture for T-03-02)
    - reference/APK_SHA256.txt
  modified:
    - app/src/main/java/com/bugzz/filter/camera/filter/FilterCatalog.kt (production entries)
    - app/src/main/java/com/bugzz/filter/camera/filter/AssetLoader.kt (full LruCache body)
    - app/src/main/java/com/bugzz/filter/camera/detector/FaceLandmarkMapper.kt (7-anchor ladder)
    - app/src/main/java/com/bugzz/filter/camera/render/FilterEngine.kt (flipbook + draw)
    - app/src/main/java/com/bugzz/filter/camera/render/OverlayEffectBuilder.kt (FilterEngine wired)
    - app/build.gradle.kts (isIncludeAndroidResources = true)
    - app/src/test/java/com/bugzz/filter/camera/render/FilterEngineTest.kt (un-Ignored, bug fix)
    - app/src/test/java/com/bugzz/filter/camera/detector/FaceLandmarkMapperTest.kt (un-Ignored)
    - app/src/test/java/com/bugzz/filter/camera/filter/AssetLoaderTest.kt (un-Ignored)
decisions:
  - "Flipbook uses absolute timestampNanos (not relative to setFilter) for stable deterministic phase"
  - "Sprite decode errors normalized to IllegalArgumentException (T-03-02) — Robolectric throws RuntimeException wrapping IIOException"
  - "Test fixtures placed in src/main/assets (not src/test/resources) for Robolectric ShadowArscAssetManager10 to serve them via includeAndroidResources=true"
  - "FilterEngine.onDraw draws FIRST in OverlayEffectBuilder listener; DebugOverlayRenderer second (D-27 Claude's Discretion)"
  - "Reference APK uses Lottie JSON with embedded base64 PNGs — extracted via Node.js decode script"
metrics:
  duration: "session continuation (Tasks 3b + 4)"
  completed: "2026-04-19"
  tasks_completed: 5
  files_modified: 13
---

# Phase 03 Plan 03: First Filter End-to-End — Production Filter Stack Summary

**One-liner:** Full live bug sprite pipeline: 35-frame ant + 23-frame spider extracted from reference APK, LruCache asset loader, 7-anchor FaceLandmarkMapper, flipbook FilterEngine with BugBehavior.Static, wired into OverlayEffectBuilder.

## Tasks Completed

| Task | Name | Commit | Key Files |
|------|------|--------|-----------|
| 1 | Sprite extraction from reference APK | 2efdc6e | 58 PNG frames in assets/, 2 manifests, APK_SHA256.txt |
| 2 | FilterCatalog + AssetLoader + SpriteManifest | 266a519 | FilterCatalog.kt, AssetLoader.kt, build.gradle.kts |
| 3a | FaceLandmarkMapper production body | 55fb79f | FaceLandmarkMapper.kt, FaceLandmarkMapperTest un-Ignored |
| 3b | FilterEngine production body | cab4b69 | FilterEngine.kt, FilterEngineTest un-Ignored (8 tests) |
| 4 | OverlayEffectBuilder FilterEngine dispatch | 7527d24 | OverlayEffectBuilder.kt |

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Robolectric asset serving requires includeAndroidResources=true**
- **Found during:** Task 2 (AssetLoaderTest.preload_* tests)
- **Issue:** Robolectric's ShadowArscAssetManager10 could not serve files from `src/main/assets/` in unit tests — FileNotFoundException on `assets.open(path)`
- **Fix:** Added `isIncludeAndroidResources = true` to `testOptions.unitTests` in `app/build.gradle.kts`; copied test fixtures (`test_filter/`, `bad_filter/`) to `src/main/assets/` so AGP merges them into the test asset bundle
- **Files modified:** `app/build.gradle.kts`, `app/src/main/assets/sprites/test_filter/`, `app/src/main/assets/sprites/bad_filter/`
- **Commit:** 266a519

**2. [Rule 1 - Bug] Robolectric BitmapFactory throws RuntimeException not returning null for malformed PNG**
- **Found during:** Task 2 (AssetLoaderTest.preload_malformedPng_throwsIllegalArgument)
- **Issue:** Robolectric's ShadowBitmapFactory wraps `javax.imageio.IIOException` as `RuntimeException` instead of returning null. T-03-02 contract requires `IllegalArgumentException`.
- **Fix:** Wrapped `BitmapFactory.decodeStream` in try/catch: re-throws `IllegalArgumentException` as-is, wraps all other `Exception` as `IllegalArgumentException` with original as cause
- **Files modified:** `app/src/main/java/com/bugzz/filter/camera/filter/AssetLoader.kt`
- **Commit:** 266a519

**3. [Rule 1 - Bug] Reference APK uses Lottie JSON with embedded base64 PNGs, not standalone frame PNGs**
- **Found during:** Task 1 (sprite extraction)
- **Issue:** Plan assumed standalone `frame_NN.png` files in APK `res/raw/`. Actual format: Lottie JSON files containing `"p": "<base64>"` entries per frame.
- **Fix:** Used Node.js to parse JSON and decode base64 data to individual PNG files. Extracted 35 ant frames from `home_lottie.json` (InsectFilter_transparent layer) and 23 spider frames from `spider_prankfilter.json`.
- **Files modified:** `app/src/main/assets/sprites/ant_on_nose_v1/`, `app/src/main/assets/sprites/spider_on_forehead_v1/`
- **Commit:** 2efdc6e

**4. [Rule 1 - Bug] FilterEngineTest setFilter_swap verify used times(1) but get() is called twice**
- **Found during:** Task 3b (FilterEngineTest.setFilter_swap_resetsBugStateFrameIndex)
- **Issue:** Test called `verify(mockAssetLoader).get(...)` (implying `times(1)`) but two `onDraw` calls across the filter swap each invoke `assetLoader.get()` — TooManyActualInvocations.
- **Fix:** Changed verify to `verify(mockAssetLoader, times(2)).get(any(), frameIdxCaptor.capture())`. `lastValue` correctly captures the filterB call index=0.
- **Files modified:** `app/src/test/java/com/bugzz/filter/camera/render/FilterEngineTest.kt`
- **Commit:** cab4b69

**5. [Rule 1 - Bug] Flipbook startTimestampNanos sentinel 0L conflicts with real t=0 timestamps**
- **Found during:** Task 3b (FilterEngineTest.flipbookIndex_advancesOverTime)
- **Issue:** Using `startTimestampNanos = 0L` as "not set" sentinel collides with test's first timestamp of 0L, causing the start to be reset on every frame until a non-zero timestamp is seen. Result: `[0, 0, 1, 2]` instead of `[0, 1, 2, 0]`.
- **Fix:** Removed start-time-relative approach entirely. Flipbook uses absolute `timestampNanos / frameDurationNanos % frameCount` — simpler and test-compatible.
- **Files modified:** `app/src/main/java/com/bugzz/filter/camera/render/FilterEngine.kt`
- **Commit:** cab4b69

## Known Stubs

None that block plan goals. `BugBehavior.Crawl`, `BugBehavior.Swarm`, and `BugBehavior.Fall` throw `NotImplementedError` — intentional Phase 4 stubs documented in `BugBehavior.kt` KDoc.

## Threat Flags

None. No new network endpoints, auth paths, file access patterns, or schema changes introduced. Asset loading reads from `assets/` (app-bundled, read-only). LruCache is in-process memory only.

## Self-Check: PASSED

| Check | Result |
|-------|--------|
| FilterEngine.kt exists | FOUND |
| OverlayEffectBuilder.kt exists | FOUND |
| ant_on_nose_v1/manifest.json exists | FOUND |
| spider_on_forehead_v1/manifest.json exists | FOUND |
| commit cab4b69 exists | FOUND |
| commit 7527d24 exists | FOUND |
| testDebugUnitTest BUILD SUCCESSFUL | PASS |
| assembleDebug BUILD SUCCESSFUL | PASS |
