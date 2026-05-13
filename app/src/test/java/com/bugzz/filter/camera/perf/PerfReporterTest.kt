package com.bugzz.filter.camera.perf

import org.junit.Assert.fail
import org.junit.Ignore
import org.junit.Test

/**
 * Nyquist Wave 0 RED scaffolds — Plan 07-03 lands PerfReporter impl + un-Ignores these.
 *
 * Pins PRF-01 (live preview ≥24 fps) aggregation correctness:
 *  - Rolling window of N=1000 frame samples (recommended in RESEARCH §Claude's Discretion).
 *  - median / p95 / p99 computed via sort-based percentile.
 *  - Skip first 60 frames (cold-start setContent jank — RESEARCH Pitfall 9).
 */
class PerfReporterTest {

    @Test @Ignore("Plan 07-03 — PerfReporter aggregator impl pending")
    fun median_of_seven_samples_returns_middle_value() {
        fail("Plan 07-03 implements record() + stats() — Wave 0 RED")
    }

    @Test @Ignore("Plan 07-03 — PerfReporter aggregator impl pending")
    fun p95_of_hundred_samples_returns_index_94() {
        fail("Plan 07-03 implements record() + stats() — Wave 0 RED")
    }

    @Test @Ignore("Plan 07-03 — PerfReporter aggregator impl pending")
    fun window_rotation_drops_oldest_when_capacity_exceeded() {
        fail("Plan 07-03 implements ring buffer rotation — Wave 0 RED")
    }
}
