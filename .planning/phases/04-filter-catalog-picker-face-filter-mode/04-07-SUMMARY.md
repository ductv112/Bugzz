---
phase: 04-filter-catalog-picker-face-filter-mode
plan: "07"
subsystem: ui/home-nav
tags: [compose, navigation, home-screen, camera-mode, routes, mod-01]
dependency_graph:
  requires:
    - 04-06 (CameraScreen + FilterPicker — unchanged; CameraRoute now receives mode param)
    - 01-01 (type-safe @Serializable routes pattern — extended here)
  provides:
    - HomeScreen: production layout with Face Filter (filled/enabled) + Insect Filter (outlined/disabled) + Settings gear + My Collection
    - CameraMode: @Serializable enum FaceFilter/InsectFilter for type-safe nav arg
    - CameraRoute: data class with CameraMode param (default FaceFilter)
    - InsectFilterStubScreen: dark full-screen stub for CameraRoute(InsectFilter)
  affects:
    - Plan 04-08 (Xiaomi 13T runbook — HomeScreen is the entry point verified on device)
    - Phase 5 (MOD-03..07 — will replace InsectFilterStubScreen with real free-placement mode)
tech_stack:
  added:
    - "androidx.compose.material:material-icons-core (BOM-pinned) — added to libs.versions.toml + build.gradle.kts for Icons.Default.Settings"
  patterns:
    - "navigation-compose 2.8.x type-safe nav: data class CameraRoute(@Serializable enum arg) + backStackEntry.toRoute<CameraRoute>() via androidx.navigation.toRoute"
    - "HomeScreen stateless composable: onFaceFilter + onMyCollection lambdas; Settings Toast wired via LocalContext.current inside composable (no lambda needed at call site)"
    - "Disabled OutlinedButton (enabled=false): Material3 suppresses onClick automatically — no separate click handler; disabled visual is self-explanatory"
key_files:
  created:
    - app/src/main/java/com/bugzz/filter/camera/ui/home/CameraMode.kt
    - app/src/main/java/com/bugzz/filter/camera/ui/home/HomeScreen.kt
    - app/src/main/java/com/bugzz/filter/camera/ui/home/InsectFilterStubScreen.kt
  modified:
    - app/src/main/java/com/bugzz/filter/camera/ui/nav/Routes.kt
    - app/src/main/java/com/bugzz/filter/camera/ui/BugzzApp.kt
    - app/src/main/java/com/bugzz/filter/camera/ui/screens/StubScreens.kt
    - gradle/libs.versions.toml
    - app/build.gradle.kts
decisions:
  - "androidx.navigation.toRoute resolved from androidx.navigation.toRoute (not androidx.navigation.compose.toRoute) — navigation-compose 2.8.9 ships this extension in the base navigation package"
  - "Icons.Default.Settings NOT available without material-icons-core dep — added to libs.versions.toml + build.gradle.kts (Rule 2 auto-fix). material-icons-core is BOM-pinned, no version ref needed."
  - "Phase 1 StubScreens.kt CameraScreen orphan stub retained — it's already unreferenced since Phase 2 moved CameraScreen to ui.camera; cleanup deferred to Phase 6 per plan decision"
  - "Toast wording: 'Settings coming soon' (04-UI-SPEC authoritative) — not 'Settings: coming soon' (04-CONTEXT D-19 draft with colon); UI-SPEC §Copywriting Contract is definitive source"
  - "HomeScreen has 2 lambdas (onFaceFilter + onMyCollection); Settings Toast handled internally via LocalContext — cleaner than 4-lambda shape in plan Interfaces block"
metrics:
  duration: "~344 seconds"
  completed_date: "2026-04-20"
  tasks_completed: 2
  files_changed: 8
---

# Phase 4 Plan 07: HomeScreen Production + CameraMode Nav + BugzzApp Rewire — Summary

**One-liner:** Production HomeScreen with Face Filter (200x80 filled) + Insect Filter (200x80 outlined/disabled) + Settings gear Toast + My Collection bottom button; @Serializable CameraMode enum introduced as typed nav arg on CameraRoute(data class); BugzzApp NavHost branches on route.mode (FaceFilter→CameraScreen, InsectFilter→InsectFilterStubScreen); Phase 1 HomeScreen stub retired — MOD-01 delivered.

## Tasks Completed

| # | Name | Commit | Key outputs |
|---|------|--------|-------------|
| 1 | Create CameraMode enum + production HomeScreen + InsectFilterStubScreen | ede4644 | CameraMode.kt (18 lines), HomeScreen.kt (109 lines), InsectFilterStubScreen.kt (50 lines); material-icons-core dep added |
| 2 | Update Routes.kt CameraRoute to data class + rewire BugzzApp + retire stub HomeScreen | ceb7dbc | Routes.kt: data class CameraRoute(mode); BugzzApp.kt: full nav rewire; StubScreens.kt: Phase 1 HomeScreen removed |

## Implementation Details

### `androidx.navigation.toRoute` import path

`import androidx.navigation.toRoute` resolved cleanly from navigation-compose 2.8.9. The extension function is in the base `navigation` package (not `navigation.compose`). No fallback needed.

### `Icons.Default.Settings` — material-icons-core missing dep

`Icons.Default.Settings` is in `material-icons-core` (as expected per CLAUDE.md + 04-UI-SPEC §Design System), but the artifact was not declared in `libs.versions.toml` or `build.gradle.kts`. Rule 2 auto-fix: added `androidx-compose-material-icons-core` BOM-pinned library alias and `implementation(libs.androidx.compose.material.icons.core)` to app dependencies. Compile succeeded immediately after.

### Phase 1 StubScreens.kt orphan stubs

Only the `HomeScreen(onOpenCamera, onOpenCollection)` stub was removed as directed. The `CameraScreen(onOpenPreview)` stub (already orphaned since Phase 2) was **retained** per plan decision: "remove ONLY the HomeScreen stub function from StubScreens.kt, leave other stubs". Phase 6 cleanup is the designated closure for remaining orphans.

### Toast wording decision

04-CONTEXT D-19 draft says `"Settings: coming soon"` (with colon). 04-UI-SPEC §Copywriting Contract says `"Settings coming soon"` (no colon). 04-UI-SPEC is the authoritative design contract per planning hierarchy — used `"Settings coming soon"`.

### HomeScreen lambda shape

Plan Interfaces block showed a 4-lambda signature (`onFaceFilter`, `onInsectFilter`, `onMyCollection`, `onSettings`). Final implementation uses 2 lambdas (`onFaceFilter`, `onMyCollection`):
- `onInsectFilter` not needed: `enabled=false` suppresses clicks at Material3 level; no nav lambda required
- `onSettings` not needed: Toast wired internally via `LocalContext.current` inside the composable

This is cleaner and matches the plan's own "Note" in Step 2 action block.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 2 - Missing Dependency] material-icons-core not declared**
- **Found during:** Task 1 `:app:compileDebugKotlin` — `Unresolved reference 'icons'` on `Icons.Default.Settings` import
- **Issue:** `androidx.compose.material:material-icons-core` was not in `libs.versions.toml` or `build.gradle.kts` despite CLAUDE.md noting it as the icon library
- **Fix:** Added BOM-pinned library alias to `libs.versions.toml` + `implementation(libs.androidx.compose.material.icons.core)` to `app/build.gradle.kts`
- **Files modified:** `gradle/libs.versions.toml`, `app/build.gradle.kts`
- **Commit:** ede4644 (fixed before commit; included in Task 1 commit)

No other deviations — plan executed as written.

## Known Stubs

None that block the plan's goal. The `InsectFilterStubScreen` is intentional (plan-specified Phase 4 stub for CameraRoute(InsectFilter); Phase 5 MOD-03..07 will replace). The StubScreens.kt `CameraScreen` and `PreviewScreen`/`CollectionScreen` stubs are pre-existing Phase 1 artifacts not in scope for this plan.

## Threat Surface Scan

No new network endpoints, auth paths, or external file access. The `CameraRoute(mode: CameraMode)` nav arg uses kotlinx.serialization with a 2-value `@Serializable enum` — only `FaceFilter` or `InsectFilter` are valid serialized values. The `when (route.mode)` branch in BugzzApp is exhaustive (sealed enum, no `else` needed). T-04-06 threat disposition (mitigate) is implemented as designed. No new threat flags.

## Self-Check: PASSED

| Check | Result |
|-------|--------|
| `CameraMode.kt` exists | FOUND |
| `CameraMode.kt` has `enum class CameraMode` | FOUND |
| `CameraMode.kt` has `FaceFilter` + `InsectFilter` | FOUND |
| `HomeScreen.kt` exists (109 lines ≥ 80 min) | FOUND |
| `HomeScreen.kt` has `"Face Filter"` | FOUND |
| `HomeScreen.kt` has `"Insect Filter"` + `enabled = false` | FOUND |
| `HomeScreen.kt` has `"My Collection"` | FOUND |
| `HomeScreen.kt` has `"Settings coming soon"` Toast | FOUND |
| `InsectFilterStubScreen.kt` exists (50 lines ≥ 25 min) | FOUND |
| `InsectFilterStubScreen.kt` has `"Coming in a future release"` | FOUND |
| `Routes.kt` has `data class CameraRoute(val mode: CameraMode` | FOUND |
| `BugzzApp.kt` imports `ui.home.HomeScreen` | FOUND |
| `BugzzApp.kt` has `CameraRoute(mode = CameraMode.FaceFilter)` | FOUND |
| `BugzzApp.kt` has `InsectFilterStubScreen` | FOUND |
| `StubScreens.kt` Phase 1 `fun HomeScreen(onOpenCamera` removed | CONFIRMED |
| `:app:compileDebugKotlin` exits 0 | PASSED |
| `:app:testDebugUnitTest` exits 0 | PASSED |
| `:app:assembleDebug` exits 0 | PASSED |
| Commits ede4644 + ceb7dbc exist | FOUND |
