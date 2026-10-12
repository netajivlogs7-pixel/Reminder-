package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "activities")
data class ActivityEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String = "",
    val category: String = ActivityCategory.ROUTINE.name,
    val iconName: String = "check_circle",
    val colorHex: Long = 0xFF4F46E5,
    val startTime: String = "08:00", // HH:mm format
    val endTime: String = "",
    val durationMinutes: Int = 30,
    val repeatType: String = RepeatType.DAILY.name,
    val selectedDaysOfWeek: String = "1,2,3,4,5,6,7", // 1=Mon, 7=Sun
    val reminderMinutesBefore: Int = 10,
    val priority: String = TaskPriority.MEDIUM.name,
    val goalQuantity: Int = 1,
    val unit: String = "times",
    val isCompleted: Boolean = false,
    val completedDate: String = "", // YYYY-MM-DD of latest completion
    val isPaused: Boolean = false,
    val customSoundUri: String = "",
    val customSoundName: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "completion_events")
data class CompletionEventEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val activityId: Long,
    val activityTitle: String,
    val category: String,
    val date: String, // YYYY-MM-DD
    val pointsEarned: Int = 15,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "hydration_logs")
data class HydrationLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val amountMl: Int,
    val timestamp: Long = System.currentTimeMillis(),
    val date: String // YYYY-MM-DD
)

@Entity(tableName = "workout_sessions")
data class WorkoutSessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val workoutType: String,
    val durationMinutes: Int,
    val distanceKm: Float = 0f,
    val caloriesEstimated: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val date: String, // YYYY-MM-DD
    val notes: String = ""
)

@Entity(tableName = "study_sessions")
data class StudySessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val subject: String,
    val durationMinutes: Int,
    val pomodorosCount: Int = 1,
    val timestamp: Long = System.currentTimeMillis(),
    val date: String, // YYYY-MM-DD
    val notes: String = ""
)

@Entity(tableName = "focus_sessions")
data class FocusSessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String = "Focus Session",
    val sessionType: String = "POMODORO", // POMODORO, BREAK, DEEP_WORK
    val durationMinutes: Int,
    val isCompleted: Boolean = true,
    val timestamp: Long = System.currentTimeMillis(),
    val date: String // YYYY-MM-DD
)

@Entity(tableName = "habits")
data class HabitEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val category: String = "General",
    val targetDaysPerWeek: Int = 7,
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,
    val totalCompletions: Int = 0,
    val lastCompletedDate: String = "" // YYYY-MM-DD
)

@Entity(tableName = "achievements")
data class AchievementEntity(
    @PrimaryKey
    val code: String,
    val titleEn: String,
    val titleHi: String,
    val descEn: String,
    val descHi: String,
    val icon: String,
    val points: Int,
    val isUnlocked: Boolean = false,
    val unlockedAt: Long = 0
)

@Entity(tableName = "user_stats")
data class UserStatsEntity(
    @PrimaryKey
    val id: Int = 1,
    val totalPoints: Int = 50,
    val userLevel: Int = 1,
    val dailyHydrationGoalMl: Int = 2500,
    val dailyStudyGoalMinutes: Int = 120,
    val dailyExerciseGoalMinutes: Int = 30,
    val sleepBedtime: String = "22:30",
    val sleepWakeTime: String = "06:30",
    val subscriptionTier: String = "FREE",
    val subscriptionExpiry: Long = 0,
    val languageCode: String = "en",
    val isDarkMode: Boolean = false,
    val notificationsEnabled: Boolean = true,
    val notificationSoundUri: String = "",
    val notificationSoundName: String = "Default Alert"
)
