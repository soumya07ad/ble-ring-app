package com.fitness.app.presentation.wellness

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitness.app.domain.model.ActiveTimer
import com.fitness.app.domain.model.Emotion
import com.fitness.app.domain.model.MeditationItem
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.max

data class WellnessUiState(
    val selectedEmotion: String? = null,
    val avgMood: Int = 78,
    val stressLevel: Int = 35,
    val wellnessScore: Int = 82,
    val emotions: List<Emotion> = defaultEmotions,
    val meditations: List<MeditationItem> = defaultMeditations,
    val activeTimer: ActiveTimer? = null
)

private val defaultEmotions = listOf(
    Emotion("Happy", "😊"),
    Emotion("Calm", "😌"),
    Emotion("Excited", "🤩"),
    Emotion("Grateful", "🙏"),
    Emotion("Anxious", "😰"),
    Emotion("Sad", "😢"),
    Emotion("Frustrated", "😤"),
    Emotion("Peaceful", "🕊️")
)

private val defaultMeditations = listOf(
    MeditationItem("1", "Morning Calm", "5 min", 5, "🌅"),
    MeditationItem("2", "Breathing Exercise", "10 min", 10, "🌬️"),
    MeditationItem("3", "Sleep Meditation", "15 min", 15, "🌙")
)

class WellnessViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(WellnessUiState())
    val uiState: StateFlow<WellnessUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null

    fun selectEmotion(emotion: String) {
        _uiState.update {
            it.copy(selectedEmotion = if (it.selectedEmotion == emotion) null else emotion)
        }
    }

    fun startMeditation(meditation: MeditationItem) {
        stopTimer()
        val totalSecs = meditation.durationMinutes * 60
        _uiState.update {
            it.copy(
                activeTimer = ActiveTimer(
                    meditationId = meditation.id,
                    title = meditation.title,
                    remainingSeconds = totalSecs,
                    totalSeconds = totalSecs,
                    isRunning = true
                )
            )
        }
        runTimer()
    }

    fun pauseTimer() {
        timerJob?.cancel()
        _uiState.update { state ->
            state.activeTimer?.let {
                state.copy(activeTimer = it.copy(isRunning = false))
            } ?: state
        }
    }

    fun resumeTimer() {
        _uiState.update { state ->
            state.activeTimer?.let {
                state.copy(activeTimer = it.copy(isRunning = true))
            } ?: state
        }
        runTimer()
    }

    fun stopTimer() {
        timerJob?.cancel()
        _uiState.update { it.copy(activeTimer = null) }
    }

    private fun runTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                val timer = _uiState.value.activeTimer ?: break
                if (!timer.isRunning) break

                val newRemaining = max(0, timer.remainingSeconds - 1)
                if (newRemaining == 0) {
                    _uiState.update {
                        it.copy(activeTimer = timer.copy(
                            remainingSeconds = 0,
                            isRunning = false,
                            isCompleted = true
                        ))
                    }
                    delay(3000)
                    _uiState.update { it.copy(activeTimer = null) }
                    break
                } else {
                    _uiState.update {
                        it.copy(activeTimer = timer.copy(remainingSeconds = newRemaining))
                    }
                }
            }
        }
    }

    fun formatTime(seconds: Int): String {
        val m = seconds / 60
        val s = seconds % 60
        return "%02d:%02d".format(m, s)
    }

    override fun onCleared() {
        super.onCleared()
        stopTimer()
    }
}
