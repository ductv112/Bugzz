package com.bugzz.filter.camera.data

import android.content.Context
import android.os.Looper
import android.provider.MediaStore
import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * Plan 07-04 D-20b — ContentObserver-backed live MediaStore refresh for [CollectionRepository].
 *
 * Pins:
 *  - Register on MediaStore.Files.getContentUri("external") with notifyForDescendants=true
 *    (single observer covers Images.Media + Video.Media per RESEARCH Pattern 3 + Pitfall 5).
 *  - Emit initial snapshot on flow collection.
 *  - Re-emit when observer.onChange fires (simulate via ShadowContentResolver.notifyChange).
 *  - Unregister on flow cancellation (awaitClose lambda fires) — T-07-03 mitigation.
 *
 * Robolectric `ShadowContentResolver` records register/unregister calls and routes
 * `notifyChange` invocations to active observers, letting us drive the flow without a real
 * MediaStore backend. Pattern aligns with [CollectionRepositoryTest] @Config(sdk = [34]).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CollectionRepositoryContentObserverTest {

    private val context: Context get() = RuntimeEnvironment.getApplication()
    private val filesUri = MediaStore.Files.getContentUri("external")

    @Test
    fun registers_on_files_uri_with_notifyForDescendants_true() = runTest {
        val repo = CollectionRepository(context)
        repo.loadMediaItems().test {
            awaitItem()  // wait for initial snapshot to land — observer is now registered
            val shadow = shadowOf(context.contentResolver)
            val registered = shadow.getContentObservers(filesUri)
            assertTrue(
                "Observer must be registered on MediaStore.Files URI (D-20b / T-07-11)",
                registered.isNotEmpty(),
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun emits_initial_snapshot_on_subscription() = runTest {
        val repo = CollectionRepository(context)
        repo.loadMediaItems().test {
            val initial = awaitItem()
            assertEquals(
                "Initial snapshot empty (no DCIM/Bugzz rows in test resolver)",
                emptyList<MediaItem>(),
                initial,
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun reemits_when_observer_onChange_fires() = runTest {
        val repo = CollectionRepository(context)
        repo.loadMediaItems().test {
            awaitItem()  // initial snapshot
            // Fire onChange manually — ShadowContentResolver routes notifyChange to observers
            // registered with notifyForDescendants=true on the same URI. The observer is
            // constructed with `Handler(Looper.getMainLooper())`, so onChange is posted to the
            // main looper; Robolectric runs the main looper in PAUSED mode by default — we
            // must idle it so the queued Handler.post fires inside this test.
            context.contentResolver.notifyChange(filesUri, null)
            shadowOf(Looper.getMainLooper()).idle()
            val second = awaitItem()
            // Even if list is still empty, we received a SECOND emission — the live-refresh
            // contract is the point (D-20b).
            assertEquals(emptyList<MediaItem>(), second)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun unregisters_observer_when_flow_collection_cancelled() = runTest {
        val repo = CollectionRepository(context)
        repo.loadMediaItems().test {
            awaitItem()  // initial — observer registered
            cancel()  // explicit cancel — should trigger awaitClose lambda
        }
        // After cancel, the shadow should show zero registered observers on filesUri.
        // T-07-03 mitigation — no listener leak through ViewModel rebind cycles.
        val shadow = shadowOf(context.contentResolver)
        val remaining = shadow.getContentObservers(filesUri)
        assertTrue(
            "Observer must be unregistered after flow cancellation (T-07-03)",
            remaining.isEmpty(),
        )
    }
}
