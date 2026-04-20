---
phase: 04-filter-catalog-picker-face-filter-mode
plan: "04"
subsystem: render/filter-catalog
tags: [multi-face, concurrent-hashmap, soft-cap, behavior-config, filter-catalog, d-13, d-14, d-22, d-29, cat-01, cat-02]
dependency_graph:
  requires:
    - 04-01 (sprite assets extracted — sprite_spider/bugA/bugB/bugC dirs)
    - 04-02 (Wave 0 test scaffolds — BehaviorStateMapTest/FilterCatalogExpandedTest/FilterEngineTest multiFace @Ignore'd)
    - 04-03 (BehaviorState sealed + BugBehavior impls + FilterEngine single-face)
  provides:
    - FilterEngine.perFaceState: ConcurrentHashMap<Int, BehaviorState> (D-13)
    - FilterEngine.onDraw(List<SmoothedFace>) primary/secondary multi-face rendering (D-22)
    - FilterEngine.MAX_DRAWS_PER_FRAME = 20 soft cap (D-14)
    - FilterEngine.onFaceLost(trackingId) state eviction (D-23)
    - BehaviorConfig.kt sealed interface (Crawl/Swarm/Fall config variants, D-29)
    - FilterDefinition.behaviorConfig: BehaviorConfig? = null field (D-29)
    - FilterDefinition.scaleFactor init check added
    - SpriteManifest.behaviorConfig: JsonElement? = null (D-29 / T-04-02 shape extension)
    - FilterCatalog.all: 15 entries per D-02 amended roster (CAT-01)
    - BehaviorState.Swarm.targetCount + BehaviorState.Fall config fields (D-29 wiring)
  affects:
    - Plan 04-05 (FilterPrefsRepository / DataStore wiring uses FilterCatalog.byId)
    - Plan 04-06 (filter picker UI reads FilterCatalog.all for 15 thumbnails)
    - Plan 05+ (FilterEngine.onFaceLost wire-up from FaceDetectorClient.createAnalyzer)
tech_stack:
  added: []
  patterns:
    - ConcurrentHashMap<Int, BehaviorState> keyed by BboxIouTracker-assigned trackingId (D-13)
    - Eager perFaceState seeding before bitmap null-check ensures state continuity during preload
    - budgetRemaining param threads D-14 soft cap through drawFace without global state
    - BehaviorState.Swarm.targetCount / BehaviorState.Fall.* populated at state-creation time from BehaviorConfig (avoids BugBehavior tick signature change)
    - SpriteManifest.behaviorConfig: JsonElement? — untrusted boundary shape extension, NOT parsed in Phase 4
key_files:
  created:
    - app/src/main/java/com/bugzz/filter/camera/filter/BehaviorConfig.kt
  modified:
    - app/src/main/java/com/bugzz/filter/camera/render/FilterEngine.kt
    - app/src/main/java/com/bugzz/filter/camera/render/BehaviorState.kt
    - app/src/main/java/com/bugzz/filter/camera/render/BugBehavior.kt
    - app/src/main/java/com/bugzz/filter/camera/filter/FilterDefinition.kt
    - app/src/main/java/com/bugzz/filter/camera/filter/FilterCatalog.kt
    - app/src/main/java/com/bugzz/filter/camera/filter/SpriteManifest.kt
    - app/src/test/java/com/bugzz/filter/camera/render/FilterEngineTest.kt
    - app/src/test/java/com/bugzz/filter/camera/render/BehaviorStateMapTest.kt
    - app/src/test/java/com/bugzz/filter/camera/filter/FilterCatalogExpandedTest.kt
    - app/src/test/java/com/bugzz/filter/camera/ui/camera/CameraViewModelTest.kt
  deleted:
    - app/src/test/java/com/bugzz/filter/camera/filter/FilterCatalogTest.kt
    - app/src/main/assets/sprites/ant_on_nose_v1/ (35 PNGs + manifest)
    - app/src/main/assets/sprites/spider_on_forehead_v1/ (23 PNGs + manifest)
decisions:
  - "BehaviorState.Swarm gains targetCount:Int; BehaviorState.Fall gains maxInstances/spawnInterval*/gravityFactor — threaded from BehaviorConfig at state-creation time in FilterEngine.createBehaviorState, avoiding BugBehavior tick signature change"
  - "perFaceState seeded eagerly before bitmap null-check so state continuity holds even while assets load"
  - "FilterCatalogTest.kt DELETED — superseded by FilterCatalogExpandedTest; ant_on_nose/spider_on_forehead Phase 3 IDs gone"
  - "CameraViewModelTest.onCycleFilter assertions updated from % 2 to % 15 (catalog grew from 2 to 15)"
  - "SpriteManifest.behaviorConfig JsonElement shape added but NOT parsed in Phase 4 — hardcoded BehaviorConfig in FilterCatalog"
metrics:
  duration: "~25 minutes"
  completed_date: "2026-04-20"
  tasks_completed: 2
  files_changed: 12
---

# Phase 4 Plan 04: FilterEngine Multi-Face + FilterCatalog 15 Entries — Summary

**One-liner:** FilterEngine refactored to `ConcurrentHashMap<Int, BehaviorState>` perFaceState with primary/secondary face policy (D-22), MAX_DRAWS_PER_FRAME=20 soft cap (D-14), and `BehaviorConfig` sealed interface wired through 15-entry FilterCatalog (D-02/CAT-01).

## Tasks Completed

| # | Name | Commit | Key outputs |
|---|------|--------|-------------|
| 1 | FilterEngine multi-face + perFaceState + soft cap | 667ee6b | FilterEngine.kt rewrite; BehaviorState.Swarm/Fall config fields; BehaviorConfig.kt NEW; FilterDefinition+behaviorConfig; 7 Wave 0 tests un-Ignored GREEN |
| 2 | FilterCatalog 15 entries + BehaviorConfig schema + legacy cleanup | db89fa1 | FilterCatalog 15 entries; SpriteManifest+behaviorConfig; FilterCatalogExpandedTest 8 tests GREEN; FilterCatalogTest DELETED; 58 legacy sprite files removed |

## Actual Frame Counts vs Research Predictions

| Sprite group | Research predicted | Actual (Plan 04-01 extracted) | Match |
|---|---|---|---|
| sprite_spider | 23 | 23 | exact |
| sprite_bugA | 7 | 7 | exact |
| sprite_bugB | 12 | 12 | exact |
| sprite_bugC | 16 | 16 | exact |

Frame counts confirmed from Plan 04-01 SUMMARY — used directly in FilterCatalog constants.

## Wave 0 Tests Un-Ignored

| File | Tests un-Ignored | Status |
|------|-----------------|--------|
| FilterEngineTest.kt | 3 (multiFace_*, softCap_*, onFaceLost_*) | GREEN |
| BehaviorStateMapTest.kt | 4 (setFilter_clears, onFaceLost_removes, getOrPut_creates, sameInstance_reused) | GREEN |
| FilterCatalogExpandedTest.kt | 8 (catalog_has_15, uniqueIds, nonEmptyDisplayName, frameCount>0, scaleFactorRange, assetDirAllowed, allFourBehaviors, frameDurationMs>0) | GREEN |
| **Total new active** | **15** | **all GREEN** |

## BehaviorConfig Wiring Decision

BugBehavior tick signature kept uniform (no per-variant params). Instead:

- `BehaviorState.Swarm` gained `targetCount: Int` (default = `BugBehavior.SWARM_INSTANCE_COUNT_DEFAULT`)
- `BehaviorState.Fall` gained `maxInstances`, `spawnIntervalMinMs/MaxMs`, `gravityFactor` config fields
- `BugBehavior.Swarm.tick` uses `s.targetCount` for initial spawn count
- `BugBehavior.Fall.tick` reads `s.gravityFactor`, `s.maxInstances`, `s.spawnIntervalMinMs/MaxMs`
- `FilterEngine.createBehaviorState` populates these from `filter.behaviorConfig` at state-creation time

Result: per-filter tuning works without changing the sealed-interface tick signature, preserving Plan 04-03's CrawlBehaviorTest/SwarmBehaviorTest/FallBehaviorTest (all still GREEN).

## SpriteManifest BehaviorConfig Parsing Status

NOT wired in Phase 04-04. `SpriteManifest.behaviorConfig: JsonElement? = null` is a shape extension only. The 15 Phase 4 catalog entries hardcode their `BehaviorConfig` directly in `FilterCatalog.kt`. Future phases that parse `manifest.json → BehaviorConfig` must wrap in `try/catch(SerializationException)` per T-04-02.

## Soft-Cap Test Strategy

Used plan option (b): prime `perFaceState.Swarm.instances` to 15 each for 2 faces (30 total > 20 cap), then verify `atMost(MAX_DRAWS_PER_FRAME)` on `canvas.drawBitmap`. This avoids needing `behaviorConfig.instanceCount` to override `BugBehavior.Swarm.tick` spawn count — the direct state manipulation approach is simpler and deterministic.

## FilterCatalogTest Fate

**DELETED.** The Phase 3 test asserted `FilterCatalog.all.size == 2` and `byId("ant_on_nose_v1")` — both fail after catalog expansion. `FilterCatalogExpandedTest` fully supersedes it with 8 stronger assertions covering all 15 entries.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] FilterDefinition.behaviorConfig needed before FilterEngine compiled**
- **Found during:** Task 1 first compile — `filter.behaviorConfig` unresolved in FilterEngine.createBehaviorState
- **Issue:** Plan expected Task 2 to add `behaviorConfig` to FilterDefinition; FilterEngine (Task 1) already references it
- **Fix:** Added `behaviorConfig: BehaviorConfig? = null` to FilterDefinition during Task 1 (before Task 2 would have done it). Task 2 only needed to add import cleanup.
- **Files modified:** `FilterDefinition.kt`
- **Commit:** 667ee6b

**2. [Rule 1 - Bug] getOrPut_createsFreshStateForNewTrackingId failed — state not created when bitmap null**
- **Found during:** Task 1 test run — BehaviorStateMapTest FAILED
- **Issue:** `perFaceState.getOrPut` was inside `drawFace` which is only called after `assetLoader.get() ?: return`. When bitmap is null (preload incomplete), state was never seeded.
- **Fix:** Eagerly seed `perFaceState` for up to 2 faces before the bitmap null-check in `onDraw`. State continuity now holds even during preload.
- **Files modified:** `FilterEngine.kt`
- **Commit:** 667ee6b

**3. [Rule 1 - Bug] CameraViewModelTest.onCycleFilter failed — % 2 modulus hardcoded for Phase 3 catalog size**
- **Found during:** Task 2 full test run — test expected 3rd cycle to wrap back to all[0] (% 2), but catalog is now 15 entries so 3rd cycle goes to all[2]
- **Fix:** Updated test comments and assertions to use 15-entry semantics: 3rd cycle → `all[2]` (spider_jawline_crawl), not `all[0]`
- **Files modified:** `CameraViewModelTest.kt`
- **Commit:** db89fa1

**4. [Rule 1 - Bug] BugBehavior.Fall.tick used companion constants instead of state fields**
- **Found during:** Plan inspection — Fall.tick called `FALL_GRAVITY_FACTOR_DEFAULT`, `FALL_MAX_INSTANCES_DEFAULT` etc. directly; D-29 BehaviorConfig wiring had no effect on Fall behavior
- **Fix:** Updated `BugBehavior.Fall.tick` to read `s.gravityFactor`, `s.maxInstances`, `s.spawnIntervalMinMs/MaxMs` from state. `BehaviorState.Fall` gained these fields with same defaults as companion constants.
- **Files modified:** `BugBehavior.kt`, `BehaviorState.kt`
- **Commit:** 667ee6b

## Known Stubs

None — all FilterCatalog entries are fully wired. BehaviorConfig fields default to BugBehavior companion constants when null (backward-compatible). SpriteManifest.behaviorConfig is an intentional shape extension, not a stub — it is not read by Phase 4 code.

## Threat Surface Scan

No new network endpoints, auth paths, or file access patterns introduced. `SpriteManifest.behaviorConfig: JsonElement?` adds a new JSON parse boundary — T-04-02 (already in plan threat register with `mitigate` disposition). Phase 4 does not parse this field; mitigation documented via KDoc TODO in SpriteManifest.kt.

## Self-Check: PASSED

| Check | Result |
|-------|--------|
| `BehaviorConfig.kt` exists | FOUND |
| `FilterEngine.kt` has `ConcurrentHashMap` | FOUND (3 occurrences) |
| `FilterEngine.kt` has `MAX_DRAWS_PER_FRAME = 20` | FOUND |
| `FilterCatalog.kt` has 15 `FilterDefinition(` calls | FOUND (grep -c = 15) |
| No old filter IDs in FilterCatalog | VERIFIED (grep -c ant_on_nose = 0) |
| Legacy sprite dirs removed | VERIFIED (only bad_filter/sprite_* /test_filter remain) |
| `require(frameCount > 0)` in FilterDefinition | FOUND |
| `testDebugUnitTest` exits 0 | PASSED (107 tests, 8 skipped) |
| `assembleDebug` exits 0 | PASSED |
| Commits 667ee6b + db89fa1 exist | FOUND |
