package com.fitness.app.network.ui

import com.fitness.app.network.models.ErrorResponse

// UI State for managing loading, success, and error states
sealed class UiState<T> {
    class Loading<T> : UiState<T>()
    data class Success<T>(val data: T) : UiState<T>()
    data class Error<T>(val message: String, val code: Int = 0, val throwable: Throwable? = null) : UiState<T>()
    class Idle<T> : UiState<T>()
}

// Data class for API request state
data class ApiRequestState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val success: Boolean = false
)

// Toast messages
sealed class UiEvent {
    data class ShowToast(val message: String, val isError: Boolean = false) : UiEvent()
    data class ShowSnackbar(val message: String) : UiEvent()
    object NavigateToHome : UiEvent()
    object NavigateToLogin : UiEvent()
    object HideLoading : UiEvent()
}
