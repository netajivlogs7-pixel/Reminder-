package com.example

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.localization.AppLanguage
import com.example.localization.AppStrings
import com.example.ui.screens.AddActivityScreen
import com.example.ui.screens.AiAssistantScreen
import com.example.ui.screens.FocusScreen
import com.example.ui.screens.HealthScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.InsightsScreen
import com.example.ui.screens.PlannerScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.DailyLifeViewModel
import com.example.ui.viewmodel.ScreenTab

class MainActivity : ComponentActivity() {

    private val viewModel: DailyLifeViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val isDarkTheme by viewModel.isDarkMode.collectAsState()
            val currentTab by viewModel.currentTab.collectAsState()
            val language by viewModel.appLanguage.collectAsState()

            // Notification permission request for Android 13+
            val notificationPermissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission()
            ) { /* granted / denied handled */ }

            LaunchedEffect(Unit) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }

            // Back handler: pop to HOME if not on HOME
            if (currentTab != ScreenTab.HOME) {
                BackHandler {
                    viewModel.selectTab(ScreenTab.HOME)
                }
            }

            MyApplicationTheme(darkTheme = isDarkTheme) {
                Scaffold(
                    contentWindowInsets = WindowInsets.safeDrawing,
                    topBar = {
                        TopAppBar(
                            title = {
                                Text(
                                    text = when (currentTab) {
                                        ScreenTab.HOME -> "Daily Life Manager"
                                        ScreenTab.PLANNER -> AppStrings.get(language, "Planner", "योजना")
                                        ScreenTab.ADD_ACTIVITY -> AppStrings.get(language, "New Activity", "नई गतिविधि")
                                        ScreenTab.HEALTH -> AppStrings.get(language, "Health & Routines", "स्वास्थ्य व दिनचर्या")
                                        ScreenTab.FOCUS -> AppStrings.get(language, "Focus Mode", "फ़ोकस मोड")
                                        ScreenTab.INSIGHTS -> AppStrings.get(language, "Insights & Streaks", "प्रगति व आदतें")
                                        ScreenTab.AI_ASSISTANT -> AppStrings.get(language, "AI Assistant", "एआई सहायक")
                                        ScreenTab.SETTINGS -> AppStrings.get(language, "Settings", "सेटिंग्स")
                                    },
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleLarge
                                )
                            },
                            actions = {
                                // Shortcut to Health
                                IconButton(
                                    onClick = { viewModel.selectTab(ScreenTab.HEALTH) },
                                    modifier = Modifier.testTag("topbar_health_action")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Favorite,
                                        contentDescription = "Health",
                                        tint = if (currentTab == ScreenTab.HEALTH) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                // Shortcut to Focus
                                IconButton(
                                    onClick = { viewModel.selectTab(ScreenTab.FOCUS) },
                                    modifier = Modifier.testTag("topbar_focus_action")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Timer,
                                        contentDescription = "Focus",
                                        tint = if (currentTab == ScreenTab.FOCUS) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                // Shortcut to AI
                                IconButton(
                                    onClick = { viewModel.selectTab(ScreenTab.AI_ASSISTANT) },
                                    modifier = Modifier.testTag("topbar_ai_action")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Psychology,
                                        contentDescription = "AI Assistant",
                                        tint = if (currentTab == ScreenTab.AI_ASSISTANT) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        )
                    },
                    bottomBar = {
                        NavigationBar(
                            modifier = Modifier
                                .windowInsetsPadding(WindowInsets.navigationBars)
                                .testTag("bottom_nav_bar")
                        ) {
                            NavigationBarItem(
                                selected = currentTab == ScreenTab.HOME,
                                onClick = { viewModel.selectTab(ScreenTab.HOME) },
                                icon = {
                                    Icon(
                                        imageVector = if (currentTab == ScreenTab.HOME) Icons.Filled.Home else Icons.Outlined.Home,
                                        contentDescription = "Home"
                                    )
                                },
                                label = { Text(AppStrings.get(language, "Home", "होम")) },
                                modifier = Modifier.testTag("nav_home_item")
                            )

                            NavigationBarItem(
                                selected = currentTab == ScreenTab.PLANNER,
                                onClick = { viewModel.selectTab(ScreenTab.PLANNER) },
                                icon = {
                                    Icon(
                                        imageVector = if (currentTab == ScreenTab.PLANNER) Icons.Filled.CalendarMonth else Icons.Outlined.CalendarMonth,
                                        contentDescription = "Planner"
                                    )
                                },
                                label = { Text(AppStrings.get(language, "Planner", "योजना")) },
                                modifier = Modifier.testTag("nav_planner_item")
                            )

                            NavigationBarItem(
                                selected = currentTab == ScreenTab.ADD_ACTIVITY,
                                onClick = { viewModel.selectTab(ScreenTab.ADD_ACTIVITY) },
                                icon = {
                                    Icon(
                                        imageVector = Icons.Default.AddCircle,
                                        contentDescription = "Add",
                                        modifier = Modifier.size(28.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                },
                                label = { Text(AppStrings.get(language, "Add", "जोड़ें")) },
                                modifier = Modifier.testTag("nav_add_item")
                            )

                            NavigationBarItem(
                                selected = currentTab == ScreenTab.INSIGHTS,
                                onClick = { viewModel.selectTab(ScreenTab.INSIGHTS) },
                                icon = {
                                    Icon(
                                        imageVector = if (currentTab == ScreenTab.INSIGHTS) Icons.Filled.Insights else Icons.Outlined.Insights,
                                        contentDescription = "Insights"
                                    )
                                },
                                label = { Text(AppStrings.get(language, "Insights", "प्रगति")) },
                                modifier = Modifier.testTag("nav_insights_item")
                            )

                            NavigationBarItem(
                                selected = currentTab == ScreenTab.SETTINGS,
                                onClick = { viewModel.selectTab(ScreenTab.SETTINGS) },
                                icon = {
                                    Icon(
                                        imageVector = if (currentTab == ScreenTab.SETTINGS) Icons.Filled.Settings else Icons.Outlined.Settings,
                                        contentDescription = "Settings"
                                    )
                                },
                                label = { Text(AppStrings.get(language, "Settings", "सेटिंग्स")) },
                                modifier = Modifier.testTag("nav_settings_item")
                            )
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        Crossfade(targetState = currentTab, label = "screen_crossfade") { tab ->
                            when (tab) {
                                ScreenTab.HOME -> HomeScreen(viewModel = viewModel)
                                ScreenTab.PLANNER -> PlannerScreen(viewModel = viewModel)
                                ScreenTab.ADD_ACTIVITY -> AddActivityScreen(viewModel = viewModel)
                                ScreenTab.HEALTH -> HealthScreen(viewModel = viewModel)
                                ScreenTab.FOCUS -> FocusScreen(viewModel = viewModel)
                                ScreenTab.INSIGHTS -> InsightsScreen(viewModel = viewModel)
                                ScreenTab.AI_ASSISTANT -> AiAssistantScreen(viewModel = viewModel)
                                ScreenTab.SETTINGS -> SettingsScreen(viewModel = viewModel)
                            }
                        }
                    }
                }
            }
        }
    }
}
