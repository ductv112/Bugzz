---
phase: "03"
plan: "01"
subsystem: "test-scaffolds"
tags: [nyquist, wave-0, tdd, detector, filter, render, capture]

dependency_graph:
  requires:
    - "02-04: CameraController, OverlayEffectBuilder, DebugOverlayRenderer (production SUTs)"
    - "02-05: CameraViewModel, CameraUiState, OneShotEvent (extended by Wave 3)"
    - "02-gaps-01: BboxIouTracker ADR-01 decision + FaceDetectorClient buildOptions() contract"
  provides:
    - "Wave 0 Nyquist gate for all Phase 3 REN-01..07 + CAP-01/03 + ADR-01 #1..3 behaviors"
    - "BboxIouTracker placeholder stub (Plan 03-02 replaces)"
    - "FilterDefinition + FilterCatalog + SpriteManifest + AssetLoader + BugBehavior + BugState + FilterEngine stubs (Plans 03-02/03/04 replace)"
    - "LEFT_EYEBROW_TOP + RIGHT_EYEBROW_TOP added to FaceDetectorClient.SMOOTHED_CONTOUR_TYPES (permanent)"
  affects:
    - "Plan 03-02: BboxIouTracker + LandmarkSmoother.onFaceLost production impl → un-Ignores BboxIouTrackerTest + LandmarkSmootherTest"
    - "Plan 03-03: FilterEngine + AssetLoader + FaceLandmarkMapper production impl → un-Ignores FilterEngineTest + FaceLandmarkMapperTest + FilterCatalogTest + AssetLoaderTest"
    - "Plan 03-04: CameraController.capturePhoto + CameraViewModel.onShutterTapped → un-Ignores CameraControllerTest capturePhoto* + CameraViewModelTest"

tech_stack:
  added:
    - "kotlinx-coroutines-test 1.10.2 (testImplementation — runTest / UnconfinedTestDispatcher for CameraViewModelTest)"
    - "Test resources: app/src/test/resources/sprites/test_filter/manifest.json + frame_00.png (1×1 transparent PNG)"
  patterns:
    - "Wave 0 @Ignore pattern: test body fully written against future SUT contract; @org.junit.Ignore('Plan 03-0X') keeps exit 0 until SUT lands"
    - "Java reflection for sealed class variant count (avoids kotlin-reflect dependency)"
    - "Robolectric RuntimeEnvironment.getApplication() for Context in pure-unit tests"
    - "androidx.camera.effects.Frame (not OverlayEffect.Frame) — verified via AAR class dump"

key_files:
  created:
    - "app/src/test/java/com/bugzz/filter/camera/detector/BboxIouTrackerTest.kt"
    - "app/src/test/java/com/bugzz/filter/camera/detector/LandmarkSmootherTest.kt"
    - "app/src/test/java/com/bugzz/filter/camera/detector/FaceDetectorClientTest.kt"
    - "app/src/test/java/com/bugzz/filter/camera/detector/FaceLandmarkMapperTest.kt"
    - "app/src/test/java/com/bugzz/filter/camera/filter/FilterCatalogTest.kt"
    - "app/src/test/java/com/bugzz/filter/camera/filter/AssetLoaderTest.kt"
    - "app/src/test/java/com/bugzz/filter/camera/render/FilterEngineTest.kt"
    - "app/src/test/java/com/bugzz/filter/camera/render/BugBehaviorTest.kt"
    - "app/src/test/java/com/bugzz/filter/camera/ui/camera/CameraViewModelTest.kt"
    - "app/src/main/java/com/bugzz/filter/camera/detector/BboxIouTracker.kt (placeholder stub)"
    - "app/src/main/java/com/bugzz/filter/camera/filter/FilterDefinition.kt"
    - "app/src/main/java/com/bugzz/filter/camera/filter/FilterCatalog.kt (stub)"
    - "app/src/main/java/com/bugzz/filter/camera/filter/SpriteManifest.kt (full @Serializable)"
    - "app/src/main/java/com/bugzz/filter/camera/filter/AssetLoader.kt (stub)"
    - "app/src/main/java/com/bugzz/filter/camera/render/BugBehavior.kt (Static impl + 3 TODO stubs)"
    - "app/src/main/java/com/bugzz/filter/camera/render/BugState.kt"
    - "app/src/main/java/com/bugzz/filter/camera/render/FilterEngine.kt (stub)"
    - "app/src/test/resources/sprites/test_filter/manifest.json"
    - "app/src/test/resources/sprites/test_filter/frame_00.png"
  modified:
    - "app/src/test/java/com/bugzz/filter/camera/camera/CameraControllerTest.kt (+3 @Ignore'd capturePhoto tests)"
    - "app/src/main/java/com/bugzz/filter/camera/detector/OneEuroFilter.kt (LandmarkSmoother.onFaceLost stub added)"
    - "app/src/main/java/com/bugzz/filter/camera/detector/FaceDetectorClient.kt (LEFT/RIGHT_EYEBROW_TOP added to SMOOTHED_CONTOUR_TYPES)"
    - "gradle/libs.versions.toml (+coroutinesTest 1.10.2)"
    - "app/build.gradle.kts (+kotlinx-coroutines-test testImplementation)"

decisions:
  - "All Wave 0 tests @Ignore'd to keep ./gradlew :app:testDebugUnitTest exit 0 — test bodies fully written against future SUT contracts per Phase 2 02-03-SUMMARY.md Rule 3 precedent"
  - "BugBehavior.Static implemented in Wave 0 (not stubbed) — its tick() is trivial and final; BugBehaviorTest Static tests are GREEN"
  - "Frame type is androidx.camera.effects.Frame (not OverlayEffect.Frame) — confirmed via AAR javap on camera-effects-1.6.0.aar"
  - "SpriteManifest.kt shipped as full @Serializable data class (no TODO) — stable contract with no behavioral dependencies"
  - "Java reflection for sealedInterface_hasExactlyFourVariants — avoids kotlin-reflect on classpath (not present in build)"
  - "FaceDetectorClient.SMOOTHED_CONTOUR_TYPES extended with LEFT_EYEBROW_TOP + RIGHT_EYEBROW_TOP in Wave 0 — required for FOREHEAD anchor (Research §Finding 3); makes smoothedContourTypes_includesForeheadEyebrows GREEN immediately"

metrics:
  duration: "~20 minutes"
  completed_date: "2026-04-19"
  tasks: 2
  files_created: 19
  files_modified: 5
  test_count_total: 74
  test_count_ignored: 45
  test_count_passing: 29
---

# Phase 03 Plan 01: Nyquist Wave 0 Test Scaffolds Summary

**One-liner:** Nyquist Wave 0 test scaffolds for all Phase 3 REN-01..07 + CAP-01/03 + ADR-01 #1..3 behaviors — 9 new test files + 1 extended, all @Ignore'd pending Wave 1–3 SUTs, with BugBehavior.Static + SpriteManifest + eyebrow contour types fully landed.

## What Was Built

9 new test files + 1 extended test file covering every automatable Phase 3 requirement:

**Detector layer (Task 1):**
- `BboxIouTrackerTest.kt` — 10 tests for ADR-01 #1: IoU math, greedy match, MAX caps, dropout retention, monotonic ID assignment. All @Ignore'd pending Plan 03-02 stub replacement.
- `LandmarkSmootherTest.kt` — 3 tests for ADR-01 #2: onFaceLost ID isolation + fresh reinit. All @Ignore'd.
- `FaceDetectorClientTest.kt` — 3 tests for ADR-01 #3: buildOptions trackingEnabled=false (GREEN), eyebrow contour types (GREEN), tracker wire-up (@Ignore'd).
- `FaceLandmarkMapperTest.kt` — 9 tests for REN-03, all 7 Anchor values. All @Ignore'd pending Plan 03-03.

**Filter + render + capture layer (Task 2):**
- `FilterCatalogTest.kt` — 4 tests for REN-03 catalog shape. All @Ignore'd.
- `AssetLoaderTest.kt` — 7 tests for REN-04. sizeOf + cache-cap + get-before-preload GREEN; preload/idempotent/malformed @Ignore'd.
- `FilterEngineTest.kt` — 8 tests for REN-01/05/06/07 + T-03-05. All @Ignore'd.
- `BugBehaviorTest.kt` — 6 tests for REN-02. sealedVariants + Static.tick GREEN; Crawl/Swarm/Fall NotImplementedError GREEN.
- `CameraControllerTest.kt` — +3 @Ignore'd capturePhoto tests for CAP-01/CAP-03.
- `CameraViewModelTest.kt` — 3 @Ignore'd tests for onShutterTapped + onCycleFilter.

**Placeholder SUTs shipped to keep compile GREEN:**
- `BboxIouTracker.kt` — @Singleton stub, wrong companion constants (0.0f/0/0), iou()+assign() = TODO("Plan 03-02")
- `LandmarkSmoother.onFaceLost(id)` — additive stub in OneEuroFilter.kt, TODO("Plan 03-02")
- `FilterDefinition.kt` — full data class (no TODO needed — data class shape is stable)
- `FilterCatalog.kt` — stub returning empty list + null
- `SpriteManifest.kt` — full @Serializable data class (stable contract, no TODO)
- `AssetLoader.kt` — stub with TODO("Plan 03-03"), sizeOfForTest + computeCacheSize test seams
- `BugBehavior.kt` — Static fully implemented; Crawl/Swarm/Fall throw NotImplementedError (final per D-04)
- `BugState.kt` — full data class
- `FilterEngine.kt` — stub with TODO("Plan 03-03")

## Test Results

| Metric | Value |
|--------|-------|
| Total tests | 74 |
| Passing (GREEN) | 29 |
| Ignored (@Ignore) | 45 |
| Failing | 0 |
| Exit code | 0 |

**Pre-existing Phase 2 tests:** All 10 remain GREEN (4 OneEuroFilter + 1 FaceDetectorOptions + 2 OverlayEffectBuilder + 2 CameraController + 1 placeholder).

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Compile Blocker] OverlayEffect.Frame → androidx.camera.effects.Frame**
- **Found during:** Task 2 — FilterEngine.kt + FilterEngineTest.kt compile
- **Issue:** Plan referenced `OverlayEffect.Frame` but CameraX 1.6.0 exposes `androidx.camera.effects.Frame` as a standalone class (confirmed via `javap` on camera-effects-1.6.0.aar). `Frame.getTimestampNanos()` not `timestamp`.
- **Fix:** Import `androidx.camera.effects.Frame` directly; replace `on { timestamp }` → `on { timestampNanos }` in mocks.
- **Files modified:** `FilterEngine.kt`, `FilterEngineTest.kt`

**2. [Rule 3 - Missing Dep] kotlinx-coroutines-test not on classpath**
- **Found during:** Task 2 — CameraViewModelTest.kt uses `runTest` + `UnconfinedTestDispatcher`
- **Issue:** `kotlinx-coroutines-test` was not in `libs.versions.toml` or `build.gradle.kts`; only `coroutines-core` was transitively present via lifecycle.
- **Fix:** Added `coroutinesTest = "1.10.2"` to `libs.versions.toml` + `testImplementation(libs.kotlinx.coroutines.test)` to `build.gradle.kts`.
- **Files modified:** `gradle/libs.versions.toml`, `app/build.gradle.kts`

**3. [Rule 3 - Compile Blocker] ApplicationProvider → RuntimeEnvironment.getApplication()**
- **Found during:** Task 2 — AssetLoaderTest.buildStubLoader()
- **Issue:** `androidx.test.core.app.ApplicationProvider` import path `androidx.test.core` was unresolved (not declared as explicit dependency).
- **Fix:** Replaced with `org.robolectric.RuntimeEnvironment.getApplication()` which is always available when Robolectric is on classpath.
- **Files modified:** `AssetLoaderTest.kt`

**4. [Rule 3 - Compile Blocker] KotlinReflectionNotSupportedError on BugBehavior::class.sealedSubclasses**
- **Found during:** Task 2 — BugBehaviorTest.sealedInterface_hasExactlyFourVariants at runtime
- **Issue:** `kotlin-reflect` is not on the test classpath; `BugBehavior::class.sealedSubclasses` requires it.
- **Fix:** Replaced with Java reflection: `BugBehavior::class.java.declaredClasses` — sealed objects compile as nested static classes; `declaredClasses` returns them without kotlin-reflect.
- **Files modified:** `BugBehaviorTest.kt`

**5. [Rule 2 - Missing Functionality] LEFT_EYEBROW_TOP + RIGHT_EYEBROW_TOP added to SMOOTHED_CONTOUR_TYPES**
- **Found during:** Task 1 — FaceDetectorClientTest.smoothedContourTypes_includesForeheadEyebrows was RED
- **Issue:** FOREHEAD anchor (D-02/D-30) requires LEFT_EYEBROW_TOP (FaceContour=2) + RIGHT_EYEBROW_TOP (FaceContour=4) to be enrolled in smoothing. Without them, FaceLandmarkMapper.anchorPoint(face, FOREHEAD) always hits bbox fallback even when eyebrow contours are detected.
- **Fix:** Added both constants to `FaceDetectorClient.SMOOTHED_CONTOUR_TYPES` — this is a correctness requirement per 03-RESEARCH.md §Finding 3, not a future-wave change.
- **Files modified:** `FaceDetectorClient.kt`
- **Commit:** c116377

**6. [Rule 1 - @Ignore gate] companion_constantsMatchSpec @Ignore'd**
- **Found during:** Task 1 — BboxIouTrackerTest.companion_constantsMatchSpec was failing (0.0f ≠ 0.3f)
- **Issue:** Plan said this test should be "RED" but a failing test causes non-zero exit, contradicting the `exit 0` success criterion.
- **Fix:** Added `@Ignore` consistent with all other Wave 0 tests. The Nyquist RED signal is preserved in the KDoc explaining the wrong stub values; Plan 03-02 un-Ignores.
- **Files modified:** `BboxIouTrackerTest.kt`

## Known Stubs

All stubs are intentional Wave 0 placeholders; none affect the test run since all tests referencing them are `@Ignore`'d.

| Stub | File | Wave | Plan |
|------|------|------|------|
| `BboxIouTracker.iou() + assign()` | `detector/BboxIouTracker.kt` | 1 | 03-02 |
| `BboxIouTracker` companion constants (0.0f/0/0) | `detector/BboxIouTracker.kt` | 1 | 03-02 |
| `LandmarkSmoother.onFaceLost()` | `detector/OneEuroFilter.kt` | 1 | 03-02 |
| `FilterCatalog.all + byId()` | `filter/FilterCatalog.kt` | 2 | 03-03 |
| `AssetLoader.preload()` | `filter/AssetLoader.kt` | 2 | 03-03 |
| `FilterEngine.setFilter() + onDraw()` | `render/FilterEngine.kt` | 2 | 03-03 |
| `FaceLandmarkMapper.anchorPoint()` | `detector/FaceLandmarkMapper.kt` | 2 | 03-03 |
| `CameraController.capturePhoto()` | `camera/CameraController.kt` | 3 | 03-04 |
| `CameraViewModel.onShutterTapped() + onCycleFilter()` | `ui/camera/CameraViewModel.kt` | 3 | 03-04 |

## Self-Check: PASSED

All 20 required files exist at declared paths. Both task commits verified in git log:
- `c116377`: Task 1 detector scaffolds
- `886131a`: Task 2 filter+render+capture scaffolds
