package com.bugzz.filter.camera.render

import android.graphics.PointF
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Nyquist unit test for [DebugOverlayRenderer] helper functions (GAP-02-B / CAM-07).
 *
 * Pins the two post-GAP-02-B correctness contracts:
 * 1. `centroidOf(points)` — mean x/y for the dot-density reduction (D-01 amendment).
 * 2. `computeSensorSpaceStroke(devicePx, scale)` — matrix-scale compensation formula
 *    (H2 root-cause fix).
 *
 * Robolectric is required because `android.graphics.PointF`'s two-arg constructor does
 * not store its `x` / `y` fields under the JVM `android.jar` stub when the project has
 * `testOptions.unitTests.isReturnDefaultValues = true` (see `app/build.gradle.kts`).
 * Robolectric provides a real shadow implementation so `PointF(x, y).x == x` holds,
 * which the `centroidOf_*` assertions depend on.
 *
 * Canvas.drawRect / drawCircle paths are verified on-device per 02-HANDOFF.md Step 8-9.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])  // Robolectric SDK shadows; matches CameraControllerTest convention.
class DebugOverlayRendererTest {

    @Test
    fun centroidOf_empty_list_is_null() {
        assertNull(DebugOverlayRenderer.centroidOf(emptyList()))
    }

    @Test
    fun centroidOf_single_point_is_the_point() {
        val c = DebugOverlayRenderer.centroidOf(listOf(PointF(100f, 200f)))!!
        assertEquals(100f, c.x, 0.001f)
        assertEquals(200f, c.y, 0.001f)
    }

    @Test
    fun centroidOf_multiple_points_is_mean() {
        val points = listOf(
            PointF(0f, 0f),
            PointF(100f, 100f),
            PointF(200f, 200f),
        )
        val c = DebugOverlayRenderer.centroidOf(points)!!
        assertEquals(100f, c.x, 0.001f)
        assertEquals(100f, c.y, 0.001f)
    }

    @Test
    fun computeSensorSpaceStroke_at_scale_1_is_identity() {
        // Matrix scale 1.0 (no transform) -> sensor-space stroke equals device-pixel stroke.
        val s = DebugOverlayRenderer.computeSensorSpaceStroke(4f, 1.0f)
        assertEquals(4f, s, 0.001f)
    }

    @Test
    fun computeSensorSpaceStroke_at_scale_2_halves_stroke() {
        // Matrix scales sensor -> buffer by 2x. To render at 4 device pixels, the stroke
        // value assigned to Paint must be 4/2 = 2 in sensor-space (H2 GAP-02-B fix).
        val s = DebugOverlayRenderer.computeSensorSpaceStroke(4f, 2.0f)
        assertEquals(2f, s, 0.001f)
    }

    @Test
    fun computeSensorSpaceStroke_at_representative_xiaomi_scale_matches_expected() {
        // Representative scaleX captured from Wave A diagnostic logs on Xiaomi 13T
        // (sensor 720x1280 -> overlay buffer ~927x1920 ~= 1.29x scale). 4f / 1.29 ~= 3.10f.
        val s = DebugOverlayRenderer.computeSensorSpaceStroke(4f, 1.29f)
        assertEquals(3.10f, s, 0.05f)
    }

    @Test
    fun computeSensorSpaceStroke_at_zero_scale_returns_finite_value() {
        // MIN_SAFE_SCALE guard prevents NaN / infinity when matrix is pathological.
        val s = DebugOverlayRenderer.computeSensorSpaceStroke(4f, 0f)
        // 4 / MIN_SAFE_SCALE (0.0001) = 40000f - finite, not infinity or NaN.
        assertEquals(40_000f, s, 1f)
    }
}
