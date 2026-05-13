---
phase: 07
report: perf-baseline
status: draft
device: Xiaomi 13T (2306EPN60G) Android 15 HyperOS
apk: app/build/outputs/apk/debug/app-debug.apk
release_apk: app/build/outputs/apk/release/app-release.apk
measurement_date: TBD
created: 2026-05-13
---

# Phase 7 Performance Report — Baseline (Xiaomi 13T)

> Per D-18: documents Phase 7 measurement evidence + GL CameraEffect escalation decision.
> Scaffold landed by Plan 07-05 Task 1. Real numbers filled by Plan 07-05 Task 2 (device CHECKPOINT — user runs measurement session).

## Test Environment

- **Device:** Xiaomi 13T (2306EPN60G), Snapdragon 8s Gen 2-class (test stand-in for 2019 Snapdragon 675 mid-tier per ROADMAP Phase 7 §1)
- **OS:** Android 15 HyperOS
- **APK under test:** **debug** build — `app/build/outputs/apk/debug/app-debug.apk`
  - Rationale (per RESEARCH §Anti-pattern "Measuring on connected ADB with cold start = false" + Decision #48):
    - JankStats + Timber.tag("Perf") log emission is gated behind `BuildConfig.DEBUG`.
    - The release APK has `BuildConfig.DEBUG = false` → those measurement logs do NOT emit.
    - Therefore PRF-01 + PRF-02 measurements use the **debug** APK.
    - The **release** APK is verified separately for size + boot smoke (see "Release APK Verification" section below).
- **Pre-conditions:** cold boot, screen brightness 50%, airplane mode ON (reduces network jitter), USB stay-on (`adb shell svc power stayon usb`).
- **Phase context:** Plans 07-02 (WebP + R8 + arm64-v8a + LeakCanary stripped from release), 07-03 (JankStats + DetectionLatencyRecorder + FaceDetectorClient Perf log), 07-04 (CollectionRepository ContentObserver + DebugOverlayRenderer release-gate + LeakCanary LAUNCHER hijack disabled).

## Methodology Overview

| Metric | Tooling | Source-of-truth | Reference |
|--------|---------|-----------------|-----------|
| PRF-01 (live fps) | JankStats + frame-duration aggregation from logcat | `adb logcat -d Perf:V *:S \| grep "jank dur="` | CONTEXT D-01 + D-02; RESEARCH Pattern 1 |
| PRF-02 (detection latency) | FaceDetectorClient `Timber.tag("Perf").d("detect=…ms …")` | `adb logcat -d Perf:V *:S \| grep "detect="` | CONTEXT D-04 + D-05; Plan 07-03 wire-in |
| PRF-03 (audio drift) | `ffprobe` per-stream start_time + duration via `scripts/verify-audio-sync.sh` | Local MP4 pulled via `adb pull` | CONTEXT D-10 + D-11; RESEARCH §Code Examples |
| D-19 (thermal stress) | Pre-warm (5 × 60s recordings) + ThermalMonitor logcat | `adb logcat -d Perf:V ThermalMonitor:V *:S` | CONTEXT D-19; Phase 5 ThermalMonitor |
| GL escalation (D-16/17/18) | Decision rubric on PRF-01 PRF-02 outcomes | This report | CONTEXT D-16-18 + CLAUDE.md "Fallback Plan" |

## OS-registered State Check (RESEARCH §Runtime State Inventory)

```bash
"C:/Users/Admin/AppData/Local/Android/Sdk/build-tools/<latest>/aapt.exe" dump xmltree \
  app/build/outputs/apk/release/app-release.apk AndroidManifest.xml | grep "MainActivity"
```

- Expected: `MainActivity` class name visible un-obfuscated. R8 keep rule from `proguard-android-optimize.txt` should preserve the FQN.
- If obfuscated → R8 keep rule missing for activity classes; inline-fix needed.

Result: **TBD — to be filled by executor in Task 2.**

## PRF-01 — Live Preview Frame Rate

### Methodology (D-01 + D-02)

- JankStats wire-in via `MainActivity.onResume/onPause` (Plan 07-03, RESEARCH Pattern 1 verbatim).
- 30-second Face Filter session with the **Spider** filter (anchored to forehead via face-landmark stick).
- `adb logcat -d Perf:V *:S | grep "jank dur="` aggregated offline.
- **First 60 frames skipped** during aggregation (RESEARCH Pitfall 9 — cold-start `setContent` jank distorts median).

### Aggregation Procedure

```bash
adb logcat -c
adb logcat -s Perf:V ThermalMonitor:V > /tmp/07-perf-session-1.log &
LOGPID=$!

# Launch + navigate to Face Filter on device
adb shell am start -n com.bugzz.filter.camera/.MainActivity
# User: Splash → Onboarding (skip) → Home → Face Filter → 30 seconds with face in view

kill $LOGPID

# Median frame duration → median fps
grep "jank dur=" /tmp/07-perf-session-1.log \
  | awk -F"dur=" '{print $2}' | awk '{print $1}' | sed 's/ms//' \
  | sort -n > /tmp/frame-durations.txt
# Skip first 60 cold-start frames:
tail -n +61 /tmp/frame-durations.txt > /tmp/frame-durations-warm.txt

count=$(wc -l < /tmp/frame-durations-warm.txt)
awk -v c="$count" 'NR == int((c+1)/2)' /tmp/frame-durations-warm.txt   # median ms
awk -v c="$count" 'NR == int(c * 95 / 100)' /tmp/frame-durations-warm.txt # p95 ms
# fps = 1000 / median_ms
```

### Result

| Metric | Value |
|--------|-------|
| Total frames sampled (after 60-frame cold-start skip) | TBD |
| Median frame duration | TBD ms |
| **Median fps** | **TBD** |
| p95 frame duration | TBD ms |
| p99 frame duration | TBD ms |
| Jank rate (frames > 16.67ms) | TBD % |

### Acceptance (PRF-01 / D-17)

- PASS if **median fps ≥ 24**.
- → **{PASS / FAIL — to be filled by executor}**

## PRF-02 — Face Detection Latency

### Methodology (D-04 + D-05)

- `FaceDetectorClient.createAnalyzer`'s MlKitAnalyzer lambda wraps `result.getValue(detector)` with `System.nanoTime()` delta + `Timber.tag("Perf").d("detect=%dms ...")` (Plan 07-03).
- 30-second Face Filter session (same session as PRF-01 — reuses `/tmp/07-perf-session-1.log`).
- `adb logcat -d Perf:V *:S | grep "detect="` aggregated offline.
- ≥1000 samples per RESEARCH CONTEXT D-04 (30 fps × 30s ≈ 900 frames; tolerate ≥800 if device throttles).

### Aggregation Procedure

```bash
grep "detect=" /tmp/07-perf-session-1.log \
  | awk -F"detect=" '{print $2}' | awk '{print $1}' | sed 's/ms//' \
  | sort -n > /tmp/detect-ms.txt

count=$(wc -l < /tmp/detect-ms.txt)
awk -v c="$count" 'NR == int((c+1)/2)' /tmp/detect-ms.txt          # median
awk -v c="$count" 'NR == int(c * 95 / 100)' /tmp/detect-ms.txt     # p95
awk -v c="$count" 'NR == int(c * 99 / 100)' /tmp/detect-ms.txt     # p99
```

### Result

| Metric | Value |
|--------|-------|
| Total samples | TBD |
| Median detect latency | TBD ms |
| p95 detect latency | TBD ms |
| p99 detect latency | TBD ms |

### Acceptance (PRF-02 / D-05)

- PASS if **median ≤ 100 ms AND p95 ≤ 150 ms**.
- → **{PASS / FAIL — to be filled by executor}**

## PRF-03 — Audio Sync Drift

### Methodology (D-10 + D-11)

- Record a 60-second video on Xiaomi 13T via **Insect Filter** mode (audio enabled — Phase 5 D-04 auto-stop fires at 60s).
- Pull the resulting MP4 to dev PC via `adb pull`.
- Run `./scripts/verify-audio-sync.sh /tmp/prf03-test.mp4`.

### Tooling

- `scripts/verify-audio-sync.sh` (this plan, Task 1):
  - Extracts audio + video `start_time` + `duration` via `ffprobe`.
  - Computes `start_drift = audio_start - video_start` and `dur_drift = audio_dur - video_dur` via `awk` (bc replaced — Rule 3 auto-fix on Windows git-bash).
  - Counts video frames via `ffprobe -count_frames`.
  - Exit 0 on PASS (both drifts under 50ms), exit 1 on FAIL.

### Result

| Metric | Value | Acceptance |
|--------|-------|------------|
| MP4 path | TBD | — |
| audio_start | TBD s | — |
| video_start | TBD s | — |
| **start_drift** | TBD s | \|drift\| < 0.050 |
| audio_dur | TBD s | — |
| video_dur | TBD s | — |
| **dur_drift** | TBD s | \|drift\| < 0.050 |
| video frame count | TBD | 1795 ≤ frames ≤ 1805 (60s @ 30fps) |

### Acceptance (PRF-03 / D-10/D-11)

- PASS if **|start_drift| < 0.050 AND |dur_drift| < 0.050 AND 1795 ≤ frames ≤ 1805**.
- → **{PASS / FAIL — to be filled by executor}**

## D-19 — Pre-warmed Thermal Stress

### Methodology

- 5 consecutive 60-second video recordings (5 min warmup; phone gets hot).
- Then 1 measurement recording while ThermalMonitor logs filtered.
- Captures `adb logcat -d Perf:V ThermalMonitor:V *:S` during the 6th recording.

### Aggregation Procedure

```bash
adb logcat -c
adb logcat -s Perf:V ThermalMonitor:V > /tmp/07-thermal-session.log &
# tap Record on phone; wait 60s
kill $!

grep "thermal=" /tmp/07-thermal-session.log | tail -20
grep "MODERATE\|SEVERE" /tmp/07-thermal-session.log | head -5
grep "jank dur=" /tmp/07-thermal-session.log \
  | awk -F"dur=" '{print $2}' | awk '{print $1}' | sed 's/ms//' \
  | sort -n > /tmp/thermal-frame-durations.txt
```

### Result

| Metric | Value |
|--------|-------|
| Thermal status reached | TBD (NONE / LIGHT / MODERATE / SEVERE) |
| Frame-skip events (Phase 5 D-14 ThermalMonitor.shouldSkipFrame) | TBD |
| Median fps during stress session | TBD |

### Acceptance (D-19)

- PASS if **fps ≥ 20** even at `THERMAL_STATUS_MODERATE+`.
- N/A if 5×60s warmup didn't reach `THERMAL_STATUS_LIGHT+` (neutral outcome — thermal protection path simply did not trigger).
- → **{PASS / FAIL / N/A — to be filled by executor}**

## GL CameraEffect Escalation Decision (D-16 + D-17 + D-18)

### Decision Rubric

Per CLAUDE.md "Fallback Plan" + D-17 trigger criterion:

> Escalation trigger: median fps < 24 on Xiaomi 13T OR average frame time > 33 ms over 10-second recording.

| PRF-01 outcome | Decision |
|----------------|----------|
| median fps ≥ 24 | **Escalation deferred** per D-18. Document rationale: Canvas-based `OverlayEffect` (Phase 2 D-01) achieves ≥24 fps median on Xiaomi 13T; the 3-5 day GL CameraEffect work is correctly identified as YAGNI per D-16. Future trigger: cross-OEM device matrix surfaces a sub-24fps device. |
| median fps < 24 | **Escalation triggered** per D-17. Executor spawns `07-gaps-01-PLAN.md` per Phase 2/3/4/5 gap-closure precedent. Plan content: 3-5 day GL CameraEffect implementation per CLAUDE.md "Fallback Plan" wording. Plan 07-05 halts; Plan 07-06 paused until gap plan closes. Document specific failing metric + device + APK SHA in the gap plan's frontmatter. |

### Decision: **{Deferred / Triggered — to be filled by executor}**

### Rationale (filled post-measurement)

TBD — quote the empirical PRF-01 median fps + cite the rubric row that applies.

## Reference APK Comparison (D-15)

### Methodology

```bash
cd reference
ls *.apk
"C:/Users/Admin/AppData/Local/Android/Sdk/platform-tools/adb.exe" install-multiple \
  com.insect.filters.funny.prank.bug.filter.face.camera.apk 2>&1
```

- Best-effort comparison. If `INSTALL_FAILED_MISSING_SPLIT` persists (reference APK is a split bundle from Play Store), document as **DEFERRED per D-15** — not a blocker.

### Result

| Aspect | Value |
|--------|-------|
| Install attempt outcome | TBD |
| Status | **{PASS — installed + compared / DEFERRED — INSTALL_FAILED_MISSING_SPLIT persists}** |
| Filter render quality (subjective) | TBD |
| Subjective fps comparison | TBD |

## Release APK Verification

(Separate from PRF-01/02 which use the debug build per "Test Environment" rationale above.)

| Check | Expected | Actual |
|-------|----------|--------|
| Release APK size | ≤ 40 MB (D-32 cap) | TBD (Phase 07-04 baseline: ~20 MB) |
| Boot to Splash without crash | Yes | TBD |
| `strings app-release.apk \| grep "detect=" \| wc -l` | 0 (T-07-01 IDS) | TBD |
| `strings app-release.apk \| grep "jank dur=" \| wc -l` | 0 (T-07-01 IDS) | TBD |
| `strings app-release.apk \| grep "JankStats" \| wc -l` | 0 (T-07-01 IDS) | TBD |
| `strings app-release.apk \| grep "DetectionLatencyRecorder" \| wc -l` | 0 (T-07-01 IDS) | TBD |
| `strings app-release.apk \| grep "metrics/performance" \| wc -l` | 0 (T-07-01 IDS) | TBD |

## Sign-off Checklist

- [ ] PRF-01 measured (median fps + p95 + jank rate) — PASS/FAIL recorded
- [ ] PRF-02 measured (median + p95 + p99 detect latency) — PASS/FAIL recorded
- [ ] PRF-03 measured (start_drift + dur_drift + frame count) — PASS/FAIL recorded
- [ ] D-19 thermal stress attempted — PASS/FAIL/N/A recorded
- [ ] GL escalation decision recorded (Deferred or Triggered + gap plan spawned)
- [ ] Reference APK install attempted — outcome recorded (PASS / DEFERRED)
- [ ] Release APK size + IDS string-grep verified
- [ ] OS-registered state (`MainActivity` un-obfuscated) verified

**Status:** TBD — executor fills after Plan 07-05 Task 2 device session.
