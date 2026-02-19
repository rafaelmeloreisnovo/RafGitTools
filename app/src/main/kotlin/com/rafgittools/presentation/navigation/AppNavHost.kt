package com.rafgittools.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.rafgittools.ui.screens.auth.AuthScreen
import com.rafgittools.ui.screens.auth.AuthViewModel

@Composable
fun AppNavHost(vm: AuthViewModel = hiltViewModel()) {
    val navController = rememberNavController()

    NavHost(navController, startDestination = if (vm.isAuthenticated.value) "main" else "login") {
        composable("login") {
            AuthScreen(
                viewModel = vm,
                onAuthSuccess = { navController.navigate("main") }
            )
        }
        composable("main") {
            // Placeholder main screen
        }
    }
}
