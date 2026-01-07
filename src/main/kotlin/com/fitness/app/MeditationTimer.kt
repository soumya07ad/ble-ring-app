package com.fitness.app

import android.content.Context
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.State
import kotlin.math.max

// Meditation session data class
data class MeditationSession(
    val id: String,
    val name: String,
    val description: String,
    val durationSeconds: Int,
    val icon: String,
    val category: String // "morning", "breathing", "sleep"
)

// Active meditation state
data class ActiveMeditation(
    val session: MeditationSession,
    val remainingSeconds: Int,
    val totalSeconds: Int,
    val isRunning: Boolean,
    val isCompleted: Boolean,
    val progress: Float // 0-1
)

/**
 * MeditationTimer: Manages meditation sessions with countdown timer and sound notifications
 */
class MeditationTimer(private val context: Context) {
    
    private val handler = Handler(Looper.getMainLooper())
    private var timerRunnable: Runnable? = null
    private var mediaPlayer: MediaPlayer? = null
    
    // Meditation sessions
    private val _meditations = listOf(
        MeditationSession(
            id = "morning_calm",
            name = "Morning Calm",
            description = "Start your day with peace and clarity",
            durationSeconds = 300, // 5 minutes
            icon = "🌅",
            category = "morning"
        ),
        MeditationSession(
            id = "breathing_exercise",
            name = "Breathing Exercise",
            description = "Reduce stress with deep breathing techniques",
            durationSeconds = 600, // 10 minutes
            icon = "🌬️",
            category = "breathing"
        ),
        MeditationSession(
            id = "sleep_meditation",
            name = "Sleep Meditation",
            description = "Prepare your mind and body for restful sleep",
            durationSeconds = 900, // 15 minutes
            icon = "🌙",
            category = "sleep"
        )
    )
    
    val meditations: List<MeditationSession> = _meditations
    
    // Active meditation state
    private val _activeMeditation = mutableStateOf<ActiveMeditation?>(null)
    val activeMeditation: State<ActiveMeditation?> = _activeMeditation
    
    // Meditation statistics
    private val _totalMeditationMinutes = mutableStateOf(0)
    val totalMeditationMinutes: State<Int> = _totalMeditationMinutes
    
    private val _sessionsCompleted = mutableStateOf(0)
    val sessionsCompleted: State<Int> = _sessionsCompleted
    
    // Callbacks
    var onTimeUpdate: ((ActiveMeditation) -> Unit)? = null
    var onSessionComplete: ((MeditationSession, Int) -> Unit)? = null
    var onSessionStop: (() -> Unit)? = null
    
    /**
     * Start a meditation session
     */
    fun startMeditation(session: MeditationSession) {
        // Stop any existing meditation
        stopMeditation()
        
        // Initialize new meditation
        _activeMeditation.value = ActiveMeditation(
            session = session,
            remainingSeconds = session.durationSeconds,
            totalSeconds = session.durationSeconds,
            isRunning = true,
            isCompleted = false,
            progress = 0f
        )
        
        startTimer()
    }
    
    /**
     * Resume the current meditation session
     */
    fun resumeMeditation() {
        _activeMeditation.value?.let { current ->
            _activeMeditation.value = current.copy(isRunning = true)
            startTimer()
        }
    }
    
    /**
     * Pause the current meditation session
     */
    fun pauseMeditation() {
        _activeMeditation.value?.let { current ->
            _activeMeditation.value = current.copy(isRunning = false)
        }
        handler.removeCallbacks(timerRunnable ?: return)
    }
    
    /**
     * Stop and discard the current meditation session
     */
    fun stopMeditation() {
        handler.removeCallbacks(timerRunnable ?: return)
        _activeMeditation.value = null
        stopSound()
        onSessionStop?.invoke()
    }
    
    /**
     * Start the countdown timer
     */
    private fun startTimer() {
        timerRunnable = object : Runnable {
            override fun run() {
                val current = _activeMeditation.value ?: return
                
                if (!current.isRunning) {
                    handler.postDelayed(this, 1000)
                    return
                }
                
                val newRemaining = max(0, current.remainingSeconds - 1)
                val progress = (current.totalSeconds - newRemaining).toFloat() / current.totalSeconds
                
                if (newRemaining == 0) {
                    // Session completed
                    _activeMeditation.value = current.copy(
                        remainingSeconds = 0,
                        isRunning = false,
                        isCompleted = true,
                        progress = 1f
                    )
                    
                    // Play completion sound and notify
                    playCompletionSound()
                    updateStatistics(current.session)
                    onSessionComplete?.invoke(current.session, current.totalSeconds)
                    
                    // Auto-clear after 3 seconds
                    handler.postDelayed({
                        _activeMeditation.value = null
                    }, 3000)
                } else {
                    // Update timer state
                    _activeMeditation.value = current.copy(
                        remainingSeconds = newRemaining,
                        progress = progress
                    )
                    onTimeUpdate?.invoke(_activeMeditation.value!!)
                    
                    // Continue timer
                    handler.postDelayed(this, 1000)
                }
            }
        }
        
        handler.post(timerRunnable!!)
    }
    
    /**
     * Play completion sound notification
     */
    private fun playCompletionSound() {
        try {
            // Use device's default notification sound
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            mediaPlayer = MediaPlayer.create(context, uri)
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
    
    /**
     * Stop any playing sound
     */
    private fun stopSound() {
        try {
            mediaPlayer?.apply {
                if (isPlaying) {
                    stop()
                }
                release()
            }
            mediaPlayer = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    /**
     * Update meditation statistics
     */
    private fun updateStatistics(session: MeditationSession) {
        val minutesCompleted = session.durationSeconds / 60
        _totalMeditationMinutes.value += minutesCompleted
        _sessionsCompleted.value += 1
    }
    
    /**
     * Get formatted time string (MM:SS)
     */
    fun formatTime(seconds: Int): String {
        val minutes = seconds / 60
        val secs = seconds % 60
        return String.format("%02d:%02d", minutes, secs)
    }
    
    /**
     * Get meditation session by ID
     */
    fun getMeditationById(id: String): MeditationSession? {
        return _meditations.find { it.id == id }
    }
    
    /**
     * Get meditations by category
     */
    fun getMeditationsByCategory(category: String): List<MeditationSession> {
        return _meditations.filter { it.category == category }
    }
    
    /**
     * Cleanup resources
     */
    fun cleanup() {
        stopMeditation()
        handler.removeCallbacksAndMessages(null)
        stopSound()
    }
}
