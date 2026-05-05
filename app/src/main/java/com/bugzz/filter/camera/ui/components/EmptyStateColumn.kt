package com.bugzz.filter.camera.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airbnb.lottie.compose.LottieConstants

/**
 * Shared empty-state Column composable (D-26 / 06-UI-SPEC §8).
 *
 * Layout (vertical-center stack on `fillMaxSize`):
 *   1. 120dp Lottie loop (animation asset path is parameterized; default `lottie/home_lottie.json`)
 *   2. 16dp Spacer
 *   3. 16sp / Medium heading text
 *   4. 16dp Spacer
 *   5. Material3 Button with [ctaLabel] (14sp / Medium via `labelLarge`)
 *
 * Used by:
 *   - [com.bugzz.filter.camera.ui.collection.CollectionScreen] — UX-07 empty state ("No bugs
 *     captured yet" / "Open Camera").
 *   - Future Plan 06-07 Settings → clear-all confirmation success state.
 *
 * Accessibility: the Lottie animation carries a `contentDescription = "Empty state animation"`
 * so TalkBack announces a meaningful label. The heading + button are naturally accessible via
 * their text content. The Column itself is unmerged — TalkBack walks the heading then the button.
 *
 * Phase 6, Plan 06-05.
 *
 * @param heading        16sp / Medium screen-level heading (e.g. "No bugs captured yet").
 * @param ctaLabel       14sp / Medium button label (e.g. "Open Camera").
 * @param onCta          Action invoked when the user taps the CTA button.
 * @param modifier       Layout modifier supplied by parent (typically `Modifier.padding(padding)`).
 * @param animationAsset Lottie asset path (relative to `app/src/main/assets/`). Default reuses the
 *                       Splash/Onboarding animation per D-29.
 */
@Composable
fun EmptyStateColumn(
    heading: String,
    ctaLabel: String,
    onCta: () -> Unit,
    modifier: Modifier = Modifier,
    animationAsset: String = "lottie/home_lottie.json",
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier.fillMaxSize(),
    ) {
        LottiePlayer(
            assetPath = animationAsset,
            modifier = Modifier
                .size(120.dp)
                .semantics { contentDescription = "Empty state animation" },
            iterations = LottieConstants.IterateForever,
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = heading,
            style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(16.dp))
        Button(onClick = onCta) {
            Text(
                text = ctaLabel,
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}
