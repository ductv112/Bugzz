package com.bugzz.filter.camera.ui.splash

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material3.Text
import com.airbnb.lottie.compose.LottieConstants
import com.bugzz.filter.camera.ui.components.LottiePlayer
import kotlinx.coroutines.delay

/**
 * SplashScreen — UX-01 / D-01 / D-02 / 06-UI-SPEC §2.
 *
 * Renders a 200dp Lottie animation centered with the "Bugzz" brand text below on a black
 * background. Auto-advances after 1500ms based on [SplashViewModel.onboardingCompleted]:
 *
 *   - `false` (or `null` post-delay — T-06-04 safe default): navigate to OnboardingRoute.
 *   - `true`: navigate to HomeRoute.
 *
 * Both transitions use `popUpTo(SplashRoute) { inclusive = true }` (wired in BugzzApp) so Back
 * from Home does NOT return to Splash — D-25.
 *
 * Spacing tokens (06-UI-SPEC §Spacing Scale): Lottie 200dp / Spacer 16dp (md) / brand text 24sp.
 * Brand text uses 24sp/Medium (per UI-SPEC iter-2 fix; NOT 32sp).
 */
@Composable
fun SplashScreen(
    onNavigateToOnboarding: () -> Unit,
    onNavigateToHome: () -> Unit,
    viewModel: SplashViewModel = hiltViewModel(),
) {
    val onboardingCompleted by viewModel.onboardingCompleted.collectAsStateWithLifecycle()

    LaunchedEffect(onboardingCompleted) {
        // null = still loading from DataStore — skip; the SharingStarted.Eagerly stateIn() will
        // re-emit shortly (in practice within microseconds for a fresh DataStore read), and this
        // LaunchedEffect re-runs because its key changed.
        if (onboardingCompleted == null) return@LaunchedEffect

        delay(1_500L)

        // T-06-04 safe default: treat null after delay (extremely unlikely with Eagerly) as
        // "first launch" — re-show onboarding rather than skip it.
        if (onboardingCompleted == true) {
            onNavigateToHome()
        } else {
            onNavigateToOnboarding()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            LottiePlayer(
                assetPath = "lottie/home_lottie.json",
                modifier = Modifier
                    .size(200.dp)
                    .semantics { contentDescription = "Bugzz logo animation" },
                iterations = 1,
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = "Bugzz",
                style = TextStyle(
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White,
                ),
            )
        }
    }
}
