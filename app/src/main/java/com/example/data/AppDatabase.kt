package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.dao.AchievementDao
import com.example.data.dao.ActivityDao
import com.example.data.dao.CompletionEventDao
import com.example.data.dao.FocusDao
import com.example.data.dao.HabitDao
import com.example.data.dao.HydrationDao
import com.example.data.dao.StudyDao
import com.example.data.dao.UserStatsDao
import com.example.data.dao.WorkoutDao
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        ActivityEntity::class,
        CompletionEventEntity::class,
        HydrationLogEntity::class,
        WorkoutSessionEntity::class,
        StudySessionEntity::class,
        FocusSessionEntity::class,
        HabitEntity::class,
        AchievementEntity::class,
        UserStatsEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun activityDao(): ActivityDao
    abstract fun completionEventDao(): CompletionEventDao
    abstract fun hydrationDao(): HydrationDao
    abstract fun workoutDao(): WorkoutDao
    abstract fun studyDao(): StudyDao
    abstract fun focusDao(): FocusDao
    abstract fun habitDao(): HabitDao
    abstract fun achievementDao(): AchievementDao
    abstract fun userStatsDao(): UserStatsDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "daily_life_manager_db"
                )
                    .fallbackToDestructiveMigration()
                    .addCallback(DatabaseCallback())
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    CoroutineScope(Dispatchers.IO).launch {
                        populateInitialData(database)
                    }
                }
            }
        }

        private suspend fun populateInitialData(database: AppDatabase) {
            // Initial Activities
            val initialActivities = listOf(
                ActivityEntity(
                    title = "Morning Warm Water & Hydration",
                    description = "Drink 500ml warm water right after waking up",
                    category = ActivityCategory.HYDRATION.name,
                    iconName = "water_drop",
                    colorHex = 0xFF0284C7,
                    startTime = "07:00",
                    durationMinutes = 10,
                    repeatType = RepeatType.DAILY.name,
                    priority = TaskPriority.HIGH.name
                ),
                ActivityEntity(
                    title = "Brisk Morning Walk / Exercise",
                    description = "30 minutes outdoor brisk walk or stretching",
                    category = ActivityCategory.WORKOUT.name,
                    iconName = "directions_run",
                    colorHex = 0xFFEA580C,
                    startTime = "07:30",
                    durationMinutes = 30,
                    repeatType = RepeatType.DAILY.name,
                    priority = TaskPriority.HIGH.name
                ),
                ActivityEntity(
                    title = "Deep Focus Study / Work Block",
                    description = "Clear top 2 high-impact tasks without distraction",
                    category = ActivityCategory.STUDY.name,
                    iconName = "menu_book",
                    colorHex = 0xFF4F46E5,
                    startTime = "09:30",
                    durationMinutes = 90,
                    repeatType = RepeatType.WEEKDAYS.name,
                    priority = TaskPriority.HIGH.name
                ),
                ActivityEntity(
                    title = "Afternoon Hydration Boost",
                    description = "Stay refreshed with 350ml water",
                    category = ActivityCategory.HYDRATION.name,
                    iconName = "water_drop",
                    colorHex = 0xFF0284C7,
                    startTime = "14:30",
                    durationMinutes = 5,
                    repeatType = RepeatType.DAILY.name,
                    priority = TaskPriority.MEDIUM.name
                ),
                ActivityEntity(
                    title = "Evening Wind-Down & Reading",
                    description = "Disconnect from blue screens and read 15 pages",
                    category = ActivityCategory.SLEEP.name,
                    iconName = "bedtime",
                    colorHex = 0xFF8B5CF6,
                    startTime = "22:00",
                    durationMinutes = 30,
                    repeatType = RepeatType.DAILY.name,
                    priority = TaskPriority.LOW.name
                )
            )

            for (activity in initialActivities) {
                database.activityDao().insertActivity(activity)
            }

            // Initial Habits
            val initialHabits = listOf(
                HabitEntity(
                    name = "Drink 2.5L Water Daily",
                    category = "Health",
                    targetDaysPerWeek = 7,
                    currentStreak = 1,
                    bestStreak = 3,
                    totalCompletions = 5
                ),
                HabitEntity(
                    name = "Daily Exercise / Walk",
                    category = "Fitness",
                    targetDaysPerWeek = 6,
                    currentStreak = 2,
                    bestStreak = 4,
                    totalCompletions = 8
                ),
                HabitEntity(
                    name = "Read or Study for 45 Mins",
                    category = "Learning",
                    targetDaysPerWeek = 5,
                    currentStreak = 1,
                    bestStreak = 5,
                    totalCompletions = 7
                )
            )
            for (habit in initialHabits) {
                database.habitDao().insertHabit(habit)
            }

            // Initial Achievements
            val achievements = listOf(
                AchievementEntity(
                    code = "FIRST_STEP",
                    titleEn = "First Step",
                    titleHi = "पहला कदम",
                    descEn = "Complete your first daily activity",
                    descHi = "अपनी पहली दैनिक गतिविधि पूरी करें",
                    icon = "flag",
                    points = 25,
                    isUnlocked = true,
                    unlockedAt = System.currentTimeMillis()
                ),
                AchievementEntity(
                    code = "HYDRATION_HERO",
                    titleEn = "Hydration Hero",
                    titleHi = "जल रक्षक",
                    descEn = "Reach 2000ml water in a single day",
                    descHi = "एक दिन में 2000ml जल लक्ष्य पूरा करें",
                    icon = "water_drop",
                    points = 35,
                    isUnlocked = false
                ),
                AchievementEntity(
                    code = "STREAK_WARRIOR",
                    titleEn = "Streak Warrior",
                    titleHi = "निरंतरता योद्धा",
                    descEn = "Keep a 3-day consecutive activity streak",
                    descHi = "3 दिन की निरंतर दिनचर्या बनाए रखें",
                    icon = "local_fire_department",
                    points = 50,
                    isUnlocked = false
                ),
                AchievementEntity(
                    code = "FOCUS_MASTER",
                    titleEn = "Focus Champion",
                    titleHi = "एकाग्रता सम्राट",
                    descEn = "Complete 2 deep focus sessions",
                    descHi = "2 गहन एकाग्रता सत्र पूरे करें",
                    icon = "timer",
                    points = 40,
                    isUnlocked = false
                ),
                AchievementEntity(
                    code = "HABIT_ARCHITECT",
                    titleEn = "Habit Architect",
                    titleHi = "आदत निर्माता",
                    descEn = "Create or personalize your routine",
                    descHi = "अपनी दिनचर्या को अनुकूलित करें",
                    icon = "emoji_events",
                    points = 30,
                    isUnlocked = true,
                    unlockedAt = System.currentTimeMillis()
                )
            )
            database.achievementDao().insertAchievements(achievements)

            // Initial User Stats
            val initialStats = UserStatsEntity(
                id = 1,
                totalPoints = 55,
                userLevel = 1,
                dailyHydrationGoalMl = 2500,
                dailyStudyGoalMinutes = 120,
                dailyExerciseGoalMinutes = 30,
                sleepBedtime = "22:30",
                sleepWakeTime = "06:30",
                subscriptionTier = "FREE",
                languageCode = "en"
            )
            database.userStatsDao().insertOrUpdate(initialStats)
        }
    }
}
