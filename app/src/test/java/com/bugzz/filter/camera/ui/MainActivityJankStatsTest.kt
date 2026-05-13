package com.bugzz.filter.camera.ui

import org.junit.Assert.fail
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Nyquist Wave 0 RED scaffolds — Plan 07-03 lands MainActivity JankStats wire-in + un-Ignores.
 *
 * Pins PRF-01 wire-in (D-01 + RESEARCH Pattern 1 + Pitfall 4):
 *  - JankStats.createAndTrack(window) bound in onCreate ONLY when BuildConfig.DEBUG.
 *  - isTrackingEnabled = true on onResume; false on onPause.
 *  - Hardware-accelerated window (Pitfall 4 — JankStats no-ops on software-rendered Window).
 *
 * Robolectric ActivityScenario harness needed to launch MainActivity in JVM.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MainActivityJankStatsTest {

    @Test @Ignore("Plan 07-03 — MainActivity JankStats wire-in pending")
    fun onCreate_registers_jankstats_when_debug_build() {
        fail("Plan 07-03 implements JankStats.createAndTrack(window) — Wave 0 RED")
    }

    @Test @Ignore("Plan 07-03 — MainActivity JankStats wire-in pending")
    fun onResume_enables_tracking_and_onPause_disables_it() {
        fail("Plan 07-03 implements lifecycle isTrackingEnabled toggle — Wave 0 RED")
    }
}
