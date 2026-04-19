package com.openappslabs.coffee.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.openappslabs.coffee.data.CoffeeDataStore
import com.openappslabs.coffee.ui.screens.aboutscreen.AboutScreen
import com.openappslabs.coffee.ui.screens.homescreen.HomeScreen
import com.openappslabs.coffee.ui.screens.onboardingscreen.OnboardingScreen

@Composable
fun AppNavGraph(
    dataStore: CoffeeDataStore,
    navController: NavHostController = rememberNavController(),
    appWidgetId: Int,
    openWidgetSheet: Boolean = false,
    initialVariant: String = "Normal"
) {
    var startDestination by remember { mutableStateOf<Screen?>(null) }

    LaunchedEffect(dataStore) {
        startDestination = if (dataStore.getOnboardingComplete()) {
            Screen.Home
        } else {
            Screen.Onboarding
        }
    }

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
        { navController.popBackStack(); Unit }
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
                openWidgetSheet = openWidgetSheet,
                initialVariant = initialVariant
            )

            aboutScreenRoute(onBackClick = onBackClick)
        }
    }
}

private fun NavGraphBuilder.homeScreenRoute(
    onAboutClick: () -> Unit,
    appWidgetId: Int,
    openWidgetSheet: Boolean,
    initialVariant: String
) {
    composable<Screen.Home> {
        HomeScreen(
            onAboutClick = onAboutClick,
            appWidgetId = appWidgetId,
            openWidgetSheet = openWidgetSheet,
            initialVariant = initialVariant
        )
    }
}

private fun NavGraphBuilder.aboutScreenRoute(onBackClick: () -> Unit) {
    composable<Screen.About> {
        AboutScreen(onBackClick = onBackClick)
    }
}