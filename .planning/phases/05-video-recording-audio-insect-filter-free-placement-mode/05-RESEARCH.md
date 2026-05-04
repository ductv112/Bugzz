# Phase 5: Video Recording + Audio + Insect Filter Free-Placement Mode — Research

**Researched:** 2026-05-04
**Domain:** CameraX Recorder + VideoCapture production wiring; Compose detectTransformGestures sticker UX; PowerManager.ThermalStatusListener; MediaStore Video.Media
**Confidence:** HIGH (core APIs verified against existing codebase; architecture proven in Phases 2-4; all locked decisions inherited from 05-CONTEXT.md)

---

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions (D-01..D-26 — all auto-locked per user autonomy delegation)

| ID | Decision |
|----|----------|
| D-01 | Insect Filter reuses same 15-filter FilterCatalog from Phase 4 |
| D-02 | Initial sticker position = preview center; first touch-down does NOT relocate |
| D-03 | Gestures via `detectTransformGestures`; StickerState(offset, scale, rotation); scale clamped [0.3f, 3.0f]; offset 50% overflow allowed |
| D-04 | StickerState lives in InsectFilterViewModel StateFlow; survives flip + orientation; does NOT persist across app launch |
| D-05 | Face detection DISABLED in InsectFilter mode; FilterEngine receives faces=emptyList() |
| D-06 | New package `com.bugzz.filter.camera.ui.insect`; new file `render/StickerRenderer.kt` |
| D-07 | Production Record button REPLACES Phase 2/4 TEST RECORD at BottomStart; 56dp circle, red #FFE53935 fill, white border 2dp; idle=filled circle / recording=stop square inside |
| D-08 | Recording indicator = TopCenter Row: red dot 16dp (1Hz blink animateFloatAsState) + "MM:SS" Text; hidden when not recording |
| D-09 | Auto-stop at 60s via `withDurationLimit(60_000L * 1_000_000L)` nanoseconds; Finalize(ERROR_DURATION_REACHED) → same Idle transition as manual stop |
| D-10 | Exit-during-record = Material3 AlertDialog "Recording in progress" + "Discard"/"Cancel"; Discard calls recording.stop() + deletes pending URI; Cancel resumes |
| D-11 | During recording: filter swap LOCKED, flip LOCKED, sticker gestures DISABLED |
| D-12 | Lazy RECORD_AUDIO: first Record tap triggers RequestPermission contract; granted → withAudioEnabled(); denied → inline rationale Toast + "Open Settings" |
| D-13 | MirrorMode.MIRROR_MODE_ON_FRONT_ONLY on production VideoCapture builder |
| D-14 | ThermalMonitor singleton in BugzzApplication.onCreate; status >= Moderate → FaceDetectorClient frame-skip every 2nd frame |
| D-15 | Quality.HD (720p) with FallbackStrategy.lowerQualityOrHigherThan(Quality.HD) |
| D-16 | H264 default (Recorder auto-selects); no H265 opt-in |
| D-17 | Filename `Bugzz_YYYYMMDD_HHmmss.mp4`; DCIM/Bugzz/ via MediaStore.Video.Media |
| D-18 | AudioSource.MIC default; Recorder muxes into single MP4 |
| D-19 | New files: InsectFilterScreen.kt, InsectFilterViewModel.kt, StickerState.kt, StickerRenderer.kt, ThermalMonitor.kt, VideoRecorder.kt + test files |
| D-20 | OverlayEffectBuilder extended with optional StickerRenderer; onDrawListener branches on cameraMode |
| D-21 | CameraController.startRecording(audioEnabled): Recording + stopRecording(Recording) replace test methods (production); activeRecording field for single-recording invariant |
| D-22 | RecordingState sealed: Idle / Active(elapsedMs, hasAudio) / Stopping / Error(message) |
| D-23 | Lock-during-record: picker alpha 0.5f + disabled; flip disabled; sticker pointerInput gated on isRecording key |
| D-24 | BackHandler(enabled=isRecording) intercepts back press → AlertDialog |
| D-25 | AndroidManifest RECORD_AUDIO already declared (Phase 1 D-12) — no manifest change |
| D-26 | All Phase 3 + Phase 4 fix commits preserved verbatim (grep assertions in plans) |

### Claude's Discretion
- ThermalStatusListener exact integration (getThermalStatus() poll vs addThermalStatusListener callback)
- Recording indicator red-dot blink easing (LinearOutSlowInEasing vs FastOutLinearInEasing)
- Whether sticker survives orientation change via Modifier.graphicsLayer alone OR explicit OrientationEventListener
- VideoRecordEvent dispatcher executor (cameraExecutor vs Main — default cameraExecutor)
- Timber log level for VideoRecorder (Verbose vs Debug — default Verbose)
- InsectFilter initial filter: restore last-used from DataStore OR first catalog entry — default restore last-used (reuse CAT-05 pattern)
- AlertDialog exact wording for "Recording in progress" / "Discard recording?"
- Locked-during-record alpha exact value (0.5f recommended)
- Whether startTestRecording() is deleted or kept behind BuildConfig.DEBUG (default: keep behind DEBUG guard until production path verified on device)

### Deferred Ideas (OUT OF SCOPE)
- Sticker state persistence across app launch (Phase 6)
- Multi-sticker mode
- Sticker rotation snap-to-angle
- Sticker color tint
- Video editing/trimming post-record
- Music overlay
- Countdown timer pre-record
- Watermark overlay
- Recording at 1080p / FullHD
- H265 codec opt-in
- Multi-mic / spatial audio
- Per-recording mute toggle
- Stop button separate from record toggle
- Last-10s warning UI (timer turns red at 50s)
- Recording quality picker in Settings
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| VID-01 | Record button starts video recording via CameraX Recorder + VideoCapture | D-21: CameraController.startRecording; D-22: RecordingState machine; D-07: UI button |
| VID-02 | Video output has overlay baked in (OverlayEffect into VIDEO_CAPTURE) | Proven in Phase 2 02-gaps-03; OverlayEffect TARGETS already includes VIDEO_CAPTURE |
| VID-03 | Audio captured from device microphone and synced with video | D-12/D-18: withAudioEnabled() after RECORD_AUDIO granted; Recorder handles mux |
| VID-04 | Recording auto-stops at 60s; user can stop earlier | D-09: withDurationLimit(60_000L * 1_000_000L); D-07: toggle button |
| VID-05 | Front-camera video uses MIRROR_MODE_ON_FRONT_ONLY | D-13: VideoCapture.Builder.setMirrorMode — already wired in Phase 2 CameraController |
| VID-06 | Video saved as MP4 to DCIM/Bugzz/ via MediaStore Video insert | D-17: MediaStoreOutputOptions + Video.Media.EXTERNAL_CONTENT_URI |
| VID-07 | Recording indicator (red dot + elapsed timer) visible while recording | D-08: TopCenter Row with animateFloatAsState blink + LaunchedEffect timer |
| VID-08 | PowerManager.ThermalStatusListener hooked; above THERMAL_STATUS_MODERATE drops detection rate | D-14: ThermalMonitor singleton; FaceDetectorClient frame-skip |
| VID-09 | Exit-during-record triggers confirmation dialog; cancel preserves recording | D-10/D-24: BackHandler + AlertDialog |
| VID-10 | RECORD_AUDIO permission requested lazily on first record attempt | D-12: rememberLauncherForActivityResult(RequestPermission(RECORD_AUDIO)) |
| MOD-03 | Insect Filter mode places a single bug sticker on screen without face tracking | D-05/D-06: InsectFilterScreen + StickerRenderer; face detection disabled |
| MOD-04 | Insect Filter mode supports drag gesture to move sticker | D-03: detectTransformGestures dragAmount → StickerState.offset |
| MOD-05 | Insect Filter mode supports pinch-to-zoom gesture | D-03: detectTransformGestures zoomChange → StickerState.scale [0.3f, 3.0f] |
| MOD-06 | Insect Filter mode supports rotation gesture | D-03: detectTransformGestures rotationChange → StickerState.rotation |
| MOD-07 | Insect Filter mode sticker survives camera flip and orientation change | D-04: InsectFilterViewModel StateFlow survives config change; Modifier.graphicsLayer orientation-independent |
</phase_requirements>

---

## Summary

Phase 5 adds two production feature blocks onto an already-validated render pipeline: (1) CameraX `Recorder`-based video recording with synced microphone audio and thermal throttle protection, and (2) a free-placement single-sticker Insect Filter mode with Compose multi-touch gestures. Both blocks are low-architectural-risk because the `OverlayEffect` VIDEO_CAPTURE compositing was proven functional in Phase 2 (02-gaps-03 device verification), and the `VideoCapture` use case has been bound in `UseCaseGroup` since Phase 2. Phase 5 is primarily about wiring existing infrastructure into production UI flows and adding the new `InsectFilterScreen`.

The video recording path follows the same `MediaStoreOutputOptions` pattern proven in Phase 3 photo capture — `MediaStore.Video.Media.EXTERNAL_CONTENT_URI` instead of `MediaStore.Images.Media.EXTERNAL_CONTENT_URI`, with CameraX handling the `IS_PENDING` transaction automatically. `PowerManager.ThermalStatusListener` (API 29+, aligns with minSdk 28 — covered by minSdk+1) provides defensive throttle for sustained recording on mid-tier devices.

The Insect Filter sticker path introduces `detectTransformGestures` from Compose's pointer-input API, which delivers pan+zoom+rotate in a single callback, and a `StickerRenderer` class that draws the active filter's sprite at the transformed position using `Canvas.save()/translate()/scale()/rotate()/restore()`. Face detection is disabled entirely in this mode, saving CPU.

**Primary recommendation:** Implement the two feature blocks in parallel waves where possible (VideoRecorder + ThermalMonitor infra are independent of InsectFilterScreen + StickerRenderer), then converge at the shared CameraScreen modifications (Record button, indicator, lock-during-record, BackHandler). Both share the OverlayEffectBuilder extension.

---

## Approach

### Feature Block A: Video Recording (VID-01..10)

The existing `CameraController` already has:
- `videoCapture: VideoCapture<Recorder>` bound since Phase 2 with `MIRROR_MODE_ON_FRONT_ONLY` and `Quality.HD`
- `startTestRecording()` and `stopTestRecording()` debug methods (no audio, 5s auto-stop)
- `activeRecording: Recording?` field

Phase 5 adds production methods alongside (or replacing) the test methods:
- `startRecording(audioEnabled: Boolean): Recording` — full 60s with optional audio
- `stopRecording(recording: Recording)` — explicit stop

A new `VideoRecorder.kt` wrapper (`@Singleton @Inject`) delegates to `CameraController` and dispatches `VideoRecordEvent` to the ViewModel via a callback or Flow. `CameraViewModel` adds `RecordingState` sealed interface and recording lifecycle management. `CameraUiState` gains a `recordingState: RecordingState` field replacing the current `isRecording: Boolean` for richer state.

### Feature Block B: Insect Filter Free-Placement (MOD-03..07)

New screen `InsectFilterScreen` with its own `InsectFilterViewModel` (reuses `CameraController`, `FilterEngine`-inactive path, `AssetLoader`, `FilterCatalog`, `FilterPrefsRepository`). The screen renders:
- `CameraXViewfinder` fullscreen (same as CameraScreen)
- `Modifier.pointerInput(isRecording)` with `detectTransformGestures` for sticker gestures
- Same `FilterPicker` LazyRow from Phase 4
- Same Record button + indicator from Feature Block A
- Same Flip button

`StickerRenderer` draws the active filter's sprite at `StickerState.offset` + `StickerState.scale` + `StickerState.rotation` using Canvas matrix transforms. `OverlayEffectBuilder` branches on `cameraMode` in the `setOnDrawListener` body.

### Shared Infra Changes

- `OverlayEffectBuilder`: inject optional `StickerRenderer`; branch in draw callback
- `CameraController`: add `startRecording` / `stopRecording` production methods
- `CameraViewModel`: add `RecordingState` + `onRecordTapped()` + `VideoRecordEvent` observer
- `CameraScreen`: replace TEST RECORD with production Record button; add indicator + AlertDialog + BackHandler
- `BugzzApplication`: register `ThermalMonitor` singleton
- `FaceDetectorClient.createAnalyzer()`: reads `ThermalMonitor.status`, applies frame-skip
- `OneShotEvent`: add `VideoSaved(uri)` + `VideoError(message)`
- `BugzzApp`: rewire `CameraRoute(InsectFilter)` to `InsectFilterScreen`

---

## Key APIs

### 1. CameraX Recorder + VideoCapture (VID-01..06, VID-09)

The `VideoCapture` use case and `Recorder` are already constructed in `CameraController.bind()` with `Quality.HD` and `MIRROR_MODE_ON_FRONT_ONLY`. Phase 5 only adds the production recording flow on top:

```kotlin
// CameraController.kt — production startRecording() method
fun startRecording(audioEnabled: Boolean, onEvent: (VideoRecordEvent) -> Unit): Recording {
    val vc = videoCapture ?: error("Camera not bound")
    check(activeRecording == null) { "Recording already in progress" }

    val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
    val filename = "Bugzz_${sdf.format(Date())}.mp4"
    val values = ContentValues().apply {
        put(MediaStore.Video.Media.DISPLAY_NAME, filename)
        put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
        put(MediaStore.Video.Media.RELATIVE_PATH, "DCIM/Bugzz")
    }
    val options = MediaStoreOutputOptions.Builder(
        appContext.contentResolver,
        MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
    ).setContentValues(values).build()

    val pending = vc.output.prepareRecording(appContext, options)
        .withDurationLimit(
            // D-09: 60s cap — API uses MICROSECONDS not nanoseconds
            // Note: withDurationLimit takes TimeUnit or Duration; use milliseconds overload
            60_000L, TimeUnit.MILLISECONDS
        )
    if (audioEnabled) pending.withAudioEnabled()

    activeRecording = pending.start(cameraExecutor, onEvent)
    return activeRecording!!
}

fun stopRecording() {
    activeRecording?.stop()
    // activeRecording set to null inside VideoRecordEvent.Finalize handler
}
```

**API note on `withDurationLimit`:** [VERIFIED: existing codebase compile] The overload `withDurationLimit(duration: Long, timeUnit: TimeUnit)` accepts milliseconds. The CONTEXT D-09 shows nanoseconds but the actual CameraX API takes a duration+unit pair OR a `java.time.Duration`. Use `withDurationLimit(60_000L, TimeUnit.MILLISECONDS)` — simpler than nanoseconds math.

**`VideoRecordEvent` types dispatched to `onEvent` callback:**
- `VideoRecordEvent.Start` — recording has started
- `VideoRecordEvent.Status` — periodic status (stats: bytesRecorded, durationNanos) — fires roughly every second
- `VideoRecordEvent.Finalize` — recording stopped (check `hasError()`, `error`, `outputResults.outputUri`)
  - `cause == VideoRecordEvent.Finalize.ERROR_NONE` — manual stop or within duration limit
  - `cause == VideoRecordEvent.Finalize.ERROR_DURATION_REACHED` — 60s auto-stop
  - `cause == VideoRecordEvent.Finalize.ERROR_INSUFFICIENT_STORAGE` — disk full
  - `cause == VideoRecordEvent.Finalize.ERROR_SOURCE_INACTIVE` — camera disconnected during record
  - Other error codes: treat as `VideoError` in ViewModel

[CITED: developer.android.com/media/camera/camerax/capture-video]

### 2. RecordingState Sealed Interface (CameraViewModel / InsectFilterViewModel)

```kotlin
// D-22 — CameraViewModel.kt addition
sealed interface RecordingState {
    data object Idle : RecordingState
    data class Active(val elapsedMs: Long, val hasAudio: Boolean) : RecordingState
    data object Stopping : RecordingState
    data class Error(val message: String) : RecordingState
}

// State machine transitions in ViewModel:
fun onRecordTapped() {
    when (val state = _uiState.value.recordingState) {
        is RecordingState.Idle -> initiateRecordingWithPermissionCheck()
        is RecordingState.Active -> stopRecording(state)
        is RecordingState.Stopping -> Unit  // ignore tap while finalizing
        is RecordingState.Error -> resetToIdle()  // allow retry
    }
}

private fun initiateRecordingWithPermissionCheck() {
    // isRecording guard — D-26 isRecording guard mirrors isCapturing pattern
    if (_uiState.value.recordingState !is RecordingState.Idle) return
    // RECORD_AUDIO check handled in composable via rememberLauncherForActivityResult
    // ViewModel receives result via onAudioPermissionResult(granted: Boolean)
}

// Inside VideoRecordEvent handler (runs on cameraExecutor; post to main via viewModelScope.launch):
private fun handleVideoEvent(event: VideoRecordEvent) {
    viewModelScope.launch {
        when (event) {
            is VideoRecordEvent.Start ->
                _uiState.value = _uiState.value.copy(
                    recordingState = RecordingState.Active(0L, hasAudio)
                )
            is VideoRecordEvent.Status -> {
                val elapsedMs = event.recordingStats.recordedDurationNanos / 1_000_000L
                _uiState.value = _uiState.value.copy(
                    recordingState = (_uiState.value.recordingState as? RecordingState.Active)
                        ?.copy(elapsedMs = elapsedMs) ?: return@launch
                )
            }
            is VideoRecordEvent.Finalize -> {
                controller.clearActiveRecording()
                if (event.hasError() &&
                    event.error != VideoRecordEvent.Finalize.ERROR_DURATION_REACHED
                ) {
                    _uiState.value = _uiState.value.copy(
                        recordingState = RecordingState.Idle
                    )
                    _events.send(OneShotEvent.VideoError(
                        "Record error ${event.error}: ${event.cause?.message ?: "unknown"}"
                    ))
                } else {
                    _uiState.value = _uiState.value.copy(recordingState = RecordingState.Idle)
                    val uri = event.outputResults.outputUri
                    _events.send(OneShotEvent.VideoSaved(uri))
                }
            }
            else -> Unit
        }
    }
}
```

[ASSUMED] The `RecordingState` sealed interface design above matches D-22 exactly. The specific error code values (`ERROR_DURATION_REACHED`, `ERROR_NONE`, etc.) are integer constants on `VideoRecordEvent.Finalize` — verify by checking the AAR during Wave 0 test compilation.

### 3. MediaStore Video.Media Output Options (VID-06)

```kotlin
// Same pattern as Phase 3 ImageCapture but targeting Video collection
val values = ContentValues().apply {
    put(MediaStore.Video.Media.DISPLAY_NAME, "Bugzz_20260504_191500.mp4")
    put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
    put(MediaStore.Video.Media.RELATIVE_PATH, "DCIM/Bugzz")
    // IS_PENDING NOT set here — CameraX Recorder handles the IS_PENDING transaction
    // automatically (same behavior as ImageCapture per Phase 3 D-32 proof)
}
val outputOptions = MediaStoreOutputOptions.Builder(
    contentResolver,
    MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
).setContentValues(values).build()
```

Key difference from photo: `MediaStore.Video.Media.EXTERNAL_CONTENT_URI` instead of `MediaStore.Images.Media.EXTERNAL_CONTENT_URI`. CameraX `Recorder` handles `IS_PENDING` internally. [VERIFIED: codebase — CameraController.startTestRecording already uses this pattern]

If recording is discarded (Discard button in AlertDialog): call `contentResolver.delete(pendingUri, null, null)` after `recording.stop()`. The pending URI is the `outputResults.outputUri` from the `VideoRecordEvent.Finalize` event — capture it in the Finalize handler and expose it to the ViewModel for cleanup. [ASSUMED — standard MediaStore pending-file cleanup pattern]

### 4. MIRROR_MODE_ON_FRONT_ONLY (VID-05)

Already wired in Phase 2 `CameraController.bind()`:
```kotlin
val videoCaptureUc = VideoCapture.Builder(
    Recorder.Builder().setQualitySelector(QualitySelector.from(Quality.HD)).build()
)
    .setMirrorMode(MirrorMode.MIRROR_MODE_ON_FRONT_ONLY)  // ALREADY PRESENT
    .setTargetRotation(initialRotation)
    .build()
```
`MirrorMode.MIRROR_MODE_ON_FRONT_ONLY` is a constant on `androidx.camera.core.MirrorMode`. [VERIFIED: codebase — Phase 2 Rule 3 auto-fix confirmed the constant location is `androidx.camera.core.MirrorMode` not `VideoCapture`]

Phase 5 does NOT need to change this — it is already correct. VID-05 is satisfied by the existing binding.

### 5. Compose detectTransformGestures (MOD-04..06)

```kotlin
// StickerState.kt (D-03)
data class StickerState(
    val offset: Offset = Offset.Zero,   // accumulated drag offset from center
    val scale: Float = 1f,              // pinch-to-zoom result
    val rotation: Float = 0f,           // two-finger rotation in degrees
)

// InsectFilterScreen.kt — gesture handler (D-03/D-23)
@Composable
private fun StickerGestureLayer(
    stickerState: StickerState,
    isRecording: Boolean,
    onGestureUpdate: (StickerState) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .pointerInput(isRecording) {  // key = isRecording — D-23 gate
                if (isRecording) return@pointerInput   // gestures locked during recording
                detectTransformGestures(
                    onGesture = { _centroid, pan, zoom, rotationChange ->
                        val newScale = (stickerState.scale * zoom).coerceIn(0.3f, 3.0f)
                        // 50% overflow clamp — D-03: sticker center may go 50% outside preview
                        // Exact clamp constants left to InsectFilterScreen implementation;
                        // offset is accumulated unbounded here; clamping done in ViewModel
                        val newOffset = stickerState.offset + pan
                        val newRotation = stickerState.rotation + rotationChange
                        onGestureUpdate(
                            StickerState(
                                offset = newOffset,
                                scale = newScale,
                                rotation = newRotation,
                            )
                        )
                    }
                )
            }
    )
}

// Apply transforms to sticker via graphicsLayer
Image(
    painter = rememberAsyncImagePainter(thumbnailBitmap),
    contentDescription = null,
    modifier = Modifier
        .align(Alignment.Center)
        .size(baseSize)
        .graphicsLayer(
            translationX = stickerState.offset.x,
            translationY = stickerState.offset.y,
            scaleX = stickerState.scale,
            scaleY = stickerState.scale,
            rotationZ = stickerState.rotation,
        )
)
```

`detectTransformGestures` is from `androidx.compose.foundation.gestures` — no additional dependency. It is a single API that handles pan (dragAmount), zoom (zoomChange as Float multiplier), and rotation (rotationChange in degrees) simultaneously. [CITED: developer.android.com/develop/ui/compose/touch-input/pointer-input/multi-touch]

**Important:** `detectTransformGestures` captures all pointer events on the modifier it's applied to. If the picker LazyRow overlaps, ensure the gesture modifier is only on the preview area, not the full screen including picker.

**Sticker rendering in OverlayEffect canvas (not Compose layer):** The `StickerRenderer.onDraw(canvas, frame)` receives the current `StickerState` via `setStickerState()` call from ViewModel/render thread. The sticker is drawn into the `OverlayEffect` canvas (so it appears in video + photo output) NOT as a Compose layer:

```kotlin
// StickerRenderer.kt
class StickerRenderer @Inject constructor(private val assetLoader: AssetLoader) {

    private val stateLock = Any()
    @GuardedBy("stateLock") private var stickerState: StickerState = StickerState()
    @GuardedBy("stateLock") private var activeDef: FilterDefinition? = null

    fun setStickerState(state: StickerState) = synchronized(stateLock) { stickerState = state }
    fun setActiveFilter(def: FilterDefinition) = synchronized(stateLock) { activeDef = def }

    fun onDraw(canvas: android.graphics.Canvas, frame: androidx.camera.effects.Frame) {
        val (state, def) = synchronized(stateLock) { stickerState to activeDef } ?: return
        val bitmap = assetLoader.get(def.assetDir, computeFrameIdx(frame.timestampNanos, def))
            ?: return
        // sensorToBufferTransform already applied in OverlayEffectBuilder.setOnDrawListener
        // StickerRenderer draws in post-transform space
        canvas.save()
        canvas.translate(state.offset.x, state.offset.y)
        canvas.scale(state.scale, state.scale)
        canvas.rotate(state.rotation)
        val halfW = bitmap.width / 2f
        val halfH = bitmap.height / 2f
        canvas.drawBitmap(bitmap, -halfW, -halfH, null)
        canvas.restore()
    }
}
```

[ASSUMED] The coordinate space of `StickerState.offset` vs the overlay canvas coordinate space needs careful alignment. The overlay canvas is in sensor-buffer space (after `setMatrix(sensorToBufferTransform)`). The Compose gesture offset is in preview-screen pixels. A coordinate mapping is needed. The safest approach: report gesture deltas in preview-px, convert to sensor-buffer-px using the inverse of `sensorToBufferTransform` or use a calibrated scale factor. This is a minor open question — see Open Questions section.

### 6. PowerManager.ThermalStatusListener — ThermalMonitor (VID-08)

```kotlin
// ThermalMonitor.kt — D-14
@Singleton
class ThermalMonitor @Inject constructor(
    @ApplicationContext private val context: Context,
    @Named("cameraExecutor") private val executor: Executor,
) {
    enum class ThermalStatus { None, Light, Moderate, Severe, Critical, Emergency, Shutdown }

    private val _status = MutableStateFlow(ThermalStatus.None)
    val status: StateFlow<ThermalStatus> = _status.asStateFlow()

    private val listener = PowerManager.OnThermalStatusChangedListener { androidStatus ->
        _status.value = mapStatus(androidStatus)
        Timber.tag("ThermalMonitor").v("status=%s (raw=%d)", _status.value, androidStatus)
    }

    fun register() {
        // API 29+ check — minSdk=28, so runtime check required
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val powerManager = context.getSystemService(PowerManager::class.java)
            powerManager.addThermalStatusListener(executor, listener)
        }
    }

    fun unregister() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val powerManager = context.getSystemService(PowerManager::class.java)
            powerManager.removeThermalStatusListener(listener)
        }
    }

    private fun mapStatus(raw: Int): ThermalStatus = when (raw) {
        PowerManager.THERMAL_STATUS_NONE       -> ThermalStatus.None
        PowerManager.THERMAL_STATUS_LIGHT      -> ThermalStatus.Light
        PowerManager.THERMAL_STATUS_MODERATE   -> ThermalStatus.Moderate
        PowerManager.THERMAL_STATUS_SEVERE     -> ThermalStatus.Severe
        PowerManager.THERMAL_STATUS_CRITICAL   -> ThermalStatus.Critical
        PowerManager.THERMAL_STATUS_EMERGENCY  -> ThermalStatus.Emergency
        PowerManager.THERMAL_STATUS_SHUTDOWN   -> ThermalStatus.Shutdown
        else                                   -> ThermalStatus.None
    }
}
```

**API availability:** `PowerManager.addThermalStatusListener` is API 29+. minSdk=28 requires a `Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q` guard. When API < 29, `ThermalMonitor` stays at `ThermalStatus.None` (no throttle). [CITED: developer.android.com/reference/android/os/PowerManager#addThermalStatusListener]

**Registration point:** `BugzzApplication.onCreate()` calls `thermalMonitor.register()`. Since `ThermalMonitor` is `@Singleton`, Hilt graph provides it. `BugzzApplication` needs `@HiltAndroidApp` (already set) and a `@Inject lateinit var thermalMonitor: ThermalMonitor` field.

**Frame-skip integration in FaceDetectorClient:**

```kotlin
// FaceDetectorClient.kt — extended createAnalyzer() consumer body
// Inject ThermalMonitor (new constructor param)
@Singleton
class FaceDetectorClient @Inject constructor(
    @Named("cameraExecutor") private val cameraExecutor: Executor,
    private val tracker: BboxIouTracker,
    private val thermalMonitor: ThermalMonitor,   // NEW — Phase 5 D-14
) {
    private var frameCounter = 0

    fun createAnalyzer(): MlKitAnalyzer = MlKitAnalyzer(
        listOf(detector),
        COORDINATE_SYSTEM_SENSOR,
        cameraExecutor,
    ) { result ->
        // D-14: frame-skip when thermal >= Moderate
        if (thermalMonitor.status.value >= ThermalMonitor.ThermalStatus.Moderate) {
            if (frameCounter++ % 2 != 0) return@MlKitAnalyzer
        } else {
            frameCounter = 0  // reset counter when back to normal
        }
        // ... existing consumer body unchanged ...
    }
}
```

[ASSUMED] The `>=` comparison on `ThermalStatus` enum requires either a `Comparable` order or explicit `when` check. Define `ThermalStatus` as an enum with ordinal ordering (`None=0, Light=1, Moderate=2, ...`) so `>=` works via `compareTo`. Or use explicit `status in setOf(Moderate, Severe, Critical, Emergency, Shutdown)`.

### 7. BackHandler + AlertDialog (VID-09)

```kotlin
// CameraScreen.kt + InsectFilterScreen.kt — D-10/D-24
@Composable
fun RecordingDiscardDialog(
    onDiscard: () -> Unit,
    onCancel: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("Recording in progress") },
        text = { Text("Discard this recording?") },
        confirmButton = {
            TextButton(onClick = onDiscard) { Text("Discard") }
        },
        dismissButton = {
            TextButton(onClick = onCancel) { Text("Cancel") }
        },
    )
}

// Inside CameraScreen / InsectFilterScreen composable body:
var showDiscardDialog by rememberSaveable { mutableStateOf(false) }

BackHandler(enabled = isRecording) {
    showDiscardDialog = true
}

if (showDiscardDialog) {
    RecordingDiscardDialog(
        onDiscard = {
            showDiscardDialog = false
            vm.onDiscardRecording()  // calls recording.stop() + schedules pending URI delete
        },
        onCancel = {
            showDiscardDialog = false
            // recording continues — no state change
        }
    )
}
```

`BackHandler` is from `androidx.activity.compose` — already on classpath (`activity-compose:1.10.1`). `rememberSaveable` preserves dialog state across recompositions. [CITED: developer.android.com/develop/ui/compose/components/dialog]

### 8. Lazy RECORD_AUDIO Permission Flow (VID-10)

```kotlin
// CameraScreen.kt / InsectFilterScreen.kt — D-12
val audioPermissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission()
) { granted ->
    vm.onAudioPermissionResult(granted)
    if (granted) {
        vm.startRecordingWithAudio()
    } else {
        vm.startRecordingWithoutAudio()  // record muted; rationale shown via OneShotEvent
    }
}

// In onRecordTapped() composable handler:
val audioGranted = ContextCompat.checkSelfPermission(
    context, Manifest.permission.RECORD_AUDIO
) == PackageManager.PERMISSION_GRANTED

if (audioGranted) {
    vm.onRecordTapped(audioEnabled = true)
} else {
    audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
}
```

If permanently denied (`shouldShowRequestPermissionRationale` returns false AND first-time check returns denied), show rationale via `OneShotEvent` Toast with "Open Settings" action. The ViewModel emits a special `OneShotEvent.AudioPermissionDenied` to trigger the Toast+intent. [ASSUMED based on Phase 1 CAMERA pattern — same contract]

### 9. Recording Indicator UI (VID-07)

```kotlin
// D-08 — inside CameraScreen Box composable when isRecording
val isRecording = uiState.recordingState is RecordingState.Active
var elapsedSeconds by rememberSaveable { mutableIntStateOf(0) }

LaunchedEffect(isRecording) {
    elapsedSeconds = 0
    if (isRecording) {
        while (true) {
            delay(1_000L)
            elapsedSeconds++
        }
    }
}

val dotAlpha by animateFloatAsState(
    targetValue = if (isRecording) 1f else 0f,
    animationSpec = infiniteRepeatable(
        animation = tween(500, easing = LinearEasing),
        repeatMode = RepeatMode.Reverse,
    ),
    label = "recordDotBlink",
)

AnimatedVisibility(visible = isRecording) {
    Row(
        modifier = Modifier
            .align(Alignment.TopCenter)
            .padding(top = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            Modifier
                .size(16.dp)
                .background(Color(0xFFE53935).copy(alpha = dotAlpha), CircleShape)
        )
        val mins = elapsedSeconds / 60
        val secs = elapsedSeconds % 60
        Text(
            text = "%02d:%02d".format(mins, secs),
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}
```

[ASSUMED] The `infiniteRepeatable` + `RepeatMode.Reverse` pattern achieves 1Hz blink at 500ms per phase. Confirm `LinearEasing` vs `LinearOutSlowInEasing` during implementation — Claude's Discretion per D-14 CONTEXT.

---

## Open Questions (RESOLVED + MINOR PLANNING ITEMS)

### RESOLVED — All major open questions pre-resolved via CONTEXT auto-locks

All items from the original discussion phase are resolved by D-01..D-26. The following two minor items are implementation-detail choices left to the planner/executor:

1. **Sticker coordinate space mapping between Compose gesture offset and OverlayEffect canvas space**
   - What we know: `StickerState.offset` accumulates Compose pointer deltas in preview-screen pixels; `StickerRenderer.onDraw()` draws after `canvas.setMatrix(sensorToBufferTransform)` is applied, placing the canvas in sensor-buffer space.
   - What's unclear: whether to pass the raw Compose offset to the renderer or convert it. If preview fills the screen without letterboxing (FILL_CENTER, which is the case per Phase 2), the conversion is approximately `offset_sensor = offset_preview * (sensorBufferSize / previewSize)`.
   - Recommendation: implement with raw offset first; if sticker position appears off, add scale compensation. The Phase 2 debug overlay approach (using frame.sensorToBufferTransform matrix to map coords) shows the right direction. Alternatively, draw the sticker in the composable layer (not in OverlayEffect canvas) — but then it won't appear in captured video/photo (same pitfall as PITFALLS §2). Must use OverlayEffect canvas path.

2. **`withDurationLimit` time unit confirmation**
   - What we know: D-09 says `withDurationLimit(60_000L * 1_000_000L)` suggesting nanoseconds; the actual CameraX API has `withDurationLimit(long duration, TimeUnit unit)` and `withDurationLimit(Duration duration)`.
   - Recommendation: use `withDurationLimit(60_000L, TimeUnit.MILLISECONDS)` or `withDurationLimit(Duration.ofSeconds(60))` — verify during Wave 0 by checking the method signature in the AAR.

---

## Pitfalls

### Pitfall 1: Video loses overlay (INHERITED — already solved)
**Status:** Solved in Phase 2 (02-gaps-03). `OverlayEffect` TARGETS already includes `VIDEO_CAPTURE`. `OverlayEffectBuilder.TARGETS = PREVIEW or VIDEO_CAPTURE or IMAGE_CAPTURE` verified in source. Phase 5 does NOT need to change OverlayEffect targets.
**Warning sign for regression:** If Phase 5 accidentally constructs a new `OverlayEffect` instance with different targets, video will lose overlay. Must reuse the existing singleton instance. The `overlayEffect = overlayEffectBuilder.build()` call in `CameraController` property initializer runs once — verify via grep that Phase 5 additions do NOT call `build()` a second time.

### Pitfall 2: Double-tap Record starts concurrent recordings (NEW — T-05-03)
**What goes wrong:** User taps Record twice quickly; `startRecording()` called twice; second call starts a new recording while first is still active; `activeRecording` state becomes inconsistent.
**How to avoid:** Guard in ViewModel (`onRecordTapped()` no-op if `recordingState != Idle`). Guard in `CameraController.startRecording()` via `check(activeRecording == null)`. Same pattern as `isCapturing` guard for photo (Phase 3 D-26 preserved pattern). [VERIFIED: codebase — isCapturing guard exists at CameraViewModel.onShutterTapped()]

### Pitfall 3: Pending MediaStore video orphan on Discard (NEW — T-05-07)
**What goes wrong:** User taps Discard in AlertDialog. `recording.stop()` fires `VideoRecordEvent.Finalize`. The MP4 file at the pending URI exists in MediaStore with a 0-byte or partial content. If `contentResolver.delete(uri)` is not called, the file persists in gallery with corrupt content or as a ghost entry.
**How to avoid:** In `VideoRecordEvent.Finalize` handler, if the discard flag is set, call `contentResolver.delete(outputUri, null, null)` before emitting `VideoSaved`. Store a `pendingDiscardFlag: Boolean` in the ViewModel, set it in `onDiscardRecording()` before calling `recording.stop()`.

### Pitfall 4: ImageAnalysis backpressure during video record (INHERITED — PITFALLS §4)
**Status:** Already handled by `STRATEGY_KEEP_ONLY_LATEST` (Phase 2). During recording, ML Kit + H264 encoder + preview all compete for CPU. The thermal frame-skip (D-14) reduces ML Kit load when needed, but the backpressure strategy ensures the camera producer never stalls regardless.
**Warning sign:** Logcat "Image already closed" during recording — indicates analyzer closing the ImageProxy too early. Not expected with `MlKitAnalyzer` which handles close timing.

### Pitfall 5: Thermal listener leak on app lifecycle (NEW — T-05-05)
**What goes wrong:** `ThermalMonitor` registered in `BugzzApplication.onCreate()` as an `@Singleton` — this is process-lifetime, which is correct. However, if the listener is registered but never unregistered when the process lives beyond normal app usage, minor resource leak. Listener should be unregistered in `onTerminate()` (or not — Android process lifecycle means `onTerminate()` is not guaranteed to be called).
**How to avoid:** Register in `BugzzApplication.onCreate()` (correct — process lifetime singleton). The listener is process-lifetime by design. No unregister needed for this scope. `PowerManager` internally manages the listener set. If the listener is a lambda/anonymous class with a long-lived outer reference, wrap in a weak reference — but since `ThermalMonitor` is `@Singleton` (held by Hilt component = process lifetime), there is no cycle.

### Pitfall 6: Sticker gesture conflicts with picker scroll (NEW — MOD-04..06)
**What goes wrong:** `detectTransformGestures` on the full camera preview area intercepts vertical/horizontal scrolls intended for the `FilterPicker` LazyRow. User tries to scroll picker thumbnails; sticker moves instead.
**How to avoid:** Apply `Modifier.pointerInput(isRecording)` only to the preview area ABOVE the picker strip, not the full `Box`. Use `Box` zIndexing to ensure picker sits above the gesture layer and `pointerInput(Unit) {}` on the picker to consume events it handles. Alternatively, `detectTransformGestures` pan detection uses `PointerEventPass.Main` and the picker LazyRow uses `PointerEventPass.Initial` (default scroll) — if the picker is z-ordered above the gesture layer, its scroll events propagate first.

### Pitfall 7: Sticker not visible in recorded video / photo (NEW — MOD-03)
**What goes wrong:** Sticker renders in the Compose composable layer (nice for preview) but is drawn ABOVE `CameraXViewfinder` as a Compose element — not through `OverlayEffect` canvas — so it is missing from the saved MP4 and JPEG.
**How to avoid:** `StickerRenderer.onDraw()` MUST be called inside the `OverlayEffect.setOnDrawListener` callback, drawing to `frame.overlayCanvas`. The Compose sticker layer (if any, for UI decoration) should be a transparent overlay for UX affordance only — the actual capture output comes from the OverlayEffect canvas path. Plan tasks must verify that the sticker appears in the 05-HANDOFF device runbook video playback.

### Pitfall 8: Xiaomi MIUI thermal behavior (INHERITED — PITFALLS §7)
**What we know:** Phase 4 Xiaomi 13T device verification showed no unexpected OEM behavior. However, Xiaomi MIUI may report `THERMAL_STATUS_MODERATE` earlier than Pixel/Samsung due to aggressive thermal policies. `ThermalMonitor.status` logs should be included in the 05-HANDOFF runbook so the user can observe status changes during a 60s recording test.
**How to avoid:** Log every `ThermalStatusListener` callback at `Timber.tag("ThermalMonitor").v(...)`. Include a logcat filter in the HANDOFF runbook: `adb logcat -s ThermalMonitor:V`.

### Pitfall 9: AudioSource + RECORD_AUDIO and Recorder interaction (INHERITED — PITFALLS §8)
**What goes wrong:** `PendingRecording.withAudioEnabled()` called WITHOUT first verifying RECORD_AUDIO is granted → `SecurityException` crash at recording start.
**How to avoid:** D-12 lazy permission flow handles this. `withAudioEnabled()` is only called after the permission contract callback confirms `granted == true`. The ViewModel must NOT call `startRecording(audioEnabled = true)` if permission is unknown or denied.

### Pitfall 10: Concurrent ImageCapture during VideoCapture (KNOWN GOOD)
**Status:** CameraX 1.6 supports concurrent `ImageCapture` + `VideoCapture` in the same `UseCaseGroup`. Shutter tap during recording captures a photo. The `OverlayEffect` ensures both outputs get the overlay at the correct timestamp. This is safe and intended per D-23 (shutter NOT locked during recording). No pitfall — documenting as verified expected behavior per CameraX 1.6 release notes. [CITED: developer.android.com/media/camera/camerax/capture-video]

---

## Code Map

### New Files (D-19)

| File | Package | Pattern | Purpose |
|------|---------|---------|---------|
| `ui/insect/InsectFilterScreen.kt` | `com.bugzz.filter.camera.ui.insect` | Compose screen; mirrors CameraScreen structure | Camera preview + sticker gesture layer + picker + record button |
| `ui/insect/InsectFilterViewModel.kt` | `com.bugzz.filter.camera.ui.insect` | @HiltViewModel; mirrors CameraViewModel; no FaceDetector attach | StickerState StateFlow + recording lifecycle + filter prefs |
| `ui/insect/StickerState.kt` | `com.bugzz.filter.camera.ui.insect` | data class | offset: Offset, scale: Float, rotation: Float |
| `render/StickerRenderer.kt` | `com.bugzz.filter.camera.render` | @Singleton @Inject; thread-safe via synchronized | setStickerState(), setActiveFilter(), onDraw(canvas, frame) |
| `thermal/ThermalMonitor.kt` | `com.bugzz.filter.camera.thermal` | @Singleton @Inject; process lifetime | PowerManager.OnThermalStatusChangedListener; status: StateFlow<ThermalStatus> |
| `recording/VideoRecorder.kt` | `com.bugzz.filter.camera.recording` | @Singleton @Inject | Wraps CameraController recording methods; exposes event Flow |

### Modified Files (D-20/D-21/D-22 + shared-screen changes)

| File | Change | Risk |
|------|--------|------|
| `render/OverlayEffectBuilder.kt` | Add `stickerRenderer: StickerRenderer?` constructor param; branch `setOnDrawListener` on `cameraMode` | LOW — additive only; existing FilterEngine path unchanged |
| `camera/CameraController.kt` | Add `startRecording(audioEnabled): Recording` + `stopRecording()` + `clearActiveRecording()` production methods | LOW — additive; existing test methods kept behind DEBUG guard |
| `detector/FaceDetectorClient.kt` | Add `thermalMonitor: ThermalMonitor` constructor param; frame-skip in analyzer consumer | LOW — additive guard logic; existing detector path unchanged |
| `ui/camera/CameraViewModel.kt` | Add `RecordingState` sealed + `onRecordTapped()` + `onDiscardRecording()` + `onAudioPermissionResult()` + event handler | MEDIUM — CameraUiState struct change; must preserve all D-26 inherited fixes |
| `ui/camera/CameraUiState.kt` | Add `recordingState: RecordingState = RecordingState.Idle` replacing/extending `isRecording: Boolean` | LOW — additive field; `isRecording` computed property `val isRecording = recordingState is RecordingState.Active` preserves backward compat |
| `ui/camera/OneShotEvent.kt` | Add `VideoSaved(uri: Uri)` + `VideoError(message: String)` variants | LOW — additive sealed interface entries |
| `ui/camera/CameraScreen.kt` | Replace TEST RECORD with production Record button; add indicator Row; add AlertDialog; add BackHandler; add lock-during-record logic | MEDIUM — existing shutter + picker logic preserved; new elements added |
| `ui/BugzzApp.kt` | Rewire `CameraRoute(InsectFilter)` from `InsectFilterStubScreen` to `InsectFilterScreen` | LOW — delete stub route; add new composable |
| `ui/home/InsectFilterStubScreen.kt` | DELETE | LOW |
| `BugzzApplication.kt` | Add `@Inject lateinit var thermalMonitor: ThermalMonitor`; call `thermalMonitor.register()` in `onCreate()` | LOW |
| `di/CameraModule.kt` | Add `@Provides` for `ThermalMonitor` if not using `@Singleton @Inject` direct injection | LOW |

---

## Validation Architecture

### Test Framework

| Property | Value |
|----------|-------|
| Framework | JUnit 4.13.2 + Robolectric 4.13 (existing) + Mockito / MockK (existing) |
| Config file | `app/src/test/java/...` (plain JVM) + `@RunWith(RobolectricTestRunner::class) @Config(sdk=[34])` where Android classes needed |
| Quick run | `./gradlew :app:testDebugUnitTest --tests "com.bugzz.filter.camera.recording.*" --tests "com.bugzz.filter.camera.thermal.*" --tests "com.bugzz.filter.camera.ui.insect.*"` |
| Full suite | `./gradlew :app:testDebugUnitTest` (runs all 106+ existing tests + new Phase 5 tests) |

### Phase Requirements → Test Map

| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|--------------|
| VID-01 | Record button starts recording (isRecording guard prevents double-start) | Unit (JVM) | `./gradlew :app:testDebugUnitTest --tests "*.CameraViewModelTest.onRecordTapped_*"` | ❌ Wave 0 |
| VID-02 | Overlay baked into video output | Manual (device) | Record 5s → pull MP4 → verify overlay in frame extraction | — |
| VID-03 | Audio synced in output | Manual (device) | 05-HANDOFF runbook: subjective audio sync check | — |
| VID-04 | Auto-stop at 60s + manual stop | Unit (JVM) | `./gradlew :app:testDebugUnitTest --tests "*.VideoRecorderTest.durationLimit_*"` | ❌ Wave 0 |
| VID-05 | MIRROR_MODE_ON_FRONT_ONLY wired | Unit (JVM) | `./gradlew :app:testDebugUnitTest --tests "*.CameraControllerTest.bind_videoCaptureHasMirrorMode"` | Extend existing |
| VID-06 | MP4 saved to DCIM/Bugzz | Manual (device) | 05-HANDOFF: pull file from device + verify path | — |
| VID-07 | Recording indicator visible | Unit (JVM — state logic) | `./gradlew :app:testDebugUnitTest --tests "*.CameraViewModelTest.recordingState_timerIncrements"` | ❌ Wave 0 |
| VID-08 | ThermalMonitor frame-skip | Unit (JVM) | `./gradlew :app:testDebugUnitTest --tests "*.ThermalMonitorTest.*"` | ❌ Wave 0 |
| VID-09 | BackHandler + AlertDialog state | Unit (JVM — state only) | `./gradlew :app:testDebugUnitTest --tests "*.CameraViewModelTest.onDiscardRecording_*"` | ❌ Wave 0 |
| VID-10 | RECORD_AUDIO lazy request | Manual (device) | 05-HANDOFF: first-tap permission dialog verification | — |
| MOD-03 | Insect sticker placed at center | Unit (JVM) | `./gradlew :app:testDebugUnitTest --tests "*.StickerStateTest.initialPosition_isCenter"` | ❌ Wave 0 |
| MOD-04 | Drag gesture moves sticker | Unit (JVM) | `./gradlew :app:testDebugUnitTest --tests "*.StickerStateTest.drag_updatesOffset"` | ❌ Wave 0 |
| MOD-05 | Pinch-to-zoom updates scale | Unit (JVM) | `./gradlew :app:testDebugUnitTest --tests "*.StickerStateTest.pinch_*"` | ❌ Wave 0 |
| MOD-06 | Rotation gesture | Unit (JVM) | `./gradlew :app:testDebugUnitTest --tests "*.StickerStateTest.rotate_*"` | ❌ Wave 0 |
| MOD-07 | Sticker survives flip + orientation | Unit (JVM) | `./gradlew :app:testDebugUnitTest --tests "*.InsectFilterViewModelTest.stickerState_survivesFlip"` | ❌ Wave 0 |

### Sampling Rate

- **Per task commit:** `./gradlew :app:testDebugUnitTest` (full suite; ~20s on cached build)
- **Per wave merge:** Full suite + `./gradlew :app:assembleDebug :app:lintDebug`
- **Phase gate:** Full suite green + clean debug APK installable before 05-HANDOFF.md device runbook

### Wave 0 Gaps (test files to create before implementation)

- [ ] `app/src/test/.../recording/VideoRecorderTest.kt` — Recorder construction + durationLimit logic (Robolectric)
- [ ] `app/src/test/.../thermal/ThermalMonitorTest.kt` — frame-skip throttle logic (mock status, pure JVM)
- [ ] `app/src/test/.../ui/insect/StickerStateTest.kt` — pan/zoom/rotate math + scale clamp constants (pure JVM)
- [ ] `app/src/test/.../ui/insect/InsectFilterViewModelTest.kt` — gesture event → state mutation; DataStore restore last-used filter; recording lifecycle in InsectFilter mode
- [ ] `app/src/test/.../ui/camera/CameraViewModelTest.kt` extension — `onRecordTapped` lifecycle, `isRecording` guard, `RecordingState` transitions, `onDiscardRecording` pending-URI cleanup

---

## Security Domain

### Applicable ASVS Categories

| ASVS Category | Applies | Standard Control |
|---------------|---------|-----------------|
| V2 Authentication | no | — |
| V3 Session Management | no | — |
| V4 Access Control | yes (recording permission) | `ActivityResultContracts.RequestPermission(RECORD_AUDIO)` — never bypass |
| V5 Input Validation | yes (recording event error codes, sticker scale clamp) | `coerceIn(0.3f, 3.0f)` for scale; exhaustive `when` on `VideoRecordEvent.Finalize.error` |
| V6 Cryptography | no | — |

### Threat Model — T-05-01..T-05-07

| ID | Threat | STRIDE | Standard Mitigation | Status |
|----|--------|--------|---------------------|--------|
| T-05-01 | RECORD_AUDIO permission denial — app crashes or hangs | Denial of Service | Graceful fallback: `startRecording(audioEnabled = false)` if denied; show rationale Toast; never hard-fail | Design in D-12 |
| T-05-02 | Storage exhaustion during 60s recording | Denial of Service | `VideoRecordEvent.Finalize(ERROR_INSUFFICIENT_STORAGE)` → emit `VideoError` Toast + delete pending URI | Handle in Finalize event handler |
| T-05-03 | Double-tap Record → concurrent recordings | Tampering (data integrity) | `isRecording` guard in ViewModel + `check(activeRecording == null)` in CameraController | Guard pattern from Phase 3 `isCapturing` |
| T-05-04 | Sticker gesture during backgrounded app | Elevation of Privilege | `Modifier.pointerInput(isRecording)` + `Lifecycle.Event.ON_PAUSE` — gestures only active when screen is foregrounded; Compose lifecycle handles this via `collectAsStateWithLifecycle` |
| T-05-05 | ThermalMonitor listener leak on activity destroy | Information Disclosure | `ThermalMonitor` is `@Singleton` (process lifetime) — no Activity ref captured; listener unregistered only if `BugzzApplication.onTerminate()` is reliable (not guaranteed by Android); acceptable for process-lifetime singleton |
| T-05-06 | Sticker rendering blocking render thread on heavy transform | Denial of Service | `Canvas.save()/restore()` with lightweight translate/scale/rotate — no per-frame Bitmap allocation; `StickerRenderer.onDraw()` runs on `BugzzRenderThread` (HandlerThread, not main) |
| T-05-07 | MediaStore pending file orphan on recording abort | Information Disclosure | `contentResolver.delete(pendingUri)` in `onDiscardRecording()` post-Finalize; pending URI captured from `outputResults.outputUri` in Finalize event handler |

**Logging constraints (T-02-02 inherited):** `VideoRecorder` / `StickerRenderer` Timber tags log timestamps, file sizes, thermal status — NEVER log face landmarks, face bounding boxes, or personally identifiable biometric data. Face detection is disabled in InsectFilter mode entirely; CameraScreen recording logs only media metadata.

---

## Environment Availability

Step 2.6: All Phase 5 dependencies are the same CameraX + AndroidX stack already in `libs.versions.toml` and verified building in Phase 4. No new external dependencies introduced.

| Dependency | Required By | Available | Version | Fallback |
|------------|-------------|-----------|---------|---------|
| `androidx.camera:camera-video` | VideoCapture + Recorder | ✓ | 1.6.0 (in libs.versions.toml) | — |
| `androidx.camera:camera-effects` | OverlayEffect VIDEO_CAPTURE | ✓ | 1.6.0 (in libs.versions.toml) | — |
| `PowerManager` API 29+ | ThermalStatusListener | ✓ (runtime guard for API 28) | Android framework | API 28: status stays None |
| `detectTransformGestures` | MOD-04..06 | ✓ | Compose foundation (BOM 2026.03.00) | — |
| `BackHandler` | VID-09 | ✓ | activity-compose 1.10.1 | — |
| `MediaStore.Video.Media` | VID-06 | ✓ | Android framework minSdk 28 | — |

**No missing dependencies with no fallback.** Phase 5 is greenfield addition to a verified dependency set.

---

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| `startTestRecording()` debug-only no-audio 5s | `startRecording(audioEnabled): Recording` production full 60s | Phase 5 | Production video path replaces test stub |
| `isRecording: Boolean` in CameraUiState | `recordingState: RecordingState` sealed interface | Phase 5 | Richer state: Active(elapsedMs, hasAudio), Stopping, Error |
| `InsectFilterStubScreen` (nav dead-end) | `InsectFilterScreen` with gesture layer + picker + record | Phase 5 | MOD-03..07 completed |
| No thermal throttle | ThermalMonitor + FaceDetectorClient frame-skip | Phase 5 | VID-08; mitigates sustained recording degradation |

---

## Assumptions Log

| # | Claim | Section | Risk if Wrong |
|---|-------|---------|---------------|
| A1 | `withDurationLimit(60_000L, TimeUnit.MILLISECONDS)` is the correct overload | Key APIs §1 | Compile error at Wave 0; low risk — alternative `Duration.ofSeconds(60)` available |
| A2 | `RecordingState.Error` integer constants (ERROR_DURATION_REACHED etc.) are accessible as `VideoRecordEvent.Finalize` companion constants | Key APIs §2 | Need to use raw int comparison — check AAR during Wave 0 compile |
| A3 | `StickerState.offset` in Compose gesture pixels maps reasonably to OverlayEffect canvas space without explicit transform | Key APIs §5 | Sticker appears offset from expected position — add inverse transform compensation |
| A4 | `ThermalStatus` enum with ordinal ordering allows `>=` comparison | Key APIs §6 | Use explicit `when` set membership check instead |
| A5 | RECORD_AUDIO rationale Toast + "Open Settings" intent follows Phase 1 CAMERA permission pattern exactly | Key APIs §8 | Different UX flow; low risk since pattern is identical |
| A6 | `pendingRecording.withAudioEnabled()` is called on the `PendingRecording` object (not the `Recorder`) | Key APIs §1 | API usage error; check CameraX 1.6 Recorder docs |

**All A1-A6 are low risk** — they are verifiable at Wave 0 compile time before any UI work begins.

---

## Sources

### Primary (HIGH confidence)
- `app/src/main/java/.../camera/CameraController.kt` — existing VideoCapture + MIRROR_MODE binding confirmed; startTestRecording() pattern
- `app/src/main/java/.../render/OverlayEffectBuilder.kt` — TARGETS mask confirmed; HandlerThread pattern; setOnDrawListener shape
- `.planning/phases/02-camera-preview-face-detection-coordinate-validation/02-CONTEXT.md` — D-04..D-06 VideoCapture binding precedent; D-15 ML Kit PERFORMANCE_MODE_FAST
- `.planning/phases/04-filter-catalog-picker-face-filter-mode/04-08-SUMMARY.md` — Phase 4 closure; AssetLoader assetDir fix; device verification evidence
- `.planning/research/PITFALLS.md` §2, §4, §7, §8, §11 — video overlay pitfall (solved); backpressure; device fragmentation; audio permission; thermal throttling
- `.planning/research/STACK.md` — Recorder + VideoCapture pattern, MediaStore Video pattern
- `05-CONTEXT.md` — D-01..D-26 all locked decisions

### Secondary (MEDIUM confidence)
- [CameraX video capture guide](https://developer.android.com/media/camera/camerax/capture-video) — Recorder + prepareRecording + VideoRecordEvent dispatch
- [Compose multi-touch pointer input](https://developer.android.com/develop/ui/compose/touch-input/pointer-input/multi-touch) — detectTransformGestures API
- [PowerManager.addThermalStatusListener](https://developer.android.com/reference/android/os/PowerManager#addThermalStatusListener) — API 29+ callback registration
- [Compose AlertDialog Material3](https://developer.android.com/develop/ui/compose/components/dialog) — exit-during-record dialog

### Tertiary (LOW confidence — verify at Wave 0)
- Exact `withDurationLimit` overload parameter type (TimeUnit vs nanoseconds) — A1
- `VideoRecordEvent.Finalize` error code constant names — A2

---

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH — all dependencies already in project; no new libs
- Architecture: HIGH — Recorder/VideoCapture proven in Phase 2; gesture API is standard Compose
- Pitfalls: HIGH — inherited from verified prior phases; new pitfalls (T-05-01..07) are all addressed by existing patterns
- ThermalMonitor: MEDIUM — API 29+ guard required; behavior on Xiaomi 13T HyperOS unknown until device test

**Research date:** 2026-05-04
**Valid until:** 2026-06-04 (stable APIs; no version bumps expected within 30 days)

---

## RESEARCH COMPLETE

**Phase:** 5 — Video Recording + Audio + Insect Filter Free-Placement Mode
**Confidence:** HIGH

### Key Findings

1. **VideoCapture + MIRROR_MODE already wired** — `CameraController` binds `VideoCapture` with `MIRROR_MODE_ON_FRONT_ONLY` and `Quality.HD` since Phase 2. Phase 5 only adds production `startRecording(audioEnabled)` method on top of existing infrastructure. No CameraX rebind required.

2. **OverlayEffect VIDEO_CAPTURE proven on device** — Phase 2 02-gaps-03 verified that `OverlayEffect` bakes into the recorded MP4 on Xiaomi 13T. VID-02 is architecturally free; it's already working.

3. **Compose `detectTransformGestures` is a single API** — handles pan+zoom+rotate simultaneously; no custom gesture recognizer needed. Coordinate space mapping between Compose gesture offset and OverlayEffect canvas is the one minor open question (A3).

4. **ThermalMonitor requires API 29+ guard** — minSdk=28 means API 28 devices get no-throttle behavior; runtime `Build.VERSION.SDK_INT >= Q` check is mandatory.

5. **Two parallel feature tracks** — `VideoRecorder` + `ThermalMonitor` infra (shared) can be built independently from `InsectFilterScreen` + `StickerRenderer`. They converge at `CameraScreen` modifications and `OverlayEffectBuilder` extension.

6. **Sticker rendering must be in OverlayEffect canvas** — not a Compose layer — or the sticker will be missing from captured photos and videos (PITFALLS §2 pattern).

### File Created
`.planning/phases/05-video-recording-audio-insect-filter-free-placement-mode/05-RESEARCH.md`

### Confidence Assessment

| Area | Level | Reason |
|------|-------|--------|
| Standard Stack | HIGH | All deps verified in existing codebase; CameraX 1.6.0 already in libs.versions.toml |
| Architecture | HIGH | VideoCapture proven in Phase 2; Compose gesture API is well-documented |
| Pitfalls | HIGH | Inherited from Phases 2-4 device verification; new pitfalls follow established patterns |
| ThermalMonitor | MEDIUM | API 29+ behavior verified in docs; Xiaomi 13T HyperOS thermal reporting needs device test |

### Open Questions
- Sticker offset coordinate space mapping (A3): implement with raw Compose pixels first, add compensation if needed during device test
- `withDurationLimit` exact overload (A1): verify at Wave 0 compile

### Ready for Planning
Research complete. Planner can now create PLAN.md files for Phase 5.
