package com.bugzz.filter.camera.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.airbnb.lottie.compose.LottieConstants
import com.bugzz.filter.camera.ui.components.LottiePlayer
import kotlinx.coroutines.launch

/**
 * Onboarding 3-page carousel — UX-02 / D-03 / D-04 / D-05 / 06-UI-SPEC §3.
 *
 * Layout (z-order bottom-to-top):
 *   1. Black background ([Color.Black]).
 *   2. [HorizontalPager] full-screen with 3 pages — each page: Lottie 200dp + Spacer 24dp +
 *      title (24sp/Medium white) + Spacer 16dp + body (bodyMedium 80% white). 32dp horizontal
 *      padding inside each page.
 *   3. Skip [TextButton] aligned TopEnd — pads 16dp from edges.
 *   4. Bottom controls Column at BottomCenter, padding bottom = 48dp:
 *      - 3-dot indicator Row, spacedBy 8dp; active dot 12dp #E53935, inactive 8dp #9E9E9E.
 *      - Spacer 24dp (lg).
 *      - Primary Button — label "Next" on pages 0/1, "Get Started" on page 2.
 *
 * Branch logic on Button tap is extracted into [decideNextAction] so the UX-02 contract is
 * unit-testable without spinning up the Compose UI runtime — see [OnboardingPagerStateTest].
 *
 * Spacing tokens used (all on the 4dp grid per UI-SPEC §Spacing): 4, 8, 12, 16, 24, 32, 48, 200.
 *
 * T-06-05 mitigation: Lottie composition load failure leaves the Lottie area blank but pager
 * swipe + buttons remain functional — failure is non-fatal.
 */
@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val pagerState = rememberPagerState(pageCount = { OnboardingPages.size })
    val scope = rememberCoroutineScope()

    val finish: () -> Unit = {
        viewModel.completeOnboarding()
        onComplete()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        // Pager — full-screen page content.
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
        ) { pageIndex ->
            OnboardingPageContent(page = OnboardingPages[pageIndex])
        }

        // Skip — TopEnd.
        TextButton(
            onClick = finish,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 16.dp, end = 16.dp)
                .semantics {
                    contentDescription = "Skip onboarding"
                    role = Role.Button
                },
        ) {
            Text(
                text = "Skip",
                style = MaterialTheme.typography.labelLarge,
                color = Color.White,
            )
        }

        // Bottom controls — page indicator dots + primary Next/Get Started button.
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.semantics {
                    contentDescription = "Page ${pagerState.currentPage + 1} of ${OnboardingPages.size}"
                },
            ) {
                repeat(OnboardingPages.size) { index ->
                    val isActive = pagerState.currentPage == index
                    Box(
                        modifier = Modifier
                            .size(if (isActive) 12.dp else 8.dp)
                            .clip(CircleShape)
                            .background(
                                if (isActive) Color(0xFFE53935) else Color(0xFF9E9E9E)
                            )
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
            val isFinalPage = pagerState.currentPage == OnboardingPages.lastIndex
            Button(
                onClick = {
                    when (val action = decideNextAction(pagerState.currentPage, OnboardingPages.size)) {
                        NextAction.Complete -> finish()
                        is NextAction.Advance -> {
                            scope.launch { pagerState.animateScrollToPage(action.toPage) }
                        }
                    }
                },
                modifier = Modifier.semantics {
                    contentDescription = if (isFinalPage) "Get started" else "Next page"
                    role = Role.Button
                },
            ) {
                Text(
                    text = if (isFinalPage) "Get Started" else "Next",
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}

@Composable
private fun OnboardingPageContent(page: OnboardingPage) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
    ) {
        LottiePlayer(
            assetPath = "lottie/home_lottie.json",
            modifier = Modifier.size(200.dp),
            iterations = LottieConstants.IterateForever,
        )
        Spacer(Modifier.height(24.dp))
        Text(
            text = page.title,
            style = TextStyle(
                fontSize = 24.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White,
            ),
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = page.body,
            style = MaterialTheme.typography.bodyMedium.copy(
                color = Color.White.copy(alpha = 0.8f),
            ),
        )
    }
}

/** UX-02 page model — title + body verbatim per 06-UI-SPEC §3 D-03. */
internal data class OnboardingPage(val title: String, val body: String)

internal val OnboardingPages: List<OnboardingPage> = listOf(
    OnboardingPage(
        title = "Welcome to Bugzz",
        body = "Bug filters that crawl on your face. Pranks made easy.",
    ),
    OnboardingPage(
        title = "Pick a filter",
        body = "15 bug filters with 4 behaviors. Static, crawl, swarm, fall.",
    ),
    OnboardingPage(
        title = "Capture and share",
        body = "Photo or video. Share to friends instantly.",
    ),
)

/**
 * Result of evaluating the primary "Next / Get Started" button branch.
 * Extracted from [OnboardingScreen] composable so the conditional is unit-testable on plain JVM
 * (no Compose UI runtime dependency). See [OnboardingPagerStateTest].
 */
internal sealed class NextAction {
    /** Final page or Skip path: persist onboarding flag and fire onComplete. */
    data object Complete : NextAction()

    /** Pages 0..n-2: animate the pager forward by one. */
    data class Advance(val toPage: Int) : NextAction()
}

/**
 * Decides what the primary button should do given the current page index.
 *   - On the last page (index == pageCount - 1): [NextAction.Complete].
 *   - Otherwise: [NextAction.Advance] to currentPage + 1.
 *
 * `pageCount` parameter (defaults to [OnboardingPages] size) lets tests pin the boundary
 * regardless of whether the page list is later expanded.
 */
internal fun decideNextAction(currentPage: Int, pageCount: Int = OnboardingPages.size): NextAction =
    if (currentPage >= pageCount - 1) NextAction.Complete
    else NextAction.Advance(currentPage + 1)
