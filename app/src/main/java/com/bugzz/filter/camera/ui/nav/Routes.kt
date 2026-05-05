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

/**
 * Preview destination — accepts a content URI string for the captured artifact (photo or video).
 *
 * Plan 06-04 atomic breaking change (RESEARCH Pitfall 6): converted from `data object PreviewRoute`
 * to `data class PreviewRoute(val uri: String)`. The string round-trips a `Uri` (e.g.
 * `content://media/external/images/media/1234`); the BugzzApp `composable<PreviewRoute>` consumer
 * parses it back via `Uri.parse(route.uri)`. Cannot use `Uri` directly as nav arg — kotlinx
 * serialization lacks a built-in serializer for the Android type, and adding a custom serializer
 * is heavier than the toString/parse round-trip.
 *
 * Used by Camera/InsectFilter post-capture navigation (D-09 — replaces Phase 3 Toast on
 * `OneShotEvent.PhotoSaved`/`VideoSaved`) and by Collection item taps (Plan 06-05).
 */
@Serializable
data class PreviewRoute(val uri: String)

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
