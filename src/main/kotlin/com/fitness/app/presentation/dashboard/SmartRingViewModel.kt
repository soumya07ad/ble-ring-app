package com.fitness.app.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitness.app.domain.model.ConnectionStatus
import com.fitness.app.domain.model.RingConnectionState
import com.fitness.app.domain.repository.IRingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for Smart Ring connection management on the Dashboard.
 *
 * Observes the ring repository's connection status and exposes a simplified
 * RingConnectionState for the UI. Provides a disconnect action.
 */
class SmartRingViewModel(
    private val ringRepository: IRingRepository
) : ViewModel() {

    private val _connectionState = MutableStateFlow(RingConnectionState.DISCONNECTED)
    val connectionState: StateFlow<RingConnectionState> = _connectionState.asStateFlow()

    init {
        observeConnectionStatus()
    }

    /**
     * Observe the repository's connection status and map it to the simplified enum.
     */
    private fun observeConnectionStatus() {
        viewModelScope.launch {
            ringRepository.connectionStatus.collect { status ->
                _connectionState.value = when (status) {
                    is ConnectionStatus.Connected -> RingConnectionState.CONNECTED
                    else -> RingConnectionState.DISCONNECTED
                }
            }
        }
    }

    /**
     * Disconnect the ring:
     * - Stops BLE communication via the repository
     * - Resets connection state to DISCONNECTED
     */
    fun disconnectRing() {
        viewModelScope.launch {
            ringRepository.disconnect()
            _connectionState.value = RingConnectionState.DISCONNECTED
        }
    }
}
