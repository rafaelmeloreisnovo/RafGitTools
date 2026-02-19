package com.rafgittools.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.rafgittools.ui.screens.auth.AuthScreen
import com.rafgittools.ui.screens.auth.AuthViewModel

@Composable
fun AppNavHost(vm: AuthViewModel) {
    val navController = rememberNavController()
    val isAuthenticated by vm.isAuthenticated.collectAsState()
    val startDestination = if (isAuthenticated) "main" else "auth"

    NavHost(navController, startDestination = startDestination) {
        composable("auth") {
            AuthScreen(viewModel = vm) {
                navController.navigate("main") {
                    popUpTo("auth") { inclusive = true }
                    launchSingleTop = true
                }
            }
        }
        composable("main") {
            // Placeholder main screen
        }
    }
}
