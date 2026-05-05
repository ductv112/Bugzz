package com.bugzz.filter.camera.ui

import android.net.Uri
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.bugzz.filter.camera.ui.camera.CameraScreen
import com.bugzz.filter.camera.ui.home.CameraMode
import com.bugzz.filter.camera.ui.home.HomeScreen
import com.bugzz.filter.camera.ui.insect.InsectFilterScreen
import com.bugzz.filter.camera.ui.nav.CameraRoute
import com.bugzz.filter.camera.ui.nav.CollectionRoute
import com.bugzz.filter.camera.ui.nav.HomeRoute
import com.bugzz.filter.camera.ui.nav.OnboardingRoute
import com.bugzz.filter.camera.ui.nav.PreviewRoute
import com.bugzz.filter.camera.ui.nav.SplashRoute
import com.bugzz.filter.camera.ui.onboarding.OnboardingScreen
import com.bugzz.filter.camera.ui.collection.CollectionScreen
import com.bugzz.filter.camera.ui.preview.PreviewScreen
import com.bugzz.filter.camera.ui.splash.SplashScreen

@Composable
fun BugzzApp() {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = SplashRoute,
    ) {
        composable<SplashRoute> {
            SplashScreen(
                onNavigateToOnboarding = {
                    navController.navigate(OnboardingRoute) {
                        popUpTo(SplashRoute) { inclusive = true }
                    }
                },
                onNavigateToHome = {
                    navController.navigate(HomeRoute) {
                        popUpTo(SplashRoute) { inclusive = true }
                    }
                }
            )
        }
        composable<OnboardingRoute> {
            OnboardingScreen(
                onComplete = {
                    navController.navigate(HomeRoute) {
                        popUpTo(OnboardingRoute) { inclusive = true }
                    }
                }
            )
        }
        composable<HomeRoute> {
            HomeScreen(
                onFaceFilter = { navController.navigate(CameraRoute(mode = CameraMode.FaceFilter)) },
                onInsectFilter = { navController.navigate(CameraRoute(mode = CameraMode.InsectFilter)) },
                onMyCollection = { navController.navigate(CollectionRoute) },
            )
        }
        composable<CameraRoute> { backStackEntry ->
            val route: CameraRoute = backStackEntry.toRoute()
            when (route.mode) {
                CameraMode.FaceFilter -> CameraScreen(
                    onCaptureSaved = { uri ->
                        navController.navigate(PreviewRoute(uri.toString()))
                    },
                )
                CameraMode.InsectFilter -> InsectFilterScreen(
                    onCaptureSaved = { uri ->
                        navController.navigate(PreviewRoute(uri.toString()))
                    },
                )
            }
        }
        composable<PreviewRoute> { backStackEntry ->
            val context = LocalContext.current
            val route: PreviewRoute = backStackEntry.toRoute()
            val uri = Uri.parse(route.uri)
            PreviewScreen(
                uri = uri,
                onDone = { navController.popBackStack() },
                onRetake = { navController.popBackStack() },
                onDeleted = { navController.popBackStack() },
                onShareNotImplemented = {
                    // Plan 06-06 wires real Intent.ACTION_SEND + FileProvider URI grant.
                    Toast.makeText(context, "Share coming next", Toast.LENGTH_SHORT).show()
                },
            )
        }
        composable<CollectionRoute> {
            CollectionScreen(
                onBack = { navController.popBackStack() },
                onItemTap = { item ->
                    navController.navigate(PreviewRoute(item.uri.toString()))
                },
                onOpenCamera = {
                    navController.navigate(HomeRoute) {
                        popUpTo(CollectionRoute) { inclusive = true }
                    }
                },
            )
        }
    }
}
