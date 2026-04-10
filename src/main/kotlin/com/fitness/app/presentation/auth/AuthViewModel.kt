package com.fitness.app.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitness.app.domain.repository.IAuthRepository
import com.fitness.app.network.auth.TokenManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Success(val message: String? = null) : AuthState()
    data class Error(val message: String) : AuthState()
}

class AuthViewModel(
    private val authRepository: IAuthRepository,
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    val currentUser: StateFlow<FirebaseUser?> = MutableStateFlow(authRepository.getCurrentUser()).apply {
        viewModelScope.launch {
            authRepository.currentUser.collect { user ->
                value = user
            }
        }
    }

    private suspend fun persistTokenFromUser(user: FirebaseUser?) {
        user?.let {
            try {
                val tokenResult = it.getIdToken(true).await()
                tokenResult.token?.let { token ->
                    tokenManager.saveToken(token)
                    tokenManager.saveUserInfo(it.uid, it.email ?: it.phoneNumber ?: "")
                }
            } catch (e: Exception) {
                // Token capture failed, but session might still be active
            }
        }
    }

    fun signIn(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _authState.value = AuthState.Error("Email and password cannot be empty")
            return
        }
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val result = authRepository.signIn(email, password)
            result.onSuccess {
                persistTokenFromUser(authRepository.getCurrentUser())
                _authState.value = AuthState.Success("Logged in successfully")
            }.onFailure {
                _authState.value = AuthState.Error(it.message ?: "Authentication failed")
            }
        }
    }

    fun signUp(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _authState.value = AuthState.Error("Email and password cannot be empty")
            return
        }
        if (password.length < 6) {
            _authState.value = AuthState.Error("Password should be at least 6 characters")
            return
        }
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val result = authRepository.signUp(email, password)
            result.onSuccess {
                persistTokenFromUser(authRepository.getCurrentUser())
                _authState.value = AuthState.Success("Account created successfully")
            }.onFailure {
                _authState.value = AuthState.Error(it.message ?: "Sign up failed")
            }
        }
    }

    /**
     * Authenticates with Firebase using the Google ID token.
     * Called after the Google Sign-In intent returns a valid account.
     */
    fun signInWithGoogle(idToken: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val credential = GoogleAuthProvider.getCredential(idToken, null)
                val authResult = FirebaseAuth.getInstance().signInWithCredential(credential).await()
                persistTokenFromUser(authResult.user)
                _authState.value = AuthState.Success("Signed in with Google")
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Google Sign-In failed")
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
            tokenManager.clearToken()
            _authState.value = AuthState.Idle
        }
    }
    
    fun resetState() {
        _authState.value = AuthState.Idle
    }
}
