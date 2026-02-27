package com.fitness.app.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitness.app.domain.repository.SleepDayUiModel
import com.fitness.app.domain.repository.SleepRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class SleepUiState(
    val last7Days: List<SleepDayUiModel> = emptyList(),
    val weeklyTotal: Double = 0.0,
    val average: Double = 0.0,
    val bestNight: Double = 0.0,
    val worstNight: Double = 0.0,
    val consistencyCount: Int = 0,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

class SleepTrackerViewModel(
    private val repository: SleepRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SleepUiState())
    val uiState: StateFlow<SleepUiState> = _uiState.asStateFlow()

    init {
        // Load initially for today
        refreshStats()
    }

    fun logSleep(date: String, sleepHours: Double) {
        val today = LocalDate.now()
        val selectedDate = LocalDate.parse(date, DateTimeFormatter.ISO_LOCAL_DATE)

        // Validation 1: Block future dates
        if (selectedDate.isAfter(today)) {
            _uiState.update { it.copy(
                errorMessage = "You cannot log sleep for a future date.",
                successMessage = null
            ) }
            return
        }

        // Validation 2: Hours between 0 and 24
        if (sleepHours < 0.0 || sleepHours > 24.0) {
            _uiState.update { it.copy(
                errorMessage = "Sleep duration must be between 0 and 24 hours.",
                successMessage = null
            ) }
            return
        }

        viewModelScope.launch {
            try {
                repository.logSleep(date, sleepHours)
                
                // Clear errors and show success message
                _uiState.update { it.copy(
                    successMessage = "Sleep updated for this date",
                    errorMessage = null
                ) }

                // Refresh the whole 7-day stats
                refreshStats()
            } catch (e: Exception) {
                _uiState.update { it.copy(
                    errorMessage = "Failed to save sleep data: ${e.message}",
                    successMessage = null
                ) }
            }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }

    private fun refreshStats() {
        viewModelScope.launch {
            try {
                val todayStr = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
                val stats = repository.getWeeklySleepStats(todayStr)
                
                _uiState.update { 
                    it.copy(
                        last7Days = stats.last7Days,
                        average = stats.average,
                        bestNight = stats.bestNight,
                        worstNight = stats.worstNight,
                        weeklyTotal = stats.weeklyTotal,
                        consistencyCount = stats.consistencyCount
                    ) 
                }
            } catch (e: Exception) {
                // If it fails (e.g. offline or DB error), we just keep current state
                // and maybe log an error.
            }
        }
    }

    companion object {
        fun provideFactory(
            repository: SleepRepository
        ): androidx.lifecycle.ViewModelProvider.Factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(SleepTrackerViewModel::class.java)) {
                    return SleepTrackerViewModel(repository) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }
    }
}
