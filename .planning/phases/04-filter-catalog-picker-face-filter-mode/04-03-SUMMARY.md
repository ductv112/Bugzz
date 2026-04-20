---
phase: 04-filter-catalog-picker-face-filter-mode
plan: "03"
subsystem: render/behavior
tags: [sealed-interface, behavior-state, crawl, swarm, fall, bug-behavior, filter-engine, unit-tests]
dependency_graph:
  requires:
    - 04-01 (sprite assets extracted)
    - 04-02 (Wave 0 test scaffolds — CrawlBehaviorTest/SwarmBehaviorTest/FallBehaviorTest @Ignore'd)
  provides:
    - BehaviorState sealed interface (Static/Crawl/Swarm/Fall variants + CrawlDirection + BugInstance + FallingBug)
    - BugBehavior.Crawl real impl (D-08 FaceContour.FACE progress traversal)
    - BugBehavior.Swarm real impl (D-09 drift-toward-anchor + bbox-edge respawn)
    - BugBehavior.Fall real impl (D-10 gravity rain with spawn timer + cap)
    - BugBehavior.crawlPosition() companion helper (linear vertex interpolation)
    - FilterEngine migrated to BehaviorState.Static single-face stub
  affects:
    - Plan 04-04 (FilterEngine.onDraw multi-face + perFaceState ConcurrentHashMap)
    - Plan 04-04 (BehaviorConfig parameterisation of hardcoded defaults)
tech_stack:
  added: []
  patterns:
    - Sealed interface with per-variant mutable state (in-place mutation, zero allocation hot path)
    - Iterator.remove() pattern for safe ConcurrentModificationException-free list mutation in FALL
    - BugBehavior.Companion exposes defaults as const val for Plan 04-04 override via BehaviorConfig
    - Companion filter in Java reflection (`simpleName != "Companion"`) when testing sealed variant count
key_files:
  created:
    - app/src/main/java/com/bugzz/filter/camera/render/BehaviorState.kt
  modified:
    - app/src/main/java/com/bugzz/filter/camera/render/BugBehavior.kt
    - app/src/main/java/com/bugzz/filter/camera/render/FilterEngine.kt
    - app/src/test/java/com/bugzz/filter/camera/render/BugBehaviorTest.kt
    - app/src/test/java/com/bugzz/filter/camera/render/CrawlBehaviorTest.kt
    - app/src/test/java/com/bugzz/filter/camera/render/SwarmBehaviorTest.kt
    - app/src/test/java/com/bugzz/filter/camera/render/FallBehaviorTest.kt
    - app/src/test/java/com/bugzz/filter/camera/render/FilterEngineTest.kt
  deleted:
    - app/src/main/java/com/bugzz/filter/camera/render/BugState.kt
decisions:
  - "BugState.kt DELETED (not typealias-shimmed) — direct migration per Pitfall 7 recommendation"
  - "Companion class filtered from Java reflection in sealedInterface_hasExactlyFourVariants — companion object added in Plan 04-03 was not present in Phase 3 so test previously passed without filter"
  - "FilterEngineTest Frame mock stubs added: Size(640,480) for frame.size — required by new tick call (previewWidth/previewHeight from frame.size)"
  - "SWARM spawns instances at random bbox-EDGE points (not random interior) — matches D-09 respawn-at-edge semantic and makes initial spawn consistent with respawn location"
  - "FALL_SPAWN_INTERVAL_MAX_MS = 401 (exclusive upper for Random.nextInt contract) — test verifies range [200ms, 401ms*1_000_000L]"
  - "CrawlDirection.CCW uses negative delta that wraps via modulo: (progress - delta) % 1f + 1f if < 0 — same formula as CW with sign flip"
metrics:
  duration: "~8 minutes"
  completed_date: "2026-04-20"
  tasks_completed: 1
  files_changed: 9
---

# Phase 4 Plan 03: BehaviorState Sealed + CRAWL/SWARM/FALL Impls — Summary

**One-liner:** Sealed `BehaviorState` interface (D-12) replaces flat `BugState`; `BugBehavior.Crawl/Swarm/Fall` TODO stubs replaced with real D-08/D-09/D-10 impls; `FilterEngine` migrated to typed state; 15 Wave 0 tests un-Ignored and GREEN.

## Tasks Completed

| # | Name | Commit | Key outputs |
|---|------|--------|-------------|
| 1 | BehaviorState sealed + CRAWL/SWARM/FALL impls + FilterEngine migration | aa4a950 | BehaviorState.kt NEW, BugState.kt DELETED, BugBehavior.kt full rewrite, FilterEngine.kt type migration, 5 test files updated |

## Implementation Notes

### D-08 CRAWL math verification
With `bbox.width=200`, `previewWidth=400`, `dtMs=1000`:
```
delta = 1.0s * 0.5f * 200 / 400 = 0.25
```
`CrawlBehaviorTest.crawl_progressAdvancesPerDtMs` asserts `state.progress ≈ 0.25f` — matches D-08 spec exactly.

### BugState.kt — DELETED (not shimmed)
Research Pitfall 7 recommended direct migration. All references in `BugBehaviorTest.kt` and `FilterEngineTest.kt` updated to use `BehaviorState.Static(pos = PointF(...))` and `state.pos` instead of `state.position`. The `FilterEngine.kt` field renamed from `bugState: BugState` to `bugState: BehaviorState` with type-appropriate dispatch.

### FilterEngineTest Frame mock — Size stub added
The updated `FilterEngine.onDraw` now reads `frame.size.width` and `frame.size.height` to derive `previewWidth`/`previewHeight` for the behavior tick. All 3 Frame mocks in `FilterEngineTest` required `on { size } doReturn Size(640, 480)` — the `@Before` mock, the inline `flipbookIndex_advancesOverTime` loop mock, and both mocks in `setFilter_swap_resetsBugStateFrameIndex`.

### Companion class in sealed variant reflection
`BugBehavior.Companion` appears as a `declaredClass` in Java reflection (new in Plan 04-03 — Phase 3's `BugBehavior` had no companion). The `sealedInterface_hasExactlyFourVariants` test now filters `simpleName == "Companion"` before asserting the set equals `{Static, Crawl, Swarm, Fall}`.

### FALL spawn interval range
`FALL_SPAWN_INTERVAL_MAX_MS = 401` (exclusive) — `Random.nextInt(401 - 200) = Random.nextInt(201)` gives `[0, 200]` → interval `[200, 400]ms` inclusive. The `FallBehaviorTest.fall_nextSpawnNanos_updatedAfterSpawn` verifies `nextSpawnNanos ∈ [tsNanos + 200ms, tsNanos + 401ms]` in nanoseconds.

## Wave 0 Tests Un-Ignored

| File | Tests | Status |
|------|-------|--------|
| CrawlBehaviorTest.kt | 5 | GREEN |
| SwarmBehaviorTest.kt | 4 | GREEN |
| FallBehaviorTest.kt | 6 | GREEN |
| BugBehaviorTest.kt | 5 (3 new + 2 migrated) | GREEN |
| FilterEngineTest.kt | 7 existing (migrated) | GREEN |
| **Total active** | **27** | **all GREEN** |

BehaviorStateMapTest.kt remains `@Ignore`'d (awaits Plan 04-04).

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Companion class appears in sealed variant reflection test**
- **Found during:** First test run — `sealedInterface_hasExactlyFourVariants` FAILED
- **Issue:** `BugBehavior.Companion` (added for default constants) appears in `declaredClasses`, breaking the `setOf("Static","Crawl","Swarm","Fall")` assertion
- **Fix:** Filter `simpleName == "Companion"` before building the set
- **Files modified:** `BugBehaviorTest.kt`
- **Commit:** aa4a950 (same task commit — fix applied before final commit)

**2. [Rule 2 - Missing] Frame.size stub missing from FilterEngineTest mocks**
- **Found during:** Test compilation analysis — new `filter.behavior.tick(…, previewWidth, previewHeight, …)` requires `frame.size`
- **Issue:** Existing FilterEngineTest mocks only stubbed `timestampNanos` and `overlayCanvas`; `frame.size` would return null on an unstubbed Mockito mock, causing NPE in `frame.size.width.toFloat()`
- **Fix:** Added `on { size } doReturn Size(640, 480)` to all 4 Frame mocks in FilterEngineTest
- **Files modified:** `FilterEngineTest.kt`
- **Commit:** aa4a950

## Known Stubs

- `FilterEngine.onDraw` single-face `when (val s = bugState) { is BehaviorState.Static → s.pos; else → anchor }` — Plan 04-04 replaces with multi-instance rendering for Crawl/Swarm/Fall
- `FilterEngine` single `var bugState: BehaviorState` — Plan 04-04 replaces with `ConcurrentHashMap<Int, BehaviorState>` (D-13)

These stubs are intentional per plan scope and documented in the `FilterEngine.kt` inline comments.

## Threat Surface Scan

No new network endpoints, auth paths, file access patterns, or schema changes. Pure in-process behavior state — all mutations confined to `renderExecutor` single thread per D-18/T-04-05.

T-04-03 (SWARM/FALL unbounded lists): mitigated — `FALL_MAX_INSTANCES_DEFAULT = 8` cap enforced at spawn; SWARM list size fixed at `SWARM_INSTANCE_COUNT_DEFAULT = 6` (never grows after init). Verified by `fall_respectsMaxInstancesCap` + `swarm_instanceCountStableAcrossMultipleTicks`.

## Self-Check: PASSED

| Check | Result |
|-------|--------|
| `BehaviorState.kt` exists | FOUND |
| `BugState.kt` deleted | DELETED |
| `BugBehavior.kt` has no `TODO("Phase 4")` | NONE |
| Zero `BugState\b` class refs in main/ | NONE |
| Zero `@Ignore.*04-03` in test/ | 0 |
| Commit aa4a950 exists | FOUND |
| `testDebugUnitTest` exits 0 | PASSED |
| `assembleDebug` exits 0 | PASSED |
