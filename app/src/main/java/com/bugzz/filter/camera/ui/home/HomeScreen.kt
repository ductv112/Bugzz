package com.bugzz.filter.camera.ui.home

import android.widget.Toast
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

/**
 * Production HomeScreen per 04-UI-SPEC §Component Specs §1 + 04-CONTEXT D-19.
 *
 * Layout (portrait-locked per D-21):
 *  - Settings gear: IconButton TopEnd, padding 16dp. Tap → Toast "Settings coming soon".
 *  - Center stack: Face Filter (200x80 filled, enabled) + Insect Filter (200x80 outlined, disabled)
 *    separated by 32dp vertical gap.
 *  - Bottom: My Collection (160x56 outlined) with 32dp bottom padding.
 *
 * MOD-01 delivery — both mode buttons visible; only Face Filter is functional in Phase 4.
 */
@Composable
fun HomeScreen(
    onFaceFilter: () -> Unit,
    onMyCollection: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    Scaffold(modifier = modifier.fillMaxSize()) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {

            // Settings gear — TopEnd (04-UI-SPEC §1 Settings gear).
            IconButton(
                onClick = { Toast.makeText(context, "Settings coming soon", Toast.LENGTH_SHORT).show() },
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

                // Insect Filter — disabled in Phase 4 (enabled = false). Material3 suppresses onClick automatically.
                // Phase 5 MOD-03..07 flips enabled to true and adds the free-placement sticker screen.
                OutlinedButton(
                    onClick = { /* no-op; enabled = false suppresses */ },
                    enabled = false,
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
