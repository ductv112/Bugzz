package com.bugzz.filter.camera.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bugzz.filter.camera.data.FilterPrefsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Onboarding view-model — UX-02 / D-05.
 *
 * Single responsibility: persist `onboarding_completed = true` via [FilterPrefsRepository] so
 * subsequent app launches route Splash → Home (D-23). Caller (composable) navigates to
 * [HomeRoute] independently — fire-and-forget pattern (no need to await DataStore write before
 * leaving the screen; the next launch reads from disk, by then the write has long since
 * resolved).
 *
 * Idempotent: calling [completeOnboarding] twice is harmless because the underlying
 * [FilterPrefsRepository.setOnboardingCompleted] sets a boolean to `true` regardless of prior
 * state.
 */
@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val prefs: FilterPrefsRepository,
) : ViewModel() {

    /** Persists `onboarding_completed = true` on Skip/Next-on-page-2/Get-Started. */
    fun completeOnboarding() {
        viewModelScope.launch {
            prefs.setOnboardingCompleted()
        }
    }
}
