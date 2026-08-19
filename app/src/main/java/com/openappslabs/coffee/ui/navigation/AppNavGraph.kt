package com.openappslabs.coffee.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.openappslabs.coffee.ui.screens.aboutscreen.AboutScreen
import com.openappslabs.coffee.ui.screens.homescreen.HomeScreen
import com.openappslabs.coffee.ui.screens.onboardingscreen.OnboardingScreen

@Composable
fun AppNavGraph(
    navController: NavHostController = rememberNavController(),
    appWidgetId: Int,
    openWidgetSheet: Boolean = false,
    viewModel: AppNavViewModel = hiltViewModel()
) {
    val startDestination by viewModel.startDestination.collectAsStateWithLifecycle()

    val onAboutClick = remember(navController) {
        {
            val currentState = navController.currentBackStackEntry?.lifecycle?.currentState
            if (currentState?.isAtLeast(Lifecycle.State.RESUMED) == true) {
                navController.navigate(Screen.About) {
                    launchSingleTop = true
                }
            }
        }
    }

    val onBackClick = remember(navController) {
        {
            val currentState = navController.currentBackStackEntry?.lifecycle?.currentState
            if (currentState?.isAtLeast(Lifecycle.State.RESUMED) == true) {
                navController.popBackStack()
            }
            Unit
        }
    }

    if (startDestination != null) {
        NavHost(
            navController = navController,
            startDestination = startDestination!!,
            enterTransition = Navimation.enterTransition,
            exitTransition = Navimation.exitTransition,
            popEnterTransition = Navimation.popEnterTransition,
            popExitTransition = Navimation.popExitTransition
        ) {

            composable<Screen.Onboarding> {
                OnboardingScreen(
                    onComplete = {
                        navController.navigate(Screen.Home) {
                            popUpTo(Screen.Onboarding) { inclusive = true }
                        }
                    }
                )
            }

            homeScreenRoute(
                onAboutClick = onAboutClick,
                appWidgetId = appWidgetId,
                openWidgetSheet = openWidgetSheet
            )

            aboutScreenRoute(onBackClick = onBackClick)
        }
    }
}

private fun NavGraphBuilder.homeScreenRoute(
    onAboutClick: () -> Unit,
    appWidgetId: Int,
    openWidgetSheet: Boolean
) {
    composable<Screen.Home> {
        HomeScreen(
            onAboutClick = onAboutClick,
            appWidgetId = appWidgetId,
            openWidgetSheet = openWidgetSheet
        )
    }
}

private fun NavGraphBuilder.aboutScreenRoute(onBackClick: () -> Unit) {
    composable<Screen.About> {
        AboutScreen(onBackClick = onBackClick)
    }
}