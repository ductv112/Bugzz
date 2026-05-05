---
phase: 06
plan: 05
subsystem: collection-screen-mediastore-repository-empty-state
tags: [wave-4, collection, mediastore, lazyverticalgrid, mediametadataretriever, empty-state, ux-polish]
dependency_graph:
  requires:
    - "Plan 06-04 (291daeb) — PreviewRoute(val uri: String) data class shape; CollectionScreen onItemTap reuses this exact route"
    - "Plan 06-02 (03ff140) — coil-compose 2.7.0 on classpath (used for AsyncImage thumbnail), lottie-compose 6.7.1 (used by EmptyStateColumn via existing LottiePlayer)"
    - "Phase 5 baseline GREEN — Bugzz captures (image/jpeg + video/mp4) flowing into DCIM/Bugzz/ via MediaStoreOutputOptions"
  provides:
    - "data/MediaItem.kt — pure data class (uri, mimeType, displayName, dateModified) per D-15; bitmap-free per RESEARCH §Anti-pattern"
    - "data/CollectionRepository.kt — @Singleton MediaStore.Files query; selectionArgs-bound RELATIVE_PATH LIKE filter (T-06-02 mitigation, NOT string-concat); per-MIME URI namespace re-construction via ContentUris.withAppendedId (D-12 / RESEARCH §Critical Note); flowOn(IO); DATE_MODIFIED DESC"
    - "ui/collection/CollectionViewModel.kt — @HiltViewModel @Inject(@ApplicationContext, repository); init collects loadMediaItems and lazy-extracts video thumbnails via MediaMetadataRetriever on Dispatchers.IO; bitmap cache in StateFlow<Map<Uri, Bitmap?>>"
    - "ui/collection/CollectionScreen.kt — full Compose; SmallTopAppBar('My Collection') + back-arrow IconButton; LazyVerticalGrid(GridCells.Adaptive(minSize=120.dp)) with 4dp×4dp spacing; CollectionThumbnail per item Box(aspectRatio(1f)) clip(RectangleShape) clickable Role.Button; image rows = AsyncImage(Crop), video rows = bitmap (or #2A2A2A placeholder) under 30% scrim with 24dp white PlayArrow icon; empty branch = EmptyStateColumn"
    - "ui/components/EmptyStateColumn.kt — shared composable (D-26 / UI-SPEC §8): 120dp Lottie loop + 16dp Spacer + 16sp/Medium heading + 16dp Spacer + Material3 Button(labelLarge); parameterized heading/ctaLabel/onCta/animationAsset; defaults to lottie/home_lottie.json per D-29"
    - "BugzzApp CollectionRoute composable rewired from stub to production with onItemTap → PreviewRoute(uri.toString()) and onOpenCamera → HomeRoute popUpTo CollectionRoute inclusive (D-13 standard back-stack)"
  affects:
    - "Suite count: 171 tests / 6 ignored / 0 failures (Plan 06-04 baseline 171/12; same total, -6 ignored — un-Ignored 4 CollectionRepositoryTest + 2 CollectionViewModelTest cases)"
    - "ui/screens/StubScreens.kt CollectionScreen now orphaned but file UNCHANGED per scope contract — Plan 07 owns deletion alongside SplashScreen + CameraScreen + PreviewScreen stubs"
    - "9 D-32 grep-asserts intact: 4/1/3/6/1/2/9/1/2 file counts (isCapturing / bindJob?.cancel() / OneShotEvent.FilterLoadError / captureFlash / require(frameCount > 0) / assetLoader.preload(def.assetDir) / isRecording / cameraMode = …CameraMode.InsectFilter / setPreviewSize)"
    - "AGP/dexBuilder: assembleDebug clean — APK ships with no new third-party deps (everything reused from Plan 06-02)"
tech-stack:
  added:
    - "(none — Plan 06-05 only consumes existing classpath: coil 2.7.0 + lottie-compose 6.7.1 + Material3 LazyVerticalGrid/SmallTopAppBar from Compose BOM 2026.03.00)"
  patterns:
    - "Pure-data MediaItem with separate ViewModel-cached thumbnail Map (RESEARCH §Anti-pattern resolution): keeping Bitmap off the model lets the cursor-mapped list survive configuration change without leaking native frame buffers, and lets the ViewModel cache thumbnails in a separate Map<Uri, Bitmap?> whose lifecycle is bound to viewModelScope"
    - "T-06-02 SQL-injection-prevention pattern: RELATIVE_PATH LIKE ? + selectionArgs[] parameter binding — never string concat. Verified by ArgumentCaptor<Array<String>> in CollectionRepositoryTest.selectionArgsBindRelativePath asserting captured[0] == 'DCIM/Bugzz/%'"
    - "Per-MIME URI namespace re-construction (D-12 / RESEARCH §Critical Note): MediaStore.Files unified cursor returns rows with _IDs scoped to Files namespace, but downstream consumers (sharing apps, openInputStream) often reject content://media/external/file/{id}. CollectionRepository re-constructs each URI via ContentUris.withAppendedId(Images.Media.EXTERNAL_CONTENT_URI, id) for image rows / Video.Media.EXTERNAL_CONTENT_URI for video rows. Verified by 2 dedicated unit tests (imageRow_constructsImagesMediaUri / videoRow_constructsVideoMediaUri)"
    - "Robolectric MatrixCursor harness for ContentResolver mocks: Mockito.mock(ContentResolver) + whenever(.query).thenReturn(MatrixCursor) — pure JVM cursor with addRow(arrayOf(...)) lets unit tests deterministically exercise the SUT's column-index lookup + per-row mapping logic without device or emulator"
    - "Robolectric mock-Context test harness (Phase 3 STATE #24 / Plan 06-04 STATE #40 pattern reused): mock<Context>() + mock<ContentResolver>() + whenever(.contentResolver).thenReturn(.) — bypasses Hilt to construct CollectionRepository directly with deterministic resolver behavior"
    - "Lazy off-critical-path video thumbnail extraction: viewModelScope launches a child coroutine PER video item to call MediaMetadataRetriever.getFrameAtTime(0, OPTION_CLOSEST_SYNC) on Dispatchers.IO; bitmap (or null on failure) writes to videoThumbnails Map via _uiState.update {} so the grid renders placeholders instantly and fills in as bitmaps decode"
    - "Empty-state CTA via popUpTo inclusive: BugzzApp's onOpenCamera lambda navigates to HomeRoute popUpTo(CollectionRoute) { inclusive = true } — Collection is removed from the back-stack so the user lands cleanly on Home (no Back-button trap)"
key-files:
  created:
    - "app/src/main/java/com/bugzz/filter/camera/data/MediaItem.kt (32 lines)"
    - "app/src/main/java/com/bugzz/filter/camera/data/CollectionRepository.kt (113 lines)"
    - "app/src/main/java/com/bugzz/filter/camera/ui/collection/CollectionViewModel.kt (107 lines)"
    - "app/src/main/java/com/bugzz/filter/camera/ui/collection/CollectionScreen.kt (200 lines)"
    - "app/src/main/java/com/bugzz/filter/camera/ui/components/EmptyStateColumn.kt (87 lines)"
  modified:
    - "app/src/main/java/com/bugzz/filter/camera/ui/BugzzApp.kt (97 → 105 lines; import swap from ui/screens/CollectionScreen → ui/collection/CollectionScreen; CollectionRoute composable rewired from `CollectionScreen(onBack = popBackStack)` stub to production triple-callback wiring)"
    - "app/src/test/java/com/bugzz/filter/camera/data/CollectionRepositoryTest.kt (87 → 188 lines; 4 RED stubs → 4 GREEN cases via Robolectric + MatrixCursor + ArgumentCaptor)"
    - "app/src/test/java/com/bugzz/filter/camera/ui/collection/CollectionViewModelTest.kt (50 → 117 lines; 2 RED stubs → 2 GREEN cases pure-JVM mock<Context> + mock<CollectionRepository> + flowOf)"
decisions:
  - "Empty-state CTA pops Collection from back-stack via popUpTo(CollectionRoute) { inclusive = true } — not just navigate(HomeRoute). Otherwise users hitting Back from Home would land on the empty Collection screen again (Back-button trap). UI-SPEC §6 specifies the popUpTo behavior; verified by code review of BugzzApp.kt L93-103."
  - "CollectionScreen 'isLoading' branch deferred — UI-SPEC §6 subjective note: a typical MediaStore query completes in <50ms; flashing a CircularProgressIndicator for that interval feels worse than rendering the empty grid for one frame. Implementation uses `when { uiState.isEmpty -> EmptyStateColumn; else -> LazyVerticalGrid }` — empty-list initial state collapses naturally to EmptyStateColumn until first emission lands, then the grid takes over."
  - "Video thumbnail caching keyed on item.uri (not displayName or _id) — URI is the natural identity for MediaStore rows and matches the lookup key in the composable: `uiState.videoThumbnails[item.uri]`. mock<Uri>() in tests because Uri.parse returns null on pure JVM (Phase 3 STATE #24 pattern)."
  - "MediaMetadataRetriever wrapped in try/catch/finally inside CollectionViewModel.extractAndCacheThumbnail: catches Exception (codec error, deleted file race, corrupt MP4) and writes uri → null into the cache so the composable still renders the placeholder + play-icon without crashing. Native release() also wrapped in try/catch — if the retriever was never successfully attached, release() can throw RuntimeException; we swallow it with a Timber.w log."
  - "Inline `androidx.compose.foundation.Image(bitmap = ...)` for video thumbnail because top-level `Image` import collision with `Icon` was avoided — used the FQCN inline. Pre-existing pattern: PreviewScreen and other screens don't render Bitmap-backed Images, so this is the first introduction. Acceptable inline FQCN per project KDoc-light convention."
  - "ArrowBack icon deprecation warning (`Icons.Default.ArrowBack` → `Icons.AutoMirrored.Filled.ArrowBack`) NOT addressed — same warning exists for Phase 4 PreviewScreen and is project-wide. Out of scope for Plan 06-05; Plan 07 cleanup pass owns icon migration."
  - "EmptyStateColumn animationAsset defaults to 'lottie/home_lottie.json' (D-29 single-asset reuse across Splash + Onboarding + EmptyState). Caller can override (e.g., future Settings clear-all confirmation could use a different animation if desired) — kept the parameter for forward extensibility."
metrics:
  duration: 535
  completed: 2026-05-05
---

# Phase 6 Plan 05: Wave 4 CollectionRepository + CollectionScreen + EmptyStateColumn Summary

**One-liner:** Production CollectionScreen + CollectionRepository + CollectionViewModel + EmptyStateColumn shipped — MediaStore.Files query scoped to DCIM/Bugzz/ with T-06-02 selectionArgs binding + per-MIME URI namespace re-construction (Images.Media for image rows, Video.Media for video rows per RESEARCH §Critical Note); LazyVerticalGrid(Adaptive(120.dp)) thumbnail UI with 4dp×4dp spacing, image-row Coil AsyncImage(Crop), video-row VM-cached MediaMetadataRetriever bitmap under 30% scrim + 24dp white PlayArrow overlay; empty-state branch routes to shared EmptyStateColumn (120dp Lottie loop + heading + Open Camera CTA); BugzzApp CollectionRoute composable rewired stub → production with onItemTap → PreviewRoute(uri.toString()) and onOpenCamera → HomeRoute popUpTo CollectionRoute inclusive; suite GREEN 171/6 ignored/0 failures (-6 ignored vs Plan 06-04 baseline; un-Ignored 4 RepoTest + 2 VMTest); 9 D-32 grep-asserts intact (4/1/3/6/1/2/9/1/2).

---

## What Landed

### Production Code (5 created + 1 modified)

1. **`app/src/main/java/com/bugzz/filter/camera/data/MediaItem.kt`** (32 lines)
   - `data class MediaItem(uri: Uri, mimeType: String, displayName: String, dateModified: Long)`
   - Pure data — bitmaps NOT stored (RESEARCH §Anti-pattern resolution)
   - KDoc references D-15, UX-05, RESEARCH §Critical Note

2. **`app/src/main/java/com/bugzz/filter/camera/data/CollectionRepository.kt`** (113 lines)
   - `@Singleton @Inject(@ApplicationContext context)`
   - `fun loadMediaItems(): Flow<List<MediaItem>>` — `flow {}` builder
   - 4-column projection: `_ID`, `DISPLAY_NAME`, `MIME_TYPE`, `DATE_MODIFIED`
   - Selection: `RELATIVE_PATH LIKE ? AND (MIME_TYPE = ? OR MIME_TYPE = ?)`
   - selectionArgs: `["DCIM/Bugzz/%", "image/jpeg", "video/mp4"]` — **T-06-02 mitigation**
   - Sort: `DATE_MODIFIED DESC`
   - Per-MIME URI namespace via `ContentUris.withAppendedId`:
     - image rows → `MediaStore.Images.Media.EXTERNAL_CONTENT_URI`
     - video rows → `MediaStore.Video.Media.EXTERNAL_CONTENT_URI`
   - `flowOn(Dispatchers.IO)` at terminal flow chain

3. **`app/src/main/java/com/bugzz/filter/camera/ui/collection/CollectionViewModel.kt`** (107 lines)
   - `data class CollectionUiState(items, videoThumbnails: Map<Uri, Bitmap?>, isLoading, isEmpty)`
   - `@HiltViewModel @Inject(@ApplicationContext context, repository)`
   - `init {}` → `viewModelScope.launch { repository.loadMediaItems().collect { items -> _uiState.update + per-video thumbnail extraction } }`
   - `private suspend fun extractAndCacheThumbnail(uri)` on Dispatchers.IO with `MediaMetadataRetriever.getFrameAtTime(0, OPTION_CLOSEST_SYNC)`; try/catch Exception → writes `uri to null`; release() in finally

4. **`app/src/main/java/com/bugzz/filter/camera/ui/components/EmptyStateColumn.kt`** (87 lines)
   - `@Composable fun EmptyStateColumn(heading, ctaLabel, onCta, modifier, animationAsset = "lottie/home_lottie.json")`
   - Layout per UI-SPEC §8: Column CenterHorizontally + Center, fillMaxSize → LottiePlayer(120.dp) + 16dp + 16sp/Medium Text + 16dp + Material3 Button(labelLarge)
   - Lottie has `Modifier.semantics { contentDescription = "Empty state animation" }`

5. **`app/src/main/java/com/bugzz/filter/camera/ui/collection/CollectionScreen.kt`** (200 lines)
   - `@OptIn(ExperimentalMaterial3Api::class) @Composable fun CollectionScreen(onBack, onItemTap, onOpenCamera, viewModel = hiltViewModel())`
   - `Scaffold(topBar = TopAppBar("My Collection", navigationIcon = back-arrow IconButton)) { padding -> ... }`
   - Branch: `uiState.isEmpty → EmptyStateColumn(...)` else `LazyVerticalGrid(GridCells.Adaptive(minSize=120.dp), verticalArrangement = Arrangement.spacedBy(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp))`
   - `items(uiState.items, key = { it.uri.toString() })` → `CollectionThumbnail`
   - `private CollectionThumbnail`: aspectRatio(1f) + clip(RectangleShape) + clickable + Role.Button + displayName as contentDescription; image branch = Coil AsyncImage(Crop) with #2A2A2A placeholder/error; video branch = bitmap (or #2A2A2A) under 30% black scrim with centered 24dp white PlayArrow

6. **`app/src/main/java/com/bugzz/filter/camera/ui/BugzzApp.kt`** (97 → 105 lines, modified)
   - Import swap: `ui.screens.CollectionScreen` removed (StubScreens still owns the file unchanged); `ui.collection.CollectionScreen` added
   - `composable<CollectionRoute>` rewired:
     ```kotlin
     CollectionScreen(
         onBack = { navController.popBackStack() },
         onItemTap = { item -> navController.navigate(PreviewRoute(item.uri.toString())) },
         onOpenCamera = { navController.navigate(HomeRoute) {
             popUpTo(CollectionRoute) { inclusive = true }
         } },
     )
     ```

### Test Code (2 modified)

7. **`app/src/test/java/com/bugzz/filter/camera/data/CollectionRepositoryTest.kt`** (87 → 188 lines)
   - `@RunWith(RobolectricTestRunner::class) @Config(sdk = [34])`
   - 4 GREEN cases:
     - `emptyCursor_emitsEmptyList` (UX-05 happy path)
     - `imageRow_constructsImagesMediaUri` (D-12 namespace correctness — MatrixCursor row mime=image/jpeg → URI contains "images/media/1")
     - `videoRow_constructsVideoMediaUri` (D-12 namespace correctness — mime=video/mp4 → URI contains "video/media/2")
     - `selectionArgsBindRelativePath` (**T-06-02 mitigation evidence** — ArgumentCaptor<Array<String>> verifies captured[0] == "DCIM/Bugzz/%")
   - `buildContextWithCursor()` helper mirrors VideoRecorderTest.buildMockContext()
   - `buildSingleRowCursor(id, mime, name)` MatrixCursor factory
   - Turbine `repository.loadMediaItems().test { awaitItem; ...; awaitComplete }`

8. **`app/src/test/java/com/bugzz/filter/camera/ui/collection/CollectionViewModelTest.kt`** (50 → 117 lines)
   - Pure JVM (no @RunWith) — StandardTestDispatcher + setMain/resetMain
   - 2 GREEN cases:
     - `emptyList_setsIsEmptyTrue` (UX-07 — empty repository emission → uiState.isEmpty=true, items=[], isLoading=false)
     - `nonEmptyList_setsIsEmptyFalse` (UX-05 — 2-item emission → isEmpty=false, items.size=2; uses `mock<Uri>()` per Phase 3 STATE #24)
   - Tiny infix helper: `infix fun OngoingStubbing<Flow<T>>.doReturnFlowOf(value: T) { thenReturn(flowOf(value)) }`

---

## T-06-02 Mitigation Grep Evidence

```
$ grep -n "selectionArgs" app/src/main/java/com/bugzz/filter/camera/data/CollectionRepository.kt
68:        val selectionArgs = arrayOf("DCIM/Bugzz/%", "image/jpeg", "video/mp4")
80:            selectionArgs,
```

```
$ grep -n "RELATIVE_PATH.*LIKE" app/src/main/java/com/bugzz/filter/camera/data/CollectionRepository.kt
65:        val selection = "${MediaStore.MediaColumns.RELATIVE_PATH} LIKE ? AND " +
```

Selection clause uses `?` placeholder + parallel selectionArgs array — never string concatenation. ContentResolver bind-parameters the args before issuing the underlying SQL. SQLi prevented at ContentResolver layer.

Test asserts:
```
$ grep -n "captured\[0\]" app/src/test/java/com/bugzz/filter/camera/data/CollectionRepositoryTest.kt
171:            "selectionArgs[0] must bind to DCIM/Bugzz/% — got: ${captured[0]}",
172:            captured[0] == "DCIM/Bugzz/%",
```

---

## End-to-End Composable-Level Round-Trip

`HomeScreen.onMyCollection` lambda (Phase 4 wiring, untouched) → `navController.navigate(CollectionRoute)` → BugzzApp's `composable<CollectionRoute>` (rewired this plan) instantiates production `CollectionScreen`. CollectionViewModel's init begins the MediaStore query on IO. First emission lands → `uiState.items` populates the grid with thumbnails. User taps a thumbnail → `onItemTap(item)` → `navController.navigate(PreviewRoute(item.uri.toString()))` → BugzzApp's `composable<PreviewRoute>` (Plan 06-04) parses `Uri.parse(route.uri)` and shows `PreviewScreen(uri)`. From Preview, "Retake" or "Done" → `popBackStack()` → returns to Collection (D-13 standard back-stack — Retake from Collection-entered Preview goes back to Collection, NOT to Camera).

Empty-collection path: fresh install (or after deleting all captures) → CollectionViewModel emits empty list → `uiState.isEmpty=true` → `EmptyStateColumn("No bugs captured yet", "Open Camera", onCta = onOpenCamera)`. Tap "Open Camera" → `navController.navigate(HomeRoute) { popUpTo(CollectionRoute) { inclusive = true } }` → Home, with Collection cleared from back-stack (no Back-button trap).

---

## D-32 Grep-Assert Invariants (9/9 intact)

| # | Pattern | File count | Plan 06-04 baseline | Status |
|---|---------|-----------:|--------------------:|--------|
| 1 | `isCapturing` | 4 | 4 | ✓ |
| 2 | `bindJob?.cancel()` | 1 | 1 | ✓ |
| 3 | `OneShotEvent.FilterLoadError` | 3 | 3 | ✓ |
| 4 | `captureFlash` | 6 | 6 | ✓ |
| 5 | `require(frameCount > 0)` | 1 | 1 | ✓ |
| 6 | `assetLoader.preload(def.assetDir)` | 2 | 2 | ✓ |
| 7 | `isRecording` | 9 | 9 | ✓ |
| 8 | `cameraMode = …CameraMode.InsectFilter` | 1 | 1 | ✓ |
| 9 | `setPreviewSize` | 2 | 2 | ✓ |

Phase 3+4+5 fix commits preserved verbatim.

---

## UI-SPEC §6 Grid Layout Verbatim (verified)

```
$ grep -nE '\.dp\b' app/src/main/java/com/bugzz/filter/camera/ui/collection/CollectionScreen.kt
38:import androidx.compose.ui.unit.dp
53: *   - Otherwise: `LazyVerticalGrid(GridCells.Adaptive(120.dp))` with `4dp` vertical and horizontal
114:                    columns = GridCells.Adaptive(minSize = 120.dp),
115:                    verticalArrangement = Arrangement.spacedBy(4.dp),
116:                    horizontalArrangement = Arrangement.spacedBy(4.dp),
198:                    modifier = Modifier.size(24.dp),
```

Spacing values used: `{4, 24, 120}` — strict subset of UI-SPEC §6 allowed `{4, 24, 120, 32}`.
- `LazyVerticalGrid` + `GridCells.Adaptive(minSize = 120.dp)` exactly once ✓
- `Arrangement.spacedBy(4.dp)` 2 occurrences (verticalArrangement + horizontalArrangement) ≥ 1 ✓
- 24dp PlayArrow overlay icon size matches UI-SPEC §6 verbatim ✓

---

## Test Count

- **Total:** 171 tests / 6 ignored / 0 failures / 0 errors (Plan 06-04 baseline: 171 / 12 / 0 / 0)
- **Net:** 0 new tests; -6 ignored (un-Ignored 4 CollectionRepositoryTest + 2 CollectionViewModelTest cases that were RED scaffolds in Plan 06 Wave 0)

| Test class | Tests | Skipped | Status |
|------------|------:|--------:|--------|
| CollectionRepositoryTest | 4 | 0 | GREEN (was 4 ignored Wave 0 → 4 GREEN now) |
| CollectionViewModelTest | 2 | 0 | GREEN (was 2 ignored Wave 0 → 2 GREEN now) |
| **Plan 06-05 contribution** | **6** | **-6** | **GREEN** |

---

## Atomic Commits (3 total)

| # | Hash | Message (truncated) |
|---|------|---------------------|
| 1 | `3aae2b3` | feat(06-05): add MediaItem + CollectionRepository (UX-05, T-06-02) |
| 2 | `e8017ef` | feat(06-05): add CollectionViewModel + EmptyStateColumn (UX-05/07, D-26) |
| 3 | `73617ff` | feat(06-05): wire production CollectionScreen + BugzzApp CollectionRoute (UX-05/06/07) |

---

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Kotlin KDoc closure parsing — `*/` literal inside backticks (CollectionRepository.kt)**
- **Found during:** Task 1 first compile pass
- **Issue:** KDoc lines 33-34 originally contained `\`image/*\`` and `\`video/*\`` (with backtick-fenced literals as documentation). Kotlin's KDoc parser does NOT honor backticks as escape — the `*/` inside ``image/*`` was interpreted as comment terminator, leading to "Syntax error: Unclosed comment" at line 114.
- **Fix:** Rewrote the two lines to "for image rows" / "for video rows" (no `*/` literal). Identical semantic content.
- **Files modified:** `app/src/main/java/com/bugzz/filter/camera/data/CollectionRepository.kt`
- **Commit:** Folded into `3aae2b3`

**2. [Rule 1 - Bug] RectangleShape import path — wrong package (CollectionScreen.kt)**
- **Found during:** Task 3 compile pass
- **Issue:** Initial import was `androidx.compose.foundation.shape.RectangleShape` (RoundedCornerShape sibling). The actual class lives at `androidx.compose.ui.graphics.RectangleShape` (object).
- **Fix:** Swapped import path. Compile clean.
- **Files modified:** `app/src/main/java/com/bugzz/filter/camera/ui/collection/CollectionScreen.kt`
- **Commit:** Folded into `73617ff`

### Plan-Compliant Choices Documented for Audit

- **CollectionScreen TopAppBar uses `TopAppBar` (not `SmallTopAppBar`):** UI-SPEC §6 specifies `SmallTopAppBar`, but Material3 has deprecated this name in favor of plain `TopAppBar` (which IS the small variant by default). Function call shape identical. No deviation in user-visible behavior.
- **Inline `androidx.compose.foundation.Image` FQCN for video bitmap rendering:** Top-level `Image` import collision with Material3's `Icon` was avoided by using the FQCN. Acceptable per project KDoc-light convention.

### No Auth Gates / No Architectural Decisions Required

Plan 06-05 was fully autonomous. No checkpoint reached. No Rule 4 escalations.

---

## Wave 5 Readiness (Plans 06-06 + 06-07 + 06-08)

Wave 4 closes the production read path for the Collection grid. Wave 5 (per phase plan budget D-34) covers:

- **Plan 06-06** — Share intent (`Intent.ACTION_SEND` + `FileProvider`) + `ShareIntentBuilder` + extract `DeleteConfirmDialog` from inline AlertDialog (PreviewScreen + future Settings clear-all reuse) + T-06-01 URI grant flag setup
- **Plan 06-07** — SettingsScreen + SettingsViewModel + HomeScreen `onSettings` lambda + StubScreens.kt deletion + Settings clear-all action (reuses CollectionRepository)
- **Plan 06-08** — 06-HANDOFF runbook + Phase 6 device-checkpoint (Splash → Onboarding → Home → Camera/InsectFilter capture → Preview → Done returns to Camera + Collection grid loads + thumbnail tap → Preview round-trip + Delete → returns to Collection + EmptyStateColumn renders correctly on cleared device)

Production data layer (MediaItem + CollectionRepository) is reusable verbatim by future Settings clear-all workflow — no further repository modifications needed.

---

## Self-Check: PASSED

**File existence:**
- ✓ FOUND: `app/src/main/java/com/bugzz/filter/camera/data/MediaItem.kt`
- ✓ FOUND: `app/src/main/java/com/bugzz/filter/camera/data/CollectionRepository.kt`
- ✓ FOUND: `app/src/main/java/com/bugzz/filter/camera/ui/collection/CollectionViewModel.kt`
- ✓ FOUND: `app/src/main/java/com/bugzz/filter/camera/ui/collection/CollectionScreen.kt`
- ✓ FOUND: `app/src/main/java/com/bugzz/filter/camera/ui/components/EmptyStateColumn.kt`

**Commit existence:**
- ✓ FOUND: `3aae2b3` — Task 1 (MediaItem + CollectionRepository + 4 RepoTest GREEN)
- ✓ FOUND: `e8017ef` — Task 2 (CollectionViewModel + EmptyStateColumn + 2 VMTest GREEN)
- ✓ FOUND: `73617ff` — Task 3 (CollectionScreen + BugzzApp wire)

**Suite status:** 171 tests / 6 ignored / 0 failures / 0 errors — GREEN  
**Build status:** assembleDebug clean  
**D-32 invariants:** 9/9 intact (4/1/3/6/1/2/9/1/2)
