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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.ActivityCategory
import com.example.localization.AppStrings
import com.example.ui.theme.WarningOrange
import com.example.ui.viewmodel.DailyLifeViewModel
import com.example.ui.viewmodel.ScreenTab
import com.example.util.TimeUtils
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun PlannerScreen(
    viewModel: DailyLifeViewModel,
    modifier: Modifier = Modifier
) {
    val activities by viewModel.activities.collectAsState()
    val language by viewModel.appLanguage.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryFilter by remember { mutableStateOf<String?>(null) }
    var selectedDayIndex by remember { mutableStateOf(0) } // 0 = Today, 1 = Tomorrow, etc.

    // 7 days horizontal date selector
    val calendarDays = remember {
        val list = mutableListOf<Triple<String, String, String>>() // Day name, Date num, YYYY-MM-DD
        val cal = Calendar.getInstance()
        val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())
        val numFormat = SimpleDateFormat("d", Locale.getDefault())
        val isoFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        for (i in 0..6) {
            list.add(
                Triple(
                    dayFormat.format(cal.time),
                    numFormat.format(cal.time),
                    isoFormat.format(cal.time)
                )
            )
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        list
    }

    val filteredActivities = activities.filter { activity ->
        val matchesQuery = activity.title.contains(searchQuery, ignoreCase = true) ||
                activity.description.contains(searchQuery, ignoreCase = true)
        val matchesCategory = selectedCategoryFilter == null || activity.category == selectedCategoryFilter
        matchesQuery && matchesCategory
    }

    // Conflict detection: detect if two or more activities share the same start time
    val timeCountMap = activities.groupingBy { it.startTime }.eachCount()
    val conflictingTimes = timeCountMap.filter { it.value > 1 }.keys

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("planner_screen"),
        contentPadding = PaddingValues(bottom = 96.dp)
    ) {
        // Title Header
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Text(
                    text = AppStrings.get(language, "Daily & Weekly Planner", "दैनिक व साप्ताहिक योजना"),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = AppStrings.get(language, "Plan routines, balance time & apply templates", "दिनचर्या व्यवस्थित करें और समय संतुलित रखें"),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Horizontal 7-Day Picker
        item {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(calendarDays.size) { index ->
                    val (dayName, dayNum, iso) = calendarDays[index]
                    val isSelected = selectedDayIndex == index

                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier
                            .width(62.dp)
                            .clickable {
                                selectedDayIndex = index
                                viewModel.setSelectedDate(iso)
                            }
                            .testTag("planner_day_chip_$index")
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = dayName,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = dayNum,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        // Routine Templates Carousel
        item {
            Column(modifier = Modifier.padding(top = 12.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = AppStrings.get(language, "Ready Routine Templates", "तैयार रूटीन टेम्पलेट्स"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        TemplateChip(
                            title = AppStrings.get(language, "🌅 Morning Boost", "🌅 सुबह की शुरुआत"),
                            desc = AppStrings.get(language, "Sunlight & Healthy Breakfast", "धूप और पौष्टिक नाश्ता"),
                            onClick = { viewModel.applyTemplate("MORNING_BOOST") }
                        )
                    }
                    item {
                        TemplateChip(
                            title = AppStrings.get(language, "📚 Student Sprint", "📚 छात्र अध्ययन योजना"),
                            desc = AppStrings.get(language, "Concepts & Problem Solving", "गहन अध्ययन और अभ्यास"),
                            onClick = { viewModel.applyTemplate("STUDENT_SPRINT") }
                        )
                    }
                    item {
                        TemplateChip(
                            title = AppStrings.get(language, "💼 Office Focus", "💼 कार्यालय उत्पादकता"),
                            desc = AppStrings.get(language, "Standup & Deep Work Block", "मीटिंग और केंद्रित कार्य"),
                            onClick = { viewModel.applyTemplate("OFFICE_FOCUS") }
                        )
                    }
                }
            }
        }

        // Conflict Warning Banner
        if (conflictingTimes.isNotEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = WarningOrange.copy(alpha = 0.15f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = WarningOrange,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        val conflictStr = conflictingTimes.map { TimeUtils.formatTo12Hour(it) }.joinToString(", ")
                        Text(
                            text = AppStrings.get(
                                language,
                                "Schedule Conflict: Multiple tasks overlapping at $conflictStr. Consider rescheduling.",
                                "समय टकराव: $conflictStr पर एक से अधिक कार्य हैं। समय बदलें।"
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // Search & Filter
        item {
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text(AppStrings.get(language, "Search activities...", "गतिविधि खोजें...")) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("planner_search_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Category Chips
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = selectedCategoryFilter == null,
                            onClick = { selectedCategoryFilter = null },
                            label = { Text(AppStrings.get(language, "All", "सभी")) }
                        )
                    }
                    items(ActivityCategory.values()) { category ->
                        FilterChip(
                            selected = selectedCategoryFilter == category.name,
                            onClick = {
                                selectedCategoryFilter = if (selectedCategoryFilter == category.name) null else category.name
                            },
                            label = { Text(if (language == com.example.localization.AppLanguage.HINDI) category.displayNameHi else category.displayNameEn) }
                        )
                    }
                }
            }
        }

        // Filtered Schedule List
        if (filteredActivities.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = AppStrings.get(language, "No activities found matching criteria", "कोई गतिविधि नहीं मिली"),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(filteredActivities, key = { it.id }) { activity ->
                ActivityCard(
                    activity = activity,
                    onToggleComplete = { viewModel.toggleActivityCompletion(activity) },
                    onDelete = { viewModel.deleteActivity(activity.id) }
                )
            }
        }
    }
}

@Composable
fun TemplateChip(title: String, desc: String, onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)),
        modifier = Modifier
            .width(220.dp)
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = desc,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "Apply Template",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
