# Phase 7: Performance & Device Matrix - Discussion Log

> **Audit trail only.** Decisions in CONTEXT.md.

**Date:** 2026-05-13
**Phase:** 07-performance-device-matrix
**Mode:** Auto-locked recommended defaults per user delegation ("OK tiếp phase 7 --chain nhé")

## Areas auto-decided (no user-presented gray-area selection — autonomous chain pattern)

| Area | Auto-decision summary |
|------|------------------------|
| Benchmark methodology | JankStats in-app + Android Studio Profiler for deep dives. No Macrobenchmark (CI overhead unjustified for solo dev). |
| Face detection latency | Inline `Timber.tag("Perf")` timing around `FaceDetector.process()` in existing FaceDetectorClient. Median ≤100ms acceptance. |
| APK size reduction | R8 + `minifyEnabled true` + `shrinkResources true` + WebP sprites + arm64-v8a-only ABI split. Should hit ≤40MB easily from current 88MB debug baseline. |
| Audio sync (PRF-03) | `ffprobe -show_streams` analysis on 60s test record. Threshold 50ms. |
| Frame drop counting | `ffprobe -count_frames` + JankStats observer. |
| Cross-OEM matrix | Xiaomi 13T primary + user-sourced Samsung/Pixel best-effort. Firebase Test Lab free-tier fallback. |
| GL CameraEffect escalation | YAGNI — measure first; escalate only on documented failure. |
| Thermal mitigation | Pre-warmed 5×60s stress + measurement session; verify ThermalMonitor throttle log + ≥20fps via JankStats. |
| Release build config | `minifyEnabled true` + R8 default rules + Compose/Hilt ProGuard rules from BOMs. |
| Reference APK comparison | Retry with `install-multiple` / `bundletool`; skip+document if `INSTALL_FAILED_MISSING_SPLIT` persists. |
| Phase 6 polish trio | All 3 folded: bbox debug gating + ContentObserver + LeakCanary manifest disable. |
| Phase 4+5 deferred UAT (8 items) | All 8 folded into device matrix runbook as cross-OEM validation criteria. |
| Plan budget | 5-7 plans / 4-5 waves estimate. Sequential execution (worktrees disabled). |

## All 26 D-decisions auto-locked to Recommended (D-01..D-26 in 07-CONTEXT.md)

Highlights:
- D-01..D-05 Measurement methodology (JankStats + Profiler + in-pipeline timing)
- D-06..D-09 APK shrink stack (R8 + minify + WebP + arm64-v8a-only)
- D-10..D-11 ffprobe-based PRF-03 verification
- D-12..D-15 Device matrix (Xiaomi primary + Samsung/Pixel best-effort + FTL fallback + reference APK retry)
- D-16..D-18 YAGNI escalation policy with `07-PERF-REPORT.md` documentation requirement
- D-19 Pre-warmed thermal stress methodology
- D-20 (a/b/c) Phase 6 polish trio folded
- D-21 Phase 4+5 deferred UAT (8 items) folded
- D-22..D-24 Release build quality + signed APK + D-32 grep-asserts survive R8
- D-25..D-26 Plan budget 5-7 plans / 4-5 waves; sequential execution

## Claude's Discretion items

12+ items left to executor judgement at impl time — see CONTEXT §Claude's Discretion.

## Deferred Ideas

Captured in CONTEXT §deferred. Summary: POL-01..06 v2 polish, real Play Store URLs, i18n, trending feed, ads, billing, Macrobenchmark CI, module split, KMP port.
