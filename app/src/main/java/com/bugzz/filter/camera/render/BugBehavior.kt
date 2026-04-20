package com.bugzz.filter.camera.render

import android.graphics.PointF
import android.graphics.RectF
import com.bugzz.filter.camera.detector.SmoothedFace
import kotlin.math.hypot
import kotlin.random.Random

/**
 * Per-variant tick loop for each behavior. Sealed — exactly 4 variants per D-04 / REN-02.
 *
 * Phase 4 (Plan 04-03) replaces all 3 Phase 3 TODO stubs with real impls:
 *   - D-08 CRAWL: FaceContour.FACE traversal + progress wrap
 *   - D-09 SWARM: 5-8 instance drift toward anchor + respawn at bbox edge
 *   - D-10 FALL:  gravity rain from y=0, despawn at previewHeight
 *
 * Tick signature changed from Phase 3: adds previewWidth/previewHeight/tsNanos params
 * (D-10 needs previewHeight for gravity scale; D-08 needs previewWidth for speed scale;
 *  D-10 needs tsNanos for spawn-interval tracking).
 */
sealed interface BugBehavior {

    fun tick(
        state: BehaviorState,
        face: SmoothedFace,
        anchor: PointF,
        previewWidth: Float,
        previewHeight: Float,
        tsNanos: Long,
        dtMs: Long,
    )

    /** STATIC — pin sprite to anchor. */
    object Static : BugBehavior {
        override fun tick(
            state: BehaviorState,
            face: SmoothedFace,
            anchor: PointF,
            previewWidth: Float,
            previewHeight: Float,
            tsNanos: Long,
            dtMs: Long,
        ) {
            val s = state as? BehaviorState.Static ?: return
            s.pos.set(anchor.x, anchor.y)
        }
    }

    /** CRAWL — traverse FaceContour.FACE perimeter (D-08). */
    object Crawl : BugBehavior {
        override fun tick(
            state: BehaviorState,
            face: SmoothedFace,
            anchor: PointF,
            previewWidth: Float,
            previewHeight: Float,
            tsNanos: Long,
            dtMs: Long,
        ) {
            val s = state as? BehaviorState.Crawl ?: return
            if (dtMs <= 0L) return
            val bbox = face.boundingBox
            val dtSec = dtMs / 1000f
            // D-08: ≈50% face-box width per second; normalize by preview width as fractional progress
            val delta = dtSec * CRAWL_SPEED_FACTOR_DEFAULT * bbox.width() / previewWidth.coerceAtLeast(1f)
            val signedDelta = if (s.direction == CrawlDirection.CW) delta else -delta
            var next = (s.progress + signedDelta) % 1f
            if (next < 0f) next += 1f
            s.progress = next
        }
    }

    /** SWARM — N instances drift toward anchor + respawn (D-09). */
    object Swarm : BugBehavior {
        override fun tick(
            state: BehaviorState,
            face: SmoothedFace,
            anchor: PointF,
            previewWidth: Float,
            previewHeight: Float,
            tsNanos: Long,
            dtMs: Long,
        ) {
            val s = state as? BehaviorState.Swarm ?: return
            val bbox = face.boundingBox
            val bboxF = RectF(bbox)
            val dtSec = dtMs / 1000f

            // On first tick: spawn all instances at random bbox-edge points
            if (s.instances.isEmpty()) {
                repeat(s.targetCount) {
                    s.instances.add(
                        BugInstance(
                            position = randomBboxEdgePoint(bboxF),
                            velocity = PointF(0f, 0f),
                            frameIndex = 0,
                        )
                    )
                }
            }

            val respawnDist = bbox.width() * SWARM_RESPAWN_FRACTION
            s.instances.forEach { inst ->
                val dx = anchor.x - inst.position.x
                val dy = anchor.y - inst.position.y
                val dist = hypot(dx.toDouble(), dy.toDouble()).toFloat()
                if (dist < respawnDist) {
                    // Respawn at random bbox-edge point
                    val r = randomBboxEdgePoint(bboxF)
                    inst.position.set(r.x, r.y)
                    inst.velocity.set(0f, 0f)
                } else if (dist > 0f) {
                    // Drift toward anchor
                    val speedFrac = SWARM_DRIFT_SPEED_MIN +
                        (SWARM_DRIFT_SPEED_MAX - SWARM_DRIFT_SPEED_MIN) * Random.nextFloat()
                    val spd = speedFrac * bbox.width()
                    inst.velocity.set(dx / dist * spd, dy / dist * spd)
                    inst.position.x += inst.velocity.x * dtSec
                    inst.position.y += inst.velocity.y * dtSec
                }
            }
        }
    }

    /** FALL — rain of bugs from y=0 down under gravity (D-10). */
    object Fall : BugBehavior {
        override fun tick(
            state: BehaviorState,
            face: SmoothedFace,
            anchor: PointF,
            previewWidth: Float,
            previewHeight: Float,
            tsNanos: Long,
            dtMs: Long,
        ) {
            val s = state as? BehaviorState.Fall ?: return
            val dtSec = dtMs / 1000f
            val gravityPxSec = s.gravityFactor * previewHeight

            // Spawn a new bug if interval has elapsed and cap not hit
            if (tsNanos >= s.nextSpawnNanos && s.instances.size < s.maxInstances) {
                val x = Random.nextFloat() * previewWidth
                s.instances.add(
                    FallingBug(
                        position = PointF(x, 0f),
                        velocity = PointF(0f, gravityPxSec),
                        spawnNanos = tsNanos,
                    )
                )
                val intervalRange = s.spawnIntervalMaxMs - s.spawnIntervalMinMs
                val intervalMs = s.spawnIntervalMinMs +
                    if (intervalRange > 0) Random.nextInt(intervalRange) else 0
                s.nextSpawnNanos = tsNanos + intervalMs * 1_000_000L
            }

            // Advance each bug's position and despawn those past previewHeight
            val iter = s.instances.iterator()
            while (iter.hasNext()) {
                val bug = iter.next()
                bug.position.y += bug.velocity.y * dtSec
                if (bug.position.y > previewHeight) iter.remove()
            }
        }
    }

    companion object {
        // Default tuning per D-08/D-09/D-10; Plan 04-04 parameterises via FilterDefinition.behaviorConfig.
        const val CRAWL_SPEED_FACTOR_DEFAULT = 0.5f

        const val SWARM_INSTANCE_COUNT_DEFAULT = 6
        const val SWARM_DRIFT_SPEED_MIN = 0.3f
        const val SWARM_DRIFT_SPEED_MAX = 0.8f
        const val SWARM_RESPAWN_FRACTION = 0.2f

        const val FALL_MAX_INSTANCES_DEFAULT = 8
        const val FALL_SPAWN_INTERVAL_MIN_MS = 200
        const val FALL_SPAWN_INTERVAL_MAX_MS = 401   // exclusive upper per Random.nextInt contract
        const val FALL_GRAVITY_FACTOR_DEFAULT = 0.5f

        /**
         * Linear vertex-count traversal along a closed-polygon contour (D-08).
         * progress in [0, 1); scaled = progress * n → (i, t) interpolates from contour[i] to
         * contour[(i+1)%n]. Throws [IllegalArgumentException] on empty contour — caller must gate.
         */
        fun crawlPosition(contour: List<PointF>, progress: Float): PointF {
            require(contour.isNotEmpty()) { "contour must not be empty" }
            val n = contour.size
            val p = progress.coerceIn(0f, 0.999999f)
            val scaled = p * n
            val i = scaled.toInt() % n
            val t = scaled - scaled.toInt()
            val a = contour[i]
            val b = contour[(i + 1) % n]
            return PointF(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t)
        }

        /** Random point on bbox edge — used by SWARM spawn/respawn. */
        fun randomBboxEdgePoint(bbox: RectF): PointF {
            // Pick an edge (0..3), then a point along it.
            return when (Random.nextInt(4)) {
                0 -> PointF(bbox.left + Random.nextFloat() * bbox.width(), bbox.top)      // top
                1 -> PointF(bbox.right, bbox.top + Random.nextFloat() * bbox.height())    // right
                2 -> PointF(bbox.left + Random.nextFloat() * bbox.width(), bbox.bottom)   // bottom
                else -> PointF(bbox.left, bbox.top + Random.nextFloat() * bbox.height())  // left
            }
        }
    }
}
