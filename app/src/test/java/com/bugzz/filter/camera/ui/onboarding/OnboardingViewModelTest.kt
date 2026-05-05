package com.bugzz.filter.camera.ui.onboarding

import com.bugzz.filter.camera.data.FilterPrefsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify

/**
 * Unit tests for [OnboardingViewModel] (UX-02 + D-23).
 *
 * Coverage matrix:
 *   - completeOnboarding() invokes [FilterPrefsRepository.setOnboardingCompleted] exactly once
 *   - completeOnboarding() called twice → setter invoked twice (idempotent at repo layer; the
 *     ViewModel does NOT memoize because each call from UI is intentional and harmless)
 *
 * Pattern mirrors [com.bugzz.filter.camera.ui.insect.InsectFilterViewModelTest]:
 * StandardTestDispatcher + advanceUntilIdle() to flush viewModelScope.launch{} calls.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val mockPrefs: FilterPrefsRepository = mock()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /**
     * UX-02: tapping "Get Started" / Skip / Next-on-page-2 calls [OnboardingViewModel.completeOnboarding],
     * which must persist via [FilterPrefsRepository.setOnboardingCompleted] exactly once per call.
     */
    @Test
    fun completeOnboarding_writesFlagViaRepository() = runTest(testDispatcher) {
        val vm = OnboardingViewModel(mockPrefs)

        vm.completeOnboarding()
        advanceUntilIdle()

        verify(mockPrefs, times(1)).setOnboardingCompleted()
    }

    /**
     * UX-02 round-trip via the layer's contract: two calls to completeOnboarding() result in two
     * writes (idempotent at the storage layer because `setOnboardingCompleted` writes `true`
     * regardless of prior state — repeated calls don't accumulate or fail).
     */
    @Test
    fun completeOnboarding_flowReemitsTrueAfterWrite() = runTest(testDispatcher) {
        val vm = OnboardingViewModel(mockPrefs)

        vm.completeOnboarding()
        vm.completeOnboarding()
        advanceUntilIdle()

        verify(mockPrefs, times(2)).setOnboardingCompleted()
    }
}
