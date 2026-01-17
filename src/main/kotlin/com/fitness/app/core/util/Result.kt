package com.fitness.app.core.util

/**
 * A generic class that holds a value with its loading status.
 * Used to wrap data responses from repository operations.
 */
sealed class Result<out T> {
    
    /**
     * Successful result containing data
     */
    data class Success<T>(val data: T) : Result<T>()
    
    /**
     * Error result with message and optional exception
     */
    data class Error(
        val message: String,
        val exception: Throwable? = null
    ) : Result<Nothing>()
    
    /**
     * Loading state
     */
    object Loading : Result<Nothing>()
    
    /**
     * Check if result is successful
     */
    val isSuccess: Boolean get() = this is Success
    
    /**
     * Check if result is error
     */
    val isError: Boolean get() = this is Error
    
    /**
     * Check if result is loading
     */
    val isLoading: Boolean get() = this is Loading
    
    /**
     * Get data or null
     */
    fun getOrNull(): T? = (this as? Success)?.data
    
    /**
     * Get data or throw exception
     */
    fun getOrThrow(): T = when (this) {
        is Success -> data
        is Error -> throw exception ?: IllegalStateException(message)
        is Loading -> throw IllegalStateException("Result is still loading")
    }
    
    /**
     * Get data or default value
     */
    fun getOrDefault(default: @UnsafeVariance T): T = (this as? Success)?.data ?: default
    
    /**
     * Transform successful result
     */
    inline fun <R> map(transform: (T) -> R): Result<R> = when (this) {
        is Success -> Success(transform(data))
        is Error -> this
        is Loading -> this
    }
    
    /**
     * Handle result with callbacks
     */
    inline fun onSuccess(action: (T) -> Unit): Result<T> {
        if (this is Success) action(data)
        return this
    }
    
    inline fun onError(action: (String, Throwable?) -> Unit): Result<T> {
        if (this is Error) action(message, exception)
        return this
    }
    
    inline fun onLoading(action: () -> Unit): Result<T> {
        if (this is Loading) action()
        return this
    }
    
    companion object {
        /**
         * Create a success result
         */
        fun <T> success(data: T): Result<T> = Success(data)
        
        /**
         * Create an error result
         */
        fun error(message: String, exception: Throwable? = null): Result<Nothing> = 
            Error(message, exception)
        
        /**
         * Create a loading result
         */
        fun loading(): Result<Nothing> = Loading
        
        /**
         * Execute a suspending block and wrap result
         */
        suspend fun <T> runCatching(block: suspend () -> T): Result<T> {
            return try {
                Success(block())
            } catch (e: Exception) {
                Error(e.message ?: "Unknown error", e)
            }
        }
    }
}
