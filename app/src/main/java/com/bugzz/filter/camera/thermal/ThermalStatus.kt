package com.bugzz.filter.camera.thermal

/**
 * Thermal status levels mapped from PowerManager constants (API 29+).
 * Ordinal ordering: None=0 < Light=1 < Moderate=2 < Severe=3 < Critical=4 < Emergency=5 < Shutdown=6
 *
 * D-14: status >= Moderate triggers FaceDetectorClient frame-skip throttle.
 * Phase 5.
 */
enum class ThermalStatus {
    None,
    Light,
    Moderate,
    Severe,
    Critical,
    Emergency,
    Shutdown,
}
