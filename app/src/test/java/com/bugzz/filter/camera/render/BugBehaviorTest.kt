package com.bugzz.filter.camera.render

import android.graphics.PointF
import android.graphics.Rect
import com.bugzz.filter.camera.detector.SmoothedFace
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Nyquist Wave 0 tests for [BugBehavior] sealed interface (REN-02 / D-04).
 *
 * Covers: sealed variant count = 4, Static.tick sets position to anchor + velocity to zero,
 * Crawl/Swarm/Fall throw NotImplementedError.
 *
 * Static is fully implemented in Wave 0 — those tests are GREEN.
 * Crawl/Swarm/Fall throw NotImplementedError — those tests are GREEN (they assert the throw).
 * sealedInterface_hasExactlyFourVariants depends on reflection — GREEN in Wave 0.
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
        val nestedClasses = BugBehavior::class.java.declaredClasses
        val variantNames = nestedClasses.map { it.simpleName }.toSet()
        assertEquals(
            "BugBehavior must have exactly 4 sealed variants: Static, Crawl, Swarm, Fall (D-04 / REN-02)",
            setOf("Static", "Crawl", "Swarm", "Fall"), variantNames,
        )
    }

    @Test
    fun static_tick_setsPositionToAnchor() {
        val state = BugState(position = PointF(0f, 0f), velocity = PointF(0f, 0f))
        val face = buildFace()
        val anchor = PointF(50f, 75f)

        BugBehavior.Static.tick(state, face, anchor, dtMs = 16L)

        assertEquals("Static.tick must set position.x to anchor.x", 50f, state.position.x, 0.01f)
        assertEquals("Static.tick must set position.y to anchor.y", 75f, state.position.y, 0.01f)
    }

    @Test
    fun static_tick_setsVelocityToZero() {
        val state = BugState(position = PointF(0f, 0f), velocity = PointF(100f, 100f))
        val face = buildFace()
        val anchor = PointF(50f, 75f)

        BugBehavior.Static.tick(state, face, anchor, dtMs = 16L)

        assertEquals("Static.tick must set velocity.x to 0f", 0f, state.velocity.x, 0.01f)
        assertEquals("Static.tick must set velocity.y to 0f", 0f, state.velocity.y, 0.01f)
    }

    @Test(expected = NotImplementedError::class)
    fun crawl_tick_throwsNotImplementedError() {
        val state = BugState()
        BugBehavior.Crawl.tick(state, buildFace(), PointF(0f, 0f), 16L)
    }

    @Test(expected = NotImplementedError::class)
    fun swarm_tick_throwsNotImplementedError() {
        val state = BugState()
        BugBehavior.Swarm.tick(state, buildFace(), PointF(0f, 0f), 16L)
    }

    @Test(expected = NotImplementedError::class)
    fun fall_tick_throwsNotImplementedError() {
        val state = BugState()
        BugBehavior.Fall.tick(state, buildFace(), PointF(0f, 0f), 16L)
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private fun buildFace() = SmoothedFace(
        trackingId = 0,
        boundingBox = Rect(0, 0, 200, 200),
        contours = emptyMap(),
        landmarks = emptyMap(),
    )
}
