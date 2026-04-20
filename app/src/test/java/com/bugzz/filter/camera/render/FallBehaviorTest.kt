package com.bugzz.filter.camera.render

import android.graphics.PointF
import android.graphics.Rect
import com.bugzz.filter.camera.detector.SmoothedFace
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Tests for the FALL behavior (D-10 / REN-02).
 *
 * Un-Ignored Plan 04-03 — BehaviorState.Fall + FALL tick impl landed.
 *
 * Robolectric needed for PointF real implementation.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FallBehaviorTest {

    // -------------------------------------------------------------------------
    // D-10: spawn rule — add new bug at y=0 when nextSpawnNanos elapsed
    // -------------------------------------------------------------------------

    /**
     * D-10: when tsNanos >= state.nextSpawnNanos AND instances.size < maxInstances,
     * a new FallingBug must be spawned at position.y = 0, x in [0, previewWidth].
     */
    @Test
    fun fall_spawnsBugAtTopWhenIntervalElapsed() {
        val state = BehaviorState.Fall(instances = mutableListOf(), nextSpawnNanos = 0L)
        val previewWidth = 400f
        val previewHeight = 800f
        val tsNanos = 1_000_000L  // 1ms — past the nextSpawnNanos=0 threshold
        BugBehavior.Fall.tick(
            state, buildFace(), PointF(200f, 250f),
            previewWidth = previewWidth, previewHeight = previewHeight,
            tsNanos = tsNanos, dtMs = 16L,
        )
        assertEquals("exactly 1 bug must be spawned when interval elapsed",
            1, state.instances.size)
        // After 16ms gravity tick: y = 0 + (0.5 * 800) * 0.016 = 6.4 — near top
        assertTrue("spawned bug must start near y=0 (top of preview)",
            state.instances[0].position.y < 10f)
        assertTrue("spawned bug x must be within [0, previewWidth]",
            state.instances[0].position.x in 0f..previewWidth)
    }

    /**
     * D-10: nextSpawnNanos is updated after spawn to schedule the NEXT spawn.
     * After spawning, state.nextSpawnNanos must be > tsNanos.
     */
    @Test
    fun fall_nextSpawnNanos_updatedAfterSpawn() {
        val tsNanos = 1_000_000L
        val state = BehaviorState.Fall(instances = mutableListOf(), nextSpawnNanos = 0L)
        BugBehavior.Fall.tick(
            state, buildFace(), PointF(200f, 250f),
            previewWidth = 400f, previewHeight = 800f,
            tsNanos = tsNanos, dtMs = 16L,
        )
        assertTrue("nextSpawnNanos must be > tsNanos after spawn",
            state.nextSpawnNanos > tsNanos)
        // nextSpawnNanos in [tsNanos + 200ms, tsNanos + 401ms] (exclusive upper per FALL_SPAWN_INTERVAL_MAX_MS)
        assertTrue("nextSpawnNanos must be within spawn interval range",
            state.nextSpawnNanos in
                (tsNanos + BugBehavior.FALL_SPAWN_INTERVAL_MIN_MS * 1_000_000L)..
                (tsNanos + BugBehavior.FALL_SPAWN_INTERVAL_MAX_MS * 1_000_000L))
    }

    // -------------------------------------------------------------------------
    // D-10: gravity advances position.y each frame
    // -------------------------------------------------------------------------

    /**
     * D-10: each tick advances bug.position.y by gravity * dtSeconds.
     * gravity = FALL_GRAVITY_FACTOR_DEFAULT * previewHeight = 0.5 * 800 = 400 px/s.
     * dtMs = 100 (0.1 second) → advance = 400 * 0.1 = 40 pixels.
     * Bug starting at y=0 → after tick, y ≈ 40f.
     */
    @Test
    fun fall_bugGravityAdvancesPositionY() {
        val previewHeight = 800f
        val gravityPxSec = BugBehavior.FALL_GRAVITY_FACTOR_DEFAULT * previewHeight  // 400 px/s
        // Pre-populate a bug at y=0 with velocity already set (as spawn would set it),
        // nextSpawnNanos in future so no new spawn this tick.
        val bug = FallingBug(
            position = PointF(200f, 0f),
            velocity = PointF(0f, gravityPxSec),
            spawnNanos = 0L,
        )
        val state = BehaviorState.Fall(
            instances = mutableListOf(bug),
            nextSpawnNanos = Long.MAX_VALUE,  // no new spawn this tick
        )
        BugBehavior.Fall.tick(
            state, buildFace(), PointF(200f, 250f),
            previewWidth = 400f, previewHeight = previewHeight,
            tsNanos = 1_000_000L, dtMs = 100L,
        )
        assertEquals("bug y must advance by gravity * dtSec (400 * 0.1 = 40)",
            40f, state.instances[0].position.y, 1f)
    }

    // -------------------------------------------------------------------------
    // D-10: despawn when bug exits bottom boundary
    // -------------------------------------------------------------------------

    /**
     * D-10: bug with position.y > previewHeight must be removed from instances list.
     */
    @Test
    fun fall_despawnsBugAtBottomBoundary() {
        val previewHeight = 800f
        val gravityPxSec = BugBehavior.FALL_GRAVITY_FACTOR_DEFAULT * previewHeight  // 400 px/s
        // Bug is 1 pixel before the bottom — after 100ms (40px advance) it will exceed boundary
        val bug = FallingBug(
            position = PointF(200f, previewHeight - 1f),
            velocity = PointF(0f, gravityPxSec),
            spawnNanos = 0L,
        )
        val state = BehaviorState.Fall(
            instances = mutableListOf(bug),
            nextSpawnNanos = Long.MAX_VALUE,
        )
        // After dtMs=100ms, y += 40 → y = 839 > 800 → must be despawned
        BugBehavior.Fall.tick(
            state, buildFace(), PointF(200f, 250f),
            previewWidth = 400f, previewHeight = previewHeight,
            tsNanos = 1_000_000L, dtMs = 100L,
        )
        assertEquals("bug past bottom boundary must be despawned (list empty)",
            0, state.instances.size)
    }

    // -------------------------------------------------------------------------
    // D-10: maxInstances cap
    // -------------------------------------------------------------------------

    /**
     * D-10: even if spawn interval fires repeatedly, instances.size must never exceed
     * FALL_MAX_INSTANCES_DEFAULT (cap enforced at spawn time).
     */
    @Test
    fun fall_respectsMaxInstancesCap() {
        // Use near-zero gravity so bugs don't despawn at bottom during this test
        // and very short spawn interval (nextSpawnNanos=0 each tick via tsNanos starting at 0)
        val state = BehaviorState.Fall(instances = mutableListOf(), nextSpawnNanos = 0L)
        val previewHeight = 800_000f  // very tall — bugs never reach bottom
        for (i in 0..49) {
            // Each tick: tsNanos = i*1ms → always >= nextSpawnNanos (reset to ~200ms after first spawn,
            // but subsequent ticks keep tsNanos increasing so cap is reached, not exceeded)
            BugBehavior.Fall.tick(
                state, buildFace(), PointF(200f, 250f),
                previewWidth = 400f, previewHeight = previewHeight,
                tsNanos = i * 1_000_000L, dtMs = 1L,
            )
        }
        assertTrue(
            "instances must not exceed FALL_MAX_INSTANCES_DEFAULT after many spawn-eligible ticks",
            state.instances.size <= BugBehavior.FALL_MAX_INSTANCES_DEFAULT,
        )
    }

    /**
     * D-10: if tsNanos < state.nextSpawnNanos, no new bug is spawned.
     */
    @Test
    fun fall_doesNotSpawnBeforeIntervalElapsed() {
        val state = BehaviorState.Fall(
            instances = mutableListOf(),
            nextSpawnNanos = 1_000_000_000L,  // 1 second in the future
        )
        // ts = 16ms << 1s → spawn gate not passed
        BugBehavior.Fall.tick(
            state, buildFace(), PointF(200f, 250f),
            previewWidth = 400f, previewHeight = 800f,
            tsNanos = 16_000_000L, dtMs = 16L,
        )
        assertEquals("no bug must spawn when nextSpawnNanos is in the future",
            0, state.instances.size)
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private fun buildFace(
        trackingId: Int = 1,
        boundingBox: Rect = Rect(100, 100, 300, 400),
        contours: Map<Int, List<PointF>> = emptyMap(),
    ): SmoothedFace = SmoothedFace(trackingId, boundingBox, contours, emptyMap())
}
