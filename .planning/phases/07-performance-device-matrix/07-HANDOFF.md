# Phase 7 Handoff — Performance + Cross-OEM Device Matrix Runbook

**Primary device:** Xiaomi 13T (2306EPN60G, HyperOS / MIUI, Android 15) — D-12 primary OEM
**Secondary device (best-effort):** Samsung A-series OR Pixel A-series — user-sourced per D-13; OR Firebase Test Lab opportunistic per D-14
**Debug APK:** `app/build/outputs/apk/debug/app-debug.apk` — required for PRF-01 (JankStats) + PRF-02 (`Perf detect=` log) measurement; BuildConfig.DEBUG must be `true` for those logs to emit (Phase 07-03 Decision #48)
**Release APK:** `app/build/outputs/apk/release/app-release.apk` — required for PRF-04 size + R8 survival verification (~20 MB at Plan 07-04 close)
**Phase 6 baseline (reference):** 91.96 MB debug APK / 172 unit tests / 13 hard gates GREEN
**Phase 7 delta:** debug APK +JankStats + DetectionLatencyRecorder + ContentObserver + LeakCanary disable; release APK = ~20 MB (R8 + WebP + arm64-v8a + LeakCanary stripped)

> Vietnamese / Tiếng Việt: Đây là runbook verify Phase 7 trên Xiaomi 13T + ít nhất 1 OEM khác. **Hard gates** PRF-01..05 → fail block sign-off (phải tạo `07-gaps-0N-PLAN.md`). **Phase-7-only verification** D-19 / D-20c / D-15 → fail log + inline-fix nếu trivial. **Soft gates** (Phase 4+5 carry-over per D-21) → log + defer là OK, không block sign-off.

---

## Pre-flight (user-side)

1. Phase 4/5/6 prerequisites still met:
   - Settings → Additional settings → Developer options → USB debugging: ON
   - "Install via USB": ON (MIUI-specific)
   - "USB debugging (Security settings)": ON (MIUI-specific)
2. USB cable connected between phone and this PC (data-capable, not charge-only).
3. When prompted on phone, tap "Allow" for the RSA fingerprint dialog.
4. `adb` at `C:/Users/Admin/AppData/Local/Android/Sdk/platform-tools/adb.exe` (Phase 1–6 validated path).
5. ~250 MB free storage on device (20 MB release APK or ~92 MB debug APK + photo/video artifacts).
6. Phone NOT in battery-saver / power-saving mode (JankStats + ThermalMonitor + ExoPlayer playback should run free).
7. At least one share target installed (Zalo / Messenger / WhatsApp / Gmail) — required for end-to-end SHR-01..04 regression (Phase 6 carry).
8. **Both debug + release APKs built** — Plan 07-04 Wave 2 close already verified both compile clean (commit `8607c2d`). Re-build if local checkout is stale:

```bash
"D:/ClaudeProject/appmobile/Bugzz/gradlew.bat" assembleDebug assembleRelease
ls -la app/build/outputs/apk/debug/app-debug.apk
ls -la app/build/outputs/apk/release/app-release.apk
```

Tiếng Việt: Cần cả debug + release APK. **Debug** để đo fps + latency (JankStats + Perf log chỉ emit khi `BuildConfig.DEBUG=true`); **release** để verify size + R8 + boot smoke.

9. **`ffprobe` available on dev PC** (PRF-03 audio sync verification):

```bash
ffprobe -version
```

If not installed → install via `winget install --id=Gyan.FFmpeg` or `scoop install ffmpeg`. Without `ffprobe`, `./scripts/verify-audio-sync.sh` (Plan 07-05 Task 1) cannot run.

10. **`apkanalyzer` available** (PRF-04 D-24 R8 survival verification):

```bash
ls "C:/Users/Admin/AppData/Local/Android/Sdk/build-tools/" | tail -1
# Then: <latest>/apkanalyzer.bat dex packages app/build/outputs/apk/release/app-release.apk | head
```

11. **Pre-clean DataStore + previous artifacts** (forces first-launch + empty-state flow + fresh RECORD_AUDIO prompt for soft gate 11f):

```bash
"C:/Users/Admin/AppData/Local/Android/Sdk/platform-tools/adb.exe" shell pm clear com.bugzz.filter.camera
"C:/Users/Admin/AppData/Local/Android/Sdk/platform-tools/adb.exe" shell rm -rf /sdcard/DCIM/Bugzz
```

> Lưu ý: `pm clear` **xoá DataStore** → `onboarding_completed=false` → lần khởi động kế tiếp chạy onboarding. `rm -rf DCIM/Bugzz` xoá artifact cũ → cần thiết cho EmptyState + fresh permission flow.

12. Keep ADB USB connection alive:

```bash
"C:/Users/Admin/AppData/Local/Android/Sdk/platform-tools/adb.exe" shell svc power stayon usb
```

13. Pre-grant permissions for ADB-driven flow (Phase 6 precedent — avoids blocking dialogs during measurement runs):

```bash
"C:/Users/Admin/AppData/Local/Android/Sdk/platform-tools/adb.exe" shell pm grant com.bugzz.filter.camera android.permission.CAMERA
"C:/Users/Admin/AppData/Local/Android/Sdk/platform-tools/adb.exe" shell pm grant com.bugzz.filter.camera android.permission.RECORD_AUDIO
"C:/Users/Admin/AppData/Local/Android/Sdk/platform-tools/adb.exe" shell pm grant com.bugzz.filter.camera android.permission.POST_NOTIFICATIONS
```

Tiếng Việt: Pre-grant permissions để ADB flow không bị block bởi dialog. Soft gate 11f (fresh-install RECORD_AUDIO dialog) cần test riêng — sẽ `pm clear` lại trước step đó.

14. `bc` available in shell — if not, the audio drift math falls back to `awk` (Plan 07-05 Task 1 script already swapped — see comment header in `scripts/verify-audio-sync.sh`).

---

## What you are verifying

### Hard gates (block sign-off on FAIL)

| # | Requirement | Method | Acceptance |
|---|-------------|--------|------------|
| 1 | **PRF-04** Release APK ≤ 40 MB | `du -h app/build/outputs/apk/release/app-release.apk` | ≤ 40 MB |
| 2 | **PRF-04 / D-24** 9 D-32 grep-asserts survive R8 minification | `apkanalyzer dex packages` + `grep` for 9 patterns | All 9 class/method names un-obfuscated in release dex |
| 3 | **PRF-01** Live preview ≥ 24 fps median | 30s Face Filter session + JankStats Logcat (debug APK) | Median frame duration ≤ 41 ms (≥ 24 fps) |
| 4 | **PRF-02** Face detection ≤ 100 ms median | 30s session + Timber `Perf detect=…` log (debug APK) | Median ≤ 100 ms AND p95 ≤ 150 ms |
| 5 | **PRF-03** 60s video audio drift < 50 ms | Record 60s + `./scripts/verify-audio-sync.sh` | `|start_drift| < 0.050s` AND `|dur_drift| < 0.050s` AND 1795 ≤ frames ≤ 1805 |
| 6 | **PRF-05** Secondary OEM works | Install + smoke test on second device OR Firebase Test Lab | Capture + record both succeed; OR documented N/A per D-13 |

### Phase 7-only verification (log + inline-fix on FAIL if trivial)

| # | Item | Method | Source |
|---|------|--------|--------|
| 7 | **D-19** Pre-warmed thermal stress | 5×60s warmup + 1 measurement session; ThermalMonitor logcat | CONTEXT D-19 |
| 8 | **D-20c** LeakCanary `LeakLauncherActivity` disabled in debug | `monkey -c LAUNCHER` selects MainActivity (NOT LeakLauncherActivity) | Plan 07-04; 06-CHECKPOINT Notes |
| 9 | **D-15** Reference APK comparison (best-effort) | `adb install` / `install-multiple` reference APK | CONTEXT D-15 |

### Soft gates (Phase 4+5 deferred UAT per D-21 — verbatim from 06-HANDOFF.md Step 11)

| # | Phase | Item | Source |
|---|-------|------|--------|
| 1 | 4 | Multi-face 2-person scene primary/secondary fallback | MOD-02 |
| 2 | 4 | Subjective fps over 30s capture session | PRF-01 manual sanity |
| 3 | 5 | Pinch + rotate gestures on InsectFilter sticker | MOD-06 |
| 4 | 5 | Sticker survives camera flip + portrait-locked orientation | MOD-07 |
| 5 | 5 | Audio sync subjective lip-sync | PRF-03 sanity beyond ffprobe |
| 6 | 5 | Fresh-install RECORD_AUDIO permission dialog | VID-04 |
| 7 | 5 | ThermalMonitor 60s+ extended stress | D-19 / PRF-01 sustained |
| 8 | 5 | 05-gaps-02 sticker drag-axis direction polish | Phase 5 visual carry |

**Hard gates** (Steps 1–8): failure blocks Phase 7 sign-off — fix inline if trivial; spawn `07-gaps-0N-PLAN.md` if non-trivial.
**Phase 7-only** (Steps 5–7): document outcome; defer to inline-fix or gap plan per severity.
**Soft gates** (Step 11): log result; Phase 7 may still close with user sign-off.

Tiếng Việt: Hard gate fail → block sign-off. Phase-7-only fail → fix inline nếu trivial. Soft gate fail → log + defer là OK.

---

## Known expected findings (NOT bugs — flag only if different)

- **Release APK ~20 MB** (Phase 07-04 baseline 20 MB; Phase 5 debug was 84 MB; Phase 6 debug 91.96 MB → release with R8 + WebP + arm64-v8a + LeakCanary stripped lands in the ~25-35 MB envelope per RESEARCH §PRF-04 prediction; actual 20 MB is excellent).
- **JankStats first-60-frames jank is normal** cold-start (RESEARCH Pitfall 9). Aggregation in Step 3 below skips the first 60 frames.
- **Per-frame `Perf detect=Xms frame=N landmarks=L` only emits in debug APK** (Phase 07-03 Decision #50 — T-07-01 IDS mitigation). Release APK `strings | grep "detect="` returns 0 — verified empirically at Plan 07-03 commit `494e22d`.
- **Per-frame `JankStats jank dur=Xms`** also debug-only (Phase 07-03 compileOnly + debugImplementation pattern — Decision #48).
- **On Xiaomi HyperOS pre-Plan-07-04:** `monkey -c LAUNCHER` chose `leakcanary.internal.activity.LeakLauncherActivity` (06-CHECKPOINT Notes). **Post-Plan-07-04 should choose MainActivity** (D-20c verification step 6 below).
- **WebP sprites render identically to PNG** (RESEARCH Pattern 5 / Plan 07-02 commit `47a6e54` Decision pattern — `AssetLoader.path generalization + SpriteManifest.frameExtension`).
- **First-launch onboarding appears only on fresh data** (DataStore-backed `onboarding_completed`). Pre-flight #11 `pm clear` re-arms it.
- **Phase 4-6 features all carry forward** (Face Filter, Insect Filter, Preview, Collection, Share, Settings, Splash, Onboarding) — no Phase 7 regressions expected to user-facing flows.
- **Debug overlays** (red bbox + orange contour dots from `DebugOverlayRenderer`) still visible BEHIND filter overlay in FaceFilter mode in **debug APK**. **Release APK** must NOT bake them in — verified by Plan 07-04 D-20a structural test + production grep (`DebugOverlayRenderer.draw` body line 1 = `if (!BuildConfig.DEBUG) return`).
- **Collection grid auto-refresh on artifact delete** — Plan 07-04 D-20b wired `ContentObserver` to `CollectionRepository`. Delete from PreviewScreen → return to Collection → grid shows N-1 items immediately (no back+re-enter needed). 06-CHECKPOINT Observations bullet 4 was the pre-fix state.
- **MIUI/HyperOS install blocker** "Install unknown apps" — expected first-time; enable for install source and retry.
- **Reference APK `INSTALL_FAILED_MISSING_SPLIT`** may persist (D-15 best-effort) — documented defer is acceptable, not a blocker.

---

## Step 0 — Verify APK + device connectivity

```bash
ls -la app/build/outputs/apk/debug/app-debug.apk
ls -la app/build/outputs/apk/release/app-release.apk
"C:/Users/Admin/AppData/Local/Android/Sdk/platform-tools/adb.exe" devices
ffprobe -version | head -1
```

Expected:
- Debug APK file listed, size ~90-100 MB.
- Release APK file listed, size ~20 MB (well under 40 MB cap).
- Device output: `<xiaomi-13t-serial>    device` (not `unauthorized` or `offline`).
- ffprobe version line shown (e.g. `ffprobe version 7.x`).

If `unauthorized`: tap "Allow" on RSA fingerprint dialog on phone, re-run.
If `offline`: unplug/replug USB; toggle USB debugging OFF then ON.
If ffprobe missing: install per Pre-flight #9.

Install release APK + start logcat capture (release APK is the **default** test target — switch to debug for measurement Steps 3 + 4 explicitly):

```bash
"C:/Users/Admin/AppData/Local/Android/Sdk/platform-tools/adb.exe" install -r app/build/outputs/apk/release/app-release.apk
"C:/Users/Admin/AppData/Local/Android/Sdk/platform-tools/adb.exe" logcat -c
"C:/Users/Admin/AppData/Local/Android/Sdk/platform-tools/adb.exe" logcat > phase7-run.log &
```

Tiếng Việt: Install release APK trước (test Step 1 + 2 + 5 + 6 + 7 + 8). Switch sang debug APK ở Step 3 + 4 để đo fps + latency.

PASS / FAIL: ________

---

## Step 1 — PRF-04 Release APK size (HARD GATE)

**Pre-condition:** Step 0 release APK built.

```bash
ls -la app/build/outputs/apk/release/app-release.apk
du -h app/build/outputs/apk/release/app-release.apk
```

Expected: ≤ 40 MB (current baseline ~20 MB per Plan 07-04).

If > 40 MB: audit recent dep additions (e.g. spurious media3 / Lottie inclusions). Try `apkanalyzer apk summary app-release.apk` to see breakdown. Inline-fix if trivial; spawn `07-gaps-NN-PLAN.md` if non-trivial.

Tiếng Việt: APK release phải ≤ 40 MB. Hiện tại ~20 MB là rất tốt.

PASS / FAIL: ________

---

## Step 2 — PRF-04 / D-24 9 D-32 grep-asserts survive R8 (HARD GATE)

The 9 D-32 grep-asserts ARE the Phase 3+4+5 inline fix call sites. R8 must NOT obfuscate or strip them in the release dex.

```bash
APKANALYZER="C:/Users/Admin/AppData/Local/Android/Sdk/build-tools/35.0.0/apkanalyzer.bat"
APK="app/build/outputs/apk/release/app-release.apk"

# Dump dex class+method names
"$APKANALYZER" dex packages "$APK" > /tmp/release-dex-packages.txt

# 9 D-32 grep-asserts (relaxed forms per CONTEXT D-24)
echo "--- 1. isCapturing ---"
grep -i "isCapturing" /tmp/release-dex-packages.txt | head -3

echo "--- 2. bindJob ---"
grep -i "bindJob" /tmp/release-dex-packages.txt | head -3

echo "--- 3. OneShotEvent.FilterLoadError ---"
grep -i "FilterLoadError" /tmp/release-dex-packages.txt | head -3

echo "--- 4. captureFlash ---"
grep -i "captureFlash" /tmp/release-dex-packages.txt | head -3

echo "--- 5. FilterEngine (require frameCount > 0 lives inside) ---"
grep -i "FilterEngine" /tmp/release-dex-packages.txt | head -3

echo "--- 6. AssetLoader (preload(assetDir)) ---"
grep -i "AssetLoader.*preload\|AssetLoader" /tmp/release-dex-packages.txt | head -3

echo "--- 7. isRecording ---"
grep -i "isRecording" /tmp/release-dex-packages.txt | head -3

echo "--- 8. CameraMode (FQN cameraMode = com.bugzz...CameraMode.InsectFilter) ---"
grep -i "CameraMode" /tmp/release-dex-packages.txt | head -3

echo "--- 9. DebugOverlayRenderer / setMatrix call site ---"
grep -i "DebugOverlayRenderer\|setMatrix" /tmp/release-dex-packages.txt | head -3
```

Expected: each of the 9 patterns returns at least 1 line with the class or method name un-obfuscated.

**If any pattern returns 0 lines:** R8 stripped or obfuscated the symbol. Inline-fix: add narrow `-keep class com.bugzz.filter.camera.<Pkg>.<Class>` rule to `app/proguard-rules.pro` (per Phase 07-02 Plan 07-02 Wave 1 commit `47a6e54` precedent), rebuild release, re-verify.

Tiếng Việt: 9 grep-asserts từ Phase 3+4+5 phải survive R8 — nếu fail, add `-keep` rule narrow trong `proguard-rules.pro` và rebuild.

PASS / FAIL: ________

---

## Step 3 — PRF-01 + PRF-02 measurement (debug APK)

**This step MUST run against debug APK** — JankStats + `Perf detect=` log gated behind `BuildConfig.DEBUG` (Plan 07-03 Decision #48 / 07-PERF-REPORT Test Environment rationale).

### 3a — Switch to debug APK + grant permissions

```bash
"C:/Users/Admin/AppData/Local/Android/Sdk/platform-tools/adb.exe" install -r app/build/outputs/apk/debug/app-debug.apk
"C:/Users/Admin/AppData/Local/Android/Sdk/platform-tools/adb.exe" shell pm grant com.bugzz.filter.camera android.permission.CAMERA
"C:/Users/Admin/AppData/Local/Android/Sdk/platform-tools/adb.exe" shell pm grant com.bugzz.filter.camera android.permission.RECORD_AUDIO
"C:/Users/Admin/AppData/Local/Android/Sdk/platform-tools/adb.exe" shell pm grant com.bugzz.filter.camera android.permission.POST_NOTIFICATIONS
```

### 3b — 30-second Face Filter session

Start clean logcat + launch app:

```bash
"C:/Users/Admin/AppData/Local/Android/Sdk/platform-tools/adb.exe" logcat -c
"C:/Users/Admin/AppData/Local/Android/Sdk/platform-tools/adb.exe" logcat -s Perf:V ThermalMonitor:V > /tmp/07-perf-session-1.log &
LOGPID=$!

"C:/Users/Admin/AppData/Local/Android/Sdk/platform-tools/adb.exe" shell am start -n com.bugzz.filter.camera/.MainActivity
# User: Splash → (Skip Onboarding) → Home → Face Filter → 30s with FACE IN FRAME
# Select "Spider Nose" filter or similar landmark-anchored filter
# Hold steady 30 seconds (count out loud)

kill $LOGPID
```

Tiếng Việt: Đo trên **debug APK** (Perf log + JankStats chỉ emit khi `BuildConfig.DEBUG=true`). Hold face trong frame 30s với 1 filter Face landmark như Spider Nose.

### 3c — PRF-01 fps aggregation (median ≥ 24 fps required)

```bash
grep "jank dur=" /tmp/07-perf-session-1.log \
  | awk -F"dur=" '{print $2}' | awk '{print $1}' | sed 's/ms//' \
  | sort -n > /tmp/frame-durations.txt

# Skip first 60 cold-start frames (RESEARCH Pitfall 9)
tail -n +61 /tmp/frame-durations.txt > /tmp/frame-durations-warm.txt

count=$(wc -l < /tmp/frame-durations-warm.txt)
echo "Samples (warm): $count"
echo "Median frame duration:"
awk -v c="$count" 'NR == int((c+1)/2)' /tmp/frame-durations-warm.txt
echo "p95:"
awk -v c="$count" 'NR == int(c * 95 / 100)' /tmp/frame-durations-warm.txt
echo "p99:"
awk -v c="$count" 'NR == int(c * 99 / 100)' /tmp/frame-durations-warm.txt
```

PRF-01 PASS criterion: **median frame duration ≤ 41 ms** (≥ 24 fps). Record to `07-PERF-REPORT.md` PRF-01 result table.

### 3d — PRF-02 detection latency aggregation (median ≤ 100 ms, p95 ≤ 150 ms required)

```bash
grep "detect=" /tmp/07-perf-session-1.log \
  | awk -F"detect=" '{print $2}' | awk '{print $1}' | sed 's/ms//' \
  | sort -n > /tmp/detect-ms.txt

count=$(wc -l < /tmp/detect-ms.txt)
echo "Samples: $count  (need ≥ 800 — D-04)"
echo "Median:"
awk -v c="$count" 'NR == int((c+1)/2)' /tmp/detect-ms.txt
echo "p95:"
awk -v c="$count" 'NR == int(c * 95 / 100)' /tmp/detect-ms.txt
echo "p99:"
awk -v c="$count" 'NR == int(c * 99 / 100)' /tmp/detect-ms.txt
```

PRF-02 PASS criterion: **median ≤ 100 ms AND p95 ≤ 150 ms**. Record to `07-PERF-REPORT.md` PRF-02 result table.

**If PRF-01 fails (median fps < 24):** D-17 escalation triggered → spawn `07-gaps-01-PLAN.md` for GL CameraEffect implementation per CLAUDE.md "Fallback Plan". Plan 07-07 paused until gap plan closes.

Tiếng Việt: PRF-01 fail (< 24 fps) → trigger GL CameraEffect escalation (D-17). PRF-02 fail → audit ML Kit options + thermal state.

PASS / FAIL (PRF-01): ________
PASS / FAIL (PRF-02): ________

---

## Step 4 — PRF-03 60s video audio drift (HARD GATE)

**Uses debug OR release APK** (both have audio recording — verify on release for production correctness).

### 4a — Record 60-second video

```bash
"C:/Users/Admin/AppData/Local/Android/Sdk/platform-tools/adb.exe" install -r app/build/outputs/apk/release/app-release.apk
# Or stay on debug from Step 3 — both produce equivalent MP4 (CameraX VideoCapture path)

"C:/Users/Admin/AppData/Local/Android/Sdk/platform-tools/adb.exe" shell am start -n com.bugzz.filter.camera/.MainActivity
# User: Splash → Home → Insect Filter (or Face Filter) → tap RED Record button
# Wait for 60s auto-stop (D-04) — record sound (clap, count) for audio sync subjective sanity (soft gate 11e)
# After auto-stop, verify file saved:
"C:/Users/Admin/AppData/Local/Android/Sdk/platform-tools/adb.exe" shell ls -la /sdcard/DCIM/Bugzz/*.mp4 | tail -1
```

### 4b — Pull MP4 + run audio sync script

```bash
LATEST_MP4=$("C:/Users/Admin/AppData/Local/Android/Sdk/platform-tools/adb.exe" shell ls -t /sdcard/DCIM/Bugzz/*.mp4 | head -1 | tr -d '\r')
"C:/Users/Admin/AppData/Local/Android/Sdk/platform-tools/adb.exe" pull "$LATEST_MP4" /tmp/prf03-test.mp4
./scripts/verify-audio-sync.sh /tmp/prf03-test.mp4
```

Expected output (Plan 07-05 Task 1 script):

```
PRF-03: audio sync verification
  audio_start: 0.000000s
  video_start: 0.000000s
  start_drift: 0.000000s  (must be |drift| < 0.050)
  audio_dur:   60.045s
  video_dur:   60.000s
  dur_drift:   0.045s  (must be |drift| < 0.050)
  frame count: 1798 (must be 1795–1805)
PASS — PRF-03 audio sync within tolerance
```

**Script exit 0 = PASS; exit 1 = FAIL.**

Tiếng Việt: Record 60s rồi pull MP4 chạy ffprobe để check audio drift. Script exit 0 = PASS.

If Plan 07-05 Task 2 already locked PRF-03 numbers in `07-PERF-REPORT.md`, **reference that result instead of re-running** (no need to re-measure the same metric on the same device).

PASS / FAIL: ________

---

## Step 5 — D-19 Pre-warmed thermal stress

**Methodology:** 5 consecutive 60s recordings (5 min warmup), then 1 measurement recording. Phone gets warm; verifies `ThermalMonitor.shouldSkipFrame` actually triggers at `THERMAL_STATUS_MODERATE+`.

### 5a — Warmup (5 × 60s recordings)

```bash
"C:/Users/Admin/AppData/Local/Android/Sdk/platform-tools/adb.exe" install -r app/build/outputs/apk/debug/app-debug.apk
"C:/Users/Admin/AppData/Local/Android/Sdk/platform-tools/adb.exe" shell am start -n com.bugzz.filter.camera/.MainActivity

# User: navigate to Insect Filter (or Face Filter)
# Tap RED Record → wait 60s auto-stop → tap Done/Retake → repeat 5 times
# Phone should feel warm to touch after session 4-5
```

### 5b — Measurement session

```bash
"C:/Users/Admin/AppData/Local/Android/Sdk/platform-tools/adb.exe" logcat -c
"C:/Users/Admin/AppData/Local/Android/Sdk/platform-tools/adb.exe" logcat -s Perf:V ThermalMonitor:V > /tmp/07-thermal-session.log &
LOGPID=$!

# User: tap RED Record again on Insect Filter → wait 60s auto-stop

kill $LOGPID

grep -i "thermal=" /tmp/07-thermal-session.log | tail -20
grep -E "MODERATE|SEVERE|frame_skip|drawSkipped" /tmp/07-thermal-session.log | head -10

# Median fps during stress:
grep "jank dur=" /tmp/07-thermal-session.log \
  | awk -F"dur=" '{print $2}' | awk '{print $1}' | sed 's/ms//' \
  | sort -n > /tmp/thermal-frame-durations.txt
count=$(wc -l < /tmp/thermal-frame-durations.txt)
echo "Median frame duration during stress:"
awk -v c="$count" 'NR == int((c+1)/2)' /tmp/thermal-frame-durations.txt
```

### 5c — Acceptance

Document observed `THERMAL_STATUS` reached (NONE / LIGHT / MODERATE / SEVERE) + median fps during stress session.

- **PASS** if fps ≥ 20 even at `THERMAL_STATUS_MODERATE+` (D-19).
- **N/A** if 5×60s warmup did not reach `THERMAL_STATUS_LIGHT+` (thermal protection path didn't trigger — neutral outcome; not a fail).

Tiếng Việt: 5 × 60s record để phone nóng lên, sau đó measure 60s thứ 6. Phải đạt ≥ 20 fps khi MODERATE+. Nếu không lên MODERATE → N/A (không phải fail).

PASS / FAIL / N/A: ________

---

## Step 6 — D-20c LeakCanary LAUNCHER disabled on-device

**Pre-condition:** debug APK installed (release APK doesn't ship LeakCanary at all per Plan 07-02 R8 + LeakCanary release-strip; this step verifies the **debug** manifest overlay from Plan 07-04 commit `8607c2d`).

```bash
"C:/Users/Admin/AppData/Local/Android/Sdk/platform-tools/adb.exe" install -r app/build/outputs/apk/debug/app-debug.apk

# Inspect LAUNCHER intent filters
"C:/Users/Admin/AppData/Local/Android/Sdk/platform-tools/adb.exe" shell pm dump com.bugzz.filter.camera | grep -A 5 "android.intent.category.LAUNCHER"
```

Expected: only **`com.bugzz.filter.camera.MainActivity`** appears — NOT `leakcanary.internal.activity.LeakLauncherActivity`.

### 6a — Monkey LAUNCHER probe

```bash
"C:/Users/Admin/AppData/Local/Android/Sdk/platform-tools/adb.exe" shell am force-stop com.bugzz.filter.camera
"C:/Users/Admin/AppData/Local/Android/Sdk/platform-tools/adb.exe" shell monkey -p com.bugzz.filter.camera -c android.intent.category.LAUNCHER 1
"C:/Users/Admin/AppData/Local/Android/Sdk/platform-tools/adb.exe" logcat -d | grep -E "Activity|monkey" | tail -10

# What is currently focused?
"C:/Users/Admin/AppData/Local/Android/Sdk/platform-tools/adb.exe" shell dumpsys window | grep "mCurrentFocus"
```

Expected: focused window = `com.bugzz.filter.camera/.MainActivity` (NOT `leakcanary.internal.activity.LeakLauncherActivity`).

Pre-Plan-07-04 on Xiaomi HyperOS: monkey selected LeakCanary (06-CHECKPOINT Observations bullet 1). Post-Plan-07-04: must select MainActivity. **This step verifies the `<activity-alias android:name="leakcanary.internal.activity.LeakLauncherActivity" tools:node="remove"/>` overlay in `app/src/debug/AndroidManifest.xml` actually merged into the debug APK.**

Tiếng Việt: Verify `monkey -c LAUNCHER` chọn MainActivity, KHÔNG phải LeakLauncherActivity. Plan 07-04 đã fix; step này verify on-device.

PASS / FAIL: ________

---

## Step 7 — D-15 Reference APK comparison (best-effort)

```bash
cd reference
ls *.apk

# Try direct install
"C:/Users/Admin/AppData/Local/Android/Sdk/platform-tools/adb.exe" install -r com.insect.filters.funny.prank.bug.filter.face.camera.apk 2>&1

# If that fails with INSTALL_FAILED_MISSING_SPLIT:
"C:/Users/Admin/AppData/Local/Android/Sdk/platform-tools/adb.exe" install-multiple -r \
  com.insect.filters.funny.prank.bug.filter.face.camera*.apk 2>&1
```

**If `INSTALL_FAILED_MISSING_SPLIT` persists:** reference APK is a split bundle from Play Store. Document as **DEFERRED per D-15** — not a blocker.

**If install succeeds:** quick subjective comparison — launch reference + Bugzz side-by-side and verify:
- Filter render quality (subjective)
- Subjective fps comparison
- Visual fidelity of bug sprites

Tiếng Việt: Try install reference APK; nếu `INSTALL_FAILED_MISSING_SPLIT` → DEFERRED, không phải blocker.

PASS / DEFERRED: ________

---

## Step 8 — PRF-05 Secondary OEM smoke test (HARD GATE)

Three strategies per D-13 / D-14 in priority order.

### Strategy (a) — User has Samsung A-series or Pixel A-series

```bash
"C:/Users/Admin/AppData/Local/Android/Sdk/platform-tools/adb.exe" devices  # verify second device connected
"C:/Users/Admin/AppData/Local/Android/Sdk/platform-tools/adb.exe" -s <SAMSUNG_OR_PIXEL_SERIAL> install -r app/build/outputs/apk/release/app-release.apk
"C:/Users/Admin/AppData/Local/Android/Sdk/platform-tools/adb.exe" -s <SAMSUNG_OR_PIXEL_SERIAL> shell pm grant com.bugzz.filter.camera android.permission.CAMERA
"C:/Users/Admin/AppData/Local/Android/Sdk/platform-tools/adb.exe" -s <SAMSUNG_OR_PIXEL_SERIAL> shell pm grant com.bugzz.filter.camera android.permission.RECORD_AUDIO
"C:/Users/Admin/AppData/Local/Android/Sdk/platform-tools/adb.exe" -s <SAMSUNG_OR_PIXEL_SERIAL> shell am start -n com.bugzz.filter.camera/.MainActivity
```

Minimum smoke per secondary OEM:
- [ ] Install + boot to Home (Splash + Onboarding flow OK)
- [ ] Face Filter live preview renders (face bounding box + sprites visible)
- [ ] Capture photo → PreviewScreen opens with photo
- [ ] Record 5s video → PreviewScreen opens with ExoPlayer playback
- [ ] Pull MP4 to dev PC → opens cleanly in Windows Media Player

### Strategy (b) — Firebase Test Lab opportunistic (D-14)

**Free tier in 2026 = 30 minutes physical device test time per day** (RESEARCH §Alternatives correction — NOT "5 runs/day" from CONTEXT D-14).

```bash
gcloud auth login
gcloud config set project <YOUR_PROJECT_ID>

# Upload release APK + run a Robo test against a Pixel
gcloud firebase test android run \
  --type robo \
  --app app/build/outputs/apk/release/app-release.apk \
  --device model=oriole,version=33,locale=en,orientation=portrait \
  --timeout 90s
```

One Robo test ≈ 3-5 min device time. Could squeeze 4-6 runs/day within the 30 min/day quota.

Expected: Test Lab UI shows the app launched, navigated through Splash → Home, no crashes. Save the deep link result URL to `07-PERF-REPORT.md` Reference APK section.

### Strategy (c) — Neither device available (D-13 conditional PASS)

If user has only Xiaomi 13T + no Firebase Test Lab opt-in: document **N/A** in sign-off table with reason:

> "Secondary OEM unavailable — Xiaomi 13T validated as primary OEM. PRF-05 conditional PASS per D-13. Future: source Samsung A-series or Pixel A-series before v1 Play Store push."

Tiếng Việt: Nếu chỉ có Xiaomi 13T → document N/A có lý do; **không block phase** per D-13 conditional clause.

PASS / N/A: ________

---

## Step 11 — Phase 4+5 deferred UAT (8 soft gates per D-21)

These are **NOT Phase 7 hard requirements**. Log results to `07-CHECKPOINT.md` Notes column (Plan 07-07 authors). Phase 7 sign-off does not depend on these.

Tiếng Việt: 8 items này từ Phase 4+5 — log result + defer là OK, không block sign-off.

### 11a — (Phase 4 carry) Multi-face 2-person scene

- HomeScreen → Face Filter → bring a second face into frame (printed photo ~8x8 cm or second person)
- [ ] No crash with 2 faces
- [ ] Primary face (larger bbox) gets full FaceLandmarkMapper-anchored sprites
- [ ] Secondary face gets bbox-center bug (MOD-02 fallback path)
- Result: ✅ / ❌ / N/A — Notes: ________

### 11b — (Phase 4 carry) FPS subjective smoothness over 30s

- In Face Filter, hold the camera on a face + select Swarm filter (e.g. "Bug B Swarm")
- [ ] Preview feels smooth for 30s — no visible stutter
- (PRF-01 in Step 3 measures this objectively; soft gate 11b is subjective sanity)
- Result: ✅ / ❌ — Notes: ________

### 11c — (Phase 5 carry) Pinch + rotate gestures on InsectFilter sticker

- HomeScreen → Insect Filter → spawn sticker → two-finger pinch (zoom) + two-finger twist (rotate)
- [ ] Sticker scales smoothly within `[0.3x..3.0x]` clamp
- [ ] Rotation is smooth + unbounded
- Result: ✅ / ❌ — Notes: ________

### 11d — (Phase 5 carry) Sticker survives camera flip + screen orientation

- In Insect Filter, position sticker at top-left + scale ~1.5x + rotate ~45°
- Tap Flip button (front ↔ back)
- [ ] Sticker preserved at same screen position/scale/rotation
- App is portrait-locked → orientation change inapplicable → **N/A** for orientation, but flip is testable.
- Result: ✅ / ❌ / N/A — Notes: ________

### 11e — (Phase 5 carry) Audio sync subjective on video playback

- Capture a 5–10s video with audible speech/sound (record sound during Step 4 60s session — re-use that MP4)
- Play in Preview ExoPlayer (or pull MP4 + play in Google Photos)
- [ ] No audible 1s+ lead/lag between lip movement and sound (PRF-03 in Step 4 measures this objectively)
- Result: ✅ / ❌ — Notes: ________

### 11f — (Phase 5 carry) Fresh-install RECORD_AUDIO permission dialog

- After `pm clear` + reinstall (do NOT pre-grant RECORD_AUDIO this time):

```bash
"C:/Users/Admin/AppData/Local/Android/Sdk/platform-tools/adb.exe" shell pm clear com.bugzz.filter.camera
"C:/Users/Admin/AppData/Local/Android/Sdk/platform-tools/adb.exe" install -r app/build/outputs/apk/debug/app-debug.apk
"C:/Users/Admin/AppData/Local/Android/Sdk/platform-tools/adb.exe" shell pm grant com.bugzz.filter.camera android.permission.CAMERA
# Note: deliberately NOT granting RECORD_AUDIO this time
"C:/Users/Admin/AppData/Local/Android/Sdk/platform-tools/adb.exe" shell am start -n com.bugzz.filter.camera/.MainActivity
# User: Splash → Onboarding → Skip → Home → Face Filter → tap RED Record
```

- [ ] System "Allow Bugzz to record audio?" dialog appears immediately on first record tap
- Result: ✅ / ❌ — Notes: ________

### 11g — (Phase 5 carry) ThermalMonitor extended recording stress

- (Already covered by Step 5 D-19 pre-warmed thermal — re-use that finding here)
- [ ] `ThermalMonitor` log lines present (listener registered)
- [ ] Document observed thermal status (NONE/LIGHT/MODERATE/SEVERE) from Step 5
- Result: NONE / LIGHT / MODERATE / SEVERE — Notes: ________

### 11h — (Phase 5 gap-02 visual polish) Sticker drag axis matches finger

- In Insect Filter, drag sticker around the screen
- [ ] Drag direction visually matches finger movement (no axis swap, no inversion)
- This was Phase 5 gap-02 fix `de27c4e` — should be solid.
- Result: ✅ / ❌ — Notes: ________

PASS / FAIL (Step 11 — soft count): ________ (target: ≥ 5 of 8 PASS or N/A)

---

## Logcat filter cheatsheet

Phase 7 additions on top of Phase 1-6 tags:

```bash
"C:/Users/Admin/AppData/Local/Android/Sdk/platform-tools/adb.exe" logcat -s \
  Perf:V ThermalMonitor:V \
  FaceTracker:V OverlayEffect:V \
  CameraVM:I InsectFilterVM:I PreviewVM:I CollectionVM:I OnboardingVM:I SplashVM:I \
  ShareIntentBuilder:V CollectionRepository:V FilterPrefsRepository:V \
  VideoRecorder:V \
  *:S
```

For Phase 7 perf-specific:

```bash
# fps frame durations (JankStats)
grep "jank dur=" phase7-run.log | tail -20

# face detect latency
grep "detect=" phase7-run.log | tail -20

# thermal events
grep -i "thermal\|MODERATE\|SEVERE" phase7-run.log | tail -20
```

For crash diagnosis:

```bash
grep -E "FATAL|AndroidRuntime|Exception" phase7-run.log | tail -30
```

For LeakCanary disable verification (Step 6):

```bash
grep -i "LeakLauncher\|MainActivity\|monkey" phase7-run.log | tail -10
```

For R8 keep-rule investigation (if Step 2 fails):

```bash
grep -E "ClassNotFoundException|NoSuchMethodError|NoSuchFieldError" phase7-run.log | tail -30
```

---

## OEM Quirks

Document anything unexpected here. Do NOT fix in this checkpoint — record for `07-CHECKPOINT.md` Notes.

### Xiaomi 13T (HyperOS / MIUI, Android 15) — primary

-
-

### Samsung A-series (Android ??) — secondary (if available)

-
-

### Pixel A-series (Android ??) — secondary (if available; via device OR Firebase Test Lab)

-
-

---

## Final sign-off table

| Step | Requirement | Outcome | Notes |
|------|-------------|---------|-------|
| 0 | APK + device connectivity (debug + release + ffprobe) | PASS / FAIL | |
| 1 | PRF-04 release APK ≤ 40 MB | PASS / FAIL | Actual size: ___ MB (Phase 07-04 baseline 20 MB) |
| 2 | PRF-04 / D-24 9 D-32 grep-asserts survive R8 | PASS / FAIL | Patterns failing: ___ |
| 3 | PRF-01 median fps ≥ 24 + PRF-02 median detect ≤ 100 ms | PASS / FAIL | Median fps: ___ / Median detect: ___ ms / p95 detect: ___ ms |
| 4 | PRF-03 60s video audio drift < 50 ms + frame count 1795–1805 | PASS / FAIL | start_drift: ___ s / dur_drift: ___ s / frames: ___ |
| 5 | D-19 pre-warmed thermal stress fps ≥ 20 at MODERATE+ | PASS / FAIL / N/A | Status reached: ___ / median fps during stress: ___ |
| 6 | D-20c LeakCanary LAUNCHER disabled in debug | PASS / FAIL | Focused activity: ___ |
| 7 | D-15 reference APK comparison (best-effort) | PASS / DEFERRED | Install outcome: ___ |
| 8 | PRF-05 secondary OEM smoke (Samsung/Pixel OR FTL OR N/A) | PASS / N/A | Device/strategy: ___ |
| 11 | Phase 4+5 deferred UAT (8 soft items per D-21) | SOFT (✅/❌/N/A per item) | ___/8 PASS, ___/8 ❌, ___/8 N/A |

**Hard gate summary:**
- Hard gates: Steps 1–4 + Step 8 (PRF-01..05) — 5 items, all must PASS
- Phase 7-only: Steps 5–7 (D-19 / D-20c / D-15) — log + inline-fix on FAIL if trivial
- All hard gates PASS: **Yes / No**

---

## Sign-off

When all hard gates (Steps 1–4 + Step 8) PASS:

1. Reply to Plan 07-07 executor: `PASS — proceed to Phase 7 close-out` with:
   - Hard gate result count (e.g., "5/5 hard gates PASS" or "4/5 — Step 8 N/A per D-13 conditional")
   - Phase-7-only result line (Steps 5–7)
   - Soft-gate result table (8 items, per Step 11)
   - Any OEM quirk notes (per "OEM Quirks" section)

2. Plan 07-07 Task 3 executor will then:
   a. Update `07-PERF-REPORT.md` with all measured values + GL escalation decision row (Deferred per D-18 if PRF-01 PASS; Triggered per D-17 if PRF-01 FAIL)
   b. Flip `07-VALIDATION.md` frontmatter `nyquist_compliant: false → true` and `status: draft → complete`
   c. Add approval line: `**Approval:** PASS — YYYY-MM-DD via 07-CHECKPOINT.md`
   d. Author `07-CHECKPOINT.md` with per-gate result table + soft-gate notes + device evidence file list
   e. Author `07-07-SUMMARY.md` covering Plan 07-07 + Phase 7 close-out
   f. Update `ROADMAP.md` — Phase 7 row [x]
   g. Run `gsd-tools roadmap update-plan-progress 07`
   h. Update `STATE.md`: Current Position = Phase 7 complete → milestone v1 ready
   i. Update `REQUIREMENTS.md` — mark PRF-01..05 complete (PRF-01 + PRF-02 already marked by Plan 07-03; PRF-03 + PRF-04 + PRF-05 marked here)
   j. Final commit `docs(phase-07): complete phase execution — N/M hard gates verified + N/M soft gates`

---

## Phase 7 Gap-Closure Path

If any **hard-gate** (Steps 1–4 + Step 8) FAILS:

1. **Trivial issues** (R8 missing single `-keep` rule, manifest typo, off-by-one frame count): fix inline before sign-off + amend the relevant SUMMARY (per Phase 5 gaps-01 / Phase 7 Plan 07-02 commit `47a6e54` inline-fix precedent — fix in same commit thread, no new plan needed).

2. **Non-trivial issues** (PRF-01 < 24 fps → GL CameraEffect escalation per D-17, T-07-01 IDS string leak in release APK, ContentObserver regression, persistent reference APK comparison need): create `07-gaps-0N-PLAN.md` per Phase 2/3/4/5 gap-closure precedent.
   - Phase 7 sign-off blocked until gap plan executes + passes re-verification.
   - Plan 07-07 Task 3 (close-out) paused; user can decide whether to bundle multiple gaps into one plan.

3. **Environmental N/A** (e.g., no secondary OEM available + no Firebase Test Lab opt-in): document as N/A with reason; PRF-05 conditional PASS per D-13. Phase 7 may still close.

If any **Phase-7-only** (Steps 5–7) FAILS:
- Trivial → inline-fix + amend SUMMARY
- Non-trivial → spawn `07-gaps-NN-PLAN.md`
- Environmental (D-19 N/A, D-15 INSTALL_FAILED_MISSING_SPLIT) → document; not a blocker

If any **soft-gate** (Step 11) FAILS:
- Document finding in sign-off table Notes column.
- Phase 7 may still close if user agrees to defer to v2 / future polish.

If ALL hard gates PASS + user signs off → Phase 7 fully signed off. Plan 07-07 Task 3 (close-out) runs next. **Milestone v1 → ready for final tag.**

---

*Phase 7 handoff runbook — 9 step sections (Steps 0–8 + Step 11 soft summary) covering PRF-01..05 (5 hard gates) + D-19/D-20c/D-15 (3 Phase-7-only) + 8 deferred UAT carries from Phase 4+5 per D-21.*
*Follows `06-HANDOFF.md` format (Phase 6 handoff structure is known-good).*
*Phase 7 acceptance gate: all hard gates PASS on Xiaomi 13T + (secondary OEM PASS OR D-13 conditional N/A documented). Phase-7-only (Steps 5–7) document outcomes. Soft gates (Step 11) log + defer is acceptable.*
*Release APK: `app/build/outputs/apk/release/app-release.apk` (~20 MB at Plan 07-04 close; 9 D-32 grep-asserts intact per Plan 07-04 SUMMARY verification).*
*Debug APK: `app/build/outputs/apk/debug/app-debug.apk` (required for PRF-01 + PRF-02 measurement — JankStats + `Perf detect=` log gated behind `BuildConfig.DEBUG`).*
*9/9 D-32 grep-asserts verified post-Plan-07-04: 4/1/3/6/1/2/9/1/2 — Phase 07-04 SUMMARY.*
