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
 * Tests for [BugBehavior] sealed interface (REN-02 / D-04).
 *
 * Phase 03-03: Static.tick tests GREEN, Crawl/Swarm/Fall assert NotImplementedError.
 * Phase 04-03: All 4 variants have real impls — NotImplementedError stubs removed.
 *   New tests: crawl_tick_advancesProgress, swarm_tick_initializesInstances,
 *   fall_tick_spawnsFirstBugImmediately — confirm basic real-impl behaviour.
 *   sealedInterface_hasExactlyFourVariants unchanged (still GREEN).
 *
 * Robolectric needed for PointF.set() and PointF constructor real implementations.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BugBehaviorTest {

    @Test
    fun sealedInterface_hasExactlyFourVariants() {
        // Use Java reflection to list nested classes — avoids kotlin-reflect dependency.
        // BugBehavior sealed objects are compiled as nested static classes.
        // Filter out Companion (added in Plan 04-03 for default constants + helpers).
        val nestedClasses = BugBehavior::class.java.declaredClasses
        val variantNames = nestedClasses
            .map { it.simpleName }
            .filter { it != "Companion" }
            .toSet()
        assertEquals(
            "BugBehavior must have exactly 4 sealed variants: Static, Crawl, Swarm, Fall (D-04 / REN-02)",
            setOf("Static", "Crawl", "Swarm", "Fall"), variantNames,
        )
    }

    @Test
    fun static_tick_setsPositionToAnchor() {
        val state = BehaviorState.Static(pos = PointF(0f, 0f))
        val face = buildFace()
        val anchor = PointF(50f, 75f)

        BugBehavior.Static.tick(
            state, face, anchor,
            previewWidth = 400f, previewHeight = 800f,
            tsNanos = 1_000_000L, dtMs = 16L,
        )

        assertEquals("Static.tick must set pos.x to anchor.x", 50f, state.pos.x, 0.01f)
        assertEquals("Static.tick must set pos.y to anchor.y", 75f, state.pos.y, 0.01f)
    }

    // Phase 03-03 stub tests removed Plan 04-03 — real impls now GREEN

    @Test
    fun crawl_tick_advancesProgress() {
        val state = BehaviorState.Crawl(progress = 0f)
        val face = buildFace(boundingBox = Rect(100, 100, 300, 400))
        BugBehavior.Crawl.tick(
            state, face, PointF(200f, 250f),
            previewWidth = 400f, previewHeight = 800f,
            tsNanos = 1_000_000L, dtMs = 1000L,
        )
        assertTrue("CRAWL tick must advance progress from 0", state.progress > 0f)
    }

    @Test
    fun swarm_tick_initializesInstances() {
        val state = BehaviorState.Swarm()
        val face = buildFace(boundingBox = Rect(100, 100, 300, 400))
        BugBehavior.Swarm.tick(
            state, face, PointF(200f, 250f),
            previewWidth = 400f, previewHeight = 800f,
            tsNanos = 1_000_000L, dtMs = 16L,
        )
        assertEquals(
            "SWARM initial tick spawns instanceCount instances",
            BugBehavior.SWARM_INSTANCE_COUNT_DEFAULT, state.instances.size,
        )
    }

    @Test
    fun fall_tick_spawnsFirstBugImmediately() {
        val state = BehaviorState.Fall(nextSpawnNanos = 0L)
        val face = buildFace(boundingBox = Rect(100, 100, 300, 400))
        BugBehavior.Fall.tick(
            state, face, PointF(200f, 250f),
            previewWidth = 400f, previewHeight = 800f,
            tsNanos = 1_000_000L, dtMs = 16L,
        )
        assertTrue("FALL first tick spawns 1+ bugs", state.instances.isNotEmpty())
        // The newly-spawned bug starts at y=0 before gravity is applied in this first tick.
        // After one 16ms tick at gravity=0.5*800=400px/s: y += 400 * 0.016 = 6.4px.
        assertTrue("FALL new bug y must be near top (spawned at y=0)", state.instances[0].position.y < 10f)
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private fun buildFace(
        boundingBox: Rect = Rect(0, 0, 200, 200),
    ) = SmoothedFace(
        trackingId = 0,
        boundingBox = boundingBox,
        contours = emptyMap(),
        landmarks = emptyMap(),
    )
}
