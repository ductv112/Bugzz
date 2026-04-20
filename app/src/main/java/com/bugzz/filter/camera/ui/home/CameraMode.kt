package com.bugzz.filter.camera.ui.home

import kotlinx.serialization.Serializable

/**
 * CameraScreen launch mode (04-CONTEXT D-20 / 04-RESEARCH Pattern 8).
 *
 * @Serializable so navigation-compose 2.8.x type-safe route serialization works with the enum arg
 * on [com.bugzz.filter.camera.ui.nav.CameraRoute].
 */
@Serializable
enum class CameraMode {
    /** Landmark-tracked face filter (Phase 3 + Phase 4 impl). */
    FaceFilter,

    /** Free-placement sticker mode — Phase 5 MOD-03..07 will activate; Phase 4 shows stub. */
    InsectFilter,
}
