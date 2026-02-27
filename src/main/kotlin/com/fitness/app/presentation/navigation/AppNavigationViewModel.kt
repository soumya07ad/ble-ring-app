package com.fitness.app.presentation.navigation

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * UI State for app-level navigation
 */
data class AppNavigationUiState(
    val userLoggedIn: Boolean = false,
    val setupComplete: Boolean = false
)

/**
 * ViewModel managing app-level navigation state.
 * Replaces the legacy AppState singleton with proper MVVM state management.
 */
class AppNavigationViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(AppNavigationUiState())
    val uiState: StateFlow<AppNavigationUiState> = _uiState.asStateFlow()

    fun onLoginSuccess() {
        _uiState.update { it.copy(userLoggedIn = true, setupComplete = false) }
    }

    fun onSetupComplete() {
        _uiState.update { it.copy(setupComplete = true) }
    }

    fun onSkip() {
        _uiState.update { it.copy(setupComplete = true) }
    }

    fun onLogout() {
        _uiState.update { AppNavigationUiState() }
    }
}
