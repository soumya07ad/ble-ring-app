package com.fitness.app.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitness.app.domain.model.ConnectionStatus
import com.fitness.app.domain.repository.IFitnessRepository
import com.fitness.app.domain.repository.IRingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel for the Dashboard screen.
 *
 * Combines data from IRingRepository (live ring hardware data)
 * and IFitnessRepository (local fitness data) into a single DashboardUiState.
 *
 * Dependencies are constructor-injected via AppViewModelFactory.
 */
class DashboardViewModel(
    private val ringRepository: IRingRepository,
    private val fitnessRepository: IFitnessRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        observeRingState()
        loadFitnessData()
    }

    /**
     * Observe ring repository flows and merge into dashboard state.
     */
    private fun observeRingState() {
        viewModelScope.launch {
            ringRepository.connectionStatus.collect { status ->
                _uiState.update { state ->
                    when (status) {
                        is ConnectionStatus.Connected -> state.copy(
                            isConnected = true,
                            connectedRing = status.ring
                        )
                        is ConnectionStatus.Disconnected -> state.copy(
                            isConnected = false,
                            connectedRing = null
                        )
                        is ConnectionStatus.Connecting -> state.copy(
                            isConnected = false
                        )
                        is ConnectionStatus.Error -> state.copy(
                            isConnected = false,
                            errorMessage = status.message
                        )
                        is ConnectionStatus.Timeout -> state.copy(
                            isConnected = false,
                            errorMessage = "Connection timed out"
                        )
                    }
                }
            }
        }

        viewModelScope.launch {
            ringRepository.ringData.collect { ringData ->
                _uiState.update { state ->
                    state.copy(
                        heartRate = ringData.heartRate,
                        spO2 = ringData.spO2,
                        steps = ringData.steps,
                        distance = ringData.distance,
                        calories = ringData.calories,
                        stressLevel = ringData.stress,
                        batteryLevel = ringData.battery
                    )
                }
            }
        }
    }

    /**
     * Load local fitness data (daily summary, etc.)
     */
    private fun loadFitnessData() {
        viewModelScope.launch {
            try {
                val summary = fitnessRepository.getDailySummary()
                _uiState.update { state ->
                    state.copy(dailySummary = summary)
                }
            } catch (e: Exception) {
                // Non-critical, dashboard still works with ring data
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
