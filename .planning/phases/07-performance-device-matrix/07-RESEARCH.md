# Phase 7: Performance & Device Matrix - Research

**Researched:** 2026-05-13
**Domain:** Android release-build hardening + performance instrumentation + cross-OEM device matrix verification
**Confidence:** HIGH

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions

All 26 decisions auto-locked per user autonomy delegation pattern (memory `feedback_autonomy.md` + Phase 5/6 precedent). Copy verbatim — planner MUST honor these and not propose alternatives.

#### Benchmark Methodology (PRF-01, PRF-02)
- **D-01:** Use **JankStats** (`androidx.metrics:metrics-performance 1.0.0-beta02`) as in-app frame timing observer (debug builds). Pipe `FrameData` to Timber Logcat tag `Perf` for offline analysis. Rationale: zero-config, runs alongside production code, no Macrobenchmark CI overhead.
- **D-02:** Use **Android Studio Profiler** (CPU / Memory / Energy) traces for deep-dive measurement runs on Xiaomi 13T. Capture profiler traces during 30s filter playback + 60s video record + pre-warmed thermal stress.
- **D-03:** Do NOT integrate Jetpack Macrobenchmark library. Rationale: solo dev with single device + opportunistic 2nd OEM doesn't benefit from CI-style automation.

#### Face Detection Latency (PRF-02)
- **D-04:** Measure ML Kit face detection latency via existing `FaceDetectorClient` instrumentation. Add timing log: `Timber.tag("Perf").d("detect=%dms frame=%d landmarks=%d", elapsed, frameIdx, landmarkCount)` around `FaceDetector.process()` call (debug only). Aggregate ≥1000 samples for stats.
- **D-05:** Acceptance: median latency ≤100ms/frame on Xiaomi 13T baseline. p95 ≤150ms acceptable. p99 noted but not blocking.

#### APK Size Reduction (PRF-04)
- **D-06:** Enable full release build configuration: `minifyEnabled true` + `shrinkResources true` + `isDebuggable false`. Wire R8 with default ProGuard rules; add Compose-specific keep rules from official `proguard-android-optimize.txt`.
- **D-07:** Convert sprite PNGs in `app/src/main/assets/sprites/` to WebP (lossless quality ≥90).
- **D-08:** Add ABI splits configuration: `abiFilters "arm64-v8a"` only (drop x86_64 and armeabi-v7a).
- **D-09:** Acceptance: release APK ≤40 MB total.

#### Audio Sync Verification (PRF-03)
- **D-10:** Verify audio drift via `ffprobe -show_streams -show_format` on a 60-second test video record. Compute `audio.start_time - video.start_time` + verify `audio.duration ≈ video.duration` within 50ms tolerance.
- **D-11:** Verify zero frame drops via `ffprobe -count_frames -show_entries stream=nb_read_frames`. Expected: 60s × 30fps = 1800 frames ± 5 acceptable.

#### Cross-OEM Device Matrix (PRF-05)
- **D-12:** Primary verification device: **Xiaomi 13T** (already validated through Phase 6).
- **D-13:** Secondary device(s): User to source Samsung A-series OR Pixel A-series for Phase 7 sign-off. Best-effort 2nd OEM.
- **D-14:** Cloud device fallback: Firebase Test Lab free tier (5 free physical device runs/day) optional for opportunistic Pixel coverage if user cannot source one.
- **D-15:** Reference APK comparison: install reference `com.insect.filters.funny.prank.bug.filter.face.camera` on Xiaomi 13T. Try with `-r --split-from-source` first; if still fails, document as deferred + skip reference comparison.

#### GL CameraEffect Escalation (Success Criterion #5)
- **D-16:** **Measure first, escalate only if needed.** Do NOT pre-implement GL CameraEffect scaffolding.
- **D-17:** Escalation trigger: median fps <24 on either Xiaomi 13T or secondary OEM device during 30s filter playback. Documented as PHASE-CHECKPOINT-FAIL with empirical numbers. Then file `07-gaps-NN-PLAN.md` for GL escalation.
- **D-18:** Documentation requirement: if Canvas passes, write `07-PERF-REPORT.md` documenting escalation deferral with rationale + measurement evidence.

#### Thermal Mitigation Verification
- **D-19:** Pre-warmed stress test: record 60s video AFTER 5-minute warmup recording (or 5 consecutive 60s sessions). Verify Logcat shows `Timber.tag("Perf")` thermal-throttle events firing when device reaches `THERMAL_STATUS_MODERATE+`, and visible fps stays ≥20fps via JankStats.

#### Phase 6 + Phase 4+5 Deferred UAT Folding
- **D-20:** Fold the 3 LOW-severity polish items from 06-CHECKPOINT.md into Phase 7 scope:
  - **(a) Bbox + landmark debug viz** — Gate `DebugOverlayRenderer.drawDebugViz()` behind `BuildConfig.DEBUG`.
  - **(b) Collection grid stale-entry** — Add `ContentResolver.registerContentObserver` to `CollectionRepository`.
  - **(c) LeakCanary LAUNCHER hijack** — In debug-only AndroidManifest, disable `leakcanary.internal.activity.LeakLauncherActivity`.
- **D-21:** Fold the 8 Phase 4+5 deferred UAT items from Phase 6 06-HANDOFF.md (per D-33) into the Phase 7 device matrix runbook.

#### Release Build Quality Bar
- **D-22:** Release build must produce signed APK (debug keystore OK for personal use).
- **D-23:** Verify release APK installs cleanly on Xiaomi 13T + secondary OEM.
- **D-24:** All Phase 6 D-32 grep-asserts (9 patterns) must continue to pass post-release-config + post-R8.

#### Plan Budget & Wave Layout
- **D-25:** Estimated plan budget: **5-7 plans / 4-5 waves**. Wave structure outlined in CONTEXT (W0..W5).
- **D-26:** Sequential within-wave execution (worktrees disabled).

### Claude's Discretion

12+ items left to executor judgement at impl time:
- Specific ProGuard rule additions if R8 strips Compose/Hilt/CameraX entry points (handle inline per Phase 4/5 inline-gap-fix protocol)
- JankStats `JankFrameListener` aggregation window (recommend 1000-frame rolling stats)
- WebP conversion tool (`cwebp` or `webp-converter-gradle-plugin`)
- ABI split file naming convention (release APK suffix)
- Test scaffold layout for Phase 7 (5-7 test files estimate)
- ContentObserver scope (per-MIME registration vs single Files URI)
- Reference APK install retry strategy (`--bundletool` or `apk-extractor` if `-r` still fails)
- Whether to add `R8` `-keep` rules for Compose/Hilt up-front vs reactive
- LeakCanary debug-only `<activity>` disable mechanism (manifest merge attribute vs `tools:replace`)
- Cloud Firebase Test Lab opt-in (default: skip unless user requests)
- Whether to dump perf measurements into `.planning/perf-data/` as JSON for reproducibility
- 07-HANDOFF Vietnamese bilingual hints depth (same as 06-HANDOFF precedent)

### Deferred Ideas (OUT OF SCOPE)

#### v2 Polish (already in REQUIREMENTS.md)
- POL-01..06 (countdown, flash, music, watermark, multi-face, debug-toggle)
- Real Play Store URLs + privacy policy hosting
- Localization (i18n)
- Trending feed / social features
- Analytics + crashlytics, Ads / billing
- Filter quality settings, Photo edit features

#### Performance / Architecture (v2)
- Macrobenchmark CI integration
- A/B testing framework for sprite asset bundles
- Module split (extract `:camera`, `:render`, `:data`, `:ui` from `:app`)
- KMP feasibility study (iOS port)
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| **PRF-01** | Live preview sustains ≥24 fps during normal filter playback on mid-tier test device | JankStats 1.0.0 stable for in-app frame timing; Android Studio Profiler for deep-dive; `Choreographer.FrameCallback` not needed (JankStats wraps `FrameMetrics` API ≥ N) |
| **PRF-02** | Face detection latency ≤100 ms per frame (measured via Android Studio profiler) | Existing `FaceDetectorClient.createAnalyzer()` MlKitAnalyzer callback is the timing instrumentation point — wrap `result.getValue(detector)` resolution in `System.nanoTime()` delta + `Timber.tag("Perf")` (D-04). Profiler trace as secondary evidence (D-02) |
| **PRF-03** | 60-second video record produces a file with audio synced within 50ms drift | `ffprobe -v error -select_streams a:0/v:0 -show_entries stream=start_time,duration,nb_read_frames` on a pulled MP4 from `/sdcard/DCIM/Bugzz/` (D-10/11) |
| **PRF-04** | Final release APK ≤40 MB (via APK Analyzer) | R8 minify + shrinkResources + WebP sprite conversion + ABI split arm64-v8a-only + isDebuggable=false (strips LeakCanary +~4 MB). Debug APK currently 91.96 MB; release projection ~25-35 MB after stripping LeakCanary, R8 30-40% bytecode reduction, WebP ~30-50% sprite reduction, single-ABI savings on native libs |
| **PRF-05** | App verified working on Samsung + Pixel (minimum 2-OEM matrix) on real Android 9+ devices | Xiaomi 13T standing in as primary OEM (D-12); user sources Samsung A-series or Pixel A-series for secondary (D-13); Firebase Test Lab free tier as opportunistic Pixel fallback (D-14) |
</phase_requirements>

## Summary

Phase 7 is verification-and-hardening — no new product features. The work splits cleanly into four concerns: (1) instrument the running app with JankStats + per-frame face-detection timing so we have numbers, not vibes; (2) flip the release build from "debug clone with LeakCanary baked in" to a properly-R8-minified, resource-shrunk, single-ABI, WebP-sprite release APK; (3) verify the captured MP4 has zero audio drift over 60s via ffprobe (objective + reproducible); (4) close the Phase 6 + Phase 4+5 deferred polish items (debug viz gating + ContentObserver + LeakCanary disable + 8 UAT items folded). The escalation question — Canvas vs custom GL `CameraEffect` — is **measurement-first** per D-16; do not pre-build the GL path. If Canvas profiles ≥24 fps median on both OEMs, document the GL deferral in `07-PERF-REPORT.md` and close.

The single biggest risk is R8 stripping a code path that one of the 9 Phase 3+4+5 D-32 grep-asserts depends on — these inline fixes (BboxIouTracker, OneShotEvent payloads, `isCapturing`/`isRecording` flags, sticker matrix reset, etc.) are reflection-free Kotlin but Hilt KSP-generated factories + Compose lambda groups are reflection-adjacent. The Hilt 2.57 and Compose Compiler plugin ship `consumer-proguard-rules.pro` automatically in their AARs — we should NOT pre-emptively add hand-written `-keep` rules for them; let R8 run, see what breaks, then add the minimum necessary inline rules per the Phase 4/5 inline-gap-fix protocol.

**Primary recommendation:** Treat Wave 1 (release build config flip) as the riskiest wave — land it BEFORE Wave 2 (JankStats instrumentation) so all subsequent measurements run against the release-shape build that will ship. If R8 strips something, we catch it before measurement-time and avoid measuring a non-shipping artifact.

## Project Constraints (from CLAUDE.md)

The following CLAUDE.md directives apply to Phase 7 — planner MUST verify compliance:

| Directive | Source | Phase 7 Compliance |
|-----------|--------|--------------------|
| Kotlin 2.1.21, AGP 8.9.1, Gradle 8.13, JVM target 17 | §"Recommended Stack" | Already pinned; verify libs.versions.toml unchanged |
| CameraX 1.6.0 uniform across all artifacts | §"Recommended Stack" | Already pinned; do NOT bump for Phase 7 |
| ML Kit Face Detection bundled 16.1.7 | §"Recommended Stack" | Already pinned; instrumentation only |
| Compose BOM 2026.04.00 | §"Recommended Stack" | Currently 2026.03.00 — **2026.04.01 IS published as of April 2026** per WebSearch; planner may bump Compose BOM in Wave 1 (Phase 2 deferred this as a "not yet published" item; April 2026 publish unblocks it) |
| Frame-sequence PNG (NOT Lottie for bug sprites) | §"Sprite Animation Recommendation" | Already in place — Phase 7 converts PNG→WebP, not replacing renderer |
| MVVM + StateFlow + UDF | §"Architecture Pattern" | Already in place; Phase 7 changes nothing here |
| Hilt 2.57 | §"Recommended Stack" | Already in place; R8 keep rules ship via Hilt AAR consumer-rules — do not pre-add |
| WRITE_EXTERNAL_STORAGE only with `android:maxSdkVersion="28"` | §"Storage" | Verify manifest unchanged through Phase 7 |
| OverlayEffect (Canvas) is the chosen path; GL CameraEffect is fallback | §"Fallback Plan" | D-16 enforces measure-first; escalation only on Canvas failure |
| Escalation budget: 3-5 days additional work | §"Fallback Plan" | If triggered, gap-plan `07-gaps-NN-PLAN.md` carries the budget |
| Triggering metric: average frame time > 33 ms on Snapdragon-675-class device over 10-second recording | §"Fallback Plan" | This is the canonical PHASE-CHECKPOINT-FAIL criterion — match D-17 wording in 07-PERF-REPORT.md |

## Standard Stack

### Core (Phase 7 additions only — base stack from Phase 1-6 unchanged)

| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| `androidx.metrics:metrics-performance` | **1.0.0** (stable Oct 8 2025) | JankStats frame timing observer | First-party Google library; FrameMetrics-API-on-API-24+ / OnPreDrawListener fallback on older; zero per-frame allocations; runs in debug builds without prod overhead [VERIFIED: developer.android.com/jetpack/androidx/releases/metrics] |

> **Version correction:** CONTEXT D-01 names `1.0.0-beta02`. Per the Android Developers Metrics release page, **`1.0.0` stable shipped 2026-10-08** (~7 months ago). The 1.0.0 release has identical API surface to beta02; the only difference is the stable lifecycle marker. **Planner should bump D-01 to `metricsPerformance = "1.0.0"`** in the catalog. This is a low-risk drift correction (Rule 1 auto-fix pattern from Phase 02-02 / Phase 02-04 precedent). [VERIFIED: developer.android.com/jetpack/androidx/releases/metrics]

### Supporting (Phase 7 — no new runtime dependencies)

| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| `cwebp` CLI | ≥1.3.x | One-shot sprite PNG → WebP lossless conversion at build script time | Run once as a pre-build pass (or via Android Studio "Convert to WebP" right-click); not a runtime dep |
| `ffprobe` (from FFmpeg ≥4.x) | host tool | Audio drift + frame count verification on pulled MP4 | Verification only — runs on dev PC, never on device |

### Tooling (already in place — confirm available)

| Tool | Source | Phase 7 Use |
|------|--------|-------------|
| Android Studio Profiler | bundled with Android Studio Ladybug+ | CPU/Memory/Energy trace capture during 30s filter playback + 60s record + thermal stress |
| APK Analyzer | bundled with Android Studio | PRF-04 verification: total size + per-bucket breakdown |
| `adb` | platform-tools | All device verification flows (existing pattern from Phase 1-6) |
| `scrcpy` | already in CLAUDE.md tooling list | Live screen mirror while observing Logcat |

### Optional Compose BOM Bump

| Decision | Rationale |
|----------|-----------|
| Compose BOM **2026.03.00 → 2026.04.01** | Phase 02-02 deferred 2026.04.00 because the BOM was not yet on Google Maven. Per [Compose April 2026 release](https://android-developers.googleblog.com/2026/04/jetpack-compose-april-2026-updates.html), `2026.04.01` shipped April 2026 with v2 testing APIs as default. **Planner's call** (D-20 area / Claude's Discretion) — bump in Wave 1 alongside the release-build flip, or leave at 2026.03.00 and document as deferred. Risk: v2 testing API switch could affect existing 172 unit tests, though most Phase 1-6 tests use `runTest` from coroutines-test, not Compose test rules. Recommendation: **stay on 2026.03.00 for Phase 7** to minimize blast radius — Phase 7 is hardening, not modernization. [CITED: developer.android.com/blog/posts/whats-new-in-the-jetpack-compose-april-26-release] |

### Alternatives Considered

| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| JankStats | `Choreographer.FrameCallback` + manual frame-time delta | Hand-rolled; no FrameMetrics integration; misses input-to-display latency; ~50 LOC + buggy. JankStats wraps the FrameMetrics API correctly. REJECT. |
| JankStats | Jetpack Macrobenchmark | CI-grade rigour; requires separate Gradle module + connectedCheck infra; D-03 explicitly rejects this for solo dev. REJECT. |
| Android Studio Profiler | Perfetto traces (manual capture) | More raw but harder to operate; Profiler internally captures Perfetto traces in 1.x. Stick with Profiler UI. |
| WebP lossless q≥90 | AVIF (better compression) | AVIF support on Android starts Android 12 (API 31); minSdk is 28. REJECT. |
| WebP lossless | WebP lossy q=80 | Sprite alpha channels are content-critical (anti-aliased edges); lossy compression introduces halos on transparent edges. REJECT. |
| `abiFilters "arm64-v8a"` in `defaultConfig` | `splits.abi { include("arm64-v8a") }` | `abiFilters` produces a single universal APK with one ABI; `splits.abi` produces multiple per-ABI APKs (one per ABI included). For a single-ABI app the simpler form is `abiFilters`. CONTEXT D-08 wording says "ABI splits configuration: abiFilters arm64-v8a only" — they're conflating terminology; use `abiFilters` (simpler, equivalent result). [VERIFIED: developer.android.com/build/configure-apk-splits] |
| Firebase Test Lab free tier | BrowserStack / AWS Device Farm | Paid; D-14 explicitly scopes to free tier. Free tier in 2026 = **30 minutes physical device time/day** (NOT the 5-runs/day number in CONTEXT). [VERIFIED: firebase.google.com/docs/test-lab/usage-quotas-pricing] |

> **CONTEXT correction (D-14):** Firebase Test Lab 2026 free tier is **30 minutes physical device test time per day**, not "5 free physical device runs/day." Same intent ("opportunistic free Pixel coverage"), but planner should use the correct quota phrasing in 07-HANDOFF.md. [VERIFIED]

**Installation (Phase 7 new deps only):**
```kotlin
// gradle/libs.versions.toml
metricsPerformance = "1.0.0"  // not 1.0.0-beta02 — see version correction above

// libraries section
androidx-metrics-performance = { module = "androidx.metrics:metrics-performance", version.ref = "metricsPerformance" }

// app/build.gradle.kts (debugImplementation — Phase 7 is for diagnostic use only)
debugImplementation(libs.androidx.metrics.performance)
```

> Note: `debugImplementation` keeps JankStats out of release. The CONTEXT D-01 wording "debug builds" supports this. Production users do not pay JankStats overhead.

## Architecture Patterns

### Recommended Module / File Layout for Phase 7 Additions

```
app/
├── build.gradle.kts                    # release block: minify + shrinkResources + abiFilters (D-06/D-08)
├── proguard-rules.pro                  # populate ONLY if R8 strips something (reactive, not preemptive)
├── src/
│   ├── debug/
│   │   └── AndroidManifest.xml         # NEW — disables LeakCanary LAUNCHER (D-20c)
│   └── main/
│       ├── assets/sprites/             # PNG → WebP (D-07) — directory structure preserved
│       └── java/com/bugzz/filter/camera/
│           ├── perf/                   # NEW package
│           │   ├── JankStatsModule.kt  # Hilt @Module providing JankStats per-Activity
│           │   ├── PerfReporter.kt     # @Singleton thin wrapper around Timber.tag("Perf")
│           │   └── DetectionLatencyRecorder.kt   # rolling 1000-sample stats (D-04)
│           ├── detector/
│           │   └── FaceDetectorClient.kt   # AMEND: add Timber.tag("Perf").d(...) inline (D-04)
│           ├── render/
│           │   └── DebugOverlayRenderer.kt # ALREADY has BuildConfig.DEBUG gate on draw() — VERIFY (D-20a may be a no-op)
│           └── data/
│               └── CollectionRepository.kt # AMEND: add ContentObserver (D-20b)
└── proguard-rules.pro                  # reactive R8 keep rules (likely stays near-empty)
```

> **D-20a investigation note:** `DebugOverlayRenderer.draw()` already has `if (!BuildConfig.DEBUG) return` as the FIRST statement (line 60 of the current file — Phase 2 D-02 / T-02-02 gate). 06-CHECKPOINT.md states "Face bounding box (red rectangle) visible in baked JPG" — this is a contradiction. **Two hypotheses for the planner to disambiguate during planning**: (1) The bbox visible in the saved JPG is NOT from `DebugOverlayRenderer` but from a SECOND debug-draw path (e.g., `FilterEngine` debugging code or `BboxIouTracker` visualization); OR (2) the JPG was captured from a debug build where the gate IS firing correctly and the "polish item" is actually about hiding the gate-respecting visualization in ALL builds (debug + release), not just release. Plan 07-Wxx for D-20a must START by grepping for `Color.RED` / `drawRect` / `BboxIouTracker` debug calls to find the actual source before editing. [ASSUMED]

### Pattern 1: JankStats per-Activity binding

JankStats is bound to a `Window` (one instance per `Activity.getWindow()`). For a Compose single-Activity app, this means one JankStats instance in `MainActivity`. The official Android samples bind in `onResume()` / unbind in `onPause()` to avoid background-frame noise.

```kotlin
// Source: github.com/android/performance-samples JankStatsSample
// Adapted for Compose single-activity + Hilt + Timber

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private lateinit var jankStats: JankStats

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { BugzzApp() }

        if (BuildConfig.DEBUG) {
            // Listener fires on every frame; isJank=true when frame exceeds budget.
            jankStats = JankStats.createAndTrack(window) { frameData ->
                if (frameData.isJank) {
                    Timber.tag("Perf").d(
                        "jank dur=%dms isJank=%s states=%s",
                        frameData.frameDurationUiNanos / 1_000_000,
                        frameData.isJank,
                        frameData.states.joinToString { "${it.key}=${it.value}" },
                    )
                }
            }
        }
    }

    override fun onResume() { super.onResume(); if (::jankStats.isInitialized) jankStats.isTrackingEnabled = true }
    override fun onPause()  { if (::jankStats.isInitialized) jankStats.isTrackingEnabled = false; super.onPause() }
}
```

**Hot path note:** `frameData` is reused — DO NOT cache it or hand it to a flow without copying fields. [CITED: developer.android.com/topic/performance/jankstats]

### Pattern 2: ML Kit face-detection latency timing (D-04)

The current `FaceDetectorClient.createAnalyzer()` callback (lines 52-97) is the timing instrumentation point. The cleanest insertion is around the `result.getValue(detector)` line because `MlKitAnalyzer` has already done the `ImageProxy → InputImage → FaceDetector.process()` work synchronously inside that call:

```kotlin
// Source: project file detector/FaceDetectorClient.kt — D-04 amendment

// inside createAnalyzer() lambda, replacing line 70:
val t0 = System.nanoTime()
val faces: List<Face> = result.getValue(detector) ?: emptyList()
val detectMs = (System.nanoTime() - t0) / 1_000_000

if (BuildConfig.DEBUG) {
    // D-04: per-frame latency log; consumed by Wave 3 PerfReporter rolling-stats aggregator
    Timber.tag("Perf").d(
        "detect=%dms frame=%d faces=%d",
        detectMs,
        frameCounter,   // already exists for thermal frame-skip
        faces.size,
    )
}
```

**Compute pXX stats offline** by `adb logcat -d | grep "Perf.*detect="` then awk to histogram. OR aggregate in-memory via `DetectionLatencyRecorder` (a `@Singleton` Hilt-injected ring buffer) that exposes `median`/`p95`/`p99` query methods for unit-testable verification. Recommend the latter — keeps measurement deterministic.

### Pattern 3: ContentObserver for live MediaStore refresh (D-20b)

`CollectionRepository.loadMediaItems()` currently emits exactly once per subscription (per its own KDoc, lines 41-43). Phase 6 left this as a Phase 7 polish item. The Compose-correct pattern is `callbackFlow` + `awaitClose { resolver.unregisterContentObserver(observer) }`:

```kotlin
// Source: adapted from common Kotlin Flow + ContentResolver pattern
// (medium.com/@sheikhzakirahmad — verified pattern from multiple community sources)

fun loadMediaItems(): Flow<List<MediaItem>> = callbackFlow {
    suspend fun queryAndEmit() {
        val items = withContext(Dispatchers.IO) { performQuery() }
        trySend(items)
    }

    val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) {
            launch { queryAndEmit() }
        }
    }

    // Register on the FILES URI — captures both image and video inserts/deletes in one observer.
    context.contentResolver.registerContentObserver(
        MediaStore.Files.getContentUri("external"),
        /* notifyForDescendants = */ true,
        observer,
    )

    queryAndEmit()  // initial snapshot
    awaitClose { context.contentResolver.unregisterContentObserver(observer) }
}.flowOn(Dispatchers.IO)
```

**Critical:** The observer registers on `MediaStore.Files.getContentUri("external")` with `notifyForDescendants=true`. This single registration covers BOTH `Images.Media` and `Video.Media` inserts/deletes because they all bubble up to `Files`. CONTEXT Claude's-Discretion item ("per-MIME registration vs single Files URI") — recommendation: **single Files URI** for simplicity (matches the existing query URI).

**Lifecycle:** `callbackFlow + awaitClose` ensures unregister fires when the CollectionViewModel's `viewModelScope.launch { repository.loadMediaItems().collect {...} }` is cancelled (i.e., on `onCleared()`). No manual `DisposableEffect` needed on the composable side — ViewModel scoping handles it. [VERIFIED via web search; pattern is community standard]

### Pattern 4: Debug-build manifest overlay for LeakCanary LAUNCHER disable (D-20c)

LeakCanary 2.14 registers a `LeakLauncherActivity` with `LAUNCHER` intent filter for its "Leaks" tile on the device launcher. On Xiaomi HyperOS this competes with `MainActivity` for `category.LAUNCHER` selection. The fix is a debug-only manifest overlay that disables the LeakCanary activity:

```xml
<!-- app/src/debug/AndroidManifest.xml — NEW FILE (D-20c) -->
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
          xmlns:tools="http://schemas.android.com/tools">

    <application>
        <!-- LeakCanary's LAUNCHER hijack disable (D-20c).
             tools:node="remove" makes manifest merger drop LeakCanary's <activity> entry. -->
        <activity
            android:name="leakcanary.internal.activity.LeakLauncherActivity"
            tools:node="remove" />
    </application>
</manifest>
```

> Alternative form per CONTEXT D-20c wording — `<activity android:enabled="false">` would also work, but `tools:node="remove"` is the idiomatic manifest-merger approach because it removes the entry entirely rather than leaving a disabled placeholder. Either is acceptable per Claude's Discretion. [CITED: developer.android.com/build/manage-manifests#node_markers]

### Pattern 5: Frame-sequence WebP at runtime

Android's `BitmapFactory.decodeStream(AssetManager.open("…/frame_00.webp"))` handles WebP transparently — no decoder change needed. WebP frames decode to the same `Bitmap.Config.ARGB_8888` (or whatever the source bit depth was), so `AssetLoader.LruCache<String, Bitmap>` behavior is byte-identical post-conversion (cache size in BYTES is identical; only on-disk asset is smaller). [VERIFIED: developer.android.com/develop/ui/views/graphics/reduce-image-sizes]

**Manifest.json reference paths:** Each sprite's `manifest.json` lists frames as `"frame_NN.png"`. **After WebP conversion, manifest.json paths MUST be updated to `frame_NN.webp`** — otherwise `AssetLoader.preload(assetDir)` reads the manifest, opens `frame_00.png`, and crashes with `FileNotFoundException`. Plan task: WebP conversion + manifest.json regeneration in a single atomic commit. [ASSUMED — needs grep verification of where AssetLoader resolves frame paths]

### Anti-Patterns to Avoid

- **Pre-emptive R8 keep-everything rules:** Adding `-keep class androidx.compose.** { *; }` is the lazy stack-overflow answer. Compose Compiler plugin and Hilt 2.57 ship their own `consumer-proguard-rules.pro` in the AAR — let manifest-merger do its job. Only add hand-written rules when an empirical R8 failure proves they're needed (matches Phase 4/5 inline-gap-fix protocol).
- **JankStats in production:** Even at zero per-frame allocation cost, the FrameMetrics integration has non-zero overhead. Keep `debugImplementation` only.
- **Measuring on connected ADB with `cold start = false`:** First-launch perf is meaningfully different from steady-state. Always force-stop + relaunch before measurement runs (matches 06-HANDOFF.md Pre-flight #9 pattern).
- **Treating reference APK comparison as required:** It's already documented as deferrable (D-15 + Phase 3 precedent). Don't gate Phase 7 sign-off on it.
- **Pre-building GL CameraEffect "just in case":** D-16 forbids this. Measure → escalate only on fail.
- **Mutating manifest.json paths and forgetting to update tests:** `FilterCatalogExpandedTest` + `AssetLoaderTest` may have hard-coded `frame_00.png` strings. Grep before edit.

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Frame-time measurement | `Choreographer.FrameCallback` + nanoTime math | JankStats 1.0.0 | Handles input-to-display latency, jitter classification, and FrameMetrics integration ≥ API 24 — would take 100+ LOC to do half as well |
| Audio drift verification | Custom MediaExtractor + sample timestamp walk | `ffprobe` CLI on dev PC | ffprobe is mature, MP4-aware, scripted output, and runs on the dev machine not the device |
| R8 keep rules for Compose | hand-written `-keep class androidx.compose.**` | Bundled `consumer-proguard-rules.pro` from Compose Compiler plugin + Compose BOM | Google ships the canonical rules; hand rolling will over-keep and dilute R8 savings |
| R8 keep rules for Hilt | hand-written `-keep @dagger.hilt.android.AndroidEntryPoint` | Bundled rules from `hilt-android:2.57` AAR | Hilt has known issues with AGP 8.5+ minification but Hilt 2.57 ships fixes — let it ship the fix |
| ContentObserver lifecycle | manual register/unregister with `Lifecycle` observer hooks | `callbackFlow` + `awaitClose { unregister }` in repository, collected from ViewModel | Pattern 3 above; ViewModel scoping cleans up automatically |
| WebP encoding | scripted ImageMagick / hand-rolled cwebp wrapper | Android Studio "Convert to WebP" right-click OR plain `cwebp -lossless -q 100 in.png -o out.webp` | One-shot conversion; not a build-step automation |
| Per-OEM device matrix | "Did it crash?" subjective vibe check | Documented gate sheet matching 06-HANDOFF.md pattern + ffprobe + JankStats numeric evidence | Numeric evidence prevents revisit creep |

**Key insight:** Phase 7 is *measurement* and *configuration*, not engineering. Resist the urge to build infrastructure (no JSON perf-dump pipeline, no GL shader prototype, no CI integration). Land the smallest viable instrumentation, measure, document, ship.

## Common Pitfalls

### Pitfall 1: R8 strips a `BboxIouTracker` or `OneShotEvent` data-class field
**What goes wrong:** R8 sees `OneShotEvent.PhotoSaved(uri: Uri)` data class fields accessed only via Kotlin reflection-like syntax (component-N extraction in `when` clauses) and inlines them away. Phase 4-5 D-32 grep-asserts then fail at runtime even though the source still grep-matches.
**Why it happens:** R8 full-mode is more aggressive on Kotlin data classes than legacy ProGuard; sealed-class hierarchies with reified type checks can confuse it.
**How to avoid:** Phase 4/5 inline-fix protocol: run release APK on Xiaomi 13T, hit the OneShotEvent code paths (capture photo → expect Toast→PhotoSaved flow), watch logcat for `Caused by: java.lang.NoSuchFieldError` or `IllegalAccessException`. If observed, add ONE narrow keep rule per missing class — never `-keep class **`.
**Warning signs:** App boots fine in release build but crashes on first capture; or capture succeeds but the post-capture Preview screen shows blank `uri.toString()`.

### Pitfall 2: WebP conversion silently loses transparency on sprites with semi-transparent edges
**What goes wrong:** `cwebp -q 80` (lossy) introduces 1-2 pixel alpha-halo artifacts around antialiased sprite edges, visible as a faint colored ring on the live preview overlay.
**Why it happens:** WebP lossy mode optimizes RGB-perceptual quality, not alpha precision.
**How to avoid:** D-07 already specifies "lossless quality ≥90" — but Android Studio "Convert to WebP" treats `quality=100` as lossless and `quality≥18` as lossy. Use `cwebp -lossless -z 9` (max compression effort, lossless) at the CLI, OR pick `quality 100` in Android Studio dialog. Then visually diff sprite Bitmap.config before/after.
**Warning signs:** Filter sprite shows a faint colored fringe on the face overlay that wasn't there pre-conversion.

### Pitfall 3: ABI split breaks Robolectric tests
**What goes wrong:** Setting `abiFilters "arm64-v8a"` in `defaultConfig` propagates to `testApk`. Robolectric runs on the JVM and doesn't need any native ABI, but some transitive native libs (SQLite, image decoders) get filtered out and Robolectric's `ShadowArscAssetManager` fails to load.
**Why it happens:** AGP applies `abiFilters` to all variants by default unless scoped per-buildType.
**How to avoid:** Scope `abiFilters` to release build type only:
```kotlin
buildTypes {
    release {
        ndk { abiFilters += listOf("arm64-v8a") }
    }
    // debug: unspecified → keeps all ABIs for emulator + Robolectric compat
}
```
**Warning signs:** `./gradlew :app:testDebugUnitTest` starts failing with `UnsatisfiedLinkError` or `Resources$NotFoundException` post-Phase-7 changes, but only on the local test suite, not on the device.

### Pitfall 4: JankStats hardware acceleration requirement
**What goes wrong:** JankStats `createAndTrack(window)` silently returns a no-op tracker if the Window is not hardware-accelerated (e.g., emulator with software rendering).
**Why it happens:** Frame timing requires hardware-accelerated rendering pipeline; the library guards against bad data sources.
**How to avoid:** Test on the physical Xiaomi 13T, never on emulator. Hardware acceleration is on by default for `<application android:hardwareAccelerated="true">` (the AGP default since AGP 4.x). If a `<activity>` overrides it to false, JankStats reports nothing on that activity.
**Warning signs:** JankStats listener registered but `Timber.tag("Perf")` produces zero output during a 30s session. [CITED: developer.android.com/jetpack/androidx/releases/metrics — beta03 fix note]

### Pitfall 5: ContentObserver leaks if observer is registered on the wrong URI scope
**What goes wrong:** Observer registered on `MediaStore.Images.Media.EXTERNAL_CONTENT_URI` does NOT fire when a row is inserted via `MediaStore.Files`. The "newest first" sort means a fresh capture may register but not fire.
**Why it happens:** Per-MIME URIs are different ContentProvider paths even though they share a backing database.
**How to avoid:** Register on `MediaStore.Files.getContentUri("external")` with `notifyForDescendants=true` (matches the query URI we already use in `CollectionRepository`). Single observer = single source of truth.
**Warning signs:** Capture a photo → return to Collection → grid does NOT refresh. Capture another → still does NOT refresh. (If observer was unregistered, you'd see NO refresh; if it's on wrong URI, you see SOMETIMES — these symptoms differ).

### Pitfall 6: `tools:node="remove"` requires `xmlns:tools` declaration
**What goes wrong:** `app/src/debug/AndroidManifest.xml` declares `<activity tools:node="remove">` without `xmlns:tools="http://schemas.android.com/tools"` in the `<manifest>` root. Manifest merger silently ignores the directive.
**Why it happens:** Manifest merger treats unrecognized namespaces as comments.
**How to avoid:** Pattern 4 above includes the `xmlns:tools` declaration. Verify via `./gradlew :app:processDebugManifest` then inspect `app/build/intermediates/merged_manifests/debug/AndroidManifest.xml` to confirm `LeakLauncherActivity` is absent.
**Warning signs:** LeakCanary "Leaks" launcher icon still appears on home screen after Phase 7 Wave 2 lands.

### Pitfall 7: ffprobe drift calculation misinterprets `start_time` 0.0 / 0.0 as "perfect sync"
**What goes wrong:** Many Android MP4s have audio.start_time = 0.000000 and video.start_time = 0.000000 — diff is zero, looks like perfect sync. But duration mismatch is the real signal.
**Why it happens:** MediaMuxer aligns both streams to t=0 at container header level; actual sync is reflected in per-sample DTS/PTS within the stream, not the header.
**How to avoid:** Compute BOTH `start_time` delta AND `duration` delta:
```bash
# audio
audio_start=$(ffprobe -v 0 -select_streams a:0 -show_entries stream=start_time -of csv=p=0 video.mp4)
audio_dur=$(ffprobe -v 0 -select_streams a:0 -show_entries stream=duration   -of csv=p=0 video.mp4)
# video
video_start=$(ffprobe -v 0 -select_streams v:0 -show_entries stream=start_time -of csv=p=0 video.mp4)
video_dur=$(ffprobe -v 0 -select_streams v:0 -show_entries stream=duration   -of csv=p=0 video.mp4)

# Drift = (audio_start - video_start) + (audio_dur - video_dur)
# Both terms should be < 50ms (0.050) for PRF-03 PASS.
```
Pass criterion: `|audio_start - video_start| < 0.050 AND |audio_dur - video_dur| < 0.050`. [CITED: ffmpeg.org/ffprobe-all.html]

### Pitfall 8: `splits.abi` vs `abiFilters` confusion in D-08
**What goes wrong:** Following CONTEXT D-08 wording literally ("ABI splits configuration: abiFilters") produces two different outputs depending on which API you use.
**Why it happens:** Terminology overlap — Google's docs use both "ABI splits" and "ABI filters" for distinct features.
**How to avoid:** Per WebFetch on Android Developers docs:
- `defaultConfig { ndk { abiFilters("arm64-v8a") } }` → ONE APK containing only arm64-v8a native code
- `splits { abi { include("arm64-v8a"); isUniversalApk = false } }` → ONE per-ABI APK (same result for single ABI)
**Recommendation:** Use the simpler `abiFilters` form. D-08 intent is "drop x86_64 and armeabi-v7a"; both forms achieve this. Single ABI = no need for splits mechanism. [VERIFIED: developer.android.com/build/configure-apk-splits]

### Pitfall 9: Compose `setContent` overhead inflates first-frame jank count
**What goes wrong:** First JankStats `frameData.isJank == true` event always fires during cold-start setContent — the initial composition + measure + layout pass exceeds the 16ms budget on every Android device by design.
**Why it happens:** First-frame setup work is amortized over subsequent frames; this is expected.
**How to avoid:** Aggregate JankStats data from second 1.0 onwards (`frameData.frameStartNanos > activityStartNanos + 1_000_000_000L`). PerfReporter aggregator should skip the first 60 frames (≈2s at 30fps).
**Warning signs:** JankStats reports 100% jank rate for the first 200ms of every session — this is normal, not a regression.

## Runtime State Inventory

Phase 7 is hardening / verification — no rename / refactor / migration. **All 5 categories: None.**

| Category | Items Found | Action Required |
|----------|-------------|------------------|
| Stored data | None — Phase 7 changes no on-disk schemas, no DataStore keys, no MediaStore conventions | None |
| Live service config | None — no external services (offline-only app per memory `project_offline_constraint.md`) | None |
| OS-registered state | **WATCH:** R8 minification may obfuscate `MainActivity` class name in release APK. Reference APK `am start` commands like `am start -n com.bugzz.filter.camera/.MainActivity` rely on the un-obfuscated `MainActivity` name. R8 keeps `<activity>` entries listed in AndroidManifest.xml by default (`-keep public class * extends android.app.Activity` is in proguard-android-optimize.txt), so the FQN survives. Verify via `aapt dump xmltree release.apk AndroidManifest.xml` after the first release build. | Verification step only — listed as 07-HANDOFF Step 0 check |
| Secrets/env vars | None — no secrets/env in Phase 7 scope | None |
| Build artifacts | **Stale build artifacts after `minifyEnabled true` flip:** `app/build/intermediates/` will have debug-shape outputs that don't match the new release shape until a clean. Recommend `./gradlew clean :app:assembleRelease` for the first release build of Phase 7 Wave 1. Also: `app/build/outputs/apk/release/` will be new (Phase 1-6 never produced a release APK). | Wave 1 plan should call `./gradlew clean :app:assembleRelease` explicitly |

## Code Examples

Verified patterns from official sources or in-project precedent:

### JankStats wire-in (D-01) — `MainActivity.kt`
Pattern 1 above. [CITED: github.com/android/performance-samples/JankStatsSample]

### Face detection latency timing (D-04) — `FaceDetectorClient.kt` amendment
Pattern 2 above. [Source: in-project current file lines 52-97]

### ContentObserver-backed Flow (D-20b) — `CollectionRepository.kt` amendment
Pattern 3 above. [Verified pattern: community-standard `callbackFlow + awaitClose`]

### `app/build.gradle.kts` release block (D-06 + D-08)

```kotlin
// Source: derived from current app/build.gradle.kts + AGP 8.9 docs

android {
    // ... existing config ...

    buildTypes {
        release {
            isMinifyEnabled = true         // D-06 (was false)
            isShrinkResources = true       // D-06 (NEW; requires minify)
            isDebuggable = false           // D-06 (default false; explicit for clarity)
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            ndk {
                abiFilters += listOf("arm64-v8a")  // D-08 — release-only (Pitfall 3)
            }
        }
        debug {
            // Unchanged. Debug keeps all ABIs for emulator/Robolectric compat.
        }
    }
}
```

### `app/proguard-rules.pro` skeleton (D-06)

```proguard
# Phase 7 (Plan 07-W1): release minification enabled.
# Phase 1-6 strategy: NO pre-emptive keep rules. Hilt 2.57 + Compose Compiler ship
# consumer-proguard-rules in their AARs that handle their own reflection needs.
# We add keep rules HERE only when empirical R8 failure proves them necessary
# (Phase 4/5 inline-gap-fix protocol).

# Generic safety nets for Timber + crash diagnostics (always safe to keep):
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Reactive section — populate as R8 failures surface during device verification:
# (add narrow -keep rules here, one per observed failure)
```

### `app/src/debug/AndroidManifest.xml` (D-20c) — NEW FILE
Pattern 4 above. [CITED: developer.android.com/build/manage-manifests#node_markers]

### Audio sync verification script (D-10/D-11)

```bash
#!/usr/bin/env bash
# Phase 7 PRF-03 verification — run on dev PC against a pulled MP4.
# Usage: ./verify-audio-sync.sh /path/to/Bugzz_*.mp4

mp4="$1"
audio_start=$(ffprobe -v 0 -select_streams a:0 -show_entries stream=start_time -of csv=p=0 "$mp4")
audio_dur=$(ffprobe -v 0   -select_streams a:0 -show_entries stream=duration   -of csv=p=0 "$mp4")
video_start=$(ffprobe -v 0 -select_streams v:0 -show_entries stream=start_time -of csv=p=0 "$mp4")
video_dur=$(ffprobe -v 0   -select_streams v:0 -show_entries stream=duration   -of csv=p=0 "$mp4")
nb_frames=$(ffprobe -v 0   -select_streams v:0 -count_frames -show_entries stream=nb_read_frames -of csv=p=0 "$mp4")

start_drift=$(echo "$audio_start - $video_start" | bc -l)
dur_drift=$(echo "$audio_dur - $video_dur" | bc -l)

echo "audio_start=$audio_start video_start=$video_start  -> start_drift=$start_drift s"
echo "audio_dur=$audio_dur     video_dur=$video_dur      -> dur_drift=$dur_drift s"
echo "video frames counted=$nb_frames (expect ~1800 for 60s @ 30fps)"

# PASS criteria (D-10/11):
#   |start_drift| < 0.050 (50ms)
#   |dur_drift|   < 0.050 (50ms)
#   1795 ≤ nb_frames ≤ 1805
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| Hand-rolled `Choreographer.FrameCallback` perf instrumentation | JankStats (`androidx.metrics:metrics-performance`) | Oct 2022 alpha; **Oct 2025 stable 1.0.0** | Phase 7 uses stable 1.0.0 — bump from CONTEXT's `1.0.0-beta02` |
| ProGuard (legacy) | R8 (Android-tuned successor) | Android Studio 3.4+ (2019); default in AGP 4.x | Already default; D-06 simply enables it |
| Multi-ABI APK | App Bundle (.aab) + Play-side per-ABI split | Aug 2021 Play Store policy | Bugzz is personal-use, no Play Store — App Bundle adds no value; arm64-v8a-only APK is correct (D-08) |
| `WRITE_EXTERNAL_STORAGE` everywhere | MediaStore + scoped storage | Android 10 (Q) hard requirement | Already adopted in Phase 3-5 |
| Per-MIME ContentObserver | Single Files URI observer | Pattern stable since API 16 | Phase 7 adopts (D-20b) |
| `cwebp` from FFmpeg suite | Android Studio "Convert to WebP" dialog | Studio 3.x | Either works; CLI is more scriptable |
| Compose BOM 2026.03.00 (Phase 1-6) | Compose BOM 2026.04.01 (April 2026 release) | April 2026 | Optional bump; defer to v2 (Phase 7 = hardening, not modernization) |

**Deprecated/outdated:**
- `WRITE_EXTERNAL_STORAGE` for any reason on API 29+ — not used in Phase 7
- `applovin / admob` — out of scope (v2 milestone)
- `splits.density` — only relevant for `<application android:installLocation="internalOnly">` with massive resource buckets; not applicable

## Assumptions Log

| # | Claim | Section | Risk if Wrong |
|---|-------|---------|---------------|
| A1 | The "face bounding box visible in baked JPG" item from 06-CHECKPOINT may NOT actually be `DebugOverlayRenderer` (its draw method already has `BuildConfig.DEBUG` early-return). May be a second debug path in `FilterEngine` or `BboxIouTracker`. | Pattern 1 / D-20a investigation note | Planner spends Wave 2 editing the wrong file. Mitigation: planner's first task is grep for `Color.RED` + `drawRect` + `BboxIou.*draw` to locate the actual source before deciding the fix scope. |
| A2 | Sprite `manifest.json` files reference frames as `frame_NN.png` and will need updating to `frame_NN.webp` post-conversion. Not grep-verified in this research pass. | Pattern 5 | WebP conversion lands but app crashes on FilterEngine.loadFilter() — `FileNotFoundException`. Mitigation: include a single-commit-atomic step in Wave 1 that converts files AND rewrites manifest paths. |
| A3 | The 9 D-32 grep-asserts (Phase 3+4+5 inline fixes) will survive R8 in their current form without explicit keep rules. Hilt 2.57 + Compose ship rules; Phase 4-5 inline fixes are plain Kotlin without reflection. | Pitfall 1 | R8 strips a code path; release APK installs but crashes on first capture/record. Mitigation: 07-HANDOFF includes explicit smoke test of every D-32 grep-assert against the RELEASE apk (not debug). |
| A4 | Compose BOM 2026.04.01 v2 testing APIs would require non-trivial test-suite migration if adopted. Phase 1-6 has 172 unit tests, most using `runTest` from kotlinx-coroutines-test rather than Compose-specific test rules. | Standard Stack — Optional Compose BOM Bump | Bump-and-pray approach breaks tests. Mitigation: defer the bump; stay on 2026.03.00. |
| A5 | Firebase Test Lab 2026 free tier supports installing a release APK ≥30 MB and running a basic Robo test or instrumented test against it within the 30-min daily quota. | Standard Stack / D-14 | User opts in to Firebase Test Lab and finds the quota exhausted by APK upload time. Mitigation: D-14 is opt-in; document as a v2 reach goal in 07-HANDOFF rather than a gate. |
| A6 | `splits.abi` and `abiFilters` produce equivalent results for the single-ABI case (per the Android Developers docs). | Pitfall 8 + Alternatives | Planner uses one and gets unexpected APK split file naming. Mitigation: recommendation is `abiFilters` for simplicity; if user wants per-ABI APK naming, fall through to `splits.abi`. |
| A7 | R8 default rules from `proguard-android-optimize.txt` keep `MainActivity` class name (because it's declared in AndroidManifest.xml). ADB `am start -n com.bugzz.filter.camera/.MainActivity` will continue to work post-minification. | Runtime State Inventory / OS-registered state | ADB launch command breaks in release builds. Mitigation: 07-HANDOFF Step 0 verifies via `aapt dump xmltree release.apk AndroidManifest.xml`. |
| A8 | Bumping `metricsPerformance` from D-01's stated `1.0.0-beta02` to verified stable `1.0.0` is a no-op API-surface change. | Standard Stack version correction | Beta API surface might differ from stable. Mitigation: 1.0.0 changelog explicitly lists "API stabilization, no method removals" — verifiable via [Metrics release notes](https://developer.android.com/jetpack/androidx/releases/metrics). |

## Open Questions

### Q1: Where does the face bounding box rendering in the saved JPG actually originate?
- **What we know:** `DebugOverlayRenderer.draw()` lines 60 already has the `BuildConfig.DEBUG` gate. Released debug builds are debug-shape so the gate doesn't fire — that explains the JPG content. The "polish item" intent of D-20a is presumably: when we build RELEASE for Phase 7, the bbox should NOT appear (because of BuildConfig.DEBUG check returning early). So D-20a may be **a no-op for release builds** but the user wants visual confirmation.
- **What's unclear:** Should bbox be visible in debug release captures too? Or should we add a SECOND gate (e.g., `DebugFlags.SHOW_BBOX = false` with manual override)?
- **Recommendation:** Plan 07-Wave-2 Task: (a) verify current `DebugOverlayRenderer` gate fires correctly in release; (b) IF the bbox still appears, find the second draw path; (c) if no second path, D-20a is automatic — document the gate is already in place. Surface this as a 4-line investigation task, not 30 lines of rendering rework.

### Q2: Will WebP conversion meaningfully shrink the sprite assets given they're already tiny?
- **What we know:** Total sprite PNG bytes today is **~666 KB** (60 files). The Phase 6 → Phase 5 debug-APK delta was +~8 MB and most of that was Lottie + media3 + Phase 6 code, not sprites.
- **What's unclear:** WebP lossless at 30-50% reduction on 666 KB = saves ~200-330 KB. APK target is 40 MB total (currently 88 MB debug → projected release ~25-35 MB). Sprite WebP savings are <1% of the goal.
- **Recommendation:** Do D-07 because it's locked, but recognize the SCALE — WebP gives quality-of-life cleanliness, R8 + LeakCanary strip + ABI filter are the real APK shrinkers.

### Q3: Is the Firebase Test Lab path worth setting up for opportunistic Pixel coverage?
- **What we know:** Free tier = 30 min/day physical device time. A single Robo test on Pixel takes ~3-5 min. Could squeeze 4-6 runs/day.
- **What's unclear:** Setting up Test Lab requires a Firebase project, service account JSON, and gcloud CLI auth. ~30-60 min one-time setup. ROI depends on whether the user can source a Pixel locally.
- **Recommendation:** Default to **skip** (matches D-14 wording "opportunistic"). Document the setup steps in 07-HANDOFF as an appendix, opt-in only if user explicitly requests.

### Q4: Should Phase 7 enable signing with a debug-style release keystore, or a fresh release keystore?
- **What we know:** D-22 says "debug keystore OK for personal use." Android's default `~/.android/debug.keystore` (or AGP's auto-generated debug key) is signing-compatible for `adb install` of release APKs.
- **What's unclear:** Some Xiaomi HyperOS builds reject release APKs signed with debug keys on first install. Reference APK install issues from Phase 3 (per memory) may recur.
- **Recommendation:** Wave 1 first attempt: sign release with the default debug keystore. If install fails on Xiaomi 13T, generate a one-off release keystore (`keytool -genkey -v -keystore bugzz-release.jks ...`) and document the password / alias in a NOT-IN-GIT file under `.planning/.private/`. Treat as a Plan 07-Wave-1 inline-gap fix if it surfaces.

### Q5: Does the existing `frameCounter` increment in FaceDetectorClient (line 60-61) overlap semantically with the Phase 7 `frame` argument in `Timber.tag("Perf").d("detect=%dms frame=%d ...")`?
- **What we know:** `frameCounter++` in line 61 already exists for thermal frame-skip (D-14 Phase 5). It's a `private var` on `FaceDetectorClient`, reset never. Pattern 2 above reuses it.
- **What's unclear:** None — reusing it is correct.
- **Recommendation:** Reuse. No new counter. Document the dual use in the inline comment.

## Environment Availability

| Dependency | Required By | Available | Version | Fallback |
|------------|------------|-----------|---------|----------|
| Android SDK at `C:\Users\Admin\AppData\Local\Android\Sdk` | Build + ADB | ✓ (per memory `env_paths.md`) | AGP 8.9.1 sufficient | — |
| Java 21 JDK (bundled with Android Studio) | Gradle 8.13 / AGP 8.9 | ✓ (memory) | JDK 21 (jbr) | Wave 0 confirms |
| `adb` at `C:/Users/Admin/AppData/Local/Android/Sdk/platform-tools/adb.exe` | All device verification flows | ✓ (Phase 1-6 validated) | platform-tools (current) | — |
| Xiaomi 13T device | Primary OEM (D-12) | ✓ (user owns; Phase 6 sign-off device) | Android 15 HyperOS | — |
| Samsung A-series OR Pixel A-series | Secondary OEM (D-13) | ✗ Awaiting user | — | Firebase Test Lab free tier 30min/day (D-14) — OPT-IN ONLY |
| `ffprobe` from FFmpeg | PRF-03 audio drift verification (D-10/D-11) | ❓ Not in CLAUDE.md tooling list; standard dev install | ≥4.x | Manual lip-sync subjective check if ffprobe unavailable |
| `cwebp` CLI OR Android Studio WebP dialog | D-07 sprite conversion | ✓ Android Studio bundles "Convert to WebP" dialog | — | Either path equivalent |
| Firebase Test Lab account (Google Cloud project + gcloud CLI) | D-14 opportunistic Pixel coverage | ❓ Not set up; opt-in only | — | Skip; document as v2 reach goal |
| `keytool` (JDK bundled) | One-off release keystore if D-22 default-debug-keystore signing fails | ✓ JDK 21 ships keytool | — | — |

**Missing dependencies with no fallback:**
- None blocking. Samsung/Pixel device is the only missing item that affects scope; Firebase Test Lab is the documented (opt-in) fallback. PRF-05 sign-off acceptance per D-13 permits Xiaomi-only + documented gap if no secondary OEM available.

**Missing dependencies with fallback:**
- Samsung/Pixel: Firebase Test Lab opt-in (D-14)
- ffprobe (if not installed): subjective audio sync test (downgrades PRF-03 to manual UAT)

## Validation Architecture

> nyquist_validation is `true` in config.json — section is included.

### Test Framework

| Property | Value |
|----------|-------|
| Framework | JUnit 4 4.13.2 + Mockito 5.11.0 + mockito-kotlin 5.2.1 + Robolectric 4.13 + Turbine 1.2.0 + kotlinx-coroutines-test 1.10.2 |
| Config file | `app/build.gradle.kts` `testOptions { unitTests { isReturnDefaultValues = true; isIncludeAndroidResources = true } }` |
| Quick run command | `gradlew :app:testDebugUnitTest --tests 'com.bugzz.filter.camera.{package}.*'` |
| Full suite command | `gradlew :app:testDebugUnitTest` |

**Current test count baseline (Phase 6 close):** 172 tests, 0 ignored, 0 failures.

### Phase Requirements → Test Map

| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| PRF-01 | Live preview ≥24 fps under filter playback | manual-only on-device (JankStats Logcat aggregation) | Read Logcat tag `Perf` during 30s session; compute pXX offline | ❌ Wave 0 — JankStats setup |
| PRF-01 | JankStats wires into Activity lifecycle (onCreate registers, onResume enables, onPause disables) | unit (Robolectric `ActivityScenario`) | `gradlew :app:testDebugUnitTest --tests '*MainActivityJankStatsTest*'` | ❌ Wave 0 |
| PRF-01 | `PerfReporter` aggregates pXX from a sequence of FrameData samples | unit (pure JVM) | `gradlew :app:testDebugUnitTest --tests '*PerfReporterTest*'` | ❌ Wave 0 |
| PRF-02 | Face detection latency ≤100ms median | manual-only on-device + Logcat aggregation (debug-only Timber tag `Perf`) | grep + awk over `adb logcat` output | ❌ — verification via runbook, not unit test |
| PRF-02 | `DetectionLatencyRecorder` ring buffer correctly aggregates median/p95/p99 | unit (pure JVM) | `gradlew :app:testDebugUnitTest --tests '*DetectionLatencyRecorderTest*'` | ❌ Wave 0 |
| PRF-02 | Latency Timber log emits only in debug builds | unit (BuildConfig flag flip via Robolectric) | covered by `FaceDetectorClientTest` extension | ❌ Wave 0 (extension to existing test) |
| PRF-03 | 60s recorded MP4 has audio drift <50ms | manual-only on-device + dev-PC ffprobe | `bash verify-audio-sync.sh /tmp/test.mp4` | ❌ — script in repo + runbook step |
| PRF-04 | Release APK ≤40 MB | manual-only on-device (APK Analyzer) | `du -h app/build/outputs/apk/release/*.apk` | ❌ — verification step in runbook |
| PRF-04 | Sprite WebP conversion preserves frame count + alpha config | unit (pure JVM Bitmap.Config assertion) | `gradlew :app:testDebugUnitTest --tests '*WebPSpriteCompatTest*'` | ❌ Wave 0 |
| PRF-04 | `manifest.json` references frame_NN.webp (not .png) post-conversion | unit (JSON parse + assertion) | `gradlew :app:testDebugUnitTest --tests '*SpriteManifestPathTest*'` | ❌ Wave 0 |
| PRF-04 | All 9 D-32 grep-asserts continue to pass against release APK | manual smoke + grep on release APK source map | runbook step (release apk decompile + grep) | ❌ — runbook step |
| PRF-05 | App installs cleanly on secondary OEM | manual-only on-device | runbook step | ❌ |
| PRF-05 | App captures + records on secondary OEM | manual-only on-device | runbook step | ❌ |
| D-20a | DebugOverlayRenderer.draw gated by BuildConfig.DEBUG | unit (verify gate fires; Robolectric or BuildConfig flag manipulation) | `gradlew :app:testDebugUnitTest --tests '*DebugOverlayRendererTest.draw_skips_in_release*'` | ✅ extend existing `DebugOverlayRendererTest` |
| D-20b | `CollectionRepository.loadMediaItems` registers + unregisters ContentObserver | unit (Robolectric ShadowContentResolver + verify register/unregister called) | `gradlew :app:testDebugUnitTest --tests '*CollectionRepositoryContentObserverTest*'` | ❌ Wave 0 — new test |
| D-20b | Observer triggers re-query | unit (simulate observer.onChange + verify new emission on Turbine flow) | extension to above | ❌ Wave 0 |
| D-20c | Debug manifest disables `LeakLauncherActivity` | unit (parse merged manifest XML + assert activity absent) | manual verification post-build | ❌ — runbook step |
| D-24 | R8 keep rules (when reactive) — each added rule has a corresponding regression test (the failing-then-passing code path) | unit per added rule | as encountered | ❌ — only if R8 strips something |

### Sampling Rate

- **Per task commit:** `gradlew :app:testDebugUnitTest --tests '<targeted package>'` (typically <30s)
- **Per wave merge:** `gradlew :app:testDebugUnitTest` (full suite; Phase 6 baseline ~3min)
- **Phase gate:** Full suite green + `gradlew :app:assembleRelease` succeeds + APK Analyzer ≤40MB + device PASS on Xiaomi 13T + (secondary OEM PASS OR documented gap per D-13)

### Wave 0 Gaps (test files to create before SUT implementation lands)

- [ ] `app/src/test/java/com/bugzz/filter/camera/perf/PerfReporterTest.kt` — covers PRF-01/PRF-02 aggregation logic
- [ ] `app/src/test/java/com/bugzz/filter/camera/perf/DetectionLatencyRecorderTest.kt` — covers PRF-02 ring buffer + pXX math
- [ ] `app/src/test/java/com/bugzz/filter/camera/data/CollectionRepositoryContentObserverTest.kt` — covers D-20b register/unregister + onChange triggers re-query (Robolectric)
- [ ] `app/src/test/java/com/bugzz/filter/camera/assets/SpriteManifestPathTest.kt` — covers PRF-04 manifest.json regen post-WebP
- [ ] `app/src/test/java/com/bugzz/filter/camera/assets/WebPSpriteCompatTest.kt` — verifies AssetLoader can decode `frame_NN.webp` (Robolectric)
- [ ] Extension to `app/src/test/java/com/bugzz/filter/camera/ui/MainActivityJankStatsTest.kt` — Robolectric ActivityScenario verifying JankStats wire-in (new file, but tests `MainActivity` SUT)
- [ ] Extension to `app/src/test/java/com/bugzz/filter/camera/detector/FaceDetectorClientTest.kt` — adds `perfTimingLog_emitsInDebugOnly` test for D-04 (existing file, add test method)
- [ ] Extension to `app/src/test/java/com/bugzz/filter/camera/render/DebugOverlayRendererTest.kt` — adds `draw_skips_in_release` test for D-20a verification (existing file, add test method)

> **Net new test files:** 5 (4 new + 1 MainActivity test). **Existing files extended:** 2 (FaceDetectorClientTest, DebugOverlayRendererTest). Aligns with CONTEXT D-25's "5-7 test files" Claude's Discretion item.

> **Manual-only requirements:** PRF-01 (live fps), PRF-02 (latency aggregate), PRF-03 (audio sync), PRF-04 (release APK size + D-24 grep), PRF-05 (cross-OEM), D-19 (thermal stress), 8 deferred UAT items (D-21). These are device-runbook items, not unit tests — covered via `07-HANDOFF.md` per Phase 6 precedent.

## Estimated Wave Structure (refinement on CONTEXT D-25's 5-7 plan budget)

D-25 outlines 7 plans across 5-6 waves. Reviewing against file-overlap risk + dependency ordering yields this refinement:

| Wave | Plan | Concern | Files Touched (approx) | Risk |
|------|------|---------|------------------------|------|
| W0 | 07-01 | Nyquist scaffolds: 5 new test files + 2 extensions; `libs.versions.toml` adds `metricsPerformance = "1.0.0"`; new package skeletons `perf/` (empty SUTs) | ~10 files | LOW — pure test additions |
| W1 | 07-02 | Release build config flip (D-06 + D-08); WebP conversion + manifest.json regen (D-07); first `assembleRelease` + R8 smoke + Xiaomi 13T release install + post-release `:app:testDebugUnitTest` regression | `app/build.gradle.kts`, `app/proguard-rules.pro`, `app/src/main/assets/sprites/**/*.png → *.webp + manifest.json`, possibly inline R8 fixes | **HIGH** — biggest risk in phase; R8 may surface keep-rule needs |
| W2 | 07-03 | JankStats Hilt module + MainActivity wire-in + PerfReporter aggregator (D-01); `FaceDetectorClient` latency log inline (D-04); DetectionLatencyRecorder | `perf/JankStatsModule.kt`, `perf/PerfReporter.kt`, `perf/DetectionLatencyRecorder.kt`, `MainActivity.kt`, `detector/FaceDetectorClient.kt` | MEDIUM — multi-file; FaceDetectorClient is a hot path |
| W2 | 07-04 | Phase 6 polish trio: D-20a (DebugOverlayRenderer gate verify or fix), D-20b (CollectionRepository ContentObserver), D-20c (debug manifest LeakLauncherActivity disable) | `render/DebugOverlayRenderer.kt` (only if Q1 finds a second draw path), `data/CollectionRepository.kt`, `app/src/debug/AndroidManifest.xml` (NEW) | LOW-MEDIUM — three small concerns; ContentObserver in CollectionRepository may break existing CollectionRepositoryTest if assertions hard-code single-emission shape (Turbine will need adjustment) |
| W3 | 07-05 | `07-PERF-REPORT.md` scaffold + Xiaomi 13T baseline measurement runbook + first measurement run (PRF-01/02 baseline + PRF-03 60s audio drift via ffprobe) | `.planning/phases/07-.../07-PERF-REPORT.md` (NEW), no code changes | LOW — documentation + measurement |
| W4 | 07-06 | Device matrix runbook (07-HANDOFF.md) + secondary OEM handoff + 8 deferred UAT items folded (D-21) | `.planning/phases/07-.../07-HANDOFF.md` (NEW) | LOW — documentation |
| W5 | 07-07 | Phase 7 close-out: final release APK + APK Analyzer report + 07-CHECKPOINT.md + nyquist flip + STATE/ROADMAP/REQUIREMENTS mark-complete + final commit | `.planning/phases/07-.../07-CHECKPOINT.md` (NEW), `.planning/STATE.md`, `.planning/ROADMAP.md`, `.planning/REQUIREMENTS.md` | LOW — close-out |

**Total: 7 plans / 6 waves** (refines CONTEXT D-25's 5-7 / 4-5 — matches the upper bound). Wave 1 is the highest-risk wave; everything downstream depends on it producing a clean release APK. Strongly recommend a CHECKPOINT-level human verification at end of Wave 1 (release APK installs + boots on Xiaomi 13T) before Wave 2 spawns JankStats integration against a possibly-broken release shape.

**Alternative: if Wave 1 stays clean (no R8 fixes needed) and W4 merges with W3 (PERF-REPORT + HANDOFF as one plan):**
- Could collapse to **5 plans / 4 waves**
- Risk: W3+W4 as one plan would be a single ~500-LOC documentation plan; manageable but breaks the per-plan one-concern principle

Recommend **7 plans / 6 waves** for clarity and per-plan reviewability.

## Sources

### Primary (HIGH confidence)
- [androidx.metrics:metrics-performance release notes](https://developer.android.com/jetpack/androidx/releases/metrics) — verified `1.0.0` stable, Oct 8 2025
- [JankStats Library guide](https://developer.android.com/topic/performance/jankstats) — integration pattern, `isTrackingEnabled` lifecycle
- [JankStatsSample official sample](https://github.com/android/performance-samples/blob/main/JankStatsSample/app/src/main/java/com/example/jankstats/JankLoggingActivity.kt) — canonical wire-in
- [Configure APK splits](https://developer.android.com/build/configure-apk-splits) — `splits.abi` vs `abiFilters` distinction
- [Manifest merger node markers](https://developer.android.com/build/manage-manifests#node_markers) — `tools:node="remove"` for debug-only overrides
- [About R8 keep rules](https://developer.android.com/topic/performance/app-optimization/keep-rules-overview)
- [Enable app optimization with R8](https://developer.android.com/topic/performance/app-optimization/enable-app-optimization)
- [Configure and troubleshoot R8 Keep Rules — blog](https://android-developers.googleblog.com/2025/11/configure-and-troubleshoot-r8-keep-rules.html)
- [Compose April 2026 release notes](https://android-developers.googleblog.com/2026/04/jetpack-compose-april-2026-updates.html) — BOM 2026.04.01 availability
- [Create WebP images in Android Studio](https://developer.android.com/studio/write/convert-webp) — Convert to WebP dialog
- [cwebp CLI docs](https://developers.google.com/speed/webp/docs/cwebp) — lossless encoding flags
- [ffprobe documentation](https://ffmpeg.org/ffprobe-all.html) — `-show_entries` + stream selection
- [Firebase Test Lab usage quotas and pricing](https://firebase.google.com/docs/test-lab/usage-quotas-pricing) — 30 min/day free physical
- [CameraX release notes](https://developer.android.com/jetpack/androidx/releases/camera) — OverlayEffect API surface
- [CameraEffect API reference](https://developer.android.com/reference/androidx/camera/core/CameraEffect) — escalation contract

### Secondary (MEDIUM confidence)
- [Mastering ProGuard in Multi-Module / R8 / AGP 8.4+ — Medium](https://drjansari.medium.com/mastering-proguard-in-android-multi-module-projects-agp-8-4-r8-and-consumable-rules-ae28074b6f1f) — AGP 8.4 minify restrictions, consumer-rules pattern
- [Hilt Class Generation Fails After Upgrading to AGP 8.5.1 — google/dagger#4384](https://github.com/google/dagger/issues/4384) — known minification regression (resolved in current Hilt)
- [Kotlin Flow + ContentResolver + MediaStore — Medium](https://medium.com/@sheikhzakirahmad/kotlin-flow-contentresolver-and-mediastore-the-key-to-effortless-media-access-in-android-fad56db16fdd) — callbackFlow pattern
- [Side-effects in Compose](https://developer.android.com/develop/ui/compose/side-effects) — DisposableEffect lifecycle

### In-project (HIGH confidence — direct reads this session)
- `.planning/phases/07-performance-device-matrix/07-CONTEXT.md` — 26 D-decisions
- `.planning/REQUIREMENTS.md` — PRF-01..05 acceptance criteria
- `.planning/STATE.md` — project state + accumulated decisions
- `.planning/ROADMAP.md` Phase 7 section — goal + 5 success criteria
- `.planning/phases/06-ux-polish-.../06-CHECKPOINT.md` — 3 polish items
- `.planning/phases/06-ux-polish-.../06-HANDOFF.md` — 8 deferred UAT items
- `app/build.gradle.kts` — current release block (minify disabled)
- `gradle/libs.versions.toml` — current pinned versions
- `app/src/main/java/com/bugzz/filter/camera/detector/FaceDetectorClient.kt` — D-04 instrumentation target
- `app/src/main/java/com/bugzz/filter/camera/render/DebugOverlayRenderer.kt` — D-20a gate (already in place at line 60)
- `app/src/main/java/com/bugzz/filter/camera/data/CollectionRepository.kt` — D-20b ContentObserver target
- `CLAUDE.md` — locked stack + Fallback Plan + What NOT to Use tables

## Metadata

**Confidence breakdown:**
- Standard stack (JankStats 1.0.0, R8 defaults, WebP encoding, ffprobe): **HIGH** — all verified against official docs
- Architecture patterns (JankStats wire-in, ContentObserver Flow, debug manifest overlay): **HIGH** — multiple sources + community-standard idioms
- Pitfalls (R8 stripping, WebP halo, ABI split test-side leak, ContentObserver URI scope): **HIGH** — pattern-matched against documented gotchas
- Compose BOM 2026.04.01 bump-or-stay decision: **MEDIUM** — verified availability, but v2 testing API switch risk on existing test suite unverified
- Firebase Test Lab quota: **HIGH** — official docs verified May 2026
- Assumed claims (A1-A8 above) are tagged in Assumptions Log

**Research date:** 2026-05-13
**Valid until:** 2026-06-12 (30 days — Android tooling moves fast; CameraX 1.7.x or Compose BOM 2026.05.xx may ship within window and shift bumps)
