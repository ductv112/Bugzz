package com.bugzz.filter.camera.ui.camera

import android.content.Context
import android.view.OrientationEventListener
import android.view.Surface
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bugzz.filter.camera.camera.CameraController
import com.bugzz.filter.camera.camera.CameraLensProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Seam between Compose UI and the @Singleton CameraController (Plan 02-04).
 *
 * Contracts (D-13 / D-14):
 *   - Exposes `uiState: StateFlow<CameraUiState>` — collected via collectAsStateWithLifecycle.
 *   - Exposes `surfaceRequest: StateFlow<SurfaceRequest?>` — reshares CameraController's flow; the
 *     composable passes the non-null value into CameraXViewfinder.
 *   - Exposes `events: Flow<OneShotEvent>` — Channel(BUFFERED).receiveAsFlow() for one-shot
 *     toasts (D-04 TEST RECORD).
 *
 * Lifecycle:
 *   - `bind(owner)` called from CameraScreen LaunchedEffect once permission is granted. On lens
 *     change, the same LaunchedEffect re-fires (keyed on uiState.lens), triggering rebind via
 *     CameraController.bind — which in turn calls flipLens semantics indirectly via unbindAll.
 *   - Orientation listener is owned by the composable's DisposableEffect (created here via
 *     `orientationListener(context)`; enable/disable lives on the composable side).
 *   - `onCleared()` stops any active test recording (safety net — normally the 5s delay has
 *     already fired).
 */
@HiltViewModel
class CameraViewModel @Inject constructor(
    private val controller: CameraController,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CameraUiState())
    val uiState = _uiState.asStateFlow()

    /** Reshared from CameraController (D-13 — flow, not a new StateFlow). */
    val surfaceRequest = controller.surfaceRequest

    private val _events = Channel<OneShotEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private var currentRotation: Int = Surface.ROTATION_0

    /** Invoked from CameraScreen LaunchedEffect when permission granted or lens changes. */
    fun bind(owner: LifecycleOwner) {
        viewModelScope.launch {
            runCatching { controller.bind(owner, _uiState.value.lens, currentRotation) }
                .onFailure {
                    _events.send(OneShotEvent.CameraError(it.message ?: "bind failed"))
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
     * D-08 — OrientationEventListener with quadrant thresholds. Composable calls enable()/disable()
     * inside a DisposableEffect. Emits only when the rotation quadrant changes (skips the ~45°
     * edge-of-quadrant flips that would thrash targetRotation).
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
