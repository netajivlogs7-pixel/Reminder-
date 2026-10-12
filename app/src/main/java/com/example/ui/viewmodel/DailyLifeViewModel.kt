package com.example.ui.viewmodel

import android.app.Application
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.AiAssistantResponse
import com.example.ai.GeminiAiService
import com.example.ai.ParsedTaskProposal
import com.example.data.AppDatabase
import com.example.data.model.AchievementEntity
import com.example.data.model.ActivityCategory
import com.example.data.model.ActivityEntity
import com.example.data.model.CompletionEventEntity
import com.example.data.model.FocusSessionEntity
import com.example.data.model.HabitEntity
import com.example.data.model.HydrationLogEntity
import com.example.data.model.RepeatType
import com.example.data.model.StudySessionEntity
import com.example.data.model.TaskPriority
import com.example.data.model.UserStatsEntity
import com.example.data.model.WorkoutSessionEntity
import com.example.data.repository.DailyLifeRepository
import com.example.localization.AppLanguage
import com.example.reminder.ReminderManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ChatMessage(
    val isUser: Boolean,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val proposal: ParsedTaskProposal? = null
)

enum class ScreenTab {
    HOME,
    PLANNER,
    ADD_ACTIVITY,
    HEALTH,
    FOCUS,
    INSIGHTS,
    AI_ASSISTANT,
    SETTINGS
}

class DailyLifeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = DailyLifeRepository(AppDatabase.getDatabase(application))
    private val reminderManager = ReminderManager(application)
    private val geminiService = GeminiAiService()

    // Navigation state
    private val _currentTab = MutableStateFlow(ScreenTab.HOME)
    val currentTab: StateFlow<ScreenTab> = _currentTab.asStateFlow()

    // Selected Planner Date (YYYY-MM-DD)
    private val _selectedDate = MutableStateFlow(repository.getTodayDateString())
    val selectedDate: StateFlow<String> = _selectedDate.asStateFlow()

    // App Language
    private val _appLanguage = MutableStateFlow(AppLanguage.ENGLISH)
    val appLanguage: StateFlow<AppLanguage> = _appLanguage.asStateFlow()

    // Dark Mode override
    private val _isDarkMode = MutableStateFlow(false)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    // Repository Flows
    val activities: StateFlow<List<ActivityEntity>> = repository.allActivities
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val todayEvents: StateFlow<List<CompletionEventEntity>> = repository.getTodayEvents()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val todayHydration: StateFlow<List<HydrationLogEntity>> = repository.getTodayHydration()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val todayWorkouts: StateFlow<List<WorkoutSessionEntity>> = repository.getTodayWorkouts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val todayStudy: StateFlow<List<StudySessionEntity>> = repository.getTodayStudy()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val habits: StateFlow<List<HabitEntity>> = repository.allHabits
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val achievements: StateFlow<List<AchievementEntity>> = repository.allAchievements
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userStats: StateFlow<UserStatsEntity?> = repository.userStats
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Focus / Pomodoro Timer State
    private val _focusTimeRemaining = MutableStateFlow(25 * 60)
    val focusTimeRemaining: StateFlow<Int> = _focusTimeRemaining.asStateFlow()

    private val _isFocusRunning = MutableStateFlow(false)
    val isFocusRunning: StateFlow<Boolean> = _isFocusRunning.asStateFlow()

    private val _focusMode = MutableStateFlow("POMODORO") // POMODORO (25m), SHORT_BREAK (5m), LONG_BREAK (15m)
    val focusMode: StateFlow<String> = _focusMode.asStateFlow()

    private var timerJob: Job? = null

    // AI Assistant State
    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage(
                isUser = false,
                text = "Namaste! I am your AI Daily Routine & Productivity Assistant. You can speak or write tasks in Hindi or English (e.g., 'Kal subah 6 baje running schedule karo' or 'Suggest a balanced study routine')."
            )
        )
    )
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _isAiLoading = MutableStateFlow(false)
    val isAiLoading: StateFlow<Boolean> = _isAiLoading.asStateFlow()

    init {
        // Observe stats to update language and dark mode
        viewModelScope.launch {
            repository.userStats.collect { stats ->
                if (stats != null) {
                    _appLanguage.value = if (stats.languageCode == "hi") AppLanguage.HINDI else AppLanguage.ENGLISH
                    _isDarkMode.value = stats.isDarkMode
                }
            }
        }
    }

    fun selectTab(tab: ScreenTab) {
        _currentTab.value = tab
    }

    fun setSelectedDate(date: String) {
        _selectedDate.value = date
    }

    fun setLanguage(language: AppLanguage) {
        _appLanguage.value = language
        viewModelScope.launch {
            repository.setLanguage(language.code)
        }
    }

    fun setDarkMode(isDark: Boolean) {
        _isDarkMode.value = isDark
        viewModelScope.launch {
            repository.setDarkMode(isDark)
        }
    }

    fun toggleActivityCompletion(activity: ActivityEntity) {
        viewModelScope.launch {
            repository.toggleActivityCompletion(activity)
        }
    }

    fun addActivity(activity: ActivityEntity) {
        viewModelScope.launch {
            val id = repository.insertActivity(activity)
            val defaultSound = userStats.value?.notificationSoundUri ?: ""
            reminderManager.scheduleReminder(activity.copy(id = id), defaultSound)
        }
    }

    fun updateActivity(activity: ActivityEntity) {
        viewModelScope.launch {
            repository.updateActivity(activity)
            reminderManager.cancelReminder(activity.id)
            val defaultSound = userStats.value?.notificationSoundUri ?: ""
            reminderManager.scheduleReminder(activity, defaultSound)
        }
    }

    fun deleteActivity(id: Long) {
        viewModelScope.launch {
            reminderManager.cancelReminder(id)
            repository.deleteActivity(id)
        }
    }

    fun addWater(amountMl: Int) {
        viewModelScope.launch {
            repository.addWater(amountMl)
        }
    }

    fun deleteWater(id: Long) {
        viewModelScope.launch {
            repository.deleteWater(id)
        }
    }

    fun addWorkout(workout: WorkoutSessionEntity) {
        viewModelScope.launch {
            repository.addWorkout(workout)
        }
    }

    fun addStudySession(session: StudySessionEntity) {
        viewModelScope.launch {
            repository.addStudySession(session)
        }
    }

    fun completeHabit(habit: HabitEntity) {
        viewModelScope.launch {
            repository.completeHabit(habit)
        }
    }

    fun addHabit(name: String, category: String, targetDays: Int) {
        viewModelScope.launch {
            repository.addHabit(
                HabitEntity(
                    name = name,
                    category = category,
                    targetDaysPerWeek = targetDays
                )
            )
        }
    }

    fun deleteHabit(id: Long) {
        viewModelScope.launch {
            repository.deleteHabit(id)
        }
    }

    // --- Focus / Pomodoro Timer ---

    fun setFocusMode(mode: String) {
        _focusMode.value = mode
        _isFocusRunning.value = false
        timerJob?.cancel()
        _focusTimeRemaining.value = when (mode) {
            "SHORT_BREAK" -> 5 * 60
            "LONG_BREAK" -> 15 * 60
            else -> 25 * 60
        }
    }

    fun toggleFocusTimer() {
        if (_isFocusRunning.value) {
            pauseFocusTimer()
        } else {
            startFocusTimer()
        }
    }

    private fun startFocusTimer() {
        _isFocusRunning.value = true
        timerJob = viewModelScope.launch {
            while (_focusTimeRemaining.value > 0 && _isFocusRunning.value) {
                delay(1000L)
                _focusTimeRemaining.value -= 1
            }
            if (_focusTimeRemaining.value <= 0) {
                _isFocusRunning.value = false
                val duration = when (_focusMode.value) {
                    "SHORT_BREAK" -> 5
                    "LONG_BREAK" -> 15
                    else -> 25
                }
                repository.addFocusSession(
                    FocusSessionEntity(
                        title = "Focus (${_focusMode.value})",
                        sessionType = _focusMode.value,
                        durationMinutes = duration,
                        date = repository.getTodayDateString()
                    )
                )
                reminderManager.sendTestNotification(
                    title = "Focus Session Finished! 🎉",
                    message = "Great work! Time for a short break or next task."
                )
                // reset
                setFocusMode(_focusMode.value)
            }
        }
    }

    private fun pauseFocusTimer() {
        _isFocusRunning.value = false
        timerJob?.cancel()
    }

    fun resetFocusTimer() {
        setFocusMode(_focusMode.value)
    }

    // --- AI Assistant ---

    fun sendAiMessage(userText: String) {
        val trimmed = userText.trim()
        if (trimmed.isEmpty()) return

        val userMsg = ChatMessage(isUser = true, text = trimmed)
        _chatMessages.value = _chatMessages.value + userMsg
        _isAiLoading.value = true

        viewModelScope.launch {
            val totalTasks = activities.value.size
            val completedTasks = activities.value.count { it.isCompleted }
            val context = "Today's date: ${repository.getTodayDateString()}. Tasks: $completedTasks completed out of $totalTasks."

            val response: AiAssistantResponse = geminiService.processUserMessage(
                userMessage = trimmed,
                currentLanguage = _appLanguage.value.code,
                contextSummary = context
            )

            val botMsg = ChatMessage(
                isUser = false,
                text = response.replyText,
                proposal = response.proposedTask
            )
            _chatMessages.value = _chatMessages.value + botMsg
            _isAiLoading.value = false
        }
    }

    fun acceptAiProposal(proposal: ParsedTaskProposal) {
        val entity = proposal.toEntity()
        addActivity(entity)
        val confirmMsg = ChatMessage(
            isUser = false,
            text = if (_appLanguage.value == AppLanguage.HINDI) {
                "शानदार! '${entity.title}' को ${entity.startTime} पर सफलतापूर्वक जोड़ दिया गया है।"
            } else {
                "Awesome! '${entity.title}' at ${entity.startTime} has been added to your schedule."
            }
        )
        _chatMessages.value = _chatMessages.value + confirmMsg
    }

    // --- Templates ---

    fun applyTemplate(templateName: String) {
        viewModelScope.launch {
            when (templateName) {
                "MORNING_BOOST" -> {
                    repository.insertActivity(
                        ActivityEntity(
                            title = "Sunlight Exposure & Gratitude",
                            category = ActivityCategory.ROUTINE.name,
                            startTime = "06:45",
                            durationMinutes = 15,
                            priority = TaskPriority.MEDIUM.name
                        )
                    )
                    repository.insertActivity(
                        ActivityEntity(
                            title = "High Protein Breakfast",
                            category = ActivityCategory.ROUTINE.name,
                            startTime = "08:00",
                            durationMinutes = 20,
                            priority = TaskPriority.HIGH.name
                        )
                    )
                }
                "STUDENT_SPRINT" -> {
                    repository.insertActivity(
                        ActivityEntity(
                            title = "Core Concept Revision (Math/Tech)",
                            category = ActivityCategory.STUDY.name,
                            startTime = "10:00",
                            durationMinutes = 60,
                            priority = TaskPriority.HIGH.name
                        )
                    )
                    repository.insertActivity(
                        ActivityEntity(
                            title = "Practice Problem Solving & Flashcards",
                            category = ActivityCategory.STUDY.name,
                            startTime = "15:00",
                            durationMinutes = 45,
                            priority = TaskPriority.MEDIUM.name
                        )
                    )
                }
                "OFFICE_FOCUS" -> {
                    repository.insertActivity(
                        ActivityEntity(
                            title = "Daily Standup & Priority Inbox Zero",
                            category = ActivityCategory.ROUTINE.name,
                            startTime = "09:30",
                            durationMinutes = 30,
                            priority = TaskPriority.HIGH.name
                        )
                    )
                    repository.insertActivity(
                        ActivityEntity(
                            title = "Strategic Deep Work Sprint",
                            category = ActivityCategory.STUDY.name,
                            startTime = "11:00",
                            durationMinutes = 90,
                            priority = TaskPriority.HIGH.name
                        )
                    )
                }
            }
        }
    }

    // --- Freemium Subscription ---

    fun upgradeSubscription(tier: String) {
        viewModelScope.launch {
            repository.setSubscriptionTier(tier)
        }
    }

    // --- Custom Audio & Notifications ---

    private var activeMediaPlayer: MediaPlayer? = null
    private var activeRingtone: Ringtone? = null
    private val _isPlayingAudio = MutableStateFlow(false)
    val isPlayingAudio: StateFlow<Boolean> = _isPlayingAudio.asStateFlow()

    fun playAudioPreview(uriStr: String) {
        val context = getApplication<Application>()
        stopAudioPreview()
        try {
            val uri = if (uriStr.isNotBlank()) {
                Uri.parse(uriStr)
            } else {
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            }

            var playbackStarted = false

            // Try MediaPlayer first (works great for custom MP3, WAV, audio files, content URIs)
            try {
                val mp = MediaPlayer()
                mp.setDataSource(context, uri)
                mp.setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .build()
                )
                mp.prepare()
                mp.setOnCompletionListener {
                    stopAudioPreview()
                }
                mp.start()
                activeMediaPlayer = mp
                playbackStarted = true
            } catch (e: Exception) {
                // Fall back to RingtoneManager
            }

            if (!playbackStarted) {
                val ringtone = RingtoneManager.getRingtone(context, uri)
                ringtone?.play()
                activeRingtone = ringtone
                playbackStarted = true
            }

            _isPlayingAudio.value = true

            // Stop preview automatically after 6 seconds
            viewModelScope.launch {
                delay(6000L)
                stopAudioPreview()
            }
        } catch (e: Exception) {
            _isPlayingAudio.value = false
        }
    }

    fun stopAudioPreview() {
        try {
            activeMediaPlayer?.let {
                if (it.isPlaying) it.stop()
                it.release()
            }
            activeMediaPlayer = null
            activeRingtone?.stop()
            activeRingtone = null
            _isPlayingAudio.value = false
        } catch (e: Exception) {
            _isPlayingAudio.value = false
        }
    }

    fun setNotificationSound(uriStr: String, name: String) {
        viewModelScope.launch {
            repository.setNotificationSound(uriStr, name)
        }
    }

    fun sendTestNotification(soundUri: String = "") {
        val soundToUse = if (soundUri.isNotBlank()) soundUri else userStats.value?.notificationSoundUri ?: ""
        reminderManager.sendTestNotification(
            title = if (_appLanguage.value == AppLanguage.HINDI) "दैनिक जीवन प्रबंधक" else "Daily Life Manager",
            message = if (_appLanguage.value == AppLanguage.HINDI) "कस्टम ऑडियो के साथ रिमाइंडर सक्रिय है!" else "Reminder active with selected audio tone!",
            soundUri = soundToUse
        )
    }

    override fun onCleared() {
        super.onCleared()
        stopAudioPreview()
    }

    suspend fun exportJson(): String {
        return repository.exportDataAsJson()
    }
}
