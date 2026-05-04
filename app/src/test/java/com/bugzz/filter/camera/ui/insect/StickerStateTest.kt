package com.bugzz.filter.camera.ui.insect

import org.junit.Assert
import org.junit.Ignore
import org.junit.Test

/**
 * RED scaffold for MOD-03..06. SUT (StickerState data class + clamp helpers) lands in Plan 05-02.
 *
 * Expected API shape (Plan 05-02):
 *   data class StickerState(
 *       val offset: androidx.compose.ui.geometry.Offset = Offset.Zero,
 *       val scale: Float = 1f,
 *       val rotation: Float = 0f,
 *   ) {
 *       fun drag(panX: Float, panY: Float): StickerState  // adds panAmount to offset
 *       fun pinch(zoomChange: Float): StickerState        // scale * zoomChange, clamped [0.3f, 3.0f]
 *       fun rotate(rotationDeg: Float): StickerState      // rotation + rotationDeg, mod 360
 *       fun clampOffset(previewWidth: Float, previewHeight: Float): StickerState
 *           // offset clamped so sticker center stays within [-50%..+150%] of preview size
 *   }
 *
 * D-03 constraints:
 *   - scale: clamped to [0.3f, 3.0f]
 *   - offset: sticker center may overflow edge by 50% of preview dimension
 *   - rotation: mod 360 (unbounded input, output in [0, 360))
 *
 * Initial state per D-02: sticker spawns at preview center when InsectFilter mode entered.
 * The ViewModel sets offset to (previewWidth/2, previewHeight/2) on mode entry.
 */
class StickerStateTest {

    @Ignore("Plan 05-02 lands StickerState SUT")
    @Test
    fun initialPosition_isCenter() {
        Assert.fail("Plan 05-02 lands SUT")
    }

    @Ignore("Plan 05-02 lands StickerState SUT")
    @Test
    fun drag_updatesOffsetByPanAmount() {
        Assert.fail("Plan 05-02 lands SUT")
    }

    @Ignore("Plan 05-02 lands StickerState SUT")
    @Test
    fun pinch_updatesScale_clampedToMax3() {
        Assert.fail("Plan 05-02 lands SUT")
    }

    @Ignore("Plan 05-02 lands StickerState SUT")
    @Test
    fun pinch_updatesScale_clampedToMin0_3() {
        Assert.fail("Plan 05-02 lands SUT")
    }

    @Ignore("Plan 05-02 lands StickerState SUT")
    @Test
    fun rotate_updatesRotation_modulo360() {
        Assert.fail("Plan 05-02 lands SUT")
    }

    @Ignore("Plan 05-02 lands StickerState SUT")
    @Test
    fun offset_clampedTo50PercentOverflow() {
        Assert.fail("Plan 05-02 lands SUT")
    }
}
