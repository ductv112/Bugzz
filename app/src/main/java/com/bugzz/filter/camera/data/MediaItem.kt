package com.bugzz.filter.camera.data

import android.net.Uri

/**
 * Single Bugzz capture artifact (photo or video) read from the MediaStore.
 *
 * D-15 — pure data class. Bitmap thumbnails are NOT stored on this type (RESEARCH §Anti-pattern):
 * keeping bitmaps off the model lets the cursor-mapped list survive configuration change without
 * leaking video frame buffers, and lets the ViewModel cache thumbnails in a separate
 * `Map<Uri, Bitmap?>` whose lifecycle is bound to `viewModelScope`.
 *
 * @property uri          MediaStore content URI — `content://media/external/images/media/{id}` for
 *                        photos, `content://media/external/video/media/{id}` for videos. Per-MIME
 *                        namespace correctness is required so that downstream consumers
 *                        (PreviewScreen `AsyncImage`, ShareIntent, ContentResolver.delete) can
 *                        open the URI without falling back to the `Files` namespace which some
 *                        sharing apps reject (RESEARCH §Critical Note).
 * @property mimeType     `image/jpeg` or `video/mp4` only — the [CollectionRepository] selection
 *                        clause filters out everything else. Used by [com.bugzz.filter.camera.ui.collection.CollectionScreen]
 *                        thumbnail composable to branch between `AsyncImage` (photo) and the
 *                        ViewModel-cached `MediaMetadataRetriever` bitmap (video).
 * @property displayName  Filename as stored in MediaStore (e.g. `"BUGZZ_20260505_120000.jpg"`).
 *                        Surfaced as `contentDescription` on the thumbnail Box for TalkBack.
 * @property dateModified Unix epoch seconds (MediaStore convention). Used as the secondary sort
 *                        key in [CollectionRepository.loadMediaItems] (DESC — newest first).
 *
 * Phase 6, Plan 06-05, UX-05.
 */
data class MediaItem(
    val uri: Uri,
    val mimeType: String,
    val displayName: String,
    val dateModified: Long,
)
