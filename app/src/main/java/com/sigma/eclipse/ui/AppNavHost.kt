package com.sigma.eclipse.ui

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.sigma.eclipse.browser.BrowserScreen

@Composable
fun AppNavHost() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "browser") {
        composable("browser") {
            BrowserScreen(
                onNavigateToSettings = { navController.navigate("settings") }
            )
        }
        composable("settings") {
            // SettingsScreen(onNavigateBack = { navController.popBackStack() })
        }
    }
}
