package com.bugzz.filter.camera.ui.camera

import android.net.Uri
import androidx.camera.core.ImageCaptureException
import com.bugzz.filter.camera.camera.CameraController
import com.bugzz.filter.camera.filter.AssetLoader
import com.bugzz.filter.camera.filter.FilterCatalog
import com.bugzz.filter.camera.render.FilterEngine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.mockito.kotlin.mock

/**
 * Nyquist Wave 0 tests for [CameraViewModel] Phase 3 additions (CAP-01/03 / REN-07).
 *
 * Tests cover:
 * 1. onShutterTapped success → emits OneShotEvent.PhotoSaved with saved URI
 * 2. onShutterTapped failure → emits OneShotEvent.PhotoError with error message
 * 3. onCycleFilter → calls filterEngine.setFilter with next catalog entry; uiState updated
 *
 * All 3 tests are @Ignore'd because:
 * - CameraViewModel does not yet have onShutterTapped() / onCycleFilter() methods
 * - CameraViewModel constructor does not yet accept FilterEngine / AssetLoader / Executor params
 * - OneShotEvent does not yet have PhotoSaved / PhotoError variants
 * - Plan 03-04 Task 2 adds these and un-Ignores this test class
 *
 * Test pattern: pure JVM via kotlinx-coroutines-test runTest + UnconfinedTestDispatcher.
 * Mock dependencies via mockito-kotlin. Synchronous direct Executor (no dispatcher plumbing).
 * Hilt-graph-free — constructs VM directly via the Plan 03-04 constructor shape.
 */
class CameraViewModelTest {

    // These mocks are declared here for reference — they compile against existing types.
    // Plan 03-04 wires them into the VM constructor.
    private val mockController: CameraController = mock()
    private val mockFilterEngine: FilterEngine = mock()
    private val mockAssetLoader: AssetLoader = mock()
    private val syncExecutor = java.util.concurrent.Executor { it.run() }

    /**
     * onShutterTapped success path: CameraController.capturePhoto invokes callback with
     * Result.success(uri) → ViewModel emits OneShotEvent.PhotoSaved(uri).
     *
     * @Ignore'd because:
     * - CameraViewModel.onShutterTapped() does not exist yet
     * - CameraViewModel constructor does not yet accept FilterEngine/AssetLoader/Executor
     * - OneShotEvent.PhotoSaved variant does not exist yet
     * Plan 03-04 Task 2 un-Ignores when all three land.
     */
    @org.junit.Ignore("Plan 03-04 — un-Ignore when ViewModel adds onShutterTapped / OneShotEvent.PhotoSaved")
    @Test
    fun onShutterTapped_capturePhotoSucceeds_emitsPhotoSaved() = runTest(UnconfinedTestDispatcher()) {
        // TODO Plan 03-04: construct VM via 4-arg constructor once it exists:
        // val vm = CameraViewModel(mockController, mockFilterEngine, mockAssetLoader, syncExecutor)
        //
        // Stub controller.capturePhoto to invoke callback synchronously with success URI:
        // whenever(mockController.capturePhoto(any())).then { invocation ->
        //     val callback = invocation.getArgument<(Result<Uri>) -> Unit>(0)
        //     callback(Result.success(Uri.parse("content://test/1")))
        // }
        //
        // vm.onShutterTapped()
        //
        // val event = vm.events.first()
        // assertTrue(event is OneShotEvent.PhotoSaved)
        // assertEquals(Uri.parse("content://test/1"), (event as OneShotEvent.PhotoSaved).uri)
        //
        // Also verify capture flash: uiState.captureFlashVisible went true then false
        // advanceUntilIdle()
        // assertFalse(vm.uiState.value.captureFlashVisible)
    }

    /**
     * onShutterTapped failure path: CameraController.capturePhoto invokes callback with
     * Result.failure(ImageCaptureException) → ViewModel emits OneShotEvent.PhotoError.
     *
     * @Ignore'd because:
     * - CameraViewModel.onShutterTapped() does not exist yet
     * - OneShotEvent.PhotoError variant does not exist yet
     * Plan 03-04 Task 2 un-Ignores when both land.
     */
    @org.junit.Ignore("Plan 03-04 — un-Ignore when ViewModel adds onShutterTapped / OneShotEvent.PhotoError")
    @Test
    fun onShutterTapped_capturePhotoFails_emitsPhotoError() = runTest(UnconfinedTestDispatcher()) {
        // TODO Plan 03-04: construct VM via 4-arg constructor:
        // val vm = CameraViewModel(mockController, mockFilterEngine, mockAssetLoader, syncExecutor)
        //
        // Stub controller.capturePhoto to invoke callback synchronously with failure:
        // whenever(mockController.capturePhoto(any())).then { invocation ->
        //     val callback = invocation.getArgument<(Result<Uri>) -> Unit>(0)
        //     callback(Result.failure(
        //         ImageCaptureException(ImageCapture.ERROR_UNKNOWN, "disk full", null)
        //     ))
        // }
        //
        // vm.onShutterTapped()
        //
        // val event = vm.events.first()
        // assertTrue(event is OneShotEvent.PhotoError)
        // assertTrue((event as OneShotEvent.PhotoError).message.contains("disk full"))
    }

    /**
     * onCycleFilter: ViewModel calls filterEngine.setFilter with next catalog entry and
     * updates uiState.activeFilterId.
     *
     * @Ignore'd because:
     * - CameraViewModel.onCycleFilter() does not exist yet
     * - CameraViewModel constructor does not yet accept FilterEngine/AssetLoader
     * - CameraUiState does not yet have activeFilterId field
     * - FilterCatalog stub has empty list (Plan 03-03 populates it)
     * Plan 03-04 Task 2 un-Ignores when all land.
     */
    @org.junit.Ignore("Plan 03-04 — un-Ignore when ViewModel adds onShutterTapped / onCycleFilter / FilterEngine+AssetLoader @Inject params")
    @Test
    fun onCycleFilter_togglesFilterEngineActive() = runTest(UnconfinedTestDispatcher()) {
        // TODO Plan 03-04: construct VM via 4-arg constructor:
        // val vm = CameraViewModel(mockController, mockFilterEngine, mockAssetLoader, syncExecutor)
        //
        // Seed with first filter active:
        // _uiState.value = _uiState.value.copy(activeFilterId = FilterCatalog.all[0].id)
        //  OR drive via onCycleFilter starting from null state
        //
        // whenever(mockAssetLoader.preload(any())).thenReturn(Unit)
        //
        // vm.onCycleFilter()
        //
        // advanceUntilIdle()
        // verify(mockFilterEngine).setFilter(FilterCatalog.all[1])   // cycles to second filter
        // assertEquals(FilterCatalog.all[1].id, vm.uiState.value.activeFilterId)
        //
        // vm.onCycleFilter()  // cycles back to first
        // advanceUntilIdle()
        // verify(mockFilterEngine).setFilter(FilterCatalog.all[0])
        // assertEquals(FilterCatalog.all[0].id, vm.uiState.value.activeFilterId)
    }
}
