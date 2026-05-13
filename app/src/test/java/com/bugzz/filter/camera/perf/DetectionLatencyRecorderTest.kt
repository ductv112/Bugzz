package com.bugzz.filter.camera.perf

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Plan 07-03 un-Ignored tests — verify ring buffer + pXX percentile contract.
 *
 * Pins PRF-02 (face detection latency ≤100ms median) aggregation:
 *  - Ring buffer of 1000 latency samples (CONTEXT D-04 "Aggregate ≥1000 samples for stats").
 *  - stats() returns LatencyStats(median, p95, p99, count) per data class shape.
 *  - Empty buffer returns count=0 + median=0 (no NPE / IndexOutOfBoundsException).
 */
class DetectionLatencyRecorderTest {

    @Test
    fun record_appends_to_ring_buffer_until_capacity() {
        val r = DetectionLatencyRecorder(capacity = 5)
        listOf(10L, 20L, 30L).forEach { r.record(it) }
        assertEquals(3, r.stats().count)
    }

    @Test
    fun stats_returns_median_p95_p99_over_known_samples() {
        val r = DetectionLatencyRecorder(capacity = 1000)
        (1L..100L).forEach { r.record(it) }
        val s = r.stats()
        assertEquals(100, s.count)
        // size/2 = 50 → sorted[50] (0-indexed) = 51
        assertEquals(51L, s.median)
        // p95Idx = 95 → sorted[95] = 96; p99Idx = 99 → sorted[99] = 100
        assertEquals(96L, s.p95)
        assertEquals(100L, s.p99)
    }

    @Test
    fun stats_on_empty_buffer_returns_count_zero_no_crash() {
        val r = DetectionLatencyRecorder(capacity = 5)
        val s = r.stats()
        assertEquals(0, s.count)
        assertEquals(0L, s.median)
        assertEquals(0L, s.p95)
        assertEquals(0L, s.p99)
    }
}
