package com.fitness.app.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitness.app.data.repository.FitnessHistoryRepository
import com.fitness.app.data.local.entity.DailyFitnessRecord
import com.fitness.app.domain.model.FitnessHistoryEntry
import kotlinx.coroutines.flow.*
import java.time.format.DateTimeFormatter
import java.time.LocalDate

data class FitnessHistoryUiState(
    val history: List<FitnessHistoryEntry> = emptyList(),
    val last7Days: List<FitnessHistoryEntry> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

class FitnessHistoryViewModel(
    private val fitnessHistoryRepository: FitnessHistoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FitnessHistoryUiState())
    val uiState: StateFlow<FitnessHistoryUiState> = _uiState.asStateFlow()

    private val dateFormatter = DateTimeFormatter.ofPattern("dd MMM")

    init {
        loadHistory()
    }

    private fun loadHistory() {
        fitnessHistoryRepository.getHistoryFlow()
            .onEach { records ->
                val entries = records.map { it.toUiEntry() }
                val last7 = entries.take(7).reversed() // oldest first for charts
                
                _uiState.update { 
                    it.copy(
                        history = entries,
                        last7Days = last7,
                        isLoading = false
                    )
                }
            }
            .catch { e ->
                _uiState.update { it.copy(errorMessage = e.message, isLoading = false) }
            }
            .launchIn(viewModelScope)
    }

    private fun DailyFitnessRecord.toUiEntry(): FitnessHistoryEntry {
        val dateObj = LocalDate.parse(date)
        return FitnessHistoryEntry(
            dateObj.format(dateFormatter),
            steps,
            distanceMeters.toInt(),
            calories.toInt()
        )
    }

    fun retrySync() {
        fitnessHistoryRepository.syncWithHealthConnect()
    }
}
