---
gsd_state_version: 1.0
milestone: v1.0
milestone_name: milestone
status: executing
last_updated: "2026-04-19T08:37:16.190Z"
progress:
  total_phases: 7
  completed_phases: 1
  total_plans: 10
  completed_plans: 5
  percent: 50
---

# State: Bugzz

**Last updated:** 2026-04-19

## Project Reference

**Core Value:** Smooth live AR preview with bug sprites tracking face landmarks. If the live preview stutters or bugs don't stick to the face, everything else is meaningless.

**Current Focus:** Phase 02 — Camera Preview + Face Detection + Coordinate Validation

**Milestone:** v1 — feature-parity clone of `com.insect.filters.funny.prank.bug.filter.face.camera` v1.2.7, MINUS monetization and i18n.

## Current Position

Phase: 02 (Camera Preview + Face Detection + Coordinate Validation) — EXECUTING
Plan: 2 of 6

- **Phase:** 2
- **Plan:** 01 complete — Nyquist Wave 0 gate satisfied; next up 02-02 (gradle deps)
- **Status:** Executing Phase 02
- **Progress:** [█████░░░░░] 50%

### Phase Map

```
Phase 1: Foundation & Skeleton                            [ complete ]
Phase 2: Camera + Face Detection + Coord Validation       [ executing — 1/6 plans done ]
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

**Last agent:** gsd-execute-phase (Plan 02-01 executor)
**Last action:** Completed 02-01-PLAN.md — Nyquist Wave 0 gate for Phase 2 satisfied. Landed 4 unit-test files (OneEuroFilterTest, FaceDetectorOptionsTest, OverlayEffectBuilderTest, CameraControllerTest) pinning CAM-03/04/05/06/09 contracts. Intentional RED state; Plans 02-02/02-03/02-04 turn them GREEN. VALIDATION.md `nyquist_compliant: true`.

**Stopped at:** Completed 02-01-PLAN.md — Nyquist Wave 0 gate for Phase 2 satisfied

**Next expected action:** Execute 02-02-PLAN.md (version-catalog + gradle dependency wiring: CameraX 1.6.0 family, ML Kit 16.1.7, mockito-kotlin for testImplementation, Timber).

**Files modified this session (Plan 02-01):**

- `app/src/test/java/com/bugzz/filter/camera/detector/OneEuroFilterTest.kt` (created)
- `app/src/test/java/com/bugzz/filter/camera/detector/FaceDetectorOptionsTest.kt` (created)
- `app/src/test/java/com/bugzz/filter/camera/render/OverlayEffectBuilderTest.kt` (created)
- `app/src/test/java/com/bugzz/filter/camera/camera/CameraControllerTest.kt` (created)
- `.planning/phases/02-camera-preview-face-detection-coordinate-validation/02-VALIDATION.md` (nyquist_compliant flipped)
- `.planning/phases/02-camera-preview-face-detection-coordinate-validation/02-01-SUMMARY.md` (created)
- `.planning/STATE.md` (updated)

---
*State initialized: 2026-04-18*
