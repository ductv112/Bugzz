package com.bugzz.filter.camera.ui.insect

import androidx.compose.ui.unit.IntSize
import com.bugzz.filter.camera.camera.CameraLens
import com.bugzz.filter.camera.ui.camera.RecordingState

/**
 * UI state for InsectFilterScreen. Mirrors CameraUiState shape (Phase 4 D-14)
 * with StickerState replacing face-anchored fields.
 *
 * Phase 5 D-04: stickerState lives in InsectFilterViewModel's StateFlow. Survives camera
 * flip + orientation change. Does NOT persist across app launch (Phase 6 may extend).
 *
 * [recordingState] uses the canonical [RecordingState] from ui/camera/ — no placeholder
 * churn (WARNING 6 closure: RecordingState was created in Plan 05-02 Task 2).
 */
data class InsectFilterUiState(
    val selectedFilterId: String? = null,
    val stickerState: StickerState = StickerState(),
    val previewSize: IntSize = IntSize.Zero,
    val bitmapSize: IntSize = IntSize.Zero,
    val lens: CameraLens = CameraLens.FRONT,
    val captureFlashVisible: Boolean = false,
    val recordingState: RecordingState = RecordingState.Idle,
) {
    val isRecording: Boolean get() = recordingState is RecordingState.Active
}
