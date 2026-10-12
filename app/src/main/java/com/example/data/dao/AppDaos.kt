package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.AchievementEntity
import com.example.data.model.ActivityEntity
import com.example.data.model.CompletionEventEntity
import com.example.data.model.FocusSessionEntity
import com.example.data.model.HabitEntity
import com.example.data.model.HydrationLogEntity
import com.example.data.model.StudySessionEntity
import com.example.data.model.UserStatsEntity
import com.example.data.model.WorkoutSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ActivityDao {
    @Query("SELECT * FROM activities WHERE isPaused = 0 ORDER BY startTime ASC")
    fun getAllActiveActivities(): Flow<List<ActivityEntity>>

    @Query("SELECT * FROM activities ORDER BY startTime ASC")
    fun getAllActivities(): Flow<List<ActivityEntity>>

    @Query("SELECT * FROM activities WHERE id = :id")
    suspend fun getActivityById(id: Long): ActivityEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivity(activity: ActivityEntity): Long

    @Update
    suspend fun updateActivity(activity: ActivityEntity)

    @Delete
    suspend fun deleteActivity(activity: ActivityEntity)

    @Query("DELETE FROM activities WHERE id = :id")
    suspend fun deleteActivityById(id: Long)

    @Query("UPDATE activities SET isCompleted = :isCompleted, completedDate = :date, updatedAt = :timestamp WHERE id = :id")
    suspend fun updateCompletionStatus(id: Long, isCompleted: Boolean, date: String, timestamp: Long = System.currentTimeMillis())
}

@Dao
interface CompletionEventDao {
    @Query("SELECT * FROM completion_events WHERE date = :date ORDER BY timestamp DESC")
    fun getEventsForDate(date: String): Flow<List<CompletionEventEntity>>

    @Query("SELECT * FROM completion_events ORDER BY timestamp DESC")
    fun getAllEvents(): Flow<List<CompletionEventEntity>>

    @Query("SELECT COUNT(*) FROM completion_events WHERE activityId = :activityId AND date = :date")
    suspend fun countEventsForActivityOnDate(activityId: Long, date: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: CompletionEventEntity): Long

    @Query("DELETE FROM completion_events WHERE activityId = :activityId AND date = :date")
    suspend fun deleteEventForActivityOnDate(activityId: Long, date: String)
}

@Dao
interface HydrationDao {
    @Query("SELECT * FROM hydration_logs WHERE date = :date ORDER BY timestamp DESC")
    fun getLogsForDate(date: String): Flow<List<HydrationLogEntity>>

    @Query("SELECT * FROM hydration_logs ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<HydrationLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: HydrationLogEntity): Long

    @Query("DELETE FROM hydration_logs WHERE id = :id")
    suspend fun deleteLogById(id: Long)
}

@Dao
interface WorkoutDao {
    @Query("SELECT * FROM workout_sessions WHERE date = :date ORDER BY timestamp DESC")
    fun getWorkoutsForDate(date: String): Flow<List<WorkoutSessionEntity>>

    @Query("SELECT * FROM workout_sessions ORDER BY timestamp DESC")
    fun getAllWorkouts(): Flow<List<WorkoutSessionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkout(workout: WorkoutSessionEntity): Long

    @Query("DELETE FROM workout_sessions WHERE id = :id")
    suspend fun deleteWorkoutById(id: Long)
}

@Dao
interface StudyDao {
    @Query("SELECT * FROM study_sessions WHERE date = :date ORDER BY timestamp DESC")
    fun getStudySessionsForDate(date: String): Flow<List<StudySessionEntity>>

    @Query("SELECT * FROM study_sessions ORDER BY timestamp DESC")
    fun getAllStudySessions(): Flow<List<StudySessionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudySession(session: StudySessionEntity): Long

    @Query("DELETE FROM study_sessions WHERE id = :id")
    suspend fun deleteStudySessionById(id: Long)
}

@Dao
interface FocusDao {
    @Query("SELECT * FROM focus_sessions WHERE date = :date ORDER BY timestamp DESC")
    fun getFocusSessionsForDate(date: String): Flow<List<FocusSessionEntity>>

    @Query("SELECT * FROM focus_sessions ORDER BY timestamp DESC")
    fun getAllFocusSessions(): Flow<List<FocusSessionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFocusSession(session: FocusSessionEntity): Long
}

@Dao
interface HabitDao {
    @Query("SELECT * FROM habits ORDER BY currentStreak DESC")
    fun getAllHabits(): Flow<List<HabitEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabit(habit: HabitEntity): Long

    @Update
    suspend fun updateHabit(habit: HabitEntity)

    @Query("DELETE FROM habits WHERE id = :id")
    suspend fun deleteHabitById(id: Long)
}

@Dao
interface AchievementDao {
    @Query("SELECT * FROM achievements")
    fun getAllAchievements(): Flow<List<AchievementEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAchievements(achievements: List<AchievementEntity>)

    @Query("UPDATE achievements SET isUnlocked = 1, unlockedAt = :timestamp WHERE code = :code AND isUnlocked = 0")
    suspend fun unlockAchievement(code: String, timestamp: Long = System.currentTimeMillis()): Int
}

@Dao
interface UserStatsDao {
    @Query("SELECT * FROM user_stats WHERE id = 1")
    fun getUserStats(): Flow<UserStatsEntity?>

    @Query("SELECT * FROM user_stats WHERE id = 1")
    suspend fun getUserStatsSync(): UserStatsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(stats: UserStatsEntity)
}
