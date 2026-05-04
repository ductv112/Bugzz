package com.bugzz.filter.camera.ui.insect

import org.junit.Assert
import org.junit.Ignore
import org.junit.Test

/**
 * RED scaffold for MOD-04..07. SUT (InsectFilterViewModel) lands in Plan 05-02.
 *
 * Pattern: Dispatchers.setMain(StandardTestDispatcher()), mock CameraController +
 * AssetLoader + FilterPrefsRepository + StickerRenderer; assert state mutations
 * via uiState.first() pattern after advanceUntilIdle().
 *
 * Expected API shape (Plan 05-02):
 *   @HiltViewModel
 *   class InsectFilterViewModel @Inject constructor(
 *       private val controller: CameraController,
 *       private val filterEngine: FilterEngine,   // receives faces=emptyList() in Insect mode (D-05)
 *       private val assetLoader: AssetLoader,
 *       private val stickerRenderer: StickerRenderer,
 *       private val filterPrefsRepo: FilterPrefsRepository,
 *       @Named("cameraExecutor") private val cameraExecutor: Executor,
 *   ) : ViewModel()
 *
 *   - uiState: StateFlow<InsectFilterUiState> with stickerState: StickerState + selectedFilterId: String
 *   - onStickerGesture(panX, panY, zoomChange, rotationDeg) — mutates stickerState via drag/pinch/rotate
 *   - onFilterSelected(filterId) — saves to DataStore via filterPrefsRepo.setLastUsedFilter
 *   - bind(owner) — restores lastUsedFilter from DataStore (CAT-05 pattern), does NOT attach MlKitAnalyzer (D-05)
 *
 * D-04: StickerState in VM StateFlow survives camera flip (flipLens does not reset VM state).
 * D-05: face detection DISABLED — bind() does NOT call faceDetector.createAnalyzer().
 */
class InsectFilterViewModelTest {

    @Ignore("Plan 05-02 lands InsectFilterViewModel SUT")
    @Test
    fun onStickerGesture_updatesStickerState() {
        Assert.fail("Plan 05-02 lands SUT")
    }

    @Ignore("Plan 05-02 lands InsectFilterViewModel SUT")
    @Test
    fun onFilterSelected_savesToDataStore() {
        Assert.fail("Plan 05-02 lands SUT")
    }

    @Ignore("Plan 05-02 lands InsectFilterViewModel SUT")
    @Test
    fun bind_restoresLastUsedFilterFromDataStore() {
        Assert.fail("Plan 05-02 lands SUT")
    }

    @Ignore("Plan 05-02 lands InsectFilterViewModel SUT")
    @Test
    fun stickerState_survivesFlipLens() {
        Assert.fail("Plan 05-02 lands SUT")
    }

    @Ignore("Plan 05-02 lands InsectFilterViewModel SUT")
    @Test
    fun bind_doesNotAttachFaceDetector() {
        Assert.fail("Plan 05-02 lands SUT")
    }
}
