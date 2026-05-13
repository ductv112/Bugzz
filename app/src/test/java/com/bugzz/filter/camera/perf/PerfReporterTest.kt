package com.bugzz.filter.camera.perf

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Plan 07-03 un-Ignored tests — verify pXX aggregator correctness.
 *
 * Pins PRF-01 (live preview ≥24 fps) aggregation correctness:
 *  - Rolling window of N=1000 frame samples (RESEARCH §Claude's Discretion).
 *  - median / p95 / p99 computed via sort-based percentile.
 *  - Skip first 60 frames is on by default — tests construct with skipFrames=0 to bypass
 *    (mirrors Phase 02-05 internal-constructor test seam pattern; Decision #14 STATE.md).
 */
class PerfReporterTest {

    @Test
    fun median_of_seven_samples_returns_middle_value() {
        val r = PerfReporter(capacity = 100, skipFrames = 0)
        listOf(10L, 20L, 30L, 40L, 50L, 60L, 70L).forEach { r.record(it) }
        val s = r.stats()
        assertEquals(7, s.count)
        // size/2 == 3 → sorted[3] == 40 (0-indexed: 10,20,30,40,50,60,70)
        assertEquals(40L, s.median)
    }

    @Test
    fun p95_of_hundred_samples_returns_high_quantile() {
        val r = PerfReporter(capacity = 200, skipFrames = 0)
        (1L..100L).forEach { r.record(it) }
        val s = r.stats()
        assertEquals(100, s.count)
        // p95Idx = (100 * 95) / 100 = 95 → sorted[95] (0-indexed) = 96
        assertEquals(96L, s.p95)
        // p99Idx = 99 → sorted[99] = 100
        assertEquals(100L, s.p99)
    }

    @Test
    fun window_rotation_drops_oldest_when_capacity_exceeded() {
        val r = PerfReporter(capacity = 10, skipFrames = 0)
        (1L..15L).forEach { r.record(it) }   // 15 samples into 10-slot ring
        val s = r.stats()
        assertEquals(10, s.count)
        // After wrap: kept samples are [6..15]; sorted → [6,7,8,9,10,11,12,13,14,15]; median sorted[5] = 11
        assertEquals(11L, s.median)
    }

    @Test
    fun stats_on_empty_returns_zero_count_no_crash() {
        val r = PerfReporter(capacity = 10, skipFrames = 0)
        val s = r.stats()
        assertEquals(0, s.count)
        assertEquals(0L, s.median)
        assertEquals(0L, s.p95)
        assertEquals(0L, s.p99)
    }

    @Test
    fun cold_start_skip_drops_first_n_samples_before_recording() {
        val r = PerfReporter(capacity = 100, skipFrames = 3)
        // First 3 samples are dropped (cold-start setContent jank — RESEARCH Pitfall 9).
        listOf(999L, 999L, 999L, 10L, 20L, 30L).forEach { r.record(it) }
        val s = r.stats()
        assertEquals(3, s.count)
        // Only [10, 20, 30] should be recorded; median sorted[1] = 20.
        assertEquals(20L, s.median)
    }
}
