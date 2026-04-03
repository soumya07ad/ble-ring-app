package com.fitness.app.presentation.auth.screens

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.fitness.app.presentation.auth.AuthViewModel

@Composable
fun AuthNavGraph(
    viewModel: AuthViewModel,
    onAuthSuccess: () -> Unit
) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "login") {
        composable("login") {
            LoginScreen(
                viewModel = viewModel,
                onLoginSuccess = onAuthSuccess,
                onNavigateToSignUp = { navController.navigate("signup") }
            )
        }
        composable("signup") {
            SignUpScreen(
                viewModel = viewModel,
                onSignUpSuccess = onAuthSuccess,
                onNavigateToLogin = { navController.popBackStack() }
            )
        }
    }
}
