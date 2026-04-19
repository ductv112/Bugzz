---
phase: 02
slug: camera-preview-face-detection-coordinate-validation
status: draft
nyquist_compliant: true
wave_0_complete: true
created: 2026-04-19
---

# Phase 02 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.
> Derived from 02-RESEARCH.md §Validation Architecture.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit 4 (`junit:junit:4.13.2`) — established Phase 1 D-19 |
| **Instrumented framework** | `androidx.test.ext:junit:1.3.0` + `espresso-core:3.7.0` — Phase 1 D-20 |
| **Config file** | none beyond `app/build.gradle.kts` `testImplementation`/`androidTestImplementation` already in place |
| **Quick run command** | `./gradlew :app:testDebugUnitTest --tests "com.bugzz.filter.camera.detector.*"` |
| **Full suite command** | `./gradlew :app:testDebugUnitTest :app:lintDebug` |
| **Instrumented (optional)** | `./gradlew :app:connectedDebugAndroidTest` — requires physical Xiaomi 13T, not blocking |
| **Estimated runtime (quick)** | ~10 seconds (pure-Kotlin unit tests only) |
| **Estimated runtime (full)** | ~40-60 seconds (adds lint pass) |

---

## Sampling Rate

- **After every task commit:** Run `./gradlew :app:testDebugUnitTest --tests "com.bugzz.filter.camera.detector.*"` (quick — pure-Kotlin logic only)
- **After every plan wave:** Run `./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug` (full debug build + unit tests + lint)
- **Before `/gsd-verify-work`:** Full suite must be green AND `02-HANDOFF.md` device runbook executed on Xiaomi 13T
- **Max feedback latency:** 10 seconds (quick run); 60 seconds (full run)

---

## Per-Task Verification Map

Filled by planner. Each task in each PLAN.md links into this table via `<automated>` block or Wave 0 dependency. Columns mirror the research Phase Requirements → Test Map.

| Task ID | Plan | Wave | Requirement | Threat Ref | Secure Behavior | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|------------|-----------------|-----------|-------------------|-------------|--------|
| 02-01-01 | 02-01 | 0 | CAM-09 | — | N/A (math) | unit | `./gradlew :app:testDebugUnitTest --tests "*OneEuroFilterTest*"` | ✅ | ❌ red (intentional — SUT lands in Plan 02-03) |
| 02-01-02 | 02-01 | 0 | CAM-04 | — | N/A | unit | `./gradlew :app:testDebugUnitTest --tests "*FaceDetectorOptionsTest*"` | ✅ | ❌ red (intentional — SUT lands in Plan 02-03) |
| 02-01-02 | 02-01 | 0 | CAM-06 | — | N/A | unit | `./gradlew :app:testDebugUnitTest --tests "*OverlayEffectBuilderTest*"` | ✅ | ❌ red (intentional — SUT lands in Plan 02-04) |
| 02-01-03 | 02-01 | 0 | CAM-03, CAM-05 | — | N/A | unit | `./gradlew :app:testDebugUnitTest --tests "*CameraControllerTest*"` | ✅ | ❌ red (intentional — SUT lands in Plan 02-04; mockito-kotlin added in Plan 02-02) |
| 02-XX-YY | — | later | CAM-01, CAM-02, CAM-07 | — | N/A | manual-only | device runbook 02-HANDOFF.md on Xiaomi 13T | ❌ manual | ⬜ pending |
| 02-gap-01 | 02-gaps-01 | 1 | CAM-08 (relaxed) | — | N/A | manual-only | adb logcat grep `FaceTracker` — trackingId may be null under contour mode (ML Kit limitation, ADR-01); acceptance = boundingBox centerX/centerY stable across consecutive frames on still head | ❌ manual | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

Four unit test files must exist before ANY `feat(02-...)` implementation commit that writes the code under test. This is the Nyquist gate — no code without a failing test.

- [x] `app/src/test/java/com/bugzz/filter/camera/detector/OneEuroFilterTest.kt` — covers CAM-09. Cases:
   1. Constant input → output equals input within ε=1e-6.
   2. Step input (jump from 0 to 100 at t=1.0) → output smoothly approaches 100 over 5+ samples.
   3. Sine-wave jitter on stationary base → output shows measurable attenuation (RMS output < RMS input).
   4. First sample initializes without division-by-zero (`tPrev` defaults handled).
- [x] `app/src/test/java/com/bugzz/filter/camera/detector/FaceDetectorOptionsTest.kt` — covers CAM-04 + CAM-15. Assert `FaceDetectorClient.buildOptions()` produces:
   - `performanceMode == PERFORMANCE_MODE_FAST`
   - `contourMode == CONTOUR_MODE_ALL`
   - `isTrackingEnabled == false` (updated 2026-04-19 per GAP-02-A / ADR-01 — ML Kit silently ignores .enableTracking() under CONTOUR_MODE_ALL)
   - `minFaceSize == 0.15f`
   - `landmarkMode == LANDMARK_MODE_NONE`
   - `classificationMode == CLASSIFICATION_MODE_NONE`
- [x] `app/src/test/java/com/bugzz/filter/camera/render/OverlayEffectBuilderTest.kt` — covers CAM-06. Assert:
   - Target mask equals `CameraEffect.PREVIEW or CameraEffect.VIDEO_CAPTURE or CameraEffect.IMAGE_CAPTURE`
   - `queueDepth == 0`
- [x] `app/src/test/java/com/bugzz/filter/camera/camera/CameraControllerTest.kt` — covers CAM-03 + CAM-05. Mock `ProcessCameraProvider`. Assert:
   - After `bind()`, `UseCaseGroup` includes exactly 4 use cases (Preview + ImageAnalysis + ImageCapture + VideoCapture)
   - After `bind()`, `UseCaseGroup` has 1 effect attached
   - `ImageAnalysis` backpressure strategy equals `STRATEGY_KEEP_ONLY_LATEST`
- [ ] (optional, not blocking) `app/src/androidTest/java/com/bugzz/filter/camera/camera/CameraControllerInstrumentedTest.kt` — smoke test bind → flip lens → unbind cycle does not leak the test Activity (LeakCanary will flag).

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| `CameraXViewfinder` renders live preview | CAM-01 | Real camera hardware required | 02-HANDOFF.md step 1 — install debug APK on Xiaomi 13T, launch, verify preview in CameraScreen |
| Front/back lens flip <500ms, 10× no error | CAM-02 | Hardware + timing + logcat | 02-HANDOFF.md step 2 — tap flip button 10×, grep logcat for `CameraInUseException`; expect zero |
| Overlay pixel-perfect across 4 device rotations × 2 lenses | CAM-07 | Physical device rotation required | 02-HANDOFF.md step 3 — rotate Xiaomi 13T portrait → landscape → reverse-portrait → reverse-landscape on both front and back lens; red rect + landmark dots must stay aligned |
| boundingBox centerX/centerY stable on still head 60+ consecutive frames (trackingId expected null — ADR-01) | CAM-08 (relaxed post-GAP-02-A) | Runtime logcat inspection + ML Kit contour-mode limitation | 02-HANDOFF.md step 10 (re-verify) — adb logcat grep `FaceTracker`, confirm `id=null` (expected), confirm `bb=X,Y` centerX/Y vary <10px across 60 consecutive frames while head held still. Full bbox-IoU tracking deferred to Phase 3 per ADR-01. |
| 5-second test recording bakes overlay into MP4 | CAM-06 (end-to-end) | MediaStore write + video playback | 02-HANDOFF.md step 5 — tap TEST RECORD button, open saved MP4 in Google Photos, verify red rect + landmark dots visible in every frame |

---

## Validation Sign-Off

- [ ] All tasks have `<automated>` verify or Wave 0 dependencies
- [ ] Sampling continuity: no 3 consecutive tasks without automated verify
- [ ] Wave 0 covers all MISSING references (4 unit test files minimum)
- [ ] No watch-mode flags
- [ ] Feedback latency < 10s (quick) / < 60s (full)
- [x] `nyquist_compliant: true` set in frontmatter (after planner assigns all Task IDs)
- [ ] `02-HANDOFF.md` device runbook executed on Xiaomi 13T (user sign-off — pastes 12/12 PASS result into STATE.md)

**Approval:** Wave 0 Nyquist gate satisfied on 2026-04-19 — Plan 02-01 landed all four test files (commits 98b3348, 97a5378, 693d986). Intentional RED state; Plans 02-02/02-03/02-04 turn them GREEN. Device-runbook sign-off remains pending (Plan 02-06 handoff).
