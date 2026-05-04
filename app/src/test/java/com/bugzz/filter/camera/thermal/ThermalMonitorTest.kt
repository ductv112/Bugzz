package com.bugzz.filter.camera.thermal

import android.os.PowerManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.util.concurrent.Executor

/**
 * Unit tests for ThermalMonitor — VID-08.
 * Un-Ignored in Plan 05-02 when SUT landed.
 *
 * Uses Robolectric for PowerManager constant access; ThermalMonitor.mapStatus() is internal
 * so we call it directly. ThermalMonitor.setStatusForTest() is the Wave 0 test seam used to
 * simulate thermal state changes without a live PowerManager callback.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ThermalMonitorTest {

    private lateinit var monitor: ThermalMonitor
    private val directExecutor = Executor { command -> command.run() }

    @Before
    fun setUp() {
        val context = RuntimeEnvironment.getApplication()
        monitor = ThermalMonitor(context, directExecutor)
    }

    @Test
    fun mapStatus_powerManagerNone_returnsNone() {
        assertEquals(ThermalStatus.None, monitor.mapStatus(PowerManager.THERMAL_STATUS_NONE))
    }

    @Test
    fun mapStatus_powerManagerModerate_returnsModerate() {
        assertEquals(ThermalStatus.Moderate, monitor.mapStatus(PowerManager.THERMAL_STATUS_MODERATE))
    }

    @Test
    fun thermalStatus_compareTo_supportsGreaterEqual() {
        assertTrue(ThermalStatus.Moderate >= ThermalStatus.Light)
        assertFalse(ThermalStatus.Light >= ThermalStatus.Moderate)
        assertTrue(ThermalStatus.Severe >= ThermalStatus.Moderate)
        assertTrue(ThermalStatus.None >= ThermalStatus.None)  // equal is >= too
    }

    @Test
    fun status_initialValue_isNone() {
        assertEquals(ThermalStatus.None, monitor.status.value)
    }

    @Test
    fun frameSkip_belowModerate_doesNotSkip() {
        monitor.setStatusForTest(ThermalStatus.Light)
        for (counter in 1..10) {
            assertFalse(
                "Expected no skip at Light for counter=$counter",
                monitor.shouldSkipFrame(counter),
            )
        }
    }

    @Test
    fun frameSkip_atModerate_skipsEverySecondFrame() {
        monitor.setStatusForTest(ThermalStatus.Moderate)
        for (counter in 1..10) {
            val expected = counter % 2 != 0  // odd frames are skipped
            assertEquals(
                "At Moderate counter=$counter expected skip=$expected",
                expected,
                monitor.shouldSkipFrame(counter),
            )
        }
    }
}
