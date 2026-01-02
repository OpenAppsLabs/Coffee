package com.openappslabs.coffee.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.openappslabs.coffee.ui.screens.aboutscreen.AboutScreen
import com.openappslabs.coffee.ui.screens.homescreen.HomeScreen

@Composable
fun AppNavGraph(
    navController: NavHostController = rememberNavController()
) {
    val onAboutClick = remember(navController) {
        {
            if (navController.currentBackStackEntry?.lifecycle?.currentState?.isAtLeast(
                    androidx.lifecycle.Lifecycle.State.RESUMED
                ) == true
            ) {
                navController.navigate(Screen.About) {
                    launchSingleTop = true
                }
            }
        }
    }

    val onBackClick = remember(navController) {
        {
            navController.popBackStack()
            Unit
        }
    }

    NavHost(
        navController = navController,
        startDestination = Screen.Home
    ) {
        homeScreenRoute(onAboutClick = onAboutClick)
        aboutScreenRoute(onBackClick = onBackClick)
    }
}

private fun NavGraphBuilder.homeScreenRoute(onAboutClick: () -> Unit) {
    composable<Screen.Home> {
        HomeScreen(onAboutClick = onAboutClick)
    }
}

private fun NavGraphBuilder.aboutScreenRoute(onBackClick: () -> Unit) {
    composable<Screen.About> {
        AboutScreen(onBackClick = onBackClick)
    }
}