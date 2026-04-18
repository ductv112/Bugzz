# Project Research Summary

**Project:** Bugzz (Android AR Face Filter Camera App — feature-parity clone of `com.insect.filters.funny.prank.bug.filter.face.camera` v1.2.7)
**Domain:** Native Android AR camera app — CameraX + ML Kit face tracking + overlay compositing + photo/video capture + share
**Researched:** 2026-04-18
**Confidence:** HIGH overall — core technical approach verified against official Android/Google 2026 docs; reference APK forensically dissected; two stack details require on-device validation in Phase 1

---

## Executive Summary

Bugzz is a solo-dev, personal-use clone of a Vietnamese prank-filter camera app (Volio Group). The product's Core Value per PROJECT.md is **smooth live AR preview with bug sprites tracking face landmarks** — if the live preview stutters or the bugs don't stick to the face, every other feature (photo capture, video record, share) is meaningless. All architecture and stack decisions subordinate to that single axis. Native Android / Kotlin / CameraX / ML Kit are hard-locked by PROJECT.md; research pinned the remaining decisions around those locks.

The recommended approach is the **CameraX 1.6 `OverlayEffect` pipeline** driven by a Canvas callback, with face landmarks delivered via `MlKitAnalyzer` in sensor-space coordinates and paired with `frame.getSensorToBufferTransform()` so the overlay lands on the face across every device orientation, both lenses, and all three output streams (preview, still photo, video) without any manual matrix math. This single decision collapses what is normally three separate compositing pipelines into one, eliminating the #1 bug of hobby face-filter apps ("filter visible in preview, missing from saved video"). On top of this, the app is a standard MVVM + `StateFlow` Android app with ~6 screens, bundled sprite assets extracted from the reference APK, and MediaStore-based scoped storage saving to `DCIM/Bugzz/`.

The two biggest risks are (1) **coordinate-space bugs** (front-camera mirror + rotation + PreviewView scale-type) — mitigated by using `MlKitAnalyzer(COORDINATE_SYSTEM_SENSOR)` with `OverlayEffect` from day 1 and validating with a debug red-rect overlay as a Phase 2 exit criterion — and (2) **thermal / performance degradation on mid-tier minSdk-28 devices** during sustained video recording — mitigated by Canvas-based rendering (cheap for <20 sprites), `STRATEGY_KEEP_ONLY_LATEST` backpressure, a thermal listener, and a fallback escalation path to a custom GL `CameraEffect` subclass if the Canvas path profiles poorly. Reference-APK forensics revealed a server-driven filter catalog (`stores.volio.vn` CDN), a 3D Filament render engine, and a full monetization stack — we deliberately cut all three for MVP in favor of a bundled offline catalog and a simpler 2D renderer.

---

## Key Findings

### Recommended Stack

Greenfield Android app on Kotlin 2.1.21 / AGP 8.9.1 / JDK 17, with **CameraX 1.6.0 as the central integration point**. CameraX 1.6 (March 2026) is the first version where `camera-effects` (`OverlayEffect`) and `camera-compose` (`CameraXViewfinder`) are both stable — making it the single most important version lock in the project. ML Kit face-detection ships **bundled** (`com.google.mlkit:face-detection:16.1.7`, ~3–4MB APK cost) to guarantee offline first-launch and eliminate the Play Services model-download race condition.

**Core technologies:**
- **Kotlin 2.1.21 + AGP 8.9.1 + JDK 17** — stable tooling baseline; explicitly NOT AGP 9.x (KSP/Compose-plugin ecosystem still catching up Apr 2026).
- **CameraX 1.6.0** (core/camera2/lifecycle/video/view/effects/mlkit-vision/compose) — single API for preview + photo + video + ML Kit analyzer + effect compositing. Version must be pinned uniformly across all artifacts (version mismatch is Pitfall #5).
- **ML Kit Face Detection 16.1.7 (bundled, CONTOUR_ALL)** — offline face landmarks at 30–80ms/frame; contour mode gives dense-enough points for bug-on-face anchoring. See resolution #2 below for why we stay on Face Detection (not Face Mesh).
- **Jetpack Compose (BOM 2026.04.00) + Material3** — UI toolkit for all screens. See resolution #1 below for rationale over Views.
- **Hilt 2.57 + KSP 2.1.21-1.0.32** — DI; forward-compatible with eventual monetization module.
- **MediaStore + DataStore Preferences** — no Room DB for MVP; captures land in `DCIM/Bugzz/`, user prefs persist via DataStore.
- **Coil 2.7 + Timber 5.0.1 + LeakCanary (debug)** — image loading, logging, leak detection.

Full table in `.planning/research/STACK.md`.

### Expected Features

Reference-APK forensics (327 layouts, 640+ drawables, 6 DEX files, 20 asset subfolders) confirmed the feature surface directly — **the filter catalog itself is server-driven**, downloaded on-demand from `https://stores.volio.vn/stores/api/v5.0/public/`. This means we cannot extract a ground-truth filter list from the APK; the clone must ship a **bundled offline catalog of 15–25 bug filters** (spider/ant/cockroach/worm/beetle/fly/scorpion/centipede/wasp/tick/caterpillar/etc) to hit table-stakes volume.

**Must have (v1 — table stakes):**
- CameraX live preview with front/back flip, runtime CAMERA + RECORD_AUDIO permissions
- Real-time face detection per frame (ML Kit), bug sprites anchored to landmarks
- Photo + video capture with filter burned in (60s video cap)
- Save to `DCIM/Bugzz/` via MediaStore, generic Android share sheet
- Two entry modes per reference: **Face Filter** (tracked) + **Insect Filter** (free-placement draggable AR sticker)
- Horizontal filter picker, Preview/Result screen with Save/Share/Delete
- My Collection (gallery of saved outputs), splash screen, 3-screen Lottie onboarding
- Exit-during-record + delete-item confirmation dialogs

**Should have (v1.x — polish):**
- Countdown timer (3/5/10s presets)
- Flash / torch control (front-cam = white-screen overlay)
- Music overlay on video (bundled tracks, no CDN) — adds Media3 Transformer; L-complexity
- Direct-share deep-link buttons (Instagram, TikTok, FB, YouTube)
- Watermark overlay (bottom-right logo PNG)
- Multi-face support (apply to all detected, not just largest)
- Catalog expansion to 30–50 filters

**Defer (v2+ / explicitly out):**
- TimeWarp Scan mode (reference has it, architecturally isolated, deferable)
- Trending feed (requires backend + curated samples — skipped)
- Monetization: AdMob + AppLovin + IAP (PROJECT.md deferred)
- Localization / 95 locales (PROJECT.md English-only)
- In-app rating (no Play Store publication)
- Remote filter CDN, welcome-back re-engagement, premium watermark-removal
- Shake-to-scare, per-filter SFX — **speculative, not in reference, skip**

Full matrix in `.planning/research/FEATURES.md`.

### Architecture Approach

**MVVM + `StateFlow` + unidirectional data flow**, with a tight component boundary around CameraX. One `CameraViewModel` owns `StateFlow<CameraUiState>` + `SharedFlow<OneShotEvent>`; UI reads state, ViewModel delegates work to a set of thin collaborators. The **`OverlayEffect` + `MlKitAnalyzer(COORDINATE_SYSTEM_SENSOR)` pairing** is the architecturally load-bearing decision — it collapses three compositing pipelines (preview, still, video) into one Canvas draw callback with a matrix already aligned.

**Major components:**
1. **`CameraController`** — owns `ProcessCameraProvider`, `UseCaseGroup`, lens selector; exposes `takePicture()` / `startRecording()`.
2. **`FaceDetector`** — thin wrapper on `MlKitAnalyzer`; delivers `List<Face>` in sensor coords to `FilterEngine` via `AtomicReference<FaceSnapshot>` (lock-free, single-writer).
3. **`FilterEngine`** — the ONLY thing that draws. Runs on `renderExecutor` inside `OverlayEffect.onDrawListener`; owns per-bug animation state machines (CRAWL / SWARM / FALL / STATIC behaviors), reads latest face snapshot, draws sprites to overlay Canvas.
4. **`CaptureCoordinator`** — wraps `ImageCapture.takePicture()` + `Recorder.prepareRecording()`; writes through `MediaStoreRepo` to `DCIM/Bugzz/`.
5. **`FilterRepository`** (hardcoded Kotlin `object`) + **`AssetLoader`** (LruCache<String, Bitmap>) — filter catalog and sprite decode cache.
6. **`PrefsRepo`** (DataStore) — last-used filter, last-used camera facing.

Three executors: Main (UI + binding), `cameraExecutor` (single-thread for `MlKitAnalyzer` + capture callbacks), `renderExecutor` (single-thread for overlay draws). Shared state across threads uses `AtomicReference`, no locks.

Full component diagram + data flow in `.planning/research/ARCHITECTURE.md`.

### Critical Pitfalls

From 14 pitfalls catalogued in `.planning/research/PITFALLS.md`, five are existential to this project:

1. **Coordinate-space chaos** — front-camera mirror + rotation + PreviewView scale-type is the #1 bug. **Mitigation:** `MlKitAnalyzer(COORDINATE_SYSTEM_SENSOR)` + `frame.getSensorToBufferTransform()` pairing; zero manual matrix math. Debug red-rect overlay is a Phase 2 exit criterion.
2. **Video recording loses the overlay** — naïve architecture records the raw camera stream, not what's painted on the overlay View. Saved `.mp4` has clean face, no bugs. **Mitigation:** bind `OverlayEffect` to `PREVIEW | IMAGE_CAPTURE | VIDEO_CAPTURE` targets from day 1; verify with a test red-rectangle recording in Phase 3, before any production sprite is drawn.
3. **Landmark jitter → bug flicker** — per-frame independent detection means landmarks oscillate ±2–5px even on a still face. **Mitigation:** insert a 1€ (One-Euro) filter as middleware between detector callback and renderer; ~30 LOC per filter, widely adopted in MediaPipe/face-tracking pipelines.
4. **ImageAnalysis backpressure stalls preview** — default `STRATEGY_BLOCK_PRODUCER` + contour-mode detection (50ms/frame) backs up the queue. **Mitigation:** `STRATEGY_KEEP_ONLY_LATEST` + single-thread `cameraExecutor` + `MlKitAnalyzer` handles `ImageProxy.close()` timing automatically.
5. **Thermal throttling on long recording** — 5+ min recordings drop from 30fps to 15fps and may cut short silently. **Mitigation:** cap video at 60s (matches user expectation), hook `PowerManager.ThermalStatusListener`, drop detection to `PERFORMANCE_MODE_FAST` above `THERMAL_STATUS_MODERATE`, use `Quality.HD` (not FHD) as fallback.

Secondary pitfalls (all covered in PITFALLS.md): MediaStore IS_PENDING flag, Bitmap leaks in capture path, camera lifecycle leak on navigate-away, Android 13 POST_NOTIFICATIONS, device fragmentation (Samsung/Xiaomi vs Pixel), APK bloat from sprite densities, CameraX/ML Kit version mismatch, multi-face contour-only-for-primary quirk.

---

## Resolved in Synthesis

Three contradictions surfaced across the four research files. This synthesis commits to firm choices so the roadmapper doesn't inherit unresolved forks.

### RESOLVED #1 — UI Toolkit: **Jetpack Compose (full, not hybrid)**

**Conflict:** STACK.md recommends Compose; ARCHITECTURE.md recommends Views + XML "because the reference APK is Views-based."

**Decision: Jetpack Compose.**

**Rationale:**
- **CameraX-Compose is production-stable in 1.6.** `CameraXViewfinder` composable (stable in CameraX 1.5.1 Nov 2025, further hardened in 1.6 Mar 2026) eliminates the old `AndroidView(PreviewView)` hack and gives correct z-ordering for overlay UI (shutter button, filter picker) over the preview surface. This was the main historical reason teams stuck with Views for CameraX work, and it's gone as of 2026.
- **Reference being Views-based is not a clone constraint.** PROJECT.md requires **feature parity + visual parity**, not **implementation parity**. The user cannot tell from the APK output whether the home screen is an XML `ConstraintLayout` or a Compose `Box`. Pixel-level visual cloning is equally achievable in either toolkit.
- **Solo dev + greenfield + 4–6 screens is Compose's sweet spot.** Less boilerplate than XML + ViewBinding + Fragments. The filter picker (`LazyRow` with selection state) and two-button home screen are one-screen-of-code each in Compose versus Adapter + ViewHolder + DiffUtil + layout XML in Views.
- **2026 ecosystem reality.** StackOverflow answers, official samples, and new tutorials for Android 14+/15+ specific APIs are Compose-first. Choosing Views means fighting the documentation current for the next two years.
- **ARCHITECTURE.md's Views argument was a risk-management hedge** written before CameraX 1.6 stability was factored in. With CameraX-Compose stable, the hedge is no longer needed.

**Impact:** `ui/` package is composable screens (SplashScreen, HomeScreen, CameraScreen, PreviewScreen, CollectionScreen, OnboardingScreen), `AndroidManifest.xml` has a single `MainActivity` with `setContent { }`, navigation via `navigation-compose 2.8.9`. CameraController still owns the CameraX binding; the viewfinder is a `CameraXViewfinder` composable.

**Architecture fallback:** if a specific screen turns out to be pathologically hard in Compose (unlikely), that single screen can swap to `AndroidView` or a Views-based Activity without touching the camera/render/capture layers — they're Compose-agnostic.

**Confidence:** HIGH.

### RESOLVED #2 — Face Tracking: **Stay on ML Kit Face Detection (contour mode). Do NOT switch to Face Mesh.**

**Conflict:** PROJECT.md locked "ML Kit Face Detection (contour mode)." FEATURES.md forensic evidence shows the reference APK ships `assets/mlkit_facemesh/face_landmark_with_attention.tflite` + `face_mesh_graph.binarypb` (ML Kit **Face Mesh**, 478 landmarks) + MediaPipe face-detector fallback, NOT ML Kit Face Detection.

**Decision: Stay on ML Kit Face Detection (contour mode).** Do not upgrade to Face Mesh.

**Rationale (by tradeoff):**

| Dimension | Face Detection (contour) | Face Mesh (478 points) | Winner |
|---|---|---|---|
| Points available | ~130 contour + 14 landmarks | 478 dense mesh | Face Detection — more than enough for 2D bug anchoring |
| CameraX integration | **First-class `MlKitAnalyzer`** | Custom TFLite plumbing + coord transform | **Face Detection** — huge |
| Tracking ID (stable cross-frame identity) | **Built-in via `enableTracking()`** | Must roll own | **Face Detection** |
| 3D mesh / depth | No | Yes | Face Mesh — but **we don't need it** (2D sprite overlay, not 3D mask) |
| Inference cost on mid-tier minSdk-28 device | 30–80ms/frame contour; 20–40ms landmark | 50–120ms/frame with attention model | Face Detection |
| Setup complexity | `FaceDetectorOptions` builder, 10 LOC | Custom TFLite interpreter, input preprocessing, output decode, coord transform | Face Detection |
| Reference APK uses it | No (uses Face Mesh + MediaPipe + Filament) | Yes | Tie — but reference uses Face Mesh because it drives 3D filters; we're 2D (see #3) |

**The reference's choice of Face Mesh is downstream of its choice of Filament 3D rendering.** Face Mesh returns a 478-point 3D mesh because that's what you feed a Filament material to render a 3D mask warping to the face. For **2D bug sprites** anchored to cheek / nose / forehead / jawline, Face Detection contour mode gives dense enough points (jawline ~17 points, left/right cheek ~9 each, lips ~22) and also provides the `trackingId` needed to keep bug animation state stable across frames — a non-trivial feature Face Mesh doesn't ship with.

**Face Mesh would be right if we needed:** 3D-aware bug crawling (bug dips behind nose bridge with correct occlusion), 3D face masks deforming under head rotation, or fine features like eyelash-level anchoring. None are MVP scope.

**Impact:** PROJECT.md "Key Decisions" row stands as locked. STACK.md and ARCHITECTURE.md are correct as written. FEATURES.md's observation about the reference using Face Mesh is a publisher implementation detail, not a clone requirement.

**Research flag:** Phase 2 should verify on real device that contour-mode landmarks are stable + dense enough for the selected bug behaviors. If a specific filter design really needs 3D face occlusion (v2+ nicety), we can add MediaPipe Face Mesh as a parallel analyzer without disturbing the Face Detection primary pipeline.

**Confidence:** HIGH.

### RESOLVED #3 — 3D Engine: **2D Canvas-only. Do NOT include Filament.**

**Conflict:** FEATURES.md found 13 Filament `.filamat` material files in the reference APK's `assets/materials/` directory. STACK.md and ARCHITECTURE.md both recommend a 2D Canvas draw via `OverlayEffect`.

**Decision: 2D Canvas-only. No Filament, no OpenGL shader pipeline for MVP.**

**Rationale:**
- **Filament is a ~4–6MB native lib per ABI** (arm64-v8a + armeabi-v7a = +12–18MB APK cost conservatively). Reference APK is 67MB partly because of this; we're targeting <40MB.
- **Filament's wins (PBR shading, 3D models, HDR environments, chroma-key video-texture filters) don't apply to our feature set.** The bug prank theme is 2D cartoon sprite crawling on face — not metallic spiders with physically-based reflections.
- **The reference APK uses Filament because it ships face-morph filters (non-bug) that warp the face mesh in 3D.** Their Filament investment is amortized across a filter library we explicitly defer to v2+ (TimeWarp is P3 per FEATURES.md). **For 2D bug-on-face, Filament is cargo-cult.**
- **Canvas via `OverlayEffect` costs <2ms/frame for 3–5 sprites on a 2019 mid-tier (Snapdragon 675-class) device.** The budget is bitmap blits, not pixel shading — exactly the workload Canvas is designed for.
- **Escalation path exists if Canvas profiles poorly.** If Phase 7 profiling shows Canvas can't sustain 24fps with full production sprite load, escalate to a **custom `CameraEffect` subclass implementing OpenGL ES 2.0 texture blit** (not Filament). This stays inside CameraX's effect API (so video + photo baking still Just Works). Budget: 3–5 days. **Filament is never the right answer for "sprite blit but faster"** — it's a PBR engine.

**What we lose by skipping Filament:**
- 3D face-morph filters (waste warping, enlarged-eyes, swap-face) — out of scope per PROJECT.md.
- TimeWarp with photorealistic chroma-key — TimeWarp itself can be a pixel-column-copy algorithm + Canvas; doesn't need Filament. Deferred regardless.
- Realistic 3D bugs with depth occlusion — out of MVP scope.

**Impact:** `assets/` ships only PNG/WebP sprite sheets + Lottie JSON for onboarding/splash. No `.filamat` files, no Filament native libs. `AssetLoader` caches decoded `Bitmap` in LruCache. Render pipeline is `OverlayEffect.onDrawListener { canvas -> filterEngine.draw(canvas, ...) }`, pure Kotlin + Android Graphics.

**Research flag:** Phase 7 MUST include sustained-load test on Snapdragon-675-class device with full sprite catalog. Escalation trigger: avg frame time >33ms over 10-second recording. Escalation path: custom `CameraEffect` + GL ES 2.0, NOT Filament.

**Confidence:** HIGH.

---

## Implications for Roadmap

Research strongly suggests a **7-phase structure**, with phase ordering driven by the architectural dependency graph from ARCHITECTURE.md §7 and the pitfall-to-phase mapping from PITFALLS.md §13. The first two phases are **risk-front-loaded**: the coordinate-space + video-overlay-compositing architectural bet must be validated end-to-end before any production sprite is drawn, otherwise Phase 5 becomes a rewrite of Phase 3.

### Phase 1: Foundation & Skeleton

**Rationale:** Gradle tooling, manifest, permission contracts, empty Compose Activities, version catalog must come first — also where version-mismatch pitfall (#5) and main-thread ANR (#10) are inoculated via BoM + StrictMode + LeakCanary in debug builds.
**Delivers:** Buildable empty app, navigable skeleton (Splash → Home → Camera stub → Preview stub), runtime permission prompts, AndroidManifest audited for Android 13+.
**Uses:** Kotlin 2.1.21 + AGP 8.9.1 + Compose BOM 2026.04.00 + Hilt 2.57 + KSP + navigation-compose — full `libs.versions.toml`.
**Avoids:** Pitfalls #5, #10, #14.

### Phase 2: Camera Preview + Face Detection + Coordinate Validation

**Rationale:** THE risk-front-loading phase. Blocks everything. Must validate the CameraX 1.6 `OverlayEffect` + `MlKitAnalyzer(COORDINATE_SYSTEM_SENSOR)` architectural bet with a debug red-rect/red-dot overlay before any production sprite code.
**Delivers:** Live CameraX preview on real device (front + back, flip), `MlKitAnalyzer` wired, `OverlayEffect` bound to `PREVIEW | IMAGE_CAPTURE | VIDEO_CAPTURE` drawing a debug test rectangle. Face `boundingBox` visibly wraps face in portrait + landscape.
**Uses:** CameraX 1.6.0, ML Kit 16.1.7 bundled, `CameraXViewfinder` composable.
**Implements:** `CameraController`, `FaceDetector`, skeleton `FilterEngine` (stub draw).
**Avoids:** Pitfalls #1, #2, #4, #9, #13.
**Exit criteria:** Debug red-rect aligns pixel-perfect in portrait + landscape + front/back swap. Recording a 5-second test `.mp4` produces a file with the red rect visible.

### Phase 3: First Filter End-to-End (single bug, photo capture only)

**Rationale:** After Phase 2 proves the pipeline, Phase 3 is the integration test: one filter, one bug, anchored to nose, animated flipbook, photo capture baking overlay into JPEG.
**Delivers:** One "ant-on-nose" filter end-to-end. User taps shutter, JPEG lands in `DCIM/Bugzz/`, opens in Google Photos with bug visible.
**Uses:** `OverlayEffect` Canvas draw, `AssetLoader` Bitmap cache, `ImageCapture.OutputFileOptions` + `MediaStore.Images` insert.
**Implements:** `FilterEngine` v1 (STATIC behavior only), `BugInstance`, `AssetLoader`, `MediaStoreRepo`, `CaptureCoordinator.takePhoto()`.
**Avoids:** Pitfalls #3 (insert 1€ filter middleware), #6, #8, #12.

### Phase 4: Filter Catalog + Bug Behaviors + Picker UI

**Rationale:** One filter works → content work scales out. 4 bug behaviors (CRAWL / SWARM / FALL / STATIC) and 15–25 bundled filters. Filter picker is a `LazyRow` with selection state.
**Delivers:** Full filter catalog in picker, all 4 behaviors, mid-preview filter swap without jitter or rebind.
**Uses:** Kotlin `object FilterCatalog`, sprite sheets in `assets/sprites/`, DataStore for last-used-filter.
**Implements:** `FilterRepository`, all `render/behaviors/`, FilterPickerBar composable.
**Avoids:** Pitfall #4 (filter change is AtomicReference update; don't rebind CameraX).

### Phase 5: Video Recording + Audio + Insect Filter Free-Placement Mode

**Rationale:** Video reuses the same `OverlayEffect` — no new render code. Risk is audio permission lifecycle and `Recorder` state machine. Insect Filter mode (drag/resize/rotate AR sticker, no face tracking) is a render mode switch.
**Delivers:** Video capture with mic audio + overlay, 60s cap, exit-without-save dialog; Insect Filter mode with pinch-zoom + rotate + drag gestures.
**Uses:** CameraX `Recorder` + `VideoCapture`, `MediaStoreOutputOptions`, `RECORD_AUDIO` prompt, `.withAudioEnabled()`.
**Implements:** `CaptureCoordinator.startRecording()`, `RenderMode { FACE_TRACKED, FREE_PLACEMENT }` switch.
**Avoids:** Pitfalls #11 (thermal listener + 60s cap), #14 (RECORD_AUDIO requested lazily), #7 (`MIRROR_MODE_ON_FRONT_ONLY`).

### Phase 6: UX Polish — Splash, Home, Onboarding, Preview/Save, Collection, Share

**Rationale:** Screen shells are trivial once engine works. Assembles reference's navigation parity + generic Android share sheet.
**Delivers:** All screens match reference visual spec, Lottie splash + 3-screen onboarding, My Collection reads MediaStore, share intent delivers to WhatsApp/IG/TikTok.
**Uses:** Lottie-Android 6.7.1, Coil 2.7, navigation-compose, `Intent.ACTION_SEND` with MediaStore content URI.
**Implements:** All `ui/` composables, `ShareIntentFactory`, empty-state composables.
**Avoids:** Pitfall #7 (cross-OEM test — Samsung + one other in this phase).

### Phase 7: Performance & Device Matrix

**Rationale:** Core Value per PROJECT.md is smooth live preview. Explicit phase because inline premature optimization before feature-complete leads to fake wins. This is where Canvas-vs-GL escalation happens with real measurements.
**Delivers:** Verified ≥24fps on target mid-tier device; detection <100ms/frame; no dropped frames during 60s record; APK ≤40MB; cross-OEM matrix pass.
**Uses:** Android Studio profilers, `PowerManager.ThermalStatusListener`, APK Analyzer.
**Implements:** Thermal mitigation, optional GL `CameraEffect` subclass if Canvas profiles poorly.
**Avoids:** Pitfalls #8, #11, #12.

### Phase Ordering Rationale

- **P1 blocks everything** — can't render without a surface; tooling must be right (Pitfall #5).
- **P2 is the architectural gate** — `OverlayEffect` + `COORDINATE_SYSTEM_SENSOR` + video-baking validated with cheap debug overlay before P3 writes production code. Deferring this turns Phase 5 into a Phase 3 rewrite.
- **P3 is the integration test** — one filter + photo capture proves three-stream compositing end-to-end.
- **P4 before P5** — video reuses the same overlay pipeline; adding video when 1 filter exists is wasted verification.
- **P5 groups video with free-placement mode** — both are "new render modes on existing pipeline."
- **P6 UX last** — screen shells are trivial when engine works.
- **P7 explicit** — Core Value is performance; different mindset (profiling + measurement + escalation).

### Research Flags

Phases likely needing `/gsd-research-phase` during planning:

- **Phase 2** — **HIGH priority**. The `OverlayEffect` + `MlKitAnalyzer(COORDINATE_SYSTEM_SENSOR)` pattern is under-documented outside official Google blog posts + `camera-samples` repo. Research: exact `OverlayEffect.setOnDrawListener` thread model, whether `frame.getSensorToBufferTransform()` handles front-camera mirror or needs additional flip, interaction between `MlKitAnalyzer` coord system choice (`SENSOR` vs `VIEW_REFERENCED`) and `OverlayEffect` canvas matrix.
- **Phase 5** — **MEDIUM priority**. CameraX `Recorder` + active-`Recording` lifecycle has subtle gotchas. Also: post-capture music-overlay muxing (v1.x) needs Media3 Transformer research.
- **Phase 7** — **MEDIUM priority**. If Canvas profiles poorly, custom `CameraEffect` + GL ES 2.0 escalation needs research: EGL context sharing, texture upload from sprite bitmap, shader draw call inside CameraEffect.

Phases with standard patterns (skip research):

- **Phase 1** — standard Android Gradle/manifest setup.
- **Phase 3** — integration of already-researched components.
- **Phase 4** — content work + straightforward Compose UI.
- **Phase 6** — composable screens + Lottie + share intent are well-documented 2026 patterns.

---

## Confidence Assessment

| Area | Confidence | Notes |
|------|------------|-------|
| Stack | HIGH | Core versions verified via Google Maven + Android Developers April 2026. `OverlayEffect` pattern verified via multiple official sources + API ref. Canvas perf estimate MEDIUM (validated in Phase 7). |
| Features | HIGH | Reference APK directly disassembled. Feature inheritance classified REF vs CAT vs NEW. Shake-to-scare investigated + dismissed. |
| Architecture | HIGH | `OverlayEffect` + `MlKitAnalyzer` + sensor-coord is Google's canonical recommendation. MVVM + StateFlow is boring/correct. |
| Pitfalls | HIGH | 14 pitfalls with official citations, mapped to preventing phase + verification step. |

**Overall confidence:** HIGH. No contradiction remains unresolved; three explicit decisions committed above.

### Gaps to Address

- **On-device Canvas performance at full sprite load** — theoretical <2ms/frame for 3–5 sprites; reference likely runs 10+ per SWARM filter. **Handle during Phase 7**: profile real device, escalate to custom GL `CameraEffect` if needed.
- **Reference's exact `MAX_DURATION` / `MIN_DURATION` video constants** — in DEX but not extractable without deobfuscation. **Handle during Phase 5**: default 60s max / 1s min.
- **`DCIM/Bugzz/` vs `Pictures/Bugzz/` convention** — not verified. **Handle during Phase 3**: inspect reference runtime; default `DCIM/Bugzz/`.
- **Exact bug-filter count and types** — server-driven, inherent uncertainty. **Handle during Phase 4**: bundle 15–25 matching common bug categories.
- **1€ filter parameter tuning** — ship literature defaults (1.0 / 0.007 / 1.0), tune empirically.
- **Front-camera photo mirror-at-save convention** — inspect reference behavior in Phase 3, match.

---

## Sources

### Primary (HIGH confidence)
- Android Developers official docs — CameraX releases, ML Kit Face Detection guide, `OverlayEffect` / `MlKitAnalyzer` / MediaStore / Compose BOM mapping / AGP 9.0 release notes
- Android Developers Blog — "Introducing CameraX 1.5" (Nov 2025), "What's new in CameraX 1.4.0" (Dec 2024), "Better Device Compatibility with CameraX" (2022)
- Reference APK forensics — `reference/com.insect.filters.funny.prank.bug.filter.face.camera.apk` v1.2.7 + extracted manifest, layouts, drawables, DEX, assets, JSON configs
- Google Maven — verified versions for all major dependencies April 2026
- CameraX developer group threads on OverlayEffect + ML Kit integration, camera lifecycle leaks
- ML Kit GitHub issues #49, #144

### Secondary (MEDIUM confidence)
- Hilt vs Koin comparison (droidcon Nov 2025)
- Google ML Kit samples repo (GraphicOverlay.java)
- ProAndroidDev Oct 2025 article on Compose-native CameraX
- Play Store listings for reference + 3 direct competitors + sister app Fazee
- Uptodown + AppBrain pages
- MediaPipe issue #825 (1€ filter)

### Tertiary (LOW confidence, flagged for validation)
- Industry "15-60s video cap" norm — validate against reference DEX in Phase 5
- "DCIM/Bugzz/" save convention — validate against reference runtime in Phase 3
- "25-60 filters across reference's server-driven catalog" — bundled 15-25 target

---

*Research completed: 2026-04-18*
*Ready for roadmap: yes*
*Contradictions resolved: 3 — UI toolkit → Compose; face tracking → stay on Face Detection contour; 3D engine → 2D Canvas only*
