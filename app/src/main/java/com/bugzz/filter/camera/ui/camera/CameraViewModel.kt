package com.bugzz.filter.camera.ui.camera

import android.content.Context
import android.view.OrientationEventListener
import android.view.Surface
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bugzz.filter.camera.camera.CameraController
import com.bugzz.filter.camera.camera.CameraLensProvider
import com.bugzz.filter.camera.filter.AssetLoader
import com.bugzz.filter.camera.filter.FilterCatalog
import com.bugzz.filter.camera.filter.FilterDefinition
import com.bugzz.filter.camera.render.FilterEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.Executor
import javax.inject.Inject
import javax.inject.Named

/**
 * Seam between Compose UI and the @Singleton CameraController (Plan 02-04).
 *
 * Phase 3 additions (Plan 03-04):
 *   - Constructor adds FilterEngine, AssetLoader, @Named("cameraExecutor") Executor.
 *   - bind(owner) extended: after controller.bind succeeds, preloads + activates the first
 *     catalog filter (ant_on_nose_v1) on cameraExecutor dispatcher (no main-thread I/O).
 *   - onShutterTapped(): delegates to controller.capturePhoto; on success emits
 *     OneShotEvent.PhotoSaved; on failure emits OneShotEvent.PhotoError. Also toggles
 *     captureFlashVisible true → false for the 150ms capture-flash overlay (D-16).
 *   - onCycleFilter(): advances activeFilterId through FilterCatalog.all (mod size); preloads
 *     + calls filterEngine.setFilter. Debug-only trigger from CameraScreen (D-10).
 *
 * Contracts (D-13 / D-14):
 *   - Exposes `uiState: StateFlow<CameraUiState>` — collected via collectAsStateWithLifecycle.
 *   - Exposes `surfaceRequest: StateFlow<SurfaceRequest?>` — reshares CameraController's flow.
 *   - Exposes `events: Flow<OneShotEvent>` — Channel(BUFFERED).receiveAsFlow() for one-shot toasts.
 *
 * Lifecycle:
 *   - `bind(owner)` called from CameraScreen LaunchedEffect once permission is granted.
 *   - Orientation listener owned by composable's DisposableEffect (D-08).
 *   - `onCleared()` stops any active test recording (safety net).
 *
 * T-03-04: viewModelScope is tied to ViewModel lifecycle (auto-cancelled on onCleared).
 * No Activity references captured in capturePhoto lambda closure.
 */
@HiltViewModel
class CameraViewModel @Inject constructor(
    private val controller: CameraController,
    private val filterEngine: FilterEngine,
    private val assetLoader: AssetLoader,
    @Named("cameraExecutor") private val cameraExecutor: Executor,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CameraUiState())
    val uiState = _uiState.asStateFlow()

    /** Reshared from CameraController (D-13 — flow, not a new StateFlow). */
    val surfaceRequest = controller.surfaceRequest

    private val _events = Channel<OneShotEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private var currentRotation: Int = Surface.ROTATION_0

    /** Tracks the active bind coroutine so concurrent lens-flip recompositions cancel the prior one (WR-03). */
    private var bindJob: Job? = null

    /**
     * Invoked from CameraScreen LaunchedEffect when permission granted or lens changes.
     * Phase 3: after bind succeeds, preloads + activates the first filter on first call.
     * WR-03: cancels any in-flight bind before starting a new one to avoid concurrent bindToLifecycle races.
     */
    fun bind(owner: LifecycleOwner) {
        bindJob?.cancel()
        bindJob = viewModelScope.launch {
            runCatching { controller.bind(owner, _uiState.value.lens, currentRotation) }
                .onFailure {
                    _events.send(OneShotEvent.CameraError(it.message ?: "bind failed"))
                    return@launch
                }

            // Phase 3 D-01 — activate the first production filter on first bind.
            if (_uiState.value.activeFilterId == null) {
                val initial = FilterCatalog.all.firstOrNull() ?: return@launch
                try {
                    withContext(cameraExecutor.asCoroutineDispatcher()) {
                        assetLoader.preload(initial.id)
                    }
                    filterEngine.setFilter(initial)
                    _uiState.value = _uiState.value.copy(activeFilterId = initial.id)
                } catch (e: Exception) {
                    _events.send(OneShotEvent.PhotoError("Filter load failed: ${e.message ?: "unknown"}"))
                }
            }
        }
    }

    /** D-24 — top-right flip button tap handler. */
    fun onFlipLens() {
        val next = CameraLensProvider.next(_uiState.value.lens)
        _uiState.value = _uiState.value.copy(lens = next)
    }

    /** D-04 / D-05 — debug TEST RECORD 5s auto-stop; no audio. */
    fun onTestRecord() {
        if (_uiState.value.isRecording) return
        _uiState.value = _uiState.value.copy(isRecording = true)

        controller.startTestRecording { result ->
            viewModelScope.launch {
                _uiState.value = _uiState.value.copy(isRecording = false)
                result.fold(
                    onSuccess = { _events.send(OneShotEvent.TestRecordSaved(it)) },
                    onFailure = { _events.send(OneShotEvent.TestRecordFailed(it.message ?: "record error")) },
                )
            }
        }

        viewModelScope.launch {
            delay(5_000L)
            controller.stopTestRecording()
        }
    }

    fun onPermissionResult(granted: Boolean) {
        _uiState.value = _uiState.value.copy(
            permissionState = if (granted) PermissionState.Granted else PermissionState.Denied,
        )
    }

    /**
     * D-13/D-15/D-35 — production shutter.
     * Haptic fires in Compose layer (RESEARCH §Example 8 — LocalView.current.performHapticFeedback).
     * captureFlashVisible toggled true only on success, then false after callback (D-16).
     * WR-04: flash is deferred to the success branch so a "camera not bound" synchronous failure
     * never shows a white flash before the error toast.
     *
     * T-03-04: onResult lambda is a one-shot callback; viewModelScope.launch captures no Activity ref.
     */
    fun onShutterTapped() {
        if (_uiState.value.isCapturing) return  // re-entrance guard (WR-02)
        _uiState.value = _uiState.value.copy(isCapturing = true)
        controller.capturePhoto { result ->
            viewModelScope.launch {
                result.fold(
                    onSuccess = { uri ->
                        // Show capture flash only when photo actually saved (WR-04).
                        _uiState.value = _uiState.value.copy(captureFlashVisible = true)
                        _uiState.value = _uiState.value.copy(isCapturing = false, captureFlashVisible = false)
                        _events.send(OneShotEvent.PhotoSaved(uri))
                    },
                    onFailure = { exc ->
                        _uiState.value = _uiState.value.copy(isCapturing = false)
                        _events.send(OneShotEvent.PhotoError(exc.message ?: "capture failed"))
                    },
                )
            }
        }
    }

    /**
     * D-10/D-11 — debug-only filter swap. Production catalog UX lands in Phase 4.
     * Advances activeFilterId index through FilterCatalog.all (mod size).
     * Preload runs on cameraExecutor dispatcher — never blocks main thread.
     */
    fun onCycleFilter() {
        val all = FilterCatalog.all
        if (all.isEmpty()) return
        val currentId = _uiState.value.activeFilterId
        val currentIdx = all.indexOfFirst { it.id == currentId }.coerceAtLeast(-1)
        val next: FilterDefinition = all[(currentIdx + 1) % all.size]
        viewModelScope.launch {
            try {
                withContext(cameraExecutor.asCoroutineDispatcher()) {
                    assetLoader.preload(next.id)
                }
                filterEngine.setFilter(next)
                _uiState.value = _uiState.value.copy(activeFilterId = next.id)
            } catch (e: Exception) {
                _events.send(OneShotEvent.PhotoError("Filter load failed: ${e.message ?: "unknown"}"))
            }
        }
    }

    /**
     * D-08 — OrientationEventListener with quadrant thresholds. Composable calls enable()/disable()
     * inside a DisposableEffect. Emits only when the rotation quadrant changes.
     */
    fun orientationListener(context: Context): OrientationEventListener =
        object : OrientationEventListener(context) {
            override fun onOrientationChanged(degrees: Int) {
                if (degrees == ORIENTATION_UNKNOWN) return
                val rot = when (degrees) {
                    in 45..134  -> Surface.ROTATION_270
                    in 135..224 -> Surface.ROTATION_180
                    in 225..314 -> Surface.ROTATION_90
                    else        -> Surface.ROTATION_0
                }
                if (rot != currentRotation) {
                    currentRotation = rot
                    controller.setTargetRotation(rot)
                }
            }
        }

    override fun onCleared() {
        super.onCleared()
        controller.stopTestRecording()
    }
}
