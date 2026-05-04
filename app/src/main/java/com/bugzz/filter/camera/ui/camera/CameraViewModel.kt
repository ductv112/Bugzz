package com.bugzz.filter.camera.ui.camera

import android.content.Context
import android.view.OrientationEventListener
import android.view.Surface
import androidx.camera.video.VideoRecordEvent
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bugzz.filter.camera.camera.CameraController
import com.bugzz.filter.camera.camera.CameraLensProvider
import com.bugzz.filter.camera.data.FilterPrefsRepository
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.concurrent.Executor
import javax.inject.Inject
import javax.inject.Named

/**
 * Seam between Compose UI and the @Singleton CameraController (Plan 02-04).
 *
 * Phase 3 additions (Plan 03-04):
 *   - Constructor adds FilterEngine, AssetLoader, @Named("cameraExecutor") Executor.
 *   - bind(owner) extended: after controller.bind succeeds, preloads + activates the first
 *     catalog filter on cameraExecutor dispatcher (no main-thread I/O).
 *   - onShutterTapped(): delegates to controller.capturePhoto; on success emits
 *     OneShotEvent.PhotoSaved; on failure emits OneShotEvent.PhotoError. Also toggles
 *     captureFlashVisible true → false for the 150ms capture-flash overlay (D-16).
 *   - onCycleFilter(): advances activeFilterId through FilterCatalog.all (mod size); preloads
 *     + calls filterEngine.setFilter. Debug-only trigger from CameraScreen (D-10).
 *
 * Phase 4 additions (Plan 04-05):
 *   - Constructor gains FilterPrefsRepository (5th arg).
 *   - bind() extended: populates uiState.filters from FilterCatalog.all on first call;
 *     reads lastUsedFilterId from DataStore to restore previous selection (CAT-05 / D-25).
 *   - onSelectFilter(id): CAT-04 picker tap handler — optimistic UI update + async preload
 *     + DataStore write (no debounce per D-27 implementation note).
 *   - onCycleFilter() marked @Deprecated (superseded by picker / onSelectFilter).
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
    private val filterPrefsRepository: FilterPrefsRepository,  // Phase 4 — CAT-05 / D-25
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
     *
     * Phase 3: after bind succeeds, preloads + activates the first filter on first call.
     * Phase 4: on first invocation, populates uiState.filters from FilterCatalog.all and
     *   restores the last-used filter from DataStore (CAT-05). Falls back to catalog.first()
     *   if stored id is unknown (stale catalog — T-04-01 post-catalog-update scenario).
     *
     * WR-03: cancels any in-flight bind before starting a new one to avoid concurrent
     *   bindToLifecycle races.
     */
    fun bind(owner: LifecycleOwner) {
        bindJob?.cancel()   // WR-03 preserved
        bindJob = viewModelScope.launch {
            runCatching { controller.bind(owner, _uiState.value.lens, currentRotation) }
                .onFailure {
                    _events.send(OneShotEvent.CameraError(it.message ?: "bind failed"))
                    return@launch
                }

            // Phase 4 — populate filter list on first bind (idempotent via isEmpty guard).
            if (_uiState.value.filters.isEmpty()) {
                val summaries = FilterCatalog.all.map { FilterSummary(it.id, it.displayName, it.assetDir) }
                _uiState.value = _uiState.value.copy(filters = summaries)
            }

            // Phase 3/4 — activate initial filter on first bind (idempotent via activeFilterId guard).
            if (_uiState.value.activeFilterId == null) {
                // Phase 4: read last-used id from DataStore; fall back to catalog first if unknown.
                val storedId = try {
                    filterPrefsRepository.lastUsedFilterId.first()
                } catch (e: Exception) {
                    FilterPrefsRepository.DEFAULT_FILTER_ID
                }
                val resolved = FilterCatalog.byId(storedId) ?: FilterCatalog.all.firstOrNull()
                if (resolved == null) {
                    _events.send(OneShotEvent.FilterLoadError("No filters in catalog"))
                    return@launch
                }
                try {
                    withContext(cameraExecutor.asCoroutineDispatcher()) {
                        assetLoader.preload(resolved.assetDir)   // 04-gaps-01: assetDir not id
                    }
                    filterEngine.setFilter(resolved)
                    _uiState.value = _uiState.value.copy(
                        activeFilterId = resolved.id,
                        selectedFilterId = resolved.id,
                    )
                } catch (e: Exception) {
                    _events.send(OneShotEvent.FilterLoadError("Filter load failed: ${e.message ?: "unknown"}"))
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

    // -------------------------------------------------------------------------
    // Phase 5: Production recording lifecycle (D-21/D-22/D-26/T-05-03/T-05-07)
    // -------------------------------------------------------------------------

    /** Set to true by onDiscardRecording() so the Finalize handler deletes the pending URI. */
    @Volatile private var pendingDiscardFlag: Boolean = false

    /**
     * Starts or stops recording depending on current [RecordingState] (D-22 toggle behavior).
     *
     * D-26 isRecording guard: early-return when recordingState != Idle (analogous to
     * isCapturing guard in onShutterTapped — prevents concurrent recording on double-tap).
     * T-05-03: backed by check(activeRecording == null) inside VideoRecorder.startRecording.
     *
     * @param audioEnabled true if RECORD_AUDIO permission has been granted (D-12).
     */
    fun onRecordTapped(audioEnabled: Boolean) {
        if (_uiState.value.recordingState !is RecordingState.Idle) return  // D-26 isRecording guard
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(
                    recordingState = RecordingState.Active(0L, audioEnabled)
                )
                controller.startRecording(audioEnabled, ::handleVideoEvent)
            } catch (t: Throwable) {
                Timber.tag("CameraVM").e(t, "startRecording failed")
                _uiState.value = _uiState.value.copy(recordingState = RecordingState.Idle)
                _events.send(OneShotEvent.VideoError("Failed to start: ${t.message}"))
            }
        }
    }

    /**
     * Stops an active recording. Recorder fires VideoRecordEvent.Finalize asynchronously,
     * which transitions state to Idle and emits VideoSaved or VideoError.
     */
    fun onStopRecording() {
        if (_uiState.value.recordingState !is RecordingState.Active) return
        _uiState.value = _uiState.value.copy(recordingState = RecordingState.Stopping)
        controller.stopRecording()
    }

    /**
     * D-10 / T-05-07: discard an in-progress recording. Sets pendingDiscardFlag so the
     * Finalize handler deletes the pending MediaStore URI instead of emitting VideoSaved.
     */
    fun onDiscardRecording() {
        pendingDiscardFlag = true
        if (_uiState.value.recordingState is RecordingState.Active) {
            _uiState.value = _uiState.value.copy(recordingState = RecordingState.Stopping)
            controller.stopRecording()
        }
    }

    /**
     * Dispatched on cameraExecutor from VideoRecorder.startRecording() callback.
     * Marshals state transitions + one-shot events back to main via viewModelScope.launch.
     */
    private fun handleVideoEvent(event: VideoRecordEvent) {
        viewModelScope.launch {
            when (event) {
                is VideoRecordEvent.Start -> {
                    // Confirm Active state (already set optimistically in onRecordTapped).
                    val current = _uiState.value.recordingState
                    if (current !is RecordingState.Active) {
                        _uiState.value = _uiState.value.copy(
                            recordingState = RecordingState.Active(0L, hasAudio = false)
                        )
                    }
                }
                is VideoRecordEvent.Status -> {
                    val active = _uiState.value.recordingState as? RecordingState.Active
                        ?: return@launch
                    val elapsedMs = event.recordingStats.recordedDurationNanos / 1_000_000L
                    _uiState.value = _uiState.value.copy(
                        recordingState = active.copy(elapsedMs = elapsedMs)
                    )
                }
                is VideoRecordEvent.Finalize -> {
                    controller.clearActiveRecording()
                    val outputUri = event.outputResults.outputUri

                    if (pendingDiscardFlag) {
                        // T-05-07: user discarded — delete pending MediaStore entry.
                        pendingDiscardFlag = false
                        try {
                            controller.contentResolver.delete(outputUri, null, null)
                            Timber.tag("CameraVM").i("discarded pending uri=%s", outputUri)
                        } catch (t: Throwable) {
                            Timber.tag("CameraVM").e(t, "delete pending failed uri=%s", outputUri)
                        }
                        _uiState.value = _uiState.value.copy(recordingState = RecordingState.Idle)
                        return@launch
                    }

                    // ERROR_DURATION_LIMIT_REACHED is not an error — treat same as manual stop (D-09).
                    // Rule 3 auto-fix: constant name is ERROR_DURATION_LIMIT_REACHED in CameraX 1.6
                    // (plan spec said ERROR_DURATION_REACHED which does not exist).
                    if (event.hasError() &&
                        event.error != VideoRecordEvent.Finalize.ERROR_DURATION_LIMIT_REACHED
                    ) {
                        val errMsg = "code=${event.error}: ${event.cause?.message ?: "unknown"}"
                        Timber.tag("CameraVM").e(event.cause, "Finalize error: %s", errMsg)
                        _uiState.value = _uiState.value.copy(recordingState = RecordingState.Idle)
                        _events.send(OneShotEvent.VideoError(errMsg))
                    } else {
                        _uiState.value = _uiState.value.copy(recordingState = RecordingState.Idle)
                        _events.send(OneShotEvent.VideoSaved(outputUri))
                    }
                }
                else -> Unit
            }
        }
    }

    /**
     * CAT-04 / CAT-05 — picker tap handler.
     *
     * Optimistic UI: uiState.selectedFilterId + activeFilterId flip immediately before preload
     * completes so the picker highlight responds on the same frame as the tap.
     *
     * Two async coroutines launched independently:
     *   (a) DataStore write — viewModelScope default dispatcher; DataStore handles its own IO.
     *   (b) Preload + setFilter — cameraExecutor dispatcher (render/asset thread).
     *
     * Guard: re-tap on the already-selected filter is a no-op (dedupe, T-04-05).
     * Unknown id: emits FilterLoadError event (no crash).
     */
    fun onSelectFilter(id: String) {
        if (id == _uiState.value.selectedFilterId) return  // dedupe re-tap on same filter (T-04-05)
        val def: FilterDefinition = FilterCatalog.byId(id) ?: run {
            viewModelScope.launch { _events.send(OneShotEvent.FilterLoadError("Unknown filter: $id")) }
            return
        }
        // Optimistic highlight — picker reflects selection immediately.
        _uiState.value = _uiState.value.copy(selectedFilterId = id, activeFilterId = id)

        // (a) DataStore write — async, non-blocking (D-27 no debounce).
        viewModelScope.launch {
            try {
                filterPrefsRepository.setLastUsedFilter(id)
            } catch (e: Exception) {
                Timber.tag("CameraVM").w(e, "DataStore write failed for filter: $id")
            }
        }

        // (b) Preload + set on cameraExecutor (render/asset thread).
        viewModelScope.launch(cameraExecutor.asCoroutineDispatcher()) {
            try {
                assetLoader.preload(def.assetDir)   // 04-gaps-01: assetDir not id
                filterEngine.setFilter(def)
            } catch (e: Exception) {
                _events.send(OneShotEvent.FilterLoadError("Filter load failed: ${e.message ?: "unknown"}"))
            }
        }
    }

    /**
     * D-10/D-11 — debug-only filter swap. Production catalog UX uses [onSelectFilter].
     * Advances activeFilterId index through FilterCatalog.all (mod size).
     * Preload runs on cameraExecutor dispatcher — never blocks main thread.
     *
     * @Deprecated — replaced by [onSelectFilter] picker tap in Phase 4 (04-UI-SPEC Impl Notes #3).
     *               Retained for Phase 3 test compatibility (CameraViewModelTest.onCycleFilter_*).
     */
    @Deprecated("Replaced by onSelectFilter in Phase 4; retained for Phase 3 test compat")
    fun onCycleFilter() {
        val all = FilterCatalog.all
        if (all.isEmpty()) return
        val currentId = _uiState.value.activeFilterId
        val currentIdx = all.indexOfFirst { it.id == currentId }.coerceAtLeast(-1)
        val next: FilterDefinition = all[(currentIdx + 1) % all.size]
        viewModelScope.launch {
            try {
                withContext(cameraExecutor.asCoroutineDispatcher()) {
                    assetLoader.preload(next.assetDir)   // 04-gaps-01: assetDir not id
                }
                filterEngine.setFilter(next)
                _uiState.value = _uiState.value.copy(activeFilterId = next.id)
            } catch (e: Exception) {
                _events.send(OneShotEvent.FilterLoadError("Filter load failed: ${e.message ?: "unknown"}"))
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
        // Also stop any active production recording to avoid orphan MediaStore entries.
        if (_uiState.value.recordingState is RecordingState.Active) {
            controller.stopRecording()
        }
    }
}
