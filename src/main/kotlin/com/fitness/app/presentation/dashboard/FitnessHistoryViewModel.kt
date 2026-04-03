package com.fitness.app.presentation.dashboard

import androidx.lifecycle.ViewModel
import com.fitness.app.domain.model.FitnessHistoryEntry
import com.fitness.app.domain.repository.IFitnessRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class FitnessHistoryUiState(
    val history: List<FitnessHistoryEntry> = emptyList(),
    val last7Days: List<FitnessHistoryEntry> = emptyList(),
    val isLoading: Boolean = true
)

class FitnessHistoryViewModel(
    private val fitnessRepository: IFitnessRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FitnessHistoryUiState())
    val uiState: StateFlow<FitnessHistoryUiState> = _uiState.asStateFlow()

    init {
        loadHistory()
    }

    private fun loadHistory() {
        val history = fitnessRepository.getFitnessHistory(30)
        val last7 = history.take(7).reversed() // oldest first for the graph
        _uiState.value = FitnessHistoryUiState(
            history = history,
            last7Days = last7,
            isLoading = false
        )
    }
}
