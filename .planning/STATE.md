---
gsd_state_version: 1.0
milestone: v1.0
milestone_name: milestone
status: planning
last_updated: "2026-04-18T18:07:46.390Z"
progress:
  total_phases: 7
  completed_phases: 1
  total_plans: 4
  completed_plans: 4
  percent: 100
---

# State: Bugzz

**Last updated:** 2026-04-18

## Project Reference

**Core Value:** Smooth live AR preview with bug sprites tracking face landmarks. If the live preview stutters or bugs don't stick to the face, everything else is meaningless.

**Current Focus:** Phase 1 — Foundation & Skeleton

**Milestone:** v1 — feature-parity clone of `com.insect.filters.funny.prank.bug.filter.face.camera` v1.2.7, MINUS monetization and i18n.

## Current Position

Phase: 1 (Foundation & Skeleton) — EXECUTING
Plan: 1 of 4

- **Phase:** 2
- **Plan:** Not started
- **Status:** Ready to plan
- **Progress:** `[░░░░░░░░░░░░░░░░░░░░]` 0/7 phases

### Phase Map

```
Phase 1: Foundation & Skeleton                            [ pending ]
Phase 2: Camera + Face Detection + Coord Validation       [ pending ]
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

## Accumulated Context

### Key Decisions Locked in Research

1. **UI Toolkit:** Jetpack Compose (not Views) — CameraX-Compose stable in 1.6; greenfield + solo dev + 6 screens is Compose sweet spot. (SUMMARY.md Resolution #1)
2. **Face Tracking:** ML Kit Face Detection contour mode (NOT Face Mesh) — 2D sprites don't need 478-point mesh; Face Detection has first-class `MlKitAnalyzer` + `trackingId`. (SUMMARY.md Resolution #2)
3. **Rendering:** 2D Canvas via `OverlayEffect` (NO Filament) — PBR engine is cargo-cult for sprite blits; escalation path is custom GL `CameraEffect`, never Filament. (SUMMARY.md Resolution #3)
4. **CameraX version:** 1.6.0 uniform across all artifacts — first version with stable `camera-effects` + `camera-compose`.
5. **ML Kit model:** Bundled (`com.google.mlkit:face-detection:16.1.7`) — offline first-launch, no Play Services model-download race.
6. **Persistence:** MediaStore for captures, DataStore for prefs — no Room DB for MVP.

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

**Last agent:** gsd-roadmapper
**Last action:** Created ROADMAP.md with 7-phase structure, 100% coverage of 67 v1 requirements, goal-backward success criteria per phase, UI hints applied to Phases 4/5/6.

**Next expected action:** Orchestrator presents roadmap to user for approval; on approval user runs `/gsd-plan-phase 1` to decompose Phase 1 into executable plans.

**Files modified this session:**

- `.planning/ROADMAP.md` (created)
- `.planning/STATE.md` (created)
- `.planning/REQUIREMENTS.md` (traceability updated)

---
*State initialized: 2026-04-18*
