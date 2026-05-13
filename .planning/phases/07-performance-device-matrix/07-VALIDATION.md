---
phase: 07
slug: performance-device-matrix
status: complete
nyquist_compliant: true
wave_0_complete: true
created: 2026-05-13
---

# Phase 07 — Validation Strategy

> Per-phase validation contract. Extends Phase 6 harness (172 baseline tests) with 5 new test files + 2 extensions covering PRF-01..05 + D-20 polish trio.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit 4.13.2 + Mockito 5.11 + Mockito-Kotlin 5.2.1 + Robolectric 4.13 + Turbine 1.2.0 + kotlinx-coroutines-test 1.10.2 (all from Phase 6) + JankStats 1.0.0 (new) |
| **Config file** | `gradle/libs.versions.toml` (+`metricsPerformance = "1.0.0"`) + `app/build.gradle.kts` (release buildType flip + abiFilters arm64-v8a) |
| **Quick run command** | `./gradlew :app:testDebugUnitTest` (with `JAVA_HOME="/c/Program Files/Android/Android Studio/jbr"`) |
| **Full suite command** | `./gradlew :app:testDebugUnitTest :app:assembleDebug :app:assembleRelease` |
| **Estimated runtime** | ~150 seconds (Phase 6 baseline ~90s + release build ~60s + 5 new test classes) |

---

## Sampling Rate

- **Per task commit:** `./gradlew :app:testDebugUnitTest -x lintDebug`
- **Per wave merge:** `./gradlew :app:testDebugUnitTest :app:assembleDebug` (and `:app:assembleRelease` from Wave 1 onward)
- **Before `/gsd-verify-work`:** Full suite green AND clean release APK ≤40MB AND manual device acceptance per 07-HANDOFF.md
- **Max feedback latency:** 150 seconds

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Threat Ref | Test Type | Automated Command | Status |
|---------|------|------|-------------|------------|-----------|-------------------|--------|
| 07-01-NN | 01 (Wave 0 scaffolds + JankStats catalog add) | 0 | PRF-01, PRF-02, PRF-04, D-20a..c | — | unit scaffolds RED | `./gradlew :app:testDebugUnitTest` | ⬜ |
| 07-02-NN | 02 (Release build config + WebP sprite conversion + ABI split) | 1 | PRF-04 | T-07-02 (R8 strips D-32 fix) | unit + release build | `./gradlew :app:assembleRelease :app:testDebugUnitTest --tests "*WebPSpriteCompatTest* *SpriteManifestPathTest*"` + APK Analyzer | ⬜ |
| 07-03-NN | 03 (JankStats + DetectionLatencyRecorder + PerfReporter + FaceDetectorClient timing) | 2 | PRF-01, PRF-02 | T-07-01 (perf log in release) | unit | `./gradlew :app:testDebugUnitTest --tests "*PerfReporterTest* *DetectionLatencyRecorderTest* *MainActivityJankStatsTest* *FaceDetectorClientTest*"` | ⬜ |
| 07-04-NN | 04 (Phase 6 polish trio: D-20a bbox gate + D-20b ContentObserver + D-20c LeakCanary disable) | 2 | (carry-fwd from D-20) | T-07-03 (ContentObserver leak) | unit | `./gradlew :app:testDebugUnitTest --tests "*DebugOverlayRendererTest* *CollectionRepositoryContentObserverTest*"` | ⬜ |
| 07-05-NN | 05 (07-PERF-REPORT.md + Xiaomi 13T baseline measurement) | 3 | PRF-01, PRF-02, PRF-03 | — | manual (device) | 07-PERF-REPORT.md sign-off (median fps + p95 latency + ffprobe audio drift) | ⬜ |
| 07-06-NN | 06 (07-HANDOFF runbook + 8 deferred UAT folded per D-21) | 4 | PRF-05, MOD-02, VID-04, MOD-06, MOD-07 | — | manual (device) | 07-HANDOFF.md sign-off | ⬜ |
| 07-07-NN | 07 (Release APK + 07-CHECKPOINT + nyquist flip + close-out) | 5 | All PRF-01..05 + D-24 | T-07-02 | manual (device) | 07-CHECKPOINT.md sign-off (≤40MB + cross-OEM PASS) | ⬜ |

### Per-Requirement Test Specification

| Req ID | Behavior | Test Type | Command | File |
|--------|----------|-----------|---------|------|
| PRF-01 live | Live preview ≥24 fps under filter playback | manual (device) | JankStats Logcat aggregation over 30s | ❌ runbook |
| PRF-01 wire | JankStats wires into MainActivity lifecycle | unit (Robolectric ActivityScenario) | `*MainActivityJankStatsTest*` | ❌ W0 |
| PRF-01 agg | PerfReporter aggregates pXX from FrameData samples | unit (pure JVM) | `*PerfReporterTest*` | ❌ W0 |
| PRF-02 dev | Face detection latency ≤100ms median | manual (device) | Logcat tag `Perf` aggregation | ❌ runbook |
| PRF-02 agg | DetectionLatencyRecorder ring buffer aggregates median/p95/p99 | unit (pure JVM) | `*DetectionLatencyRecorderTest*` | ❌ W0 |
| PRF-02 debug | Timing log emits only in debug builds | unit (BuildConfig flag manipulation) | `*FaceDetectorClientTest*perfTimingLog_emitsInDebugOnly*` | ❌ W0 (extend) |
| PRF-03 drift | 60s recorded MP4 audio drift <50ms | manual (device + dev-PC ffprobe) | `bash scripts/verify-audio-sync.sh /tmp/test.mp4` | ❌ runbook |
| PRF-04 size | Release APK ≤40 MB | manual (APK Analyzer) | `du -h app/build/outputs/apk/release/*.apk` | ❌ runbook |
| PRF-04 webp | Sprite WebP conversion preserves frame count + alpha | unit (Bitmap.Config assertion) | `*WebPSpriteCompatTest*` | ❌ W0 |
| PRF-04 manifest | manifest.json references .webp paths post-conversion | unit (JSON parse) | `*SpriteManifestPathTest*` | ❌ W0 |
| PRF-04 D32 | 9 D-32 grep-asserts survive R8 minification | manual (release APK decompile + grep) | runbook step | ❌ runbook |
| PRF-05 install | App installs cleanly on secondary OEM | manual (device) | runbook step | ❌ runbook |
| PRF-05 work | App captures + records on secondary OEM | manual (device) | runbook step | ❌ runbook |
| D-20a | DebugOverlayRenderer.draw gated by BuildConfig.DEBUG | unit (Robolectric BuildConfig manipulation) | `*DebugOverlayRendererTest*draw_skips_in_release*` | ❌ W0 (extend) |
| D-20b | CollectionRepository registers/unregisters ContentObserver + triggers re-query | unit (Robolectric ShadowContentResolver + Turbine) | `*CollectionRepositoryContentObserverTest*` | ❌ W0 |
| D-20c | Debug manifest disables LeakLauncherActivity (debug-only overlay) | manual (merged manifest parse post-assembleDebug) | runbook step | ❌ runbook |

---

## Wave 0 Requirements

5 new test files:
- [ ] `app/src/test/java/com/bugzz/filter/camera/perf/PerfReporterTest.kt` (PRF-01 agg)
- [ ] `app/src/test/java/com/bugzz/filter/camera/perf/DetectionLatencyRecorderTest.kt` (PRF-02 agg)
- [ ] `app/src/test/java/com/bugzz/filter/camera/data/CollectionRepositoryContentObserverTest.kt` (D-20b)
- [ ] `app/src/test/java/com/bugzz/filter/camera/assets/SpriteManifestPathTest.kt` (PRF-04 manifest)
- [ ] `app/src/test/java/com/bugzz/filter/camera/assets/WebPSpriteCompatTest.kt` (PRF-04 webp)
- [ ] `app/src/test/java/com/bugzz/filter/camera/ui/MainActivityJankStatsTest.kt` (PRF-01 wire — Robolectric ActivityScenario)

Plus 2 EXTENSIONS:
- [ ] **EXTEND** `app/src/test/java/com/bugzz/filter/camera/detector/FaceDetectorClientTest.kt` with `perfTimingLog_emitsInDebugOnly` test (PRF-02 debug gating)
- [ ] **EXTEND** `app/src/test/java/com/bugzz/filter/camera/render/DebugOverlayRendererTest.kt` with `draw_skips_in_release` test (D-20a verification)

**New deps:** `androidx.metrics:metrics-performance 1.0.0` (corrected from CONTEXT D-01's `1.0.0-beta02` — stable 1.0.0 shipped 2025-10-08 per RESEARCH).

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| Live preview ≥24 fps | PRF-01 | Real Canvas + OverlayEffect render with face detection | 07-HANDOFF: 30s Face Filter session; JankStats Logcat tag `Perf`; median + p95 fps |
| Face detection latency ≤100ms | PRF-02 | Real ML Kit inference on device hardware | 07-HANDOFF: 30s session; `adb logcat -d Perf:V *:S \| grep detect=` then awk median |
| 60s video audio drift <50ms | PRF-03 | Real CameraX Recorder + microphone | 07-HANDOFF: record 60s; pull MP4; `ffprobe -show_streams`; compute audio.start_time - video.start_time |
| Release APK ≤40 MB | PRF-04 | Production R8 minification + WebP sprites | 07-HANDOFF: `./gradlew :app:assembleRelease`; APK Analyzer `du -h` |
| 9 D-32 grep-asserts survive R8 | PRF-04 / D-24 | R8 may strip via dead-code elimination | 07-HANDOFF: decompile release APK + grep 9 patterns |
| Cross-OEM install + work | PRF-05 | Real Samsung/Pixel hardware (HyperOS differs from stock Android) | 07-HANDOFF: secondary device install + capture photo + record video + share |
| Pre-warmed thermal stress | D-19 | Real device thermal sensors | 07-HANDOFF: 5×60s warmup + 1 measurement session; ThermalMonitor Logcat |
| Reference APK comparison | D-15 | INSTALL_FAILED_MISSING_SPLIT requires multi-APK install | 07-HANDOFF: `install-multiple` retry; skip+document if persists |
| LeakCanary launcher disabled | D-20c | Manifest merger output verification | 07-HANDOFF: debug build; check merged manifest; verify monkey selects MainActivity |
| 8 Phase 4+5 deferred UAT items | various | Carry over from prior phases per D-21 | 07-HANDOFF includes: multi-face 2-person, fps subjective 30s, pinch/rotate visual, sticker survival flip+orientation, audio sync subjective, fresh-install RECORD_AUDIO, ThermalMonitor stress, 05-gaps-02 sticker drag-axis polish |

---

## Validation Sign-Off

- [ ] All tasks have `<automated>` verify or Wave 0 dependencies
- [ ] Sampling continuity preserved (Phase 6 baseline + new tests)
- [ ] Wave 0 covers all 5 new + 2 EXTEND test files
- [ ] No watch-mode flags
- [ ] Feedback latency < 150s
- [ ] `nyquist_compliant: true` flipped post-PASS via Plan 07-07 Task 3

**Approval:** PASS — 2026-05-13 via 07-CHECKPOINT.md (status PARTIAL — 8/9 hard PASS on Xiaomi 13T + 3 documented gaps DEFERRED per personal-use scope per D-13: PRF-05 secondary OEM, D-19 pre-warmed thermal, D-15 reference APK)
