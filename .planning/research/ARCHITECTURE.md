# Architecture Research

**Domain:** Android AR face filter camera app (CameraX + ML Kit + overlay compositing)
**Researched:** 2026-04-18
**Confidence:** HIGH (CameraX 1.4+ OverlayEffect, ML Kit MlKitAnalyzer, MediaStore all verified via official docs)

---

## TL;DR (Opinionated)

1. **Rendering approach: CameraX `OverlayEffect` (camera-effects 1.4.0+) with a Canvas draw callback.** One API handles overlay on Preview + ImageCapture + VideoCapture simultaneously. No separate "re-composite on capture" pipeline. No OpenGL boilerplate. No manual MediaMuxer. This is THE decision that makes everything else simple.
2. **Face detection bridge: CameraX `MlKitAnalyzer` with `COORDINATE_SYSTEM_SENSOR`.** Paired with `OverlayEffect.frame.getSensorToBufferTransform()`, landmarks land on the overlay canvas in the correct place without manual matrix math.
3. **Architecture: MVVM + unidirectional state.** `ViewModel` owns `StateFlow<CameraUiState>`; UI reads it; `CameraController` / `FilterEngine` push events up. No RxJava, no custom event bus.
4. **Threading: 3 executors.** Main (UI), `cameraExecutor` (single-thread for CameraX binding + ML Kit analyze callback), `renderExecutor` (single-thread for overlay draw callback). ML Kit itself runs on an internal worker thread; results are delivered to `cameraExecutor`.
5. **No persistence beyond `DataStore<Preferences>` for last-used filter / camera facing.** Photos/videos → `MediaStore` (DCIM/Bugzz). No Room DB for MVP.

---

## 1. Component Boundaries

### System Overview (ASCII)

```
┌──────────────────────────────────────────────────────────────────────────┐
│                             UI LAYER (Views + XML)                       │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐      │
│  │ SplashAct   │  │ HomeAct     │  │ CameraAct   │  │ PreviewAct  │      │
│  │             │  │             │  │ (main)      │  │ (save/share)│      │
│  └─────────────┘  └─────────────┘  └──────┬──────┘  └─────────────┘      │
│                                           │                               │
│                        ┌──────────────────┴──────────────────┐            │
│                        │  PreviewView  + FilterPickerBar     │            │
│                        │  + ShutterButton + ModeToggle       │            │
│                        └──────────────────┬──────────────────┘            │
├────────────────────────────────────────────┼─────────────────────────────┤
│                        VIEWMODEL LAYER (state holders)                   │
│  ┌─────────────────────────────────────────┴───────────────────────────┐ │
│  │  CameraViewModel                                                     │ │
│  │    StateFlow<CameraUiState>   (lens, mode, selectedFilter, faces)    │ │
│  │    SharedFlow<OneShotEvent>   (CaptureSaved, CaptureFailed, Perm..)  │ │
│  └────────┬───────────────────┬──────────────────┬───────────────────┬──┘ │
├───────────┼───────────────────┼──────────────────┼───────────────────┼────┤
│           │                   │                  │                   │    │
│           ▼                   ▼                  ▼                   ▼    │
│  ┌────────────────┐  ┌────────────────┐ ┌─────────────────┐ ┌───────────┐ │
│  │ CameraControl- │  │ FaceDetector   │ │ FilterEngine    │ │ Capture   │ │
│  │  ler           │  │ (MlKitAnalyzer │ │ (OverlayEffect  │ │ Coordina- │ │
│  │ (CameraX bind) │──│  wrapper)      │─│  draw callback) │ │ tor       │ │
│  │ - Preview UC   │  │ - options      │ │ - Canvas paint  │ │ - take-   │ │
│  │ - ImageCapture │  │ - frame→faces  │ │ - sprite anim   │ │   Picture │ │
│  │ - VideoCapture │  │ - SENSOR coord │ │ - landmark map  │ │ - Recording│ │
│  │ - UseCaseGroup │  │                │ │                 │ │ - MediaStore│
│  └───────┬────────┘  └────────────────┘ └────────┬────────┘ └────┬──────┘ │
│          │                                       │                │        │
│          └───────────────────┬───────────────────┘                │        │
│                              │                                    │        │
├──────────────────────────────┼────────────────────────────────────┼───────┤
│                       DATA LAYER                                  │        │
│  ┌────────────────┐  ┌────────────────┐  ┌────────────────┐     │        │
│  │ FilterRepo     │  │ AssetLoader    │  │ MediaStoreRepo │◄────┘        │
│  │ (list of       │  │ (PNG sprite    │  │ (DCIM/Bugzz    │              │
│  │  FilterDef)    │  │  Bitmap cache) │  │  insert URIs)  │              │
│  └────────────────┘  └────────────────┘  └────────────────┘              │
│  ┌────────────────┐                                                       │
│  │ PrefsRepo      │   (DataStore<Preferences> — last filter, facing)     │
│  └────────────────┘                                                       │
└───────────────────────────────────────────────────────────────────────────┘

                 FRAME LIFETIME (conceptual arrows — details in §2)

   Camera HW ──► Preview UC ──► [OverlayEffect draw] ──► PreviewView (SurfaceView)
                      │                 ▲
                      │                 │ reads latest faces
                      ▼                 │
                ImageAnalysis ──► MlKitAnalyzer ──► faces: List<Face>
                      │                            (stored in FilterEngine)
                      ▼
                ImageCapture ──► [same OverlayEffect] ──► JPEG + MediaStore
                      │
                VideoCapture ──► [same OverlayEffect] ──► Recording → MP4 + MediaStore
```

### Component Responsibilities

| Component | Owns | Consumes | Produces | Threading |
|-----------|------|----------|----------|-----------|
| `CameraController` | CameraX `ProcessCameraProvider`, `UseCaseGroup`, lens selector, zoom/exposure state | `LifecycleOwner`, `PreviewView.surfaceProvider`, `FaceDetector` analyzer, `FilterEngine` overlay | Bound `Camera` object; exposes `takePicture()`, `startRecording()` | Binding on main; internal on `cameraExecutor` |
| `FaceDetector` | `FaceDetectorOptions` (CONTOUR_ALL, PERFORMANCE_MODE_FAST, tracking on), `MlKitAnalyzer` instance | `ImageProxy` from ImageAnalysis | `List<Face>` pushed to `FilterEngine` via callback | `cameraExecutor` (analyze) + ML Kit worker (detect) |
| `FilterEngine` | Current `FilterDef`, per-bug animation state, latest face snapshot, `OverlayEffect` drawFrame callback | Latest `List<Face>` from `FaceDetector`, selected filter from VM, `Bitmap` sprites from `AssetLoader` | Draws to overlay `Canvas` each frame | `renderExecutor` (single thread, called by OverlayEffect) |
| `CaptureCoordinator` | `ImageCapture` OutputFileOptions, `Recorder` + active `Recording`, filename builder | Trigger from VM, `MediaStoreRepo` URIs | `CaptureResult` (uri, type) emitted back to VM | Callbacks on `cameraExecutor`; delivered to VM on main |
| `FilterRepository` | Hardcoded `List<FilterDef>` (name, thumbnail, list of `BugConfig`) | — | `Flow<List<FilterDef>>` to VM | Any (pure data) |
| `AssetLoader` | `LruCache<String, Bitmap>` of decoded sprites | Asset paths from `BugConfig` | Ready-to-draw `Bitmap` | IO dispatcher; preload on camera open |
| `MediaStoreRepo` | ContentValues builders, MIME types, collection URIs | Capture output file/URI | Published `Uri` ready for share Intent | IO dispatcher |
| `PrefsRepo` | `DataStore<Preferences>` instance | User toggles | `Flow<UserSettings>` | IO dispatcher |
| `CameraViewModel` | `StateFlow<CameraUiState>`, `SharedFlow<OneShotEvent>` | Actions from UI | State updates | Main (StateFlow); work delegated |

### Why these boundaries

- **CameraController** isolates the entire CameraX surface. Swapping CameraX versions or adding a new use case touches one file.
- **FaceDetector** is deliberately thin — it's a bridge from `MlKitAnalyzer` to a Kotlin callback. Keeping it separate from `FilterEngine` means you can unit-test detection mocks without rendering.
- **FilterEngine** is the ONLY consumer of face data AND the ONLY thing that draws. Single writer to the canvas → no race conditions.
- **CaptureCoordinator** exists because `takePicture()` and `Recorder.start()` have different callback shapes, different error paths, and different MediaStore metadata. Keeping them together (both write to DCIM/Bugzz) but separate from `CameraController` (binding concerns) cleanly splits "how camera is configured" from "what the user clicked".

---

## 2. Data Flow — Three Pipelines, One Overlay

### 2.1 Live Preview Flow (24+ fps target)

```
[Camera HW sensor]
      │
      ▼  (GPU surface, continuous)
Preview use case
      │
      ▼  (OverlayEffect applied — runs on renderExecutor, per frame)
┌─────────────────────────────────────────────┐
│ OverlayEffect.onDrawFrame(frame, canvas) {  │
│   canvas.setMatrix(frame.getSensorToBuffer  │
│                     Transform())            │
│   FilterEngine.draw(canvas,                 │
│     latestFaces,  // read from AtomicRef    │
│     elapsedNanos) // drives animation       │
│ }                                           │
└─────────────────┬───────────────────────────┘
                  │
                  ▼
          PreviewView (SurfaceView backend)
                  │
                  ▼
               [Display]

         MEANWHILE — parallel branch:

[Camera HW sensor]
      │
      ▼  (YUV_420 ImageProxy, throttled to ~15fps)
ImageAnalysis use case (STRATEGY_KEEP_ONLY_LATEST)
      │
      ▼
MlKitAnalyzer (COORDINATE_SYSTEM_SENSOR)
      │
      ▼  (ML Kit worker thread, ~30-80ms)
FaceDetector.onFaces(List<Face>)
      │
      ▼  (cameraExecutor)
FilterEngine.latestFaces.set(faces)  // AtomicReference
                                     // next overlay draw picks it up
```

**Key properties:**
- Preview pipeline is **GPU-backed** — CameraX routes the camera sensor surface through `OverlayEffect`'s OpenGL shader internally. The app only provides a Canvas callback; CameraX converts that to a texture.
- ImageAnalysis runs at a **lower rate than Preview on purpose**. Preview draws at 30+ fps; analysis at ~15 fps is enough for bug tracking (bugs interpolate between face updates via animation state). `STRATEGY_KEEP_ONLY_LATEST` drops stale frames automatically.
- **Coordinate system: both branches use SENSOR.** MlKitAnalyzer emits face points in sensor coords; OverlayEffect canvas is already transformed to sensor space via `getSensorToBufferTransform()`. **Zero manual matrix math.**
- **Face buffer is `AtomicReference<FaceSnapshot>`** — no locks. Overlay thread reads whatever the analyzer wrote last.

### 2.2 Photo Capture Flow

```
User taps shutter
      │
      ▼
CameraViewModel.onShutter()
      │
      ▼
CaptureCoordinator.takePhoto()
      │
      ▼
ImageCapture.takePicture(outputOptions, executor, callback)
      │
      ▼  (CameraX internally — same OverlayEffect bound to ImageCapture)
  [HW still frame] → [OverlayEffect draws SAME canvas logic] → JPEG encoder
      │
      ▼
MediaStore URI written (DCIM/Bugzz/IMG_<ts>.jpg)
      │
      ▼
onImageSaved(outputResults) → VM → SharedFlow OneShotEvent.CaptureSaved(uri, PHOTO)
      │
      ▼
UI navigates to PreviewActivity(uri) or shows confirmation
```

**Critical insight (verified, HIGH confidence):** with `OverlayEffect` added to the `UseCaseGroup` including `ImageCapture`, the photo pipeline re-invokes the same `onDrawFrame` callback on the still frame. **We do NOT composite manually.** No `Bitmap.createBitmap() + Canvas.drawBitmap()` dance, no custom JPEG re-encoding.

### 2.3 Video Capture Flow

```
User toggles to Video mode, taps record
      │
      ▼
CaptureCoordinator.startRecording()
      │
      ▼
Recorder.prepareRecording(context, mediaStoreOutputOptions)
   .withAudioEnabled()
   .start(executor, videoRecordEventListener)
      │
      ▼  Recording handle returned; status events stream:
           VideoRecordEvent.Start → VM state = RECORDING
           VideoRecordEvent.Status (every ~1s — duration, bytes)
           ...
      ▼
(during recording — CameraX muxes Preview-surface-with-overlay into MP4)

Frame pipeline while recording:
  [Camera HW] → Preview UC → OverlayEffect → {PreviewView, VideoCapture encoder}
                                              (both share the same effected surface)

User taps stop
      │
      ▼
activeRecording.stop()
      │
      ▼
VideoRecordEvent.Finalize(outputResults, hasError, cause)
      │
      ▼
MediaStore URI (DCIM/Bugzz/VID_<ts>.mp4) → VM → OneShotEvent.CaptureSaved
```

**Audio:** `.withAudioEnabled()` on the `PendingRecording` — `RECORD_AUDIO` permission required at runtime alongside `CAMERA`. Audio track is muxed by CameraX; app writes zero audio code.

**Orientation:** Set `Recorder.Builder().setTargetRotation(display.rotation)` at bind time (or `VideoCapture.setTargetRotation()` dynamically on config change). No manual rotation in the overlay.

### Threading Model (explicit)

| Thread / Executor | What runs on it | Why |
|---|---|---|
| **Main (UI)** | Activity/Fragment lifecycle, `PreviewView` attach, CameraX bind call, observing `StateFlow` | CameraX binding, view operations must be main. |
| **cameraExecutor** (`Executors.newSingleThreadExecutor()`) | `MlKitAnalyzer` analyze callback, `ImageCapture.OnImageSavedCallback`, `VideoRecordEventListener` | Single thread → ordering guarantees, no locks around face buffer write. |
| **renderExecutor** (`Executors.newSingleThreadExecutor()`) | `OverlayEffect.onDrawFrame` Canvas draws | CameraX calls the effect callback on its own thread; giving it a dedicated executor keeps it off analyzer. |
| **ML Kit internal worker** | Actual face detection inference | Managed by ML Kit — we don't control it. Results delivered back to `cameraExecutor`. |
| **Dispatchers.IO (coroutines)** | DataStore reads, asset decoding, MediaStore queries | Disk I/O must not block main. |
| **Dispatchers.Default** | Animation state math (if CPU-heavy, unlikely) | Not needed in MVP; renderExecutor handles it inline. |

Shared state between threads:
- `FilterEngine.latestFaces: AtomicReference<FaceSnapshot>` — written by cameraExecutor, read by renderExecutor.
- `FilterEngine.currentFilter: AtomicReference<FilterDef>` — written by main (via VM), read by renderExecutor.
- No `synchronized`, no `Mutex`. Atomics are enough because we never need to read-modify-write across threads.

---

## 3. Rendering Pipeline — Decision: CameraX `OverlayEffect`

### Options evaluated

| Approach | Preview | Photo composite | Video composite | Effort | Verdict |
|---|---|---|---|---|---|
| **Canvas overlay on top of PreviewView** (sibling `View` with `onDraw`) | Easy | **Must re-implement** — capture JPEG then redraw onto Bitmap, re-encode | **Must re-implement** — requires custom MediaMuxer pipeline or Media3 Transformer post-step | Medium upfront, high for capture | REJECT |
| **GLSurfaceView + OpenGL ES custom shader** | Full control, fastest | Must render to offscreen FBO, readPixels, encode | Must feed encoder Surface via MediaCodec | Very high | REJECT (overkill for sprites) |
| **Custom `SurfaceView` compositing** with manual camera Surface forwarding | Medium | Same as GL — manual | Same as GL — manual | Very high | REJECT |
| **CameraX `OverlayEffect`** (camera-effects 1.4.0+) | Canvas callback | **Automatic** — same effect on ImageCapture | **Automatic** — same effect on VideoCapture | Low | ✅ **CHOSEN** |

### Why OverlayEffect wins

**From official Android Developers documentation and CameraX 1.4.0+ release notes (HIGH confidence):**
- `OverlayEffect` accepts targets `PREVIEW | VIDEO_CAPTURE | IMAGE_CAPTURE` via `CameraEffect.PREVIEW or CameraEffect.IMAGE_CAPTURE or CameraEffect.VIDEO_CAPTURE`.
- **One `onDrawFrame(frame, overlayCanvas)` callback covers all three use cases.** Single source of truth for how bugs look.
- `frame.getSensorToBufferTransform()` returns a `Matrix` you apply to the canvas — handles all rotation/flip/scale concerns. Pair with `MlKitAnalyzer(COORDINATE_SYSTEM_SENSOR)` and landmarks land correctly across every device orientation and both lenses.
- Runs on CameraX's internal GL thread; your Canvas draws are converted to a texture and composited into the pipeline output. No app-level encoder, no muxer, no readPixels.
- Used together with `VideoCapture` + `Recorder`, the MP4 includes the overlay baked in. No post-processing.

### Trade-offs honestly

- **Requires CameraX 1.4.0+** (stable Dec 2024). `androidx.camera:camera-effects:1.4.x` + `androidx.camera:camera-video:1.4.x`. Since we're greenfield and minSdk 28, no blocker.
- **Canvas API, not OpenGL.** Cannot do pixel shaders, bloom, warp. For our use case (PNG sprite bugs crawling on face) Canvas is more than enough. `Canvas.drawBitmap(bug, matrix, paint)` per bug, ~5–20 bugs per frame = trivial.
- **Still frame drawing is synchronous per capture** — if `FilterEngine.draw()` takes 50ms for a still, that 50ms is added to capture latency. Keep draw logic identical and fast; no special "render hero frame" branch.

### Integration sketch

```kotlin
// In CameraController.bind()
val overlayEffect = OverlayEffect(
    targets = CameraEffect.PREVIEW or
              CameraEffect.IMAGE_CAPTURE or
              CameraEffect.VIDEO_CAPTURE,
    queueDepth = 0,
    executor = renderExecutor,
    onFrameAvailableListener = { /* unused */ }
).apply {
    setOnDrawListener { frame ->
        val canvas = frame.overlayCanvas
        canvas.setMatrix(frame.sensorToBufferTransform)
        filterEngine.draw(canvas, frame.timestampNanos)
        true // = "I drew something"
    }
}

val mlkitAnalyzer = MlKitAnalyzer(
    listOf(faceDetectorClient),
    COORDINATE_SYSTEM_SENSOR,
    cameraExecutor
) { result ->
    val faces = result.getValue(faceDetectorClient) ?: emptyList()
    filterEngine.latestFaces.set(FaceSnapshot(faces, System.nanoTime()))
}

imageAnalysis.setAnalyzer(cameraExecutor, mlkitAnalyzer)

val useCaseGroup = UseCaseGroup.Builder()
    .addUseCase(preview)
    .addUseCase(imageAnalysis)
    .addUseCase(imageCapture)
    .addUseCase(videoCapture)
    .addEffect(overlayEffect)
    .build()

cameraProvider.bindToLifecycle(lifecycleOwner, lensSelector, useCaseGroup)
```

---

## 4. Filter Model

### Data classes

```kotlin
enum class FaceAnchor {
    LEFT_EYE, RIGHT_EYE, NOSE_TIP, MOUTH_BOTTOM,
    LEFT_CHEEK, RIGHT_CHEEK, FOREHEAD,
    LEFT_EAR, RIGHT_EAR, CHIN,
    FACE_CONTOUR  // walk along the oval
}

enum class BugBehavior {
    CRAWL,     // slow walk along contour / cheek
    SWARM,     // many bugs, random drift + mean pull toward anchor
    FALL,      // spawn above face, drop down
    STATIC     // sit on anchor, mild idle animation
}

data class SpriteFrame(val assetPath: String, val durationMs: Int)

data class BugConfig(
    val id: String,                       // "ant_1", "spider_big", ...
    val frames: List<SpriteFrame>,        // flipbook animation
    val widthPx: Int,                     // size on face (scaled by face box)
    val anchor: FaceAnchor,
    val behavior: BugBehavior,
    val speedDpPerSec: Float = 40f,
    val instanceCount: Int = 1            // how many of this bug to spawn
)

data class FilterDef(
    val id: String,                       // "ants_on_face", "spider_crawl"
    val name: String,                     // display name
    val thumbnailAsset: String,
    val bugs: List<BugConfig>             // 1..N bugs make up a filter
)
```

### Animation — recommendation: **simple per-instance state machine, NO physics engine**

```kotlin
class BugInstance(
    val config: BugConfig,
    startPos: PointF
) {
    var pos: PointF = startPos
    var velocity: PointF = PointF(0f, 0f)
    var frameIndex: Int = 0
    var frameElapsedMs: Int = 0
    var pathProgress: Float = 0f  // 0..1 along anchor path, for CRAWL

    fun tick(dtMs: Int, face: Face?) {
        // 1. Advance flipbook frame
        frameElapsedMs += dtMs
        if (frameElapsedMs >= config.frames[frameIndex].durationMs) {
            frameElapsedMs = 0
            frameIndex = (frameIndex + 1) % config.frames.size
        }
        // 2. Update position by behavior
        when (config.behavior) {
            CRAWL  -> advanceAlongContour(face, dtMs)
            SWARM  -> driftTowardAnchor(face, dtMs)
            FALL   -> applyGravity(dtMs)
            STATIC -> snapToAnchor(face)
        }
    }

    fun draw(canvas: Canvas, bitmap: Bitmap) {
        // rotate sprite to match direction of travel
        val angle = atan2(velocity.y, velocity.x).toDegrees()
        canvas.withRotation(angle, pos.x, pos.y) {
            canvas.drawBitmap(bitmap, pos.x - w/2, pos.y - h/2, paint)
        }
    }
}
```

**Why this over alternatives:**
- **Not pre-baked keyframes:** bug positions depend on live face, which moves. Keyframes would detach from tracking.
- **Not a physics engine (Box2D etc.):** massive overkill; frame budget tight (≤40ms total). Hand-rolled per-behavior math in ≤200 LOC total.
- **Flipbook for sprite animation:** matches how the reference APK almost certainly stores them (PNG sequences). Decode once via `AssetLoader`, cache `Bitmap` in `LruCache`.

### Tracking ↔ animation coupling

- `FilterEngine.draw(canvas, nowNs)`:
  1. Snapshot `latestFaces`.
  2. For each `BugInstance`, compute `dt = nowNs - lastDrawNs`.
  3. `instance.tick(dt, primaryFace)`.
  4. `instance.draw(canvas, AssetLoader.get(frame.assetPath))`.
- If no face detected: freeze bugs in place, play idle sprite frames, OR fade out after 500ms (TBD during phase 3 tuning).
- If face lost & regained: reset `pathProgress` for CRAWL bugs; others snap to new anchor.

---

## 5. Suggested Project Structure

```
app/
├── build.gradle.kts
└── src/main/
    ├── AndroidManifest.xml           # CAMERA, RECORD_AUDIO, MEDIA permissions
    ├── kotlin/com/bugzz/filter/camera/
    │   ├── BugzzApp.kt               # Application — inits DI container
    │   │
    │   ├── camera/                   # CameraX layer
    │   │   ├── CameraController.kt
    │   │   ├── CameraLensProvider.kt
    │   │   └── CameraExecutors.kt    # cameraExecutor, renderExecutor holders
    │   │
    │   ├── detector/                 # ML Kit wrapper
    │   │   ├── FaceDetectorClient.kt # MlKitAnalyzer construction
    │   │   ├── FaceSnapshot.kt       # data + atomic ref helper
    │   │   └── FaceLandmarkMapper.kt # Face → AnchorPoints in sensor coords
    │   │
    │   ├── render/                   # Overlay rendering
    │   │   ├── OverlayEffectBuilder.kt
    │   │   ├── FilterEngine.kt
    │   │   ├── BugInstance.kt
    │   │   ├── behaviors/
    │   │   │   ├── CrawlBehavior.kt
    │   │   │   ├── SwarmBehavior.kt
    │   │   │   ├── FallBehavior.kt
    │   │   │   └── StaticBehavior.kt
    │   │   └── AssetLoader.kt        # LruCache<String, Bitmap>
    │   │
    │   ├── capture/                  # Photo / Video
    │   │   ├── CaptureCoordinator.kt
    │   │   ├── PhotoCapture.kt
    │   │   ├── VideoCapture.kt       # Recorder + active Recording
    │   │   └── MediaStoreRepo.kt     # DCIM/Bugzz inserts
    │   │
    │   ├── filter/                   # Filter catalog
    │   │   ├── FilterDef.kt
    │   │   ├── BugConfig.kt
    │   │   └── FilterRepository.kt   # hardcoded list for MVP
    │   │
    │   ├── data/                     # Persistence
    │   │   ├── PrefsRepo.kt          # DataStore<Preferences>
    │   │   └── UserSettings.kt
    │   │
    │   ├── ui/
    │   │   ├── splash/
    │   │   │   └── SplashActivity.kt
    │   │   ├── home/
    │   │   │   └── HomeActivity.kt
    │   │   ├── camera/
    │   │   │   ├── CameraActivity.kt
    │   │   │   ├── CameraViewModel.kt
    │   │   │   ├── CameraUiState.kt
    │   │   │   ├── OneShotEvent.kt
    │   │   │   └── FilterPickerAdapter.kt
    │   │   ├── preview/
    │   │   │   ├── PreviewActivity.kt  # save/share screen
    │   │   │   └── PreviewViewModel.kt
    │   │   └── common/
    │   │       ├── PermissionHelper.kt
    │   │       └── ShareIntentFactory.kt
    │   │
    │   └── di/
    │       └── ServiceLocator.kt     # manual DI for MVP (Hilt optional)
    │
    └── res/
        ├── drawable/                 # icons, ic_shutter, etc.
        ├── layout/                   # activity_camera.xml, etc.
        ├── values/                   # strings, colors, themes
        └── raw/                      # optional: audio effects
    
    assets/
    ├── filters/                      # manifest JSON? or hardcoded in code
    └── sprites/
        ├── ant/
        │   ├── walk_0.png ... walk_7.png
        │   └── idle_0.png ...
        ├── spider/
        ├── cockroach/
        ├── worm/
        └── fly/
```

### Rationale

- **By-layer top-level** (`camera/`, `detector/`, `render/`, `capture/`, `ui/`) — matches the component diagram 1:1; each package has ≤5 files at MVP size. Easy to find things.
- **`filter/` separate from `render/`** because filter DATA (FilterDef, catalog) has zero dependency on rendering. Tests for FilterRepository don't pull in CameraX.
- **`render/behaviors/` sub-package** because we expect 4 behavior implementations; keeps `FilterEngine` file small.
- **`ui/` groups by screen** not by type (Activity vs ViewModel separated). Each screen folder has everything for that screen — easier navigation.
- **No `domain/` layer.** Clean Architecture would add use-case classes but for a UI-heavy camera app with almost no business rules, it's ceremony. `ViewModel` → `Controller/Coordinator/Engine` is already enough separation.

### Gradle module strategy

**Single `:app` module for MVP.** Splitting into `:core`, `:feature-camera`, `:feature-home` buys parallel compile but we have ~30 files; not worth the config cost. Revisit if file count exceeds ~150 or if we add monetization in a later milestone.

---

## 6. Architectural Patterns

### Pattern 1: Unidirectional State with `StateFlow` + `SharedFlow`

**What:** `CameraViewModel` exposes `StateFlow<CameraUiState>` (continuous state: selected filter, lens, recording status) and `SharedFlow<OneShotEvent>` (transient: "photo saved", "permission denied", "record error"). UI collects both in `repeatOnLifecycle(STARTED)`.

**When:** Always for Android MVVM in 2026.

**Trade-offs:** Slight ceremony vs LiveData, but covers one-shot events cleanly (LiveData doesn't).

```kotlin
data class CameraUiState(
    val lens: CameraLens = FRONT,
    val mode: CaptureMode = PHOTO,
    val selectedFilter: FilterDef,
    val availableFilters: List<FilterDef>,
    val recordingState: RecordingState = Idle,
    val permissionState: PermissionState = Checking
)

sealed class OneShotEvent {
    data class CaptureSaved(val uri: Uri, val type: MediaType) : OneShotEvent()
    data class CaptureFailed(val reason: String) : OneShotEvent()
    data object NavigateToSettings : OneShotEvent()
}
```

### Pattern 2: Single-writer Atomic Reference for cross-thread frame data

**What:** `AtomicReference<FaceSnapshot>` between analyzer thread and render thread. One writer, multiple readers allowed (though we have one).

**When:** Frame-rate-critical producer/consumer where stale-read is fine.

**Trade-offs:** No backpressure (caller drops on write), but that matches `STRATEGY_KEEP_ONLY_LATEST` semantics.

### Pattern 3: Service Locator for DI (MVP)

**What:** A single `ServiceLocator` object constructs CameraController, FilterRepository, etc. lazily.

**When:** Small app, solo dev, want to avoid Hilt/Koin setup cost.

**Trade-offs:** Harder to mock in tests vs Hilt; but for MVP we're not unit-testing camera code anyway. **Revisit** if monetization milestone adds many services.

### Pattern 4: Effect-driven capture (zero manual composition)

**What:** Rely entirely on `OverlayEffect` on `UseCaseGroup` for preview / still / video. Never `Bitmap` copy, never `MediaMuxer`, never `Canvas.drawBitmap(cameraFrame,...)`.

**When:** CameraX 1.4+ with camera-effects available — i.e., us.

**Trade-offs:** Ties us to CameraX version; gain is a massive complexity reduction. Worth it.

---

## 7. Build Order — Drives Phase Structure

### Dependency graph (strict order where arrows shown)

```
┌─────────────────────┐
│ A. Project skeleton │ (gradle, manifest, permissions, empty Activities)
└──────────┬──────────┘
           │
           ▼
┌─────────────────────────────┐
│ B. Camera preview pipeline  │ ← blocks all others
│    - CameraController       │
│    - PreviewView            │
│    - Permission flow        │
└──────────┬──────────────────┘
           │
           ├──────────────┬───────────────────────┐
           ▼              ▼                       ▼
┌──────────────────┐ ┌───────────────────┐ ┌──────────────────┐
│ C. Face detect   │ │ D. OverlayEffect  │ │ E. Capture       │
│    MlKitAnalyzer │ │    stub (static   │ │    (photo basic, │
│    log landmarks │ │    red dot)       │ │    no overlay)   │
└──────────┬───────┘ └─────────┬─────────┘ └──────────────────┘
           │                   │
           └─────────┬─────────┘
                     ▼
         ┌───────────────────────────┐
         │ F. FilterEngine v1        │
         │    (one filter, one bug,  │
         │    anchor-locked sprite)  │
         └──────────┬────────────────┘
                    │
      ┌─────────────┼──────────────────┐
      ▼             ▼                  ▼
┌──────────┐ ┌─────────────┐ ┌────────────────────┐
│ G. Bug   │ │ H. Video    │ │ I. Filter catalog  │
│ behaviors│ │ capture +   │ │    (all N filters  │
│ (crawl,  │ │ audio mux   │ │    wired to picker)│
│ swarm..) │ │             │ │                    │
└─────┬────┘ └──────┬──────┘ └──────────┬─────────┘
      │             │                   │
      └─────────────┴───────────────────┘
                    │
                    ▼
         ┌─────────────────────────┐
         │ J. UI polish (splash,   │
         │    home, preview/save,  │
         │    share intent)        │
         └──────────┬──────────────┘
                    │
                    ▼
         ┌─────────────────────────┐
         │ K. Performance tuning   │
         │    (fps profiling,      │
         │    mid-tier device test)│
         └─────────────────────────┘
```

### Phase recommendations (feeds roadmap)

| Phase | Contents | Blocks | Exit criteria |
|---|---|---|---|
| **P1. Foundation** | A + B | Everything | Live camera preview on real device, both lenses, runtime permissions. |
| **P2. Detection + overlay primitives** | C + D | F, G | Face landmarks logged; static shape drawn via OverlayEffect anchored to nose tip. Coord system verified. |
| **P3. First filter end-to-end** | F + basic E (photo) | Rest | One filter (e.g. "ant on nose") renders, photo capture writes JPEG to DCIM with overlay baked in. |
| **P4. Filter catalog + behaviors** | G + I | J | All N filter types in picker (matching reference APK count), all 4 behaviors working. |
| **P5. Video + audio** | H | J | MP4 with audio + overlay written to DCIM. |
| **P6. UX polish** | J | K | Splash, home, camera, preview/save/share flows all wired. Matches reference navigation. |
| **P7. Performance & device matrix** | K | Ship | ≥24 fps on target mid-tier device; detection < 100ms/frame; no dropped frames during record. |

### Why this order

- **P1 blocks everything** — can't render without a surface.
- **P2 validates the coordinate system** with a cheap stub (red dot on nose). If `getSensorToBufferTransform()` + `COORDINATE_SYSTEM_SENSOR` pairing fails, find out here, not after 5 filters built.
- **P3 is the integration test** — if photo capture works with overlay baked, the hard architecture bet (OverlayEffect) is validated end-to-end. After P3 the rest is content work.
- **P5 (video) after P4 (filter variety)** because video capture reuses the same effect pipeline — no new render code. Video-specific risk is audio permission + Recording lifecycle + storage.
- **P6 UX last** because swapping out activities is trivial once the engine works.
- **P7 explicit** because performance is the Core Value per PROJECT.md. Not doing this inline because premature optimization without a feature-complete app leads to fake wins.

---

## 8. Persistence Strategy

| Data | Storage | Why |
|---|---|---|
| Captured photos | `MediaStore.Images.Media.EXTERNAL_CONTENT_URI` → `DCIM/Bugzz/` | Scoped storage mandatory Android 10+; gallery-visible; no WRITE_EXTERNAL_STORAGE permission needed on Q+. |
| Captured videos | `MediaStore.Video.Media.EXTERNAL_CONTENT_URI` → `DCIM/Bugzz/` | Same reason. |
| Last-used filter ID | `DataStore<Preferences>` | Survives process death; type-safe; async API. |
| Last-used camera facing | `DataStore<Preferences>` | Same. |
| Filter catalog | **In-code `object FilterCatalog`** (Kotlin) | Static for MVP; no remote config. Avoids Room for a 10-row dataset. |
| Sprite bitmaps | `assets/sprites/...` + `LruCache<String, Bitmap>` in `AssetLoader` | Shipped with APK; decoded lazily on camera open, cached until activity destroyed. |

**No Room DB for MVP.** No capture history (MediaStore is the history). No filter favorites (can add later to DataStore if needed). If a "saved filter presets" feature emerges post-MVP → Room.

**Permission handling:**
- `CAMERA`: runtime, requested before bind.
- `RECORD_AUDIO`: runtime, requested before switching to video mode (lazy — don't ask on app launch).
- `POST_NOTIFICATIONS` (Android 13+): only if we add foreground recording notification. Not needed for MVP in-app recording.
- **No `WRITE_EXTERNAL_STORAGE`** on Android 10+ thanks to MediaStore. Reference manifest has it likely for pre-Q support; since our minSdk=28 overlaps Android 9 we may need it conditionally with `maxSdkVersion="28"`.

---

## 9. Anti-Patterns (domain-specific)

### Anti-Pattern 1: "Screenshot the overlay view after capture"

**What people do:** Capture JPEG with `ImageCapture`, then take a screenshot of the overlay View, composite in code, re-encode JPEG.
**Why it's wrong:** Two captures are never the same frame — face will have moved 16ms+; overlay aligns wrong. Also: re-encode quality loss, extra CPU, visible latency.
**Do this instead:** Use `OverlayEffect` with `IMAGE_CAPTURE` target. One frame, one compose, one JPEG.

### Anti-Pattern 2: "Sync face detection to preview frame rate"

**What people do:** Run face detection on every preview frame (30+ fps), blocking the analyzer queue.
**Why it's wrong:** ML Kit detection costs 30–80ms; queue backs up; preview stutters; battery drains.
**Do this instead:** `ImageAnalysis.Builder().setBackpressureStrategy(STRATEGY_KEEP_ONLY_LATEST)`. Let analyzer run at whatever rate ML Kit can keep up with (~15 fps). Interpolate bug positions across stale face data on the render thread.

### Anti-Pattern 3: "Translate ML Kit coordinates manually"

**What people do:** Read `PreviewView` width/height, compute scale factor, flip X for front lens, rotate based on display rotation, apply translation — custom matrix code.
**Why it's wrong:** Each edge case (landscape, reverse landscape, wide-screen previews, split-screen) breaks it. Cross-device bugs take weeks to fix.
**Do this instead:** `MlKitAnalyzer(COORDINATE_SYSTEM_SENSOR)` + `overlayCanvas.setMatrix(frame.sensorToBufferTransform)`. Let CameraX own the math.

### Anti-Pattern 4: "Recreate `ImageCapture` / rebind on every shutter press"

**What people do:** `cameraProvider.unbindAll()` then `bindToLifecycle()` inside the tap handler.
**Why it's wrong:** Causes 300+ ms bind latency, pipeline flush, visible preview flash. Capture feels broken.
**Do this instead:** Bind all use cases (Preview + ImageCapture + VideoCapture + ImageAnalysis) once on start; keep them bound. Switching photo↔video just toggles which is active (or use mode-dependent bind — still do it at mode-switch, not per-capture).

### Anti-Pattern 5: "Draw overlay inside `ImageAnalysis.analyze()`"

**What people do:** Treat ImageAnalysis as the "render loop"; draw overlay from analyzer callback to a custom View.
**Why it's wrong:** Analyzer runs at 15 fps; overlay will look choppy. Also tightly couples detection and render threads — if detection is slow, overlay freezes.
**Do this instead:** Analyzer only updates the `AtomicReference<FaceSnapshot>`. OverlayEffect draws at preview fps using the latest snapshot (even if stale by 1–2 frames).

### Anti-Pattern 6: "Use `FileOutputStream` for saved captures"

**What people do:** `new File(Environment.getExternalStoragePublicDirectory(DCIM), "bug.jpg")`.
**Why it's wrong:** Broken on Android 10+ (scoped storage), requires legacy flag that doesn't work on 11+.
**Do this instead:** `MediaStoreOutputOptions` (for video) / `ImageCapture.OutputFileOptions` with `MediaStore.Images.Media.EXTERNAL_CONTENT_URI` (for photo). CameraX writes the URI; no File API.

---

## 10. Integration Points

### External services / APIs

| Service | Integration Pattern | Notes |
|---|---|---|
| ML Kit Face Detection | `MlKitAnalyzer` bridge (CameraX 1.2+) | Use `COORDINATE_SYSTEM_SENSOR` with `OverlayEffect`. Ship bundled model (`com.google.mlkit:face-detection:16.1.x`) not unbundled — no Play Services dependency, works offline. |
| MediaStore | `ContentResolver.insert()` via CameraX `OutputFileOptions` / `MediaStoreOutputOptions` | Targets `DCIM/Bugzz/`. No permission on Q+. |
| Android Share Sheet | `Intent.ACTION_SEND` with `content://` URI from MediaStore | `FLAG_GRANT_READ_URI_PERMISSION`. MIME `image/jpeg` or `video/mp4`. |
| (Deferred) AdMob / AppLovin / Billing | N/A for MVP | Milestone 2. Architecturally: separate `:monetization` module injected into UI; doesn't touch camera/render layers. |

### Internal boundaries

| Boundary | Communication | Notes |
|---|---|---|
| UI ↔ ViewModel | `StateFlow` (state) + `SharedFlow` (events) + method calls (intents) | No direct Activity references in VM. |
| ViewModel ↔ CameraController | Kotlin suspend fns + flows | VM passes lifecycle-scoped `LifecycleOwner`; controller owns the binding. |
| CameraController ↔ FilterEngine | Direct reference, injected at construct | FilterEngine is passed into `OverlayEffect.onDraw`. |
| FaceDetector → FilterEngine | `AtomicReference` write | Only cross-thread comm point; lock-free. |
| Capture → ViewModel | `suspend` fn returning `Result<Uri>` wrapped in `OneShotEvent` emit | VM awaits, emits. |
| AssetLoader ↔ anything | Blocking `get(path) → Bitmap`; pre-warm on camera open | LruCache, thread-safe. |

---

## 11. Scaling Considerations

Not user-count scaling (single-device app) — instead **frame-rate / feature scaling**:

| Scale | Adjustments |
|---|---|
| **1 filter, 1 bug** (P3) | Trivial; draw in <1ms. |
| **~15 filters × up to 10 bugs on screen** (target per reference APK) | Flipbook animation, sprite atlas (optional), pre-decoded bitmaps. ~3–5ms draw per frame — well within budget. |
| **30 bugs + effects** (e.g. swarm worst case) | `Canvas.drawBitmap` becomes hot — switch to `drawBitmapMesh` or a `TextureView` + GL path. Only if profiling shows >16ms. |
| **Multi-face filters (2+ faces)** | `FaceDetectorOptions.Builder().setPerformanceMode(FAST)` already supports multiple; bump bug instance count per detected face. Render cost scales linearly. |

### First bottleneck (measured expectations)

1. **ML Kit detection on low-end devices** (~80ms/frame on Cortex-A53). Mitigation: already handled by `KEEP_ONLY_LATEST` + interpolation. If worse, drop from CONTOUR_ALL to LANDMARK_ALL (fewer points, faster).
2. **Bitmap decode on first filter switch** — decoding 8 frames × 5 bugs = 40 PNGs at full res blows frame budget. Mitigation: decode off-thread, pre-warm on camera open, cache in LRU. Use `BitmapFactory.Options.inSampleSize` if sprite is oversized.
3. **Video encoder throughput on Android 9 devices** — budget Qualities.SD or HD (not FHD) for minSdk=28 fallback. `Recorder.Builder().setQualitySelector(QualitySelector.fromOrderedList(listOf(HD, SD)))`.

---

## 12. Key Decisions Summary

| Decision | Choice | Alternative Rejected | Driver |
|---|---|---|---|
| Rendering pipeline | CameraX `OverlayEffect` (camera-effects 1.4+) | Canvas-on-View, GLSurfaceView, Compose Canvas | Single effect covers preview + photo + video; no manual compose |
| Coordinate system | `COORDINATE_SYSTEM_SENSOR` + `sensorToBufferTransform` | Manual matrix on PreviewView scale | CameraX owns the math; zero device-specific bugs |
| Face detector | ML Kit bundled + CONTOUR_ALL | ARCore Augmented Faces, Banuba SDK | Free, offline, matches PROJECT.md stack lock |
| Architecture | MVVM + StateFlow/SharedFlow | MVI (Orbit/Mavericks), Clean Arch | Idiomatic 2026 Android; minimal ceremony for small app |
| DI | Service Locator object | Hilt, Koin | MVP size; revisit at monetization milestone |
| Persistence | DataStore + MediaStore (no DB) | Room | No relational data for MVP |
| Threading | 3 executors + atomic face ref | Channels, single executor | Lock-free; matches CameraX thread model |
| UI toolkit | **Views + XML** (not Compose) | Jetpack Compose | CameraX-Compose stable 1.5.1 (Nov 2025) but reference app is Views; staying in Views lowers risk for clone-parity; PROJECT.md leaves this open so we pick Views. |

### UI toolkit note (Views vs Compose)

CameraX + Compose via `CameraXViewfinder` reached stable in 1.5.1 (Nov 2025, HIGH confidence). Both are viable. **Recommendation: Views with XML** for this project because:
- Reference APK is Views-based — easier 1:1 visual clone.
- `PreviewView` + `SurfaceView` backend is mature, well-documented, no surprises at minSdk 28.
- Activity-per-screen maps cleanly to the reference's navigation.
- Compose's win (state-driven UI) is less impactful for this app's mostly-static chrome around a camera preview.

If chosen-project-owner prefers Compose, it's not architecturally blocking — swap `ui/` package contents; rest of the layers untouched.

---

## Sources

### Official documentation (HIGH confidence)

- [CameraX Releases — AndroidX Jetpack](https://developer.android.com/jetpack/androidx/releases/camera) — 1.4.0 stable, 1.5.x video/compose features
- [OverlayEffect API reference](https://developer.android.com/reference/kotlin/androidx/camera/effects/OverlayEffect) — targets, setOnDrawListener, Frame
- [CameraX Architecture](https://developer.android.com/media/camera/camerax/architecture) — UseCaseGroup, effects, lifecycle
- [CameraX video capturing architecture](https://developer.android.com/media/camera/camerax/video-capture) — Recorder, Recording, VideoRecordEvent
- [Detect faces with ML Kit on Android](https://developers.google.com/ml-kit/vision/face-detection/android) — FaceDetectorOptions, contours, landmarks
- [Access media files from shared storage](https://developer.android.com/training/data-storage/shared/media) — MediaStore, DCIM, scoped storage

### Release notes / announcements (HIGH confidence)

- [What's new in CameraX 1.4.0 (Dec 2024)](https://android-developers.googleblog.com/2024/12/whats-new-in-camerax-140-and-jetpack-compose-support.html) — camera-effects, OverlayEffect, getSensorToBufferTransform
- [Introducing CameraX 1.5 (Nov 2025)](https://android-developers.googleblog.com/2025/11/introducing-camerax-15-powerful-video.html) — camera-compose stable, composition settings
- [CameraX 1.3 Beta — CameraEffect framework](https://android-developers.googleblog.com/2023/06/camerax-13-is-now-in-beta.html) — the original effect plumbing

### Community / sample code (MEDIUM confidence — verified against official)

- [Google ML Kit samples — GraphicOverlay.java](https://github.com/googlesamples/mlkit/blob/master/android/vision-quickstart/app/src/main/java/com/google/mlkit/vision/demo/GraphicOverlay.java) — reference coordinate transform code (note: pre-OverlayEffect era; our approach is simpler)
- [camera-mlkit-vision library announcement](https://groups.google.com/a/android.com/g/camerax-developers/c/PFBwGxCbadY) — MlKitAnalyzer origin
- [CameraX Preview overlay and saved Video Capture Overlay thread](https://groups.google.com/a/android.com/g/camerax-developers/c/64eahzvdY4U) — confirms OverlayEffect applies to all three use cases

### Intentionally not sourced

- Claims about reference APK internals are from PROJECT.md context only — manifest verified via reference/manifest.json; actual layout/sprite analysis is a separate research task (not this document).

---

*Architecture research for: Android AR face filter camera app (Bugzz)*
*Researched: 2026-04-18*
