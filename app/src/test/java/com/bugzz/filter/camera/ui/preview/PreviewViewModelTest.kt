package com.bugzz.filter.camera.ui.preview

import org.junit.Ignore
import org.junit.Test

/**
 * RED scaffold per 06-VALIDATION Wave 0.
 *
 * Un-Ignored in **Plan 06-04** when PreviewViewModel lands.
 *
 * Coverage matrix (UX-04 + T-06-01 + T-06-03):
 *   - Photo MIME ("image/...") → uiState.isVideo == false (renders Image / Coil AsyncImage)
 *   - Video MIME ("video/mp4") → uiState.isVideo == true (renders ExoPlayer PlayerView)
 *   - deleteArtifact() invokes ContentResolver.delete(uri, ...) on Dispatchers.IO
 *   - deleteArtifact() returns false / does NOT crash when the delete call throws
 *     (defense against MediaStore.delete races + uri permission revocation T-06-01)
 *
 * Purpose: pre-creates the test path Plan 06-04's per-task verification command targets
 * (`*PreviewViewModelTest*`). Tests intentionally @Ignored. When Plan 06-04 lands the SUT,
 * the implementer will replace `markMissing()` with real construction:
 *   - mock<ContentResolver>() + mock<Uri>() (Phase 3 STATE #24 trick — no Robolectric needed)
 *   - StandardTestDispatcher() in @Before, resetMain in @After
 *   - SavedStateHandle with mime/uri pre-populated
 *
 * Pure JVM scaffold — no @RunWith, no Android type imports.
 */
class PreviewViewModelTest {

    /** Stub helper — replaced with real assertions in Plan 06-04. */
    private fun markMissing() {
        // Intentional no-op.
    }

    /**
     * UX-04 photo branch: when SavedStateHandle["mime"] starts with "image", uiState.isVideo=false.
     */
    @Test
    @Ignore("Plan 06-04 — un-ignore when PreviewViewModel lands")
    fun mimeImage_uiStateIsVideoFalse() {
        markMissing()
    }

    /**
     * UX-04 video branch: when SavedStateHandle["mime"] == "video/mp4", uiState.isVideo=true.
     */
    @Test
    @Ignore("Plan 06-04 — un-ignore when PreviewViewModel lands")
    fun mimeVideoMp4_uiStateIsVideoTrue() {
        markMissing()
    }

    /**
     * UX-04 delete contract: deleteArtifact() invokes ContentResolver.delete on the IO dispatcher
     * (NOT the main thread — disk IO blocking violation otherwise).
     */
    @Test
    @Ignore("Plan 06-04 — un-ignore when PreviewViewModel lands")
    fun deleteArtifact_invokesContentResolverDeleteOnIo() {
        markMissing()
    }

    /**
     * T-06-01 mitigation: deleteArtifact() must NOT crash if ContentResolver.delete throws
     * (e.g., URI permission revoked, file removed concurrently). Returns false / emits failure
     * event instead of propagating the exception.
     */
    @Test
    @Ignore("Plan 06-04 — un-ignore when PreviewViewModel lands")
    fun deleteArtifact_returnsFalseWhenDeleteThrows_noCrash() {
        markMissing()
    }
}
