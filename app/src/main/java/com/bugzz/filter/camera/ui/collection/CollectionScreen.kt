package com.bugzz.filter.camera.ui.collection

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.bugzz.filter.camera.data.MediaItem
import com.bugzz.filter.camera.ui.components.EmptyStateColumn

/**
 * Phase 6 CollectionScreen — 3-column thumbnail grid of Bugzz captures, with empty state.
 *
 * Layout per 06-UI-SPEC §6:
 *   - `Scaffold(topBar = SmallTopAppBar("My Collection") with back-arrow IconButton)`
 *   - When `uiState.isEmpty`: [EmptyStateColumn] with heading "No bugs captured yet" + CTA
 *     "Open Camera" — taps invoke [onOpenCamera] (BugzzApp routes to HomeRoute popUpTo
 *     CollectionRoute inclusive).
 *   - Otherwise: `LazyVerticalGrid(GridCells.Adaptive(120.dp))` with `4dp` vertical and horizontal
 *     spacing (xs token, D-11). Each cell is a [CollectionThumbnail] Box at `aspectRatio(1f)`.
 *
 * Per-item:
 *   - Image rows render via Coil `AsyncImage(ContentScale.Crop)` with a #2A2A2A gray placeholder
 *     and error painter — same color as Phase 4's filter-picker error state.
 *   - Video rows render the [CollectionViewModel] thumbnail-cache bitmap (or gray placeholder
 *     while extraction is pending) under a 30%-black scrim with a centered 24dp white PlayArrow
 *     icon overlay (UI-SPEC §6 — accent-white reserved usage #6).
 *
 * Tap → [onItemTap]: BugzzApp routes to `PreviewRoute(uri.toString())`. Plan 04's atomic breaking
 * change made [com.bugzz.filter.camera.ui.nav.PreviewRoute] a `data class(val uri: String)`, so the
 * thumbnail tap and the post-capture navigation both share one route shape (D-13 — Retake from
 * Preview entered via Collection pops to Collection, not Camera; standard back-stack semantics).
 *
 * Phase 6, Plan 06-05, UX-05, UX-06, UX-07. D-11, D-13.
 *
 * @param onBack       Back-arrow tap (typically `navController.popBackStack()`).
 * @param onItemTap    Thumbnail tap (BugzzApp navigates to `PreviewRoute(item.uri.toString())`).
 * @param onOpenCamera Empty-state CTA tap (BugzzApp navigates to `HomeRoute` popUpTo Collection).
 * @param viewModel    [CollectionViewModel] (default Hilt-scoped instance via `hiltViewModel()`).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionScreen(
    onBack: () -> Unit,
    onItemTap: (MediaItem) -> Unit,
    onOpenCamera: () -> Unit,
    viewModel: CollectionViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Collection") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
            )
        },
    ) { padding ->
        when {
            // Defer the loading branch until first emission has actually landed and is empty —
            // a typical query completes in <50ms; flashing a spinner for that interval feels worse
            // than just rendering the empty grid for one frame. (UI-SPEC §6 subjective note.)
            uiState.isEmpty -> {
                EmptyStateColumn(
                    heading = "No bugs captured yet",
                    ctaLabel = "Open Camera",
                    onCta = onOpenCamera,
                    modifier = Modifier.padding(padding),
                )
            }
            else -> {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 120.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                ) {
                    items(uiState.items, key = { it.uri.toString() }) { item ->
                        CollectionThumbnail(
                            item = item,
                            videoThumbnail = uiState.videoThumbnails[item.uri],
                            onTap = { onItemTap(item) },
                        )
                    }
                }
            }
        }
    }
}

/**
 * Single grid cell — square (`aspectRatio(1f)`) clickable Box that renders either a Coil-loaded
 * photo, or a [Bitmap]-based video thumbnail with a play-icon overlay.
 *
 * Per UI-SPEC §6:
 *   - Box is `RectangleShape` clipped (no rounding — flush-edge grid)
 *   - Photo: `AsyncImage` with #2A2A2A placeholder + error painter
 *   - Video: bitmap (or #2A2A2A placeholder while extracting) + 30% black scrim + 24dp white
 *     `Icons.Default.PlayArrow`
 *   - Accessibility: parent Box has `Role.Button` + `contentDescription = displayName`; inner
 *     image content descriptions are null so TalkBack reads the parent label only.
 */
@Composable
private fun CollectionThumbnail(
    item: MediaItem,
    videoThumbnail: Bitmap?,
    onTap: () -> Unit,
) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RectangleShape)
            .clickable { onTap() }
            .semantics {
                role = Role.Button
                contentDescription = item.displayName
            },
    ) {
        if (item.mimeType.startsWith("image")) {
            AsyncImage(
                model = item.uri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                placeholder = ColorPainter(Color(0xFF2A2A2A)),
                error = ColorPainter(Color(0xFF2A2A2A)),
            )
        } else {
            // Video branch — bitmap from VM cache, else gray placeholder.
            if (videoThumbnail != null) {
                androidx.compose.foundation.Image(
                    bitmap = videoThumbnail.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF2A2A2A)),
                )
            }
            // Play-icon overlay — 30% black scrim + 24dp white PlayArrow
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f)),
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp),
                )
            }
        }
    }
}
