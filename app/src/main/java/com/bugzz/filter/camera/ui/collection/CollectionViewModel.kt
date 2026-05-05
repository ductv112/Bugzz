package com.bugzz.filter.camera.ui.collection

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bugzz.filter.camera.data.CollectionRepository
import com.bugzz.filter.camera.data.MediaItem
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

/**
 * UI state for the Collection screen (UX-05 / UX-07).
 *
 * @property items            All Bugzz captures, newest-first. Empty list when collection is empty.
 * @property videoThumbnails  Lazily-populated cache of decoded video thumbnails. The map grows
 *                            as `MediaMetadataRetriever` extractions complete; absent keys mean
 *                            "still extracting" — render the gray placeholder. The map is held
 *                            in UI state (NOT on [MediaItem]) so the data model stays bitmap-free
 *                            and survives configuration change without leaking native frame buffers
 *                            (RESEARCH §Open Question 8 / Pattern 5).
 * @property isLoading        `true` until the first repository emission lands.
 * @property isEmpty          `true` iff the repository emitted an empty list. Drives EmptyStateColumn
 *                            rendering downstream.
 *
 * Phase 6, Plan 06-05.
 */
data class CollectionUiState(
    val items: List<MediaItem> = emptyList(),
    val videoThumbnails: Map<Uri, Bitmap?> = emptyMap(),
    val isLoading: Boolean = true,
    val isEmpty: Boolean = false,
)

/**
 * Hilt-injected ViewModel backing the Collection screen.
 *
 * Lifecycle:
 *   1. `init {}` collects [CollectionRepository.loadMediaItems] in `viewModelScope`.
 *   2. On each emission, [_uiState] updates with `items` + `isEmpty` + `isLoading=false`.
 *   3. For every video item in the emission, a child coroutine launches to extract its thumbnail
 *      via [MediaMetadataRetriever.getFrameAtTime] on [Dispatchers.IO] — off the critical path
 *      so the grid renders placeholders instantly and fills in as bitmaps decode.
 *
 * Thumbnail caching design (RESEARCH §Pattern 5 / §Open Question 8):
 *   - Bitmaps live in [_uiState.videoThumbnails] — a `Map<Uri, Bitmap?>` keyed by URI.
 *   - On extraction failure (corrupt MP4, deleted concurrently, codec error) the entry is set to
 *     `null` — the composable renders the gray placeholder + play-icon overlay just as it does
 *     before extraction completes. No crash, no Toast.
 *   - `MediaMetadataRetriever.release()` runs in `finally` to free native resources deterministically.
 *
 * Phase 6, Plan 06-05, UX-05, UX-07.
 */
@HiltViewModel
class CollectionViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: CollectionRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CollectionUiState(isLoading = true))
    val uiState: StateFlow<CollectionUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.loadMediaItems().collect { items ->
                _uiState.update {
                    it.copy(
                        items = items,
                        isLoading = false,
                        isEmpty = items.isEmpty(),
                    )
                }
                // Lazy-load video thumbnails per item — off the critical path. Each launch runs
                // independently; failures are swallowed inside extractAndCacheThumbnail.
                items.filter { it.mimeType.startsWith("video") }.forEach { item ->
                    launch { extractAndCacheThumbnail(item.uri) }
                }
            }
        }
    }

    private suspend fun extractAndCacheThumbnail(uri: Uri) = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)
            val bmp = retriever.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
            _uiState.update { it.copy(videoThumbnails = it.videoThumbnails + (uri to bmp)) }
        } catch (e: Exception) {
            Timber.tag("Collection").w(e, "Thumbnail extraction failed for $uri")
            _uiState.update { it.copy(videoThumbnails = it.videoThumbnails + (uri to null)) }
        } finally {
            try {
                retriever.release()
            } catch (e: Exception) {
                Timber.tag("Collection").w(e, "MediaMetadataRetriever.release failed")
            }
        }
    }
}
