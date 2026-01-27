package com.rafgittools.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.*
import com.rafgittools.presentation.auth.AuthViewModel
import com.rafgittools.presentation.auth.LoginScreen

@Composable
fun AppNavHost(vm: AuthViewModel) {
    val navController = rememberNavController()

    NavHost(navController, startDestination = if (vm.hasSession()) "main" else "login") {
        composable("login") {
            LoginScreen(vm) { navController.navigate("main") }
        }
        composable("main") {
            // Placeholder main screen
        }
    }
}
