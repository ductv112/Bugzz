# Phase 6: UX Polish — Splash, Home, Onboarding, Preview, Collection, Share - Context

**Gathered:** 2026-05-04
**Status:** Ready for planning
**Source:** Auto-locked recommended defaults (user delegated full autonomous run per memory `feedback_autonomy.md`)

<domain>
## Phase Boundary

Replace ALL navigation stubs with production screens matching the reference visual spec, giving users complete end-to-end journey from first launch through capture → preview → save artifacts → re-open Collection → share to social apps. Scope: 5 production screens (Splash, Onboarding, Preview/Result, Collection, Settings) + HomeScreen polish wiring + Share intent integration. UX-01..09 + SHR-01..04 = 13 requirements.

**Out of scope:** Per-filter sound effects (PROJECT.md), trending feed/cloud sync (PROJECT.md), multi-select delete (Phase 7+ if requested), music overlay for video (POL-03 v2), watermark (POL-04 v2), in-app rating real implementation (UX-09 stub only — no Play Store), localization (LOC v2 milestone), beauty filter (out of scope), MediaStore query optimizations beyond basic LazyVerticalGrid (Phase 7).

</domain>

<decisions>
## Implementation Decisions

### Splash Screen (UX-01)
- **D-01:** **Lottie splash auto-advance in ≤1.5s** (under 2s target). Lottie animation source: reuse `home_lottie.json` group A (7 frames, 360×360) extracted in Phase 4 — no new asset extraction. Use Compose `LottieAnimation` composable from new dep `com.airbnb.android:lottie-compose:6.7.1`. Layout: full-screen Lottie centered + app name "Bugzz" 32sp/Medium below + auto-finish via `LaunchedEffect(Unit) { delay(1_500); navigate(next) }`.
- **D-02:** **Conditional first-launch routing.** Splash queries DataStore `onboarding_completed: Boolean`:
  - First launch (false / unset) → `OnboardingRoute` (3-screen carousel)
  - Subsequent launches → `HomeRoute` directly
  Splash is on the navigation stack but `popUpTo(HomeRoute|OnboardingRoute) { inclusive = true }` so Back from Home doesn't return to Splash.

### Onboarding (UX-02)
- **D-03:** **3-screen Lottie carousel via `HorizontalPager` (Compose Foundation 2026.04+).** Page content:
  - Page 1: Lottie animation top + "Welcome to Bugzz" 24sp/Medium + body "Bug filters that crawl on your face. Pranks made easy."
  - Page 2: Lottie + "Pick a filter" + "15 bug filters with 4 behaviors. Static, crawl, swarm, fall."
  - Page 3: Lottie + "Capture and share" + "Photo or video. Share to friends instantly."
  Lotties reuse `home_lottie.json` groups A/B/C extracted Phase 4.
- **D-04:** **Bottom controls:** PageIndicator dots (3 dots, active dot scale 1.5x + accent-record `#FFE53935` fill) + Skip button TopEnd (skips to Home, sets onboarding_completed=true) + Next/Get Started button bottom-right (last page = "Get Started").
- **D-05:** **DataStore key `onboarding_completed: Boolean`** in extended FilterPrefsRepository (same DataStore instance — single Preferences file `bugzz_prefs`). Set true on either Skip or Get Started tap. Subsequent launches Splash → Home direct (D-02).

### HomeScreen Polish (UX-03)
- **D-06:** **HomeScreen layout from Phase 4 D-19 RETAINED VERBATIM.** Phase 6 polishes WIRING only:
  - Settings gear (top-right) — Phase 4 had Toast placeholder; Phase 6 navigates to `SettingsRoute`.
  - My Collection button (bottom) — Phase 4 stub; Phase 6 navigates to `CollectionRoute`.
  - Face Filter button — unchanged (navigates to `CameraRoute(FaceFilter)`).
  - Insect Filter button — unchanged (navigates to `CameraRoute(InsectFilter)`, Phase 5 enabled).
  No new layout changes. ContentDescription strings preserved.
- **D-07:** **Second-launch behavior:** Splash → onboarding_completed=true → Home direct (no onboarding). UX-09 success criterion #5.

### Preview/Result Screen (UX-04)
- **D-08:** **New Compose screen `ui/preview/PreviewScreen.kt`.** Shows captured photo OR video (auto-detect by MIME type via ContentResolver.getType(uri)). Layout: full-screen Image (Coil AsyncImage) for photo OR `AndroidView(VideoView)` / ExoPlayer for video. Bottom action bar 80dp Surface with 4 IconButton + label:
  - **Done** (Phase 6 rename from "Save" — file already saved by Phase 3 D-31 IS_PENDING transaction; "Done" semantically correct)
  - **Share** — launches `Intent.ACTION_SEND` with content URI + MIME (D-17)
  - **Delete** — shows AlertDialog confirmation (D-14) → on confirm `MediaStore.delete(uri, null, null)` → pop nav back to Camera
  - **Retake** — pop nav back to Camera (file remains saved — Done semantics)
- **D-09:** **Navigation:** `PreviewRoute(val uri: String)` — `@Serializable data class`. Triggered after capture from Camera → `navController.navigate(PreviewRoute(uri.toString()))`. Phase 6 wires the navigation; previously no navigation post-capture (just Toast).
- **D-10:** **Video playback:** Use Media3 `androidx.media3:media3-ui:1.4.1` with ExoPlayer for inline video preview. Add to libs.versions.toml. Auto-play loop on entry; pause on screen leave.

### Collection Screen (UX-05/06/07)
- **D-11:** **New `ui/collection/CollectionScreen.kt`.** LazyVerticalGrid (3 columns, fixed `Adaptive(120.dp)`), square thumbnail cells (1:1 aspect), 4dp grid spacing. Coil AsyncImage thumbnails for photos; for videos: `Box` with thumbnail (extracted via `MediaMetadataRetriever.getFrameAtTime(0)`) + center play-icon overlay (24dp white triangle).
- **D-12:** **MediaStore query on `Dispatchers.IO`** via `ContentResolver.query(MediaStore.Files.getContentUri("external"), projection, selection, selectionArgs, "${MediaStore.MediaColumns.DATE_MODIFIED} DESC")` filtered to `RELATIVE_PATH = 'DCIM/Bugzz/'` and MIME types `image/jpeg` OR `video/mp4`. Repository pattern: `data/CollectionRepository.kt @Singleton @Inject` exposing `Flow<List<MediaItem>>`.
- **D-13:** **Tap thumbnail** → navigates to `PreviewRoute(uri)` (reuses Phase 6 D-08 PreviewScreen). Same Done/Share/Delete/Retake actions; "Retake" returns to Collection (not Camera) when entered from Collection — track via nav back-stack.
- **D-14:** **Empty state (UX-07):** When MediaItem list empty, render centered Column with:
  - Lottie animation (small bug, reuse home_lottie group A)
  - Text "No bugs captured yet" 16sp/Medium
  - "Open Camera" Button → navigates to HomeRoute → user picks Face/Insect Filter
- **D-15:** **MediaItem data class:** `data class MediaItem(val uri: Uri, val mimeType: String, val displayName: String, val dateModified: Long)`. Pure data — no Bitmap held (Coil handles).

### Delete Confirmation Dialog (UX-08)
- **D-16:** **Shared `DeleteConfirmDialog` composable** in `ui/components/DeleteConfirmDialog.kt`. Title 16sp/Medium "Delete this artifact?", body 14sp/Normal "This can't be undone.", Cancel TextButton + Delete TextButton (`colorScheme.error` red text). Used by Preview Delete + Collection Delete + future Settings clear-all.

### Settings Screen (UX-09 stub)
- **D-17:** **New `ui/settings/SettingsScreen.kt`** with TopAppBar "Settings" + back arrow. Body: vertical Column with:
  - **App version** Row: "Version" + `BuildConfig.VERSION_NAME` (e.g. "1.0.0-debug")
  - **Privacy Policy** Row: clickable, opens `Intent(Intent.ACTION_VIEW, Uri.parse("https://bugzz.example.com/privacy"))` OR shows Toast "Coming in next release" if no URL yet
  - **Rate the App** Row: clickable Toast "Coming when published to Play Store"
  - **About** Row: read-only "Bugzz — Bug filter prank camera"
- **D-18:** **NO Settings content beyond stub for Phase 6** — actual user prefs (filter quality, debug overlay toggle) deferred to v2 milestone. Phase 6 ships the screen + navigation + 4 Row stubs.

### Share Intent (SHR-01..04)
- **D-19:** **`Intent.ACTION_SEND` with MediaStore content URI directly.** No FileProvider needed since MediaStore content URIs (`content://media/external/...`) are already cross-app-readable. Set:
  - `type = mimeType` (image/jpeg OR video/mp4)
  - `putExtra(Intent.EXTRA_STREAM, uri)` (Parcelable Uri)
  - `addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)`
- **D-20:** **`Intent.createChooser(intent, "Share via")`** to give user app picker. Don't try to detect installed targets — let Android show all available image/video receivers (WhatsApp, Instagram, TikTok, FB, Messenger, Zalo if installed).
- **D-21:** **No `EXTRA_TEXT` promotional text** — Phase 6 ships clean URI-only share. Phase 6+ may add "Made with Bugzz" footer if user requests.
- **D-22:** **Share works for both photo + video** uniformly. PreviewScreen handler reads MIME from MediaStore + builds intent + launches chooser. Verify on Xiaomi 13T device handoff.

### DataStore Extension
- **D-23:** **Extend `FilterPrefsRepository`** (Phase 4 single instance) with new key `onboarding_completed: Boolean` + `setOnboardingCompleted()` + `Flow<Boolean> onboardingCompleted` API. Same DataStore instance — single Preferences file. NO new repository class needed.
  - Default false on first launch
  - Splash queries via `repository.onboardingCompleted.first()` (Dispatchers.IO)

### Navigation Architecture
- **D-24:** **Routes.kt extended:**
  ```kotlin
  @Serializable data object SplashRoute  // Phase 1 stub - now production
  @Serializable data object OnboardingRoute  // NEW
  @Serializable data object HomeRoute  // unchanged
  @Serializable data class CameraRoute(val mode: CameraMode = CameraMode.FaceFilter)  // unchanged
  @Serializable data class PreviewRoute(val uri: String)  // NEW (Phase 1 had stub PreviewRoute)
  @Serializable data object CollectionRoute  // Phase 1 stub - now production
  @Serializable data object SettingsRoute  // NEW
  ```
- **D-25:** **BugzzApp.kt nav graph:** every composable wired with proper back-stack behavior. Splash uses `popUpTo` to clear back-stack on first launch. Camera → PreviewRoute uses default push; Preview → Camera uses popBackStack. Collection ↔ Preview cross-nav handled correctly (Retake from Collection-derived Preview pops to Collection).

### New Compose Components Package
- **D-26:** **New shared components in `ui/components/`:**
  - `DeleteConfirmDialog.kt` (D-16)
  - `EmptyStateColumn.kt` (D-14 generalized)
  - `LottiePlayer.kt` wrapper around lottie-compose `LottieAnimation` with default looping config
- **D-27:** **Phase 5 components in `ui/camera/components/` UNCHANGED:**
  - `RecordButton.kt` (Phase 5 D-07)
  - `RecordingIndicator.kt` (Phase 5 D-08)

### Lottie Asset Strategy
- **D-28:** **Lottie animations source from existing extracted assets in `app/src/main/assets/sprites/sprite_*/manifest.json` flipbooks** for Splash + Onboarding + EmptyState. NOT new Lottie JSONs (would require new asset extraction). For full polish, Phase 6+ may swap in proper Lottie JSONs (would re-extract reference APK Lottie sources Phase 4 left untouched). Phase 6 ships with PNG-flipbook-rendered-as-Lottie via custom composable that reads frame_NN.png from assets and renders sequentially. Acceptable — same UX, different format.
- **D-29:** **Alternative if cleaner:** copy 1-2 Lottie JSON files (`spider_prankfilter.json`, `home_lottie.json`) from `reference/extracted/assets/` to `app/src/main/assets/lottie/` and use lottie-compose `LottieCompositionSpec.Asset()` to play. Choose at Wave 0 implementation time based on simpler path.

### New Dependencies
- **D-30:** **Add to `gradle/libs.versions.toml`:**
  - `lottie-compose = "6.7.1"` → `com.airbnb.android:lottie-compose:6.7.1` (Splash + Onboarding + EmptyState animations)
  - `media3-ui = "1.4.1"` → `androidx.media3:media3-ui:1.4.1` (Preview video playback)
  - `media3-exoplayer = "1.4.1"` → `androidx.media3:media3-exoplayer:1.4.1` (Preview video playback)
- **D-31:** **AndroidManifest.xml unchanged** — RECORD_AUDIO + CAMERA + POST_NOTIFICATIONS already declared Phase 1. No new permissions for Phase 6 (MediaStore reads work without READ_MEDIA_IMAGES on minSdk 28+ for files-app-wrote — Phase 1 D-12 lazy permission pattern preserved).

### Phase 5 Inheritance + Fix Preservation
- **D-32:** **All Phase 3+4+5 fix commits preserved verbatim.** Phase 6 plans must grep-assert:
  - `isCapturing` guard (Phase 3 dafc21e)
  - `bindJob.cancel()` (Phase 3 9abbd0b)
  - `OneShotEvent.FilterLoadError` (Phase 3 6ff00e0)
  - `captureFlash` inside onSuccess (Phase 3 4e94591)
  - `require(frameCount > 0)` (Phase 3 b7f74cf)
  - `assetLoader.preload(def.assetDir)` (Phase 4 514410c)
  - `isRecording` guard (Phase 5 D-26)
  - `cameraMode = CameraMode.InsectFilter` propagation (Phase 5 gap-01 37b7a17)
  - `StickerRenderer.setPreviewSize` + matrix reset (Phase 5 gap-02 de27c4e)

### Phase 4 + 5 Deferred UAT Items (folded into 06-HANDOFF as opportunistic close)
- **D-33:** **06-HANDOFF.md runbook includes bonus checks for:**
  - Phase 4 multi-face 2-person scene
  - Phase 4 fps subjective 30s
  - Phase 5 pinch + rotate gestures (visual)
  - Phase 5 sticker survives flip + orientation (visual)
  - Phase 5 audio sync subjective playback
  - Phase 5 fresh-install RECORD_AUDIO lazy permission dialog
  - Phase 5 ThermalMonitor stress observation
  - Phase 5 05-gaps-02 visual axis polish (sticker drag direction)

### Plan Budget Note
- **D-34:** **Phase 6 is the LARGEST production phase** — 13 requirements + 5 screens + Lottie + Media3 + Share intent + Settings. Planner may need 8-10 plans across 5-6 waves. Wave-by-wave structure recommended:
  - Wave 0: Nyquist test scaffolds for new components
  - Wave 1: Lottie + Media3 deps catalog + DataStore extension (onboarding_completed key)
  - Wave 2: Splash + Onboarding (sequential due to nav graph)
  - Wave 3: Preview screen (photo + video playback)
  - Wave 4: Collection screen + Repository + Empty state
  - Wave 5: Settings + Share intent + HomeScreen polish wiring
  - Wave 6: Clean build + 06-HANDOFF + device checkpoint

### Claude's Discretion
- Exact Lottie asset choice (PNG-flipbook custom render vs raw Lottie JSON copy) — pick at Wave 0 based on simpler path
- Splash duration (1.0–2.0s acceptable; default 1.5s)
- Onboarding text wording exact strings (English; match PROJECT.md tone — playful prank app vibe)
- HorizontalPager swipe sensitivity / animation duration (Compose defaults OK)
- Empty state Lottie animation choice (group A bug crawl recommended)
- DeleteConfirmDialog exact wording ("Delete this artifact?" recommended; user may prefer "Delete photo?" / "Delete video?" type-aware)
- Settings stub URLs (placeholder Toast acceptable for personal-use app)
- Share intent EXTRA_TEXT (Phase 6 ships without; Phase 6+ may add)
- ExoPlayer auto-play loop default (recommended on; pause on screen leave)
- Collection grid column count (3 recommended for portrait; 5 for landscape — but app is portrait-locked)
- Privacy Policy stub URL choice
- Settings divider style between rows (Material3 default OK)
- TopAppBar style (small or center-aligned — Settings small recommended; Onboarding skip button TopEnd in Box)

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Project specs
- `.planning/PROJECT.md` — locked stack + English UI + portrait-locked + offline-only
- `.planning/REQUIREMENTS.md` §UX Screens (UX-01..09) + §Share (SHR-01..04) — Phase 6 primary scope
- `.planning/ROADMAP.md` §Phase 6 — goal + 5 success criteria
- `.planning/STATE.md` §Accumulated Context — execution learnings from Phases 1-5

### Prior phases — MANDATORY reading
- `.planning/phases/05-video-recording-audio-insect-filter-free-placement-mode/05-CONTEXT.md` (D-19 new files, D-26 fix preservation, recording API + InsectFilter mode patterns)
- `.planning/phases/05-video-recording-audio-insect-filter-free-placement-mode/05-07-SUMMARY.md` (Phase 5 closure + 2 inline gap fixes)
- `.planning/phases/05-video-recording-audio-insect-filter-free-placement-mode/05-VERIFICATION.md` (5 deferred UAT items — opportunistic close in Phase 6)
- `.planning/phases/05-video-recording-audio-insect-filter-free-placement-mode/05-HUMAN-UAT.md` (deferred items list)
- `.planning/phases/04-filter-catalog-picker-face-filter-mode/04-CONTEXT.md` (D-19 HomeScreen layout — Phase 6 retains; D-25 DataStore pattern — Phase 6 extends)
- `.planning/phases/04-filter-catalog-picker-face-filter-mode/04-UI-SPEC.md` (design tokens INHERITED verbatim by Phase 6)
- `.planning/phases/03-first-filter-end-to-end-photo-capture/03-CONTEXT.md` (D-31..D-36 ImageCapture + MediaStore — Phase 6 PreviewScreen consumes saved URI)
- `.planning/phases/01-foundation-skeleton/01-CONTEXT.md` (D-09/D-10 nav routes pattern — Phase 6 extends; D-12 RECORD_AUDIO declared)

### Research base
- `.planning/research/STACK.md` (lottie-compose + Media3 ExoPlayer pre-catalogued; CameraX 1.6 + Compose BOM 2026.04.00 stack carries)
- `.planning/research/ARCHITECTURE.md` §6 (patterns — StateFlow + UDF) — Phase 6 ViewModels follow

### Compose / Android external docs
- [Lottie Compose guide](https://airbnb.io/lottie/#/android-compose) — animation playback, LottieCompositionSpec
- [HorizontalPager (Compose Foundation 1.7+)](https://developer.android.com/develop/ui/compose/layouts/pager) — onboarding carousel
- [Media3 ExoPlayer Compose](https://developer.android.com/media/media3/exoplayer/composables) — video preview playback
- [MediaStore query for own files](https://developer.android.com/training/data-storage/shared/media#access-own-media) — Collection no permission needed
- [Intent.ACTION_SEND share guide](https://developer.android.com/training/sharing/send) — share sheet integration
- [navigation-compose type-safe routes](https://developer.android.com/develop/ui/compose/navigation#type-safety) — Phase 1 D-09 pattern preserved

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets (from Phases 1-5)
- `com.bugzz.filter.camera.ui.home.HomeScreen` — Phase 4 D-19 + Phase 5 D-19 enable; Phase 6 polishes onClick handlers (Settings/Collection nav)
- `com.bugzz.filter.camera.ui.home.CameraMode` — Phase 4 enum, unchanged
- `com.bugzz.filter.camera.ui.nav.Routes` — Phase 1 + 4 + 5 routes; Phase 6 extends with OnboardingRoute, SettingsRoute + adds `uri: String` arg to PreviewRoute
- `com.bugzz.filter.camera.ui.BugzzApp` — nav graph; Phase 6 wires Onboarding, Settings, Preview, Collection composables
- `com.bugzz.filter.camera.ui.screens.StubScreens` — Phase 1 stubs for SplashScreen, PreviewScreen, CollectionScreen — Phase 6 DELETES these (replaces with production)
- `com.bugzz.filter.camera.data.FilterPrefsRepository` — Phase 4 DataStore wrapper; Phase 6 extends with onboarding_completed key (single Preferences file)
- `com.bugzz.filter.camera.di.DataModule` — Hilt provides DataStore<Preferences>; unchanged
- `com.bugzz.filter.camera.ui.camera.CameraViewModel.onShutterTapped` — emits OneShotEvent.PhotoSaved(uri); Phase 6 CameraScreen LaunchedEffect navigates to PreviewRoute(uri.toString()) on this event
- Phase 5 RecordButton + RecordingIndicator + AlertDialog (`ui/camera/components/`) — UNCHANGED

### Established Patterns (replicate)
- **Hilt @Singleton @Inject + constructor-split** (Phase 2 STATE #14)
- **DataStore Preferences** (Phase 4 D-25 — extend, single instance per project)
- **Compose Material3 + 04-UI-SPEC tokens** — color/spacing/typography all inherited
- **Coil 2.7 for thumbnail / image loading** — Phase 4 D-07 wired
- **kotlinx.serialization.json** for nav route args (Phase 1 D-09)
- **AndroidManifest unchanged** — Phase 6 adds no permissions

### Integration Points
- `gradle/libs.versions.toml` — Phase 6 adds lottie-compose 6.7.1 + media3 1.4.1 (D-30)
- `app/build.gradle.kts` — implementation entries for new deps
- `app/src/main/assets/lottie/` — possible new directory for Lottie JSONs OR reuse `assets/sprites/sprite_*/` PNG flipbooks (D-28/29 Claude discretion)
- New packages: `ui/splash/`, `ui/onboarding/`, `ui/preview/`, `ui/collection/`, `ui/settings/`, `ui/components/`, `data/CollectionRepository.kt`
- `MainActivity.kt` — unchanged; nav graph entry point still BugzzApp composable

</code_context>

<specifics>
## Specific Ideas

- User runs **stop-test per phase on Xiaomi 13T** (memory `feedback_cadence.md`). Phase 6 follows Phase 3+4+5 pattern: chain → Waves N-1 autonomous → stop at device checkpoint Wave 6.
- User explicitly delegated full autonomy ("Tiếp" continuing autonomous chain). Per memory `feedback_autonomy.md` — recommended defaults for all gray areas above. Mid-execution agent questions → recommended.
- Phase 6 is the LAST UX-feature phase. After this phase + device PASS: Phase 7 perf + cross-OEM matrix is the final phase before milestone close.
- Phase 4 + 5 deferred UAT items (8 total — multi-face, fps, pinch/rotate visual, sticker survival, audio sync, fresh-install perm, thermal stress, gap-02 visual polish) — Phase 6 device handoff is opportunistic close.
- Phase 6 plan budget IS the largest. Planner may produce 8-10 plans / 5-6 waves. If plan budget exceeded, planner can split into Phase 6a (Splash/Onboarding/Home) + Phase 6b (Preview/Collection/Settings/Share) — but Phase 6 currently kept whole.

</specifics>

<deferred>
## Deferred Ideas

- **Multi-select Collection delete** — Phase 7+ if user wants bulk deletion.
- **Album/grouping in Collection** (by date, by filter type) — Phase 7+ UX polish.
- **Filter quality settings** (resolution toggle 720p/1080p) — Phase 7 settings expansion.
- **Debug overlay toggle in Settings** (D-27 Phase 3 deferred) — Phase 6 doesn't add it; Phase 7+ if helpful.
- **Music overlay for video** — POL-03 v2.
- **Watermark overlay** — POL-04 v2.
- **Sticker color tint, rotation snap, multi-sticker** — Phase 5 deferred.
- **Real Privacy Policy URL + content** — Phase 6 stub only; needs real URL when published.
- **Real Play Store rate flow** — Phase 6 stub Toast only; needs Play Store availability.
- **Localization (LOC-01..02)** — separate milestone.
- **Trending feed / cloud sync** — out of scope per PROJECT.md.
- **In-app analytics** — none in Phase 6 (privacy + offline-only intent).
- **Onboarding A/B test infrastructure** — Phase 6 ships static 3-page flow.
- **Phase 4 + 5 deferred UAT items** — folded into 06-HANDOFF as bonus checks per D-33.

</deferred>

---

*Phase: 06-ux-polish-splash-home-onboarding-preview-collection-share*
*Context gathered: 2026-05-04 (auto-locked recommended defaults per user delegation)*
