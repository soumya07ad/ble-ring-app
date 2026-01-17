package com.fitness.app.domain.model

/**
 * Represents the connection status of the ring
 */
sealed class ConnectionStatus {
    /**
     * Not connected to any device
     */
    object Disconnected : ConnectionStatus()
    
    /**
     * Currently attempting to connect
     */
    object Connecting : ConnectionStatus()
    
    /**
     * Successfully connected to a device
     */
    data class Connected(val ring: Ring) : ConnectionStatus()
    
    /**
     * Connection failed with an error
     */
    data class Error(val message: String) : ConnectionStatus()
    
    /**
     * Connection timed out
     */
    object Timeout : ConnectionStatus()
    
    /**
     * Check if currently connected
     */
    val isConnected: Boolean get() = this is Connected
    
    /**
     * Check if connecting
     */
    val isConnecting: Boolean get() = this is Connecting
    
    /**
     * Check if disconnected
     */
    val isDisconnected: Boolean get() = this is Disconnected
    
    /**
     * Check if error occurred
     */
    val isError: Boolean get() = this is Error
}
