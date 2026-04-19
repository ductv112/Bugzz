# Phase 2: Camera Preview + Face Detection + Coordinate Validation - Context

**Gathered:** 2026-04-19
**Status:** Ready for planning

<domain>
## Phase Boundary

Validate the architecturally load-bearing `OverlayEffect` + `MlKitAnalyzer(COORDINATE_SYSTEM_SENSOR)` + `getSensorToBufferTransform()` pairing end-to-end on real hardware (Xiaomi 13T) so Phase 3+ can draw production sprites without rewriting the camera pipeline. Scope: live CameraX preview via `CameraXViewfinder`, front/back lens flip, `UseCaseGroup` binding `Preview | ImageAnalysis | ImageCapture | VideoCapture`, ML Kit contour-mode face detection via `MlKitAnalyzer`, `STRATEGY_KEEP_ONLY_LATEST` backpressure, 1€ filter smoothing, debug overlay proving coordinate transform + trackingId stability, and a 5-second debug-only video recording proving three-stream overlay compositing. **Out of scope**: production sprite rendering, filter catalog, filter picker UI, photo capture UX, production video UX, RECORD_AUDIO permission prompts (lazy — deferred to Phase 5), home/splash/onboarding screens.

</domain>

<decisions>
## Implementation Decisions

### Debug Overlay Content (gray area 1)
- **D-01 (amended 2026-04-19 post-GAP-02-B):** Debug overlay draws `face.boundingBox` as a red stroked rectangle (stroke width compensated for `sensorToBufferTransform` matrix scale — target ≈ 4 device pixels regardless of scale factor). Contour-mode ingestion is visually validated by drawing **one centroid dot per populated FaceContour type** (≤ 15 dots per primary face, not ~97 per-point dots); the centroid suffices to confirm each contour region (nose bridge, lips, cheeks, eyes, jawline) was populated without saturating the preview with overlapping dots. Secondary faces draw bounding box + fallback-landmark dots per D-23 / PITFALLS #13. Phase 3 anchors production sprites to the same contour points (full set, not centroid — Phase 3 concern), so this validation still reduces Phase 3 risk: if any contour type's centroid fails to render, D-01's Phase 3 contract is broken. See `DebugOverlayRenderer.companion` (`centroidOf`, `computeSensorSpaceStroke`, `MIN_SAFE_SCALE`) pinned by `DebugOverlayRendererTest`.
- **D-02:** Overlay is debug-only — gated by `BuildConfig.DEBUG`. Release builds (v1.0 does not ship a release build, but the gate is correct) draw nothing; Phase 3 replaces the debug overlay with production `FilterEngine`.
- **D-03:** No FPS counter or trackingId text label in Phase 2 overlay (kept minimal). trackingId stability is verified via logcat output (Timber tag `FaceTracker`) per success criterion #5; FPS is measured via Android Studio profiler per Phase 7.

### Test Video Recording Trigger (gray area 2)
- **D-04:** Debug-only `TEST RECORD` button rendered on `CameraScreen` when `BuildConfig.DEBUG == true`. Tap triggers a 5-second `VideoCapture.output.prepareRecording()` with auto-stop, saves MP4 to `DCIM/Bugzz/` via `MediaStoreOutputOptions`, toasts the resulting content URI on completion. Trigger UX does NOT reuse the future Phase 5 production record button — keeps Phase 2 scope clear and avoids premature RECORD_AUDIO prompt (FND-06 says audio permission is lazy at Phase 5).
- **D-05:** Audio track is NOT enabled on the test recording (`.withAudioEnabled()` is NOT called). Phase 2 only proves the overlay-in-video pipeline; audio muxing + RECORD_AUDIO permission is a Phase 5 concern.
- **D-06:** The `VideoCapture` use case IS bound to the `UseCaseGroup` from the start of Phase 2 — even though only used by the debug test button — because CAM-03 requires all four use cases (Preview + ImageAnalysis + ImageCapture + VideoCapture) to bind under one lifecycle, and the three-stream overlay proof requires VideoCapture to be part of the group the `OverlayEffect` is attached to.

### Orientation Policy (gray area 3)
- **D-07:** App is **portrait-locked** app-wide. `AndroidManifest.xml` declares `android:screenOrientation="portrait"` on `MainActivity` (or `sensorPortrait` if testing reveals device handoff issues). Matches reference app convention and reduces Compose + CameraX rotation edge cases.
- **D-08:** Success criterion #3 ("debug overlay pixel-perfectly wraps face in portrait, landscape, reverse-portrait, reverse-landscape") is validated by rotating the **device** (phone physically turned) while the app UI stays portrait-locked. CameraX receives device rotation via `OrientationEventListener` → `videoCapture.targetRotation = rotation` + `imageCapture.targetRotation = rotation` + `preview.targetRotation = rotation`. The overlay matrix (`getSensorToBufferTransform`) adjusts automatically; the red rect + landmark dots should remain pixel-aligned to the face across all 4 sensor rotations even though the UI does not rotate.
- **D-09:** Validation runbook for Phase 2 handoff instructs the user to rotate the Xiaomi 13T through 4 orientations and confirm overlay alignment on front AND back camera. This directly exercises PITFALLS #1 (coordinate-space chaos) at the earliest possible moment.

### Preview Scale Type (gray area 4)
- **D-10:** `CameraXViewfinder` `ImplementationMode = PERFORMANCE` (the default — `SurfaceView` backend) and `ScaleType = FIT_CENTER`. Black letterbox bars on top/bottom in portrait are accepted as temporary UX tradeoff during Phase 2 validation.
- **D-11:** Revisit `FILL_CENTER` in Phase 6 (UX Polish) to match reference-app UX. If switched, Phase 6 must re-verify overlay alignment with the cropped preview — `OverlayEffect`'s `getSensorToBufferTransform()` already accounts for `PreviewView` scale type, so this should be a visual-only change, but re-verification is mandatory.

### Architecture Placement (derived from locked Phase 1 + research decisions)
- **D-12:** New packages under `com.bugzz.filter.camera.`:
  - `camera/` — `CameraController.kt`, `CameraExecutors.kt` (holds `cameraExecutor`, `renderExecutor` singletons), `CameraLensProvider.kt`
  - `detector/` — `FaceDetectorClient.kt` (MlKitAnalyzer wiring), `FaceSnapshot.kt` (immutable data + `AtomicReference` holder), `OneEuroFilter.kt` (landmark jitter smoother), `FaceLandmarkMapper.kt` (Face → anchor-point helper stubs for Phase 3)
  - `render/` — `OverlayEffectBuilder.kt`, `DebugOverlayRenderer.kt` (Phase 2 debug-only; Phase 3 replaces with `FilterEngine`)
  - `ui/camera/` — replaces Phase 1's `ui/screens/StubScreens.kt` `CameraScreen` stub with a real `CameraScreen.kt` + `CameraViewModel.kt` + `CameraUiState.kt` + `OneShotEvent.kt`
- **D-13:** `CameraController` and `FaceDetectorClient` are Hilt-injected as `@Singleton` via a new `CameraModule` Hilt module under `di/`. `@HiltViewModel class CameraViewModel` consumes them.
- **D-14:** `CameraUiState` minimum fields for Phase 2: `lens: CameraLens = FRONT`, `permissionState: PermissionState`, `isDetectorReady: Boolean`, `lastErrorMessage: String?`. Fields for filter selection, recording state, capture results are Phase 3+ concerns — do NOT prematurely add them here.

### ML Kit Configuration (locked from research; recorded for planner)
- **D-15 (amended 2026-04-19 post-GAP-02-A):** `FaceDetectorOptions.Builder()` = `setPerformanceMode(PERFORMANCE_MODE_FAST)` + `setContourMode(CONTOUR_MODE_ALL)` + `setLandmarkMode(LANDMARK_MODE_NONE)` + `setClassificationMode(CLASSIFICATION_MODE_NONE)` + `setMinFaceSize(0.15f)`. **`.enableTracking()` is intentionally NOT called** — Google ML Kit silently ignores it under `CONTOUR_MODE_ALL` and the detector emits faces with `trackingId == null`. Verified on Xiaomi 13T / HyperOS (GAP-02-A, 459/459 null trackingIds over 20s). Face identity across frames is deferred to Phase 3 via a `BboxIouTracker` (see `02-ADR-01-no-ml-kit-tracking-with-contour.md`). Bugs anchor off contour points (Phase 3); bounding-box center is fallback. Landmarks + classification are NOT enabled (PITFALLS #3: contour + landmarks + classification together degrades accuracy).
- **D-16:** ImageAnalysis target resolution via `ResolutionSelector` with preferred `Size(720, 1280)` (portrait sensor orientation) — not 1080p (PITFALLS #4: CPU overload risk on mid-tier devices). If the selector rejects that on Xiaomi 13T, fall back to nearest supported ≤720p.
- **D-17:** `MlKitAnalyzer` constructor uses `COORDINATE_SYSTEM_SENSOR` (not `VIEW_REFERENCED`). Rationale: `OverlayEffect.onDrawListener`'s `canvas.setMatrix(frame.sensorToBufferTransform)` already converts sensor coordinates to the overlay canvas's buffer space. Using `VIEW_REFERENCED` would introduce a second transform and defeat the zero-manual-matrix-math goal of CAM-07.

### Threading Model (locked from research)
- **D-18:** Three executors: `Main` (UI + `cameraProvider.bindToLifecycle`), `cameraExecutor = Executors.newSingleThreadExecutor()` (MlKitAnalyzer analyze callback, ImageCapture callback, VideoRecordEvent listener), `renderExecutor = Executors.newSingleThreadExecutor()` (OverlayEffect onDrawListener). Both single-thread executors for ordering guarantees with no locks on shared state.
- **D-19:** Cross-thread face-data handoff: `FaceSnapshot` wrapped in `AtomicReference<FaceSnapshot>` held by `DebugOverlayRenderer` (Phase 2) / `FilterEngine` (Phase 3). `cameraExecutor` writes; `renderExecutor` reads-latest on each draw. No `Mutex`, no `synchronized` — intentional.

### 1€ Filter (CAM-09)
- **D-20:** Apply 1€ filter per-landmark (x and y independently) inside `FaceDetectorClient` **after** `MlKitAnalyzer` emits raw Face results, **before** writing to `AtomicReference<FaceSnapshot>`. Parameters: `minCutoff=1.0`, `beta=0.007`, `dCutoff=1.0` (ship literature defaults from STATE.md open questions; tune empirically in Phase 3).
- **D-21:** Validation order: first land raw-landmark rendering (no 1€) to verify coordinate transform is correct (red rect wraps face). THEN insert 1€ filter and verify <1px/frame jitter on still head per success criterion #5. Order matters — don't want smoothing to mask a broken transform.
- **D-22 (amended 2026-04-19 post-GAP-02-A):** 1€ filter state is keyed on a frame-stable face identity. In Phase 2 this falls back to the `id = -1` sentinel because `face.trackingId` is always null under `CONTOUR_MODE_ALL` (see D-15 + ADR-01) — all faces share one smoother bucket, acceptable for the single-face device runbook. In Phase 3, a `BboxIouTracker` provides the stable ID; when a face loses tracking, the corresponding 1€ filter state is cleared; when the same ID reappears, re-initialize (don't carry over stale state).

### Multi-Face Policy for Phase 2
- **D-23:** Draw debug overlay on ALL detected faces (not just primary). ML Kit contour mode only populates contours for the primary face (PITFALLS #13), so secondary faces will show bounding box + a subset of landmarks only — this is acceptable and actually validates the pipeline's graceful degradation, which Phase 3+ relies on. No multi-face crash expected; cover this in the Phase 2 validation runbook.

### Lens Flip
- **D-24:** Simple front/back toggle button in `CameraScreen` top-right corner (Phase 2 utility UI; final button position in Phase 6). Tap handler: `cameraProvider.unbindAll()` then `bindToLifecycle(newLensSelector, useCaseGroup)`. NOT rebinding per capture — only per lens flip, which is rare. Success criterion #1 requires <500ms for flip and no "Camera in use" error on 10 consecutive toggles.
- **D-25:** `OverlayEffect` instance is constructed **once** and re-used on every `bindToLifecycle` call (don't recreate per flip). Same for `MlKitAnalyzer` and the `FaceDetectorClient` instance. ImageAnalysis use case is recreated on flip if necessary (flip toggles the lens, not the detector).

### Permissions
- **D-26:** Phase 2 requires ONLY the CAMERA permission already wired in Phase 1 (FND-06). RECORD_AUDIO is NOT requested in Phase 2 (test recording has audio disabled, see D-05). POST_NOTIFICATIONS remains not-requested.
- **D-27:** If user enters CameraScreen without CAMERA granted, show the Phase 1 rationale + "Open Settings" CTA (already implemented) — don't bind CameraX until permission granted.

### Debug Validation Runbook (Phase 2 Exit Artifact)
- **D-28:** Phase 2 produces a `02-HANDOFF.md` (analogous to Phase 1's `01-04-HANDOFF.md`) with step-by-step adb + device instructions for Xiaomi 13T: install debug APK, grant camera permission, verify red rect wraps face (front cam portrait), verify red rect wraps face (back cam portrait), rotate phone through 4 orientations verifying alignment on each lens, tap TEST RECORD and play back the saved MP4 in Google Photos confirming red rect + landmark dots are baked into every frame, logcat check for `FaceTracker` trackingId stability over 60+ frames.

### Claude's Discretion
- Exact Hilt module wiring (`@Provides` vs `@Binds` choices)
- Exact Compose composable tree structure inside `CameraScreen` (e.g., whether debug overlay is a separate `Composable` drawn in `CameraXViewfinder`'s `onSurfaceRequest` or sits outside — note actual overlay drawing happens in `OverlayEffect.onDrawListener`, NOT in Compose)
- Lifecycle binding site (Activity vs Composable) — but must use `LocalLifecycleOwner.current` and `DisposableEffect` if Composable-bound
- `OrientationEventListener` exact threshold values for emitting target-rotation updates
- Exact Timber tree wiring for `FaceTracker` log tag
- Log level / format for trackingId tracing
- 1€ filter Kotlin implementation source (transliterate reference implementation vs minor adaptations)
- Whether to reuse Phase 1's `ui/screens/StubScreens.kt` `CameraScreen` composable signature or fully replace
- Test scaffold additions (unit tests for `OneEuroFilter`, instrumented smoke test for CameraController lifecycle) — nice-to-have, not blocking
- Exact Timber tag names, log verbosity
- Whether the lens-flip button icon uses Material Icons extended (already available via Compose) or a drawable asset

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Project specs
- `.planning/PROJECT.md` — locked tech decisions (CameraX, ML Kit, Compose, Hilt, minSdk 28)
- `.planning/REQUIREMENTS.md` §Camera & Face Detection — CAM-01..CAM-09 (Phase 2 scope)
- `.planning/ROADMAP.md` §Phase 2 — goal + 5 success criteria
- `.planning/STATE.md` §Key Decisions Locked in Research — locked architectural decisions
- `.planning/phases/01-foundation-skeleton/01-CONTEXT.md` — Phase 1 decisions that constrain Phase 2 (package naming, module structure, DI, permission pattern)
- `.planning/phases/01-foundation-skeleton/01-RESEARCH.md` — Phase 1 research that feeds Phase 2 expectations

### Research (read in full before planning)
- `.planning/research/STACK.md` — prescriptive version catalog (CameraX 1.6.0 family, ML Kit 16.1.7)
- `.planning/research/ARCHITECTURE.md` §2 (data flow — three pipelines, one overlay), §3 (rendering pipeline — OverlayEffect decision), §6 (patterns), §12 (decisions summary)
- `.planning/research/PITFALLS.md` §1 (coordinate-space chaos), §2 (video loses overlay), §3 (landmark jitter / 1€ filter), §4 (ImageAnalysis backpressure), §7 (device fragmentation), §9 (camera lifecycle leak), §13 (multi-face contour bug) — all seven are ACTIVE for Phase 2
- `.planning/research/SUMMARY.md` — resolved UI toolkit, face tracking, 3D engine decisions

### CameraX / ML Kit external docs
- [CameraX 1.6.0 release notes](https://developer.android.com/jetpack/androidx/releases/camera) — camera-effects + camera-compose stable
- [Introducing CameraX 1.5 blog (Nov 2025)](https://android-developers.googleblog.com/2025/11/introducing-camerax-15-powerful-video.html) — effect-baked video compositing
- [OverlayEffect API reference](https://developer.android.com/reference/kotlin/androidx/camera/effects/OverlayEffect) — `setOnDrawListener`, Frame, getSensorToBufferTransform
- [ML Kit Analyzer with CameraX](https://developer.android.com/media/camera/camerax/mlkitanalyzer) — `COORDINATE_SYSTEM_SENSOR` usage
- [ML Kit Face Detection Android guide](https://developers.google.com/ml-kit/vision/face-detection/android) — FaceDetectorOptions, contour vs landmark vs classification
- [CameraX Compose integration (`CameraXViewfinder`)](https://developer.android.com/media/camera/camerax/compose-camerax-use-cases) — Compose-native preview
- [CameraX rotation guide](https://developer.android.com/training/camerax/orientation-rotation) — targetRotation + OrientationEventListener pattern

### 1€ Filter reference
- [Géry Casiez — 1€ Filter paper + JS reference implementation](https://gery.casiez.net/1euro/) — parameter tuning guidance
- [OneEuro Filter walkthrough](https://mohamedalirashad.github.io/FreeFaceMoCap/2021-12-25-filters-for-stability/) — practical tuning

### Reference APK (behavioral reference only)
- `reference/com.insect.filters.funny.prank.bug.filter.face.camera.apk` — visual/UX ground truth for preview scale type and orientation, inspect if ambiguity arises

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets (from Phase 1)
- `com.bugzz.filter.camera.BugzzApplication` (`@HiltAndroidApp`) — Hilt root; add `CameraModule` as a new `@InstallIn(SingletonComponent::class)` module
- `com.bugzz.filter.camera.MainActivity` (`@AndroidEntryPoint`) — host activity; orientation lock goes in manifest entry for this activity
- `com.bugzz.filter.camera.ui.BugzzApp` + `com.bugzz.filter.camera.ui.nav.Routes` — existing navigation shell; `CameraRoute` composable to be replaced (currently stub with permission gating)
- `com.bugzz.filter.camera.ui.screens.StubScreens.kt` — contains Phase 1 `CameraScreen(granted)` stub; Phase 2 replaces the body with a real camera composable but the route wiring stays
- `com.bugzz.filter.camera.di.AppModule` — empty placeholder; can be reused or left alone (Phase 2 adds a new `CameraModule`)
- Version catalog at `gradle/libs.versions.toml` — ADD new entries for `cameraX = "1.6.0"`, `mlkitFaceDetection = "16.1.7"`, `timber = "5.0.1"` (per research), `coilCompose = "2.7.0"` (optional — deferred to Phase 4 if not used in Phase 2)

### Established Patterns (from Phase 1)
- **Hilt + KSP** (not kapt) — Phase 2 CameraModule uses KSP codegen too
- **Type-safe nav** via `@Serializable` route objects — Phase 2 does not add new routes; reuses `CameraRoute`
- **`ActivityResultContracts.RequestPermission`** for runtime permissions — reuse existing CAMERA contract; do NOT introduce RECORD_AUDIO contract in Phase 2 (D-05)
- **StrictMode + LeakCanary in debug** — already active; Phase 2 will hit StrictMode violations if Bitmap decode or MediaStore insert happens on main thread (don't)
- **JDK 21** (Android Studio `jbr`) via `gradle.properties` `org.gradle.java.home` — no change
- Unit tests in `src/test/` (JUnit 4) + instrumented in `src/androidTest/` (androidx.test.ext:junit + espresso-core)

### Integration Points
- `AndroidManifest.xml` CAMERA permission already declared; Phase 2 adds `android:screenOrientation="portrait"` to `MainActivity` entry, and `android:configChanges` if we want to suppress activity recreation on sensor orientation change (consider — depends on whether Compose + CameraX react cleanly to configuration change when portrait-locked; test on Xiaomi 13T)
- `local.properties` with `sdk.dir` already present
- LeakCanary active — will catch any `CameraActivity`/`MainActivity` retention from mis-bound CameraX lifecycle
- Package root `com.bugzz.filter.camera` established; Phase 2 adds sibling packages `camera/`, `detector/`, `render/` (per D-12)

</code_context>

<specifics>
## Specific Ideas

- User will verify Phase 2 on **Xiaomi 13T** via USB ADB (locked from memory + Phase 1 handoff precedent). Same device used for Phase 1 FND-08.
- User explicitly prefers stop-test per phase (no multi-phase auto-chain execution) — `--chain` on this invocation runs discuss → plan → execute for Phase 2 alone; user will manually verify the on-device runbook before advancing to Phase 3.
- Phase 2 is the **risk-front-loaded** phase per ROADMAP §Key Decisions — it exists specifically to validate PITFALLS #1 (coord space) and #2 (video overlay) before Phase 3 draws production sprites. Every decision above is made to maximize validation signal, NOT to minimize Phase 2 code volume.
- Xiaomi 13T runs MIUI/HyperOS which has historically caused camera HAL quirks (PITFALLS #7). If any pixel-alignment issue surfaces that CameraX Quirks doesn't auto-compensate, the finding goes in `02-HANDOFF.md` and becomes a Phase 7 cross-OEM matrix item — do NOT introduce OEM-specific workarounds in Phase 2.

</specifics>

<deferred>
## Deferred Ideas

- **FPS counter / trackingId text label / detection latency readout**: deferred to Phase 7 (Performance & Device Matrix) — Android Studio profiler gives better data than an in-app HUD; full HUD in Phase 2 would be clutter.
- **FILL_CENTER preview scale type**: deferred to Phase 6 (UX Polish); FIT_CENTER preserved for validation clarity in Phase 2.
- **Production record button with RECORD_AUDIO prompt and 60s cap**: Phase 5 (`VID-01..10`). Phase 2's TEST RECORD is debug-only, no audio, 5s fixed.
- **Settings / developer-flag toggles for detection mode, overlay opacity, etc.**: Phase 6 scope, and even then only if actually useful. Not in Phase 2.
- **Multi-face contour workaround (drop to ACCURATE mode or custom fallback)**: deferred; Phase 2 accepts primary-face-only contour limitation per PITFALLS #13. Design Phase 3 sprite anchoring to fall back to boundingBox center for secondary faces.
- **Lens-flip icon polish / Material Icons extended / custom drawable**: Phase 6 (UX Polish).
- **Compose CameraX test harness using `androidx.compose.ui:ui-test-junit4`**: still deferred per Phase 1 D-21.
- **OEM-specific (Xiaomi MIUI) camera HAL workarounds**: document findings in Phase 2 handoff; actual compensation goes in Phase 7.
- **Thermal listener (`PowerManager.addThermalStatusListener`)**: Phase 5 scope (`VID-08`) — Phase 2 brief sessions don't thermally saturate.
- **`WRITE_EXTERNAL_STORAGE` (`android:maxSdkVersion="28"`)**: explicitly not needed since minSdk=28 overlaps with Android 9 exactly and MediaStore works from API 29; deferred decision is to leave it OUT unless Xiaomi 13T MIUI behaves unexpectedly (it won't).

</deferred>

---

*Phase: 02-camera-preview-face-detection-coordinate-validation*
*Context gathered: 2026-04-19*
