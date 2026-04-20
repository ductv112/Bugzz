package com.bugzz.filter.camera.ui.camera

import android.net.Uri

/**
 * One-shot side-effect events consumed via Channel(BUFFERED).receiveAsFlow() in the composable.
 * D-04 (TEST RECORD toast URI): TestRecordSaved / TestRecordFailed.
 */
sealed interface OneShotEvent {
    data class TestRecordSaved(val uri: Uri) : OneShotEvent
    data class TestRecordFailed(val reason: String) : OneShotEvent
    data class CameraError(val message: String) : OneShotEvent
    // Phase 3 (D-35) — photo capture outcomes
    data class PhotoSaved(val uri: Uri) : OneShotEvent
    data class PhotoError(val message: String) : OneShotEvent
    /** Filter asset preload failure — distinct from PhotoError so Toasts read "Filter error: …" not "Photo error: …" (WR-05). */
    data class FilterLoadError(val message: String) : OneShotEvent
}
