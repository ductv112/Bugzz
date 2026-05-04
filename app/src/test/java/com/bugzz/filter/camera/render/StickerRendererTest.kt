package com.bugzz.filter.camera.render

import org.junit.Assert
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * RED scaffold for sticker draw transform contract. SUT lands in Plan 05-02.
 *
 * Verifies canvas.save() → translate → rotate → scale → drawBitmap → restore
 * exact call order via Mockito InOrder verifier on a mock Canvas.
 *
 * Expected API shape (Plan 05-02):
 *   @Singleton
 *   class StickerRenderer @Inject constructor(private val assetLoader: AssetLoader) {
 *       fun setStickerState(state: StickerState)
 *       fun setActiveFilter(filter: FilterDefinition)
 *       fun onDraw(canvas: Canvas, frame: Frame)
 *           // draws sprite at state.offset with state.scale + state.rotation applied:
 *           //   canvas.save()
 *           //   canvas.translate(state.offset.x, state.offset.y)
 *           //   canvas.rotate(state.rotation)
 *           //   canvas.scale(state.scale, state.scale)
 *           //   canvas.drawBitmap(frame, -w/2, -h/2, paint)  // centered at origin
 *           //   canvas.restore()
 *   }
 *
 * D-06: StickerRenderer is a @Singleton @Inject class sharing OverlayEffectBuilder infrastructure.
 * D-20: OverlayEffectBuilder.setOnDrawListener branches on cameraMode — StickerRenderer.onDraw
 *        called when mode == InsectFilter; FilterEngine.onDraw called when mode == FaceFilter.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class StickerRendererTest {

    @Ignore("Plan 05-02 lands StickerRenderer SUT")
    @Test
    fun setStickerState_mutatesInternalState() {
        Assert.fail("Plan 05-02 lands SUT")
    }

    @Ignore("Plan 05-02 lands StickerRenderer SUT")
    @Test
    fun setActiveFilter_loadsFromAssetLoader() {
        Assert.fail("Plan 05-02 lands SUT")
    }

    @Ignore("Plan 05-02 lands StickerRenderer SUT")
    @Test
    fun onDraw_appliesTranslateScaleRotateInOrder() {
        Assert.fail("Plan 05-02 lands SUT")
    }

    @Ignore("Plan 05-02 lands StickerRenderer SUT")
    @Test
    fun onDraw_emptyFrames_returnsEarly() {
        Assert.fail("Plan 05-02 lands SUT")
    }

    @Ignore("Plan 05-02 lands StickerRenderer SUT")
    @Test
    fun onDraw_drawsBitmapCenteredAtOrigin() {
        Assert.fail("Plan 05-02 lands SUT")
    }
}
