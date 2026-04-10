package com.fitness.app.presentation.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitness.app.network.auth.TokenManager
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * UI State for app-level navigation
 */
data class AppNavigationUiState(
    val userLoggedIn: Boolean = false,
    val setupComplete: Boolean = false,
    val isLoading: Boolean = true
)

/**
 * ViewModel managing app-level navigation state.
 * Replaces the legacy AppState singleton with proper MVVM state management.
 */
class AppNavigationViewModel(
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(AppNavigationUiState())
    val uiState: StateFlow<AppNavigationUiState> = _uiState.asStateFlow()

    init {
        // Load persistent state on startup
        viewModelScope.launch {
            val storedToken = tokenManager.getToken()
            val firebaseUser = FirebaseAuth.getInstance().currentUser
            
            // Auto-recovery: If firebase is logged in but storage is empty
            if (storedToken.isNullOrEmpty() && firebaseUser != null) {
                try {
                    val tokenResult = firebaseUser.getIdToken(true).await()
                    tokenResult.token?.let { newToken ->
                        tokenManager.saveToken(newToken)
                        tokenManager.saveUserInfo(firebaseUser.uid, firebaseUser.email ?: "")
                    }
                } catch (e: Exception) {
                    // Recovery failed, stay on login
                }
            }
            
            val finalToken = tokenManager.getToken()
            val setupCompleteStatus = tokenManager.setupCompleteFlow.first()
            
            _uiState.update { 
                it.copy(
                    userLoggedIn = !finalToken.isNullOrEmpty(),
                    setupComplete = setupCompleteStatus,
                    isLoading = false
                )
            }
        }
    }

    fun onLoginSuccess() {
        _uiState.update { it.copy(userLoggedIn = true, setupComplete = false) }
    }

    fun onSetupComplete() {
        viewModelScope.launch {
            tokenManager.setSetupComplete(true)
            _uiState.update { it.copy(setupComplete = true) }
        }
    }

    fun onSkip() {
        viewModelScope.launch {
            tokenManager.setSetupComplete(true)
            _uiState.update { it.copy(setupComplete = true) }
        }
    }

    fun onLogout() {
        viewModelScope.launch {
            tokenManager.clearToken()
            _uiState.update { AppNavigationUiState(isLoading = false) }
        }
    }
}
