package com.bugzz.filter.camera.ui.nav

import com.bugzz.filter.camera.ui.home.CameraMode
import kotlinx.serialization.Serializable

@Serializable
data object SplashRoute

/**
 * D-24 — Onboarding 3-page carousel destination, reached only when DataStore
 * `onboarding_completed = false` (first launch). Splash auto-advances here, and on Skip /
 * Get Started the screen pops Onboarding from the back-stack and lands on [HomeRoute].
 */
@Serializable
data object OnboardingRoute

@Serializable
data object HomeRoute

/** D-20 — accepts CameraMode so HomeScreen's Face Filter vs Insect Filter buttons land on the right screen. */
@Serializable
data class CameraRoute(val mode: CameraMode = CameraMode.FaceFilter)

@Serializable
data object PreviewRoute

@Serializable
data object CollectionRoute

/**
 * D-24 — Settings destination (composable wiring lands in Plan 06-07 alongside HomeScreen
 * `onSettings` lambda). Added in Plan 06-03 alongside [OnboardingRoute] so all new route
 * declarations land in a single Wave 2 commit; the actual destination composable + nav graph
 * entry come later.
 */
@Serializable
data object SettingsRoute
