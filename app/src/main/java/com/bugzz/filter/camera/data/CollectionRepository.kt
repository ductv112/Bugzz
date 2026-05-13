package com.bugzz.filter.camera.data

import android.content.ContentUris
import android.content.Context
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
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
 * **Phase 7 D-20b live refresh:** Threading now uses [callbackFlow] with a [ContentObserver]
 * registered on `MediaStore.Files.getContentUri("external")` with `notifyForDescendants=true`.
 * A single observer covers BOTH image and video changes (RESEARCH Pattern 3 + Pitfall 5 —
 * per-MIME registration would miss cross-namespace updates). The flow emits the initial
 * snapshot on subscription, then RE-emits each time MediaStore signals a change. The
 * observer is unregistered in [awaitClose] when collection is cancelled — T-07-03 mitigation
 * (no listener leak through ViewModel rebind cycles).
 *
 * The cursor walk runs on [Dispatchers.IO] via [flowOn] applied to the callbackFlow — the
 * producer body itself executes on IO, so [performQuery] (synchronous I/O) is correctly
 * dispatched off Main. Callers that only need the initial snapshot can use `.first()` —
 * `awaitClose` fires correctly on cancellation.
 *
 * Phase 6, Plan 06-05, UX-05, D-12, T-06-02. Phase 7, Plan 07-04, D-20b, T-07-03, T-07-11.
 * RESEARCH §Pattern 3 + §Pattern 4 + §Critical Note + §Pitfall 5.
 */
@Singleton
class CollectionRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    /**
     * Emits the current snapshot of Bugzz captures (DCIM/Bugzz/, image/jpeg + video/mp4)
     * sorted newest-first, then RE-emits each time a MediaStore.Files row is added or removed.
     *
     * **Phase 7 D-20b — Live MediaStore refresh:** registers a [ContentObserver] on
     * `MediaStore.Files.getContentUri("external")` with `notifyForDescendants=true`. A single
     * observer covers both image and video changes (RESEARCH Pattern 3 + Pitfall 5 —
     * per-MIME registration would miss cross-namespace notifications). When the observer's
     * `onChange` fires, [performQuery] is re-invoked on IO and a fresh list is emitted.
     *
     * **T-06-02 mitigation preserved verbatim:** selectionArgs bind via parallel array —
     * no string concatenation. See [performQuery].
     *
     * **T-07-03 mitigation:** the observer is unregistered via [awaitClose] when the flow
     * collection is cancelled (ViewModel scoping handles this — no manual DisposableEffect
     * needed on the composable side).
     *
     * Returns an empty list when:
     *   - The directory has no qualifying files
     *   - `ContentResolver.query` returns `null` (rare — happens when storage permission is
     *     revoked mid-session). The `?.use {}` short-circuits cleanly without crashing.
     */
    fun loadMediaItems(): Flow<List<MediaItem>> = callbackFlow {
        // ContentObserver dispatches onChange via Handler.post — when constructed with the main
        // Looper, callbacks land on Main. We re-enter the callbackFlow's coroutine scope via
        // launch{} so trySend is invoked inside the producer scope (capturing structured
        // cancellation when the collector cancels).
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                launch {
                    trySend(performQuery())
                }
            }
        }

        context.contentResolver.registerContentObserver(
            MediaStore.Files.getContentUri("external"),
            /* notifyForDescendants = */ true,
            observer,
        )

        // Initial snapshot — emit current contents immediately on subscription.
        // flowOn(Dispatchers.IO) below moves the producer body to IO, so performQuery runs there.
        trySend(performQuery())

        awaitClose {
            context.contentResolver.unregisterContentObserver(observer)
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Synchronous MediaStore query. Extracted from the Phase 6 `flow {}` body so the Phase 7
     * [callbackFlow] can call it both for the initial snapshot AND each `observer.onChange`.
     *
     * Phase 6 D-12 + T-06-02 + RESEARCH §Critical Note logic preserved verbatim — per-MIME
     * URI namespace re-construction via [ContentUris.withAppendedId]; selectionArgs binding;
     * sort by `DATE_MODIFIED DESC`.
     */
    private fun performQuery(): List<MediaItem> {
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
        return items
    }
}
