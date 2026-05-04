package com.bugzz.filter.camera.thermal

import android.content.Context
import android.os.Build
import android.os.PowerManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import java.util.concurrent.Executor
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * Process-lifetime singleton wrapping PowerManager.OnThermalStatusChangedListener (API 29+).
 * On API 28 (minSdk), status remains None — callback is never registered (API guard).
 *
 * D-14: status >= Moderate → FaceDetectorClient skips every 2nd ML Kit invocation.
 * T-05-05: listener is process-lifetime; no Activity ref captured; no leak.
 *
 * Phase 5.
 */
@Singleton
class ThermalMonitor @Inject constructor(
    @ApplicationContext private val context: Context,
    @Named("cameraExecutor") private val executor: Executor,
) {
    private val _status = MutableStateFlow(ThermalStatus.None)
    val status: StateFlow<ThermalStatus> = _status.asStateFlow()

    private val listener = PowerManager.OnThermalStatusChangedListener { rawStatus ->
        _status.value = mapStatus(rawStatus)
        Timber.tag("ThermalMonitor").v("status=%s (raw=%d)", _status.value, rawStatus)
    }

    /**
     * Register the thermal status listener. API 29+ only; no-op on API 28.
     * Called from BugzzApplication.onCreate().
     *
     * Wrapped in try/catch: Robolectric's ShadowPowerManager does not implement the thermal
     * listener API and throws RuntimeException("Listener failed to set"). On real devices
     * this call succeeds silently. Logging the failure allows detection in CI without crashing.
     */
    fun register() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return
        val pm = context.getSystemService(PowerManager::class.java) ?: return
        try {
            pm.addThermalStatusListener(executor, listener)
            Timber.tag("ThermalMonitor").i("registered (API %d)", Build.VERSION.SDK_INT)
        } catch (e: RuntimeException) {
            // Robolectric ShadowPowerManager does not support thermal listener API — expected in
            // unit-test environments. Production devices do not throw here.
            Timber.tag("ThermalMonitor").w(e, "addThermalStatusListener failed (test environment?)")
        }
    }

    /**
     * Remove the thermal status listener. Called from BugzzApplication.onTerminate (test/emulator only)
     * or if explicitly deregistering. Safe to call before register().
     */
    fun unregister() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return
        val pm = context.getSystemService(PowerManager::class.java) ?: return
        pm.removeThermalStatusListener(listener)
    }

    /**
     * Frame-skip helper called by FaceDetectorClient.
     * Returns true if the current frame should be skipped to reduce ML Kit load.
     *
     * D-14: when status >= Moderate, skip every other frame (frameCounter % 2 != 0).
     */
    fun shouldSkipFrame(frameCounter: Int): Boolean {
        return status.value >= ThermalStatus.Moderate && frameCounter % 2 != 0
    }

    /**
     * Maps a raw PowerManager THERMAL_STATUS_* int constant to a [ThermalStatus] enum value.
     * Internal visibility allows direct testing without needing PowerManager instrumented context.
     */
    internal fun mapStatus(raw: Int): ThermalStatus = when (raw) {
        PowerManager.THERMAL_STATUS_NONE      -> ThermalStatus.None
        PowerManager.THERMAL_STATUS_LIGHT     -> ThermalStatus.Light
        PowerManager.THERMAL_STATUS_MODERATE  -> ThermalStatus.Moderate
        PowerManager.THERMAL_STATUS_SEVERE    -> ThermalStatus.Severe
        PowerManager.THERMAL_STATUS_CRITICAL  -> ThermalStatus.Critical
        PowerManager.THERMAL_STATUS_EMERGENCY -> ThermalStatus.Emergency
        PowerManager.THERMAL_STATUS_SHUTDOWN  -> ThermalStatus.Shutdown
        else -> ThermalStatus.None
    }

    /**
     * Wave 0 test seam — allows tests to inject a specific ThermalStatus without needing
     * a live PowerManager. Internal visibility; not callable from production code outside
     * this module.
     */
    internal fun setStatusForTest(s: ThermalStatus) {
        _status.value = s
    }
}
