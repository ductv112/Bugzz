package com.bugzz.filter.camera.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition

/**
 * Shared Lottie animation player wrapper (D-26 / 06-UI-SPEC §1).
 *
 * Centralises [LottieAnimation] usage so [SplashScreen], [OnboardingScreen], and
 * [EmptyStateColumn] (Plan 06-04+) share a single composable surface and identical asset-loading
 * pattern: [LottieCompositionSpec.Asset] reads from `assets/...` (NOT `res/raw/`) per RESEARCH
 * Pitfall 4 — D-29 chose the asset path so the same JSON is reused across screens without
 * duplicating it under `res/raw/`.
 *
 * T-06-05 mitigation: when [rememberLottieComposition] returns `composition = null` (asset
 * missing or JSON parse error), [LottieAnimation] renders nothing rather than crashing — caller
 * screens use independent timing (Splash `delay(1500)`) so navigation flow proceeds regardless.
 *
 * @param assetPath Asset-relative path, e.g. `"lottie/home_lottie.json"`.
 * @param modifier  Layout modifier (caller controls size; default does not enforce one).
 * @param iterations Number of times to play; defaults to [LottieConstants.IterateForever].
 *                   Splash uses `1` (one-shot); Onboarding/EmptyState use forever.
 * @param isPlaying Pauses the animation when `false`. Default `true`.
 */
@Composable
fun LottiePlayer(
    assetPath: String,
    modifier: Modifier = Modifier,
    iterations: Int = LottieConstants.IterateForever,
    isPlaying: Boolean = true,
) {
    val composition by rememberLottieComposition(LottieCompositionSpec.Asset(assetPath))
    val progress by animateLottieCompositionAsState(
        composition = composition,
        isPlaying = isPlaying,
        iterations = iterations,
    )
    LottieAnimation(
        composition = composition,
        progress = { progress },
        modifier = modifier,
    )
}
