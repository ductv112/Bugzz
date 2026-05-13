package com.bugzz.filter.camera.data

import org.junit.Assert.fail
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Nyquist Wave 0 RED scaffolds — Plan 07-04 lands CollectionRepository.callbackFlow rewrite + un-Ignores.
 *
 * Pins D-20b (Phase 6 polish item) — ContentObserver-backed live MediaStore refresh:
 *  - Register on MediaStore.Files.getContentUri("external") with notifyForDescendants=true
 *    (single observer covers Images.Media + Video.Media per RESEARCH Pattern 3 + Pitfall 5).
 *  - Emit initial snapshot on flow collection.
 *  - Re-emit when observer.onChange fires (simulate via ShadowContentResolver.notifyChange).
 *  - Unregister on flow cancellation (awaitClose lambda fires).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CollectionRepositoryContentObserverTest {

    @Test @Ignore("Plan 07-04 — CollectionRepository.callbackFlow rewrite pending")
    fun registers_on_files_uri_with_notifyForDescendants_true() {
        fail("Plan 07-04 implements ContentObserver register — Wave 0 RED")
    }

    @Test @Ignore("Plan 07-04 — CollectionRepository.callbackFlow rewrite pending")
    fun emits_initial_snapshot_on_subscription() {
        fail("Plan 07-04 implements callbackFlow initial emit — Wave 0 RED")
    }

    @Test @Ignore("Plan 07-04 — CollectionRepository.callbackFlow rewrite pending")
    fun reemits_when_observer_onChange_fires() {
        fail("Plan 07-04 implements onChange → re-query — Wave 0 RED")
    }

    @Test @Ignore("Plan 07-04 — CollectionRepository.callbackFlow rewrite pending")
    fun unregisters_observer_when_flow_collection_cancelled() {
        fail("Plan 07-04 implements awaitClose unregister — Wave 0 RED")
    }
}
