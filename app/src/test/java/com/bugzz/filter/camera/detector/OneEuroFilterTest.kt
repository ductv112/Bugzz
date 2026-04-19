package com.bugzz.filter.camera.detector

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Nyquist unit test for the 1€ filter (CAM-09 / D-20 / D-21).
 *
 * Pins the mathematical behaviour Plan 02-03 must implement in
 * [com.bugzz.filter.camera.detector.OneEuroFilter]. The tests assert the four
 * canonical properties of the 1€ filter documented in 02-VALIDATION.md
 * §Wave 0 Requirements bullet 1:
 *  1. Constant input passes through unchanged.
 *  2. Step input converges smoothly (no instantaneous jump).
 *  3. High-frequency sine jitter is attenuated vs. raw RMS.
 *  4. First sample initialises without division-by-zero / NaN.
 *
 * Intentional Nyquist RED state: the class [OneEuroFilter] does not yet exist;
 * Plan 02-03 lands it. Compilation against this test must fail with
 * "Unresolved reference: OneEuroFilter" until then.
 */
class OneEuroFilterTest {

    @Test
    fun constant_input_passes_through_within_epsilon() {
        val f = OneEuroFilter(minCutoff = 1.0, beta = 0.007, dCutoff = 1.0)
        val epsilon = 1e-6
        for (i in 0..10) {
            val out = f.filter(5.0, i * 100_000_000L)
            assertEquals("constant input at i=$i must pass through", 5.0, out, epsilon)
        }
    }

    @Test
    fun step_input_converges_smoothly() {
        val f = OneEuroFilter()
        // Initialisation — first sample at t=0 returns the input verbatim.
        assertEquals(0.0, f.filter(0.0, 0L), 1e-9)

        // Step change to 100.0 at t = 1s; filter must smooth, not jump.
        val firstStep = f.filter(100.0, 1_000_000_000L)
        assertTrue(
            "first step must smooth (not jump straight to 100, got $firstStep)",
            firstStep < 100.0,
        )

        // After ~10 samples at 100.0 the filter should have converged within 5%.
        var last = firstStep
        for (i in 2..10) {
            last = f.filter(100.0, i * 1_000_000_000L)
        }
        assertTrue(
            "after ~10 samples must have converged within 5% of 100 (got $last)",
            abs(last - 100.0) < 5.0,
        )
    }

    @Test
    fun sine_jitter_attenuated_vs_rms() {
        val f = OneEuroFilter()
        val raws = mutableListOf<Double>()
        val smoothed = mutableListOf<Double>()
        val base = 50.0
        // 30 samples at ~30 fps (33 ms per sample) of a 2-unit amplitude sine
        // riding on a stationary base of 50.0. Period = 5 samples → ~6 Hz at 30 fps,
        // well above the default minCutoff of 1 Hz, so the filter must attenuate.
        for (i in 0 until 30) {
            val noise = 2.0 * sin(2.0 * Math.PI * i / 5.0)
            val raw = base + noise
            raws += raw
            smoothed += f.filter(raw, i * 33_000_000L)
        }
        val rawRms = sqrt(raws.sumOf { (it - base) * (it - base) } / raws.size)
        val smoothedRms = sqrt(smoothed.sumOf { (it - base) * (it - base) } / smoothed.size)
        assertTrue(
            "smoothed RMS ($smoothedRms) must be less than raw RMS ($rawRms)",
            smoothedRms < rawRms,
        )
    }

    @Test
    fun first_sample_no_nan_no_division_by_zero() {
        val f = OneEuroFilter()
        val first = f.filter(42.5, 0L)
        assertEquals("first sample must initialise to input value", 42.5, first, 1e-12)

        // Second call with dt = 1 ns exercises the divisor floor — must not
        // produce NaN or Infinity even with the minimum possible timestamp delta.
        val second = f.filter(42.5, 1L)
        assertFalse("second call must not produce NaN", second.isNaN())
        assertFalse("second call must not produce Infinity", second.isInfinite())
    }
}
