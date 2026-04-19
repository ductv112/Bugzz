---
gsd_state_version: 1.0
milestone: v1.0
milestone_name: milestone
status: executing
stopped_at: Completed 02-02-PLAN.md — CameraX 1.6 + ML Kit 16.1.7 + Timber + Mockito deps wired; portrait-lock + Timber debug-tree + Hilt CameraModule landed
last_updated: "2026-04-19T08:53:24.287Z"
progress:
  total_phases: 7
  completed_phases: 1
  total_plans: 10
  completed_plans: 6
  percent: 60
---

# State: Bugzz

**Last updated:** 2026-04-19

## Project Reference

**Core Value:** Smooth live AR preview with bug sprites tracking face landmarks. If the live preview stutters or bugs don't stick to the face, everything else is meaningless.

**Current Focus:** Phase 02 — Camera Preview + Face Detection + Coordinate Validation

**Milestone:** v1 — feature-parity clone of `com.insect.filters.funny.prank.bug.filter.face.camera` v1.2.7, MINUS monetization and i18n.

## Current Position

Phase: 02 (Camera Preview + Face Detection + Coordinate Validation) — EXECUTING
Plan: 3 of 6

- **Phase:** 2
- **Plan:** 02 complete — dep catalog + Hilt CameraModule + portrait-lock + Timber debug-tree landed; next up 02-03 (detector layer: OneEuroFilter + FaceDetectorClient)
- **Status:** Executing Phase 02
- **Progress:** [██████░░░░] 60%

### Phase Map

```
Phase 1: Foundation & Skeleton                            [ complete ]
Phase 2: Camera + Face Detection + Coord Validation       [ executing — 2/6 plans done ]
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

**Last agent:** gsd-execute-phase (Plan 02-02 executor)
**Last action:** Completed 02-02-PLAN.md — dependency infrastructure for Phase 2 landed. Pinned CameraX 1.6.0 (8 artifacts) + ML Kit 16.1.7 + Timber 5.0.1 + Mockito 5.11.0 + mockito-kotlin 5.2.1 in version catalog; wired all 11 runtime + 2 test deps into app/build.gradle.kts; portrait-locked MainActivity with defensive configChanges; planted Timber.DebugTree inside `if (BuildConfig.DEBUG)` guard (T-02-02 biometric gate); created Hilt CameraModule providing `@Named("cameraExecutor")` + `@Named("renderExecutor")` as `@Singleton Executor` with named threads (D-18). 2 deviations auto-fixed (Rule 1: Compose BOM 2026.04.00 not published → revert to 2026.03.00; Rule 3: CameraX 1.6.0 requires compileSdk>=36 → bump 35→36). `./gradlew :app:assembleDebug` green; Nyquist tests still RED on SUT references only (Mockito imports resolve).

**Stopped at:** Completed 02-02-PLAN.md — CameraX 1.6 + ML Kit 16.1.7 + Timber + Mockito deps wired; portrait-lock + Timber debug-tree + Hilt CameraModule landed

**Next expected action:** Execute 02-03-PLAN.md (detector layer: OneEuroFilter.kt + FaceDetectorClient.kt — turns OneEuroFilterTest + FaceDetectorOptionsTest GREEN).

**Files modified this session (Plan 02-02):**

- `gradle/libs.versions.toml` (added camerax + mlkitFace + timber + mockito + mockitoKotlin versions + 11 library aliases; Compose BOM reverted to 2026.03.00)
- `app/build.gradle.kts` (compileSdk 35→36; added 8 CameraX + 1 ML Kit + 1 Timber implementation() + 2 Mockito testImplementation() lines)
- `app/src/main/AndroidManifest.xml` (MainActivity screenOrientation=portrait + configChanges)
- `app/src/main/java/com/bugzz/filter/camera/BugzzApplication.kt` (Timber.plant(DebugTree) inside BuildConfig.DEBUG guard)
- `app/src/main/java/com/bugzz/filter/camera/di/CameraModule.kt` (created — Hilt module with 2 @Named Executor providers)
- `.planning/phases/02-camera-preview-face-detection-coordinate-validation/02-02-SUMMARY.md` (created)
- `.planning/STATE.md` (updated)

---
*State initialized: 2026-04-18*
