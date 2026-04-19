package com.bugzz.filter.camera.detector

import android.graphics.PointF

/**
 * Face → anchor-point helper.
 *
 * Phase 2 stub — Phase 3 will populate with per-anchor logic (nose tip, forehead, left cheek, etc.)
 * that production FilterEngine consumes to place bug sprites on landmark points.
 *
 * In Phase 2 only DebugOverlayRenderer draws, and it does NOT use this mapper — it iterates
 * SmoothedFace.contours + SmoothedFace.landmarks directly. So this file ships as an empty
 * object to satisfy package-layout D-12 + make Phase 3's anchor wiring an additive change.
 */
object FaceLandmarkMapper {

    /** Anchor keys Phase 3 FilterEngine will consume. Phase 2 does not resolve any. */
    enum class Anchor { NOSE_TIP, FOREHEAD, LEFT_CHEEK, RIGHT_CHEEK, CHIN, LEFT_EYE, RIGHT_EYE }

    /**
     * Resolve [anchor] on [face] to a sensor-coord point, or null if face lacks the data.
     *
     * Phase 2: always returns null (stub). Phase 3 implements the contour/landmark resolution.
     */
    fun anchorPoint(face: SmoothedFace, anchor: Anchor): PointF? {
        // Phase 3 TODO: resolve from face.contours (primary) or face.landmarks (fallback).
        return null
    }
}
