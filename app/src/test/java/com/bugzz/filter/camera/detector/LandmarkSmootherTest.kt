package com.bugzz.filter.camera.detector

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Nyquist Wave 0 tests for [LandmarkSmoother.onFaceLost] (ADR-01 #2).
 *
 * Wave 1 state: @Ignore annotations removed — Plan 03-02 has implemented
 * [LandmarkSmoother.onFaceLost] with a real body. All tests must be GREEN.
 *
 * Pure JVM — no Robolectric needed (LandmarkSmoother only uses Double arithmetic).
 */
class LandmarkSmootherTest {

    private val smoother = LandmarkSmoother(minCutoff = 1.0, beta = 0.007, dCutoff = 1.0)

    @Test
    fun onFaceLost_clearsThatIdOnly_otherIdsSurvive() {
        // Seed two IDs with initial values
        smoother.smoothPoint(5, "nose", 10f, 10f, 1_000_000_000L)
        smoother.smoothPoint(9, "nose", 20f, 20f, 1_000_000_000L)

        // Lose id=5
        smoother.onFaceLost(5)

        // id=5: should behave as first-call (returns raw input — OneEuroFilter reinitializes)
        val (x5, y5) = smoother.smoothPoint(5, "nose", 100f, 100f, 2_000_000_000L)
        assertEquals(
            "After onFaceLost(5), first new smoothPoint call must return raw x (fresh filter)",
            100f, x5, 1e-4f,
        )
        assertEquals(
            "After onFaceLost(5), first new smoothPoint call must return raw y (fresh filter)",
            100f, y5, 1e-4f,
        )

        // id=9: should continue using prior seeded state (not be cleared)
        // Call again at the same location — 1€ filter with prior state should return
        // a value close to (but smoothed from) 20f since velocity is low
        val (x9, _) = smoother.smoothPoint(9, "nose", 20f, 20f, 2_000_000_000L)
        // The filter for id=9 was seeded at 20f and called again at 20f → result should be ~20f
        assertEquals(
            "id=9 filter must survive onFaceLost(5) — state retained, returns ~20f",
            20f, x9, 0.5f,
        )
    }

    @Test
    fun sameIdReappears_startsFreshState() {
        // Seed id=3 at time 1000ms
        smoother.smoothPoint(3, "nose", 50f, 50f, 1_000_000_000L)

        // Lose id=3
        smoother.onFaceLost(3)

        // Reappear at a very different value — must return the new raw value exactly (fresh init)
        val (x, y) = smoother.smoothPoint(3, "nose", 100f, 100f, 2_000_000_000L)
        assertEquals(
            "Fresh 1€ filter must return raw x=100f on first call after onFaceLost",
            100f, x, 1e-4f,
        )
        assertEquals(
            "Fresh 1€ filter must return raw y=100f on first call after onFaceLost",
            100f, y, 1e-4f,
        )
    }

    @Test
    fun onFaceLost_unknownId_isNoOp() {
        // onFaceLost on an ID that never had state — must not throw
        smoother.onFaceLost(42)

        // Subsequent smoothPoint for id=42 must start fresh (returns raw input)
        val (x, y) = smoother.smoothPoint(42, "nose", 77f, 88f, 1_000_000_000L)
        assertEquals("Fresh filter for previously-unknown id must return raw x", 77f, x, 1e-4f)
        assertEquals("Fresh filter for previously-unknown id must return raw y", 88f, y, 1e-4f)
    }
}
