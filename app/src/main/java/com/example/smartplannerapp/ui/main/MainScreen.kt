package com.example.smartplannerapp.ui.main

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation3.runtime.NavKey
import com.example.smartplannerapp.data.DataRepository
import com.example.smartplannerapp.ui.screens.*

enum class AppTab(val title: String, val icon: ImageVector) {
    DASHBOARD("Home", Icons.Default.Home),
    PLANNER("Planner", Icons.Default.DateRange),
    TASKS("Tasks", Icons.Default.List),
    POMODORO("Focus", Icons.Default.PlayArrow),
    NOTES("Notes", Icons.Default.Edit),
    ANALYTICS("Stats", Icons.Default.Info),
    SETTINGS("Settings", Icons.Default.Settings)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onItemClick: (NavKey) -> Unit,
    modifier: Modifier = Modifier,
    repository: DataRepository? = null
) {
    val context = androidx.compose.ui.platform.LocalContext.current.applicationContext
    val activeRepository = repository ?: remember { DataRepository(context) }
    var isLoggedIn by remember { mutableStateOf(activeRepository.isLoggedIn()) }

    if (!isLoggedIn) {
        AuthScreen(
            repository = activeRepository,
            onAuthSuccess = {
                isLoggedIn = true
            },
            modifier = modifier
        )
    } else {
        var currentTab by remember { mutableStateOf(AppTab.DASHBOARD) }

        Scaffold(
            bottomBar = {
                NavigationBar {
                    AppTab.values().forEach { tab ->
                        NavigationBarItem(
                            selected = currentTab == tab,
                            onClick = { currentTab = tab },
                            icon = { Icon(tab.icon, contentDescription = tab.title) },
                            label = { Text(tab.title) }
                        )
                    }
                }
            }
        ) { paddingValues ->
            val screenModifier = Modifier.padding(paddingValues)

            when (currentTab) {
                AppTab.DASHBOARD -> DashboardScreen(
                    repository = activeRepository,
                    onNavigateToTasks = { currentTab = AppTab.TASKS },
                    modifier = screenModifier
                )
                AppTab.PLANNER -> PlannerScreen(
                    repository = activeRepository,
                    modifier = screenModifier
                )
                AppTab.TASKS -> TasksScreen(
                    repository = activeRepository,
                    modifier = screenModifier
                )
                AppTab.POMODORO -> PomodoroScreen(
                    repository = activeRepository,
                    modifier = screenModifier
                )
                AppTab.NOTES -> NotesScreen(
                    repository = activeRepository,
                    modifier = screenModifier
                )
                AppTab.ANALYTICS -> AnalyticsScreen(
                    repository = activeRepository,
                    modifier = screenModifier
                )
                AppTab.SETTINGS -> SettingsScreen(
                    repository = activeRepository,
                    onLogout = {
                        activeRepository.logout()
                        isLoggedIn = false
                    },
                    modifier = screenModifier
                )
            }
        }
    }
}
