package com.bugzz.filter.camera.render

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.camera.effects.Frame
import androidx.compose.ui.geometry.Offset
import com.bugzz.filter.camera.filter.AssetLoader
import com.bugzz.filter.camera.filter.FilterDefinition
import com.bugzz.filter.camera.ui.insect.StickerState
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.InOrder
import org.mockito.Mockito.inOrder
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.isNull
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.concurrent.Executor

/**
 * Robolectric tests for StickerRenderer — verifies canvas transform call order and
 * early-return behaviour. Un-Ignored in Plan 05-02 when SUT landed.
 *
 * Uses a direct (same-thread) Executor so preloadScope.launch runs synchronously in tests.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class StickerRendererTest {

    // Direct executor — runs coroutine body on the calling thread, making preload synchronous.
    private val directExecutor = Executor { command -> command.run() }

    private lateinit var assetLoader: AssetLoader
    private lateinit var renderer: StickerRenderer
    private lateinit var canvas: Canvas
    private lateinit var frame: Frame
    private lateinit var filterDef: FilterDefinition

    // A real 10×10 ARGB_8888 bitmap so width/height are non-zero.
    private lateinit var bitmap: Bitmap

    @Before
    fun setUp() {
        assetLoader = mock(AssetLoader::class.java)
        renderer = StickerRenderer(assetLoader, directExecutor)
        canvas = mock(Canvas::class.java)
        frame = mock(Frame::class.java)
        filterDef = mock(FilterDefinition::class.java)
        bitmap = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888)

        // Default frame timestamp
        whenever(frame.timestampNanos).thenReturn(0L)
        // FilterDefinition mock values
        whenever(filterDef.frameCount).thenReturn(1)
        whenever(filterDef.frameDurationMs).thenReturn(33L)
        whenever(filterDef.assetDir).thenReturn("sprites/sprite_spider")
        whenever(filterDef.id).thenReturn("spider_test")
    }

    @Test
    fun setStickerState_mutatesInternalState() = runTest {
        val newState = StickerState(
            offset = Offset(100f, 200f),
            scale = 2f,
            rotation = 45f,
        )
        renderer.setStickerState(newState)
        renderer.setActiveFilter(filterDef)
        whenever(assetLoader.get(eq("sprites/sprite_spider"), eq(0))).thenReturn(bitmap)

        renderer.onDraw(canvas, frame)

        // Verify the new state's transform values were applied
        verify(canvas).translate(100f, 200f)
        verify(canvas).rotate(45f)
        verify(canvas).scale(2f, 2f)
    }

    @Test
    fun setActiveFilter_loadsFromAssetLoader() = runTest {
        // directExecutor makes preloadScope.launch run synchronously
        renderer.setActiveFilter(filterDef)
        verify(assetLoader).preload("sprites/sprite_spider")
    }

    @Test
    fun onDraw_appliesTranslateScaleRotateInOrder() = runTest {
        renderer.setActiveFilter(filterDef)
        renderer.setStickerState(StickerState(
            offset = Offset(50f, 75f),
            scale = 1.5f,
            rotation = 30f,
        ))
        whenever(assetLoader.get(any(), any())).thenReturn(bitmap)

        renderer.onDraw(canvas, frame)

        val inOrderVerifier: InOrder = inOrder(canvas)
        inOrderVerifier.verify(canvas).save()
        inOrderVerifier.verify(canvas).translate(50f, 75f)
        inOrderVerifier.verify(canvas).rotate(30f)
        inOrderVerifier.verify(canvas).scale(1.5f, 1.5f)
        inOrderVerifier.verify(canvas).drawBitmap(
            eq(bitmap),
            eq(-bitmap.width / 2f),
            eq(-bitmap.height / 2f),
            isNull(),
        )
        inOrderVerifier.verify(canvas).restore()
    }

    @Test
    fun onDraw_emptyFrames_returnsEarly() = runTest {
        renderer.setActiveFilter(filterDef)
        // assetLoader.get returns null — simulates preload not yet complete
        whenever(assetLoader.get(any(), any())).thenReturn(null)

        renderer.onDraw(canvas, frame)

        verify(canvas, never()).save()
    }

    @Test
    fun onDraw_drawsBitmapCenteredAtOrigin() = runTest {
        renderer.setActiveFilter(filterDef)
        renderer.setStickerState(StickerState())
        whenever(assetLoader.get(any(), any())).thenReturn(bitmap)

        renderer.onDraw(canvas, frame)

        // Bitmap drawn at (-width/2, -height/2) — centred on translated origin
        val expectedLeft = -bitmap.width / 2f   // -5f for 10px bitmap
        val expectedTop  = -bitmap.height / 2f  // -5f for 10px bitmap
        verify(canvas).drawBitmap(
            eq(bitmap),
            eq(expectedLeft),
            eq(expectedTop),
            isNull(),
        )
    }
}
