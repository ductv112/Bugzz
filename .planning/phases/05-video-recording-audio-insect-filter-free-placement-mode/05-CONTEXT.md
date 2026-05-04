# Phase 5: Video Recording + Audio + Insect Filter Free-Placement Mode - Context

**Gathered:** 2026-05-04
**Status:** Ready for planning
**Source:** Auto-locked recommended defaults (user delegated full autonomous run per memory `feedback_autonomy.md`)

<domain>
## Phase Boundary

Two feature blocks merged per ROADMAP key decision ("both reuse the validated render pipeline"):

1. **Video Recording** (VID-01..10) — production 60s MP4 capture with synced microphone audio, active filter overlay baked into every frame via `OverlayEffect` `VIDEO_CAPTURE` target, front-camera mirror via `MIRROR_MODE_ON_FRONT_ONLY`, lazy `RECORD_AUDIO` permission flow, `PowerManager.ThermalStatusListener` defensive throttle for sustained recording.
2. **Insect Filter Free-Placement Mode** (MOD-03..07) — single sticker overlay placed at user-touch position, supports drag + pinch-to-zoom + two-finger rotation gestures via Compose `detectTransformGestures`, state survives camera flip and device orientation change.

**Out of scope:** Video editing/trimming (deferred — POL-04 v2), music overlay (POL-03 v2), countdown timer pre-record (POL-01 v2), watermark overlay (POL-04 v2), multi-sticker mode (current scope is single-sticker), sticker rotation snap-to-angle (free rotation only Phase 5), sticker color tint (out of scope), Phase 6 production Preview/Collection screens (still stubs at Phase 5 close), formal ≥24fps measured + audio drift <50ms validation (Phase 7 PRF-01..03), cross-OEM matrix (Phase 7), sticker state persistence across app launch (lives only in ViewModel — Phase 6 may extend if desired).

</domain>

<decisions>
## Implementation Decisions

### Insect Filter Mode UX + State (gray area 1)
- **D-01:** **Insect Filter mode reuses the same 15-filter `FilterCatalog` from Phase 4** (no separate "sticker catalog"). User picks via the same `LazyRow` picker strip from Phase 4 04-UI-SPEC §Component Specs §2 — only difference is rendering mode (face-anchored vs free-placement). Picker behavior, thumbnails, selection animation identical.
- **D-02:** **Initial sticker position = preview center.** When user enters InsectFilter mode (HomeScreen → Insect Filter button), sticker spawns at `(previewWidth/2, previewHeight/2)`, scale=1.0f, rotation=0f. First touch-down does NOT relocate — drag gestures translate from initial position. (Avoids surprise jump on first tap.)
- **D-03:** **Gestures via Compose `detectTransformGestures`** (single API for pan + zoom + rotate). State model:
  ```kotlin
  data class StickerState(
      val offset: Offset = Offset.Zero,        // accumulated drag offset
      val scale: Float = 1f,                   // pinch-to-zoom result
      val rotation: Float = 0f,                // two-finger rotation degrees
  )
  ```
  Constraints: scale clamped `[0.3f, 3.0f]`; offset clamped to keep sticker center within preview bounds (sticker may overflow edges by 50% to allow off-edge placement); rotation unbounded (mod 360).
- **D-04:** **State survival = `InsectFilterViewModel` holds `StickerState` in `StateFlow`.** Survives camera flip (`CameraController.flipLens` does not reset VM state), survives orientation change (Modifier.graphicsLayer applies transforms in preview-space, orientation-independent). State does NOT persist across app launch (Phase 6 may extend — see Deferred). Mid-session sticker state is preserved via Compose `viewModelScope`.
- **D-05:** **Face detection DISABLED in InsectFilter mode.** `FaceDetectorClient` not attached as `MlKitAnalyzer` when `CameraMode == InsectFilter`. Saves CPU + battery, simplifies render path. `FilterEngine.onDraw` receives `faces = emptyList()` and skips face-anchored draw entirely. Sticker rendered separately via dedicated logic in `OverlayEffectBuilder.setOnDrawListener`.
- **D-06:** **New package `com.bugzz.filter.camera.ui.insect`** for `InsectFilterScreen.kt` + `InsectFilterViewModel.kt` + `StickerState.kt`. Sticker rendering shares `OverlayEffectBuilder` infrastructure but adds optional `StickerRenderer` callback alongside `FilterEngine` and `DebugOverlayRenderer` (D-27 Phase 3 pattern). New file `render/StickerRenderer.kt` consumes `StickerState` + active `FilterDefinition` from `FilterEngine.activeFilter`, draws sprite at `state.offset` with `state.scale` + `state.rotation` transforms applied via `canvas.save()` + `canvas.translate(offset)` + `canvas.scale()` + `canvas.rotate()`.

### Video Recording UI + Lifecycle (gray area 2)
- **D-07:** **Production Record button REPLACES Phase 2/4 TEST RECORD debug button** at `Alignment.BottomStart` (24dp from edge, vertically aligned with shutter). Visual: 56dp circular Surface, red fill `#FFE53935`, white border 2dp. Idle state shows red filled circle; recording state shows white square inside red circle (universal "stop" icon convention). Tap toggle: idle → start record → recording state → tap → stop. **REMOVES `BuildConfig.DEBUG` gate** — production-grade button now.
- **D-08:** **Recording indicator = top-center `Row` with red dot (16dp) + elapsed timer "MM:SS" Text (16sp/Medium)**, 24dp from top. Red dot animates with `animateFloatAsState` blink at 1Hz (alpha 1.0 ↔ 0.5). Timer counts up from "00:00" to "01:00" via `LaunchedEffect(isRecording) { while(isRecording) { delay(1000); elapsedMs += 1000 } }`. Hidden when not recording.
- **D-09:** **Auto-stop at 60s via `Recorder.PendingRecording.withDurationLimit(60_000L * 1_000_000L /* µs */)`.** CameraX Recorder fires `VideoRecordEvent.Finalize(cause = ERROR_DURATION_REACHED)` → ViewModel handles same as manual stop. Manual stop via second tap on Record button calls `recording.stop()` → `VideoRecordEvent.Finalize(cause = ERROR_NONE)`. Both paths converge to same UI state transition (Idle).
- **D-10:** **Exit-during-record confirmation dialog** = Material3 `AlertDialog` with title "Recording in progress" + body "Are you sure you want to discard this recording?" + Cancel button (resumes recording — no state change) + Discard button (calls `recording.stop()` + deletes pending file via `MediaStore.Files.delete` of pending URI). Triggered by: system back press during record OR navigating away (Home button NOT triggering — system level). Use `BackHandler(enabled = isRecording)` to intercept.
- **D-11:** **During recording: filter swap LOCKED + Flip lens LOCKED + InsectFilter sticker gestures DISABLED.** Picker thumbnails get 50% alpha + `enabled = false` on tap; Flip button greyed; sticker drag/pinch/rotate ignored. Prevents mid-recording filter or lens change which would produce inconsistent video. Lock applied via `_uiState.value.isRecording` check at composable + handler levels.

### RECORD_AUDIO Permission + Mirror + Thermal (gray area 3)
- **D-12:** **Lazy RECORD_AUDIO permission flow.** First tap of Record button triggers `ActivityResultContracts.RequestPermission(Manifest.permission.RECORD_AUDIO)`. If granted: `recorder.withAudioEnabled()` activates + recording proceeds immediately. If denied: inline rationale Toast/Snackbar "Mic access needed for video sound. Tap record again to grant or visit Settings." with action button "Open Settings" reusing Phase 1 D-13 pattern (`Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)`). Subsequent denials within session: continue showing rationale; do NOT auto-disable record (user can still record without audio if they tap-through).
- **D-13:** **`MirrorMode.MIRROR_MODE_ON_FRONT_ONLY` wired on production VideoCapture.** Phase 2 had this as a placeholder constant (STATE #11 already mentioned), Phase 5 makes it functional via `VideoCapture.Builder<Recorder>().setMirrorMode(MirrorMode.MIRROR_MODE_ON_FRONT_ONLY).build()`. Front-cam MP4 recordings auto-mirror; back-cam recordings un-mirrored. No per-recording flag — set once on builder.
- **D-14:** **`PowerManager.ThermalStatusListener` defensive throttle.** Registered in `BugzzApplication.onCreate` as singleton. Exposes hot `StateFlow<ThermalStatus>` enum (`Light`, `Moderate`, `Severe`). `FaceDetectorClient.createAnalyzer()` consumer reads this state and applies frame-skip throttle: when status `>= Moderate`, skip every other ML Kit detector invocation (effective ~15fps from 30fps input). Detector remains in `PERFORMANCE_MODE_FAST` (Phase 2 D-15) regardless. Sticker render and video record paths NOT throttled (face detection is the CPU hot spot per PITFALLS §4).

### Recording Quality + Format + Filename (gray area 4)
- **D-15:** **Resolution = `Quality.HD` (720p)** matching Phase 2 D-16 ImageAnalysis preview resolution. Avoids FullHD overhead on mid-tier devices per PRF-04 budget (≤40 MB APK + smooth recording on 2019 Snapdragon-675-class). Quality fallback chain via `QualitySelector.from(Quality.HD, FallbackStrategy.lowerQualityOrHigherThan(Quality.HD))`.
- **D-16:** **Codec = H264 default** (CameraX `Recorder` chooses automatically — H264 baseline profile, max compatibility). Bitrate auto (Recorder defaults). No H265 opt-in (Android 9+ minSdk supports both but compatibility favors H264 for share intent targets).
- **D-17:** **Filename `Bugzz_YYYYMMDD_HHmmss.mp4`** matching Phase 3 D-32 JPEG pattern. Same `DCIM/Bugzz/` directory via `MediaStoreOutputOptions(contentResolver, MediaStore.Video.Media.EXTERNAL_CONTENT_URI, contentValues)` where `contentValues` sets `RELATIVE_PATH=DCIM/Bugzz`, `MIME_TYPE=video/mp4`, `DISPLAY_NAME=Bugzz_<timestamp>.mp4`. CameraX handles `IS_PENDING` transaction internally (validated for ImageCapture in Phase 3; same Recorder API).
- **D-18:** **Audio source = device microphone (AudioSource.MIC default).** Recorder muxes audio + video into single MP4 output. No separate audio stream — Recorder handles MediaMuxer internals. Audio sync target <50ms drift over 60s (PRF-03 — Phase 7 measures formally; Phase 5 ships and observes subjectively).

### Architecture Additions (derived from D-01..D-18)
- **D-19:** **New files Phase 5:**
  - `app/src/main/java/com/bugzz/filter/camera/ui/insect/InsectFilterScreen.kt` (Compose screen with `pointerInput { detectTransformGestures }`)
  - `app/src/main/java/com/bugzz/filter/camera/ui/insect/InsectFilterViewModel.kt` (StickerState StateFlow + handlers + reuses CameraController + FilterEngine + AssetLoader + FilterCatalog)
  - `app/src/main/java/com/bugzz/filter/camera/ui/insect/StickerState.kt` (data class with offset/scale/rotation)
  - `app/src/main/java/com/bugzz/filter/camera/render/StickerRenderer.kt` (@Singleton @Inject; `setStickerState(StickerState)`, `setActiveFilter(FilterDefinition)`, `onDraw(canvas, frame)`)
  - `app/src/main/java/com/bugzz/filter/camera/thermal/ThermalMonitor.kt` (@Singleton @Inject ApplicationContext; registers PowerManager listener; exposes `status: StateFlow<ThermalStatus>`)
  - `app/src/main/java/com/bugzz/filter/camera/recording/VideoRecorder.kt` (@Singleton @Inject; wraps Recorder + handles VideoRecordEvent dispatch)
  - Tests: `InsectFilterViewModelTest`, `StickerStateTest` (Pure JVM gesture math), `StickerRendererTest` (Robolectric Canvas), `ThermalMonitorTest`, `VideoRecorderTest` (Robolectric Recorder)
- **D-20:** **OverlayEffectBuilder extended** to inject optional `StickerRenderer` alongside `FilterEngine` and `DebugOverlayRenderer`. `setOnDrawListener` body branches on `cameraMode`:
  ```kotlin
  if (cameraMode == FaceFilter) {
      filterEngine.onDraw(canvas, frame, faces)
  } else {  // InsectFilter
      stickerRenderer.onDraw(canvas, frame)
  }
  if (BuildConfig.DEBUG) debugOverlayRenderer.draw(canvas, frame, faces)
  ```
- **D-21:** **CameraController extends** for video recording lifecycle:
  - `fun startRecording(audioEnabled: Boolean): Recording` — invokes `videoCapture.output.prepareRecording()`, sets `withDurationLimit(60_000L * 1_000_000L)`, conditional `withAudioEnabled()`, then `start(cameraExecutor) { event -> handleEvent(event) }`. Returns active `Recording` handle.
  - `fun stopRecording(recording: Recording)` — invokes `recording.stop()`. Recorder fires Finalize callback.
  - Internal: `private var activeRecording: Recording? = null` for state tracking. Single-recording invariant (no concurrent recordings).
- **D-22:** **Recording state machine** in CameraViewModel:
  ```kotlin
  sealed interface RecordingState {
    data object Idle : RecordingState
    data class Active(val elapsedMs: Long, val hasAudio: Boolean) : RecordingState
    data object Stopping : RecordingState
    data class Error(val message: String) : RecordingState
  }
  ```
  Idle → tap record → check RECORD_AUDIO → grant: Active(0, true) | deny: Active(0, false) | rationale: Idle. Active → tap record OR 60s timeout → Stopping → Recorder.Finalize → Idle + emit `OneShotEvent.VideoSaved(uri)`. On error → Error(msg) → emit `OneShotEvent.VideoError(msg)`.

### Lock-During-Recording Implementation
- **D-23:** Lock UI in `CameraScreen` and `InsectFilterScreen`:
  - Picker `LazyRow` items: `Modifier.alpha(if (isRecording) 0.5f else 1f)` + tap handler early-return when `isRecording`
  - Flip button: `enabled = !isRecording`
  - Sticker gestures (InsectFilterScreen): `Modifier.pointerInput(isRecording) { if (isRecording) return@pointerInput; detectTransformGestures(...) }`
  - Shutter (CameraScreen): NOT locked — shutter still captures photo during video recording (CameraX 1.6 supports concurrent ImageCapture + VideoCapture). Photo gets baked overlay at the time of shutter; video continues uninterrupted.
- **D-24:** `BackHandler(enabled = isRecording)` in CameraScreen + InsectFilterScreen → intercepts back press → shows `AlertDialog` per D-10. Passing back press OUT of the screen during record requires explicit "Discard" tap.

### Permission Manifest
- **D-25:** AndroidManifest already declares RECORD_AUDIO (Phase 1 D-12) — no manifest change. Permission is requested at runtime per D-12.

### Phase 4 Inheritance Preservation
- **D-26:** **All Phase 3 + Phase 4 fix commits preserved verbatim.** Phase 5 plans must grep-assert:
  - `isCapturing` guard in CameraViewModel.onShutterTapped (Phase 3 commit `dafc21e`)
  - `bindJob?.cancel()` pattern (Phase 3 commit `9abbd0b`)
  - `OneShotEvent.FilterLoadError` variant (Phase 3 commit `6ff00e0`)
  - capture-flash visible inside `onSuccess` branch (Phase 3 commit `4e94591`)
  - `require(frameCount > 0)` in FilterDefinition init (Phase 3 commit `b7f74cf`)
  - `assetLoader.preload(def.assetDir)` not `def.id` (Phase 4 commit `514410c` — 04-gaps-01 fix)
  - Phase 5 adds analogous: `isRecording` guard on Record button to prevent double-tap concurrent recordings.

### Claude's Discretion
- Exact ThermalStatusListener integration details (PowerManager API since API 29 — `getThermalStatus()` poll vs callback registration vs `addThermalStatusListener`)
- Recording indicator exact animation easing for red dot blink (LinearOutSlowInEasing vs FastOutLinearInEasing — taste call)
- Whether to show 60s countdown last-10s warning (e.g. timer text turns red at 50s) — Phase 5 ships without; Phase 6 UX may add
- Sticker boundary clamp formula exact constants (50% overflow allowed — fine-tune per device orientation)
- Whether sticker survives orientation change via Modifier.graphicsLayer alone OR explicit OrientationEventListener observation (test on Xiaomi 13T; default Modifier-only)
- VideoRecordEvent dispatcher (cameraExecutor vs renderExecutor vs Main — default cameraExecutor matching Phase 3 ImageCapture pattern)
- Whether to log video recording metadata (Timber.tag("VideoRecorder")) at Verbose vs Debug level — default Verbose for development; Phase 7 may quiet
- Insect Filter sticker initial filter selection logic — restore last-used from DataStore (extending FilterPrefsRepository)? OR always start with first catalog entry? Default: restore last-used (reuse Phase 4 CAT-05 pattern)
- AlertDialog wording exact text for "Recording in progress" / "Discard recording?" — match 04-UI-SPEC tone (English, terse)
- Locked-during-record visual treatment exact alpha (0.5f recommended; could be 0.3f for stronger signal)

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Project specs
- `.planning/PROJECT.md` — locked stack, English UI, fully offline, no Firebase
- `.planning/REQUIREMENTS.md` §Video Recording (VID-01..10) + §Dual Mode (MOD-03..07) — Phase 5 primary scope
- `.planning/ROADMAP.md` §Phase 5 — goal + 5 success criteria
- `.planning/STATE.md` §Accumulated Context — execution learnings from Phases 1-4

### Prior phases — MANDATORY reading
- `.planning/phases/04-filter-catalog-picker-face-filter-mode/04-CONTEXT.md` — D-01..D-30 lock catalog + picker + Home + multi-face — Phase 5 inherits
- `.planning/phases/04-filter-catalog-picker-face-filter-mode/04-08-SUMMARY.md` — Phase 4 closure including 04-gaps-01 AssetLoader assetDir fix
- `.planning/phases/04-filter-catalog-picker-face-filter-mode/04-UI-SPEC.md` — design tokens (color/spacing/typography/motion) Phase 5 inherits + extends with sticker gesture interaction patterns
- `.planning/phases/04-filter-catalog-picker-face-filter-mode/04-VERIFICATION.md` — Phase 4 5/5 must-haves; 04-HUMAN-UAT.md 2 deferred items still open
- `.planning/phases/03-first-filter-end-to-end-photo-capture/03-CONTEXT.md` — D-01..D-40 Phase 3 locks (FilterEngine API, AssetLoader, BboxIouTracker, ImageCapture pattern — VideoCapture follows same MediaStore pattern)
- `.planning/phases/02-camera-preview-face-detection-coordinate-validation/02-CONTEXT.md` — D-04..D-06 TEST RECORD VideoCapture binding precedent; D-15 ML Kit PERFORMANCE_MODE_FAST baseline
- `.planning/phases/02-camera-preview-face-detection-coordinate-validation/02-ADR-01-no-ml-kit-tracking-with-contour.md` — ADR-01 BboxIouTracker context (Insect mode disables face detection so this ADR doesn't apply to Insect path)

### Research base
- `.planning/research/STACK.md` — CameraX 1.6 Recorder + VideoCapture pre-catalogued
- `.planning/research/ARCHITECTURE.md` §3 (rendering pipeline — same OverlayEffect; sticker just routes through different draw callback), §5 (data flow for capture — Recorder mirrors ImageCapture)
- `.planning/research/PITFALLS.md` §2 (video loses overlay — already solved Phase 2 OverlayEffect VIDEO_CAPTURE target validated), §4 (ImageAnalysis backpressure — preserved), §7 (device fragmentation — Xiaomi 13T MIUI thermal behaviors), §8 (audio sync — RECORD_AUDIO permission + AudioSource.MIC pattern)

### CameraX / Compose external docs
- [CameraX Recorder + VideoCapture guide](https://developer.android.com/media/camera/camerax/capture-video) — full pattern: Recorder.Builder + setQualitySelector + VideoCapture.withOutput + prepareRecording + withDurationLimit + withAudioEnabled + start
- [Compose detectTransformGestures](https://developer.android.com/develop/ui/compose/touch-input/pointer-input/multi-touch) — single API for pan/zoom/rotate
- [PowerManager.addThermalStatusListener](https://developer.android.com/reference/android/os/PowerManager#addThermalStatusListener%28android.os.PowerManager.OnThermalStatusChangedListener%29) — API 29+ thermal callback
- [MediaStore Video.Media](https://developer.android.com/training/data-storage/shared/media#video) — IS_PENDING + RELATIVE_PATH for MP4
- [ActivityResultContracts.RequestPermission](https://developer.android.com/reference/androidx/activity/result/contract/ActivityResultContracts.RequestPermission) — Phase 1 D-13 pattern reuse
- [Compose AlertDialog (Material3)](https://developer.android.com/develop/ui/compose/components/dialog) — exit-during-record confirmation

### Reference APK (behavioral reference)
- `reference/com.insect.filters.funny.prank.bug.filter.face.camera.apk` — observed Phase 4 device runs; reference app records video via similar Recorder pattern (validates approach but not directly extractable)

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets (from Phases 1-4)
- `com.bugzz.filter.camera.camera.CameraController` — already binds VideoCapture in UseCaseGroup (Phase 2 D-06); existing `startTestRecording()` method (debug-only, 5s, no audio) — replace with production `startRecording(audioEnabled): Recording` + `stopRecording(Recording)` per D-21. **Do NOT delete `startTestRecording`** until verified production path replaces it; Phase 5 plan can choose to delete or keep behind `BuildConfig.DEBUG` (Claude discretion).
- `com.bugzz.filter.camera.render.OverlayEffectBuilder` — extend constructor to inject `StickerRenderer`; setOnDrawListener body branches per `cameraMode` (D-20). Existing wiring with `FilterEngine` + `DebugOverlayRenderer` preserved.
- `com.bugzz.filter.camera.render.FilterEngine` — unchanged for Phase 5 (face-anchored render only used in FaceFilter mode); `FilterDefinition` schema unchanged.
- `com.bugzz.filter.camera.filter.FilterCatalog` — unchanged; reused by InsectFilter mode.
- `com.bugzz.filter.camera.filter.AssetLoader` — unchanged (assetDir-based per Phase 4 04-gaps-01 fix).
- `com.bugzz.filter.camera.detector.FaceDetectorClient` — extends `createAnalyzer()` consumer to read `ThermalMonitor.status` and frame-skip when ≥ Moderate (D-14).
- `com.bugzz.filter.camera.detector.BboxIouTracker` — only used in FaceFilter mode; Insect mode bypasses entirely.
- `com.bugzz.filter.camera.ui.camera.CameraScreen` — extend with Production Record button at BottomStart (replacing TEST RECORD per D-07), recording indicator at TopCenter (D-08), AlertDialog (D-10), lock-during-record state (D-23). Shutter button preserved at BottomCenter.
- `com.bugzz.filter.camera.ui.camera.CameraViewModel` — extend with RecordingState sealed (D-22), onRecordTapped() handler, recording lifecycle observer collecting VideoRecordEvent.
- `com.bugzz.filter.camera.ui.camera.OneShotEvent` — add `VideoSaved(uri)` + `VideoError(message)` variants (alongside existing PhotoSaved/PhotoError/FilterLoadError).
- `com.bugzz.filter.camera.ui.nav.Routes` — `CameraRoute(mode = CameraMode.InsectFilter)` already enum'd in Phase 4 D-20; Phase 5 activates → routes to new `InsectFilterScreen` instead of stub.
- `com.bugzz.filter.camera.ui.BugzzApp` — nav graph rewires `CameraRoute(InsectFilter)` from Phase 4 stub to new InsectFilterScreen. CameraRoute(FaceFilter) unchanged.
- `com.bugzz.filter.camera.ui.home.InsectFilterStubScreen` — DELETE (replaced by production InsectFilterScreen).
- `com.bugzz.filter.camera.BugzzApplication` — register ThermalMonitor singleton on `onCreate`.

### Established Patterns (replicate)
- **Hilt @Singleton @Inject + constructor-split** (Phase 2 STATE #14) for any test-substitutable factory
- **OneShotEvent Channel + collectAsStateWithLifecycle** (Phase 3 + 4) for one-time UI events
- **DataStore preferences** (Phase 4 D-25) — sticker state COULD persist via similar repository (deferred to Phase 6 — see D-04 + Deferred)
- **Compose Material3 with 04-UI-SPEC tokens** — color (`#FFE53935` red for record button), spacing 4dp grid, typography Material3 type scale
- **kotlinx.serialization.json** for any new JSON parsing (e.g. recording metadata, if needed)
- **AndroidManifest unchanged** — RECORD_AUDIO already declared since Phase 1 D-12

### Integration Points
- `gradle/libs.versions.toml` — verify `androidx.camera:camera-video:1.6.0` already present (came with Phase 2 CameraX bundle); no new deps expected
- `app/build.gradle.kts` — no changes
- `AndroidManifest.xml` — RECORD_AUDIO already present; no changes
- `di/CameraModule.kt` — add `@Provides` for `ThermalMonitor` (or use @Singleton @Inject directly)
- `di/DataModule.kt` — unchanged (sticker state not persisted Phase 5)

</code_context>

<specifics>
## Specific Ideas

- User runs **stop-test per phase on Xiaomi 13T** (memory `feedback_cadence.md`). Phase 5 follows Phase 3 + 4 pattern: discuss → plan → execute Waves 0-N autonomously → stop at device checkpoint → user verifies via 05-HANDOFF.md runbook.
- User explicitly delegated full autonomy for Phase 5 ("Bạn chạy auto toàn bộ đi nhé, ko cần hỏi tôi"). Per memory `feedback_autonomy.md`, all gray areas auto-locked to Recommended above. Mid-execution questions during planner/executor agents → recommended/default.
- Phase 5 is the LAST production feature phase before Phase 6 UX polish. After Phase 5 device-PASS, the app is feature-complete: photos + videos + 2 modes + 15 filters + share-ready (Phase 6 wires share intent).
- ROADMAP "Key Decision" merges 2 feature blocks: video and Insect Filter. Phase 5 plans should organize them as parallel waves where possible (no shared file edits) OR sequential if shared infra changes (CameraScreen + CameraViewModel are shared touch-points).
- Phase 4 04-HUMAN-UAT.md 2 deferred items (multi-face 2-person + fps subjective) still open — Phase 5 device handoff is opportunity to close them. Plan 05-HANDOFF runbook should include these as bonus checks.

</specifics>

<deferred>
## Deferred Ideas

- **Sticker state persistence across app launch** — Phase 5 ships in-VM only; survives flip + orientation but not process death. Phase 6 may extend via `StickerPrefsRepository` (DataStore) if user wants. No-op for MVP personal-use.
- **Multi-sticker mode** (place 2-3 stickers simultaneously) — Phase 5 single sticker only. Future creative expansion.
- **Sticker rotation snap-to-angle** (snap every 15° or 45°) — Phase 5 free rotation only. Phase 6 UX polish if needed.
- **Sticker color tint / hue rotation** — out of scope.
- **Video editing/trimming post-record** — POL-04 v2.
- **Music overlay / soundtrack** — POL-03 v2.
- **Countdown timer (3s/5s/10s)** before record start — POL-01 v2.
- **Watermark overlay** — POL-04 v2.
- **Recording at 1080p / FullHD** — Phase 5 720p only; Phase 7 cross-OEM may revisit if devices support without performance regression.
- **H265 / HEVC codec opt-in** — Phase 5 H264 only for compatibility. Phase 6+ may add settings toggle.
- **Multi-mic / spatial audio** — single device mic only.
- **Per-recording mute toggle** — record audio is binary (granted or denied at first request); no in-session mute. Future extension.
- **Stop button separate from record toggle** — Phase 5 single toggle; Phase 6 UX may split.
- **Last-10s warning UI** (timer turns red at 50s) — Phase 5 plain timer; Phase 6 polish.
- **Recording quality picker in Settings** — Phase 6 UX-09 (settings screen content).
- **Reference APK runtime comparison for video file format** — Phase 4 already established reference is online-first; offline match-parity for video pattern not feasible.
- **Phase 4 04-HUMAN-UAT 2 items** — multi-face 2-person + fps subjective; Phase 5 device handoff opportunity to close opportunistically.

</deferred>

---

*Phase: 05-video-recording-audio-insect-filter-free-placement-mode*
*Context gathered: 2026-05-04 (auto-locked recommended defaults per user delegation)*
