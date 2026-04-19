---
phase: 02-camera-preview-face-detection-coordinate-validation
plan: 03
subsystem: detector-pipeline
tags: [mlkit-face-detection-16.1.7, 1-euro-filter, camerax-mlkit-analyzer, atomicreference, hilt-singleton, nyquist-green]

# Dependency graph
requires:
  - phase: 01-foundation-skeleton
    provides: Hilt @Inject/@Singleton baseline; BugzzApplication Timber.DebugTree in debug guard; CameraX + ML Kit 16.1.7 on classpath (from plan 02-02)
  - plan: 02-01
    provides: Nyquist RED OneEuroFilterTest + FaceDetectorOptionsTest (this plan turns both GREEN)
  - plan: 02-02
    provides: @Named("cameraExecutor") single-thread Executor provider in CameraModule (D-18); mockito-kotlin testImplementation (compile unblock for CameraControllerTest); Timber.DebugTree planted under BuildConfig.DEBUG guard (T-02-02 gate)
provides:
  - OneEuroFilter (Casiez CHI 2012 port) + LandmarkSmoother with per-trackingId × per-channel state (CAM-09 / D-20/21/22)
  - FaceSnapshot + SmoothedFace immutable data classes as D-19 AtomicReference payload contract
  - FaceDetectorClient @Singleton producing MlKitAnalyzer(COORDINATE_SYSTEM_SENSOR) + AtomicReference<FaceSnapshot> writer + Timber "FaceTracker" verbose logs (CAM-04/05/08, D-15/17/19/20-22)
  - FaceLandmarkMapper Phase 3 stub (Anchor enum + anchorPoint() returning null)
  - CameraLensProvider enum {FRONT, BACK} + next() toggle (consumed by Plan 02-05 ViewModel)
  - Plan 02-04 compile-unblock stubs: OverlayEffectBuilder (wrong TARGETS/QUEUE_DEPTH to keep OverlayEffectBuilderTest RED) + CameraController (NotImplementedError body for @Ignored CameraControllerTest)
  - OneEuroFilterTest GREEN (4/4) and FaceDetectorOptionsTest GREEN (1/1) — CAM-04 + CAM-09 verified at unit-test level
affects: [plan-02-04, plan-02-05, plan-02-06, phase-03]

# Tech tracking
tech-stack:
  added: []  # No new runtime deps — all needed libs pulled by Plan 02-02
  patterns:
    - "**Per-trackingId × per-channel 1€ filter state map:** LandmarkSmoother keys filters on `$trackingId:$landmarkName:$channel` so tracking-loss clears only that face's state (retainActive) while other faces keep their converged filters. Plain HashMap — no locks needed because cameraExecutor is single-threaded (D-18). PITFALLS #6 (stale state on lens flip) handled via clear()."
    - "**AtomicReference lock-free handoff for cross-thread face data:** FaceDetectorClient.latestSnapshot: AtomicReference<FaceSnapshot> — cameraExecutor writes (single writer), renderExecutor will read on each draw tick (multi reader). FaceSnapshot is immutable (kotlin data class with List/Map<Int,PointF>), so readers get a stable view of all faces at that analyzer tick. No Mutex, no synchronized — intentional per D-19."
    - "**MlKitAnalyzer-over-manual-detection:** FaceDetectorClient.createAnalyzer() returns `MlKitAnalyzer(listOf(detector), COORDINATE_SYSTEM_SENSOR, cameraExecutor, consumer)`. No `imageProxy.close()`, no `detector.process(inputImage)` — the analyzer handles both internally (PITFALLS #4, #13 avoided). COORDINATE_SYSTEM_SENSOR matches OverlayEffect.onDrawListener's `canvas.setMatrix(frame.sensorToBufferTransform)` (D-17); using VIEW_REFERENCED would defeat the zero-manual-matrix-math contract of CAM-07."
    - "**T-02-06 information-disclosure mitigation in FaceTracker logs:** Verbose log format is pinned to `\"t=%d id=%s bb=%d,%d contours=%d\"` — emits timestamp, trackingId, boundingBox center (one pair), and contour array SIZE. Never logs raw contour coordinate lists. Prevents reconstructing face geometry from logcat even in debug builds. Combined with T-02-02's DebugTree gate (no tree in release), face-adjacent data stays in-process."
    - "**Plan-02-04 compile-unblock placeholder stubs:** When a Nyquist-RED test file from an earlier plan references SUTs that won't land until a later plan, that test blocks the whole test-sourceset from compiling (Gradle compiles all tests before running any). Ship minimal placeholder SUT files (`OverlayEffectBuilder` with deliberately-wrong companion constants; `CameraController` with NotImplementedError body) so compile succeeds while runtime RED is preserved (failing tests for OverlayEffectBuilder; @Ignore for CameraController). Inline doc comments in each stub point at the later plan that replaces them."

key-files:
  created:
    - app/src/main/java/com/bugzz/filter/camera/detector/OneEuroFilter.kt
    - app/src/main/java/com/bugzz/filter/camera/detector/FaceSnapshot.kt
    - app/src/main/java/com/bugzz/filter/camera/detector/FaceLandmarkMapper.kt
    - app/src/main/java/com/bugzz/filter/camera/detector/FaceDetectorClient.kt
    - app/src/main/java/com/bugzz/filter/camera/camera/CameraLensProvider.kt
    - app/src/main/java/com/bugzz/filter/camera/render/OverlayEffectBuilder.kt
    - app/src/main/java/com/bugzz/filter/camera/camera/CameraController.kt
    - .planning/phases/02-camera-preview-face-detection-coordinate-validation/02-03-SUMMARY.md
  modified:
    - app/src/test/java/com/bugzz/filter/camera/detector/FaceDetectorOptionsTest.kt

key-decisions:
  - "**FaceDetectorOptionsTest verification strategy rewritten to toString + equals.** Plan 01's per-field Kotlin-property accessors (opts.performanceMode, opts.isTrackingEnabled, etc.) don't exist — ML Kit's published AAR is R8-minified, all accessors obfuscated to zza()..zzg(). Rewrote test to combine equals() comparison against expected options (definitive 6-field gate) with toString() substring assertions per field (diagnostic clarity). Contract coverage identical; failure messages now inspect the actual toString output. Rule 3 auto-fix in 02-03-SUMMARY.md Deviations."
  - "**Compile-unblock stubs over scope-expanding fixup.** Plan 01 committed OverlayEffectBuilderTest + CameraControllerTest which reference Plan 02-04 SUTs. Those Unresolved references blocked the whole test-sourceset from compiling — not just those specific tests. Chose to ship thin placeholder OverlayEffectBuilder + CameraController files (with inline 'lands in Plan 02-04' comments) rather than (a) delete the tests, (b) rewrite the plan, or (c) move the two blocking tests into a separate sourceset. Placeholder values keep OverlayEffectBuilderTest intentionally RED at runtime."
  - "**Literature-default 1€ filter params preserved.** minCutoff=1.0, beta=0.007, dCutoff=1.0 — exactly the D-20 defaults, transliterated from Casiez CHI 2012 reference. No empirical tuning in Phase 2; Phase 3 revisits after sprite rendering reveals whether current smoothing passes the <1px/frame jitter success criterion."
  - "**FaceDetectorClient SMOOTHED_CONTOUR_TYPES explicit list of 9 FaceContour ints.** Rather than iterating `face.allContours` (all 9 + sub-contours), whitelist the 9 specific FaceContour.Type ints the app cares about for sprite anchoring later (FACE, NOSE_BRIDGE, NOSE_BOTTOM, LEFT_EYE, RIGHT_EYE, LEFT_CHEEK, RIGHT_CHEEK, UPPER_LIP_TOP, LOWER_LIP_BOTTOM). Rules out UPPER_LIP_BOTTOM + LOWER_LIP_TOP which are effectively the same lip contours reversed. Keeps smoother state map smaller + matches Phase 3 anchor plan."
  - "**Timber FaceTracker log format locked to T-02-06 shape.** `\"t=%d id=%s bb=%d,%d contours=%d\"` with per-face call in a simple forEach. Never logs the contour point lists themselves. Log shape is grep-stable for the Phase 2-06 runbook step 4 (verify trackingId stability over 60+ frames via logcat). Release builds plant no Timber tree → these calls become no-ops (T-02-02 gate)."

patterns-established:
  - "**R8-minified AAR accessor workaround via toString() + equals():** When a third-party library ships an R8-obfuscated value class whose public accessors become zza()..zzg(), unit tests verifying its configuration must use (1) equals() against a hand-built expected instance for the definitive pass/fail gate, and (2) toString() substring assertions per field for diagnostic clarity. Reflection on obfuscated fields is brittle; Kotlin property-syntax on `zza`/`zzb` fails. This pattern applies to any future ML Kit options class, Play Services builder output, etc."
  - "**Placeholder-stub pattern for blocking-test-SUT cross-plan dependencies:** When plan N's test sourceset already references SUTs that land in plan N+K, ship minimal stub SUTs in plan N+1..N+(K-1) so the test compiles but the contract remains RED at runtime (wrong constants) or skipped (@Ignore). Inline doc comment must point forward at the landing plan. Alternative (move tests into separate sourceset, delete-and-re-add) would break git blame and require build config changes. This is a Rule 3 fix, not an architectural deviation."
  - "**Per-trackingId LandmarkSmoother retainActive in every analyzer tick:** retainActive(activeIds) is called FIRST each tick, before any smoothing, where activeIds = faces.mapNotNull { it.trackingId }.toSet(). Guarantees the filter map is bounded by current face count — on empty-face frames, activeIds = emptySet() → all state cleared. T-02-08 (unbounded map growth DoS) mitigated by default. Pattern: unbounded per-entity-id cache maps require eviction on every write path, never only on entity disappear."

requirements-completed: [CAM-04, CAM-08, CAM-09]
# Note: CAM-05 (KEEP_ONLY_LATEST) is deferred to Plan 02-04 (lives in CameraController.bind).
# CAM-04 GREEN: FaceDetectorOptionsTest passes against buildOptions(); D-15 pinned.
# CAM-08 GREEN: enableTracking() in buildOptions + Timber FaceTracker logs enable Phase 06 runbook verification.
# CAM-09 GREEN: OneEuroFilterTest passes — 1€ filter math correct per literature.

# Metrics
duration: 8m 07s
completed: 2026-04-19
---

# Phase 2 Plan 3: Detector Pipeline (OneEuroFilter + FaceDetectorClient + FaceSnapshot) Summary

**Transliterated 02-RESEARCH.md Sketches A + B into 5 detector/camera files (~324 LOC) + 2 Plan 02-04 compile-unblock stubs (~76 LOC). OneEuroFilterTest and FaceDetectorOptionsTest both GREEN. FaceDetectorOptionsTest rewritten to work around FaceDetectorOptions R8 obfuscation (Rule 3 auto-fix). OverlayEffectBuilderTest + CameraControllerTest remain RED / @Ignored until Plan 02-04 lands the real SUTs.**

## Performance

- **Duration:** 8 min 07 s (wall clock)
- **Started:** 2026-04-19T08:57:09Z
- **Completed:** 2026-04-19T09:05:16Z
- **Tasks:** 3 primary (per 02-03-PLAN.md) + 1 Rule 3 auto-fix commit
- **Files created:** 7 (5 per plan spec + 2 compile-unblock stubs); **Files modified:** 1 (FaceDetectorOptionsTest.kt)
- **LOC added:** 382 total across 7 .kt files (95 OneEuroFilter + 37 FaceSnapshot + 29 FaceLandmarkMapper + 127 FaceDetectorClient + 18 CameraLensProvider + 41 OverlayEffectBuilder stub + 35 CameraController stub)

## Accomplishments

- **Task 1 — OneEuroFilter.kt (95 LOC):** `OneEuroFilter(minCutoff=1.0, beta=0.007, dCutoff=1.0)` class with `filter(x: Double, tNanos: Long): Double` + `reset()` — Casiez CHI 2012 algorithm transliterated from 02-RESEARCH.md Sketch A verbatim. `LandmarkSmoother(minCutoff, beta, dCutoff)` holds per-trackingId × per-channel OneEuroFilter state in a HashMap keyed by `$trackingId:$landmarkName:$channel`. `retainActive(activeIds: Set<Int>)` removes state for absent trackingIds (D-22); `clear()` empties the map (D-25 / PITFALLS #6 for lens flip). Plan 01's `OneEuroFilterTest` turns GREEN (4/4 pass: passthrough, step convergence, sine attenuation, divide-by-zero safety).
- **Task 2 — FaceSnapshot.kt (37 LOC) + FaceLandmarkMapper.kt (29 LOC) + CameraLensProvider.kt (18 LOC):**
  - `SmoothedFace(trackingId, boundingBox, contours, landmarks)` + `FaceSnapshot(faces, timestampNanos)` with `EMPTY` companion singleton — D-19 AtomicReference payload.
  - `FaceLandmarkMapper` object with nested `enum class Anchor { NOSE_TIP, FOREHEAD, LEFT_CHEEK, RIGHT_CHEEK, CHIN, LEFT_EYE, RIGHT_EYE }` + `anchorPoint(face, anchor): PointF?` stub returning `null` (Phase 3 scope).
  - `enum class CameraLens { FRONT, BACK }` + `object CameraLensProvider { fun next(current): CameraLens }` — lens flip toggle consumed by Plan 02-05 ViewModel.
- **Task 3 — FaceDetectorClient.kt (127 LOC):** `@Singleton class FaceDetectorClient @Inject constructor(@Named("cameraExecutor") cameraExecutor: Executor)` — D-18 wiring. `companion object { fun buildOptions(): FaceDetectorOptions }` exposing all 6 D-15 configurators exactly (FAST + CONTOUR_ALL + LANDMARK_NONE + CLASSIFICATION_NONE + enableTracking + 0.15f). `createAnalyzer(): MlKitAnalyzer` constructs with `COORDINATE_SYSTEM_SENSOR` (D-17). Analyzer consumer reads `result.getValue(detector)`, computes `tNanos`, calls `smoother.retainActive(activeIds)` FIRST, maps faces to SmoothedFace via `smoothFace()` (per-contour-type smoothing keyed on trackingId), writes `latestSnapshot.set(FaceSnapshot(smoothed, tNanos))`, emits `Timber.tag("FaceTracker").v(...)` per face. `onLensFlipped()` calls `smoother.clear()`. `close()` releases ML Kit detector. Plan 01's `FaceDetectorOptionsTest` turns GREEN (1/1 pass) after Rule 3 test rewrite.

## Task Commits

Each task (and the Rule 3 auto-fix) was committed atomically:

1. **Task 1 — OneEuroFilter + LandmarkSmoother** — `2508af9` (feat)
2. **Task 2 — FaceSnapshot + FaceLandmarkMapper + CameraLensProvider** — `e05d37d` (feat)
3. **Task 3 — FaceDetectorClient** — `2aa5098` (feat)
4. **Rule 3 auto-fix — FaceDetectorOptionsTest rewrite + Plan 02-04 compile-unblock stubs (OverlayEffectBuilder + CameraController)** — `611ebb8` (fix)

(Plan-level docs commit for this SUMMARY.md + STATE.md + ROADMAP.md comes in the final-commit step below.)

## Quoted Diffs

### OneEuroFilter.kt — constructor + filter() signature (pins Plan 01 contract)

```kotlin
class OneEuroFilter(
    private val minCutoff: Double = 1.0,
    private val beta: Double = 0.007,
    private val dCutoff: Double = 1.0,
) {
    fun filter(x: Double, tNanos: Long): Double { … }
    fun reset() { initialized = false }
}

class LandmarkSmoother(
    private val minCutoff: Double = 1.0,
    private val beta: Double = 0.007,
    private val dCutoff: Double = 1.0,
) {
    fun smoothPoint(trackingId: Int, landmarkName: String, xRaw: Float, yRaw: Float, tNanos: Long): Pair<Float, Float>
    fun retainActive(activeIds: Set<Int>)
    fun clear()
}
```

### FaceDetectorClient.kt — D-15 companion + MlKitAnalyzer + AtomicReference

```kotlin
@Singleton
class FaceDetectorClient @Inject constructor(
    @Named("cameraExecutor") private val cameraExecutor: Executor,
) {
    private val detector: FaceDetector = FaceDetection.getClient(buildOptions())
    private val smoother = LandmarkSmoother(minCutoff = 1.0, beta = 0.007, dCutoff = 1.0)
    val latestSnapshot: AtomicReference<FaceSnapshot> = AtomicReference(FaceSnapshot.EMPTY)

    fun createAnalyzer(): MlKitAnalyzer = MlKitAnalyzer(
        listOf(detector), COORDINATE_SYSTEM_SENSOR, cameraExecutor,
    ) { result ->
        val faces = result.getValue(detector) ?: emptyList()
        val tNanos = System.nanoTime()
        smoother.retainActive(faces.mapNotNull { it.trackingId }.toSet())
        val smoothedFaces = faces.map { smoothFace(it, tNanos) }
        latestSnapshot.set(FaceSnapshot(smoothedFaces, tNanos))
        faces.forEach { f ->
            Timber.tag("FaceTracker").v("t=%d id=%s bb=%d,%d contours=%d",
                tNanos, f.trackingId?.toString() ?: "null",
                f.boundingBox.centerX(), f.boundingBox.centerY(), f.allContours.size)
        }
    }

    fun onLensFlipped() = smoother.clear()
    fun close() = detector.close()

    companion object {
        fun buildOptions(): FaceDetectorOptions =
            FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
                .enableTracking()
                .setMinFaceSize(0.15f)
                .build()
    }
}
```

### FaceDetectorOptionsTest.kt — Rule 3 rewrite (equals + toString hybrid)

**Before (Plan 01 — does not compile against R8-minified ML Kit AAR):**
```kotlin
assertEquals(FaceDetectorOptions.PERFORMANCE_MODE_FAST, opts.performanceMode)  // FAILS — no property
assertTrue(opts.isTrackingEnabled)                                              // FAILS — no property
```

**After (Plan 02-03 Rule 3 fix):**
```kotlin
val expected = FaceDetectorOptions.Builder()
    .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
    .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
    .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
    .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
    .enableTracking()
    .setMinFaceSize(0.15f)
    .build()
val actualStr = opts.toString()
// 6 per-field toString substring assertions for diagnostic clarity
assertTrue("performanceMode must be FAST; toString=$actualStr",
    actualStr.contains("performanceMode=${FaceDetectorOptions.PERFORMANCE_MODE_FAST}"))
// …5 more per-field assertions…
assertEquals("options must equal D-15 expected", expected, opts)  // definitive gate
```

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] FaceDetectorOptions R8 obfuscation defeats per-field property accessors**

- **Found during:** Task 3 verification (`./gradlew :app:compileDebugUnitTestKotlin` after FaceDetectorClient landed)
- **Issue:** Plan 01's FaceDetectorOptionsTest.kt accessed Kotlin-property-syntax getters on the options instance — `opts.performanceMode`, `opts.contourMode`, `opts.isTrackingEnabled`, `opts.minFaceSize`, `opts.landmarkMode`, `opts.classificationMode`. The published ML Kit face-detection 16.1.7 AAR is R8-minified — all accessors are obfuscated to `zza()`..`zzg()` with names like `final int zzb()` / `final int zzc()`. No Kotlin-property-compatible getters exist. `javap` dump of FaceDetectorOptions.class confirmed: public methods are `hashCode`, `toString`, `equals`, and 7 obfuscated `zza()`..`zzg()` accessors. Test would not compile against the real AAR.
- **Root cause:** Plan 01 was authored without the ML Kit dep on the classpath (deps landed in Plan 02-02), so the test file's Kotlin-property access syntax was not type-checked against the actual API surface until Plan 02-03 integrated against the live JAR.
- **Fix:** Rewrote FaceDetectorOptionsTest.options_configured_per_D15 to use a hybrid verification strategy:
  1. Six per-field `toString()` substring assertions for diagnostic clarity (e.g., `actualStr.contains("performanceMode=${FaceDetectorOptions.PERFORMANCE_MODE_FAST}")`).
  2. One definitive `assertEquals(expected, opts)` call where `expected` is built with the exact D-15 configurators — ML Kit's `equals()` override covers all 6 fields, so any drift fails.
  Added a documentation comment at the top of the test explaining the R8 workaround for future maintainers.
  Observed toString format: `FaceDetectorOptions{landmarkMode=1, contourMode=2, classificationMode=1, performanceMode=1, trackingEnabled=true, minFaceSize=0.15}` — note the key is `trackingEnabled` (not `isTrackingEnabled`); corrected in a second iteration.
- **Files modified:** `app/src/test/java/com/bugzz/filter/camera/detector/FaceDetectorOptionsTest.kt`
- **Commit:** `611ebb8`
- **Verification:** `./gradlew :app:testDebugUnitTest --tests "*FaceDetectorOptionsTest*"` exits 0; FaceDetectorOptionsTest GREEN (1/1).

**2. [Rule 3 - Blocking] Test sourceset compilation blocked by Plan 02-04 SUT references in Plan 01 test files**

- **Found during:** Task 3 verification (attempting `./gradlew :app:testDebugUnitTest --tests "*OneEuroFilterTest*"`)
- **Issue:** Plan 01 committed `OverlayEffectBuilderTest.kt` and `CameraControllerTest.kt` which reference `com.bugzz.filter.camera.render.OverlayEffectBuilder` (2 references) and `com.bugzz.filter.camera.camera.CameraController` (2 references) — SUTs not landing until Plan 02-04. Gradle compiles the WHOLE test sourceset before running ANY test, so these Unresolved references blocked compilation of OneEuroFilterTest + FaceDetectorOptionsTest too. Plan 02-03's `<verification>` required both to run GREEN.
- **Root cause:** Plan 02-01's Nyquist-TDD pattern committed failing tests for CAM-03/04/05/06/09 in Wave 0, but Gradle's per-sourceset-compilation model means a missing SUT in any test file blocks all tests. Plan 02-01-SUMMARY.md's "intentional Nyquist RED" description was technically accurate for how RED manifests at compile time, but the phase was structured so that Plan 02-03 must close `OneEuroFilterTest` + `FaceDetectorOptionsTest` while 02-04 closes the other two — Plan 02-03 therefore needs an escape hatch.
- **Fix:** Shipped minimal placeholder SUT files for OverlayEffectBuilder + CameraController:
  - `app/src/main/java/com/bugzz/filter/camera/render/OverlayEffectBuilder.kt` (41 LOC) — `class OverlayEffectBuilder { fun build(): OverlayEffect = throw NotImplementedError(...) }` + `companion object { const val TARGETS: Int = 0; const val QUEUE_DEPTH: Int = -1 }`. Deliberately-wrong constants keep `OverlayEffectBuilderTest` intentionally RED at runtime (2/2 fail) per Plan 02-03's "remain RED" spec.
  - `app/src/main/java/com/bugzz/filter/camera/camera/CameraController.kt` (35 LOC) — `class CameraController(appContext, cameraExecutor, faceDetector, overlayEffectBuilder) { suspend fun bind(owner, lens, rotation) = throw NotImplementedError(...) }`. Both `CameraControllerTest` methods are already `@Ignore`d in Plan 01, so the stub body is never exercised at runtime.
  - Inline KDoc comments in both files point forward at Plan 02-04 and enumerate what the real implementation must do.
- **Files modified:** `app/src/main/java/com/bugzz/filter/camera/render/OverlayEffectBuilder.kt` (created), `app/src/main/java/com/bugzz/filter/camera/camera/CameraController.kt` (created)
- **Commit:** `611ebb8` (combined with FaceDetectorOptionsTest rewrite)
- **Verification:** Full `./gradlew :app:testDebugUnitTest` now runs and reports: 10 tests total; 5 pass (OneEuroFilterTest 4/4 + ExampleUnitTest 1/1) + 1 pass (FaceDetectorOptionsTest 1/1) + 2 fail intentionally (OverlayEffectBuilderTest — RED preserved) + 2 skipped (CameraControllerTest — @Ignore preserved). Exactly matches Plan 02-03's `<verification>` expectation.

**Total deviations:** 2 auto-fixed (both Rule 3 blocking). No Rule 4 escalations.

## Authentication Gates

None — plan is pure-Kotlin unit-testable code, no network, no external services, no device interaction.

## Issues Encountered

- **Windows CRLF warnings:** Git warned `LF will be replaced by CRLF the next time Git touches it` on every committed `.kt` file. Benign on Windows hosts; matches repo convention from Phases 1–2.
- **ML Kit face-detection artifact dual-sourced:** Both `com.google.mlkit:face-detection:16.1.7` (bundled model + internal zz* classes) and `com.google.android.gms:play-services-mlkit-face-detection:17.1.0` (FaceDetectorOptions API surface) end up on the classpath via transitive resolution. The API class FaceDetectorOptions lives in the gms artifact, not the bundled one — discovered during javap investigation. No action needed — Hilt + Gradle resolve it correctly.

## User Setup Required

None — no adb, no device, no keys.

## Known Stubs

Two intentional placeholder stubs ship as part of this plan's Rule 3 auto-fix:

1. **`OverlayEffectBuilder.kt` — `TARGETS = 0`, `QUEUE_DEPTH = -1`, `build() → NotImplementedError`.** Plan 02-04 replaces with the real `CameraEffect.PREVIEW or VIDEO_CAPTURE or IMAGE_CAPTURE` target mask + `0` queue depth + real `OverlayEffect(targets, handler, queueDepth, listener)` construction.
2. **`CameraController.kt` — `bind() → NotImplementedError`, no `providerFactory` param yet.** Plan 02-04 replaces with the real 4-usecase + 1-effect `UseCaseGroup` + `STRATEGY_KEEP_ONLY_LATEST` + `providerFactory: suspend (Context) -> ProcessCameraProvider = { ProcessCameraProvider.awaitInstance(it) }` constructor default (per 02-01-SUMMARY.md provider-factory seam requirement).

Both stubs carry forward-pointing inline KDoc comments and are explicitly flagged here. **Plan 02-04 MUST fully replace both files — these stubs are not a partial implementation to build on top of, they are placeholders to discard.**

No other stubs. `FaceLandmarkMapper.anchorPoint()` returns `null` per plan spec (Phase 3 stub, explicitly scoped in Plan 02-03 `<objective>`); that is not a stub introduced here but a stub defined by the plan.

## Next Plan Readiness

- **Plan 02-04 (render + camera lifecycle layer) can proceed:**
  - `FaceDetectorClient` @Singleton is Hilt-injectable; `createAnalyzer()` returns `MlKitAnalyzer` ready to attach via `ImageAnalysis.setAnalyzer()`.
  - `FaceDetectorClient.latestSnapshot: AtomicReference<FaceSnapshot>` ready for `DebugOverlayRenderer` to read on each `OverlayEffect.onDrawListener` tick.
  - `FaceDetectorClient.onLensFlipped()` exposed — must be invoked by CameraController.flipLens().
  - `CameraLens` enum + `CameraLensProvider.next()` exposed — consumed by CameraViewModel (Plan 02-05).
  - Plan 02-04 must REPLACE (not edit) `OverlayEffectBuilder.kt` and `CameraController.kt` — the current contents are compile-unblock stubs only.
  - Plan 02-04 must set `OverlayEffectBuilder.TARGETS = CameraEffect.PREVIEW or VIDEO_CAPTURE or IMAGE_CAPTURE` and `QUEUE_DEPTH = 0` to turn OverlayEffectBuilderTest GREEN.
  - Plan 02-04 must un-`@Ignore` CameraControllerTest methods after adding the providerFactory constructor-default seam.
- **Plan 02-05 (ViewModel + UI) unblocked transitively.**
- **Plan 02-06 (device handoff) unblocked transitively** — FaceTracker log format already pinned for logcat grep step 4.

## Self-Check: PASSED

- [x] `app/src/main/java/com/bugzz/filter/camera/detector/OneEuroFilter.kt` exists
- [x] `app/src/main/java/com/bugzz/filter/camera/detector/FaceSnapshot.kt` exists
- [x] `app/src/main/java/com/bugzz/filter/camera/detector/FaceLandmarkMapper.kt` exists
- [x] `app/src/main/java/com/bugzz/filter/camera/detector/FaceDetectorClient.kt` exists
- [x] `app/src/main/java/com/bugzz/filter/camera/camera/CameraLensProvider.kt` exists
- [x] `app/src/main/java/com/bugzz/filter/camera/render/OverlayEffectBuilder.kt` exists (Rule 3 compile-unblock stub)
- [x] `app/src/main/java/com/bugzz/filter/camera/camera/CameraController.kt` exists (Rule 3 compile-unblock stub)
- [x] `OneEuroFilter` class has `(minCutoff: Double = 1.0, beta: Double = 0.007, dCutoff: Double = 1.0)` defaults
- [x] `OneEuroFilter` has `fun filter(x: Double, tNanos: Long): Double` and `fun reset()`
- [x] `LandmarkSmoother` has `smoothPoint` + `retainActive` + `clear`
- [x] `FaceSnapshot` data class + `EMPTY` companion singleton
- [x] `FaceLandmarkMapper.Anchor` enum with 7 values
- [x] `CameraLens` enum + `CameraLensProvider.next(current)` toggle
- [x] `FaceDetectorClient` has `@Singleton` + `@Inject constructor(@Named("cameraExecutor")`
- [x] `FaceDetectorClient.buildOptions()` companion returns D-15 options (FAST + CONTOUR_ALL + enableTracking + minFaceSize=0.15f + LANDMARK_NONE + CLASSIFICATION_NONE)
- [x] `FaceDetectorClient.createAnalyzer()` uses `COORDINATE_SYSTEM_SENSOR` (D-17)
- [x] `FaceDetectorClient.latestSnapshot: AtomicReference<FaceSnapshot>` initialized to `FaceSnapshot.EMPTY`
- [x] `FaceDetectorClient.onLensFlipped()` calls `smoother.clear()` (PITFALLS #6)
- [x] `Timber.tag("FaceTracker").v(...)` in analyzer callback with `"t=%d id=%s bb=%d,%d contours=%d"` format (T-02-06)
- [x] No `imageProxy.close()` call (anti-pattern)
- [x] No `detector.process(` call (anti-pattern)
- [x] `SMOOTHED_CONTOUR_TYPES` lists all 9 FaceContour ints (FACE + NOSE_BRIDGE + NOSE_BOTTOM + LEFT_EYE + RIGHT_EYE + LEFT_CHEEK + RIGHT_CHEEK + UPPER_LIP_TOP + LOWER_LIP_BOTTOM)
- [x] `./gradlew :app:compileDebugKotlin` succeeds with new files (Hilt KSP processes @Singleton + @Inject + @Named)
- [x] `./gradlew :app:assembleDebug` still succeeds — `app-debug.apk` produced
- [x] `./gradlew :app:testDebugUnitTest --tests "*OneEuroFilterTest*"` exits 0, 4/4 pass
- [x] `./gradlew :app:testDebugUnitTest --tests "*FaceDetectorOptionsTest*"` exits 0, 1/1 pass
- [x] `./gradlew :app:testDebugUnitTest` full run: 10 tests, 5+1 pass, 2 fail intentionally (OverlayEffectBuilderTest RED), 2 skipped (CameraControllerTest @Ignore)
- [x] Commit `2508af9` (feat(02-03-01) OneEuroFilter) present in `git log`
- [x] Commit `e05d37d` (feat(02-03-02) FaceSnapshot+FaceLandmarkMapper+CameraLensProvider) present in `git log`
- [x] Commit `2aa5098` (feat(02-03-03) FaceDetectorClient) present in `git log`
- [x] Commit `611ebb8` (fix(02-03) Rule 3 auto-fixes) present in `git log`

---
*Phase: 02-camera-preview-face-detection-coordinate-validation*
*Completed: 2026-04-19*
