---
phase: 05
plan: 06
subsystem: nav-rewire
tags: [nav-graph, home-screen, insect-filter-route, stub-delete, button-enable]
dependency_graph:
  requires: [05-05-SUMMARY]
  provides: [InsectFilterScreen reachable from HomeScreen via nav graph]
  affects: [BugzzApp nav graph, HomeScreen button state]
tech_stack:
  added: []
  patterns:
    - CameraRoute(mode) when-branch wired to production InsectFilterScreen (hiltViewModel default param)
    - HomeScreen onInsectFilter lambda param added; button enabled = true
key_files:
  created: []
  modified:
    - app/src/main/java/com/bugzz/filter/camera/ui/BugzzApp.kt
    - app/src/main/java/com/bugzz/filter/camera/ui/home/HomeScreen.kt
  deleted:
    - app/src/main/java/com/bugzz/filter/camera/ui/home/InsectFilterStubScreen.kt
decisions:
  - "InsectFilterScreen() invoked with no arguments — uses hiltViewModel() default; nav graph does not need to pass navController since InsectFilterScreen manages its own back navigation via BackHandler (D-24)"
  - "HomeScreen signature extended with onInsectFilter: () -> Unit parameter; BugzzApp passes CameraRoute(InsectFilter) lambda — consistent with existing onFaceFilter pattern"
  - "Settings gear Toast 'Settings coming soon' retained — that is Phase 4 D-19 intentional placeholder for Settings screen (Phase 6); not a stub for Insect Filter"
metrics:
  duration_seconds: 180
  completed_date: "2026-05-04"
  tasks_completed: 1
  tasks_total: 1
  files_created: 0
  files_modified: 2
  files_deleted: 1
---

# Phase 05 Plan 06: Nav Rewire + HomeScreen Insect Filter Enable Summary

**One-liner:** Deleted Phase 4 InsectFilterStubScreen; wired BugzzApp CameraRoute(InsectFilter) to production InsectFilterScreen; changed HomeScreen Insect Filter button from `enabled=false` no-op to `enabled=true` with `onInsectFilter` nav callback.

---

## What Was Built

### Task 1 — Rewire BugzzApp nav graph + enable HomeScreen button + delete stub

| Change | Detail |
|--------|--------|
| `BugzzApp.kt` — import | Replaced `import com.bugzz.filter.camera.ui.home.InsectFilterStubScreen` with `import com.bugzz.filter.camera.ui.insect.InsectFilterScreen` |
| `BugzzApp.kt` — route body | `CameraMode.InsectFilter -> InsectFilterScreen()` (hiltViewModel() default; no navController arg needed — screen uses BackHandler D-24) |
| `BugzzApp.kt` — HomeScreen call | Added `onInsectFilter = { navController.navigate(CameraRoute(mode = CameraMode.InsectFilter)) }` |
| `HomeScreen.kt` — signature | Added `onInsectFilter: () -> Unit` parameter between `onFaceFilter` and `onMyCollection` |
| `HomeScreen.kt` — button | `enabled = false` → `enabled = true`; `onClick = { /* no-op */ }` → `onClick = onInsectFilter` |
| `InsectFilterStubScreen.kt` | DELETED — Phase 4 placeholder superseded by Plan 05-05 production screen |

---

## Acceptance Criteria Verification

| Check | Result |
|-------|--------|
| `InsectFilterStubScreen.kt` deleted | PASS — file does not exist |
| `grep -rn "InsectFilterStubScreen" app/src/...` | PASS — 0 matches |
| `grep -c "InsectFilterScreen" BugzzApp.kt` | 2 (import + invocation) ✓ |
| `grep -c "CameraMode.InsectFilter" BugzzApp.kt` | 2 ✓ |
| `grep -c "CameraMode.FaceFilter" BugzzApp.kt` | 2 (preserved) ✓ |
| `grep -c "CameraScreen" BugzzApp.kt` | 2 (FaceFilter route unchanged) ✓ |
| `grep -c "enabled = false" HomeScreen.kt` | 0 ✓ |
| `grep -c "onInsectFilter" HomeScreen.kt` | 2 (param decl + onClick) ✓ |
| Insect Filter "Coming soon" stub text | Removed — only "Settings coming soon" Toast remains (intentional D-19 Settings placeholder) ✓ |
| Phase 4 layout preserved (4 strings) | 17 matches for Face Filter / Insect Filter / My Collection / Settings ✓ |
| `./gradlew :app:testDebugUnitTest :app:assembleDebug` | BUILD SUCCESSFUL in 25s ✓ |

---

## Deviations from Plan

None — plan executed exactly as written.

The `InsectFilterScreen()` call uses no explicit arguments because the composable signature has `viewModel: InsectFilterViewModel = hiltViewModel()` as a default — no navController param (InsectFilterScreen handles back navigation internally via BackHandler). This matches the plan's intent ("InsectFilterScreen(navController = navController)" was an illustrative pattern; the actual signature does not accept navController).

---

## Known Stubs

None. The only remaining stub in HomeScreen is the Settings gear Toast ("Settings coming soon") — this is Phase 4 D-19 intentional, to be replaced by Phase 6 Settings screen. It does not block this plan's goal.

---

## Threat Surface Scan

No new network endpoints, auth paths, file access, or schema changes. Nav rewire is mechanical type-safe routing — compile-time validated by Kotlin `when` exhaustive check on `CameraMode` enum.

---

## Self-Check: PASSED

| Check | Result |
|-------|--------|
| `InsectFilterStubScreen.kt` deleted | CONFIRMED — `[ -f ... ]` returns false |
| `BugzzApp.kt` modified | FOUND |
| `HomeScreen.kt` modified | FOUND |
| commit `87340ab` | FOUND — `feat(05-06-01): nav rewire + Insect Filter button enable + stub delete` |
| BUILD SUCCESSFUL | CONFIRMED — 25s, 52 tasks |
