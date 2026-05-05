package com.bugzz.filter.camera.data

import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * RED scaffold per 06-VALIDATION Wave 0.
 *
 * Un-Ignored in **Plan 06-05** when CollectionRepository lands.
 *
 * Coverage matrix (UX-05 + UX-07 + T-06-02):
 *   - ContentResolver.query returns null → repository returns empty list (no NPE)
 *   - Cursor row → MediaItem mapping preserves mimeType + uri + display name
 *   - rows whose mimeType starts "image/" produce Images.Media URIs (T-06-02)
 *   - rows whose mimeType starts "video/" produce Video.Media URIs (T-06-02)
 *   - Rows outside DCIM/Bugzz/ excluded via RELATIVE_PATH selection clause
 *
 * Robolectric runner is required because the eventual SUT will touch Context, ContentResolver,
 * and MediaStore.* URI constants — same pattern as
 * [com.bugzz.filter.camera.recording.VideoRecorderTest] which uses
 * @RunWith(RobolectricTestRunner::class) @Config(sdk = [34]) for the same reason.
 *
 * Tests are intentionally @Ignored at this wave — CollectionRepository SUT does not exist yet,
 * so we do not import it. When Plan 06-05 lands, the implementer will:
 *   - Add real imports + a buildMockContext() helper (mirroring VideoRecorderTest)
 *   - Stub ContentResolver.query() with MatrixCursor instances per scenario
 *   - Replace markMissing() with assertEquals/assertTrue against repository.queryAll()
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CollectionRepositoryTest {

    /** Stub helper — replaced with real assertions in Plan 06-05. */
    private fun markMissing() {
        // Intentional no-op.
    }

    /**
     * UX-07 empty-state seed: when ContentResolver.query returns null (rare but legal —
     * e.g., user revoked storage access mid-session), repository must return emptyList()
     * rather than crashing or returning null itself.
     */
    @Test
    @Ignore("Plan 06-05 — un-ignore when CollectionRepository lands")
    fun query_returnsNull_repositoryReturnsEmptyList() {
        markMissing()
    }

    /**
     * UX-05 happy path: a Cursor row with (id, display_name, mime_type, date_added) maps
     * to a MediaItem with the same fields and a non-null content URI.
     */
    @Test
    @Ignore("Plan 06-05 — un-ignore when CollectionRepository lands")
    fun cursorRow_mapsToMediaItem_withCorrectMimeType() {
        markMissing()
    }

    /**
     * T-06-02 namespace correctness: rows with mime_type starting "image/" must produce
     * URIs in the MediaStore.Images.Media namespace. Rows with "video/" must produce
     * URIs in the MediaStore.Video.Media namespace. Mismatch → ContentResolver.openInputStream
     * fails for the consumer (Preview screen, share intent).
     */
    @Test
    @Ignore("Plan 06-05 — un-ignore when CollectionRepository lands")
    fun mimeBranching_imageRowsToImagesMediaUri_videoRowsToVideoMediaUri() {
        markMissing()
    }

    /**
     * UX-05 scope: query selection clause uses RELATIVE_PATH = "DCIM/Bugzz/" so files saved
     * elsewhere on the device do not appear in the collection grid. Tested by stubbing the
     * Cursor with both in-scope and out-of-scope rows and asserting only in-scope items
     * surface in the result list (the production code is expected to add the WHERE clause —
     * out-of-scope rows simply will not be present in the cursor it receives).
     */
    @Test
    @Ignore("Plan 06-05 — un-ignore when CollectionRepository lands")
    fun selection_excludesRowsOutsideDcimBugzzRelativePath() {
        markMissing()
    }
}
