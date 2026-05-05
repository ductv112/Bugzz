package com.bugzz.filter.camera.ui.onboarding

import org.junit.Ignore
import org.junit.Test

/**
 * RED scaffold per 06-VALIDATION Wave 0.
 *
 * Un-Ignored in **Plan 06-03** when the OnboardingScreen pager-control logic lands.
 *
 * Coverage matrix (UX-02 — pager interaction contract):
 *   - "Skip" tap on any page (0/1/2) fires the onComplete callback (writes onboarding_completed=true)
 *   - "Next" tap on page 0 advances pagerState.currentPage to 1
 *   - "Next" tap on page 1 advances pagerState.currentPage to 2
 *   - "Get Started" tap on page 2 fires onComplete (NOT next; final page transitions out)
 *
 * Purpose: tests the controller logic that wires HorizontalPager state transitions to
 * Skip/Next/GetStarted button callbacks — independent of Compose UI wiring. The intent is
 * to verify the *callback contract* via plain functions / lambdas; no Compose UI test
 * harness needed (Plan 06-03 will likely extract this into a `OnboardingPagerControl` helper
 * function or VM method).
 *
 * Pattern: pure JVM (no @RunWith). The implementer in Plan 06-03 chooses whether to mock
 * Compose's PagerState or to extract a thin domain wrapper testable without androidx.compose.
 */
class OnboardingPagerStateTest {

    /** Stub helper — replaced with real assertions in Plan 06-03. */
    private fun markMissing() {
        // Intentional no-op.
    }

    /**
     * UX-02: regardless of which page (0, 1, or 2) the user is on, tapping "Skip" must invoke
     * the onComplete callback (which the screen wires to OnboardingViewModel.completeOnboarding).
     */
    @Test
    @Ignore("Plan 06-03 — un-ignore when OnboardingScreen pager controls land")
    fun skipOnAnyPage_firesOnCompleteCallback() {
        markMissing()
    }

    /**
     * UX-02: "Next" on page 0 or page 1 advances pagerState by exactly one page.
     * Combined assertion — implementer may split into two tests if cleaner.
     */
    @Test
    @Ignore("Plan 06-03 — un-ignore when OnboardingScreen pager controls land")
    fun nextOnPage0Or1_advancesPagerStateByOne() {
        markMissing()
    }

    /**
     * UX-02: page 2 (final page) shows "Get Started" instead of "Next"; tapping it fires
     * onComplete (not advancing the pager — there is no page 3).
     */
    @Test
    @Ignore("Plan 06-03 — un-ignore when OnboardingScreen pager controls land")
    fun getStartedOnPage2_firesOnCompleteCallback() {
        markMissing()
    }
}
