package com.bugzz.filter.camera.camera

/**
 * Lens selector enum + toggle helper.
 *
 * Default to FRONT on first launch (D-24 — prank app starts on selfie cam). Consumed by
 * CameraViewModel (Plan 05) to drive CameraController.flipLens(). Separate object so lens
 * logic stays out of the ViewModel + CameraController and is unit-testable.
 */
enum class CameraLens { FRONT, BACK }

object CameraLensProvider {
    /** Toggle FRONT⇄BACK. Called on flip button tap. */
    fun next(current: CameraLens): CameraLens = when (current) {
        CameraLens.FRONT -> CameraLens.BACK
        CameraLens.BACK  -> CameraLens.FRONT
    }
}
