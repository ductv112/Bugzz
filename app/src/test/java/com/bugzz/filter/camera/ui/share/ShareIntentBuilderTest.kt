package com.bugzz.filter.camera.ui.share

import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * RED scaffold per 06-VALIDATION Wave 0.
 *
 * Un-Ignored in **Plan 06-06** when ShareIntentBuilder lands.
 *
 * Coverage matrix (SHR-01 + SHR-02 + SHR-03 + T-06-01):
 *   - SHR-03: result Intent.action == ACTION_CHOOSER (Intent.createChooser wrap)
 *   - SHR-02: inner Intent.type == passed mimeType (image/jpeg or video/mp4)
 *   - SHR-01: inner Intent EXTRA_STREAM == passed Uri
 *   - T-06-01: inner Intent has FLAG_GRANT_READ_URI_PERMISSION set so the receiving app can
 *              read the MediaStore URI (without it, share-target preview crashes)
 *
 * Robolectric runner required because Intent + Uri are Android types (Uri.parse returns null
 * under plain JVM; Intent.createChooser is a static factory). Same @Config(sdk = [34]) pattern
 * used by [com.bugzz.filter.camera.recording.VideoRecorderTest].
 *
 * Tests intentionally @Ignored — ShareIntentBuilder.buildShareIntent() does not exist yet.
 * When Plan 06-06 lands, the implementer will:
 *   - Construct a real Uri via Uri.parse("content://...")
 *   - Call ShareIntentBuilder.buildShareIntent(uri, mimeType)
 *   - Assert intent.action, intent.getParcelableExtra(EXTRA_INTENT)?.type, etc.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ShareIntentBuilderTest {

    /** Stub helper — replaced with real assertions in Plan 06-06. */
    private fun markMissing() {
        // Intentional no-op.
    }

    /**
     * SHR-03: buildShareIntent must wrap the inner ACTION_SEND intent in Intent.createChooser,
     * resulting in action == ACTION_CHOOSER. Without the chooser wrap, Android would not show
     * the picker UI on first share (it would auto-pick the last-used target).
     */
    @Test
    @Ignore("Plan 06-06 — un-ignore when ShareIntentBuilder lands")
    fun buildShareIntent_resultActionIsActionChooser() {
        markMissing()
    }

    /**
     * SHR-02: the inner intent (extracted via EXTRA_INTENT) must have type == the mimeType
     * passed to buildShareIntent — image/jpeg for photos, video/mp4 for videos. The receiving
     * app uses this to disambiguate handler logic.
     */
    @Test
    @Ignore("Plan 06-06 — un-ignore when ShareIntentBuilder lands")
    fun innerIntentType_matchesPassedMimeType() {
        markMissing()
    }

    /**
     * SHR-01: the inner intent must carry EXTRA_STREAM equal to the passed content:// Uri.
     * This is the bytes-to-share parcel — without it, the receiving app gets an empty share.
     */
    @Test
    @Ignore("Plan 06-06 — un-ignore when ShareIntentBuilder lands")
    fun innerIntentExtraStream_equalsPassedUri() {
        markMissing()
    }

    /**
     * T-06-01 mitigation: FLAG_GRANT_READ_URI_PERMISSION must be set on the inner intent so
     * the share-sheet target gets temporary read access to the MediaStore URI. Without this
     * flag, e.g. WhatsApp share-preview throws SecurityException reading the bytes.
     */
    @Test
    @Ignore("Plan 06-06 — un-ignore when ShareIntentBuilder lands")
    fun innerIntent_hasFlagGrantReadUriPermissionSet() {
        markMissing()
    }
}
