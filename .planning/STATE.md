---
gsd_state_version: 1.0
milestone: v1.0
milestone_name: milestone
status: executing
stopped_at: Completed 02-03-PLAN.md — detector pipeline (OneEuroFilter + FaceDetectorClient + FaceSnapshot) landed; OneEuroFilterTest + FaceDetectorOptionsTest GREEN
last_updated: "2026-04-19T09:08:42.468Z"
progress:
  total_phases: 7
  completed_phases: 1
  total_plans: 10
  completed_plans: 7
  percent: 70
---

# State: Bugzz

**Last updated:** 2026-04-19

## Project Reference

**Core Value:** Smooth live AR preview with bug sprites tracking face landmarks. If the live preview stutters or bugs don't stick to the face, everything else is meaningless.

**Current Focus:** Phase 02 — Camera Preview + Face Detection + Coordinate Validation

**Milestone:** v1 — feature-parity clone of `com.insect.filters.funny.prank.bug.filter.face.camera` v1.2.7, MINUS monetization and i18n.

## Current Position

Phase: 02 (Camera Preview + Face Detection + Coordinate Validation) — EXECUTING
Plan: 4 of 6

- **Phase:** 2
- **Plan:** 03 complete — detector pipeline landed: OneEuroFilter + LandmarkSmoother + FaceSnapshot + FaceDetectorClient (MlKitAnalyzer + 1€ smoothing + AtomicReference) + CameraLensProvider + FaceLandmarkMapper stub; OneEuroFilterTest (4/4) + FaceDetectorOptionsTest (1/1) GREEN; next up 02-04 (render + camera lifecycle: OverlayEffectBuilder + CameraController — replacing the Rule 3 compile-unblock stubs)
- **Status:** Executing Phase 02
- **Progress:** [███████░░░] 70%

### Phase Map

```
Phase 1: Foundation & Skeleton                            [ complete ]
Phase 2: Camera + Face Detection + Coord Validation       [ executing — 3/6 plans done ]
Phase 3: First Filter End-to-End + Photo Capture          [ pending ]
Phase 4: Filter Catalog + Picker + Face Filter Mode       [ pending ]
Phase 5: Video Recording + Audio + Insect Filter Mode     [ pending ]
Phase 6: UX Polish (Splash/Home/Onboarding/...)           [ pending ]
Phase 7: Performance & Device Matrix                      [ pending ]
```

## Performance Metrics

(Populated during execution)

| Metric | Value | Target |
|--------|-------|--------|
| Phases complete | 0/7 | 7 |
| v1 requirements complete | 0/67 | 67 |
| Current phase plans | —/— | — |
| Phase 02 P01 | 3m 17s | 3 tasks | 5 files |
| Phase 02 P02 | 10m 13s | 3 tasks | 5 files |
| Phase 02 P03 | 8m 07s | 3 tasks | 7 files |

## Accumulated Context

### Key Decisions Locked in Research

1. **UI Toolkit:** Jetpack Compose (not Views) — CameraX-Compose stable in 1.6; greenfield + solo dev + 6 screens is Compose sweet spot. (SUMMARY.md Resolution #1)
2. **Face Tracking:** ML Kit Face Detection contour mode (NOT Face Mesh) — 2D sprites don't need 478-point mesh; Face Detection has first-class `MlKitAnalyzer` + `trackingId`. (SUMMARY.md Resolution #2)
3. **Rendering:** 2D Canvas via `OverlayEffect` (NO Filament) — PBR engine is cargo-cult for sprite blits; escalation path is custom GL `CameraEffect`, never Filament. (SUMMARY.md Resolution #3)
4. **CameraX version:** 1.6.0 uniform across all artifacts — first version with stable `camera-effects` + `camera-compose`.
5. **ML Kit model:** Bundled (`com.google.mlkit:face-detection:16.1.7`) — offline first-launch, no Play Services model-download race.
6. **Persistence:** MediaStore for captures, DataStore for prefs — no Room DB for MVP.

### Key Decisions During Execution

1. **[Phase 02-01] Nyquist-TDD Wave 0 gate:** 4 failing unit-test files for CAM-03/04/05/06/09 land before any `feat(02-...)` commit; SUT classes land in Plans 02-03 and 02-04. (02-01-SUMMARY.md)
2. **[Phase 02-01] Testability seam pattern:** Android-Handler-dependent SDK wrappers (`OverlayEffectBuilder`) expose config surface as companion `const val`/`val` (`TARGETS`, `QUEUE_DEPTH`) so contracts are unit-testable without Robolectric. (02-01-SUMMARY.md)
3. **[Phase 02-01] Provider-factory seam on CameraController:** Plan 02-04 must add constructor default param `providerFactory: suspend (Context) -> ProcessCameraProvider = { ProcessCameraProvider.awaitInstance(it) }` so `CameraControllerTest` can un-`@Ignore`. (02-01-SUMMARY.md)
4. **[Phase 02-02] Compose BOM 2026.04.00 not yet published:** Plan prescribed bumping composeBom 2026.03.00 → 2026.04.00 (per CLAUDE.md Executive Recommendation + 02-RESEARCH.md), but that BOM is not on Google Maven as of 2026-04-19. Reverted to 2026.03.00; revisit when BOM lands. (Rule 1 auto-fix in 02-02-SUMMARY.md)
5. **[Phase 02-02] compileSdk 35 → 36:** CameraX 1.6.0 AAR metadata requires compileSdk>=36. Bumped `app/build.gradle.kts compileSdk = 36` (targetSdk stays 35 per CLAUDE.md lock). Phase 1's pre-armed `android.suppressUnsupportedCompileSdk=36` in `gradle.properties` silences the cross-version warning. (Rule 3 auto-fix in 02-02-SUMMARY.md)
6. **[Phase 02-02] Hilt CameraModule with named single-thread Executors:** `@Named("cameraExecutor")` (thread `BugzzCameraExecutor`) + `@Named("renderExecutor")` (thread `BugzzRenderExecutor`) — D-18 threading model. Named threads surface in logcat/profiler for Xiaomi 13T debugging. (02-02-SUMMARY.md)
7. **[Phase 02-03] FaceDetectorOptions R8 obfuscation workaround:** Published ML Kit AAR obfuscates accessors to `zza()..zzg()` — Plan 01's Kotlin-property-syntax assertions (`opts.performanceMode`, `opts.isTrackingEnabled`, etc.) don't compile. Rewrote `FaceDetectorOptionsTest` to combine `equals()` against expected options (definitive 6-field gate via ML Kit's equals override) + `toString()` substring assertions per field for diagnostic clarity. Observed toString format: `FaceDetectorOptions{landmarkMode=1, contourMode=2, classificationMode=1, performanceMode=1, trackingEnabled=true, minFaceSize=0.15}`. Rule 3 auto-fix. (02-03-SUMMARY.md)
8. **[Phase 02-03] Compile-unblock placeholder stubs for Plan 02-04 SUTs:** `OverlayEffectBuilderTest.kt` + `CameraControllerTest.kt` (committed by Plan 02-01) reference SUTs not landing until Plan 02-04, blocking the whole test sourceset from compiling. Shipped minimal placeholder `OverlayEffectBuilder.kt` (wrong `TARGETS=0` + `QUEUE_DEPTH=-1` to keep `OverlayEffectBuilderTest` intentionally RED at runtime) and `CameraController.kt` (NotImplementedError body; `CameraControllerTest` methods stay `@Ignore`d). Both stubs carry forward-pointing KDoc to Plan 02-04. **Plan 02-04 MUST fully REPLACE both files, not edit on top.** Rule 3 auto-fix. (02-03-SUMMARY.md)
9. **[Phase 02-03] FaceDetectorClient detector pipeline landed:** `@Singleton` with `@Inject constructor(@Named("cameraExecutor") cameraExecutor)` (D-18). `buildOptions()` companion returns D-15 exact options. `createAnalyzer()` returns `MlKitAnalyzer(listOf(detector), COORDINATE_SYSTEM_SENSOR, cameraExecutor, consumer)` (D-17). Consumer runs `smoother.retainActive(activeIds)` FIRST, maps to `SmoothedFace` via per-contour 1€ filter keyed on trackingId, writes `AtomicReference<FaceSnapshot>` (D-19). Emits `Timber.tag("FaceTracker").v("t=%d id=%s bb=%d,%d contours=%d", ...)` per face — never raw landmark coord lists (T-02-06). `onLensFlipped()` clears smoother state (D-25 / PITFALLS #6). OneEuroFilterTest (4/4) + FaceDetectorOptionsTest (1/1) GREEN; OverlayEffectBuilderTest + CameraControllerTest remain RED / @Ignored. (02-03-SUMMARY.md)

### Architectural Gates

- **Phase 2 exit criterion:** Debug red-rect overlay pixel-perfect on face in portrait + landscape + front/back swap. Recording a 5-second test `.mp4` contains the red rect (proves `OverlayEffect` baked into VIDEO_CAPTURE target).
- **Phase 3 integration test:** One filter + photo capture proves three-stream compositing end-to-end.
- **Phase 7 escalation trigger:** Avg frame time >33ms over 10-second recording → custom GL `CameraEffect` (NOT Filament).

### Active Todos

- [ ] Plan Phase 1 via `/gsd-plan-phase 1`
- [ ] Set up real Android 9+ device via USB ADB for on-device testing (required from Phase 2 onward)
- [ ] Extract bug sprite assets from reference APK `reference/com.insect.filters.funny.prank.bug.filter.face.camera.apk` before Phase 3

### Blockers

None.

### Open Questions (resolved during execution)

- `DCIM/Bugzz/` vs `Pictures/Bugzz/` save convention → inspect reference runtime in Phase 3; default `DCIM/Bugzz/`.
- Reference's exact video `MAX_DURATION` → default 60s in Phase 5; verify against DEX if time permits.
- Front-camera photo mirror-at-save convention → inspect reference in Phase 3, match.
- 1€ filter parameter tuning → ship literature defaults (1.0 / 0.007 / 1.0), tune empirically in Phase 3.
- Exact bug-filter count/types → bundle 15-25 common categories in Phase 4; reference catalog is server-driven and un-extractable.

## Session Continuity

**Last agent:** gsd-execute-phase (Plan 02-03 executor)
**Last action:** Completed 02-03-PLAN.md — detector pipeline landed (OneEuroFilter + LandmarkSmoother + FaceSnapshot + FaceDetectorClient + FaceLandmarkMapper stub + CameraLensProvider). OneEuroFilter: Casiez CHI 2012 port with minCutoff=1.0 / beta=0.007 / dCutoff=1.0 defaults (D-20). FaceDetectorClient: @Singleton @Inject(@Named("cameraExecutor")) with buildOptions() companion returning exact D-15 options, createAnalyzer() using COORDINATE_SYSTEM_SENSOR (D-17), per-contour 1€ smoothing keyed on trackingId with retainActive/clear (D-22/25), AtomicReference<FaceSnapshot> producer (D-19), Timber FaceTracker verbose logs using T-02-06-safe "t=%d id=%s bb=%d,%d contours=%d" format. OneEuroFilterTest (4/4) + FaceDetectorOptionsTest (1/1) GREEN. Two Rule 3 auto-fixes: (1) FaceDetectorOptionsTest rewrite for R8 obfuscation (opts.performanceMode etc. don't exist — used toString/equals hybrid); (2) placeholder OverlayEffectBuilder + CameraController stubs so test sourceset compiles (OverlayEffectBuilderTest stays RED at runtime; CameraControllerTest stays @Ignored). Plan 02-04 must REPLACE these stubs.

**Stopped at:** Completed 02-03-PLAN.md — detector pipeline (OneEuroFilter + FaceDetectorClient + FaceSnapshot) landed; OneEuroFilterTest + FaceDetectorOptionsTest GREEN

**Next expected action:** Execute 02-04-PLAN.md (render + camera lifecycle: OverlayEffectBuilder with real TARGETS/QUEUE_DEPTH constants + OverlayEffect.onDrawListener, CameraController with providerFactory seam + UseCaseGroup of 4 use cases + STRATEGY_KEEP_ONLY_LATEST + OverlayEffect attached; turns OverlayEffectBuilderTest GREEN and un-@Ignores CameraControllerTest).

**Files modified this session (Plan 02-03):**

- `app/src/main/java/com/bugzz/filter/camera/detector/OneEuroFilter.kt` (created — 95 LOC; OneEuroFilter + LandmarkSmoother)
- `app/src/main/java/com/bugzz/filter/camera/detector/FaceSnapshot.kt` (created — 37 LOC; SmoothedFace + FaceSnapshot data classes + EMPTY)
- `app/src/main/java/com/bugzz/filter/camera/detector/FaceLandmarkMapper.kt` (created — 29 LOC; Phase 3 stub)
- `app/src/main/java/com/bugzz/filter/camera/detector/FaceDetectorClient.kt` (created — 127 LOC; @Singleton MlKitAnalyzer + 1€ + AtomicReference + Timber)
- `app/src/main/java/com/bugzz/filter/camera/camera/CameraLensProvider.kt` (created — 18 LOC; CameraLens enum + next())
- `app/src/main/java/com/bugzz/filter/camera/render/OverlayEffectBuilder.kt` (created — 41 LOC; Rule 3 compile-unblock stub with wrong constants, Plan 02-04 replaces)
- `app/src/main/java/com/bugzz/filter/camera/camera/CameraController.kt` (created — 35 LOC; Rule 3 compile-unblock stub with NotImplementedError, Plan 02-04 replaces)
- `app/src/test/java/com/bugzz/filter/camera/detector/FaceDetectorOptionsTest.kt` (modified — Rule 3 rewrite for R8 obfuscation)
- `.planning/phases/02-camera-preview-face-detection-coordinate-validation/02-03-SUMMARY.md` (created)
- `.planning/STATE.md` (updated)

---
*State initialized: 2026-04-18*
