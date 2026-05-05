package com.bugzz.filter.camera.ui.components

import org.junit.Ignore
import org.junit.Test

/**
 * RED scaffold per 06-VALIDATION Wave 0.
 *
 * Un-Ignored in **Plan 06-06** when DeleteConfirmDialog lands.
 *
 * Coverage matrix (UX-08 — Cancel vs Confirm callback contract):
 *   - Tapping Confirm/Delete invokes the onConfirm lambda exactly once
 *   - Tapping Cancel/Dismiss invokes the onDismiss lambda exactly once and NOT onConfirm
 *
 * Pure JVM scaffold — tests the lambda wiring contract that Preview/CollectionViewModel uses
 * when constructing the dialog. No Compose UI test harness needed; the dialog will likely be
 * decomposed into a small state-machine wrapper or composable that exposes the two callbacks
 * cleanly. The implementer in Plan 06-06 chooses whether to write a Compose UI test
 * (createComposeRule) or to extract the state contract into a plain function.
 *
 * Tests intentionally @Ignored at this wave — the SUT does not exist yet.
 */
class DeleteConfirmDialogTest {

    /** Stub helper — replaced with real assertions in Plan 06-06. */
    private fun markMissing() {
        // Intentional no-op.
    }

    /**
     * UX-08 Confirm path: when the user taps the Delete button, onConfirm is invoked
     * exactly once. The caller (PreviewViewModel) reacts by invoking deleteArtifact.
     */
    @Test
    @Ignore("Plan 06-06 — un-ignore when DeleteConfirmDialog lands")
    fun onConfirmTap_invokesOnConfirmCallbackOnce() {
        markMissing()
    }

    /**
     * UX-08 Cancel path: tapping Cancel must invoke onDismiss exactly once and MUST NOT
     * invoke onConfirm. Mis-wiring this is the most common bug in confirmation dialogs.
     */
    @Test
    @Ignore("Plan 06-06 — un-ignore when DeleteConfirmDialog lands")
    fun onCancelTap_invokesOnDismissOnce_doesNotInvokeOnConfirm() {
        markMissing()
    }
}
