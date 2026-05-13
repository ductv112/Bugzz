package com.bugzz.filter.camera.perf

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Aggregates JankStats FrameData samples into a rolling-window pXX summary.
 *
 * **Skeleton — Plan 07-03 implements the ring buffer + percentile math.**
 *
 * Phase 7 D-01 (JankStats wire-in) + PRF-01 (≥24 fps live preview validation aggregator).
 * Debug-only: this @Singleton is instantiated only because JankStats itself is
 * debugImplementation. Release builds never construct it (Hilt module is no-op in release).
 */
@Singleton
class PerfReporter @Inject constructor() {
    /** Record a single frame duration sample in milliseconds. Wave 2 implements full body. */
    fun record(frameDurationMs: Long): Unit = TODO("Plan 07-03 — pXX aggregator impl")
}
