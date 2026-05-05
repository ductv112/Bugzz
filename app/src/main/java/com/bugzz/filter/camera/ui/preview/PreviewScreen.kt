package com.bugzz.filter.camera.ui.preview

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import kotlinx.coroutines.launch

/**
 * Phase 6 PreviewScreen — full-screen photo / video preview with 4-action bottom bar.
 *
 * Layout per 06-UI-SPEC §5 (z-order, bottom → top):
 *   1. Black background (Box fillMaxSize, Color.Black)
 *   2. Media content — `AsyncImage(ContentScale.Fit)` for photo OR `VideoPreview` for video.
 *      MIME branch decided by [PreviewViewModel.resolveMimeType] result (loaded in
 *      `LaunchedEffect(uri)`).
 *   3. Bottom action bar — `Surface(color = Color(0xFF1E1E1E), height = 80.dp)` aligned
 *      `BottomCenter`, full-width. Inside: `Row(Arrangement.SpaceEvenly)` with 4
 *      `PreviewAction` items.
 *
 * Actions (UI-SPEC §5):
 *   - **Done** (Check icon)  → [onDone]                — file remains saved; pop back
 *   - **Share** (Share icon) → [onShareNotImplemented] — Plan 06-06 wires real Intent.ACTION_SEND
 *   - **Delete** (Delete icon) → inline AlertDialog → [PreviewViewModel.deleteArtifact] →
 *     [onDeleted] on success
 *   - **Retake** (Refresh icon) → [onRetake] — file remains saved; pop back to capture screen
 *
 * Plan 06-04 ships Share as a Toast placeholder via [onShareNotImplemented]. Plan 06-06 will
 * replace the Toast with `Intent.ACTION_SEND` + `FileProvider`-style URI grant (T-06-01).
 *
 * Plan 06-04 ships Delete with an INLINE Material3 AlertDialog. Plan 06-06 will refactor the
 * dialog out to `ui/components/DeleteConfirmDialog.kt` shared composable for reuse across
 * Collection screen and future Settings clear-all.
 *
 * Phase 6, Plan 06-04, UX-04, D-08, D-09, D-10. T-06-03 mitigation delegated to [VideoPreview].
 *
 * @param uri Captured artifact URI (content://media/...). Always non-null per BugzzApp nav arg.
 * @param onDone "Done" button action (typically `navController.popBackStack()`).
 * @param onRetake "Retake" button action (typically `navController.popBackStack()`).
 * @param onDeleted Called when delete succeeds (typically `navController.popBackStack()`).
 * @param onShareNotImplemented Called on Share button — Plan 06-04 caller shows a Toast.
 *   Plan 06-06 will swap this for the real share Intent.
 */
@Composable
fun PreviewScreen(
    uri: Uri,
    onDone: () -> Unit,
    onRetake: () -> Unit,
    onDeleted: () -> Unit,
    onShareNotImplemented: () -> Unit,
    viewModel: PreviewViewModel = hiltViewModel(),
) {
    var mimeType by remember { mutableStateOf<String?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(uri) {
        mimeType = viewModel.resolveMimeType(uri)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        // ---- Media content (z-layer 2) — branch on MIME once resolved ----
        when {
            mimeType == null -> {
                // Transient pre-resolution: black background only. MIME resolves on IO in
                // ~ms; no spinner needed.
            }
            mimeType?.startsWith("image") == true -> {
                AsyncImage(
                    model = uri,
                    contentDescription = "Captured photo",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            else -> {
                // video/* branch — also handles application/octet-stream-style edge cases
                // by routing to ExoPlayer, which gracefully fails for unknown formats.
                VideoPreview(
                    uri = uri,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

        // ---- Bottom action bar (z-layer 3) — 80dp Surface(#1E1E1E), full-width ----
        Surface(
            color = Color(0xFF1E1E1E),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(80.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxSize(),
            ) {
                PreviewAction(
                    icon = Icons.Default.Check,
                    label = "Done",
                    onClick = onDone,
                )
                PreviewAction(
                    icon = Icons.Default.Share,
                    label = "Share",
                    // Plan 06-06 will replace this with a real Intent.ACTION_SEND.
                    onClick = onShareNotImplemented,
                )
                PreviewAction(
                    icon = Icons.Default.Delete,
                    label = "Delete",
                    onClick = { showDeleteDialog = true },
                )
                PreviewAction(
                    icon = Icons.Default.Refresh,
                    label = "Retake",
                    onClick = onRetake,
                )
            }
        }
    }

    // Plan 06-06 will refactor to ui/components/DeleteConfirmDialog.kt shared composable.
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = {
                Text(
                    text = "Delete this artifact?",
                    // UI-SPEC §3 + §Typography: 2-weight system — explicit Medium (500).
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
                // "Cancel" on right (Material3 confirmButton slot) — safe action.
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            },
            dismissButton = {
                // "Delete" on left (Material3 dismissButton slot) — destructive, error color.
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        scope.launch {
                            val ok = viewModel.deleteArtifact(uri)
                            if (ok) onDeleted()
                            // On failure, dialog is already dismissed; user sees no change.
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Text("Delete")
                }
            },
        )
    }
}

/**
 * Single action item in the [PreviewScreen] bottom bar — 48dp `IconButton` over a 24dp `Icon`,
 * 4dp `Spacer`, then a 10sp `labelSmall` `Text`. White tint on the secondary surface.
 *
 * UI-SPEC §5: 48dp touch target exceeds the 44dp minimum; the wrapping Column adds label
 * height but the actual hit region remains the 48dp IconButton.
 */
@Composable
private fun PreviewAction(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.semantics { role = Role.Button },
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier.size(48.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Color.White,
                modifier = Modifier.size(24.dp),
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
        )
    }
}
