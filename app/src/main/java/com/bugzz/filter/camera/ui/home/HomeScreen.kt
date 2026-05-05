package com.bugzz.filter.camera.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Production HomeScreen per 04-UI-SPEC §Component Specs §1 + 04-CONTEXT D-19.
 *
 * Layout (portrait-locked per D-21):
 *  - Settings gear: IconButton TopEnd, padding 16dp. Tap → invokes [onSettings] lambda
 *    (Phase 6 Plan 06-06 wiring; Plan 06-07 finalizes the BugzzApp call site to navigate to
 *    SettingsRoute). Replaces Plan 04's Toast "Settings coming soon" placeholder.
 *  - Center stack: Face Filter (200x80 filled, enabled) + Insect Filter (200x80 outlined,
 *    enabled in Phase 5) separated by 32dp vertical gap.
 *  - Bottom: My Collection (160x56 outlined) with 32dp bottom padding.
 *
 * Phase 6 Plan 06-06 changes (D-06): added `onSettings` lambda parameter; removed Toast +
 * `LocalContext` usage. Plan 06-07 will replace BugzzApp's placeholder lambda with
 * `navController.navigate(SettingsRoute)` once SettingsScreen lands.
 *
 * MOD-01 delivery — both mode buttons visible; Face + Insect filter shipped Phase 4 + 5.
 */
@Composable
fun HomeScreen(
    onFaceFilter: () -> Unit,
    onInsectFilter: () -> Unit,
    onMyCollection: () -> Unit,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(modifier = modifier.fillMaxSize()) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {

            // Settings gear — TopEnd (04-UI-SPEC §1 Settings gear).
            // Plan 06-06: Toast "Settings coming soon" replaced by onSettings lambda (D-06).
            IconButton(
                onClick = onSettings,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 16.dp, end = 16.dp)
                    .size(48.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }

            // Primary focal point: Face Filter button (FilledButton)
            // Center button stack (Face Filter + Insect Filter, 32dp spacing, centered).
            Column(
                modifier = Modifier.align(Alignment.Center),
                verticalArrangement = Arrangement.spacedBy(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Face Filter — primary CTA, Material3 filled button default colors.
                Button(
                    onClick = onFaceFilter,
                    modifier = Modifier.size(width = 200.dp, height = 80.dp),
                ) {
                    Text(
                        text = "Face Filter",
                        style = MaterialTheme.typography.labelLarge,
                    )
                }

                // Insect Filter — enabled in Phase 5 (MOD-03..07). Navigates to InsectFilterScreen.
                OutlinedButton(
                    onClick = onInsectFilter,
                    enabled = true,
                    modifier = Modifier.size(width = 200.dp, height = 80.dp),
                ) {
                    Text(
                        text = "Insect Filter",
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }

            // My Collection — BottomCenter (04-UI-SPEC §1).
            OutlinedButton(
                onClick = onMyCollection,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 32.dp)
                    .size(width = 160.dp, height = 56.dp),
            ) {
                Text(
                    text = "My Collection",
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}
