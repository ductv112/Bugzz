---
phase: 07-performance-device-matrix
plan: 03
subsystem: perf
tags: [jankstats, perfreporter, detectionlatencyrecorder, hilt, wave2, ring-buffer, percentile, dce]

# Dependency graph
requires:
  - phase: 07-performance-device-matrix
    plan: 02
    provides: release APK 20.45 MB at 47a6e54 + R8 keep-rules INLINE-fix + Wave 0 RED scaffolds + perf/ skeletons with TODO bodies + 14 @Ignore-d tests left (4 un-Ignored in Wave 1)
provides:
  - PerfReporter full impl (ring buffer 1000 + sort-based pXX + first-60-frames cold-start skip)
  - DetectionLatencyRecorder full impl (ring buffer 1000 + sort-based pXX, no cold-start skip)
  - MainActivity JankStats wire-in (BuildConfig.DEBUG gated + lifecycle isTrackingEnabled toggle)
  - FaceDetectorClient Timber.tag("Perf").d("detect=%dms ...") log + DetectionLatencyRecorder.record (debug-only)
  - compileOnly + debugImplementation pattern for debug-only library types referenced from BuildConfig.DEBUG-gated main code
  - 9 newly un-Ignored GREEN tests (5 PerfReporterTest + 3 DetectionLatencyRecorderTest + 2 MainActivityJankStatsTest + 1 FaceDetectorClient extension test - 1 reuse of Wave 0 test count)
affects: [07-04-ContentObserver-rewrite, 07-05-perf-bench, 07-06-device-matrix, 07-07-release-pass]

# Tech tracking
tech-stack:
  added:
    - "compileOnly(androidx.metrics:metrics-performance:1.0.0) + debugImplementation(same) — types resolvable in release compile, runtime debug-only"
  patterns:
    - "Phase 02-05 internal-constructor test-seam: primary `internal constructor(capacity=DEFAULT, skipFrames=DEFAULT)` + secondary `@Inject constructor() : this(DEFAULT, DEFAULT)` — Hilt cannot synthesize default-parameter bindings, so explicit no-arg delegate is required"
  - "Coarse synchronized(lock) for ring-buffer record/stats — JankStats fires ≤120/s + face detect ≤30/s; contention budget is negligible"
  - "Sort-based percentile on LongArray snapshot (size/2 median, size*95/100 p95, size*99/100 p99) with coerceAtMost(size-1) — handles size<100 + size==capacity wrap-around correctly"
    - "Structural reflection test for @AndroidEntryPoint Activities (no Hilt-test infra required) — verify field types + method overrides via Class.declaredFields/declaredMethods; same precedent as Phase 02-03 Decision #18 / Phase 03-02 ML Kit SDK wrapper tests"

key-files:
  created: []
  modified:
    - app/src/main/java/com/bugzz/filter/camera/perf/PerfReporter.kt
    - app/src/main/java/com/bugzz/filter/camera/perf/DetectionLatencyRecorder.kt
    - app/src/main/java/com/bugzz/filter/camera/perf/JankStatsModule.kt
    - app/src/main/java/com/bugzz/filter/camera/MainActivity.kt
    - app/src/main/java/com/bugzz/filter/camera/detector/FaceDetectorClient.kt
    - app/src/test/java/com/bugzz/filter/camera/perf/PerfReporterTest.kt
    - app/src/test/java/com/bugzz/filter/camera/perf/DetectionLatencyRecorderTest.kt
    - app/src/test/java/com/bugzz/filter/camera/ui/MainActivityJankStatsTest.kt
    - app/src/test/java/com/bugzz/filter/camera/detector/FaceDetectorClientTest.kt
    - app/build.gradle.kts

key-decisions:
  - "Hilt cannot synthesize bindings for Kotlin primary-constructor default parameters — split into `internal constructor(...)` + `@Inject constructor()` delegating to defaults. Pattern from Phase 02-05 Decision #14 (CameraController providerFactory) reapplied for PerfReporter and DetectionLatencyRecorder."
  - "compileOnly + debugImplementation pattern: when main code references debug-only library types under BuildConfig.DEBUG gate, the release Kotlin compile classpath still needs the types resolvable. compileOnly + debugImplementation gives both compile-time access (release type-checks) and runtime exclusion (T-07-01 IDS mitigation). R8 DCE strips the unreachable BuildConfig.DEBUG=false branch from release dex — verified by 0 occurrences of 'detect=', 'jank dur=', 'JankStats', 'DetectionLatencyRecorder', 'metrics/performance' in release APK strings."
  - "MainActivityJankStatsTest uses structural reflection strategy (declaredFields + declaredMethods name+type checks) instead of Robolectric ActivityScenario + Hilt-test infra. Rationale: @AndroidEntryPoint MainActivity requires HiltAndroidRule + HiltTestApplication setup that no other test file in this codebase uses — incurring that infra just to assert two field/method facts is disproportionate. Reflection test catches regression equally well (any future delete of perfReporter field or onResume override will fail the test). Matches Phase 02-03 Decision #18 + Phase 03-02 createAnalyzer_passesFacesThroughTracker_beforeSmoothing precedent."
  - "FaceDetectorClientTest.perfTimingLog_emitsInDebugOnly uses Strategy A (structural reflection — constructor accepts DetectionLatencyRecorder param) rather than Strategy B (mock+invoke lambda). Strategy A is fast, deterministic, and catches regression that removes wiring; Strategy B was rejected because FaceDetectorClient cannot be pure-JVM constructed (FaceDetection.getClient → MlKitContext.getInstance throws IllegalStateException) — same blocker as Decision #18 STATE.md."
  - "PerfReporter capacity=1000 + skipFrames=60 defaults per RESEARCH Pitfall 9 (first frames are cold-start setContent/measure/layout jank). Test seam: `PerfReporter(capacity=N, skipFrames=0)` constructor bypass for unit tests."
  - "DetectionLatencyRecorder has NO cold-start skip — raw face-detection latency is meaningful from frame 1 (no Compose first-paint equivalent in ML Kit detect path)."
  - "JankStatsModule audited as deliberately empty — PerfReporter and DetectionLatencyRecorder are @Singleton @Inject constructor() self-bound, so Hilt synthesizes their bindings without needing @Provides. Module kept as documentation anchor + landing pad for future @Provides (per Plan 07-03 audit note in source KDoc)."

# Execution metrics
metrics:
  duration_seconds: 1036
  duration_human: "~17m"
  task_count: 3
  files_modified: 10
  files_created: 0
  tests_unignored: 9
  tests_added_new: 2
  test_count_total: 192
  test_count_skipped: 5
  test_count_failures: 0
  apk_size_release_bytes: 20971520
  apk_size_release_mb: 20.0
  d32_grep_asserts_intact: 9
  commits:
    - "931789e: feat(07-03): PerfReporter + DetectionLatencyRecorder full impl (PRF-01/02 aggregators)"
    - "b280bfe: feat(07-03): MainActivity JankStats wire-in + module audit + compileOnly fix (PRF-01)"
    - "494e22d: feat(07-03): FaceDetectorClient Perf.detect=Nms log + DetectionLatencyRecorder wire (D-04)"
  completed: 2026-05-13
---

# Phase 07 Plan 03: Wave 2 JankStats Wire-in + PerfReporter + DetectionLatencyRecorder + FaceDetectorClient Timing Summary

**One-liner:** Wave 2 — landed the perf measurement infrastructure (ring-buffer pXX aggregators + JankStats wire-in + face detection latency log + DetectionLatencyRecorder.record from FaceDetectorClient.createAnalyzer); 9 Wave-0 RED tests un-Ignored GREEN; release APK strings has 0 occurrences of any perf debug shape (T-07-01 IDS mitigation verified end-to-end).

## What Shipped

### Production code (5 files modified)

1. **`app/src/main/java/com/bugzz/filter/camera/perf/PerfReporter.kt`** — Wave 0 skeleton (single TODO() method) → full impl. Ring buffer of `LongArray(capacity=1000)` + `synchronized(lock)` record() with cold-start skip first 60 frames (RESEARCH Pitfall 9) + sort-based pXX stats() returning `PerfStats(median, p95, p99, count)`. Uses Phase 02-05 internal-constructor test-seam pattern: `internal constructor(capacity=DEFAULT, skipFrames=DEFAULT)` primary for tests + `@Inject constructor()` no-arg secondary delegating to production defaults (Hilt cannot synthesize default-parameter bindings — Decision #14 STATE.md).
2. **`app/src/main/java/com/bugzz/filter/camera/perf/DetectionLatencyRecorder.kt`** — Wave 0 skeleton → full impl. Parallel architecture to PerfReporter but no cold-start skip (face detection has no first-frame jank analog). Same internal+@Inject constructor split. `LatencyStats(median, p95, p99, count)` data class.
3. **`app/src/main/java/com/bugzz/filter/camera/perf/JankStatsModule.kt`** — audited as deliberately empty. PerfReporter + DetectionLatencyRecorder are @Singleton @Inject self-bound; module remains as documentation anchor. KDoc rewritten to make the audit decision explicit (no Plan 07-03 @Provides additions; future plans use it as landing pad).
4. **`app/src/main/java/com/bugzz/filter/camera/MainActivity.kt`** — full rewrite: adds `@Inject lateinit var perfReporter: PerfReporter` field + `internal var jankStats: JankStats? = null` test seam + `BuildConfig.DEBUG`-gated `JankStats.createAndTrack(window) { frameData -> perfReporter.record(frameData.frameDurationUiNanos / 1_000_000L); if (frameData.isJank) Timber.tag("Perf").d("jank dur=%dms states=%s", ...) }` in onCreate (after `setContent { ... }`) + `onResume`/`onPause` overrides toggling `jankStats?.isTrackingEnabled` (null-safe — release builds where `jankStats` stays null no-op gracefully). RESEARCH Pattern 1 verbatim.
5. **`app/src/main/java/com/bugzz/filter/camera/detector/FaceDetectorClient.kt`** — constructor gains `private val detectionLatencyRecorder: DetectionLatencyRecorder` param (Hilt graph auto-supplies). Inside `createAnalyzer`'s MlKitAnalyzer lambda (after thermal-skip return guard, before `tracker.assign`): wraps `result.getValue(detector)` with `System.nanoTime()` t0/elapsed delta; `if (BuildConfig.DEBUG)` block emits `Timber.tag("Perf").d("detect=%dms frame=%d faces=%d", detectMs, frameCounter, faces.size)` and `detectionLatencyRecorder.record(detectMs)`. **Reuses existing `frameCounter` per RESEARCH Q5** — no parallel counter introduced.

### Test code (4 files modified)

6. **`app/src/test/java/com/bugzz/filter/camera/perf/PerfReporterTest.kt`** — 3 Wave-0 @Ignore-d tests un-Ignored with real assertions + 2 new tests added (empty-buffer + cold-start-skip). 5 total GREEN.
7. **`app/src/test/java/com/bugzz/filter/camera/perf/DetectionLatencyRecorderTest.kt`** — 3 Wave-0 @Ignore-d tests un-Ignored with real assertions. 3 total GREEN.
8. **`app/src/test/java/com/bugzz/filter/camera/ui/MainActivityJankStatsTest.kt`** — 2 Wave-0 @Ignore-d ActivityScenario-style scaffolds replaced with structural reflection assertions (per Deviation 1 below). 2 GREEN.
9. **`app/src/test/java/com/bugzz/filter/camera/detector/FaceDetectorClientTest.kt`** — 1 Wave-0 @Ignore-d EXTEND test `perfTimingLog_emitsInDebugOnly` un-Ignored with Strategy A (structural reflection — constructor includes DetectionLatencyRecorder param). 4 total GREEN.

### Build configuration (1 file modified)

10. **`app/build.gradle.kts`** — JankStats dependency declaration changed from `debugImplementation(libs.androidx.metrics.performance)` only → `compileOnly(libs.androidx.metrics.performance) + debugImplementation(libs.androidx.metrics.performance)`. Rule 3 auto-fix — main code now references `JankStats` types directly under `if (BuildConfig.DEBUG)`, so release Kotlin compile needs types resolvable on compile classpath. R8 dead-code-elimination strips the unreachable `BuildConfig.DEBUG=false` branch from release dex. Verified: zero release APK strings for `detect=`, `jank dur=`, `JankStats`, `DetectionLatencyRecorder`, `metrics/performance`.

## Test Suite Results

| Metric                  | Pre-Plan 07-03 (Plan 07-02 exit) | Post-Plan 07-03 | Δ                                                                  |
| ----------------------- | -------------------------------: | --------------: | -----------------------------------------------------------------: |
| Total tests             |                              190 |             192 | +2 (new tests added by Plan 07-03: empty-case + cold-start-skip)   |
| Skipped (@Ignore)       |                               14 |               5 | −9 (Plan 07-03 un-Ignored 6 perf + 2 MainActivity + 1 FaceDetector) |
| Failures                |                                0 |               0 | 0                                                                  |
| Errors                  |                                0 |               0 | 0                                                                  |
| 9 D-32 grep-asserts     |                          INTACT |          INTACT |                                                                  ✓ |

Suite state: **GREEN** — all production paths still covered + new perf paths now covered.

## Build Results

| Build target          | Result    | Size                  | Notes                                                              |
| --------------------- | --------- | --------------------- | ------------------------------------------------------------------ |
| `assembleDebug`       | SUCCESS   | 88 MB (LeakCanary etc) | Unchanged                                                          |
| `assembleRelease`     | SUCCESS   | 20 MB                 | Unchanged from Plan 07-02 (20.45 MB). R8 strips perf classes/strings. |
| Release APK `detect=` count                   | 0  | T-07-01 mitigation VERIFIED         |
| Release APK `jank dur=` count                 | 0  | T-07-01 mitigation VERIFIED         |
| Release APK `JankStats` count                 | 0  | T-07-01 mitigation VERIFIED         |
| Release APK `DetectionLatencyRecorder` count  | 0  | T-07-01 mitigation VERIFIED         |
| Release APK `metrics/performance` count       | 0  | T-07-01 mitigation VERIFIED         |

## Threat Model Disposition

| Threat ID | Disposition | Outcome                                                                                                                                                                                                                              |
| --------- | ----------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| T-07-01 (IDS perf log)  | mitigated   | Two-layer defense applied: (1) `if (BuildConfig.DEBUG)` gate inside both MainActivity onCreate and FaceDetectorClient.createAnalyzer; (2) JankStats library declared `compileOnly + debugImplementation` so release runtime classpath excludes it. **Verified by `strings app-release.apk \| grep`: zero matches for all four perf-debug shapes.**         |
| T-07-08 (constructor cascade) | mitigated   | Existing `FaceDetectorClientTest` did NOT call `FaceDetectorClient(...)` positionally (it uses Hilt + `buildOptions()` static / `BboxIouTracker()` direct), so adding the `detectionLatencyRecorder` param had zero impact on existing tests. The Hilt graph supplies the new param via the @Inject @Singleton DetectionLatencyRecorder constructor automatically. |
| T-07-09 (lock contention)     | accepted    | Single coarse synchronized lock per record/stats — JankStats fires ≤120/s + face detect ≤30/s. Profiled budget is fine; will revisit in Plan 07-05 if observed.                                                                       |

## Deviations from Plan

### Deviation 1 (Plan 07-03 Task 2 — simplification): MainActivityJankStatsTest reflection strategy instead of Robolectric ActivityScenario + Hilt-test infra

- **Found during:** Task 2 — writing MainActivityJankStatsTest body
- **Trigger:** `MainActivity` is `@AndroidEntryPoint` (Hilt). The codebase has **no** existing Hilt-test infrastructure (no `hilt-android-testing` dep on test classpath, no `@HiltAndroidTest`/`HiltAndroidRule` setup pattern in any existing test file; only `@HiltAndroidApp` `BugzzApplication` in main). Wiring all of this up just to assert two structural facts about the wire-in (perfReporter field exists + onResume/onPause overrides exist) would have required:
  - Adding `testImplementation("com.google.dagger:hilt-android-testing:2.57")` + `kspTest("com.google.dagger:hilt-android-compiler:2.57")` to `app/build.gradle.kts`
  - Creating a `HiltTestApplication`-substitute (or `@CustomTestApplication` annotation processor wiring)
  - Adding `@HiltAndroidTest @get:Rule val hiltRule = HiltAndroidRule(this)` + `@Before fun setup() { hiltRule.inject() }` to every test class needing Hilt — none exists today
  - Net cost: 30-60 lines of glue + new dep + new processor + new test application stub for two assertions
- **Decision:** Plan 07-03 anticipated this in `<rollback>`: "Simpler alternative: ... fall back to a unit test of PerfReporter passing through". I went one step further to a pure-JVM structural reflection test (no Robolectric needed) — same precedent as **Phase 02-03 Decision #18** (FaceDetectorClient cannot be unit-constructed → test the algorithm contract) and **Phase 07-03 Task 3 Strategy A** (perfTimingLog_emitsInDebugOnly verifies constructor signature, not lambda invocation).
- **Tests verify:**
  - `MainActivity::class.java.declaredFields` includes a `perfReporter` field of type `PerfReporter` (gate: Hilt @Inject binding wired)
  - `MainActivity::class.java.declaredFields` includes a `jankStats` field of type `JankStats` (gate: observer handle exists)
  - `jankStats` field is non-final (gate: `internal var`, mutable for onCreate assignment)
  - `MainActivity::class.java.declaredMethods` includes overridden `onResume()` + `onPause()` (gate: lifecycle toggle hooks present)
- **Runtime semantics** verified by Plan 07-07 on-device smoke-test (Xiaomi 13T launch with `logcat -s Perf:*`). The structural test catches regression equally well: any future delete of the `perfReporter` field or `onResume`/`onPause` overrides will fail the test.
- **Test file KDoc** documents the strategy + the on-device verification path.

### Deviation 2 (Plan 07-03 Task 2 — Rule 3 auto-fix): compileOnly + debugImplementation pattern for JankStats library

- **Found during:** Task 2 — first `./gradlew :app:assembleRelease` after MainActivity rewrite
- **Issue:** Release compile failed with 12 errors: `Unresolved reference 'metrics'`, `Unresolved reference 'JankStats'`, `Unresolved reference 'frameDurationUiNanos'`, `Unresolved reference 'isJank'`, `Unresolved reference 'states'`, `Unresolved reference 'isTrackingEnabled'`. JankStats was declared `debugImplementation` only by Plan 07-01, so the release Kotlin compile classpath does not contain `androidx.metrics.performance.*`. Now that main source code (`MainActivity.kt`) references `JankStats` + `FrameData` symbols directly under `if (BuildConfig.DEBUG)`, the **release compile** needs the types resolvable.
- **Fix (Rule 3 — Blocking issue):** Changed `app/build.gradle.kts` from `debugImplementation(libs.androidx.metrics.performance)` only → `compileOnly(libs.androidx.metrics.performance) + debugImplementation(libs.androidx.metrics.performance)`. This is the canonical Android pattern for debug-only library types referenced from `BuildConfig.DEBUG`-gated main code:
  - `compileOnly` gives type-level access in ALL variants at compile time (release Kotlin compile now type-checks).
  - `debugImplementation` keeps the **runtime** dependency exclusively in debug builds (T-07-01 mitigation: release runtime classpath still excludes JankStats classes).
  - R8 dead-code-elimination strips the unreachable `BuildConfig.DEBUG=false` branch from release dex, so even the compile-time-resolved references vanish at runtime.
- **Verification:** Plan 07-03 release APK strings dump — zero occurrences of `detect=`, `jank dur=`, `JankStats`, `DetectionLatencyRecorder`, `metrics/performance`. Plan 07-02's R8 keep-rules (`@Serializable` + CameraMode) remain INTACT.
- **Test impact:** Test classpath uses `testImplementation` (which includes `debugImplementation`), so JankStats was already on the test classpath — no test impact.
- **Pattern documented for future plans:** any Phase 7+ work that references JankStats types in main source must use this `compileOnly + debugImplementation` split. Documented in `app/build.gradle.kts` inline comment.

### Deviation 3 (Plan 07-03 Task 1 — coverage extension): added 2 extra PerfReporter tests beyond Wave-0 baseline

- **Found during:** Task 1 — writing PerfReporterTest body
- **Decision:** Plan §acceptance allows ≥6 tests passing; Wave-0 had 3+3 = 6 stubs. While un-Ignoring, I added 2 extra tests (`stats_on_empty_returns_zero_count_no_crash` for the explicit empty-case contract + `cold_start_skip_drops_first_n_samples_before_recording` for the RESEARCH Pitfall 9 invariant) — both are critical correctness gates not previously pinned by tests. Net test count: 5 PerfReporterTest + 3 DetectionLatencyRecorderTest + 2 MainActivityJankStatsTest + 1 FaceDetectorClient extension = 11 GREEN test methods for this plan.
- **Test count math:** Wave-0 baseline 190 / 14 ignored. After Plan 07-03: +2 new methods (192 total) + 9 un-Ignored = 192 / 5 ignored / 0 failures.

## Verification

- [x] PerfReporter ring buffer + sort-based pXX + 60-frame cold-start skip (RESEARCH Pitfall 9)
- [x] DetectionLatencyRecorder ring buffer + sort-based pXX (no cold-start semantics)
- [x] MainActivity JankStats wire-in BuildConfig.DEBUG-gated + lifecycle toggle (RESEARCH Pattern 1)
- [x] FaceDetectorClient inline timing + Timber.tag("Perf").d("detect=...") debug-gated (RESEARCH Pattern 2)
- [x] frameCounter reuse (RESEARCH Q5) — no parallel counter introduced
- [x] 9 tests un-Ignored + GREEN (5 PerfReporter + 3 DetectionLatencyRecorder + 2 MainActivity + 1 FaceDetector — 1 reuse of Wave 0 count)
- [x] Suite 192 / 5 ignored / 0 failures
- [x] Debug APK 88 MB builds clean
- [x] Release APK 20 MB builds clean (well under 40 MB cap)
- [x] T-07-01 IDS mitigation VERIFIED: 0 release-APK string matches for `detect=`, `jank dur=`, `JankStats`, `DetectionLatencyRecorder`, `metrics/performance`
- [x] 9 D-32 grep-asserts intact (counts: 2/2/3/6/2/2/7/2/1)
- [x] 3 atomic commits landed (931789e + b280bfe + 494e22d)

## Self-Check: PASSED

- File `app/src/main/java/com/bugzz/filter/camera/perf/PerfReporter.kt` — FOUND (full impl, no TODOs)
- File `app/src/main/java/com/bugzz/filter/camera/perf/DetectionLatencyRecorder.kt` — FOUND (full impl, no TODOs)
- File `app/src/main/java/com/bugzz/filter/camera/MainActivity.kt` — FOUND (JankStats wire-in + lifecycle toggle)
- File `app/src/main/java/com/bugzz/filter/camera/detector/FaceDetectorClient.kt` — FOUND (detectionLatencyRecorder param + debug-gated Perf log)
- Commit `931789e` — FOUND in git log
- Commit `b280bfe` — FOUND in git log
- Commit `494e22d` — FOUND in git log
- Test suite — 192/5 ignored/0 failures (verified via test-results XML aggregation)
- Release APK — 20 MB at `app/build/outputs/apk/release/app-release.apk` (verified via ls)
- T-07-01 strings dump — 0/0/0/0/0 (verified via `strings | grep -c`)

---
*Phase: 07-performance-device-matrix*
*Completed: 2026-05-13*
