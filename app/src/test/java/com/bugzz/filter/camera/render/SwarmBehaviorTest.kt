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
 * Tests for the SWARM behavior (D-09 / REN-02).
 *
 * Un-Ignored Plan 04-03 — BehaviorState.Swarm + SWARM tick impl landed.
 *
 * Robolectric needed for PointF real implementation.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SwarmBehaviorTest {

    // -------------------------------------------------------------------------
    // D-09: instance initialization on first tick
    // -------------------------------------------------------------------------

    /**
     * D-09: on first tick with empty instances list, SWARM must spawn exactly
     * SWARM_INSTANCE_COUNT_DEFAULT bug instances at bbox-edge points.
     */
    @Test
    fun swarm_initializesInstancesOnFirstTick() {
        val state = BehaviorState.Swarm(instances = mutableListOf())
        val face = buildFace(boundingBox = Rect(100, 100, 300, 400))
        val anchor = PointF(200f, 250f)
        BugBehavior.Swarm.tick(
            state, face, anchor,
            previewWidth = 400f, previewHeight = 800f,
            tsNanos = 1_000_000L, dtMs = 16L,
        )
        assertEquals(
            "SWARM must initialize exactly SWARM_INSTANCE_COUNT_DEFAULT instances",
            BugBehavior.SWARM_INSTANCE_COUNT_DEFAULT, state.instances.size,
        )
    }

    // -------------------------------------------------------------------------
    // D-09: drift toward anchor
    // -------------------------------------------------------------------------

    /**
     * D-09: after one 1-second tick, each bug instance must have moved closer to the anchor.
     * Velocity direction = (anchor - position).normalize() * speed.
     */
    @Test
    fun swarm_instanceDriftsTowardAnchor() {
        val anchor = PointF(200f, 250f)
        // Place a single bug at bbox left-top edge, far from anchor
        val bugPosition = PointF(100f, 100f)
        val initialDistance = distance(anchor, bugPosition)
        val bug = BugInstance(position = bugPosition, velocity = PointF(0f, 0f), frameIndex = 0)
        val state = BehaviorState.Swarm(instances = mutableListOf(bug))
        val face = buildFace(boundingBox = Rect(50, 50, 350, 450))
        BugBehavior.Swarm.tick(
            state, face, anchor,
            previewWidth = 400f, previewHeight = 800f,
            tsNanos = 1_000_000L, dtMs = 1000L,
        )
        val finalDistance = distance(anchor, state.instances[0].position)
        assertTrue("bug must move closer to anchor after 1s tick",
            finalDistance < initialDistance)
    }

    // -------------------------------------------------------------------------
    // D-09: respawn at bbox edge when close to anchor
    // -------------------------------------------------------------------------

    /**
     * D-09: when a bug is within 0.2 * faceBoxWidth from the anchor, it must respawn
     * at a bbox-edge point farther from the anchor than the respawn threshold.
     */
    @Test
    fun swarm_respawnsAtBboxEdgeWhenCloseToAnchor() {
        val anchor = PointF(200f, 250f)
        val faceBoxWidth = 200f  // bbox.right(300) - bbox.left(100)
        val threshold = BugBehavior.SWARM_RESPAWN_FRACTION * faceBoxWidth  // = 40f
        // Place bug at anchor + 5px → distance = 5f < 40f → should respawn
        val bug = BugInstance(position = PointF(205f, 250f), velocity = PointF(0f, 0f), frameIndex = 0)
        val state = BehaviorState.Swarm(instances = mutableListOf(bug))
        val bbox = Rect(100, 100, 300, 400)
        val face = buildFace(boundingBox = bbox)
        BugBehavior.Swarm.tick(
            state, face, anchor,
            previewWidth = 400f, previewHeight = 800f,
            tsNanos = 1_000_000L, dtMs = 16L,
        )
        val respawnedPos = state.instances[0].position
        val distAfterRespawn = distance(anchor, respawnedPos)
        // After respawn, bug must be farther from anchor than threshold
        assertTrue("respawned bug must be farther from anchor than threshold",
            distAfterRespawn > threshold)
        // Bug must be on bbox perimeter (±1f tolerance)
        val onEdge =
            respawnedPos.x in (bbox.left.toFloat() - 1f)..(bbox.left.toFloat() + 1f) ||
            respawnedPos.x in (bbox.right.toFloat() - 1f)..(bbox.right.toFloat() + 1f) ||
            respawnedPos.y in (bbox.top.toFloat() - 1f)..(bbox.top.toFloat() + 1f) ||
            respawnedPos.y in (bbox.bottom.toFloat() - 1f)..(bbox.bottom.toFloat() + 1f)
        assertTrue("respawned bug must be on bbox perimeter", onEdge)
    }

    // -------------------------------------------------------------------------
    // D-09: instance count stable on subsequent ticks
    // -------------------------------------------------------------------------

    /**
     * D-09: instance count must remain == SWARM_INSTANCE_COUNT_DEFAULT across multiple ticks.
     * SWARM does not add/remove instances after initialization — only positions update.
     */
    @Test
    fun swarm_instanceCountStableAcrossMultipleTicks() {
        val state = BehaviorState.Swarm(instances = mutableListOf())
        val face = buildFace(boundingBox = Rect(100, 100, 300, 400))
        val anchor = PointF(200f, 250f)
        repeat(30) {
            BugBehavior.Swarm.tick(
                state, face, anchor,
                previewWidth = 400f, previewHeight = 800f,
                tsNanos = it * 16_000_000L, dtMs = 16L,
            )
            assertEquals(
                "instance count must stay at SWARM_INSTANCE_COUNT_DEFAULT across all ticks",
                BugBehavior.SWARM_INSTANCE_COUNT_DEFAULT, state.instances.size,
            )
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private fun buildFace(
        trackingId: Int = 1,
        boundingBox: Rect = Rect(100, 100, 300, 400),
        contours: Map<Int, List<PointF>> = emptyMap(),
    ): SmoothedFace = SmoothedFace(trackingId, boundingBox, contours, emptyMap())

    // Euclidean distance helper — replicated in each test file per plan's "duplicate" pattern
    @Suppress("SameParameterValue")
    private fun distance(a: PointF, b: PointF): Float {
        val dx = a.x - b.x
        val dy = a.y - b.y
        return kotlin.math.sqrt(dx * dx + dy * dy)
    }
}
