---
phase: 03
slug: first-filter-end-to-end-photo-capture
status: draft
nyquist_compliant: false
wave_0_complete: false
created: 2026-04-19
---

# Phase 03 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution. Extends Phase 2's JUnit 4 + Robolectric 4.13 + Mockito-Kotlin harness with 6 new test files + 2 additive extensions.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit 4 (unit, pure JVM) + Robolectric 4.13 (where CameraX construction, Canvas, PointF, or Bitmap shadowing required — Phase 2 learned pattern STATE #12) + Mockito-Kotlin (stubbing `Face`, `FaceContour`, `ImageCapture`) |
| **Config file** | `gradle/libs.versions.toml` entries already present (`junit`, `mockito-core`, `mockito-kotlin`, `robolectric`, `truth`); `app/build.gradle.kts` already configures `testOptions.unitTests.isReturnDefaultValues = true` + Robolectric + Compose testing dep |
| **Quick run command** | `./gradlew :app:testDebugUnitTest` |
| **Full suite command** | `./gradlew :app:testDebugUnitTest :app:assembleDebug` |
| **Estimated runtime** | ~45 seconds (Phase 2 baseline was ~20s for 10 tests; Phase 3 adds ~10 tests) |

---

## Sampling Rate

- **After every task commit:** Run `./gradlew :app:testDebugUnitTest`
- **After every plan wave:** Run `./gradlew :app:testDebugUnitTest :app:assembleDebug`
- **Before `/gsd-verify-work`:** Full suite must be green AND clean Debug APK builds AND manual 03-HANDOFF.md runbook signed off on Xiaomi 13T
- **Max feedback latency:** 60 seconds

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Threat Ref | Secure Behavior | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|------------|-----------------|-----------|-------------------|-------------|--------|
| 03-01-01..NN | 01 (Wave 0 scaffolds) | 0 | REN-01..08, CAP-01..06, ADR-01 #1..3 | T-03-02/04/05 | Tests land RED before SUT (Nyquist gate) | unit scaffold | `./gradlew :app:testDebugUnitTest` | ❌ Wave 0 creates | ⬜ pending |
| 03-02-01..NN | 02 (ADR-01 close + tracker) | 1 | ADR-01 #1, #2, #3 | — | trackingId stability now comes from BboxIouTracker, not ML Kit | unit | `./gradlew :app:testDebugUnitTest --tests "*BboxIouTrackerTest* *LandmarkSmootherTest* *FaceDetectorClientTest*tracker*"` | ❌ W0 → ✅ W1 | ⬜ pending |
| 03-03-01..NN | 03 (Filter data + AssetLoader + FilterEngine) | 2 | REN-01, REN-02, REN-03, REN-04, REN-05, REN-06, REN-07 | T-03-02 (OOM decode), T-03-05 (biometric log policy) | malformed sprite decode → OneShotEvent.Error, no crash; no face coords in Timber | unit + Robolectric | `./gradlew :app:testDebugUnitTest --tests "*FilterCatalogTest* *AssetLoaderTest* *FaceLandmarkMapperTest* *BugBehaviorTest* *FilterEngineTest*"` | ❌ W0 → ✅ W2 | ⬜ pending |
| 03-04-01..NN | 04 (CameraController.capturePhoto + CameraViewModel/Screen shutter UX) | 3 | CAP-01, CAP-02, CAP-03 | T-03-01 (storage full), T-03-04 (callback leak) | storage full → OneShotEvent.Error("Storage full"); ImageCapture callback weakly held or cleared in onCleared | unit + Robolectric | `./gradlew :app:testDebugUnitTest --tests "*CameraControllerTest*capturePhoto* *CameraControllerTest*mediaStore*"` | ❌ W0 → ✅ W3 | ⬜ pending |
| 03-05-01..NN | 05 (Build verify + HANDOFF + CAM-08 VERIFICATION update) | 4 | REN-08, CAP-04, CAP-05, CAP-06, ADR-01 #4 | T-03-04 (leak runbook), T-03-06 (asset provenance) | LeakCanary green after 30 captures; reference asset sha256 logged in HANDOFF | manual (device) + doc | `./gradlew :app:assembleDebug` + HANDOFF.md runbook | ❌ W0 → ✅ W4 | ⬜ pending |

*Task IDs (03-NN-MM) finalize when PLAN.md files land — planner sets exact count per wave. This map pins the plan↔requirement↔test mapping so planner emits matching test commands in `<automated>` blocks.*

### Per-Requirement Test Specification

| Req ID | Behavior | Test Type | Automated Command | File |
|--------|----------|-----------|-------------------|------|
| REN-01 | `FilterEngine.onDraw` invokes behavior tick + draws bitmap at computed position | unit (Robolectric for Canvas) | `./gradlew :app:testDebugUnitTest --tests "*FilterEngineTest*"` | ❌ W0 — `FilterEngineTest.kt` |
| REN-02 | `BugBehavior` sealed interface has exactly 4 variants; STATIC.tick sets position=anchor, velocity=0 | unit (pure JVM) | `./gradlew :app:testDebugUnitTest --tests "*BugBehaviorTest*"` | ❌ W0 — `BugBehaviorTest.kt` |
| REN-03 | `FaceLandmarkMapper.anchorPoint(face, NOSE_TIP)` returns NOSE_BRIDGE last point when populated; falls back to boundingBox center when not | unit (Robolectric for PointF — same as Phase 2 `DebugOverlayRendererTest`) | `./gradlew :app:testDebugUnitTest --tests "*FaceLandmarkMapperTest*"` | ❌ W0 — `FaceLandmarkMapperTest.kt` (replaces Phase 2 stub body) |
| REN-04 | `AssetLoader.sizeOf` returns `bitmap.allocationByteCount`; cache capped at min(32MB, maxMemory/8); `get` returns null before preload, Bitmap after | unit + Robolectric | `./gradlew :app:testDebugUnitTest --tests "*AssetLoaderTest*"` | ❌ W0 — `AssetLoaderTest.kt` |
| REN-05 | Flipbook frame index = `((elapsed / frameDurationNanos) % frameCount)`; verified by driving synthetic `frame.timestamp` values | unit | `./gradlew :app:testDebugUnitTest --tests "*FilterEngineTest*flipbook*"` | ❌ W0 — `FilterEngineTest.kt` covers |
| REN-06 | `FilterEngine.onDraw(face=null)` returns early without drawing | unit (Canvas mock `verify(canvas, never()).drawBitmap`) | `./gradlew :app:testDebugUnitTest --tests "*FilterEngineTest*noFace*"` | ❌ W0 |
| REN-07 | `setFilter(B)` followed by `onDraw` returns without drawing (preload pending); `setFilter(B)` after preload draws B's sprite; no CameraX rebind (AtomicReference only) | unit | `./gradlew :app:testDebugUnitTest --tests "*FilterEngineTest*swap*"` | ❌ W0 |
| REN-08 | ≥24fps subjective smoothness on Xiaomi 13T during normal filter playback | **manual** | — | 03-HANDOFF.md Step |
| CAP-01 | `CameraController.capturePhoto` invokes `ImageCapture.takePicture` with populated OutputFileOptions | unit (Robolectric, Phase 2 CameraControllerTest pattern) | `./gradlew :app:testDebugUnitTest --tests "*CameraControllerTest*capturePhoto*"` | ❌ W0 — extend existing `CameraControllerTest.kt` |
| CAP-02 | JPEG on Xiaomi 13T shows bug sprite baked in (same position as preview) | **manual** (Phase 2 three-stream OverlayEffect bake already proven via gaps-03 MP4) | — | 03-HANDOFF.md Step |
| CAP-03 | OutputFileOptions built with `RELATIVE_PATH="DCIM/Bugzz"`, `MIME_TYPE="image/jpeg"`; IS_PENDING handled by CameraX | unit (Robolectric) | `./gradlew :app:testDebugUnitTest --tests "*CameraControllerTest*mediaStore*"` | ❌ W0 |
| CAP-04 | Front-cam JPEG mirror matches reference app convention | **manual** — Wave 0 device inspection precedes implementation spec | — | 03-HANDOFF.md Step + Wave 0 apk-install task |
| CAP-05 | Saved photo appears in Google Photos on Xiaomi 13T within 1s of capture | **manual** | — | 03-HANDOFF.md Step |
| CAP-06 | No LeakCanary notification after 30 consecutive captures + kill/relaunch | **manual** | — | 03-HANDOFF.md Step (runbook per D-36) |
| ADR-01 #1 | `BboxIouTracker.assign` matches existing IDs by IoU ≥ 0.3; assigns monotonic new IDs; removes entries after 5 dropout frames | unit (pure JVM with Mockito `Face` stubs) | `./gradlew :app:testDebugUnitTest --tests "*BboxIouTrackerTest*"` | ❌ W0 — `BboxIouTrackerTest.kt` |
| ADR-01 #2 | `LandmarkSmoother.onFaceLost(id)` clears that id's filter state; re-creation starts fresh | unit | `./gradlew :app:testDebugUnitTest --tests "*LandmarkSmootherTest*onFaceLost*"` | ❌ W0 — extend `OneEuroFilterTest.kt` or new `LandmarkSmootherTest.kt` |
| ADR-01 #3 | `FaceDetectorClient.createAnalyzer` consumer passes faces through tracker before SmoothedFace mapping; SmoothedFace.trackingId = tracker-assigned ID | integration (Robolectric; mock MlKitAnalyzer consumer) | `./gradlew :app:testDebugUnitTest --tests "*FaceDetectorClientTest*tracker*"` | ❌ W0 — new `FaceDetectorClientTest.kt` |
| ADR-01 #4 | `02-VERIFICATION.md` CAM-08 row updated post-handoff to reference tracker-assigned ID stability | **doc task** (no automated test) | — | Wave 4 planner task |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

New test files required (all pure JVM unless Robolectric noted):

- [ ] `app/src/test/java/com/bugzz/filter/camera/detector/BboxIouTrackerTest.kt` — unit (pure JVM); covers IoU math on `android.graphics.Rect`, greedy match, MAX_TRACKED_FACES=2 cap, dropout increment, monotonic ID assignment
- [ ] `app/src/test/java/com/bugzz/filter/camera/detector/FaceLandmarkMapperTest.kt` — unit (Robolectric for PointF shadowing — same pattern as Phase 2's `DebugOverlayRendererTest`); covers all 7 anchors (NOSE_TIP, FOREHEAD, LEFT_CHEEK, RIGHT_CHEEK, CHIN, LEFT_EYE, RIGHT_EYE) for primary + fallback + ultimate-fallback paths
- [ ] `app/src/test/java/com/bugzz/filter/camera/detector/FaceDetectorClientTest.kt` — integration (Robolectric; mock `MlKitAnalyzer` consumer path); covers tracker handoff into SmoothedFace mapping, `SMOOTHED_CONTOUR_TYPES` includes LEFT_EYEBROW_TOP + RIGHT_EYEBROW_TOP
- [ ] `app/src/test/java/com/bugzz/filter/camera/filter/FilterCatalogTest.kt` — unit (pure JVM); pins exactly 2 filters registered (ant_on_nose_v1, spider_on_forehead_v1) + anchor types + behavior=STATIC + scale factor + asset dir
- [ ] `app/src/test/java/com/bugzz/filter/camera/filter/AssetLoaderTest.kt` — Robolectric (Bitmap decode + AssetManager shadow); covers sizeOf formula (allocationByteCount), preload idempotency, get-before-preload returns null, manifest.json parse via kotlinx.serialization
- [ ] `app/src/test/java/com/bugzz/filter/camera/render/FilterEngineTest.kt` — unit (Robolectric for Canvas); covers no-face early return, flipbook frame-index math on synthetic `frame.frameTimeNanos`, swap-without-preload skips drawing, canvas.setMatrix(sensorToBufferTransform) called before drawBitmap
- [ ] `app/src/test/java/com/bugzz/filter/camera/render/BugBehaviorTest.kt` — unit (pure JVM); pins STATIC.tick sets `position=anchor, velocity=0`; CRAWL/SWARM/FALL throw NotImplementedError
- [ ] **EXTEND** `app/src/test/java/com/bugzz/filter/camera/camera/CameraControllerTest.kt` with `capturePhoto_invokesTakePicture`, `capturePhoto_mediaStoreOutputOptions_hasCorrectRelativePath`, `capturePhoto_onError_emitsErrorEvent` — Robolectric already wired (Phase 2 STATE #12)
- [ ] **EXTEND** `app/src/test/java/com/bugzz/filter/camera/detector/OneEuroFilterTest.kt` (or create `LandmarkSmootherTest.kt`) with `onFaceLost_clearsThatIdOnly_otherIdsSurvive` + `sameIdReappears_startsFreshState` — verifies ADR-01 #2

No framework install needed — JUnit 4 + Robolectric 4.13 + Mockito-Kotlin + kotlinx.serialization.json already on classpath (verified via libs.versions.toml grep).

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| Live preview ≥24fps subjective smoothness with ant-on-nose filter active | REN-08 | FPS measurement requires real device sensor + OverlayEffect pipeline; unit-level Canvas mocks cannot measure frame rate | 03-HANDOFF.md Step: "With filter active on front camera, hold phone steady 30 seconds. Observe smoothness — no visible stutter or frame drops. Optional: capture Android Studio CPU profiler trace (10s) for Phase 7 reference." |
| JPEG contains bug sprite baked in at preview position | CAP-02 | Requires opening saved JPEG in Google Photos and visually confirming overlay; no unit test can verify pixel-level JPEG output of CameraX effect pipeline | 03-HANDOFF.md Step: "Tap shutter with ant-on-nose filter active on front cam. Wait for Toast. Open Google Photos → DCIM/Bugzz/. Confirm newest JPEG shows the ant on subject's nose at the same position preview showed." |
| Front-cam JPEG mirror convention matches reference app | CAP-04 | Requires A/B comparison of reference APK output vs Bugzz output on same device | 03-HANDOFF.md Wave 0: `adb install -r reference/com.insect.filters.funny.prank.bug.filter.face.camera.apk`; capture front-cam photo in reference app; open in Google Photos; note mirror orientation. Then capture in Bugzz; compare. If different, apply `ImageCapture.setMirrorMode(MirrorMode.MIRROR_MODE_ON)` or post-process. |
| Photo visible in Google Photos within 1s of capture | CAP-05 | MediaStore pending-transaction timing + Google Photos indexing latency are end-to-end device behaviors | 03-HANDOFF.md Step: "Tap shutter → start stopwatch → open Google Photos immediately. Newest item should show saved photo thumbnail within 1000ms." |
| No LeakCanary after 30 consecutive captures + kill/relaunch | CAP-06 | Full ImageCapture → MediaStore → callback lifecycle retention requires real CameraX pipeline; Robolectric cannot replicate real Lifecycle tree | 03-HANDOFF.md Step: "Tap shutter 30 times consecutively. After each: confirm Toast fires. 10s after 30th capture: kill app (back button out + swipe from Recents). Relaunch app. LeakCanary notification in notification drawer should be ABSENT. If present: expand + screenshot for diagnosis." |
| Reference APK asset provenance | T-03-06 | Legal/IP checkpoint — not code behavior | 03-HANDOFF.md Wave 0: `sha256sum reference/com.insect.filters.funny.prank.bug.filter.face.camera.apk`; record hash in HANDOFF.md. Extracted sprites (`apktool d` output) tracked in git under `app/src/main/assets/sprites/` with extraction provenance noted in HANDOFF. |

---

## Validation Sign-Off

- [ ] All tasks have `<automated>` verify or Wave 0 dependencies — PLAN.md sets exact automated commands per task when plans land
- [ ] Sampling continuity: no 3 consecutive tasks without automated verify (manual-only tasks are bracketed by automated tasks in each wave)
- [ ] Wave 0 covers all MISSING references — 8 new test files + 2 extensions documented above
- [ ] No watch-mode flags (`:app:testDebugUnitTest` exits after run — Phase 2 enforcement pattern preserved)
- [ ] Feedback latency < 60s (45s Gradle baseline)
- [ ] `nyquist_compliant: true` set in frontmatter — flipped from `false` to `true` in plan-phase step 13 after planner emits `<automated>` blocks for every task

**Approval:** pending
