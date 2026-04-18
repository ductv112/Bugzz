<!-- GSD:project-start source:PROJECT.md -->
## Project

**Bugzz**

Bugzz là một Android AR camera app chạy trên điện thoại: bật camera trước, detect khuôn mặt, và phủ các hiệu ứng con côn trùng/bug (kiến, gián, nhện, sâu...) bò trên mặt người theo thời gian thực. User chụp ảnh hoặc quay video có filter rồi chia sẻ — một app "prank" để làm trò với bạn bè.

Sản phẩm là **clone feature-parity** của app tham chiếu `com.insect.filters.funny.prank.bug.filter.face.camera` (tên Play Store: "Bugzz Filters Prank", version 1.2.7). Mục đích cá nhân — chủ dự án dùng để học stack AR + face filter và sử dụng riêng, không phát hành Play Store ở giai đoạn hiện tại.

**Core Value:** **Live AR preview mượt + bug animation bám chính xác landmark khuôn mặt.** Nếu filter giật hoặc bug không bám theo mặt, mọi thứ khác (chụp, quay, share) vô nghĩa — đây là phần "wow" của app. Mọi tradeoff về kiến trúc và UI đều phục vụ chất lượng live preview.

### Constraints

- **Tech stack**: Native Android — Kotlin, CameraX, ML Kit Face Detection (contour mode cho landmark accurate), Jetpack Compose *hoặc* Views (sẽ quyết ở phase 1). Không xét Flutter / React Native / Unity — đã khóa.
- **Target SDK**: minSdk = 28 (Android 9), targetSdk = 35 (Android 15) — match reference app 1:1.
- **Architecture**: MVVM / MVI tiêu chuẩn Android (quyết chi tiết ở phase 1 sau khi research).
- **Performance**: Live preview phải đạt ≥ 24 fps trên thiết bị test (Android 9+ tầm trung). Face detection latency < 100ms/frame.
- **Storage**: Scoped storage (bắt buộc từ Android 10), dùng MediaStore API để lưu ảnh/video vào DCIM/Bugzz.
- **Legal**: Asset extract từ reference APK dùng trực tiếp cho personal use — OK ở giai đoạn này, KHÔNG được dùng nếu sau này public store. Chủ dự án đã xác nhận sẽ thay UI + assets trước khi publish nếu có.
- **Solo dev**: Không có áp lực deadline; ưu tiên code quality + feature parity chính xác hơn tốc độ.
<!-- GSD:project-end -->

<!-- GSD:stack-start source:research/STACK.md -->
## Technology Stack

## Locked Decisions (from PROJECT.md — not revisited)
| Locked | Value |
|--------|-------|
| Language | Kotlin |
| Camera | CameraX (not raw Camera2) |
| Face detection | ML Kit Face Detection, contour mode |
| Platform | Native Android (no Flutter/RN/Unity) |
| minSdk | 28 (Android 9) |
| targetSdk | 35 (Android 15) |
## Executive Recommendation (TL;DR)
| Concern | Recommendation |
|---------|----------------|
| Live overlay rendering | **`OverlayEffect` from `androidx.camera:camera-effects` driving a `Canvas` with pre-decoded Bitmaps (APNG/frame-seq) in a custom renderer.** Canvas is sufficient — 3-5 sprites at 30 fps on Android 9+ midrange is well within CPU budget, and `OverlayEffect` ensures the overlay is baked into Preview + VideoCapture + ImageCapture streams automatically. |
| Sprite animation | **Frame-sequence PNG atlases** driven by `Choreographer` (or CameraX frame callbacks). **Not Lottie** — bug sprites are bitmap cartoons, not vector, and Lottie adds overhead without benefit. |
| Video recording | **CameraX `Recorder` + `VideoCapture` with `OverlayEffect` attached to the same `UseCaseGroup`**. The effect is composited into the encoded stream by CameraX — no manual MediaMuxer / FBO plumbing. |
| Photo capture | **CameraX `ImageCapture` with the same `OverlayEffect`**. Overlay is baked into the JPEG by the effect pipeline — no separate bitmap compositing step. |
| UI framework | **Jetpack Compose** (Compose-first, not hybrid). Use `androidx.camera:camera-compose` `CameraXViewfinder`. |
| DI | **Hilt**. Compile-time safety; app is small but likely to grow (ads/billing in next milestone). |
| Architecture | **MVVM + `StateFlow` + unidirectional data flow**. Standard Google-sanctioned pattern; sufficient for app size. |
| Build | AGP 8.9.x + Gradle 8.13 + Kotlin 2.1.21 + JVM 17 target. **Not AGP 9.x** (too new; Compose plugin and third-party KSP processors lag). |
| Share | **`Intent.ACTION_SEND` + `FileProvider`** with MIME type from the saved URI. |
| Storage | **`MediaStore` with `RELATIVE_PATH = DCIM/Bugzz` + `IS_PENDING` flag**. No raw `File` paths; `WRITE_EXTERNAL_STORAGE` only for API <= 28 fallback. |
## Recommended Stack
### Core Technologies
| Technology | Version | Purpose | Why Recommended |
|------------|---------|---------|-----------------|
| Kotlin | **2.1.21** | Primary language | Stable K2 compiler; compatible with Compose compiler plugin 1.5.x; avoids 2.2 early-adopter churn |
| Android Gradle Plugin | **8.9.1** | Build system | Mature, stable, wide third-party support. AGP 9.x (Jan 2026) introduces built-in Kotlin plugin and breaks several KSP processors — defer until Hilt/Compose ecosystem catches up |
| Gradle | **8.13** | Build engine | Required minimum for AGP 8.9 |
| JVM target | **17** | Bytecode target | Standard for AGP 8.x; Android Studio bundles JDK 17+ |
| CameraX | **1.6.0** (core/lifecycle/video/view/effects/mlkit-vision/compose) | Camera API | Released March 2026; first version with stable `camera-compose` + stable `camera-effects` (`OverlayEffect`) + integrated Media3 muxer for VideoCapture. This is the single most important version lock — **1.6 is where effect-based overlay compositing is first-class** |
| ML Kit Face Detection (bundled) | **16.1.7** | Face landmark/contour detection | Bundled model (~20MB) = deterministic first-run (no Play Services model download wait). Package is offline; contour mode gives the dense landmark points needed for bugs to crawl along face edges |
| Jetpack Compose BOM | **2026.04.00** | UI toolkit versions | Pins all Compose artifacts to tested-together versions (foundation/ui 1.10.6, material3 1.4.0) |
| Compose Compiler Gradle plugin | bundled with Kotlin 2.1.21 | Compose codegen | Since Kotlin 2.0, the Compose compiler ships as a Kotlin plugin — no manual version matrix |
| Hilt (Dagger) | **2.57** | Dependency injection | Compile-time DI, first-party Google integration with ViewModel/Compose/WorkManager |
| KSP | **2.1.21-1.0.32** | Annotation processing | Must match Kotlin version exactly; powers Hilt codegen |
| AndroidX Activity | **1.10.1** | Activity + `ComponentActivity` + permission contracts | Required for Compose + `rememberLauncherForActivityResult` |
| AndroidX Lifecycle | **2.9.0** | `ViewModel`, `lifecycleScope`, `StateFlow` integration | `androidx.lifecycle:lifecycle-viewmodel-compose` for Compose `viewModel()` + `lifecycle-runtime-compose` for `collectAsStateWithLifecycle` |
| Coroutines | **1.10.2** | Async/frame loops | Standard for ViewModel work + ML Kit `await` wrappers |
| Navigation Compose | **2.8.9** | Screen nav (splash → home → camera → preview/save) | First-party Compose nav |
### Supporting Libraries
| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| `androidx.camera:camera-effects` | **1.6.0** | `OverlayEffect` API for Canvas-based compositing into preview + video + photo | **Core to this app.** Attach once, all three output streams get overlay |
| `androidx.camera:camera-mlkit-vision` | **1.6.0** | `MlKitAnalyzer` — glue between CameraX `ImageAnalysis` and ML Kit detectors | Eliminates manual `ImageProxy → InputImage` plumbing + automatic sensor-to-buffer transform matrix |
| `androidx.camera:camera-compose` | **1.6.0** | `CameraXViewfinder` composable | Replaces `AndroidView(PreviewView)` hack; correct Compose z-ordering for overlays |
| `com.google.mlkit:face-detection` | **16.1.7** | Bundled face detection model | App works offline without Play Services face-detection module download |
| `androidx.core:core-ktx` | **1.15.0** | Kotlin extensions on framework APIs | `ContentValues` builders, `Uri` helpers |
| `androidx.activity:activity-compose` | **1.10.1** | `setContent { }`, permission contracts | Runtime camera + mic permissions |
| `androidx.lifecycle:lifecycle-viewmodel-compose` | **2.9.0** | `viewModel()` in composables | MVVM bridging |
| `androidx.lifecycle:lifecycle-runtime-compose` | **2.9.0** | `collectAsStateWithLifecycle()` | Prevents StateFlow collection on backgrounded screens |
| `androidx.navigation:navigation-compose` | **2.8.9** | Screen routing | Splash → Home → Camera → Preview |
| `com.google.dagger:hilt-android` | **2.57** | Runtime DI | Inject ViewModels, repositories |
| `com.google.dagger:hilt-compiler` (ksp) | **2.57** | Hilt codegen | KSP version |
| `androidx.hilt:hilt-navigation-compose` | **1.2.0** | `hiltViewModel()` | Scoped VMs in nav graph |
| `io.coil-kt:coil-compose` | **2.7.0** | Image loading for filter-picker thumbnails | Lighter than Glide; Compose-native; Kotlin-first |
| `androidx.datastore:datastore-preferences` | **1.1.3** | Persist user prefs (selected filter, last-used camera) | Replaces SharedPreferences with a coroutine/flow API |
| Timber | **5.0.1** | Logging wrapper | Strip logs from release via `BuildConfig.DEBUG` tree |
### Development Tools
| Tool | Purpose | Notes |
|------|---------|-------|
| Android Studio Ladybug (2024.2.1) or newer | IDE | Minimum for AGP 8.9 + Compose 1.10 tooling |
| Gradle Version Catalog (`libs.versions.toml`) | Central dep versions | Required — hand-edited versions in `build.gradle.kts` drift rapidly with 20+ Camera/Compose/Hilt modules |
| LeakCanary | Memory leak detection (debug) | Camera lifecycle is a leak minefield (Surface, ImageAnalyzer) |
| detekt | Kotlin static analysis | Catches common Kotlin anti-patterns; lightweight |
| `scrcpy` | Live screen mirror for ADB-connected device | Essential because emulator cannot simulate real camera/face detection |
## The Critical Decision: Live Preview Overlay Compositing
### Option Matrix
| Approach | Preview path | Video path | Photo path | Complexity | Perf on minSdk 28 midrange | Verdict |
|----------|--------------|------------|------------|------------|----------------------------|---------|
| **Canvas on top of `PreviewView` (separate `View`)** | `View.onDraw` over `PreviewView` | Manual — snapshot `Bitmap` each frame + `MediaRecorder`/`MediaMuxer` | Manual — `ImageCapture.takePicture` → decode → composite on Bitmap → re-encode | Low for preview; **high for video** | OK for preview; video path is a minefield | REJECT — video pipeline duplicates work and is the known source of "my filter shows on screen but not in recording" bugs |
| **Custom `GLSurfaceView` + shader pipeline** | Fullscreen GL surface, custom texture from camera | Shared GL context → MediaCodec input Surface (FBO) | Read pixels from FBO | Very high | Best possible (native GPU) | REJECT — overkill for 3-5 static bitmap sprites; 2-3 week tangent just to plumb the GL context + EGL share |
| **GPUImage-Android (`jp.co.cyberagent.android:gpuimage`)** | Built-in filter chain | Separate integration | Via bitmap output | Medium | Good | REJECT — library is in maintenance mode, last release 2021; designed for image filters (blur, sepia) not arbitrary sprite overlays |
| **CameraX `OverlayEffect` (Canvas-based, official)** | ✅ auto | ✅ auto — same effect baked into video | ✅ auto — same effect baked into photo | Low-medium | Canvas on GPU-backed surface; fine for 3-5 sprites @ 30fps | **RECOMMENDED** |
| **Custom `CameraEffect` subclass (OpenGL)** | Author-written GL shader | ✅ auto | ✅ auto | High | Best | Future upgrade path if Canvas-based `OverlayEffect` proves insufficient in profiling |
### Why `OverlayEffect` Wins
### Draw Loop Shape
### Fallback Plan
## Sprite Animation Recommendation
### Decision: Frame-sequence PNG atlases, custom mini-renderer
| Option | Pros | Cons | Verdict |
|--------|------|------|---------|
| **Lottie** (`com.airbnb.android:lottie` 6.7.1) | Beautiful for vector | Source art (reference app's bug sprites) is raster bitmap, not After Effects; Lottie adds ~1MB and renders SVG paths — wasted work for bitmap frames | REJECT |
| **`AnimationDrawable` (frame-by-frame)** | Framework built-in | Not designed for Canvas — it's a `View`-level Drawable; pulling per-frame bitmaps out to draw manually requires `current` hacks | REJECT |
| **Custom frame-sequence renderer** (list of `Bitmap` + frame index driven by `frameTimeNanos`) | Zero dependency, exact control, trivially correct | ~50 LOC to write | **RECOMMEND** |
| **Sprite sheet (atlas) + `Canvas.drawBitmap(src, dst)`** | Faster Bitmap decode (one load), smaller memory if tightly packed | Marginally more complex than separate PNGs | Optional optimization — do this if decoded PNG set exceeds ~16MB heap |
## Photo and Video Capture (Detailed)
### Photo capture
### Video capture
## Architecture Pattern
### Decision: MVVM + StateFlow + UDF (Unidirectional Data Flow)
- **Screen state** = `data class CameraUiState(...)` exposed as `StateFlow<CameraUiState>` from the ViewModel.
- **User intents** = method calls on ViewModel (`onFilterSelected`, `onShutterTapped`, `onRecordToggled`).
- **Side effects** (saving to MediaStore, share intent launch) = one-shot `Channel<UiEvent>` collected as flow in the composable.
- **Repositories** injected via Hilt: `FilterRepository`, `MediaRepository` (wraps `MediaStore` ops), `FaceDetectionEngine` (wraps ML Kit + holds `AtomicReference<Face>`).
## Share + Storage APIs
### Share
### Storage (MediaStore + Scoped Storage)
- `WRITE_EXTERNAL_STORAGE` in manifest with `android:maxSdkVersion="28"` — legacy path only (we don't actually need it for minSdk 28 + MediaStore, but reference APK requests it and it harms nothing).
- No `READ_MEDIA_IMAGES`/`READ_MEDIA_VIDEO` needed — app only reads files it just wrote, which MediaStore grants access to implicitly.
- CameraX `ImageCapture` and `VideoCapture` both accept `MediaStoreOutputOptions` directly — use that rather than writing streams manually when possible.
## UI Framework: Compose vs Views — Firm Recommendation
### Recommendation: Jetpack Compose (full, not hybrid)
- Massive existing XML codebase (greenfield project — N/A).
- Complex custom `ViewGroup` performance requirements (overlay is handled by `OverlayEffect`, not a Compose widget — N/A).
- Team unfamiliar with Compose and under deadline (solo dev, no deadline — N/A).
## Dependency Injection: Hilt vs Koin
### Recommendation: Hilt
## Build Configuration Details
# gradle/libs.versions.toml (excerpt)
### `build.gradle.kts` (app) essentials
## Installation (Quick Reference)
## Alternatives Considered
| Recommended | Alternative | When to Use Alternative |
|-------------|-------------|-------------------------|
| `OverlayEffect` (Canvas) | Custom `CameraEffect` w/ OpenGL ES shaders | If profiling shows Canvas can't sustain 24 fps with production sprite load; or if pixel-level shaders (distortion, chroma) are needed later |
| Frame-seq PNG | Lottie | If designer delivers After Effects JSON instead of bitmap frames |
| Hilt | Koin | If developer strongly prefers Kotlin-DSL-only, runtime DI; KMP plans |
| Compose | Views (XML + Fragments) | N/A for this project — no reason applies |
| MVVM + StateFlow | MVI with reducer | If state-class count grows past ~8 screens and testability of reducer becomes the bottleneck |
| AGP 8.9 | AGP 9.1 | Wait ~3-6 months after release for KSP processors and Compose plugin to fully support — not now |
| Kotlin 2.1.21 | Kotlin 2.2.x | When Compose compiler plugin + Hilt KSP processor publish matching 2.2 builds and remain stable for a month |
| CameraX `Recorder` + MediaStore output | Media3 Transformer post-processing | If the recording pipeline needs post-capture edits (trim, watermark second pass) — not MVP |
| MediaStore URI direct share | `FileProvider` share | If artifact is in app-private storage (`filesDir`) — we aren't |
## What NOT to Use
| Avoid | Why | Use Instead |
|-------|-----|-------------|
| **ARCore Augmented Faces** | Locked out by project decision, but also: needs Google Play Services ARCore, requires ARCore-capable devices (subset of minSdk 28 devices), adds ~100MB model download. Overkill for 2D overlay | ML Kit Face Detection contour mode (locked) |
| **MediaPipe Face Mesh** | Locked out by project decision. Richer 478-point mesh than ML Kit, but requires more plumbing (TensorFlow Lite, custom rendering). Only wins if you need 3D mask — we need 2D sprites | ML Kit (locked) |
| **Raw Camera2 API** | 5-10x more code than CameraX for equivalent behavior; locked out | CameraX (locked) |
| **`TextureView` + manual matrix overlay** | Pre-CameraX-1.4 pattern; mirror/rotation bugs on front camera; does not propagate overlay into video/photo automatically | `PreviewView` or `CameraXViewfinder` + `OverlayEffect` |
| **`MediaRecorder` directly** | Low-level, fiddly, rotation bugs. CameraX wraps it with `Recorder` and bakes in effect compositing | CameraX `VideoCapture` + `Recorder` |
| **Glide** | Still works, but annotation processing (kapt) forces kapt usage which slows build vs Coil's KSP-free design | Coil 2.7 |
| **Dagger 2 (without Hilt)** | Boilerplate that Hilt eliminates (Component, Module, Subcomponent hand-wiring) | Hilt |
| **Rx Java / Rx Kotlin** | Pre-coroutines paradigm; ML Kit returns `Task` (already awaitable from coroutines) and CameraX is listener-based | Kotlin Coroutines + Flow |
| **`SharedPreferences` directly** | Synchronous I/O on main thread, no type safety, no flow API | DataStore Preferences |
| **`android.support.*` / legacy support lib** | Removed years ago | `androidx.*` |
| **Lottie for bitmap bugs** | Designed for vector; bitmap sprite animation wastes its runtime | Frame-sequence renderer |
| **`android:requestLegacyExternalStorage="true"`** | Only a stopgap for apps not migrated off File I/O. minSdk 28 has MediaStore cleanly available | MediaStore scoped-storage pattern |
| **WRITE_EXTERNAL_STORAGE permission (unqualified)** | Unnecessary on API 29+ when using MediaStore; reference APK requests it but we can scope it | `<uses-permission android:name="...WRITE_EXTERNAL_STORAGE" android:maxSdkVersion="28" />` only (defensive; strictly optional since minSdk=28 means only one API level ever sees it) |
| **`PreviewView.bitmap`** for photo capture | Gives you the View's bitmap (may be low-res, missing effects, with UI chrome leaking in) | `ImageCapture.takePicture()` with effect attached |
## Stack Patterns by Variant
- Escalate to a custom `CameraEffect` subclass implementing OpenGL ES 2.0 texture rendering.
- Keep everything else (ML Kit, CameraX, Compose, Hilt) identical.
- Budget: 3-5 days of additional work.
- Triggering metric: average frame time > 33 ms on a Snapdragon-675-class device over a 10-second recording.
- Switch from per-frame PNGs to **sprite sheet atlasing** (single big PNG, `Canvas.drawBitmap(src, dst)` with source rect per frame).
- Use `BitmapFactory.Options().apply { inPreferredConfig = Bitmap.Config.RGB_565 }` for sprites with no alpha-critical gradients to halve memory.
- Keep Hilt (Google Ads SDK + Billing client fit the pattern naturally).
- Add `com.google.android.gms:play-services-ads` + `com.applovin:applovin-sdk` + `com.android.billingclient:billing-ktx` at that time — don't pre-install.
## Version Compatibility
| Package A | Compatible With | Notes |
|-----------|-----------------|-------|
| Kotlin 2.1.21 | KSP 2.1.21-1.0.32 | KSP **version must match** Kotlin exactly (major.minor.patch) |
| Kotlin 2.1.21 | Compose Compiler Gradle plugin (bundled) | Compose compiler auto-syncs via Kotlin plugin since 2.0 |
| AGP 8.9.x | Gradle 8.13+ | AGP 8.9 requires Gradle 8.11.1 minimum; 8.13 is current stable |
| AGP 8.9.x | JDK 17 | AGP 8.x requires Java 17 (Studio Ladybug+ bundles it) |
| CameraX 1.6.0 | minSdk 21 | Well below our 28 |
| CameraX 1.6.0 | Kotlin 1.9+ | 2.1.21 fine |
| ML Kit face-detection 16.1.7 | minSdk 21 | Fine |
| Hilt 2.57 | KSP 2.x | Must use `ksp(...)` not `kapt(...)` for Hilt processor |
| Compose BOM 2026.04.00 | Kotlin 2.1.x | BOM tested with Kotlin 2.1 |
| Navigation Compose 2.8.x | Compose BOM 2025.x+ | 2026.04 BOM fine |
## Sources
- [CameraX release notes (Android Developers)](https://developer.android.com/jetpack/androidx/releases/camera) — verified 1.6.0 stable March 2026
- [Introducing CameraX 1.5 (Android Developers Blog, Nov 2025)](https://android-developers.googleblog.com/2025/11/introducing-camerax-15-powerful-video.html) — Media3 muxer integration, OverlayEffect + effect-baked video
- [What's new in CameraX 1.4.0 (Android Developers Blog, Dec 2024)](https://android-developers.googleblog.com/2024/12/whats-new-in-camerax-140-and-jetpack-compose-support.html) — `OverlayEffect` introduction, Canvas API + sensorToBufferTransform
- [OverlayEffect API reference](https://developer.android.com/reference/kotlin/androidx/camera/effects/OverlayEffect)
- [ML Kit Face Detection on Android](https://developers.google.com/ml-kit/vision/face-detection/android) — version 16.1.7, contour mode
- [ML Kit Analyzer with CameraX](https://developer.android.com/media/camera/camerax/mlkitanalyzer)
- [Compose-Native CameraX guide (ProAndroidDev, Oct 2025)](https://proandroiddev.com/goodbye-androidview-camerax-goes-full-compose-4d21ca234c4e) — `CameraXViewfinder` stable
- [Jetpack Compose BOM mapping (Android Developers)](https://developer.android.com/develop/ui/compose/bom/bom-mapping) — 2026.04.00 current April 2026
- [AGP 9.0 release notes (Android Developers)](https://developer.android.com/build/releases/agp-9-0-0-release-notes) — confirms 8.x still current for KSP ecosystem
- [Access media files from shared storage (Android Developers)](https://developer.android.com/training/data-storage/shared/media) — MediaStore + scoped storage canonical pattern
- [Send simple data to other apps (Android Developers)](https://developer.android.com/training/sharing/send) — `ACTION_SEND` + content URI
- [Hilt vs Koin comparison (droidcon, Nov 2025)](https://www.droidcon.com/2025/11/26/hilt-vs-koin-the-hidden-cost-of-runtime-injection-and-why-compile-time-di-wins/) — DI tradeoff validation
- [Lottie Android releases](https://github.com/airbnb/lottie-android/releases) — 6.7.1 current
- [Maven repository: com.google.mlkit:face-detection](https://mvnrepository.com/artifact/com.google.mlkit/face-detection) — 16.1.7 current
- [CameraX OverlayEffect + ML Kit sample discussion](https://groups.google.com/a/android.com/g/camerax-developers/c/64eahzvdY4U) — integration pattern
- Core version numbers (CameraX 1.6.0, Compose BOM 2026.04.00, Kotlin 2.1.21, AGP 8.9.1, ML Kit 16.1.7) — verified April 2026 — HIGH
- OverlayEffect-as-compositing-solution — HIGH (multiple official sources + API reference)
- Canvas-vs-GL perf estimate at 24 fps for 3-5 sprites on Snapdragon-675 — MEDIUM (industry convention; must be validated on real device in Phase 1)
- Compose-over-Views for this specific project — HIGH (all four decision factors align)
- Hilt-over-Koin — MEDIUM-HIGH (both viable; Hilt slightly stronger for stated trajectory)
<!-- GSD:stack-end -->

<!-- GSD:conventions-start source:CONVENTIONS.md -->
## Conventions

Conventions not yet established. Will populate as patterns emerge during development.
<!-- GSD:conventions-end -->

<!-- GSD:architecture-start source:ARCHITECTURE.md -->
## Architecture

Architecture not yet mapped. Follow existing patterns found in the codebase.
<!-- GSD:architecture-end -->

<!-- GSD:skills-start source:skills/ -->
## Project Skills

No project skills found. Add skills to any of: `.claude/skills/`, `.agents/skills/`, `.cursor/skills/`, or `.github/skills/` with a `SKILL.md` index file.
<!-- GSD:skills-end -->

<!-- GSD:workflow-start source:GSD defaults -->
## GSD Workflow Enforcement

Before using Edit, Write, or other file-changing tools, start work through a GSD command so planning artifacts and execution context stay in sync.

Use these entry points:
- `/gsd-quick` for small fixes, doc updates, and ad-hoc tasks
- `/gsd-debug` for investigation and bug fixing
- `/gsd-execute-phase` for planned phase work

Do not make direct repo edits outside a GSD workflow unless the user explicitly asks to bypass it.
<!-- GSD:workflow-end -->



<!-- GSD:profile-start -->
## Developer Profile

> Profile not yet configured. Run `/gsd-profile-user` to generate your developer profile.
> This section is managed by `generate-claude-profile` -- do not edit manually.
<!-- GSD:profile-end -->
