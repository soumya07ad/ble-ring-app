package com.fitness.app.presentation.coach

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.ViewModelProvider
import com.fitness.app.data.local.entity.CoachMessageEntity
import com.fitness.app.domain.model.CoachMessage
import com.fitness.app.domain.model.CoachSession
import com.fitness.app.domain.repository.ICoachRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import java.util.UUID

data class CoachUiState(
    val messageInput: String = "",
    val selectedCategory: String? = null,
    val isLoading: Boolean = false,
    val activeSessionId: String? = null,
    val messages: List<CoachMessage> = emptyList(),
    val sessions: List<CoachSession> = emptyList(),
    val errorMessage: String? = null
)

class CoachViewModel(
    private val coachRepository: ICoachRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CoachUiState())
    val uiState: StateFlow<CoachUiState> = _uiState.asStateFlow()

    private var messagesJob: Job? = null

    init {
        viewModelScope.launch {
            coachRepository.getAllSessions().collect { entities ->
                val sessions = entities.map { 
                    CoachSession(id = it.sessionId, firstMessage = it.text, timestamp = it.timestamp) 
                }
                _uiState.update { it.copy(sessions = sessions) }
            }
        }
    }

    private fun observeMessages(sessionId: String) {
        messagesJob?.cancel()
        messagesJob = viewModelScope.launch {
            coachRepository.getMessagesBySession(sessionId).collect { entities ->
                val messages = entities.map { it.toUiModel() }
                _uiState.update { it.copy(messages = messages) }
            }
        }
    }

    fun startNewChat() {
        messagesJob?.cancel()
        _uiState.update { 
            it.copy(
                activeSessionId = null,
                messages = listOf(CoachMessage("Hello! I'm your Wellness Coach. How can I help you reach your goals today?", false)),
                messageInput = ""
            ) 
        }
    }

    fun loadSession(sessionId: String) {
        _uiState.update { it.copy(activeSessionId = sessionId) }
        observeMessages(sessionId)
    }

    fun onInputChange(newValue: String) {
        _uiState.update { it.copy(messageInput = newValue) }
    }

    fun onCategorySelect(category: String) {
        _uiState.update { it.copy(selectedCategory = category, messageInput = category) }
    }

    fun sendMessage() {
        val input = _uiState.value.messageInput.trim()
        if (input.isEmpty()) return

        val currentSessionId = _uiState.value.activeSessionId ?: UUID.randomUUID().toString()
        
        if (_uiState.value.activeSessionId == null) {
            _uiState.update { it.copy(activeSessionId = currentSessionId) }
            observeMessages(currentSessionId)
        }

        _uiState.update { 
            it.copy(
                messageInput = "",
                isLoading = true
            ) 
        }

        viewModelScope.launch {
            coachRepository.saveMessage(input, true, currentSessionId)

            delay(1500)
            val responseText = when {
                input.contains("motivation", ignoreCase = true) -> 
                    "Remember, consistency is better than perfection. Every small step counts towards your ultimate goal!"
                input.contains("workout", ignoreCase = true) -> 
                    "A balanced workout include strength, cardio, and flexibility. Try alternating between them this week."
                input.contains("nutrition", ignoreCase = true) -> 
                    "Focus on whole foods and stayed hydrated. Small changes in your diet can lead to big improvements in energy."
                input.contains("sleep", ignoreCase = true) -> 
                    "Your sleep quality directly impacts your recovery. Try to keep a consistent wake-up time even on weekends."
                else -> "That's interesting! Let's explore how we can optimize your wellness journey together."
            }
            
            coachRepository.saveMessage(responseText, false, currentSessionId)
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            coachRepository.clearHistory()
            startNewChat()
        }
    }

    fun deleteSession(sessionId: String) {
        viewModelScope.launch {
            coachRepository.deleteSession(sessionId)
            if (_uiState.value.activeSessionId == sessionId) {
                startNewChat()
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    private fun CoachMessageEntity.toUiModel() = CoachMessage(
        text = text,
        isUser = isUser,
        timestamp = timestamp
    )

    companion object {
        fun provideFactory(repository: ICoachRepository): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return CoachViewModel(repository) as T
            }
        }
    }
}
