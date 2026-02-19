package com.rafgittools.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.rafgittools.presentation.auth.CredentialScreen
import com.rafgittools.presentation.auth.CredentialViewModel

@Composable
fun AppNavHost(vm: CredentialViewModel) {
    val navController = rememberNavController()
    val isAuthenticated by vm.isAuthenticated.collectAsState()
    val startDestination = if (isAuthenticated) "main" else "auth"

    NavHost(navController = navController, startDestination = "credentials") {
        composable("credentials") {
            CredentialScreen(vm)
        }
    }
}
