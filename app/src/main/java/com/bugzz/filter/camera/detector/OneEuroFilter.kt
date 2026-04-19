package com.bugzz.filter.camera.detector

import kotlin.math.abs

/**
 * 1€ Filter — low-pass filter with adaptive cutoff for jitter-free live signals.
 * Casiez et al. CHI 2012. Port of the canonical C++/Python reference.
 *
 * Usage: construct one per (face-id × channel); channel = {x, y} for each landmark.
 *
 * @param minCutoff  Base cutoff frequency (Hz). Lower = smoother / laggier. D-20 default 1.0.
 * @param beta       Velocity-scaling of cutoff. Higher = more responsive to motion. D-20 default 0.007.
 * @param dCutoff    Derivative low-pass cutoff (Hz). D-20 default 1.0.
 */
class OneEuroFilter(
    private val minCutoff: Double = 1.0,
    private val beta: Double = 0.007,
    private val dCutoff: Double = 1.0,
) {
    private var xPrev: Double = 0.0
    private var dxPrev: Double = 0.0
    private var tPrevNanos: Long = 0L
    private var initialized = false

    /** @return filtered value at [tNanos] for raw input [x]. */
    fun filter(x: Double, tNanos: Long): Double {
        if (!initialized) {
            xPrev = x
            dxPrev = 0.0
            tPrevNanos = tNanos
            initialized = true
            return x
        }
        val dt = (tNanos - tPrevNanos).coerceAtLeast(1L) / 1e9 // seconds, floor 1ns
        val dx = (x - xPrev) / dt
        val aD = alpha(dt, dCutoff)
        val dxHat = aD * dx + (1 - aD) * dxPrev
        val cutoff = minCutoff + beta * abs(dxHat)
        val aX = alpha(dt, cutoff)
        val xHat = aX * x + (1 - aX) * xPrev
        xPrev = xHat
        dxPrev = dxHat
        tPrevNanos = tNanos
        return xHat
    }

    fun reset() { initialized = false }

    private fun alpha(dt: Double, cutoff: Double): Double {
        // tau = 1 / (2π·cutoff); alpha = 1 / (1 + tau/dt)
        val tau = 1.0 / (2.0 * Math.PI * cutoff)
        return 1.0 / (1.0 + tau / dt)
    }
}

/**
 * Holds per-trackingId × per-channel filters. Clears state when a trackingId disappears
 * (D-22). Re-initializes when the same trackingId reappears — do not carry stale state.
 */
class LandmarkSmoother(
    private val minCutoff: Double = 1.0,
    private val beta: Double = 0.007,
    private val dCutoff: Double = 1.0,
) {
    // Key = "$trackingId:$landmarkName:$channel"
    private val filters = HashMap<String, OneEuroFilter>()

    fun smoothPoint(
        trackingId: Int,
        landmarkName: String,
        xRaw: Float,
        yRaw: Float,
        tNanos: Long,
    ): Pair<Float, Float> {
        val xKey = "$trackingId:$landmarkName:x"
        val yKey = "$trackingId:$landmarkName:y"
        val fx = filters.getOrPut(xKey) { OneEuroFilter(minCutoff, beta, dCutoff) }
        val fy = filters.getOrPut(yKey) { OneEuroFilter(minCutoff, beta, dCutoff) }
        return fx.filter(xRaw.toDouble(), tNanos).toFloat() to
               fy.filter(yRaw.toDouble(), tNanos).toFloat()
    }

    /** Remove state for tracking IDs no longer present (D-22). */
    fun retainActive(activeIds: Set<Int>) {
        val iter = filters.keys.iterator()
        while (iter.hasNext()) {
            val key = iter.next()
            val id = key.substringBefore(':').toIntOrNull() ?: continue
            if (id !in activeIds) iter.remove()
        }
    }

    /** Clear entirely — invoke on lens flip to avoid cross-session state leak (PITFALLS #6). */
    fun clear() = filters.clear()
}
