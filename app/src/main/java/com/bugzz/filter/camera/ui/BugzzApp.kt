package com.bugzz.filter.camera.ui

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.bugzz.filter.camera.ui.nav.CameraRoute
import com.bugzz.filter.camera.ui.nav.CollectionRoute
import com.bugzz.filter.camera.ui.nav.HomeRoute
import com.bugzz.filter.camera.ui.nav.PreviewRoute
import com.bugzz.filter.camera.ui.nav.SplashRoute
import com.bugzz.filter.camera.ui.camera.CameraScreen
import com.bugzz.filter.camera.ui.screens.CollectionScreen
import com.bugzz.filter.camera.ui.screens.HomeScreen
import com.bugzz.filter.camera.ui.screens.PreviewScreen
import com.bugzz.filter.camera.ui.screens.SplashScreen

@Composable
fun BugzzApp() {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = SplashRoute
    ) {
        composable<SplashRoute> {
            SplashScreen(onContinue = { navController.navigate(HomeRoute) })
        }
        composable<HomeRoute> {
            HomeScreen(
                onOpenCamera = { navController.navigate(CameraRoute) },
                onOpenCollection = { navController.navigate(CollectionRoute) }
            )
        }
        composable<CameraRoute> {
            CameraScreen(onOpenPreview = { navController.navigate(PreviewRoute) })
        }
        composable<PreviewRoute> {
            PreviewScreen(onBack = { navController.popBackStack() })
        }
        composable<CollectionRoute> {
            CollectionScreen(onBack = { navController.popBackStack() })
        }
    }
}
