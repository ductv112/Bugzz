---
phase: 04-filter-catalog-picker-face-filter-mode
verified: 2026-05-04T19:30:00Z
status: human_needed
score: 5/5 must-haves verified
overrides_applied: 0
human_verification:
  - test: "Navigate Home → Face Filter → watch CRAWL filter (bugB_crawl or spider_jawline_crawl) for 10s on Xiaomi 13T"
    expected: "Bug visibly traverses face-contour perimeter (not frozen); progress wraps; no jitter"
    why_human: "CRAWL direct screenshot deferred during device test (Step 7 soft gate). Architecture proven via unit tests and same BehaviorState pattern confirmed on SWARM/FALL, but visual confirmation of perimeter traversal on live face requires physical device"
  - test: "Hold second photo/printed face in front of camera while active; verify two instances of bug render simultaneously"
    expected: "Primary face gets contour-anchor position; secondary face gets bbox-center fallback; no crash; total draws respects 20-draw soft cap"
    why_human: "Multi-face Step 11 soft gate — only one person available during automated verification run; unit tests inject synthetic 2-face data but visual on-device confirmation not yet done"
  - test: "Open app, tap Face Filter, observe picker strip; subjectively assess smoothness during filter scrolling"
    expected: "Picker scrolls without dropped frames; no stutter visible to naked eye; selecting a filter switches immediately"
    why_human: "Formal ≥24fps measurement (PRF-01) is Phase 7 scope; visual smoothness subjectively OK per device tester notes but not formally measured"
---

# Phase 4: Filter Catalog + Picker + Face Filter Mode — Verification Report

**Phase Goal:** Scale from one filter to a shipping catalog (15 bug filters across 4 behaviors) with a polished filter picker so users can browse and switch bug effects live on-camera — delivering the Face Filter mode end-to-end.
**Verified:** 2026-05-04T19:30:00Z
**Status:** human_needed
**Re-verification:** No — initial verification

---

## Goal Achievement

### Observable Truths (ROADMAP Success Criteria)

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| 1 | `FilterCatalog` bundles 15-25 filters spanning multiple insect types, each with id, display name, thumbnail, sprite atlas reference, behavior, and landmark anchor spec | VERIFIED | `grep -c "FilterDefinition(" FilterCatalog.kt` = 15; `FilterCatalogExpandedTest` (8 GREEN tests): unique IDs, non-empty displayNames, frameCount > 0, scaleFactor in range, assetDir in allowed set, all 4 behaviors represented; 4 sprite groups (sprite_spider 23f, sprite_bugA 7f, sprite_bugB 12f, sprite_bugC 16f) extracted and present in assets/ |
| 2 | All four behaviors (STATIC, CRAWL, SWARM, FALL) render correctly on-face — each visible on the test device | VERIFIED (with soft gap) | STATIC: device Step 6 PASS — bugB_nose_static screenshot, logcat draws=1. SWARM: device Step 8 PASS — p4_swarm.png 8 instances drifting, logcat draws=8. FALL: device Step 9 PASS — 5 instances in different vertical positions, logcat draws=5. CRAWL: **architecture proven** (BugBehavior.Crawl object, crawlPosition() helper, FaceContour.FACE traversal — 5 GREEN unit tests in CrawlBehaviorTest) but visual on-face screenshot not captured during device run (soft gate — see human verification) |
| 3 | Horizontal `LazyRow` filter picker renders thumbnails, highlights selected, scrolls smoothly, survives rapid-tap (10 swaps in 5s) without stutter or CameraX rebind | VERIFIED | Device Step 10 PASS: rapid-tap verified, white border + 1.15x scale animation visible, filter changes within 1 frame, no CameraInUseException in logcat, no black flash; FilterPicker.kt 176 lines — LazyRow + animateFloatAsState + FastOutSlowInEasing + file:///android_asset/ Coil thumbnails; `rapidSelectFilter_noCameraRebind` GREEN unit test |
| 4 | Home screen "Face Filter" button launches camera in tracked mode with last-used filter restored from DataStore; "Insect Filter" button is present (non-functional until Phase 5) | VERIFIED | Device Step 3 PASS (screenshot p4_home.png): Settings gear top-right, Face Filter filled primary, Insect Filter outlined disabled, My Collection bottom — per D-19 spec. Step 4 PASS: Insect Filter tap → no nav; Settings tap → Toast. Step 12 PASS (CAT-05): bugB_swarm active → force-stop → relaunch → logcat immediately shows `filter=bugB_swarm` — DataStore persistence confirmed. FilterPrefsRepository.kt 68 lines with .catch IOException fallback; 4 FilterPrefsRepositoryTest GREEN |
| 5 | Multi-face scene (2 faces) does not crash; primary face receives full filter; contour-dependent behaviors fall back gracefully to boundingBox center on secondary faces | VERIFIED (with soft gap) | FilterEngine.kt: ConcurrentHashMap<Int, BehaviorState> perFaceState; onDraw(List<SmoothedFace>) multi-face signature; primary/secondary dispatch; MAX_DRAWS_PER_FRAME=20. FilterEngineTest: `multiFace_primaryGetsContourAnchor_secondaryGetsBboxCenter` GREEN, `softCap_cappedAtMaxDrawsPerFrame_acrossFaces` GREEN, `onFaceLost_removesStateEntry` GREEN. Real-device 2-person test deferred (soft gate — see human verification) |

**Score: 5/5 truths verified** (2 carry soft gaps routed to human verification)

---

### Required Artifacts

| Artifact | Description | Exists | Substantive | Wired | Status |
|----------|-------------|--------|-------------|-------|--------|
| `app/src/main/assets/sprites/sprite_spider/` | 23-frame spider sprite group | Yes | 23 PNGs + manifest.json (frameCount=23) | Yes — assetDir="sprites/sprite_spider" in FilterCatalog (4 entries) | VERIFIED |
| `app/src/main/assets/sprites/sprite_bugA/` | 7-frame bugA sprite group | Yes | 7 PNGs + manifest.json (frameCount=7) | Yes — assetDir="sprites/sprite_bugA" in FilterCatalog | VERIFIED |
| `app/src/main/assets/sprites/sprite_bugB/` | 12-frame bugB sprite group | Yes | 12 PNGs + manifest.json (frameCount=12) | Yes — assetDir="sprites/sprite_bugB" in FilterCatalog | VERIFIED |
| `app/src/main/assets/sprites/sprite_bugC/` | 16-frame bugC sprite group | Yes | 16 PNGs + manifest.json (frameCount=16) | Yes — assetDir="sprites/sprite_bugC" in FilterCatalog | VERIFIED |
| `app/src/main/java/.../filter/FilterCatalog.kt` | 15-entry filter catalog (CAT-01/CAT-02) | Yes | 15 FilterDefinition entries, all 4 behaviors, 4 assetDirs | Yes — consumed by CameraViewModel.bind(), FilterCatalogExpandedTest | VERIFIED |
| `app/src/main/java/.../render/BehaviorState.kt` | Sealed BehaviorState interface (D-12) | Yes | Static/Crawl/Swarm/Fall variants + BugInstance/FallingBug/CrawlDirection | Yes — used throughout FilterEngine + BugBehavior | VERIFIED |
| `app/src/main/java/.../render/BugBehavior.kt` | CRAWL/SWARM/FALL real impls (D-08/D-09/D-10) | Yes | 260 lines: Crawl.tick (FaceContour traversal), Swarm.tick (drift+respawn), Fall.tick (gravity+spawn+despawn), crawlPosition() | Yes — invoked from FilterEngine.drawFace | VERIFIED |
| `app/src/main/java/.../render/FilterEngine.kt` | Multi-face engine with perFaceState + soft cap (D-13/D-14/D-22) | Yes | 260 lines: ConcurrentHashMap perFaceState, onDraw(List<SmoothedFace>), MAX_DRAWS_PER_FRAME=20, onFaceLost() | Yes — called from CameraOverlayRenderer; 11 FilterEngineTest GREEN | VERIFIED |
| `app/src/main/java/.../filter/BehaviorConfig.kt` | BehaviorConfig sealed interface (D-29) | Yes | CrawlConfig/SwarmConfig/FallConfig variants | Yes — referenced in FilterDefinition.behaviorConfig, createBehaviorState in FilterEngine | VERIFIED |
| `app/src/main/java/.../data/FilterPrefsRepository.kt` | DataStore wrapper (CAT-05/D-25) | Yes | 68 lines: lastUsedFilterId Flow with .catch IOException, setLastUsedFilter suspend fun, DEFAULT_FILTER_ID | Yes — injected into CameraViewModel 5-arg ctor; 4 FilterPrefsRepositoryTest GREEN | VERIFIED |
| `app/src/main/java/.../di/DataModule.kt` | Hilt DataModule anchor (D-26) | Yes | @InstallIn(SingletonComponent) object | Yes — in di/ package alongside CameraModule | VERIFIED |
| `app/src/main/java/.../ui/camera/FilterSummary.kt` | Picker DTO (D-17) | Yes | id + displayName + assetDir fields | Yes — in CameraUiState.filters list | VERIFIED |
| `app/src/main/java/.../ui/camera/CameraUiState.kt` | Extended with picker fields | Yes | filters: List<FilterSummary>, selectedFilterId: String | Yes — rendered by FilterPicker; written by onSelectFilter | VERIFIED |
| `app/src/main/java/.../ui/camera/FilterPicker.kt` | LazyRow picker composable (CAT-03/D-15/D-16) | Yes | 176 lines: LazyRow, Coil AsyncImage file:///android_asset/, animateFloatAsState 1.15f scale, 2dp white border, VIRTUAL_KEY haptic, animateScrollToItem | Yes — wired in CameraScreen at 104dp bottom offset | VERIFIED |
| `app/src/main/java/.../ui/camera/CameraViewModel.kt` | ViewModel with onSelectFilter + bind DataStore (CAT-04/CAT-05) | Yes | onSelectFilter() optimistic UI + DataStore write; bind() reads lastUsedFilterId; isCapturing guard + bindJob.cancel preserved | Yes — FilterPicker calls vm.onSelectFilter; FilterPrefsRepository injected | VERIFIED |
| `app/src/main/java/.../ui/home/HomeScreen.kt` | Production HomeScreen (MOD-01/D-19) | Yes | 109 lines: Settings gear IconButton, Face Filter FilledButton, Insect Filter OutlinedButton enabled=false, My Collection button | Yes — wired in BugzzApp NavHost as HomeRoute destination | VERIFIED |
| `app/src/main/java/.../ui/home/CameraMode.kt` | @Serializable CameraMode enum (D-20) | Yes | FaceFilter + InsectFilter variants | Yes — used in CameraRoute data class + BugzzApp when branch | VERIFIED |
| `app/src/main/java/.../ui/home/InsectFilterStubScreen.kt` | Phase 5 placeholder for InsectFilter route (D-20) | Yes | 50 lines: "Coming in a future release" message, back button | Yes — wired in BugzzApp CameraMode.InsectFilter branch | VERIFIED (intentional stub) |

---

### Key Link Verification

| From | To | Via | Status | Details |
|------|----|-----|--------|---------|
| FilterCatalog.all (15 entries) | sprites/sprite_{spider,bugA,bugB,bugC}/ in assets | FilterDefinition.assetDir | WIRED | assetDir values confirmed: "sprites/sprite_spider", "sprites/sprite_bugA", "sprites/sprite_bugB", "sprites/sprite_bugC" — all 4 directories exist in assets/ |
| AssetLoader.get/preload | sprites/sprite_* files | assetDir param (gap fix 514410c) | WIRED | All 4 call sites updated to use `filter.assetDir` / `def.assetDir` not filterId; device verified post-fix |
| FilterPicker composable | CameraViewModel.onSelectFilter | onSelect lambda in CameraScreen | WIRED | CameraScreen line 163: `onSelect = { id -> vm.onSelectFilter(id) }` |
| CameraViewModel.onSelectFilter | FilterPrefsRepository.setLastUsedFilter | viewModelScope.launch (background coroutine) | WIRED | `filterPrefsRepository.setLastUsedFilter(id)` in two-coroutine pattern; CAT-04 unit test GREEN |
| CameraViewModel.bind() | FilterPrefsRepository.lastUsedFilterId Flow | `filterPrefsRepository.lastUsedFilterId.first()` | WIRED | Line 119 in CameraViewModel.kt; `initialBind_readsLastUsedFromDataStore` GREEN |
| HomeScreen "Face Filter" button | CameraRoute(mode=CameraMode.FaceFilter) | BugzzApp navController.navigate lambda | WIRED | BugzzApp: `onFaceFilter = { navController.navigate(CameraRoute(mode = CameraMode.FaceFilter)) }` |
| BugzzApp NavHost | HomeScreen + CameraScreen + InsectFilterStubScreen | composable<HomeRoute> + when(route.mode) dispatch | WIRED | Imports confirmed: ui.home.HomeScreen, ui.home.InsectFilterStubScreen; CameraMode.FaceFilter → CameraScreen, CameraMode.InsectFilter → InsectFilterStubScreen |
| FilterEngine.onDraw | BugBehavior.Crawl/Swarm/Fall tick functions | BehaviorState sealed dispatch (when) | WIRED | FilterEngine.drawFace dispatches on BehaviorState variant; BugBehavior objects called accordingly |

---

### Data-Flow Trace (Level 4)

| Artifact | Data Variable | Source | Produces Real Data | Status |
|----------|---------------|--------|-------------------|--------|
| FilterPicker (LazyRow thumbnails) | uiState.filters (List<FilterSummary>) | CameraViewModel.bind() reads FilterCatalog.all → maps to FilterSummary list | Yes — FilterCatalog.all is 15 hardcoded FilterDefinitions; no empty return path | FLOWING |
| FilterPicker (selection highlight) | uiState.selectedFilterId | FilterPrefsRepository.lastUsedFilterId.first() on bind; optimistic update on tap | Yes — DataStore round-trip verified by unit tests + device Step 12 | FLOWING |
| FilterPicker (Coil thumbnails) | file:///android_asset/${filter.assetDir}/frame_00.png | Assets filesystem (extracted sprite PNGs) | Yes — 58 frames on disk; device Step 5 confirmed thumbnails visible | FLOWING |
| FilterEngine.drawFace (bug sprites) | BehaviorState from perFaceState ConcurrentHashMap | assetLoader.get(filter.assetDir, frameIdx) → AssetLoader LruCache | Yes — gap fix 514410c; device Steps 6/8/9 all show rendered bugs | FLOWING |
| HomeScreen (mode buttons) | Static layout — no dynamic data | Composable constants | N/A — static UI; device Step 3 PASS screenshot confirms layout | N/A |

---

### Behavioral Spot-Checks

| Behavior | Check | Result | Status |
|----------|-------|--------|--------|
| FilterCatalog has 15 entries | `grep -c "FilterDefinition(" FilterCatalog.kt` = 15 | 15 | PASS |
| BehaviorState.kt exists and BugState.kt deleted | file existence check | BugState.kt DELETED, BehaviorState.kt EXISTS | PASS |
| FilterEngine MAX_DRAWS_PER_FRAME=20 | grep in FilterEngine.kt | Found at companion const | PASS |
| FilterEngine has ConcurrentHashMap | grep -c "ConcurrentHashMap" | 3 occurrences | PASS |
| DataStore .catch IOException fallback | grep in FilterPrefsRepository.kt | `if (e is IOException) emit(emptyPreferences())` confirmed | PASS |
| AssetLoader uses assetDir (gap fix) | grep in FilterEngine + CameraViewModel | All 4 call sites use `filter.assetDir` / `def.assetDir` | PASS |
| Phase 3 isCapturing guard preserved | grep in CameraViewModel.kt | `if (_uiState.value.isCapturing) return` — 1 match | PASS |
| Phase 3 bindJob.cancel preserved | grep in CameraViewModel.kt | `bindJob?.cancel()` — 1 match | PASS |
| Phase 3 captureFlash after success preserved | grep in CameraViewModel.kt | `captureFlashVisible = true` inside onSuccess block | PASS |
| Phase 3 require(frameCount > 0) preserved | grep in FilterDefinition.kt | 1 match | PASS |
| 106 unit tests GREEN (reported) | 04-08-SUMMARY build metrics | 106 tests, 20 test classes, 0 failures | PASS |
| CRAWL visual on-face | Device Step 7 (soft gate) | Deferred — logcat shows CRAWL filters active; direct screenshot not taken | HUMAN |
| Multi-face 2-person | Device Step 11 (soft gate) | Deferred — single person in frame; architecture proven via unit tests | HUMAN |

---

### Requirements Coverage

| Requirement | Description | Source Plan | Status | Evidence |
|-------------|-------------|-------------|--------|----------|
| CAT-01 | FilterCatalog bundles 15-25 bug filters spanning multiple types | 04-04 | SATISFIED | FilterCatalog.kt: 15 FilterDefinitions covering spider/bugA/bugB/bugC sprite groups; FilterCatalogExpandedTest `catalog_has_15_entries` GREEN; device Step 5 PASS |
| CAT-02 | Each filter has: id, displayName, thumbnail, sprite atlas ref, behavior config, landmark anchor | 04-04 | SATISFIED | FilterCatalogExpandedTest: `allEntries_haveUniqueIds`, `allEntries_haveNonEmptyDisplayName`, `allEntries_haveFrameCountGreaterThan0`, `allEntries_assetDirInAllowedSet` — all GREEN; FilterDefinition fields confirmed in code |
| CAT-03 | Filter picker (LazyRow) shows thumbnails, highlights selected, scrolls smoothly | 04-06 | SATISFIED | FilterPicker.kt 176 lines wired in CameraScreen; device Step 10 PASS — picker visible, rapid-tap stable |
| CAT-04 | Tapping a filter thumbnail switches active filter immediately | 04-05/04-06 | SATISFIED | onSelectFilter optimistic-UI sets selectedFilterId before preload; device Step 10 PASS — filter changes within 1 frame; `onSelectFilter_callsEngineAndWritesDataStore` GREEN |
| CAT-05 | Last-used filter persisted in DataStore, restored on app relaunch | 04-05 | SATISFIED | FilterPrefsRepository.kt DataStore wrapper; 4 FilterPrefsRepositoryTest GREEN; device Step 12 PASS — bugB_swarm restored after force-stop (PID 9059 → 15588) |
| MOD-01 | Home screen has two primary buttons: "Face Filter" (enabled) and "Insect Filter" (disabled) | 04-07 | SATISFIED | HomeScreen.kt: Face Filter FilledButton + Insect Filter OutlinedButton (enabled=false) + Settings gear + My Collection; device Step 3 PASS screenshot; Step 4 PASS disabled-state |
| MOD-02 | Face Filter mode anchors bugs to face landmarks with real-time tracking | 04-04 | SATISFIED (with soft gap) | FilterEngine.kt multi-face ConcurrentHashMap; primary face gets FaceLandmarkMapper anchor (contour-based), secondary gets bbox-center; `multiFace_primaryGetsContourAnchor_secondaryGetsBboxCenter` GREEN; device single-face verified Steps 6/8/9; 2-person visual deferred to human check |

---

### Anti-Patterns Found

| File | Pattern | Severity | Impact |
|------|---------|----------|--------|
| `ui/home/InsectFilterStubScreen.kt` | Intentional stub — "Coming in a future release" message | Info | Expected Phase 4 behavior; Phase 5 MOD-03..07 replaces |
| `ui/screens/StubScreens.kt` | Phase 1 CameraScreen/PreviewScreen/CollectionScreen stubs still present (orphaned) | Info | Pre-existing Phase 1 artifacts, unreferenced since Phase 2; scheduled for Phase 6 cleanup; no impact on Phase 4 goal |
| `di/DataModule.kt` | Empty object — no @Provides methods | Info | Intentional — FilterPrefsRepository uses @Inject constructor, no manual binding needed; documented in 04-05-SUMMARY |
| `ui/home/HomeScreen.kt` | Settings gear Toast "Settings coming soon" | Info | Intentional Phase 4 placeholder per D-19; Phase 6 UX-09 wires real settings |
| `ui/camera/CameraScreen.kt` | My Collection nav → stub behavior | Info | Phase 1 stub behavior preserved; Phase 6 UX wires real collection |

No blockers found. All pattern matches are intentional deferred stubs documented in CONTEXT/SUMMARY files.

---

### Human Verification Required

#### 1. CRAWL Behavior Visual Confirmation (Soft Gate — Step 7)

**Test:** On Xiaomi 13T, open app, navigate Face Filter, select a CRAWL filter (e.g. `Spider Crawl` or `BugC Crawl`) from the picker, and watch the bug for 10 seconds.
**Expected:** The bug visibly traverses the face-contour perimeter in a looping path (clockwise or counter-clockwise), not sitting static at one point. The speed should be roughly 50% of face width per second (per D-08 formula).
**Why human:** Step 7 in the 04-HANDOFF runbook was designated a soft gate during the 2026-05-04 automated verification run — the tester had the CRAWL filter active briefly during picker scrolling (logcat confirmed CRAWL filters selected) but no dedicated screenshot was captured showing bug position traversal. The unit tests (CrawlBehaviorTest 5 GREEN tests) prove the `progress` math and `crawlPosition()` linear interpolation are correct, but visual confirmation on a live face has not been photographically documented.

#### 2. Multi-Face 2-Person Real-Device Test (Soft Gate — Step 11)

**Test:** On Xiaomi 13T, open app, navigate Face Filter, activate any SWARM filter, hold a second face (printed photo, second phone screen, or second person) in frame next to your own.
**Expected:** Two sets of bug sprites appear simultaneously — one on the primary face (full contour-based anchor position) and one on the secondary face (bbox-center fallback at ~40% height). No crash. Total visible bugs across both faces should stay within the 20-draw soft cap (D-14). When one face leaves the frame, only that face's bugs disappear.
**Why human:** No second person was available during the 2026-05-04 automated verification window. The architecture is proven via `FilterEngineTest.multiFace_primaryGetsContourAnchor_secondaryGetsBboxCenter` (synthetic 2-face injection GREEN), `softCap_cappedAtMaxDrawsPerFrame_acrossFaces` (GREEN), and `onFaceLost_removesStateEntry` (GREEN). Physical 2-person visual confirmation is outstanding.

#### 3. FPS Smoothness Subjective Assessment (Soft Gate — Step 5)

**Test:** Open Face Filter with any SWARM filter active; use the app normally for ~30 seconds with smooth picker scrolling and filter switching.
**Expected:** No visible dropped frames, no jank, picker scrolling feels fluid. Bug animation appears continuous (no jump cuts between animation frames).
**Why human:** Formal ≥24fps measurement with Android Studio profiler is Phase 7 PRF-01 scope. Device tester noted "no obvious jank observed" during the 2026-05-04 run but this was subjective. A brief subjective quality pass suffices here; formal measurement is deferred.

---

### Deferred Items

Items not yet met but explicitly addressed in later milestone phases:

| Item | Addressed In | Evidence |
|------|-------------|----------|
| `InsectFilter` free-placement sticker mode — MOD-03..07 | Phase 5 | Phase 5 ROADMAP SC 5: "Insect Filter mode places a single draggable sticker..." |
| Formal ≥24fps profiler measurement — PRF-01 | Phase 7 | Phase 7 success criteria include formal FPS measurement |
| Settings screen functional content — UX-09 | Phase 6 | Phase 6 requirements include UX-09 |
| My Collection screen real content | Phase 6 | Phase 6 UX-05 — MediaStore query + grid render |
| Phase 1 StubScreens.kt orphan cleanup | Phase 6 | Phase 6 UX polish pass; pre-existing artifact not blocking any Phase 4 functionality |

---

### Phase 3 Regression Check

All five 03-REVIEW-FIX contracts verified present in current codebase:

| Contract | Location | Grep Result |
|----------|----------|-------------|
| `if (_uiState.value.isCapturing) return` (WR-02) | CameraViewModel.kt line 187 | 1 match — PRESERVED |
| `bindJob?.cancel()` (WR-03) | CameraViewModel.kt line 101 | 1 match — PRESERVED |
| `captureFlashVisible = true` inside onSuccess (WR-04) | CameraViewModel.kt line 194 | 1 match — PRESERVED |
| `OneShotEvent.FilterLoadError` | CameraViewModel.kt lines 125/138 + CameraScreen | 5+ matches — PRESERVED |
| `require(frameCount > 0)` | FilterDefinition.kt line 35 | 1 match — PRESERVED |

Phase 3 capture path regression: device Step 13 PASS — `Bugzz_20260504_192609.jpg` (757KB) with 8 spider bugs baked into JPEG, confirming OverlayEffect ImageCapture compositing still works end-to-end under Phase 4 multi-instance render.

---

### Gaps Summary

No gaps found. All 5 ROADMAP success criteria are verified. All 7 requirement IDs (CAT-01..05, MOD-01, MOD-02) are satisfied. Phase 3 regressions are clean. The inline gap fix (04-gaps-01, commit 514410c) that resolved the AssetLoader assetDir API mismatch is integrated and device-verified.

Status is `human_needed` (not `passed`) because 3 behavioral confirmations require physical device interaction: CRAWL visual traversal, 2-person multi-face, and subjective FPS smoothness. These are architectural soft gates from the 04-HANDOFF runbook, not blockers — the code paths are proven via unit tests and device-tested behavioral analogs (SWARM/FALL multi-instance confirm the same BehaviorState dispatch pattern CRAWL uses). Human sign-off on these 3 items completes Phase 4.

---

_Verified: 2026-05-04T19:30:00Z_
_Verifier: Claude (gsd-verifier)_
