package com.fitness.app

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.State

// Data class for discovered Bluetooth device
data class BluetoothDevice(
    val name: String,
    val address: String,
    val signalStrength: Int = -50 // dBm
)

// Data class for paired smart ring
data class SmartRing(
    val name: String,
    val macAddress: String,
    val batteryLevel: Int = 85,
    val isConnected: Boolean = false,
    val lastSync: String = "Just now"
)

// Shared app state
object AppState {
    private val _pairedRing = mutableStateOf<SmartRing?>(null)
    val pairedRing: State<SmartRing?> = _pairedRing
    
    private val _stressLevel = mutableStateOf(45)
    val stressLevel: State<Int> = _stressLevel
    
    private val _userLoggedIn = mutableStateOf(false)
    val userLoggedIn: State<Boolean> = _userLoggedIn
    
    private val _setupComplete = mutableStateOf(false)
    val setupComplete: State<Boolean> = _setupComplete
    
    fun setPairedRing(ring: SmartRing?) {
        _pairedRing.value = ring
    }
    
    fun setStressLevel(level: Int) {
        _stressLevel.value = level.coerceIn(0, 100)
    }
    
    fun setUserLoggedIn(loggedIn: Boolean) {
        _userLoggedIn.value = loggedIn
    }
    
    fun setSetupComplete(complete: Boolean) {
        _setupComplete.value = complete
    }
    
    // Simulate stress level updates
    fun updateStressLevel() {
        val current = _stressLevel.value
        val change = (-20..20).random()
        _stressLevel.value = (current + change).coerceIn(0, 100)
    }
}
