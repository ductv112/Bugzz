package com.bugzz.filter.camera.ui.nav

import com.bugzz.filter.camera.ui.home.CameraMode
import kotlinx.serialization.Serializable

@Serializable
data object SplashRoute

@Serializable
data object HomeRoute

/** D-20 — accepts CameraMode so HomeScreen's Face Filter vs Insect Filter buttons land on the right screen. */
@Serializable
data class CameraRoute(val mode: CameraMode = CameraMode.FaceFilter)

@Serializable
data object PreviewRoute

@Serializable
data object CollectionRoute
