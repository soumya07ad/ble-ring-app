package com.fitness.app.presentation.wellness

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitness.app.domain.model.MeditationData
import com.fitness.app.domain.model.MeditationExercise
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.max

data class MeditationTimerState(
    val exercise: MeditationExercise? = null,
    val remainingSeconds: Int = 0,
    val totalSeconds: Int = 0,
    val isRunning: Boolean = false,
    val isPaused: Boolean = false,
    val isCompleted: Boolean = false,
    val customDurationMinutes: Int = 5,
    val selectedDurationMinutes: Int = 5
) {
    val progress: Float
        get() = if (totalSeconds > 0) (totalSeconds - remainingSeconds).toFloat() / totalSeconds else 0f

    val formattedTime: String
        get() {
            val m = remainingSeconds / 60
            val s = remainingSeconds % 60
            return "%02d:%02d".format(m, s)
        }
}

class MeditationViewModel : ViewModel() {

    private val _timerState = MutableStateFlow(MeditationTimerState())
    val timerState: StateFlow<MeditationTimerState> = _timerState.asStateFlow()

    private var timerJob: Job? = null

    fun loadExercise(exerciseId: String) {
        val exercise = MeditationData.findExercise(exerciseId) ?: return
        val totalSecs = exercise.durationMinutes * 60
        _timerState.value = MeditationTimerState(
            exercise = exercise,
            remainingSeconds = totalSecs,
            totalSeconds = totalSecs,
            selectedDurationMinutes = exercise.durationMinutes
        )
    }

    fun setDuration(minutes: Int) {
        val totalSecs = minutes * 60
        _timerState.update {
            it.copy(
                remainingSeconds = totalSecs,
                totalSeconds = totalSecs,
                selectedDurationMinutes = minutes,
                isCompleted = false
            )
        }
    }

    fun startTimer() {
        _timerState.update { it.copy(isRunning = true, isPaused = false) }
        runTimer()
    }

    fun startCustomTimer(durationMinutes: Int, category: String) {
        val exercise = MeditationExercise(
            id = "custom",
            name = "Custom Session",
            description = "Your personal meditation session",
            durationMinutes = durationMinutes,
            emoji = MeditationData.categoryEmoji(category),
            category = category
        )
        val totalSecs = durationMinutes * 60
        _timerState.value = MeditationTimerState(
            exercise = exercise,
            remainingSeconds = totalSecs,
            totalSeconds = totalSecs,
            isRunning = true
        )
        runTimer()
    }

    fun pauseTimer() {
        timerJob?.cancel()
        _timerState.update { it.copy(isRunning = false, isPaused = true) }
    }

    fun resumeTimer() {
        _timerState.update { it.copy(isRunning = true, isPaused = false) }
        runTimer()
    }

    fun stopTimer() {
        timerJob?.cancel()
        _timerState.update {
            MeditationTimerState(
                exercise = it.exercise,
                remainingSeconds = it.totalSeconds,
                totalSeconds = it.totalSeconds
            )
        }
    }

    fun setCustomDuration(minutes: Int) {
        _timerState.update { it.copy(customDurationMinutes = minutes.coerceIn(1, 60)) }
    }

    private fun runTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                val state = _timerState.value
                if (!state.isRunning) break

                val newRemaining = max(0, state.remainingSeconds - 1)
                if (newRemaining == 0) {
                    _timerState.update {
                        it.copy(
                            remainingSeconds = 0,
                            isRunning = false,
                            isCompleted = true
                        )
                    }
                    break
                } else {
                    _timerState.update { it.copy(remainingSeconds = newRemaining) }
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}
