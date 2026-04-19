# Roadmap: Bugzz

**Created:** 2026-04-18
**Granularity:** standard
**Milestone:** v1 — feature-parity clone (no monetization, no i18n)
**Core Value:** Smooth live AR preview with bug sprites tracking face landmarks — every phase must serve this outcome.

## Phases

- [x] **Phase 1: Foundation & Skeleton** — Gradle, Hilt, Compose nav shell, manifest, permissions, StrictMode installed and app boots on real device. (completed 2026-04-18)
- [ ] **Phase 2: Camera Preview + Face Detection + Coordinate Validation** — CameraX 1.6 pipeline live on-device with `OverlayEffect` + `MlKitAnalyzer` debug overlay proving sensor-to-buffer transform across all orientations and lenses.
- [ ] **Phase 3: First Filter End-to-End + Photo Capture** — one production filter tracks face in real time, shutter bakes overlay into JPEG saved to `DCIM/Bugzz/`.
- [ ] **Phase 4: Filter Catalog + Picker + Face Filter Mode** — 15-25 bundled filters with 4 bug behaviors, horizontal picker, instant in-preview filter swap.
- [ ] **Phase 5: Video Recording + Audio + Insect Filter Free-Placement Mode** — 60s video capture with audio+overlay muxed, plus draggable/pinch/rotate sticker mode.
- [ ] **Phase 6: UX Polish — Splash, Home, Onboarding, Preview, Collection, Share** — every reference screen wired, MediaStore-backed collection, Android share sheet delivering artifacts with overlay intact.
- [ ] **Phase 7: Performance & Device Matrix** — measured ≥24fps on mid-tier device, thermal mitigation verified, APK ≤40MB, cross-OEM pass on Samsung + Pixel.

## Phase Details

### Phase 1: Foundation & Skeleton
**Goal**: Buildable, runnable app skeleton on a real Android 9+ device with all dependency, DI, navigation, and permission scaffolding in place so every subsequent phase can focus on camera/render work.
**Depends on**: Nothing (first phase)
**Requirements**: FND-01, FND-02, FND-03, FND-04, FND-05, FND-06, FND-07, FND-08
**Success Criteria** (what must be TRUE):
  1. `./gradlew :app:assembleDebug` produces an APK from a clean checkout using the pinned Kotlin 2.1.21 / AGP 8.9.x / Compose BOM 2026.04.00 stack; no version warnings.
  2. `adb install` on a physical Android 9+ device opens the app, lands on a navigable Splash → Home → Camera (stub) → Preview (stub) → Collection (stub) Compose flow with no crashes.
  3. On first launch, CAMERA permission is requested via `ActivityResultContracts`; denying it shows a "Open Settings" CTA rather than a blank screen; RECORD_AUDIO and POST_NOTIFICATIONS are NOT requested yet.
  4. Debug builds have StrictMode + LeakCanary active; zero StrictMode violations on cold start to Home screen.
  5. `AndroidManifest.xml` declares CAMERA, RECORD_AUDIO, POST_NOTIFICATIONS only (no WRITE_EXTERNAL_STORAGE); Hilt `@HiltAndroidApp` wired, injectable `@AndroidEntryPoint` MainActivity resolves dependencies.
**Plans**: 4 plans

Plans:
- [x] 01-01-PLAN.md — Gradle toolchain + version catalog + wrapper bootstrap (Wave 0)
- [x] 01-02-PLAN.md — App module build + Hilt + Compose nav shell + routes + stub screens (Wave 1)
- [x] 01-03-PLAN.md — AndroidManifest + resources + CAMERA permission flow + test scaffolds (Wave 2)
- [x] 01-04-PLAN.md — Clean debug build + FND-08 device handoff runbook (Wave 3)

### Phase 2: Camera Preview + Face Detection + Coordinate Validation
**Goal**: Validate the architecturally load-bearing `OverlayEffect` + `MlKitAnalyzer(COORDINATE_SYSTEM_SENSOR)` + `getSensorToBufferTransform()` pairing end-to-end on real hardware so Phase 3+ can draw production sprites without rewriting the pipeline.
**Depends on**: Phase 1
**Requirements**: CAM-01, CAM-02, CAM-03, CAM-04, CAM-05, CAM-06, CAM-07, CAM-08, CAM-09
**Success Criteria** (what must be TRUE):
  1. Live CameraX preview renders via `CameraXViewfinder` composable; front/back flip button swaps lens in <500ms without "Camera in use" errors on repeated toggles (10x).
  2. `MlKitAnalyzer` (bundled model, contour mode) runs with `STRATEGY_KEEP_ONLY_LATEST`; preview sustains visibly smooth motion with face detection enabled (no sticky/stalled frames, no `Image already closed` logs).
  3. Debug overlay drawn via `OverlayEffect.setOnDrawListener` renders a red rectangle that pixel-perfectly wraps the detected face in portrait, landscape, reverse-portrait, and reverse-landscape, on BOTH front and back lens, with zero manual matrix math.
  4. A 5-second test recording produced via `VideoCapture` on the bound `UseCaseGroup` saves an `.mp4` in which the red debug rectangle is visibly baked into every frame (proves three-stream `PREVIEW | IMAGE_CAPTURE | VIDEO_CAPTURE` binding).
  5. Face `trackingId` remains stable for the same face across 60+ consecutive frames; a One-Euro filter smooths raw landmark jitter to <1px/frame on a still head.
**Plans**: 6 plans
**Gap-closure plans**: 3 (02-gaps-01..03 — initial device verification on 2026-04-19 surfaced CAM-07 overlay over-draw + CAM-08 null trackingId blockers; see 02-VERIFICATION.md)

Plans:
- [x] 02-01-PLAN.md — Nyquist unit test scaffolds (OneEuroFilter + FaceDetectorOptions + OverlayEffectBuilder + CameraController) (Wave 0)
- [x] 02-02-PLAN.md — CameraX 1.6 + ML Kit 16.1.7 + Timber version catalog + app deps + portrait manifest + CameraModule (Wave 1)
- [x] 02-03-PLAN.md — Detector pipeline: OneEuroFilter + LandmarkSmoother + FaceSnapshot + FaceDetectorClient + CameraLensProvider + FaceLandmarkMapper stub (Wave 2)
- [x] 02-04-PLAN.md — Render + Controller: DebugOverlayRenderer + OverlayEffectBuilder + CameraController + CameraControllerTest un-Ignore (Wave 3)
- [x] 02-05-PLAN.md — Compose UI: CameraScreen + CameraViewModel + CameraUiState + OneShotEvent + BugzzApp CameraRoute rewire (Wave 4)
- [x] 02-06-PLAN.md — Clean debug build + 02-HANDOFF.md Xiaomi 13T device runbook + user sign-off checkpoint (Wave 5)
- [x] 02-gaps-01-PLAN.md — GAP-02-A detector + research amendment (remove .enableTracking() under contour; ADR-01) (Gap Wave 1)
- [x] 02-gaps-02-PLAN.md — GAP-02-B renderer matrix-scale compensation + centroid dot reduction + device re-verification (Gap Wave 2)
- [x] 02-gaps-03-PLAN.md — GAP-02-C MP4 frame extraction + Phase 2 final sign-off (Gap Wave 3)

### Phase 3: First Filter End-to-End + Photo Capture
**Goal**: Prove the full render + capture pipeline with one production filter so every remaining phase is content/feature work on a validated engine — user can capture a photo with a bug visibly on their face and find it in Google Photos.
**Depends on**: Phase 2
**Requirements**: REN-01, REN-02, REN-03, REN-04, REN-05, REN-06, REN-07, REN-08, CAP-01, CAP-02, CAP-03, CAP-04, CAP-05, CAP-06
**Success Criteria** (what must be TRUE):
  1. A single production filter (e.g. "ant-on-nose", STATIC behavior) renders on the face in live preview at ≥24fps on a 2019 mid-tier test device, with the bug anchored to ML Kit landmark points and no last-frame ghost when the face leaves frame.
  2. Tapping the shutter button via `ImageCapture.takePicture()` saves a JPEG to `DCIM/Bugzz/` using the MediaStore `IS_PENDING` transaction pattern; the photo is visible in Google Photos within 1 second of capture.
  3. The saved JPEG contains the bug sprite baked in at the same position it appeared in live preview (proves `OverlayEffect` on `IMAGE_CAPTURE` target) with correct front-camera mirror convention matching reference app.
  4. `AssetLoader` decodes sprites from `assets/sprites/` into an `LruCache<String, Bitmap>`; a flipbook animation plays at configured frame rate; swapping from test filter to production filter takes effect within one preview frame without rebinding CameraX.
  5. LeakCanary reports zero leaks after 30 consecutive captures; memory profiler shows flat allocation curve (no per-frame Bitmap churn in hot path).
**Plans**: TBD

### Phase 4: Filter Catalog + Picker + Face Filter Mode
**Goal**: Scale from one filter to a shipping catalog (15-25 bug filters across 4 behaviors) with a polished filter picker so users can browse and switch bug effects live on-camera — delivering the Face Filter mode end-to-end.
**Depends on**: Phase 3
**Requirements**: CAT-01, CAT-02, CAT-03, CAT-04, CAT-05, MOD-01, MOD-02
**Success Criteria** (what must be TRUE):
  1. `FilterCatalog` (Kotlin `object`) bundles 15-25 filters spanning spider / ant / cockroach / worm / beetle / fly / scorpion / centipede / wasp / tick / caterpillar / etc., each with id, display name, thumbnail, sprite atlas, behavior, and landmark anchor spec.
  2. All four behaviors (STATIC, CRAWL, SWARM, FALL) render correctly on-face: CRAWL follows face contour, SWARM drifts toward anchor, FALL spawns above and drops, STATIC snaps to landmark — each visible at ≥24fps on the test device.
  3. Horizontal `LazyRow` filter picker renders thumbnails, highlights selected filter, scrolls smoothly, and survives mid-session rapid-tap (10 swaps in 5s) without visible stutter or CameraX rebind.
  4. Home screen's "Face Filter" button launches camera in tracked mode with last-used filter restored from DataStore; "Insect Filter" button is present (non-functional until Phase 5).
  5. Multi-face scene (2 faces in frame) does not crash; primary face receives full filter; contour-dependent behaviors fall back gracefully to boundingBox center on secondary faces.
**Plans**: TBD
**UI hint**: yes

### Phase 5: Video Recording + Audio + Insect Filter Free-Placement Mode
**Goal**: Add the two features that reuse the validated render pipeline — 60s video capture with synced audio and overlay baked in, plus the free-placement Insect Filter sticker mode with drag/pinch/rotate gestures.
**Depends on**: Phase 4
**Requirements**: VID-01, VID-02, VID-03, VID-04, VID-05, VID-06, VID-07, VID-08, VID-09, VID-10, MOD-03, MOD-04, MOD-05, MOD-06, MOD-07
**Success Criteria** (what must be TRUE):
  1. Record button starts CameraX `Recorder` + `VideoCapture`; recording indicator (red dot + elapsed timer) is visible; auto-stops at 60s cap; user stop-button ends recording earlier; exit-during-record shows confirmation dialog that preserves recording on cancel.
  2. Saved MP4 in `DCIM/Bugzz/` contains video + synced microphone audio (drift <50ms over 60s) with the active filter overlay baked into every frame; front-camera videos use `MIRROR_MODE_ON_FRONT_ONLY`.
  3. RECORD_AUDIO permission is requested lazily on the first tap of record (not at app launch or camera open); denial shows an inline rationale; acceptance allows recording to proceed immediately.
  4. `PowerManager.ThermalStatusListener` is active: above `THERMAL_STATUS_MODERATE` the detector drops to `PERFORMANCE_MODE_FAST`; a 60s recording on a pre-warmed device maintains ≥20fps end-to-end.
  5. Insect Filter mode places a single draggable sticker that responds to drag, pinch-to-zoom, and two-finger rotation gestures; sticker state (position, scale, rotation) survives camera flip and device orientation change.
**Plans**: TBD
**UI hint**: yes

### Phase 6: UX Polish — Splash, Home, Onboarding, Preview, Collection, Share
**Goal**: Replace all navigation stubs with production screens matching the reference visual spec, giving users the complete end-to-end journey from splash to saving artifacts to re-opening the Collection and sharing to social apps.
**Depends on**: Phase 5
**Requirements**: UX-01, UX-02, UX-03, UX-04, UX-05, UX-06, UX-07, UX-08, UX-09, SHR-01, SHR-02, SHR-03, SHR-04
**Success Criteria** (what must be TRUE):
  1. First-launch flow: Lottie splash auto-advances in ≤2s → 3-screen skippable Lottie onboarding carousel → Home screen with "Face Filter" and "Insect Filter" buttons, settings gear, and collection icon matching reference visual spec.
  2. After capture, Preview/Result screen shows the captured photo or video with Save / Share / Delete / Retake actions; Delete shows a confirmation dialog; Share invokes `Intent.ACTION_SEND` with the MediaStore content URI and correct MIME type.
  3. My Collection screen queries MediaStore for `DCIM/Bugzz/` artifacts and renders a grid; empty state is shown when no artifacts exist; tapping an item opens full-screen preview with share/delete; delete requires confirmation.
  4. Android share sheet shows available targets (WhatsApp, Instagram, TikTok, Facebook, Messenger, Zalo if installed); chosen target receives the content URI with `FLAG_GRANT_READ_URI_PERMISSION` and successfully opens the file with filter overlay intact.
  5. Settings screen shows app version, privacy policy link (stub), and rate-app placeholder; all screens navigate via `navigation-compose` with consistent back-stack behavior; second-launch skips onboarding and lands on Home directly.
**Plans**: TBD
**UI hint**: yes

### Phase 7: Performance & Device Matrix
**Goal**: Verify that the Core Value (smooth live AR preview) holds under measurement on real mid-tier hardware across at least two OEMs, with the escalation path (Canvas → custom GL `CameraEffect`) invoked only if Canvas profiles poorly.
**Depends on**: Phase 6
**Requirements**: PRF-01, PRF-02, PRF-03, PRF-04, PRF-05
**Success Criteria** (what must be TRUE):
  1. Live preview sustains ≥24fps measured via Android Studio profiler during normal filter playback on a 2019 mid-tier test device (Snapdragon 675-class equivalent); bug sprite visibly tracks face across device rotation without drift.
  2. ML Kit face detection latency measured ≤100ms/frame (profiler trace); a 60-second video record produces a file whose audio track drifts <50ms from video and drops zero frames (verified via `ffprobe`).
  3. APK Analyzer shows final release APK ≤40MB (total + per-ABI); no single resource bucket exceeds 50% of total size; sprite assets normalized to single-density WebP.
  4. App is verified working end-to-end (preview + photo + video + share) on at least one Samsung device AND one Pixel device, both running Android 9+, on real hardware (not emulator).
  5. If Canvas-based rendering fails to hit 24fps target with full sprite load, the documented GL `CameraEffect` escalation path is implemented and re-measured to pass criterion #1; if Canvas passes, escalation is documented as deferred with rationale.
**Plans**: TBD

## Progress

| Phase | Plans Complete | Status | Completed |
|-------|----------------|--------|-----------|
| 1. Foundation & Skeleton | 4/4 | Complete    | 2026-04-18 |
| 2. Camera Preview + Face Detection + Coord Validation | 0/6 | Planned | - |
| 3. First Filter End-to-End + Photo Capture | 0/? | Not started | - |
| 4. Filter Catalog + Picker + Face Filter Mode | 0/? | Not started | - |
| 5. Video Recording + Audio + Insect Filter Mode | 0/? | Not started | - |
| 6. UX Polish — Splash, Home, Onboarding, Preview, Collection, Share | 0/? | Not started | - |
| 7. Performance & Device Matrix | 0/? | Not started | - |

## Coverage Summary

- **Total v1 requirements:** 67
- **Mapped to phases:** 67 (100%)
- **Orphans:** 0
- **Duplicates:** 0

| Phase | Requirements | Count |
|-------|--------------|-------|
| 1 | FND-01..08 | 8 |
| 2 | CAM-01..09 | 9 |
| 3 | REN-01..08, CAP-01..06 | 14 |
| 4 | CAT-01..05, MOD-01, MOD-02 | 7 |
| 5 | VID-01..10, MOD-03..07 | 15 |
| 6 | UX-01..09, SHR-01..04 | 13 |
| 7 | PRF-01..05 | 5 |
| **Total** | | **67** |

## Key Roadmap Decisions

| Decision | Rationale |
|----------|-----------|
| 7 phases (not fewer) | Research ARCHITECTURE.md §7 build-order graph maps to 7 risk-calibrated deliverables; standard granularity supports 5-8. |
| Phase 2 is risk-front-loaded | Coord-space + video-overlay-compositing pitfalls (PITFALLS.md #1, #2) must validate before Phase 3 draws production sprites, else Phase 5 becomes a Phase 3 rewrite. |
| MOD-01 + MOD-02 in Phase 4, MOD-03..07 in Phase 5 | Face Filter mode (tracked) belongs with filter catalog/picker it drives; Insect Filter free-placement mode shares Phase 5 with video because both are "new render mode on existing pipeline." |
| CAP-01..06 in Phase 3 (not its own phase) | Photo capture is the integration test that proves three-stream compositing end-to-end; splitting it from first-filter would be artificial. |
| Phase 7 explicit (not inline tuning) | Core Value per PROJECT.md is performance; profiling requires a feature-complete app to avoid fake wins and to invoke the Canvas→GL escalation path with real measurements. |

---
*Roadmap created: 2026-04-18*
*Phase 2 planned: 2026-04-19 — 6 plans, 5 waves, Wave 5 ends with Xiaomi 13T device handoff*
