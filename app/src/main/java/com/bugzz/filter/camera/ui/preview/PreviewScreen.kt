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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.bugzz.filter.camera.ui.components.DeleteConfirmDialog
import com.bugzz.filter.camera.ui.share.buildShareIntent
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
 *   - **Done** (Check icon)   → [onDone]    — file remains saved; pop back
 *   - **Share** (Share icon)  → real `Intent.ACTION_SEND` via [buildShareIntent] +
 *                                `context.startActivity` (Plan 06-06 wiring; replaces Plan 04
 *                                Toast placeholder). T-06-01 mitigation lives in [buildShareIntent].
 *   - **Delete** (Delete icon) → shared [DeleteConfirmDialog] (Plan 06-06 — replaces inline
 *                                AlertDialog from Plan 04) → [PreviewViewModel.deleteArtifact] →
 *                                [onDeleted] on success
 *   - **Retake** (Refresh icon) → [onRetake] — file remains saved; pop back to capture screen
 *
 * Plan 06-06 changes (vs Plan 06-04):
 *   - `onShareNotImplemented` parameter removed; Share action now reads `LocalContext` and calls
 *     `context.startActivity(buildShareIntent(uri, mimeType))` directly.
 *   - Inline `AlertDialog(...)` block replaced with the shared
 *     [com.bugzz.filter.camera.ui.components.DeleteConfirmDialog] composable.
 *
 * Phase 6, Plan 06-06, UX-04, UX-08, SHR-01..04, T-06-01, T-06-03 (delegated to [VideoPreview]),
 * D-08, D-09, D-10, D-16.
 *
 * @param uri Captured artifact URI (content://media/...). Always non-null per BugzzApp nav arg.
 * @param onDone "Done" button action (typically `navController.popBackStack()`).
 * @param onRetake "Retake" button action (typically `navController.popBackStack()`).
 * @param onDeleted Called when delete succeeds (typically `navController.popBackStack()`).
 */
@Composable
fun PreviewScreen(
    uri: Uri,
    onDone: () -> Unit,
    onRetake: () -> Unit,
    onDeleted: () -> Unit,
    viewModel: PreviewViewModel = hiltViewModel(),
) {
    var mimeType by remember(uri) { mutableStateOf<String?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

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
                    // Plan 06-06: real Intent.ACTION_SEND wiring (SHR-01..04, T-06-01).
                    onClick = {
                        scope.launch {
                            val mime = mimeType ?: viewModel.resolveMimeType(uri)
                            context.startActivity(buildShareIntent(uri, mime))
                        }
                    },
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

    // Plan 06-06: shared DeleteConfirmDialog (replaces Plan 04's inline AlertDialog).
    if (showDeleteDialog) {
        DeleteConfirmDialog(
            onDismiss = { showDeleteDialog = false },
            onConfirm = {
                showDeleteDialog = false
                scope.launch {
                    val ok = viewModel.deleteArtifact(uri)
                    if (ok) onDeleted()
                    // On failure, dialog is already dismissed; user sees no change.
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
