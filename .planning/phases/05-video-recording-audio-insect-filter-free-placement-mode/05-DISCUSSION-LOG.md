# Phase 5: Video Recording + Audio + Insect Filter Free-Placement Mode - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.

**Date:** 2026-05-04
**Phase:** 05 — Video Recording + Audio + Insect Filter Free-Placement Mode
**Mode:** Auto-locked recommended defaults — user delegated full autonomous run ("Bạn chạy auto toàn bộ đi nhé, ko cần hỏi tôi") per memory `feedback_autonomy.md`

---

## Areas presented

User chose: **all 4 areas auto** (no individual selection — full delegation).

| Area | Auto-decision summary |
|------|------------------------|
| 1. Insect Filter UX + state architecture | Reuse Phase 4 catalog; sticker spawns at preview center; Compose `detectTransformGestures` for pan/zoom/rotate; state in ViewModel only (no DataStore Phase 5); face detection disabled in Insect mode; new package `ui/insect/` |
| 2. Video recording UI + lifecycle | Production Record button replaces TEST RECORD at BottomStart; red dot + timer TopCenter; auto-stop via `Recorder.withDurationLimit(60s)`; AlertDialog on back-during-record; lock filter swap + Flip + sticker gestures during recording |
| 3. RECORD_AUDIO + mirror + thermal | Lazy permission via ActivityResultContracts on first record tap; `MIRROR_MODE_ON_FRONT_ONLY` wired; ThermalMonitor frame-skip throttle when ≥ Moderate |
| 4. Quality + format + filename | HD 720p via QualitySelector; H264 default codec; `Bugzz_YYYYMMDD_HHmmss.mp4` filename pattern; DCIM/Bugzz/ via MediaStore; AudioSource.MIC default |

---

## All decisions auto-locked to Recommended (no per-question alternatives evaluated since user delegated)

D-01..D-26 captured in 05-CONTEXT.md. Highlights of recommended choices:

### Area 1 (Insect Filter UX)
- D-01 sticker source = same 15-filter catalog (consistency win, zero asset duplication)
- D-02 initial position = preview center (avoids surprise jump on first tap)
- D-03 gestures = `detectTransformGestures` (single Compose API for pan+zoom+rotate)
- D-04 state in ViewModel only (Phase 5 scope; DataStore extension deferred to Phase 6)
- D-05 face detection DISABLED in Insect mode (saves CPU; aligned with mode definition)
- D-06 new package `ui/insect/`

### Area 2 (Video UI + lifecycle)
- D-07 Record button replaces TEST RECORD (no scope creep — debug button gone)
- D-08 indicator = red dot + timer TopCenter
- D-09 auto-stop via `withDurationLimit(60s)` (CameraX-native, no manual timer math)
- D-10 AlertDialog "Recording in progress / Discard recording?" wording
- D-11 lock filter+flip+gestures during recording (data integrity)

### Area 3 (Permission + mirror + thermal)
- D-12 lazy RECORD_AUDIO via ActivityResultContracts (Phase 1 D-13 pattern reuse)
- D-13 `MIRROR_MODE_ON_FRONT_ONLY` wired on VideoCapture.Builder
- D-14 ThermalMonitor frame-skip throttle (defensive; only above Moderate)

### Area 4 (Quality + format)
- D-15 HD 720p via QualitySelector (matches Phase 2 D-16 preview resolution)
- D-16 H264 codec default (compatibility with share targets)
- D-17 `Bugzz_YYYYMMDD_HHmmss.mp4` filename (consistent with Phase 3 JPEG pattern)
- D-18 AudioSource.MIC default (single-mic, no spatial audio scope)

### Architecture additions (D-19..D-26)
- D-19 new files: `InsectFilterScreen/ViewModel/StickerState`, `StickerRenderer`, `ThermalMonitor`, `VideoRecorder`
- D-20 OverlayEffectBuilder extends with StickerRenderer + branch on `cameraMode`
- D-21 CameraController.startRecording/stopRecording API
- D-22 RecordingState sealed (Idle/Active/Stopping/Error)
- D-23 lock-during-record alpha 0.5f + enabled=false on relevant controls (shutter NOT locked — concurrent ImageCapture+VideoCapture supported by CameraX 1.6)
- D-24 BackHandler intercepts back press during recording
- D-25 manifest unchanged (RECORD_AUDIO already declared Phase 1)
- D-26 grep-assert all Phase 3+4 fix commits preserved (isCapturing, bindJob, FilterLoadError, capture-flash success, frameCount>0, AssetLoader assetDir)

---

## Claude's Discretion (recorded in 05-CONTEXT.md)

10 items left to executor judgement at implementation time:
- ThermalMonitor exact API choice (poll vs callback)
- Recording indicator dot blink easing
- 60s last-10s warning (deferred Phase 6)
- Sticker boundary clamp constants
- Sticker orientation-survival mechanism (graphicsLayer alone vs OrientationEventListener)
- VideoRecordEvent dispatcher (cameraExecutor recommended)
- Logging tag verbosity
- Initial sticker filter selection (DataStore restore last-used recommended)
- AlertDialog wording exact text
- Locked-during-record alpha exact value (0.5f recommended)

## Deferred Ideas

Captured in 05-CONTEXT.md `<deferred>` section. Summary:
- Sticker state persistence across app launch (Phase 6)
- Multi-sticker mode (future)
- Sticker rotation snap-to-angle (Phase 6)
- Video editing/trimming, music overlay, watermark, countdown timer (POL v2)
- 1080p, H265, multi-mic (future)
- Stop button separate from record toggle (Phase 6)
- Recording quality picker in Settings (Phase 6 UX-09)
- Phase 4 04-HUMAN-UAT 2 deferred items — opportunistic close in Phase 5 handoff
