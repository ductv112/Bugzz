package com.bugzz.filter.camera.ui.share

import android.content.Intent
import android.net.Uri
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Phase 6 Plan 06-06 — ShareIntentBuilder contract tests.
 *
 * Robolectric runner required because `Intent` + `Uri` are Android types (`Uri.parse` returns
 * null under plain JVM; `Intent.createChooser` is a static factory). Same `@Config(sdk = [34])`
 * pattern used by `VideoRecorderTest` (Phase 5).
 *
 * Coverage matrix (SHR-01 + SHR-02 + SHR-03 + T-06-01):
 *   - SHR-03: result `Intent.action == ACTION_CHOOSER` (createChooser wrap)
 *   - SHR-02: inner `Intent.type == passed mimeType` (image/jpeg or video/mp4)
 *   - SHR-01: inner `Intent.EXTRA_STREAM == passed Uri`
 *   - T-06-01: inner `Intent.flags & FLAG_GRANT_READ_URI_PERMISSION != 0`
 *
 * Inner-intent extraction: `result.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)` per the
 * Android `Intent.createChooser` documented behavior.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ShareIntentBuilderTest {

    private val photoUri: Uri = Uri.parse("content://media/external/images/media/12345")
    private val videoUri: Uri = Uri.parse("content://media/external/video/media/67890")

    /**
     * SHR-03: buildShareIntent must wrap the inner ACTION_SEND intent in `Intent.createChooser`,
     * resulting in `action == ACTION_CHOOSER`. Without the chooser wrap, Android could silently
     * reuse the last-used target and skip the picker UI.
     */
    @Test
    fun buildShareIntent_resultActionIsActionChooser() {
        val result = buildShareIntent(photoUri, "image/jpeg")
        assertEquals("Outer intent action must be ACTION_CHOOSER", Intent.ACTION_CHOOSER, result.action)
    }

    /**
     * SHR-02 (image branch): the inner intent (extracted via `EXTRA_INTENT`) must have
     * `type == "image/jpeg"` when buildShareIntent is called for a photo artifact.
     */
    @Test
    fun innerIntentType_matchesImageMimeType() {
        val result = buildShareIntent(photoUri, "image/jpeg")
        @Suppress("DEPRECATION")
        val inner = result.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)
        assertNotNull("Inner ACTION_SEND intent must be present in EXTRA_INTENT", inner)
        assertEquals("Inner intent action must be ACTION_SEND", Intent.ACTION_SEND, inner!!.action)
        assertEquals("Inner intent type must match passed mimeType (image/jpeg)", "image/jpeg", inner.type)
    }

    /**
     * SHR-02 (video branch): polymorphism check — buildShareIntent must respect the caller's
     * mimeType for the video case as well as the image case.
     */
    @Test
    fun innerIntentType_matchesVideoMimeType() {
        val result = buildShareIntent(videoUri, "video/mp4")
        @Suppress("DEPRECATION")
        val inner = result.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)
        assertNotNull("Inner ACTION_SEND intent must be present in EXTRA_INTENT", inner)
        assertEquals("Inner intent type must match passed mimeType (video/mp4)", "video/mp4", inner!!.type)
    }

    /**
     * SHR-01: the inner intent must carry `EXTRA_STREAM` equal to the passed content:// URI.
     * Without it, the receiving app gets an empty share.
     */
    @Test
    fun innerIntentExtraStream_equalsPassedUri() {
        val result = buildShareIntent(photoUri, "image/jpeg")
        @Suppress("DEPRECATION")
        val inner = result.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)
        assertNotNull(inner)
        @Suppress("DEPRECATION")
        val extraStream = inner!!.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
        assertEquals("Inner EXTRA_STREAM must equal passed Uri", photoUri, extraStream)
    }

    /**
     * T-06-01 mitigation: `FLAG_GRANT_READ_URI_PERMISSION` must be set on the inner intent so
     * the share-sheet target gets temporary read access to the MediaStore URI. Without this
     * flag, e.g. WhatsApp share-preview throws SecurityException reading the bytes.
     */
    @Test
    fun innerIntent_hasFlagGrantReadUriPermissionSet() {
        val result = buildShareIntent(photoUri, "image/jpeg")
        @Suppress("DEPRECATION")
        val inner = result.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)
        assertNotNull(inner)
        val grantBit = inner!!.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION
        assertTrue(
            "T-06-01: FLAG_GRANT_READ_URI_PERMISSION must be set on inner ACTION_SEND intent",
            grantBit != 0,
        )
    }
}
