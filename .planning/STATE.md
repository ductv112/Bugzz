---
gsd_state_version: 1.0
milestone: v1.0
milestone_name: milestone
status: executing
stopped_at: Completed 02-04-PLAN.md — render + camera lifecycle layer landed; OverlayEffectBuilderTest (2/2) + CameraControllerTest (2/2) GREEN; all 9 Phase 2 Nyquist tests GREEN
last_updated: "2026-04-19T09:31:53.031Z"
progress:
  total_phases: 7
  completed_phases: 1
  total_plans: 10
  completed_plans: 8
  percent: 80
---

# State: Bugzz

**Last updated:** 2026-04-19

## Project Reference

**Core Value:** Smooth live AR preview with bug sprites tracking face landmarks. If the live preview stutters or bugs don't stick to the face, everything else is meaningless.

**Current Focus:** Phase 02 — Camera Preview + Face Detection + Coordinate Validation

**Milestone:** v1 — feature-parity clone of `com.insect.filters.funny.prank.bug.filter.face.camera` v1.2.7, MINUS monetization and i18n.

## Current Position

Phase: 02 (Camera Preview + Face Detection + Coordinate Validation) — EXECUTING
Plan: 5 of 6

- **Phase:** 2
- **Plan:** 04 complete — render + camera lifecycle layer landed: DebugOverlayRenderer (BuildConfig.DEBUG-gated multi-face Canvas drawer) + OverlayEffectBuilder (real TARGETS=PREVIEW|VIDEO|IMAGE + QUEUE_DEPTH=0 + HandlerThread + setMatrix(frame.sensorToBufferTransform) pairing) + CameraController (UseCaseGroup of 4 use cases + 1 effect + STRATEGY_KEEP_ONLY_LATEST + Size(720,1280) + providerFactory seam + flipLens/setTargetRotation/startTestRecording — no .withAudioEnabled()). OverlayEffectBuilderTest (2/2) + CameraControllerTest (2/2) GREEN; all 9 Phase 2 Nyquist tests GREEN. Five Rule 3 auto-fixes: CameraX 1.6.0 does not ship awaitInstance (→ manual ListenableFuture.await() bridge), MIRROR_MODE_ON_FRONT_ONLY lives on MirrorMode not VideoCapture, listenablefuture stub jar is rewritten by Gradle (→ full guava dep), Mockito cannot construct CameraX Preview.Builder (→ Robolectric 4.13 + @RunWith(RobolectricTestRunner) + @Config(sdk=[34])), mock OverlayEffect.getTargets=0 rejected by UseCaseGroup.Builder (→ stub `on { targets } doReturn PREVIEW|VIDEO|IMAGE`).
- **Status:** Executing Phase 02
- **Progress:** [████████░░] 80%

### Phase Map

```
Phase 1: Foundation & Skeleton                            [ complete ]
Phase 2: Camera + Face Detection + Coord Validation       [ executing — 4/6 plans done ]
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

**Last agent:** gsd-execute-phase (Plan 02-04 executor)
**Last action:** Completed 02-04-PLAN.md — render + camera lifecycle layer landed. Plan 02-03 compile-unblock stubs fully REPLACED by production implementations. DebugOverlayRenderer (@Singleton Canvas renderer; BuildConfig.DEBUG first-line gate; multi-face iteration with red stroked boundingBox + orange-red contour/landmark dots). OverlayEffectBuilder (@Singleton @Inject; real TARGETS=PREVIEW|VIDEO|IMAGE companion + QUEUE_DEPTH=0; HandlerThread("BugzzRenderThread"); setOnDrawListener body runs `canvas.setMatrix(frame.sensorToBufferTransform)` FIRST then `renderer.draw(canvas, faceDetector.latestSnapshot.get(), frame.timestampNanos)`). CameraController (@Singleton @Inject; overlayEffect = overlayEffectBuilder.build() ONCE in property init per D-25; bind() produces UseCaseGroup with EXACTLY 4 use cases + 1 effect; ImageAnalysis STRATEGY_KEEP_ONLY_LATEST + Size(720,1280); VideoCapture MirrorMode.MIRROR_MODE_ON_FRONT_ONLY + Quality.HD; providerFactory seam with default `getInstance(ctx).await()`; flipLens calls faceDetector.onLensFlipped() BEFORE bind; startTestRecording MediaStoreOutputOptions DCIM/Bugzz — NO `.withAudioEnabled()` grep-verified). Five Rule 3 auto-fixes logged in 02-04-SUMMARY.md Deviations (research §A5 awaitInstance drift, MIRROR_MODE location, listenablefuture stub rewrite + guava direct dep, Robolectric required for CameraX builder tests, mockEffect.targets stub for UseCaseGroup validation). All 9 Phase 2 Nyquist unit tests GREEN: OneEuroFilterTest (4/4) + FaceDetectorOptionsTest (1/1) + OverlayEffectBuilderTest (2/2) + CameraControllerTest (2/2). `./gradlew :app:testDebugUnitTest` exits 0 with 10 tests / 0 failures / 0 skipped. `./gradlew :app:assembleDebug` exits 0.

**Stopped at:** Completed 02-04-PLAN.md — render + camera lifecycle layer landed; OverlayEffectBuilderTest (2/2) + CameraControllerTest (2/2) GREEN; all 9 Phase 2 Nyquist tests GREEN

**Next expected action:** Execute 02-05-PLAN.md (CameraViewModel + CameraScreen Compose UI — consumes `CameraController.surfaceRequest: StateFlow<SurfaceRequest?>` + `bind(owner, lens, rotation)` + `flipLens` + `setTargetRotation` + `startTestRecording` / `stopTestRecording`; `CameraLensProvider.next(current)` from Plan 02-03 drives toggle logic; gates TEST RECORD button behind `BuildConfig.DEBUG`).

**Files modified this session (Plan 02-04):**

- `app/src/main/java/com/bugzz/filter/camera/render/DebugOverlayRenderer.kt` (created — 64 LOC; @Singleton Canvas renderer, BuildConfig.DEBUG first-line gate, multi-face iteration)
- `app/src/main/java/com/bugzz/filter/camera/render/OverlayEffectBuilder.kt` (REPLACED — 67 LOC; real TARGETS companion + QUEUE_DEPTH=0 + HandlerThread + setMatrix pairing)
- `app/src/main/java/com/bugzz/filter/camera/camera/CameraController.kt` (REPLACED — ~245 LOC including ListenableFuture.await() extension; 4 use cases + 1 effect + providerFactory seam + flipLens + setTargetRotation + startTestRecording + NO .withAudioEnabled)
- `app/src/test/java/com/bugzz/filter/camera/camera/CameraControllerTest.kt` (un-@Ignore'd; @RunWith(RobolectricTestRunner) + @Config(sdk=[34]); argumentCaptor UseCaseGroup; mockEffect.targets stubbed)
- `app/build.gradle.kts` (added testOptions.unitTests.isReturnDefaultValues; implementation libs.guava; testImplementation libs.robolectric)
- `gradle/libs.versions.toml` (added guava=33.3.1-android + robolectric=4.13 version refs + library entries)
- `.planning/phases/02-camera-preview-face-detection-coordinate-validation/02-04-SUMMARY.md` (created)
- `.planning/STATE.md` (updated — this file)
- `.planning/ROADMAP.md` (updated via roadmap update-plan-progress)
- `.planning/phases/02-camera-preview-face-detection-coordinate-validation/02-03-SUMMARY.md` (created)
- `.planning/STATE.md` (updated)

---
*State initialized: 2026-04-18*
