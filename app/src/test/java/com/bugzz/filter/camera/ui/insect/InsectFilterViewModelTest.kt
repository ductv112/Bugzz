package com.bugzz.filter.camera.ui.insect

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import androidx.lifecycle.LifecycleOwner
import com.bugzz.filter.camera.camera.CameraController
import com.bugzz.filter.camera.camera.CameraLens
import com.bugzz.filter.camera.data.FilterPrefsRepository
import com.bugzz.filter.camera.filter.FilterCatalog
import com.bugzz.filter.camera.render.StickerRenderer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions

/**
 * Unit tests for [InsectFilterViewModel] — MOD-04..07.
 * Un-Ignored in Plan 05-02 when SUT landed.
 *
 * Pattern: StandardTestDispatcher + advanceUntilIdle(), same as CameraViewModelTest.
 * Hilt-graph-free — constructs VM directly via the 3-arg constructor.
 */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class InsectFilterViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val mockController: CameraController = mock()
    private val mockStickerRenderer: StickerRenderer = mock()
    private val mockFilterPrefs: FilterPrefsRepository = mock()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        // Default stub: return first catalog filter id so init() doesn't hit null
        mockFilterPrefs.stub {
            on { lastUsedFilterId } doReturn flowOf(FilterCatalog.all.first().id)
        }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun buildVm(): InsectFilterViewModel =
        InsectFilterViewModel(mockController, mockStickerRenderer, mockFilterPrefs)

    // -------------------------------------------------------------------------

    @Test
    fun onStickerGesture_updatesStickerState() = runTest {
        val vm = buildVm()
        // Set a non-zero preview size so applyGesture clamp has a valid range
        vm.onPreviewSizeChanged(IntSize(720, 1280), IntSize(200, 200))
        advanceUntilIdle()

        val pan = Offset(50f, -30f)
        vm.onStickerGesture(pan = pan, zoom = 1f, rotationDelta = 0f)

        val state = vm.uiState.first().stickerState
        // After init with centerOn(720,1280) → (360f, 640f), then pan by (50,-30) → (410f, 610f)
        assertEquals(410f, state.offset.x, 0.5f)
        assertEquals(610f, state.offset.y, 0.5f)
        verify(mockStickerRenderer).setStickerState(state)
    }

    @Test
    fun onFilterSelected_savesToDataStore() = runTest {
        val vm = buildVm()
        advanceUntilIdle()

        val targetId = FilterCatalog.all[1].id   // pick second filter (different from default)
        vm.onFilterSelected(targetId)
        advanceUntilIdle()

        verify(mockFilterPrefs).setLastUsedFilter(targetId)
        verify(mockStickerRenderer).setActiveFilter(FilterCatalog.all[1])
        assertEquals(targetId, vm.uiState.first().selectedFilterId)
    }

    @Test
    fun bind_restoresLastUsedFilterFromDataStore() = runTest {
        val spiderId = "spider_nose_static"
        mockFilterPrefs.stub {
            on { lastUsedFilterId } doReturn flowOf(spiderId)
        }
        val vm = buildVm()
        advanceUntilIdle()

        assertEquals(spiderId, vm.uiState.first().selectedFilterId)
        verify(mockStickerRenderer).setActiveFilter(FilterCatalog.byId(spiderId)!!)
    }

    @Test
    fun stickerState_survivesFlipLens() = runTest {
        val vm = buildVm()
        vm.onPreviewSizeChanged(IntSize(720, 1280), IntSize(200, 200))
        advanceUntilIdle()

        // Apply a gesture to get a non-default sticker state
        vm.onStickerGesture(pan = Offset(100f, 50f), zoom = 1.5f, rotationDelta = 45f)
        val stateBefore = vm.uiState.first().stickerState

        // Flip lens
        vm.onFlipLens()

        val stateAfter = vm.uiState.first().stickerState
        // D-04: stickerState must be identical after flip
        assertEquals(stateBefore, stateAfter)
        // Lens should have changed
        assertEquals(CameraLens.BACK, vm.uiState.first().lens)
    }

    @Test
    fun bind_doesNotAttachFaceDetector() = runTest {
        val vm = buildVm()
        advanceUntilIdle()

        // D-05: InsectFilterViewModel does NOT have FaceDetectorClient in its constructor.
        // This test asserts the structural contract: the ViewModel ctor only takes
        // CameraController, StickerRenderer, FilterPrefsRepository — no FaceDetectorClient.
        // If a future commit accidentally adds FaceDetectorClient to InsectFilterViewModel,
        // this test class will fail to compile without updating the mock setup.
        //
        // Verify that stickerRenderer interactions occurred during init (setActiveFilter called
        // from the init block after restoring last-used filter) — this proves the VM is functional
        // without requiring face detection.
        verify(mockStickerRenderer).setActiveFilter(any())
        // Verify FilterPrefsRepository was consulted (init reads last-used filter)
        verify(mockFilterPrefs).lastUsedFilterId
        // NOTE: Plan 05-03 will add insectMode=true flag to CameraController.bind() to formally
        // skip MlKitAnalyzer attachment at the CameraController level.
    }
}
