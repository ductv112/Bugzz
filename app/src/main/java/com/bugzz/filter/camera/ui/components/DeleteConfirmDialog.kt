package com.bugzz.filter.camera.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Phase 6 Plan 06-06 — shared destructive-action confirmation dialog (UX-08, D-16).
 *
 * Replaces the inline AlertDialog that Plan 06-04 shipped in [com.bugzz.filter.camera.ui.preview.PreviewScreen].
 * Reused by Collection screen (future) and Settings clear-all (future).
 *
 * Layout per 06-UI-SPEC §7:
 *   - Title:   "Delete this artifact?" — explicit `TextStyle(16sp, FontWeight.Medium)` override of
 *              Material3 `titleMedium` SemiBold default. Phase 5 convention carried verbatim.
 *   - Body:    "This can't be undone." — `MaterialTheme.typography.bodyMedium` (14sp / Normal).
 *   - Buttons: Cancel = `confirmButton` slot (RIGHT, safe action, default `colorScheme.primary` tint).
 *              Delete = `dismissButton` slot (LEFT, destructive, `colorScheme.error` tint = #FFB00020).
 *              The Material3-naming-vs-visual-placement inversion is intentional and matches the
 *              Phase 5 Exit-during-record dialog (D-16, UI-SPEC §7 Note).
 *
 * Wiring contract:
 *   - `confirmButton` onClick → invokes [onDismiss] (Cancel = dismiss the dialog without acting)
 *   - `dismissButton` onClick → invokes [onConfirm] (Delete = perform the destructive action)
 *   - Outside-tap / hardware Back → invokes [onDismiss] via `onDismissRequest`
 *
 * Accessibility (UI-SPEC §7):
 *   - Cancel: `contentDescription = "Cancel deletion"`, `Role.Button`
 *   - Delete: `contentDescription = "Confirm delete"`, `Role.Button`
 *   - Material3 AlertDialog handles focus trap + title-then-body announcement order.
 *
 * Animation: Material3 default 200ms scale+fade enter/exit.
 *
 * Phase 6, Plan 06-06, UX-08, D-16, UI-SPEC §7.
 *
 * @param onDismiss Called when the user taps Cancel, taps outside the dialog, or presses Back.
 * @param onConfirm Called when the user taps Delete (the destructive action). The caller is
 *   responsible for closing the dialog (typically by toggling its own showDialog state) before
 *   or after launching the destructive coroutine.
 */
@Composable
fun DeleteConfirmDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Delete this artifact?",
                // UI-SPEC §3 + §Typography: 2-weight system — explicit Medium (500) override.
                style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Medium),
            )
        },
        text = {
            Text(
                text = "This can't be undone.",
                style = MaterialTheme.typography.bodyMedium,
            )
        },
        confirmButton = {
            // Cancel = confirmButton slot (RIGHT — safe action). Phase 5 convention.
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.semantics {
                    role = Role.Button
                    contentDescription = "Cancel deletion"
                },
            ) {
                Text(
                    text = "Cancel",
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        },
        dismissButton = {
            // Delete = dismissButton slot (LEFT — destructive, error tint).
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error,
                ),
                modifier = Modifier.semantics {
                    role = Role.Button
                    contentDescription = "Confirm delete"
                },
            ) {
                Text(
                    text = "Delete",
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        },
    )
}
