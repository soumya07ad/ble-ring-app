package com.fitness.app.presentation.wellness

import android.app.Application
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitness.app.data.local.entity.JournalEntry
import com.fitness.app.data.repository.JournalRepository
import com.fitness.app.data.repository.MoodDayAggregate
import com.fitness.app.data.repository.MoodRepository
import com.fitness.app.domain.model.ActiveTimer
import com.fitness.app.domain.model.Emotion
import com.fitness.app.domain.model.MeditationItem
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.max

data class WellnessUiState(
    val selectedEmotion: String? = null,
    val wellnessScore: Int = 0,
    val emotions: List<Emotion> = defaultEmotions,
    val meditations: List<MeditationItem> = defaultMeditations,
    val activeTimer: ActiveTimer? = null,

    // Mood Meter State
    val selectedTab: Int = 1, // 0=Daily, 1=Weekly, 2=Monthly
    val chartData: List<MoodDayAggregate> = emptyList(),
    val averageMoodScore: Float = 0f,
    val bestDay: MoodDayAggregate? = null,
    val worstDay: MoodDayAggregate? = null,

    // Journal Dialog State
    val showJournalDialog: Boolean = false,
    val dialogEmotion: String = "",
    val dialogEmoji: String = "",
    val isRecording: Boolean = false,
    val hasRecording: Boolean = false,
    val isPlayingPreview: Boolean = false,
    val playingAudioPath: String? = null,
    val recordingSeconds: Int = 0,
    val audioDurationMs: Int = 0,
    val playbackPositionMs: Int = 0
)

private val defaultEmotions = listOf(
    Emotion("Happy", "😊", 2),
    Emotion("Calm", "😌", 1),
    Emotion("Excited", "🤩", 2),
    Emotion("Grateful", "🙏", 1),
    Emotion("Anxious", "😰", -1),
    Emotion("Sad", "😢", -2),
    Emotion("Frustrated", "😤", -2),
    Emotion("Peaceful", "🕊️", 1)
)

private val defaultMeditations = listOf(
    MeditationItem("1", "Morning Calm", "5 min", 5, "🌅"),
    MeditationItem("2", "Breathing Exercise", "10 min", 10, "🌬️"),
    MeditationItem("3", "Sleep Meditation", "15 min", 15, "🌙")
)

class WellnessViewModel(
    private val moodRepository: MoodRepository,
    private val journalRepository: JournalRepository,
    private val application: Application
) : ViewModel() {

    private val _uiState = MutableStateFlow(WellnessUiState())
    val uiState: StateFlow<WellnessUiState> = _uiState.asStateFlow()

    val journalEntries: StateFlow<List<JournalEntry>> =
        journalRepository.getAllEntries()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var timerJob: Job? = null
    private var mediaRecorder: MediaRecorder? = null
    private var mediaPlayer: MediaPlayer? = null
    private var currentAudioPath: String? = null

    init {
        loadMoodData()
    }

    // --- Emotion Selection & Journal Dialog ---

    fun selectEmotion(emotionName: String) {
        val emotion = defaultEmotions.find { it.name == emotionName } ?: return

        // Save mood score (existing behavior)
        viewModelScope.launch {
            moodRepository.insertMood(emotionName, emotion.score)
            loadMoodData()
        }

        // Open journal dialog
        _uiState.update {
            it.copy(
                selectedEmotion = emotionName,
                showJournalDialog = true,
                dialogEmotion = emotionName,
                dialogEmoji = emotion.emoji,
                isRecording = false,
                hasRecording = false,
                isPlayingPreview = false,
                playingAudioPath = null,
                playbackPositionMs = 0,
                audioDurationMs = 0
            )
        }
        currentAudioPath = null
    }

    fun dismissDialog() {
        stopRecordingInternal()
        stopPlayback()
        currentAudioPath = null
        _uiState.update {
            it.copy(
                showJournalDialog = false,
                selectedEmotion = null,
                isRecording = false,
                hasRecording = false,
                isPlayingPreview = false,
                playingAudioPath = null
            )
        }
    }

    fun saveJournalEntry(message: String) {
        val emotion = _uiState.value.dialogEmotion
        val audioPath = currentAudioPath
        viewModelScope.launch {
            journalRepository.insertEntry(emotion, message.ifBlank { null }, audioPath)
        }
        dismissDialog()
    }

    // --- Audio Recording ---

    private var recordingTimerJob: Job? = null
    private var recordingStarted = false

    fun startRecording() {
        recordingStarted = false
        try {
            val dir = File(application.filesDir, "mood_audio")
            if (!dir.exists()) dir.mkdirs()
            val fileName = "mood_audio_${System.currentTimeMillis()}.m4a"
            val file = File(dir, fileName)
            val path = file.absolutePath

            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(application)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC)
            recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            recorder.setOutputFile(path)
            recorder.prepare()
            recorder.start()

            // Only set path and state AFTER successful start
            mediaRecorder = recorder
            currentAudioPath = path
            recordingStarted = true

            _uiState.update {
                it.copy(
                    isRecording = true,
                    recordingSeconds = 0,
                    hasRecording = false,
                    playingAudioPath = null
                )
            }

            // Start timer, auto-stop at 120s
            recordingTimerJob?.cancel()
            recordingTimerJob = viewModelScope.launch {
                while (_uiState.value.isRecording) {
                    delay(1000)
                    val newSec = _uiState.value.recordingSeconds + 1
                    _uiState.update { it.copy(recordingSeconds = newSec) }
                    if (newSec >= 120) {
                        stopRecording()
                        break
                    }
                }
            }
        } catch (e: Exception) {
            // Permission not granted or MediaRecorder setup failed
            android.util.Log.e("WellnessVM", "startRecording failed: ${e.message}")
            currentAudioPath = null
            recordingStarted = false
            _uiState.update { it.copy(isRecording = false) }
        }
    }

    fun stopRecording() {
        recordingTimerJob?.cancel()

        // If recording never actually started, just reset state
        if (!recordingStarted) {
            _uiState.update { it.copy(isRecording = false) }
            return
        }

        stopRecordingInternal()
        recordingStarted = false
        val path = currentAudioPath

        if (path != null) {
            viewModelScope.launch {
                val file = File(path)
                val durationMs = if (file.exists() && file.length() > 0) {
                    try {
                        MediaPlayer().let { mp ->
                            mp.setDataSource(path)
                            mp.prepare()
                            val d = mp.duration
                            mp.release()
                            d
                        }
                    } catch (_: Exception) { 0 }
                } else 0

                if (durationMs > 0) {
                    // Valid recording
                    _uiState.update {
                        it.copy(
                            isRecording = false,
                            hasRecording = true,
                            audioDurationMs = durationMs,
                            playbackPositionMs = 0
                        )
                    }
                } else {
                    // Invalid/empty recording — discard file and return to idle
                    try { file.delete() } catch (_: Exception) {}
                    currentAudioPath = null
                    _uiState.update {
                        it.copy(
                            isRecording = false,
                            hasRecording = false,
                            audioDurationMs = 0,
                            playbackPositionMs = 0
                        )
                    }
                }
            }
        } else {
            _uiState.update { it.copy(isRecording = false) }
        }
    }

    private fun stopRecordingInternal() {
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
        } catch (_: Exception) {}
        mediaRecorder = null
    }

    fun playPreview() {
        val path = currentAudioPath ?: return
        startPlaybackInternal(path)
    }

    private var playbackTimerJob: Job? = null

    private fun startPlaybackInternal(path: String) {
        // File validation
        val file = File(path)
        if (!file.exists()) {
            android.widget.Toast.makeText(application, "Audio file not found.", android.widget.Toast.LENGTH_SHORT).show()
            return
        }
        try {
            stopPlayback()
            mediaPlayer = MediaPlayer().apply {
                setDataSource(path)
                prepare()
                start()
                setOnCompletionListener {
                    playbackTimerJob?.cancel()
                    _uiState.update { it.copy(isPlayingPreview = false, playbackPositionMs = 0, playingAudioPath = null) }
                }
            }
            _uiState.update { 
                it.copy(
                    isPlayingPreview = true, 
                    playbackPositionMs = 0, 
                    playingAudioPath = path,
                    audioDurationMs = mediaPlayer?.duration ?: 0
                ) 
            }
            startPlaybackTimer()
        } catch (_: Exception) {
            android.widget.Toast.makeText(application, "Audio file not found.", android.widget.Toast.LENGTH_SHORT).show()
            _uiState.update { it.copy(playingAudioPath = null, isPlayingPreview = false) }
        }
    }

    private fun startPlaybackTimer() {
        playbackTimerJob?.cancel()
        playbackTimerJob = viewModelScope.launch {
            while (_uiState.value.isPlayingPreview) {
                delay(200)
                val pos = try { mediaPlayer?.currentPosition ?: 0 } catch (_: Exception) { 0 }
                _uiState.update { it.copy(playbackPositionMs = pos) }
            }
        }
    }

    fun seekTo(positionMs: Int) {
        try {
            mediaPlayer?.seekTo(positionMs)
            _uiState.update { it.copy(playbackPositionMs = positionMs) }
        } catch (_: Exception) {}
    }

    fun pausePlayback() {
        try { mediaPlayer?.pause() } catch (_: Exception) {}
        playbackTimerJob?.cancel()
        _uiState.update { it.copy(isPlayingPreview = false) }
    }

    fun resumePlayback() {
        try { mediaPlayer?.start() } catch (_: Exception) {}
        _uiState.update { it.copy(isPlayingPreview = true) }
        startPlaybackTimer()
    }

    fun stopPlayback() {
        playbackTimerJob?.cancel()
        try {
            mediaPlayer?.apply {
                if (isPlaying) stop()
                release()
            }
        } catch (_: Exception) {}
        mediaPlayer = null
        _uiState.update { 
            it.copy(
                isPlayingPreview = false, 
                playbackPositionMs = 0, 
                playingAudioPath = null 
            ) 
        }
    }

    // Play audio from a saved journal entry
    fun playJournalAudio(audioPath: String) {
        startPlaybackInternal(audioPath)
    }

    // Discard the current recording and return to idle state
    fun discardRecording() {
        stopPlayback()
        val path = currentAudioPath
        if (path != null) {
            try { File(path).delete() } catch (_: Exception) {}
        }
        currentAudioPath = null
        _uiState.update {
            it.copy(
                hasRecording = false,
                isPlayingPreview = false,
                playingAudioPath = null,
                audioDurationMs = 0,
                playbackPositionMs = 0
            )
        }
    }

    // --- Mood Data ---

    fun setTab(index: Int) {
        _uiState.update { it.copy(selectedTab = index) }
        loadMoodData()
    }

    private fun loadMoodData() {
        viewModelScope.launch {
            val state = _uiState.value
            val days = when (state.selectedTab) {
                0 -> 1    // Daily
                1 -> 7    // Weekly
                else -> 30 // Monthly
            }

            val chartData = moodRepository.getChartData(days)
            val (best, worst) = moodRepository.getBestAndWorstDays(days)

            val avgScore = when (state.selectedTab) {
                0 -> moodRepository.getDailyAverage(java.time.LocalDate.now())
                1 -> moodRepository.getWeeklyAverage()
                else -> moodRepository.getMonthlyAverage()
            }

            // Normalize score (-2 to +2) to (0 to 100)
            val normalizedScore = (((avgScore + 2f) / 4f) * 100f).toInt().coerceIn(0, 100)

            _uiState.update {
                it.copy(
                    chartData = chartData,
                    bestDay = best,
                    worstDay = worst,
                    averageMoodScore = avgScore,
                    wellnessScore = if (chartData.all { day -> day.entries.isEmpty() }) 0 else normalizedScore
                )
            }
        }
    }

    // --- Meditation Timer Logic ---

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
        recordingTimerJob?.cancel()
        playbackTimerJob?.cancel()
        stopRecordingInternal()
        stopPlayback()
    }
}
