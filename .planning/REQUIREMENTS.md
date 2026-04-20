# Requirements: Bugzz

**Defined:** 2026-04-18
**Core Value:** Smooth live AR preview with bug sprites tracking face landmarks — if live preview stutters or bugs don't stick to the face, everything else is meaningless.

## v1 Requirements

Feature-parity clone of reference app (`com.insect.filters.funny.prank.bug.filter.face.camera` v1.2.7), MINUS monetization and i18n. Each requirement maps to one roadmap phase.

### Foundation

- [ ] **FND-01**: Project builds successfully on AGP 8.9.x with Kotlin 2.1.21, targeting minSdk 28 / targetSdk 35
- [ ] **FND-02**: All major dependencies managed via `libs.versions.toml` catalog (CameraX, ML Kit, Compose, Hilt, Coil, Lottie, Timber pinned to researched versions)
- [ ] **FND-03**: Hilt dependency injection wired into `Application` and `MainActivity`
- [ ] **FND-04**: Navigation skeleton (navigation-compose) between Splash → Home → Camera → Preview → Collection screens (stubs OK)
- [ ] **FND-05**: AndroidManifest declares CAMERA, RECORD_AUDIO, POST_NOTIFICATIONS permissions; no WRITE_EXTERNAL_STORAGE
- [ ] **FND-06**: Runtime permission flow (CAMERA first-launch, RECORD_AUDIO lazy on record, POST_NOTIFICATIONS lazy)
- [ ] **FND-07**: Debug builds have StrictMode + LeakCanary wired
- [ ] **FND-08**: App installs via `adb install` on real Android 9+ device and opens without crashing

### Camera & Face Detection

- [x] **CAM-01**: Live CameraX preview renders on `CameraXViewfinder` composable in CameraScreen
- [x] **CAM-02**: User can flip between front and back camera via on-screen button
- [x] **CAM-03**: CameraX `UseCaseGroup` binds Preview + ImageCapture + VideoCapture + ImageAnalysis under one lifecycle
- [x] **CAM-04**: ML Kit Face Detection (contour mode, bundled model) runs on preview frames via `MlKitAnalyzer(COORDINATE_SYSTEM_SENSOR)`
- [x] **CAM-05**: ImageAnalysis backpressure set to `STRATEGY_KEEP_ONLY_LATEST`; preview does not stall when detection is slow
- [x] **CAM-06**: `OverlayEffect` binds to `PREVIEW | IMAGE_CAPTURE | VIDEO_CAPTURE` targets; debug overlay (red rect on face boundingBox) renders on preview
- [ ] **CAM-07**: Debug overlay stays aligned in portrait + landscape, front + back lens (no manual matrix math — uses `frame.getSensorToBufferTransform()`)
- [x] **CAM-08**: Face tracking IDs (`trackingId`) remain stable across frames for the same face
- [x] **CAM-09**: 1€ (One-Euro) filter smooths landmark jitter between detector callback and renderer

### Filter Render Engine

- [ ] **REN-01**: `FilterEngine` draws bug sprites onto overlay Canvas via `OverlayEffect.onDrawListener`
- [ ] **REN-02**: Per-bug state machine supports 4 behaviors: STATIC, CRAWL, SWARM, FALL
- [ ] **REN-03**: Bugs anchor to face landmarks (nose, cheeks, forehead, jawline, lips) using ML Kit contour points
- [ ] **REN-04**: Sprite assets loaded from `assets/sprites/` via `AssetLoader` with LruCache<String, Bitmap>
- [ ] **REN-05**: Flipbook animation plays per sprite at configured frame rate
- [ ] **REN-06**: When no face detected, renderer draws nothing (no crash, no last-frame ghost)
- [ ] **REN-07**: Changing filter mid-preview takes effect within 1 frame without rebinding CameraX
- [ ] **REN-08**: Render pipeline maintains ≥24 fps on a 2019 mid-tier test device (Snapdragon 675-class equivalent)

### Filter Catalog

- [x] **CAT-01**: `FilterCatalog` bundles 15-25 bug filters (spider, ant, cockroach, worm, beetle, fly, scorpion, centipede, wasp, tick, caterpillar, roach, moth, mosquito, mantis, etc.)
- [x] **CAT-02**: Each filter has: id, display name, thumbnail, sprite atlas reference, bug behavior config, landmark anchor spec
- [x] **CAT-03**: Filter picker UI (horizontal `LazyRow`) shows thumbnails, highlights selected, scrolls smoothly
- [x] **CAT-04**: Tapping a filter thumbnail switches active filter immediately
- [x] **CAT-05**: Last-used filter persisted in DataStore, restored on app relaunch

### Dual Mode (Face Filter + Insect Filter)

- [x] **MOD-01**: Home screen has two primary buttons: "Face Filter" (landmark-tracked) and "Insect Filter" (free-placement sticker)
- [x] **MOD-02**: Face Filter mode anchors bugs to face landmarks with real-time tracking
- [ ] **MOD-03**: Insect Filter mode places a single bug sticker on screen without face tracking
- [ ] **MOD-04**: Insect Filter mode supports drag gesture to move sticker
- [ ] **MOD-05**: Insect Filter mode supports pinch-to-zoom gesture
- [ ] **MOD-06**: Insect Filter mode supports rotation gesture
- [ ] **MOD-07**: Insect Filter mode sticker survives camera flip and orientation change

### Photo Capture

- [x] **CAP-01**: Shutter button captures photo via CameraX `ImageCapture.takePicture()`
- [x] **CAP-02**: Captured JPEG has filter overlay baked in (OverlayEffect composites into IMAGE_CAPTURE output)
- [x] **CAP-03**: Photo saved to `DCIM/Bugzz/` via MediaStore `Images` insert with `IS_PENDING` transaction pattern
- [ ] **CAP-04**: Front-camera photos saved with correct mirror convention matching reference app behavior
- [ ] **CAP-05**: Saved photo visible in Google Photos / device gallery within 1 second of capture
- [ ] **CAP-06**: No Bitmap memory leaks in capture path (verified via LeakCanary)

### Video Recording

- [ ] **VID-01**: Record button starts video recording via CameraX `Recorder` + `VideoCapture`
- [ ] **VID-02**: Video output has overlay baked in (OverlayEffect composites into VIDEO_CAPTURE output)
- [ ] **VID-03**: Audio captured from device microphone and synced with video
- [ ] **VID-04**: Recording auto-stops at 60-second cap; user can stop earlier via button
- [ ] **VID-05**: Front-camera video uses `MIRROR_MODE_ON_FRONT_ONLY` to match reference convention
- [ ] **VID-06**: Video saved as MP4 to `DCIM/Bugzz/` via MediaStore `Video` insert
- [ ] **VID-07**: Recording indicator (red dot + elapsed timer) visible while recording
- [ ] **VID-08**: `PowerManager.ThermalStatusListener` hooked; above `THERMAL_STATUS_MODERATE` drops detection to `PERFORMANCE_MODE_FAST`
- [ ] **VID-09**: Exit-during-record triggers confirmation dialog; cancel preserves recording
- [ ] **VID-10**: RECORD_AUDIO permission requested lazily on first record attempt, not at app launch

### UX Screens

- [ ] **UX-01**: Splash screen displays app logo via Lottie animation, auto-advances in ≤ 2 seconds
- [ ] **UX-02**: First-launch flow shows 3-screen Lottie onboarding carousel (skippable)
- [ ] **UX-03**: Home screen matches reference visual spec: two large mode buttons (Face Filter / Insect Filter), settings gear icon, collection icon
- [ ] **UX-04**: Preview/Result screen shows captured photo/video with Save, Share, Delete, Retake actions
- [ ] **UX-05**: My Collection screen lists saved photos and videos from `DCIM/Bugzz/` via MediaStore query
- [ ] **UX-06**: Collection items open full-screen preview with share/delete actions
- [ ] **UX-07**: Empty state shown when collection is empty (matches reference)
- [ ] **UX-08**: Delete-item confirmation dialog prevents accidental deletion
- [ ] **UX-09**: Settings screen shows app version, privacy policy link, rate app (no-op placeholder)

### Share

- [ ] **SHR-01**: Share button on Preview and Collection screens invokes `Intent.ACTION_SEND` with MediaStore content URI
- [ ] **SHR-02**: Share intent `type` matches artifact MIME (image/jpeg or video/mp4)
- [ ] **SHR-03**: Android share sheet shows available targets (WhatsApp, Instagram, TikTok, Facebook, Messenger, Zalo if installed)
- [ ] **SHR-04**: Shared content arrives at target app with overlay intact

### Performance & Device Matrix

- [ ] **PRF-01**: Live preview sustains ≥ 24 fps during normal filter playback on mid-tier test device
- [ ] **PRF-02**: Face detection latency ≤ 100 ms per frame (measured via Android Studio profiler)
- [ ] **PRF-03**: 60-second video record produces a file with audio synced within 50 ms drift
- [ ] **PRF-04**: Final release APK ≤ 40 MB (via APK Analyzer)
- [ ] **PRF-05**: App verified working on Samsung + Pixel (minimum 2-OEM matrix) on real Android 9+ devices

## v2 Requirements

Deferred to future release. Tracked but not in current roadmap.

### Polish

- **POL-01**: Countdown timer (3s / 5s / 10s presets) before photo/video capture
- **POL-02**: Flash / torch toggle (back cam = LED; front cam = white-screen overlay)
- **POL-03**: Music overlay on video (bundled audio tracks, Media3 Transformer post-mux)
- **POL-04**: Watermark overlay (bottom-right app logo PNG, user-togglable off if v2 adds premium)
- **POL-05**: Multi-face support (apply filter to every detected face, not just the largest)
- **POL-06**: Direct-share deep-link buttons for Instagram, TikTok, Facebook, YouTube (preselect target)
- **POL-07**: Catalog expansion to 30-50 filters
- **POL-08**: TimeWarp Scan filter mode

### Monetization (separate milestone)

- **MON-01**: AdMob banner on home/collection screens
- **MON-02**: Interstitial ad between capture and preview
- **MON-03**: Rewarded ad for unlocking premium filters
- **MON-04**: AppLovin SDK mediation layer
- **MON-05**: Google Play Billing IAP for "remove ads" / premium filter pack

### Localization (separate milestone)

- **LOC-01**: Translate UI strings to Vietnamese, Spanish, Portuguese, Hindi (top 5 Play markets)
- **LOC-02**: Auto-detect device locale and switch UI language

## Out of Scope

Explicitly excluded. Documented to prevent scope creep.

| Feature | Reason |
|---------|--------|
| Cloud filter catalog (Volio-style CDN) | No backend infrastructure; all filters bundled offline |
| User accounts / cloud sync | Fully offline app; no server |
| Beauty filter / face retouch | Reference is bug-prank-only; not in scope |
| Trending video feed | Requires backend + curated content pipeline |
| In-app rating prompt | No Play Store publication planned for MVP |
| iOS port | Android-only (PROJECT.md locked) |
| Jetpack Views + XML UI | Synthesis chose Compose (SUMMARY.md Resolution #1) |
| ML Kit Face Mesh (478-point) | Overkill for 2D bug sprites; Face Detection contour suffices (SUMMARY.md Resolution #2) |
| Filament 3D engine | +12-18MB APK cost with no MVP benefit; Canvas 2D suffices (SUMMARY.md Resolution #3) |
| Per-filter sound effects | Not in reference; speculative |
| Shake-to-scare motion trigger | Not in reference; not in category norms |
| WRITE_EXTERNAL_STORAGE permission | Unnecessary from Android 10+; MediaStore API used instead |

## Traceability

Final mapping locked by roadmap (2026-04-18). Every v1 requirement is assigned to exactly one phase; no orphans, no duplicates.

| Requirement | Phase | Status |
|-------------|-------|--------|
| FND-01 | Phase 1 | Pending |
| FND-02 | Phase 1 | Pending |
| FND-03 | Phase 1 | Pending |
| FND-04 | Phase 1 | Pending |
| FND-05 | Phase 1 | Pending |
| FND-06 | Phase 1 | Pending |
| FND-07 | Phase 1 | Pending |
| FND-08 | Phase 1 | Pending |
| CAM-01 | Phase 2 | Complete |
| CAM-02 | Phase 2 | Complete |
| CAM-03 | Phase 2 | Complete |
| CAM-04 | Phase 2 | Complete |
| CAM-05 | Phase 2 | Complete |
| CAM-06 | Phase 2 | Complete |
| CAM-07 | Phase 2 | Pending |
| CAM-08 | Phase 2 | Complete |
| CAM-09 | Phase 2 | Complete |
| REN-01 | Phase 3 | Pending |
| REN-02 | Phase 3 | Pending |
| REN-03 | Phase 3 | Pending |
| REN-04 | Phase 3 | Pending |
| REN-05 | Phase 3 | Pending |
| REN-06 | Phase 3 | Pending |
| REN-07 | Phase 3 | Pending |
| REN-08 | Phase 3 | Pending |
| CAP-01 | Phase 3 | Complete |
| CAP-02 | Phase 3 | Complete |
| CAP-03 | Phase 3 | Complete |
| CAP-04 | Phase 3 | Pending |
| CAP-05 | Phase 3 | Pending |
| CAP-06 | Phase 3 | Pending |
| CAT-01 | Phase 4 | Complete |
| CAT-02 | Phase 4 | Complete |
| CAT-03 | Phase 4 | Complete |
| CAT-04 | Phase 4 | Complete |
| CAT-05 | Phase 4 | Complete |
| MOD-01 | Phase 4 | Complete |
| MOD-02 | Phase 4 | Complete |
| MOD-03 | Phase 5 | Pending |
| MOD-04 | Phase 5 | Pending |
| MOD-05 | Phase 5 | Pending |
| MOD-06 | Phase 5 | Pending |
| MOD-07 | Phase 5 | Pending |
| VID-01 | Phase 5 | Pending |
| VID-02 | Phase 5 | Pending |
| VID-03 | Phase 5 | Pending |
| VID-04 | Phase 5 | Pending |
| VID-05 | Phase 5 | Pending |
| VID-06 | Phase 5 | Pending |
| VID-07 | Phase 5 | Pending |
| VID-08 | Phase 5 | Pending |
| VID-09 | Phase 5 | Pending |
| VID-10 | Phase 5 | Pending |
| UX-01 | Phase 6 | Pending |
| UX-02 | Phase 6 | Pending |
| UX-03 | Phase 6 | Pending |
| UX-04 | Phase 6 | Pending |
| UX-05 | Phase 6 | Pending |
| UX-06 | Phase 6 | Pending |
| UX-07 | Phase 6 | Pending |
| UX-08 | Phase 6 | Pending |
| UX-09 | Phase 6 | Pending |
| SHR-01 | Phase 6 | Pending |
| SHR-02 | Phase 6 | Pending |
| SHR-03 | Phase 6 | Pending |
| SHR-04 | Phase 6 | Pending |
| PRF-01 | Phase 7 | Pending |
| PRF-02 | Phase 7 | Pending |
| PRF-03 | Phase 7 | Pending |
| PRF-04 | Phase 7 | Pending |
| PRF-05 | Phase 7 | Pending |

**Coverage:**
- v1 requirements: 67 total
- Mapped to phases: 67
- Unmapped: 0 ✓
- Duplicates: 0 ✓

---
*Requirements defined: 2026-04-18*
*Last updated: 2026-04-18 after roadmap creation — traceability locked*
