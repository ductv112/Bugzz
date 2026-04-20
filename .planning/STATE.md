---
gsd_state_version: 1.0
milestone: v1.0
milestone_name: milestone
status: executing
stopped_at: Completed 04-07-PLAN.md
last_updated: "2026-04-20T18:11:53.820Z"
progress:
  total_phases: 7
  completed_phases: 3
  total_plans: 27
  completed_plans: 26
  percent: 96
---

# State: Bugzz

**Last updated:** 2026-04-19

## Project Reference

**Core Value:** Smooth live AR preview with bug sprites tracking face landmarks. If the live preview stutters or bugs don't stick to the face, everything else is meaningless.

**Current Focus:** Phase 04 — Filter Catalog + Picker + Face Filter Mode

**Milestone:** v1 — feature-parity clone of `com.insect.filters.funny.prank.bug.filter.face.camera` v1.2.7, MINUS monetization and i18n.

## Current Position

Phase: 04 (Filter Catalog + Picker + Face Filter Mode) — EXECUTING
Plan: 5 of 8

- **Phase:** 4
- **Plan:** 5 complete (04-05)
- **Previous plan:** 05 complete — CameraViewModel + CameraScreen Compose UI landed: CameraUiState (5-field D-14 data class) + PermissionState sealed interface + OneShotEvent sealed interface for toasts; @HiltViewModel CameraViewModel @Inject(CameraController) exposing uiState:StateFlow + surfaceRequest reshared + events:Flow via Channel(BUFFERED).receiveAsFlow, with onFlipLens (CameraLensProvider.next), onTestRecord (delay(5_000L) auto-stop per D-04, no audio path per D-05), and orientationListener (quadrant-thresholded Surface.ROTATION_{0/90/180/270} emit per D-08); CameraScreen @Composable rendering CameraXViewfinder(ImplementationMode.EXTERNAL) fullscreen + OutlinedButton { Text("Flip") } Alignment.TopEnd (D-24 — text fallback, material-icons-extended not on classpath) + BuildConfig.DEBUG-gated Button { Text("TEST RECORD 5s" | "REC...") } Alignment.BottomCenter (D-04); CAMERA-only permission gate with rationale + Settings CTA reusing Phase 1 StubScreens pattern (D-26/27); DisposableEffect enables/disables OrientationEventListener (D-08). BugzzApp.kt CameraRoute import rewired to com.bugzz.filter.camera.ui.camera.CameraScreen (Phase 1 ui/screens stub orphaned but file retained for other routes). 4 Rule 3 auto-fixes: (1) Hilt cannot synthesize a binding for Kotlin @Inject constructor default-value Function2 param — split CameraController into internal primary constructor (test seam) + secondary @Inject constructor (production factory inlined), (2) ImplementationMode lives in androidx.camera.viewfinder.core NOT .surface — research §Open Questions #1 resolved with AAR class dump (EXTERNAL enum confirmed — no fallback to PERFORMANCE needed), (3) Icons.Default.Cameraswitch not on classpath — OutlinedButton { Text("Flip") } per plan's explicit fallback + CLAUDE.md D-24 icon polish deferred to Phase 6, (4) MutableCoordinateTransformer import dropped (unused in body). APK assembles (79 MB); 10 unit tests GREEN (9 Phase 2 Nyquist + 1 placeholder).
- **Status:** Executing Phase 04
- **Progress:** [██████████] 96%

### Phase Map

```
Phase 1: Foundation & Skeleton                            [ complete ]
Phase 2: Camera + Face Detection + Coord Validation       [ gap closure — 6/6 base + 1/3 gap plans done (gaps-02, gaps-03 remaining) ]
Phase 3: First Filter End-to-End + Photo Capture          [ pending ]
Phase 4: Filter Catalog + Picker + Face Filter Mode       [ pending ]
Phase 5: Video Recording + Audio + Insect Filter Mode     [ pending ]
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

## Accumulated Context

### Key Decisions Locked in Research

1. **UI Toolkit:** Jetpack Compose (not Views) — CameraX-Compose stable in 1.6; greenfield + solo dev + 6 screens is Compose sweet spot. (SUMMARY.md Resolution #1)
2. **Face Tracking:** ML Kit Face Detection contour mode (NOT Face Mesh) — 2D sprites don't need 478-point mesh; Face Detection has first-class `MlKitAnalyzer` + `trackingId`. (SUMMARY.md Resolution #2)
3. **Rendering:** 2D Canvas via `OverlayEffect` (NO Filament) — PBR engine is cargo-cult for sprite blits; escalation path is custom GL `CameraEffect`, never Filament. (SUMMARY.md Resolution #3)
4. **CameraX version:** 1.6.0 uniform across all artifacts — first version with stable `camera-effects` + `camera-compose`.
5. **ML Kit model:** Bundled (`com.google.mlkit:face-detection:16.1.7`) — offline first-launch, no Play Services model-download race.
6. **Persistence:** MediaStore for captures, DataStore for prefs — no Room DB for MVP.

### Key Decisions During Execution

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

**Last agent:** gsd-execute-phase (Plan 03-05 continuation executor, post-checkpoint Task 4)
**Last action:** Completed 03-05-PLAN.md — Task 4: 02-VERIFICATION.md CAM-08 row updated (ADR-01 follow-up #4 closed, fd2a7ad); 03-05-SUMMARY.md written; 03-gaps-01-PLAN.md filed (spider sprite re-extraction, Phase 4 prerequisite, not executed); STATE.md + ROADMAP.md updated. Phase 3 device verification 4/4 hard gates PASS on Xiaomi 13T 2026-04-20.

**Stopped at:** Completed 04-07-PLAN.md

**Next expected action:** Execute 04-06-PLAN.md — CameraScreen LazyRow picker strip + HomeScreen redesign (Face Filter / Insect Filter buttons + settings gear).

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
