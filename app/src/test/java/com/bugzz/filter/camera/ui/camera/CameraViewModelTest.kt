package com.bugzz.filter.camera.ui.camera

import android.net.Uri
import androidx.lifecycle.LifecycleOwner
import com.bugzz.filter.camera.camera.CameraController
import com.bugzz.filter.camera.data.FilterPrefsRepository
import com.bugzz.filter.camera.filter.AssetLoader
import com.bugzz.filter.camera.filter.FilterCatalog
import com.bugzz.filter.camera.render.FilterEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.clearInvocations
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify

/**
 * Unit tests for [CameraViewModel] covering Phase 3 + Phase 4 additions.
 *
 * Phase 3 tests (Plans 03-04 + 03-REVIEW-FIX):
 *   1. onShutterTapped success → emits OneShotEvent.PhotoSaved with saved URI
 *   2. onShutterTapped failure → emits OneShotEvent.PhotoError with error message
 *   3. onCycleFilter → calls filterEngine.setFilter with next catalog entry; uiState updated
 *
 * Phase 4 tests (Plan 04-05 Task 3 — un-Ignored):
 *   4. onSelectFilter → calls engine.setFilter + prefs.setLastUsedFilter + updates selectedFilterId
 *   5. initialBind reads lastUsedFilterId from DataStore → applies it to engine + uiState
 *   6. rapidSelectFilter → NO camera rebind (T-04-05 mitigation)
 *
 * Test pattern: pure JVM via kotlinx-coroutines-test runTest + StandardTestDispatcher.
 * Dispatchers.Main is replaced with StandardTestDispatcher so viewModelScope.launch
 * advances deterministically with advanceUntilIdle().
 * Mock dependencies via mockito-kotlin. Synchronous direct Executor (no dispatcher plumbing).
 * Hilt-graph-free — constructs VM directly via the 5-arg constructor shape.
 */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class CameraViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val mockController: CameraController = mock()
    private val mockFilterEngine: FilterEngine = mock()
    private val mockAssetLoader: AssetLoader = mock()
    private val mockPrefsRepo: FilterPrefsRepository = mock()
    // Synchronous executor — runs submitted Runnable immediately on calling thread.
    private val syncExecutor = java.util.concurrent.Executor { it.run() }

    @Before
    fun setUp() {
        // Replace Dispatchers.Main with testDispatcher so viewModelScope coroutines
        // advance under runTest / advanceUntilIdle() control.
        Dispatchers.setMain(testDispatcher)
        // Default stub so Phase 3 bind() tests don't get a null Flow from mockPrefsRepo.
        mockPrefsRepo.stub {
            on { lastUsedFilterId } doReturn flowOf(FilterPrefsRepository.DEFAULT_FILTER_ID)
        }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun buildVm(): CameraViewModel =
        CameraViewModel(mockController, mockFilterEngine, mockAssetLoader, syncExecutor, mockPrefsRepo)

    // =========================================================================
    // Phase 3 tests (03-04 + 03-REVIEW-FIX) — all preserved verbatim, ctor updated to 5-arg
    // =========================================================================

    /**
     * onShutterTapped success path: CameraController.capturePhoto invokes callback with
     * Result.success(uri) → ViewModel emits OneShotEvent.PhotoSaved(uri).
     */
    @Test
    fun onShutterTapped_capturePhotoSucceeds_emitsPhotoSaved() = runTest(testDispatcher) {
        // Use mock<Uri>() — Uri.parse() returns null under plain JVM (no Android runtime).
        val testUri: Uri = mock()

        // Stub controller.capturePhoto to invoke callback synchronously with success URI.
        // The callback is invoked on cameraExecutor (syncExecutor here) — immediate.
        mockController.stub {
            on { capturePhoto(any()) } doAnswer { invocation ->
                val callback = invocation.getArgument<(Result<Uri>) -> Unit>(0)
                callback(Result.success(testUri))
                Unit
            }
        }

        val vm = buildVm()

        // Collect the first event concurrently — must start before onShutterTapped so the
        // deferred is ready to receive the item when the coroutine produces it.
        val eventDeferred = async { vm.events.first() }

        vm.onShutterTapped()

        // Advance coroutines so viewModelScope.launch inside the callback completes.
        advanceUntilIdle()

        val event = eventDeferred.await()
        assertTrue("event must be PhotoSaved", event is OneShotEvent.PhotoSaved)
        assertEquals(testUri, (event as OneShotEvent.PhotoSaved).uri)

        // After callback resolves, captureFlashVisible should be false.
        assertFalse(
            "captureFlashVisible must be false after callback",
            vm.uiState.value.captureFlashVisible
        )
    }

    /**
     * onShutterTapped failure path: CameraController.capturePhoto invokes callback with
     * Result.failure → ViewModel emits OneShotEvent.PhotoError.
     * T-03-01: storage-full / capture-error path handled without crash.
     */
    @Test
    fun onShutterTapped_capturePhotoFails_emitsPhotoError() = runTest(testDispatcher) {
        val errorMessage = "disk full"

        mockController.stub {
            on { capturePhoto(any()) } doAnswer { invocation ->
                val callback = invocation.getArgument<(Result<Uri>) -> Unit>(0)
                callback(Result.failure(RuntimeException(errorMessage)))
                Unit
            }
        }

        val vm = buildVm()
        val eventDeferred = async { vm.events.first() }

        vm.onShutterTapped()
        advanceUntilIdle()

        val event = eventDeferred.await()
        assertTrue("event must be PhotoError", event is OneShotEvent.PhotoError)
        assertTrue(
            "PhotoError message must contain error text",
            (event as OneShotEvent.PhotoError).message.contains(errorMessage)
        )
    }

    /**
     * onCycleFilter: ViewModel calls filterEngine.setFilter with next catalog entry and
     * updates uiState.activeFilterId.
     * REN-07: filter swap cycles through FilterCatalog.all (mod size).
     */
    @Test
    fun onCycleFilter_togglesFilterEngineActive() = runTest(testDispatcher) {
        // Stub assetLoader.preload to return Unit (suspend no-op).
        mockAssetLoader.stub {
            onBlocking { preload(any()) } doReturn Unit
        }

        val vm = buildVm()

        // Starting from null → first cycle: index = (-1 + 1) % 15 = 0 → all[0] (spider_nose_static)
        vm.onCycleFilter()
        advanceUntilIdle()

        verify(mockFilterEngine).setFilter(FilterCatalog.all[0])
        assertEquals(FilterCatalog.all[0].id, vm.uiState.value.activeFilterId)

        // Second cycle: (0 + 1) % 15 = 1 → all[1] (spider_forehead_static)
        vm.onCycleFilter()
        advanceUntilIdle()

        verify(mockFilterEngine).setFilter(FilterCatalog.all[1])
        assertEquals(FilterCatalog.all[1].id, vm.uiState.value.activeFilterId)

        // Third cycle: (1 + 1) % 15 = 2 → all[2] (spider_jawline_crawl)
        vm.onCycleFilter()
        advanceUntilIdle()

        // Verify strict ordering: all[0] → all[1] → all[2]
        org.mockito.kotlin.inOrder(mockFilterEngine).also { inOrder ->
            inOrder.verify(mockFilterEngine).setFilter(FilterCatalog.all[0])
            inOrder.verify(mockFilterEngine).setFilter(FilterCatalog.all[1])
            inOrder.verify(mockFilterEngine).setFilter(FilterCatalog.all[2])
        }
        assertEquals(FilterCatalog.all[2].id, vm.uiState.value.activeFilterId)
    }

    // =========================================================================
    // Phase 4 tests (Plan 04-05 Task 3) — un-Ignored from Plan 04-02 Wave 0 scaffold
    // =========================================================================

    /**
     * CAT-04: vm.onSelectFilter("bugC_fall") must:
     *   1. Call filterEngine.setFilter() with the FilterDefinition whose id == "bugC_fall"
     *   2. Call filterPrefsRepository.setLastUsedFilter("bugC_fall")
     *   3. Update uiState.selectedFilterId to "bugC_fall"
     *
     * T-04-05 mitigation: rapid-select does NOT trigger camera rebind.
     */
    @Test
    fun onSelectFilter_callsEngineAndWritesDataStore() = runTest(testDispatcher) {
        mockAssetLoader.stub {
            onBlocking { preload(any()) } doReturn Unit
        }

        val vm = buildVm()
        vm.onSelectFilter("bugC_fall")
        advanceUntilIdle()

        verify(mockFilterEngine).setFilter(argThat { id == "bugC_fall" })
        verify(mockPrefsRepo).setLastUsedFilter("bugC_fall")
        assertEquals("bugC_fall", vm.uiState.value.selectedFilterId)
    }

    /**
     * CAT-05: on initial bind(), CameraViewModel must read lastUsedFilterId from DataStore
     * and apply it — calling filterEngine.setFilter() + updating uiState.selectedFilterId.
     *
     * Scenario: DataStore returns "bugB_crawl" from lastUsedFilterId flow.
     * Expected: bind() reads that value, calls setFilter with bugB_crawl FilterDefinition,
     * and sets uiState.selectedFilterId = "bugB_crawl".
     */
    @Test
    fun initialBind_readsLastUsedFromDataStore() = runTest(testDispatcher) {
        mockPrefsRepo.stub {
            on { lastUsedFilterId } doReturn flowOf("bugB_crawl")
        }
        mockAssetLoader.stub {
            onBlocking { preload(any()) } doReturn Unit
        }

        val vm = buildVm()
        val mockOwner: LifecycleOwner = mock()
        vm.bind(mockOwner)
        advanceUntilIdle()

        verify(mockFilterEngine).setFilter(argThat { id == "bugB_crawl" })
        assertEquals("bugB_crawl", vm.uiState.value.selectedFilterId)
    }

    /**
     * T-04-05 mitigation + CAT-03: 10 rapid onSelectFilter calls in quick succession must NOT
     * trigger a camera rebind (controller.bind() called again after the initial bind).
     *
     * Invariant: filter selection only calls filterEngine.setFilter + DataStore write.
     * CameraController.bind() fires ONLY once during initial bind() — never on filter swap.
     * This prevents "Camera in use" errors during rapid picker taps.
     */
    @Test
    fun rapidSelectFilter_noCameraRebind() = runTest(testDispatcher) {
        mockAssetLoader.stub {
            onBlocking { preload(any()) } doReturn Unit
        }

        val vm = buildVm()
        val mockOwner: LifecycleOwner = mock()

        // Initial bind — fires controller.bind once
        vm.bind(mockOwner)
        advanceUntilIdle()

        // Reset bind call count tracking so rapid-taps start from a clean slate
        clearInvocations(mockController)

        // 10 rapid filter swaps (alternating two filters to avoid dedupe guard)
        repeat(10) { vm.onSelectFilter(if (it % 2 == 0) "bugA_swarm" else "bugB_crawl") }
        advanceUntilIdle()

        // controller.bind must NEVER fire again — only filter engine + DataStore updated
        verify(mockController, never()).bind(any(), any(), any())
    }
}
