package com.bugzz.filter.camera.ui

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.bugzz.filter.camera.ui.camera.CameraScreen
import com.bugzz.filter.camera.ui.home.CameraMode
import com.bugzz.filter.camera.ui.home.HomeScreen
import com.bugzz.filter.camera.ui.home.InsectFilterStubScreen
import com.bugzz.filter.camera.ui.nav.CameraRoute
import com.bugzz.filter.camera.ui.nav.CollectionRoute
import com.bugzz.filter.camera.ui.nav.HomeRoute
import com.bugzz.filter.camera.ui.nav.PreviewRoute
import com.bugzz.filter.camera.ui.nav.SplashRoute
import com.bugzz.filter.camera.ui.screens.CollectionScreen
import com.bugzz.filter.camera.ui.screens.PreviewScreen
import com.bugzz.filter.camera.ui.screens.SplashScreen

@Composable
fun BugzzApp() {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = SplashRoute,
    ) {
        composable<SplashRoute> {
            SplashScreen(onContinue = { navController.navigate(HomeRoute) })
        }
        composable<HomeRoute> {
            HomeScreen(
                onFaceFilter = { navController.navigate(CameraRoute(mode = CameraMode.FaceFilter)) },
                onMyCollection = { navController.navigate(CollectionRoute) },
            )
        }
        composable<CameraRoute> { backStackEntry ->
            val route: CameraRoute = backStackEntry.toRoute()
            when (route.mode) {
                CameraMode.FaceFilter -> CameraScreen(
                    onOpenPreview = { navController.navigate(PreviewRoute) }
                )
                CameraMode.InsectFilter -> InsectFilterStubScreen(
                    onBack = { navController.popBackStack() }
                )
            }
        }
        composable<PreviewRoute> {
            PreviewScreen(onBack = { navController.popBackStack() })
        }
        composable<CollectionRoute> {
            CollectionScreen(onBack = { navController.popBackStack() })
        }
    }
}
