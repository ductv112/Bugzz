package com.bugzz.filter.camera.ui.collection

import org.junit.Ignore
import org.junit.Test

/**
 * RED scaffold per 06-VALIDATION Wave 0.
 *
 * Un-Ignored in **Plan 06-05** when CollectionViewModel lands.
 *
 * Coverage matrix (UX-07):
 *   - Repository returns empty MediaItem list → uiState.isEmpty == true (drives EmptyStateColumn)
 *   - Repository returns non-empty list → uiState.isEmpty == false + items match the input list
 *
 * Pure JVM (no @RunWith) — VM constructed with a mock CollectionRepository, no Android types.
 *
 * Tests intentionally @Ignored at this wave — CollectionViewModel SUT does not exist yet, so we
 * do not import it. When Plan 06-05 lands, the implementer will:
 *   - mock<CollectionRepository>() and stub `queryAll()` (or whatever method shape lands)
 *   - StandardTestDispatcher in @Before, resetMain in @After
 *   - vm.uiState.first() / collectAsStateInTest assertion against `isEmpty` and `items`
 */
class CollectionViewModelTest {

    /** Stub helper — replaced with real assertions in Plan 06-05. */
    private fun markMissing() {
        // Intentional no-op.
    }

    /**
     * UX-07: when CollectionRepository.queryAll() emits empty list, uiState.isEmpty must be true.
     * The CollectionScreen reads this flag to render EmptyStateColumn instead of the grid.
     */
    @Test
    @Ignore("Plan 06-05 — un-ignore when CollectionViewModel lands")
    fun emptyMediaItemList_uiStateIsEmptyTrue() {
        markMissing()
    }

    /**
     * UX-05: when CollectionRepository.queryAll() emits a non-empty list, uiState.isEmpty must be
     * false and uiState.items must equal the emitted list (no filtering / dropping at VM layer).
     */
    @Test
    @Ignore("Plan 06-05 — un-ignore when CollectionViewModel lands")
    fun nonEmptyMediaItemList_uiStateIsEmptyFalse_itemsMatch() {
        markMissing()
    }
}
