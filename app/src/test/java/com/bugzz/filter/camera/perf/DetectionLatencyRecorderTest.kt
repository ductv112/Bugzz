package com.bugzz.filter.camera.perf

import org.junit.Assert.fail
import org.junit.Ignore
import org.junit.Test

/**
 * Nyquist Wave 0 RED scaffolds — Plan 07-03 lands DetectionLatencyRecorder impl + un-Ignores.
 *
 * Pins PRF-02 (face detection latency ≤100ms median) aggregation:
 *  - Ring buffer of 1000 latency samples (CONTEXT D-04 "Aggregate ≥1000 samples for stats").
 *  - stats() returns LatencyStats(median, p95, p99, count) per data class shape.
 *  - Empty buffer returns count=0 + median=0 (no NPE / IndexOutOfBoundsException).
 */
class DetectionLatencyRecorderTest {

    @Test @Ignore("Plan 07-03 — DetectionLatencyRecorder ring buffer impl pending")
    fun record_appends_to_ring_buffer_until_capacity() {
        fail("Plan 07-03 implements record() — Wave 0 RED")
    }

    @Test @Ignore("Plan 07-03 — DetectionLatencyRecorder ring buffer impl pending")
    fun stats_returns_median_p95_p99_over_known_samples() {
        fail("Plan 07-03 implements stats() pXX math — Wave 0 RED")
    }

    @Test @Ignore("Plan 07-03 — DetectionLatencyRecorder ring buffer impl pending")
    fun stats_on_empty_buffer_returns_count_zero_no_crash() {
        fail("Plan 07-03 — empty ring contract — Wave 0 RED")
    }
}
