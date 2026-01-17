package com.fitness.app.core.base

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Base ViewModel with common functionality for error handling and state management
 */
abstract class BaseViewModel<S>(initialState: S) : ViewModel() {
    
    /**
     * Current UI state
     */
    protected val _uiState = MutableStateFlow(initialState)
    val uiState: StateFlow<S> = _uiState.asStateFlow()
    
    /**
     * Current state value
     */
    protected val currentState: S get() = _uiState.value
    
    /**
     * Update UI state
     */
    protected fun updateState(update: S.() -> S) {
        _uiState.value = currentState.update()
    }
    
    /**
     * Error handler for coroutines
     */
    private val errorHandler = CoroutineExceptionHandler { _, throwable ->
        handleError(throwable)
    }
    
    /**
     * Override to handle errors in subclasses
     */
    protected open fun handleError(throwable: Throwable) {
        // Default implementation - can be overridden
        throwable.printStackTrace()
    }
    
    /**
     * Launch a coroutine with error handling
     */
    protected fun launchSafe(block: suspend CoroutineScope.() -> Unit) {
        viewModelScope.launch(errorHandler) {
            block()
        }
    }
    
    /**
     * Launch a coroutine with custom error handling
     */
    protected fun launchWithErrorHandler(
        onError: (Throwable) -> Unit = ::handleError,
        block: suspend CoroutineScope.() -> Unit
    ) {
        viewModelScope.launch(CoroutineExceptionHandler { _, throwable -> onError(throwable) }) {
            block()
        }
    }
}
