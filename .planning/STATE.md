---
gsd_state_version: 1.0
milestone: v1.0
milestone_name: milestone
status: in_progress
stopped_at: Plan 06-02 complete — Wave 1 deps + Lottie asset + DataStore onboarding key landed; commit 03ff140; suite GREEN at 170/24 ignored/0 (-3 ignored vs 06-01 = 3 onboarding tests un-ignored & passing); 9 D-32 grep-asserts pass; APK assembles; Wave 2 (Splash + Onboarding) unblocked
last_updated: "2026-05-05T15:30:00.000Z"
progress:
  total_phases: 7
  completed_phases: 5
  total_plans: 42
  completed_plans: 36
  percent: 86
---

# State: Bugzz

**Last updated:** 2026-04-19

## Project Reference

**Core Value:** Smooth live AR preview with bug sprites tracking face landmarks. If the live preview stutters or bugs don't stick to the face, everything else is meaningless.

**Current Focus:** Phase 06 — UX Polish (Splash, Home, Onboarding, Preview, Collection, Share)

**Milestone:** v1 — feature-parity clone of `com.insect.filters.funny.prank.bug.filter.face.camera` v1.2.7, MINUS monetization and i18n.

## Current Position

Phase: 05 (Video Recording + Audio + Insect Filter Free-Placement Mode) — COMPLETE
Plan: 7 of 7 complete

- **Phase:** 6
- **Plan:** 06-02 complete (2 of 8) → next Plan 06-03 (Wave 2: Splash + Onboarding)
- **Previous plan:** 06-02 complete — Wave 1 lottie-compose 6.7.1 + media3-exoplayer/ui 1.4.1→1.9.0 (transitive resolution to CameraX-pulled 1.9.0) on classpath; home_lottie.json (746491 B, sha256 5d3cfc5c…) byte-identical at app/src/main/assets/lottie/; FilterPrefsRepository extended in-place with onboardingCompleted: Flow<Boolean> + setOnboardingCompleted() (no-arg, idempotent) + KEY_ONBOARDING_COMPLETED booleanPreferencesKey; T-06-04 mitigation (.catch IOException → false) present; 3 FilterPrefsRepositoryTest @Ignore('Plan 06-02') cases un-ignored & GREEN (7/7 in that class); full suite 170/24 ignored/0; D-32 invariants intact. Commit 03ff140.
- **Status:** In Progress (Wave 2 unblocked)
- **Progress:** [█████████░] 86%

### Phase Map

```
Phase 1: Foundation & Skeleton                            [ complete ]
Phase 2: Camera + Face Detection + Coord Validation       [ gap closure — 6/6 base + 1/3 gap plans done (gaps-02, gaps-03 remaining) ]
Phase 3: First Filter End-to-End + Photo Capture          [ pending ]
Phase 4: Filter Catalog + Picker + Face Filter Mode       [ complete ]
Phase 5: Video Recording + Audio + Insect Filter Mode     [ complete ]
Phase 6: UX Polish (Splash/Home/Onboarding/...)           [ pending ]
Phase 7: Performance & Device Matrix                      [ pending ]
```

## Performance Metrics

(Populated during execution)

| Metric | Value | Target |
|--------|-------|--------|
| Phases complete | 0/7 | 7 |
| v1 requirements complete | 0/67 | 67 |
| Current phase plans | —/— | — |
| Phase 02 P01 | 3m 17s | 3 tasks | 5 files |
| Phase 02 P02 | 10m 13s | 3 tasks | 5 files |
| Phase 02 P03 | 8m 07s | 3 tasks | 7 files |
| Phase 02 P04 | 14m 56s | 3 tasks | 7 files |
| Phase 02 P05 | 5m 39s | 3 tasks | 7 files |
| Phase 02 Pgaps-01 | 18m | 3 tasks | 6 files |
| Phase 03 P01 | 1178s | 2 tasks | 24 files |
| Phase 03 P02 | 482 | 2 tasks | 7 files |
| Phase 03 P03 | session-continuation | 5 tasks | 13 files |
| Phase 03 P04 | 862 | 3 tasks | 7 files |
| Phase 04 P01 | 720 | 2 tasks | 69 files |
| Phase 04 P02 | 600 | 2 tasks | 8 files |
| Phase 04 P03 | 480 | 1 task | 9 files |
| Phase 04 P04 | 1500 | 2 tasks | 12 files |
| Phase 04 P05 | 333 | 3 tasks | 7 files |
| Phase 04 P06 | 178 | 2 tasks | 2 files |
| Phase 04-filter-catalog-picker-face-filter-mode P07 | 344 | 2 tasks | 8 files |
| Phase 04-filter-catalog-picker-face-filter-mode P08 | ~5400 | 4 tasks | 8 files |
| Phase 05 P01 | 297 | 2 tasks | 7 files |
| Phase 05 P02 | 976 | 3 tasks | 13 files |
| Phase 05 P03 | 938 | 3 tasks | 11 files |
| Phase 05 P04 | 273 | 2 tasks | 3 files |
| Phase 05 P05 | 420 | 1 tasks | 3 files |
| Phase 05 P06 | 180 | 1 tasks | 3 files |
| Phase 05 P07 | ~2100 | 4 tasks | 10 files |
| Phase 06 P01 | 600 | 3 tasks | 9 files |
| Phase 06 P02 | 400s | 3 tasks | 5 files |

## Accumulated Context

### Key Decisions Locked in Research

1. **UI Toolkit:** Jetpack Compose (not Views) — CameraX-Compose stable in 1.6; greenfield + solo dev + 6 screens is Compose sweet spot. (SUMMARY.md Resolution #1)
2. **Face Tracking:** ML Kit Face Detection contour mode (NOT Face Mesh) — 2D sprites don't need 478-point mesh; Face Detection has first-class `MlKitAnalyzer` + `trackingId`. (SUMMARY.md Resolution #2)
3. **Rendering:** 2D Canvas via `OverlayEffect` (NO Filament) — PBR engine is cargo-cult for sprite blits; escalation path is custom GL `CameraEffect`, never Filament. (SUMMARY.md Resolution #3)
4. **CameraX version:** 1.6.0 uniform across all artifacts — first version with stable `camera-effects` + `camera-compose`.
5. **ML Kit model:** Bundled (`com.google.mlkit:face-detection:16.1.7`) — offline first-launch, no Play Services model-download race.
6. **Persistence:** MediaStore for captures, DataStore for prefs — no Room DB for MVP.

### Key Decisions During Execution

34. **[Phase 05-gaps-01] InsectFilterViewModel.bind must pass cameraMode=CameraMode.InsectFilter explicitly:** Omitting the `cameraMode` argument caused `controller.bind(lifecycle)` to default to `CameraMode.FaceFilter`, attaching `MlKitAnalyzer` in InsectFilter mode (CPU waste) and running `FilterEngine.onDraw` instead of `StickerRenderer.onDraw` (wrong render path). Fix at commit `37b7a17`. Pattern: any ViewModel that binds `CameraController` for a non-FaceFilter mode MUST pass its mode explicitly — never rely on default. Verified: ZERO FaceTracker logcat lines in Insect mode post-fix; StickerRenderer active.

35. **[Phase 05-gaps-02] StickerRenderer coordinate transform: Compose preview px → OverlayEffect buffer canvas px requires axis-swap + scale + front-cam mirror:** `StickerState.offset` is in portrait Compose preview px (e.g. 1220×2712 on Xiaomi 13T). `OverlayEffect` buffer canvas is landscape 1920×1080. Transform: (1) axis swap (portrait Y → landscape X; portrait X → landscape Y) for 90° CW PreviewView rotation, (2) scale by `bufferW/previewH` and `bufferH/previewW`, (3) front-cam mirror inversion on mapped axis. `StickerRenderer.setPreviewSize(w,h)` must be called when InsectFilterScreen lays out. `onDraw` resets matrix to identity before applying custom transform — does NOT inherit `sensorToBufferTransform` from OverlayEffectBuilder. Fix at commit `de27c4e`. Visual drag direction polish (axis-mirror fine-tune) deferred to Phase 7 cross-OEM matrix.

36. **[Phase 06-02] FilterPrefsRepository extended in-place with second key (onboarding_completed) — D-23 single-instance rule:** Same `@Singleton class FilterPrefsRepository` now holds both `lastUsedFilterId: Flow<String>` (Phase 4) and `onboardingCompleted: Flow<Boolean>` (Phase 6) keys, sharing one DataStore instance + one .catch IOException handler pattern. NOT a separate OnboardingPrefsRepository class (rejected per D-23 to prevent drift). Setter signature: `suspend fun setOnboardingCompleted()` (no Boolean arg — single-shot mark-complete, idempotent on repeat). Default value `false` so first launch routes to OnboardingScreen; T-06-04 mitigation mirrors T-04-01 verbatim (`if (e is IOException) emit(emptyPreferences())` → maps to false). **Pattern: future preference keys land as additional `val …: Flow<T>` + `suspend fun set…()` on this same class; only add a new repo when keys' lifecycle/scope differs.** (06-02-SUMMARY.md)

37. **[Phase 06-02] media3-exoplayer/ui declared at 1.4.1 but resolved to 1.9.0 by Gradle conflict resolution (CameraX 1.6.0 transitive):** Catalog pins `media3 = "1.4.1"` for both `media3-exoplayer` and `media3-ui`, but CameraX 1.6.0 transitively pulls `androidx.media3:media3-common:1.9.0` (and friends) for VideoCapture+muxer integration. Mixing 1.4.1 + 1.9.0 transitive across `media3-common` would crash at runtime due to ABI mismatch on shared `media3-common`. Gradle's resolver auto-upgrades both to 1.9.0 (correct + safe). Catalog declarations stay at 1.4.1 as floor; resolver harmonizes. **Pattern: when introducing media3 modules alongside CameraX, always allow Gradle to resolve up — do NOT exclude CameraX-transitive media3 deps. Public API surface from 1.4.1→1.9.0 is backward-compatible for the basic ExoPlayer.Builder + setMediaItem(Uri) flow Plan 06-04 PreviewScreen will use.** (06-02-SUMMARY.md Deviation 1)

38. **[Phase 06-02] Lottie composition asset path = `lottie/home_lottie.json` (under assets/), NOT res/raw/:** RESEARCH Pitfall 4 — production code calls `LottieCompositionSpec.Asset("lottie/home_lottie.json")` which resolves relative to `assets/` root, so the file MUST live at `app/src/main/assets/lottie/home_lottie.json`. Placing it at `res/raw/` would require `LottieCompositionSpec.RawRes(R.raw.home_lottie)` (different API) — DO NOT mix the two paths. Single 746491 B JSON shared across Splash + Onboarding pages 1-3 + EmptyState (D-29). SHA256 byte-identical to `reference/raw_extract/res/raw/home_lottie.json` source verified at copy time (no JSON re-formatting drift). (06-02-SUMMARY.md)

31. **[Phase 04-08 gap-01] AssetLoader must use assetDir not filterId as cache key + path source:** `AssetLoader.preload(assetDir)` and `get(assetDir, frameIdx)` take the full asset-relative path (e.g. `"sprites/sprite_spider"`) — NOT the `filterId` (e.g. `"spider_nose_static"`). FilterCatalog D-30 shares 4 sprite directories across 15 filters; using `filterId` caused `FilterLoadFailed` toast on first launch (manifest.json path was `sprites/spider_nose_static/manifest.json` — a path that does not exist). Fix at commit `514410c`. Cache key scoped to `assetDir` means shared sprites decode once for all catalog entries — D-30 perf win realized on device. **Pattern: any class that bridges `FilterDefinition.id` (logical) ↔ `FilterDefinition.assetDir` (filesystem) must use `assetDir` for I/O operations.**

32. **[Phase 04-08 device] FilterEngine multi-face draws=N logcat pattern confirmed on Xiaomi 13T:** Logcat tag `OverlayEffect` emits `filter=<id> frame=<n> faces=<f> draws=<d>` per frame. SWARM `draws=8` (8 instances within D-14 soft cap 20), FALL `draws=5` consistent with max-8 instance cap. Primary face `faces=1` for single-person verification. Pattern is the authoritative real-device render health signal for Phase 5+ debugging.

33. **[Phase 04-08 device] DataStore round-trip verified via PID change:** Force-stop (PID 9059) → relaunch (PID 15588) → logcat shows correct `filter=bugB_swarm` immediately. Confirms `FilterPrefsRepository.writeLastUsed` persists to disk (not just in-memory) and `CameraViewModel.bind` reads it before first frame render. No delay or fallback to DEFAULT_FILTER_ID observed.

30. **[Phase 04-05] FilterPrefsRepository constructor-split pattern + DataStore last-used filter:** `FilterPrefsRepository` uses `internal constructor(DataStore<Preferences>)` as test seam + `@Inject constructor(@ApplicationContext Context)` delegating to production `Context.preferencesDataStore` (top-level delegate outside class body per Pitfall 5). `CameraViewModel.onSelectFilter(id)` fires two independent coroutines: (a) DataStore write on default dispatcher, (b) preload+setFilter on cameraExecutor. Optimistic `selectedFilterId` update happens synchronously before both coroutines so picker highlight responds on same frame as tap (D-17). (04-05-SUMMARY.md)

29. **[Phase 04-04] BehaviorState config fields thread BehaviorConfig through tick loops:** `BehaviorState.Swarm.targetCount` + `BehaviorState.Fall.maxInstances/spawnInterval*/gravityFactor` fields pre-populated from `FilterDefinition.behaviorConfig` at `createBehaviorState` time in FilterEngine. BugBehavior tick signature unchanged — reads config from state, not companion constants. (04-04-SUMMARY.md)

28. **[Phase 04-04] perFaceState eagerly seeded before bitmap null-check:** State entries created for all tracked faces before `assetLoader.get() ?: return` so Crawl/Swarm/Fall state accumulates during preload. Fixes BehaviorStateMapTest.getOrPut_createsFreshStateForNewTrackingId. (04-04-SUMMARY.md Rule 1)

27. **[Phase 04-04] FilterCatalogTest.kt DELETED — superseded by FilterCatalogExpandedTest:** Phase 3 test asserted size==2 and byId("ant_on_nose_v1"); catalog is now 15 entries with all new IDs. FilterCatalogExpandedTest (8 tests) fully covers CAT-01/CAT-02 for the 15-entry roster. (04-04-SUMMARY.md)

26. **[Phase 04-01] D-05 PNG density formula incorrect — replaced with MIN_BYTES threshold:** Research formula `buf.length / (w * h * 4) > 0.10` compares compressed PNG size against uncompressed RGBA budget. Even content-rich sprites on transparent backgrounds fail (spider: 0.0007, ant: 0.023). Fix: `MIN_BYTES = 2000` absolute threshold — a fully-transparent PNG of any size compresses to <500B; any real sprite ≥2KB. All 58 frames extracted with 0 rejected. (04-01-SUMMARY.md Rule 1 deviation)

23. **[Phase 03-04] imageCaptureFactory constructor-split seam in CameraController:** `imageCaptureFactory: () -> ImageCapture` added as 6th internal primary constructor param (mirrors Phase 2 `providerFactory` pattern). `@Inject` secondary constructor delegates with production `ImageCapture.Builder()`. Tests inject mock ImageCapture for takePicture stubbing. Canonical pattern for future CameraX use-case test seams. (03-04-SUMMARY.md)
24. **[Phase 03-04] mock<Uri>() for Android Uri in plain JVM unit tests:** `Uri.parse(string)` returns null under plain JVM (no Android runtime). `OneShotEvent.PhotoSaved(uri)` has non-null `val uri: Uri` — causes NPE inside viewModelScope coroutine. Fix: use `mock<Uri>()` (Mockito mock is non-null). Pattern for all future tests that need an Android Uri without Robolectric. (03-04-SUMMARY.md)
25. **[Phase 03-04] Dispatchers.setMain + async{events.first()} pattern for viewModelScope Channel testing:** `viewModelScope` uses `Dispatchers.Main` which is unavailable under plain JVM. Fix: `Dispatchers.setMain(StandardTestDispatcher())` in `@Before`. To collect Channel events produced inside `viewModelScope.launch`, start `async { flow.first() }` before calling the action, then `advanceUntilIdle()`, then `deferred.await()`. Direct `flow.first()` after `advanceUntilIdle()` hangs because the channel is already drained. (03-04-SUMMARY.md)

19. **[Phase 03-03] Flipbook uses absolute timestampNanos (not relative to setFilter):** `frameIdx = (tsNanos / frameDurationNanos) % frameCount`. Deterministic, stable phase; eliminates start-time sentinel 0L collision issue. (03-03-SUMMARY.md)
20. **[Phase 03-03] Reference APK Lottie JSON base64 extraction pattern:** Reference sprites are embedded as base64 PNGs inside Lottie JSON `"p":` fields, not standalone PNG files. Extraction requires JSON parse + base64 decode (Node.js script). Ant = 35 frames from `home_lottie.json` InsectFilter layer; Spider = 23 frames from `spider_prankfilter.json`. (03-03-SUMMARY.md)
21. **[Phase 03-03] FilterEngine draw order in OverlayEffectBuilder — Claude's Discretion (D-27):** FilterEngine.onDraw called FIRST; DebugOverlayRenderer.draw second. Sprite renders under debug bbox/contour grid in DEBUG builds; in release DebugOverlayRenderer is a no-op so only order matters for sprite z-order. (03-03-SUMMARY.md)
22. **[Phase 03-03] Robolectric asset serving requires isIncludeAndroidResources=true + fixtures in src/main/assets:** ShadowArscAssetManager10 cannot serve `src/test/resources/` via `AssetManager.open()` even with `includeAndroidResources=true`. Fixtures must be in `src/main/assets/` so AGP merges them into the test asset bundle. (03-03-SUMMARY.md)

17. **[Phase 03-02] TrackerResult return type over drainRemovedIds():** `assign()` returns `TrackerResult(tracked, removedIds)` atomically — FaceDetectorClient gets both tracked faces and dropped IDs from one call. No side-channel drain needed; cleaner contract per Research §Open Questions Q1 resolution. (03-02-SUMMARY.md)
18. **[Phase 03-02] FaceDetectorClientTest MlKitContext limitation — tracker contract tested directly:** `FaceDetectorClient` cannot be unit-constructed (calls `FaceDetection.getClient()` → `MlKitContext.getInstance()` → `IllegalStateException`). Test verifies `BboxIouTracker.assign()` contract directly; structural ordering (tracker before smoother) enforced by sequential consumer body + compile-time Hilt codegen. Pattern: ML Kit SDK wrapper classes cannot be unit-constructed without full Android runtime; test the algorithm contract instead. (03-02-SUMMARY.md Deviation 1)

1. **[Phase 02-01] Nyquist-TDD Wave 0 gate:** 4 failing unit-test files for CAM-03/04/05/06/09 land before any `feat(02-...)` commit; SUT classes land in Plans 02-03 and 02-04. (02-01-SUMMARY.md)
2. **[Phase 02-01] Testability seam pattern:** Android-Handler-dependent SDK wrappers (`OverlayEffectBuilder`) expose config surface as companion `const val`/`val` (`TARGETS`, `QUEUE_DEPTH`) so contracts are unit-testable without Robolectric. (02-01-SUMMARY.md)
3. **[Phase 02-01] Provider-factory seam on CameraController:** Plan 02-04 must add constructor default param `providerFactory: suspend (Context) -> ProcessCameraProvider = { ProcessCameraProvider.awaitInstance(it) }` so `CameraControllerTest` can un-`@Ignore`. (02-01-SUMMARY.md)
4. **[Phase 02-02] Compose BOM 2026.04.00 not yet published:** Plan prescribed bumping composeBom 2026.03.00 → 2026.04.00 (per CLAUDE.md Executive Recommendation + 02-RESEARCH.md), but that BOM is not on Google Maven as of 2026-04-19. Reverted to 2026.03.00; revisit when BOM lands. (Rule 1 auto-fix in 02-02-SUMMARY.md)
5. **[Phase 02-02] compileSdk 35 → 36:** CameraX 1.6.0 AAR metadata requires compileSdk>=36. Bumped `app/build.gradle.kts compileSdk = 36` (targetSdk stays 35 per CLAUDE.md lock). Phase 1's pre-armed `android.suppressUnsupportedCompileSdk=36` in `gradle.properties` silences the cross-version warning. (Rule 3 auto-fix in 02-02-SUMMARY.md)
6. **[Phase 02-02] Hilt CameraModule with named single-thread Executors:** `@Named("cameraExecutor")` (thread `BugzzCameraExecutor`) + `@Named("renderExecutor")` (thread `BugzzRenderExecutor`) — D-18 threading model. Named threads surface in logcat/profiler for Xiaomi 13T debugging. (02-02-SUMMARY.md)
7. **[Phase 02-03] FaceDetectorOptions R8 obfuscation workaround:** Published ML Kit AAR obfuscates accessors to `zza()..zzg()` — Plan 01's Kotlin-property-syntax assertions (`opts.performanceMode`, `opts.isTrackingEnabled`, etc.) don't compile. Rewrote `FaceDetectorOptionsTest` to combine `equals()` against expected options (definitive 6-field gate via ML Kit's equals override) + `toString()` substring assertions per field for diagnostic clarity. Observed toString format: `FaceDetectorOptions{landmarkMode=1, contourMode=2, classificationMode=1, performanceMode=1, trackingEnabled=true, minFaceSize=0.15}`. Rule 3 auto-fix. (02-03-SUMMARY.md)
8. **[Phase 02-03] Compile-unblock placeholder stubs for Plan 02-04 SUTs:** `OverlayEffectBuilderTest.kt` + `CameraControllerTest.kt` (committed by Plan 02-01) reference SUTs not landing until Plan 02-04, blocking the whole test sourceset from compiling. Shipped minimal placeholder `OverlayEffectBuilder.kt` (wrong `TARGETS=0` + `QUEUE_DEPTH=-1` to keep `OverlayEffectBuilderTest` intentionally RED at runtime) and `CameraController.kt` (NotImplementedError body; `CameraControllerTest` methods stay `@Ignore`d). Both stubs carry forward-pointing KDoc to Plan 02-04. **Plan 02-04 MUST fully REPLACE both files, not edit on top.** Rule 3 auto-fix. (02-03-SUMMARY.md)
9. **[Phase 02-03] FaceDetectorClient detector pipeline landed:** `@Singleton` with `@Inject constructor(@Named("cameraExecutor") cameraExecutor)` (D-18). `buildOptions()` companion returns D-15 exact options. `createAnalyzer()` returns `MlKitAnalyzer(listOf(detector), COORDINATE_SYSTEM_SENSOR, cameraExecutor, consumer)` (D-17). Consumer runs `smoother.retainActive(activeIds)` FIRST, maps to `SmoothedFace` via per-contour 1€ filter keyed on trackingId, writes `AtomicReference<FaceSnapshot>` (D-19). Emits `Timber.tag("FaceTracker").v("t=%d id=%s bb=%d,%d contours=%d", ...)` per face — never raw landmark coord lists (T-02-06). `onLensFlipped()` clears smoother state (D-25 / PITFALLS #6). OneEuroFilterTest (4/4) + FaceDetectorOptionsTest (1/1) GREEN; OverlayEffectBuilderTest + CameraControllerTest remain RED / @Ignored. (02-03-SUMMARY.md)
10. **[Phase 02-04] Render + camera lifecycle layer landed:** DebugOverlayRenderer (@Singleton Canvas drawer, FIRST line `if (!BuildConfig.DEBUG) return` per D-02/T-02-02, iterates ALL faces per D-23/PITFALLS #13) + OverlayEffectBuilder (@Singleton @Inject(faceDetector, renderer); real TARGETS=PREVIEW|VIDEO|IMAGE + QUEUE_DEPTH=0; dedicated HandlerThread("BugzzRenderThread"); setOnDrawListener body calls `canvas.setMatrix(frame.sensorToBufferTransform)` BEFORE `renderer.draw` per CAM-07/PITFALLS #5) + CameraController (@Singleton @Inject; `overlayEffect = overlayEffectBuilder.build()` ONCE in property init per D-25; bind() builds UseCaseGroup with EXACTLY 4 use cases + 1 effect per CAM-03/CAM-06; ImageAnalysis with STRATEGY_KEEP_ONLY_LATEST + ResolutionSelector preferred Size(720,1280) + FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER per CAM-05/D-16; VideoCapture with MirrorMode.MIRROR_MODE_ON_FRONT_ONLY + Quality.HD; flipLens calls faceDetector.onLensFlipped() BEFORE bind per PITFALLS #6; setTargetRotation updates all 4 use cases per D-08; startTestRecording saves MP4 to DCIM/Bugzz — NO `.withAudioEnabled()` per D-05/T-02-06 strict grep-verified). All 9 Phase 2 Nyquist unit tests GREEN. (02-04-SUMMARY.md)
11. **[Phase 02-04] CameraX 1.6.0 research-drift Rule 3 auto-fixes:** (a) `ProcessCameraProvider.awaitInstance(ctx)` does not exist in published CameraX 1.6.0 AAR (research §A5 was wrong); only static `getInstance(ctx): ListenableFuture<ProcessCameraProvider>`. Added minimal private `suspend fun <T> ListenableFuture<T>.await(): T` extension using suspendCancellableCoroutine + addListener(Runnable::run). (b) `VideoCapture.MIRROR_MODE_ON_FRONT_ONLY` does not exist; constants live on `androidx.camera.core.MirrorMode`. (c) Guava's `listenablefuture:1.0` stub jar is rewritten by Gradle to `9999.0-empty-to-avoid-conflict-with-guava` whenever guava is transitively present (camera-core brings it); must depend on full `com.google.guava:guava:33.3.1-android` directly for ListenableFuture compile-time type resolution. (02-04-SUMMARY.md Deviations 1-3)
12. **[Phase 02-04] Robolectric required for CameraX-SUT unit tests:** Plan 02-01's assumption that Mockito-only would suffice was wrong for CameraController. CameraX `Preview.Builder().build()` internally calls `android.util.ArrayMap.put` (JVM android.jar stubs it to throw) and `Collections.unmodifiableSet(arraymap.keySet())` (returns NPE with `returnDefaultValues=true`). Added `org.robolectric:robolectric:4.13` as `testImplementation`; `@RunWith(RobolectricTestRunner::class) @Config(sdk = [34])` on `CameraControllerTest`. Also had to stub `mockEffect.targets doReturn PREVIEW|VIDEO|IMAGE` because `UseCaseGroup.Builder.checkEffectTargets` rejects Mockito's default 0. **Pattern for future: any SUT test that constructs CameraX UseCase builders in the test body needs Robolectric + stubbed CameraEffect.targets.** (02-04-SUMMARY.md Deviations 4-5)
13. **[Phase 02-05] CameraViewModel + CameraScreen Compose UI landed:** @HiltViewModel CameraViewModel @Inject(CameraController) exposes uiState:StateFlow<CameraUiState(5 fields per D-14 + isRecording)> + surfaceRequest reshared from controller + events:Flow<OneShotEvent> via Channel(BUFFERED).receiveAsFlow; onFlipLens uses CameraLensProvider.next; onTestRecord launches delay(5_000L) auto-stop (D-04, no audio path D-05); orientationListener is a quadrant-thresholded OrientationEventListener (D-08). CameraScreen @Composable renders CameraXViewfinder(ImplementationMode.EXTERNAL) fullscreen + OutlinedButton {Text("Flip")} Alignment.TopEnd (D-24 — text fallback since material-icons-extended not on classpath) + BuildConfig.DEBUG-gated Button {Text("TEST RECORD 5s" | "REC...")} Alignment.BottomCenter (D-04); CAMERA-only permission gate with rationale + Settings CTA reuses Phase 1 StubScreens pattern (D-26/27); DisposableEffect(lifecycleOwner) enable/disable OrientationEventListener. BugzzApp CameraRoute rewired to ui/camera/CameraScreen (ui/screens/StubScreens.kt retained unchanged — other routes still use it). 10 unit tests GREEN (9 Phase 2 Nyquist + 1 placeholder). (02-05-SUMMARY.md)
14. **[Phase 02-05] Hilt ↔ Kotlin default-parameter incompatibility — constructor split pattern (Rule 3):** Plan 02-04 shipped `CameraController @Inject constructor(..., providerFactory: suspend (Context) -> ProcessCameraProvider = { default })`. The test seam works (CameraControllerTest GREEN) but Plan 02-05's first Hilt-graph consumer (CameraViewModel) forces Hilt KSP-codegen to synthesize a binding for the `Function2<Context, Continuation, ProcessCameraProvider>` parameter — Dagger does not invoke Kotlin's synthetic `$default` method and has no `@Provides` for the lambda. Fix: SPLIT the constructor. Primary `internal constructor(..., providerFactory: T)` — no @Inject, no default — test entry point. Secondary `@Inject constructor(...)` — omits the seam, delegates `: this(..., providerFactory = { ctx -> ProcessCameraProvider.getInstance(ctx).await() })` to primary. Production Hilt graph now builds; tests unchanged. **Canonical pattern for all future @Singleton @Inject classes with test-substitutable factory/strategy parameters.** (02-05-SUMMARY.md Deviation 1)
15. **[Phase 02-05] ImplementationMode package canonicalized — research §Open Questions #1 resolved (Rule 3):** Sketch G import path was `androidx.camera.viewfinder.surface.ImplementationMode`; AAR class dump of `viewfinder-core-1.6.0.aar` confirmed the correct path is `androidx.camera.viewfinder.core.ImplementationMode`. Enum values `EXTERNAL` (SurfaceView) and `EMBEDDED` (TextureView); `PERFORMANCE` does not exist. No fallback needed — `ImplementationMode.EXTERNAL` compiled cleanly. Documented for Phase 3+ sprite work. (02-05-SUMMARY.md Deviation 2)
16. **[Phase 02-gaps-01] GAP-02-A closed — ML Kit `.enableTracking()` + `CONTOUR_MODE_ALL` mutually exclusive at runtime:** Google ML Kit silently ignores `.enableTracking()` when `setContourMode(CONTOUR_MODE_ALL)` is active; `FaceDetectorOptions.isTrackingEnabled` reflective-reports `true` but the detector emits faces with `trackingId == null`. Verified Xiaomi 13T / HyperOS — 459/459 FaceTracker log frames over 20s showed `id=null`. Root cause was research correctness: `.planning/research/PITFALLS.md §3 line 110` recommended `.enableTracking()` without documenting the mutual exclusivity. Fix: (1) removed `.enableTracking()` from `FaceDetectorClient.buildOptions()`; (2) flipped `FaceDetectorOptionsTest` to assert `trackingEnabled=false` with GAP-02-A/ADR-01 diagnostic messages; (3) created `02-ADR-01-no-ml-kit-tracking-with-contour.md` (569 words, full ADR format) listing four Phase 3 follow-ups (implement `BboxIouTracker`, re-key `LandmarkSmoother`, thread tracker through `createAnalyzer()`, update VERIFICATION CAM-08 row); (4) amended CONTEXT D-15/D-22 with ADR cross-ref + `-1` sentinel flow; (5) amended VALIDATION Per-Task + Manual-Only CAM-08 rows with relaxed boundingBox-centroid-stability acceptance; (6) flipped Wave 0 checklist `isTrackingEnabled == true` → `== false`; (7) patched PITFALLS §3 root — replaced misleading bullet with three-bullet callout (do NOT enable tracking under CONTOUR_MODE_ALL + MediaPipe-style bbox-IoU alternative + `LANDMARK_MODE_ALL` fallback). **Canonical pattern: research-correction ADR** — when device-verification surfaces a research correctness issue, the closure plan amends BOTH the research root (inheritance) AND phase CONTEXT (immediate) AND writes an ADR (future-phase discovery). Full bbox-IoU face-identity tracking deferred to Phase 3 per ADR-01. (02-gaps-01-SUMMARY.md)

### Architectural Gates

- **Phase 2 exit criterion:** Debug red-rect overlay pixel-perfect on face in portrait + landscape + front/back swap. Recording a 5-second test `.mp4` contains the red rect (proves `OverlayEffect` baked into VIDEO_CAPTURE target).
- **Phase 3 integration test:** One filter + photo capture proves three-stream compositing end-to-end.
- **Phase 7 escalation trigger:** Avg frame time >33ms over 10-second recording → custom GL `CameraEffect` (NOT Filament).

### Active Todos

- [ ] Plan Phase 1 via `/gsd-plan-phase 1`
- [ ] Set up real Android 9+ device via USB ADB for on-device testing (required from Phase 2 onward)
- [ ] Extract bug sprite assets from reference APK `reference/com.insect.filters.funny.prank.bug.filter.face.camera.apk` before Phase 3

### Blockers

None.

### Open Questions (resolved during execution)

- `DCIM/Bugzz/` vs `Pictures/Bugzz/` save convention → inspect reference runtime in Phase 3; default `DCIM/Bugzz/`.
- Reference's exact video `MAX_DURATION` → default 60s in Phase 5; verify against DEX if time permits.
- Front-camera photo mirror-at-save convention → inspect reference in Phase 3, match.
- 1€ filter parameter tuning → ship literature defaults (1.0 / 0.007 / 1.0), tune empirically in Phase 3.
- Exact bug-filter count/types → bundle 15-25 common categories in Phase 4; reference catalog is server-driven and un-extractable.

## Session Continuity

**Last agent:** gsd-execute-phase (Plan 06-02 Wave 1 executor — autonomous per `feedback_autonomy.md`)
**Last action:** Completed 06-02-PLAN.md — 3 tasks autonomous execution: (1) catalog + build script: lottie-compose 6.7.1 + media3 1.4.1 (resolved up to 1.9.0 transitively by CameraX 1.6.0 — Rule 3 deviation accepted); (2) home_lottie.json byte-identical copy (746491 B, sha256 5d3cfc5c…) to app/src/main/assets/lottie/; (3) FilterPrefsRepository extended with onboardingCompleted: Flow<Boolean> + setOnboardingCompleted() (no-arg per `<interfaces>` spec — Rule 3 deviation re plan-text drift); 3 @Ignore('Plan 06-02') FilterPrefsRepositoryTest cases un-ignored & GREEN. Atomic commit 03ff140. 9 D-32 grep-asserts re-verified. APK assembles. Suite 170 / 24 ignored / 0 failures (-3 ignored vs 06-01 = +3 net GREEN).

**Stopped at:** Plan 06-02 complete — Wave 1 deps + Lottie asset + onboarding prefs key landed; commit 03ff140; Wave 2 (Plan 06-03 Splash + Onboarding) unblocked.

**Next expected action:** Start Phase 6 UX Polish via `/gsd-discuss-phase 6` — Splash, Home, Onboarding, Preview, Collection, Share.

**Files modified this session (Plan 03-02):**

- `app/src/main/java/com/bugzz/filter/camera/detector/BboxIouTracker.kt` (replaced stub with full production body — greedy IoU matcher, TrackerResult return type, companion constants 0.3f/5/2)
- `app/src/main/java/com/bugzz/filter/camera/detector/OneEuroFilter.kt` (LandmarkSmoother.onFaceLost TODO replaced with real iterator.remove body)
- `app/src/main/java/com/bugzz/filter/camera/detector/FaceDetectorClient.kt` (tracker param added; createAnalyzer consumer rewritten; smoothFace signature changed to TrackedFace; Timber format updated; SMOOTHED_CONTOUR_TYPES reordered)
- `app/src/main/java/com/bugzz/filter/camera/detector/FaceSnapshot.kt` (KDoc updated — trackingId is tracker-assigned stable ID, not ML Kit sentinel)
- `app/src/test/java/com/bugzz/filter/camera/detector/BboxIouTrackerTest.kt` (all 10 @Ignore annotations removed)
- `app/src/test/java/com/bugzz/filter/camera/detector/LandmarkSmootherTest.kt` (all 3 @Ignore annotations removed)
- `app/src/test/java/com/bugzz/filter/camera/detector/FaceDetectorClientTest.kt` (@Ignore removed; tracker wire-up test body implemented)
- `.planning/phases/03-first-filter-end-to-end-photo-capture/03-02-SUMMARY.md` (created)
- `.planning/STATE.md` (updated — this file)
- `.planning/ROADMAP.md` (updated via roadmap update-plan-progress 03)
- `.planning/ROADMAP.md` (updated via roadmap update-plan-progress 02)
- `.planning/REQUIREMENTS.md` (updated — CAM-08 marked complete via requirements mark-complete)

---
*State initialized: 2026-04-18*
