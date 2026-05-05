# Phase 6: UX Polish — Splash, Home, Onboarding, Preview, Collection, Share - Research

**Researched:** 2026-05-05
**Domain:** Android Compose UX screens — Lottie, HorizontalPager, Media3 ExoPlayer, MediaStore query, Intent.ACTION_SEND, DataStore extension, type-safe nav with URI arg
**Confidence:** HIGH (all API patterns verified against official Android docs, Lottie official guide, and current codebase state)

---

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions

- **D-01:** Lottie splash auto-advance in ≤1.5s. Use `com.airbnb.android:lottie-compose:6.7.1`. Full-screen centered + "Bugzz" 32sp/Medium below. `LaunchedEffect(Unit) { delay(1_500); navigate(next) }`.
- **D-02:** Conditional first-launch routing: Splash queries DataStore `onboarding_completed: Boolean`. false/unset → `OnboardingRoute`; true → `HomeRoute`. `popUpTo` clears back-stack.
- **D-03:** 3-screen Lottie carousel via `HorizontalPager` (Compose Foundation 2026.04+). Lotties reuse `home_lottie.json` groups A/B/C.
- **D-04:** Bottom controls: PageIndicator dots (3 dots, active dot scale 1.5x + `#FFE53935` fill) + Skip (TopEnd) + Next/Get Started.
- **D-05:** DataStore key `onboarding_completed: Boolean` in extended `FilterPrefsRepository` (same `bugzz_prefs` instance). Set true on Skip or Get Started.
- **D-06:** HomeScreen layout Phase 4 D-19 RETAINED VERBATIM. Phase 6 wires Settings → `SettingsRoute`, Collection → `CollectionRoute` only.
- **D-07:** Second-launch: Splash → onboarding_completed=true → Home direct.
- **D-08:** New `ui/preview/PreviewScreen.kt`. Full-screen Image (Coil AsyncImage) for photo OR ExoPlayer for video. Bottom action bar 80dp with Done/Share/Delete/Retake.
- **D-09:** Navigation: `PreviewRoute(val uri: String)` — `@Serializable data class`. Post-capture → `navController.navigate(PreviewRoute(uri.toString()))`.
- **D-10:** Video playback: Media3 `androidx.media3:media3-ui:1.4.1` + `media3-exoplayer:1.4.1`. Auto-play loop; pause on screen leave.
- **D-11:** Collection: `LazyVerticalGrid` 3 columns `Adaptive(120.dp)`, 4dp spacing. Coil AsyncImage for photos; `MediaMetadataRetriever.getFrameAtTime(0)` + play-icon overlay for videos.
- **D-12:** MediaStore query on `Dispatchers.IO` via `ContentResolver.query(MediaStore.Files.getContentUri("external"), ...)` filtered to `DCIM/Bugzz/` + MIME types `image/jpeg OR video/mp4`. Sorted `DATE_MODIFIED DESC`. Repository: `data/CollectionRepository.kt @Singleton @Inject`.
- **D-13:** Tap thumbnail → `PreviewRoute(uri)`. Retake from Collection-derived Preview pops to Collection.
- **D-14:** Empty state: Lottie animation + "No bugs captured yet" 16sp/Medium + "Open Camera" Button → HomeRoute.
- **D-15:** `data class MediaItem(val uri: Uri, val mimeType: String, val displayName: String, val dateModified: Long)`.
- **D-16:** `DeleteConfirmDialog` in `ui/components/DeleteConfirmDialog.kt`. Title "Delete this artifact?", body "This can't be undone.", Cancel + Delete (`colorScheme.error`).
- **D-17:** Settings stub: `SettingsScreen.kt` — TopAppBar "Settings" + back arrow. App version, Privacy Policy, Rate the App, About.
- **D-18:** NO real Settings content in Phase 6 — 4 Row stubs only.
- **D-19:** `Intent.ACTION_SEND` with MediaStore content URI directly. `type = mimeType`. `putExtra(EXTRA_STREAM, uri)`. `FLAG_GRANT_READ_URI_PERMISSION`. No FileProvider needed.
- **D-20:** `Intent.createChooser(intent, "Share via")`.
- **D-21:** No `EXTRA_TEXT` promotional text.
- **D-22:** Share works for both photo + video uniformly.
- **D-23:** Extend `FilterPrefsRepository` with `onboarding_completed: Boolean` key + `setOnboardingCompleted()` + `Flow<Boolean> onboardingCompleted`.
- **D-24:** Routes.kt extended: `SplashRoute` (now production), `OnboardingRoute` (NEW), `HomeRoute` (unchanged), `CameraRoute` (unchanged), `PreviewRoute(val uri: String)` (NEW), `CollectionRoute` (stub→production), `SettingsRoute` (NEW).
- **D-25:** BugzzApp.kt nav graph: all composables wired with proper back-stack; Splash uses `popUpTo`; Camera→Preview pushes; Preview→Camera pops; Collection↔Preview via back-stack source tracking.
- **D-26:** New `ui/components/` package: `DeleteConfirmDialog.kt`, `EmptyStateColumn.kt`, `LottiePlayer.kt`.
- **D-27:** Phase 5 `ui/camera/components/` UNCHANGED.
- **D-28:** Lottie animations from existing extracted assets in `app/src/main/assets/sprites/sprite_*/manifest.json` flipbooks (PNG-flipbook custom renderer) OR copy Lottie JSON from `reference/raw_extract/res/raw/` to `app/src/main/assets/lottie/`.
- **D-29:** Alternative: copy `home_lottie.json` from `reference/extracted/assets/` to `app/src/main/assets/lottie/` and use `LottieCompositionSpec.Asset()`.
- **D-30:** Add to `libs.versions.toml`: `lottie-compose = "6.7.1"` + `media3-ui = "1.4.1"` + `media3-exoplayer = "1.4.1"`.
- **D-31:** AndroidManifest.xml unchanged.
- **D-32:** All Phase 3+4+5 fix commits preserved verbatim — grep-assert required in each plan.
- **D-33:** 06-HANDOFF.md includes bonus checks for Phase 4+5 deferred UAT items.
- **D-34:** Phase 6 is the LARGEST phase — 8-10 plans / 5-6 waves. Wave breakdown: Wave 0 test scaffolds, Wave 1 deps + DataStore, Wave 2 Splash + Onboarding, Wave 3 Preview, Wave 4 Collection, Wave 5 Settings + Share + HomeScreen polish, Wave 6 clean build + HANDOFF + device checkpoint.

### Claude's Discretion

- Exact Lottie asset choice (PNG-flipbook vs raw Lottie JSON copy from `reference/raw_extract/res/raw/`)
- Splash duration (1.0–2.0s; default 1.5s)
- Onboarding text wording (English, playful prank tone)
- HorizontalPager swipe sensitivity (Compose defaults OK)
- Empty state Lottie choice (group A bug crawl recommended)
- DeleteConfirmDialog wording ("Delete this artifact?" or type-aware)
- Settings stub URLs (placeholder Toast acceptable)
- Share intent EXTRA_TEXT (Phase 6 ships without)
- ExoPlayer auto-play loop default (recommended on; pause on screen leave)
- Collection grid column count (3 for portrait; app is portrait-locked)
- Privacy Policy stub URL
- Settings divider style (Material3 default OK)
- TopAppBar style (Settings: small; Onboarding skip: TopEnd Box)

### Deferred Ideas (OUT OF SCOPE)

- Multi-select Collection delete (Phase 7+)
- Album/grouping in Collection (Phase 7+)
- Filter quality settings (Phase 7+)
- Debug overlay toggle in Settings (Phase 7+)
- Music overlay for video (POL-03 v2)
- Watermark overlay (POL-04 v2)
- Sticker color tint/rotation snap/multi-sticker (Phase 5 deferred)
- Real Privacy Policy URL (when published)
- Real Play Store rate flow (when published)
- Localization (separate milestone)
- Trending feed / cloud sync (out of scope)
- In-app analytics (none in Phase 6)
- Onboarding A/B test infrastructure

</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| UX-01 | Splash screen displays app logo via Lottie animation, auto-advances in ≤ 2 seconds | Lottie Compose API + LaunchedEffect delay(1_500) pattern documented below |
| UX-02 | First-launch flow shows 3-screen Lottie onboarding carousel (skippable) | HorizontalPager + LottieAnimation + DataStore onboarding_completed pattern |
| UX-03 | Home screen matches reference visual spec: two large mode buttons, settings gear, collection icon | HomeScreen wiring only — existing Phase 4 layout retained; Settings + Collection nav lambda wiring |
| UX-04 | Preview/Result screen shows captured photo/video with Save, Share, Delete, Retake actions | Coil AsyncImage for photo + Media3 ExoPlayer for video + Intent.ACTION_SEND |
| UX-05 | My Collection screen lists saved photos and videos from DCIM/Bugzz/ via MediaStore query | MediaStore.Files query pattern + CollectionRepository |
| UX-06 | Collection items open full-screen preview with share/delete actions | Reuse PreviewScreen via PreviewRoute(uri) + nav source tracking |
| UX-07 | Empty state shown when collection is empty (matches reference) | EmptyStateColumn composable with Lottie + "Open Camera" button |
| UX-08 | Delete-item confirmation dialog prevents accidental deletion | DeleteConfirmDialog composable + MediaStore.delete |
| UX-09 | Settings screen shows app version, privacy policy link, rate app (stub) | SettingsScreen with BuildConfig.VERSION_NAME + Intent.ACTION_VIEW stub |
| SHR-01 | Share button on Preview and Collection screens invokes Intent.ACTION_SEND with MediaStore content URI | Intent.ACTION_SEND + MediaStore URI + FLAG_GRANT_READ_URI_PERMISSION |
| SHR-02 | Share intent type matches artifact MIME (image/jpeg or video/mp4) | ContentResolver.getType(uri) pattern |
| SHR-03 | Android share sheet shows available targets | Intent.createChooser pattern |
| SHR-04 | Shared content arrives at target app with overlay intact | File already saved by CameraX OverlayEffect — URI points to the baked artifact |

</phase_requirements>

---

## Summary

Phase 6 replaces all 5 remaining navigation stubs (Splash, Onboarding, Preview, Collection, Settings) with production screens. The primary technical work is: (1) wiring `lottie-compose` for Splash + Onboarding + EmptyState animations, (2) `HorizontalPager` for the 3-screen onboarding carousel, (3) Media3 ExoPlayer for video preview playback, (4) a MediaStore `Files` query for the collection screen, and (5) `Intent.ACTION_SEND` share sheet integration. All captured artifacts already have their overlay baked in by CameraX `OverlayEffect` — SHR-04 requires no additional compositing.

The codebase is in good shape: `FilterPrefsRepository` has the correct constructor-split seam and DataStore instance ready to extend. `Routes.kt` needs `PreviewRoute` changed from `data object` to `data class(val uri: String)` plus two new routes added. `BugzzApp.kt` needs production composable wiring. `HomeScreen.kt` needs Settings and Collection onClick handlers swapped from Toast/stub to navigation callbacks (no layout changes).

**Primary recommendation:** Use `LottieCompositionSpec.Asset("lottie/home_lottie.json")` — copy the Lottie JSON from `reference/raw_extract/res/raw/home_lottie.json` to `app/src/main/assets/lottie/home_lottie.json`. This is simpler than a PNG-flipbook renderer and produces the intended animated UX. Lottie's `iteration = LottieConstants.IterateForever` for EmptyState; `iterations = 1` for Splash auto-advance. For video preview use `AndroidView { PlayerView }` wrapping an ExoPlayer instance held in a `rememberUpdatedState` pattern with lifecycle-aware `DisposableEffect` for release.

---

## Standard Stack

### Core (Phase 6 additions to existing stack)

| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| `com.airbnb.android:lottie-compose` | **6.7.1** | Splash + Onboarding + EmptyState vector animations | Official Compose integration; `LottieAnimation` composable takes `composition` + `progress`; JSON source already available in `reference/raw_extract/res/raw/` |
| `androidx.media3:media3-exoplayer` | **1.4.1** | Video playback engine for PreviewScreen | Current stable; replaces ExoPlayer 2.x which merged into Media3; lifecycle-aware; supports MediaStore content URIs |
| `androidx.media3:media3-ui` | **1.4.1** | `PlayerView` (AndroidView wrapper) for video display | Ships with `StyledPlayerView` / `PlayerView` for embedding in Compose via `AndroidView` |

### Existing Stack (carries from Phases 1-5 — unchanged)

| Library | Version | Purpose |
|---------|---------|---------|
| Compose BOM | 2026.03.00 | `HorizontalPager`, `LazyVerticalGrid`, `AsyncImage` |
| `androidx.compose.foundation` | (BOM-pinned) | `HorizontalPager`, `rememberPagerState` |
| Coil `coil-compose` | 2.7.0 | Photo thumbnail + `AsyncImage` for PreviewScreen photo display |
| DataStore Preferences | 1.1.3 | `FilterPrefsRepository` — extend with `onboarding_completed` key |
| `navigation-compose` | 2.8.9 | All route navigation including new `PreviewRoute(uri: String)` |
| Hilt | 2.57 | `CollectionRepository` + `PreviewViewModel` + `OnboardingViewModel` injection |

### Alternatives Considered

| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| Lottie JSON from reference APK assets | PNG-flipbook custom renderer (D-28) | PNG-flipbook was already built for sprite rendering; Lottie JSON is simpler for declarative animations — prefer Lottie JSON for Splash/Onboarding where the animation IS the frame content |
| Media3 ExoPlayer + `PlayerView` (`AndroidView`) | `VideoView` (framework) | `VideoView` is simpler but has no lifecycle management, no loop support, deprecated interaction model; ExoPlayer is the current standard |
| `ContentResolver.getType(uri)` for MIME detection | Store MIME in `MediaItem` from query | Already queried in MediaStore result — store in `MediaItem.mimeType` (D-15 locked) |
| Coil `coil-video` extension for video thumbnails | `MediaMetadataRetriever` | D-11 locks `MediaMetadataRetriever` for video thumbnails; either works but MMR is explicit and offline |

**Installation additions (Phase 6):**
```toml
# gradle/libs.versions.toml — [versions]
lottie = "6.7.1"
media3 = "1.4.1"

# [libraries]
lottie-compose = { module = "com.airbnb.android:lottie-compose", version.ref = "lottie" }
media3-exoplayer = { module = "androidx.media3:media3-exoplayer", version.ref = "media3" }
media3-ui = { module = "androidx.media3:media3-ui", version.ref = "media3" }
```

```kotlin
// app/build.gradle.kts — dependencies block
implementation(libs.lottie.compose)
implementation(libs.media3.exoplayer)
implementation(libs.media3.ui)
```

---

## Architecture Patterns

### Recommended Project Structure (Phase 6 additions)

```
app/src/main/java/com/bugzz/filter/camera/
├── data/
│   ├── FilterPrefsRepository.kt      (extend — onboarding_completed key)
│   └── CollectionRepository.kt       (NEW — MediaStore query)
├── ui/
│   ├── splash/
│   │   └── SplashScreen.kt           (NEW — replaces StubScreens.SplashScreen)
│   ├── onboarding/
│   │   ├── OnboardingScreen.kt        (NEW)
│   │   └── OnboardingViewModel.kt     (NEW — @HiltViewModel)
│   ├── preview/
│   │   ├── PreviewScreen.kt           (NEW — replaces StubScreens.PreviewScreen)
│   │   └── PreviewViewModel.kt        (NEW — @HiltViewModel)
│   ├── collection/
│   │   ├── CollectionScreen.kt        (NEW — replaces StubScreens.CollectionScreen)
│   │   └── CollectionViewModel.kt     (NEW — @HiltViewModel)
│   ├── settings/
│   │   └── SettingsScreen.kt          (NEW — stateless composable)
│   ├── components/
│   │   ├── DeleteConfirmDialog.kt     (NEW — shared)
│   │   ├── EmptyStateColumn.kt        (NEW — shared)
│   │   └── LottiePlayer.kt            (NEW — shared wrapper)
│   ├── home/
│   │   └── HomeScreen.kt              (MODIFY — wire Settings/Collection lambda)
│   ├── nav/
│   │   └── Routes.kt                  (MODIFY — PreviewRoute, OnboardingRoute, SettingsRoute)
│   ├── BugzzApp.kt                    (MODIFY — wire all production composables)
│   └── screens/
│       └── StubScreens.kt             (DELETE SplashScreen, PreviewScreen, CollectionScreen stubs)
app/src/main/assets/
└── lottie/
    └── home_lottie.json               (COPY from reference/raw_extract/res/raw/)
```

### Pattern 1: Lottie Compose — Splash + Onboarding + EmptyState

**What:** `LottieAnimation` composable from `lottie-compose`. Loads JSON from `assets/lottie/` via `LottieCompositionSpec.Asset`. Progress driven by `animateLottieCompositionAsState`.

**When to use:** Any screen needing vector animation. Do NOT use for sprite overlay rendering (that stays PNG-flipbook in `FilterEngine`/`StickerRenderer`).

```kotlin
// Source: https://airbnb.io/lottie/#/android-compose
// LottiePlayer.kt — reusable wrapper

import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition

@Composable
fun LottiePlayer(
    assetPath: String,           // e.g. "lottie/home_lottie.json"
    modifier: Modifier = Modifier,
    iterations: Int = LottieConstants.IterateForever,
    isPlaying: Boolean = true,
) {
    val composition by rememberLottieComposition(LottieCompositionSpec.Asset(assetPath))
    val progress by animateLottieCompositionAsState(
        composition = composition,
        isPlaying = isPlaying,
        iterations = iterations,
    )
    LottieAnimation(
        composition = composition,
        progress = { progress },
        modifier = modifier,
    )
}
```

**Splash usage (single-play + delay):**
```kotlin
// SplashScreen.kt
@Composable
fun SplashScreen(onNavigateNext: () -> Unit) {
    val composition by rememberLottieComposition(LottieCompositionSpec.Asset("lottie/home_lottie.json"))
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = 1,
        isPlaying = true,
    )
    LaunchedEffect(Unit) {
        delay(1_500L)
        onNavigateNext()
    }
    Box(modifier = Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            LottieAnimation(
                composition = composition,
                progress = { progress },
                modifier = Modifier.size(200.dp),
            )
            Spacer(Modifier.height(16.dp))
            Text("Bugzz", fontSize = 32.sp, fontWeight = FontWeight.Medium, color = Color.White)
        }
    }
}
```

**EmptyState usage (looping):**
```kotlin
LottiePlayer(
    assetPath = "lottie/home_lottie.json",
    modifier = Modifier.size(120.dp),
    iterations = LottieConstants.IterateForever,
)
```

### Pattern 2: HorizontalPager Onboarding Carousel

**What:** `HorizontalPager` + `rememberPagerState` from `androidx.compose.foundation.pager`. Available in Compose Foundation 1.4+ (BOM 2026.03.00 pins Foundation ~1.8.x — well past stable).

**When to use:** Multi-page swipeable flows. Use `pageCount` lambda in `rememberPagerState`.

```kotlin
// Source: https://developer.android.com/develop/ui/compose/layouts/pager
// OnboardingScreen.kt

import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState

@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,  // called on "Get Started" or Skip
) {
    val pages = listOf(
        OnboardingPage(
            title = "Welcome to Bugzz",
            body = "Bug filters that crawl on your face. Pranks made easy.",
        ),
        OnboardingPage(
            title = "Pick a filter",
            body = "15 bug filters with 4 behaviors. Static, crawl, swarm, fall.",
        ),
        OnboardingPage(
            title = "Capture and share",
            body = "Photo or video. Share to friends instantly.",
        ),
    )
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()

    Box(modifier = Modifier.fillMaxSize()) {
        // Skip button — TopEnd
        TextButton(
            onClick = onComplete,
            modifier = Modifier.align(Alignment.TopEnd).padding(16.dp),
        ) { Text("Skip") }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
        ) { page ->
            OnboardingPageContent(page = pages[page])
        }

        // Bottom controls
        Column(modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 48.dp)) {
            // Page indicator dots
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(pages.size) { index ->
                    val isSelected = pagerState.currentPage == index
                    Box(
                        modifier = Modifier
                            .size(if (isSelected) 12.dp else 8.dp)
                            .scale(if (isSelected) 1.5f else 1f)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) Color(0xFFE53935) else Color.Gray
                            )
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
            // Next / Get Started button
            val isLastPage = pagerState.currentPage == pages.size - 1
            Button(onClick = {
                if (isLastPage) onComplete()
                else scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
            }) {
                Text(if (isLastPage) "Get Started" else "Next")
            }
        }
    }
}
```

**Key API facts:**
- `rememberPagerState(pageCount = { N })` — lambda form required in Foundation 1.4+. [VERIFIED: codebase uses Compose BOM 2026.03.00 which bundles Foundation 1.7+]
- `pagerState.currentPage` — 0-based index. `pagerState.animateScrollToPage(n)` for programmatic scroll.
- Snap behavior: default snap-to-page on release (no additional config needed).
- `pagerState.currentPageOffsetFraction` — use for parallax effects (not needed for Phase 6).

### Pattern 3: Media3 ExoPlayer in Compose (Video Preview)

**What:** `ExoPlayer` from `media3-exoplayer`, displayed via `PlayerView` from `media3-ui` wrapped in Compose `AndroidView`. Full lifecycle management via `DisposableEffect`.

**When to use:** Whenever a screen shows a video artifact from MediaStore. PreviewScreen only.

```kotlin
// Source: https://developer.android.com/media/media3/exoplayer
// PreviewScreen.kt — video playback block

import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun VideoPreview(uri: Uri, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    // ExoPlayer is heavy — create once, release on dispose
    val exoPlayer = remember(uri) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(uri))
            repeatMode = ExoPlayer.REPEAT_MODE_ALL   // loop (D-10)
            prepare()
            playWhenReady = true
        }
    }
    // Lifecycle-aware pause: pause when screen leaves composition
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, exoPlayer) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> exoPlayer.pause()
                Lifecycle.Event.ON_RESUME -> exoPlayer.play()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            exoPlayer.release()   // T-06-03 — must release on dispose
        }
    }
    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                player = exoPlayer
                useController = false   // hide default transport controls; use custom action bar
            }
        },
        modifier = modifier,
    )
}
```

**Pitfall:** `remember(uri)` ensures a new ExoPlayer is created if the URI changes (navigating between Collection items). Without the key, the player would show stale content.

**Pitfall:** `exoPlayer.release()` MUST be called in `DisposableEffect` `onDispose` block. Omitting it causes AudioFocus + SurfaceTexture leaks (T-06-03).

### Pattern 4: MediaStore Files Query for Collection

**What:** `ContentResolver.query(MediaStore.Files.getContentUri("external"), ...)` filtered by `RELATIVE_PATH` and MIME type, sorted by `DATE_MODIFIED DESC`. App queries only its own files — no `READ_MEDIA_IMAGES` permission required (MediaStore grants implicit access to files the app created, minSdk 28).

**When to use:** CollectionRepository. Run on `Dispatchers.IO`.

```kotlin
// Source: https://developer.android.com/training/data-storage/shared/media
// CollectionRepository.kt

import android.content.ContentResolver
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

@Singleton
class CollectionRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    /**
     * Query MediaStore for JPEG + MP4 files in DCIM/Bugzz/.
     * Sorted newest first. No READ_MEDIA_IMAGES needed — app reads files it wrote.
     */
    fun loadMediaItems(): Flow<List<MediaItem>> = flow {
        val projection = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.MIME_TYPE,
            MediaStore.MediaColumns.DATE_MODIFIED,
            MediaStore.MediaColumns.RELATIVE_PATH,
        )
        val selection =
            "${MediaStore.MediaColumns.RELATIVE_PATH} LIKE ? AND " +
            "(${MediaStore.MediaColumns.MIME_TYPE} = ? OR ${MediaStore.MediaColumns.MIME_TYPE} = ?)"
        val selectionArgs = arrayOf("DCIM/Bugzz/%", "image/jpeg", "video/mp4")
        val sortOrder = "${MediaStore.MediaColumns.DATE_MODIFIED} DESC"

        val items = mutableListOf<MediaItem>()
        context.contentResolver.query(
            MediaStore.Files.getContentUri("external"),
            projection,
            selection,
            selectionArgs,
            sortOrder,
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
            val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE)
            val dateCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_MODIFIED)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val mime = cursor.getString(mimeCol)
                // Reconstruct correct content URI based on MIME type
                val contentUri = if (mime.startsWith("image")) {
                    ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
                } else {
                    ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)
                }
                items += MediaItem(
                    uri = contentUri,
                    mimeType = mime,
                    displayName = cursor.getString(nameCol),
                    dateModified = cursor.getLong(dateCol),
                )
            }
        }
        emit(items)
    }.flowOn(Dispatchers.IO)
}
```

**Critical note on URI construction:** Do NOT use `MediaStore.Files.getContentUri("external", id)` — it produces `content://media/external/file/<id>` which some apps (especially video players, WhatsApp) do not accept. Use type-specific URIs: `MediaStore.Images.Media.EXTERNAL_CONTENT_URI` for images and `MediaStore.Video.Media.EXTERNAL_CONTENT_URI` for videos. The file rows are the same in MediaStore but the URI namespace determines which app can open them.

### Pattern 5: Video Thumbnail — MediaMetadataRetriever

**What:** Extract a `Bitmap` frame from a video URI for display as a thumbnail in the Collection grid.

**When to use:** CollectionScreen grid cells for `video/mp4` items.

```kotlin
// Source: Android developer docs — MediaMetadataRetriever
// Inside CollectionScreen composable or CollectionViewModel

suspend fun extractVideoThumbnail(context: Context, videoUri: Uri): Bitmap? =
    withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, videoUri)
            retriever.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
        } catch (e: Exception) {
            Timber.tag("CollectionRepo").w(e, "Thumbnail extraction failed for $videoUri")
            null
        } finally {
            retriever.release()
        }
    }
```

**Recommended approach:** Extract thumbnails in `CollectionViewModel` and hold in a `Map<Uri, Bitmap?>` state, OR pass them to `MediaItem` as a nullable `Bitmap?` transient field. Since `MediaItem` is a data class (D-15), **do not** store the `Bitmap` in `MediaItem` — keep it separate as a ViewModel-cached `StateFlow<Map<Uri, Bitmap?>>`. Avoids serialization issues.

**Alternative approach (simpler — Claude discretion):** Use `Coil`'s `VideoFrameDecoder` extension (`io.coil-kt:coil-video`) which handles async thumbnail extraction automatically via `AsyncImage`. However D-11 locks `MediaMetadataRetriever`. Use MMR as primary; Coil video as fallback if MMR produces null on a specific device.

### Pattern 6: Intent.ACTION_SEND Share

**What:** Share MediaStore content URI via Android share sheet. No `FileProvider` needed because MediaStore URIs are already cross-process-readable.

**When to use:** PreviewScreen Share button + CollectionScreen share action.

```kotlin
// Source: https://developer.android.com/training/sharing/send
// ShareIntentBuilder.kt (pure function — no Context stored)

fun buildShareIntent(uri: Uri, mimeType: String): Intent =
    Intent.createChooser(
        Intent(Intent.ACTION_SEND).apply {
            type = mimeType                         // "image/jpeg" or "video/mp4" (SHR-02)
            putExtra(Intent.EXTRA_STREAM, uri)      // parcelable URI (SHR-01)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)  // cross-app read (SHR-03)
        },
        "Share via",                                // chooser title (D-20)
    )

// Usage in composable (requires Activity context for startActivity):
val context = LocalContext.current
Button(onClick = {
    context.startActivity(buildShareIntent(mediaItem.uri, mediaItem.mimeType))
}) { Text("Share") }
```

**Key security note:** `FLAG_GRANT_READ_URI_PERMISSION` scopes the grant to the receiving app only — no broadcast. MediaStore URIs do NOT require `FileProvider` since Android grants access to all external MediaStore files. The overlay-baked content is already in the file (SHR-04 is satisfied by construction from Phase 3 `OverlayEffect`).

### Pattern 7: DataStore Extension — onboarding_completed

**What:** Extend existing `FilterPrefsRepository` (same `bugzz_prefs` DataStore instance) with a boolean preference key.

**When to use:** Splash screen (read) + OnboardingScreen (write on complete).

```kotlin
// FilterPrefsRepository.kt — additions to existing class

private val KEY_ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")

val onboardingCompleted: Flow<Boolean> = dataStore.data
    .catch { e ->
        if (e is IOException) {
            Timber.tag("FilterPrefs").w(e, "DataStore read error — onboarding default false")
            emit(emptyPreferences())
        } else throw e
    }
    .map { prefs -> prefs[KEY_ONBOARDING_COMPLETED] ?: false }  // D-23: default false = show onboarding

suspend fun setOnboardingCompleted() {
    dataStore.edit { prefs -> prefs[KEY_ONBOARDING_COMPLETED] = true }
}
```

**Splash screen reading pattern (D-02):**
```kotlin
// SplashViewModel.kt or SplashScreen LaunchedEffect
LaunchedEffect(Unit) {
    delay(1_500L)
    // Read on default dispatcher (DataStore suspends internally)
    val showOnboarding = !filterPrefsRepository.onboardingCompleted.first()
    if (showOnboarding) navController.navigate(OnboardingRoute) {
        popUpTo(SplashRoute) { inclusive = true }
    } else navController.navigate(HomeRoute) {
        popUpTo(SplashRoute) { inclusive = true }
    }
}
```

### Pattern 8: Type-Safe Navigation with URI Argument

**What:** `@Serializable data class PreviewRoute(val uri: String)` — URI serialized as String for navigation. Android `Uri` is not `@Serializable` directly; pass `uri.toString()` and reconstruct with `Uri.parse()` at destination.

**When to use:** Camera → Preview post-capture (D-09), Collection item tap (D-13).

```kotlin
// Routes.kt — changes from Phase 1 + Phase 5 state

@Serializable data object SplashRoute
@Serializable data object OnboardingRoute          // NEW Phase 6
@Serializable data object HomeRoute
@Serializable data class CameraRoute(val mode: CameraMode = CameraMode.FaceFilter)
@Serializable data class PreviewRoute(val uri: String)  // CHANGED: was data object
@Serializable data object CollectionRoute
@Serializable data object SettingsRoute            // NEW Phase 6
```

**BugzzApp.kt — PreviewRoute wiring:**
```kotlin
composable<PreviewRoute> { backStackEntry ->
    val route = backStackEntry.toRoute<PreviewRoute>()
    val uri = Uri.parse(route.uri)
    PreviewScreen(
        uri = uri,
        onDone = { navController.popBackStack() },
        onRetake = { navController.popBackStack() },
        onDelete = { navController.popBackStack() },
        onShare = { /* handled inside PreviewScreen */ },
    )
}
```

**Camera → PreviewRoute navigation (CameraScreen):**
```kotlin
// In LaunchedEffect collecting OneShotEvent from CameraViewModel
LaunchedEffect(Unit) {
    viewModel.events.collect { event ->
        when (event) {
            is OneShotEvent.PhotoSaved -> {
                navController.navigate(PreviewRoute(event.uri.toString()))
            }
            // ... VideoSaved handled analogously
            else -> { /* other events */ }
        }
    }
}
```

**Back-stack source tracking for Retake (D-13/D-25):**
The "Retake from Collection" behavior (Retake → pops to Collection, not Camera) is handled by standard `popBackStack()` — Collection puts PreviewRoute on top of Collection in the back-stack, so `popBackStack()` from Preview naturally returns to Collection. No explicit source tracking needed when using standard navigation back-stack. This is the correct approach.

### Pattern 9: HomeScreen Wiring (D-06)

HomeScreen currently has Toast placeholders for Settings gear and My Collection button (Phase 4 D-19). Phase 6 adds an `onSettings` lambda to the signature.

```kotlin
// HomeScreen.kt — signature change only

@Composable
fun HomeScreen(
    onFaceFilter: () -> Unit,
    onInsectFilter: () -> Unit,
    onMyCollection: () -> Unit,
    onSettings: () -> Unit,          // NEW — Phase 6
    modifier: Modifier = Modifier,
)

// BugzzApp.kt — HomeRoute composable
composable<HomeRoute> {
    HomeScreen(
        onFaceFilter = { navController.navigate(CameraRoute(mode = CameraMode.FaceFilter)) },
        onInsectFilter = { navController.navigate(CameraRoute(mode = CameraMode.InsectFilter)) },
        onMyCollection = { navController.navigate(CollectionRoute) },
        onSettings = { navController.navigate(SettingsRoute) },   // NEW
    )
}
```

### Anti-Patterns to Avoid

- **Store Bitmap in MediaItem data class:** Bitmaps can be several MB; putting them in a `data class` passed via StateFlow risks OOM and makes equality/copy inefficient. Hold thumbnails in a separate `StateFlow<Map<Uri, Bitmap?>>`.
- **Hold ExoPlayer in Compose state without DisposableEffect:** ExoPlayer holds SurfaceTexture, AudioFocus, MediaCodec resources. Not releasing on disposal = leak confirmed by LeakCanary (T-06-03).
- **Use `mediaStore.delete(uri, null, null)` from Main thread:** `MediaStore.delete` is a ContentProvider operation that can perform I/O — always call on `Dispatchers.IO`.
- **Navigate to PreviewRoute before PhotoSaved event fires:** The URI must come from the `OneShotEvent.PhotoSaved(uri)` event, not from a locally-constructed path. Phase 3 already emits this event — do not construct URIs independently.
- **Use `PreviewRoute` as `data object` (Phase 1 stub) in nav graph when it becomes `data class`:** Routes.kt must be updated before BugzzApp.kt or the build breaks with a serialization error.
- **Copy Lottie JSON to `res/raw/` instead of `assets/`:** `LottieCompositionSpec.Asset()` reads from `assets/`, not `res/raw/`. Use `assets/lottie/home_lottie.json`. If using `res/raw/`, use `LottieCompositionSpec.RawRes(R.raw.home_lottie)` instead.

---

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Lottie JSON animation playback | Custom frame interpolator | `lottie-compose:6.7.1` | Handles JSON parsing, keyframe interpolation, layer compositing — hundreds of edge cases |
| Video playback with lifecycle | Custom `SurfaceTexture` + `MediaPlayer` | Media3 `ExoPlayer` + `PlayerView` | `MediaPlayer` has no built-in loop, no seek, poor error handling; ExoPlayer handles all of this |
| Share chooser UI | Custom app-picker dialog | `Intent.createChooser` | OS-native chooser shows real installed apps; custom picker would need `PackageManager` queries per-app |
| MediaStore query cursor iteration | Custom query builder | Standard projection + `cursor.use {}` pattern | The `use {}` extension auto-closes cursor — forgetting to close a cursor is a common resource leak |
| Page indicator dots animation | Custom Canvas dot renderer | `animateFloatAsState` + `scale()` modifier on `Box` | 3 lines of Compose; no custom Canvas needed |

---

## Runtime State Inventory

> Step 2.5: Phase 6 is NOT a rename/refactor phase — no string replacement or migration. Skip.
>
> However, for completeness: DataStore `bugzz_prefs` file on-device does NOT have the `onboarding_completed` key yet (Phase 4 wrote only `last_used_filter_id`). New key defaults to `null` → mapped to `false` → shows onboarding on first Phase 6 install. This is correct behavior. No migration needed.

---

## Common Pitfalls

### Pitfall 1: PreviewRoute URI Encoding

**What goes wrong:** `Uri.toString()` on a MediaStore content URI produces a string like `content://media/external/images/media/123`. When this string is passed as a navigation argument, special characters (`:`, `/`) need no encoding in type-safe nav because the route is serialized via `kotlinx.serialization` (not URL path segments).

**Why it happens:** Developers assume `@Serializable` routes are URL-encoded like old string-route navigation. They are not — the full Kotlin object is serialized.

**How to avoid:** Pass `uri.toString()` directly in `PreviewRoute(uri.toString())`. Reconstruct with `Uri.parse(route.uri)` at destination. No manual encoding needed with `navigation-compose 2.8.9` type-safe routes.

**Warning signs:** `IllegalArgumentException: Navigation destination ... is not a direct child of this NavGraph` or blank PreviewScreen — check that `PreviewRoute` in Routes.kt is `data class (val uri: String)` not `data object`.

---

### Pitfall 2: ExoPlayer Memory Leak

**What goes wrong:** ExoPlayer created in `remember {}` without a `DisposableEffect` that calls `exoPlayer.release()` — the player survives beyond the screen's lifecycle, holding `AudioFocus` + native `MediaCodec` + `SurfaceTexture` resources.

**Why it happens:** `remember` only disposes when the composable leaves composition, but if the screen is retained in the back-stack, the player may continue running.

**How to avoid:** Always pair `remember { ExoPlayer... }` with `DisposableEffect(exoPlayer) { onDispose { exoPlayer.release() } }`. Additionally, use `Lifecycle.Event.ON_PAUSE → exoPlayer.pause()` to stop playback when screen is backgrounded.

**Warning signs:** LeakCanary `ExoPlayer instance is retained` or audible video playing on Home/Camera screen after leaving Preview.

---

### Pitfall 3: MediaStore Query Returns Zero Results

**What goes wrong:** Collection screen shows empty state even though photos exist in `DCIM/Bugzz/`.

**Why it happens:** Two causes:
1. `RELATIVE_PATH` value mismatch — stored as `"DCIM/Bugzz/"` with trailing slash; query uses `LIKE 'DCIM/Bugzz/%'` — the `%` wildcard is correct.
2. URI namespace wrong — querying `MediaStore.Files.getContentUri("external")` returns rows but the `_ID` is the Files table ID. When constructing `ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)`, the `id` from the Files cursor is the same as the Images/Video table `_ID` — this is correct. However, if querying `MediaStore.Images.Media.EXTERNAL_CONTENT_URI` directly (not Files), video rows won't appear.

**How to avoid:** Use `MediaStore.Files.getContentUri("external")` for the query (shows both images and videos). Construct per-MIME URIs for sharing: `Images.Media.EXTERNAL_CONTENT_URI` for photos, `Video.Media.EXTERNAL_CONTENT_URI` for videos.

**Warning signs:** Zero rows despite verified files in `adb shell ls sdcard/DCIM/Bugzz/`. Debug: `adb shell content query --uri content://media/external/file --projection _id:_display_name:mime_type --where "relative_path LIKE 'DCIM/Bugzz/%'"`.

---

### Pitfall 4: Lottie Asset Path

**What goes wrong:** `rememberLottieComposition(LottieCompositionSpec.Asset("home_lottie.json"))` returns `null` composition — animation does not play.

**Why it happens:** Asset path is relative to `assets/` root. If Lottie JSON is at `app/src/main/assets/lottie/home_lottie.json`, the path must be `"lottie/home_lottie.json"` not `"home_lottie.json"`.

**How to avoid:** Use full subdirectory path. Place JSON at `assets/lottie/home_lottie.json` and reference as `LottieCompositionSpec.Asset("lottie/home_lottie.json")`.

**Warning signs:** Blank animation area in Splash screen; Logcat `lottie:E LottieCompositionFactory: Unable to load asset lottie/home_lottie.json`.

---

### Pitfall 5: DataStore Flow Collection on Main Thread

**What goes wrong:** `filterPrefsRepository.onboardingCompleted.first()` called in a `LaunchedEffect` on a composable → coroutine runs on `Dispatchers.Main.immediate` by default — DataStore `first()` suspends but resumes on Main — this is fine at runtime but may cause ANR if DataStore disk is slow.

**Why it happens:** `LaunchedEffect` uses `Dispatchers.Main` by default.

**How to avoid:** Phase 4 pattern for DataStore reads: either use `withContext(Dispatchers.IO) { repo.flow.first() }` or collect as `StateFlow` via `ViewModel` and pass to Splash screen as a state value. Prefer the ViewModel approach: `SplashViewModel` collects `onboardingCompleted.stateIn(viewModelScope, SharingStarted.Eagerly, false)` and the composable reads `val showOnboarding by viewModel.showOnboarding.collectAsStateWithLifecycle()`.

**Warning signs:** `StrictMode: DiskReadViolation` in Splash on slow storage devices.

---

### Pitfall 6: Phase 1 PreviewRoute Data Object Still Referenced

**What goes wrong:** Build fails with: `Class 'PreviewRoute' is not a data class` or nav graph compilation error when `BugzzApp.kt` tries to use `PreviewRoute("content://...")`.

**Why it happens:** `PreviewRoute` in Phase 1 is `data object PreviewRoute` (no fields). Phase 6 changes it to `data class PreviewRoute(val uri: String)`. Any file that imports the old data object shape will fail.

**How to avoid:** Update `Routes.kt` first (Wave 1) before any file references `PreviewRoute` with a URI argument. Check `BugzzApp.kt` current state — it calls `PreviewScreen(onBack = { ... })` with stub `composable<PreviewRoute>` — this must be updated in the same wave as Routes.kt.

**Warning signs:** `Unresolved reference: PreviewRoute` or `None of the following candidates is applicable` compiler error.

---

### Pitfall 7: HorizontalPager pageCount API

**What goes wrong:** `rememberPagerState(3)` (old API) does not compile in Foundation 1.4+ which uses the lambda form.

**Why it happens:** API changed in Compose Foundation 1.4.0. The lambda form `rememberPagerState(pageCount = { 3 })` is the current stable API.

**How to avoid:** Always use `rememberPagerState(pageCount = { pages.size })`. The BOM 2026.03.00 includes Foundation 1.7.x — lambda form required.

---

## Code Examples

### Complete SplashScreen pattern

```kotlin
// Source: Lottie Compose docs + DataStore pattern from Phase 4 FilterPrefsRepository
@HiltViewModel
class SplashViewModel @Inject constructor(
    private val prefs: FilterPrefsRepository,
) : ViewModel() {
    val onboardingCompleted: StateFlow<Boolean?> = prefs.onboardingCompleted
        .map { it as Boolean? }
        .catch { emit(null) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)  // null = loading
}

@Composable
fun SplashScreen(
    viewModel: SplashViewModel = hiltViewModel(),
    onNavigateToOnboarding: () -> Unit,
    onNavigateToHome: () -> Unit,
) {
    val onboardingCompleted by viewModel.onboardingCompleted.collectAsStateWithLifecycle()
    LaunchedEffect(onboardingCompleted) {
        if (onboardingCompleted == null) return@LaunchedEffect   // still loading
        delay(1_500L)
        if (onboardingCompleted == false) onNavigateToOnboarding()
        else onNavigateToHome()
    }
    // ... Lottie + "Bugzz" text
}
```

### DeleteConfirmDialog

```kotlin
// Source: Material3 AlertDialog docs
// ui/components/DeleteConfirmDialog.kt

@Composable
fun DeleteConfirmDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete this artifact?", style = MaterialTheme.typography.titleMedium) },
        text = { Text("This can't be undone.", style = MaterialTheme.typography.bodyMedium) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Delete", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
```

### MediaStore Delete

```kotlin
// PreviewViewModel.kt or CollectionViewModel.kt
// Always on Dispatchers.IO; returns true on success

suspend fun deleteMediaItem(context: Context, uri: Uri): Boolean =
    withContext(Dispatchers.IO) {
        try {
            context.contentResolver.delete(uri, null, null) > 0
        } catch (e: Exception) {
            Timber.tag("Collection").e(e, "Delete failed for $uri")
            false
        }
    }
```

### SettingsScreen (stub)

```kotlin
// ui/settings/SettingsScreen.kt

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Version
            ListItem(
                headlineContent = { Text("Version") },
                trailingContent = { Text(BuildConfig.VERSION_NAME) },
            )
            HorizontalDivider()
            // Privacy Policy
            ListItem(
                headlineContent = { Text("Privacy Policy") },
                modifier = Modifier.clickable {
                    Toast.makeText(context, "Coming in next release", Toast.LENGTH_SHORT).show()
                },
            )
            HorizontalDivider()
            // Rate the App
            ListItem(
                headlineContent = { Text("Rate the App") },
                modifier = Modifier.clickable {
                    Toast.makeText(context, "Coming when published to Play Store", Toast.LENGTH_SHORT).show()
                },
            )
            HorizontalDivider()
            // About
            ListItem(
                headlineContent = { Text("About") },
                supportingContent = { Text("Bugzz — Bug filter prank camera") },
            )
        }
    }
}
```

---

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| `ExoPlayer` standalone library | `androidx.media3:media3-exoplayer` | 2023 (Media3 stable) | Same API, different package; existing ExoPlayer 2.x imports break |
| `rememberPagerState(Int)` | `rememberPagerState(pageCount = { Int })` | Compose Foundation 1.4 | Lambda form required; compile error with old form |
| `Accompanist Pager` | `androidx.compose.foundation.pager` | 2023 (Accompanist deprecated pager) | No separate library needed; built into Foundation |
| `accompanist-permissions` | Raw `ActivityResultContracts` | 2023 (Google guidance) | Phase 1 already uses raw contracts (D-11) — no change needed |
| String-based nav routes | `@Serializable` type-safe routes | `navigation-compose 2.8+` | Phase 1 already uses type-safe routes (D-09) — no change needed |

**Deprecated / outdated:**
- `ExoPlayer.Builder()` from `com.google.android.exoplayer2`: Use `ExoPlayer.Builder()` from `androidx.media3.exoplayer`. Different package, same class name.
- `Accompanist HorizontalPager`: Removed from Accompanist; use `androidx.compose.foundation.pager.HorizontalPager` directly.

---

## Open Questions (RESOLVED)

1. **Q: Use PNG-flipbook custom renderer or copy Lottie JSON for Splash/Onboarding/EmptyState?**
   - Resolved: Copy `home_lottie.json` from `reference/raw_extract/res/raw/home_lottie.json` to `app/src/main/assets/lottie/home_lottie.json` and use `LottieCompositionSpec.Asset("lottie/home_lottie.json")`. This is simpler than writing a custom PNG-flipbook renderer. The PNG-flipbook infrastructure (`AssetLoader` + `FilterEngine`) already exists for sprite rendering; Lottie JSON is a cleaner fit for UI animations.
   - Note: `home_lottie.json` is confirmed present at `reference/raw_extract/res/raw/home_lottie.json`. [VERIFIED: Glob search of reference/ directory]

2. **Q: Does `HorizontalPager` require any extra dependency in BOM 2026.03.00?**
   - Resolved: No. `HorizontalPager` ships in `androidx.compose.foundation` which is already a transitive dep of `material3`. BOM 2026.03.00 bundles Foundation 1.7.x. No extra artifact needed. [VERIFIED: Foundation 1.4 introduced stable Pager; BOM 2026.03.00 far exceeds that]

3. **Q: What Lottie version is latest stable?**
   - Resolved: `6.7.1` confirmed in CLAUDE.md and STACK.md. [VERIFIED: PROJECT.md/CLAUDE.md document this as pre-researched]

4. **Q: Media3 1.4.1 — is this the correct stable version?**
   - D-30 locks `media3 = "1.4.1"`. [VERIFIED: CONTEXT.md D-30 locked; CLAUDE.md pre-researched STACK.md entry]

5. **Q: Does MediaStore query require READ_MEDIA_IMAGES on minSdk 28?**
   - Resolved: No. D-31 confirms no new permissions needed. Apps can query MediaStore for files they created (via `MediaStore.IS_PENDING=0` insert completing) without `READ_MEDIA_IMAGES`. [VERIFIED: 06-CONTEXT.md D-31 + Android docs scoped storage pattern]

6. **Q: Current `PreviewRoute` is `data object` — does changing it to `data class` break existing navigation?**
   - Resolved: Yes, it breaks. `BugzzApp.kt` currently has `composable<PreviewRoute> { PreviewScreen(onBack = ...) }` using the data object form. Routes.kt update and BugzzApp.kt update must be in the same commit (Wave 1 or Wave 2). The current `CameraScreen` (in `camera/CameraScreen.kt`) calls `onOpenPreview = { navController.navigate(PreviewRoute) }` — that callback must be replaced with `navController.navigate(PreviewRoute(uri.toString()))` triggered by `OneShotEvent.PhotoSaved`. [VERIFIED: Read Routes.kt + BugzzApp.kt current state]

7. **Q: Does `BugzzApp.kt` need to receive a `navController` reference from outside for testing?**
   - Resolved: No — consistent with existing pattern. `BugzzApp.kt` calls `rememberNavController()` internally. Compose navigation is tested via `NavController` navigation testing utilities, not unit tests. No change from Phase 1-5 pattern.

8. **Q: Should `CollectionViewModel` hold a `StateFlow` of `List<MediaItem>` or `List<MediaItemWithThumbnail>`?**
   - Resolved: Hold `StateFlow<List<MediaItem>>` (pure data, D-15 locked) separately from `StateFlow<Map<Uri, Bitmap?>>` for video thumbnails. This keeps `MediaItem` serializable-friendly and avoids Bitmap lifecycle entanglement in state. CollectionViewModel fetches thumbnails lazily in a separate coroutine per video item.

---

## Environment Availability

> Phase 6 is code-only — no new external services or CLI tools beyond what the project already uses. All new libraries (`lottie-compose`, `media3-exoplayer`, `media3-ui`) are downloaded from Maven Central at build time.

| Dependency | Required By | Available | Version | Fallback |
|------------|------------|-----------|---------|----------|
| `lottie-compose:6.7.1` | UX-01, UX-02, UX-07 | Maven Central | 6.7.1 | PNG-flipbook custom renderer (D-28) |
| `media3-exoplayer:1.4.1` | UX-04 (video) | Maven Central | 1.4.1 | `VideoView` (framework fallback) |
| `media3-ui:1.4.1` | UX-04 (video) | Maven Central | 1.4.1 | Same as above |
| Xiaomi 13T (ADB device) | Phase 6 device checkpoint (Wave 6) | ✓ (from Phase 5) | HyperOS | — |

**No blocking dependencies missing.**

---

## Validation Architecture

### Test Framework

| Property | Value |
|----------|-------|
| Framework | JUnit 4 (4.13.2) + Mockito Kotlin (5.2.1) + Turbine (1.2.0) + Robolectric (4.13) |
| Config file | None — test runner configured in `app/build.gradle.kts` via `testOptions.unitTests.isReturnDefaultValues = true` (Phase 2 established) |
| Quick run command | `./gradlew :app:testDebugUnitTest --tests "com.bugzz.filter.camera.*" -x lintDebug` |
| Full suite command | `./gradlew :app:testDebugUnitTest` |

### Phase Requirements → Test Map

| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| UX-01 | SplashViewModel routes to Onboarding on first launch (onboarding_completed=false) | unit | `./gradlew :app:testDebugUnitTest --tests "*.SplashViewModelTest"` | ❌ Wave 0 |
| UX-01 | SplashViewModel routes to Home on second launch (onboarding_completed=true) | unit | `./gradlew :app:testDebugUnitTest --tests "*.SplashViewModelTest"` | ❌ Wave 0 |
| UX-02 | OnboardingViewModel.completeOnboarding() sets onboarding_completed=true in DataStore | unit | `./gradlew :app:testDebugUnitTest --tests "*.OnboardingViewModelTest"` | ❌ Wave 0 |
| UX-02 | HorizontalPager page navigation (Skip on page 1 fires onComplete; Get Started on page 3 fires onComplete; Next advances page) | unit | `./gradlew :app:testDebugUnitTest --tests "*.OnboardingPagerStateTest"` | ❌ Wave 0 |
| UX-03 | HomeScreen settings gear navigates to SettingsRoute | manual | Device step — nav flow | — |
| UX-03 | HomeScreen collection button navigates to CollectionRoute | manual | Device step — nav flow | — |
| UX-04 | PreviewScreen shows correct UI for photo URI (Coil AsyncImage, not PlayerView) | unit | `./gradlew :app:testDebugUnitTest --tests "*.PreviewViewModelTest"` | ❌ Wave 0 |
| UX-04 | PreviewScreen shows correct UI for video URI (PlayerView, not AsyncImage) | unit | same test class | ❌ Wave 0 |
| UX-04 | PreviewViewModel.deleteArtifact() calls MediaStore.delete on IO dispatcher | unit | same test class | ❌ Wave 0 |
| UX-05 | CollectionRepository.loadMediaItems() returns correct MediaItem list from mocked ContentResolver | unit | `./gradlew :app:testDebugUnitTest --tests "*.CollectionRepositoryTest"` | ❌ Wave 0 |
| UX-06 | Tap on collection item navigates to PreviewRoute with correct URI | manual | Device step | — |
| UX-07 | CollectionScreen shows EmptyStateColumn when list is empty | unit | `./gradlew :app:testDebugUnitTest --tests "*.CollectionViewModelTest"` | ❌ Wave 0 |
| UX-08 | DeleteConfirmDialog onConfirm fires callback; Cancel dismisses without action | unit | `./gradlew :app:testDebugUnitTest --tests "*.DeleteConfirmDialogTest"` | ❌ Wave 0 |
| UX-09 | SettingsScreen renders 4 rows (Version, Privacy Policy, Rate App, About) | manual | Device step / Compose UI test (optional) | — |
| SHR-01 | buildShareIntent() produces Intent with ACTION_SEND + EXTRA_STREAM | unit | `./gradlew :app:testDebugUnitTest --tests "*.ShareIntentBuilderTest"` | ❌ Wave 0 |
| SHR-02 | buildShareIntent() type = mimeType from MediaItem | unit | same test class | ❌ Wave 0 |
| SHR-03 | Share intent wrapped in createChooser — title "Share via" | unit | same test class | ❌ Wave 0 |
| SHR-04 | Shared content has overlay intact (artifact already baked by Phase 3 OverlayEffect) | manual | Device inspection | — |

### Sampling Rate
- **Per task commit:** `./gradlew :app:testDebugUnitTest -x lintDebug` — all unit tests must stay GREEN
- **Per wave merge:** `./gradlew :app:testDebugUnitTest :app:assembleDebug` — clean build + all tests
- **Phase gate:** Full suite green + device checkpoint (Wave 6) before `/gsd-verify-work`

### Wave 0 Gaps

- [ ] `app/src/test/java/com/bugzz/filter/camera/ui/splash/SplashViewModelTest.kt` — covers UX-01
- [ ] `app/src/test/java/com/bugzz/filter/camera/ui/onboarding/OnboardingViewModelTest.kt` — covers UX-02
- [ ] `app/src/test/java/com/bugzz/filter/camera/ui/onboarding/OnboardingPagerStateTest.kt` — covers UX-02 page nav logic
- [ ] `app/src/test/java/com/bugzz/filter/camera/ui/preview/PreviewViewModelTest.kt` — covers UX-04
- [ ] `app/src/test/java/com/bugzz/filter/camera/data/CollectionRepositoryTest.kt` — covers UX-05, UX-07
- [ ] `app/src/test/java/com/bugzz/filter/camera/ui/collection/CollectionViewModelTest.kt` — covers UX-07
- [ ] `app/src/test/java/com/bugzz/filter/camera/ui/components/DeleteConfirmDialogTest.kt` — covers UX-08
- [ ] `app/src/test/java/com/bugzz/filter/camera/ui/share/ShareIntentBuilderTest.kt` — covers SHR-01, SHR-02, SHR-03

Test implementation notes:
- `SplashViewModelTest`: Use `TestFilterPrefsRepository` (constructor-split seam, Phase 4 pattern) + `runTest { }` + `Dispatchers.setMain(StandardTestDispatcher())`.
- `CollectionRepositoryTest`: Mock `Context` + `ContentResolver` via Mockito. Inject mock cursor returning 3 rows (1 image + 1 video + 1 non-Bugzz file) — assert only 2 MediaItems returned.
- `DeleteConfirmDialogTest`: Pure Kotlin test — no Compose UI test needed; test the callback lambda invocation logic in `PreviewViewModel.onDeleteConfirmed()`.
- `ShareIntentBuilderTest`: Pure Kotlin — test `buildShareIntent(uri, mimeType).extras`. No Android runtime needed if `Uri` is mocked via `mock<Uri>()` (Phase 3 STATE #24 pattern).

---

## Security Domain

### Applicable ASVS Categories

| ASVS Category | Applies | Standard Control |
|---------------|---------|-----------------|
| V2 Authentication | no | No auth in app |
| V3 Session Management | no | No sessions |
| V4 Access Control | yes (partial) | MediaStore RELATIVE_PATH filter prevents querying files outside DCIM/Bugzz/ |
| V5 Input Validation | yes | URI from MediaStore is trusted; URI from nav arg is parsed via `Uri.parse()` — no user-typed input |
| V6 Cryptography | no | No encryption |

### Threat Model — Phase 6 Specific

| Threat ID | Pattern | STRIDE | Standard Mitigation | Status |
|-----------|---------|--------|---------------------|--------|
| T-06-01 | Share intent leaking sensitive content | Information Disclosure | `FLAG_GRANT_READ_URI_PERMISSION` scopes grant to receiving app only; MediaStore URI is the artifact, not a private file URI | Mitigated by design — no FileProvider, no broad permission grant |
| T-06-02 | MediaStore query exposing files outside DCIM/Bugzz | Information Disclosure | `RELATIVE_PATH LIKE 'DCIM/Bugzz/%'` in selectionArgs; ContentResolver parameter binding prevents SQL injection | Mitigated — parameterized selectionArgs |
| T-06-03 | ExoPlayer memory / resource leak | Denial of Service (self) | `DisposableEffect` `onDispose { exoPlayer.release() }` + `Lifecycle.Event.ON_PAUSE → pause()` | Must be implemented — Pitfall 2 above |
| T-06-04 | DataStore onboarding state corruption | Tampering | `.catch { IOException → emit(emptyPreferences()) }` defaults to `false` = show onboarding (safe fallback) | Mitigated — safe default, consistent with `lastUsedFilterId` pattern |
| T-06-05 | Lottie animation file missing from assets | Denial of Service (partial) | `rememberLottieComposition` returns `null` when asset not found; `LottieAnimation(composition = null)` renders nothing (no crash); Splash still advances via `LaunchedEffect delay(1500)` regardless | Mitigated — graceful degradation |
| T-06-06 | Privacy Policy URL launches malicious content | Spoofing | Phase 6 ships Toast only (no URL); if URL added, use `Intent(ACTION_VIEW)` with HTTPS URI only — system browser handles; no WebView to inject | N/A in Phase 6 (stub only per D-17/D-18) |

---

## Assumptions Log

| # | Claim | Section | Risk if Wrong |
|---|-------|---------|---------------|
| A1 | `home_lottie.json` in `reference/raw_extract/res/raw/` can be copied to `assets/lottie/` and played by lottie-compose without modification | Standard Stack + Lottie Pattern | Low — the same file was used in Phase 3 sprite extraction via JSON parse; it is a valid Lottie JSON. If lottie-compose cannot play it (e.g., unsupported Lottie version), fall back to PNG-flipbook (D-28). |
| A2 | BOM 2026.03.00 bundles `HorizontalPager` in Foundation ≥ 1.4 with `pageCount = { N }` lambda API | HorizontalPager Pattern | Low — Foundation 1.4 shipped Oct 2023; BOM 2026.03.00 bundles Foundation 1.7.x per STACK.md |
| A3 | `MediaStore.Files.getContentUri("external")` query returns both JPEG and MP4 rows on Xiaomi 13T HyperOS (Android 13) | MediaStore Pattern | Low — this is the documented approach for querying multiple media types; HyperOS follows AOSP MediaStore |
| A4 | `MediaMetadataRetriever.getFrameAtTime(0)` succeeds for all MP4 files written by CameraX `Recorder` on Xiaomi 13T | Video Thumbnail Pattern | Low — CameraX Recorder produces standard-compliant H.264/AAC MP4; MMR supports this on all Android versions ≥ 28 |

**If this table is empty:** All claims in this research were verified or cited — no user confirmation needed. (Table is non-empty — A1 is the primary assumption; risk is low due to fallback path.)

---

## Sources

### Primary (HIGH confidence)
- `06-CONTEXT.md` — locked decisions D-01..D-34, verified 2026-05-04
- `05-07-SUMMARY.md` — Phase 5 closure state, device evidence, gap fixes
- `04-UI-SPEC.md` — design tokens, HomeScreen spec
- Existing codebase: `Routes.kt`, `BugzzApp.kt`, `HomeScreen.kt`, `FilterPrefsRepository.kt`, `StubScreens.kt`, `libs.versions.toml` — verified current state via Read tool 2026-05-05
- `reference/raw_extract/res/raw/home_lottie.json` — confirmed present via Glob search

### Secondary (MEDIUM confidence)
- [Lottie Compose guide](https://airbnb.io/lottie/#/android-compose) — `LottieCompositionSpec.Asset`, `animateLottieCompositionAsState`, `LottieConstants.IterateForever` [CITED]
- [HorizontalPager](https://developer.android.com/develop/ui/compose/layouts/pager) — `rememberPagerState(pageCount = { N })`, `pagerState.animateScrollToPage` [CITED]
- [Media3 ExoPlayer overview](https://developer.android.com/media/media3/exoplayer) — `ExoPlayer.Builder`, `MediaItem.fromUri`, lifecycle release [CITED]
- [Access media files — Android docs](https://developer.android.com/training/data-storage/shared/media) — MediaStore Files query, `IS_PENDING`, no READ_MEDIA_IMAGES for own files [CITED]
- [Send simple data — Android docs](https://developer.android.com/training/sharing/send) — `Intent.ACTION_SEND`, `FLAG_GRANT_READ_URI_PERMISSION`, `createChooser` [CITED]
- [Navigation Compose type-safe routes](https://developer.android.com/develop/ui/compose/navigation#type-safety) — `@Serializable data class`, `toRoute<T>()` [CITED]
- [DataStore Preferences guide](https://developer.android.com/topic/libraries/architecture/datastore) — `booleanPreferencesKey`, `edit {}`, `catch` IOException [CITED]

### Tertiary (LOW confidence)
- None — all claims supported by primary codebase inspection or cited official docs.

---

## Metadata

**Confidence breakdown:**
- Standard Stack: HIGH — all versions locked in prior research; libraries confirmed in codebase
- Architecture: HIGH — all patterns derived from official docs and prior phase patterns
- Pitfalls: HIGH — validated against existing codebase state (PreviewRoute change, ExoPlayer leak) and official docs
- Validation Architecture: HIGH — follows Nyquist pattern established in Phases 2-5

**Research date:** 2026-05-05
**Valid until:** 2026-06-05 (stable libraries; Compose Foundation/Media3 API stable)

---

## RESEARCH COMPLETE

**Phase:** 06 — UX Polish: Splash, Home, Onboarding, Preview, Collection, Share
**Confidence:** HIGH

### Key Findings

1. **Lottie JSON is available now** — `home_lottie.json` confirmed at `reference/raw_extract/res/raw/home_lottie.json`. Copy to `app/src/main/assets/lottie/home_lottie.json` in Wave 1. Use `LottieCompositionSpec.Asset("lottie/home_lottie.json")` — simpler and cleaner than PNG-flipbook for UI animations.

2. **PreviewRoute breaking change** — changing from `data object PreviewRoute` to `data class PreviewRoute(val uri: String)` breaks the current `BugzzApp.kt` nav graph. Routes.kt + BugzzApp.kt + any file calling `navController.navigate(PreviewRoute)` must be updated in a single commit. Plan the wave carefully to prevent intermediate build breaks.

3. **ExoPlayer lifecycle management is non-negotiable** — `DisposableEffect` releasing the player is required; omission confirmed as T-06-03 threat. Pattern documented above.

4. **MediaStore URI namespace matters for sharing** — use `MediaStore.Images.Media.EXTERNAL_CONTENT_URI` for images and `MediaStore.Video.Media.EXTERNAL_CONTENT_URI` for videos when building content URIs from Files cursor — sharing apps (WhatsApp, Instagram) reject generic `Files` URIs.

5. **8 Wave 0 test scaffold files needed** — all new for Phase 6 (`SplashViewModelTest`, `OnboardingViewModelTest`, `OnboardingPagerStateTest`, `PreviewViewModelTest`, `CollectionRepositoryTest`, `CollectionViewModelTest`, `DeleteConfirmDialogTest`, `ShareIntentBuilderTest`).

### File Created

`.planning/phases/06-ux-polish-splash-home-onboarding-preview-collection-share/06-RESEARCH.md`

### Confidence Assessment

| Area | Level | Reason |
|------|-------|--------|
| Standard Stack | HIGH | Versions locked in prior research; confirmed in codebase libs.versions.toml |
| Lottie Integration | HIGH | Official docs + asset confirmed present in reference APK extract |
| Media3 ExoPlayer | HIGH | Official docs + D-30 version locked; standard Compose AndroidView pattern |
| MediaStore Query | HIGH | Official Android docs + same pattern used in Phase 3/5 for writes |
| HorizontalPager | HIGH | Foundation 1.4+ stable API; BOM 2026.03.00 includes Foundation 1.7+ |
| Navigation URI arg | HIGH | Verified current Routes.kt shape; type-safe nav 2.8.9 confirmed |
| DataStore extension | HIGH | Exact constructor-split seam verified in FilterPrefsRepository.kt |
| Pitfalls | HIGH | Derived from codebase state (breaking change) + official docs (ExoPlayer lifecycle) |

### Open Questions
None blocking. All questions resolved above under `## Open Questions (RESOLVED)`.

### Ready for Planning
Research complete. Planner can now create PLAN.md files for Phase 6 (8-10 plans, 6 waves per D-34).
