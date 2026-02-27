package com.fitness.app.presentation.meditation

import android.app.Application
import android.media.MediaPlayer
import android.media.RingtoneManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fitness.app.core.di.AppContainer
import com.fitness.app.domain.model.ActiveMeditation
import com.fitness.app.domain.model.MeditationSession
import com.fitness.app.domain.repository.IMeditationRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.max

/**
 * UI State for the Meditation screen.
 */
data class MeditationUiState(
    val sessions: List<MeditationSession> = emptyList(),
    val activeMeditation: ActiveMeditation? = null,
    val totalMinutes: Int = 0,
    val sessionsCompleted: Int = 0,
    val isLoading: Boolean = false
)

/**
 * ViewModel for managing meditation sessions and timer logic.
 */
class MeditationViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: IMeditationRepository = 
        AppContainer.getInstance(application).meditationLocalRepository

    private val _uiState = MutableStateFlow(MeditationUiState())
    val uiState: StateFlow<MeditationUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null
    private var mediaPlayer: MediaPlayer? = null

    init {
        loadData()
    }

    private fun loadData() {
        _uiState.update { it.copy(isLoading = true, sessions = repository.getSessions()) }
        
        viewModelScope.launch {
            repository.getTotalMinutes().collect { minutes ->
                _uiState.update { it.copy(totalMinutes = minutes) }
            }
        }

        viewModelScope.launch {
            repository.getSessionsCompletedCount().collect { count ->
                _uiState.update { it.copy(sessionsCompleted = count) }
            }
        }
        
        _uiState.update { it.copy(isLoading = false) }
    }

    fun startMeditation(session: MeditationSession) {
        stopMeditation()
        
        val initialActive = ActiveMeditation(
            session = session,
            remainingSeconds = session.durationSeconds,
            totalSeconds = session.durationSeconds,
            isRunning = true,
            isCompleted = false,
            progress = 0f
        )
        
        _uiState.update { it.copy(activeMeditation = initialActive) }
        startTimer()
    }

    fun resumeMeditation() {
        val current = _uiState.value.activeMeditation ?: return
        if (current.isRunning) return

        _uiState.update { it.copy(activeMeditation = current.copy(isRunning = true)) }
        startTimer()
    }

    fun pauseMeditation() {
        timerJob?.cancel()
        val current = _uiState.value.activeMeditation ?: return
        _uiState.update { it.copy(activeMeditation = current.copy(isRunning = false)) }
    }

    fun stopMeditation() {
        timerJob?.cancel()
        _uiState.update { it.copy(activeMeditation = null) }
        stopSound()
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                val current = _uiState.value.activeMeditation ?: break
                if (!current.isRunning) continue

                val newRemaining = max(0, current.remainingSeconds - 1)
                val progress = (current.totalSeconds - newRemaining).toFloat() / current.totalSeconds

                if (newRemaining == 0) {
                    _uiState.update { 
                        it.copy(activeMeditation = current.copy(
                            remainingSeconds = 0,
                            isRunning = false,
                            isCompleted = true,
                            progress = 1f
                        )) 
                    }
                    playCompletionSound()
                    repository.trackCompletedSession(current.session, current.totalSeconds / 60)
                    
                    delay(3000)
                    _uiState.update { it.copy(activeMeditation = null) }
                    break
                } else {
                    _uiState.update { 
                        it.copy(activeMeditation = current.copy(
                            remainingSeconds = newRemaining,
                            progress = progress
                        )) 
                    }
                }
            }
        }
    }

    private fun playCompletionSound() {
        try {
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            mediaPlayer = MediaPlayer.create(getApplication(), uri)
            mediaPlayer?.apply {
                setVolume(1f, 1f)
                setOnCompletionListener {
                    release()
                    mediaPlayer = null
                }
                start()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun stopSound() {
        try {
            mediaPlayer?.apply {
                if (isPlaying) stop()
                release()
            }
            mediaPlayer = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun formatTime(seconds: Int): String {
        val minutes = seconds / 60
        val secs = seconds % 60
        return String.format("%02d:%02d", minutes, secs)
    }

    override fun onCleared() {
        super.onCleared()
        stopMeditation()
    }
}
