package com.example.ui.screens

import android.app.Activity
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Title
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.ActivityCategory
import com.example.data.model.ActivityEntity
import com.example.data.model.RepeatType
import com.example.data.model.TaskPriority
import com.example.localization.AppLanguage
import com.example.localization.AppStrings
import com.example.ui.viewmodel.DailyLifeViewModel
import com.example.ui.viewmodel.ScreenTab
import com.example.util.AudioUtils
import com.example.util.TimeUtils

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AddActivityScreen(
    viewModel: DailyLifeViewModel,
    modifier: Modifier = Modifier
) {
    val language by viewModel.appLanguage.collectAsState()
    val isPlayingAudio by viewModel.isPlayingAudio.collectAsState()
    val context = LocalContext.current

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(ActivityCategory.ROUTINE) }
    var startTime by remember { mutableStateOf("08:00") }
    var showTimePickerDialog by remember { mutableStateOf(false) }
    var durationMinutes by remember { mutableFloatStateOf(30f) }
    var selectedRepeat by remember { mutableStateOf(RepeatType.DAILY) }
    var selectedPriority by remember { mutableStateOf(TaskPriority.MEDIUM) }
    var reminderOffsetMinutes by remember { mutableIntStateOf(10) }

    // Custom Audio & Sound Tone State for Activity Reminder
    var customAudioUri by remember { mutableStateOf("") }
    var customAudioName by remember { mutableStateOf("") }

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successSaved by remember { mutableStateOf(false) }

    // Launcher for System Ringtones / Alarms Picker
    val ringtonePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val pickedUri: Uri? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java)
            } else {
                @Suppress("DEPRECATION")
                result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            }
            if (pickedUri != null) {
                val name = AudioUtils.getAudioFileName(context, pickedUri)
                customAudioUri = pickedUri.toString()
                customAudioName = name
            }
        }
    }

    // Launcher for Custom Audio File Picker (MP3 / WAV / Songs from Device Storage)
    val audioFilePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            AudioUtils.takePersistableUriPermissionSafely(context, uri)
            val name = AudioUtils.getAudioFileName(context, uri)
            customAudioUri = uri.toString()
            customAudioName = name
        }
    }

    // Clock Time Picker Dialog (Material 3 with Clock Dial & AM/PM)
    if (showTimePickerDialog) {
        val initialParts = startTime.split(":")
        val initH = initialParts.getOrNull(0)?.toIntOrNull() ?: 8
        val initM = initialParts.getOrNull(1)?.toIntOrNull() ?: 0
        val timePickerState = rememberTimePickerState(
            initialHour = initH,
            initialMinute = initM,
            is24Hour = false
        )

        AlertDialog(
            onDismissRequest = { showTimePickerDialog = false },
            confirmButton = {
                Button(
                    onClick = {
                        startTime = String.format("%02d:%02d", timePickerState.hour, timePickerState.minute)
                        showTimePickerDialog = false
                    },
                    modifier = Modifier.testTag("confirm_time_picker_button")
                ) {
                    Text(AppStrings.get(language, "Set Time", "समय सेट करें"), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePickerDialog = false }) {
                    Text(AppStrings.get(language, "Cancel", "रद्द करें"))
                }
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AccessTime, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = AppStrings.get(language, "Pick Time with Clock & AM/PM", "घड़ी से समय व AM/PM चुनें"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    TimePicker(
                        state = timePickerState,
                        modifier = Modifier.testTag("material_time_picker")
                    )
                }
            }
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("add_activity_screen"),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp)
    ) {
        item {
            Text(
                text = AppStrings.get(language, "Create Custom Activity", "कस्टम गतिविधि बनाएं"),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = AppStrings.get(language, "Define your routine, reminders, and target duration", "अपनी दिनचर्या, रिमाइंडर और समय निर्धारित करें"),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Title Input
        item {
            OutlinedTextField(
                value = title,
                onValueChange = {
                    title = it
                    errorMessage = null
                },
                label = { Text(AppStrings.get(language, "Activity Title *", "गतिविधि का नाम *")) },
                placeholder = { Text(AppStrings.get(language, "e.g., Morning Yoga & Meditation", "उदा. सुबह का योग और ध्यान")) },
                leadingIcon = { Icon(Icons.Default.Title, contentDescription = null) },
                shape = RoundedCornerShape(16.dp),
                isError = errorMessage != null && title.isBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("activity_title_input")
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Description Input
        item {
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text(AppStrings.get(language, "Notes / Description (Optional)", "विवरण या नोट्स (वैकल्पिक)")) },
                placeholder = { Text(AppStrings.get(language, "Specific goals or steps...", "लक्ष्य या चरण...")) },
                leadingIcon = { Icon(Icons.Default.Description, contentDescription = null) },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("activity_description_input")
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Category Selection
        item {
            Text(
                text = AppStrings.get(language, "Category", "श्रेणी"),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ActivityCategory.values().forEach { category ->
                    FilterChip(
                        selected = selectedCategory == category,
                        onClick = { selectedCategory = category },
                        label = {
                            Text(
                                if (language == AppLanguage.HINDI) category.displayNameHi else category.displayNameEn
                            )
                        }
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Start Time Section with Clock Dialog & AM/PM
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("time_picker_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AccessTime,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = AppStrings.get(language, "Activity Start Time", "गतिविधि का समय"),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // AM / PM Quick Toggle Chips
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            val isCurrentlyAm = TimeUtils.isAm(startTime)
                            FilterChip(
                                selected = isCurrentlyAm,
                                onClick = {
                                    startTime = TimeUtils.setAmPm(startTime, true)
                                },
                                label = { Text("AM", fontWeight = FontWeight.Bold) },
                                modifier = Modifier.testTag("am_chip")
                            )
                            FilterChip(
                                selected = !isCurrentlyAm,
                                onClick = {
                                    startTime = TimeUtils.setAmPm(startTime, false)
                                },
                                label = { Text("PM", fontWeight = FontWeight.Bold) },
                                modifier = Modifier.testTag("pm_chip")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Prominent Display of Formatted 12-Hour Time & Clock Button
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 2.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showTimePickerDialog = true }
                            .testTag("clock_time_display_surface")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = AppStrings.get(language, "Selected Time", "चुना गया समय"),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = TimeUtils.formatTo12Hour(startTime),
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            Button(
                                onClick = { showTimePickerDialog = true },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.testTag("open_clock_picker_button")
                            ) {
                                Icon(Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(AppStrings.get(language, "Clock", "घड़ी"))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = AppStrings.get(language, "Quick Presets:", "त्वरित समय विकल्प:"),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Quick time presets with AM/PM labels
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            "06:00" to "06:00 AM",
                            "08:00" to "08:00 AM",
                            "10:00" to "10:00 AM",
                            "13:00" to "01:00 PM",
                            "18:00" to "06:00 PM",
                            "22:00" to "10:00 PM"
                        ).forEach { (timeVal, label) ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (startTime == timeVal) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                                modifier = Modifier.clickable { startTime = timeVal }
                            ) {
                                Text(
                                    text = label,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (startTime == timeVal) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Duration Slider
        item {
            Text(
                text = "${AppStrings.get(language, "Duration", "अवधि")}: ${durationMinutes.toInt()} ${AppStrings.get(language, "minutes", "मिनट")}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Slider(
                value = durationMinutes,
                onValueChange = { durationMinutes = it },
                valueRange = 5f..180f,
                steps = 34,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("activity_duration_slider")
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Repeat Type
        item {
            Text(
                text = AppStrings.get(language, "Repeat Frequency", "दोहराव आवृत्ति"),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                RepeatType.values().forEach { repeat ->
                    FilterChip(
                        selected = selectedRepeat == repeat,
                        onClick = { selectedRepeat = repeat },
                        label = {
                            Text(
                                if (language == AppLanguage.HINDI) repeat.displayNameHi else repeat.displayNameEn
                            )
                        }
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Priority
        item {
            Text(
                text = AppStrings.get(language, "Priority", "प्राथमिकता"),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TaskPriority.values().forEach { priority ->
                    FilterChip(
                        selected = selectedPriority == priority,
                        onClick = { selectedPriority = priority },
                        label = {
                            Text(
                                if (language == AppLanguage.HINDI) priority.displayNameHi else priority.displayNameEn
                            )
                        }
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Reminder Offset
        item {
            Text(
                text = AppStrings.get(language, "Reminder Notification", "रिमाइंडर सूचना"),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(0, 5, 10, 15, 30).forEach { mins ->
                    FilterChip(
                        selected = reminderOffsetMinutes == mins,
                        onClick = { reminderOffsetMinutes = mins },
                        label = { Text(if (mins == 0) "At time" else "$mins m before") }
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Reminder Sound & Custom Audio Option (कस्टम ऑडियो विकल्प)
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("activity_custom_audio_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.MusicNote,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = AppStrings.get(language, "Reminder Sound & Custom Audio", "रिमाइंडर ऑडियो व कस्टम टोन"),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = AppStrings.get(language, "Select custom MP3, song or ringtone", "कस्टम MP3, गाना या रिंगटोन जोड़ें"),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Preview Listen / Stop Button
                        OutlinedButton(
                            onClick = {
                                if (isPlayingAudio) {
                                    viewModel.stopAudioPreview()
                                } else {
                                    viewModel.playAudioPreview(customAudioUri)
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("preview_activity_audio_button")
                        ) {
                            Icon(
                                imageVector = if (isPlayingAudio) Icons.Default.Stop else Icons.Default.VolumeUp,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = if (isPlayingAudio) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isPlayingAudio) "Stop" else AppStrings.get(language, "Listen", "सुनें"),
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Selected Audio Tone Box
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 2.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.NotificationsActive,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = AppStrings.get(language, "Selected Audio Tone:", "चुनी गई ऑडियो टोन:"),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = if (customAudioName.isNotBlank()) customAudioName else AppStrings.get(language, "Default Notification Sound", "डिफ़ॉल्ट नोटिफिकेशन आवाज़"),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1
                                    )
                                }
                            }

                            if (customAudioUri.isNotBlank()) {
                                TextButton(
                                    onClick = {
                                        customAudioUri = ""
                                        customAudioName = ""
                                    }
                                ) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text(AppStrings.get(language, "Reset", "रीसेट"))
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Presets selection
                    Text(
                        text = AppStrings.get(language, "Quick Tones:", "तैयार विकल्प:"),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val presets = AudioUtils.getPresets()
                        presets.forEach { preset ->
                            val isSelected = (preset.uriString.isBlank() && customAudioUri.isBlank()) ||
                                    (preset.uriString.isNotBlank() && customAudioUri == preset.uriString)
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    customAudioUri = preset.uriString
                                    customAudioName = if (language == AppLanguage.HINDI) preset.nameHi else preset.nameEn
                                },
                                label = { Text(if (language == AppLanguage.HINDI) preset.nameHi else preset.nameEn) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Buttons to Pick Custom Audio: Storage MP3 or System Ringtones
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                try {
                                    audioFilePickerLauncher.launch(arrayOf("audio/*"))
                                } catch (e: Exception) {
                                    // Fallback
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("pick_storage_audio_activity_btn")
                        ) {
                            Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = AppStrings.get(language, "Pick MP3 / File", "फ़ोन से MP3/गाना"),
                                style = MaterialTheme.typography.labelMedium
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                                    putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION or RingtoneManager.TYPE_ALARM)
                                    putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Select Reminder Sound")
                                    putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                                    putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                                    if (customAudioUri.isNotBlank()) {
                                        putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Uri.parse(customAudioUri))
                                    }
                                }
                                ringtonePickerLauncher.launch(intent)
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("pick_ringtone_activity_btn")
                        ) {
                            Icon(Icons.Default.MusicNote, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = AppStrings.get(language, "Ringtones", "रिंगटोन चुनें"),
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }

        // Error message if any
        if (errorMessage != null) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    Text(
                        text = errorMessage ?: "",
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        // Success banner
        if (successSaved) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF10B981).copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF10B981))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = AppStrings.get(language, "Activity scheduled & reminder registered!", "गतिविधि जोड़ी गई और रिमाइंडर सेट हो गया!"),
                            color = Color(0xFF10B981),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Submit Button
        item {
            Button(
                onClick = {
                    if (title.isBlank()) {
                        errorMessage = if (language == AppLanguage.HINDI) "कृपया गतिविधि का नाम दर्ज करें" else "Please enter an activity title"
                        return@Button
                    }
                    if (!startTime.matches(Regex("\\d{1,2}:\\d{2}"))) {
                        errorMessage = if (language == AppLanguage.HINDI) "अमान्य समय प्रारूप (HH:mm उपयोग करें)" else "Invalid time format (Use HH:mm)"
                        return@Button
                    }

                    val color = when (selectedCategory) {
                        ActivityCategory.WORKOUT -> 0xFFEA580C
                        ActivityCategory.HYDRATION -> 0xFF0284C7
                        ActivityCategory.STUDY -> 0xFF4F46E5
                        ActivityCategory.SLEEP -> 0xFF8B5CF6
                        ActivityCategory.MEDICATION -> 0xFFEF4444
                        ActivityCategory.ROUTINE -> 0xFF0D9488
                        ActivityCategory.CUSTOM -> 0xFF6366F1
                    }

                    val activity = ActivityEntity(
                        title = title.trim(),
                        description = description.trim(),
                        category = selectedCategory.name,
                        startTime = startTime.trim(),
                        durationMinutes = durationMinutes.toInt(),
                        repeatType = selectedRepeat.name,
                        priority = selectedPriority.name,
                        reminderMinutesBefore = reminderOffsetMinutes,
                        customSoundUri = customAudioUri,
                        customSoundName = customAudioName,
                        colorHex = color
                    )

                    viewModel.addActivity(activity)
                    successSaved = true
                    title = ""
                    description = ""
                    customAudioUri = ""
                    customAudioName = ""
                },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("save_activity_button")
            ) {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = AppStrings.get(language, "Save & Schedule Activity", "सुरक्षित करें और शेड्यूल करें"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}
