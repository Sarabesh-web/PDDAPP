package com.example.smartplannerapp.ui.main

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import androidx.navigation3.runtime.NavKey
import com.example.smartplannerapp.data.DataRepository
import com.example.smartplannerapp.ui.screens.*

enum class AppTab(val title: String, val icon: ImageVector) {
    DASHBOARD("Home", Icons.Default.Home),
    PLANNER("Planner", Icons.Default.DateRange),
    TASKS("Tasks", Icons.AutoMirrored.Filled.List),
    POMODORO("Focus", Icons.Default.PlayArrow),
    NOTES("Notes", Icons.Default.Edit),
    ANALYTICS("Stats", Icons.Default.Info),
    SETTINGS("Settings", Icons.Default.Settings),
    ADMIN("Admin", Icons.Default.Lock)
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

        // Primary 5 tabs for bottom navigation bar to avoid crowding/text wrapping
        val bottomNavTabs = listOf(
            AppTab.DASHBOARD,
            AppTab.PLANNER,
            AppTab.TASKS,
            AppTab.POMODORO,
            AppTab.NOTES
        )

        Scaffold(
            modifier = modifier,
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ) {
                    bottomNavTabs.forEach { tab ->
                        val selected = (currentTab == tab)
                        NavigationBarItem(
                            selected = selected,
                            onClick = { currentTab = tab },
                            icon = { Icon(tab.icon, contentDescription = tab.title) },
                            label = {
                                Text(
                                    text = tab.title,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    fontSize = 11.sp
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
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
                    onNavigateToStats = { currentTab = AppTab.ANALYTICS },
                    onNavigateToSettings = { currentTab = AppTab.SETTINGS },
                    onNavigateToAdmin = { currentTab = AppTab.ADMIN },
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
                AppTab.ADMIN -> AdminScreen(
                    repository = activeRepository,
                    modifier = screenModifier
                )
            }
        }
    }
}
