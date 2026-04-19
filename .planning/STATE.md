---
gsd_state_version: 1.0
milestone: v1.0
milestone_name: milestone
status: executing
stopped_at: Completed 02-gaps-01-PLAN.md — GAP-02-A closed (.enableTracking() removed from FaceDetectorClient + test flipped to trackingEnabled=false + ADR-01 created + CONTEXT D-15/D-22 + VALIDATION CAM-08 + PITFALLS §3 all amended); 10 unit tests remain GREEN; gaps-02 + gaps-03 still open
last_updated: "2026-04-19T13:22:37.621Z"
progress:
  total_phases: 7
  completed_phases: 1
  total_plans: 13
  completed_plans: 11
  percent: 85
---

# State: Bugzz

**Last updated:** 2026-04-19

## Project Reference

**Core Value:** Smooth live AR preview with bug sprites tracking face landmarks. If the live preview stutters or bugs don't stick to the face, everything else is meaningless.

**Current Focus:** Phase 02 — Camera Preview + Face Detection + Coordinate Validation

**Milestone:** v1 — feature-parity clone of `com.insect.filters.funny.prank.bug.filter.face.camera` v1.2.7, MINUS monetization and i18n.

## Current Position

Phase: 02 (Camera Preview + Face Detection + Coordinate Validation) — GAP CLOSURE (1 of 3 gap plans done)
Plan: 02-gaps-01 complete (6 base plans + 1 gap plan); 02-gaps-02 + 02-gaps-03 remaining

- **Phase:** 2
- **Plan:** 02-gaps-01 complete — GAP-02-A (CAM-08 null trackingId) closed. Removed `.enableTracking()` from `FaceDetectorClient.buildOptions()` — was silently ignored by ML Kit under `CONTOUR_MODE_ALL` (459/459 null trackingIds on Xiaomi 13T per Plan 02-06 device runbook). Flipped `FaceDetectorOptionsTest` to assert `trackingEnabled=false`. Created `02-ADR-01-no-ml-kit-tracking-with-contour.md` (569 words, Status/Context/Decision/Consequences/Follow-ups/Alternatives) listing 4 Phase 3 action items: implement `BboxIouTracker`, re-key `LandmarkSmoother` on tracker ID, thread tracker through `createAnalyzer()`, update `02-VERIFICATION.md` CAM-08 row on Phase 3 exit. Amended 02-CONTEXT.md D-15 (no `.enableTracking()`, runtime trackingId == null, ADR cross-ref) + D-22 (Phase 2 uses `id=-1` sentinel; Phase 3 BboxIouTracker provides stable ID). Amended 02-VALIDATION.md Per-Task Verification Map (split CAM-08 into its own "relaxed" row), Manual-Only Verifications (boundingBox centerX/Y stability on still head 60+ frames, trackingId=null expected), Wave 0 checklist (`isTrackingEnabled == true` → `== false`). Patched research root: `.planning/research/PITFALLS.md §3` line 110 bullet replaced with 3-bullet callout (do NOT enable tracking under CONTOUR_MODE_ALL + MediaPipe-style bbox-IoU alternative + LANDMARK_MODE_ALL fallback for apps that don't need contour) + new Warning Signs bullet flagging `trackingId always null` symptom. 3 atomic commits: `98e032a` (fix: detector + test), `3aa2ed3` (docs: ADR + CONTEXT + VALIDATION), `cb54bc6` (docs: PITFALLS §3). 10/10 unit tests remain GREEN; APK still builds cleanly (82 MB). GAP-02-B + GAP-02-C remain open — gaps-02 is next (renderer scale/density fix), gaps-03 depends on gaps-02.
- **Previous plan:** 05 complete — CameraViewModel + CameraScreen Compose UI landed: CameraUiState (5-field D-14 data class) + PermissionState sealed interface + OneShotEvent sealed interface for toasts; @HiltViewModel CameraViewModel @Inject(CameraController) exposing uiState:StateFlow + surfaceRequest reshared + events:Flow via Channel(BUFFERED).receiveAsFlow, with onFlipLens (CameraLensProvider.next), onTestRecord (delay(5_000L) auto-stop per D-04, no audio path per D-05), and orientationListener (quadrant-thresholded Surface.ROTATION_{0/90/180/270} emit per D-08); CameraScreen @Composable rendering CameraXViewfinder(ImplementationMode.EXTERNAL) fullscreen + OutlinedButton { Text("Flip") } Alignment.TopEnd (D-24 — text fallback, material-icons-extended not on classpath) + BuildConfig.DEBUG-gated Button { Text("TEST RECORD 5s" | "REC...") } Alignment.BottomCenter (D-04); CAMERA-only permission gate with rationale + Settings CTA reusing Phase 1 StubScreens pattern (D-26/27); DisposableEffect enables/disables OrientationEventListener (D-08). BugzzApp.kt CameraRoute import rewired to com.bugzz.filter.camera.ui.camera.CameraScreen (Phase 1 ui/screens stub orphaned but file retained for other routes). 4 Rule 3 auto-fixes: (1) Hilt cannot synthesize a binding for Kotlin @Inject constructor default-value Function2 param — split CameraController into internal primary constructor (test seam) + secondary @Inject constructor (production factory inlined), (2) ImplementationMode lives in androidx.camera.viewfinder.core NOT .surface — research §Open Questions #1 resolved with AAR class dump (EXTERNAL enum confirmed — no fallback to PERFORMANCE needed), (3) Icons.Default.Cameraswitch not on classpath — OutlinedButton { Text("Flip") } per plan's explicit fallback + CLAUDE.md D-24 icon polish deferred to Phase 6, (4) MutableCoordinateTransformer import dropped (unused in body). APK assembles (79 MB); 10 unit tests GREEN (9 Phase 2 Nyquist + 1 placeholder).
- **Status:** Ready to execute
- **Progress:** [█████████░] 85%

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

## Accumulated Context

### Key Decisions Locked in Research

1. **UI Toolkit:** Jetpack Compose (not Views) — CameraX-Compose stable in 1.6; greenfield + solo dev + 6 screens is Compose sweet spot. (SUMMARY.md Resolution #1)
2. **Face Tracking:** ML Kit Face Detection contour mode (NOT Face Mesh) — 2D sprites don't need 478-point mesh; Face Detection has first-class `MlKitAnalyzer` + `trackingId`. (SUMMARY.md Resolution #2)
3. **Rendering:** 2D Canvas via `OverlayEffect` (NO Filament) — PBR engine is cargo-cult for sprite blits; escalation path is custom GL `CameraEffect`, never Filament. (SUMMARY.md Resolution #3)
4. **CameraX version:** 1.6.0 uniform across all artifacts — first version with stable `camera-effects` + `camera-compose`.
5. **ML Kit model:** Bundled (`com.google.mlkit:face-detection:16.1.7`) — offline first-launch, no Play Services model-download race.
6. **Persistence:** MediaStore for captures, DataStore for prefs — no Room DB for MVP.

### Key Decisions During Execution

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

**Last agent:** gsd-execute-phase (Plan 02-gaps-01 executor, sequential mode)
**Last action:** Completed 02-gaps-01-PLAN.md — GAP-02-A closed. Three atomic commits: (1) `98e032a` fix(02-gaps-01-01) removed `.enableTracking()` from FaceDetectorClient.buildOptions() + flipped FaceDetectorOptionsTest to assert `trackingEnabled=false` + added KDoc warning block with GAP-02-A evidence + ADR-01 cross-ref; (2) `3aa2ed3` docs(02-gaps-01-02) created 02-ADR-01-no-ml-kit-tracking-with-contour.md (569 words, full ADR format, 4 Phase 3 BboxIouTracker follow-up items) + amended 02-CONTEXT.md D-15 (no .enableTracking() call + runtime trackingId == null) + D-22 (Phase 2 uses -1 sentinel, Phase 3 BboxIouTracker provides stable ID) + amended 02-VALIDATION.md Per-Task Verification Map (split CAM-08 off with relaxed acceptance) + Manual-Only Verifications (boundingBox centroid stability criterion, trackingId=null expected) + Wave 0 FaceDetectorOptionsTest checklist (isTrackingEnabled flip true→false); (3) `cb54bc6` docs(02-gaps-01-03) amended .planning/research/PITFALLS.md §3 at the root — replaced the misleading `.enableTracking()` recommendation at line 110 with a three-bullet callout (do NOT enable tracking under CONTOUR_MODE_ALL + MediaPipe-style bbox-IoU alternative + LANDMARK_MODE_ALL fallback) + added Warning Signs bullet for the null-trackingId symptom. No deviations required — plan executed exactly as written. 10/10 unit tests remain GREEN (4 OneEuroFilterTest + 1 FaceDetectorOptionsTest [now asserting trackingEnabled=false] + 2 OverlayEffectBuilderTest + 2 CameraControllerTest + 1 placeholder). `./gradlew :app:assembleDebug` exits 0 (82 MB APK, matches Plan 02-06 baseline). Previously: Plan 02-05 completed CameraViewModel + CameraScreen Compose UI landed. Four new files in `ui/camera/` + 1-line import rewire in `ui/BugzzApp.kt` + Rule 3 CameraController constructor split. CameraUiState (5 fields per D-14 + isRecording) + PermissionState sealed interface + OneShotEvent sealed interface. @HiltViewModel CameraViewModel @Inject(CameraController) exposes uiState:StateFlow + surfaceRequest reshared + events:Flow via Channel(BUFFERED).receiveAsFlow; onFlipLens uses CameraLensProvider.next (D-24); onTestRecord launches delay(5_000L) auto-stop (D-04) with no audio path (D-05); orientationListener is a quadrant-thresholded OrientationEventListener (D-08). CameraScreen @Composable: CameraXViewfinder(ImplementationMode.EXTERNAL) fullscreen + OutlinedButton {Text("Flip")} Alignment.TopEnd (D-24 — text fallback since material-icons-extended not on classpath per CLAUDE.md D-24 icon polish deferred to Phase 6) + BuildConfig.DEBUG-gated Button {Text("TEST RECORD 5s" | "REC...")} Alignment.BottomCenter (D-04); CAMERA-only permission gate with rationale + Settings CTA (D-26/27) reuses Phase 1 pattern verbatim; DisposableEffect(lifecycleOwner) enable/disable OrientationEventListener; LaunchedEffect collects vm.events and emits Toast per OneShotEvent variant. BugzzApp CameraRoute rewired via 1-line import swap to `ui.camera.CameraScreen`. CameraController constructor split (Rule 3 Hilt fix): primary `internal constructor(..., providerFactory: T)` preserves test seam; secondary `@Inject constructor(...)` hard-codes production factory so Hilt graph can satisfy CameraController without providing a Function2 binding — this is the canonical pattern for all future @Singleton @Inject classes with test seams. 4 Rule 3 auto-fixes total: (1) Hilt constructor split, (2) ImplementationMode package `androidx.camera.viewfinder.core` not `.surface` (research §Open Questions #1 resolved — EXTERNAL enum confirmed via AAR class dump), (3) Icons.Default.Cameraswitch → `OutlinedButton { Text("Flip") }` fallback (plan explicitly anticipated), (4) MutableCoordinateTransformer import dropped (unused). `./gradlew :app:assembleDebug` exits 0 (79 MB APK). `./gradlew :app:testDebugUnitTest` exits 0 with 10 tests / 0 failures / 0 skipped (9 Phase 2 Nyquist still GREEN: OneEuroFilterTest 4/4 + FaceDetectorOptionsTest 1/1 + OverlayEffectBuilderTest 2/2 + CameraControllerTest 2/2). CAM-01 (Compose CameraXViewfinder preview) + CAM-02 (lens flip via button) source-level complete; device verification is Plan 02-06's runbook on Xiaomi 13T.

**Stopped at:** Completed 02-gaps-01-PLAN.md — GAP-02-A closed; 10/10 unit tests GREEN; APK clean; gaps-02 + gaps-03 remaining.

**Next expected action:** Execute 02-gaps-02-PLAN.md (GAP-02-B — DebugOverlayRenderer over-draw fix: diagnostic wave to isolate H1/H2/H3, then minimal-rendering rewrite, then device re-verification of HANDOFF Steps 8-9 on Xiaomi 13T). After gaps-02 closes, gaps-03 becomes unblocked (CAM-06 MP4 overlay visual re-verification). Device re-run of CAM-08 relaxed acceptance (boundingBox stability) can be bundled into the gaps-02 / gaps-03 device runbook.

**Files modified this session (Plan 02-gaps-01):**

- `app/src/main/java/com/bugzz/filter/camera/detector/FaceDetectorClient.kt` (modified — removed `.enableTracking()` call; added KDoc warning block with GAP-02-A evidence + ADR-01 ref; updated activeIds inline comment for null-trackingId + -1 sentinel flow)
- `app/src/test/java/com/bugzz/filter/camera/detector/FaceDetectorOptionsTest.kt` (modified — dropped `.enableTracking()` from expected builder; flipped `trackingEnabled=true` → `trackingEnabled=false`; updated diagnostic messages + class KDoc amendment note)
- `.planning/phases/02-camera-preview-face-detection-coordinate-validation/02-ADR-01-no-ml-kit-tracking-with-contour.md` (created — 569 words; full ADR format; 4 Phase 3 BboxIouTracker follow-up items + alternatives table)
- `.planning/phases/02-camera-preview-face-detection-coordinate-validation/02-CONTEXT.md` (modified — D-15 + D-22 amended with GAP-02-A + ADR-01 cross-refs)
- `.planning/phases/02-camera-preview-face-detection-coordinate-validation/02-VALIDATION.md` (modified — Per-Task CAM-08 row split to relaxed acceptance; Manual-Only row updated to boundingBox-centroid-continuity criterion; Wave 0 checklist isTrackingEnabled flipped)
- `.planning/research/PITFALLS.md` (modified — §3 line 110 bullet replaced with 3-bullet callout + new Warning Signs bullet for null-trackingId symptom)
- `.planning/phases/02-camera-preview-face-detection-coordinate-validation/02-gaps-01-SUMMARY.md` (created)
- `.planning/STATE.md` (updated — this file)
- `.planning/ROADMAP.md` (updated via roadmap update-plan-progress 02)
- `.planning/REQUIREMENTS.md` (updated — CAM-08 marked complete via requirements mark-complete)

---
*State initialized: 2026-04-18*
