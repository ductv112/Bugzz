---
phase: 04-filter-catalog-picker-face-filter-mode
plan: "08"
status: awaiting-device-verification
tasks_complete: 2
tasks_total: 4
created: 2026-04-21
---

# Plan 04-08 Checkpoint — Tasks 1–2 Complete, Awaiting Xiaomi 13T Verification

## Completed Tasks

| Task | Name | Commit | Key outputs |
|------|------|--------|-------------|
| 1 | Clean debug build + unit-test sweep | 5d9eb4a | `app/build/outputs/apk/debug/app-debug.apk` (83 MB); 106 unit tests GREEN across 20 test classes; lintDebug 0 new errors |
| 2 | Author 04-HANDOFF.md Xiaomi 13T runbook | a23a6a3 | `.planning/phases/04-filter-catalog-picker-face-filter-mode/04-HANDOFF.md` — 442 lines, 13 steps covering all 5 ROADMAP Phase 4 success criteria + 4 behavior visuals + regression; hard/soft gates explicit |

## Current Status

**Task 3** (checkpoint:human-verify) — BLOCKED on user device verification.
**Task 4** (VALIDATION flip + STATE/ROADMAP/REQUIREMENTS updates) — blocked until Task 3 PASS.

## Task 3 Summary (checkpoint)

The user must run the 04-HANDOFF.md runbook on Xiaomi 13T and report the result.

**APK path (absolute):** `D:/ClaudeProject/appmobile/Bugzz/app/build/outputs/apk/debug/app-debug.apk`

**Runbook path (absolute):** `D:/ClaudeProject/appmobile/Bugzz/.planning/phases/04-filter-catalog-picker-face-filter-mode/04-HANDOFF.md`

**Steps summary (13 steps, 10 hard gates, 2 soft gates):**

| Step | What to check | Gate |
|------|---------------|------|
| 1 | APK file size ~83 MB + `adb devices` shows device | Soft |
| 2 | `adb install -r` + launch — Splash → Home, no crash | **Hard** |
| 3 | Home UI: Face Filter (filled) + Insect Filter (disabled) + settings gear + My Collection | **Hard** |
| 4 | Settings gear → Toast "Settings coming soon"; Insect Filter tap → no response | **Hard** |
| 5 | Face Filter nav → Camera opens + picker strip with 15 thumbnails; no Cycle button | **Hard** |
| 6 | STATIC behavior: bug on anchor, tracks head, animation plays | **Hard** |
| 7 | CRAWL behavior: continuous contour traversal, no teleport, loops | **Hard** |
| 8 | SWARM behavior: 5–8 bugs drift toward anchor, respawn at edge | **Hard** |
| 9 | FALL behavior: bugs rain from top, fall with gravity, despawn at bottom | **Hard** |
| 10 | Rapid-tap 10 filters in 5s: no black flash, no rebind, no freeze | **Hard** |
| 11 | Multi-face (2 faces): no crash + primary gets bug | Soft |
| 12 | DataStore persist: select bugC_fall → force-stop → relaunch → bugC_fall auto-restored | **Hard** |
| 13 | Phase 3 regression: shutter tap → JPEG with baked bug sprite in Google Photos | **Hard** |

**Resume signals:**
- `"Phase 4 closed — proceed"` → spawn continuation agent to execute Task 4 (VALIDATION flip)
- `"Phase 4 blocked — step N failed"` → spawn gap-closure plan for the failing hard gate before Task 4

## Build Metrics (Task 1)

| Metric | Value |
|--------|-------|
| APK size | 83 MB (86,857,826 bytes) |
| Unit tests | 106 tests, 20 test classes, 0 failures |
| Lint errors | 0 new errors |
| Build time | 52s (from cache after :app:clean) |
| Build command | `:app:clean :app:assembleDebug :app:testDebugUnitTest :app:lintDebug` |

## Deviations from Plan

None — both tasks executed exactly as specified. Build was clean on first attempt (all Phase 4 code already committed through 04-07).

## Notes

- Task 1 produced no source code changes — it was a verification build of code committed across Plans 04-01 through 04-07.
- 04-08-SUMMARY.md will be written AFTER Task 4 completes (post device PASS).
- ROADMAP.md Plan 04-08 row remains pending until Task 4.
- STATE.md stopped_at updated to reflect Tasks 1-2 complete.
