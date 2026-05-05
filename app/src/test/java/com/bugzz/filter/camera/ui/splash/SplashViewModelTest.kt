package com.bugzz.filter.camera.ui.splash

import app.cash.turbine.test
import com.bugzz.filter.camera.data.FilterPrefsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub

/**
 * Unit tests for [SplashViewModel] (UX-01 + T-06-04).
 *
 * Coverage matrix:
 *   - onboarding_completed=false → onboardingCompleted StateFlow resolves to false (route to Onboarding)
 *   - onboarding_completed=true  → onboardingCompleted StateFlow resolves to true  (route to Home)
 *   - DataStore IOException via .catch → onboardingCompleted StateFlow resolves to null (T-06-04 safe default — composable treats null after delay as "show onboarding")
 *
 * Pattern mirrors [com.bugzz.filter.camera.ui.insect.InsectFilterViewModelTest]: pure JVM,
 * StandardTestDispatcher + advanceUntilIdle, mock FilterPrefsRepository (no Hilt graph).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SplashViewModelTest {

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
     * UX-01 path A: first launch (DataStore key onboarding_completed=false)
     * → SplashViewModel.onboardingCompleted resolves to false.
     */
    @Test
    fun onboardingNotCompleted_emitsOnboardingNavTarget() = runTest(testDispatcher) {
        mockPrefs.stub {
            on { onboardingCompleted } doReturn flowOf(false)
        }
        val vm = SplashViewModel(mockPrefs)
        advanceUntilIdle()

        vm.onboardingCompleted.test {
            assertEquals(false, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    /**
     * UX-01 path B: subsequent launch (DataStore key onboarding_completed=true)
     * → SplashViewModel.onboardingCompleted resolves to true.
     */
    @Test
    fun onboardingCompleted_emitsHomeNavTarget() = runTest(testDispatcher) {
        mockPrefs.stub {
            on { onboardingCompleted } doReturn flowOf(true)
        }
        val vm = SplashViewModel(mockPrefs)
        advanceUntilIdle()

        vm.onboardingCompleted.test {
            assertEquals(true, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    /**
     * T-06-04 mirror: if DataStore read throws (corrupted prefs file), SplashViewModel must NOT
     * crash and must default to null (composable treats null after the 1.5s delay as
     * "show onboarding" — defence-in-depth on top of the repository's own .catch fallback).
     */
    @Test
    fun dataStoreIoException_emitsOnboardingNavTarget_safeDefault() = runTest(testDispatcher) {
        mockPrefs.stub {
            on { onboardingCompleted } doReturn flow { throw java.io.IOException("simulated corruption") }
        }
        val vm = SplashViewModel(mockPrefs)
        advanceUntilIdle()

        // .catch { emit(null) } → first emission after error is null.
        vm.onboardingCompleted.test {
            assertNull(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }
}
