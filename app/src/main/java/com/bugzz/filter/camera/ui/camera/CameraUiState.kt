package com.bugzz.filter.camera.ui.camera

import com.bugzz.filter.camera.camera.CameraLens

/**
 * Phase 2 UI state. Fields match D-14 exactly plus `isRecording` for TEST RECORD UX (D-04).
 * Phase 3 additions: activeFilterId, captureFlashVisible, isCapturing.
 * Phase 4 additions (Plan 04-05): filters + selectedFilterId for picker strip (D-15/D-17).
 *
 * `activeFilterId` vs `selectedFilterId`:
 *   - `selectedFilterId` = optimistic selection set on picker tap (responds instantly).
 *   - `activeFilterId`   = what FilterEngine has actually loaded + is rendering.
 *   Both converge once preload completes; splitting them lets the picker highlight respond
 *   immediately even during a multi-frame asset preload.
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
    // Phase 4 additions (Plan 04-05)
    /** All available filters — populated from FilterCatalog.all on first bind(). */
    val filters: List<FilterSummary> = emptyList(),
    /** Optimistic selection id — set immediately on picker tap before preload completes (D-17). */
    val selectedFilterId: String = "",
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
