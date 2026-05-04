package com.bugzz.filter.camera.ui.insect

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bugzz.filter.camera.camera.CameraController
import com.bugzz.filter.camera.camera.CameraLens
import com.bugzz.filter.camera.camera.CameraLensProvider
import com.bugzz.filter.camera.data.FilterPrefsRepository
import com.bugzz.filter.camera.filter.FilterCatalog
import com.bugzz.filter.camera.filter.FilterDefinition
import com.bugzz.filter.camera.render.StickerRenderer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * InsectFilter mode ViewModel.
 *
 * Sticker free-placement (D-03); face detection DISABLED (D-05) — bind() does NOT attach
 * FaceDetectorClient as MlKitAnalyzer. StickerState StateFlow survives camera flip and
 * orientation change (D-04).
 *
 * Restores last-used filter from FilterPrefsRepository on init (CAT-05 reuse, D-01).
 *
 * Lock-during-record (D-23 / T-05-04): onStickerGesture, onFilterSelected, onFlipLens all
 * early-return when [InsectFilterUiState.isRecording] is true.
 *
 * Phase 5.
 */
@HiltViewModel
class InsectFilterViewModel @Inject constructor(
    private val controller: CameraController,
    private val stickerRenderer: StickerRenderer,
    private val filterPrefs: FilterPrefsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(InsectFilterUiState())
    val uiState: StateFlow<InsectFilterUiState> = _uiState.asStateFlow()

    /** Reshared from CameraController (same pattern as CameraViewModel D-13). */
    val surfaceRequest = controller.surfaceRequest

    init {
        // CAT-05 reuse: restore last-used filter from DataStore on VM creation.
        viewModelScope.launch {
            val storedId = try {
                filterPrefs.lastUsedFilterId.first()
            } catch (e: Exception) {
                FilterPrefsRepository.DEFAULT_FILTER_ID
            }
            val def = FilterCatalog.byId(storedId) ?: FilterCatalog.all.firstOrNull()
            def?.let {
                _uiState.value = _uiState.value.copy(selectedFilterId = it.id)
                stickerRenderer.setActiveFilter(it)
                Timber.tag("InsectVM").i("init filter=%s", it.id)
            }
        }
    }

    /**
     * Bind camera without FaceDetector (D-05). Plan 05-03 will add a dedicated
     * bindForInsectMode() flag on CameraController to formally skip MlKitAnalyzer.
     * Wave 1 placeholder: uses standard bind() — face detection is present at camera level
     * but InsectFilterScreen never collects latestSnapshot from FaceDetectorClient.
     *
     * Note: Plan 05-03 strengthens this by adding insectMode=true flag to CameraController.bind().
     */
    fun bind(lifecycleOwner: LifecycleOwner) {
        viewModelScope.launch {
            runCatching {
                controller.bind(lifecycleOwner, _uiState.value.lens)
            }.onFailure { e ->
                Timber.tag("InsectVM").e(e, "bind failed")
            }
        }
    }

    /**
     * D-03: process detectTransformGestures delta.
     * D-23: no-op while recording.
     */
    fun onStickerGesture(pan: Offset, zoom: Float, rotationDelta: Float) {
        if (_uiState.value.isRecording) return
        val current = _uiState.value
        val newStickerState = current.stickerState.applyGesture(
            pan = pan,
            zoom = zoom,
            rotationDelta = rotationDelta,
            previewSize = current.previewSize,
            bitmapSize = current.bitmapSize,
        )
        _uiState.value = current.copy(stickerState = newStickerState)
        stickerRenderer.setStickerState(newStickerState)
    }

    /**
     * CAT-05 reuse: picker tap handler — updates selected filter, persists to DataStore,
     * notifies StickerRenderer.
     * D-23: no-op while recording.
     */
    fun onFilterSelected(id: String) {
        if (_uiState.value.isRecording) return
        val def: FilterDefinition = FilterCatalog.byId(id) ?: return
        _uiState.value = _uiState.value.copy(selectedFilterId = id)
        viewModelScope.launch {
            try {
                filterPrefs.setLastUsedFilter(id)
            } catch (e: Exception) {
                Timber.tag("InsectVM").w(e, "DataStore write failed for filter: %s", id)
            }
        }
        stickerRenderer.setActiveFilter(def)
    }

    /**
     * D-04: flip camera lens. StickerState preserved in _uiState (not reset).
     * D-23: no-op while recording.
     */
    fun onFlipLens() {
        if (_uiState.value.isRecording) return
        val newLens = CameraLensProvider.next(_uiState.value.lens)
        // D-04: copy preserves stickerState — only lens changes.
        _uiState.value = _uiState.value.copy(lens = newLens)
        // Plan 05-03 will wire actual flipLens() on CameraController with lifecycleOwner.
        // Wave 1 placeholder: lens state updated in VM; rebind happens when Plan 05-03 lands.
    }

    /**
     * D-02: called when CameraXViewfinder reports its size. Centers sticker on first measurement.
     */
    fun onPreviewSizeChanged(size: IntSize, bitmapSize: IntSize) {
        val current = _uiState.value
        val isFirstSize = current.previewSize == IntSize.Zero
        val newStickerState = if (isFirstSize) current.stickerState.centerOn(size) else current.stickerState
        _uiState.value = current.copy(
            previewSize = size,
            bitmapSize = bitmapSize,
            stickerState = newStickerState,
        )
        if (isFirstSize) stickerRenderer.setStickerState(newStickerState)
    }
}
