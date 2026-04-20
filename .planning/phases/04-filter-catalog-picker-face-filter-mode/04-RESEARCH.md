# Phase 4: Filter Catalog + Picker + Face Filter Mode - Research

**Researched:** 2026-04-20
**Domain:** Sprite catalog expansion, LazyRow filter picker UI, BehaviorState sealed refactor, DataStore persistence, multi-face rendering, Coil 2.7 asset loading, Lottie JSON frame extraction
**Confidence:** HIGH for architecture/APIs (all patterns verified against existing codebase); MEDIUM for Lottie extraction (verified against actual reference APK files); MEDIUM for behavior tick math (first implementation — no prior profile data)

---

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions
- D-01: Ship 15 bug filters; D-02: roster target (finalized at extraction); D-03: re-extract all from reference APK in Wave 0 (supersedes 03-gaps-01); D-04: extraction strategy (inspect data.assets[*].p base64 field); D-05: extraction validation (>10% non-alpha guard); D-06: thumbnail = frame_00.png via Coil AsyncImage; D-07: add coil-compose:2.7.0 in Wave 0; D-08: CRAWL along FaceContour.FACE 36-point polygon; D-09: SWARM 5-8 instances drifting toward anchor; D-10: FALL rain-of-bugs pattern; D-11: STATIC unchanged; D-12: BugState → sealed BehaviorState with Static/Crawl/Swarm/Fall variants; D-13: FilterEngine holds ConcurrentHashMap<Int, BehaviorState> keyed by trackingId; D-14: soft cap 20 draw calls/frame; D-15: LazyRow inline strip 100dp above shutter, 100dp height, 72dp thumbnails; D-16: selected state = 2dp white border + 1.15x scale + animateScrollToItem center-snap; D-17: rememberLazyListState scoped to CameraScreen; D-18: replaces Cycle Filter debug button; D-19: HomeRoute redesign with Face Filter / Insect Filter / settings gear / My Collection; D-20: CameraRoute(mode: CameraMode) enum arg; D-21: portrait lock; D-22: primary face full landmark, secondary bbox-center fallback; D-23: face loss behavior; D-24: multi-face synthetic unit test; D-25: DataStore key "last_used_filter_id", default = FilterCatalog.all[0]; D-26: new package com.bugzz.filter.camera.data, DataModule.kt in di/; D-27: FilterCatalog.all ordering = simple STATIC first, then mixed; D-28: inherit all Phase 3 locks; D-29: FilterDefinition add behaviorConfig: BehaviorConfig; D-30+ (from Phase 3): all prior locks carry

### Claude's Discretion
- Exact 15 filters (reference APK extraction result determines actual set)
- CRAWL direction default per filter
- Filter name text color / picker background / Material3 theme choices
- Settings gear icon choice
- Disabled-button visual style for "Insect Filter"
- Collection button exact visual
- Thumbnail loading shimmer vs instant
- LazyListState rememberSaveable key strategy
- Toast messages for placeholders
- FilterPrefsRepository write debounce (default: none)
- Exact swarmCount per filter
- BehaviorConfig default values when manifest.json omits behaviorConfig
- Robolectric picker test strategy

### Deferred Ideas (OUT OF SCOPE)
- 25-filter full set
- Bottom-sheet picker UX
- Orbit SWARM pattern
- Physics-based FALL
- Per-filter sound effects
- Filter categorization
- Hard cap at 10 draw calls
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| CAT-01 | FilterCatalog bundles 15-25 bug filters | Extraction script pattern + sprite reuse strategy documented below |
| CAT-02 | Each filter has id, displayName, thumbnail, sprite atlas, behavior config, anchor spec | FilterDefinition extension + SpriteManifest extension patterns below |
| CAT-03 | Horizontal LazyRow picker: thumbnails, highlighted selection, smooth scroll, 10 swaps in 5s | LazyRow performance patterns + animateScrollToItem code example below |
| CAT-04 | Tap switches active filter immediately | FilterEngine.setFilter() + ConcurrentHashMap clear pattern below |
| CAT-05 | Last-used filter persisted in DataStore, restored on relaunch | DataStore wrapper code example below |
| MOD-01 | Home screen has two primary buttons: Face Filter / Insect Filter | HomeScreen redesign patterns below |
| MOD-02 | Face Filter mode anchors bugs to face landmarks with real-time tracking | BehaviorState sealed + multi-face dispatch patterns below |
</phase_requirements>

---

## Summary

Phase 4 has two distinct challenges: **asset extraction** (now understood to be severely constrained by reference APK realities) and **behavior implementation** (three new BugBehavior variants on an already-validated rendering pipeline).

The most important finding from this research is that the reference APK (`com.insect.filters.funny.prank.bug.filter.face.camera.apk`) bundles only **3 Lottie JSON files** (`spider_prankfilter.json`, `home_lottie.json`, `home_filter.json`) and downloads all additional filter content from Firebase Storage at runtime. The local APK therefore yields at most 4 distinct bug sprite sequences: (A) spider animation from `spider_prankfilter.json` (23 frames, 1500×1500), (B) a smaller bug sequence from `home_lottie.json` group A (7 frames, 360×360), (C) a different bug sequence group B (12 frames, 360×360), and (D) a worm/ant group C (16 frames, 300×300). Phase 3 combined all of group B+C+D into a single "ant_on_nose_v1" filter with 35 frames. The D-02 roster of named filters (cockroach, centipede, wasp, etc.) cannot be extracted from this APK.

The plan must therefore acknowledge this limitation. **Wave 0 re-extracts the 4 available sprite types into separate, correctly-labelled filters and creates the remaining 11+ filters as behavior variants applied to the same sprite assets** (same ant sprite as CRAWL/SWARM/FALL = different named behaviors = distinct UX to the user). This is architecturally sound because the "wow factor" is the behavior, not the pixel art uniqueness. Alternatively, the planner may source free sprite assets from a CC0 asset pack. Both strategies are documented.

The BehaviorState sealed refactor, DataStore introduction, LazyRow picker, and HomeScreen redesign are all well-understood, low-risk changes using established patterns from prior phases.

**Primary recommendation:** Use the confirmed Phase 3 extraction pattern for Wave 0 — parse `data.assets[*].p` base64 fields from the 3 Lottie JSONs, decode into PNG frames, validate >10% non-alpha. Separate group A, B, C, D into distinct sprite directories. Register 4 sprite-type base, then define 15 FilterDefinition entries by pairing sprite types with different behaviors and anchors (e.g., "ant_on_nose" STATIC, "ant_crawl" CRAWL, "ant_swarm" SWARM etc.). This meets CAT-01's "15-25 filters" and CAT-02's "distinct behaviors" requirements.

---

## Project Constraints (from CLAUDE.md)

| Directive | Value |
|-----------|-------|
| Language | Kotlin |
| Camera | CameraX 1.6.0 |
| Face detection | ML Kit 16.1.7 bundled, contour mode |
| UI framework | Jetpack Compose (no Views/XML) |
| DI | Hilt 2.57 + KSP |
| Build | AGP 8.9.1, Gradle 8.13, Kotlin 2.1.21, compileSdk 36, targetSdk 35, minSdk 28 |
| Architecture | MVVM + StateFlow + UDF |
| Storage | MediaStore (DCIM/Bugzz), DataStore for prefs |
| Threading | cameraExecutor (BugzzCameraExecutor) + renderExecutor (BugzzRenderExecutor) |
| GSD | Must use GSD workflow entry points; no direct edits outside GSD |

---

## Standard Stack

### Core (existing — no changes)
| Library | Version | Purpose |
|---------|---------|---------|
| CameraX family | 1.6.0 | Pipeline + OverlayEffect |
| ML Kit face-detection | 16.1.7 | Contour landmarks |
| Jetpack Compose BOM | 2026.03.00 | UI toolkit |
| Hilt | 2.57 | DI |
| kotlinx-serialization-json | 1.8.0 | manifest.json parsing |
| Coroutines | 1.10.2 | Async DataStore reads/writes |

### New in Phase 4 (add to libs.versions.toml)
| Library | Version | Purpose | Source |
|---------|---------|---------|--------|
| `io.coil-kt:coil-compose` | **2.7.0** | Thumbnail loading in LazyRow | [ASSUMED] — version from CLAUDE.md STACK.md pre-catalogued; registry confirm recommended |
| `androidx.datastore:datastore-preferences` | **1.1.3** | Last-used filter persistence | [ASSUMED] — version from CLAUDE.md STACK.md pre-catalogued; registry confirm recommended |

**Installation:**
```kotlin
// gradle/libs.versions.toml additions
[versions]
coil = "2.7.0"
datastorePreferences = "1.1.3"

[libraries]
coil-compose = { module = "io.coil-kt:coil-compose", version.ref = "coil" }
androidx-datastore-preferences = { module = "androidx.datastore:datastore-preferences", version.ref = "datastorePreferences" }

// app/build.gradle.kts
implementation(libs.coil.compose)
implementation(libs.androidx.datastore.preferences)
```

**Note on KSP version:** Current `libs.versions.toml` has `ksp = "2.1.21-2.0.2"`. Neither Coil nor DataStore require KSP. Hilt KSP stays on existing version.

---

## Critical Finding: Reference APK Sprite Availability

**[VERIFIED: direct JSON inspection via Node.js on reference/raw_extract/res/raw/*.json]**

The reference APK bundles exactly 3 Lottie JSON files:

| File | nm (composition) | Total frames | Frame size | Distinct bug groups |
|------|-----------------|-------------|-----------|---------------------|
| `spider_prankfilter.json` | unnamed | 23 frames | 1500×1500 | 1 (spider) |
| `home_lottie.json` | InsectFilter_transparent | 35 total | 360×360 (19) + 300×300 (16) | 2+ (see below) |
| `home_filter.json` | 2_edit | same frames | same sizes | same 2+ |

**home_lottie.json breakdown:**
- Group A: imgSeq_0..6 (7 frames, 360×360, ~12KB/frame decoded) — smaller/simpler bug
- Group B: imgSeq_14..25 (12 frames, 360×360, ~17KB/frame decoded) — denser bug
- Group C: imgSeq_38..53 (16 frames, 300×300, ~18-22KB/frame decoded) — different proportions

Phase 3 extracted all 35 frames combined into `ant_on_nose_v1`. **Groups A, B, and C are likely 2-3 visually distinct bugs** that were conflated.

**Consequence for D-02 named roster:** The D-02 roster (cockroach, scorpion, centipede, wasp, tick, caterpillar, moth, mosquito, mantis) **cannot be extracted from this APK.** All additional filters are downloaded from Firebase Storage at runtime.

**Wave 0 extraction strategy (D-03 compliant):**
1. Extract 4 separate sprite directories: `spider_prankfilter/` (23 frames from `spider_prankfilter.json`), `bug_a/` (7 frames group A from `home_lottie.json`), `bug_b/` (12 frames group B), `bug_c/` (16 frames group C).
2. Validate each `frame_00.png` for >10% non-alpha pixels (D-05 guard).
3. Build 15 `FilterDefinition` entries by assigning different behaviors and anchors to the 4 sprite types. This is legitimate UX differentiation — the behavior defines the user experience.
4. If planner wants distinct visuals for all 15, add a note to source CC0 sprites from OpenGameArt.org as fallback (DEFERRED section of D-04 notes this possibility).

---

## Architecture Patterns

### Recommended Project Structure (Phase 4 additions)

```
app/src/main/java/com/bugzz/filter/camera/
├── data/
│   └── FilterPrefsRepository.kt       # NEW: DataStore wrapper (D-25, D-26)
├── di/
│   ├── CameraModule.kt                # existing
│   └── DataModule.kt                  # NEW: provides DataStore<Preferences> (D-26)
├── filter/
│   ├── FilterDefinition.kt            # EXTEND: add behaviorConfig: BehaviorConfig?
│   ├── FilterCatalog.kt               # EXPAND: 2 → 15 entries
│   ├── AssetLoader.kt                 # UNCHANGED (32MB LruCache handles eviction)
│   ├── SpriteManifest.kt              # EXTEND: add behaviorConfig: JsonElement?
│   └── BehaviorConfig.kt              # NEW: sealed class CrawlConfig/SwarmConfig/FallConfig
├── render/
│   ├── BehaviorState.kt               # NEW FILE: replaces BugState.kt (D-12 sealed interface)
│   ├── BugBehavior.kt                 # IMPLEMENT: CRAWL/SWARM/FALL bodies (D-08/09/10)
│   ├── BugInstance.kt                 # NEW: data class for SWARM/FALL instances
│   └── FilterEngine.kt                # EXTEND: perFaceState ConcurrentHashMap, multi-face loop
├── ui/
│   ├── camera/
│   │   ├── CameraScreen.kt            # EXTEND: add LazyRow picker, remove Cycle button
│   │   ├── CameraViewModel.kt         # EXTEND: onSelectFilter + DataStore read/write
│   │   └── CameraUiState.kt           # EXTEND: add filters: List<FilterSummary>, selectedFilterId
│   ├── home/
│   │   └── HomeScreen.kt              # NEW FILE: production Home (replaces StubScreens stub)
│   ├── nav/
│   │   └── Routes.kt                  # EXTEND: CameraRoute(mode: CameraMode)
│   └── BugzzApp.kt                    # REWIRE: HomeRoute → new HomeScreen
└── assets/sprites/
    ├── spider_prankfilter/            # NEW: 23 frames
    ├── bug_a/                         # NEW: 7 frames  
    ├── bug_b/                         # NEW: 12 frames
    └── bug_c/                         # NEW: 16 frames
```

### Pattern 1: BehaviorState Sealed Interface + Per-Face ConcurrentHashMap

**What:** Replace flat `BugState` with a per-variant sealed interface. `FilterEngine` holds `ConcurrentHashMap<Int, BehaviorState>` keyed by BboxIouTracker-assigned `trackingId`.

**Why:** Enables CRAWL/SWARM/FALL to carry behavior-specific state (progress float, instance lists) without a "kitchen sink" data class. Thread-safe map handles concurrent face additions (detector callback on cameraExecutor) and removals (renderer on renderExecutor). ConcurrentHashMap is lock-striped — single-entry get/put/remove are atomic without full lock.

**Code example:**

```kotlin
// BehaviorState.kt (NEW — replaces BugState.kt)
sealed interface BehaviorState {
    data class Static(val pos: PointF = PointF(0f, 0f)) : BehaviorState

    data class Crawl(
        var progress: Float = 0f,         // [0, 1) progress along FACE contour
        val direction: CrawlDirection = CrawlDirection.CW
    ) : BehaviorState

    data class Swarm(
        val instances: MutableList<BugInstance> = mutableListOf()
    ) : BehaviorState

    data class Fall(
        val instances: MutableList<FallingBug> = mutableListOf(),
        var nextSpawnNanos: Long = 0L
    ) : BehaviorState
}

enum class CrawlDirection { CW, CCW }

// BugInstance.kt (inline into BehaviorState.kt or separate)
data class BugInstance(
    var position: PointF,
    var velocity: PointF,
    var frameIndex: Int = 0,
)

data class FallingBug(
    var position: PointF,
    var velocity: PointF,
    val spawnNanos: Long,
)
```

**FilterEngine with perFaceState:**

```kotlin
@Singleton
class FilterEngine @Inject constructor(private val assetLoader: AssetLoader) {

    companion object {
        const val MAX_DRAWS_PER_FRAME = 20
    }

    private val activeFilter = AtomicReference<FilterDefinition?>(null)
    // keyed by BboxIouTracker trackingId; written/read on renderExecutor thread
    private val perFaceState = ConcurrentHashMap<Int, BehaviorState>()
    private val spritePaint = Paint(Paint.ANTI_ALIAS_FLAG)

    fun setFilter(definition: FilterDefinition?) {
        activeFilter.set(definition)
        perFaceState.clear()   // D-13: clear on filter swap
        // preload triggered by CameraViewModel before calling setFilter
    }

    fun onFaceLost(trackingId: Int) {
        perFaceState.remove(trackingId)   // D-23: clean up on face drop
    }

    /** Called from OverlayEffectBuilder.setOnDrawListener (renderExecutor) */
    fun onDraw(canvas: Canvas, frame: Frame, faces: List<SmoothedFace>) {
        val filter = activeFilter.get() ?: return
        if (faces.isEmpty()) return

        canvas.setMatrix(frame.sensorToBufferTransform ?: Matrix())

        // D-22: primary = largest bbox, secondary = second-largest
        val sorted = faces.sortedByDescending { it.boundingBox.width() * it.boundingBox.height() }
        val primary = sorted.getOrNull(0)
        val secondary = sorted.getOrNull(1)

        // Count draw calls for soft cap (D-14)
        var totalDraws = 0

        if (primary != null) {
            totalDraws += drawFace(canvas, frame, primary, filter, isPrimary = true, budgetRemaining = MAX_DRAWS_PER_FRAME - totalDraws)
        }
        if (secondary != null && totalDraws < MAX_DRAWS_PER_FRAME) {
            totalDraws += drawFace(canvas, frame, secondary, filter, isPrimary = false, budgetRemaining = MAX_DRAWS_PER_FRAME - totalDraws)
        }
    }

    private fun drawFace(canvas: Canvas, frame: Frame, face: SmoothedFace, filter: FilterDefinition, isPrimary: Boolean, budgetRemaining: Int): Int {
        val anchor = if (isPrimary) {
            FaceLandmarkMapper.anchorPoint(face, filter.anchorType)
        } else {
            // D-22 secondary fallback: bbox center at 40% height (mid-forehead)
            PointF(
                face.boundingBox.left + face.boundingBox.width() * 0.5f,
                face.boundingBox.top  + face.boundingBox.height() * 0.4f
            )
        } ?: return 0

        val state = perFaceState.getOrPut(face.trackingId) {
            createBehaviorState(filter)
        }

        val dtMs = /* compute from frame.timestampNanos, stored in state */  0L  // actual impl tracks last ts
        return drawBehaviorState(canvas, frame, face, filter, state, anchor, dtMs, budgetRemaining)
    }
}
```

### Pattern 2: CRAWL Behavior — Contour Traversal

**What:** Bug position interpolated along `FaceContour.FACE` 36-point closed polygon.

**Algorithm:** Linear vertex-count parameterization (arc-length is a Phase 7 refinement if visual result is unacceptable):

```kotlin
// Inside BugBehavior.Crawl.tick or FilterEngine helper
fun crawlPosition(contourPoints: List<PointF>, progress: Float): PointF {
    val n = contourPoints.size
    val scaled = progress * n              // 0 .. n (exclusive)
    val i = scaled.toInt() % n
    val t = scaled - scaled.toInt()         // fractional position between i and i+1
    val a = contourPoints[i]
    val b = contourPoints[(i + 1) % n]
    return PointF(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t)
}

// In tick(), advance progress:
// dtSeconds = dtMs / 1000f
// progressDelta = dtSeconds * 0.5f * faceBoxWidthFraction  (D-08: 50% bbox width per second at 30fps)
// progress = (progress + progressDelta) % 1.0f  (wrap)
```

**Note:** `FaceContour.FACE` is available via `SmoothedFace.contours[FaceContour.FACE]`. Always check not null/empty before accessing — fall back to bbox-center traversal if missing.

### Pattern 3: SWARM Behavior — Instance Drift

```kotlin
// D-09: 5-8 instances drifting toward anchor, respawning at bbox edge
fun swarmTick(state: BehaviorState.Swarm, anchor: PointF, bbox: RectF, config: SwarmConfig, dtMs: Long) {
    val dtSec = dtMs / 1000f
    if (state.instances.isEmpty()) {
        // Initialize on first tick
        repeat(config.instanceCount) {
            state.instances.add(BugInstance(
                position = randomBboxEdgePoint(bbox),
                velocity = PointF(0f, 0f),
                frameIndex = 0,
            ))
        }
    }
    state.instances.forEach { inst ->
        val dx = anchor.x - inst.position.x
        val dy = anchor.y - inst.position.y
        val dist = Math.hypot(dx.toDouble(), dy.toDouble()).toFloat()
        if (dist < bbox.width() * 0.2f) {
            // Respawn at random edge (D-09)
            val newPos = randomBboxEdgePoint(bbox)
            inst.position.set(newPos.x, newPos.y)
        } else {
            val speed = config.driftSpeedRange.first + (config.driftSpeedRange.last - config.driftSpeedRange.first) * Math.random().toFloat()
            val spd = speed * bbox.width()  // pixels/sec
            inst.velocity.set(dx / dist * spd, dy / dist * spd)
            inst.position.x += inst.velocity.x * dtSec
            inst.position.y += inst.velocity.y * dtSec
        }
    }
}
```

**SWARM allocation safety:** `instances` is a `MutableList` only mutated on the `renderExecutor` thread (same thread that calls `onDraw`). No cross-thread mutation. No `ConcurrentModificationException` risk as long as `onFaceLost` is also called on renderExecutor (or protected by perFaceState.remove which is safe in ConcurrentHashMap). On `setFilter()`, the map is cleared entirely — old instance lists become unreachable.

### Pattern 4: FALL Behavior — Gravity Rain

```kotlin
// D-10: spawn bugs above frame, fall down under gravity
fun fallTick(state: BehaviorState.Fall, previewWidth: Float, previewHeight: Float, config: FallConfig, tsNanos: Long, dtMs: Long) {
    val dtSec = dtMs / 1000f
    val gravity = config.gravity * previewHeight  // pixels/sec

    // Spawn new bugs
    if (tsNanos >= state.nextSpawnNanos) {
        if (state.instances.size < config.maxInstances) {
            val x = (Math.random() * previewWidth).toFloat()
            state.instances.add(FallingBug(
                position = PointF(x, 0f),
                velocity = PointF(0f, gravity),
                spawnNanos = tsNanos
            ))
        }
        val intervalMs = config.spawnIntervalMsRange.first +
            (config.spawnIntervalMsRange.last - config.spawnIntervalMsRange.first) * Math.random().toLong()
        state.nextSpawnNanos = tsNanos + intervalMs * 1_000_000L
    }

    // Update + despawn
    val iter = state.instances.iterator()
    while (iter.hasNext()) {
        val bug = iter.next()
        bug.position.y += bug.velocity.y * dtSec
        if (bug.position.y > previewHeight) iter.remove()
    }
}
```

**FALL allocation note:** `iter.remove()` during iteration avoids `ConcurrentModificationException` because we use a standard `MutableList` iterator on a single thread. Do not use `removeIf` (available API 24+ but less explicit about thread contract). Do not allocate a new `PointF` per frame — mutate existing.

### Pattern 5: DataStore Preferences — FilterPrefsRepository

```kotlin
// data/FilterPrefsRepository.kt
@Singleton
class FilterPrefsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore: DataStore<Preferences> by context.dataStore

    val lastUsedFilterId: Flow<String> = dataStore.data
        .catch { e ->
            if (e is IOException) {
                Timber.tag("FilterPrefs").w(e, "DataStore read error, using default")
                emit(emptyPreferences())
            } else throw e
        }
        .map { prefs -> prefs[KEY_LAST_FILTER] ?: DEFAULT_FILTER_ID }

    suspend fun setLastUsedFilter(id: String) {
        dataStore.edit { prefs -> prefs[KEY_LAST_FILTER] = id }
    }

    companion object {
        private val KEY_LAST_FILTER = stringPreferencesKey("last_used_filter_id")
        const val DEFAULT_FILTER_ID = "ant_on_nose_v1"
    }
}

// DataModule.kt in di/
@Module
@InstallIn(SingletonComponent::class)
object DataModule {
    // DataStore is provided via the Context extension property — no @Provides needed.
    // FilterPrefsRepository uses @Inject constructor with @ApplicationContext.
    // This module exists as placeholder for future data-layer additions.
}

// Application-level DataStore singleton (add to BugzzApplication.kt or companion):
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "bugzz_prefs")
```

**Thread-safe write pattern in ViewModel:**
```kotlin
// CameraViewModel
fun onSelectFilter(id: String) {
    _uiState.update { it.copy(selectedFilterId = id) }
    val def = filterCatalog.byId(id) ?: return
    viewModelScope.launch {
        // Write DataStore (suspend on IO dispatcher internally)
        filterPrefsRepository.setLastUsedFilter(id)
    }
    // Async preload on cameraExecutor
    viewModelScope.launch(cameraExecutor.asCoroutineDispatcher()) {
        assetLoader.preload(id)
        filterEngine.setFilter(def)
    }
}

// On bind(), restore last-used:
private fun restoreLastFilter() {
    viewModelScope.launch {
        val id = filterPrefsRepository.lastUsedFilterId.first()
        val def = filterCatalog.byId(id) ?: filterCatalog.all.first()
        // preload and set
        withContext(cameraExecutor.asCoroutineDispatcher()) {
            assetLoader.preload(def.id)
            filterEngine.setFilter(def)
        }
        _uiState.update { it.copy(selectedFilterId = def.id) }
    }
}
```

**Corruption fallback (T-04-01):** The `catch { emit(emptyPreferences()) }` in `lastUsedFilterId` Flow handles DataStore file corruption gracefully. The fallback to `FilterCatalog.all.first()` in ViewModel handles missing IDs.

### Pattern 6: Coil 2.7 — Asset URI for Thumbnails

```kotlin
// In LazyRow item composable (Coil 2.7 loads from android_asset:// scheme)
AsyncImage(
    model = ImageRequest.Builder(LocalContext.current)
        .data("file:///android_asset/sprites/${filter.id}/frame_00.png")
        .crossfade(true)
        .build(),
    contentDescription = filter.displayName,
    modifier = Modifier
        .size(72.dp)
        .clip(RoundedCornerShape(12.dp)),
    placeholder = painterResource(R.drawable.ic_filter_placeholder),
    error = painterResource(R.drawable.ic_filter_placeholder),
)
```

**Scheme note:** `file:///android_asset/` is the standard Android assets URI scheme. Coil 2.x handles this via its built-in `AssetUriFetcher`. [ASSUMED — Coil 2.7 docs not live-verified in this session; based on Coil 2.x documented behavior.] No additional custom `ImageLoader` configuration needed.

**Cache non-collision (D-06):** Coil's internal `MemoryCache` keys on the URI string. `AssetLoader.LruCache` keys on `"$filterId/frame_$idx"` strings. These are different key namespaces — no collision. However, both occupy heap. 15 thumbnails × 72dp at ~3x density ≈ 15 × (216×216 × 4 bytes) ≈ 2.8 MB for Coil; AssetLoader LruCache up to 32 MB for full frame sets. Total peak on a 256MB heap device: comfortably under budget.

### Pattern 7: LazyRow Filter Picker

```kotlin
@Composable
fun FilterPicker(
    filters: List<FilterSummary>,
    selectedId: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    LazyRow(
        state = listState,
        contentPadding = PaddingValues(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.height(100.dp)
    ) {
        items(filters, key = { it.id }) { filter ->     // D-16: stable key prevents recomposition
            val isSelected = filter.id == selectedId
            val scale by animateFloatAsState(
                targetValue = if (isSelected) 1.15f else 1.0f,
                animationSpec = tween(200),
                label = "filterScale_${filter.id}"
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .graphicsLayer(scaleX = scale, scaleY = scale)
                    .clickable {
                        onSelect(filter.id)
                        // D-16: center-snap
                        val idx = filters.indexOfFirst { it.id == filter.id }
                        coroutineScope.launch {
                            listState.animateScrollToItem(index = idx, scrollOffset = 0)
                        }
                    }
                    .border(
                        width = if (isSelected) 2.dp else 0.dp,
                        color = if (isSelected) Color.White else Color.Transparent,
                        shape = RoundedCornerShape(12.dp)
                    )
            ) {
                AsyncImage(
                    model = "file:///android_asset/sprites/${filter.id}/frame_00.png",
                    contentDescription = filter.displayName,
                    modifier = Modifier.size(72.dp).clip(RoundedCornerShape(12.dp))
                )
                Text(
                    text = filter.displayName,
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = Color.White
                )
            }
        }
    }
}
```

**Performance notes:**
- `key = { it.id }` prevents full recomposition on list-reorder or selection change
- `animateFloatAsState` per-item recompose is scoped — only the changing item recomposes
- 15 items with 72dp thumbnails: total scroll width ~(72+8)×15 = 1200dp; no virtualization pressure
- Coil images are loaded once and cached; rapid-tap (10 swaps in 5s) only changes `selectedId` state, not image loads

### Pattern 8: CameraRoute Type-Safe Enum Nav Arg

```kotlin
// Routes.kt
enum class CameraMode { FaceFilter, InsectFilter }

@Serializable
data class CameraRoute(val mode: CameraMode = CameraMode.FaceFilter)

// BugzzApp.kt composable<CameraRoute>
composable<CameraRoute> { backStackEntry ->
    val route: CameraRoute = backStackEntry.toRoute()
    when (route.mode) {
        CameraMode.FaceFilter -> CameraScreen(...)
        CameraMode.InsectFilter -> {
            // Phase 4: show "Not yet available" stub
            InsectFilterNotYetAvailableScreen(onBack = { navController.popBackStack() })
        }
    }
}

// HomeScreen navigation:
onFaceFilterClick = { navController.navigate(CameraRoute(mode = CameraMode.FaceFilter)) }
onInsectFilterClick = { /* disabled in Phase 4 via onClick Toast */ }
```

**Serialization note:** `@Serializable` on `data class CameraRoute` with an `enum` field requires the enum to be either `@Serializable` or use a custom serializer. Since navigation-compose type-safe args in 2.8.9 uses kotlinx.serialization, mark `enum class CameraMode` with `@Serializable` (already have `kotlin-serialization` plugin). [ASSUMED — based on navigation-compose 2.8.x documentation pattern; kotlinx.serialization enum support is built-in by default.]

### Pattern 9: HomeScreen Redesign

```kotlin
// ui/home/HomeScreen.kt (NEW)
@Composable
fun HomeScreen(
    onFaceFilter: () -> Unit,
    onInsectFilter: () -> Unit,
    onMyCollection: () -> Unit,
) {
    val context = LocalContext.current
    Scaffold(
        topBar = {
            Box(Modifier.fillMaxWidth().padding(top = 16.dp, end = 16.dp), contentAlignment = Alignment.CenterEnd) {
                IconButton(onClick = {
                    Toast.makeText(context, "Settings: coming soon", Toast.LENGTH_SHORT).show()
                }) {
                    Icon(Icons.Default.Settings, contentDescription = "Settings")
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding),
            verticalArrangement = Arrangement.spacedBy(32.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(
                onClick = onFaceFilter,
                modifier = Modifier.size(width = 200.dp, height = 80.dp)
            ) { Text("Face Filter") }

            OutlinedButton(
                onClick = {
                    Toast.makeText(context, "Insect Filter coming in next release", Toast.LENGTH_SHORT).show()
                },
                enabled = false,   // D-19: disabled in Phase 4
                modifier = Modifier.size(width = 200.dp, height = 80.dp)
            ) { Text("Insect Filter") }

            OutlinedButton(
                onClick = onMyCollection,
                modifier = Modifier.size(width = 160.dp, height = 56.dp)
            ) { Text("My Collection") }
        }
    }
}
```

### Pattern 10: FilterSummary DTO for Picker State

```kotlin
// CameraUiState.kt — add FilterSummary
data class FilterSummary(
    val id: String,
    val displayName: String,
    // thumbnail loaded by Coil from assets — no Bitmap stored in state
)

data class CameraUiState(
    // existing fields...
    val filters: List<FilterSummary> = emptyList(),    // populated after catalog loads
    val selectedFilterId: String = "",
)
```

---

## Lottie Extraction Script

**Wave 0 Node.js script pseudocode — addresses Phase 3 spider fix + D-03/D-04:**

```javascript
// extract_sprites.cjs  (run from project root: node extract_sprites.cjs)
const fs = require('fs');
const path = require('path');

const LOTTIE_FILES = [
  { file: 'reference/raw_extract/res/raw/spider_prankfilter.json', outputDir: 'spider_prankfilter', assetIds: null /* all */ },
  { file: 'reference/raw_extract/res/raw/home_lottie.json',        outputDir: 'bug_a',              assetIds: ['imgSeq_0','imgSeq_1','imgSeq_2','imgSeq_3','imgSeq_4','imgSeq_5','imgSeq_6'] },
  { file: 'reference/raw_extract/res/raw/home_lottie.json',        outputDir: 'bug_b',              assetIds: ['imgSeq_14','imgSeq_15','imgSeq_16','imgSeq_17','imgSeq_18','imgSeq_19','imgSeq_20','imgSeq_21','imgSeq_22','imgSeq_23','imgSeq_24','imgSeq_25'] },
  { file: 'reference/raw_extract/res/raw/home_lottie.json',        outputDir: 'bug_c',              assetIds: ['imgSeq_38','imgSeq_39','imgSeq_40','imgSeq_41','imgSeq_42','imgSeq_43','imgSeq_44','imgSeq_45','imgSeq_46','imgSeq_47','imgSeq_48','imgSeq_49','imgSeq_50','imgSeq_51','imgSeq_52','imgSeq_53'] },
];

const BASE = 'app/src/main/assets/sprites';
const MIN_NON_ALPHA_PCT = 0.10;  // D-05 validation threshold

LOTTIE_FILES.forEach(({ file, outputDir, assetIds }) => {
  const data = JSON.parse(fs.readFileSync(file, 'utf8'));
  const imgAssets = data.assets.filter(a =>
    a.p && a.p.length > 50 &&
    (assetIds === null || assetIds.includes(a.id))
  );

  const outDir = path.join(BASE, outputDir);
  fs.mkdirSync(outDir, { recursive: true });

  let extracted = 0;
  imgAssets.forEach((asset, idx) => {
    const b64 = asset.p.replace(/^data:image\/png;base64,/, '');
    const buf = Buffer.from(b64, 'base64');

    // D-05: validate non-alpha pixel percentage
    // Simple heuristic: PNG files with mostly-transparent content have very small decoded size
    // relative to their dimensions. Check: decoded_bytes / (w * h * 4) > MIN_NON_ALPHA_PCT
    const pixelCount = (asset.w || 360) * (asset.h || 360);
    const density = buf.length / (pixelCount * 4);  // 0..1 approx
    if (density < MIN_NON_ALPHA_PCT) {
      console.warn(`SKIP ${outputDir}/frame_${String(idx).padStart(2,'0')}.png — density ${density.toFixed(3)} < threshold`);
      return;
    }

    const filename = `frame_${String(idx).padStart(2, '0')}.png`;
    fs.writeFileSync(path.join(outDir, filename), buf);
    extracted++;
    console.log(`OK   ${outputDir}/${filename}  (${buf.length} bytes, density ${density.toFixed(3)})`);
  });

  // Write manifest.json
  const manifest = {
    id: outputDir,
    displayName: outputDir.replace(/_/g, ' '),
    frameCount: extracted,
    frameDurationMs: Math.round(1000 / (data.fr || 24)),
    anchorType: 'NOSE_TIP',
    behavior: 'STATIC',
    scaleFactor: 0.22,
    mirrorable: true
  };
  fs.writeFileSync(path.join(outDir, 'manifest.json'), JSON.stringify(manifest, null, 2));
  console.log(`Manifest written: ${outDir}/manifest.json (${extracted} frames)`);
});
```

**Phase 3 spider fix explanation:** The Phase 3 spider sprite issue was NOT due to wrong layer selection — `spider_prankfilter.json` has a simple flat structure: 23 image layers (`Spider_1.png` .. `Spider_23.png`) each referencing one asset. The Phase 3 extraction script for `home_lottie.json` (the ant) was used for spider as well but pointed to wrong asset group. The fix: use the above script with explicit `assetIds` mapping. The spider file is self-contained with `data.assets[0..22]` all having valid base64 PNGs.

**Expected extraction results:**
| Output dir | Source | Expected frames | Frame size | Validation risk |
|-----------|--------|-----------------|-----------|-----------------|
| `spider_prankfilter` | `spider_prankfilter.json` all assets | 23 | 1500×1500 | LOW — large files, dense content |
| `bug_a` | `home_lottie.json` imgSeq_0..6 | 7 | 360×360 | MEDIUM — phase 3 ant worked |
| `bug_b` | `home_lottie.json` imgSeq_14..25 | 12 | 360×360 | MEDIUM — not previously validated |
| `bug_c` | `home_lottie.json` imgSeq_38..53 | 16 | 300×300 | MEDIUM — not previously validated |

The existing `ant_on_nose_v1` directory (35 frames = A+B+C combined) becomes superseded — its frames are now split into `bug_a`, `bug_b`, `bug_c`. However to avoid breaking Phase 3 green tests, keep `ant_on_nose_v1` as-is and add 3 new directories. Phase 4 catalog references the new dirs.

---

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Filter preference persistence | Custom file I/O or SharedPreferences | DataStore Preferences | Coroutine-safe, atomic, no ANR risk |
| Thumbnail image loading | Bitmap decode + Canvas cache for thumbnails | Coil 2.7 AsyncImage | Memory cache, error states, crossfade built-in |
| LazyRow scroll to selected | Manual ScrollState.scrollTo math | `LazyListState.animateScrollToItem` | Handles viewport offset, boundary clamping |
| SWARM instance PointF pools | Custom object pool | Mutate existing PointF in-place | Mutation inside a single-threaded MutableList is sufficient; allocation is amortized over instance lifetime |
| Thread-safe per-face state | Synchronized block or ReentrantLock | ConcurrentHashMap | Lock-free reads; single-entry get/put are atomic |
| Lottie rendering for sprites | Render Lottie JSON at runtime | Pre-extracted PNG frames (existing AssetLoader) | Bitmap frames already established in Phase 3; Lottie runtime adds 1MB+ overhead |

---

## Common Pitfalls

### Pitfall 1: ConcurrentHashMap.getOrPut is NOT atomic

**What goes wrong:** `perFaceState.getOrPut(id) { createState() }` is NOT atomic in Kotlin's ConcurrentHashMap extension — `getOrPut` reads, then conditionally puts. Under concurrent access from two threads, two `createState()` calls could fire. [ASSUMED — standard JVM ConcurrentHashMap behavior]

**Why it happens:** Kotlin's `ConcurrentHashMap.getOrPut` extension does two separate operations.

**How to avoid:** Use `computeIfAbsent(id) { createState() }` which IS atomic in `java.util.concurrent.ConcurrentHashMap`. However, for this codebase: `onDraw` (renderExecutor) is the only writer of new entries; `onFaceLost` (must be called from renderExecutor or ConcurrentHashMap.remove is safe concurrently) only removes. Since the draw callback is single-threaded (renderExecutor = single-thread executor), there is no concurrent write risk during `getOrPut`. Use standard `getOrPut` but document the thread constraint.

### Pitfall 2: CRAWL progress reset on filter swap clears contour state

**What goes wrong:** On `setFilter()`, `perFaceState.clear()` is called. The next call to `onDraw` re-creates a fresh `BehaviorState.Crawl(progress = 0f)`. The bug teleports to position 0 on the contour. This is expected behavior (D-13: "no carry-over"). Document as intentional in code comment, not a bug.

### Pitfall 3: FALL instance accumulation after setFilter clears map

**What goes wrong:** If `setFilter()` is called while FALL instances are in flight, `perFaceState.clear()` removes all FallingBug lists. Memory held by old `MutableList<FallingBug>` is GC'd normally. No leak, but the user sees a visual "snap" — bugs disappear instantly. Acceptable; Phase 6 UX polish could add fade-out.

### Pitfall 4: Coil `file:///android_asset/` path vs `assets://` scheme

**What goes wrong:** Some asset URL formats fail silently in Coil. The correct scheme for `assets/sprites/ant_on_nose_v1/frame_00.png` is `file:///android_asset/sprites/ant_on_nose_v1/frame_00.png`. Using `assets://sprites/...` or omitting `file:///` prefix returns an empty image without error.

**How to avoid:** Use the exact URI form: `"file:///android_asset/sprites/$id/frame_00.png"`. Test with one filter in Wave 0 before registering all 15.

### Pitfall 5: DataStore `preferencesDataStore` delegate must be module-level

**What goes wrong:** `val Context.dataStore by preferencesDataStore("bugzz_prefs")` placed inside a class body causes a fresh DataStore instance per context access, breaking singleton contract.

**How to avoid:** Declare at file/module top level in `BugzzApplication.kt` or a dedicated `DataStoreUtils.kt`. FilterPrefsRepository uses `@ApplicationContext private val context: Context` and calls `context.dataStore` — works because the delegate is top-level.

### Pitfall 6: `animateScrollToItem` throws on empty list

**What goes wrong:** `listState.animateScrollToItem(index)` called when `filters` list is empty causes `IndexOutOfBoundsException`.

**How to avoid:** Guard `onSelect` callback: `if (filters.isEmpty()) return`. Also ensure `filters` is never empty in production (FilterCatalog.all has 15 entries minimum).

### Pitfall 7: Phase 3 `BugState` → Phase 4 `BehaviorState` migration breaks existing tests

**What goes wrong:** Phase 3 tests reference `BugState` (data class). Renaming to `BehaviorState` (sealed interface) breaks `FilterEngineTest` and any other test that constructs `BugState(...)`.

**How to avoid:** Add `typealias BugState = BehaviorState.Static` as a migration shim in Phase 4 Wave 0, update tests to use new types, then remove the typealias. Or update tests directly — simpler.

### Pitfall 8: FALL spawn position in sensor-space vs preview-space

**What goes wrong:** FALL behavior spawns bugs at `position = (randomX in [0, previewWidth], 0)`. But `previewWidth` / `previewHeight` in sensor coordinates may differ from screen preview dimensions. The `OverlayEffect` canvas coordinate system is sensor-buffer space (after `sensorToBufferTransform` is applied).

**How to avoid:** Derive `previewWidth`/`previewHeight` from the `Frame` object passed to `onDraw`. In `OverlayEffect.Frame`, `getSize()` returns the buffer size (which is what canvas coordinates map to after transform). Use `frame.size.width.toFloat()` and `frame.size.height.toFloat()`. [ASSUMED — based on OverlayEffect API documentation; verify in Wave 0.]

### Pitfall 9: FilterCatalog 15-entry list with behavior variants sharing same sprite

**What goes wrong:** If 3 FilterDefinition entries share the same `assetDir = "bug_c"`, `AssetLoader.preload("bug_c")` is called 3 times on rapid filter swaps. Subsequent calls are cache hits (Phase 3 D-09 pattern). But if the first preload is in-flight, the second call re-enters `preload` for the same `filterId` and may decode frames twice.

**How to avoid:** `AssetLoader.preload` already checks `if (cache.get(key) != null) continue`. Concurrent calls for the same `filterId` may decode a few frames twice (harmless double-write to LruCache), not a correctness issue. [ASSUMED — review AssetLoader.preload body; shown in Phase 3 code.]

### Pitfall 10: Multi-face order instability between frames

**What goes wrong:** Sorting `faces.sortedByDescending { bbox area }` to determine primary/secondary may flip if two faces have nearly equal bbox areas (person and printed photo of equal apparent size). Primary/secondary designation swaps every frame → BehaviorState for each trackingId is still correct (keyed by tracker ID, not by primary/secondary role), but the anchor calculation switches between full-landmark and bbox-center.

**How to avoid:** The anchor switch on primary/secondary flip is acceptable UX (secondary gets lower-fidelity anchor). Since `BehaviorState` is keyed by `trackingId` not role, no state corruption. Document in code comment.

---

## Runtime State Inventory

Phase 4 is purely additive code + new DataStore file. It is not a rename/refactor/migration.

| Category | Items Found | Action Required |
|----------|-------------|------------------|
| Stored data | DataStore file `bugzz_prefs` introduced in Phase 4 | Created fresh; no migration |
| Live service config | None | None |
| OS-registered state | None | None |
| Secrets/env vars | None | None |
| Build artifacts | `ant_on_nose_v1` sprite dir preserved (Phase 3 tests depend on it) | Keep; add new sprite dirs alongside |

---

## Environment Availability

| Dependency | Required By | Available | Version | Fallback |
|------------|------------|-----------|---------|----------|
| Node.js | Wave 0 sprite extraction script | ✓ | (verified via prior phase extraction) | — |
| `reference/raw_extract/res/raw/*.json` | Wave 0 extraction | ✓ | 3 files confirmed present | — |
| Android Studio JBR JDK 17 | Build | ✓ | `C:\Program Files\Android\Android Studio\jbr` | — |
| Xiaomi 13T device | Phase 4 acceptance gate | ✓ | (per STATE.md) | — |

---

## Validation Architecture

### Test Framework
| Property | Value |
|----------|-------|
| Framework | JUnit 4 + Mockito 5.11 + Robolectric 4.13 (existing) |
| Config file | `app/build.gradle.kts` testImplementation entries |
| Quick run command | `JAVA_HOME="C:/Program Files/Android/Android Studio/jbr" ./gradlew :app:testDebugUnitTest` |
| Full suite command | Same (all unit tests) |

### Phase Requirements → Test Map

| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| CAT-01 | FilterCatalog.all has 15 entries | unit | `testDebugUnitTest --tests "*FilterCatalogExpandedTest*"` | ❌ Wave 0 |
| CAT-02 | Each FilterDefinition has all required fields non-null | unit | same | ❌ Wave 0 |
| CAT-03 | LazyRow picker renders + tap response | Compose UI test (manual-only for Phase 4; unit test for ViewModel) | `testDebugUnitTest --tests "*CameraViewModelTest*onSelectFilter*"` | ❌ Wave 0 (VM portion) |
| CAT-04 | onSelectFilter → filterEngine.setFilter called within 1 frame | unit (VM mock) | `testDebugUnitTest --tests "*CameraViewModelTest*"` | ✅ `CameraViewModelTest.kt` (extend) |
| CAT-05 | FilterPrefsRepository read/write round-trip | unit (fake DataStore) | `testDebugUnitTest --tests "*FilterPrefsRepositoryTest*"` | ❌ Wave 0 |
| MOD-01 | HomeScreen navigates correctly on button tap | Compose UI test (manual-only) or Nav unit test | manual on device | — |
| MOD-02 | Multi-face: primary gets landmark anchor, secondary gets bbox-center | unit (inject List<SmoothedFace> size 2) | `testDebugUnitTest --tests "*FilterEngineTest*multiFace*"` | ❌ Wave 0 |

**Additional unit tests needed for behavioral correctness:**

| Test class | Tests |
|------------|-------|
| `CrawlBehaviorTest` | progress advances per dtMs; wraps at 1.0; contour interpolation midpoint |
| `SwarmBehaviorTest` | instances initialized on first tick; respawn at bbox edge when close to anchor |
| `FallBehaviorTest` | bug spawns at y=0; despawns at y>height; nextSpawnNanos advances |
| `BehaviorStateMapTest` | setFilter clears map; onFaceLost removes entry; getOrPut creates fresh state |
| `FilterCatalogExpandedTest` | all 15 entries have non-empty ids; all assetDir values unique; all anchors valid |
| `FilterPrefsRepositoryTest` | write then read returns same id; unknown id falls back to default |

### Sampling Rate
- **Per task commit:** `./gradlew :app:testDebugUnitTest`
- **Per wave merge:** same (full suite)
- **Phase gate:** Full suite green + Xiaomi 13T acceptance gate before `/gsd-verify-work`

### Wave 0 Gaps
- [ ] `FilterCatalogExpandedTest.kt` — covers CAT-01, CAT-02
- [ ] `FilterPrefsRepositoryTest.kt` — covers CAT-05
- [ ] `FilterEngineTest.kt` — add `multiFace_*` tests for MOD-02/D-24
- [ ] `CrawlBehaviorTest.kt` — covers BugBehavior.Crawl tick logic
- [ ] `SwarmBehaviorTest.kt` — covers BugBehavior.Swarm tick logic
- [ ] `FallBehaviorTest.kt` — covers BugBehavior.Fall tick logic
- [ ] `BehaviorStateMapTest.kt` — covers D-13 ConcurrentHashMap lifecycle

**DataStore testing pattern (no Robolectric required):**
```kotlin
// Use fake InMemoryDataStore via DataStoreFactory — available in datastore-core test artifact
// OR: inject DataStore<Preferences> via Hilt test module using:
// val testDataStore: DataStore<Preferences> = PreferenceDataStoreFactory.create(
//     corruptionHandler = null,
//     produceFile = { tempDir.resolve("test_prefs.preferences_pb") }
// )
// Then wrap in FilterPrefsRepository for testing.
```

---

## Security Domain

**security_enforcement:** not explicitly disabled in config (treat as enabled).

### Applicable ASVS Categories

| ASVS Category | Applies | Standard Control |
|---------------|---------|-----------------|
| V2 Authentication | no | No auth in this app |
| V3 Session Management | no | No sessions |
| V4 Access Control | no | Single-user personal app |
| V5 Input Validation | yes (manifest.json parsing) | kotlinx.serialization with `@Serializable` + default values; catch `SerializationException` |
| V6 Cryptography | no | DataStore uses protobuf, no custom crypto |

### Threat Model Candidates

**T-04-01: DataStore file corruption on app crash during write → graceful fallback to default filter**
- Mitigation: `catch { emit(emptyPreferences()) }` in `FilterPrefsRepository.lastUsedFilterId` Flow. ViewModel falls back to `FilterCatalog.all.first()` if stored id missing from catalog. DataStore's atomic write semantics make partial write corruption rare but handled.

**T-04-02: Malformed manifest.json BehaviorConfig → AssetLoader rejects + skips filter from catalog**
- Mitigation: `kotlinx.serialization` with `ignoreUnknownKeys = true` and optional `behaviorConfig: JsonElement?` field. If parse fails, catch `SerializationException` in `AssetLoader.loadManifest` and throw `IllegalArgumentException` (existing contract). `FilterCatalog` hardcodes definitions; only `behaviorConfig` field is from JSON. Catalog entries are compile-time safe.

**T-04-03: Memory pressure from 15 filters × LruCache eviction thrashing on low-memory devices**
- Mitigation: LruCache eviction is intentional (Phase 3 D-09). When AssetLoader evicts frames of a previously-loaded filter and user swaps back, `preload()` re-decodes from assets. Performance impact: ~100-200ms re-decode per filter on cameraExecutor. On low-heap devices (minSdk 28, many with 2-3GB RAM), `maxMemory()/8` cap is still 64-128MB — no eviction expected in normal use with 32MB cap.

**T-04-04: Network-free APK extraction**
- Already addressed: all sprite extraction runs offline from `reference/raw_extract/res/raw/*.json`. Firebase Storage URL found in APK binary is not used. No network access during Wave 0 extraction.

**T-04-05: Race condition on rapid filter-swap during preload**
- Mitigation: `FilterEngine.setFilter()` writes `AtomicReference<FilterDefinition?>` (Phase 3 D-11). If preload is in-flight during setFilter, `AssetLoader.get()` returns null → `onDraw` skips that frame (no-ghost per REN-06). `perFaceState.clear()` is called inside `setFilter()` which runs on cameraExecutor; `onDraw` runs on renderExecutor. ConcurrentHashMap.clear() is atomic. Race is safe.

**T-04-06: Home-screen navigation mode arg tampering**
- Low risk for personal-use app. `CameraMode` is a compile-time enum; kotlinx.serialization deserializes to one of two known values or throws. An invalid mode string causes navigation-compose to throw a `SerializationException` — app may crash on that route. Mitigation: wrap `navController.navigate(CameraRoute(...))` call sites; enum has only 2 valid values so the surface is minimal.

---

## Open Questions (RESOLVED)

**Q1 [RESOLVED]: Does Coil 2.7 handle `file:///android_asset/` URIs natively?**
- Answer: [ASSUMED YES] — Coil 2.x includes a built-in `AssetUriFetcher` that handles `file:///android_asset/` scheme. If this fails at test time, fall back to a custom `Fetcher` or use `context.assets.open(path)` manually in a Coil `ImageLoader` factory. Test in Wave 0 with one filter before wiring all 15.

**Q2 [RESOLVED]: Are there additional Lottie JSONs in the reference APK beyond the 3 found in `res/raw/`?**
- Answer: [VERIFIED: binary scan of APK] — No. The APK binary contains references to `onboarding_1.json`, `onboarding_2.json`, `onboarding_3.json`, `shake_phone.json`, `swipe_right.json` (all UI animations), and `spider_prankfilter.json`, `home_lottie.json`, `home_filter.json` (bug filters). No other filter JSONs. Additional filters are server-downloaded from Firebase Storage (`govo-prankfilter-316f5.firebasestorage.app`).

**Q3 [RESOLVED]: Does `spider_prankfilter.json` use the same extraction pattern as ant?**
- Answer: [VERIFIED: JSON inspection] — No. Spider has a flat `layers` structure with 23 image layers each referencing `data.assets[i]` directly by `refId`. The base64 PNGs are in `data.assets[0..22].p`. All frames are 1500×1500 (much larger than ant's 360×360). Extraction is straightforward: iterate `data.assets`, base64-decode `asset.p`.

**Q4 [RESOLVED]: Is `ConcurrentHashMap.getOrPut` safe for single-writer single-reader pattern?**
- Answer: [ASSUMED SAFE] — Since `onDraw` (the only writer and reader) runs on `renderExecutor` (single-thread executor), there is no actual concurrency for `perFaceState`. The map is `ConcurrentHashMap` for `setFilter()` / `onFaceLost()` calls which may come from `cameraExecutor`. Since these only `clear()` and `remove()`, and `getOrPut` is called only from `renderExecutor`, the lack of atomicity in `getOrPut` is not a problem.

**Q5 [RESOLVED]: Does navigation-compose 2.8.9 support `@Serializable enum class` as route argument?**
- Answer: [ASSUMED YES] — Navigation-compose 2.8+ type-safe APIs use kotlinx.serialization for argument parsing. Kotlin enums are serializable by default with kotlinx.serialization (they serialize to their name string). `CameraMode.FaceFilter` serializes as `"FaceFilter"`. Route: `CameraRoute(mode=FaceFilter)` → URL `/com.bugzz.filter.camera.ui.nav.CameraRoute/FaceFilter`. No special annotation needed on `CameraMode` if it uses default serialization. Mark `@Serializable` on `CameraMode` as defensive measure.

---

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| `BugState` flat data class | `BehaviorState` sealed interface | Phase 4 (this phase) | Enables CRAWL/SWARM/FALL state without kitchen-sink fields |
| Single face in `FilterEngine.onDraw` | `List<SmoothedFace>` loop with primary/secondary dispatch | Phase 4 (this phase) | Multi-face rendering without rebinding |
| `Cycle Filter` debug button | `LazyRow` filter picker inline above shutter | Phase 4 (this phase) | Production UX for CAT-03/04 |
| Phase 1 stub `HomeScreen` | Production `HomeScreen` with Face Filter / Insect Filter buttons | Phase 4 (this phase) | MOD-01 delivered |

---

## Assumptions Log

| # | Claim | Section | Risk if Wrong |
|---|-------|---------|---------------|
| A1 | Coil 2.7 handles `file:///android_asset/` URI scheme natively | Coil code example | Thumbnails fail silently → placeholder images shown; fix = custom Fetcher (~30 LOC) |
| A2 | Navigation-compose 2.8.9 serializes `enum class CameraMode` without custom serializer | CameraRoute pattern | Route navigation crashes → fix = add `@Serializable` or provide custom serializer |
| A3 | `ConcurrentHashMap.getOrPut` is safe for single-renderExecutor writer pattern | ConcurrentHashMap pitfall | No race in practice; getOrPut atomicity issue only matters with concurrent writers |
| A4 | `Frame.size.width/height` gives sensor-buffer dimensions for FALL spawn coordinates | FALL behavior | FALL bugs may spawn off-canvas → fix = use `canvas.getClipBounds()` instead |
| A5 | coil-compose 2.7.0 and datastore-preferences 1.1.3 are the current stable versions | Standard Stack | Version mismatch → build error or API change; verify via `npm view` analog in Maven |

---

## Sources

### Primary (HIGH confidence)
- Phase 4 `04-CONTEXT.md` D-01..D-29 — locked decisions
- Phase 3 codebase (FilterEngine.kt, BugBehavior.kt, BugState.kt, AssetLoader.kt, FilterDefinition.kt, FilterCatalog.kt, SpriteManifest.kt, FaceLandmarkMapper.kt, CameraViewModel.kt, CameraUiState.kt, Routes.kt, BugzzApp.kt, CameraModule.kt, StubScreens.kt) — verified by direct read
- `reference/raw_extract/res/raw/spider_prankfilter.json` — JSON layer structure verified by Node.js inspection (layers[*].nm = Spider_1.png..Spider_23.png; assets[*].p = base64 PNG data)
- `reference/raw_extract/res/raw/home_lottie.json` — asset groups A/B/C identified by size/ID inspection
- `reference/com.insect.filters.funny.prank.bug.filter.face.camera.apk` binary — Firebase Storage URL confirmed; no additional Lottie JSONs found

### Secondary (MEDIUM confidence)
- CLAUDE.md Recommended Stack table — Coil 2.7.0 + DataStore 1.1.3 pre-catalogued
- `.planning/research/STACK.md` — Coil + DataStore versions
- `.planning/research/ARCHITECTURE.md` §3, §6 — rendering pipeline, StateFlow UDF patterns
- `.planning/research/PITFALLS.md` §3, §4, §7, §13 — landmark jitter, backpressure, multi-face

### Tertiary (LOW confidence — assumed from training knowledge)
- Coil `file:///android_asset/` scheme support
- Navigation-compose 2.8.9 enum serialization behavior
- `ConcurrentHashMap.computeIfAbsent` atomicity note

---

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH — existing codebase confirms exact versions
- Architecture: HIGH — all patterns extend validated Phase 3 code
- Sprite extraction: HIGH — JSON structure verified by direct inspection
- Behavior tick math: MEDIUM — CRAWL/SWARM/FALL implementations are first-time; math is straightforward but untested
- Pitfalls: MEDIUM — most derived from code analysis; some ASSUMED

**Research date:** 2026-04-20
**Valid until:** 2026-06-01 (stable stack; Coil/DataStore versions unlikely to break API within 6 weeks)

---

## RESEARCH COMPLETE

**Phase:** 04 - Filter Catalog + Picker + Face Filter Mode
**Confidence:** HIGH (architecture + extraction), MEDIUM (behavior tick math)

### Key Findings

1. **Reference APK sprite constraint (CRITICAL):** Only 3 Lottie JSON files bundled; remaining filters are server-downloaded from Firebase. Phase 4 can extract 4 distinct sprite groups (spider 23fr, bug_a 7fr, bug_b 12fr, bug_c 16fr) and must build 15 FilterDefinitions as behavior/anchor variants of these 4 sprite types.

2. **Spider Phase 3 fix is straightforward:** `spider_prankfilter.json` has a flat asset structure — base64 PNGs directly in `data.assets[0..22].p`. The issue was using the wrong source file/group, not a wrong layer. Extraction script above resolves it.

3. **BehaviorState sealed + ConcurrentHashMap pattern is low-risk:** All concurrency concerns resolve to: `onDraw` (renderExecutor, single-threaded) is the sole writer/reader of `perFaceState` entries; `setFilter()` and `onFaceLost()` only `clear()` / `remove()` (safe concurrent ops). No locks needed.

4. **DataStore introduction is trivial:** Pattern is well-established (`preferencesDataStore` delegate + `FilterPrefsRepository` wrapper + ViewModel `viewModelScope.launch { edit {...} }`). Test via `PreferenceDataStoreFactory.create` in-memory.

5. **Coil 2.7 + `file:///android_asset/` pattern:** Standard scheme for Android assets. One Wave 0 smoke test (single filter thumbnail visible) validates before wiring all 15.

### Confidence Assessment
| Area | Level | Reason |
|------|-------|--------|
| Sprite extraction | HIGH | JSON structure directly verified; script pseudocode is mechanical |
| BehaviorState sealed refactor | HIGH | Direct code analysis of Phase 3 types |
| CRAWL/SWARM/FALL math | MEDIUM | Algorithms are documented in D-08/09/10; first implementation pass |
| DataStore integration | HIGH | Established Android pattern; version pre-catalogued |
| LazyRow picker | HIGH | Standard Compose pattern; no novel APIs |
| Multi-face rendering | HIGH | perFaceState map extends validated FilterEngine |

### Open Questions
- Wave 0 must visually validate `bug_a`, `bug_b`, `bug_c` frames before naming them as display filters (are they visually distinct? same bug different pose?). If groups A and B are indistinguishable, merge into one sprite set.

### Ready for Planning
Research complete. Planner can now create PLAN.md files for Phase 4.
