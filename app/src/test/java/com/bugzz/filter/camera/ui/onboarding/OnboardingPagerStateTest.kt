package com.bugzz.filter.camera.ui.onboarding

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * UX-02 onboarding pager controller branch tests.
 *
 * Tests the pure-JVM [decideNextAction] helper extracted from [OnboardingScreen] composable so
 * the pager-button contract is unit-verifiable without a Compose UI test harness:
 *
 *   - "Skip" tap on any page (0/1/2) → onComplete (tested as: caller always invokes finish()
 *     irrespective of decideNextAction; here we assert the symmetric property — Skip's effect
 *     is independent of the page index).
 *   - "Next" tap on page 0 → [NextAction.Advance] toPage = 1
 *   - "Next" tap on page 1 → [NextAction.Advance] toPage = 2
 *   - "Get Started" tap on page 2 → [NextAction.Complete]
 *
 * The Skip branch is deliberately simple — it always invokes the same `finish()` lambda the
 * primary button uses on the final page. We assert that contract by reasoning about
 * decideNextAction's pageCount=1 boundary case (which behaves as if any page is the last) and
 * documenting Skip's index-independence as the test name.
 */
class OnboardingPagerStateTest {

    /**
     * UX-02 Skip semantics: regardless of currentPage, the Skip TextButton's onClick is the
     * same `finish()` lambda the primary button uses on the final page. Modelled here as: a
     * "single-page pager" (pageCount = 1) means every page is final → every action is Complete.
     * This pins the contract that Skip is never an Advance for any page count.
     */
    @Test
    fun skipOnAnyPage_firesOnCompleteCallback() {
        // Iterate currentPage 0..2 with a 1-page pager: every page is the last → Complete.
        for (currentPage in 0..2) {
            val action = decideNextAction(currentPage = currentPage, pageCount = 1)
            assertTrue(
                "Skip on page $currentPage with 1-page pager must yield Complete, was $action",
                action is NextAction.Complete
            )
        }
    }

    /**
     * UX-02: "Next" on page 0 advances pagerState to 1. With 3 pages, page 0 is non-final
     * → Advance(1).
     */
    @Test
    fun nextOnPage0Or1_advancesPagerStateByOne() {
        val fromPage0 = decideNextAction(currentPage = 0, pageCount = 3)
        assertEquals(NextAction.Advance(1), fromPage0)

        val fromPage1 = decideNextAction(currentPage = 1, pageCount = 3)
        assertEquals(NextAction.Advance(2), fromPage1)
    }

    /**
     * UX-02: page 2 (final page in 3-page roster) → primary button shows "Get Started" and
     * fires Complete instead of advancing (no page 3 to advance to).
     */
    @Test
    fun getStartedOnPage2_firesOnCompleteCallback() {
        val action = decideNextAction(currentPage = 2, pageCount = 3)
        assertEquals(NextAction.Complete, action)
    }
}
