# Phase 3: First Filter End-to-End + Photo Capture — Research

**Researched:** 2026-04-19
**Domain:** Android AR face filter rendering pipeline (OverlayEffect + Canvas) + ImageCapture → MediaStore + ADR-01 BboxIouTracker follow-up
**Confidence:** HIGH (all architecture decisions downstream of Phase 2 locks + CONTEXT D-01..D-40; external unknowns are limited to Xiaomi 13T/HyperOS runtime inspection + apktool output which are explicit Wave 0 tasks)

---

## Summary

Phase 3 implements exactly what Phase 2 planted seams for: a production `FilterEngine` drawing animated bug sprites through the **same** `OverlayEffect` that Phase 2 validated for `PREVIEW | IMAGE_CAPTURE | VIDEO_CAPTURE` target baking, plus an `ImageCapture.takePicture()` shutter that saves a JPEG to `DCIM/Bugzz/` via CameraX's built-in `MediaStoreOutputOptions` writer, plus the **mandatory ADR-01 follow-up** (`BboxIouTracker` + re-keyed `LandmarkSmoother`). Every major technical decision was pre-locked in `03-CONTEXT.md` D-01..D-40; this document specifies the exact Kotlin shapes, verifies version/API contracts, and writes the Validation Architecture that `gsd-planner` will consume.

**The four research findings that drive planning:**

1. **`OverlayEffect.setOnDrawListener` fires uniformly for every target in its target mask.** The `Frame` object exposes `overlayCanvas`, `sensorToBufferTransform`, and `timestamp` (nanos) — **but NOT `targetType`** (training data + `Frame` API searches confirm no such field). When `ImageCapture.takePicture()` triggers, the effect pipeline invokes the **same** callback on the still-frame buffer; CameraX handles target-switching internally. Phase 2's `OverlayEffectBuilder.setOnDrawListener` is already correct for Phase 3 — no target-branching is needed or possible. [VERIFIED: Phase 2 gaps-03 MP4 validation — red rect baked into VIDEO_CAPTURE stream via identical callback]
2. **CameraX 1.6 `ImageCapture.OutputFileOptions` handles `IS_PENDING` transaction automatically.** Caller supplies `ContentResolver` + collection URI + `ContentValues` (DISPLAY_NAME, MIME_TYPE, RELATIVE_PATH only — no `IS_PENDING` set manually); CameraX does insert → stream write → update under the hood. `OnImageSavedCallback.onImageSaved(OutputFileResults)` delivers `savedUri` (a `MediaStore` content URI ready for `Intent.ACTION_VIEW` / `ACTION_SEND` without `FileProvider`). [CITED: developer.android.com/media/camera/camerax/take-photo]
3. **ML Kit `FaceContour` constants are integer types `FACE=1, LEFT_EYEBROW_TOP=2, LEFT_EYEBROW_BOTTOM=3, RIGHT_EYEBROW_TOP=4, RIGHT_EYEBROW_BOTTOM=5, LEFT_EYE=6, RIGHT_EYE=7, UPPER_LIP_TOP=8, UPPER_LIP_BOTTOM=9, LOWER_LIP_TOP=10, LOWER_LIP_BOTTOM=11, NOSE_BRIDGE=12, NOSE_BOTTOM=13, LEFT_CHEEK=14, RIGHT_CHEEK=15`.** Phase 2's `SMOOTHED_CONTOUR_TYPES` list already enrolls FACE/NOSE_BRIDGE/NOSE_BOTTOM/LEFT_EYE/RIGHT_EYE/LEFT_CHEEK/RIGHT_CHEEK/UPPER_LIP_TOP/LOWER_LIP_BOTTOM. For Phase 3's FOREHEAD anchor, `LEFT_EYEBROW_TOP` (contour type 2) + `RIGHT_EYEBROW_TOP` (contour type 4) must be **added** to `SMOOTHED_CONTOUR_TYPES`, else `FaceLandmarkMapper.anchorPoint(face, FOREHEAD)` will always hit the boundingBox fallback. [CITED: developers.google.com/android/reference/com/google/mlkit/vision/face/FaceContour]
4. **BboxIouTracker's greedy O(F × D) assignment is sufficient for `MAX_TRACKED_FACES = 2`.** Hungarian algorithm is unnecessary overhead for ≤2 faces × ≤2 detections; greedy best-IoU-first produces identical output at that scale. Per-frame worst case: 4 IoU calculations + 2 sorts of 4 = ~10μs on Kotlin JVM — nowhere near the ML-Kit 30-80ms detection budget. [VERIFIED: IoU-based trackers are the standard approach at this scale per MediaPipe + ByteTrack lineage]

**Primary recommendation:** Plan Phase 3 as a Wave 0 asset+runbook prep → Wave 1 BboxIouTracker+re-key (ADR-01 close) → Wave 2 FilterEngine+AssetLoader+Catalog → Wave 3 ImageCapture production path → Wave 4 clean build + handoff. Keep every new class as `@Singleton @Inject`, following the Phase 2 constructor-split pattern (primary `internal constructor(..., seam: T)` + secondary `@Inject constructor(...)` hard-coding production factory). Test everything FilterEngine-downstream in pure JVM JUnit where possible; Robolectric only where CameraX 1.6 internals demand it (proven pattern from Phase 2 Plan 04).

---

## User Constraints (from CONTEXT.md)

### Locked Decisions (D-01..D-40 — not negotiable)

Copied verbatim from `03-CONTEXT.md`:

**Filter #1 spec (D-01..D-04):**
- D-01: First filter is **ant-on-nose, STATIC**. Id `ant_on_nose_v1`. Anchor `NOSE_TIP` (from `FaceContour.NOSE_BRIDGE` last point with `NOSE_BOTTOM` fallback). Scale ~20% face-box width.
- D-02: Filter #2 is **spider-on-forehead, STATIC**. Id `spider_on_forehead_v1`. Anchor `FOREHEAD` = midpoint(LEFT_EYEBROW_TOP first, RIGHT_EYEBROW_TOP last) offset up ~15% face-box height. Exists for REN-07 swap verification.
- D-03: Both sprites are **animated flipbook** (multi-frame PNG sequence). `FrameLoop` at research-prescribed timing — Claude chooses timing source; this research locks `OverlayEffect.Frame.timestamp` (nanos) as the clock per Claude's discretion bullet §Claude's Discretion.
- D-04: `BugBehavior` sealed interface ships with **all 4 variants declared** (STATIC, CRAWL, SWARM, FALL); `BugState` data class sized for all; **only STATIC has implemented body** in Phase 3. CRAWL/SWARM/FALL bodies `TODO("Phase 4")`.

**Asset pipeline (D-05..D-09):**
- D-05: Extract 2 bug sprites (ant + spider) from reference APK in Wave 0 via `apktool`. Not more — asset scale-up is Phase 4.
- D-06: Layout = `app/src/main/assets/sprites/<filterId>/frame_00.png ... frame_NN.png` + `manifest.json` with `id, displayName, frameCount, frameDurationMs, anchorType, behavior, scaleFactor, mirrorable`.
- D-07: Single-frame fallback if reference only has one frame; `FilterEngine` supports `frameCount = 1`.
- D-08: Decode `Bitmap.Config.ARGB_8888` default (RGB_565 opt-in via manifest). Decode on `cameraExecutor`; cache `LruCache<String,Bitmap>` keyed `"$filterId/frame_$idx"`.
- D-09: LruCache max = 32 MB (capped at `Runtime.getRuntime().maxMemory()/8`).

**REN-07 Filter swap (D-10..D-11):**
- D-10: Debug-only `Cycle Filter` button `BuildConfig.DEBUG` gated, BottomEnd. Handoff validates swap <1 frame, no freeze, no rebind, no ghost.
- D-11: `FilterEngine.setFilter(filterId)` writes `AtomicReference<FilterDefinition>`. `AssetLoader.preload(filterId)` invoked synchronously inside `setFilter()` on cameraExecutor. If preload incomplete when next frame draws, **skip drawing that frame** (no ghost).

**Shutter UX (D-12..D-16):**
- D-12: Stay on CameraScreen post-capture. Toast in English ("Saved to gallery"). Optional Snackbar with "View" action.
- D-13: Shutter button: **72dp circle, white fill, gray stroke 2dp**, bottom-center, 24dp bottom padding. Z-order above CameraXViewfinder.
- D-14: Phase 2 TEST RECORD button kept, moved to BottomStart (debug-gated unchanged).
- D-15: Haptic-only on tap: `HapticFeedbackConstants.LONG_PRESS`. **No MediaActionSound.**
- D-16: Optional 150ms white overlay alpha-fade capture flash in Compose (NOT in OverlayEffect — UI concern).

**Front-cam mirror (D-17..D-19):**
- D-17: **Wave 0 task** — install reference APK on Xiaomi 13T, capture front-cam photo, compare mirror convention. Default implementation = mirror. Override if inspection reveals un-mirrored.
- D-18: Default front-cam JPEG mirrored (matches preview). Back-cam not mirrored. Verify via device.
- D-19: Non-default findings go to `03-SUMMARY.md` + amend `PITFALLS.md`.

**BboxIouTracker (D-20..D-26 — ADR-01 mandatory):**
- D-20: `@Singleton @Inject class BboxIouTracker`. API: `fun assign(faces: List<Face>): List<TrackedFace>` where `TrackedFace = (id: Int, face: Face)`. Maintains `Map<Int, TrackedEntry(lastBoundingBox, framesSinceLastSeen, scheduledForRemoval)>`.
- D-21: `IOU_MATCH_THRESHOLD = 0.3f`. `MAX_DROPOUT_FRAMES = 5`. Both `companion object const val` for unit test pinning.
- D-22: `MAX_TRACKED_FACES = 2`. When more detected, retain 2 largest-bbox faces.
- D-23: ID assignment per frame: greedy best-IoU ≥ threshold match → assign existing ID; unmatched detected → `nextId++` (respect MAX cap, drop lowest-IoU); unmatched tracked → increment dropout counter, mark for removal after threshold.
- D-24: **Primary face only draws filter in Phase 3.** Primary = largest-bbox tracked face. Secondary tracked but not drawn.
- D-25: `LandmarkSmoother` re-keys to tracker ID (replace `-1` sentinel). `onFaceLost(id)` clears that id's filter state. Reappearing same id → re-initialize fresh (algorithm D-23 guarantees removed IDs are not recycled, but defensive code).
- D-26: Wire tracker inside `FaceDetectorClient.createAnalyzer()` MlKitAnalyzer consumer **before** mapping to SmoothedFace. Replace `SmoothedFace.trackingId` with tracker-assigned id (no nullable). Update VERIFICATION CAM-08 row post-handoff.

**Filter engine architecture (D-27..D-30):**
- D-27: New `@Singleton @Inject class FilterEngine` replaces DebugOverlayRenderer for production. DebugOverlayRenderer **retained** + gated by `BuildConfig.DEBUG && prefs.debugOverlayEnabled`; both can draw in same frame (FilterEngine first, Debug on top).
- D-28: `FilterEngine.onDraw(canvas, frame, face: SmoothedFace?)` signature matches DebugOverlayRenderer. `face == null` → early return (REN-06). Caller already calls `canvas.setMatrix(frame.sensorToBufferTransform)`.
- D-29: `FilterDefinition` immutable data class: `id, displayName, anchorType, behavior, frameCount, frameDurationMs, scaleFactor, assetDir, mirrorable`.
- D-30: `FaceLandmarkMapper.anchorPoint()` production body — NOSE_TIP from NOSE_BRIDGE last / NOSE_BOTTOM fallback / boundingBox center ultimate fallback. FOREHEAD = mean(LEFT_EYEBROW_TOP[0], RIGHT_EYEBROW_TOP[last]) offset up 15% bbox height. LEFT_CHEEK / RIGHT_CHEEK = `FaceContour.FACE` inner contour at 40% jaw progress. Returns null only if boundingBox is null (never).

**Photo capture pipeline (D-31..D-36):**
- D-31: Use **CameraX `ImageCapture.OutputFileOptions(contentResolver, IMAGE_CONTENT_URI, contentValues)` built-in writer** (preferred over manual IS_PENDING insert+update — CameraX 1.6 handles the transaction). MIUI fallback to manual pattern if Xiaomi 13T has a ContentResolver quirk.
- D-32: Fields: `RELATIVE_PATH="DCIM/Bugzz"`, `DISPLAY_NAME="Bugzz_YYYYMMDD_HHmmss.jpg"`, `MIME_TYPE="image/jpeg"`. No WRITE_EXTERNAL_STORAGE (minSdk 28 + MediaStore covers).
- D-33: IMAGE_CAPTURE target validated Phase 2 (STATE #10). Confirm on-device that bug bakes into JPEG.
- D-34: Capture draw reads **same** `AtomicReference<FaceSnapshot>` as PREVIEW. Head motion during capture → lag by one detector frame (~33ms). Acceptable.
- D-35: Capture callback on `cameraExecutor`; ViewModel receives via existing `Channel<OneShotEvent>` → UI Toast.
- D-36: CAP-06 leak verification = **manual handoff runbook** (30 taps → kill app → LeakCanary notification absent). No instrumented test.

**REN-08 performance (D-37..D-38):**
- D-37: Phase 3 = subjective smoothness + optional profiler trace. Formal measured-fps gate is Phase 7.
- D-38: No in-app FPS HUD (same as Phase 2 D-03).

**Architecture placement (D-39..D-40):**
- D-39: New files: `render/FilterEngine.kt`, `render/BugBehavior.kt`, `render/BugState.kt`, `filter/FilterDefinition.kt`, `filter/FilterCatalog.kt`, `filter/AssetLoader.kt`, `detector/BboxIouTracker.kt`, optional `capture/PhotoCaptureService.kt`. Unit tests for: BboxIouTracker, FilterEngine.onDraw, AssetLoader, FaceLandmarkMapper.anchorPoint, FilterCatalog.
- D-40: `CameraModule` gains bindings (or classes are constructor-injected). `FaceDetectorClient` constructor gets `BboxIouTracker`. `OverlayEffectBuilder` gains `FilterEngine` alongside `DebugOverlayRenderer`.

### Claude's Discretion (research locks these below)

- Coroutine plumbing for `AssetLoader.preload()` — **locked: `suspend fun preload(filterId)` on `cameraExecutor.asCoroutineDispatcher()`** (thread consolidation D-18; no new executor).
- Capture-flash animation — **locked: `AnimatedVisibility(visible=flashVisible)` over the Box, 150ms tween; `flashVisible` toggled in ViewModel on shutter tap**.
- `PhotoCaptureService` class vs methods on `CameraController` — **locked: methods on `CameraController`** (Phase 2 owns all CameraX state; keeps use-case refs co-located). Deviation OK if Claude finds cleaner separation, but default is in-controller.
- `ImageCapture.OnImageSavedCallback` wrapper — **locked: `suspendCancellableCoroutine<Uri>` wrapper on `cameraExecutor`**. Simple; matches Phase 2 `ListenableFuture.await()` pattern.
- LruCache size formula — **locked: `min(32 * 1024 * 1024, (Runtime.getRuntime().maxMemory() / 8).toInt())`**. `sizeOf` returns `bitmap.allocationByteCount` (not `byteCount` — see Pitfall #2 below).
- Cycle Filter button label — **locked: `OutlinedButton { Text("Cycle") }`** (Phase 2 fallback pattern).
- Shutter button ripple — **locked: default Material3 `IconButton`-equivalent clickable ripple** on the Modifier.
- FrameLoop timing source — **locked: `OverlayEffect.Frame.timestamp` (nanos) as clock.** Zero allocation in draw path.
- `FilterDefinition.mirrorable` — **locked: default `true` for all Phase 3 filters** (both ant and spider). Phase 4 audits per filter.
- DebugOverlayRenderer kill-switch — **locked: keep wired + `BuildConfig.DEBUG` gated** (zero release footprint).
- `manifest.json` parsing — **locked: `kotlinx.serialization.json.Json` (already in catalog from Phase 1 D-09 for nav routes)**. Verified present in `libs.versions.toml`.
- Bitmap decode dispatcher — **locked: `cameraExecutor.asCoroutineDispatcher()`** (thread consolidation).

### Deferred Ideas (OUT OF SCOPE — copied from CONTEXT)

- Filter catalog LazyRow picker UI — Phase 4 CAT-01..05
- 15-25 bug filter asset extract — Phase 4
- CRAWL / SWARM / FALL behavior bodies — Phase 4
- Face/Insect mode selector on Home — Phase 4 + 5
- Production PreviewRoute with Save/Share/Delete/Retake — Phase 6
- MediaActionSound shutter + global sound toggle — Phase 6 UX-09
- FPS HUD in-app — never (profiler policy)
- Formal measured ≥24fps (PRF-01) — Phase 7
- Instrumented CAP-06 30-capture leak test — Phase 7 if gap detected
- Video recording production UX — Phase 5
- Filter thumbnails — Phase 4
- DataStore last-used-filter persistence — Phase 4
- MIUI/OEM ImageCapture workarounds — Phase 7 cross-OEM matrix
- Custom GL CameraEffect escalation — Phase 7
- BugBehavior per-filter parameterization — Phase 4
- Bitmap pool / sharing — Phase 7 perf
- EXIF tag audit — Phase 6

---

## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| REN-01 | `FilterEngine` draws bug sprites onto overlay Canvas via `OverlayEffect.onDrawListener` | §Architecture Patterns (FilterEngine.onDraw signature); §Code Examples (FilterEngine dispatch snippet) |
| REN-02 | Per-bug state machine supports 4 behaviors: STATIC, CRAWL, SWARM, FALL | §Architecture Patterns (sealed interface + BugState); D-04 lock (STATIC impl + 3 TODO stubs) |
| REN-03 | Bugs anchor to face landmarks via contour points | §Code Examples (FaceLandmarkMapper.anchorPoint body); §Standard Stack (FaceContour types 1-15 enumerated); D-30 lock |
| REN-04 | AssetLoader + LruCache<String,Bitmap> for sprite decode | §Code Examples (AssetLoader + sizeOf); §Pitfall 2 (allocationByteCount vs byteCount); D-08/09 locks |
| REN-05 | Flipbook animation at configured frame rate | §Code Examples (frame-index calculation from Frame.timestamp); D-03 lock + Claude discretion locks timing source |
| REN-06 | No face → draw nothing; no ghost | §Code Examples (early return in onDraw); D-28 lock |
| REN-07 | Filter swap within 1 frame, no rebind | §Code Examples (AtomicReference<FilterDefinition> + preload skip-frame semantics); D-10/11 locks |
| REN-08 | ≥24fps on 2019 mid-tier | §Validation Architecture (manual + optional profiler); D-37 lock (subjective in Phase 3, formal in Phase 7) |
| CAP-01 | Shutter button via ImageCapture.takePicture | §Code Examples (CameraController.capturePhoto); D-13 lock (72dp circle) |
| CAP-02 | JPEG has overlay baked in (OverlayEffect IMAGE_CAPTURE target) | §Architecture Patterns (OverlayEffect fires for all targets in mask); Phase 2 STATE #10 already validated for VIDEO_CAPTURE; D-33 |
| CAP-03 | MediaStore DCIM/Bugzz with IS_PENDING | §Code Examples (OutputFileOptions ContentValues); §Key APIs (CameraX handles IS_PENDING automatically); D-31/32 |
| CAP-04 | Front-cam mirror matches reference | §Validation Architecture (Wave 0 device inspection task); D-17/18 |
| CAP-05 | Saved photo in Google Photos within 1s | §Validation Architecture (manual handoff); CAP follows Phase 2 gaps-03 precedent |
| CAP-06 | No leaks after 30 captures | §Validation Architecture (manual handoff runbook); D-36 |

**ADR-01 follow-ups (additional, not in REQUIREMENTS.md but mandatory per Phase 2 gaps-01):**

| ADR-01 item | Description | Research Support |
|-------------|-------------|------------------|
| Follow-up 1 | Implement `BboxIouTracker` (~100 LOC + unit tests) | §Code Examples (BboxIouTracker.assign body); D-20..D-23 locks |
| Follow-up 2 | Re-key `LandmarkSmoother` on tracker ID (replace `-1` sentinel) | §Code Examples (LandmarkSmoother.onFaceLost); D-25 lock |
| Follow-up 3 | Thread tracker through `FaceDetectorClient.createAnalyzer()` | §Code Examples (analyzer consumer body); D-26 lock |
| Follow-up 4 | Update `02-VERIFICATION.md` CAM-08 after tracker ships | §Validation Architecture (post-device-handoff doc task) |

---

## Project Constraints (from CLAUDE.md)

- Kotlin only (no Java)
- CameraX (not raw Camera2), ML Kit (not Face Mesh), native Android (no Flutter/RN/Unity)
- minSdk 28, targetSdk 35
- English UI strings ("Saved to gallery", not "Đã lưu vào Gallery"); Vietnamese is conversation-only
- GSD workflow enforcement: no direct edits outside `/gsd-execute-phase` (or quick/debug)
- Hilt + KSP (not kapt)
- Compose-first, not hybrid; `CameraXViewfinder`
- MVVM + StateFlow + unidirectional data flow
- MediaStore with `RELATIVE_PATH = DCIM/Bugzz` + scoped storage (no WRITE_EXTERNAL_STORAGE unqualified)
- Frame-sequence PNG + custom mini-renderer (NOT Lottie for bitmap bugs)
- Never use `PreviewView.bitmap` for photo capture; use `ImageCapture.takePicture()` with effect attached
- Never hand-roll MediaMuxer / MediaCodec; CameraX `Recorder` + `VideoCapture` own muxing
- Solo dev, no timeline — quality over speed; stop-test per phase on Xiaomi 13T

---

## Standard Stack

### Core (reused from Phase 2 — no version changes)

| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| Kotlin | 2.1.21 | Language | Phase 1 lock |
| AGP / Gradle | 8.9.1 / 8.13 | Build | Phase 1 lock |
| CameraX | 1.6.0 | Camera API surface | Phase 2 lock — `OverlayEffect` + `camera-compose` stable |
| ML Kit Face Detection (bundled) | 16.1.7 | Landmark/contour detection | Phase 2 lock — offline first-launch |
| Compose BOM | 2026.03.00 (NOT .04 — not yet published per Phase 2 Rule 1 auto-fix) | UI | Phase 2 lock |
| Hilt | 2.57 | DI | Phase 1 lock |
| KSP | 2.1.21-2.0.2 | Codegen | Phase 1 lock (note this is `-2.0.2` per libs.versions.toml, not `-1.0.32` as research/STACK.md drafted) |
| kotlinx.serialization.json | 1.8.0 | manifest.json parsing | **Already catalogued** (`kotlinx-serialization-json` lib entry at libs.versions.toml:73) — reuse for Phase 3 |

### Additions (Phase 3 — new)

**None.** All Phase 3 needs are already on the classpath:
- `androidx.camera:camera-core` 1.6.0 — has `ImageCapture`, `ImageCapture.OutputFileOptions`, `ImageCapture.OnImageSavedCallback`, `MediaStoreOutputOptions` (already used in Phase 2 for VideoCapture)
- `android.util.LruCache` — framework class, no dep needed
- `kotlinx.serialization.json` — already in catalog
- Haptic: `androidx.compose.ui.hapticfeedback.HapticFeedback` (Compose foundation, present via BOM)
- AssetManager — framework class, no dep needed

### Tools (Phase 3 Wave 0)

| Tool | Purpose | Notes |
|------|---------|-------|
| `apktool` (latest 2.x) | Extract reference APK → `assets/` + `res/drawable-*/` for ant + spider sprites | Install via `brew install apktool` (macOS), `choco install apktool` (Windows), or direct download from [apktool.org](https://apktool.org/). Command: `apktool d -o reference-extracted reference/com.insect.filters.funny.prank.bug.filter.face.camera.apk`. Fast mode `-s` skips DEX disassembly; `-r` skips resource decode. For our use case, full decode needed. |
| Android Studio profiler | Optional REN-08 subjective-smoothness-backed trace | User-opt handoff step per D-37 |
| `adb install -r` | Install reference APK on Xiaomi 13T for CAP-04 mirror inspection | Already used in Phase 2 handoff |

### Alternatives Considered (all rejected per CONTEXT locks)

| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| LruCache | SoftReferenceCache | Less deterministic eviction; harder to reason about APK memory |
| `ImageCapture.OutputFileOptions` MediaStore writer | Manual `insert(IS_PENDING=1)` → `openOutputStream` → `update(IS_PENDING=0)` | More code, more edge cases. Only fall back if Xiaomi 13T HyperOS shows ContentResolver quirk in CAP-04 handoff. |
| Greedy IoU | Hungarian algorithm | O(n^3) vs O(n^2) — irrelevant at n=2 |
| `BitmapFactory.decodeStream` | `ImageDecoder` (API 28+) | ImageDecoder is nicer but adds no value for sprite decode (no HEIF, no WebP animation); keep simpler API |
| kotlinx.serialization | Moshi / JSONObject | kotlinx.serialization already in catalog — consistency |
| `AtomicReference<FilterDefinition>` | Channel-based filter broadcast | AtomicReference matches Phase 2 D-19 pattern exactly |

**Version verification (cross-check against `gradle/libs.versions.toml`):**
```
camerax = 1.6.0          ✓ matches
mlkitFace = 16.1.7       ✓ matches
composeBom = 2026.03.00  ✓ current (2026.04 not published per STATE #4)
hilt = 2.57              ✓ matches
kotlinxSerialization = 1.8.0  ✓ ready for manifest.json
```

---

## Architecture Patterns

### Recommended Project Structure (incremental atop Phase 2)

```
app/src/main/java/com/bugzz/filter/camera/
├── capture/                    # NEW Phase 3 (or methods on CameraController per discretion)
│   └── (shutter wrapper lives on CameraController per locked discretion)
├── camera/                     # EXISTS Phase 2
│   ├── CameraController.kt     # extend with capturePhoto(onResult) method
│   ├── CameraExecutors.kt
│   └── CameraLensProvider.kt
├── detector/                   # EXISTS Phase 2
│   ├── FaceDetectorClient.kt   # extend to accept BboxIouTracker + thread tracker into analyzer consumer
│   ├── FaceLandmarkMapper.kt   # REPLACE stub body with D-30 production logic
│   ├── FaceSnapshot.kt         # unchanged
│   ├── OneEuroFilter.kt        # unchanged
│   ├── LandmarkSmoother (in OneEuroFilter.kt) # REPLACE retainActive(emptySet) path + add onFaceLost(id)
│   └── BboxIouTracker.kt       # NEW — ADR-01 follow-up
├── filter/                     # NEW Phase 3
│   ├── FilterDefinition.kt     # data class per D-29
│   ├── FilterCatalog.kt        # object holding 2 filters for Phase 3
│   └── AssetLoader.kt          # @Singleton @Inject LruCache<String,Bitmap> + suspend preload()
├── render/                     # EXISTS Phase 2
│   ├── OverlayEffectBuilder.kt # extend @Inject to receive FilterEngine alongside DebugOverlayRenderer
│   ├── DebugOverlayRenderer.kt # unchanged — retained behind BuildConfig.DEBUG
│   ├── FilterEngine.kt         # NEW — @Singleton production renderer
│   ├── BugBehavior.kt          # NEW — sealed interface + 4 variants
│   └── BugState.kt             # NEW — per-bug mutable state data class
├── ui/camera/                  # EXISTS Phase 2
│   ├── CameraScreen.kt         # extend: add shutter button BottomCenter, Cycle Filter BottomEnd, TEST RECORD to BottomStart
│   ├── CameraViewModel.kt      # extend: onShutterTapped, onCycleFilterTapped, flashVisible state
│   ├── CameraUiState.kt        # extend with capture-flash state
│   └── OneShotEvent.kt         # extend with PhotoSaved(uri) + Error(msg) variants
└── di/CameraModule.kt          # unchanged — all new classes use @Singleton @Inject constructor

app/src/main/assets/             # NEW (Wave 0 manual extract)
└── sprites/
    ├── ant_on_nose_v1/
    │   ├── frame_00.png
    │   ├── frame_01.png
    │   ├── ... (per apktool extract)
    │   └── manifest.json
    └── spider_on_forehead_v1/
        ├── frame_00.png
        ├── ...
        └── manifest.json

app/src/test/java/com/bugzz/filter/camera/
├── detector/
│   ├── BboxIouTrackerTest.kt   # NEW — pure JVM JUnit
│   └── FaceLandmarkMapperTest.kt # NEW — pure JVM (or Robolectric if PointF needs shadow like DebugOverlayRendererTest)
├── filter/
│   ├── FilterCatalogTest.kt    # NEW — pure JVM
│   └── AssetLoaderTest.kt      # NEW — Robolectric if Bitmap.recycle on JVM shadow
└── render/
    └── FilterEngineTest.kt     # NEW — pure JVM dispatch test + Robolectric if Canvas.drawBitmap required
```

### Pattern 1: `OverlayEffect` → `FilterEngine` dispatch (extend Phase 2's OverlayEffectBuilder)

**What:** Inject `FilterEngine` into `OverlayEffectBuilder` alongside existing `DebugOverlayRenderer`. Inside `setOnDrawListener`, draw FilterEngine FIRST (production), DebugOverlayRenderer SECOND (debug-gated overlay on top). Both share the post-`setMatrix` canvas.

**When to use:** Always in Phase 3 for production filter rendering. The debug overlay is for on-device regression debugging only.

**Example:**

```kotlin
// Source: Phase 2's OverlayEffectBuilder.kt (extended per D-27/28/40)
@Singleton
class OverlayEffectBuilder @Inject constructor(
    private val faceDetector: FaceDetectorClient,
    private val filterEngine: FilterEngine,           // NEW Phase 3
    private val debugRenderer: DebugOverlayRenderer,  // EXISTS Phase 2 — retained
) {
    // ... renderThread + renderHandler unchanged

    fun build(): OverlayEffect {
        renderThread = HandlerThread("BugzzRenderThread").apply { start() }
        renderHandler = Handler(renderThread.looper)

        val effect = OverlayEffect(
            TARGETS,           // CameraEffect.PREVIEW | VIDEO_CAPTURE | IMAGE_CAPTURE (unchanged)
            QUEUE_DEPTH,       // 0 (unchanged)
            renderHandler,
            { t -> Timber.e(t, "OverlayEffect internal error") },
        )

        effect.setOnDrawListener { frame ->
            val canvas = frame.overlayCanvas
            // Clear buffer (Phase 2 gaps-02 fix) — BEFORE setMatrix so clear covers full buffer
            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
            canvas.setMatrix(frame.sensorToBufferTransform)

            val snapshot = faceDetector.latestSnapshot.get()
            // Primary face = largest-bbox tracked face per D-24. Returns null if no faces.
            val primary: SmoothedFace? = snapshot.faces
                .maxByOrNull { it.boundingBox.width() * it.boundingBox.height() }

            // Phase 3 production rendering (REN-01..06)
            filterEngine.onDraw(canvas, frame, primary)

            // Debug overlay ON TOP — gated inside DebugOverlayRenderer by BuildConfig.DEBUG
            debugRenderer.draw(canvas, snapshot, frame.timestamp)

            true
        }
        return effect
    }
}
```

**Trade-offs:**
- Pro: Two renderers can coexist for handoff debugging (validate bug position against red rect).
- Pro: Zero CameraX rebind on filter swap (AtomicReference handoff).
- Con: Debug + production in same callback adds ~0.1ms to draw path on debug builds (negligible).

### Pattern 2: Filter definition + sealed BugBehavior + per-bug state

**What:** `FilterDefinition` is immutable spec (what to draw). `BugState` is mutable per-bug runtime state (position, velocity, frame index, path progress). `BugBehavior` is sealed interface — each variant (STATIC/CRAWL/SWARM/FALL) knows how to mutate `BugState` given `face` + `dtMs`. STATIC has real body; CRAWL/SWARM/FALL ship as `TODO("Phase 4")`.

**When to use:** Every bug sprite in the filter dispatches through this tri-triplet. Phase 4 extends by implementing the 3 stub behavior bodies.

**Example:**

```kotlin
// filter/FilterDefinition.kt — D-29
data class FilterDefinition(
    val id: String,
    val displayName: String,
    val anchorType: FaceLandmarkMapper.Anchor,
    val behavior: BugBehavior,
    val frameCount: Int,
    val frameDurationMs: Long,   // per-frame duration; derives flipbook FPS = 1000 / frameDurationMs
    val scaleFactor: Float,      // fraction of face bbox width
    val assetDir: String,        // relative under `assets/sprites/`
    val mirrorable: Boolean,
)

// render/BugBehavior.kt — D-04 (interface + 4 variants; only STATIC implemented)
sealed interface BugBehavior {
    fun tick(state: BugState, face: SmoothedFace, anchor: PointF, dtMs: Long)

    data object Static : BugBehavior {
        override fun tick(state: BugState, face: SmoothedFace, anchor: PointF, dtMs: Long) {
            // STATIC snaps to anchor each frame; velocity = 0. Frame-index advance happens
            // in FilterEngine.draw based on Frame.timestamp, not here.
            state.position = anchor
            state.velocity.set(0f, 0f)
        }
    }

    data object Crawl : BugBehavior {
        override fun tick(state: BugState, face: SmoothedFace, anchor: PointF, dtMs: Long) {
            TODO("Phase 4 MOD-02 — CRAWL behavior implementation")
        }
    }

    data object Swarm : BugBehavior {
        override fun tick(state: BugState, face: SmoothedFace, anchor: PointF, dtMs: Long) {
            TODO("Phase 4 MOD-02 — SWARM behavior implementation")
        }
    }

    data object Fall : BugBehavior {
        override fun tick(state: BugState, face: SmoothedFace, anchor: PointF, dtMs: Long) {
            TODO("Phase 4 MOD-02 — FALL behavior implementation")
        }
    }
}

// render/BugState.kt — D-04 (mutable, fields size for all 4 behaviors)
data class BugState(
    val position: PointF = PointF(),
    val velocity: PointF = PointF(),   // used by CRAWL/SWARM/FALL in Phase 4
    var pathProgress: Float = 0f,      // used by CRAWL along FACE contour in Phase 4
    var lastFrameIndex: Int = -1,
    var lastFrameAdvanceTimestampNanos: Long = 0L,
)
```

**Trade-offs:**
- Pro: REN-02 satisfied in Phase 3 (state machine supports 4) without implementing 3 behaviors.
- Pro: Phase 4 is purely additive — 3 function bodies to write.
- Con: `TODO` stubs will throw `NotImplementedError` if invoked; FilterCatalog must never return a CRAWL/SWARM/FALL filter in Phase 3 (enforced by FilterCatalog shipping only 2 STATIC filters D-01/D-02).

### Pattern 3: AssetLoader + LruCache zero-churn draw path

**What:** `@Singleton @Inject class AssetLoader(...)` owns an `LruCache<String, Bitmap>` keyed `"$filterId/frame_$idx"`. `suspend fun preload(filterId)` decodes all frames for that filter on `cameraExecutor.asCoroutineDispatcher()`. `fun get(filterId, frameIdx): Bitmap?` is non-blocking lookup. On LRU eviction, bitmaps are **NOT recycled** — GC handles it; recycling a bitmap while the render thread still holds a reference would crash the overlay.

**When to use:** Always for sprite decode. Phase 4 scales to 25 filters × ~15 frames = ~375 entries; the 32MB cap forces real eviction behavior and validates the path in Phase 3 already.

**Example:** see §Code Examples.

**Trade-offs:**
- Pro: Zero allocation in the render hot path.
- Pro: LRU eviction under memory pressure is automatic.
- Con: First-use of a filter pays the decode cost (~5-20ms for a 15-frame flipbook). `preload()` on `setFilter()` hides this — but if the swap happens faster than decode completes, the frame draws nothing (D-11 — no ghost).

### Pattern 4: BboxIouTracker greedy assignment

**What:** Per-frame algorithm: (1) compute IoU for every (tracked_entry, detected_face) pair within dropout retention; (2) greedy-match: pick highest IoU ≥ 0.3, assign tracked ID, remove both from candidate pools; repeat; (3) remaining detected faces get new IDs (respecting `MAX_TRACKED_FACES = 2` cap); (4) remaining tracked entries increment `framesSinceLastSeen`; if > 5, removed after iteration completes.

**When to use:** Inside `FaceDetectorClient.createAnalyzer()` MlKitAnalyzer consumer, BEFORE 1€ smoothing. Replaces Phase 2's `-1` sentinel pattern (D-25/26).

**Example:** see §Code Examples.

**Trade-offs:**
- Pro: Greedy is O(D × T) — for D=T=2 that's 4 IoU calcs — trivially cheap.
- Pro: `nextId++` monotonic — removed IDs never recycled (prevents accidental state carry-over in LandmarkSmoother).
- Con: Greedy is not globally optimal. At D=T=2, a pathological case could assign wrong IDs — but with IoU threshold 0.3 this manifests only when two faces overlap > 30% AND one of them just arrived. For a 2-face prank-app scene this is acceptable.

### Anti-Patterns to Avoid

- **Recycling Bitmaps on LruCache eviction:** Android docs explicitly warn against this on API 26+; the system handles bitmap GC. Calling `.recycle()` in `entryRemoved()` will crash the render thread if eviction races with a draw call.
- **Using `Frame.targetType` to branch draw logic:** No such API exists in CameraX 1.6. The callback fires identically for every target in the effect's target mask; trying to detect "is this a photo capture vs a preview frame" is impossible via the public API. Don't special-case drawing for photo capture.
- **Bitmap decode on main thread:** StrictMode (enabled in debug per Phase 1 D-07) will trip immediately. Always decode on `cameraExecutor.asCoroutineDispatcher()`.
- **Hand-rolling `IS_PENDING` transaction:** CameraX 1.6 handles it automatically when using `ImageCapture.OutputFileOptions(contentResolver, collectionUri, contentValues)`. Manual insert-update only if MIUI quirk surfaces in D-17 inspection (then documented as a MIUI-specific path).
- **Using `PreviewView.bitmap` for photo capture:** forbidden by CLAUDE.md. Always `ImageCapture.takePicture()`.
- **Assuming `face.trackingId` is non-null:** under contour mode it is always null per ADR-01. `BboxIouTracker` provides the stable ID.
- **Mutating `BugState` from the main thread:** render thread ownership only. ViewModel should only observe filter state, not bug state.

---

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| MediaStore photo insert with pending transaction | Manual `insert(IS_PENDING=1)` → `openOutputStream` → `update(IS_PENDING=0)` with rollback path | `ImageCapture.OutputFileOptions.Builder(contentResolver, IMAGE_CONTENT_URI, values)` | CameraX 1.6 handles the transaction + error rollback + cross-API-level differences |
| JPEG encode with overlay compositing | `Bitmap.createBitmap` + manual `Canvas.drawBitmap(cameraFrame, srcRect, dstRect, paint)` + JPEG re-encode | `OverlayEffect` bound to `IMAGE_CAPTURE` target (already done in Phase 2) | Frame-accurate compositing into the JPEG pipeline; no double-encode |
| Bitmap memory cache | HashMap<String, Bitmap> with manual eviction | `android.util.LruCache<String, Bitmap>` with `sizeOf` override | Battle-tested, thread-safe, automatic LRU |
| JSON manifest parsing | `JSONObject`/`JSONArray` hand-walk | `kotlinx.serialization.json.Json.decodeFromString<SpriteManifest>(text)` | Already in catalog; type-safe; compile-error if schema drifts |
| Face identity tracking across frames | Custom `-1 sentinel` smoothing path | `BboxIouTracker` (~100 LOC) | Industry standard per MediaPipe/ByteTrack; robust at 2-face scale |
| Canvas matrix transform for sprites | Manual multiply with sensor→view scaling | `Frame.sensorToBufferTransform` + `canvas.setMatrix(matrix)` | CameraX owns the math; zero device-specific bugs (Phase 2 CAM-07 validated) |
| ListenableFuture → suspend bridge | Pull in `kotlinx-coroutines-guava` | Reuse Phase 2's local `ListenableFuture<T>.await()` extension in CameraController.kt | Already on classpath; avoids classpath bloat |
| Flipbook frame indexing | `Handler.postDelayed` loop counting frames | `(frame.timestamp - startTs) / frameDurationNanos % frameCount` inside onDrawListener | Zero allocation, drift-free, no separate timer thread |
| Haptic feedback | `Vibrator.vibrate()` with duration | Compose `LocalView.current.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)` | Respects user system haptic settings; API-level-agnostic |

**Key insight:** Phase 3 is an **integration** phase. Every primitive already exists in either CameraX 1.6, ML Kit 16.1.7, Android framework, or Phase 2's own code. The only genuinely new code is:
- `BboxIouTracker.assign` algorithm (~100 LOC)
- `FilterEngine.onDraw` dispatch (~50 LOC)
- `AssetLoader.preload` + `sizeOf` (~30 LOC)
- `FaceLandmarkMapper.anchorPoint` body (~40 LOC per the D-30 spec)
- Glue in CameraController.capturePhoto (~40 LOC)

Everything else is wiring.

---

## Runtime State Inventory

> Phase 3 is additive/greenfield — no rename/refactor/migration. Inventory reduced to **verify no stale state** but kept for rigor.

| Category | Items Found | Action Required |
|----------|-------------|------------------|
| Stored data | **None** — DataStore not yet written to in Phase 3; MediaStore DCIM/Bugzz is new. | None |
| Live service config | **None** — app is fully offline per PROJECT.md | None |
| OS-registered state | **None** — no foreground service, no pm2/task-scheduler/launchd equivalent | None |
| Secrets/env vars | **None** — app has no network, no API keys | None |
| Build artifacts | **Phase 2 test APK (82MB)** exists in `app/build/outputs/apk/debug/` — will be invalidated by Phase 3 build; no action needed (Gradle handles) | None |

**Nothing in any category:** Verified by inspecting Phase 1/2 CONTEXT files and grepping for `DataStore` / `FileProvider` / `foreground_service` / `alarm_manager` usages. None found. Phase 3 is pure additive.

---

## Common Pitfalls

### Pitfall 1: `OverlayEffect.Frame` does NOT expose `targetType`

**What goes wrong:** Developer tries to branch draw logic ("if this is IMAGE_CAPTURE, draw higher-quality sprite; if PREVIEW, draw lower-res"). The API doesn't support it.

**Why it happens:** Public `Frame` API surfaces `overlayCanvas`, `sensorToBufferTransform`, `timestamp` only. Training-data confusion with internal CameraX structures.

**How to avoid:** Don't branch. The same draw call runs for every target. If a filter needs per-target quality, the whole filter system needs redesign — out of scope. [VERIFIED: Phase 2 gaps-03 red rect baked uniformly into MP4 via identical callback; CAP-02 in Phase 3 will confirm for JPEG]

**Warning signs:** "How do I detect if this is a preview frame?" — you can't. Draw the same way every time.

---

### Pitfall 2: `Bitmap.byteCount` vs `Bitmap.allocationByteCount` — LruCache underreports

**What goes wrong:** `sizeOf` returns `bitmap.byteCount` which reports the **current** byte count — but a re-used (reconfigured) Bitmap's `byteCount` can be smaller than its backing buffer. LruCache then under-evicts and OOMs.

**Why it happens:** Android docs for API 19+ recommend `allocationByteCount` precisely for LRU cache sizing. Older tutorials still say `byteCount`.

**How to avoid:** Always `sizeOf(key, value) = value.allocationByteCount` (we don't reconfigure bitmaps — it's theoretical for our code — but following the API 19+ recommendation is zero cost and catches future regressions).

**Warning signs:** LruCache size() climbs past `maxSize` without evicting; OOMs at ~25 filters × 15 frames.

---

### Pitfall 3: Recycling a cached Bitmap on eviction crashes the render thread

**What goes wrong:** `entryRemoved(evicted, key, oldValue, newValue) { oldValue.recycle() }` — evicted Bitmap is torn down while the render thread is mid-draw holding a reference.

**Why it happens:** Pre-API-26 pattern leaking into modern code.

**How to avoid:** Do not override `entryRemoved` to recycle. GC handles it. From API 26+, Bitmap memory is in `Bitmap.Config.HARDWARE` or ashmem; recycling is a relic.

**Warning signs:** `java.lang.RuntimeException: Canvas: trying to use a recycled bitmap` in onDraw.

---

### Pitfall 4: Front-camera ImageCapture mirror behaves DIFFERENTLY than VideoCapture

**What goes wrong:** Phase 2's `VideoCapture.Builder().setMirrorMode(MIRROR_MODE_ON_FRONT_ONLY)` bakes mirror into the recording — **but `ImageCapture.Builder` does NOT ship an equivalent `setMirrorMode` on 1.6 in all AARs.** The front-cam JPEG may save un-mirrored while preview is mirrored — causing user confusion (CAP-04 gap).

**Why it happens:** CameraX 1.6 has `ImageCapture.Builder.setMirrorMode(MirrorMode)` on paper but ML Kit analyzer pipelines can short-circuit it; on some Xiaomi HyperOS builds the sensor orientation is reported differently and the JPEG saves as the "raw" un-mirrored sensor frame.

**How to avoid:** **D-17 Wave 0 device inspection is the only safe way to lock this.** Inspect reference APK behavior on Xiaomi 13T FIRST (not simulated). If reference mirrors, default to CameraX `MirrorMode.MIRROR_MODE_ON_FRONT_ONLY` on ImageCapture. If CameraX default doesn't produce mirrored output on Xiaomi 13T, two fallbacks: (a) apply mirror in `OverlayEffect.setOnDrawListener` via `canvas.scale(-1f, 1f, centerX, 0f)` when `frame` is IMAGE_CAPTURE — **but we can't detect target**, so this would mirror all streams → rejected; (b) post-process the saved JPEG via `Bitmap.createBitmap(src, 0, 0, w, h, matrixWithPreScale(-1, 1), true)` in `onImageSaved` callback and rewrite to the same URI — added complexity but isolated to the capture path. Default plan: trust CameraX mirror mode + verify on device.

**Warning signs:** User: "my filter is backwards in the saved photo!" Or: preview shows mirrored, JPEG saved un-mirrored.

---

### Pitfall 5: `face.trackingId` re-introduction regresses ADR-01

**What goes wrong:** Developer re-adds `.enableTracking()` to FaceDetectorOptions during Phase 3 rework because "we need stable IDs now." ADR-01 already proved this silently fails under CONTOUR_MODE_ALL.

**Why it happens:** ADR-01 is easy to miss under pressure; `.enableTracking()` is right there in the builder.

**How to avoid:** Phase 3 `FaceDetectorClient.buildOptions` must be unchanged. Stable IDs come from `BboxIouTracker.assign()`, not ML Kit. Verification: `FaceDetectorOptionsTest` already asserts `trackingEnabled=false` post-gaps-01 — regression-protected.

**Warning signs:** `face.trackingId == null` suddenly "works" in debug logs — you broke contour mode.

---

### Pitfall 6: LandmarkSmoother state leak on same ID re-appearance

**What goes wrong:** Face A (tracker id=3) leaves frame; 10 frames later, a different face B arrives. If nextId were to recycle, B might get id=3, and 1€ filter state from A is applied to B → visible snap.

**Why it happens:** Counter-intuitive: algorithm-level, `BboxIouTracker.nextId++` never reuses IDs (D-23 lock). But defensive code in `LandmarkSmoother.onFaceLost(id)` clears state regardless. Phase 2's `retainActive(emptySet())` already cleared everything every frame — Phase 3 moves that to per-ID clear only on tracker removal.

**How to avoid:** `BboxIouTracker` MUST use monotonic `nextId` (never recycle). `LandmarkSmoother.onFaceLost(id)` MUST be called when tracker emits "removed for id X". Unit test: pin that `tracker.assign` on removed+new-face returns higher id than removed.

**Warning signs:** Bug sprite "snaps" from old face position to new face when face B enters quickly.

---

### Pitfall 7: Flipbook frame-index jitter from drift-accumulating clock

**What goes wrong:** Developer uses `System.nanoTime()` inside onDrawListener instead of `Frame.timestamp`. On OverlayEffect the Frame.timestamp comes from the camera sensor pipeline (monotonic vsync-aligned); `nanoTime()` ticks at its own rate. Cross-stream (preview vs capture) the clocks desync and the flipbook index can jump on capture frames.

**Why it happens:** Intuition says "time is time" but `Frame.timestamp` is **camera frame timestamp** — not wallclock. Fixing wallclock drift requires tracking `startTs` separately.

**How to avoid:** Use `Frame.timestamp` (nanos) exclusively. Record `startTimestampNanos` on first draw; compute `elapsedNanos = frame.timestamp - startTimestampNanos`; frame index = `((elapsedNanos / frameDurationNanos) % frameCount).toInt()`. **Zero-allocation** in the draw path.

**Warning signs:** Flipbook "stutters" briefly during photo capture; frames skip by more than one.

---

### Pitfall 8: `Canvas.setMatrix` is reset by `drawColor(CLEAR)`? (NO — verified)

**What goes wrong:** Confusion whether `canvas.drawColor(TRANSPARENT, CLEAR)` resets the canvas matrix set earlier in the frame.

**Why it happens:** Phase 2 gaps-02 discovered the Canvas does NOT auto-clear between frames; the fix put `drawColor(CLEAR)` BEFORE `setMatrix`. Developer could assume the clear affects matrix.

**How to avoid:** Phase 2's `OverlayEffectBuilder` already has the correct order: clear → setMatrix → draw. Phase 3's FilterEngine/DebugOverlayRenderer both receive a canvas with matrix already set. Do not call `setMatrix` again inside draw. Do not call `drawColor(CLEAR)` inside draw.

**Warning signs:** FilterEngine draws sprite in wrong coordinates — caused by accidentally resetting matrix.

---

### Pitfall 9: `Bitmap.Config.RGB_565` loses alpha — bugs get rectangular shadows

**What goes wrong:** Developer sets `RGB_565` globally to halve memory. Bug sprites have soft alpha edges + drop shadows; with RGB_565 they render as opaque rectangles with ugly background.

**Why it happens:** Research STACK says "RGB_565 for sprites with no alpha-critical gradients" — but bug sprites ARE alpha-critical.

**How to avoid:** Default `ARGB_8888` per D-08. RGB_565 is **opt-in per-filter via `manifest.json`**, and Phase 3's ant + spider default `null` (= ARGB_8888).

**Warning signs:** Bug has a visible rectangular border, or shadow is missing, or background pokes through alpha edges.

---

### Pitfall 10: Xiaomi 13T / HyperOS MIUI ContentResolver quirk (speculative — watch for it)

**What goes wrong:** On some MIUI builds, MediaStore insert returns a URI but the file never appears in Google Photos. Known class of HyperOS issues (bug report aggregates 2025-2026 show camera-app crashes + Pro Mode preset bugs + green-tint WB bugs) — **no specific ContentResolver-for-CameraX case reported** but vigilance warranted.

**Why it happens:** MIUI's customized Camera-related HALs sometimes delay MediaStore scan until device-restart.

**How to avoid:** If CAP-05 (visible in Google Photos within 1s) fails on Xiaomi 13T in handoff, fall back to manual `MediaScannerConnection.scanFile(ctx, arrayOf(filepath), arrayOf(mimeType), null)` after the CameraX callback fires. Document any occurrence in `03-HANDOFF.md` + promote to Phase 7 cross-OEM matrix.

**Warning signs:** Toast fires ("Saved to gallery"), MediaStore query returns URI, but Google Photos grid doesn't show it for 60+ seconds.

[CITED: en.xiaomi-miui.gr/hyperos-bug-tracker, xiaomitime.com — no CameraX-specific reports; general camera-app caution]

---

## Code Examples

All examples verified against Phase 2 codebase shapes (grep-matched against existing files).

### Example 1: OverlayEffect + FilterEngine dispatch (extends Phase 2's OverlayEffectBuilder)

See §Architecture Patterns Pattern 1 above.

### Example 2: `FilterEngine.onDraw` full body (NEW Phase 3)

```kotlin
package com.bugzz.filter.camera.render

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PointF
import androidx.camera.effects.OverlayEffect
import com.bugzz.filter.camera.detector.FaceLandmarkMapper
import com.bugzz.filter.camera.detector.SmoothedFace
import com.bugzz.filter.camera.filter.AssetLoader
import com.bugzz.filter.camera.filter.FilterDefinition
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Production filter renderer — REN-01..07. Replaces DebugOverlayRenderer for non-debug draw.
 * Invoked from OverlayEffect.onDrawListener after canvas.setMatrix(sensorToBufferTransform).
 *
 * Thread model: single caller (render thread). State is internal; no locks needed for the
 * per-bug BugState (one filter, one primary face, one state object mutated in-order).
 *
 * Filter swap (REN-07 / D-11): setFilter writes AtomicReference + triggers preload on
 * cameraExecutor. If preload has not resolved by next draw, filter is not drawn — no ghost.
 */
@Singleton
class FilterEngine @Inject constructor(
    private val assetLoader: AssetLoader,
) {
    private val activeFilter: AtomicReference<FilterDefinition?> = AtomicReference(null)
    private val paint = Paint().apply {
        isAntiAlias = true
        isFilterBitmap = true  // linear sampling for scaled sprites
    }

    // Per-bug state. One bug per filter in Phase 3 (D-01/02 each ship one sprite instance).
    private val bugState = BugState()
    private var startTimestampNanos: Long = 0L

    /** D-11 — write-latest AtomicReference; preload delegated to caller (CameraViewModel/
     *  ViewModelScope invokes AssetLoader.preload(filterId) first). */
    fun setFilter(definition: FilterDefinition?) {
        activeFilter.set(definition)
        // Reset per-bug state on filter change so new sprite starts from frame 0.
        bugState.lastFrameIndex = -1
        bugState.lastFrameAdvanceTimestampNanos = 0L
        startTimestampNanos = 0L
    }

    /** Invoked by OverlayEffectBuilder.onDrawListener. */
    fun onDraw(canvas: Canvas, frame: OverlayEffect.Frame, face: SmoothedFace?) {
        val filter = activeFilter.get() ?: return          // no filter selected — REN-06
        if (face == null) return                            // no face — REN-06 (no ghost)

        // Resolve anchor in sensor space (canvas is already matrix-transformed to sensor coords).
        val anchor = FaceLandmarkMapper.anchorPoint(face, filter.anchorType) ?: run {
            // Ultimate fallback: bbox center. D-30 guarantees non-null, but defensive.
            PointF(face.boundingBox.centerX().toFloat(), face.boundingBox.centerY().toFloat())
        }

        // Dispatch behavior (STATIC snaps; CRAWL/SWARM/FALL throw in Phase 3 — enforced by
        // FilterCatalog shipping only STATIC filters).
        val dtMs = if (bugState.lastFrameAdvanceTimestampNanos == 0L) 0L
                   else (frame.timestamp - bugState.lastFrameAdvanceTimestampNanos) / 1_000_000L
        filter.behavior.tick(bugState, face, anchor, dtMs)

        // Advance flipbook frame index from Frame.timestamp (zero-allocation clock).
        if (startTimestampNanos == 0L) startTimestampNanos = frame.timestamp
        val elapsedNanos = (frame.timestamp - startTimestampNanos).coerceAtLeast(0L)
        val frameDurationNanos = filter.frameDurationMs * 1_000_000L
        val frameIdx = ((elapsedNanos / frameDurationNanos) % filter.frameCount).toInt()

        // Load sprite from cache. If preload hasn't resolved, skip this frame (D-11 — no ghost).
        val bitmap = assetLoader.get(filter.id, frameIdx) ?: return

        // Scale to filter.scaleFactor × face-box width, draw centered on anchor position.
        val targetWidth = filter.scaleFactor * face.boundingBox.width()
        val scale = targetWidth / bitmap.width
        val scaledH = bitmap.height * scale
        val x = bugState.position.x - targetWidth / 2f
        val y = bugState.position.y - scaledH / 2f

        canvas.save()
        canvas.translate(x, y)
        canvas.scale(scale, scale)
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        canvas.restore()

        bugState.lastFrameIndex = frameIdx
        bugState.lastFrameAdvanceTimestampNanos = frame.timestamp
    }
}
```

### Example 3: `BboxIouTracker.assign` full body (NEW — ADR-01 follow-up)

```kotlin
package com.bugzz.filter.camera.detector

import android.graphics.Rect
import com.google.mlkit.vision.face.Face
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max
import kotlin.math.min

/**
 * Spatial-IoU face-identity tracker. Replaces ML Kit's trackingId (null under CONTOUR_MODE_ALL —
 * see 02-ADR-01). Per-frame: greedy best-IoU match against retained entries within dropout
 * retention window, then new-id assignment for unmatched detections, then dropout increment.
 *
 * Algorithm contract (D-23):
 *   1. For each detected face, compute IoU against every TrackedEntry.lastBoundingBox still
 *      within dropout retention (framesSinceLastSeen <= MAX_DROPOUT_FRAMES).
 *   2. Greedy best-match: pick pair with highest IoU >= threshold; assign existing ID;
 *      remove from both pools; repeat.
 *   3. Unmatched detected faces -> assign nextId++. Skip if exceeds MAX_TRACKED_FACES
 *      (lowest-IoU unmatched face dropped silently per D-23).
 *   4. Unmatched tracked entries -> framesSinceLastSeen++; if > MAX_DROPOUT_FRAMES, remove
 *      after frame callback completes (don't mutate during iteration).
 *
 * IDs are monotonic — never recycled — so LandmarkSmoother state cannot accidentally carry
 * over to a different face (D-25).
 */
@Singleton
class BboxIouTracker @Inject constructor() {

    data class TrackedFace(val id: Int, val face: Face)

    private data class TrackedEntry(
        val id: Int,
        var lastBoundingBox: Rect,
        var framesSinceLastSeen: Int,
    )

    private val tracked = mutableMapOf<Int, TrackedEntry>()
    private var nextId: Int = 0

    /** D-20 — per-frame assignment. Returns tracked faces (at most MAX_TRACKED_FACES). */
    fun assign(faces: List<Face>): List<TrackedFace> {
        // D-22 — retain 2 largest-bbox detected faces only
        val sortedFaces = faces
            .sortedByDescending { it.boundingBox.width().toLong() * it.boundingBox.height() }
            .take(MAX_TRACKED_FACES)

        val out = mutableListOf<TrackedFace>()
        val unmatchedDetectedIdx = sortedFaces.indices.toMutableSet()
        val unmatchedTrackedIds = tracked.keys.toMutableSet()

        // Step 1-2: greedy IoU-best-first match
        while (unmatchedDetectedIdx.isNotEmpty() && unmatchedTrackedIds.isNotEmpty()) {
            var bestIou = IOU_MATCH_THRESHOLD
            var bestDetectedIdx = -1
            var bestTrackedId = -1
            for (dIdx in unmatchedDetectedIdx) {
                for (tId in unmatchedTrackedIds) {
                    val iou = iou(sortedFaces[dIdx].boundingBox, tracked[tId]!!.lastBoundingBox)
                    if (iou > bestIou) {
                        bestIou = iou
                        bestDetectedIdx = dIdx
                        bestTrackedId = tId
                    }
                }
            }
            if (bestDetectedIdx < 0) break   // no remaining pair exceeds threshold

            // Match: update entry, emit result
            val face = sortedFaces[bestDetectedIdx]
            val entry = tracked[bestTrackedId]!!
            entry.lastBoundingBox = face.boundingBox
            entry.framesSinceLastSeen = 0
            out.add(TrackedFace(bestTrackedId, face))
            unmatchedDetectedIdx.remove(bestDetectedIdx)
            unmatchedTrackedIds.remove(bestTrackedId)
        }

        // Step 3: assign new IDs to unmatched detections (respecting MAX cap)
        for (dIdx in unmatchedDetectedIdx) {
            if (tracked.size >= MAX_TRACKED_FACES) break   // cap reached — drop silently per D-23
            val face = sortedFaces[dIdx]
            val newId = nextId++
            tracked[newId] = TrackedEntry(newId, face.boundingBox, framesSinceLastSeen = 0)
            out.add(TrackedFace(newId, face))
        }

        // Step 4: increment dropout, mark removals (collect first, then mutate)
        val removedIds = mutableListOf<Int>()
        for (tId in unmatchedTrackedIds) {
            val entry = tracked[tId]!!
            entry.framesSinceLastSeen++
            if (entry.framesSinceLastSeen > MAX_DROPOUT_FRAMES) removedIds.add(tId)
        }
        for (id in removedIds) tracked.remove(id)

        return out
    }

    /** List IDs that just dropped (for LandmarkSmoother.onFaceLost hookup). */
    fun drainRemovedIds(): List<Int> {
        // Simple alternative: emit as side-channel via callback. For Phase 3, FaceDetectorClient
        // can diff previous vs current IDs — clear implementation, no state carry.
        TODO("Phase 3 plan will decide either drain/callback or diff-by-id in FaceDetectorClient")
    }

    companion object {
        const val IOU_MATCH_THRESHOLD: Float = 0.3f
        const val MAX_DROPOUT_FRAMES: Int = 5
        const val MAX_TRACKED_FACES: Int = 2

        /** Axis-aligned IoU for android.graphics.Rect. */
        internal fun iou(a: Rect, b: Rect): Float {
            val interLeft = max(a.left, b.left)
            val interTop = max(a.top, b.top)
            val interRight = min(a.right, b.right)
            val interBottom = min(a.bottom, b.bottom)
            val interW = (interRight - interLeft).coerceAtLeast(0)
            val interH = (interBottom - interTop).coerceAtLeast(0)
            val interArea = interW.toLong() * interH
            if (interArea == 0L) return 0f
            val areaA = a.width().toLong() * a.height()
            val areaB = b.width().toLong() * b.height()
            val unionArea = areaA + areaB - interArea
            if (unionArea <= 0L) return 0f
            return interArea.toFloat() / unionArea.toFloat()
        }
    }
}
```

**Note on `drainRemovedIds`:** the plan will decide between (a) returning removed IDs from `assign()` as a `Pair<List<TrackedFace>, List<Int>>`, or (b) diffing "ids in previous snapshot" vs "ids in this snapshot" inside `FaceDetectorClient.createAnalyzer` consumer. Either is fine. Research recommends **(a)** — cleaner contract. Suggested signature refinement:

```kotlin
data class TrackerResult(val tracked: List<TrackedFace>, val removedIds: List<Int>)
fun assign(faces: List<Face>): TrackerResult
```

### Example 4: `FaceLandmarkMapper.anchorPoint` production body (replace Phase 2 stub)

```kotlin
package com.bugzz.filter.camera.detector

import android.graphics.PointF
import com.google.mlkit.vision.face.FaceContour

object FaceLandmarkMapper {

    enum class Anchor { NOSE_TIP, FOREHEAD, LEFT_CHEEK, RIGHT_CHEEK, CHIN, LEFT_EYE, RIGHT_EYE }

    /**
     * D-30 — production body. Returns sensor-space PointF or null ONLY if face.boundingBox is
     * null (should never happen for a detected face).
     *
     * Fallback ladder per anchor:
     *   NOSE_TIP: NOSE_BRIDGE[last] -> NOSE_BOTTOM[0] -> boundingBox center
     *   FOREHEAD: mean(LEFT_EYEBROW_TOP[0], RIGHT_EYEBROW_TOP[last]) offset -15% bbox.height
     *             -> mean of eye landmarks offset up -25% bbox.height
     *             -> boundingBox top-center
     *   LEFT_CHEEK / RIGHT_CHEEK: FACE contour at 40% jaw progress (indices vary; see below)
     *             -> boundingBox side-center
     *   CHIN: FACE contour midpoint-of-back-half
     *             -> boundingBox bottom-center
     *   LEFT_EYE / RIGHT_EYE: LEFT_EYE / RIGHT_EYE contour centroid
     *             -> boundingBox quadrant-center
     */
    fun anchorPoint(face: SmoothedFace, anchor: Anchor): PointF? {
        val bbox = face.boundingBox
        return when (anchor) {
            Anchor.NOSE_TIP -> {
                val bridge = face.contours[FaceContour.NOSE_BRIDGE]
                if (!bridge.isNullOrEmpty()) return bridge.last()
                val bottom = face.contours[FaceContour.NOSE_BOTTOM]
                if (!bottom.isNullOrEmpty()) return bottom.first()
                PointF(bbox.centerX().toFloat(), bbox.centerY().toFloat())
            }

            Anchor.FOREHEAD -> {
                val leb = face.contours[FaceContour.LEFT_EYEBROW_TOP]
                val reb = face.contours[FaceContour.RIGHT_EYEBROW_TOP]
                if (!leb.isNullOrEmpty() && !reb.isNullOrEmpty()) {
                    val lp = leb.first()
                    val rp = reb.last()
                    val mid = PointF((lp.x + rp.x) / 2f, (lp.y + rp.y) / 2f)
                    // Offset upward by 15% bbox height (D-30). In SENSOR coords with portrait
                    // orientation, "up" in the face is -Y. The sensorToBufferTransform already
                    // maps this to the correct buffer direction.
                    mid.y -= bbox.height() * 0.15f
                    return mid
                }
                PointF(bbox.centerX().toFloat(), bbox.top.toFloat())
            }

            Anchor.LEFT_CHEEK -> {
                val face2 = face.contours[FaceContour.FACE]
                if (!face2.isNullOrEmpty()) {
                    // FACE contour is ordered clockwise starting at top; ~36 points. Left
                    // cheek ~= index N * 0.40. For ~36 points, that's idx 14.
                    val idx = (face2.size * 0.40f).toInt().coerceIn(0, face2.size - 1)
                    return face2[idx]
                }
                PointF(bbox.left.toFloat(), bbox.centerY().toFloat())
            }

            Anchor.RIGHT_CHEEK -> {
                val face2 = face.contours[FaceContour.FACE]
                if (!face2.isNullOrEmpty()) {
                    // Mirror of LEFT_CHEEK: 60% progress.
                    val idx = (face2.size * 0.60f).toInt().coerceIn(0, face2.size - 1)
                    return face2[idx]
                }
                PointF(bbox.right.toFloat(), bbox.centerY().toFloat())
            }

            Anchor.CHIN -> {
                val face2 = face.contours[FaceContour.FACE]
                if (!face2.isNullOrEmpty()) {
                    // Midpoint of back-half (index ~50% of contour).
                    val idx = (face2.size * 0.50f).toInt().coerceIn(0, face2.size - 1)
                    return face2[idx]
                }
                PointF(bbox.centerX().toFloat(), bbox.bottom.toFloat())
            }

            Anchor.LEFT_EYE -> {
                val eye = face.contours[FaceContour.LEFT_EYE]
                if (!eye.isNullOrEmpty()) return centroid(eye)
                PointF(bbox.left + bbox.width() * 0.3f, bbox.top + bbox.height() * 0.35f)
            }

            Anchor.RIGHT_EYE -> {
                val eye = face.contours[FaceContour.RIGHT_EYE]
                if (!eye.isNullOrEmpty()) return centroid(eye)
                PointF(bbox.left + bbox.width() * 0.7f, bbox.top + bbox.height() * 0.35f)
            }
        }
    }

    private fun centroid(points: List<PointF>): PointF {
        var sx = 0f; var sy = 0f
        for (p in points) { sx += p.x; sy += p.y }
        return PointF(sx / points.size, sy / points.size)
    }
}
```

**Note:** Phase 2's `SMOOTHED_CONTOUR_TYPES` in `FaceDetectorClient.kt:105-115` must be extended to include `FaceContour.LEFT_EYEBROW_TOP` and `FaceContour.RIGHT_EYEBROW_TOP` (for FOREHEAD resolution). Currently it has FACE, NOSE_BRIDGE, NOSE_BOTTOM, LEFT_EYE, RIGHT_EYE, LEFT_CHEEK, RIGHT_CHEEK, UPPER_LIP_TOP, LOWER_LIP_BOTTOM. Addition is ~2 lines.

### Example 5: `ImageCapture.takePicture` + MediaStore via OutputFileOptions (extend CameraController)

```kotlin
// Add to CameraController.kt (after startTestRecording)

/**
 * CAP-01/02/03 — capture photo via ImageCapture.takePicture + MediaStoreOutputFileOptions.
 * CameraX 1.6 handles IS_PENDING transaction automatically per
 * developer.android.com/media/camera/camerax/take-photo.
 *
 * On success: emits Uri via onResult(Result.success(uri)). Uri is a MediaStore content URI
 * shareable via Intent.ACTION_SEND / ACTION_VIEW without FileProvider.
 *
 * On error: onResult(Result.failure(ImageCaptureException)).
 *
 * Thread: takePicture callback runs on cameraExecutor. Caller (ViewModel) forwards to main
 * via viewModelScope.launch.
 */
fun capturePhoto(onResult: (Result<Uri>) -> Unit) {
    val ic = imageCapture ?: run {
        onResult(Result.failure(IllegalStateException("Camera not bound")))
        return
    }
    val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
    val filename = "Bugzz_${sdf.format(Date())}.jpg"
    val values = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, filename)
        put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        put(MediaStore.Images.Media.RELATIVE_PATH, "DCIM/Bugzz")
        // Do NOT set IS_PENDING here — CameraX 1.6 handles the transaction automatically
        // per ImageCapture.OutputFileOptions contract.
    }
    val options = ImageCapture.OutputFileOptions.Builder(
        appContext.contentResolver,
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        values,
    ).build()

    ic.takePicture(
        options,
        cameraExecutor,
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(results: ImageCapture.OutputFileResults) {
                val uri = results.savedUri ?: run {
                    onResult(Result.failure(IllegalStateException("savedUri null on success")))
                    return
                }
                Timber.tag("CameraController").i("Photo saved %s", uri)
                onResult(Result.success(uri))
            }
            override fun onError(exc: ImageCaptureException) {
                Timber.tag("CameraController").e(exc, "Photo capture failed")
                onResult(Result.failure(exc))
            }
        },
    )
}
```

### Example 6: `AssetLoader` with correct sizeOf

```kotlin
package com.bugzz.filter.camera.filter

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.util.concurrent.Executor
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * Decode sprite frames from assets/ into LruCache<String, Bitmap>. REN-04 / D-08 / D-09.
 *
 * Cache size = min(32 MB, maxMemory/8 in bytes). sizeOf returns allocationByteCount (not byteCount)
 * per Android 19+ recommendation (§Pitfall 2).
 *
 * Do NOT override entryRemoved to recycle bitmaps — GC handles it (§Pitfall 3).
 */
@Singleton
class AssetLoader @Inject constructor(
    @ApplicationContext private val appContext: Context,
    @Named("cameraExecutor") private val cameraExecutor: Executor,
) {
    private val decodeDispatcher: CoroutineDispatcher = cameraExecutor.asCoroutineDispatcher()

    private val cache: LruCache<String, Bitmap> = object : LruCache<String, Bitmap>(computeCacheSize()) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.allocationByteCount
        // No entryRemoved override — DO NOT recycle (§Pitfall 3).
    }

    /** D-11 — preload all frames for filterId. Suspends until complete or throws. */
    suspend fun preload(filterId: String) = withContext(decodeDispatcher) {
        val manifest = loadManifest(filterId)
        for (idx in 0 until manifest.frameCount) {
            val key = "$filterId/frame_$idx"
            if (cache.get(key) != null) continue   // already cached
            val path = "sprites/$filterId/frame_${idx.toString().padStart(2, '0')}.png"
            val opts = BitmapFactory.Options().apply {
                inPreferredConfig = when (manifest.bitmapConfig) {
                    "RGB_565" -> Bitmap.Config.RGB_565
                    else      -> Bitmap.Config.ARGB_8888   // D-08 default
                }
            }
            val bitmap = appContext.assets.open(path).use { stream ->
                BitmapFactory.decodeStream(stream, null, opts)
                    ?: throw IllegalArgumentException("decode failed: $path")
            }
            cache.put(key, bitmap)
        }
    }

    /** Non-blocking lookup. Returns null if preload incomplete (D-11 semantics). */
    fun get(filterId: String, frameIdx: Int): Bitmap? =
        cache.get("$filterId/frame_$frameIdx")

    private fun loadManifest(filterId: String): SpriteManifest {
        val path = "sprites/$filterId/manifest.json"
        val text = appContext.assets.open(path).bufferedReader().use { it.readText() }
        return Json.decodeFromString(SpriteManifest.serializer(), text)
    }

    companion object {
        private const val MAX_BYTES: Int = 32 * 1024 * 1024   // 32 MB cap per D-09

        /** D-09 — min(32 MB, maxMemory/8). */
        private fun computeCacheSize(): Int {
            val oneEighthMaxMemory = (Runtime.getRuntime().maxMemory() / 8L).toInt()
            return minOf(MAX_BYTES, oneEighthMaxMemory.coerceAtLeast(8 * 1024 * 1024))
        }
    }
}

// NEW — data class for sprites/*/manifest.json (D-06).
@kotlinx.serialization.Serializable
data class SpriteManifest(
    val id: String,
    val displayName: String,
    val frameCount: Int,
    val frameDurationMs: Long,
    val anchorType: String,      // matches FaceLandmarkMapper.Anchor.name
    val behavior: String,        // "STATIC" | "CRAWL" | "SWARM" | "FALL"
    val scaleFactor: Float,
    val mirrorable: Boolean = true,
    val bitmapConfig: String? = null,  // optional "RGB_565" opt-in per D-08
)
```

### Example 7: Flipbook frame-index calculation (zero-allocation)

Already embedded inside `FilterEngine.onDraw` above (Example 2). Key lines:

```kotlin
if (startTimestampNanos == 0L) startTimestampNanos = frame.timestamp
val elapsedNanos = (frame.timestamp - startTimestampNanos).coerceAtLeast(0L)
val frameDurationNanos = filter.frameDurationMs * 1_000_000L
val frameIdx = ((elapsedNanos / frameDurationNanos) % filter.frameCount).toInt()
```

- `frame.timestamp` is `Long` (nanos) from CameraX. No allocation.
- Arithmetic on primitives. No allocation.
- Index never overflows: `elapsedNanos / frameDurationNanos` is `Long`; `% filter.frameCount` wraps.

### Example 8: Haptic feedback on shutter tap

```kotlin
// In CameraScreen.kt — shutter button modifier
import android.view.HapticFeedbackConstants
import androidx.compose.ui.platform.LocalView

val view = LocalView.current
Box(
    modifier = Modifier
        .align(Alignment.BottomCenter)
        .padding(bottom = 24.dp)
        .size(72.dp)
        .clip(CircleShape)
        .background(Color.White)
        .border(2.dp, Color.Gray, CircleShape)
        .clickable {
            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            vm.onShutterTapped()
        }
)
```

---

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| Manual `insert(IS_PENDING=1)` → `openOutputStream` → `update(IS_PENDING=0)` | `ImageCapture.OutputFileOptions.Builder(contentResolver, collectionUri, values)` | CameraX 1.4+ | Eliminates ~30 LOC + error-rollback branches per capture |
| `FileProvider` authorities for share | MediaStore content URIs direct in share intent | Android 10+ / CameraX MediaStore output | No provider setup; URIs already grant READ |
| `AnimationDrawable` with many PNG frames | Frame-sequence renderer driven by `Frame.timestamp` | 2026 / this project | Zero timer thread, zero allocation in draw path |
| `ML Kit .enableTracking()` for face identity | `BboxIouTracker` (MediaPipe-style) | 2026-04-19 (ADR-01 from Phase 2) | Required because CONTOUR_MODE_ALL silently disables tracking |
| `bitmap.byteCount` in LruCache sizeOf | `bitmap.allocationByteCount` | API 19+ | Accurate sizing; prevents under-eviction OOM |
| OpenGL ES custom shader for simple overlay | CameraX `OverlayEffect` Canvas callback | CameraX 1.4+ | 10× less code; same output quality for sprite compositing |

**Deprecated/outdated:**
- `FileProvider` for MediaStore-sourced URIs (not wrong, just unnecessary).
- `entryRemoved` recycling bitmaps in LruCache (Pre-API-26 pattern, harmful post-26).
- Compose BOM 2026.04.00 — **not yet published** on Google Maven as of 2026-04-19 per Phase 2 Rule 1 auto-fix; stay on 2026.03.00.

---

## Assumptions Log

| # | Claim | Section | Risk if Wrong |
|---|-------|---------|---------------|
| A1 | `OverlayEffect.Frame` has no `targetType` property exposed on the public API | §Summary finding 1, §Pitfall 1 | If false: plan should expose target branching to FilterEngine. Mitigation: Phase 2 gaps-03 already confirmed uniform target callback by baking red rect into MP4. |
| A2 | CameraX 1.6 `ImageCapture.OutputFileOptions` handles IS_PENDING transaction internally | §Key APIs, §Code Example 5 | Planner has Phase 3 fallback ready (manual insert-update). User explicitly covered in D-31. |
| A3 | `FaceContour.LEFT_EYEBROW_TOP = 2`, `RIGHT_EYEBROW_TOP = 4` integer values are stable across ML Kit 16.x minor versions | §Code Example 4 | Low risk — ML Kit integer constants have been stable since 2018. If values shift, compile error (symbolic reference — we use `FaceContour.LEFT_EYEBROW_TOP`, not literal 2). |
| A4 | FACE contour has ~36 points ordered clockwise from forehead in sensor-space | §Code Example 4 cheek/chin fallback | If ordering/count differs, cheek/chin anchors map to wrong location. Phase 2 runbook already shows FACE contour renders — validate cheek anchor visually in Phase 3 handoff. |
| A5 | `Frame.timestamp` is monotonic vsync-aligned nanos (not wallclock) | §Code Example 7, §Pitfall 7 | Low risk — Phase 2's OverlayEffectBuilder already reads `frame.timestampNanos` (per the code: `renderer.draw(canvas, snapshot, frame.timestampNanos)`). |
| A6 | ML Kit `com.google.mlkit:face-detection` 16.1.7 preserves ADR-01 null-trackingId behavior under CONTOUR_MODE_ALL | §Pitfall 5 | High priority — if Google fixes this in a minor version (unlikely per ADR-01 alternatives table), the BboxIouTracker still works; it just becomes redundant. No regression path. |
| A7 | Xiaomi 13T / HyperOS `ImageCapture` + MediaStore DCIM/Bugzz works without MIUI quirks | §Pitfall 10 | Medium — if a quirk surfaces (file saves but invisible in Google Photos), plan has fallback (`MediaScannerConnection.scanFile`). Document in handoff. |
| A8 | `apktool d reference/com.insect...apk` extracts bug sprite PNGs to `assets/` or `res/drawable-xxhdpi/` | §Standard Stack (Tools) | If sprites are webp / jpeg / atlas'd, Wave 0 manifest.json + sizing may need adjustment. Plan budgets 1 task for this investigation. |
| A9 | Reference app front-cam JPEG is mirrored (matches selfie-preview convention) | §User Constraints D-17/18 | Wave 0 explicitly verifies this on-device. If reference is un-mirrored, override `ImageCapture.setReversedHorizontal(false)` — documented D-19 path. |
| A10 | `kotlinx.serialization.json` 1.8.0 is on classpath | §Standard Stack | VERIFIED at libs.versions.toml:73 — `kotlinx-serialization-json` entry exists. Not an assumption. |
| A11 | Xiaomi 13T / HyperOS has no known camera-HAL quirks affecting CameraX 1.6 + ML Kit 16.1.7 + Snapdragon 8+ Gen 1 combo | §Pitfall 10 | Low — Phase 2 device handoff succeeded without OEM quirks. |

**If this table is empty:** Not applicable — 11 assumptions flagged. A7–A9 are high-priority Wave 0 device-inspection items (planner must stage those tasks first).

---

## Open Questions

### Q1: Does `BboxIouTracker.assign()` return removed IDs inline or via side-channel?

- **What we know:** D-23/D-25 specify that removed IDs must trigger `LandmarkSmoother.onFaceLost(id)`.
- **What's unclear:** Implementation signature — `fun assign(faces): List<TrackedFace>` (D-20 literal) vs `fun assign(faces): TrackerResult(tracked, removedIds)`.
- **Recommendation:** Refine to `TrackerResult` — cleaner contract than side-channel. Planner should pick during Plan writing; either satisfies D-20..26. Research recommends `TrackerResult`.

### Q2: Front-camera ImageCapture mirror — `MirrorMode` on ImageCapture.Builder in CameraX 1.6?

- **What we know:** CameraX 1.6 added mirror-mode support to ImageCapture (per research blog). VideoCapture has `MIRROR_MODE_ON_FRONT_ONLY` since 1.3.
- **What's unclear:** Whether the `ImageCapture.Builder.setMirrorMode(...)` API is present on the 1.6.0 AAR published on Google Maven (vs preview/beta).
- **Recommendation:** Wave 0 task: `@Grep` the CameraX AAR on local Gradle cache (`~/.gradle/caches/modules-2/files-2.1/androidx.camera/camera-core/1.6.0/*/camera-core-1.6.0.aar` → unzip → `classes.jar` → javap `ImageCapture.Builder`). If absent, the fallback is post-capture bitmap mirror. If present, set `ImageCapture.Builder().setMirrorMode(MirrorMode.MIRROR_MODE_ON_FRONT_ONLY)` in `CameraController.bind`.

### Q3: `apktool` decode output — where do ant + spider sprites live in the reference APK?

- **What we know:** reference APK is 67MB; SUMMARY forensics confirmed 20 asset subfolders + 640+ drawables. Server-driven catalog download — sprites are in the bundled assets or drawables.
- **What's unclear:** Exact path (`assets/bugs/ant/*.png` vs `res/drawable-xxhdpi/ant_frame_*.png` vs atlas).
- **Recommendation:** Wave 0 Task: run `apktool d -o reference-extracted reference/com.insect.filters.funny.prank.bug.filter.face.camera.apk`; scan `find reference-extracted -iname "*ant*.png"`. Document in plan tasks; update D-06 path if different from guess.

### Q4: Frame durations for ant + spider flipbook — from reference animation?

- **What we know:** D-06 says `frameDurationMs` is per-filter in manifest.json.
- **What's unclear:** Reference app's actual frame durations (if ant animation is stored as 30fps GIF vs 15fps APNG).
- **Recommendation:** Default to 66ms/frame (~15fps playback) for Phase 3 Wave 0; tune empirically in handoff. Per-filter override in manifest.json.

### Q5: `DCIM/Bugzz/` vs `Pictures/Bugzz/` — matches reference?

- **What we know:** Phase 2 VideoCapture saves to `DCIM/Bugzz/`. STATE.md open question flags this as "inspect reference runtime in Phase 3; default DCIM/Bugzz/".
- **What's unclear:** Reference app's actual photo save directory.
- **Recommendation:** Wave 0 task — install reference APK, capture a photo, run `adb shell find /sdcard/DCIM /sdcard/Pictures -iname "*.jpg" -newermt "10 minutes ago"`. Default to `DCIM/Bugzz/` if ambiguous; document finding in handoff.

---

## Environment Availability

| Dependency | Required By | Available | Version | Fallback |
|------------|------------|-----------|---------|----------|
| Xiaomi 13T device via USB ADB | CAP-04/05/06 device handoff, D-17 mirror inspection, REN-08 subjective smoothness | ✓ (Phase 2 precedent) | HyperOS | — |
| Google Photos on device | CAP-05 validation | ✓ (preinstalled on Xiaomi 13T) | — | `adb shell content query --uri content://media/external/images/media` |
| Android Studio profiler | D-37 optional REN-08 trace | ✓ | Ladybug+ | Skip — D-37 says subjective is sufficient |
| `apktool` CLI | D-05 Wave 0 asset extract | ✗ (not verified on current dev box) | — | Install via `brew install apktool` / `choco install apktool` / [apktool.org direct download](https://apktool.org/). Required before Wave 0 asset task. |
| reference APK on dev box | Wave 0 extract + CAP-04 install | ✓ | `reference/com.insect.filters.funny.prank.bug.filter.face.camera.apk` (verified present) | — |
| Kotlin JDK 21 (Android Studio `jbr`) | Build | ✓ (Phase 1 verified) | 21 | — |

**Missing dependencies with no fallback:** None.

**Missing dependencies with fallback:** `apktool` CLI — Wave 0 first task is "install apktool if not present" on the dev box.

---

## Validation Architecture

### Test Framework

| Property | Value |
|----------|-------|
| Framework | JUnit 4 (unit, pure JVM) + Robolectric 4.13 (where CameraX construction / PointF shadowing required — Phase 2 learned pattern STATE #12) |
| Config file | `gradle/libs.versions.toml` entries already present (`junit`, `mockito-core`, `mockito-kotlin`, `robolectric`); `app/build.gradle.kts` already configures `testOptions.unitTests.isReturnDefaultValues = true` + Robolectric dep |
| Quick run command | `./gradlew :app:testDebugUnitTest` |
| Full suite command | `./gradlew :app:testDebugUnitTest :app:assembleDebug` |

### Phase Requirements → Test Map

| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| REN-01 | `FilterEngine.onDraw` invokes behavior tick + draws bitmap at computed position | unit (Robolectric if Canvas mock needed) | `./gradlew :app:testDebugUnitTest --tests "*FilterEngineTest*"` | ❌ Wave 0 — `FilterEngineTest.kt` new |
| REN-02 | `BugBehavior` sealed interface has exactly 4 variants; STATIC.tick sets position=anchor, velocity=0 | unit | `./gradlew :app:testDebugUnitTest --tests "*BugBehaviorStaticTest*"` | ❌ Wave 0 — `BugBehaviorStaticTest.kt` new |
| REN-03 | `FaceLandmarkMapper.anchorPoint(face, NOSE_TIP)` returns NOSE_BRIDGE last point when populated; falls back to boundingBox center when not | unit (Robolectric for `PointF`, per Phase 2 `DebugOverlayRendererTest` pattern) | `./gradlew :app:testDebugUnitTest --tests "*FaceLandmarkMapperTest*"` | ❌ Wave 0 — `FaceLandmarkMapperTest.kt` new (replaces Phase 2 stub body; tests land alongside implementation) |
| REN-04 | `AssetLoader` `sizeOf` returns `bitmap.allocationByteCount`; cache capped at min(32MB, maxMemory/8); `get` returns null before preload, bitmap after | unit + Robolectric (Bitmap creation) | `./gradlew :app:testDebugUnitTest --tests "*AssetLoaderTest*"` | ❌ Wave 0 — `AssetLoaderTest.kt` new |
| REN-05 | Flipbook frame index = `((elapsed / frameDurationNanos) % frameCount)`; verified by driving synthetic `frame.timestamp` values | unit | `./gradlew :app:testDebugUnitTest --tests "*FilterEngineTest*flipbook*"` | ❌ Wave 0 — `FilterEngineTest.kt` covers |
| REN-06 | `FilterEngine.onDraw(face=null)` returns early without drawing | unit (Canvas mock with `verify(canvas, never()).drawBitmap(...)`) | `./gradlew :app:testDebugUnitTest --tests "*FilterEngineTest*noFace*"` | ❌ Wave 0 |
| REN-07 | `setFilter(B)` followed immediately by `onDraw` returns without drawing (preload not done); `setFilter(B)` after preload draws B's sprite; no CameraX rebind happens (AtomicReference only) | unit | `./gradlew :app:testDebugUnitTest --tests "*FilterEngineTest*swap*"` | ❌ Wave 0 |
| REN-08 | ≥24fps subjective smoothness on Xiaomi 13T during normal filter playback | **manual** | — | 03-HANDOFF.md Step |
| CAP-01 | `CameraController.capturePhoto` invokes `ImageCapture.takePicture` with populated OutputFileOptions | unit (Robolectric — same pattern as Phase 2 `CameraControllerTest`) | `./gradlew :app:testDebugUnitTest --tests "*CameraControllerTest*capturePhoto*"` | ❌ Wave 0 — extend existing `CameraControllerTest.kt` |
| CAP-02 | JPEG on Xiaomi 13T shows bug sprite baked in (same position as preview) | **manual** (Phase 2's OverlayEffect + three-stream baking already proven for VIDEO_CAPTURE via gaps-03) | — | 03-HANDOFF.md Step |
| CAP-03 | OutputFileOptions built with RELATIVE_PATH="DCIM/Bugzz", MIME_TYPE="image/jpeg", IS_PENDING absent (CameraX handles) | unit (Robolectric) | `./gradlew :app:testDebugUnitTest --tests "*CameraControllerTest*mediaStore*"` | ❌ Wave 0 |
| CAP-04 | Front-cam JPEG mirror matches reference app convention (Wave 0 device inspection result applied) | **manual** — Wave 0 inspection precedes test spec | — | 03-HANDOFF.md Step + Wave 0 apk-install task |
| CAP-05 | Saved photo appears in Google Photos on Xiaomi 13T within 1s of capture | **manual** | — | 03-HANDOFF.md Step |
| CAP-06 | No LeakCanary notification after 30 consecutive captures | **manual** | — | 03-HANDOFF.md Step (30-capture + kill/relaunch runbook) |
| **ADR-01 #1** | `BboxIouTracker.assign` matches existing IDs by IoU ≥ 0.3; assigns monotonic new IDs; removes after 5 dropout frames | unit | `./gradlew :app:testDebugUnitTest --tests "*BboxIouTrackerTest*"` | ❌ Wave 0 — `BboxIouTrackerTest.kt` new |
| **ADR-01 #2** | `LandmarkSmoother.onFaceLost(id)` clears that id's filter state; re-creation starts fresh | unit | `./gradlew :app:testDebugUnitTest --tests "*LandmarkSmootherTest*onFaceLost*"` | ❌ Wave 0 — extend OneEuroFilterTest or new file |
| **ADR-01 #3** | `FaceDetectorClient.createAnalyzer` consumer passes faces through tracker before SmoothedFace mapping; SmoothedFace.trackingId is tracker-assigned ID | integration (Robolectric; mock MlKitAnalyzer consumer) | `./gradlew :app:testDebugUnitTest --tests "*FaceDetectorClientTest*tracker*"` | ❌ Wave 0 |
| **ADR-01 #4** | `02-VERIFICATION.md` CAM-08 row updated post-handoff to reference tracker-assigned ID stability | **doc task** (no automated test) | — | Planner task in post-handoff wave |

### Sampling Rate (Nyquist)

- **Per task commit:** `./gradlew :app:testDebugUnitTest` — must be green before the `feat`/`fix` commit lands (Phase 2 enforcement pattern).
- **Per wave merge:** `./gradlew :app:testDebugUnitTest :app:assembleDebug` — must be green + clean 82MB APK build.
- **Phase gate:** Full suite + device handoff per 03-HANDOFF.md before `/gsd-verify-work`.

### Wave 0 Gaps

New test files required (all pure JVM unless noted):

- [ ] `app/src/test/java/com/bugzz/filter/camera/detector/BboxIouTrackerTest.kt` — unit (pure JVM, uses mock `Face` via Mockito stubbing `boundingBox`); covers IoU math, greedy match, MAX_TRACKED_FACES cap, dropout increment, monotonic ID
- [ ] `app/src/test/java/com/bugzz/filter/camera/detector/FaceLandmarkMapperTest.kt` — unit (Robolectric if `PointF` shadowing needed like existing `DebugOverlayRendererTest.kt`); covers all 7 anchors for primary, fallback, ultimate-fallback paths
- [ ] `app/src/test/java/com/bugzz/filter/camera/filter/FilterCatalogTest.kt` — unit (pure JVM); pins 2 filters registered + anchor types + behavior = STATIC + scale factor
- [ ] `app/src/test/java/com/bugzz/filter/camera/filter/AssetLoaderTest.kt` — Robolectric (Bitmap decode); covers sizeOf formula, preload idempotency, get-before-preload returns null
- [ ] `app/src/test/java/com/bugzz/filter/camera/render/FilterEngineTest.kt` — unit (Robolectric for Canvas); covers no-face early return, flipbook frame-index math on synthetic timestamps, swap-without-preload skips drawing
- [ ] `app/src/test/java/com/bugzz/filter/camera/render/BugBehaviorTest.kt` — unit (pure JVM); pins STATIC.tick(state, face, anchor, dt) sets position=anchor; CRAWL/SWARM/FALL throw NotImplementedError
- [ ] Extend `app/src/test/java/com/bugzz/filter/camera/camera/CameraControllerTest.kt` with `capturePhoto_*` tests — Robolectric already wired (Phase 2 STATE #12)
- [ ] Extend `app/src/test/java/com/bugzz/filter/camera/detector/OneEuroFilterTest.kt` with `LandmarkSmoother_onFaceLost_clears_that_id_only` test

No framework install needed — JUnit 4 + Robolectric + Mockito already on classpath.

---

## Security Domain

> Phase 3 ships shutter-button + MediaStore photo write + on-device-only sprite overlay. No network, no user accounts, no analytics. Threat surface is narrow.

### Applicable ASVS Categories

| ASVS Category | Applies | Standard Control |
|---------------|---------|-----------------|
| V2 Authentication | no | — (no login) |
| V3 Session Management | no | — |
| V4 Access Control | no | — |
| V5 Input Validation | partial | manifest.json parsing must reject malformed JSON; sprite PNG decode must handle `IllegalArgumentException` without crashing |
| V6 Cryptography | no | — (no encryption; photos are user-initiated writes to public MediaStore) |
| V7 Error Handling | yes | `capturePhoto` error path → Toast; never log biometric data (T-02-06 already enforced) |
| V8 Data Protection | yes | Photos saved to DCIM/Bugzz are **user-visible** by design. No silent exfiltration. No EXIF location (revisit Phase 6). |
| V10 Malicious Code | yes | reference APK assets extracted and bundled — verify provenance (`sha256sum reference/*.apk`). Currently personal-use only; CLAUDE.md bans publishing with these assets. |
| V12 Files/Resources | yes | Sprite assets loaded from APK, not external. Bitmap decode via `BitmapFactory.decodeStream` is already sandboxed by Android (no shell out). |
| V14 Configuration | yes | `android:allowBackup` default should be `false` for biometric-adjacent data (revisit Phase 6 UX — not Phase 3) |

### Known Threat Patterns for the Phase 3 Stack

| Pattern | STRIDE | Standard Mitigation |
|---------|--------|---------------------|
| **OOM decode on malformed sprite asset** | Denial of Service | Wrap `BitmapFactory.decodeStream` in try/catch `IllegalArgumentException` + `OutOfMemoryError`; emit `OneShotEvent.Error("filter decode failed")` and fall back to DebugOverlayRenderer or no-op |
| **Storage exhaustion during MediaStore write** | Denial of Service / Availability | `ImageCapture.OnImageSavedCallback.onError` path; emit error Toast; no silent failure |
| **Biometric data logging** (T-02-06, Phase 2) | Information Disclosure | Already enforced: `Timber.tag("FaceTracker")` logs aggregate coords only (center x/y, contour count) — never per-landmark lists. FilterEngine must not log face data in production. |
| **Tapjacking on shutter button** | Elevation of Privilege | Low risk — personal-use app, no sensitive permission gating. Compose Material3 buttons are not tappable-through by default. Phase 7 cross-OEM can audit `filterTouchesWhenObscured` if Play Store bound. |
| **Overlay biometric leak in release build** (DebugOverlayRenderer drawing face contours) | Information Disclosure | `DebugOverlayRenderer.draw` FIRST statement is `if (!BuildConfig.DEBUG) return` — Phase 2 already enforced. Verify regression on release build at phase gate. |
| **Reference APK asset IP** | Legal / Reputation | Out of Phase 3 scope but noted per CLAUDE.md — personal-use only; must replace assets before Play Store publication. `reference/manifest.json` contains original package ID — do not ship with Bugzz release builds. |

**Phase 3 threat model candidate bullets (feed into each PLAN.md `<threat_model>` per workflow step 5.55):**

1. **MediaStore write denial-of-service:** Device storage full → `ImageCapture.OnImageSavedCallback.onError(ImageCaptureException)`. Mitigation: error path emits `OneShotEvent.Error("Không đủ bộ nhớ")` (English per CLAUDE.md: "Storage full"); no crash, no retry loop.
2. **OOM on malformed sprite decode:** Corrupted PNG in assets → `BitmapFactory.decodeStream` returns null or throws. Mitigation: `AssetLoader.preload` catches + emits structured error; filter is disabled; user sees Toast. Test: unit test with malformed bytes → verify fallback.
3. **Tapjacking on shutter button** (Snapchat/similar prank risk): third-party overlay invokes shutter without user intent. Mitigation: Phase 3 is personal-use; no action required. Phase 7 cross-OEM audit re-visits.
4. **LeakCanary-detected memory leak after 30 captures** (CAP-06): `ImageCapture` callback retains ViewModel or Activity reference via lambda closure. Mitigation: `capturePhoto(onResult)` callback lifecycle — ViewModel always clears active callbacks in `onCleared`. Manual 30-capture runbook per D-36.
5. **Biometric data in logs:** FilterEngine must not log SmoothedFace contours. Mitigation: `Timber` statements in FilterEngine log only filter id + frame index + anchor type (never PointF lists). Phase 2 T-02-06 grep policy extended to Phase 3 files.
6. **Reference APK assets shipped to release** (legal): Phase 3 release build would embed ant + spider sprites from reference APK. Mitigation: personal-use only (CLAUDE.md lock); Release-build gate in Phase 7 cross-OEM audits asset provenance before Play Store submission.

---

## Sources

### Primary (HIGH confidence)

- [Capture an image | Android Developers](https://developer.android.com/media/camera/camerax/take-photo) — canonical `ImageCapture.OutputFileOptions` code sample; IS_PENDING automatic
- [FaceContour | ML Kit | Google Developers](https://developers.google.com/android/reference/com/google/mlkit/vision/face/FaceContour) — integer constants 1-15 enumerated
- [OverlayEffect | API reference | Android Developers](https://developer.android.com/reference/kotlin/androidx/camera/effects/OverlayEffect) — onDrawListener contract
- [What's new in CameraX 1.4.0 (Android Developers Blog, Dec 2024)](https://android-developers.googleblog.com/2024/12/whats-new-in-camerax-140-and-jetpack-compose-support.html) — OverlayEffect Canvas API + Frame properties (overlayCanvas, sensorToBufferTransform, timestamp)
- [Introducing CameraX 1.5 (Android Developers Blog, Nov 2025)](https://android-developers.googleblog.com/2025/11/introducing-camerax-15-powerful-video.html) — CameraEffect ImageCapture flash/3A parity
- [Caching Bitmaps | Android Developers](https://developer.android.com/topic/performance/graphics/cache-bitmap) — LruCache sizeOf + maxMemory/8 pattern
- [LruCache | API reference | Android Developers](https://developer.android.com/reference/android/util/LruCache) — allocationByteCount recommendation
- [Apktool](https://apktool.org/) — reference APK decode workflow

### Secondary (MEDIUM confidence)

- [CameraX Preview overlay and saved Video Capture Overlay thread (camerax-developers Google group)](https://groups.google.com/a/android.com/g/camerax-developers/c/64eahzvdY4U) — community confirmation of uniform target callback
- [OverlayEffect does not work correctly (camerax-developers Google group)](https://groups.google.com/a/android.com/g/camerax-developers/c/k3eVmhXejpk) — known issues thread
- [ML Kit adds face contours — Firebase blog, Nov 2018](https://firebase.blog/posts/2018/11/ml-kit-adds-face-contours-to-create/) — original contour mode announcement (historical context)
- [Object Tracking: ByteTrack lineage (medium.com)](https://medium.com/tech-blogs-by-nest-digital/object-tracking-object-detection-tracking-using-bytetrack-0aafe924d292) — IoU greedy vs Hungarian at small scale
- [Computer Vision for Multi-Object Tracking](https://www.thinkautonomous.ai/blog/computer-vision-for-tracking/) — IoU threshold heuristics

### Tertiary (LOW confidence — for awareness only)

- [HyperOS bug tracker (xiaomi-miui.gr)](https://en.xiaomi-miui.gr/hyperos-bug-tracker-108770-2/) — general HyperOS camera issue aggregation; no CameraX-specific reports
- [Xiaomi HyperOS Weekly Bug Report (md-eksperiment.org)](https://md-eksperiment.org/en/post/20251230-xiaomi-hyperos-weekly-bug-report) — Redmi / Xiaomi camera app crashes; not CameraX

### Internal references (project-resident)

- `.planning/phases/02-camera-preview-face-detection-coordinate-validation/02-ADR-01-no-ml-kit-tracking-with-contour.md` — MANDATORY ADR feeding Phase 3 follow-ups
- `.planning/phases/02-camera-preview-face-detection-coordinate-validation/02-CONTEXT.md` D-12..D-28 — Phase 2 decisions carried into Phase 3
- `.planning/research/STACK.md`, `ARCHITECTURE.md`, `PITFALLS.md` §1-13, `SUMMARY.md` — root research used by Phase 2, all still apply
- `.planning/STATE.md` — accumulated execution context (#10 three-stream compositing validated, #12 Robolectric required pattern, #14 Hilt constructor-split pattern)
- Phase 2 source code read for this research:
  - `app/src/main/java/com/bugzz/filter/camera/render/OverlayEffectBuilder.kt`
  - `app/src/main/java/com/bugzz/filter/camera/render/DebugOverlayRenderer.kt`
  - `app/src/main/java/com/bugzz/filter/camera/camera/CameraController.kt`
  - `app/src/main/java/com/bugzz/filter/camera/detector/FaceDetectorClient.kt`
  - `app/src/main/java/com/bugzz/filter/camera/detector/FaceLandmarkMapper.kt` (stub)
  - `app/src/main/java/com/bugzz/filter/camera/detector/OneEuroFilter.kt` + LandmarkSmoother
  - `app/src/main/java/com/bugzz/filter/camera/detector/FaceSnapshot.kt`
  - `app/src/main/java/com/bugzz/filter/camera/ui/camera/CameraScreen.kt`
  - `app/src/main/java/com/bugzz/filter/camera/ui/camera/CameraViewModel.kt`
  - `app/src/main/java/com/bugzz/filter/camera/di/CameraModule.kt`
  - `gradle/libs.versions.toml`

---

## Metadata

**Confidence breakdown:**

- Standard Stack: **HIGH** — all versions verified in Phase 2 + libs.versions.toml grep
- Architecture Patterns: **HIGH** — every pattern is a documented CameraX 1.6 / ML Kit 16.x / Android LruCache / Kotlin Hilt pattern; Phase 2 already validated OverlayEffect + three-stream compositing + Robolectric on CameraX
- Don't Hand-Roll: **HIGH** — every listed alternative has first-party framework or library coverage
- Pitfalls: **HIGH** — Pitfalls 1, 2, 3, 5, 6, 7, 8, 9 are verified against training + Phase 2 gap-closure learnings. Pitfalls 4 and 10 are **MEDIUM** (need device inspection — explicit Wave 0 tasks).
- Runtime State Inventory: **N/A** (additive phase)
- Environment Availability: **HIGH** — apktool only non-verified dep, install trivial
- Validation Architecture: **HIGH** — extends Phase 2 JUnit+Robolectric harness unchanged
- Security Domain: **HIGH** — personal-use app, narrow surface, all V-categories covered or explicitly out of scope

**Overall confidence: HIGH.** Three `[ASSUMED]` items (A7, A8, A9) are mitigated by explicit Wave 0 device-inspection tasks that the planner must stage first. No hidden assumptions about CameraX 1.6 behavior — Phase 2 already ran every load-bearing primitive on Xiaomi 13T.

**Research date:** 2026-04-19
**Valid until:** 2026-05-19 (30 days — CameraX 1.6.0 is stable; ML Kit 16.1.7 is stable; no pending major releases known)

---

## RESEARCH COMPLETE

**Phase:** 3 - First Filter End-to-End + Photo Capture
**Confidence:** HIGH

### Key Findings

1. `OverlayEffect.setOnDrawListener` fires uniformly for every target in its target mask (PREVIEW | VIDEO_CAPTURE | IMAGE_CAPTURE) — Phase 2 already validated via MP4 gaps-03 red-rect bake; Phase 3 CAP-02 inherits this for free on JPEG. No target branching possible or needed.
2. CameraX 1.6 `ImageCapture.OutputFileOptions(contentResolver, collectionUri, contentValues)` handles IS_PENDING transaction automatically — no manual insert+update. Fallback pattern ready if Xiaomi 13T MIUI surfaces a ContentResolver quirk (D-17 Wave 0 task catches this).
3. `BboxIouTracker.assign` greedy algorithm is sufficient at `MAX_TRACKED_FACES = 2` — O(D × T) = 4 IoU calculations per frame, ~10μs. Monotonic `nextId++` guarantees LandmarkSmoother state cannot accidentally carry across faces (ADR-01 follow-ups closable in Wave 1).
4. `FaceContour` integer constants enumerated — `LEFT_EYEBROW_TOP=2` and `RIGHT_EYEBROW_TOP=4` must be added to `FaceDetectorClient.SMOOTHED_CONTOUR_TYPES` (~2-line change) for FOREHEAD anchor resolution in Filter #2.
5. All Phase 3 new files are additive — no Phase 2 code deleted. Three Phase 2 files get additive edits: `FaceDetectorClient.SMOOTHED_CONTOUR_TYPES`, `FaceLandmarkMapper.anchorPoint` body, `OverlayEffectBuilder` constructor (add FilterEngine inject), `CameraController.capturePhoto` method, `CameraViewModel` shutter handler, `CameraScreen` shutter + cycle buttons. `kotlinx.serialization.json` already in catalog — no new deps.

### File Created

`.planning/phases/03-first-filter-end-to-end-photo-capture/03-RESEARCH.md`

### Confidence Assessment

| Area | Level | Reason |
|------|-------|--------|
| Standard Stack | HIGH | Phase 2 validated + libs.versions.toml grep-verified |
| Architecture | HIGH | Every pattern is documented Google/CameraX/ML Kit pattern; Phase 2 already sliced 3 of 5 through production |
| Pitfalls | HIGH (8/10) / MEDIUM (2/10) | Pitfalls 4 (front-cam mirror) and 10 (HyperOS quirks) need Wave 0 device inspection — explicit planner tasks |
| Validation Architecture | HIGH | Extends Phase 2 JUnit+Robolectric harness unchanged; 6 new test files + 2 extensions |
| Security Domain | HIGH | Narrow personal-use surface; 6 threat bullets ready for planner `<threat_model>` blocks |

### Open Questions (to resolve during planning or Wave 0)

1. `BboxIouTracker.assign` signature — `List<TrackedFace>` (D-20 literal) vs `TrackerResult(tracked, removedIds)`? Research recommends latter.
2. CameraX 1.6 `ImageCapture.Builder.setMirrorMode` presence on published AAR — Wave 0 quick javap check.
3. apktool extract — exact sprite path in `assets/` vs `res/drawable-*` — Wave 0 extract + grep.
4. Reference app frame durations — Wave 0 extract + visual inspection.
5. Reference photo save dir (DCIM/Bugzz vs Pictures/Bugzz) — Wave 0 device adb scan.

### Ready for Planning

Research complete. Planner can now create PLAN.md files. Recommended plan structure:

- **Wave 0 (asset prep + device inspection):**
  - Plan 03-00: `apktool` install + reference APK extract (ant + spider frames) + reference device-install + front-cam photo inspection + DCIM-vs-Pictures check + Nyquist test scaffolds (8 test files RED)
- **Wave 1 (ADR-01 close — mandatory prerequisite):**
  - Plan 03-01: `BboxIouTracker` + re-key `LandmarkSmoother` + thread tracker through `FaceDetectorClient.createAnalyzer` + extend `SmoothedFace.trackingId` to non-null tracker-assigned int
- **Wave 2 (filter engine):**
  - Plan 03-02: `FilterDefinition` + `FilterCatalog` (2 filters) + `BugBehavior` sealed + `BugState` + `AssetLoader`
  - Plan 03-03: `FilterEngine` + extend `FaceLandmarkMapper.anchorPoint` body + extend `OverlayEffectBuilder` to inject FilterEngine + extend `FaceDetectorClient.SMOOTHED_CONTOUR_TYPES`
- **Wave 3 (photo capture + UI):**
  - Plan 03-04: `CameraController.capturePhoto` + extend `CameraViewModel` + extend `OneShotEvent` (PhotoSaved/Error) + extend `CameraScreen` (shutter button + Cycle Filter button + TEST RECORD reposition + capture-flash animation)
- **Wave 4 (sign-off):**
  - Plan 03-05: Clean build + `03-HANDOFF.md` Xiaomi 13T runbook + 30-capture leak test + post-handoff `02-VERIFICATION.md` CAM-08 update (ADR-01 follow-up #4)

Five plans, four waves, ~15 new files, ~8 Phase 2 files additively extended, 8 new unit test files. Risk profile is **LOW** — every decision is locked in CONTEXT, every primitive was Phase 2-validated, and the longest greenfield path (BboxIouTracker ~100 LOC) is a well-specified algorithm with HIGH-confidence pseudocode already in this research.
