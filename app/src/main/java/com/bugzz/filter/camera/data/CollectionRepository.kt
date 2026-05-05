package com.bugzz.filter.camera.data

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject
import javax.inject.Singleton

/**
 * MediaStore-backed read-only repository surfacing Bugzz captures (photos + videos) saved under
 * `DCIM/Bugzz/`.
 *
 * UX-05 — query is scoped to:
 *   - `RELATIVE_PATH LIKE 'DCIM/Bugzz/%'` — only files Bugzz wrote (no other apps' artifacts)
 *   - MIME type ∈ `{image/jpeg, video/mp4}` — strict whitelist; anything else (HEIC, WebP, MOV)
 *     is filtered server-side by ContentResolver, so it never reaches the cursor loop
 *
 * **T-06-02 mitigation (Information Disclosure):** the selection clause uses `?` placeholders
 * with [selectionArgs] passed as a parallel array. ContentResolver bind-parameters the args
 * before issuing the underlying SQL — string concatenation / SQL injection is impossible at
 * this layer. Verified by [CollectionRepositoryTest.selectionArgsBindRelativePath] which captures
 * the args array and asserts `selectionArgs[0] == "DCIM/Bugzz/%"`.
 *
 * **Per-MIME URI namespace (D-12 / RESEARCH §Critical Note):** MediaStore.Files is a *unified*
 * cursor over images + videos, but the rows return per-row `_ID`s scoped to the `Files` provider
 * namespace. Sharing apps (Gmail, Messages, etc.) and `ContentResolver.openInputStream` calls
 * sometimes reject `content://media/external/file/{id}` URIs. We therefore re-construct the URI
 * via [ContentUris.withAppendedId] using `Images.Media.EXTERNAL_CONTENT_URI` for image rows
 * and `Video.Media.EXTERNAL_CONTENT_URI` for video rows so every produced [MediaItem.uri] is
 * in its native per-MIME namespace.
 *
 * Sort order: `DATE_MODIFIED DESC` — newest first, matching reference Bugzz Filters Prank's
 * Collection screen ordering.
 *
 * Threading: the cursor walk runs on [Dispatchers.IO] via [flowOn]. The flow emits exactly once
 * per subscription (terminal `emit`); to refresh, callers re-subscribe. ContentObserver-backed
 * live updates are deferred — Plan 06-06 will revisit when share + delete round-trips need it.
 *
 * Phase 6, Plan 06-05, UX-05, D-12, T-06-02. RESEARCH §Pattern 4 + §Critical Note.
 */
@Singleton
class CollectionRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    /**
     * Emits a single snapshot of Bugzz captures, sorted newest-first.
     *
     * Returns an empty list when:
     *   - The directory has no qualifying files
     *   - `ContentResolver.query` returns `null` (rare — happens when storage permission is
     *     revoked mid-session). The `?.use {}` short-circuits cleanly without crashing.
     */
    fun loadMediaItems(): Flow<List<MediaItem>> = flow {
        val projection = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.MIME_TYPE,
            MediaStore.MediaColumns.DATE_MODIFIED,
        )
        // T-06-02: bind via selectionArgs — never concat user/path data into selection.
        val selection = "${MediaStore.MediaColumns.RELATIVE_PATH} LIKE ? AND " +
            "(${MediaStore.MediaColumns.MIME_TYPE} = ? OR ${MediaStore.MediaColumns.MIME_TYPE} = ?)"
        val selectionArgs = arrayOf("DCIM/Bugzz/%", "image/jpeg", "video/mp4")
        val sortOrder = "${MediaStore.MediaColumns.DATE_MODIFIED} DESC"

        val items = mutableListOf<MediaItem>()
        context.contentResolver.query(
            MediaStore.Files.getContentUri("external"),
            projection,
            selection,
            selectionArgs,
            sortOrder,
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
            val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE)
            val dateCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_MODIFIED)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val name = cursor.getString(nameCol) ?: ""
                val mime = cursor.getString(mimeCol) ?: continue
                val date = cursor.getLong(dateCol)

                // RESEARCH §Critical Note — per-MIME namespace re-construction.
                val contentUri = if (mime.startsWith("image")) {
                    ContentUris.withAppendedId(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        id,
                    )
                } else {
                    ContentUris.withAppendedId(
                        MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                        id,
                    )
                }
                items += MediaItem(
                    uri = contentUri,
                    mimeType = mime,
                    displayName = name,
                    dateModified = date,
                )
            }
        }
        emit(items)
    }.flowOn(Dispatchers.IO)
}
