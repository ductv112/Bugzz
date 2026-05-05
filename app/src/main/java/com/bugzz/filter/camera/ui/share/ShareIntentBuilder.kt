package com.bugzz.filter.camera.ui.share

import android.content.Intent
import android.net.Uri

/**
 * Phase 6 Plan 06-06 — pure share-intent factory (SHR-01..04, T-06-01).
 *
 * Builds the canonical Android share-sheet intent for a captured artifact (photo or video) at a
 * MediaStore content URI. Replaces Plan 06-04's Toast "Share coming next" placeholder.
 *
 * Wire pattern (RESEARCH §Pattern 6 / UI-SPEC §5 PreviewScreen):
 * ```
 * val mime = viewModel.resolveMimeType(uri)
 * context.startActivity(buildShareIntent(uri, mime))
 * ```
 *
 * Contract verified by [com.bugzz.filter.camera.ui.share.ShareIntentBuilderTest]:
 *   - SHR-03: result `Intent.action == ACTION_CHOOSER` (createChooser wrap)
 *   - SHR-02: inner intent `type == mimeType` (image/jpeg or video/mp4)
 *   - SHR-01: inner intent `EXTRA_STREAM == uri`
 *   - T-06-01: inner intent has `FLAG_GRANT_READ_URI_PERMISSION` set so the receiving app can
 *     temporarily read the MediaStore URI without `SecurityException`
 *
 * Threat mitigation (T-06-01 — Information Disclosure via share leakage):
 *   - `FLAG_GRANT_READ_URI_PERMISSION` scopes the URI grant to the chosen app only — Android does
 *     NOT broadcast the bytes to all apps; only the receiver picked from the chooser sheet gets
 *     temporary read access for the lifetime of the share.
 *   - No `EXTRA_TEXT` is added (D-21) — minimum disclosure surface; only the bytes themselves.
 *   - `Intent.createChooser` ensures the user explicitly picks the receiver every time (no silent
 *     auto-share to the last-used app, which an attacker could exploit if they had pre-installed
 *     a malicious "last share target").
 *
 * Pure function: no `Context` captured, no state held — trivially testable under Robolectric and
 * trivially safe to call from any thread.
 *
 * Phase 6, Plan 06-06, SHR-01..04, T-06-01, D-19..D-21.
 *
 * @param uri      MediaStore content URI of the captured artifact (photo or video).
 * @param mimeType IANA MIME type matching [uri] (e.g., `"image/jpeg"`, `"video/mp4"`). Resolved
 *   upstream via `ContentResolver.getType(uri)` in [com.bugzz.filter.camera.ui.preview.PreviewViewModel.resolveMimeType].
 * @return An `Intent.ACTION_CHOOSER`-wrapped intent ready to pass to `Context.startActivity`.
 */
fun buildShareIntent(uri: Uri, mimeType: String): Intent {
    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = mimeType
        putExtra(Intent.EXTRA_STREAM, uri)
        // T-06-01 mitigation — scope URI grant to the chosen receiver app only.
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    return Intent.createChooser(sendIntent, "Share via")
}
