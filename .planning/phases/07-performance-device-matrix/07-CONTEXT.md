# Phase 7: Performance & Device Matrix - Context

**Gathered:** 2026-05-13
**Status:** Ready for planning
**Mode:** Auto-locked recommended defaults per user delegation pattern ("Tiếp phase 7 --chain")

<domain>
## Phase Boundary

Verify and harden the Core Value (smooth live AR preview) under **measurement** on real hardware across ≥2 OEMs.

**In scope (PRF-01..05):**
- Measure fps, face detection latency, audio sync, frame drops
- Shrink APK from current 88 MB (debug) → ≤40 MB (release)
- Cross-OEM matrix: Xiaomi 13T (have) + Samsung + Pixel
- GL `CameraEffect` escalation IF AND ONLY IF Canvas profiles below target
- Close 3 Phase 6 deferred polish items + 8 Phase 4+5 deferred UAT items per D-33 / Phase 6 CHECKPOINT carry-forward
- Release build configuration: R8 + minify + WebP sprites + per-ABI split

**Out of scope (defer to v2 / future milestones):**
- POL-01..06 polish features (countdown, flash, watermark, music, multi-face)
- Real Play Store URLs / privacy policy URLs (UX-09 stub stays stub)
- Trending feeds, ads, billing, i18n (PROJECT.md milestone definition)
- Cloud device farms beyond opportunistic use (Firebase Test Lab free tier OK; no BrowserStack purchase)
</domain>

<decisions>
## Implementation Decisions

All decisions auto-locked per user autonomy delegation. Rationale recorded inline so downstream agents (researcher + planner + executor) see WHY.

### Benchmark Methodology (PRF-01, PRF-02)

- **D-01:** Use **JankStats** (`androidx.metrics:metrics-performance 1.0.0-beta02`) as in-app frame timing observer (debug builds). Pipe `FrameData` to Timber Logcat tag `Perf` for offline analysis. Rationale: zero-config, runs alongside production code, no Macrobenchmark CI overhead.
- **D-02:** Use **Android Studio Profiler** (CPU / Memory / Energy) traces for deep-dive measurement runs on Xiaomi 13T. Capture profiler traces during 30s filter playback + 60s video record + pre-warmed thermal stress. Rationale: gold-standard objective measurement; user can run on-demand on physical device.
- **D-03:** Do NOT integrate Jetpack Macrobenchmark library. Rationale: Macrobenchmark adds Gradle module + CI setup overhead; solo dev with single device + opportunistic 2nd OEM doesn't benefit from CI-style automation. JankStats + manual profiler covers the measurement need.

### Face Detection Latency (PRF-02)

- **D-04:** Measure ML Kit face detection latency via existing `FaceDetectorClient` instrumentation. Add timing log: `Timber.tag("Perf").d("detect=%dms frame=%d landmarks=%d", elapsed, frameIdx, landmarkCount)` around `FaceDetector.process()` call (debug only). Aggregate ≥1000 samples for stats. Rationale: in-pipeline measurement is more accurate than profiler sampling.
- **D-05:** Acceptance: median latency ≤100ms/frame on Xiaomi 13T baseline. p95 ≤150ms acceptable. p99 noted but not blocking. Rationale: matches PRF-02 success criterion with statistical realism (single frame outliers fine if median holds).

### APK Size Reduction (PRF-04)

- **D-06:** Enable full release build configuration: `minifyEnabled true` + `shrinkResources true` + `isDebuggable false`. Wire R8 with default ProGuard rules; add Compose-specific keep rules from official `proguard-android-optimize.txt`. Rationale: R8 is the standard Android shrinker; full release config gates LeakCanary out automatically (which adds ~4MB).
- **D-07:** Convert sprite PNGs in `app/src/main/assets/sprites/` to WebP (lossless quality ≥90). Rationale: typical 30-50% size reduction with no visual quality loss; CLAUDE.md already recommends this as size optimization.
- **D-08:** Add ABI splits configuration: `abiFilters "arm64-v8a"` only (drop x86_64 and armeabi-v7a). Rationale: minSdk 28 = Android 9+ which is ≥98% 64-bit ARM on Vietnam market; armeabi-v7a still produced separate APK = wasteful for personal use.
- **D-09:** Acceptance: release APK ≤40 MB total. If debug APK at 88 MB → release with R8 + WebP + arm64-v8a-only should land ~25-35 MB (LeakCanary stripped + R8 ~30-40% reduction + WebP sprite reduction).

### Audio Sync Verification (PRF-03)

- **D-10:** Verify audio drift via `ffprobe -show_streams -show_format` on a 60-second test video record. Compute `audio.start_time - video.start_time` + verify `audio.duration ≈ video.duration` within 50ms tolerance. Rationale: objective + reproducible; matches success criterion #4 with no specialized hardware needed.
- **D-11:** Verify zero frame drops via `ffprobe -count_frames -show_entries stream=nb_read_frames`. Expected: 60s × 30fps = 1800 frames ± 5 acceptable. Rationale: covers PRF-03's "drops zero frames" criterion deterministically.

### Cross-OEM Device Matrix (PRF-05)

- **D-12:** Primary verification device: **Xiaomi 13T** (already validated through Phase 6). Continue ADB-driven runbook here.
- **D-13:** Secondary device(s): User to source Samsung A-series OR Pixel A-series for Phase 7 sign-off. Best-effort 2nd OEM. If only 1 additional OEM available, sign-off conditional with documented gap (matches "minimum 2-OEM matrix" — Xiaomi + 1 other).
- **D-14:** Cloud device fallback: Firebase Test Lab free tier (5 free physical device runs/day) optional for opportunistic Pixel coverage if user cannot source one. Rationale: free, no setup beyond Google account; matches "personal use" budget.
- **D-15:** Reference APK comparison: install reference `com.insect.filters.funny.prank.bug.filter.face.camera` on Xiaomi 13T (deferred from earlier phases per memory — `INSTALL_FAILED_MISSING_SPLIT` was the blocker). Try with `-r --split-from-source` first; if still fails, document as deferred + skip reference comparison (matches Phase 3 precedent).

### GL CameraEffect Escalation (Success Criterion #5)

- **D-16:** **Measure first, escalate only if needed.** Do NOT pre-implement GL CameraEffect scaffolding. Rationale: YAGNI — Canvas-based OverlayEffect has worked through Phases 2–6 on Xiaomi 13T; pre-building 3-5 days of GL infrastructure that may never run is wasted effort. CLAUDE.md explicitly lists "Fallback Plan" as future upgrade.
- **D-17:** Escalation trigger: median fps <24 on either Xiaomi 13T or secondary OEM device during 30s filter playback. Documented as PHASE-CHECKPOINT-FAIL with empirical numbers. Then file `07-gaps-NN-PLAN.md` for GL escalation. Rationale: data-driven decision.
- **D-18:** Documentation requirement: if Canvas passes, write `07-PERF-REPORT.md` documenting escalation deferral with rationale + measurement evidence (the Phase 7 verifier closes by referencing this doc). Rationale: matches success criterion #5 wording "documented as deferred with rationale".

### Thermal Mitigation Verification (existing Phase 5)

- **D-19:** Pre-warmed stress test: record 60s video AFTER 5-minute warmup recording (or 5 consecutive 60s sessions). Verify Logcat shows `Timber.tag("Perf")` thermal-throttle events firing when device reaches `THERMAL_STATUS_MODERATE+`, and visible fps stays ≥20fps via JankStats. Rationale: Phase 5 ThermalMonitor already implemented; Phase 7 just verifies it works under stress.

### Phase 6 + Phase 4+5 Deferred UAT Folding (CHECKPOINT carry-forward)

- **D-20:** Fold the 3 LOW-severity polish items from 06-CHECKPOINT.md into Phase 7 scope:
  - **(a) Bbox + landmark debug viz** — Gate `DebugOverlayRenderer.drawDebugViz()` behind `BuildConfig.DEBUG`. Production captures should NOT have red rectangle + landmark dots baked in.
  - **(b) Collection grid stale-entry** — Add `ContentResolver.registerContentObserver` to `CollectionRepository` so deletion triggers live refresh without back+re-enter.
  - **(c) LeakCanary LAUNCHER hijack** — In debug-only AndroidManifest, disable `leakcanary.internal.activity.LeakLauncherActivity` to prevent monkey selecting it on Xiaomi HyperOS.
  Rationale: All 3 are pre-release polish that matter before milestone close — landing them in Phase 7 keeps v1 clean without adding a separate phase.
- **D-21:** Fold the 8 Phase 4+5 deferred UAT items from Phase 6 06-HANDOFF.md (per D-33) into the Phase 7 device matrix runbook. These ARE the cross-OEM validation criteria in practice:
  1. Multi-face 2-person scene (Phase 4 MOD-02 fallback path verification)
  2. fps subjective over 30s capture session (PRF-01 manual sanity)
  3. Pinch + rotate gestures on InsectFilter sticker (Phase 5 MOD-06)
  4. Sticker survival across camera flip + portrait-locked orientation (Phase 5 MOD-07)
  5. Audio sync subjective lip-sync test (PRF-03 sanity)
  6. Fresh-install RECORD_AUDIO permission dialog flow (Phase 5 VID-04)
  7. ThermalMonitor 60s+ extended stress (PRF-01 sustained)
  8. 05-gaps-02 sticker drag-axis direction polish (Phase 5 visual)

### Release Build Quality Bar

- **D-22:** Release build must produce signed APK (debug keystore OK for personal use; document as such). Rationale: PRF-04 measures release APK size, not debug APK.
- **D-23:** Verify release APK installs cleanly on Xiaomi 13T + secondary OEM. Rationale: matches PRF-05 "verified working end-to-end on at least one Samsung AND one Pixel" (with Xiaomi standing in as one OEM + Samsung/Pixel as second).
- **D-24:** All Phase 6 D-32 grep-asserts (9 patterns: `isCapturing`, `bindJob?.cancel()`, `OneShotEvent.FilterLoadError`, `captureFlash`, `require(frameCount > 0)`, `assetLoader.preload(def.assetDir)`, `isRecording`, FQN `cameraMode = com.bugzz...CameraMode.InsectFilter`, `canvas.setMatrix(Matrix())`) must continue to pass post-release-config + post-R8. Rationale: Phase 3+4+5 inline fix preservation — R8 must not strip the fix call sites.

### Plan Budget & Wave Layout

- **D-25:** Estimated plan budget: **5-7 plans / 4-5 waves**. Concerns split by file-overlap risk:
  - W0: Wave 0 Nyquist scaffolds (1 plan)
  - W1: Release build config + R8 + sprite WebP conversion + ABI split (1 plan)
  - W2: JankStats integration + face detection latency timing (1 plan)
  - W2/W3: Phase 6 polish trio — bbox gating + ContentObserver + LeakCanary manifest disable (1 plan, possibly split into 2)
  - W3: 07-PERF-REPORT scaffold + Xiaomi 13T baseline measurement runbook (1 plan)
  - W4: Device matrix runbook + secondary OEM handoff + 8 deferred UAT items folded (1 plan)
  - W5: Clean release APK + final HANDOFF + device PASS + nyquist flip + Phase 7 close-out (1 plan)
- **D-26:** Sequential within-wave execution (worktrees disabled per Phase 4+5+6 precedent). File-overlap constraints same as prior phases.

### Claude's Discretion

12+ items left to executor judgement at impl time:
- Specific ProGuard rule additions if R8 strips Compose/Hilt/CameraX entry points (handle inline per Phase 4/5 inline-gap-fix protocol)
- JankStats `JankFrameListener` aggregation window (recommend 1000-frame rolling stats)
- WebP conversion tool (`cwebp` or `webp-converter-gradle-plugin`)
- ABI split file naming convention (release APK suffix)
- Test scaffold layout for Phase 7 (5-7 test files estimate)
- ContentObserver scope (per-MIME registration vs single Files URI)
- Reference APK install retry strategy (try `--bundletool` or `apk-extractor` if `-r` still fails)
- Whether to add `R8` `-keep` rules for Compose/Hilt up-front vs reactive
- LeakCanary debug-only `<activity>` disable mechanism (manifest merge attribute vs `tools:replace`)
- Cloud Firebase Test Lab opt-in (default: skip unless user requests)
- Whether to dump perf measurements into `.planning/perf-data/` as JSON for reproducibility
- 07-HANDOFF Vietnamese bilingual hints depth (same as 06-HANDOFF precedent)

### Folded Todos

No GSD-tracked todos pending for Phase 7 (per memory). All deferred items entered via Phase 6 CHECKPOINT carry-forward (see D-20, D-21).

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Roadmap & Requirements
- `.planning/ROADMAP.md` §"Phase 7: Performance & Device Matrix" — phase goal + 5 success criteria
- `.planning/REQUIREMENTS.md` PRF-01..05 — measurable acceptance bars
- `.planning/PROJECT.md` — Core Value statement + locked tech stack constraints

### Prior Phase Decisions (file-overlap risk + invariant preservation)
- `.planning/phases/02-camera-preview-face-detection-coordinate-validation/02-CONTEXT.md` — CameraX + OverlayEffect + MlKitAnalyzer architecture
- `.planning/phases/03-first-filter-end-to-end-photo-capture/03-CONTEXT.md` — Filter render stack + AssetLoader + 9 D-32 grep-asserts origin
- `.planning/phases/04-filter-catalog-picker-face-filter-mode/04-CONTEXT.md` — Sprite catalog + Coil deps + DataStore
- `.planning/phases/05-video-recording-audio-insect-filter-free-placement-mode/05-CONTEXT.md` — ThermalMonitor + VideoRecorder + sticker mode
- `.planning/phases/06-ux-polish-splash-home-onboarding-preview-collection-share/06-CONTEXT.md` — UX-01..09 + SHR-01..04 + D-32 grep pattern table

### Phase 6 Handoff Carryover (folded into Phase 7 scope)
- `.planning/phases/06-ux-polish-splash-home-onboarding-preview-collection-share/06-CHECKPOINT.md` §"Gap Inventory" — 3 LOW-severity items folded per D-20
- `.planning/phases/06-ux-polish-splash-home-onboarding-preview-collection-share/06-HANDOFF.md` §"Soft Gates" — 8 deferred UAT items folded per D-21

### External Documentation (cited in PROJECT.md / CLAUDE.md)
- [CameraX release notes](https://developer.android.com/jetpack/androidx/releases/camera) — Canvas `OverlayEffect` API + `CameraEffect` subclass path
- [AGP 8.9 ABI splits](https://developer.android.com/build/configure-apk-splits) — `abiFilters` config
- [JankStats library](https://developer.android.com/reference/androidx/metrics/performance/package-summary)
- [R8 / Compose ProGuard rules](https://developer.android.com/jetpack/compose/setup#kotlin_1)
- `CLAUDE.md` §"Fallback Plan" — GL CameraEffect escalation contract

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `app/src/main/java/com/bugzz/filter/camera/detector/FaceDetectorClient.kt` — already wraps `FaceDetector.process()`; perf timing add-in straightforward (D-04)
- `app/src/main/java/com/bugzz/filter/camera/data/CollectionRepository.kt` — already `Flow<List<MediaItem>>` from MediaStore query; ContentObserver wiring is single-line addition (D-20b)
- `app/src/main/java/com/bugzz/filter/camera/render/DebugOverlayRenderer.kt` — bbox + landmark debug viz target for `BuildConfig.DEBUG` gating (D-20a)
- `app/build.gradle.kts` — release `buildType` block exists in template state (`isMinifyEnabled = false` currently); enable here (D-06)
- `app/src/main/AndroidManifest.xml` — base manifest where LeakCanary's `LeakLauncherActivity` needs `tools:node="remove"` in `debug/AndroidManifest.xml` overlay (D-20c)

### Established Patterns
- All 9 D-32 grep-asserts (Phase 3+4+5 inline fixes) must survive R8 minification → may require `-keep` rules in `proguard-rules.pro`
- Inline gap-fix protocol from Phase 5 precedent (05-gaps-01 + 05-gaps-02) carries forward: trivial issues fixed inline in plan SUMMARY; non-trivial → `07-gaps-NN-PLAN.md`
- Device verification ADB workflow (memory `reference_adb_workflow.md`): `install -r`, `pm clear`, `pm grant CAMERA RECORD_AUDIO POST_NOTIFICATIONS`, `uiautomator dump` for coord lookup, `screencap -p` via `/sdcard/sc.png` (NOT pipe to avoid CRLF corruption on Windows)
- Phase 4+5+6 plan structure: `<task type="auto">` (autonomous) vs `<task type="checkpoint:human-verify" gate="blocking">` (device handoff only)

### Integration Points
- `gradle/libs.versions.toml` — add `metricsPerformance = "1.0.0-beta02"` (D-01)
- `app/build.gradle.kts` `android.buildTypes.release` block — minify + shrinkResources (D-06)
- `app/build.gradle.kts` `android.splits.abi` block — arm64-v8a only (D-08)
- `app/src/main/assets/sprites/` — WebP conversion target (D-07)
- `app/src/debug/AndroidManifest.xml` — needs creation if not exists (D-20c)
- `app/src/main/java/com/bugzz/filter/camera/render/DebugOverlayRenderer.kt` — debug gating (D-20a)
- `app/src/main/java/com/bugzz/filter/camera/data/CollectionRepository.kt` — ContentObserver (D-20b)

</code_context>

<specifics>
## Specific Ideas

- Pre-warmed thermal test pattern: record 60s × 5 consecutive sessions, then 1 measurement session. Logcat tail filtered to `Perf` + `ThermalMonitor` + `JankStats`. (D-19)
- Audio sync verification: run `ffprobe -v error -select_streams a:0 -show_entries stream=start_time,duration -of csv=p=0 video.mp4` then same for `v:0`. Compute diff. (D-10)
- WebP conversion: use `cwebp -q 90 sprite.png -o sprite.webp` in pre-build script OR Android Studio's built-in "Convert to WebP..." right-click on assets folder.
- R8 keep rules: lean on Hilt 2.57 + Compose BOM 2026.04 which both ship recent ProGuard rules in their AARs automatically. Add custom keeps ONLY if R8 strips a D-32 grep-assert target.
- Reference APK install retry order: (1) `adb install-multiple`, (2) `apkpure-style` bundletool from `apks-extractor`, (3) skip + document (D-15).

</specifics>

<deferred>
## Deferred Ideas

These are NOT Phase 7 scope; capture for v2 / future roadmap.

### v2 Polish (already in REQUIREMENTS.md)
- POL-01..06 (countdown, flash, music, watermark, multi-face, debug-toggle)
- Real Play Store URLs + privacy policy hosting
- Localization (i18n) — Vietnamese-first then en-US
- Trending feed / social features
- Analytics + crashlytics
- Ads / billing
- Filter quality settings (low / medium / high)
- Photo edit features (crop, rotate, brightness)

### Performance / Architecture (v2)
- Macrobenchmark CI integration (when team grows + multi-device CI budget exists)
- A/B testing framework for sprite asset bundles
- Module split (extract `:camera`, `:render`, `:data`, `:ui` from `:app`)
- KMP feasibility study (iOS port)

### Reviewed Todos (not folded)
None — Phase 7 folds all carry-forward items via D-20 / D-21.

</deferred>

---

*Phase: 07-performance-device-matrix*
*Context gathered: 2026-05-13*
*Auto-locked per user delegation chain pattern (memory `feedback_autonomy.md` + Phase 5/6 precedent)*
