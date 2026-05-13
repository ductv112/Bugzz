package com.bugzz.filter.camera.perf

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Aggregates JankStats FrameData samples into rolling-window pXX statistics.
 *
 * Phase 7 D-01 + PRF-01 (live preview ≥24 fps validation).
 *
 * Thread-safety: single coarse `synchronized(lock)` per record/stats — JankStats fires at
 * vsync rate (≤120/s) so contention is negligible. Reader from arbitrary thread (e.g.
 * MainActivityJankStatsTest scenarios) holds the same lock briefly.
 *
 * Cold-start skip: first [skipFrames] samples are ignored per RESEARCH Pitfall 9 (first
 * Compose setContent + measure + layout always blows the budget; including it skews median).
 *
 * **Test seam:** [capacity] and [skipFrames] are constructor parameters with defaults so unit
 * tests can disable the skip and shrink the buffer. Production uses `@Inject constructor()`
 * which delegates to the defaults (Phase 02-05 / Decision #14 STATE.md constructor-split pattern).
 */
@Singleton
class PerfReporter internal constructor(
    internal val capacity: Int = DEFAULT_CAPACITY,
    internal val skipFrames: Int = DEFAULT_SKIP_FRAMES,
) {
    /**
     * No-arg constructor for Hilt — Dagger does not invoke Kotlin synthetic `$default` methods
     * for primary-constructor defaults, so we expose an explicit zero-arg `@Inject` constructor
     * that delegates to the primary with production defaults. Same pattern as
     * [com.bugzz.filter.camera.camera.CameraController] (Decision #14 in STATE.md).
     */
    @Inject constructor() : this(DEFAULT_CAPACITY, DEFAULT_SKIP_FRAMES)

    private val lock = Any()
    private val ring: LongArray = LongArray(capacity)
    private var head = 0
    private var size = 0
    private var dropped = 0

    /** Record a single frame duration sample in milliseconds. Cold-start frames are dropped. */
    fun record(frameDurationMs: Long) {
        synchronized(lock) {
            if (dropped < skipFrames) {
                dropped++
                return
            }
            ring[head] = frameDurationMs
            head = (head + 1) % capacity
            if (size < capacity) size++
        }
    }

    /** Aggregate the current rolling-window samples. Returns all-zero when empty (no NPE/OOB). */
    fun stats(): PerfStats = synchronized(lock) {
        if (size == 0) return PerfStats(0L, 0L, 0L, 0)
        val sorted = LongArray(size)
        if (size == capacity) {
            // Ring wrapped: oldest is at `head`, newest just before `head`.
            System.arraycopy(ring, head, sorted, 0, capacity - head)
            System.arraycopy(ring, 0, sorted, capacity - head, head)
        } else {
            // No wrap yet: head equals size; slots [0, size) hold the samples in insertion order.
            System.arraycopy(ring, 0, sorted, 0, size)
        }
        sorted.sort()
        PerfStats(
            median = sorted[size / 2],
            p95 = sorted[((size * 95) / 100).coerceAtMost(size - 1)],
            p99 = sorted[((size * 99) / 100).coerceAtMost(size - 1)],
            count = size,
        )
    }

    companion object {
        internal const val DEFAULT_CAPACITY: Int = 1000

        /** RESEARCH Pitfall 9 — first frames are cold-start setContent jank; not representative. */
        internal const val DEFAULT_SKIP_FRAMES: Int = 60
    }
}

/** Snapshot of aggregated frame-duration percentiles over the current rolling window. */
data class PerfStats(
    val median: Long,
    val p95: Long,
    val p99: Long,
    val count: Int,
)
