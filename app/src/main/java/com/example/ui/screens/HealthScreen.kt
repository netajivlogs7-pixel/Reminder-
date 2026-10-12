package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.StudySessionEntity
import com.example.data.model.WorkoutSessionEntity
import com.example.localization.AppStrings
import com.example.ui.theme.WaterBlue
import com.example.ui.viewmodel.DailyLifeViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HealthScreen(
    viewModel: DailyLifeViewModel,
    modifier: Modifier = Modifier
) {
    val language by viewModel.appLanguage.collectAsState()
    val todayHydration by viewModel.todayHydration.collectAsState()
    val todayWorkouts by viewModel.todayWorkouts.collectAsState()
    val todayStudy by viewModel.todayStudy.collectAsState()
    val userStats by viewModel.userStats.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) } // 0=Water, 1=Workout, 2=Study, 3=Sleep

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("health_screen")
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
            Text(
                text = AppStrings.get(language, "Health & Daily Routines", "स्वास्थ्य व दैनिक दिनचर्या"),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = AppStrings.get(language, "Hydration, workouts, deep study & restful sleep", "जल, व्यायाम, अध्ययन और संतुलित नींद"),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        TabRow(selectedTabIndex = selectedTab) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text(AppStrings.get(language, "Water", "जल")) },
                icon = { Icon(Icons.Default.WaterDrop, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text(AppStrings.get(language, "Workout", "व्यायाम")) },
                icon = { Icon(Icons.Default.DirectionsRun, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                text = { Text(AppStrings.get(language, "Study", "पढ़ाई")) },
                icon = { Icon(Icons.Default.MenuBook, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )
            Tab(
                selected = selectedTab == 3,
                onClick = { selectedTab = 3 },
                text = { Text(AppStrings.get(language, "Sleep", "नींद")) },
                icon = { Icon(Icons.Default.Bedtime, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )
        }

        when (selectedTab) {
            0 -> WaterTabContent(viewModel, todayHydration, userStats?.dailyHydrationGoalMl ?: 2500)
            1 -> WorkoutTabContent(viewModel, todayWorkouts)
            2 -> StudyTabContent(viewModel, todayStudy)
            3 -> SleepTabContent(userStats?.sleepBedtime ?: "22:30", userStats?.sleepWakeTime ?: "06:30")
        }
    }
}

@Composable
fun WaterTabContent(
    viewModel: DailyLifeViewModel,
    todayHydration: List<com.example.data.model.HydrationLogEntity>,
    goalMl: Int
) {
    val language by viewModel.appLanguage.collectAsState()
    val totalWaterMl = todayHydration.sumOf { it.amountMl }
    val progress = (totalWaterMl.toFloat() / goalMl).coerceIn(0f, 1f)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = WaterBlue.copy(alpha = 0.12f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.WaterDrop,
                        contentDescription = null,
                        tint = WaterBlue,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "$totalWaterMl ml",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = WaterBlue
                    )
                    Text(
                        text = "Goal: $goalMl ml (${(progress * 100).toInt()}% completed)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp)),
                        color = WaterBlue,
                        trackColor = WaterBlue.copy(alpha = 0.2f)
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        item {
            Text(
                text = AppStrings.get(language, "Quick Add Water", "त्वरित जल जोड़ें"),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                listOf(150, 250, 500, 750).forEach { amount ->
                    Button(
                        onClick = { viewModel.addWater(amount) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("add_water_$amount"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("+$amount")
                    }
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }

        item {
            Text(
                text = AppStrings.get(language, "Today's Intake History", "आज का जल इतिहास"),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (todayHydration.isEmpty()) {
            item {
                Text(
                    text = AppStrings.get(language, "No water logged today. Tap a button above to log!", "आज कोई जल दर्ज नहीं किया गया। ऊपर टैप करें!"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            }
        } else {
            items(todayHydration, key = { it.id }) { log ->
                val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
                val timeStr = timeFormat.format(Date(log.timestamp))

                Card(
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.WaterDrop, contentDescription = null, tint = WaterBlue, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("+${log.amountMl} ml", fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("at $timeStr", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        IconButton(onClick = { viewModel.deleteWater(log.id) }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Delete, contentDescription = "Undo", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WorkoutTabContent(
    viewModel: DailyLifeViewModel,
    todayWorkouts: List<WorkoutSessionEntity>
) {
    val language by viewModel.appLanguage.collectAsState()
    var workoutType by remember { mutableStateOf("Outdoor Walk") }
    var durationMinutes by remember { mutableStateOf("30") }
    var distanceKm by remember { mutableStateOf("2.5") }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFEA580C).copy(alpha = 0.1f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = AppStrings.get(language, "Log Workout / Exercise", "व्यायाम दर्ज करें"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFEA580C)
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = workoutType,
                        onValueChange = { workoutType = it },
                        label = { Text("Exercise Type (e.g. Brisk Walk, Yoga, Gym)") },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = durationMinutes,
                            onValueChange = { durationMinutes = it },
                            label = { Text("Duration (mins)") },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = distanceKm,
                            onValueChange = { distanceKm = it },
                            label = { Text("Distance (km)") },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    val dur = durationMinutes.toIntOrNull() ?: 30
                    val estCalories = dur * 6 // ~6 kcal/min estimate clearly labeled

                    Text(
                        text = "Estimated: ~$estCalories kcal (Manual estimate based on standard metabolic rate)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            viewModel.addWorkout(
                                WorkoutSessionEntity(
                                    workoutType = workoutType,
                                    durationMinutes = dur,
                                    distanceKm = distanceKm.toFloatOrNull() ?: 0f,
                                    caloriesEstimated = estCalories,
                                    date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                                )
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("save_workout_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(AppStrings.get(language, "Save Workout Session", "व्यायाम सत्र सुरक्षित करें"))
                    }
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }

        item {
            Text(
                text = AppStrings.get(language, "Today's Workouts", "आज के व्यायाम"),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (todayWorkouts.isEmpty()) {
            item {
                Text(
                    text = AppStrings.get(language, "No workouts logged today yet.", "आज कोई व्यायाम दर्ज नहीं किया गया।"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(todayWorkouts) { workout ->
                Card(
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(workout.workoutType, fontWeight = FontWeight.Bold)
                            Text("${workout.durationMinutes} mins • ${workout.distanceKm} km • ~${workout.caloriesEstimated} kcal", style = MaterialTheme.typography.bodySmall)
                        }
                        Icon(Icons.Default.DirectionsRun, contentDescription = null, tint = Color(0xFFEA580C))
                    }
                }
            }
        }
    }
}

@Composable
fun StudyTabContent(
    viewModel: DailyLifeViewModel,
    todayStudy: List<StudySessionEntity>
) {
    val language by viewModel.appLanguage.collectAsState()
    var subject by remember { mutableStateOf("Core Concepts / Research") }
    var duration by remember { mutableStateOf("45") }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF4F46E5).copy(alpha = 0.1f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = AppStrings.get(language, "Log Deep Study / Work", "अध्ययन सत्र दर्ज करें"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4F46E5)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = subject,
                        onValueChange = { subject = it },
                        label = { Text("Subject / Topic") },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = duration,
                        onValueChange = { duration = it },
                        label = { Text("Duration (minutes)") },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            viewModel.addStudySession(
                                StudySessionEntity(
                                    subject = subject,
                                    durationMinutes = duration.toIntOrNull() ?: 45,
                                    date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                                )
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("save_study_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(AppStrings.get(language, "Save Study Session", "अध्ययन सत्र सुरक्षित करें"))
                    }
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }

        item {
            Text(
                text = AppStrings.get(language, "Today's Study Sessions", "आज के अध्ययन सत्र"),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (todayStudy.isEmpty()) {
            item {
                Text(
                    text = AppStrings.get(language, "No study sessions logged today yet.", "आज कोई अध्ययन सत्र दर्ज नहीं किया गया।"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(todayStudy) { session ->
                Card(
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(session.subject, fontWeight = FontWeight.Bold)
                            Text("${session.durationMinutes} mins deep work", style = MaterialTheme.typography.bodySmall)
                        }
                        Icon(Icons.Default.MenuBook, contentDescription = null, tint = Color(0xFF4F46E5))
                    }
                }
            }
        }
    }
}

@Composable
fun SleepTabContent(bedtime: String, wakeTime: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF8B5CF6).copy(alpha = 0.12f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Default.Bedtime, contentDescription = null, tint = Color(0xFF8B5CF6), modifier = Modifier.size(48.dp))
                Spacer(modifier = Modifier.height(10.dp))
                Text("Sleep Routine & Circadian Rhythm", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Bedtime: $bedtime  •  Wake-up: $wakeTime", style = MaterialTheme.typography.titleMedium, color = Color(0xFF8B5CF6), fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "Target: 8 hours restful recovery. Wind-down reminders trigger 30 minutes before bedtime.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
