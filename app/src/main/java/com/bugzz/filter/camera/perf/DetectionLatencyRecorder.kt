package com.bugzz.filter.camera.perf

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Ring buffer of recent ML Kit face-detection latency samples (PRF-02 / D-04).
 *
 * Same architecture as [PerfReporter] — coarse synchronized lock, ring buffer with
 * sort-based percentile snapshot. No cold-start skip semantics (face detection has no
 * first-frame-jank analog; raw latency is meaningful from frame 1).
 *
 * Single-writer (cameraExecutor — FaceDetectorClient.createAnalyzer consumer), multi-reader.
 * Capacity hint: 1000 samples per RESEARCH §Claude's Discretion bullet "JankStats aggregation window".
 *
 * **Test seam:** [capacity] is a constructor parameter with default so tests can shrink the
 * buffer. Production uses `@Inject constructor()` delegating to the default.
 */
@Singleton
class DetectionLatencyRecorder internal constructor(
    internal val capacity: Int = DEFAULT_CAPACITY,
) {
    /** No-arg constructor for Hilt (see [PerfReporter] / Decision #14 STATE.md). */
    @Inject constructor() : this(DEFAULT_CAPACITY)

    private val lock = Any()
    private val ring: LongArray = LongArray(capacity)
    private var head = 0
    private var size = 0

    /** Record one detect call's latency in ms. */
    fun record(detectMs: Long) {
        synchronized(lock) {
            ring[head] = detectMs
            head = (head + 1) % capacity
            if (size < capacity) size++
        }
    }

    /** Aggregate over the current window. Returns all-zero when empty (no NPE/OOB). */
    fun stats(): LatencyStats = synchronized(lock) {
        if (size == 0) return LatencyStats(0L, 0L, 0L, 0)
        val sorted = LongArray(size)
        if (size == capacity) {
            // Ring wrapped: oldest at `head`, newest just before `head`.
            System.arraycopy(ring, head, sorted, 0, capacity - head)
            System.arraycopy(ring, 0, sorted, capacity - head, head)
        } else {
            // No wrap yet.
            System.arraycopy(ring, 0, sorted, 0, size)
        }
        sorted.sort()
        LatencyStats(
            median = sorted[size / 2],
            p95 = sorted[((size * 95) / 100).coerceAtMost(size - 1)],
            p99 = sorted[((size * 99) / 100).coerceAtMost(size - 1)],
            count = size,
        )
    }

    companion object {
        internal const val DEFAULT_CAPACITY: Int = 1000
    }
}

/** Snapshot of aggregated latency statistics over a rolling sample window. */
data class LatencyStats(
    val median: Long,
    val p95: Long,
    val p99: Long,
    val count: Int,
)
