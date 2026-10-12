package com.example.data.repository

import com.example.data.AppDatabase
import com.example.data.model.AchievementEntity
import com.example.data.model.ActivityEntity
import com.example.data.model.CompletionEventEntity
import com.example.data.model.FocusSessionEntity
import com.example.data.model.HabitEntity
import com.example.data.model.HydrationLogEntity
import com.example.data.model.StudySessionEntity
import com.example.data.model.UserStatsEntity
import com.example.data.model.WorkoutSessionEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DailyLifeRepository(private val database: AppDatabase) {

    private val activityDao = database.activityDao()
    private val completionEventDao = database.completionEventDao()
    private val hydrationDao = database.hydrationDao()
    private val workoutDao = database.workoutDao()
    private val studyDao = database.studyDao()
    private val focusDao = database.focusDao()
    private val habitDao = database.habitDao()
    private val achievementDao = database.achievementDao()
    private val userStatsDao = database.userStatsDao()

    val allActivities: Flow<List<ActivityEntity>> = activityDao.getAllActivities()
    val allHabits: Flow<List<HabitEntity>> = habitDao.getAllHabits()
    val allAchievements: Flow<List<AchievementEntity>> = achievementDao.getAllAchievements()
    val userStats: Flow<UserStatsEntity?> = userStatsDao.getUserStats()

    fun getTodayDateString(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    fun getTodayEvents(date: String = getTodayDateString()): Flow<List<CompletionEventEntity>> {
        return completionEventDao.getEventsForDate(date)
    }

    fun getTodayHydration(date: String = getTodayDateString()): Flow<List<HydrationLogEntity>> {
        return hydrationDao.getLogsForDate(date)
    }

    fun getTodayWorkouts(date: String = getTodayDateString()): Flow<List<WorkoutSessionEntity>> {
        return workoutDao.getWorkoutsForDate(date)
    }

    fun getTodayStudy(date: String = getTodayDateString()): Flow<List<StudySessionEntity>> {
        return studyDao.getStudySessionsForDate(date)
    }

    fun getTodayFocus(date: String = getTodayDateString()): Flow<List<FocusSessionEntity>> {
        return focusDao.getFocusSessionsForDate(date)
    }

    suspend fun insertActivity(activity: ActivityEntity): Long = withContext(Dispatchers.IO) {
        val id = activityDao.insertActivity(activity)
        checkAchievementUnlocked("HABIT_ARCHITECT")
        id
    }

    suspend fun updateActivity(activity: ActivityEntity) = withContext(Dispatchers.IO) {
        activityDao.updateActivity(activity)
    }

    suspend fun deleteActivity(id: Long) = withContext(Dispatchers.IO) {
        activityDao.deleteActivityById(id)
    }

    suspend fun toggleActivityCompletion(activity: ActivityEntity, date: String = getTodayDateString()): Boolean =
        withContext(Dispatchers.IO) {
            val existingCount = completionEventDao.countEventsForActivityOnDate(activity.id, date)
            val nowComplete = existingCount == 0

            if (nowComplete) {
                // Mark completed and award points idempotently
                completionEventDao.insertEvent(
                    CompletionEventEntity(
                        activityId = activity.id,
                        activityTitle = activity.title,
                        category = activity.category,
                        date = date,
                        pointsEarned = 15
                    )
                )
                activityDao.updateCompletionStatus(activity.id, true, date)
                addPoints(15)
                checkAchievementUnlocked("FIRST_STEP")
            } else {
                // Unmark completed and deduct points
                completionEventDao.deleteEventForActivityOnDate(activity.id, date)
                activityDao.updateCompletionStatus(activity.id, false, "")
                addPoints(-15)
            }
            nowComplete
        }

    suspend fun addWater(amountMl: Int, date: String = getTodayDateString()): Long = withContext(Dispatchers.IO) {
        val logId = hydrationDao.insertLog(HydrationLogEntity(amountMl = amountMl, date = date))
        addPoints(5)
        logId
    }

    suspend fun deleteWater(id: Long) = withContext(Dispatchers.IO) {
        hydrationDao.deleteLogById(id)
    }

    suspend fun addWorkout(workout: WorkoutSessionEntity): Long = withContext(Dispatchers.IO) {
        val id = workoutDao.insertWorkout(workout)
        addPoints(20)
        id
    }

    suspend fun deleteWorkout(id: Long) = withContext(Dispatchers.IO) {
        workoutDao.deleteWorkoutById(id)
    }

    suspend fun addStudySession(session: StudySessionEntity): Long = withContext(Dispatchers.IO) {
        val id = studyDao.insertStudySession(session)
        addPoints(20)
        id
    }

    suspend fun deleteStudySession(id: Long) = withContext(Dispatchers.IO) {
        studyDao.deleteStudySessionById(id)
    }

    suspend fun addFocusSession(session: FocusSessionEntity): Long = withContext(Dispatchers.IO) {
        val id = focusDao.insertFocusSession(session)
        addPoints(25)
        checkAchievementUnlocked("FOCUS_MASTER")
        id
    }

    suspend fun addHabit(habit: HabitEntity): Long = withContext(Dispatchers.IO) {
        habitDao.insertHabit(habit)
    }

    suspend fun completeHabit(habit: HabitEntity, date: String = getTodayDateString()) = withContext(Dispatchers.IO) {
        if (habit.lastCompletedDate != date) {
            val newCurrent = habit.currentStreak + 1
            val newBest = maxOf(newCurrent, habit.bestStreak)
            val updated = habit.copy(
                currentStreak = newCurrent,
                bestStreak = newBest,
                totalCompletions = habit.totalCompletions + 1,
                lastCompletedDate = date
            )
            habitDao.updateHabit(updated)
            addPoints(10)
            if (newCurrent >= 3) {
                checkAchievementUnlocked("STREAK_WARRIOR")
            }
        }
    }

    suspend fun deleteHabit(id: Long) = withContext(Dispatchers.IO) {
        habitDao.deleteHabitById(id)
    }

    private suspend fun addPoints(points: Int) {
        val stats = userStatsDao.getUserStatsSync() ?: UserStatsEntity()
        val newPoints = maxOf(0, stats.totalPoints + points)
        val newLevel = (newPoints / 100) + 1
        userStatsDao.insertOrUpdate(stats.copy(totalPoints = newPoints, userLevel = newLevel))
    }

    suspend fun checkAchievementUnlocked(code: String) = withContext(Dispatchers.IO) {
        val unlockedCount = achievementDao.unlockAchievement(code)
        if (unlockedCount > 0) {
            addPoints(30)
        }
    }

    suspend fun updateStats(stats: UserStatsEntity) = withContext(Dispatchers.IO) {
        userStatsDao.insertOrUpdate(stats)
    }

    suspend fun setLanguage(languageCode: String) = withContext(Dispatchers.IO) {
        val stats = userStatsDao.getUserStatsSync() ?: UserStatsEntity()
        userStatsDao.insertOrUpdate(stats.copy(languageCode = languageCode))
    }

    suspend fun setDarkMode(isDark: Boolean) = withContext(Dispatchers.IO) {
        val stats = userStatsDao.getUserStatsSync() ?: UserStatsEntity()
        userStatsDao.insertOrUpdate(stats.copy(isDarkMode = isDark))
    }

    suspend fun setSubscriptionTier(tier: String) = withContext(Dispatchers.IO) {
        val stats = userStatsDao.getUserStatsSync() ?: UserStatsEntity()
        userStatsDao.insertOrUpdate(stats.copy(subscriptionTier = tier))
    }

    suspend fun setNotificationSound(uri: String, name: String) = withContext(Dispatchers.IO) {
        val stats = userStatsDao.getUserStatsSync() ?: UserStatsEntity()
        userStatsDao.insertOrUpdate(stats.copy(notificationSoundUri = uri, notificationSoundName = name))
    }

    suspend fun exportDataAsJson(): String = withContext(Dispatchers.IO) {
        val root = JSONObject()
        val stats = userStatsDao.getUserStatsSync() ?: UserStatsEntity()
        root.put("version", 1)
        root.put("exportTime", System.currentTimeMillis())

        val statsObj = JSONObject().apply {
            put("totalPoints", stats.totalPoints)
            put("userLevel", stats.userLevel)
            put("waterGoal", stats.dailyHydrationGoalMl)
            put("studyGoal", stats.dailyStudyGoalMinutes)
            put("exerciseGoal", stats.dailyExerciseGoalMinutes)
            put("tier", stats.subscriptionTier)
        }
        root.put("stats", statsObj)
        root.toString(2)
    }
}
