---
phase: 04
slug: filter-catalog-picker-face-filter-mode
status: draft
nyquist_compliant: false
wave_0_complete: false
created: 2026-04-20
---

# Phase 04 — Validation Strategy

> Per-phase validation contract. Extends Phase 3 JUnit 4 + Robolectric 4.13 + Mockito-Kotlin harness with 7 new test files + 2 extensions. Phase 4 is the first phase with a genuine UI test dimension (Compose picker) — treated as manual-only for unit scope, formal Compose UI tests deferred to Phase 6.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit 4 + Mockito 5.11 + Mockito-Kotlin + Robolectric 4.13 + Turbine (Flow testing, added Wave 0) + DataStore test factory |
| **Config file** | `gradle/libs.versions.toml` + `app/build.gradle.kts` (+`turbine`, `+datastore-preferences` testImplementation) |
| **Quick run command** | `./gradlew :app:testDebugUnitTest` (with `JAVA_HOME="/c/Program Files/Android/Android Studio/jbr"` in bash env) |
| **Full suite command** | `./gradlew :app:testDebugUnitTest :app:assembleDebug` |
| **Estimated runtime** | ~60 seconds (Phase 3 baseline 45s + 7 new test classes) |

---

## Sampling Rate

- **After every task commit:** Run `./gradlew :app:testDebugUnitTest`
- **After every wave merge:** Run `./gradlew :app:testDebugUnitTest :app:assembleDebug`
- **Before `/gsd-verify-work`:** Full suite green AND clean debug APK builds AND manual device acceptance per 04-HANDOFF.md on Xiaomi 13T
- **Max feedback latency:** 90 seconds

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Threat Ref | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|------------|-----------|-------------------|-------------|--------|
| 04-01-NN | 01 (Wave 0 scaffolds + sprite extraction) | 0 | CAT-01..05, MOD-02, ADR inheritance | T-04-02 (manifest reject), T-04-05 (swap race) | unit scaffolds RED | `./gradlew :app:testDebugUnitTest` | ❌ Wave 0 creates | ⬜ pending |
| 04-02-NN | 02 (BehaviorState + CRAWL/SWARM/FALL impl) | 1 | MOD-02 behavioral correctness | T-04-05 | unit | `./gradlew :app:testDebugUnitTest --tests "*CrawlBehaviorTest* *SwarmBehaviorTest* *FallBehaviorTest* *BehaviorStateMapTest*"` | ❌ W0 → ✅ W1 | ⬜ pending |
| 04-03-NN | 03 (FilterCatalog expansion + FilterDefinition + BehaviorConfig) | 2 | CAT-01, CAT-02 | T-04-02 | unit | `./gradlew :app:testDebugUnitTest --tests "*FilterCatalogExpandedTest*"` | ❌ W0 → ✅ W2 | ⬜ pending |
| 04-04-NN | 04 (DataStore FilterPrefsRepository + CameraViewModel.onSelectFilter) | 3 | CAT-05, CAT-04 | T-04-01 (DataStore corruption) | unit + Robolectric | `./gradlew :app:testDebugUnitTest --tests "*FilterPrefsRepositoryTest* *CameraViewModelTest*onSelect*"` | ❌ W0 → ✅ W3 | ⬜ pending |
| 04-05-NN | 05 (LazyRow picker + CameraScreen wire + Cycle button removal) | 3 | CAT-03, CAT-04 | T-04-05, T-04-06 | manual (device) + VM unit | `./gradlew :app:testDebugUnitTest --tests "*CameraViewModelTest*"` + 04-HANDOFF | ❌ W0 → partial W3 | ⬜ pending |
| 04-06-NN | 06 (HomeScreen redesign + CameraRoute mode) | 3 | MOD-01 | T-04-06 (mode tampering) | unit + manual device | `./gradlew :app:testDebugUnitTest --tests "*HomeViewModelTest*"` + 04-HANDOFF | ❌ W0 → ✅ W3 | ⬜ pending |
| 04-07-NN | 07 (Multi-face render policy + soft cap) | 2-3 | MOD-02 success criterion #5 | T-04-03 (memory pressure) | unit (synthetic multi-face) | `./gradlew :app:testDebugUnitTest --tests "*FilterEngineTest*multiFace* *FilterEngineTest*softCap*"` | ❌ W0 → ✅ W2 | ⬜ pending |
| 04-08-NN | 08 (Clean build + 04-HANDOFF + device runbook) | 4 | All Phase 4 reqs device-verified | T-04-01..06 | manual (device) | `./gradlew :app:assembleDebug` + 04-HANDOFF.md sign-off | ❌ W0 → ✅ W4 | ⬜ pending |

*Task IDs (04-NN-MM) finalize when PLAN.md files land — planner may split 6-8 plans above. This map pins plan↔requirement↔test mapping so planner emits matching test commands in `<automated>` blocks.*

### Per-Requirement Test Specification

| Req ID | Behavior | Test Type | Automated Command | File |
|--------|----------|-----------|-------------------|------|
| CAT-01 | `FilterCatalog.all` has exactly 15 entries; all entries have non-empty `id`, `displayName`, `anchorType`, `behavior`, `frameCount > 0`, `assetDir` | unit (pure JVM) | `./gradlew :app:testDebugUnitTest --tests "*FilterCatalogExpandedTest*catalog_has_15_entries*"` | ❌ W0 — `FilterCatalogExpandedTest.kt` |
| CAT-02 | Each `FilterDefinition` field populated: `id` unique across catalog, `displayName` non-empty, `anchorType` in enum, `behavior` in enum, `frameCount > 0`, `frameDurationMs > 0`, `assetDir` in {sprite_spider, sprite_bugA, sprite_bugB, sprite_bugC}, `scaleFactor in (0, 1]`, `mirrorable: Boolean` | unit | same | ❌ W0 |
| CAT-03 | LazyRow picker renders all 15 items; tap → filter swap < 1 frame; rapid-tap 10 swaps in 5s does not cause CameraX rebind | **manual (device)** + ViewModel unit test | 04-HANDOFF Step + `./gradlew :app:testDebugUnitTest --tests "*CameraViewModelTest*"` | 04-HANDOFF + extend existing `CameraViewModelTest` |
| CAT-04 | `CameraViewModel.onSelectFilter(id)` invokes `filterEngine.setFilter(id)` + DataStore write within same coroutine | unit (mock FilterEngine + fake DataStore) | `./gradlew :app:testDebugUnitTest --tests "*CameraViewModelTest*onSelectFilter*"` | ❌ W0 — extend `CameraViewModelTest.kt` |
| CAT-05 | `FilterPrefsRepository.writeLastUsed(id)` then `readLastUsed()` returns same id; unknown id → default fallback; DataStore corruption → default | unit (fake InMemory DataStore) | `./gradlew :app:testDebugUnitTest --tests "*FilterPrefsRepositoryTest*"` | ❌ W0 — `FilterPrefsRepositoryTest.kt` |
| MOD-01 | `HomeScreen` renders Face Filter + Insect Filter buttons; Face Filter → `navController.navigate(CameraRoute(FaceFilter))`; Insect Filter → Toast (not navigate); settings gear + My Collection accessible | **manual (device)** + optional HomeViewModel unit | 04-HANDOFF + `./gradlew :app:testDebugUnitTest --tests "*HomeViewModelTest*"` (if extracted) | 04-HANDOFF + `HomeViewModelTest.kt` optional |
| MOD-02 | Face Filter mode = current Phase 3 camera pipeline with catalog picker wired; multi-face scene (2 faces) — primary full anchor + secondary bbox-center fallback; no crash | unit (synthetic 2-face injection) + manual device | `./gradlew :app:testDebugUnitTest --tests "*FilterEngineTest*multiFace*"` + 04-HANDOFF | ❌ W0 — extend `FilterEngineTest.kt` |

---

## Wave 0 Requirements

New test files required (all pure JVM unless Robolectric noted):

- [ ] `app/src/test/java/com/bugzz/filter/camera/filter/FilterCatalogExpandedTest.kt` — unit (pure JVM); pins 15 entries + field validation (CAT-01, CAT-02)
- [ ] `app/src/test/java/com/bugzz/filter/camera/data/FilterPrefsRepositoryTest.kt` — unit (InMemoryDataStore + Turbine for Flow); covers read/write round-trip + fallback (CAT-05)
- [ ] `app/src/test/java/com/bugzz/filter/camera/render/CrawlBehaviorTest.kt` — unit (pure JVM + mock contour); covers progress advance, wrap, contour interpolation (D-08)
- [ ] `app/src/test/java/com/bugzz/filter/camera/render/SwarmBehaviorTest.kt` — unit (pure JVM); covers instance init, drift toward anchor, respawn on close-to-anchor (D-09)
- [ ] `app/src/test/java/com/bugzz/filter/camera/render/FallBehaviorTest.kt` — unit (pure JVM); covers spawn timing, gravity advance, despawn at boundary (D-10)
- [ ] `app/src/test/java/com/bugzz/filter/camera/render/BehaviorStateMapTest.kt` — unit (Robolectric for PointF); covers D-13 ConcurrentHashMap lifecycle — setFilter clears map, onFaceLost removes entry, getOrPut creates fresh state
- [ ] **EXTEND** `app/src/test/java/com/bugzz/filter/camera/render/FilterEngineTest.kt` with `multiFace_primaryGetsContourAnchor_secondaryGetsBboxCenter` + `softCap_halvesSwarmInstancesWhenExceeded` (MOD-02, D-14)
- [ ] **EXTEND** `app/src/test/java/com/bugzz/filter/camera/ui/camera/CameraViewModelTest.kt` with `onSelectFilter_callsEngineAndWritesDataStore` + `initialBind_readsLastUsedFromDataStore` + `rapidSelectFilter_nocameraRebind` (CAT-04, CAT-05)

**Deps additions (libs.versions.toml + build.gradle.kts):**
- `io.coil-kt:coil-compose:2.7.0` (production — D-07)
- `androidx.datastore:datastore-preferences:1.1.3` (production — D-25)
- `app.cash.turbine:turbine:1.2.0` (testImplementation — Flow testing)
- `androidx.datastore:datastore-preferences-core:1.1.3` (testImplementation — InMemoryDataStore factory)

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| LazyRow picker renders 15 filters, smooth scroll, rapid-tap 10 swaps in 5s, no visible jank, no CameraX rebind | CAT-03 | Real Canvas + scroll gesture + CameraX preview interaction cannot be replicated in unit-test harness | 04-HANDOFF Step: "Open camera → observe picker strip above shutter → swipe left/right → confirm thumbnails scroll smoothly → tap 10 different filters within 5 seconds → verify each swap takes effect within 1 preview frame (no freeze, no 'Camera in use' error, no black flash)" |
| Home screen button navigation + Toast for disabled Insect Filter | MOD-01 | Nav-compose back-stack behavior + Compose Toast visual cannot be reliably asserted in headless unit | 04-HANDOFF Step: "From Home → tap Face Filter → camera opens → back button returns to Home. Tap Insect Filter → Toast 'Insect Filter coming in next release' → no navigation. Tap settings gear → Toast placeholder. Tap My Collection → Collection stub opens." |
| Multi-face rendering on device (2 people in frame) | MOD-02 #5 | Requires real ML Kit face detection with 2 faces — synthetic injection tests data flow but device confirms ML Kit behavior | 04-HANDOFF Step: "Stand with a friend in front of front camera OR hold up a printed photo of a face. Select any filter. Confirm: (a) primary face (largest bbox) gets full anchor-resolved bug, (b) secondary face gets bug centered on bbox middle, (c) no crash, (d) fps remains smooth with soft cap 20 in effect" |
| DataStore persistence across app restart (CAT-05) | CAT-05 | Process-level restart + DataStore file I/O is inherent to the feature | 04-HANDOFF Step: "Select filter 'bugC_fall' in picker → force-stop app → relaunch → observe filter picker auto-highlights 'bugC_fall' as selected + camera shows bugC FALL behavior immediately without user intervention" |
| Visual verification of CRAWL path on jawline | D-08 | Animated path smoothness + visual correctness of contour traversal requires real face + live preview | 04-HANDOFF Step: "Select `bugB_crawl` or `spider_jawline_crawl` → hold face steady → observe bug traversing along jawline → confirm loops continuously + direction consistent (CW or CCW per filter config) + speed reasonable (visible motion, not frozen/stuck)" |
| Visual verification of SWARM + FALL | D-09 / D-10 | Multi-instance animation with respawn and despawn requires live rendering | 04-HANDOFF Step (SWARM): "Select `spider_swarm` → observe 5-8 spiders drifting toward nose tip from various directions → when reach nose, respawn at face edge. SWARM stays active indefinitely."  Step (FALL): "Select `bugC_fall` → observe bugs raining from top of preview → each bug falls with constant speed → disappears at bottom → new bugs spawn every 200-400ms → max 8 simultaneous." |

---

## Validation Sign-Off

- [ ] All tasks have `<automated>` verify or Wave 0 dependencies
- [ ] Sampling continuity: no 3 consecutive tasks without automated verify
- [ ] Wave 0 covers all MISSING test references (7 new + 2 extensions)
- [ ] `io.coil-kt:coil-compose:2.7.0` and `androidx.datastore:datastore-preferences:1.1.3` added to catalog
- [ ] Turbine 1.2.0 added to testImplementation for Flow testing
- [ ] No watch-mode flags
- [ ] Feedback latency < 90s (60s Gradle baseline)
- [ ] `nyquist_compliant: true` set in frontmatter — flipped from `false` to `true` in plan-phase step 13 after planner emits `<automated>` blocks for every task

**Approval:** pending
