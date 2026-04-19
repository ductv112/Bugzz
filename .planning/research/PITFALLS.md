# Pitfalls Research

**Domain:** Android AR face-filter camera app (CameraX + ML Kit + overlay compositing + capture + video recording)
**Researched:** 2026-04-18
**Confidence:** HIGH — based on official CameraX/ML Kit docs, Google CameraX developer group threads, issue trackers, and direct failure modes known from production AR apps. Video-overlay pitfall is verified via CameraX 1.4 OverlayEffect release notes.

---

## Executive Note for the Roadmap

Two pitfalls kill 90% of hobby AR-filter projects in this space:

1. **Coordinate space mismatch** — face detected in ImageAnalysis coords, drawn in View/Canvas coords, preview cropped by PreviewView scale type, front camera horizontally mirrored. Bugs end up rendered on the wrong side of the face, offset by tens of pixels, or glued to a fixed screen point instead of tracking the nose. Every solo dev re-derives the wrong transform at least once.
2. **Video recording that drops the overlay** — CameraX Recorder records the raw camera stream, not what is painted on your overlay View. Naively wiring `VideoCapture` produces clean video with zero bugs. Fix requires either (a) CameraX 1.4+ `OverlayEffect` bound to both `PREVIEW | VIDEO_CAPTURE`, or (b) GL-based compositor pipeline writing into a MediaRecorder `Surface`. Picking the wrong path here forces a Phase 5 rewrite.

The phase plan must front-load (1) in Phase 2 and lock the architectural decision for (2) at the end of Phase 2 — before Phase 3 renders anything — so Phase 5 isn't a rewrite of Phase 3.

---

## Critical Pitfalls

### Pitfall 1: Coordinate space chaos (PreviewView vs ImageAnalysis vs Canvas/GL)

**What goes wrong:**
ML Kit returns face bounding box, contours, and landmarks in the **analyzer image's pixel coordinates** (rotated sensor frame, typically 640×480 or 1280×720 YUV). Your overlay is drawn on a `View`/`Canvas` whose coordinates are **PreviewView coordinates** (e.g., 1080×2400 display pixels). These two spaces differ by:

- Scale (image is smaller than screen)
- Aspect ratio (image is 4:3 or 16:9, screen is ~19.5:9 — so PreviewView crops or letterboxes)
- Rotation (`imageProxy.imageInfo.rotationDegrees` is 0/90/180/270 depending on device orientation)
- **Front camera mirror** (PreviewView visually mirrors the front camera preview but ML Kit coords are NOT mirrored — draw a bug on "left eye" and it appears on the right side of the screen)

Concrete symptom: bug sprite tracks the face but is offset 200px right and lags during device rotation; when switching from back to front camera, the bug jumps to the wrong half of the face.

**Why it happens:**
Developers read `face.boundingBox` and pass it directly to `canvas.drawRect()` without transformation. Or they roll a custom transform using `image.width`/`image.height` and `view.width`/`view.height` but forget scale type cropping and front-mirror. Blog examples are usually buggy — they work on one aspect ratio and break on another.

**How to avoid:**
- **Use `camera-mlkit-vision` (`MlKitAnalyzer`) with `COORDINATE_SYSTEM_VIEW_REFERENCED`** — this is the *only* supported, device-tested path. CameraX does the transform for you; results arrive pre-mapped to PreviewView coordinates.
- If you need coords in a custom overlay (e.g., a `GLSurfaceView` sibling of `PreviewView`), still let `MlKitAnalyzer` hand you view-referenced points, then apply your overlay's own view→local transform — do not try to compose the transform from sensor space yourself.
- For front camera, explicitly set the overlay to mirror horizontally (`scaleX = -1f` or flip in GL). Do **not** try to unmirror ML Kit coords — by VIEW_REFERENCED contract they're already in the mirrored preview's space.
- Write a small "coordinate validator" debug overlay in Phase 2: draw the raw `face.boundingBox` as a red rect on top of preview. If the red rect doesn't wrap the face on rotate, front/back swap, and both portrait/landscape → your transform is wrong. Do not proceed to Phase 3.

**Warning signs:**
- Bug sprite lags behind face on fast head motion (transform applied, but at wrong scale).
- Bug sprite appears at a fixed screen location regardless of head position.
- Switching front↔back camera moves bug to wrong side.
- Rotating device 90° offsets bug by a large amount.
- "Works on my Pixel but not on Samsung/Xiaomi" — different default preview aspect ratios.

**Phase to address:**
**Phase 2 (camera/detection).** Transform validation is a **Phase 2 exit criterion**. A debug overlay showing `face.boundingBox` pixel-perfect on the user's face in both orientations and both cameras must ship before Phase 3 starts.

---

### Pitfall 2: Video recording loses the overlay

**What goes wrong:**
Developer wires `Preview` + `ImageAnalysis` (for face detection) + `VideoCapture` use cases. Preview shows bugs tracking the face perfectly. User taps record. The saved `.mp4` contains the raw camera stream — **no bugs, no overlay, nothing**. The filter appears only on-screen during preview, not in the output file.

**Why it happens:**
`VideoCapture`/`Recorder` is wired directly to the camera sensor stream. The overlay `View` is a separate Android View drawn over `PreviewView` — it is never part of the pixel pipeline going into the encoder. This is the #1 "looks done but isn't" issue in face-filter apps.

**How to avoid:**
Pick one of two correct approaches and commit at end of Phase 2:

**Approach A — CameraX `OverlayEffect` (recommended for this project):**
- Requires `androidx.camera:camera-effects:1.4.0+`.
- Create an `OverlayEffect(CameraEffect.PREVIEW or CameraEffect.VIDEO_CAPTURE, queueDepth, mainExecutor, onDrawListener)`.
- Inside `onDrawListener`, get `Frame.getOverlayCanvas()` and `Frame.getSensorToBufferTransform()`. Draw bugs using Canvas API with that transform matrix pre-applied.
- Attach via `cameraProvider.bindToLifecycle(..., UseCaseGroup.Builder().addEffect(overlayEffect)...)`.
- Overlay now burns into both preview and recorded video through a single draw call.

**Approach B — Custom GL compositor + MediaRecorder Surface (heavier, full control):**
- Render camera SurfaceTexture + bug sprites through OpenGL ES shaders.
- Present GL output to both `GLSurfaceView` (preview) and `MediaRecorder.getSurface()` (encoder input) via `EGL14.eglCreateWindowSurface` dual-surface pattern.
- More code; only use if OverlayEffect can't express the effect (e.g., custom shaders).

**Anti-patterns — do NOT do these:**
- Taking a screen recording of PreviewView + overlay (audio won't sync, resolution is screen-bound, huge bitrate).
- Calling `View.draw(Canvas)` on the overlay and trying to muxer-composite it with recorded video post-hoc (coord drift, frame count mismatch, hours of debugging).
- Using `PixelCopy` per frame from PreviewView into a custom encoder (GPU stall, dropped frames).

**Warning signs:**
- First video test in Phase 5 shows clean face with no filter → you picked the wrong architecture.
- "Let me just screen-record" as a workaround → you picked the wrong architecture.
- Overlay in preview but recording requires its own renderer → you picked the wrong architecture.

**Phase to address:**
**End of Phase 2 / start of Phase 3.** Pick `OverlayEffect` vs custom GL before drawing any production sprite. Verify in Phase 3 by recording a test `.mp4` with a dummy red rectangle overlay and confirming the red rect is in the saved file. Do not let this slip to Phase 5.

---

### Pitfall 3: Landmark jitter → bugs flicker/shake

**What goes wrong:**
ML Kit detects face contour/landmarks per frame independently. Positions jitter ±2–5 pixels even when face is static. Bugs pinned to these raw landmarks shake visibly, ruining the "wow" factor — especially on small sprites attached to nose, eye corners, lips.

**Why it happens:**
- Detection is a per-frame probabilistic model, not a tracker.
- FAST mode + low-res input amplifies noise.
- Head micro-motion + sensor noise + model quantization → landmark positions oscillate.
- Enabling contour + landmarks + classification together degrades accuracy further.

**How to avoid:**
- **Apply a 1€ (One-Euro) filter** per-landmark (x and y independently). 1€ is designed for exactly this: low-pass at rest (kills jitter), adaptive cutoff on motion (minimal lag during real head motion). Simple to implement (~30 lines Kotlin per filter instance), no matrix math.
- Parameters to tune: `minCutoff ≈ 1.0`, `beta ≈ 0.007`, `dCutoff ≈ 1.0`. Lower `minCutoff` = smoother but laggier; higher `beta` = more aggressive unblocking on motion.
- **Do NOT** use Kalman filter unless you already know it — 1€ is strictly simpler for this use case and widely adopted in MediaPipe/face-tracking pipelines.
- Simple EMA (`pos = α·new + (1−α)·old`) is the cheap fallback; works but trades lag for smoothness at a fixed ratio. Acceptable for Phase 3 prototype, replace with 1€ before shipping.
- Use **`setMinFaceSize(0.15f)`** (or 0.2f) — very small faces have noisier landmarks; filter them out.
- **Do NOT enable tracking (`.enableTracking()`) when `CONTOUR_MODE_ALL` is active** — Google ML Kit silently ignores the call at runtime; `face.trackingId` is always null, and `FaceDetectorOptions.isTrackingEnabled` reflective-reports `true` so unit tests miss the drift. Verified on Xiaomi 13T / HyperOS (Bugzz project GAP-02-A, 459/459 null trackingIds in 20s, 2026-04-19).
- **If you need stable per-face identity with contour detection:** implement a boundingBox-IoU tracker (spatial centroid-overlap between consecutive frames assigns a monotonic local ID). This is what MediaPipe does internally. Budget ~100 LOC Kotlin + unit tests. Bugzz defers this to Phase 3 per `02-ADR-01-no-ml-kit-tracking-with-contour.md`.
- **If your app does not need contour points** (you can anchor off bounding-box + coarse landmarks only): use `LANDMARK_MODE_ALL` + `.enableTracking()` instead. This path preserves ML Kit trackingId but loses the 100+ contour points needed for Phase 4 CRAWL behavior along face edges.

**Warning signs:**
- Filter looks "good in screenshots, bad in motion."
- Bugs visibly buzz/shake when face is still.
- Bugs feel "laggy" on head turn (cutoff too low).
- Occasional large jumps when second face enters/leaves frame (tracking IDs not stable → losing filter state).
- trackingId always `null` in logcat despite `.enableTracking()` being wired — you hit the contour/tracking exclusivity; see above fix.

**Phase to address:**
**Phase 3 (rendering).** Land raw-landmark overlay first to verify coords, then insert 1€ filter as a middleware layer between detector callback and renderer. Revisit parameters in Phase 6 (polish) with real device testing.

---

### Pitfall 4: ImageAnalysis backpressure → UI stall or dropped detections

**What goes wrong:**
With default `STRATEGY_BLOCK_PRODUCER`, CameraX queues frames while ML Kit processes. If ML Kit is slower than preview FPS (common: ~50ms/frame contour detection on mid-range devices = 20Hz max), the queue fills, camera stalls, preview stutters, and input lag climbs.

With naive coding, developers also do the mistake of calling `detector.process()` asynchronously but closing `ImageProxy` immediately → `"Image already closed"` exception and silent detection failures.

**Why it happens:**
- Default backpressure blocks camera producer.
- Contour mode is heavier than bounding-box only.
- Dev doesn't realize `ImageProxy.close()` must happen only after `detector.process()` task completes.

**How to avoid:**
- Set `ImageAnalysis.Builder().setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)` — newer frames replace older, camera never stalls. Detection runs at whatever rate ML Kit can sustain; preview stays smooth.
- Close `ImageProxy` only inside `.addOnCompleteListener {}` of the `detector.process()` Task — not before. Use try/finally pattern.
- Better: use `MlKitAnalyzer` from `camera-mlkit-vision`, which handles both close timing and coordinate transform for you. Strongly recommended for this project.
- Set analyzer resolution via `setTargetResolution(Size(640, 480))` or `setResolutionSelector` — 720p input is the sweet spot for contour detection (faces ≥ 200px as ML Kit requires) without overloading CPU.

**Warning signs:**
- Preview feels "sticky" or low FPS while face detection enabled but smooth with it off.
- Logcat spam: `"Image already closed"` / `IllegalStateException` from ML Kit.
- Increasing latency between head motion and bug position (queue growing).
- Janky preview on older devices only (thermal/CPU limit + wrong strategy).

**Phase to address:**
**Phase 2 (camera/detection).** Strategy choice and `ImageProxy` close timing are non-negotiable setup decisions. Use `MlKitAnalyzer` from day one to dodge both.

---

### Pitfall 5: CameraX + ML Kit version/dependency thrash

**What goes wrong:**
`camera-mlkit-vision`'s `MlKitAnalyzer` is tied to specific CameraX versions. Updating only one of `camera-core`, `camera-camera2`, `camera-lifecycle`, `camera-view`, `camera-mlkit-vision`, `camera-effects`, `camera-video` produces runtime `NoSuchMethodError` or silent feature degradation (e.g., OverlayEffect not drawing).

Additionally, ML Kit bundled (`face-detection`) vs unbundled (`face-detection-playservices`) behaves differently on Model-download gating and first-launch latency.

**How to avoid:**
- Pin all `androidx.camera:*` artifacts to the **same** version (e.g., all 1.4.x) using a BoM or shared version variable.
- Use **bundled** `com.google.mlkit:face-detection` so model is shipped in APK — first launch works offline, no Play Services model download race. Cost: ~3–4 MB APK increase (acceptable; project already accepts 67MB reference).
- Do not mix `com.google.android.gms:play-services-mlkit-*` with `com.google.mlkit:*` — pick one family.
- Lock versions in `libs.versions.toml`.

**Warning signs:**
- Works on one machine/CI, fails on another (cached artifacts).
- Feature works in Android Studio run, fails in release build (R8 stripping wrong classes).
- Face detection returns empty on first launch but works after 30 seconds (unbundled model downloading).

**Phase to address:**
**Phase 1 (setup).** Version catalog + BoM + bundled model decision is part of the initial gradle setup.

---

### Pitfall 6: Scoped storage save silently fails or buries files

**What goes wrong:**
Developer saves video via `File("/sdcard/DCIM/Bugzz/out.mp4")` → `FileNotFoundException` on Android 10+. Or uses MediaStore but forgets `IS_PENDING=1` during write → file appears in gallery mid-write, half-corrupt, users see black thumbnails. Or uses wrong `RELATIVE_PATH` so video lands in `Pictures/` instead of `DCIM/Bugzz/` and the share UX is broken.

**How to avoid:**
- Never use raw `/sdcard/...` paths for output.
- MediaStore insert pattern:
  1. `ContentValues` with `DISPLAY_NAME`, `MIME_TYPE` (`video/mp4` or `image/jpeg`), `RELATIVE_PATH` (`Environment.DIRECTORY_DCIM + "/Bugzz"` for videos OR `Environment.DIRECTORY_PICTURES + "/Bugzz"` for photos — check reference app convention), `IS_PENDING=1`.
  2. `resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)` → returns `uri`.
  3. Write bytes via `resolver.openOutputStream(uri)`.
  4. On success: `values.put(IS_PENDING, 0)` then `resolver.update(uri, values, null, null)`.
  5. On failure: `resolver.delete(uri, ...)` to remove the pending row.
- Photos go to `MediaStore.Images.Media.EXTERNAL_CONTENT_URI`; videos to `MediaStore.Video.Media.EXTERNAL_CONTENT_URI`. Don't mix.
- Use `DCIM/Bugzz/` (per reference app — DCIM surfaces in Google Photos' Camera roll section). Verify by inspecting reference APK's save path.
- On Android 13+, you don't need `WRITE_EXTERNAL_STORAGE` for saving to MediaStore collections you own. Request `READ_MEDIA_IMAGES`/`READ_MEDIA_VIDEO` only if you need to **read** other apps' media (likely not needed for this project — app only reads its own outputs).
- On Android 14+, consider partial-photo access if re-reading gallery.

**Warning signs:**
- Saved file opens corrupt in gallery.
- `FileNotFoundException` on device but not emulator.
- File lands in app-private directory (`Android/data/<pkg>/`) and disappears on uninstall — that means you used `context.getExternalFilesDir()` instead of MediaStore. Not user-visible media.
- Gallery shows zero-byte thumbnail (forgot to clear IS_PENDING).

**Phase to address:**
**Phase 4 (photo capture)** — introduce MediaStore helper class. Reuse unchanged in **Phase 5 (video capture)**.

---

### Pitfall 7: Device fragmentation — front camera mirror/rotation quirks

**What goes wrong:**
Front camera preview shows correctly on Pixel but stretches on Samsung S22, mirrors twice on some Xiaomi MIUI, or records un-mirrored video on certain OEMs despite a mirrored preview. Landmarks that "worked on my device" are off by 180° on another.

**Why it happens:**
OEMs tweak camera HAL; sensor orientation and mirroring defaults vary. CameraX ships **Quirks** that paper over ~30 known device issues, but the Quirks database only helps if you stay on supported patterns (PreviewView + CameraX use cases; not Camera2 direct).

**How to avoid:**
- Use `PreviewView` + CameraX use cases exclusively. Do not drop to Camera2 for "more control" — you lose Quirks compensation.
- For front camera, use `CameraSelector.DEFAULT_FRONT_CAMERA`; CameraX auto-mirrors preview. For VideoCapture mirroring, explicitly set `.setMirrorMode(MirrorMode.MIRROR_MODE_ON_FRONT_ONLY)` on the VideoCapture builder — match user expectation (recorded video looks like mirror preview).
- Test matrix minimum: **one Pixel + one Samsung + one Xiaomi/Oppo/OnePlus**. Do not ship without cross-OEM check — solo dev blind spot #1.
- Keep `PreviewView.ScaleType = FILL_CENTER` (default) only if you also handle cropping in coord transform. For face filters, `FIT_CENTER` (letterboxed) is simpler to reason about; black bars are acceptable UX tradeoff in Phase 2, revisit in Phase 6 if needed.
- Pin CameraX version to a stable release, not `-alpha` — Quirks are most comprehensive on stable.

**Warning signs:**
- Bug sprite is correctly positioned on your device but visibly offset on a friend's phone.
- Recorded video is un-mirrored while preview is mirrored (or vice versa).
- Preview is stretched/squished on specific OEMs.

**Phase to address:**
**Phase 2 (camera/detection)** — establish FILL vs FIT decision. **Phase 6 (polish)** — cross-device test matrix; don't defer to "later."

---

### Pitfall 8: Bitmap leaks in capture/share path

**What goes wrong:**
Photo capture takes high-res JPEG, decodes to Bitmap for overlay compositing, shares to another app — and leaks the bitmap. Stack multiple captures → `OutOfMemoryError`. Or loads a 12MP image into a thumbnail ImageView without downsampling → 48MB Bitmap allocation per view.

**How to avoid:**
- Prefer the `OverlayEffect` path so photo capture output already contains bugs burned in — no separate compositing Bitmap needed.
- If compositing manually: use `BitmapFactory.Options.inSampleSize` to downsample to display resolution before loading; pass through `Bitmap.Config.ARGB_8888` only when alpha is needed, otherwise `RGB_565` for previews.
- Always call `bitmap.recycle()` in `finally` block after use if you created it (only if you're certain no View is still rendering it).
- Use `use { }` pattern on `InputStream` / `OutputStream` — leaks of file descriptors also trigger OOM symptoms.
- For preview thumbnails after capture, use Glide or Coil (`Coil` is Kotlin-idiomatic, 2MB APK cost) with explicit `override(w, h)` — never `SIZE_ORIGINAL`.
- In ImageAnalysis: the `ImageProxy`/`media.Image` is **already a native buffer**, not a Bitmap. Do not decode it to Bitmap per-frame — that allocates on every frame and triggers GC pauses that manifest as preview stutter. Pass `InputImage.fromMediaImage(imageProxy.image, rotation)` directly.

**Warning signs:**
- App crashes with OOM after ~20–30 captures.
- Preview gets janky over time but is smooth at start (GC churn from per-frame Bitmap decode).
- Memory profiler shows `byte[]` allocation climbing during preview without capture.

**Phase to address:**
**Phase 4 (photo capture)** — bitmap discipline written once, reused. **Ongoing** — memory profiler check at end of each phase.

---

### Pitfall 9: Camera not released on lifecycle → "Camera in use" / leak

**What goes wrong:**
User navigates away from camera screen (home button, phone call, deep link to another activity) and back. Camera fails to initialize: "Camera is in use by another process." Or Activity is leaked — LeakCanary flags `PreviewView`/`CameraXActivity` retained because an unbinding race kept `SurfaceRequest` alive.

**Why it happens:**
- Manual `onResume/onPause` management competing with CameraX's own lifecycle binding.
- Binding/unbinding UseCases too frequently (on every navigation event).
- PreviewView receiving new SurfaceRequest before old one completed.
- Fragment binding to `fragment.lifecycle` instead of `viewLifecycleOwner`.

**How to avoid:**
- Call `cameraProvider.bindToLifecycle(lifecycleOwner, ...)` **once** in `onCreate` (Activity) or in `viewLifecycleOwner` block (Fragment). Do not manually `unbindAll()` in `onPause`.
- For Fragment-based camera screen: use `viewLifecycleOwner`, not `this` or `fragment.lifecycle`.
- If using Compose: use the CameraX Compose `CameraXViewfinder` (from 1.4+) which handles lifecycle correctly.
- Don't rebind UseCases on configuration changes unless necessary — CameraX handles re-binding on lifecycle state change automatically.
- LeakCanary in debug builds from Phase 1 — catches retained Activities early.

**Warning signs:**
- "Camera in use" toast/exception on second entry to camera screen.
- LeakCanary notification: `CameraActivity instance retained`.
- Black preview on resume from background.
- Audio recording hangs open (if using VideoCapture, audio source not released).

**Phase to address:**
**Phase 2 (camera/detection)** — lifecycle binding done right from first commit. Revisit at **Phase 5 (video)** since VideoCapture adds audio lifecycle.

---

### Pitfall 10: ANR — heavy work on main thread

**What goes wrong:**
Photo capture callback decodes + composites bitmap on main thread → 500ms freeze → ANR if user taps again. Or: face landmark callback runs smoothing, sprite layout, and state mutation on main thread at 30Hz → cumulative lag.

**How to avoid:**
- ImageAnalysis analyzer: already runs on the executor you provide. Provide a single-thread `Executors.newSingleThreadExecutor()` — not main thread, not Dispatchers.Default (uncontrolled parallelism on ImageProxy closing).
- Image/video save → `Dispatchers.IO` coroutine.
- Overlay draw (Canvas/GL) → dedicated render thread (`GLSurfaceView` handles this; OverlayEffect runs on `Executor` you pass).
- Sprite animation state update → tie to draw callback, not `Handler.post` on main.
- Wrap ML Kit detection in `withContext(Dispatchers.Default)` only if you're post-processing; the `detector.process()` call itself is async and safe to invoke from main.
- Use StrictMode in debug builds to catch disk/network on main.

**Warning signs:**
- "Application not responding" dialog during capture/save.
- Preview freezes for a moment when filter changes.
- StrictMode violations in Logcat.

**Phase to address:**
**Phase 1 (setup)** — StrictMode debug config. **Ongoing** — every new feature reviews its thread boundary.

---

### Pitfall 11: Thermal throttling on long video recording

**What goes wrong:**
3-minute video records fine. 8-minute video: FPS drops from 30 to 15, bugs start lagging, encoder can't keep up, recording may cut short or degrade quality silently. Phone gets hot, user experience degrades.

**Why it happens:**
Sustained camera preview + ML Kit face detection + OpenGL compositing + H.264 encoder all running simultaneously saturates SoC. GPU clock can drop ~40% above 42°C. Android 10+ `PowerManager.Thermal` API reports throttling states but most apps ignore them.

**How to avoid:**
- Use `PowerManager.addThermalStatusListener` (Android 11+). On `THERMAL_STATUS_MODERATE` or higher: reduce ML Kit detection rate (e.g., skip every other frame), lower preview resolution, or show a "device is warm" toast if very hot.
- Cap video recording duration at something reasonable (reference app check; 60–90s is common for "prank" clips). Aligns with user expectation anyway — no one watches a 10-min bug filter.
- Drop to FAST mode for face detection during video record (already slower with encoder running). Contour-heavy processing during record is overkill.
- Ensure ML Kit frames are not double-processed (one for preview display, one for video) — `OverlayEffect` shares the draw, don't duplicate detection streams.
- Set video bitrate reasonably (`Quality.HD` ~5Mbps; `Quality.FHD` ~10Mbps; avoid 4K — both thermal and file-size problem for a prank app).

**Warning signs:**
- Phone palpably hot during record.
- FPS counter drops after 3–5 minutes.
- Battery % drops >5%/min during record (normal: 1–2%/min).
- Encoder errors in Logcat after sustained record.

**Phase to address:**
**Phase 5 (video recording).** Implement thermal listener and test a 5-minute record. Tune in **Phase 6**.

---

### Pitfall 12: APK size bloat from bug sprite assets

**What goes wrong:**
Multiple bugs (ants, roaches, spiders, worms) × multiple animation frames × multiple density buckets = hundreds of PNGs in `drawable-mdpi/hdpi/xhdpi/xxhdpi/xxxhdpi`. Each transparent PNG animation (30 frames × 512px × 6 densities × 10 bugs) trivially exceeds 50MB of assets. Reference APK is already 67MB partly for this reason.

**How to avoid:**
- Ship a **single xxhdpi** (or xxxhdpi if sprites appear large) density per sprite in `drawable-xxhdpi/` — Android upscales/downscales acceptably for decorative sprites. Do not provide every bucket.
- Better: put sprites in `drawable-nodpi/` with explicit sizing in code (`Bitmap.createScaledBitmap` to face size). Android won't auto-scale these based on device density, saving ~5× storage.
- **WebP lossless** for sprite frames with alpha — ~25–34% smaller than PNG at visually identical quality. Android Studio's right-click → "Convert to WebP" handles the conversion. Supported since API 17, no compatibility concern at minSdk 28.
- For multi-frame animations, prefer **animated WebP** (single file) over `AnimationDrawable` (many files + XML). Decoded via `ImageDecoder` (API 28+ — matches our minSdk).
- Alternative: bug sprite sheets (single atlas PNG/WebP, render with source rects) — 1 file, uniform compression, faster decode.
- Run Android Studio **APK Analyzer** at the end of each phase to watch size growth. Set a soft budget: APK ≤ 40MB (our reference is 67MB including ads/billing SDKs we're deferring, so target < 40MB without ads).

**Warning signs:**
- APK grows >5MB per bug added.
- `res/drawable-*` is >80% of APK.
- Asset decode takes noticeable time on filter-switch.

**Phase to address:**
**Phase 3 (rendering)** — establish sprite format (WebP sheet or animated WebP, single density) when first bug is integrated. Once set, all subsequent bugs follow the template.

---

### Pitfall 13: ML Kit "multiple faces but only one contour populated" bug

**What goes wrong:**
App detects 2 faces correctly (2 `Face` objects in the result list). Bugs render on face #0. Face #1 shows bounding box but contour is empty → bugs that need contour (e.g., "bug crawls along jawline") silently disappear on non-primary faces.

**Why it happens:**
Known ML Kit behavior: in contour mode, contours are populated for the **largest/most prominent** face only. Documentation says 5 faces, reality is often 1.

**How to avoid:**
- **Design filters that don't depend on contour for non-primary faces** — use `boundingBox` center + a few landmarks (nose, eyes) for secondary faces. These ARE populated for all detected faces.
- Or: declare MVP to single-face only. Prank app on selfie cam is usually one person anyway. Reference app should be inspected to confirm.
- If multi-face contour is required, drop to `PERFORMANCE_MODE_ACCURATE` + smaller face count limit (but frame rate drops).

**Warning signs:**
- "Filter works on self but disappears when a second person enters frame."
- Empty `face.getContour(...)` result on secondary faces.

**Phase to address:**
**Phase 2 (camera/detection)** — decide single vs multi-face policy up front. **Phase 3 (rendering)** — verify sprite rendering path handles empty contours gracefully (fallback to bounding box).

---

### Pitfall 14: Permission flow fails on Android 13+

**What goes wrong:**
App requests CAMERA + RECORD_AUDIO only. Works on Android 12. On Android 13+ user never gets a notification permission prompt — any "share completed" / "save success" notification silently no-ops. Or dev blindly requests `WRITE_EXTERNAL_STORAGE` which is a no-op on Android 10+ and flagged by store reviewers.

**How to avoid:**
- Runtime permissions needed for this app:
  - `CAMERA` — always.
  - `RECORD_AUDIO` — only when video recording starts (request at that moment, not app start).
  - `POST_NOTIFICATIONS` (Android 13+) — only if you show notifications. For this app, MVP probably doesn't need notifications at all → omit permission entirely. Reference manifest lists it; verify if it's actually used by the reference app before matching.
- `WRITE_EXTERNAL_STORAGE` in manifest: **do not** use (no-op Android 10+; save via MediaStore). Reference manifest has it for legacy — our minSdk 28 means we only need `maxSdkVersion="28"` qualifier if we want it to only apply on Android 9 edge cases. Cleaner: drop it entirely since MediaStore works from API 29.
- `READ_MEDIA_IMAGES` / `READ_MEDIA_VIDEO`: only if we let the user browse past captures. If MVP share path is "share immediately after capture" and we keep a URI reference, we don't need read-media perms.
- Use `ActivityResultContracts.RequestMultiplePermissions` — clean, Activity-Result-API-compliant pattern. Don't use the legacy `onRequestPermissionsResult`.
- Handle "denied + don't ask again" by showing a settings-intent rationale. Don't trap user in permission loop.

**Warning signs:**
- Notification never shows on Android 13 device (silently blocked).
- Save success toast shows but notification doesn't.
- Permission dialog not appearing on first request (already permanently denied).

**Phase to address:**
**Phase 1 (setup)** — manifest audit. **Phase 5 (video capture)** — audio permission prompt. **Phase 6 (polish)** — permission UX review.

---

## Technical Debt Patterns

Shortcuts that seem reasonable but create long-term problems.

| Shortcut | Immediate Benefit | Long-term Cost | When Acceptable |
|----------|-------------------|----------------|-----------------|
| Render overlay as separate Android View on top of PreviewView | Fast to prototype in Phase 3 | Cannot record overlay into video → Phase 5 rewrite | **Never** past Phase 3. Use OverlayEffect from day 1. |
| Hardcode one device's preview aspect ratio in coord transform | Works on dev's phone | Breaks on every other phone | Never. Must use MlKitAnalyzer or equivalent. |
| Skip 1€ filter / use simple EMA with α=0.5 | 5 lines of code | Visible lag OR visible jitter — one or the other | OK for Phase 3 prototype; replace before Phase 6 polish. |
| Save to `/sdcard/DCIM/Bugzz/` via raw File | Works on emulator + Android 9 | Crashes on Android 10+ device | Never at minSdk 28. |
| Decode ImageProxy → Bitmap → process | Easy to reason about pixels | Per-frame GC pressure, dropped frames | Never in the per-frame hot path. Use InputImage.fromMediaImage. |
| Ship PNG sprites at 3 densities | Fast to add bugs | APK bloat | Never at our scale. Single xxhdpi WebP. |
| Test only on Pixel emulator | Fast iteration | Ships broken on Samsung/Xiaomi front cam | Emulator for logic only. Real device mandatory for camera/ML. |
| Manual `unbindAll()` + rebind on every navigation | Feels "clean" and explicit | Race conditions, camera-in-use errors, leaks | Never. Rely on lifecycle binding. |
| Use `GlobalScope.launch` for save | Works immediately | Leaks, no cancellation on screen exit | Never. Use `viewModelScope` or `lifecycleScope`. |

---

## Integration Gotchas

| Integration | Common Mistake | Correct Approach |
|-------------|----------------|------------------|
| ML Kit face detector + CameraX ImageAnalysis | Close ImageProxy before detector.process() completes → "Image already closed" | Close inside `.addOnCompleteListener {}`, or use `MlKitAnalyzer` from `camera-mlkit-vision`. |
| PreviewView coords + ML Kit analyzer coords | Manual transform math that forgets front-mirror or scale-type crop | Use `MlKitAnalyzer(COORDINATE_SYSTEM_VIEW_REFERENCED)` — done for you. |
| MediaStore + ContentResolver | Forgetting IS_PENDING flag → half-written files appear in gallery | Set IS_PENDING=1 before write, =0 after, delete row on failure. |
| VideoCapture + OverlayEffect | Binding only to PREVIEW target, overlay absent in video | Bind effect to `CameraEffect.PREVIEW or CameraEffect.VIDEO_CAPTURE`. |
| VideoCapture + audio | Forgetting `withAudioEnabled()` on PendingRecording → silent video | Explicit `pendingRecording.withAudioEnabled().start(...)` after RECORD_AUDIO granted. |
| Share intent | Passing `file://` URI → `FileUriExposedException` on Android 7+ | Use MediaStore `content://` URI directly (already have it from insert), or FileProvider for app-private. |
| Gradle dependencies | Mixing `com.google.mlkit` and `com.google.android.gms:play-services-mlkit-*` | Pick one family (recommend bundled `com.google.mlkit:face-detection`). |
| Front camera + VideoCapture | Preview mirrored but recorded video un-mirrored | `VideoCapture.Builder().setMirrorMode(MirrorMode.MIRROR_MODE_ON_FRONT_ONLY)`. |

---

## Performance Traps

| Trap | Symptoms | Prevention | When It Breaks |
|------|----------|------------|----------------|
| Per-frame Bitmap allocation in analyzer | FPS starts fine, degrades over 30 sec; occasional 100ms GC pauses | Pass `InputImage.fromMediaImage(proxy.image, rot)` directly; no Bitmap conversion | Every session >30s on mid-range devices |
| Contour + landmarks + classification all enabled | Detection FPS <15, visible lag | Pick contour OR landmarks/classification, not both | All devices, all times |
| Default `STRATEGY_BLOCK_PRODUCER` backpressure | Preview jank increases with face detection load | `STRATEGY_KEEP_ONLY_LATEST` | Any device where ML Kit < preview FPS (most mid-range) |
| 4K or 1080p ImageAnalysis input | CPU pegged, thermal throttle in 2 min | `setTargetResolution(Size(1280, 720))` or lower | Mid/low-end devices immediately |
| Running ML Kit during video recording at full rate | Recording FPS drops, audio desync | Throttle detection to every 2nd frame during record, or drop to FAST mode | Recordings >3 min |
| Synchronous disk write in analyzer callback | Dropped frames, logcat warnings | All I/O on `Dispatchers.IO` | Always |
| AnimationDrawable for sprite animation (many PNGs) | Slow frame advance, memory churn | Animated WebP + ImageDecoder, or sprite sheet | >10 frames per animation |
| PreviewView in RecyclerView/ViewPager2 | Race conditions, "Camera in use" errors | Single PreviewView per Activity lifecycle | Any multi-screen camera design |

---

## Security Mistakes

| Mistake | Risk | Prevention |
|---------|------|------------|
| Logging `face.boundingBox` or landmark data with user-identifiable context | Biometric data leak in crash logs | Never log ML Kit face data at all — it has regulatory weight (biometric identifier) in some jurisdictions |
| Storing photos/videos in app-private + uploading silently | Covert biometric collection — policy violation even for personal app | MVP is offline only. If cloud sync is added later, require explicit opt-in per-save. |
| `file://` URI in share intent | `FileUriExposedException` + path disclosure | Use `content://` URI from MediaStore (already on hand) |
| Exported activities with camera / deeplink to record | Third-party apps can force silent recording | Camera activity is `android:exported="false"` (default for explicit intent filters absent) |
| `android:allowBackup="true"` on debug (Android default) | User's personal prank videos backed up to cloud without consent | Set `allowBackup="false"` OR define `fullBackupContent` excluding media |
| Missing `usesCleartextTraffic="false"` | Irrelevant (app is offline) but good hygiene | Set explicitly in manifest |

---

## UX Pitfalls

| Pitfall | User Impact | Better Approach |
|---------|-------------|-----------------|
| No "camera loading" state between app launch and first preview | User sees black screen, thinks app broke | Show loading spinner / placeholder; bind camera in onCreate, preview appears when ready |
| Filter picker shows before camera ready | Tapping filter does nothing → frustration | Gate filter UI on camera-ready state |
| No haptic/sound on capture | User unsure if photo captured | Short click sound + vibration (respect system silent mode) |
| No visible recording indicator | User records forever unintentionally | Red dot + elapsed time; cap at 60s default |
| Captured image looks different from preview (no filter in output) | Core feature broken perception | OverlayEffect applied to `ImageCapture` target too; test in Phase 4 |
| Front camera un-mirrors captured photo (standard Camera API behavior) but user expects mirror | "My filter is backwards!" | Apply mirror to saved photo for front camera (like Instagram/Snapchat do) — research reference app behavior |
| Permission denial leaves blank screen | User stuck | Show "Enable camera in Settings" CTA with intent to app settings |
| Face not detected → no filter, no feedback | User thinks app broke | "Looking for a face..." placeholder text or helper overlay |
| Filter "swaps" abruptly mid-record | Jarring in final video | Lock filter selection during recording, or fade transition |
| No undo for capture (tap-share flow) | Mistake captures pollute gallery | Preview/discard screen before save — match reference app pattern |

---

## "Looks Done But Isn't" Checklist

- [ ] **Preview:** Works in portrait — verify **landscape** + **reverse-portrait** + **reverse-landscape** all render correctly.
- [ ] **Face detection:** Works with one face — verify **two faces** in frame doesn't crash or freeze (even if filter only applies to primary).
- [ ] **Coordinate transform:** Correct in preview — verify **captured photo** has sprite in correct position (not off by mirror/scale).
- [ ] **Video recording:** Starts and stops — verify **saved .mp4 actually contains the overlay sprites** (this is THE gotcha).
- [ ] **Video recording:** Video and audio both saved — verify audio sync doesn't drift over 60s recording.
- [ ] **Front/back camera:** Preview works — verify **switching mid-session** releases and rebinds correctly without "Camera in use."
- [ ] **Save:** MediaStore insert returns URI — verify file **appears in Google Photos / Gallery app**, not just in your MediaStore query.
- [ ] **Share:** Intent fires — verify **receiving app (WhatsApp/IG/TikTok) actually gets the file** (content URI permissions).
- [ ] **Lifecycle:** Resume from background — verify **after phone call / home button / 10-min background**, preview returns cleanly.
- [ ] **Permission denial:** User denies camera — verify **doesn't crash**, shows settings CTA.
- [ ] **Sprite animation:** Plays on one filter — verify **frame timing doesn't drift** over 60s.
- [ ] **Jitter:** Filter tracks face — verify **head still = bug still** (no visible shake), **fast head turn = bug follows** (no >100ms lag).
- [ ] **Thermal:** 1-minute recording works — verify **5-minute recording** doesn't degrade FPS or crash encoder.
- [ ] **APK size:** Ships — verify **final APK ≤ 40MB** excluding ads/billing (target; adjust based on reference).
- [ ] **Cross-device:** Works on dev's phone — verify **Samsung + one other OEM** before declaring done.
- [ ] **Scoped storage:** File saves — verify **clears IS_PENDING** so file is not stuck in limbo.
- [ ] **Memory:** No OOM after 10 captures — verify **memory profiler flat line** after 30 captures.

---

## Recovery Strategies

When pitfalls occur despite prevention.

| Pitfall | Recovery Cost | Recovery Steps |
|---------|---------------|----------------|
| Video recording has no overlay (wrong architecture picked) | HIGH | Rewrite render path to OverlayEffect. 1–3 days. Sprites and detection logic survive; only the drawing target changes. |
| Coord transform is device-specific | MEDIUM | Rip out manual transform; migrate to `MlKitAnalyzer` with VIEW_REFERENCED. 1 day + retest matrix. |
| APK size ballooned | LOW–MEDIUM | Convert PNG→WebP (batch in Android Studio), drop density buckets except xxhdpi, re-measure. 0.5 day. |
| Memory leak / OOM | MEDIUM | LeakCanary + memory profiler session; ~70% of leaks are missing `close()`/`recycle()` calls traceable in one sitting. |
| Jitter on shipped filter | LOW | Insert 1€ filter at detector→renderer boundary; no other code changes. ~2 hours. |
| Device-specific camera quirk | MEDIUM | File issue in CameraX discussion group; short-term: detect model with `Build.MANUFACTURER`+`Build.MODEL` and hard-code workaround. |
| Scoped storage file appears corrupt | LOW | Add IS_PENDING transaction wrapper; one-time fix, reused app-wide. |
| Thermal throttling mid-record | MEDIUM | Add thermal listener; reduce detection rate above MODERATE; cap record duration. |
| Permission flow broken on Android 13 | LOW | Audit manifest, switch to ActivityResultContracts API, add POST_NOTIFICATIONS only if actually used. |

---

## Pitfall-to-Phase Mapping

| # | Pitfall | Prevention Phase | Verification |
|---|---------|------------------|--------------|
| 1 | Coordinate space chaos | Phase 2 | Debug red-rect overlay aligns with face in portrait + landscape, front + back, on 2+ devices |
| 2 | Video loses overlay | End of Phase 2 (decision) → Phase 3 (verify) | First video test mp4 must contain a visible red test rectangle |
| 3 | Landmark jitter | Phase 3 | Static-head recording: bug movement should be <1px/frame |
| 4 | ImageAnalysis backpressure | Phase 2 | Preview maintains ≥24 FPS with face detection enabled |
| 5 | CameraX/ML Kit version mismatch | Phase 1 | Clean gradle + BoM; clean build succeeds on CI and local |
| 6 | Scoped storage silent failure | Phase 4 | Saved file opens in Google Photos, full-size, correct aspect |
| 7 | Device fragmentation | Phase 2 baseline + Phase 6 matrix | Works on Samsung + Xiaomi + Pixel by Phase 6 exit |
| 8 | Bitmap leaks | Phase 4 (photo capture) + ongoing | 30-capture session shows flat memory profile |
| 9 | Camera lifecycle leak | Phase 2 | LeakCanary clean on navigate-away-and-back, 10× repetitions |
| 10 | Main-thread ANR | Phase 1 (StrictMode) + ongoing | StrictMode violations = 0 in debug builds |
| 11 | Thermal throttling | Phase 5 | 5-minute recording maintains ≥20 FPS; ThermalStatus hooked |
| 12 | APK bloat | Phase 3 (sprite template) + Phase 6 audit | Final APK ≤ 40MB |
| 13 | Multi-face contour bug | Phase 2 (policy) + Phase 3 (graceful fallback) | Two-face scene: no crash, primary face filtered correctly |
| 14 | Android 13 permission gaps | Phase 1 (manifest) + Phase 5 (audio) | Test on Android 13/14 device: CAMERA + RECORD_AUDIO prompts correctly |

---

## Sources

### Official documentation (HIGH confidence)
- [ML Kit Analyzer | Android media | Android Developers](https://developer.android.com/media/camera/camerax/mlkitanalyzer) — coordinate systems, VIEW_REFERENCED transform
- [Detect faces with ML Kit on Android | Google for Developers](https://developers.google.com/ml-kit/vision/face-detection/android) — 200×200 min face for contour, FAST vs ACCURATE, tracking
- [Face detection concepts | ML Kit](https://developers.google.com/ml-kit/vision/face-detection/face-detection-concepts) — face size, landmark definitions
- [Image analysis | Android media | Android Developers](https://developer.android.com/media/camera/camerax/analyze) — STRATEGY_KEEP_ONLY_LATEST, ImageProxy.close semantics
- [CameraX video capturing architecture | Android](https://developer.android.com/media/camera/camerax/video-capture) — VideoCapture + Recorder + mirror mode
- [What's new in CameraX 1.4.0](https://android-developers.googleblog.com/2024/12/whats-new-in-camerax-140-and-jetpack-compose-support.html) — OverlayEffect for PREVIEW + VIDEO_CAPTURE
- [OverlayEffect | API reference](https://developer.android.com/reference/kotlin/androidx/camera/effects/OverlayEffect) — Canvas-based overlay, getSensorToBufferTransform
- [CameraEffect | API reference](https://developer.android.com/reference/androidx/camera/core/CameraEffect) — PREVIEW | VIDEO_CAPTURE targets
- [PreviewView.ScaleType | API reference](https://developer.android.com/reference/androidx/camera/view/PreviewView.ScaleType) — FILL_CENTER vs FIT_CENTER math
- [CameraX use case rotations](https://developer.android.com/training/camerax/orientation-rotation) — rotation handling
- [Better Device Compatibility with CameraX (Quirks)](https://android-developers.googleblog.com/2022/10/better-device-compatibility-with-camerax.html) — 30+ device quirks auto-compensated
- [Access media files from shared storage](https://developer.android.com/training/data-storage/shared/media) — MediaStore + RELATIVE_PATH + IS_PENDING
- [Behavior changes: Apps targeting Android 13+](https://developer.android.com/about/versions/13/behavior-changes-13) — POST_NOTIFICATIONS, READ_MEDIA_*
- [Notification runtime permission](https://developer.android.com/develop/ui/views/notifications/notification-permission) — Android 13 notification flow
- [Managing Bitmap Memory](https://developer.android.com/topic/performance/graphics/manage-memory) — recycle, sample size, RGB_565
- [Reduce your app size](https://developer.android.com/topic/performance/reduce-apk-size) — WebP, density strategy, resource management
- [Thermal mitigation | Android Open Source Project](https://source.android.com/docs/core/power/thermal-mitigation) — thermal API

### Community / engineering (MEDIUM confidence)
- [Convert YUV To RGB for CameraX ImageAnalysis (Android Developers Medium)](https://medium.com/androiddevelopers/convert-yuv-to-rgb-for-camerax-imageanalysis-6c627f3a0292) — libyuv pipeline, setOutputImageFormat
- [CameraX Preview overlay and saved Video Capture Overlay (CameraX group)](https://groups.google.com/a/android.com/g/camerax-developers/c/64eahzvdY4U) — overlay+video confirmed pattern
- [CameraX Camera Leaks (CameraX group)](https://groups.google.com/a/android.com/g/camerax-developers/c/j8T9iRtTGtw) — rebind race condition
- [Support multiple face contours — ML Kit issue #49](https://github.com/googlesamples/mlkit/issues/49) — confirmed contour-on-primary-face-only behavior
- [Barcode Scanner on Android "Image already closed" — mlkit issue #144](https://github.com/googlesamples/mlkit/issues/144) — ImageProxy close timing
- [How to reduce jittering of face landmarks — mediapipe issue #825](https://github.com/google/mediapipe/issues/825) — 1€ filter as community-standard solution
- [OneEuro Filter walkthrough](https://mohamedalirashad.github.io/FreeFaceMoCap/2021-12-25-filters-for-stability/) — practical parameter tuning

---
*Pitfalls research for: Android AR face-filter camera app (Bugzz)*
*Researched: 2026-04-18*
