package com.bugzz.filter.camera.render

import android.graphics.PointF

/**
 * Mutable per-bug animation state (D-04 / REN-02).
 *
 * Sized for all 4 [BugBehavior] variants so Phase 4 can implement CRAWL/SWARM/FALL
 * without changing this data class shape.
 *
 * All fields are mutable to allow zero-allocation in-place updates inside the draw loop.
 *
 * @property position                  Current sprite centre in sensor-space coordinates.
 * @property velocity                  Current movement vector in sensor-space pixels/ms.
 * @property pathProgress              Progress [0..1] along the current contour path (CRAWL).
 * @property lastFrameIndex            Flipbook frame index last drawn.
 * @property lastFrameAdvanceTimestampNanos  [OverlayEffect.Frame.timestamp] when the frame last advanced.
 */
data class BugState(
    val position: PointF = PointF(0f, 0f),
    val velocity: PointF = PointF(0f, 0f),
    var pathProgress: Float = 0f,
    var lastFrameIndex: Int = 0,
    var lastFrameAdvanceTimestampNanos: Long = 0L,
)
