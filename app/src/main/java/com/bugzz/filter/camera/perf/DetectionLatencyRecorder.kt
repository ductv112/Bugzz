package com.bugzz.filter.camera.perf

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Ring buffer of recent ML Kit face-detection latency samples. Exposes median / p95 / p99 query.
 *
 * **Skeleton — Plan 07-03 implements the ring buffer + sort-based percentile.**
 *
 * Phase 7 D-04 (face detection latency log) + PRF-02 (≤100ms median).
 * Single-writer (cameraExecutor — FaceDetectorClient.createAnalyzer consumer), multi-reader.
 * Capacity hint: 1000 samples per RESEARCH §Claude's Discretion bullet "JankStats aggregation window".
 */
@Singleton
class DetectionLatencyRecorder @Inject constructor() {
    /** Record one detect call's latency in ms. Wave 2 implements the ring write. */
    fun record(detectMs: Long): Unit = TODO("Plan 07-03 — ring buffer write impl")

    /** Aggregate over the current window. Wave 2 implements the sort + percentile math. */
    fun stats(): LatencyStats = TODO("Plan 07-03 — sort + pXX impl")
}

/** Snapshot of aggregated latency statistics over a rolling sample window. */
data class LatencyStats(
    val median: Long,
    val p95: Long,
    val p99: Long,
    val count: Int,
)
