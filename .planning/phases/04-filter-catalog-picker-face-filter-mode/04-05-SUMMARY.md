---
phase: 04-filter-catalog-picker-face-filter-mode
plan: "05"
subsystem: data/viewmodel-wiring
tags: [datastore, filter-prefs, hilt, viewmodel, constructor-split, cat-04, cat-05, t-04-01, t-04-05]
dependency_graph:
  requires:
    - 04-04 (FilterCatalog 15 entries + FilterCatalog.byId — used in onSelectFilter + bind fallback)
    - 04-02 (Wave 0 FilterPrefsRepositoryTest + CameraViewModelTest @Ignore'd scaffolds to un-Ignore)
  provides:
    - FilterPrefsRepository: @Singleton DataStore wrapper with T-04-01 .catch corruption fallback
    - DataModule: Hilt anchor @InstallIn(SingletonComponent) for data-layer
    - FilterSummary: immutable DTO for picker rendering (id, displayName, assetDir)
    - CameraUiState.filters: List<FilterSummary> + selectedFilterId: String
    - CameraViewModel.onSelectFilter(id): CAT-04 picker tap handler with DataStore write
    - CameraViewModel.bind(): DataStore read on first invocation + catalog fallback (CAT-05)
  affects:
    - Plan 04-06 (CameraScreen LazyRow picker reads uiState.filters + calls onSelectFilter)
    - Plan 05+ (FilterEngine.onFaceLost wire-up; no DataStore changes expected)
tech_stack:
  added: []
  patterns:
    - "Constructor-split @Inject pattern (Phase 2 STATE #14): internal constructor(DataStore) for tests + @Inject constructor(@ApplicationContext Context) for Hilt production"
    - "Top-level Context.preferencesDataStore delegate (outside class body — Pitfall 5 from 04-RESEARCH)"
    - "Flow .catch { if IOException emit emptyPreferences() } for T-04-01 corruption mitigation"
    - "Optimistic UI: selectedFilterId+activeFilterId set immediately on onSelectFilter tap before preload completes"
    - "Two-coroutine pattern in onSelectFilter: (a) DataStore write on default dispatcher, (b) preload+setFilter on cameraExecutor"
    - "DataStore test isolation: PreferenceDataStoreFactory.create(produceFile = { File(tempFolder.root, name) }) per test — no singleton pollution"
key_files:
  created:
    - app/src/main/java/com/bugzz/filter/camera/data/FilterPrefsRepository.kt
    - app/src/main/java/com/bugzz/filter/camera/di/DataModule.kt
    - app/src/main/java/com/bugzz/filter/camera/ui/camera/FilterSummary.kt
  modified:
    - app/src/main/java/com/bugzz/filter/camera/ui/camera/CameraUiState.kt
    - app/src/main/java/com/bugzz/filter/camera/ui/camera/CameraViewModel.kt
    - app/src/test/java/com/bugzz/filter/camera/data/FilterPrefsRepositoryTest.kt
    - app/src/test/java/com/bugzz/filter/camera/ui/camera/CameraViewModelTest.kt
decisions:
  - "Constructor-split pattern matched Phase 2 STATE #14 exactly — no deviations; internal constructor(dataStore) provides the test seam, @Inject constructor(@ApplicationContext ctx) delegates for production"
  - "DataModule is an empty object placeholder — FilterPrefsRepository uses @Inject + @ApplicationContext (auto-bound by Hilt SingletonComponent) so no @Provides needed at this time"
  - "corruptedDataStore_emitsDefault uses mock DataStore approach (strategy b per plan) — simpler and more deterministic than writing corrupt proto bytes to a temp file"
  - "CameraViewModelTest updated: mockPrefsRepo added as 5th arg to buildVm(); default stub (flowOf(DEFAULT_FILTER_ID)) in @Before so all Phase 3 bind() tests pass without null Flow"
  - "onCycleFilter retained as @Deprecated (not deleted) — Phase 3 test onCycleFilter_togglesFilterEngineActive still exercises it; Plan 04-06 will remove the UI button"
metrics:
  duration: "~6 minutes"
  completed_date: "2026-04-20"
  tasks_completed: 3
  files_changed: 7
---

# Phase 4 Plan 05: DataStore Repo + ViewModel Wiring — Summary

**One-liner:** FilterPrefsRepository @Singleton DataStore wrapper with T-04-01 IOException fallback + DataModule Hilt anchor + CameraViewModel 5-arg constructor wired to read/write last-used filter via DataStore, with optimistic-UI onSelectFilter and FilterSummary/CameraUiState picker fields — CAT-04 + CAT-05 closed.

## Tasks Completed

| # | Name | Commit | Key outputs |
|---|------|--------|-------------|
| 1 | FilterPrefsRepository + DataModule + FilterPrefsRepositoryTest un-Ignored | b7bb433 | FilterPrefsRepository.kt NEW (65 lines); DataModule.kt NEW (10 lines); FilterPrefsRepositoryTest 4 tests GREEN |
| 2 | FilterSummary DTO + CameraUiState extended | 596a406 | FilterSummary.kt NEW (18 lines); CameraUiState +2 fields (filters, selectedFilterId); Phase 3 fields preserved |
| 3 | CameraViewModel 5-arg ctor + onSelectFilter + bind DataStore read; CameraViewModelTest +3 | c7ff05f | CameraViewModel full rewrite with Phase 4 additions; CameraViewModelTest 3 un-Ignored tests GREEN |

## Wave 0 Tests Un-Ignored

| File | Tests un-Ignored | Status |
|------|-----------------|--------|
| FilterPrefsRepositoryTest.kt | 4 (writeThenRead, read_beforeWrite, writeAgain_overwrites, corruptedDataStore_emitsDefault) | GREEN |
| CameraViewModelTest.kt | 3 (onSelectFilter_callsEngineAndWritesDataStore, initialBind_readsLastUsedFromDataStore, rapidSelectFilter_noCameraRebind) | GREEN |
| **Total new active** | **7** | **all GREEN** |

Note: `unknownIdFromDataStore_viewModelFallsBackToDefault` was a REMINDER stub in the Wave 0 scaffold — it was intentionally a no-op test body. It remains @Ignore'd per the plan comment ("may be deleted"); the invariant it describes is covered by CameraViewModel.bind() resolving via FilterCatalog.byId with firstOrNull fallback.

## Constructor-Split Pattern Match

The `internal constructor(dataStore: DataStore<Preferences>)` + `@Inject constructor(@ApplicationContext context: Context) : this(context.bugzzPrefsDataStore)` pattern matched Phase 2 STATE #14 exactly. No deviations required. The top-level `private val Context.bugzzPrefsDataStore` extension delegate was placed outside the class body as required by 04-RESEARCH Pitfall 5 — Hilt does not need to know about this extension; only the secondary `@Inject` ctor uses it.

## Phase 3 CameraViewModelTest Updates

4 existing tests needed the new 5th ctor arg (`mockPrefsRepo`):

- `buildVm()` helper: updated from 4-arg to 5-arg `CameraViewModel(...)` call
- `@Before setUp()`: added default `mockPrefsRepo.stub { on { lastUsedFilterId } doReturn flowOf(DEFAULT_FILTER_ID) }` so Phase 3 bind() tests that call `bind()` don't hang on a null Flow

The 3 Phase 3 test methods themselves were not modified (test logic identical). The @Deprecated annotation on `onCycleFilter()` generates expected warnings in the test compile output — these are intentional, not errors.

## Phase 3 Fix Preservation

All 03-REVIEW-FIX contracts verified by grep-assert after Task 3 commit:

| Contract | Grep result | Status |
|----------|-------------|--------|
| `if (_uiState.value.isCapturing) return` (WR-02) | 1 match | PRESERVED |
| `bindJob?.cancel()` (WR-03) | 1 match | PRESERVED |
| `captureFlashVisible = true` inside onSuccess (WR-04) | 1 match | PRESERVED |
| `OneShotEvent.FilterLoadError` | 5 matches | PRESERVED |
| `require(frameCount > 0)` in FilterDefinition.kt | 1 match | PRESERVED |

## Hilt Compile Result

Clean — no binding-missing errors. Hilt auto-binds `FilterPrefsRepository` via its `@Inject constructor(@ApplicationContext context)`. The empty `DataModule` object compiles without warnings. `assembleDebug` exits 0.

## Deviations from Plan

### Auto-fixed Issues

None — plan executed exactly as written. The constructor-split, DataModule pattern, test mock setup, and onSelectFilter two-coroutine design all matched the Interfaces block without modification.

## Known Stubs

None — all wiring is fully functional. `DataModule` is intentionally empty (not a stub — FilterPrefsRepository requires no explicit @Provides). `uiState.filters` is populated on first `bind()` call; it starts as `emptyList()` which is correct default behavior (not a rendering stub — Plan 04-06 renders this list).

## Threat Surface Scan

No new network endpoints or auth paths. One new file-system access pattern:

- `bugzz_prefs.preferences_pb` written/read via DataStore in app-private storage (not shared storage) — no new permissions required.
- T-04-01 (DataStore corruption) and T-04-05 (rapid filter swap race) are both mitigated as specified in the plan threat register: `.catch IOException` + `if (id == selectedFilterId) return` dedupe guard.

No new threat flags.

## Self-Check: PASSED

| Check | Result |
|-------|--------|
| `FilterPrefsRepository.kt` exists | FOUND |
| `DataModule.kt` exists | FOUND |
| `FilterSummary.kt` exists | FOUND |
| `CameraUiState.kt` has `filters: List<FilterSummary>` | FOUND |
| `CameraUiState.kt` has `selectedFilterId: String` | FOUND |
| `CameraViewModel.kt` has `fun onSelectFilter` | FOUND (1 match) |
| `CameraViewModel.kt` has `filterPrefsRepository` in ctor | FOUND |
| `CameraViewModel.kt` has `lastUsedFilterId.first()` in bind | FOUND |
| `isCapturing` re-entrance guard preserved | FOUND (1 match) |
| `bindJob?.cancel()` preserved | FOUND (1 match) |
| `captureFlashVisible = true` in onSuccess | FOUND (1 match) |
| `OneShotEvent.FilterLoadError` preserved | FOUND (5 matches) |
| `require(frameCount > 0)` in FilterDefinition.kt | FOUND |
| Commits b7bb433 + 596a406 + c7ff05f exist | FOUND |
| `testDebugUnitTest` exits 0 | PASSED |
| `assembleDebug` exits 0 | PASSED |
| No `@Ignore` in FilterPrefsRepositoryTest | VERIFIED (0 matches) |
| No `@Ignore` on Phase 4 tests in CameraViewModelTest | VERIFIED |
