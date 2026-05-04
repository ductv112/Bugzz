package com.bugzz.filter.camera.thermal

import org.junit.Assert
import org.junit.Ignore
import org.junit.Test

/**
 * RED scaffold for VID-08. SUT lands in Plan 05-02.
 *
 * Mock ThermalStatus values; verify ordinal ordering supports >= comparison;
 * verify frame-skip logic skips every 2nd frame when status >= Moderate.
 *
 * Expected API shape (Plan 05-02):
 *   - ThermalStatus enum: None, Light, Moderate, Severe (ordinal ordering supports >= comparison)
 *   - ThermalMonitor @Singleton @Inject constructor(@ApplicationContext context: Context)
 *     - status: StateFlow<ThermalStatus> (initial = None)
 *     - registers PowerManager.OnThermalStatusChangedListener on init (API 29+)
 *     - cleanup() — removes listener (called from BugzzApplication.onTerminate or Application coroutine)
 *   - FaceDetectorClient frame-skip: when ThermalMonitor.status.value >= ThermalStatus.Moderate,
 *     skip every other MlKitAnalyzer invocation (frame counter % 2 == 1 → skip)
 *
 * D-14: ThermalMonitor registered as @Singleton in BugzzApplication.onCreate.
 */
class ThermalMonitorTest {

    @Ignore("Plan 05-02 lands ThermalMonitor SUT")
    @Test
    fun mapStatus_powerManagerNone_returnsNone() {
        Assert.fail("Plan 05-02 lands SUT")
    }

    @Ignore("Plan 05-02 lands ThermalMonitor SUT")
    @Test
    fun mapStatus_powerManagerModerate_returnsModerate() {
        Assert.fail("Plan 05-02 lands SUT")
    }

    @Ignore("Plan 05-02 lands ThermalMonitor SUT")
    @Test
    fun thermalStatus_compareTo_supportsGreaterEqual() {
        Assert.fail("Plan 05-02 lands SUT")
    }

    @Ignore("Plan 05-02 lands ThermalMonitor SUT")
    @Test
    fun status_initialValue_isNone() {
        Assert.fail("Plan 05-02 lands SUT")
    }

    @Ignore("Plan 05-02 lands ThermalMonitor SUT")
    @Test
    fun frameSkip_belowModerate_doesNotSkip() {
        Assert.fail("Plan 05-02 lands SUT")
    }

    @Ignore("Plan 05-02 lands ThermalMonitor SUT")
    @Test
    fun frameSkip_atModerate_skipsEverySecondFrame() {
        Assert.fail("Plan 05-02 lands SUT")
    }
}
