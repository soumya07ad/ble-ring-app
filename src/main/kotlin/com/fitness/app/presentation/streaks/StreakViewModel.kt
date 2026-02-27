package com.fitness.app.presentation.streaks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitness.app.domain.model.ActivityStreakData
import com.fitness.app.domain.model.MilestoneData
import com.fitness.app.domain.repository.IStreakRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class StreakUiState(
    val longestStreakActivity: String? = null,
    val longestStreakCount: Int = 0,
    val activityStreaks: List<ActivityStreakData> = emptyList(),
    val milestoneProgress: List<MilestoneData> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

class StreakViewModel(private val streakRepository: IStreakRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(StreakUiState())
    val uiState: StateFlow<StreakUiState> = _uiState.asStateFlow()

    init {
        loadStreaks()
    }

    fun loadStreaks() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val streaks = streakRepository.getAllActivityStreaks()
                val (longestActivity, longestCount) = streakRepository.getLongestCurrentStreak()
                val milestones = streakRepository.getMilestoneProgress(longestCount)

                _uiState.update {
                    it.copy(
                        longestStreakActivity = longestActivity.takeIf { a -> a.isNotEmpty() },
                        longestStreakCount = longestCount,
                        activityStreaks = streaks,
                        milestoneProgress = milestones,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
            }
        }
    }

    fun logActivity(activityType: String, date: LocalDate) {
        viewModelScope.launch {
            try {
                if (date.isAfter(LocalDate.now())) {
                    _uiState.update { it.copy(errorMessage = "Cannot log future dates") }
                    return@launch
                }
                streakRepository.markActivityCompleted(
                    activityType = activityType,
                    date = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
                )
                loadStreaks()
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message) }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
