package com.bugzz.filter.camera.data

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.provider.MediaStore
import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for [CollectionRepository] (UX-05 + D-12 + T-06-02).
 *
 * Plan 06-05 un-Ignore — the four cases scaffolded in Plan 06 Wave 0 now have a concrete SUT.
 *
 * Coverage matrix:
 *   1. emptyCursor_emitsEmptyList         — UX-05 happy path: no rows → empty Flow emission
 *   2. imageRow_constructsImagesMediaUri  — D-12 / T-06-02: image/jpeg row → Images.Media URI
 *   3. videoRow_constructsVideoMediaUri   — D-12 / T-06-02: video/mp4 row  → Video.Media URI
 *   4. selectionArgsBindRelativePath      — T-06-02: selectionArgs[0] starts with "DCIM/Bugzz/"
 *      (parameter binding, NOT string concat — SQLi prevented by ContentResolver implementation)
 *
 * Robolectric runner is required because the SUT touches `MediaStore.*` URI constants and
 * `ContentResolver.query` with a real `MatrixCursor`. Pattern mirrors VideoRecorderTest's
 * `buildMockContext()` (STATE #14 / Phase 5 #24).
 *
 * @see CollectionRepository
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CollectionRepositoryTest {

    /** Builds a Context whose `contentResolver` returns the supplied [cursor] for any query. */
    private fun buildContextWithCursor(cursor: Cursor?): Pair<Context, ContentResolver> {
        val mockResolver = Mockito.mock(ContentResolver::class.java)
        whenever(
            mockResolver.query(
                any<Uri>(),
                anyOrNull(),
                anyOrNull(),
                anyOrNull(),
                anyOrNull(),
            )
        ).thenReturn(cursor)
        val mockContext = Mockito.mock(Context::class.java)
        whenever(mockContext.contentResolver).thenReturn(mockResolver)
        whenever(mockContext.applicationContext).thenReturn(mockContext)
        return mockContext to mockResolver
    }

    /** Builds a single-row MatrixCursor with the supplied id + mime. */
    private fun buildSingleRowCursor(id: Long, mime: String, name: String = "row.$id"): MatrixCursor {
        val cursor = MatrixCursor(
            arrayOf(
                MediaStore.MediaColumns._ID,
                MediaStore.MediaColumns.DISPLAY_NAME,
                MediaStore.MediaColumns.MIME_TYPE,
                MediaStore.MediaColumns.DATE_MODIFIED,
            )
        )
        cursor.addRow(arrayOf<Any>(id, name, mime, 1_700_000_000L))
        return cursor
    }

    /**
     * UX-05 empty seed: an empty cursor (no rows in DCIM/Bugzz/) emits an empty list — drives
     * EmptyStateColumn rendering downstream in CollectionScreen.
     */
    @Test
    fun emptyCursor_emitsEmptyList() = runTest {
        val emptyCursor = MatrixCursor(
            arrayOf(
                MediaStore.MediaColumns._ID,
                MediaStore.MediaColumns.DISPLAY_NAME,
                MediaStore.MediaColumns.MIME_TYPE,
                MediaStore.MediaColumns.DATE_MODIFIED,
            )
        )
        val (context, _) = buildContextWithCursor(emptyCursor)
        val repository = CollectionRepository(context)

        repository.loadMediaItems().test {
            assertEquals(emptyList<MediaItem>(), awaitItem())
            awaitComplete()
        }
    }

    /**
     * D-12 / T-06-02 image-namespace correctness: a row with `mime=image/jpeg` produces a URI
     * in the `MediaStore.Images.Media` namespace (not Files). Sharing apps and AsyncImage
     * decoders rely on this — see RESEARCH §Critical Note.
     */
    @Test
    fun imageRow_constructsImagesMediaUri() = runTest {
        val cursor = buildSingleRowCursor(id = 1L, mime = "image/jpeg")
        val (context, _) = buildContextWithCursor(cursor)
        val repository = CollectionRepository(context)

        repository.loadMediaItems().test {
            val list = awaitItem()
            assertEquals(1, list.size)
            val uriString = list[0].uri.toString()
            assertTrue(
                "Image row must produce Images.Media URI, got: $uriString",
                uriString.contains("images/media/1"),
            )
            assertEquals("image/jpeg", list[0].mimeType)
            awaitComplete()
        }
    }

    /**
     * D-12 / T-06-02 video-namespace correctness: a row with `mime=video/mp4` produces a URI
     * in the `MediaStore.Video.Media` namespace. ExoPlayer's MediaItem requires this for
     * proper Surface attachment in VideoPreview.
     */
    @Test
    fun videoRow_constructsVideoMediaUri() = runTest {
        val cursor = buildSingleRowCursor(id = 2L, mime = "video/mp4")
        val (context, _) = buildContextWithCursor(cursor)
        val repository = CollectionRepository(context)

        repository.loadMediaItems().test {
            val list = awaitItem()
            assertEquals(1, list.size)
            val uriString = list[0].uri.toString()
            assertTrue(
                "Video row must produce Video.Media URI, got: $uriString",
                uriString.contains("video/media/2"),
            )
            assertEquals("video/mp4", list[0].mimeType)
            awaitComplete()
        }
    }

    /**
     * T-06-02 parameter-binding mitigation: the repository passes the relative-path filter as
     * a positional `?` placeholder + selectionArgs entry — never via string concat. Captures
     * the actual args array and asserts the first element binds to `DCIM/Bugzz/%`.
     */
    @Test
    fun selectionArgsBindRelativePath() = runTest {
        val emptyCursor = MatrixCursor(
            arrayOf(
                MediaStore.MediaColumns._ID,
                MediaStore.MediaColumns.DISPLAY_NAME,
                MediaStore.MediaColumns.MIME_TYPE,
                MediaStore.MediaColumns.DATE_MODIFIED,
            )
        )
        val (context, mockResolver) = buildContextWithCursor(emptyCursor)
        val repository = CollectionRepository(context)

        // Trigger the query — emission isn't necessary for arg capture; it's enough that
        // resolver.query() ran.
        repository.loadMediaItems().test {
            awaitItem()
            awaitComplete()
        }

        val argsCaptor: ArgumentCaptor<Array<String>> =
            ArgumentCaptor.forClass(Array<String>::class.java)
        Mockito.verify(mockResolver).query(
            any(),
            anyOrNull(),
            anyOrNull(),
            argsCaptor.capture(),
            anyOrNull(),
        )
        val captured = argsCaptor.value
        assertTrue(
            "selectionArgs[0] must bind to DCIM/Bugzz/% — got: ${captured[0]}",
            captured[0] == "DCIM/Bugzz/%",
        )
    }
}
