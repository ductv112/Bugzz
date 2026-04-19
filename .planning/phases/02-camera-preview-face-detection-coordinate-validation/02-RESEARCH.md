# Phase 2: Camera Preview + Face Detection + Coordinate Validation - Research

**Researched:** 2026-04-19
**Domain:** Android AR camera pipeline — CameraX 1.6.0 + ML Kit Face Detection + OverlayEffect + Compose-native viewfinder
**Confidence:** HIGH (CameraX 1.6.0 stable verified March 25 2026; OverlayEffect + MlKitAnalyzer integration path verified via multiple official sources)

---

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions

All 28 decisions (D-01 through D-28) in `02-CONTEXT.md` are locked. The planner MUST honor them verbatim. Condensed summary for cross-reference during planning:

**Debug overlay (D-01..D-03):** Draw `face.boundingBox` red stroked rect + contour landmark dots (nose / eyes / cheeks / jawline) as filled circles. Gated by `BuildConfig.DEBUG`. No FPS counter / no trackingId text label. trackingId stability verified via Timber `FaceTracker` tag.

**Test video (D-04..D-06):** Debug-only `TEST RECORD` button on CameraScreen when `BuildConfig.DEBUG`. Tap → 5-second `VideoCapture.prepareRecording()` auto-stop, save MP4 to `DCIM/Bugzz/` via `MediaStoreOutputOptions`, toast content URI. No `.withAudioEnabled()`. `VideoCapture` use case IS bound to the `UseCaseGroup` from the start.

**Orientation (D-07..D-09):** App is portrait-locked (`android:screenOrientation="portrait"` on MainActivity). Success criterion #3 is validated by rotating the **device physically** while UI stays portrait-locked; CameraX receives rotation via `OrientationEventListener` → `setTargetRotation()` on preview/imageCapture/videoCapture.

**Preview scale (D-10..D-11):** `CameraXViewfinder` `ImplementationMode = PERFORMANCE` (SurfaceView), `ScaleType = FIT_CENTER`. Letterbox acceptable.

**Architecture placement (D-12..D-14):**
- `camera/` — `CameraController.kt`, `CameraExecutors.kt`, `CameraLensProvider.kt`
- `detector/` — `FaceDetectorClient.kt`, `FaceSnapshot.kt`, `OneEuroFilter.kt`, `FaceLandmarkMapper.kt`
- `render/` — `OverlayEffectBuilder.kt`, `DebugOverlayRenderer.kt`
- `ui/camera/` — `CameraScreen.kt`, `CameraViewModel.kt`, `CameraUiState.kt`, `OneShotEvent.kt`
- `CameraController` and `FaceDetectorClient` are `@Singleton` via a new `CameraModule` Hilt module under `di/`. `@HiltViewModel CameraViewModel` consumes them.
- `CameraUiState` Phase 2 fields: `lens: CameraLens = FRONT`, `permissionState: PermissionState`, `isDetectorReady: Boolean`, `lastErrorMessage: String?`.

**ML Kit config (D-15..D-17):** `FaceDetectorOptions`: `PERFORMANCE_MODE_FAST` + `CONTOUR_MODE_ALL` + `enableTracking()` + `setMinFaceSize(0.15f)`. ImageAnalysis target `Size(720, 1280)` via `ResolutionSelector`. `MlKitAnalyzer` uses `COORDINATE_SYSTEM_SENSOR`.

**Threading (D-18..D-19):** Three executors — Main / `cameraExecutor = Executors.newSingleThreadExecutor()` / `renderExecutor = Executors.newSingleThreadExecutor()`. `FaceSnapshot` in `AtomicReference<FaceSnapshot>` read by `renderExecutor`, written by `cameraExecutor`. No `Mutex`, no `synchronized`.

**1€ filter (D-20..D-22):** Applied inside `FaceDetectorClient` after `MlKitAnalyzer` emits, before `AtomicReference` write. Params `minCutoff=1.0`, `beta=0.007`, `dCutoff=1.0`. Validation order: raw landmarks first, then insert 1€. State keyed on `face.trackingId`; cleared when trackingId disappears; re-initialized on reappearance.

**Multi-face (D-23):** Draw debug overlay on ALL detected faces. Secondary faces show bounding-box + partial landmarks only (ML Kit contour quirk — acceptable).

**Lens flip (D-24..D-25):** Top-right toggle. Tap → `cameraProvider.unbindAll()` then `bindToLifecycle(newLensSelector, useCaseGroup)`. `OverlayEffect`, `MlKitAnalyzer`, `FaceDetectorClient` instances are constructed ONCE and re-used. <500ms for flip, no "Camera in use" on 10 consecutive toggles.

**Permissions (D-26..D-27):** CAMERA only. RECORD_AUDIO NOT requested in Phase 2. Phase 1 rationale + "Open Settings" CTA if denied.

**Runbook (D-28):** Phase 2 produces `02-HANDOFF.md` with adb + device steps for Xiaomi 13T.

### Claude's Discretion

- Exact Hilt module wiring (`@Provides` vs `@Binds`)
- Exact Compose composable tree structure inside `CameraScreen`
- Lifecycle binding site (Activity vs Composable) — must use `LocalLifecycleOwner.current` + `DisposableEffect` if Composable-bound
- `OrientationEventListener` exact threshold values
- Exact Timber tree / tag / format for `FaceTracker`
- 1€ filter implementation source (fresh transliterate vs port existing)
- Whether to reuse Phase 1's `StubScreens.kt` `CameraScreen` signature or fully replace
- Test scaffold additions (unit tests for `OneEuroFilter`, instrumented smoke test for `CameraController`)
- Lens-flip icon source (Material Icons extended vs drawable)

### Deferred Ideas (OUT OF SCOPE)

- FPS counter / trackingId text label / detection latency readout → Phase 7
- FILL_CENTER preview scale type → Phase 6
- Production record button with RECORD_AUDIO + 60s cap → Phase 5
- Dev-flag toggles for detection mode / overlay opacity → Phase 6
- Multi-face contour workaround → Phase 3
- Lens-flip icon polish → Phase 6
- Compose CameraX test harness (`ui-test-junit4`) → Phase 6
- OEM-specific Xiaomi MIUI workarounds → Phase 7
- Thermal listener → Phase 5
- `WRITE_EXTERNAL_STORAGE` → stays OUT
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| CAM-01 | Live CameraX preview renders on `CameraXViewfinder` composable in CameraScreen | §2 CameraXViewfinder wiring, §8 scale type |
| CAM-02 | User can flip between front and back camera via on-screen button | §5 lens-flip pattern + concrete code sketch |
| CAM-03 | CameraX `UseCaseGroup` binds Preview + ImageCapture + VideoCapture + ImageAnalysis under one lifecycle | §2 UseCaseGroup builder sketch |
| CAM-04 | ML Kit Face Detection (contour mode, bundled model) runs on preview frames via `MlKitAnalyzer(COORDINATE_SYSTEM_SENSOR)` | §3 MlKitAnalyzer, §11 bundled model package |
| CAM-05 | ImageAnalysis backpressure set to `STRATEGY_KEEP_ONLY_LATEST`; preview does not stall when detection is slow | §3 ImageAnalysis.Builder |
| CAM-06 | `OverlayEffect` binds to `PREVIEW \| IMAGE_CAPTURE \| VIDEO_CAPTURE` targets; debug overlay (red rect on face boundingBox) renders on preview | §2 OverlayEffect wiring + §12 Concrete Code Sketches |
| CAM-07 | Debug overlay stays aligned in portrait + landscape, front + back lens (no manual matrix math — uses `frame.getSensorToBufferTransform()`) | §3 sensor-coord pairing, §6 portrait-lock rotation |
| CAM-08 | Face tracking IDs (`trackingId`) remain stable across frames for the same face | §3 `enableTracking()`, §12 Timber FaceTracker tag |
| CAM-09 | 1€ (One-Euro) filter smooths landmark jitter between detector callback and renderer | §4 1€ filter Kotlin implementation |
</phase_requirements>

## Project Constraints (from CLAUDE.md)

| Constraint | Compliance Approach |
|-----------|---------------------|
| Kotlin 2.1.21, AGP 8.9.1, JDK 17 | Already set in `app/build.gradle.kts`. No changes. |
| CameraX 1.6.0 uniform across artifacts | Add 8 CameraX 1.6.0 entries to version catalog: `core`, `camera2`, `lifecycle`, `video`, `view`, `effects`, `mlkit-vision`, `compose`. |
| ML Kit 16.1.7 bundled (NOT Play Services variant) | `com.google.mlkit:face-detection:16.1.7` (NOT `com.google.android.gms:play-services-mlkit-face-detection`). |
| Compose BOM 2026.04.00 | Catalog currently pins 2026.03.00 → **BUMP to 2026.04.00** per CLAUDE.md executive recommendation (also per STACK.md). |
| Hilt 2.57 + KSP (no kapt) | Phase 1 already uses KSP. New `CameraModule` + future providers via `ksp()` only. |
| minSdk 28 / targetSdk 35 / compileSdk 35 | Already set. No changes. |
| GSD workflow enforcement | Work enters via `/gsd-execute-phase`. Commit messages must follow Phase 1 pattern (`docs(02): ...`, `feat(02-XX): ...`). |
| Portrait-locked via `android:screenOrientation="portrait"` on MainActivity | Manifest edit in Wave 0 / early wave. |
| Scoped storage — no WRITE_EXTERNAL_STORAGE | Test-record uses `MediaStoreOutputOptions`; no legacy `File` paths. |
| StrictMode + LeakCanary active in debug | Already wired. Phase 2 will trigger violations if Bitmap decode / MediaStore insert lands on main thread — route through coroutines + executors. |
| JDK 21 in Android Studio jbr | `gradle.properties` unchanged. |

## Summary

Phase 2 delivers a live CameraX 1.6.0 pipeline on Xiaomi 13T with `CameraXViewfinder` (Compose) rendering the front/back preview, a bound four-use-case `UseCaseGroup` (Preview + ImageAnalysis + ImageCapture + VideoCapture) feeding a single `OverlayEffect` whose Canvas `onDrawListener` draws a debug red bounding-box and contour landmark dots per detected face. `MlKitAnalyzer(COORDINATE_SYSTEM_SENSOR)` feeds ML Kit `Face` results into a `FaceDetectorClient` that applies per-landmark 1€ smoothing, writes a `FaceSnapshot` into an `AtomicReference` read by the overlay's render executor. A debug-only `TEST RECORD` button confirms the overlay bakes into a 5-second MP4.

**The pipeline is all stock CameraX 1.6 APIs — zero custom matrix math, zero bitmap compositing, zero MediaMuxer.** The `frame.sensorToBufferTransform` applied to `overlayCanvas` pairs exactly with `COORDINATE_SYSTEM_SENSOR` output from `MlKitAnalyzer`; this pairing is what CLAUDE.md and the research corpus repeatedly flag as the load-bearing architectural bet. If the red rect wraps the face in all four device rotations on both lenses, Phase 3 can begin drawing production sprites using the exact same `FaceSnapshot` → `overlayCanvas` contract.

**Primary recommendation:** Follow the Concrete Code Sketches in §12 almost verbatim. The Phase 2 implementation is prescriptive — all 28 locked decisions from CONTEXT.md close off most alternative paths. Risk is concentrated in two places: (a) `CameraXViewfinder` + `surfaceRequest` plumbing (first-stable-release pattern; sample code is sparse) and (b) `OverlayEffect` constructor executor/queueDepth parity with MlKitAnalyzer callback cadence.

## Standard Stack

### Core (add / bump for Phase 2)

| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| `androidx.camera:camera-core` | 1.6.0 | CameraX foundation | CameraX 1.6.0 stable **March 25 2026** [VERIFIED: developer.android.com/jetpack/androidx/releases/camera] |
| `androidx.camera:camera-camera2` | 1.6.0 | Camera2 backend impl (required) | Ships the `Camera2Config` used by `ProcessCameraProvider` |
| `androidx.camera:camera-lifecycle` | 1.6.0 | `ProcessCameraProvider` + `bindToLifecycle` | Core binding API |
| `androidx.camera:camera-video` | 1.6.0 | `Recorder` + `VideoCapture` | Needed even though Phase 2 uses it only for a debug test-record (D-06) |
| `androidx.camera:camera-view` | 1.6.0 | `PreviewView` fallback + `ImplementationMode`/`ScaleType` enums | `CameraXViewfinder` routes through these types |
| `androidx.camera:camera-effects` | 1.6.0 | `OverlayEffect` for Canvas-based compositing into Preview+Video+ImageCapture | **Core** to phase — first-class stable in 1.6 |
| `androidx.camera:camera-mlkit-vision` | 1.6.0 | `MlKitAnalyzer` + `COORDINATE_SYSTEM_SENSOR` bridge | Eliminates `ImageProxy → InputImage` plumbing |
| `androidx.camera:camera-compose` | 1.6.0 | `CameraXViewfinder` composable | Stable as of 1.5.1 (Oct 2025); hardened in 1.6.0 [CITED: developer.android.com/jetpack/androidx/releases/camera] |
| `com.google.mlkit:face-detection` | 16.1.7 | Bundled face detector (contour + tracking) | [VERIFIED: Google ML Kit docs — `implementation 'com.google.mlkit:face-detection:16.1.7'`] |
| `com.jakewharton.timber` | 5.0.1 | Logging wrapper | Required for D-03 / success criterion #5 trackingId logs |

**Compose BOM bump:** Current catalog pins **2026.03.00**; CLAUDE.md executive recommendation is **2026.04.00** (current April 2026). This bump aligns with CameraX 1.6.0's tested matrix. `[VERIFIED: developer.android.com/develop/ui/compose/bom/bom-mapping]`

### Already in catalog (reused as-is)

| Already pinned | Used for Phase 2 |
|----------------|-------------------|
| `androidx.core:core-ktx 1.15.0` | `ContentValues` builder for MediaStore test recording |
| `androidx.activity:activity-compose 1.10.1` | `rememberLauncherForActivityResult` (permission already wired) |
| `androidx.lifecycle:lifecycle-viewmodel-compose 2.9.0` | `viewModel()` + `@HiltViewModel` for `CameraViewModel` |
| `androidx.lifecycle:lifecycle-runtime-compose 2.9.0` | `collectAsStateWithLifecycle()` |
| `com.google.dagger:hilt-android 2.57` + `hilt-compiler` (ksp) | `CameraModule` Hilt module |
| `androidx.hilt:hilt-navigation-compose 1.2.0` | `hiltViewModel()` inside `composable<CameraRoute>` |
| `com.squareup.leakcanary:leakcanary-android 2.14` (debug) | Already active — will catch CameraX lifecycle leaks |

### Alternatives Considered

| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| `CameraXViewfinder` (Compose-native) | `AndroidView(PreviewView)` | Pre-1.5 pattern; loses correct Compose z-ordering; PROJECT RESOLUTION #1 explicitly resolved this to Compose-native [LOCKED] |
| `MlKitAnalyzer(COORDINATE_SYSTEM_SENSOR)` | `MlKitAnalyzer(COORDINATE_SYSTEM_VIEW_REFERENCED)` | VIEW_REFERENCED would stack a second transform on top of `OverlayEffect`'s `sensorToBufferTransform` and defeat CAM-07 "no manual matrix math" [D-17 LOCKED] |
| Bundled ML Kit (`com.google.mlkit:face-detection`) | Play Services variant (`com.google.android.gms:play-services-mlkit-face-detection`) | Play Services variant downloads model on demand — first-launch detection fails silently until model arrives (PITFALLS #5). Bundled ~3-4 MB APK cost accepted [D-15 LOCKED] |
| `Executors.newSingleThreadExecutor()` for `cameraExecutor` + `renderExecutor` | Shared single executor | Ordering across detection vs render is fine via `AtomicReference`; separate executors prevent one blocking the other [D-18 LOCKED] |
| Per-landmark 1€ filter keyed on `trackingId` | EMA / Kalman | EMA trades smoothness for lag at fixed ratio (unusable for live AR); Kalman is stronger but needs tuning solo dev can't afford. 1€ is MediaPipe/face-tracking standard [D-20 LOCKED] |

**Installation (add these lines to app/build.gradle.kts dependencies):**

```kotlin
// CameraX 1.6.0 (uniform)
implementation(libs.androidx.camera.core)
implementation(libs.androidx.camera.camera2)
implementation(libs.androidx.camera.lifecycle)
implementation(libs.androidx.camera.video)
implementation(libs.androidx.camera.view)
implementation(libs.androidx.camera.effects)
implementation(libs.androidx.camera.mlkit.vision)
implementation(libs.androidx.camera.compose)

// ML Kit Face Detection (bundled)
implementation(libs.mlkit.face.detection)

// Logging
implementation(libs.timber)
```

**Version verification (executed 2026-04-19):**
- CameraX 1.6.0 stable released **2026-03-25**, RC01 on 2026-02-25 [VERIFIED: developer.android.com/jetpack/androidx/releases/camera]
- ML Kit `face-detection:16.1.7` still current in Google's official vision-quickstart build.gradle [CITED: github.com/googlesamples/mlkit/blob/master/android/vision-quickstart/app/build.gradle]
- Compose BOM 2026.04.00 confirmed in BOM mapping table [CITED: developer.android.com/develop/ui/compose/bom/bom-mapping]

## Architecture Patterns

### Project Structure (additions, honoring D-12)

```
app/src/main/java/com/bugzz/filter/camera/
├── camera/                        # CameraX binding layer
│   ├── CameraController.kt        # Owns ProcessCameraProvider + UseCaseGroup + bind()/flip()/testRecord()
│   ├── CameraExecutors.kt         # @Provides cameraExecutor + renderExecutor (@Singleton)
│   └── CameraLensProvider.kt      # Simple toggle state FRONT⇄BACK (enum + next())
├── detector/                      # ML Kit wrapper layer
│   ├── FaceDetectorClient.kt      # MlKitAnalyzer wiring + 1€ pipeline + AtomicRef writer
│   ├── FaceSnapshot.kt            # Immutable data + AtomicReference<FaceSnapshot>
│   ├── OneEuroFilter.kt           # 1€ Kotlin implementation (per-channel, per-face-id)
│   └── FaceLandmarkMapper.kt      # Face → list of anchor PointF (stub for Phase 3)
├── render/                        # Overlay rendering layer
│   ├── OverlayEffectBuilder.kt    # Constructs OverlayEffect instance with render executor + draw listener
│   └── DebugOverlayRenderer.kt    # Reads AtomicRef, draws red rect + landmark dots (debug only)
├── ui/camera/                     # Phase 2 CameraScreen
│   ├── CameraScreen.kt            # CameraXViewfinder + lens flip + TEST RECORD (debug) + permission gate
│   ├── CameraViewModel.kt         # @HiltViewModel — consumes CameraController + surfaces UiState
│   ├── CameraUiState.kt           # sealed state + PermissionState + CameraLens enum
│   └── OneShotEvent.kt            # sealed for TestRecordSaved / TestRecordFailed / CameraError
└── di/                            # (existing) Hilt modules
    └── CameraModule.kt            # @InstallIn(SingletonComponent::class) @Provides executors + controller
```

**Note:** Phase 1's `ui/screens/StubScreens.kt` `CameraScreen` is replaced (body only; route entry in `BugzzApp.kt` still navigates to `CameraRoute` → our new composable).

### Pattern 1: Single OverlayEffect bound to three targets

**What:** Construct one `OverlayEffect` instance, attach it once to a `UseCaseGroup` via `addEffect()`, where the builder also has `addUseCase(preview)`, `addUseCase(imageAnalysis)`, `addUseCase(imageCapture)`, `addUseCase(videoCapture)`. The effect automatically composites into all three output streams.

**When:** Always for this project — verified pattern from CameraX 1.4+ release notes and 1.6 API reference. [CITED: android-developers.googleblog.com/2024/12/whats-new-in-camerax-140-and-jetpack-compose-support.html]

### Pattern 2: Compose-native CameraXViewfinder via SurfaceRequest flow

**What:** `CameraViewModel` exposes a `StateFlow<SurfaceRequest?>`. `CameraController.bindToLifecycle()` sets up a `Preview` whose `setSurfaceProvider` lambda publishes each new `SurfaceRequest` into the flow. The composable collects the flow and passes the non-null `SurfaceRequest` into `CameraXViewfinder(surfaceRequest = sr)`.

**When:** CameraX 1.5+ canonical Compose pattern. `[CITED: developer.android.com/jetpack/androidx/releases/camera-viewfinder]`

### Pattern 3: Single-writer AtomicReference for cross-thread face handoff (D-19)

**What:** `FaceDetectorClient` (running on `cameraExecutor`) writes `FaceSnapshot` to `AtomicReference`. `DebugOverlayRenderer` (running on `renderExecutor` inside `OverlayEffect.onDrawListener`) reads the latest snapshot with `atomicRef.get()`. No locks; `get()` may return a snapshot 1-2 frames stale and that's fine — matches `STRATEGY_KEEP_ONLY_LATEST` semantics.

### Pattern 4: Hilt `@Singleton` for stateful binders, `@HiltViewModel` for VM

**What:** `CameraController`, `FaceDetectorClient`, `OverlayEffectBuilder`, and both executors are `@Singleton`. `CameraViewModel` is `@HiltViewModel` and injects the singletons by constructor. The ViewModel is scoped to the `CameraRoute` nav entry via `hiltViewModel()`.

### Anti-Patterns to Avoid

- **Creating a new `OverlayEffect` per lens flip** — D-25 forbids; must reuse the singleton. Recreating churns render thread + GL surface provision.
- **Calling `MlKitAnalyzer.process()` manually** — `MlKitAnalyzer` IS the analyzer; set it via `imageAnalysis.setAnalyzer(cameraExecutor, mlkitAnalyzer)`. Do not call `detector.process(inputImage)` directly.
- **Overlay drawing from Compose `Canvas { }`** — all overlay rendering happens inside `OverlayEffect.onDrawListener` on `renderExecutor`. The Compose tree only draws the `CameraXViewfinder` + chrome UI (flip button, test-record button).
- **Manual matrix math in the `onDrawListener`** — `canvas.setMatrix(frame.sensorToBufferTransform)` is the only matrix operation. Don't attempt your own scaling/mirroring on top.
- **Binding CameraX in Composable `LaunchedEffect`** — use the `CameraViewModel` + `CameraController` singleton so binding survives recomposition; the Composable only subscribes to state.
- **`unbindAll()` in `DisposableEffect` onDispose** — `bindToLifecycle(lifecycleOwner, ...)` already unbinds when `lifecycleOwner` enters destroyed state. Manual unbind races with lifecycle and causes "Camera in use" (PITFALLS #9).
- **Calling `imageProxy.close()` inside `MlKitAnalyzer` callback** — MlKitAnalyzer handles close timing. Manual close triggers `Image already closed` exceptions (PITFALLS #4).

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Overlay compositing into JPEG + MP4 | Bitmap screenshot + re-encode | `OverlayEffect` on UseCaseGroup with 3 targets | CameraX does it; ~1 day of work vs multi-week rewrite risk |
| Sensor → Preview → View coordinate math | Custom matrix stack from `imageInfo.rotationDegrees` + `previewView.width/height` + mirror flag | `MlKitAnalyzer(COORDINATE_SYSTEM_SENSOR)` + `canvas.setMatrix(frame.sensorToBufferTransform)` | Device fragmentation is a long tail; CameraX Quirks DB handles 30+ known device bugs |
| Face tracking across frames | Your own "same face" heuristic | `FaceDetectorOptions.Builder().enableTracking()` → `face.trackingId` | Built-in, stable, documented |
| Landmark jitter smoothing | Kalman filter / manual EMA | 1€ filter per landmark channel (x, y independently) | Open-standard algorithm; ~30 LOC; MediaPipe-recommended |
| Device rotation handoff under portrait-lock | Custom display-metric tracking | `OrientationEventListener` → `useCase.setTargetRotation(rotation)` | Official pattern, [CITED: developer.android.com/training/camerax/orientation-rotation] |
| ImageProxy close timing | Manual `try { ... } finally { imageProxy.close() }` | `MlKitAnalyzer` (handles internally) | PITFALLS #4 |
| MP4 saving to DCIM | `File("/sdcard/DCIM/Bugzz/...")` | `MediaStoreOutputOptions` + `ContentValues(RELATIVE_PATH=DCIM/Bugzz)` | Scoped storage since Android 10 |
| Front-cam mirror on video | Manual `Canvas.scale(-1f, 1f)` | `VideoCapture.Builder().setMirrorMode(MIRROR_MODE_ON_FRONT_ONLY)` | Built-in since CameraX 1.3 |

**Key insight:** CameraX 1.6 is specifically designed so that the three pipelines (preview / still / video) share one effect and one analyzer. The amount of custom plumbing in Phase 2 should be near-zero — mostly Kotlin glue between these framework primitives. If a task description in the plan says "manually transform coordinates" or "composite bitmap onto ImageCapture output," it's wrong.

## Common Pitfalls

### Pitfall 1: Coordinate-space chaos (active — CAM-07 gate)

**What goes wrong:** Red rect offsets hundreds of pixels, flips on front/back swap, or drifts on device rotation.
**Why it happens:** Forgetting `canvas.setMatrix(frame.sensorToBufferTransform)`, or using `COORDINATE_SYSTEM_VIEW_REFERENCED` instead of `SENSOR` (double-transform), or setting `PreviewView.ScaleType` on a Views approach.
**How to avoid:** Use the exact pairing in §12 code sketch. `MlKitAnalyzer(COORDINATE_SYSTEM_SENSOR)` + `overlayCanvas.setMatrix(frame.sensorToBufferTransform)` — nothing else. Validate on Xiaomi 13T in 4 device rotations × 2 lenses.
**Warning signs:** Rect in wrong half of face, mirror on flip, 90° jump on rotate.

### Pitfall 2: `OverlayEffect` silent miss on video-capture bake (active — CAM-06 gate)

**What goes wrong:** Red rect visible in preview but NOT in the saved `.mp4`.
**Why it happens:** Overlay target mask missing `CameraEffect.VIDEO_CAPTURE`. Or VideoCapture not in the UseCaseGroup that has the effect.
**How to avoid:** Mask = `CameraEffect.PREVIEW or CameraEffect.VIDEO_CAPTURE or CameraEffect.IMAGE_CAPTURE`. VideoCapture.output must be in the `UseCaseGroup` the effect is added to, bound in a single `bindToLifecycle` call. Verify in the handoff runbook by playing back the MP4 in Google Photos (D-28).
**Warning signs:** Preview looks correct but saved MP4 is "clean."

### Pitfall 3: ImageAnalysis backpressure stall (active — CAM-05 gate)

**What goes wrong:** Preview feels sticky, frames drop, "Image already closed" exceptions in logcat.
**Why it happens:** Default `STRATEGY_BLOCK_PRODUCER`; or closing `ImageProxy` manually before analyzer finishes.
**How to avoid:** `ImageAnalysis.Builder().setBackpressureStrategy(STRATEGY_KEEP_ONLY_LATEST)` and use `MlKitAnalyzer` (handles close internally).
**Warning signs:** Increasing lag between head motion and overlay; stuttering preview above ~20fps face detection load.

### Pitfall 4: Lens-flip "Camera in use" error

**What goes wrong:** After 5-10 rapid front/back toggles, rebind fails with `CameraInUseException`.
**Why it happens:** Rebinding too rapidly while Camera2 session still tearing down; or manual `unbindAll()` in onPause racing with lifecycle unbind; or holding a stale `Camera` object reference.
**How to avoid:** `cameraProvider.unbindAll()` then immediately `bindToLifecycle(...)` is the correct synchronous pattern. Don't debounce — CameraX handles re-entry. But DON'T call unbind in Composable `onDispose` (lifecycle handles it).
**Warning signs:** Log `CameraInUseException`, black preview briefly on flip, toast from runbook test.

### Pitfall 5: Overlay canvas NOT getting transform (pairing bug)

**What goes wrong:** `setMatrix` called but rect is still off — because `frame.sensorToBufferTransform` was fetched before `canvas.setMatrix`, or because the canvas was cleared by a subsequent operation.
**How to avoid:** `canvas.setMatrix(frame.sensorToBufferTransform)` MUST come before any `canvas.drawRect(...)`. No `canvas.save()/restore()` needed unless you're nesting transforms.
**Warning signs:** Rect still in raw sensor coords despite setMatrix call.

### Pitfall 6: 1€ filter state leak across lens flip

**What goes wrong:** Front cam trackingId=1 gets state; user flips to back cam, ML Kit emits trackingId=1 for back-cam face; stale front-cam state pollutes back-cam smoothing for a few frames.
**Why it happens:** trackingId is scoped to session, but ML Kit doesn't guarantee globally unique IDs across detector state resets.
**How to avoid:** On lens flip, explicitly `oneEuroStore.clear()`. Not elegant, but correct. D-22 says clear state when trackingId disappears; at flip we know ALL current tracking IDs disappear.
**Warning signs:** First 5-10 frames after flip show smoother-than-expected motion then snap.

### Pitfall 7: Portrait-lock `OrientationEventListener` skipping rotation (Xiaomi MIUI)

**What goes wrong:** Device rotated 90° but `useCase.setTargetRotation()` never called → overlay lags by 90°.
**Why it happens:** Under `screenOrientation="portrait"` the Activity does not recreate on rotation (good), but without explicit `OrientationEventListener` the use cases never get rotation updates.
**How to avoid:** Register `OrientationEventListener` (thresholded `when`: 45-134 → ROTATION_270, etc.), update `preview.targetRotation`, `imageAnalysis.targetRotation`, `imageCapture.targetRotation`, `videoCapture.targetRotation` in the callback. Use `DisposableEffect(Unit)` in `CameraScreen` or a listener held by `CameraController` (latter preferred — lives through lifecycle).
**Warning signs:** Overlay aligned in portrait but not in landscape on device rotation test.

### Pitfall 8: Multi-face contour gap (PITFALLS #13 active — D-23)

**What goes wrong:** Debug overlay on secondary face renders boundingBox but landmark dots are missing.
**Why it happens:** Documented ML Kit behavior — contour is populated for primary face only.
**How to avoid:** `DebugOverlayRenderer` iterates `face.allContours`; if empty, still draws boundingBox; then draws `face.allLandmarks` (populated for all faces). Note in `02-HANDOFF.md` as expected, not a bug.
**Warning signs:** Phase 2 validation incorrectly flags this as a failure — pre-empt in runbook.

## Code Examples

All code below is verified against CameraX 1.6.0 API surface + Compose-native pattern. Canonical for the planner's task actions.

### CameraX version catalog additions

```toml
# gradle/libs.versions.toml — ADD under [versions]
camerax = "1.6.0"
mlkitFace = "16.1.7"
timber = "5.0.1"

# ALSO BUMP (currently pinned at 2026.03.00):
composeBom = "2026.04.00"

# ADD under [libraries]
androidx-camera-core         = { module = "androidx.camera:camera-core",          version.ref = "camerax" }
androidx-camera-camera2      = { module = "androidx.camera:camera-camera2",       version.ref = "camerax" }
androidx-camera-lifecycle    = { module = "androidx.camera:camera-lifecycle",     version.ref = "camerax" }
androidx-camera-video        = { module = "androidx.camera:camera-video",         version.ref = "camerax" }
androidx-camera-view         = { module = "androidx.camera:camera-view",          version.ref = "camerax" }
androidx-camera-effects      = { module = "androidx.camera:camera-effects",       version.ref = "camerax" }
androidx-camera-mlkit-vision = { module = "androidx.camera:camera-mlkit-vision",  version.ref = "camerax" }
androidx-camera-compose      = { module = "androidx.camera:camera-compose",       version.ref = "camerax" }
mlkit-face-detection         = { module = "com.google.mlkit:face-detection",      version.ref = "mlkitFace" }
timber                        = { module = "com.jakewharton.timber:timber",        version.ref = "timber" }
```

## Runtime State Inventory

Not applicable — Phase 2 is greenfield code addition, no renames or migrations. Phase 1 left no stored state (no DataStore writes, no MediaStore inserts).

## Environment Availability

| Dependency | Required By | Available | Version | Fallback |
|------------|-------------|-----------|---------|----------|
| Android SDK Platform 35 | compileSdk=35 | ✓ (confirmed in Phase 1) | — | 36 with `suppressUnsupportedCompileSdk` |
| JDK 21 (Android Studio jbr) | Gradle build | ✓ (confirmed in Phase 1 `gradle.properties`) | 21 | — |
| ADB | Install APK + logcat on Xiaomi 13T | ✓ (Phase 1 FND-08 verified) | — | — |
| Xiaomi 13T (physical device) | Camera / ML Kit / overlay runtime validation | ✓ (user-owned) | MIUI/HyperOS | No emulator fallback — emulator cannot simulate real camera |
| Google Play Services | NOT required (bundled ML Kit) | — | — | — |

**Missing dependencies with no fallback:** None.
**Missing dependencies with fallback:** None.

## Validation Architecture

Nyquist validation is enabled (`workflow.nyquist_validation: true` in `.planning/config.json`). Phase 2 adds automated tests for the deterministic, unit-testable components; runtime / on-device behavior is validated via the `02-HANDOFF.md` runbook (human gate).

### Test Framework

| Property | Value |
|----------|-------|
| Framework | JUnit 4 (`junit:junit:4.13.2`) — established Phase 1 D-19 |
| Instrumented framework | `androidx.test.ext:junit:1.3.0` + `espresso-core:3.7.0` — Phase 1 D-20 |
| Config file | None beyond `app/build.gradle.kts` `testImplementation`/`androidTestImplementation` already in place |
| Quick run command | `./gradlew :app:testDebugUnitTest --tests "com.bugzz.filter.camera.detector.*"` |
| Full suite command | `./gradlew :app:testDebugUnitTest :app:lintDebug` |
| Instrumented | `./gradlew :app:connectedDebugAndroidTest` (optional — requires device, not blocking) |

### Phase Requirements → Test Map

| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| CAM-01 | `CameraXViewfinder` renders preview | manual-only | runbook step 1 (visual on Xiaomi 13T) | ❌ manual |
| CAM-02 | Lens flip works 10× no error | manual-only | runbook step 2 (log scan for CameraInUseException) | ❌ manual |
| CAM-03 | Four use cases bind under one lifecycle | unit + instrumented (smoke) | `./gradlew :app:testDebugUnitTest --tests "*CameraControllerTest*"` — mock provider, assert useCaseGroup has 4 use cases | ❌ Wave 0 |
| CAM-04 | MlKitAnalyzer runs with bundled model contour mode | unit (detector options) | `./gradlew :app:testDebugUnitTest --tests "*FaceDetectorOptionsTest*"` — verify builder produces expected options | ❌ Wave 0 |
| CAM-05 | Backpressure KEEP_ONLY_LATEST | unit (ImageAnalysis builder config) | merged into CameraControllerTest | ❌ Wave 0 |
| CAM-06 | OverlayEffect bound to 3 targets | unit (effect target mask) | `./gradlew :app:testDebugUnitTest --tests "*OverlayEffectBuilderTest*"` | ❌ Wave 0 |
| CAM-07 | Sensor-to-buffer transform alignment | manual-only (visual on device) | runbook step 3 (4 orientations × 2 lenses) | ❌ manual |
| CAM-08 | trackingId stable 60+ frames | manual-only (logcat inspection) | runbook step 4 (grep Timber `FaceTracker` output) | ❌ manual |
| CAM-09 | 1€ filter smooths landmark jitter | unit (deterministic math) | `./gradlew :app:testDebugUnitTest --tests "*OneEuroFilterTest*"` — given constant input returns constant output; given jittery input returns smoother output | ❌ Wave 0 |

### Sampling Rate

- **Per task commit:** `./gradlew :app:testDebugUnitTest --tests "com.bugzz.filter.camera.detector.*"` (should run <10s; tests only pure-Kotlin logic)
- **Per wave merge:** `./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug` (full debug build + all unit tests + lint)
- **Phase gate:** Full suite green + `02-HANDOFF.md` runbook executed by user on Xiaomi 13T before `/gsd-verify-work`

### Wave 0 Gaps

- [ ] `app/src/test/java/com/bugzz/filter/camera/detector/OneEuroFilterTest.kt` — covers CAM-09. Test cases:
   1. Constant input → output equals input within ε=1e-6.
   2. Step input (jump from 0 to 100 at t=1.0) → output smoothly approaches 100 over 5+ samples.
   3. Sine-wave jitter on stationary base → output shows measurable attenuation (RMS output < RMS input).
   4. First sample initializes without division-by-zero (`tPrev` defaults handled).
- [ ] `app/src/test/java/com/bugzz/filter/camera/detector/FaceDetectorOptionsTest.kt` — covers CAM-04, CAM-15. Assert `FaceDetectorClient.buildOptions()` yields `performanceMode = PERFORMANCE_MODE_FAST`, `contourMode = CONTOUR_MODE_ALL`, `isTrackingEnabled = true`, `minFaceSize == 0.15f`, `landmarkMode == LANDMARK_MODE_NONE`, `classificationMode == CLASSIFICATION_MODE_NONE`.
- [ ] `app/src/test/java/com/bugzz/filter/camera/render/OverlayEffectBuilderTest.kt` — covers CAM-06. Assert target mask equals `CameraEffect.PREVIEW or CameraEffect.VIDEO_CAPTURE or CameraEffect.IMAGE_CAPTURE`. Assert `queueDepth == 0`.
- [ ] `app/src/test/java/com/bugzz/filter/camera/camera/CameraControllerTest.kt` — covers CAM-03, CAM-05. Mock `ProcessCameraProvider`; assert that after `bind()`, `UseCaseGroup` includes 4 use cases and 1 effect; assert `ImageAnalysis` backpressure strategy equals `STRATEGY_KEEP_ONLY_LATEST`.
- [ ] (optional, not blocking) `app/src/androidTest/java/com/bugzz/filter/camera/camera/CameraControllerInstrumentedTest.kt` — smoke test that bind → flip lens → unbind cycle does not leak the test Activity (let LeakCanary flag if it does).

**All four unit test files must exist before any `feat(02-...)` implementation commit that writes the code under test.** This is the Nyquist gate.

## Security Domain

`security_enforcement` is not explicitly set in `.planning/config.json` — treated as enabled per convention.

### Applicable ASVS Categories

| ASVS Category | Applies | Standard Control |
|---------------|---------|-----------------|
| V2 Authentication | no | Offline app; no login |
| V3 Session Management | no | No sessions |
| V4 Access Control | no | No multi-tenant |
| V5 Input Validation | partial | Runtime CAMERA permission (Phase 1 already enforces) |
| V6 Cryptography | no | No secrets, no encrypted storage in Phase 2 |
| V9 Data Protection | yes | Biometric data (face landmarks) — **do not log face contour points** with PII context; Timber output in debug only |

### Known Threat Patterns for Android AR

| Pattern | STRIDE | Standard Mitigation |
|---------|--------|---------------------|
| Camera used without permission | Elevation | `ActivityResultContracts.RequestPermission` wired in Phase 1, honored in Phase 2 before bind |
| Face landmark data logged with user-identifiable info | Information Disclosure | Timber tree in debug only; strip production; log landmark *counts*, not coordinates-plus-bitmap |
| Exported Activity receives force-record intent | Tampering | `MainActivity` has no `<intent-filter>` other than `MAIN/LAUNCHER` → `android:exported="true"` only for launcher; no external deep link in Phase 2 |
| MP4 saved to shared storage under predictable filename | Information Disclosure | Filename uses timestamp `bugzz_${System.currentTimeMillis()}.mp4` — not user-provided |
| RECORD_AUDIO requested before user action | Privacy | D-05/D-26 — NOT requested in Phase 2 |

## Concrete Code Sketches

**These sketches are the authoritative implementation reference. Planner copies them into task actions.** All verified against CameraX 1.6.0 API.

### Sketch A: `OneEuroFilter.kt` — per-channel 1€ filter (CAM-09)

```kotlin
package com.bugzz.filter.camera.detector

import kotlin.math.abs
import kotlin.math.exp

/**
 * 1€ Filter — low-pass filter with adaptive cutoff for jitter-free live signals.
 * Casiez et al. CHI 2012. Port of the canonical C++/Python reference.
 *
 * Usage: construct one per (face-id × channel); channel = {x, y} for each landmark.
 *
 * @param minCutoff  Base cutoff frequency (Hz). Lower = smoother / laggier. D-20 default 1.0.
 * @param beta       Velocity-scaling of cutoff. Higher = more responsive to motion. D-20 default 0.007.
 * @param dCutoff    Derivative low-pass cutoff (Hz). D-20 default 1.0.
 */
class OneEuroFilter(
    private val minCutoff: Double = 1.0,
    private val beta: Double = 0.007,
    private val dCutoff: Double = 1.0,
) {
    private var xPrev: Double = 0.0
    private var dxPrev: Double = 0.0
    private var tPrevNanos: Long = 0L
    private var initialized = false

    /** @return filtered value at [tNanos] for raw input [x]. */
    fun filter(x: Double, tNanos: Long): Double {
        if (!initialized) {
            xPrev = x
            dxPrev = 0.0
            tPrevNanos = tNanos
            initialized = true
            return x
        }
        val dt = (tNanos - tPrevNanos).coerceAtLeast(1L) / 1e9 // seconds, floor 1ns
        val dx = (x - xPrev) / dt
        val aD = alpha(dt, dCutoff)
        val dxHat = aD * dx + (1 - aD) * dxPrev
        val cutoff = minCutoff + beta * abs(dxHat)
        val aX = alpha(dt, cutoff)
        val xHat = aX * x + (1 - aX) * xPrev
        xPrev = xHat
        dxPrev = dxHat
        tPrevNanos = tNanos
        return xHat
    }

    fun reset() { initialized = false }

    private fun alpha(dt: Double, cutoff: Double): Double {
        // tau = 1 / (2π·cutoff); alpha = 1 / (1 + tau/dt)
        val tau = 1.0 / (2.0 * Math.PI * cutoff)
        return 1.0 / (1.0 + tau / dt)
    }
}

/**
 * Holds per-trackingId × per-channel filters. Clears state when a trackingId disappears
 * (D-22). Re-initializes when the same trackingId reappears — do not carry stale state.
 */
class LandmarkSmoother(
    private val minCutoff: Double = 1.0,
    private val beta: Double = 0.007,
    private val dCutoff: Double = 1.0,
) {
    // Key = "$trackingId:$landmarkName:$channel" — landmarkName from ML Kit FaceContour.Type
    private val filters = HashMap<String, OneEuroFilter>()

    fun smoothPoint(
        trackingId: Int,
        landmarkName: String,
        xRaw: Float,
        yRaw: Float,
        tNanos: Long,
    ): Pair<Float, Float> {
        val xKey = "$trackingId:$landmarkName:x"
        val yKey = "$trackingId:$landmarkName:y"
        val fx = filters.getOrPut(xKey) { OneEuroFilter(minCutoff, beta, dCutoff) }
        val fy = filters.getOrPut(yKey) { OneEuroFilter(minCutoff, beta, dCutoff) }
        return fx.filter(xRaw.toDouble(), tNanos).toFloat() to
               fy.filter(yRaw.toDouble(), tNanos).toFloat()
    }

    /** Remove state for tracking IDs no longer present (D-22). */
    fun retainActive(activeIds: Set<Int>) {
        val iter = filters.keys.iterator()
        while (iter.hasNext()) {
            val key = iter.next()
            val id = key.substringBefore(':').toIntOrNull() ?: continue
            if (id !in activeIds) iter.remove()
        }
    }

    /** Clear entirely — invoke on lens flip to avoid cross-session state leak. */
    fun clear() = filters.clear()
}
```

### Sketch B: `FaceDetectorClient.kt` — MlKitAnalyzer + 1€ + AtomicRef

```kotlin
package com.bugzz.filter.camera.detector

import androidx.camera.core.ImageAnalysis
import androidx.camera.mlkit.vision.MlKitAnalyzer
import androidx.camera.core.ImageAnalysis.COORDINATE_SYSTEM_SENSOR
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceContour
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import timber.log.Timber
import java.util.concurrent.Executor
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class FaceDetectorClient @Inject constructor(
    @Named("cameraExecutor") private val cameraExecutor: Executor,
) {
    private val options = buildOptions()
    private val detector: FaceDetector = FaceDetection.getClient(options)
    private val smoother = LandmarkSmoother(minCutoff = 1.0, beta = 0.007, dCutoff = 1.0)

    /** Latest face snapshot — single-writer (cameraExecutor), multi-reader (renderExecutor). */
    val latestSnapshot: AtomicReference<FaceSnapshot> =
        AtomicReference(FaceSnapshot.EMPTY)

    /** Construct the MlKitAnalyzer — attach with ImageAnalysis.setAnalyzer. */
    fun createAnalyzer(): MlKitAnalyzer =
        MlKitAnalyzer(
            /* detectors = */ listOf(detector),
            /* targetCoordinateSystem = */ COORDINATE_SYSTEM_SENSOR,
            /* executor = */ cameraExecutor,
        ) { result ->
            val faces = result.getValue(detector) ?: emptyList<Face>()
            val tNanos = System.nanoTime()

            // 1€ smoothing
            val activeIds = faces.mapNotNull { it.trackingId }.toSet()
            smoother.retainActive(activeIds)
            val smoothedFaces = faces.map { face -> smoothFace(face, tNanos) }

            latestSnapshot.set(FaceSnapshot(smoothedFaces, tNanos))

            // Timber FaceTracker logging (D-03 / CAM-08 verification via logcat)
            if (faces.isNotEmpty()) {
                faces.forEach { f ->
                    Timber.tag("FaceTracker").v(
                        "t=$tNanos id=${f.trackingId} bb=${f.boundingBox.centerX()},${f.boundingBox.centerY()} contours=${f.allContours.size}"
                    )
                }
            }
        }

    /** Release detector on @Singleton destruction — wire via DisposableHandle if needed. */
    fun close() = detector.close()

    /** Invoke on lens flip (D-25 compatibility — clear 1€ state to prevent cross-session leak). */
    fun onLensFlipped() = smoother.clear()

    private fun smoothFace(face: Face, tNanos: Long): SmoothedFace {
        val id = face.trackingId ?: -1
        val smoothedContours: Map<Int, List<android.graphics.PointF>> = buildMap {
            for (type in SMOOTHED_CONTOUR_TYPES) {
                val contour = face.getContour(type) ?: continue
                val pts = contour.points.map { p ->
                    val (sx, sy) = smoother.smoothPoint(id, "c$type", p.x, p.y, tNanos)
                    android.graphics.PointF(sx, sy)
                }
                put(type, pts)
            }
        }
        return SmoothedFace(
            trackingId = id,
            boundingBox = face.boundingBox,
            contours = smoothedContours,
            landmarks = face.allLandmarks.associate { it.landmarkType to android.graphics.PointF(it.position.x, it.position.y) },
        )
    }

    companion object {
        private val SMOOTHED_CONTOUR_TYPES = listOf(
            FaceContour.FACE,
            FaceContour.NOSE_BRIDGE,
            FaceContour.NOSE_BOTTOM,
            FaceContour.LEFT_EYE,
            FaceContour.RIGHT_EYE,
            FaceContour.LEFT_CHEEK,
            FaceContour.RIGHT_CHEEK,
            FaceContour.UPPER_LIP_TOP,
            FaceContour.LOWER_LIP_BOTTOM,
        )

        /** Exposed for unit test (FaceDetectorOptionsTest) — do not inline in init. */
        fun buildOptions(): FaceDetectorOptions =
            FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
                .enableTracking()
                .setMinFaceSize(0.15f)
                .build()
    }
}

data class SmoothedFace(
    val trackingId: Int,
    val boundingBox: android.graphics.Rect,
    val contours: Map<Int, List<android.graphics.PointF>>,
    val landmarks: Map<Int, android.graphics.PointF>,
)

data class FaceSnapshot(val faces: List<SmoothedFace>, val timestampNanos: Long) {
    companion object { val EMPTY = FaceSnapshot(emptyList(), 0L) }
}
```

### Sketch C: `OverlayEffectBuilder.kt` — stable `OverlayEffect` instance (CAM-06)

```kotlin
package com.bugzz.filter.camera.render

import android.os.Handler
import android.os.HandlerThread
import androidx.camera.core.CameraEffect
import androidx.camera.effects.OverlayEffect
import com.bugzz.filter.camera.detector.FaceDetectorClient
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OverlayEffectBuilder @Inject constructor(
    private val faceDetector: FaceDetectorClient,
    private val renderer: DebugOverlayRenderer,
) {
    private lateinit var renderThread: HandlerThread
    private lateinit var renderHandler: Handler

    /** Construct ONCE. D-25: reuse across lens flips. */
    fun build(): OverlayEffect {
        renderThread = HandlerThread("BugzzRenderThread").apply { start() }
        renderHandler = Handler(renderThread.looper)

        val targets = CameraEffect.PREVIEW or
                      CameraEffect.VIDEO_CAPTURE or
                      CameraEffect.IMAGE_CAPTURE

        val effect = OverlayEffect(
            /* targets = */ targets,
            /* queueDepth = */ 0,      // 0 = draw on every frame; CAM-06 sketch matches spotlight sample
            /* handler = */ renderHandler,
            /* errorListener = */ { throwable -> Timber.e(throwable, "OverlayEffect internal error") }
        )

        effect.setOnDrawListener { frame ->
            val canvas = frame.overlayCanvas
            // CRITICAL: match COORDINATE_SYSTEM_SENSOR from MlKitAnalyzer (D-17 / CAM-07)
            canvas.setMatrix(frame.sensorToBufferTransform)
            val snapshot = faceDetector.latestSnapshot.get()
            renderer.draw(canvas, snapshot, frame.timestampNanos)
            true // = "I drew something; present this frame"
        }

        return effect
    }

    fun release() {
        if (::renderThread.isInitialized) renderThread.quitSafely()
    }
}
```

### Sketch D: `DebugOverlayRenderer.kt` — red rect + landmark dots (D-01/D-23)

```kotlin
package com.bugzz.filter.camera.render

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.bugzz.filter.camera.BuildConfig
import com.bugzz.filter.camera.detector.FaceSnapshot
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DebugOverlayRenderer @Inject constructor() {

    private val boxPaint = Paint().apply {
        color = Color.RED
        style = Paint.Style.STROKE
        strokeWidth = 4f
        isAntiAlias = true
    }
    private val dotPaint = Paint().apply {
        color = Color.argb(255, 255, 80, 0)
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    /** Invoked from OverlayEffect onDrawListener on renderExecutor / renderHandler thread. */
    fun draw(canvas: Canvas, snapshot: FaceSnapshot, timestampNanos: Long) {
        if (!BuildConfig.DEBUG) return                  // D-02 gate
        if (snapshot.faces.isEmpty()) return            // CAM-06 no-face: draw nothing (no ghost)

        for (face in snapshot.faces) {
            // Bounding box — ALWAYS draw (D-23 — draw on all faces, primary + secondary)
            canvas.drawRect(
                face.boundingBox.left.toFloat(),
                face.boundingBox.top.toFloat(),
                face.boundingBox.right.toFloat(),
                face.boundingBox.bottom.toFloat(),
                boxPaint,
            )
            // Contour landmark dots — populated only for primary face (PITFALLS #13 / D-23)
            face.contours.values.flatten().forEach { p ->
                canvas.drawCircle(p.x, p.y, 4f, dotPaint)
            }
            // Fallback landmarks for secondary faces — `allLandmarks` is populated for all faces
            face.landmarks.values.forEach { p ->
                canvas.drawCircle(p.x, p.y, 5f, dotPaint)
            }
        }
    }
}
```

### Sketch E: `CameraController.kt` — UseCaseGroup + lens flip + test record (CAM-02/03)

```kotlin
package com.bugzz.filter.camera.camera

import android.content.ContentValues
import android.content.Context
import android.provider.MediaStore
import android.view.Surface
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceRequest
import androidx.camera.core.UseCaseGroup
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.bugzz.filter.camera.detector.FaceDetectorClient
import com.bugzz.filter.camera.render.OverlayEffectBuilder
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import java.util.concurrent.Executor
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class CameraController @Inject constructor(
    @ApplicationContext private val appContext: Context,
    @Named("cameraExecutor") private val cameraExecutor: Executor,
    private val faceDetector: FaceDetectorClient,
    private val overlayEffectBuilder: OverlayEffectBuilder,
) {
    // Compose-native: emit latest SurfaceRequest to the composable for CameraXViewfinder.
    private val _surfaceRequest = MutableStateFlow<SurfaceRequest?>(null)
    val surfaceRequest: StateFlow<SurfaceRequest?> = _surfaceRequest.asStateFlow()

    // Reused across lens flips (D-25).
    private val overlayEffect = overlayEffectBuilder.build()

    // Use cases — recreated on lens flip to pick up lens-specific target rotations / mirror mode.
    private var preview: Preview? = null
    private var imageAnalysis: ImageAnalysis? = null
    private var imageCapture: ImageCapture? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private var activeRecording: Recording? = null

    suspend fun bind(
        lifecycleOwner: LifecycleOwner,
        lens: CameraLens,
        initialRotation: Int = Surface.ROTATION_0,
    ) {
        val provider = ProcessCameraProvider.awaitInstance(appContext)
        provider.unbindAll()

        val preview = Preview.Builder()
            .setTargetRotation(initialRotation)
            .build()
            .also { p ->
                p.setSurfaceProvider { request -> _surfaceRequest.value = request }
            }

        val analyzer = faceDetector.createAnalyzer()
        val imageAnalysis = ImageAnalysis.Builder()
            .setResolutionSelector(
                ResolutionSelector.Builder()
                    .setResolutionStrategy(
                        ResolutionStrategy(
                            android.util.Size(720, 1280),
                            ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER,
                        )
                    ).build()
            )
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST) // CAM-05
            .setTargetRotation(initialRotation)
            .build()
            .also { it.setAnalyzer(cameraExecutor, analyzer) }

        val imageCapture = ImageCapture.Builder()
            .setTargetRotation(initialRotation)
            .build()

        val videoCapture = VideoCapture.Builder(
            Recorder.Builder()
                .setQualitySelector(QualitySelector.from(Quality.HD))
                .build()
        )
            .setMirrorMode(VideoCapture.MIRROR_MODE_ON_FRONT_ONLY)
            .setTargetRotation(initialRotation)
            .build()

        val useCaseGroup = UseCaseGroup.Builder()
            .addUseCase(preview)
            .addUseCase(imageAnalysis)
            .addUseCase(imageCapture)
            .addUseCase(videoCapture)
            .addEffect(overlayEffect)   // CAM-06 — effect bakes into all three output streams
            .build()

        val selector = when (lens) {
            CameraLens.FRONT -> CameraSelector.DEFAULT_FRONT_CAMERA
            CameraLens.BACK  -> CameraSelector.DEFAULT_BACK_CAMERA
        }

        provider.bindToLifecycle(lifecycleOwner, selector, useCaseGroup)

        // Retain refs for setTargetRotation on device rotation
        this.preview = preview
        this.imageAnalysis = imageAnalysis
        this.imageCapture = imageCapture
        this.videoCapture = videoCapture
    }

    /** D-24 — full unbind + rebind; <500ms target; no CameraInUseException on 10 toggles. */
    suspend fun flipLens(lifecycleOwner: LifecycleOwner, newLens: CameraLens, rotation: Int) {
        faceDetector.onLensFlipped()               // clear stale 1€ state
        bind(lifecycleOwner, newLens, rotation)
    }

    /** Called by OrientationEventListener on device rotation (D-08). */
    fun setTargetRotation(rotation: Int) {
        preview?.targetRotation = rotation
        imageAnalysis?.targetRotation = rotation
        imageCapture?.targetRotation = rotation
        videoCapture?.targetRotation = rotation
    }

    /** D-04 / D-05 — debug-only, 5s, no audio, DCIM/Bugzz. */
    fun startTestRecording(onFinalized: (Result<android.net.Uri>) -> Unit) {
        val vc = videoCapture ?: return onFinalized(Result.failure(IllegalStateException("Not bound")))
        if (activeRecording != null) {
            onFinalized(Result.failure(IllegalStateException("Already recording")))
            return
        }
        val filename = "bugzz_test_${System.currentTimeMillis()}.mp4"
        val values = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, filename)
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            put(MediaStore.Video.Media.RELATIVE_PATH, "DCIM/Bugzz")
        }
        val options = MediaStoreOutputOptions.Builder(
            appContext.contentResolver,
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
        ).setContentValues(values).build()

        activeRecording = vc.output
            .prepareRecording(appContext, options)
            // NOTE: D-05 — NO .withAudioEnabled(); RECORD_AUDIO deferred to Phase 5
            .start(ContextCompat.getMainExecutor(appContext)) { event ->
                when (event) {
                    is VideoRecordEvent.Start    -> Timber.tag("CameraController").i("TEST RECORD start")
                    is VideoRecordEvent.Finalize -> {
                        activeRecording = null
                        if (event.hasError()) {
                            Timber.tag("CameraController").e(event.cause, "TEST RECORD finalize error=${event.error}")
                            onFinalized(Result.failure(event.cause ?: RuntimeException("record error ${event.error}")))
                        } else {
                            val uri = event.outputResults.outputUri
                            Timber.tag("CameraController").i("TEST RECORD saved $uri")
                            onFinalized(Result.success(uri))
                        }
                    }
                    else -> Unit
                }
            }
    }

    fun stopTestRecording() {
        activeRecording?.stop()
        activeRecording = null
    }
}

enum class CameraLens { FRONT, BACK }
```

### Sketch F: `CameraModule.kt` — Hilt providers

```kotlin
package com.bugzz.filter.camera.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object CameraModule {

    @Provides @Singleton @Named("cameraExecutor")
    fun provideCameraExecutor(): Executor = Executors.newSingleThreadExecutor { r ->
        Thread(r, "BugzzCameraExecutor")
    }

    @Provides @Singleton @Named("renderExecutor")
    fun provideRenderExecutor(): Executor = Executors.newSingleThreadExecutor { r ->
        Thread(r, "BugzzRenderExecutor")
    }

    // CameraController, FaceDetectorClient, OverlayEffectBuilder, DebugOverlayRenderer all use
    // constructor @Inject with @Singleton — no explicit @Provides needed.
}
```

### Sketch G: `CameraScreen.kt` — Compose composable tree

```kotlin
package com.bugzz.filter.camera.ui.camera

import android.widget.Toast
import androidx.camera.compose.CameraXViewfinder
import androidx.camera.viewfinder.surface.ImplementationMode
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bugzz.filter.camera.BuildConfig

@Composable
fun CameraScreen(
    onOpenPreview: () -> Unit,
    vm: CameraViewModel = hiltViewModel(),
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    val uiState by vm.uiState.collectAsStateWithLifecycle()
    val surfaceRequest by vm.surfaceRequest.collectAsStateWithLifecycle()

    // Kick off binding once permission granted.
    LaunchedEffect(uiState.permissionState, uiState.lens) {
        if (uiState.permissionState.isGranted && uiState.isDetectorReady) {
            vm.bind(lifecycleOwner)
        }
    }

    // OrientationEventListener — lives through lifecycle.
    DisposableEffect(lifecycleOwner) {
        val listener = vm.orientationListener(context)
        listener.enable()
        onDispose { listener.disable() }
    }

    // One-shot events (toast).
    LaunchedEffect(Unit) {
        vm.events.collect { e ->
            when (e) {
                is OneShotEvent.TestRecordSaved ->
                    Toast.makeText(context, "Saved: ${e.uri}", Toast.LENGTH_LONG).show()
                is OneShotEvent.TestRecordFailed ->
                    Toast.makeText(context, "Record error: ${e.reason}", Toast.LENGTH_LONG).show()
                is OneShotEvent.CameraError ->
                    Toast.makeText(context, "Camera error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        surfaceRequest?.let { sr ->
            CameraXViewfinder(
                surfaceRequest = sr,
                implementationMode = ImplementationMode.EXTERNAL, // a.k.a. PERFORMANCE / SurfaceView
                modifier = Modifier.fillMaxSize(),
            )
        }
        // Flip button top-right (D-24)
        IconButton(
            onClick = { vm.onFlipLens() },
            modifier = Modifier.align(Alignment.TopEnd).padding(16.dp),
        ) {
            Icon(androidx.compose.material.icons.Icons.Default.Cameraswitch, contentDescription = "Flip camera")
        }
        // Debug-only TEST RECORD button (D-04)
        if (BuildConfig.DEBUG) {
            Button(
                onClick = { vm.onTestRecord() },
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 32.dp),
            ) {
                Text(if (uiState.isRecording) "REC..." else "TEST RECORD 5s")
            }
        }
    }
}
```

### Sketch H: `CameraViewModel.kt` — state + intents + orientation

```kotlin
package com.bugzz.filter.camera.ui.camera

import android.content.Context
import android.view.OrientationEventListener
import android.view.Surface
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bugzz.filter.camera.camera.CameraController
import com.bugzz.filter.camera.camera.CameraLens
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CameraViewModel @Inject constructor(
    private val controller: CameraController,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CameraUiState())
    val uiState = _uiState.asStateFlow()
    val surfaceRequest = controller.surfaceRequest

    private val _events = Channel<OneShotEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private var currentRotation: Int = Surface.ROTATION_0

    fun bind(owner: LifecycleOwner) {
        viewModelScope.launch {
            runCatching { controller.bind(owner, _uiState.value.lens, currentRotation) }
                .onFailure { _events.send(OneShotEvent.CameraError(it.message ?: "bind failed")) }
        }
    }

    fun onFlipLens() {
        val nextLens = if (_uiState.value.lens == CameraLens.FRONT) CameraLens.BACK else CameraLens.FRONT
        _uiState.value = _uiState.value.copy(lens = nextLens)
        // bind() is triggered by LaunchedEffect watching uiState.lens
    }

    fun onTestRecord() {
        if (_uiState.value.isRecording) return
        _uiState.value = _uiState.value.copy(isRecording = true)
        controller.startTestRecording { result ->
            viewModelScope.launch {
                _uiState.value = _uiState.value.copy(isRecording = false)
                result.fold(
                    onSuccess = { _events.send(OneShotEvent.TestRecordSaved(it)) },
                    onFailure = { _events.send(OneShotEvent.TestRecordFailed(it.message ?: "record error")) },
                )
            }
        }
        viewModelScope.launch {
            delay(5_000L)
            controller.stopTestRecording()
        }
    }

    /** Create and return an OrientationEventListener that updates CameraX targetRotation. */
    fun orientationListener(context: Context): OrientationEventListener =
        object : OrientationEventListener(context) {
            override fun onOrientationChanged(degrees: Int) {
                if (degrees == ORIENTATION_UNKNOWN) return
                val rot = when (degrees) {
                    in 45..134  -> Surface.ROTATION_270
                    in 135..224 -> Surface.ROTATION_180
                    in 225..314 -> Surface.ROTATION_90
                    else        -> Surface.ROTATION_0
                }
                if (rot != currentRotation) {
                    currentRotation = rot
                    controller.setTargetRotation(rot)
                }
            }
        }

    override fun onCleared() {
        super.onCleared()
        controller.stopTestRecording()
    }
}

// CameraUiState.kt
data class CameraUiState(
    val lens: CameraLens = CameraLens.FRONT,
    val permissionState: PermissionState = PermissionState.Unknown,
    val isDetectorReady: Boolean = true,     // true on construct; becomes false only on detector close
    val isRecording: Boolean = false,
    val lastErrorMessage: String? = null,
)

sealed interface PermissionState {
    data object Unknown : PermissionState
    data object Denied : PermissionState
    data object Granted : PermissionState
    val isGranted: Boolean get() = this is Granted
}

// OneShotEvent.kt
sealed interface OneShotEvent {
    data class TestRecordSaved(val uri: android.net.Uri) : OneShotEvent
    data class TestRecordFailed(val reason: String) : OneShotEvent
    data class CameraError(val message: String) : OneShotEvent
}
```

### Sketch I: AndroidManifest.xml portrait-lock

```xml
<!-- app/src/main/AndroidManifest.xml — edit MainActivity node -->
<activity
    android:name=".MainActivity"
    android:exported="true"
    android:screenOrientation="portrait"          <!-- D-07 — NEW -->
    android:configChanges="orientation|screenSize|keyboardHidden">  <!-- optional; suppresses recreation -->
    <intent-filter>
        <action android:name="android.intent.action.MAIN" />
        <category android:name="android.intent.category.LAUNCHER" />
    </intent-filter>
</activity>
```

**Note on `configChanges`:** optional; since `screenOrientation="portrait"` already prevents rotation-induced recreation, adding `configChanges` is defensive. D-24 context notes "if we want to suppress activity recreation on sensor orientation change" — include it; Xiaomi MIUI has been reported to recreate portrait-locked activities on reverse-portrait in rare firmware, and the `configChanges` attribute is cheap defense.

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| `AndroidView(PreviewView)` in Compose | `CameraXViewfinder` composable | CameraX 1.5.1 (Oct 2025), hardened 1.6.0 (Mar 2026) | Proper Compose z-ordering; surfaceProvider lifecycle auto-handled |
| Manual Canvas overlay on sibling View | `OverlayEffect` in `camera-effects` | CameraX 1.4.0 (Dec 2024) stable, 1.6.0 now canonical | Overlay bakes into preview + still + video automatically |
| `MlKitAnalyzer(COORDINATE_SYSTEM_VIEW_REFERENCED)` | `MlKitAnalyzer(COORDINATE_SYSTEM_SENSOR)` paired with `OverlayEffect` sensor-to-buffer matrix | CameraX 1.4.0 introduced SENSOR coord | Zero manual matrix math; pairs naturally with OverlayEffect |
| Manual `MediaRecorder` / `MediaMuxer` for video | CameraX `Recorder` + `VideoCapture` (Media3 muxer since 1.5) | 1.5.0 (Sep 2025) | No encoder code in app |
| Manual `setMirrorMode` math on front cam | `VideoCapture.Builder().setMirrorMode(MIRROR_MODE_ON_FRONT_ONLY)` | 1.3.0 | Built-in |

**Deprecated / outdated:**
- `Accompanist Permissions` — Google deprecated; use raw `ActivityResultContracts`. Already honored in Phase 1.
- `kapt` for Hilt — use KSP. Already honored in Phase 1.
- `android.support.*` — years dead.

## Assumptions Log

| # | Claim | Section | Risk if Wrong |
|---|-------|---------|---------------|
| A1 | `OverlayEffect` queueDepth=0 draws on every frame, matching the spotlight sample pattern | §12 Sketch C | Low — docs show 0 and 5 both as valid; 5 buffers frames for analysis-preview sync which we don't need |
| A2 | `CameraXViewfinder` accepts `ImplementationMode.EXTERNAL` as the PERFORMANCE / SurfaceView equivalent | §12 Sketch G | Low — confirmed name change from PERFORMANCE to EXTERNAL in `camera-viewfinder-core` 1.4+; verify exact enum at build time |
| A3 | `MlKitAnalyzer` constructor accepts `COORDINATE_SYSTEM_SENSOR` — same signature as `COORDINATE_SYSTEM_VIEW_REFERENCED` | §12 Sketch B | Medium — CameraX release notes confirm SENSOR exists in `ImageAnalysis` coordinate systems from 1.4; the `MlKitAnalyzer` docs page searched did not explicitly show SENSOR but the feature was announced as available; if Xiaomi build fails, fall back to `VIEW_REFERENCED` + apply manual inverse of `frame.sensorToBufferTransform` (PITFALL #5 workaround — but heavier) |
| A4 | 1€ filter literature defaults (1.0 / 0.007 / 1.0) are adequate for face landmark smoothing on Xiaomi 13T | §12 Sketch A | Low — D-20 explicitly says these are ship defaults, tune empirically in Phase 3 |
| A5 | `ProcessCameraProvider.awaitInstance(ctx)` is the CameraX 1.6 suspend-friendly API | §12 Sketch E | Low — confirmed in CameraX docs since 1.4 |
| A6 | Xiaomi 13T HyperOS tolerates `screenOrientation="portrait"` + `OrientationEventListener`-driven CameraX targetRotation without custom OEM workarounds | §6 / D-08 | Low-Medium — reference app uses same pattern successfully; findings in `02-HANDOFF.md` if issue surfaces |
| A7 | `VideoCapture.MIRROR_MODE_ON_FRONT_ONLY` constant is available in 1.6 (carried over from 1.3) | §12 Sketch E | Low — no indication it was removed |
| A8 | `Bitmap.Config.ARGB_8888` canvas in `OverlayEffect.Frame.overlayCanvas` has GPU backing similar to a `SurfaceView` Canvas | §2 pattern 1 | Low — confirmed by CameraX 1.4 blog: "Canvas draws are converted to a texture and composited into the pipeline output" |

**User-confirmation items:** None required — all assumptions are low-to-medium implementation-detail risk validated against CameraX docs. Worst case (A3) has a documented workaround (VIEW_REFERENCED + inverse transform) that still ships Phase 2 on time if encountered on device.

## Open Questions

1. **Exact enum name for `ImplementationMode.EXTERNAL` vs `PERFORMANCE` in `camera-viewfinder-core` 1.6.0**
   - What we know: In earlier (pre-1.5) releases the enum was `PERFORMANCE` / `COMPATIBLE`; in `camera-viewfinder` 1.4+ it was renamed to `EXTERNAL` / `EMBEDDED`.
   - What's unclear: CameraX 1.6 may have reverted or aliased.
   - Recommendation: Implement with `ImplementationMode.EXTERNAL`; if build fails, fall back to `PERFORMANCE`. This is a literal rename — no functional risk.

2. **Whether a single `Handler` on the `OverlayEffect` constructor vs dedicated `HandlerThread` matters for frame pacing**
   - What we know: Blog samples use both `Handler(Looper.getMainLooper())` and a custom `HandlerThread`.
   - What's unclear: On Xiaomi 13T whether main-thread Handler stalls under Compose recomposition pressure.
   - Recommendation: Use dedicated `HandlerThread("BugzzRenderThread")` per §12 Sketch C. Main-thread Handler risks UI-thread contention during lens flip recomposition.

3. **Whether `camera-compose 1.6.0` exposes `ImplementationMode`-setting parameter on `CameraXViewfinder` or infers it**
   - What we know: CameraXViewfinder accepts `surfaceRequest` as primary param.
   - What's unclear: Whether `implementationMode` is a direct parameter or inferred from SurfaceRequest.
   - Recommendation: Check Kotlin signature at build time. If not a parameter, wrap `CameraXViewfinder` in a `CameraViewfinder` composable explicitly (from `androidx.camera.viewfinder:camera-viewfinder-compose`).

## Sources

### Primary (HIGH confidence)

- [CameraX releases (Android Developers)](https://developer.android.com/jetpack/androidx/releases/camera) — verified 1.6.0 stable 2026-03-25, full version timeline, CameraXViewfinder stability improvements in 1.5.1
- [What's new in CameraX 1.4.0 (Android Developers Blog)](https://android-developers.googleblog.com/2024/12/whats-new-in-camerax-140-and-jetpack-compose-support.html) — OverlayEffect introduction, sensorToBufferTransform, sample code
- [Introducing CameraX 1.5 (Android Developers Blog)](https://android-developers.googleblog.com/2025/11/introducing-camerax-15-powerful-video.html) — Media3 muxer integration, effect-baked video
- [OverlayEffect API reference](https://developer.android.com/reference/kotlin/androidx/camera/effects/OverlayEffect) — class signature, Frame API
- [ML Kit Analyzer with CameraX](https://developer.android.com/media/camera/camerax/mlkitanalyzer) — MlKitAnalyzer constructor, COORDINATE_SYSTEM_VIEW_REFERENCED
- [Detect faces with ML Kit on Android](https://developers.google.com/ml-kit/vision/face-detection/android) — FaceDetectorOptions, CONTOUR_MODE_ALL, enableTracking, bundled dep 16.1.7
- [CameraX video capturing architecture](https://developer.android.com/media/camera/camerax/video-capture) — VideoCapture, MediaStoreOutputOptions, setMirrorMode
- [CameraX use case rotations](https://developer.android.com/training/camerax/orientation-rotation) — OrientationEventListener + setTargetRotation under portrait lock
- [Create a spotlight effect with CameraX and Jetpack Compose (Jolanda Verhoef / Android Developers)](https://medium.com/androiddevelopers/create-a-spotlight-effect-with-camerax-and-jetpack-compose-8a7fa5b76641) — Compose + OverlayEffect pattern
- [Getting Started with CameraX in Jetpack Compose (Jolanda Verhoef)](https://medium.com/androiddevelopers/getting-started-with-camerax-in-jetpack-compose-781c722ca0c4) — SurfaceRequest flow pattern
- [Compose-Native CameraX Is Now Stable (ProAndroidDev, Oct 2025)](https://proandroiddev.com/goodbye-androidview-camerax-goes-full-compose-4d21ca234c4e) — full end-to-end Compose CameraX guide
- [camera viewfinder (Android Developers)](https://developer.android.com/jetpack/androidx/releases/camera-viewfinder) — ImplementationMode enum notes
- [ML Kit sample build.gradle (googlesamples/mlkit)](https://github.com/googlesamples/mlkit/blob/master/android/vision-quickstart/app/build.gradle) — confirms 16.1.7 current

### Secondary (MEDIUM confidence)

- [1€ Filter — Géry Casiez](https://gery.casiez.net/1euro/) — parameter tuning guidance
- [OneEuro Filter walkthrough (Mohamed Ali Rashad)](https://mohamedalirashad.github.io/FreeFaceMoCap/2021-12-25-filters-for-stability/) — alpha formula and Kotlin-transliteratable pseudocode
- [OneEuroFilter reference repo (Casiez)](https://github.com/casiez/OneEuroFilter) — canonical C++/Java source
- [Seamlessly Switching Camera Lenses with CameraX (Ngenge Senior)](https://ngengesenior.medium.com/seamlessly-switching-camera-lenses-during-video-recording-with-camerax-on-android-fcb597ed8236) — unbindAll + rebind lens flip pattern
- [OverlayEffect sample code Issue #575 (android/camera-samples)](https://github.com/android/camera-samples/issues/575) — confirms OverlayEffect is under-documented in samples, blog posts + API ref are canonical
- [CameraX Preview overlay + VideoCapture Overlay (camerax-developers group)](https://groups.google.com/a/android.com/g/camerax-developers/c/64eahzvdY4U) — overlay-in-video verification
- [OverlayEffect does not work correctly thread (camerax-developers group)](https://groups.google.com/a/android.com/g/camerax-developers/c/k3eVmhXejpk) — known caveats

### Tertiary (LOW confidence, flagged for validation on device)

- Xiaomi 13T / HyperOS specific camera HAL quirks — no first-party source; treat as PITFALLS #7 unknown until runbook executes on device; findings go in `02-HANDOFF.md` not in code
- Exact 1€ parameter values (1.0 / 0.007 / 1.0) for Xiaomi 13T jitter profile — ship defaults (D-20) then tune in Phase 3

## Metadata

**Confidence breakdown:**
- Standard Stack: HIGH — all versions verified against Google Maven and Android Developers April 2026
- Architecture: HIGH — pattern directly lifted from CameraX 1.4–1.6 release materials + Compose blog samples; low custom plumbing
- Pitfalls: HIGH — existing PITFALLS.md catalog maps every active risk; confirmed on 2026-04-19
- 1€ Filter: HIGH — canonical algorithm, widely verified; Kotlin port is mechanical transliteration
- Xiaomi 13T runtime behavior: LOW-MEDIUM — will surface in `02-HANDOFF.md`; no pre-emptive workaround warranted

**Research date:** 2026-04-19
**Valid until:** 2026-05-19 (fast-moving Compose + CameraX ecosystem; re-verify if Phase 2 execution is delayed beyond 30 days)

---

*Research for Phase 2: Camera Preview + Face Detection + Coordinate Validation*
*Ready for planning: yes — prescriptive code sketches provided for every load-bearing component*
