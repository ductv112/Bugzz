package com.bugzz.filter.camera.ui.camera

import com.bugzz.filter.camera.camera.CameraLens

/**
 * Phase 2 UI state. Fields match D-14 exactly plus `isRecording` for TEST RECORD UX (D-04).
 * No filter/selectedFilter/captureResult fields — those land in Phase 3+.
 */
data class CameraUiState(
    val lens: CameraLens = CameraLens.FRONT,
    val permissionState: PermissionState = PermissionState.Unknown,
    val isDetectorReady: Boolean = true,
    val isRecording: Boolean = false,
    val lastErrorMessage: String? = null,
    // Phase 3
    val activeFilterId: String? = null,
    val captureFlashVisible: Boolean = false,
    /** True while a capturePhoto call is in-flight; prevents re-entrance on rapid double-tap (WR-02). */
    val isCapturing: Boolean = false,
)

/**
 * Runtime CAMERA permission state (D-26/27 — Phase 2 requests ONLY Manifest.permission.CAMERA).
 */
sealed interface PermissionState {
    data object Unknown : PermissionState
    data object Denied : PermissionState
    data object Granted : PermissionState

    val isGranted: Boolean get() = this is Granted
}
