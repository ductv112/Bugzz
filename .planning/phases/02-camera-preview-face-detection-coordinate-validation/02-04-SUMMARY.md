---
phase: 02-camera-preview-face-detection-coordinate-validation
plan: 04
subsystem: render-lifecycle
tags: [overlay-effect, camerax-usecasegroup, sensor-to-buffer-transform, mlkit-analyzer, nyquist-green, robolectric]

# Dependency graph
requires:
  - phase: 01-foundation-skeleton
    provides: Hilt @Inject/@Singleton baseline; BugzzApplication Timber.DebugTree under BuildConfig.DEBUG guard
  - plan: 02-01
    provides: OverlayEffectBuilderTest RED + CameraControllerTest @Ignored — turned GREEN / un-Ignored by this plan
  - plan: 02-02
    provides: CameraX 1.6.0 deps + ML Kit 16.1.7 + mockito-kotlin + Hilt CameraModule (cameraExecutor / renderExecutor @Named providers)
  - plan: 02-03
    provides: FaceDetectorClient (MlKitAnalyzer + AtomicReference<FaceSnapshot> + 1€ smoothing + onLensFlipped) + FaceSnapshot data classes + CameraLens enum + Plan 02-04 compile-unblock stubs for OverlayEffectBuilder + CameraController (REPLACED by this plan)
provides:
  - DebugOverlayRenderer — @Singleton Canvas renderer, BuildConfig.DEBUG gated (D-02/T-02-02), multi-face iteration (D-23/PITFALLS #13)
  - OverlayEffectBuilder — real @Singleton @Inject producing OverlayEffect with TARGETS=PREVIEW|VIDEO|IMAGE + QUEUE_DEPTH=0 + HandlerThread("BugzzRenderThread") + setMatrix(frame.sensorToBufferTransform) pairing (CAM-06/CAM-07, D-17/D-25)
  - CameraController — real @Singleton @Inject UseCaseGroup(4 usecases + 1 effect) + providerFactory seam + flipLens(onLensFlipped-before-bind) + setTargetRotation(all-4) + startTestRecording(MediaStore DCIM/Bugzz, no audio) (CAM-03/05/06, D-04/05/06/16/18/24/25, T-02-05/06/07)
  - ListenableFuture<T>.await() private suspend extension — CameraX 1.6 ProcessCameraProvider.getInstance bridge (research §A5 drift)
  - OverlayEffectBuilderTest GREEN (2/2) — CAM-06 TARGETS + QUEUE_DEPTH pinned
  - CameraControllerTest GREEN (2/2) — CAM-03 (4 uc + 1 effect) + CAM-05 (KEEP_ONLY_LATEST) pinned
affects: [plan-02-05, plan-02-06, phase-03]

# Tech tracking
tech-stack:
  added:
    - "com.google.guava:guava:33.3.1-android — CameraX ProcessCameraProvider.getInstance() returns ListenableFuture<T>; guava needed on compile classpath for our ListenableFuture.await() bridge. Transitively present at runtime via camera-core; listenablefuture stub-jar is rewritten by Gradle to 9999.0-empty-to-avoid-conflict-with-guava, so direct guava dep is required."
    - "org.robolectric:robolectric:4.13 — testImplementation. CameraX Preview.Builder + ImageAnalysis.Builder internals touch android.util.ArrayMap + OptionsBundle.from(Collections.unmodifiableSet); plain Mockito JVM unit tests fail. @RunWith(RobolectricTestRunner) + @Config(sdk=[34]) on CameraControllerTest provides the Android framework shadows."
  patterns:
    - "**Single-instance OverlayEffect across lens flips (D-25):** CameraController builds `overlayEffect = overlayEffectBuilder.build()` ONCE in a property initializer. Every `bind()` call re-adds the SAME CameraEffect instance to a new UseCaseGroup via `addEffect(overlayEffect)`. `flipLens` → `provider.unbindAll()` + `bindToLifecycle(newLensSelector, useCaseGroup)` reuses the effect — no render-thread teardown, no HandlerThread restart, no GL surface churn. Mitigates T-02-07 (frame drop on flip) by construction."
    - "**setMatrix-before-draw pairing (CAM-07 / PITFALLS #5):** `OverlayEffect.setOnDrawListener { frame -> canvas.setMatrix(frame.sensorToBufferTransform); renderer.draw(canvas, snapshot); true }` — setMatrix MUST precede any drawRect/drawCircle, else the red rect renders in raw sensor coords and the whole CAM-07 contract silently fails. Pairs with `COORDINATE_SYSTEM_SENSOR` on MlKitAnalyzer (established in Plan 02-03 FaceDetectorClient) — together they eliminate all manual matrix math from the app."
    - "**Provider-factory seam for suspend-function static:** CameraController constructor default `providerFactory: suspend (Context) -> ProcessCameraProvider = { ProcessCameraProvider.getInstance(it).await() }`. Production binding uses the default (no Hilt module change); CameraControllerTest passes `{ mockProvider }`. Sidesteps `ProcessCameraProvider.getInstance` being a static call that Mockito cannot intercept without PowerMock or mockito-inline static-mocking (brittle). Same pattern works for any static-factory SDK call site."
    - "**Per-framework-class fallback: Robolectric for CameraX builder unit tests:** CameraX Preview.Builder / ImageAnalysis.Builder call OptionsBundle.from → Collections.unmodifiableSet(arraymap.keySet()) on construction. Plain JVM android.jar returns null for framework stubs; isReturnDefaultValues=true surfaces NPEs instead. Robolectric provides real Android-framework shadows (ArrayMap, Looper, Handler, Color, Bitmap, etc.) so tests running on JVM execute CameraX code paths identically to a real device — without needing a device/emulator. Pattern: any SUT that constructs CameraX UseCase builders in a test body needs Robolectric."
    - "**Guava-jar-is-needed-directly (ListenableFuture transitive trap):** Gradle rewrites `com.google.guava:listenablefuture:1.0` to `9999.0-empty-to-avoid-conflict-with-guava` whenever guava is on the dependency graph transitively. Adding the stub jar as an explicit dep appears to succeed but results in an empty jar on the compile classpath; the type `com.google.common.util.concurrent.ListenableFuture` remains unresolved. Fix: depend on full guava (`com.google.guava:guava:33.3.1-android`). Transparent at runtime (camera-core ships guava); only matters for compile-time source references."

key-files:
  created:
    - app/src/main/java/com/bugzz/filter/camera/render/DebugOverlayRenderer.kt
    - .planning/phases/02-camera-preview-face-detection-coordinate-validation/02-04-SUMMARY.md
  modified:
    - app/src/main/java/com/bugzz/filter/camera/render/OverlayEffectBuilder.kt (REPLACED — stub → real impl)
    - app/src/main/java/com/bugzz/filter/camera/camera/CameraController.kt (REPLACED — stub → real impl)
    - app/src/test/java/com/bugzz/filter/camera/camera/CameraControllerTest.kt (un-@Ignore'd + Robolectric + mockEffect.targets stub)
    - app/build.gradle.kts (guava + robolectric deps + testOptions.unitTests.isReturnDefaultValues)
    - gradle/libs.versions.toml (guava=33.3.1-android, robolectric=4.13 version refs + library entries)

key-decisions:
  - "**Single OverlayEffect instance built in property initializer (D-25):** CameraController's `overlayEffect` field is initialized at construction time via `overlayEffectBuilder.build()`. This guarantees the same CameraEffect instance is reused across every `bind()` call (initial + flip). Acceptance grep-verifies the call appears exactly once in CameraController.kt, outside `bind()`. Mitigates T-02-07 by construction."
  - "**providerFactory seam AS the default parameter, not an optional override:** Per Plan 02-01's 'testability seam' pattern — the default `{ ProcessCameraProvider.getInstance(it).await() }` IS the production path (DI container injects nothing). Tests pass `{ mockProvider }`. No Hilt module, no Q.Ualified injection. Same pattern will apply to Plan 02-05's ViewModel injection — downstream consumers never mention providerFactory."
  - "**Robolectric over Mockito-only for CameraX builder tests:** Plan 02-01 hoped Mockito-only testing would suffice. CameraX 1.6.0 Preview.Builder's OptionsBundle.from reaches into android.util.ArrayMap.keySet, which the JVM android.jar returns RuntimeException (default) or null (with returnDefaultValues=true — causing NPE in Collections.unmodifiableSet). Robolectric 4.13 provides the Android framework shadow. Added as testImplementation with @RunWith(RobolectricTestRunner) + @Config(sdk=[34]) — future CameraX SUT tests follow the same pattern."
  - "**Full guava:33.3.1-android instead of listenablefuture stub:** Plan execution discovered that `com.google.guava:listenablefuture:1.0` is rewritten by Gradle to `9999.0-empty-to-avoid-conflict-with-guava` whenever guava is also on the graph transitively. Net effect: the listenablefuture stub is unusable when guava is anywhere nearby. Chose to depend on full guava directly — ~2.7MB cost is acceptable, and guava ships transitively at runtime via camera-core regardless."
  - "**@Config(sdk=[34]), not 36:** compileSdk=36 (from Plan 02-02); Robolectric 4.13 bundles Android framework shadows up to API 34 only. Test runs on shadow SDK 34, real app compiles against 36 — orthogonal concerns."

patterns-established:
  - "**CameraX-SUT-testable seam pattern:** For every SUT class that wraps a CameraX factory call with static resolution (ProcessCameraProvider.getInstance, CameraInfo factories, etc.), expose a constructor-default suspend-factory parameter. Production code injects nothing; tests substitute a mock. Pair with @RunWith(RobolectricTestRunner) + @Config(sdk=[34]) — CameraX use-case builders touch framework classes that require the Android shadow."
  - "**Stub-CameraEffect-targets when mocking:** Mockito-mocked CameraEffect returns 0 for getTargets() — rejected by UseCaseGroup.Builder.checkEffectTargets. Always stub the mock: `on { targets } doReturn (PREVIEW or VIDEO_CAPTURE or IMAGE_CAPTURE)`. Without it, test fails with IllegalArgumentException before reaching assertions."
  - "**ListenableFuture.await() minimal bridge:** When CameraX or other Google APIs return ListenableFuture and the project doesn't want kotlinx-coroutines-guava, declare a private `suspend fun <T> ListenableFuture<T>.await(): T` with suspendCancellableCoroutine + addListener(action, Runnable::run) + invokeOnCancellation { cancel(true) }. ~10 LOC, zero new deps (assuming guava is already on the compile classpath)."
  - "**setMatrix-must-come-FIRST-in-OverlayEffect-listener:** Every `setOnDrawListener { frame -> … }` body must call `canvas.setMatrix(frame.sensorToBufferTransform)` as its FIRST Canvas operation. Any drawRect / drawCircle before that appears in raw sensor coords. Enforce via acceptance-criteria grep and code-review rule; applies forever in Phase 3+ sprite rendering (FilterEngine replaces DebugOverlayRenderer but the setMatrix pairing stays)."

requirements-completed: [CAM-03, CAM-05, CAM-06, CAM-07]
# Note:
#   CAM-03 — UseCaseGroup(Preview+IA+IC+VC) + addEffect(OverlayEffect) — CameraControllerTest GREEN
#   CAM-05 — STRATEGY_KEEP_ONLY_LATEST — CameraControllerTest GREEN
#   CAM-06 — OverlayEffect targets PREVIEW|VIDEO|IMAGE with QUEUE_DEPTH=0 — OverlayEffectBuilderTest GREEN
#   CAM-07 — canvas.setMatrix(frame.sensorToBufferTransform) pairing — grep-verified in OverlayEffectBuilder.build(); on-device Plan 02-06 runbook validates visually

# Metrics
duration: 14m 56s
completed: 2026-04-19
---

# Phase 2 Plan 4: Render + Camera Lifecycle (OverlayEffectBuilder + DebugOverlayRenderer + CameraController) Summary

**Replaced Plan 02-03's compile-unblock stubs at `OverlayEffectBuilder.kt` and `CameraController.kt` with production implementations; created `DebugOverlayRenderer.kt`; un-@Ignore'd CameraControllerTest with Robolectric + mockEffect.targets stub. All 9 Phase 2 Nyquist tests GREEN (4 OneEuroFilter + 1 FaceDetectorOptions + 2 OverlayEffectBuilder + 2 CameraController). Two significant Rule 3 auto-fixes — research §A5 drift (awaitInstance does not exist in CameraX 1.6.0) + test-runner drift (Mockito-only cannot test CameraX builders; Robolectric required).**

## Performance

- **Duration:** 14 min 56 s (wall clock)
- **Started:** 2026-04-19T09:12:32Z
- **Completed:** 2026-04-19T09:27:28Z
- **Tasks:** 3 primary (per 02-04-PLAN.md)
- **Files created:** 2 (DebugOverlayRenderer.kt + 02-04-SUMMARY.md)
- **Files modified:** 5 (OverlayEffectBuilder.kt stub→real, CameraController.kt stub→real, CameraControllerTest.kt un-@Ignore, app/build.gradle.kts +guava/robolectric/testOptions, libs.versions.toml +guava/robolectric)
- **LOC added:** ~285 (64 DebugOverlayRenderer + 67 OverlayEffectBuilder + 245 CameraController including await() bridge, minus ~76 stub LOC deleted)

## Accomplishments

- **Task 1 — DebugOverlayRenderer.kt (64 LOC) + OverlayEffectBuilder.kt (67 LOC, replacing 41 LOC stub):**
  - `DebugOverlayRenderer`: `@Singleton @Inject constructor()`. `fun draw(canvas, snapshot, tNanos)`: FIRST line `if (!BuildConfig.DEBUG) return` (D-02 / T-02-02), SECOND line `if (snapshot.faces.isEmpty()) return` (CAM-06 no-face no-ghost), then `for (face in snapshot.faces)` drawing red stroke rect (boundingBox) + orange-red `argb(255,255,80,0)` dots for every `contours.values.flatten()` point (primary face) + every `landmarks.values` point (D-23 secondary fallback, PITFALLS #13).
  - `OverlayEffectBuilder`: `@Singleton @Inject constructor(faceDetector, renderer)`. Companion `internal val TARGETS = CameraEffect.PREVIEW or CameraEffect.VIDEO_CAPTURE or CameraEffect.IMAGE_CAPTURE` + `internal const val QUEUE_DEPTH = 0`. `build()` spins `HandlerThread("BugzzRenderThread")`, constructs `OverlayEffect(TARGETS, QUEUE_DEPTH, Handler(thread.looper), errorListener)`, `setOnDrawListener { frame -> canvas.setMatrix(frame.sensorToBufferTransform); renderer.draw(canvas, faceDetector.latestSnapshot.get(), frame.timestampNanos); true }`. `release()` calls `renderThread.quitSafely()`.
  - **OverlayEffectBuilderTest now GREEN (2/2):** target_mask_covers_preview_image_video + queue_depth_is_zero.

- **Task 2 — CameraController.kt (245 LOC, replacing 35 LOC stub):** `@Singleton @Inject constructor(@ApplicationContext Context, @Named("cameraExecutor") Executor, FaceDetectorClient, OverlayEffectBuilder, providerFactory: suspend (Context) -> ProcessCameraProvider = { getInstance(it).await() })`. Private `overlayEffect = overlayEffectBuilder.build()` property init (D-25). `suspend fun bind(owner, lens, rotation)`: builds Preview + ImageAnalysis (ResolutionSelector preferred Size(720,1280) + FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER + STRATEGY_KEEP_ONLY_LATEST + setAnalyzer(cameraExecutor, faceDetector.createAnalyzer())) + ImageCapture + VideoCapture (Recorder with Quality.HD + MirrorMode.MIRROR_MODE_ON_FRONT_ONLY); UseCaseGroup.Builder().addUseCase(×4).addEffect(overlayEffect).build(); provider.bindToLifecycle(owner, selector, useCaseGroup). `flipLens`: faceDetector.onLensFlipped() FIRST (PITFALLS #6), then bind(). `setTargetRotation(rotation)`: touches preview/imageAnalysis/imageCapture/videoCapture all 4 (D-08). `startTestRecording(onFinalized)`: ContentValues DCIM/Bugzz + MediaStoreOutputOptions + prepareRecording(appContext, options).start(mainExecutor, listener) — NO `.withAudioEnabled()` (D-05). Private `ListenableFuture<T>.await()` extension using suspendCancellableCoroutine + addListener(Runnable::run) + cancel(true) on cancellation.
- **Task 3 — CameraControllerTest.kt (107 LOC, un-@Ignore'd + rewritten):** `@RunWith(RobolectricTestRunner) @Config(sdk=[34])`. Helper `buildController(mockProvider)` creates mock Context + mock Analyzer + mock FaceDetectorClient (stub `createAnalyzer() doReturn mockAnalyzer`) + mock OverlayEffect (stub `targets doReturn PREVIEW|VIDEO_CAPTURE|IMAGE_CAPTURE` — required by UseCaseGroup.Builder.checkEffectTargets) + mock OverlayEffectBuilder (stub `build() doReturn mockEffect`) + CameraController with `providerFactory = { mockProvider }`. Two tests: `bind_produces_4_use_cases_plus_1_effect` (argumentCaptor UseCaseGroup; assert useCases.size==4 + effects.size==1) + `image_analysis_uses_keep_only_latest_strategy` (same capture pattern; filter ImageAnalysis; assert backpressureStrategy==STRATEGY_KEEP_ONLY_LATEST). **Both GREEN.**

## Task Commits

- **Task 1** — `a6ea233` — `feat(02-04-01): DebugOverlayRenderer + real OverlayEffectBuilder`
- **Task 2** — `f3af46b` — `feat(02-04-02): CameraController with UseCaseGroup + providerFactory seam` (includes Rule 3 fixes for awaitInstance + MIRROR_MODE + guava dep)
- **Task 3** — `0d75c6c` — `test(02-04-03): un-@Ignore CameraControllerTest; wire Robolectric + mock provider` (includes Rule 3 Robolectric introduction)

## Quoted Diffs

### OverlayEffectBuilder.build() — the CAM-07 setMatrix pairing (grep-stable)

```kotlin
effect.setOnDrawListener { frame ->
    val canvas = frame.overlayCanvas
    // CRITICAL: match COORDINATE_SYSTEM_SENSOR from MlKitAnalyzer (D-17 / CAM-07).
    // setMatrix MUST precede any drawRect / drawCircle (PITFALLS #5).
    canvas.setMatrix(frame.sensorToBufferTransform)
    val snapshot = faceDetector.latestSnapshot.get()
    renderer.draw(canvas, snapshot, frame.timestampNanos)
    true  // = "I drew something; present this frame"
}
```

Line ordering is load-bearing — `canvas.setMatrix(frame.sensorToBufferTransform)` appears strictly before `renderer.draw(...)`. Plan 02-06 on-device runbook validates the overlay pixel-aligns to the face across all 4 orientations + both lenses, which is only possible if the matrix pairing is correct.

### OverlayEffectBuilder companion — CAM-06 contract constants

```kotlin
companion object {
    /** Exposed for OverlayEffectBuilderTest (Plan 01) — asserts the exact target mask (CAM-06). */
    internal val TARGETS: Int =
        CameraEffect.PREVIEW or CameraEffect.VIDEO_CAPTURE or CameraEffect.IMAGE_CAPTURE

    /** Exposed for OverlayEffectBuilderTest (Plan 01) — asserts queueDepth == 0 (draw every frame). */
    internal const val QUEUE_DEPTH: Int = 0
}
```

### CameraController constructor — D-25 single-instance overlayEffect + providerFactory seam

```kotlin
@Singleton
class CameraController @Inject constructor(
    @ApplicationContext private val appContext: Context,
    @Named("cameraExecutor") private val cameraExecutor: Executor,
    private val faceDetector: FaceDetectorClient,
    private val overlayEffectBuilder: OverlayEffectBuilder,
    private val providerFactory: suspend (Context) -> ProcessCameraProvider =
        { ctx -> ProcessCameraProvider.getInstance(ctx).await() },
) {
    // Reused across lens flips (D-25 — OverlayEffect is singleton-ish per session).
    private val overlayEffect: CameraEffect = overlayEffectBuilder.build()
    …
}
```

### CameraController.bind() — UseCaseGroup composition (CAM-03 + CAM-06)

```kotlin
val useCaseGroup = UseCaseGroup.Builder()
    .addUseCase(previewUc)
    .addUseCase(imageAnalysisUc)
    .addUseCase(imageCaptureUc)
    .addUseCase(videoCaptureUc)
    .addEffect(overlayEffect)   // CAM-06 — effect bakes into Preview + Image + Video
    .build()
```

Exactly 4 addUseCase + 1 addEffect; verified by grep + by CameraControllerTest.bind_produces_4_use_cases_plus_1_effect (green assertion on `group.useCases.size == 4` + `group.effects.size == 1`).

### CameraController.startTestRecording — D-05 no-audio strict gate

```kotlin
activeRecording = vc.output
    .prepareRecording(appContext, options)
    // NOTE: D-05 — do NOT call .withAudioEnabled(); RECORD_AUDIO is deferred to Phase 5.
    .start(ContextCompat.getMainExecutor(appContext)) { event -> … }
```

The only occurrences of the literal string `withAudioEnabled` in `CameraController.kt` are three COMMENT references (lines 57, 166, 190 — docstrings explaining why we don't call it). Strict grep for `\.withAudioEnabled\(` returns **0 matches** — T-02-06 enforcement verified.

### ListenableFuture.await() minimal bridge (Rule 3 for research §A5 drift)

```kotlin
private suspend fun <T> ListenableFuture<T>.await(): T =
    suspendCancellableCoroutine { cont ->
        addListener(
            {
                try {
                    cont.resume(get())
                } catch (e: ExecutionException) {
                    cont.resumeWithException(e.cause ?: e)
                } catch (t: Throwable) {
                    cont.resumeWithException(t)
                }
            },
            Runnable::run,
        )
        cont.invokeOnCancellation { cancel(true) }
    }
```

Bridges CameraX's `getInstance(ctx): ListenableFuture<ProcessCameraProvider>` into the suspend providerFactory seam without pulling kotlinx-coroutines-guava.

### CameraControllerTest — Robolectric + targets stub

```kotlin
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])  // CameraX Preview.Builder internals exercise android.util.ArrayMap
class CameraControllerTest {
    private fun buildController(mockProvider: ProcessCameraProvider): CameraController {
        …
        // UseCaseGroup.Builder.build() validates effect.getTargets() against a whitelist —
        // mock default 0 is rejected. Stub to the real PREVIEW|VIDEO|IMAGE mask (CAM-06).
        val mockEffect: OverlayEffect = mock<OverlayEffect>().stub {
            on { targets } doReturn (
                CameraEffect.PREVIEW or CameraEffect.VIDEO_CAPTURE or CameraEffect.IMAGE_CAPTURE
            )
        }
        …
        return CameraController(…, providerFactory = { mockProvider })
    }
}
```

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] CameraX 1.6.0 ProcessCameraProvider.awaitInstance does not exist (research §A5 drift)**

- **Found during:** Task 2 verification (`./gradlew :app:compileDebugKotlin` on the transliterated Sketch E source).
- **Issue:** 02-RESEARCH.md §Concrete Code Sketches E — transliterated verbatim per plan — contained `val provider = ProcessCameraProvider.awaitInstance(appContext)`. 02-RESEARCH.md §A5 claimed "`ProcessCameraProvider.awaitInstance(ctx)` is the CameraX 1.6 suspend-friendly API". A `javap` dump of `androidx.camera.lifecycle.ProcessCameraProvider` (1.6.0-api.jar) shows only static `getInstance(Context): ListenableFuture<ProcessCameraProvider>` — no `awaitInstance`. Compile failed: `Unresolved reference 'awaitInstance'`.
- **Root cause:** Either CameraX 1.6 never shipped `awaitInstance` (the research cited blog claims not materialized in the published AAR), or `awaitInstance` lives in a separate coroutines-helper module we don't depend on. Either way, the published 1.6.0 surface only exposes `getInstance(ListenableFuture)`.
- **Fix:** Rewrote the providerFactory default to `{ ctx -> ProcessCameraProvider.getInstance(ctx).await() }`. Added a minimal private suspend extension `ListenableFuture<T>.await(): T` using `suspendCancellableCoroutine` + `addListener(action, Runnable::run)` + `invokeOnCancellation { cancel(true) }` at the bottom of `CameraController.kt`. Avoids pulling `kotlinx-coroutines-guava` (not on the classpath); ~15 LOC overhead.
- **Files modified:** `app/src/main/java/com/bugzz/filter/camera/camera/CameraController.kt`
- **Commit:** `f3af46b`
- **Verification:** `./gradlew :app:compileDebugKotlin` succeeds; CameraControllerTest GREEN (provider seam exercised via mock).

**2. [Rule 3 - Blocking] VideoCapture.MIRROR_MODE_ON_FRONT_ONLY does not exist on VideoCapture; lives on MirrorMode**

- **Found during:** Task 2 verification (same compile pass as #1).
- **Issue:** 02-RESEARCH.md Sketch E and 02-04-PLAN.md both use `.setMirrorMode(VideoCapture.MIRROR_MODE_ON_FRONT_ONLY)`. The constant exists at `androidx.camera.core.MirrorMode.MIRROR_MODE_ON_FRONT_ONLY`, not on `VideoCapture`. `VideoCapture.getMirrorMode(): Int` exists but `VideoCapture.MIRROR_MODE_ON_FRONT_ONLY` does not.
- **Root cause:** Research cited `VideoCapture.Builder(...).setMirrorMode(VideoCapture.MIRROR_MODE_ON_FRONT_ONLY)` assuming the constant class-level alias. CameraX 1.6 keeps the constants only on the `MirrorMode` class.
- **Fix:** Added `import androidx.camera.core.MirrorMode`; changed call to `.setMirrorMode(MirrorMode.MIRROR_MODE_ON_FRONT_ONLY)`.
- **Files modified:** `app/src/main/java/com/bugzz/filter/camera/camera/CameraController.kt`
- **Commit:** `f3af46b`
- **Verification:** Compile GREEN.

**3. [Rule 3 - Blocking] Guava ListenableFuture not on compile classpath; listenablefuture stub jar is rewritten by Gradle**

- **Found during:** Task 2 verification (after applying fixes 1+2, compile still failed with `Unresolved reference 'common'` + `Cannot access class 'ListenableFuture'`).
- **Issue:** ProcessCameraProvider.getInstance returns `com.google.common.util.concurrent.ListenableFuture<ProcessCameraProvider>`. Guava is transitive at runtime via camera-core, but camera-core declares it `implementation` (not `api`), so it's invisible to our compile classpath. First attempt: added `com.google.guava:listenablefuture:1.0` (the standalone ~5KB interface-only jar). `./gradlew :app:dependencies` revealed: `com.google.guava:listenablefuture:1.0 -> 9999.0-empty-to-avoid-conflict-with-guava` — Gradle's conflict-resolution rewrites the stub jar to an empty artifact whenever guava is transitively on the graph (camera-core brings it).
- **Root cause:** Well-known Gradle + guava 21+ conflict-resolution policy. The stub jar is NEVER usable when guava is transitively present. Either use guava directly, or shadow.
- **Fix:** Removed the listenablefuture stub; added `com.google.guava:guava:33.3.1-android` (matches camera-core transitive) to `app/build.gradle.kts` + `libs.versions.toml`. Net runtime cost: zero (already present transitively); compile-classpath cost: ~2.7MB jar on Kotlin compiler classpath; binary cost: zero (R8 prunes unused guava in release — and we only use the `ListenableFuture` interface).
- **Files modified:** `gradle/libs.versions.toml`, `app/build.gradle.kts`
- **Commit:** `f3af46b` (combined with fixes 1+2)
- **Verification:** Compile GREEN; release assembly unaffected (not tested here — `./gradlew :app:assembleDebug` GREEN).

**4. [Rule 3 - Blocking] Mockito-only test harness cannot construct CameraX Preview.Builder (requires Robolectric)**

- **Found during:** Task 3 verification (`./gradlew :app:testDebugUnitTest --tests "*CameraControllerTest*"` after writing the test body per plan spec).
- **Issue:** Test ran Preview.Builder().build() indirectly via `controller.bind()`. First exception: `java.lang.RuntimeException: Method put in android.util.ArrayMap not mocked`. First attempted fix (add `testOptions { unitTests { isReturnDefaultValues = true } }` to `app/build.gradle.kts`) made the stub return null instead of throwing, which surfaced a second exception: `NullPointerException: Cannot invoke "java.util.Set.getClass()" because "<parameter1>" is null` at `Collections.unmodifiableSet` called from `OptionsBundle.from` in CameraX internals. **Mockito-only testing is fundamentally incompatible with CameraX 1.6 UseCase builders** because they exercise framework Android classes that the JVM `android.jar` stubs out.
- **Root cause:** Plan 02-01 assumed CameraX builder construction could be tested with Mockito alone (Plan 01 Nyquist designer didn't run the test; it was @Ignore'd). CameraX 1.6 exercises ArrayMap (+ by transitive need, Looper, Handler, Color via Paint in OverlayEffect's error listener). Robolectric is the canonical Android-JVM unit-test runner for this class of SUT — it provides framework shadows.
- **Fix:** 
  1. Added `org.robolectric:robolectric:4.13` as `testImplementation`.
  2. Annotated CameraControllerTest with `@RunWith(RobolectricTestRunner::class)` + `@Config(sdk = [34])` (Robolectric 4.13 bundles Android shadows up to API 34; compileSdk=36 is unrelated — Robolectric runs on shadow-SDK 34).
  3. Kept `testOptions.unitTests.isReturnDefaultValues = true` as defense-in-depth (harmless with Robolectric active).
- **Files modified:** `gradle/libs.versions.toml` (+robolectric 4.13), `app/build.gradle.kts` (+testImplementation + testOptions), `app/src/test/java/com/bugzz/filter/camera/camera/CameraControllerTest.kt` (+@RunWith +@Config)
- **Commit:** `0d75c6c`
- **Verification:** Both CameraControllerTest methods GREEN. Full `./gradlew :app:testDebugUnitTest` exits 0 with 10 tests / 0 failures / 0 skipped.

**5. [Rule 3 - Blocking] Mockito OverlayEffect.getTargets() returns 0; UseCaseGroup.Builder rejects**

- **Found during:** Task 3 verification (after applying Robolectric fix #4, new test exception: `IllegalArgumentException: Effects target  is not in the supported list`).
- **Issue:** `UseCaseGroup.Builder.checkEffectTargets` validates `effect.getTargets()` against the whitelist `[PREVIEW, VIDEO_CAPTURE, IMAGE_CAPTURE, PREVIEW|VIDEO_CAPTURE, IMAGE_CAPTURE|PREVIEW|VIDEO_CAPTURE]`. The mock `OverlayEffect` returns Mockito default 0 for `getTargets()` — NOT in the whitelist — rejected at `UseCaseGroup.Builder.build()`.
- **Root cause:** Mock default primitive returns are 0/false/null. CameraX validates the value.
- **Fix:** Added a `.stub { on { targets } doReturn (CameraEffect.PREVIEW or VIDEO_CAPTURE or IMAGE_CAPTURE) }` block on the mockEffect — mirrors the real `OverlayEffectBuilder.TARGETS` constant. This is a test-setup deviation, not an SUT-behavior deviation.
- **Files modified:** `app/src/test/java/com/bugzz/filter/camera/camera/CameraControllerTest.kt`
- **Commit:** `0d75c6c` (combined with Robolectric introduction)
- **Verification:** Both tests GREEN.

**Total deviations:** 5 auto-fixed (all Rule 3 blocking). No Rule 4 escalations. Four of the five are research / plan drift against the published CameraX 1.6.0 AAR surface (awaitInstance, MIRROR_MODE location, ListenableFuture classpath, test-harness compatibility); one is a Mockito mock-stubbing detail that only surfaces when real CameraX validation code runs.

## Authentication Gates

None — plan is pure-Kotlin SUT code + Robolectric unit tests, no network, no external services, no device interaction.

## Issues Encountered

- **Windows CRLF warnings:** Git continues to warn `LF will be replaced by CRLF the next time Git touches it` on every committed `.kt` file. Benign on Windows host; matches repo convention.
- **Robolectric 4.13 first-run shadow download (~50 MB):** First `./gradlew :app:testDebugUnitTest` with Robolectric in the classpath downloads Android framework shadow JARs from maven central; subsequent runs use the cache. One-time cost; not blocking but visible in the 1m 51s first-run test duration vs 10s subsequent.
- **Deprecated Gradle features warning on every build:** Standard AGP 8.9 output; acknowledged in Phase 1 / 2 already, not new.

## User Setup Required

None — no adb, no device, no keys. Plan 02-06 runbook (next) is the on-device verification step for Phase 2's five success criteria.

## Known Stubs

**None introduced by this plan.** The Plan 02-03 compile-unblock stubs for `OverlayEffectBuilder.kt` and `CameraController.kt` have been **fully REPLACED with production implementations** as the plan mandated. `FaceLandmarkMapper.anchorPoint()` still returns `null` per its Phase 3 scope (not touched by this plan).

Verified-not-stub check:
- `OverlayEffectBuilder.TARGETS` — real `PREVIEW or VIDEO_CAPTURE or IMAGE_CAPTURE` disjunction (not placeholder 0)
- `OverlayEffectBuilder.QUEUE_DEPTH` — real `0` (not placeholder -1)
- `OverlayEffectBuilder.build()` — constructs real `OverlayEffect` (not `NotImplementedError`)
- `CameraController.bind()` — full UseCaseGroup wiring (not `NotImplementedError`)

## Threat Flags

No new trust boundaries introduced. This plan executes `<threat_model>` mitigations for T-02-02, T-02-03, T-02-05, T-02-06, T-02-07 as planned:
- T-02-02 (biometric data in saved media): `DebugOverlayRenderer.draw` FIRST line `if (!BuildConfig.DEBUG) return` — verified by grep.
- T-02-03 (MediaStore write visibility): filename pattern `bugzz_test_${timestamp}.mp4` grep-stable; Plan 02-06 handoff documents.
- T-02-05 (lifecycle leak on flip): `flipLens` → `bind` → `provider.unbindAll() + bindToLifecycle(...)`; no manual `unbindAll` elsewhere.
- T-02-06 (accidental audio enable): `\.withAudioEnabled\(` grep returns **0** matches in `CameraController.kt` (only docstring references to "no withAudioEnabled").
- T-02-07 (OverlayEffect lifecycle churn): `overlayEffect` is a property init single instance; no `overlayEffectBuilder.build()` call inside `bind()`.

## Self-Check: PASSED

- [x] `app/src/main/java/com/bugzz/filter/camera/render/DebugOverlayRenderer.kt` exists
- [x] `app/src/main/java/com/bugzz/filter/camera/render/OverlayEffectBuilder.kt` replaced (not edited on top of stub)
- [x] `app/src/main/java/com/bugzz/filter/camera/camera/CameraController.kt` replaced (not edited on top of stub)
- [x] `DebugOverlayRenderer.draw` first line is `if (!BuildConfig.DEBUG) return` (verified by code review + acceptance criteria grep)
- [x] `DebugOverlayRenderer.draw` iterates `for (face in snapshot.faces)` — ALL faces (D-23)
- [x] `OverlayEffectBuilder.TARGETS = CameraEffect.PREVIEW or CameraEffect.VIDEO_CAPTURE or CameraEffect.IMAGE_CAPTURE` (exact string)
- [x] `OverlayEffectBuilder.QUEUE_DEPTH = 0` (exact const)
- [x] `HandlerThread("BugzzRenderThread")` present (dedicated thread, not main looper)
- [x] `canvas.setMatrix(frame.sensorToBufferTransform)` appears in `setOnDrawListener` body BEFORE `renderer.draw(...)`
- [x] `CameraController` has `providerFactory: suspend (Context) -> ProcessCameraProvider = { ctx -> ProcessCameraProvider.getInstance(ctx).await() }` constructor default
- [x] `overlayEffectBuilder.build()` called ONCE in property initializer, not inside `bind()` (D-25)
- [x] `STRATEGY_KEEP_ONLY_LATEST` present (CAM-05)
- [x] `Size(720, 1280)` present (D-16)
- [x] `FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER` present
- [x] `MirrorMode.MIRROR_MODE_ON_FRONT_ONLY` present (D-05 mirror parity with reference)
- [x] Exactly 4 `addUseCase(` calls + exactly 1 `addEffect(` call
- [x] `DEFAULT_FRONT_CAMERA` and `DEFAULT_BACK_CAMERA` in `when (lens)` branch
- [x] `faceDetector.onLensFlipped()` called BEFORE `bind(` inside `flipLens()` (PITFALLS #6)
- [x] `RELATIVE_PATH, "DCIM/Bugzz"` present (D-04 save location)
- [x] `\.withAudioEnabled\(` grep returns 0 matches (D-05 enforcement)
- [x] `CameraControllerTest` no longer contains `@Ignore` annotation
- [x] Test file contains `@RunWith(RobolectricTestRunner::class)` + `@Config(sdk = [34])`
- [x] Test contains `providerFactory = { mockProvider }` (seam used)
- [x] Test contains `argumentCaptor<UseCaseGroup>()` (mockito-kotlin DSL)
- [x] Test stubs `on { targets } doReturn (CameraEffect.PREVIEW or VIDEO_CAPTURE or IMAGE_CAPTURE)`
- [x] `./gradlew :app:compileDebugKotlin` exits 0
- [x] `./gradlew :app:testDebugUnitTest` exits 0 — 10 tests / 0 failures / 0 skipped
- [x] `./gradlew :app:assembleDebug` exits 0 — `app-debug.apk` produced
- [x] Commit `a6ea233` (feat(02-04-01) DebugOverlayRenderer + OverlayEffectBuilder) present in `git log`
- [x] Commit `f3af46b` (feat(02-04-02) CameraController) present in `git log`
- [x] Commit `0d75c6c` (test(02-04-03) un-@Ignore CameraControllerTest) present in `git log`

## Next Plan Readiness

- **Plan 02-05 (CameraViewModel + CameraScreen Compose UI) unblocked:**
  - `CameraController.surfaceRequest: StateFlow<SurfaceRequest?>` ready for `collectAsStateWithLifecycle` in `CameraScreen.kt` composable → pass to `CameraXViewfinder`.
  - `CameraController.bind(owner, lens, rotation)` ready to call from `CameraViewModel.init` (use `viewModelScope.launch { controller.bind(lifecycleOwner, state.lens, initialRotation) }`).
  - `CameraController.flipLens(owner, newLens, rotation)` exposed for flip button tap handler; uses `faceDetector.onLensFlipped()` internally.
  - `CameraController.setTargetRotation(rotation)` exposed for `OrientationEventListener` wire-up (D-08).
  - `CameraController.startTestRecording(onFinalized)` exposed for debug TEST RECORD button in `CameraScreen.kt` (D-04 — debug-only, gated by `BuildConfig.DEBUG`).
  - `CameraController.stopTestRecording()` exposed for 5s auto-stop via `viewModelScope.launch { delay(5_000L); controller.stopTestRecording() }` or similar.
  - `CameraLensProvider.next(current)` (from Plan 02-03) consumed for the toggle logic.

- **Plan 02-06 (device handoff runbook) transitively unblocked** — real SUTs all in place; Plan 02-06 assembles a debug APK and runs the 5-point on-device verification (overlay pixel-alignment × 4 orientations × 2 lenses + 5s recording playback + trackingId stability in logcat).

- **Phase 3 pre-readiness:** `OverlayEffect` pipeline architecture is now validated end-to-end at unit-test level; Phase 3's `FilterEngine` replaces `DebugOverlayRenderer.draw()` at the same call site without touching `OverlayEffectBuilder` or `CameraController` (per D-25 / research §Open Questions #2). `DebugOverlayRenderer` remains on-disk as a BuildConfig.DEBUG companion for ongoing dev.

---
*Phase: 02-camera-preview-face-detection-coordinate-validation*
*Completed: 2026-04-19*
