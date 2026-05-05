package com.bugzz.filter.camera.ui.onboarding

import org.junit.Ignore
import org.junit.Test

/**
 * RED scaffold per 06-VALIDATION Wave 0.
 *
 * Un-Ignored in **Plan 06-03** when OnboardingViewModel lands.
 *
 * Coverage matrix (UX-02 + D-23):
 *   - completeOnboarding() invokes FilterPrefsRepository.setOnboardingCompleted(true)
 *   - The DataStore flow re-emits true after the write completes (round-trip)
 *
 * Purpose: this file pre-creates the test path that Plan 06-03's per-task verification command
 * will reference (`*OnboardingViewModelTest*`). Tests are intentionally @Ignored — the
 * OnboardingViewModel SUT does not exist yet, so we do not import it; we reference it only
 * by name in the @Ignore message. When Plan 06-03 lands the production class, the implementer
 * will replace `markMissing()` calls with real construction (mock FilterPrefsRepository +
 * StandardTestDispatcher in @Before) and verify(...) / first() assertions, then drop @Ignore.
 *
 * Pattern mirrored from Phase 5 Wave 0 [com.bugzz.filter.camera.ui.insect.InsectFilterViewModelTest].
 * Pure JVM — no @RunWith.
 */
class OnboardingViewModelTest {

    /** Stub helper — replaced with real assertions in Plan 06-03. */
    private fun markMissing() {
        // Intentional no-op.
    }

    /**
     * UX-02: tapping "Get Started" on the final onboarding page calls
     * OnboardingViewModel.completeOnboarding(), which must persist the flag via
     * FilterPrefsRepository.setOnboardingCompleted(true). Verified with mock VM dependency.
     */
    @Test
    @Ignore("Plan 06-03 — un-ignore when OnboardingViewModel lands")
    fun completeOnboarding_writesFlagViaRepository() {
        markMissing()
    }

    /**
     * UX-02 round-trip: after completeOnboarding() resolves, the
     * FilterPrefsRepository.onboardingCompleted flow emits true on next read.
     * Catches a regression where setOnboardingCompleted writes to a different key than read.
     */
    @Test
    @Ignore("Plan 06-03 — un-ignore when OnboardingViewModel lands")
    fun completeOnboarding_flowReemitsTrueAfterWrite() {
        markMissing()
    }
}
