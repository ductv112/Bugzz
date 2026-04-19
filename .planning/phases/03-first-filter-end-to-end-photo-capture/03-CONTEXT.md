# Phase 3: First Filter End-to-End + Photo Capture - Context

**Gathered:** 2026-04-19
**Status:** Ready for planning

<domain>
## Phase Boundary

Prove the full render + capture pipeline with **one production filter** so every remaining phase is content/feature work on a validated engine. Scope: `FilterEngine` drawing animated bug sprites onto the `OverlayEffect` canvas via `setOnDrawListener`, `AssetLoader` + `LruCache<String, Bitmap>` for sprite decode/reuse, `FilterDefinition` data model with state-machine architecture for 4 behaviors (STATIC, CRAWL, SWARM, FALL) with only STATIC implemented, `FaceLandmarkMapper` production body resolving anchor keys to sensor-space points from `SmoothedFace.contours`, `BboxIouTracker` as ADR-01 mandatory follow-up that replaces the `-1` sentinel in `LandmarkSmoother`, production shutter flow via `ImageCapture.takePicture()` + `MediaStoreOutputOptions` saving to `DCIM/Bugzz/` with `IS_PENDING` transaction, Toast confirmation on camera, CAP-04 front-camera mirror convention matched to reference app behavior.

**Out of scope:** filter catalog (Phase 4), filter picker LazyRow UI (Phase 4), Face Filter vs Insect Filter mode selector (Phase 4 + 5), CRAWL / SWARM / FALL behavior implementations (Phase 4), video recording production UX (Phase 5), full PreviewRoute production UX with Save/Share/Delete/Retake (Phase 6), formal ≥24fps profiler-measured validation (Phase 7 — Phase 3 only confirms observed smoothness + optional profiler trace during handoff).

</domain>

<decisions>
## Implementation Decisions

### Filter #1 Spec (gray area 1)
- **D-01:** First production filter is **ant-on-nose, STATIC behavior**. Filter id `ant_on_nose_v1`. Anchor `FaceLandmarkMapper.Anchor.NOSE_TIP` (resolved from `FaceContour.NOSE_BRIDGE` centroid with `FaceContour.NOSE_BOTTOM` fallback). Bug sprite drawn at the resolved sensor-space point, scaled to ~20% of face boundingBox width. Matches ROADMAP §Phase 3 example explicitly.
- **D-02:** Filter #2 (for REN-07 swap runtime verification) is **spider-on-forehead, STATIC behavior**. Filter id `spider_on_forehead_v1`. Anchor `FaceLandmarkMapper.Anchor.FOREHEAD` (resolved as midpoint between left+right eyebrow outer contours, offset upward by ~15% face-box height). Second filter exists to exercise `FilterEngine.setFilter()` swap + LruCache key eviction on separate asset set.
- **D-03:** Both sprites are **animated flipbook** (multi-frame PNG sequence). `FrameLoop` at research-prescribed 24fps playback (see Claude discretion for timing source). Single still image not acceptable — would defer REN-05 run-time validation into Phase 4.
- **D-04:** REN-02 "state machine supports 4 behaviors" means: `BugBehavior` sealed interface ships with **all 4 variants declared** (STATIC, CRAWL, SWARM, FALL) + `BugState` data class fields sized for all behaviors (position, velocity, path progress, frame index). Only STATIC has implemented body in Phase 3. CRAWL/SWARM/FALL bodies are `TODO("Phase 4")` — the interface exists, the renderer dispatch exists, the data class supports them, but calling them throws or no-ops explicitly. This satisfies REN-02 without pulling Phase 4 scope forward.

### Sprite Source + Asset Pipeline (gray area 1)
- **D-05:** **Extract 2 bug sprites from reference APK in Phase 3 Wave 0** (ant for filter #1, spider for filter #2). Use `apktool` to unpack `reference/com.insect.filters.funny.prank.bug.filter.face.camera.apk`; locate assets under `assets/` or `res/drawable-xxhdpi/` (path confirmed at extract time). Phase 4 extracts the remaining 13-23 bugs for the catalog. **Do NOT extract all bugs in Phase 3** — keeps Phase 3 focused on pipeline, not asset prep.
- **D-06:** Sprite asset layout: `app/src/main/assets/sprites/<filterId>/frame_00.png ... frame_NN.png` + `app/src/main/assets/sprites/<filterId>/manifest.json` (fields: `id`, `displayName`, `frameCount`, `frameDurationMs`, `anchorType`, `behavior`, `scaleFactor`, `mirrorable`). Consistent with Phase 4's expected asset scale-up (catalog just enumerates directories).
- **D-07:** If reference APK only has a single frame for ant (unlikely — the app is animated), Wave 0 extractor falls back to single-frame rendering **for that filter** and files a follow-up for Phase 4 to source additional frames. The FilterEngine supports `frameCount = 1` as a valid degenerate case (no-op flipbook advance).
- **D-08:** Bitmap decode config default: `Bitmap.Config.ARGB_8888` (alpha-aware — bugs have soft edges + shadow). Per-filter `manifest.json` can opt into `RGB_565` for fully-opaque sprites. Decode happens on `cameraExecutor` (single-thread, off-main) at `AssetLoader.load(filterId)` time; results cached in `LruCache<String, Bitmap>` keyed on `"$filterId/frame_$idx"`.
- **D-09:** `LruCache` size: 32 MB max (`Runtime.getRuntime().maxMemory()/8` cap, whichever is smaller). With 2 filters × ~15 frames × ~200KB = ~6 MB typical; Phase 4's 25-filter catalog needs ~75 MB which triggers eviction — this is intentional and validates REN-04 eviction path in Phase 3.

### REN-07 Filter Swap Verification (gray area 1)
- **D-10:** Debug-only `Cycle Filter` button in `CameraScreen` (BuildConfig.DEBUG gated), positioned BottomEnd. Tap cycles active filter A → B → A. Handoff runbook validates: (a) swap takes effect <1 frame visually, (b) no preview freeze, (c) no CameraX rebind (no "Camera in use" errors, no black flash), (d) LruCache loads filter B's asset set on first swap without main-thread stall.
- **D-11:** `FilterEngine.setFilter(filterId: String)` writes to `AtomicReference<FilterDefinition>`; `renderExecutor`'s `onDrawListener` reads-latest each frame. No rebind path. `AssetLoader.preload(filterId)` is invoked synchronously inside `setFilter()` on `cameraExecutor` before the new ID becomes current; if preload has not completed by the next draw, the renderer skips drawing for that frame (no last-frame ghost from the old filter).

### Shutter UX (gray area 2)
- **D-12:** After shutter tap: **stay on `CameraScreen`**. Show Toast "Đã lưu vào Gallery" (or "Saved to gallery" — app UI is English per PROJECT.md, so pick English; Vietnamese here is conversation-only). Optional: Snackbar with "View" action that opens the saved MediaStore URI via `Intent.ACTION_VIEW`. **Do not** navigate to PreviewRoute — Phase 6 owns PreviewRoute production UX.
- **D-13:** Shutter button: **72dp circle, white fill, gray stroke 2dp**, bottom-center with 24dp bottom padding. Z-order above the `CameraXViewfinder`. Uses `Modifier.clip(CircleShape)` + `.background(Color.White)`. Tap handler calls `viewModel.onShutterTapped()` which launches `cameraController.capturePhoto()` on `cameraExecutor`.
- **D-14:** Phase 2's TEST RECORD button is **kept** but moved to `BottomStart` (offset to not overlap shutter) with BuildConfig.DEBUG gate unchanged. Phase 5 replaces it with production record UI. Rationale: keeps gap-verification ability for Phase 2 CAM-06 (MP4 overlay bake) if regression suspected.
- **D-15:** Shutter feedback: **haptic only** via `HapticFeedback.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)` on tap. **No MediaActionSound** (avoid region-locked audio enforcement edge cases — Phase 6 UX-09 settings will decide global sound policy). Capture success: Toast (haptic already fired on tap).
- **D-16:** Brief capture-flash animation: 150ms white overlay alpha-fade on successful capture (drawn in Compose over the Viewfinder, not in OverlayEffect — capture-flash is UI concern, not a camera-pipeline concern). Optional; Claude decides exact timing.

### Front-Camera Mirror Convention (gray area 4 — CAP-04)
- **D-17:** **Inspect reference APK runtime as a Wave 0 task** before locking implementation: install `reference/com.insect.filters.funny.prank.bug.filter.face.camera.apk` on Xiaomi 13T via `adb install -r`, capture a front-camera photo, open it in Google Photos, compare to selfie-preview orientation. Document result in `03-RESEARCH.md` (or planner's research output). Default implementation assumes reference mirrors (common selfie convention); if inspection reveals un-mirrored, override with `ImageCapture.setReversedHorizontal(false)` for front camera.
- **D-18:** Implementation: default = front-camera JPEG is **mirrored** (matches what user sees in preview). Back-camera JPEG is **not mirrored** (standard photography convention). CameraX `ImageCapture` respects `MIRROR_MODE` on some paths; for ImageCapture specifically, front-cam mirror handling is via the camera sensor orientation + buffer transform. Verify on device — if CameraX default doesn't match expected "mirrored selfie", post-process the captured bitmap or use effect-level transform.
- **D-19:** If reference inspection reveals a non-default convention (e.g., un-mirrored save despite mirrored preview — some apps do this intentionally), the deviation is documented in `03-SUMMARY.md` and the research cross-reference `PITFALLS.md` grows a new entry for future phases.

### BboxIouTracker Spec (gray area 3 — ADR-01 mandatory)
- **D-20:** Implement `com.bugzz.filter.camera.detector.BboxIouTracker` as `@Singleton @Inject`. API: `fun assign(faces: List<Face>): List<TrackedFace>` where `TrackedFace = (id: Int, face: Face)`. Internally maintains `Map<Int, TrackedEntry>` where `TrackedEntry = (lastBoundingBox: Rect, framesSinceLastSeen: Int, scheduledForRemoval: Boolean)`.
- **D-21:** **IoU threshold = 0.3** (permissive, matches MediaPipe tracker defaults + tolerates natural bbox jitter + head rotation). **Dropout retention = 5 frames** (~165ms at 30fps). Both as `companion object const val IOU_MATCH_THRESHOLD = 0.3f` + `MAX_DROPOUT_FRAMES = 5` for unit-test pinning + Phase 4 tuning.
- **D-22:** **Max tracked faces = 2**. When more than 2 detected, tracker retains the 2 largest-bbox faces by area. Rationale: Phase 4 CAT success criterion #5 specifies 2-face scene support; wider cap is unneeded CPU cost. Cap as `companion object const val MAX_TRACKED_FACES = 2`.
- **D-23:** ID assignment algorithm per frame:
  1. For each detected face, compute IoU against every `TrackedEntry.lastBoundingBox` still within dropout retention.
  2. Greedy best-match: pick pair with highest IoU ≥ threshold; assign existing ID; remove from both pools; repeat.
  3. Unmatched detected faces → assign new monotonically-increasing integer ID (`nextId++`). Skip if would exceed `MAX_TRACKED_FACES` (lowest-IoU unmatched face dropped silently).
  4. Unmatched tracked entries → `framesSinceLastSeen++`; if > `MAX_DROPOUT_FRAMES`, flag `scheduledForRemoval`; removed after frame callback completes (avoid mutating during iteration).
- **D-24:** Filter rendering targets **primary face only** in Phase 3. Primary = largest-bbox tracked face. Secondary face has its ID tracked + smoother state retained (for Phase 4 multi-face CRAWL continuity) but no sprite drawn. Phase 4 promotes secondary rendering based on filter behavior policy.
- **D-25:** `LandmarkSmoother` re-keys from `-1` sentinel (Phase 2 D-22 ADR-01) to `BboxIouTracker`-assigned ID. On tracker emitting "removed" for an ID, `LandmarkSmoother.onFaceLost(id: Int)` clears the corresponding per-landmark 1€ filter state map. On same ID reappearing (shouldn't happen per D-23 algorithm since removed IDs are not recycled, but defensive code): re-initialize with fresh filter state (no stale carry-over).
- **D-26:** Wire tracker: `FaceDetectorClient.createAnalyzer()` invokes `tracker.assign(faces)` inside `MlKitAnalyzer` consumer **before** mapping to `SmoothedFace`; `trackingId` field on `SmoothedFace` is replaced by tracker-assigned `id: Int` (no nullability — tracker always produces an ID). Update `02-VERIFICATION.md` CAM-08 row: acceptance criterion changes from "face.trackingId stable for 60+ frames" to "tracker-assigned ID stable across 60+ frames while face is held still". Re-verify during Phase 3 handoff.

### Filter Engine Architecture
- **D-27:** `com.bugzz.filter.camera.render.FilterEngine` is a new `@Singleton @Inject` class. Replaces `DebugOverlayRenderer` for production filter drawing. DebugOverlayRenderer is **retained** (not deleted) + gated behind `BuildConfig.DEBUG && prefs.debugOverlayEnabled`; production filter is always drawn when `FilterEngine.activeFilter != null`. Both renderers can draw in the same frame (DebugOverlayRenderer on top) — useful for Phase 3 handoff to verify bug position against red-rect overlay.
- **D-28:** `FilterEngine.onDraw(canvas: Canvas, frame: OverlayEffect.Frame, face: SmoothedFace?)` is called inside `OverlayEffectBuilder`'s `setOnDrawListener`. Signature aligns with `DebugOverlayRenderer`'s existing pattern. `face` is null when no face detected → early return, draw nothing (REN-06). Same `canvas.setMatrix(frame.sensorToBufferTransform)` pattern as Phase 2 before draw.
- **D-29:** `FilterDefinition` immutable data class: `id: String`, `displayName: String`, `anchorType: FaceLandmarkMapper.Anchor`, `behavior: BugBehavior`, `frameCount: Int`, `frameDurationMs: Long`, `scaleFactor: Float` (fraction of face-box width), `assetDir: String` (relative under `assets/sprites/`), `mirrorable: Boolean`.
- **D-30:** `FaceLandmarkMapper.anchorPoint()` production body: resolve each anchor from `SmoothedFace.contours` primarily, fallback to `SmoothedFace.landmarks` (Phase 2 stub's enum is reused). NOSE_TIP = `FaceContour.NOSE_BRIDGE` last point (closest to tip) or boundingBox center as ultimate fallback. FOREHEAD = mean of `FaceContour.LEFT_EYEBROW_TOP` first + `FaceContour.RIGHT_EYEBROW_TOP` last, offset upward by 15% face-box height. LEFT_CHEEK / RIGHT_CHEEK = `FaceContour.FACE` inner contour at 40% jaw progress. Fallback-to-boundingBox is explicit; returns `null` only when even boundingBox is null (should never happen for a detected face).

### Photo Capture Pipeline
- **D-31:** `CameraController.capturePhoto()` uses `ImageCapture.takePicture(outputOptions, executor, callback)` with `ImageCapture.OutputFileOptions.Builder(contentResolver, IMAGE_CONTENT_URI, contentValues)` — **CameraX's built-in MediaStore writer** (preferred over manual two-step `insert(IS_PENDING=1)` → `openOutputStream` → `update(IS_PENDING=0)` because CameraX 1.6 handles the `IS_PENDING` transaction for us via `OutputFileOptions`). If CameraX's writer has a known issue on Xiaomi 13T (e.g., MIUI ContentResolver quirk), fall back to manual pattern — documented as a Phase 3 risk.
- **D-32:** MediaStore insert fields: `RELATIVE_PATH = "DCIM/Bugzz"`, `DISPLAY_NAME = "Bugzz_YYYYMMDD_HHmmss.jpg"`, `MIME_TYPE = "image/jpeg"`. No `WRITE_EXTERNAL_STORAGE` permission required (minSdk 28, API 29+ MediaStore covers both 28 via legacy-compat and 29+).
- **D-33:** `OverlayEffect` target `IMAGE_CAPTURE` was validated in Phase 2 (STATE #10). Phase 3 confirms: production filter is drawn into the IMAGE_CAPTURE buffer — `onDrawListener` fires with `OverlayEffect.Frame.targetType == CameraEffect.IMAGE_CAPTURE` during takePicture, bug sprite bakes into JPEG pixels. Handoff runbook: tap shutter, open Google Photos, zoom to bug position — bug visible baked in.
- **D-34:** Face state used for the IMAGE_CAPTURE draw: the **same** `AtomicReference<FaceSnapshot>` read by PREVIEW renderer — capture draw reads-latest, no synchronization difference. This means: if user moves head during capture, the bug position in JPEG may lag preview by one detector frame (~33ms). Acceptable tradeoff; documenting for Phase 7 review.
- **D-35:** Capture success callback (`ImageCapture.OnImageSavedCallback.onImageSaved`) fires on `cameraExecutor`; ViewModel receives via `Channel<OneShotEvent>` → UI collects and shows Toast. Error callback likewise emits `OneShotEvent.Error(message)`.
- **D-36:** CAP-06 leak verification: handoff runbook task = "Tap shutter 30 times consecutively; after each capture, confirm Toast fires; 10 seconds after 30th capture, kill + relaunch app; LeakCanary notification must be absent in notification drawer". No instrumented test (Robolectric + `ImageCapture` would be heavy to wire for Phase 3 — defer to Phase 7 if leak detected).

### REN-08 Performance (≥24fps on Xiaomi 13T)
- **D-37:** Phase 3 REN-08 validation = **subjective smoothness on device + optional Android Studio profiler trace** during handoff (user opens Android Studio → "Profile app" → captures 10-second CPU trace while filter is live). Formal measured-fps gate is Phase 7 (PRF-01 mapped there). Phase 3 acceptance: "no visible jank to naked eye under normal filter playback on Xiaomi 13T". If visible jank detected, raise a Phase 3 gap (follow Phase 2 gap-closure precedent) and investigate before Phase 4.
- **D-38:** Phase 3 DOES NOT ship an in-app FPS counter HUD (same as Phase 2 D-03). Profiler trace is the diagnostic tool of choice.

### Architecture Placement
- **D-39:** New Phase 3 files under `com.bugzz.filter.camera.`:
  - `render/FilterEngine.kt` — production filter renderer (@Singleton)
  - `render/BugBehavior.kt` — sealed interface + 4 variants (STATIC impl, 3 stubs)
  - `render/BugState.kt` — per-bug mutable state data class
  - `filter/FilterDefinition.kt` — immutable filter spec
  - `filter/FilterCatalog.kt` — Phase 3 stub holding 2 filters (Phase 4 expands to 15-25)
  - `filter/AssetLoader.kt` — sprite decode + `LruCache<String, Bitmap>` (@Singleton)
  - `detector/BboxIouTracker.kt` — ADR-01 tracker (@Singleton)
  - `capture/PhotoCaptureService.kt` — optional wrapper around ImageCapture + MediaStore (could stay as methods on CameraController; Claude decides)
  - Unit tests under `src/test/java/` for: `BboxIouTracker`, `FilterEngine.onDraw`, `AssetLoader`, `FaceLandmarkMapper.anchorPoint`, `FilterCatalog`
- **D-40:** `FilterEngine` + `AssetLoader` + `BboxIouTracker` injected via `CameraModule` (existing from Phase 2). `FaceDetectorClient` receives `BboxIouTracker` as `@Inject` constructor param. `OverlayEffectBuilder` receives `FilterEngine` in addition to `DebugOverlayRenderer`.

### Claude's Discretion
- Exact Kotlin coroutine / thread plumbing for `AssetLoader.preload()` (suspend fun on Dispatchers.Default vs explicit executor submission)
- Exact capture-flash animation implementation (Compose `AnimatedVisibility` + `Canvas` vs simple alpha layer)
- Whether `PhotoCaptureService` is a separate class or methods on `CameraController`
- Kotlin implementation of `ImageCapture.OnImageSavedCallback` wrapper (callbackFlow vs suspendCancellableCoroutine)
- LruCache size exact formula (as long as it's ≤ 32 MB and sane for 2-filter workload)
- Whether `Cycle Filter` button label is "Cycle" / "Filter" / an icon — default to `OutlinedButton { Text("Cycle") }` unless material-icons-extended is added (same fallback pattern as Phase 2 D-24)
- Shutter button ripple + pressed-state animation (default Material3 ripple)
- `FrameLoop` timing source: prefer `OverlayEffect.Frame.frameTimeNanos` (already passed into onDrawListener — zero allocation) over `Choreographer` (extra listener, redundant data). Deferred to plan.
- `FilterDefinition.mirrorable` semantics (whether flipping lens flips the sprite — probably yes for asymmetric bugs; Claude decides per-filter default)
- Whether `DebugOverlayRenderer` stays wired or gets a kill-switch toggle (keep wired + gated by BuildConfig.DEBUG — zero release footprint)
- `manifest.json` parsing: kotlinx.serialization vs Moshi vs hand-rolled JSONObject (kotlinx.serialization already pulled in by Phase 1 D-09 for nav — reuse)
- Exact bitmap decode dispatcher (cameraExecutor vs Dispatchers.IO) — prefer cameraExecutor for thread consolidation

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Project specs
- `.planning/PROJECT.md` — locked tech decisions (CameraX, ML Kit, Compose, Hilt, minSdk 28); reference APK location; English UI convention
- `.planning/REQUIREMENTS.md` §Filter Render Engine — REN-01..REN-08; §Photo Capture — CAP-01..CAP-06; §Performance — PRF-01, PRF-02 cross-referenced but mapped to Phase 7
- `.planning/ROADMAP.md` §Phase 3 — goal + 5 success criteria (REN-01..08 + CAP-01..06)
- `.planning/STATE.md` §Key Decisions Locked in Research (Canvas overlay, Canvas → GL escalation path Phase 7); §Active Todos (sprite extraction, device ADB); §Open Questions (DCIM/Bugzz vs Pictures/Bugzz default, mirror convention, 1€ tuning)

### Prior phases (read before planning)
- `.planning/phases/01-foundation-skeleton/01-CONTEXT.md` — package naming (`com.bugzz.filter.camera`), Hilt setup, navigation shell, permission pattern
- `.planning/phases/02-camera-preview-face-detection-coordinate-validation/02-CONTEXT.md` — D-01..D-28 (debug overlay contract, OverlayEffect/MlKitAnalyzer wiring, executor topology, OneEuroFilter defaults, ADR-01 cross-ref)
- `.planning/phases/02-camera-preview-face-detection-coordinate-validation/02-ADR-01-no-ml-kit-tracking-with-contour.md` — **MANDATORY**: Phase 3 implements all 4 Phase 3 follow-up items (BboxIouTracker, re-key LandmarkSmoother, thread tracker through createAnalyzer, update VERIFICATION)
- `.planning/phases/02-camera-preview-face-detection-coordinate-validation/02-VERIFICATION.md` — CAM-08 row needs update (relaxed boundingBox-centroid to tracker-ID stability) during Phase 3 handoff
- `.planning/phases/02-camera-preview-face-detection-coordinate-validation/02-HANDOFF.md` — Xiaomi 13T device runbook precedent; Phase 3 handoff follows same shape with added filter/shutter steps

### Research (read in full before planning)
- `.planning/research/STACK.md` — CameraX 1.6.0 + ML Kit 16.1.7 + Compose BOM already pinned; no new deps except possibly `kotlinx-serialization-json` if not already in catalog
- `.planning/research/ARCHITECTURE.md` §3 (rendering pipeline — OverlayEffect + Canvas), §5 (data flow for capture), §7 (build-order graph — Phase 3 depends on Phase 2 overlay contract), §12 (decisions summary)
- `.planning/research/PITFALLS.md` §1 (coord-space chaos — carried from Phase 2), §2 (video loses overlay — same OverlayEffect validated), §3 (landmark jitter — carried + tune empirically per STATE open question), §4 (ImageAnalysis backpressure — preserve STRATEGY_KEEP_ONLY_LATEST), §5 (canvas matrix applied before draw), §7 (device fragmentation — Xiaomi 13T / MIUI), §13 (multi-face contour — primary-only rendering confirmed in D-24)
- `.planning/research/SUMMARY.md` — Canvas overlay decision rationale (not Filament); upgrade path to GL `CameraEffect` in Phase 7 documented

### CameraX / ML Kit / MediaStore external docs
- [CameraX ImageCapture guide](https://developer.android.com/media/camera/camerax/take-photo) — `takePicture(outputOptions, executor, callback)` pattern, MediaStore output
- [CameraX 1.6 OverlayEffect + IMAGE_CAPTURE target](https://developer.android.com/reference/kotlin/androidx/camera/core/CameraEffect#IMAGE_CAPTURE) — confirms overlay bakes into JPEG
- [MediaStore scoped-storage Images API](https://developer.android.com/training/data-storage/shared/media) — `RELATIVE_PATH` + `IS_PENDING` transaction
- [Android LruCache](https://developer.android.com/reference/android/util/LruCache) — bitmap memory cache pattern + `sizeOf()` override
- [`Intent.ACTION_VIEW` with MediaStore URI](https://developer.android.com/training/sharing/send) — optional Snackbar "View" action
- [AssetManager.open / list](https://developer.android.com/reference/android/content/res/AssetManager) — asset reading pattern for `assets/sprites/`

### Reference APK (behavioral ground truth)
- `reference/com.insect.filters.funny.prank.bug.filter.face.camera.apk` — mandatory Wave 0 inspection target:
  1. `apktool d` to extract ant + spider sprite assets
  2. `adb install -r` on Xiaomi 13T + capture front-cam photo to establish CAP-04 mirror convention

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets (from Phase 2)
- `com.bugzz.filter.camera.detector.FaceDetectorClient` — receives a `BboxIouTracker` constructor param in Phase 3 (additive change); `createAnalyzer()` consumer body updated to call `tracker.assign(faces)` before mapping to `SmoothedFace`.
- `com.bugzz.filter.camera.detector.LandmarkSmoother` — `onFaceLost(id: Int)` method added (was keyed on `-1`); re-keyed on BboxIouTracker IDs.
- `com.bugzz.filter.camera.detector.FaceLandmarkMapper` — Phase 2 stub's enum (NOSE_TIP, FOREHEAD, LEFT_CHEEK, RIGHT_CHEEK, CHIN, LEFT_EYE, RIGHT_EYE) reused directly; `anchorPoint()` gets the production body (see D-30).
- `com.bugzz.filter.camera.detector.FaceSnapshot` — existing `AtomicReference<FaceSnapshot>` holder pattern reused for `FilterEngine` (Phase 2 D-19).
- `com.bugzz.filter.camera.render.OverlayEffectBuilder` — extended to inject `FilterEngine` in addition to `DebugOverlayRenderer`; `onDrawListener` body calls both renderers in order (FilterEngine first, DebugOverlayRenderer on top when enabled).
- `com.bugzz.filter.camera.render.DebugOverlayRenderer` — **not deleted**; gated behind `BuildConfig.DEBUG && prefs.debugOverlayEnabled` for regression-debug-on-demand.
- `com.bugzz.filter.camera.camera.CameraController` — `capturePhoto()` method added; binds `ImageCapture` to existing UseCaseGroup (already bound in Phase 2); overlay already baked in IMAGE_CAPTURE target.
- `com.bugzz.filter.camera.ui.camera.CameraViewModel` — `onShutterTapped()` method added; emits `OneShotEvent.PhotoSaved(uri)` on success + `OneShotEvent.Error(msg)` on failure via existing `Channel<OneShotEvent>`.
- `com.bugzz.filter.camera.ui.camera.CameraScreen` — shutter button composable added BottomCenter; Cycle Filter debug button added BottomEnd; TEST RECORD button moved to BottomStart; Toast collection on `OneShotEvent.PhotoSaved`.
- `com.bugzz.filter.camera.ui.camera.OneShotEvent` — new variant `PhotoSaved(uri: Uri)` + `Error(message: String)` already covers capture errors.
- `com.bugzz.filter.camera.di.CameraModule` — new `@Provides` entries for `FilterEngine`, `FilterCatalog`, `AssetLoader`, `BboxIouTracker` (or constructor-injected directly with `@Inject` — Claude decides per Hilt canonical pattern).

### Established Patterns (from Phase 1 + 2)
- **Hilt `@Singleton` + `@Inject` constructor split pattern** for test-substitutable factories (Phase 2 D-25 learned pattern — apply to any Phase 3 class with providerFactory seams)
- **Single-thread executors:** `cameraExecutor` (MlKit + ImageCapture + VideoRecord callbacks) + `renderExecutor` (OverlayEffect onDraw). Phase 3 asset decode goes on `cameraExecutor` (thread consolidation).
- **`AtomicReference` handoff** for cross-thread state (Phase 2 D-19) — Phase 3 uses this for `FilterEngine.activeFilter` (writes from onShutterTapped / setFilter, reads from onDraw).
- **KSP (not kapt)** for Hilt codegen (Phase 1 D-06) — continues for Phase 3.
- **Robolectric required** for any CameraX-construction-in-test-body cases (Phase 2 D-25 learned pattern) — may apply to `FilterEngineTest` if it constructs `ImageCapture` stubs; mostly avoidable by testing `FilterEngine.onDraw(canvas, frame, face)` in pure JVM unit tests.
- **`@Named` executors** (`cameraExecutor` / `renderExecutor`) — Phase 3 reuses both; no new named executors.
- **Compose `BuildConfig.DEBUG` gates** for debug-only UI (TEST RECORD from Phase 2; Cycle Filter new in Phase 3).
- **`FaceContour` primary-only contour population** (PITFALLS #13) — Phase 3 FilterEngine primary-face rendering matches this limitation.

### Integration Points
- `CameraController.bind()` already binds `ImageCapture` use case with `MirrorMode.MIRROR_MODE_ON_FRONT_ONLY` for video (Phase 2 STATE #10); verify IMAGE_CAPTURE needs analogous mirror handling (D-17, D-18).
- `UseCaseGroup` already has OverlayEffect attached to PREVIEW | IMAGE_CAPTURE | VIDEO_CAPTURE (Phase 2 validated) — **no CameraX rebind needed** for Phase 3; only additive: new renderer callbacks inside existing `onDrawListener`.
- `AndroidManifest.xml` needs no changes for Phase 3 (CAMERA permission already granted from Phase 1; no `WRITE_EXTERNAL_STORAGE` — MediaStore covers API 29+ scoped storage on minSdk 28).
- `libs.versions.toml` likely needs `kotlinx-serialization-json` entry if not already present (for `manifest.json` parsing — verify via grep at plan time).
- `gradle/libs.versions.toml` bump: confirm Compose BOM still at `2026.03.00` (Phase 2 D-02 auto-fix: 2026.04.00 not yet on Google Maven) — Phase 3 waits, not a blocker.

</code_context>

<specifics>
## Specific Ideas

- User will verify Phase 3 on **Xiaomi 13T** via USB ADB (locked — Phase 2 precedent). Same device, same runbook shape (`03-HANDOFF.md` analogous to `02-HANDOFF.md`).
- User preference: **stop-test per phase** (no multi-phase auto-chain execution). `--chain` on this invocation runs discuss → plan → execute for Phase 3 alone; user manually verifies on-device runbook before advancing to Phase 4.
- Phase 3 Wave 0 has **two distinct asset-prep tasks** that are prerequisites: (1) `apktool` extract ant + spider sprite assets from reference APK to `app/src/main/assets/sprites/`, (2) install reference APK on Xiaomi 13T + capture front-cam photo + document mirror convention in `03-RESEARCH.md`. Both are non-Kotlin tasks; neither should land in same commit as production code.
- Phase 3 is **the integration test** phase per ROADMAP Key Roadmap Decisions — if photo capture + overlay-bake-into-JPEG fails here, Phase 5 video pipeline inherits the failure. Three-stream compositing was architecturally validated in Phase 2 (debug red rect in 5s test MP4 via 02-gaps-03); Phase 3 validates with a production filter + a real shutter.
- User said mid-discussion: "Bạn tự chạy toàn bộ Phase 3 theo recommended hết đi" — remaining gray areas (multi-face cap = 2, smoother-clear-on-lose-ID, mirror = inspect-reference-then-match) locked to recommended defaults; logged in DISCUSSION-LOG.md as auto-selected.
- Xiaomi 13T / HyperOS — continue monitoring for MIUI-specific camera HAL quirks (PITFALLS #7); if MediaStore insert misbehaves or `ImageCapture.takePicture` shows OEM-specific rotation, document in `03-HANDOFF.md`, defer OEM compensation to Phase 7 cross-OEM matrix.

</specifics>

<deferred>
## Deferred Ideas

- **Filter catalog UI (LazyRow picker)** — Phase 4 (CAT-01..05). Phase 3's Cycle Filter is debug-only.
- **15-25 bug filters extract + bundle** — Phase 4 asset-prep. Phase 3 extracts only 2 (ant + spider).
- **CRAWL / SWARM / FALL behavior implementations** — Phase 4 (MOD-01/02 pull them in with the catalog).
- **Face Filter vs Insect Filter mode selector on Home screen** — Phase 4 MOD-01 (button) + Phase 5 MOD-03..07 (free-placement Insect Filter).
- **Production PreviewRoute with Save/Share/Delete/Retake** — Phase 6 (UX-04); Phase 3 stays on camera + Toast.
- **`MediaActionSound` shutter sound + global sound toggle in settings** — Phase 6 UX-09.
- **FPS counter HUD in-app** — deferred; Phase 7 uses Android Studio profiler (same policy as Phase 2 D-03).
- **Formal measured ≥24fps validation (PRF-01)** — Phase 7 (PRF-01..05 mapped there). Phase 3 uses subjective + optional profiler trace.
- **Instrumented `CAP-06` 30-capture leak test** — deferred to Phase 7 if manual handoff runbook detects leaks.
- **Video recording production UX (60s cap, record button, RECORD_AUDIO lazy prompt, thermal listener)** — Phase 5 (VID-01..10).
- **Filter thumbnail images for picker** — Phase 4 (each FilterDefinition gains a `thumbnailPath: String`; generated via first-frame-of-flipbook crop or separate thumbnail asset).
- **`DataStore` last-used-filter persistence** — Phase 4 (CAT-05). Phase 3 always starts with filter A (ant-on-nose) on app launch.
- **MIUI / OEM-specific ImageCapture workarounds** — document findings in `03-HANDOFF.md`, compensation in Phase 7.
- **Custom GL `CameraEffect` escalation (Canvas → GL)** — Phase 7 if Canvas profiles below 24fps under full catalog. Phase 3 deliberately keeps Canvas.
- **BugBehavior per-frame parameterization via `FilterDefinition` (e.g., STATIC offset, CRAWL speed)** — Phase 4; Phase 3 ships STATIC with hardcoded defaults inside `StaticBehavior` class.
- **`FilterDefinition.mirrorable` per-filter lens-flip behavior refinement** — Phase 4 (default all-true Phase 3; Phase 4 audits per-filter).
- **Bitmap pool / sharing across filters** — Phase 7 perf optimization if LruCache eviction thrashes on 25-filter catalog; Phase 3 ships simple LruCache.
- **Capture timestamp EXIF tags (datetime, rotation, location)** — CameraX default writes most; explicit audit deferred to Phase 6 UX polish.

</deferred>

---

*Phase: 03-first-filter-end-to-end-photo-capture*
*Context gathered: 2026-04-19*
