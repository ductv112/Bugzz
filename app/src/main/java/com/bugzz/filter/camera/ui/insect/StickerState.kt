package com.bugzz.filter.camera.ui.insect

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize

/**
 * Sticker placement state for InsectFilter free-placement mode (D-03).
 *
 * Scale clamped [0.3f, 3.0f]; offset allows 50% sprite overflow at preview edges
 * (sticker center may move off-screen by half its rendered size); rotation unbounded mod 360.
 *
 * Phase 5.
 */
data class StickerState(
    val offset: Offset = Offset.Zero,
    val scale: Float = 1f,
    val rotation: Float = 0f,
) {
    /**
     * Returns a new [StickerState] with gesture transforms applied.
     *
     * @param pan         Translation delta from detectTransformGestures (D-03).
     * @param zoom        Scale multiplier from pinch gesture; output clamped to [MIN_SCALE, MAX_SCALE].
     * @param rotationDelta  Rotation delta in degrees from two-finger rotation gesture.
     * @param previewSize Current preview dimensions — used to compute overflow clamp boundary.
     * @param bitmapSize  Rendered sprite size — used to compute half-width/height for overflow.
     */
    fun applyGesture(
        pan: Offset,
        zoom: Float,
        rotationDelta: Float,
        previewSize: IntSize,
        bitmapSize: IntSize,
    ): StickerState {
        val newScale = (scale * zoom).coerceIn(MIN_SCALE, MAX_SCALE)
        val halfW = (bitmapSize.width * newScale) / 2f
        val halfH = (bitmapSize.height * newScale) / 2f
        val newOffsetX = (offset.x + pan.x).coerceIn(-halfW, previewSize.width + halfW)
        val newOffsetY = (offset.y + pan.y).coerceIn(-halfH, previewSize.height + halfH)
        // Normalise to [0, 360) to keep values predictable for tests.
        val newRotation = ((rotation + rotationDelta) % 360f + 360f) % 360f
        return StickerState(
            offset = Offset(newOffsetX, newOffsetY),
            scale = newScale,
            rotation = newRotation,
        )
    }

    /**
     * Returns a [StickerState] centred on the preview (D-02 initial position).
     * scale=1f, rotation=0f.
     */
    fun centerOn(previewSize: IntSize): StickerState = StickerState(
        offset = Offset(previewSize.width / 2f, previewSize.height / 2f),
        scale = 1f,
        rotation = 0f,
    )

    companion object {
        const val MIN_SCALE = 0.3f
        const val MAX_SCALE = 3.0f
    }
}
