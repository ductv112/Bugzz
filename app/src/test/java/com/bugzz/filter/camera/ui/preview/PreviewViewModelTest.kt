package com.bugzz.filter.camera.ui.preview

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * PreviewViewModel unit tests — Plan 06-04 un-Ignore (was Wave 0 RED scaffold).
 *
 * Coverage matrix (UX-04 + T-06-01 mitigation):
 *   1. resolveMimeType_imageMime_returnsImageJpeg — ContentResolver returns "image/jpeg" → "image/jpeg"
 *   2. resolveMimeType_videoMime_returnsVideoMp4 — ContentResolver returns "video/mp4" → "video/mp4"
 *   3. resolveMimeType_nullMime_fallsBackToImageJpeg — ContentResolver returns null → "image/jpeg" safe default
 *   4. deleteArtifact_success_returnsTrue — ContentResolver.delete returns 1 → true
 *   5. deleteArtifact_throws_returnsFalseNoCrash — ContentResolver.delete throws SecurityException → false (no rethrow)
 *
 * Robolectric required for [Context]/[ContentResolver]/[Uri] type stubs (CollectionRepositoryTest
 * pattern, STATE #24). PreviewViewModel is constructed directly with a mocked Context, bypassing
 * Hilt — same shape as VideoRecorderTest.buildMockContext().
 *
 * @see PreviewViewModel
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PreviewViewModelTest {

    private fun buildVm(
        mimeType: String? = "image/jpeg",
        deleteCount: Int = 1,
        deleteThrows: Throwable? = null,
    ): Pair<PreviewViewModel, Uri> {
        val mockUri: Uri = mock(Uri::class.java)
        val mockResolver = mock(ContentResolver::class.java)
        whenever(mockResolver.getType(any())).thenReturn(mimeType)
        if (deleteThrows != null) {
            whenever(mockResolver.delete(any(), anyOrNull(), anyOrNull())).thenThrow(deleteThrows)
        } else {
            whenever(mockResolver.delete(any(), anyOrNull(), anyOrNull())).thenReturn(deleteCount)
        }
        val mockContext = mock(Context::class.java)
        whenever(mockContext.contentResolver).thenReturn(mockResolver)
        whenever(mockContext.applicationContext).thenReturn(mockContext)
        return PreviewViewModel(mockContext) to mockUri
    }

    /**
     * UX-04 photo branch: a JPEG MediaStore image returns its declared MIME unchanged. The
     * composable will see `mimeType.startsWith("image")` == true and route to `AsyncImage`.
     */
    @Test
    fun resolveMimeType_imageMime_returnsImageJpeg() = runTest {
        val (vm, uri) = buildVm(mimeType = "image/jpeg")
        assertEquals("image/jpeg", vm.resolveMimeType(uri))
    }

    /**
     * UX-04 video branch: an MP4 MediaStore video returns its declared MIME unchanged. The
     * composable will see `mimeType.startsWith("image")` == false and route to `VideoPreview`.
     */
    @Test
    fun resolveMimeType_videoMime_returnsVideoMp4() = runTest {
        val (vm, uri) = buildVm(mimeType = "video/mp4")
        assertEquals("video/mp4", vm.resolveMimeType(uri))
    }

    /**
     * Safe-default fallback: a null MIME (rare — e.g., MediaStore row inserted but type column
     * not yet populated) maps to "image/jpeg". The composable then renders the photo path
     * — the worst case is a video URI shown via `AsyncImage` which simply fails to decode and
     * surfaces Coil's error painter. No crash, no NPE.
     */
    @Test
    fun resolveMimeType_nullMime_fallsBackToImageJpeg() = runTest {
        val (vm, uri) = buildVm(mimeType = null)
        assertEquals("image/jpeg", vm.resolveMimeType(uri))
    }

    /**
     * UX-04 delete contract: ContentResolver.delete returns 1 (one row removed) → ViewModel
     * surfaces `true` and the composable invokes `onDeleted()` (popBackStack).
     */
    @Test
    fun deleteArtifact_success_returnsTrue() = runTest {
        val (vm, uri) = buildVm(deleteCount = 1)
        assertTrue(vm.deleteArtifact(uri))
    }

    /**
     * T-06-01 mitigation: a SecurityException from ContentResolver.delete (URI permission
     * revoked mid-session, file removed concurrently from outside the app, etc.) must NOT
     * crash. ViewModel returns `false` and the composable leaves the screen as-is.
     */
    @Test
    fun deleteArtifact_throws_returnsFalseNoCrash() = runTest {
        val (vm, uri) = buildVm(deleteThrows = SecurityException("permission revoked"))
        assertFalse(vm.deleteArtifact(uri))
    }
}
