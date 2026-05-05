package com.bugzz.filter.camera.ui.splash

import org.junit.Ignore
import org.junit.Test

/**
 * RED scaffold per 06-VALIDATION Wave 0.
 *
 * Un-Ignored in **Plan 06-03** when SplashViewModel lands.
 *
 * Coverage matrix (UX-01 + T-06-04):
 *   - onboarding_completed=false → emits Onboarding navigation target
 *   - onboarding_completed=true  → emits Home navigation target
 *   - DataStore IOException      → emits Onboarding (safe-default false per T-06-04 mirror of T-04-01)
 *
 * Purpose: this file pre-creates the test path that Plan 06-03's per-task verification command
 * will reference (`*SplashViewModelTest*`). Tests are intentionally @Ignored — the SplashViewModel
 * SUT does not exist yet, so we do not import it; we reference it only by name in the @Ignore
 * message. When Plan 06-03 lands the production class, the implementer will:
 *   1. Add real imports + setUp/tearDown blocks
 *   2. Replace each `markMissing()` call with the real assertion using a constructed VM
 *   3. Remove the @Ignore annotation
 *
 * Pattern mirrored from Phase 5 Wave 0: [com.bugzz.filter.camera.ui.insect.StickerStateTest]
 * (when it was @Ignored prior to Plan 05-02 landing). Pure JVM — no @RunWith, no Android types.
 */
class SplashViewModelTest {

    /** Stub helper: lets each test body compile cleanly while remaining @Ignored at runtime. */
    private fun markMissing() {
        // Intentional no-op. Body replaced with real construction + assertion in Plan 06-03.
    }

    /**
     * UX-01 path A: first launch (DataStore key onboarding_completed missing or false)
     * → SplashViewModel emits a navigation event routing to OnboardingRoute.
     */
    @Test
    @Ignore("Plan 06-03 — un-ignore when SplashViewModel lands")
    fun onboardingNotCompleted_emitsOnboardingNavTarget() {
        markMissing()
    }

    /**
     * UX-01 path B: subsequent launch (DataStore key onboarding_completed=true)
     * → SplashViewModel emits a navigation event routing to HomeRoute.
     */
    @Test
    @Ignore("Plan 06-03 — un-ignore when SplashViewModel lands")
    fun onboardingCompleted_emitsHomeNavTarget() {
        markMissing()
    }

    /**
     * T-06-04 mirror: if DataStore read throws IOException (corrupted prefs file),
     * SplashViewModel must NOT crash and must default to the OnboardingRoute target.
     * Same fail-safe pattern as FilterPrefsRepository corruptedDataStore_emitsDefault.
     */
    @Test
    @Ignore("Plan 06-03 — un-ignore when SplashViewModel lands")
    fun dataStoreIoException_emitsOnboardingNavTarget_safeDefault() {
        markMissing()
    }
}
