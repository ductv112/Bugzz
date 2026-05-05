package com.bugzz.filter.camera.ui.settings

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import com.bugzz.filter.camera.BuildConfig

/**
 * Phase 6 SettingsScreen — stub-only Settings list (UX-09, D-17, D-18, UI-SPEC §9).
 *
 * Renders a [Scaffold] with a [TopAppBar] ("Settings" + back arrow) above a [Column] of four
 * [ListItem] rows separated by [HorizontalDivider]s:
 *   1. **Version** — read-only; trailing text = `BuildConfig.VERSION_NAME` (e.g. `"0.1.0"`).
 *      Marked `disabled()` for TalkBack (non-interactive).
 *   2. **Privacy Policy** — clickable; tap shows a Toast `"Coming in next release"`. No real URL is
 *      launched per D-17 (T-06-06 mitigation deferred to v2 milestone — when a real Privacy URL is
 *      added it MUST use HTTPS-only `Intent.ACTION_VIEW` so the system browser handles it; never a
 *      `WebView`).
 *   3. **Rate the App** — clickable; tap shows a Toast `"Coming when published to Play Store"`.
 *      D-18: app is not (yet) on Play Store; deep-link target does not exist.
 *   4. **About** — read-only; supporting text = `"Bugzz — Bug filter prank camera"`. `disabled()`.
 *
 * **Phase 6 stub only** — D-17/D-18 lock this to Toast stubs. v2 milestone replaces:
 *   - Privacy Policy: real HTTPS URL via `Intent.ACTION_VIEW` (system browser; no WebView).
 *   - Rate the App: `market://details?id=...` deep-link with HTTPS fallback.
 *   - Adds: theme toggle, default-camera selector, debug overlay (D-18 explicit — none ship Phase 6).
 *
 * No ViewModel and no state are used — this is a pure presentational composable. Toast launches
 * are direct framework calls; there is nothing to inject for a v2 swap.
 *
 * Typography per UI-SPEC §9: every row label and trailing text uses `MaterialTheme.typography.bodyMedium`
 * (14sp Normal) — preserves the 4-size {10,14,16,24} inventory and 2-weight {Normal,Medium} palette.
 *
 * Divider style per UI-SPEC §9: `HorizontalDivider()` Material3 default — no color override
 * (1dp `colorScheme.outlineVariant`).
 *
 * @param onBack Back-arrow tap — typically `navController.popBackStack()`. BugzzApp wires this in
 *   the `composable<SettingsRoute>` block (Plan 06-07 finalize).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            // Row 1 — Version (read-only).
            ListItem(
                headlineContent = {
                    Text("Version", style = MaterialTheme.typography.bodyMedium)
                },
                trailingContent = {
                    Text(BuildConfig.VERSION_NAME, style = MaterialTheme.typography.bodyMedium)
                },
                modifier = Modifier.semantics {
                    contentDescription = "Version: ${BuildConfig.VERSION_NAME}"
                    disabled()
                },
            )
            HorizontalDivider()

            // Row 2 — Privacy Policy (Toast stub; D-17, T-06-06).
            ListItem(
                headlineContent = {
                    Text("Privacy Policy", style = MaterialTheme.typography.bodyMedium)
                },
                modifier = Modifier
                    .clickable {
                        Toast.makeText(
                            context,
                            "Coming in next release",
                            Toast.LENGTH_SHORT,
                        ).show()
                    }
                    .semantics {
                        role = Role.Button
                        contentDescription = "Privacy Policy"
                    },
            )
            HorizontalDivider()

            // Row 3 — Rate the App (Toast stub; D-18).
            ListItem(
                headlineContent = {
                    Text("Rate the App", style = MaterialTheme.typography.bodyMedium)
                },
                modifier = Modifier
                    .clickable {
                        Toast.makeText(
                            context,
                            "Coming when published to Play Store",
                            Toast.LENGTH_SHORT,
                        ).show()
                    }
                    .semantics {
                        role = Role.Button
                        contentDescription = "Rate the App"
                    },
            )
            HorizontalDivider()

            // Row 4 — About (read-only).
            ListItem(
                headlineContent = {
                    Text("About", style = MaterialTheme.typography.bodyMedium)
                },
                supportingContent = {
                    Text(
                        "Bugzz — Bug filter prank camera",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                },
                modifier = Modifier.semantics {
                    contentDescription = "About: Bugzz — Bug filter prank camera"
                    disabled()
                },
            )
            HorizontalDivider()
        }
    }
}
