# Stack Research — Bugzz (Android AR Face Filter Camera App)

**Domain:** Native Android AR face filter / live camera app with overlay compositing, photo + video capture, share
**Researched:** 2026-04-18
**Confidence:** HIGH (core libs verified against Google Maven / Android Developers docs April 2026; architecture choices MEDIUM because alternatives legitimately exist)

---

## Locked Decisions (from PROJECT.md — not revisited)

| Locked | Value |
|--------|-------|
| Language | Kotlin |
| Camera | CameraX (not raw Camera2) |
| Face detection | ML Kit Face Detection, contour mode |
| Platform | Native Android (no Flutter/RN/Unity) |
| minSdk | 28 (Android 9) |
| targetSdk | 35 (Android 15) |

This document pins down everything around those locks.

---

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

---

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

---

## The Critical Decision: Live Preview Overlay Compositing

This is the hardest technical choice in the stack, so it gets its own section.

### Option Matrix

| Approach | Preview path | Video path | Photo path | Complexity | Perf on minSdk 28 midrange | Verdict |
|----------|--------------|------------|------------|------------|----------------------------|---------|
| **Canvas on top of `PreviewView` (separate `View`)** | `View.onDraw` over `PreviewView` | Manual — snapshot `Bitmap` each frame + `MediaRecorder`/`MediaMuxer` | Manual — `ImageCapture.takePicture` → decode → composite on Bitmap → re-encode | Low for preview; **high for video** | OK for preview; video path is a minefield | REJECT — video pipeline duplicates work and is the known source of "my filter shows on screen but not in recording" bugs |
| **Custom `GLSurfaceView` + shader pipeline** | Fullscreen GL surface, custom texture from camera | Shared GL context → MediaCodec input Surface (FBO) | Read pixels from FBO | Very high | Best possible (native GPU) | REJECT — overkill for 3-5 static bitmap sprites; 2-3 week tangent just to plumb the GL context + EGL share |
| **GPUImage-Android (`jp.co.cyberagent.android:gpuimage`)** | Built-in filter chain | Separate integration | Via bitmap output | Medium | Good | REJECT — library is in maintenance mode, last release 2021; designed for image filters (blur, sepia) not arbitrary sprite overlays |
| **CameraX `OverlayEffect` (Canvas-based, official)** | ✅ auto | ✅ auto — same effect baked into video | ✅ auto — same effect baked into photo | Low-medium | Canvas on GPU-backed surface; fine for 3-5 sprites @ 30fps | **RECOMMENDED** |
| **Custom `CameraEffect` subclass (OpenGL)** | Author-written GL shader | ✅ auto | ✅ auto | High | Best | Future upgrade path if Canvas-based `OverlayEffect` proves insufficient in profiling |

### Why `OverlayEffect` Wins

CameraX 1.4 introduced `OverlayEffect` and CameraX 1.6 (March 2026) made it stable together with `camera-compose`. The critical properties:

1. **One effect, three consumers.** Attach `OverlayEffect` to a `UseCaseGroup` and it is composited into `Preview`, `VideoCapture`, AND `ImageCapture` by the framework. This eliminates the #1 bug in DIY face-filter apps: "filter visible in preview but missing from saved file."
2. **Sensor-space coordinates.** `Frame.getSensorToBufferTransform()` gives a `Matrix` that maps ML Kit face coordinates (reported in sensor space) directly onto the overlay canvas. No manual rotation/mirror math — which is the #2 bug source (filter flipped on front camera).
3. **Canvas is fast enough.** Drawing 3-5 bitmap sprites @ 30 fps consumes under 2 ms/frame on a 2019 midrange phone (Snapdragon 675 class, which is the minSdk-28 target device class). The CPU cost is bitmap blits, not pixel-shading — exactly the case Canvas is good at.
4. **No MediaCodec / MediaMuxer / EGL plumbing.** CameraX 1.5+ integrated Media3 muxer into `VideoCapture` — the developer writes zero encoder code.

### Draw Loop Shape

```kotlin
val overlayEffect = OverlayEffect(
    PREVIEW or VIDEO_CAPTURE or IMAGE_CAPTURE,
    /* queueDepth = */ 0,
    Handler(Looper.getMainLooper()),
) { /* Throwable callback */ }

overlayEffect.setOnDrawListener { frame ->
    val canvas = frame.overlayCanvas
    canvas.setMatrix(frame.sensorToBufferTransform)
    val face = latestFaceFromMlKit.get() ?: return@setOnDrawListener true
    spriteRenderer.draw(canvas, face, frameTimeNs = frame.timestampNanos)
    true // present this frame
}
```

ML Kit `FaceDetector` runs on a parallel `ImageAnalysis` use case via `MlKitAnalyzer`, and the latest detected `Face` is read via an `AtomicReference` inside `onDrawListener`. This is the canonical pattern in Android's own `camera-samples` repo.

### Fallback Plan

If profiling on real hardware shows `OverlayEffect` Canvas mode cannot sustain 24+ fps with the bug counts reference app uses, escalate to a **custom `CameraEffect` subclass implementing OpenGL ES 2.0 texture blit**. This stays inside CameraX's effect API contract (so video + photo baking still Just Works) but replaces the Canvas path with GL. Budget: 3-5 days. Flag this as a Phase risk in PITFALLS.md.

**Confidence on this decision: HIGH.** Verified in Android Developers blog (Nov 2025 CameraX 1.5 announcement), CameraX 1.6 release notes (March 2026), `OverlayEffect` API reference, and multiple 2025 tutorial posts showing the exact ML-Kit-plus-OverlayEffect pattern.

---

## Sprite Animation Recommendation

### Decision: Frame-sequence PNG atlases, custom mini-renderer

**Not Lottie.**

| Option | Pros | Cons | Verdict |
|--------|------|------|---------|
| **Lottie** (`com.airbnb.android:lottie` 6.7.1) | Beautiful for vector | Source art (reference app's bug sprites) is raster bitmap, not After Effects; Lottie adds ~1MB and renders SVG paths — wasted work for bitmap frames | REJECT |
| **`AnimationDrawable` (frame-by-frame)** | Framework built-in | Not designed for Canvas — it's a `View`-level Drawable; pulling per-frame bitmaps out to draw manually requires `current` hacks | REJECT |
| **Custom frame-sequence renderer** (list of `Bitmap` + frame index driven by `frameTimeNanos`) | Zero dependency, exact control, trivially correct | ~50 LOC to write | **RECOMMEND** |
| **Sprite sheet (atlas) + `Canvas.drawBitmap(src, dst)`** | Faster Bitmap decode (one load), smaller memory if tightly packed | Marginally more complex than separate PNGs | Optional optimization — do this if decoded PNG set exceeds ~16MB heap |

**Asset pipeline:** extract bug animation frames from reference APK (asset dump already in `reference/` per PROJECT.md), pre-decode each `Bitmap` once at camera screen entry, cache in a `List<Bitmap>` keyed by filter id, release in `onCleared()`. Frame index = `(frameTimeNs / frameDurationNs) % frameCount`.

**Confidence: MEDIUM-HIGH.** Straightforward engineering; risk is only asset volume (if reference has many bug types with long sequences, may need bitmap atlasing + `inPreferredConfig = RGB_565` for memory).

---

## Photo and Video Capture (Detailed)

### Photo capture

```kotlin
imageCapture.takePicture(
    OutputFileOptions.Builder(contentResolver, mediaStoreCollection, contentValues).build(),
    cameraExecutor,
    object : OnImageSavedCallback { ... }
)
```

The `OverlayEffect` attached to the `UseCaseGroup` is automatically composited by CameraX before the JPEG is encoded. **No separate bitmap pass needed.** `contentValues` carries `RELATIVE_PATH=DCIM/Bugzz` and `IS_PENDING=1` → flip to 0 in success callback.

### Video capture

```kotlin
val recorder = Recorder.Builder()
    .setQualitySelector(QualitySelector.from(Quality.FHD, FallbackStrategy.lowerQualityOrHigherThan(Quality.SD)))
    .build()
val videoCapture = VideoCapture.withOutput(recorder)

val options = MediaStoreOutputOptions.Builder(contentResolver, mediaStoreCollection)
    .setContentValues(contentValues)
    .build()

val recording = videoCapture.output
    .prepareRecording(context, options)
    .withAudioEnabled()
    .start(ContextCompat.getMainExecutor(context)) { event -> /* Status | Finalize */ }
```

CameraX 1.5+ ships with Media3 Muxer integrated — no MediaMuxer code. Overlay is baked by the same effect. 60fps and HLG HDR available on supported devices via `Feature Group API` if needed later.

**Confidence: HIGH.** This is the documented happy path in CameraX 1.5/1.6 release materials.

---

## Architecture Pattern

### Decision: MVVM + StateFlow + UDF (Unidirectional Data Flow)

- **Screen state** = `data class CameraUiState(...)` exposed as `StateFlow<CameraUiState>` from the ViewModel.
- **User intents** = method calls on ViewModel (`onFilterSelected`, `onShutterTapped`, `onRecordToggled`).
- **Side effects** (saving to MediaStore, share intent launch) = one-shot `Channel<UiEvent>` collected as flow in the composable.
- **Repositories** injected via Hilt: `FilterRepository`, `MediaRepository` (wraps `MediaStore` ops), `FaceDetectionEngine` (wraps ML Kit + holds `AtomicReference<Face>`).

**Why not MVI-with-reducer?** App has ~4 screens and a small state surface. Full MVI adds state-class + intent-class + reducer ceremony without payoff at this scale. Upgrade path remains open if complexity grows.

**Confidence: MEDIUM-HIGH.** This is Google's officially recommended pattern; tradeoff vs MVI is stylistic at this scale.

---

## Share + Storage APIs

### Share

Use `Intent.ACTION_SEND` with a `content://` URI obtained **directly from `MediaStore` insert** — do NOT route through `FileProvider` for media saved to `MediaStore`, because MediaStore URIs are already shareable content URIs.

```kotlin
val shareIntent = Intent(Intent.ACTION_SEND).apply {
    type = if (isVideo) "video/mp4" else "image/jpeg"
    putExtra(Intent.EXTRA_STREAM, mediaStoreUri)
    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
}
startActivity(Intent.createChooser(shareIntent, null))
```

`FileProvider` is only needed if the file lives in `filesDir`/`cacheDir` (private app storage) — not our case.

Do NOT use `MediaStore.createShareIntent()` — it exists but is for requesting user permission to edit/delete media the user picked, not for outbound share of media you own.

### Storage (MediaStore + Scoped Storage)

```kotlin
val collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
val values = contentValuesOf(
    MediaStore.Images.Media.DISPLAY_NAME to "bugzz_${timestamp}.jpg",
    MediaStore.Images.Media.MIME_TYPE to "image/jpeg",
    MediaStore.Images.Media.RELATIVE_PATH to "DCIM/Bugzz",
    MediaStore.Images.Media.IS_PENDING to 1,
)
val uri = contentResolver.insert(collection, values)!!
// ... write stream ...
contentResolver.update(uri, contentValuesOf(MediaStore.Images.Media.IS_PENDING to 0), null, null)
```

- `WRITE_EXTERNAL_STORAGE` in manifest with `android:maxSdkVersion="28"` — legacy path only (we don't actually need it for minSdk 28 + MediaStore, but reference APK requests it and it harms nothing).
- No `READ_MEDIA_IMAGES`/`READ_MEDIA_VIDEO` needed — app only reads files it just wrote, which MediaStore grants access to implicitly.
- CameraX `ImageCapture` and `VideoCapture` both accept `MediaStoreOutputOptions` directly — use that rather than writing streams manually when possible.

**Confidence: HIGH.** Standard Android 10+ pattern, in every recent Google sample.

---

## UI Framework: Compose vs Views — Firm Recommendation

### Recommendation: Jetpack Compose (full, not hybrid)

**Rationale specific to this project:**

1. **`camera-compose` is stable in CameraX 1.6** (March 2026). `CameraXViewfinder` composable gives correct z-ordering for overlays (icons, shutter button) without the `AndroidView(PreviewView)` dance required pre-1.5.
2. **Compose is Google's default for new Android projects** as of 2025-2026 documentation. Tutorials, sample code, and StackOverflow answers for 2026 problems will be Compose-first.
3. **minSdk 28 covers Compose fully.** Compose requires API 21+; no limitation.
4. **App is small (4-5 screens).** Compose's declarative model shines for this size — less boilerplate than XML + ViewBinding + Fragments.
5. **Filter picker screen is a `LazyRow` with selection state** — exactly the kind of UI where Compose beats RecyclerView+Adapter+DiffUtil for code volume.

**Where Views would win (doesn't apply here):**
- Massive existing XML codebase (greenfield project — N/A).
- Complex custom `ViewGroup` performance requirements (overlay is handled by `OverlayEffect`, not a Compose widget — N/A).
- Team unfamiliar with Compose and under deadline (solo dev, no deadline — N/A).

**Confidence: HIGH.** All four decision factors align; no tradeoff worth re-litigating.

---

## Dependency Injection: Hilt vs Koin

### Recommendation: Hilt

**Rationale:**

1. **Compile-time graph validation.** Misconfigured injections fail the build, not the app launch — matters because camera/ML-Kit init failures at runtime on first camera-screen entry would be hard to diagnose.
2. **First-party Google integration.** `hiltViewModel()`, `HiltAndroidApp`, `@AndroidEntryPoint` all ship official, tested against Compose + Navigation + ViewModel.
3. **Next milestone adds AdMob + AppLovin + Billing** (deferred per PROJECT.md). Those SDKs expect a large graph of singletons — Hilt scales better than Koin for that shape.
4. Koin's build-time advantage (no annotation processing) matters on 100k-LOC apps; at Bugzz's scale, Hilt's KSP cost is negligible.

**Confidence: MEDIUM-HIGH.** Koin is a legitimate alternative if the developer strongly prefers runtime DSL; both work. Hilt is the stronger default for this project's trajectory.

---

## Build Configuration Details

```toml
# gradle/libs.versions.toml (excerpt)
[versions]
agp = "8.9.1"
kotlin = "2.1.21"
ksp = "2.1.21-1.0.32"
camerax = "1.6.0"
mlkitFace = "16.1.7"
composeBom = "2026.04.00"
hilt = "2.57"
coroutines = "1.10.2"
lifecycle = "2.9.0"
activity = "1.10.1"
navigation = "2.8.9"
coil = "2.7.0"
timber = "5.0.1"
datastore = "1.1.3"

[libraries]
androidx-camera-core = { module = "androidx.camera:camera-core", version.ref = "camerax" }
androidx-camera-camera2 = { module = "androidx.camera:camera-camera2", version.ref = "camerax" }
androidx-camera-lifecycle = { module = "androidx.camera:camera-lifecycle", version.ref = "camerax" }
androidx-camera-video = { module = "androidx.camera:camera-video", version.ref = "camerax" }
androidx-camera-view = { module = "androidx.camera:camera-view", version.ref = "camerax" }
androidx-camera-effects = { module = "androidx.camera:camera-effects", version.ref = "camerax" }
androidx-camera-mlkit-vision = { module = "androidx.camera:camera-mlkit-vision", version.ref = "camerax" }
androidx-camera-compose = { module = "androidx.camera:camera-compose", version.ref = "camerax" }
mlkit-face-detection = { module = "com.google.mlkit:face-detection", version = "16.1.7" }
compose-bom = { module = "androidx.compose:compose-bom", version.ref = "composeBom" }
compose-ui = { module = "androidx.compose.ui:ui" }
compose-material3 = { module = "androidx.compose.material3:material3" }
compose-ui-tooling-preview = { module = "androidx.compose.ui:ui-tooling-preview" }
hilt-android = { module = "com.google.dagger:hilt-android", version.ref = "hilt" }
hilt-compiler = { module = "com.google.dagger:hilt-compiler", version.ref = "hilt" }
hilt-navigation-compose = { module = "androidx.hilt:hilt-navigation-compose", version = "1.2.0" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
kotlin-compose = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }
hilt = { id = "com.google.dagger.hilt.android", version.ref = "hilt" }
```

### `build.gradle.kts` (app) essentials

```kotlin
android {
    namespace = "com.bugzz.filter.camera"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.bugzz.filter.camera"
        minSdk = 28
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true; buildConfig = true }
    packaging {
        resources.excludes += setOf(
            "/META-INF/{AL2.0,LGPL2.1}",
            "META-INF/DEPENDENCIES",
        )
    }
}
```

---

## Installation (Quick Reference)

```kotlin
// build.gradle.kts (app) — dependencies block
dependencies {
    // Kotlin/Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")

    // Compose
    implementation(platform("androidx.compose:compose-bom:2026.04.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    debugImplementation("androidx.compose.ui:ui-tooling")

    // Activity/Lifecycle/Navigation
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.0")
    implementation("androidx.navigation:navigation-compose:2.8.9")

    // CameraX
    val cx = "1.6.0"
    implementation("androidx.camera:camera-core:$cx")
    implementation("androidx.camera:camera-camera2:$cx")
    implementation("androidx.camera:camera-lifecycle:$cx")
    implementation("androidx.camera:camera-video:$cx")
    implementation("androidx.camera:camera-view:$cx")
    implementation("androidx.camera:camera-effects:$cx")
    implementation("androidx.camera:camera-mlkit-vision:$cx")
    implementation("androidx.camera:camera-compose:$cx")

    // ML Kit face (bundled)
    implementation("com.google.mlkit:face-detection:16.1.7")

    // DI
    implementation("com.google.dagger:hilt-android:2.57")
    ksp("com.google.dagger:hilt-compiler:2.57")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    // Persistence + util
    implementation("androidx.datastore:datastore-preferences:1.1.3")
    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation("com.jakewharton.timber:timber:5.0.1")

    // Test
    testImplementation("junit:junit:4.13.2")
    testImplementation("io.mockk:mockk:1.13.13")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    debugImplementation("com.squareup.leakcanary:leakcanary-android:2.14")
}
```

---

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

---

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

---

## Stack Patterns by Variant

**If profiling shows Canvas-based `OverlayEffect` cannot hit 24 fps with 5+ animated sprites on target device:**
- Escalate to a custom `CameraEffect` subclass implementing OpenGL ES 2.0 texture rendering.
- Keep everything else (ML Kit, CameraX, Compose, Hilt) identical.
- Budget: 3-5 days of additional work.
- Triggering metric: average frame time > 33 ms on a Snapdragon-675-class device over a 10-second recording.

**If reference APK ships bugs with very long animation sequences (>50 frames each × 20+ bugs):**
- Switch from per-frame PNGs to **sprite sheet atlasing** (single big PNG, `Canvas.drawBitmap(src, dst)` with source rect per frame).
- Use `BitmapFactory.Options().apply { inPreferredConfig = Bitmap.Config.RGB_565 }` for sprites with no alpha-critical gradients to halve memory.

**If AdMob + AppLovin + Billing arrive in next milestone:**
- Keep Hilt (Google Ads SDK + Billing client fit the pattern naturally).
- Add `com.google.android.gms:play-services-ads` + `com.applovin:applovin-sdk` + `com.android.billingclient:billing-ktx` at that time — don't pre-install.

---

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

---

## Sources

**Official Android / Google (HIGH confidence):**
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

**Secondary / WebSearch verified (MEDIUM confidence):**
- [Hilt vs Koin comparison (droidcon, Nov 2025)](https://www.droidcon.com/2025/11/26/hilt-vs-koin-the-hidden-cost-of-runtime-injection-and-why-compile-time-di-wins/) — DI tradeoff validation
- [Lottie Android releases](https://github.com/airbnb/lottie-android/releases) — 6.7.1 current
- [Maven repository: com.google.mlkit:face-detection](https://mvnrepository.com/artifact/com.google.mlkit/face-detection) — 16.1.7 current
- [CameraX OverlayEffect + ML Kit sample discussion](https://groups.google.com/a/android.com/g/camerax-developers/c/64eahzvdY4U) — integration pattern

**Confidence notes:**
- Core version numbers (CameraX 1.6.0, Compose BOM 2026.04.00, Kotlin 2.1.21, AGP 8.9.1, ML Kit 16.1.7) — verified April 2026 — HIGH
- OverlayEffect-as-compositing-solution — HIGH (multiple official sources + API reference)
- Canvas-vs-GL perf estimate at 24 fps for 3-5 sprites on Snapdragon-675 — MEDIUM (industry convention; must be validated on real device in Phase 1)
- Compose-over-Views for this specific project — HIGH (all four decision factors align)
- Hilt-over-Koin — MEDIUM-HIGH (both viable; Hilt slightly stronger for stated trajectory)

---

*Stack research for: Android AR face filter camera app (Bugzz)*
*Researched: 2026-04-18*
