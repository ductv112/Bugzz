package com.bugzz.filter.camera.ui.camera

/**
 * Recording lifecycle state machine (D-22).
 *
 * State transitions:
 *   Idle → tap record → check RECORD_AUDIO → granted: Active(0, true) | denied: Active(0, false)
 *   Active → tap record OR 60s timeout → Stopping
 *   Stopping → Recorder.Finalize → Idle + emit OneShotEvent.VideoSaved(uri)
 *   Any state → error → Error(msg) → Idle
 *
 * Consolidated here (Plan 05-02) per WARNING 6: Plan 05-03 only wires the CameraController
 * recording lifecycle handlers — it does NOT recreate or move this type. InsectFilterUiState
 * imports this canonical type directly.
 *
 * Shared by:
 *   - CameraViewModel (Plan 05-03 wires the lifecycle handlers)
 *   - InsectFilterUiState (Plan 05-02 Task 3 imports this type)
 *
 * Phase 5.
 */
sealed interface RecordingState {
    data object Idle : RecordingState
    data class Active(val elapsedMs: Long = 0L, val hasAudio: Boolean) : RecordingState
    data object Stopping : RecordingState
    data class Error(val message: String) : RecordingState
}
