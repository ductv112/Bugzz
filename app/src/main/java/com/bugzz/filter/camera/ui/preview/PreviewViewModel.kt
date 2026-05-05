package com.bugzz.filter.camera.ui.preview

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

/**
 * Phase 6 PreviewScreen ViewModel — MIME branch + delete contract (UX-04).
 *
 * Two pure-suspend operations exposed to the composable:
 *   1. [resolveMimeType] — `ContentResolver.getType(uri)` on `Dispatchers.IO`. The composable
 *      calls this on `LaunchedEffect(uri)` to pick photo vs video render path. Falls back to
 *      `"image/jpeg"` when the resolver returns null (rare but legal — e.g., a freshly-inserted
 *      MediaStore entry whose MIME wasn't queried yet).
 *   2. [deleteArtifact] — `ContentResolver.delete(uri, null, null)` on `Dispatchers.IO`,
 *      wrapped in try/catch so the composable receives a clean `Boolean` result without
 *      propagating crashes. Returns `true` iff the resolver reports ≥1 row deleted.
 *
 * No mutable state held here — the composable owns `mimeType`/`showDeleteDialog`/etc. via
 * `remember { mutableStateOf(...) }`. This ViewModel is intentionally a thin Hilt-injected
 * wrapper around `ContentResolver` so it survives configuration change without re-querying.
 *
 * **T-06-01 mitigation (deferred to Plan 06-06):** Plan 06 will read `mimeType` here and pass
 * it into a `ShareIntentBuilder`. This plan ships Share as a Toast placeholder, so MIME is
 * only used for the photo-vs-video render branch.
 *
 * **T-06-03 mitigation:** Owned by the [VideoPreview] composable's `DisposableEffect`, not
 * this ViewModel. ExoPlayer is a Compose-scoped resource (lives with the composable's
 * `remember(uri)`), not a ViewModel-scoped resource.
 *
 * Phase 6, Plan 06-04, UX-04, T-06-01 (deferred), T-06-03 (delegated).
 */
@HiltViewModel
class PreviewViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
) : ViewModel() {

    /**
     * Resolves the MIME type of the captured artifact at [uri]. Used by the composable to
     * branch between photo (`AsyncImage`) and video (`VideoPreview`) render paths.
     *
     * @return MIME type string (e.g., `"image/jpeg"`, `"video/mp4"`) — never null. Falls back
     *   to `"image/jpeg"` (safe photo default) when the resolver yields null.
     */
    suspend fun resolveMimeType(uri: Uri): String = withContext(Dispatchers.IO) {
        context.contentResolver.getType(uri) ?: "image/jpeg"
    }

    /**
     * Deletes the artifact at [uri] via `MediaStore.delete`. Runs on [Dispatchers.IO] so the
     * Compose main thread is never blocked on disk I/O.
     *
     * Catches all `Exception` subclasses (notably `SecurityException` from URI permission
     * revocation and `IllegalArgumentException` from a stale URI) — logs via Timber and
     * returns `false`. The composable can then choose to surface a Toast or simply leave
     * the screen open.
     *
     * @return `true` iff `ContentResolver.delete` reports ≥1 row removed; `false` otherwise.
     */
    suspend fun deleteArtifact(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.delete(uri, null, null) > 0
        } catch (e: Exception) {
            Timber.tag("Preview").e(e, "Delete failed for $uri")
            false
        }
    }
}
