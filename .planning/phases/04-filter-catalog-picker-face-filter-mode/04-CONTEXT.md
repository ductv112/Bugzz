# Phase 4: Filter Catalog + Picker + Face Filter Mode - Context

**Gathered:** 2026-04-20
**Status:** Ready for planning

<domain>
## Phase Boundary

Scale the Phase 3 single-filter pipeline into a shipping catalog of 15 bundled bug filters across all 4 `BugBehavior` variants (STATIC + CRAWL + SWARM + FALL), with a horizontal `LazyRow` filter picker inline above the shutter, Home-screen redesign introducing "Face Filter" / "Insect Filter" mode buttons plus placeholder settings gear + collection navigation, `DataStore` last-used-filter persistence, and multi-face rendering policy (primary-full + secondary-bbox-fallback, soft cap 20 bugs/frame).

**Out of scope:** Insect Filter free-placement sticker mode (Phase 5 MOD-03..07), video recording UX (Phase 5 VID-01..10), production splash/onboarding/preview/collection screens beyond minimal wiring (Phase 6 UX-01..09), share intent integration (Phase 6 SHR-01..04), formal ≥24fps measured validation with profiler (Phase 7 PRF-01), cross-OEM device matrix (Phase 7 PRF-05), settings screen functional content (Phase 6 UX-09).

</domain>

<decisions>
## Implementation Decisions

### Catalog Scope (gray area 1)
- **D-01:** Ship **15 bug filters** in Phase 4 (trimmed mid-point vs reference's ~20-25). Budget math: 15 × ~500KB sprite avg = ~7-8 MB total sprite payload; keeps debug APK well under 40 MB PRF-04 ceiling. Full catalog coverage for all 4 behaviors (roughly 3-4 filters per behavior variant). Phase-extensible: later milestone can add more filters additively without catalog refactor.
- **D-02:** Filter roster target (finalized at extraction time — if reference APK is missing one, substitute from remaining pool): `ant_on_nose`, `spider_on_forehead`, `cockroach_on_cheek`, `worm_crawl`, `beetle_swarm`, `fly_fall`, `scorpion_on_chin`, `centipede_crawl`, `wasp_swarm`, `tick_on_forehead`, `caterpillar_crawl`, `moth_fall`, `mosquito_swarm`, `mantis_on_cheek`, `roach_swarm`. Behavior distribution: ~4 STATIC, ~4 CRAWL, ~4 SWARM, ~3 FALL.

### Sprite Extraction (gray area 1)
- **D-03:** **Re-extract all 15 filters from reference APK uniformly** in Phase 4 Wave 0. **This supersedes `03-gaps-01-PLAN.md`** (spider sprite fix becomes a subset of the catalog extraction pass). The existing working `ant_on_nose_v1` sprite set is re-extracted with the corrected Lottie layer detection for consistency — if the layer is unchanged, asset bytes are identical. Phase 3 PLan `03-gaps-01` is marked superseded, not executed.
- **D-04:** **Extraction strategy per Lottie file:** inspect `*_prankfilter.json` under `reference/extracted/assets/`, enumerate all `data.layers[*].nm` (layer names), identify the FILL/BODY layer (highest non-alpha pixel count) vs OUTLINE/STROKE layer, target the fill layer's `"p":` base64 PNG entries. Fallback if fill layer is vector-only (no embedded raster): render Lottie via `lottie-render` npm or `rlottie` CLI. Document each filter's resolved layer name in Wave 0 SUMMARY.
- **D-05:** **Extraction validation:** each extracted `frame_00.png` must have >10% non-alpha pixels (guard against Phase 3's spider silhouette bug). If a filter fails the validation, log the failure, skip the filter from catalog, substitute from the remaining roster pool; Phase 4 ships whatever 15 extract successfully (target 15; minimum 12 acceptable).

### Thumbnail Source (gray area 1)
- **D-06:** **Picker thumbnail = first frame of flipbook** (`assets/sprites/<filterId>/frame_00.png`), loaded via **Coil 2.7** (research STACK.md prescribed, Phase 3 deferred) at runtime with `AsyncImage(model = "file:///android_asset/sprites/$id/frame_00.png", modifier = Modifier.size(72.dp))`. Zero extra asset bytes. Coil internal memory cache does not collide with `AssetLoader.LruCache` (FilterEngine render-thread cache) — different scope.
- **D-07:** **Add `io.coil-kt:coil-compose:2.7.0` to `libs.versions.toml` in Phase 4 Wave 0** (Phase 3 deferred this; CLAUDE.md Executive Recommendation table pre-allocated it). Error-state fallback: default placeholder bitmap (white circle with question mark) if sprite load fails. Loading state: thumbnail shimmer (optional — Claude discretion).

### BugBehavior Implementations (gray area 2)
- **D-08:** **CRAWL** — bug traverses `FaceContour.FACE` (36-point face perimeter) loop. Progress as `Float in [0, 1)` advancing each frame by `dt * (0.5 * faceBoxWidth / previewWidth)` (≈ 50% face-box width per second at 30fps). Wrap at 1.0 back to 0. Direction: per-filter config (CW or CCW default), fixed — no reverse mid-session. Point interpolation: linear between adjacent contour vertices based on progress * 36.
- **D-09:** **SWARM** — 5–8 bug instances (per-filter tunable via `FilterDefinition.behaviorConfig.swarmCount`). On behavior init: spawn all instances at random PointF within face boundingBox. Each instance: `velocity = (anchor - position).normalize() * randomSpeed` where `randomSpeed ∈ [0.3, 0.8] * faceBoxWidth / second`. When `distance(position, anchor) < 0.2 * faceBoxWidth`, respawn at random boundingBox-edge point. Anchor = filter's `anchorType` (NOSE_TIP default).
- **D-10:** **FALL** — rain-of-bugs pattern. Behavior state: `instances: MutableList<FallingBug>, nextSpawnNanos: Long`. Spawn rule: every 200–400ms (per-filter tunable, randomized per spawn), add new FallingBug at `position = (randomX in [0, previewWidth], 0)`, `velocity = (0, gravityPixelsPerSecond)` where `gravity ≈ 50% * previewHeight / second`. Update: `position.y += velocity.y * dt`. Despawn: when `position.y > previewHeight`, remove from list. Max 8 simultaneous instances per face (cap-enforced via D-12).
- **D-11:** **STATIC** — unchanged from Phase 3 (position = anchor, velocity = 0). Ships already in Phase 3 (03-CONTEXT D-04).

### FilterEngine State Schema (gray area 2)
- **D-12:** **Refactor `BugState` → sealed `BehaviorState`** with per-variant shape:
  ```kotlin
  sealed interface BehaviorState {
    data class Static(val pos: PointF) : BehaviorState
    data class Crawl(var progress: Float, val direction: CrawlDirection) : BehaviorState
    data class Swarm(val instances: MutableList<BugInstance>) : BehaviorState
    data class Fall(val instances: MutableList<FallingBug>, var nextSpawnNanos: Long) : BehaviorState
  }
  data class BugInstance(var position: PointF, var velocity: PointF, var frameIndex: Int)
  data class FallingBug(var position: PointF, var velocity: PointF, val spawnNanos: Long)
  enum class CrawlDirection { CW, CCW }
  ```
- **D-13:** **FilterEngine holds `perFaceState: ConcurrentHashMap<Int, BehaviorState>`** keyed by BboxIouTracker-assigned `TrackedId: Int` (Phase 3 ADR-01 ID). On `setFilter()`: clear map (per-face state is filter-specific — no carry-over). On `onDraw` for each tracked face: `getOrPut(face.trackingId) { create(activeFilter.behavior) }`. On face drop (BboxIouTracker removedIds): remove entry from map. Render for primary + secondary (D-19) within soft-cap budget (D-20).

### Soft-Cap for Bug Draw Calls (gray area 4)
- **D-14:** **Soft cap 20 total bug draw calls per frame across all faces.** Evaluation order in `FilterEngine.onDraw`: primary face first (all instances), secondary face second. If primary + secondary cumulative draw count > 20, halve per-face SWARM/FALL instance count for this frame only (compute `scaleFactor = 20 / totalCount`, take `instances.subList(0, (size * scaleFactor).toInt())`). STATIC + CRAWL always draw (1 instance per face). Cap kicks in only for SWARM + FALL heavy scenes. Configurable constant `FilterEngine.MAX_DRAWS_PER_FRAME = 20` companion.

### Filter Picker UX (gray area 3)
- **D-15:** **LazyRow inline strip** positioned 100dp above the bottom-center shutter button. Full preview width, 100dp height (72dp thumbnail + 16dp top + 12dp bottom padding). Thumbnails 72dp × 72dp rounded corners 12dp with 8dp horizontal spacing.
- **D-16:** **Selected state:** 2dp white border ring + 1.15× scale-up (`Modifier.scale(1.15f)`) + `animateFloatAsState` 200ms transition. Tap → `LazyListState.animateScrollToItem(index, scrollOffset = -viewportWidth/2 + thumbnailWidth/2 + padding)` for center-snap. Filter `displayName` rendered below thumbnail as `Text(fontSize = 10sp, maxLines = 1)`.
- **D-17:** **Picker state hoisting:** `rememberLazyListState()` scoped to `CameraScreen` so scroll position survives recomposition. Selected filter ID bubbles up to ViewModel → `filterEngine.setFilter(filterId)` + DataStore write via `CameraViewModel.onSelectFilter(id)`.
- **D-18:** **Replaces Phase 3 debug Cycle Filter button** (03-CONTEXT D-10). TEST RECORD debug button (Phase 2 D-04) stays at `Alignment.BottomStart` `BuildConfig.DEBUG`-gated.

### Home Screen Redesign (gray area 3)
- **D-19:** **HomeRoute redesign** replacing Phase 1 stub. Layout (portrait-locked):
  - Top-right: settings gear `IconButton` (placeholder — Phase 6 UX-09 wires content; Phase 4 shows Toast "Settings: coming soon" on tap)
  - Center (vertical stack, 32dp spacing):
    - "Face Filter" button — 200dp × 80dp, primary filled button, navigates to `CameraRoute` in Face Filter mode
    - "Insect Filter" button — same dimensions, secondary outlined button, **disabled** in Phase 4 with `onClick { Toast("Insect Filter coming in next release") }`
  - Bottom: "My Collection" button (Phase 1 stub behavior preserved) — 160dp × 56dp outlined button; Phase 6 UX wires real content
- **D-20:** **`CameraRoute` receives mode parameter:** `@Serializable data class CameraRoute(val mode: CameraMode = CameraMode.FaceFilter)`. Phase 4 implements only `FaceFilter`; `InsectFilter` enum variant exists but `CameraRoute` with that mode → shows "Not yet available" stub (Phase 5 MOD-03..07 activates).
- **D-21:** **Orientation lock** (Phase 2 D-07 carried): Home + Camera still portrait-locked.

### Multi-Face Policy (gray area 4)
- **D-22:** **Primary face = full FaceLandmarkMapper anchor resolution** (03-CONTEXT D-30 7-anchor ladder). Secondary face = boundingBox-center fallback (anchor coordinate = `(bbox.left + bbox.width * 0.5, bbox.top + bbox.height * 0.4)` where y=0.4 places bug mid-forehead height). Primary = tracker-assigned ID with largest bbox area; secondary = second-largest. MAX_TRACKED_FACES=2 from Phase 3 D-22 carries (03-CONTEXT D-22) — no third face rendered.
- **D-23:** **Behavior on primary-face loss (user walks out of frame):** `onFaceLost(primaryId)` → BboxIouTracker `removedIds` → FilterEngine removes `perFaceState[primaryId]`. Next frame: secondary face (if present) becomes new primary (largest bbox). BehaviorState re-initializes for the new primary ID (fresh state, no carry-over). This exercises the `perFaceState.getOrPut` flow.
- **D-24:** **Test fixture:** Phase 4 VALIDATION.md adds a "multi-face synthetic scene" unit test — feed FilterEngine `List<SmoothedFace>` of size 2 with controlled boundingBox areas; assert primary gets contour-anchor state, secondary gets bbox-center state, draw count respects soft cap.

### Last-Used Filter Persistence (CAT-05)
- **D-25:** **DataStore Preferences** (Phase 0 research STACK.md already allocated `androidx.datastore:datastore-preferences:1.1.3`). Key: `stringPreferencesKey("last_used_filter_id")`. Default: first filter id in `FilterCatalog.all` (`ant_on_nose`). Written on `CameraViewModel.onSelectFilter()` (async via `viewModelScope.launch`). Read on `CameraViewModel.bind()` initial load — if stored id doesn't exist in current catalog, fall back to default. Repository class: `com.bugzz.filter.camera.data.FilterPrefsRepository` (@Singleton @Inject).

### Data-layer Module Placement
- **D-26:** **New package:** `com.bugzz.filter.camera.data` for `FilterPrefsRepository.kt`. Hilt module `DataModule.kt` under `com.bugzz.filter.camera.di` provides `DataStore<Preferences>` singleton wiring. Phase 4 is the first data-layer addition — Phase 1 D-01 deferred DataStore until needed, now it's needed.

### Filter Registration Order
- **D-27:** **FilterCatalog.all List ordering:** match the user-facing picker order in the catalog `object`. Order is curated for picker UX flow (simple STATIC first, then CRAWL/SWARM/FALL mixed for visual variety) rather than alphabetical. Exact order finalized at implementation time; planner/executor has latitude.

### Integration with Phase 3
- **D-28:** **Inherit 03-CONTEXT D-01..D-40 decisions** — every Phase 3 lock is carried forward. Phase 4 is strictly additive (5 new files + additive edits to CameraScreen, CameraViewModel, HomeRoute); NO Phase 3 production classes deleted.
- **D-29:** **FilterDefinition schema extended** (03-CONTEXT D-29) — add optional `behaviorConfig: BehaviorConfig` sealed class with per-behavior tuning: `CrawlConfig(direction, speedFactor)`, `SwarmConfig(instanceCount, driftSpeedRange)`, `FallConfig(spawnIntervalMsRange, gravity, maxInstances)`. Backwards-compatible — Phase 3 STATIC filter ignores `behaviorConfig`. Manifest.json grows optional fields.

### Claude's Discretion
- Exact 15 filters (reference APK may reveal different set than D-02 roster — planner picks during Wave 0 based on extraction success)
- CRAWL direction default (CW or CCW) per filter — pick aesthetically during implementation
- Filter name text color / picker background tint / Material3 theme choices
- Settings gear icon choice (Icons.Default.Settings vs custom)
- Disabled-button visual style for "Insect Filter" (greyed vs full with tooltip)
- Collection icon/button exact visual (Phase 1 stub vs new shape)
- Thumbnail loading shimmer vs instant (Coil default vs explicit placeholder)
- LazyListState rememberSaveable key strategy for surviving configuration changes
- Toast messages for Settings + Insect Filter placeholders (wording)
- FilterPrefsRepository write debounce (whether to throttle rapid filter swaps — default no, DataStore handles coalescing)
- Exact swarmCount per filter (5-8 range, picker's choice based on sprite visual density)
- BehaviorConfig default values when manifest.json omits `behaviorConfig` (ship sensible defaults)
- Any Robolectric test strategy for picker LazyListState (likely Compose UI test — defer if complex)

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Project specs (carry-forward from Phase 3)
- `.planning/PROJECT.md` — project vision, locked stack, English UI convention
- `.planning/REQUIREMENTS.md` §Filter Catalog (CAT-01..05) + §Dual Mode (MOD-01, MOD-02) — Phase 4 primary scope
- `.planning/ROADMAP.md` §Phase 4 — goal + 5 success criteria (15-25 filters, 4 behaviors, picker, DataStore, multi-face)
- `.planning/STATE.md` §Accumulated Context — all 16 execution learnings from Phases 1-3

### Phase 3 — MANDATORY reading for constraint inheritance
- `.planning/phases/03-first-filter-end-to-end-photo-capture/03-CONTEXT.md` — **MANDATORY**: D-01..D-40 lock every architectural decision Phase 4 must honor (package layout, FilterEngine API, AssetLoader LruCache, BboxIouTracker spec, FaceLandmarkMapper anchor ladder, shutter UX)
- `.planning/phases/03-first-filter-end-to-end-photo-capture/03-RESEARCH.md` §Open Questions (RESOLVED) — Q1-Q5 answers
- `.planning/phases/03-first-filter-end-to-end-photo-capture/03-VERIFICATION.md` — 5/5 must-haves evidence
- `.planning/phases/03-first-filter-end-to-end-photo-capture/03-05-SUMMARY.md` — 2 soft gaps documentation (CAP-04 Phase 6 deferral, spider extraction superseded by Phase 4 D-03)
- `.planning/phases/03-first-filter-end-to-end-photo-capture/03-REVIEW.md` + `03-REVIEW-FIX.md` — 5 warnings fixed in Phase 3 polish pass; 7 info items deferred (IN-01..IN-07) — some may naturally close in Phase 4 work (IN-07 multi-face BugState refactor = D-12/D-13)
- `.planning/phases/03-first-filter-end-to-end-photo-capture/03-gaps-01-PLAN.md` — **superseded by Phase 4 D-03** (spider re-extraction is now part of catalog extraction pass, not standalone gap plan)

### Phase 2 — ADR inheritance
- `.planning/phases/02-camera-preview-face-detection-coordinate-validation/02-ADR-01-no-ml-kit-tracking-with-contour.md` — ADR 01 all 4 follow-ups closed in Phase 3; Phase 4 respects tracker-assigned ID contract (non-null Int from BboxIouTracker)
- `.planning/phases/02-camera-preview-face-detection-coordinate-validation/02-CONTEXT.md` — executor/threading topology (D-18 cameraExecutor + renderExecutor) — Phase 4 preserves

### Research
- `.planning/research/STACK.md` — Coil 2.7 (D-07 wiring), DataStore Preferences 1.1.3 (D-25), kotlinx.serialization.json (manifest.json parser carryover)
- `.planning/research/ARCHITECTURE.md` §3 (rendering pipeline — OverlayEffect Canvas), §6 (patterns — StateFlow + unidirectional data flow), §12 (decisions)
- `.planning/research/PITFALLS.md` §3 (landmark jitter + 1€ carryover), §4 (ImageAnalysis backpressure — preserved), §7 (device fragmentation), §13 (multi-face contour limit — D-22 secondary fallback addresses)
- `.planning/research/SUMMARY.md` — Canvas-not-Filament lock; Phase 4 continues Canvas path

### CameraX / Compose external docs
- [Jetpack Compose LazyRow](https://developer.android.com/develop/ui/compose/lists#lazy) — picker implementation
- [LazyListState.animateScrollToItem](https://developer.android.com/reference/kotlin/androidx/compose/foundation/lazy/LazyListState) — D-16 center-snap
- [Coil Compose](https://coil-kt.github.io/coil/compose/) — AsyncImage + memory cache
- [DataStore Preferences guide](https://developer.android.com/topic/libraries/architecture/datastore) — D-25 last-used persistence
- [Material3 Button + IconButton](https://developer.android.com/develop/ui/compose/components/button) — Home redesign
- [navigation-compose type-safe args](https://developer.android.com/develop/ui/compose/navigation#type-safety) — D-20 CameraRoute(mode)

### Reference APK (asset source for D-03 extraction)
- `reference/com.insect.filters.funny.prank.bug.filter.face.camera.apk` — 15 Lottie JSON files (`*_prankfilter.json` pattern) under extracted `assets/`

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets (from Phases 1-3)
- `com.bugzz.filter.camera.filter.FilterDefinition` — extend with optional `behaviorConfig: BehaviorConfig` per D-29 (additive field, Phase 3 filters unaffected)
- `com.bugzz.filter.camera.filter.FilterCatalog` — currently 2 filters (ant, spider); Phase 4 expands to 15; change shape from hardcoded `object` to `@Singleton @Inject` class loading from catalog descriptor (Claude discretion — hardcoded list still acceptable if simpler)
- `com.bugzz.filter.camera.filter.AssetLoader` — already has 32MB LruCache; Phase 4's 15 filters × ~15 frames × ~200KB = ~45MB total, so eviction is intentional + exercises Phase 3 D-09 eviction path
- `com.bugzz.filter.camera.filter.SpriteManifest` — extend `@Serializable` with optional `behaviorConfig: JsonElement?` field (parse at load time)
- `com.bugzz.filter.camera.render.BugBehavior` — sealed interface, STATIC impl + 3 stubs; Phase 4 replaces stubs with CRAWL/SWARM/FALL per D-08/D-09/D-10
- `com.bugzz.filter.camera.render.BugState` — **rename to `BehaviorState`** per D-12 sealed refactor. Phase 3 Static state embedded in refactored sealed variant.
- `com.bugzz.filter.camera.render.FilterEngine` — `perFaceState: ConcurrentHashMap<Int, BehaviorState>` added per D-13; onDraw loops over `faces` list (currently single face)
- `com.bugzz.filter.camera.detector.FaceLandmarkMapper` — 7-anchor ladder untouched; Phase 4 reuses verbatim for primary; secondary uses bbox-center outside the mapper
- `com.bugzz.filter.camera.detector.FaceSnapshot` — currently `faces: List<SmoothedFace>` (Phase 3 already list-shaped); Phase 4 uses full list (not just primary)
- `com.bugzz.filter.camera.detector.BboxIouTracker` — MAX_TRACKED_FACES=2 unchanged; tracker returns TrackerResult(tracked, removedIds); Phase 4 uses removedIds to clean `perFaceState`
- `com.bugzz.filter.camera.ui.camera.CameraScreen` — add LazyRow picker inline above shutter; remove Cycle Filter debug button (03-CONTEXT D-10 superseded by picker)
- `com.bugzz.filter.camera.ui.camera.CameraViewModel` — add `onSelectFilter(id)` + DataStore write path + initial read on bind()
- `com.bugzz.filter.camera.ui.camera.CameraUiState` — add `filters: List<FilterSummary>, selectedFilterId: String` fields for picker
- `com.bugzz.filter.camera.ui.nav.Routes` — Phase 1 type-safe @Serializable route; Phase 4 updates `CameraRoute` to `data class CameraRoute(val mode: CameraMode = CameraMode.FaceFilter)`
- `com.bugzz.filter.camera.ui.screens.StubScreens` — Phase 1 stub `HomeScreen`; Phase 4 **replaces** with production `HomeScreen.kt` under `com.bugzz.filter.camera.ui.home`
- `com.bugzz.filter.camera.ui.BugzzApp` — nav graph rewire HomeRoute → new HomeScreen

### New Packages (Phase 4 additions)
- `com.bugzz.filter.camera.data` — FilterPrefsRepository (DataStore wrapper)
- `com.bugzz.filter.camera.ui.home` — production HomeScreen + HomeViewModel (if needed; may be composable-only)

### Established Patterns (to replicate)
- **Hilt `@Singleton` + `@Inject` constructor-split pattern** (STATE #14 Phase 2 learned) — apply to any new class with test seam
- **kotlinx.serialization.json** (Phase 1 nav + Phase 3 SpriteManifest) — BehaviorConfig uses JsonElement polymorphism
- **AtomicReference for cross-thread handoff** (03-CONTEXT D-13, D-19) — `perFaceState: ConcurrentHashMap` replaces AtomicReference for map-shape (still thread-safe)
- **DataStore preferences async write** — `viewModelScope.launch` pattern, never block Main
- **Compose Material3** — use M3 components (Button, OutlinedButton, IconButton, Scaffold); no Material2
- **Type-safe nav routes** via `@Serializable` — extend CameraRoute additively

### Integration Points
- `gradle/libs.versions.toml` — add `coil-compose = "2.7.0"` + `datastorePreferences = "1.1.3"` entries + library aliases + plugin entries if needed (neither previously catalogued per Phase 3 `kotlinx-serialization-json` grep confirmed)
- `app/build.gradle.kts` — add `implementation(libs.coil.compose)` + `implementation(libs.androidx.datastore.preferences)`
- `AndroidManifest.xml` — no changes for Phase 4 (CAMERA already granted, no new permissions)
- `di/CameraModule.kt` — add FilterPrefsRepository binding OR create new `di/DataModule.kt` (Claude discretion)
- Unit test scaffolds (Wave 0 Nyquist gate per Phase 3 precedent): `CrawlBehaviorTest`, `SwarmBehaviorTest`, `FallBehaviorTest`, `FilterCatalogExpandedTest` (15 entries), `FilterPrefsRepositoryTest`, `HomeScreenTest` (optional Compose UI), multi-face `FilterEngineTest.multiFace_*`

</code_context>

<specifics>
## Specific Ideas

- User prefers **stop-test per phase on Xiaomi 13T** — `--chain` this invocation runs discuss → plan → execute for Phase 4, then user verifies on device before Phase 5.
- Phase 4 is the **first non-trivial UI phase** — LazyRow picker + Home redesign bring design-system concerns that Phase 1-3 deferred. If gsd-ui-phase workflow re-triggers, accept auto-generation since ROADMAP has `UI hint: yes` for Phase 4.
- Phase 4 validates **two multi-part architectural refactors simultaneously**: (1) BehaviorState sealed + per-face ConcurrentHashMap (Phase 3 IN-07 code review finding), (2) DataStore introduction (first data-layer component). Plan should sequence these in separate waves.
- **Phase 4 acceptance gate** on Xiaomi 13T: 15 filters visible in picker, ant visible on nose with CRAWL filter (the worm), multi-face test (hold up a second printed photo), DataStore persists across app restart.
- **Soft CAP-04 from Phase 3** (mirror A/B) — NOT scoped into Phase 4. Phase 6 UX Polish is the designated closure.
- **Code review IN-07** ("single bugState blocks multi-face") — naturally closes via D-12 + D-13 (sealed BehaviorState + perFaceState map).

</specifics>

<deferred>
## Deferred Ideas

- **25-filter full set** (match reference exactly) — Phase 4 ships 15; later milestone can expand additively. No code refactor required to go from 15 → 25.
- **Bottom-sheet picker UX** — inline strip chosen (D-15); Phase 6 UX Polish could revisit if user feedback wants modal swipe-up.
- **Orbit SWARM pattern** (10-12 bugs circling anchor) — simpler drift pattern chosen (D-09); future creative expansion.
- **Physics-based FALL** (bounce off chin, spin) — constant-gravity chosen (D-10); future creative expansion.
- **Per-filter sound effects** (buzzing mosquito, tapping cockroach) — out of scope per PROJECT.md (reference has no SFX).
- **Filter categorization** (group by behavior or insect type in picker) — flat list chosen; Phase 6 UX Polish could add sub-navigation if catalog grows past 25.
- **Hard cap on draw calls** (10 per frame) — soft cap 20 chosen (D-14); Phase 7 measurement decides if tighter cap needed.
- **Thumbnail error placeholder design polish** — default placeholder acceptable for Phase 4; Phase 6 UX polish.
- **Picker loading shimmer** — Claude discretion; default to Coil's fade-in. Polish in Phase 6 if jarring.
- **LazyRow scroll haptic** — no haptic on scroll; only on selection tap (Phase 3 D-15 haptic for shutter carries). Could add in Phase 6 if desired.
- **Accessibility TalkBack labels for filter picker** — defer to Phase 6 UX polish (non-blocker for MVP personal-use).
- **Filter grouping by behavior color-coding in picker** — deferred; flat neutral styling Phase 4.
- **Advanced Settings screen content** (detection mode toggle, overlay opacity) — Phase 6 UX-09; Phase 4 ships icon-only placeholder.
- **Insect Filter mode full enablement** — Phase 5 MOD-03..07 (free-placement + drag + pinch + rotate).
- **Formal Canvas-vs-GL escalation decision** — Phase 7 performance measurement; Phase 4 stays Canvas.
- **Reference APK alternative source** (if D-03 extraction finds <12 working filters) — falls back to OpenGameArt / itch.io free packs or AI-generated sprites — Phase 4 planner negotiates at Wave 0 time.

</deferred>

---

*Phase: 04-filter-catalog-picker-face-filter-mode*
*Context gathered: 2026-04-20*
