package com.bugzz.filter.camera.ui.insect

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure JVM unit tests for StickerState clamp math — MOD-03..06.
 * Un-Ignored in Plan 05-02 when SUT landed.
 */
class StickerStateTest {

    private val previewSize = IntSize(720, 1280)
    private val bitmapSize = IntSize(200, 200)

    @Test
    fun initialPosition_isCenter() {
        val state = StickerState().centerOn(previewSize)
        assertEquals(360f, state.offset.x, 0.001f)
        assertEquals(640f, state.offset.y, 0.001f)
        assertEquals(1f, state.scale, 0.001f)
        assertEquals(0f, state.rotation, 0.001f)
    }

    @Test
    fun drag_updatesOffsetByPanAmount() {
        val initial = StickerState(offset = Offset(200f, 300f))
        val pan = Offset(50f, -30f)
        val result = initial.applyGesture(
            pan = pan, zoom = 1f, rotationDelta = 0f,
            previewSize = previewSize, bitmapSize = bitmapSize,
        )
        assertEquals(250f, result.offset.x, 0.001f)
        assertEquals(270f, result.offset.y, 0.001f)
    }

    @Test
    fun pinch_updatesScale_clampedToMax3() {
        val initial = StickerState(scale = 1f)
        val result = initial.applyGesture(
            pan = Offset.Zero, zoom = 10f, rotationDelta = 0f,
            previewSize = previewSize, bitmapSize = bitmapSize,
        )
        assertEquals(StickerState.MAX_SCALE, result.scale, 0.001f)
    }

    @Test
    fun pinch_updatesScale_clampedToMin0_3() {
        val initial = StickerState(scale = 1f)
        val result = initial.applyGesture(
            pan = Offset.Zero, zoom = 0.01f, rotationDelta = 0f,
            previewSize = previewSize, bitmapSize = bitmapSize,
        )
        assertEquals(StickerState.MIN_SCALE, result.scale, 0.001f)
    }

    @Test
    fun rotate_updatesRotation_modulo360() {
        val initial = StickerState(rotation = 0f)
        val result = initial.applyGesture(
            pan = Offset.Zero, zoom = 1f, rotationDelta = 400f,
            previewSize = previewSize, bitmapSize = bitmapSize,
        )
        // 400 % 360 = 40
        assertEquals(40f, result.rotation, 0.001f)
    }

    @Test
    fun offset_clampedTo50PercentOverflow() {
        // halfW = bitmapSize.width * scale / 2 = 200 * 1f / 2 = 100
        // maxX = previewSize.width + halfW = 720 + 100 = 820
        val initial = StickerState(offset = Offset(0f, 0f))
        val result = initial.applyGesture(
            pan = Offset(10000f, 0f), zoom = 1f, rotationDelta = 0f,
            previewSize = previewSize, bitmapSize = bitmapSize,
        )
        assertEquals(820f, result.offset.x, 0.001f)
        // Y unchanged
        assertTrue(result.offset.y >= -100f)
    }
}
