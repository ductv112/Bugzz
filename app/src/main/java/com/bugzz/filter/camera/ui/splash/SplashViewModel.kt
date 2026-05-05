package com.bugzz.filter.camera.ui.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bugzz.filter.camera.data.FilterPrefsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * Splash screen view-model — exposes the [FilterPrefsRepository.onboardingCompleted] state as a
 * 3-valued [StateFlow] so the composable can branch on (loading, first-launch, subsequent-launch):
 *
 *   - `null`  → DataStore not yet read; SplashScreen waits on the LaunchedEffect.
 *   - `false` → Onboarding incomplete; route to [OnboardingRoute] (D-02 first-launch path).
 *   - `true`  → Onboarding done; route to [HomeRoute] (D-02 subsequent-launch path).
 *
 * T-06-04 mitigation: if the upstream Flow throws (DataStore corruption), the `.catch` arm emits
 * `null`; the composable's LaunchedEffect treats `null` after the 1.5s delay as "show
 * onboarding" — safer to re-onboard than to crash on Splash. Mirrors T-04-01 mitigation in the
 * repository itself (which already returns `false` on corruption — this catch is a defence in
 * depth in case any future Flow operator above us throws).
 */
@HiltViewModel
class SplashViewModel @Inject constructor(
    prefs: FilterPrefsRepository,
) : ViewModel() {

    val onboardingCompleted: StateFlow<Boolean?> = prefs.onboardingCompleted
        .map<Boolean, Boolean?> { it }
        .catch { emit(null) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)
}
