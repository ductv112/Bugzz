package com.bugzz.filter.camera.ui.collection

import android.content.Context
import android.net.Uri
import com.bugzz.filter.camera.data.CollectionRepository
import com.bugzz.filter.camera.data.MediaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock

/**
 * Unit tests for [CollectionViewModel] (UX-05 + UX-07).
 *
 * Plan 06-05 un-Ignore — replaces the Wave 0 RED scaffold (`markMissing()`) with concrete
 * `uiState.isEmpty` assertions against a mocked [CollectionRepository].
 *
 * Coverage matrix:
 *   1. emptyList_setsIsEmptyTrue       — repository emits emptyList → isEmpty=true, items=[]
 *   2. nonEmptyList_setsIsEmptyFalse   — repository emits 2 items → isEmpty=false, items.size=2
 *
 * Pure JVM (no @RunWith) — VM constructed directly with `mock(Context::class)` +
 * `mock<CollectionRepository>()`. The mocked repository returns a `flowOf(...)` so emission is
 * synchronous on the StandardTestDispatcher.
 *
 * Note on thumbnail extraction in `init`:
 *   The VM init eagerly launches MediaMetadataRetriever child coroutines for each video item.
 *   On pure JVM these throw inside `setDataSource(context, uri)` (no Android runtime), but
 *   [CollectionViewModel.extractAndCacheThumbnail] catches `Exception` so the test does not
 *   fail. The non-empty test uses `mock<Uri>()` for both items — see Phase 3 STATE #24 pattern
 *   (`mock<Uri>()` is non-null where `Uri.parse` returns null on JVM).
 *
 * @see CollectionViewModel
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CollectionViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /**
     * UX-07: when repository.loadMediaItems() emits emptyList, uiState.isEmpty must be true. The
     * CollectionScreen reads this flag to render EmptyStateColumn instead of the grid.
     */
    @Test
    fun emptyList_setsIsEmptyTrue() = runTest(testDispatcher) {
        val mockRepo: CollectionRepository = mock {
            on { loadMediaItems() } doReturnFlowOf emptyList()
        }
        val mockContext = mock<Context>()
        val vm = CollectionViewModel(mockContext, mockRepo)

        advanceUntilIdle()

        val state = vm.uiState.value
        assertTrue("isEmpty must be true on empty emission", state.isEmpty)
        assertEquals(0, state.items.size)
        assertFalse("isLoading must be false after emission", state.isLoading)
    }

    /**
     * UX-05: when repository.loadMediaItems() emits a non-empty list, uiState.isEmpty must be
     * false and uiState.items must equal the emitted list (no filtering / dropping at VM layer).
     */
    @Test
    fun nonEmptyList_setsIsEmptyFalse() = runTest(testDispatcher) {
        val itemA = MediaItem(
            uri = mock<Uri>(),
            mimeType = "image/jpeg",
            displayName = "a.jpg",
            dateModified = 0L,
        )
        val itemB = MediaItem(
            uri = mock<Uri>(),
            mimeType = "video/mp4",
            displayName = "b.mp4",
            dateModified = 0L,
        )
        val mockRepo: CollectionRepository = mock {
            on { loadMediaItems() } doReturnFlowOf listOf(itemA, itemB)
        }
        val mockContext = mock<Context>()
        val vm = CollectionViewModel(mockContext, mockRepo)

        advanceUntilIdle()

        val state = vm.uiState.value
        assertFalse("isEmpty must be false on non-empty emission", state.isEmpty)
        assertEquals(2, state.items.size)
        assertEquals("a.jpg", state.items[0].displayName)
        assertEquals("b.mp4", state.items[1].displayName)
    }

    // --- Helpers ---

    /** Tiny infix helper to make `on { x } doReturnFlowOf list` read naturally. */
    private infix fun <T : Any> org.mockito.stubbing.OngoingStubbing<kotlinx.coroutines.flow.Flow<T>>.doReturnFlowOf(
        value: T,
    ) {
        thenReturn(flowOf(value))
    }
}
