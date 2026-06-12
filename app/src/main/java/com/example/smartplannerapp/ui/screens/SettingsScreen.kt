package com.example.smartplannerapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartplannerapp.data.AppSettings
import com.example.smartplannerapp.data.DataRepository

@Composable
fun SettingsScreen(
    repository: DataRepository,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val settings by repository.settings.collectAsState()
    val userProfile by repository.userProfile.collectAsState()

    var notificationsEnabled by remember { mutableStateOf(settings.notifications) }
    var dailyGoalReminder by remember { mutableStateOf(settings.dailyGoalReminder) }
    var studyTime by remember { mutableStateOf(settings.pomodoroStudy.toString()) }
    var breakTime by remember { mutableStateOf(settings.pomodoroBreak.toString()) }
    var themeSelection by remember { mutableStateOf(settings.theme) }

    val name = userProfile?.user?.name ?: repository.getSessionManager().getUserName().ifBlank { "User Student" }
    val email = userProfile?.user?.email ?: repository.getSessionManager().getUserEmail().ifBlank { "student@university.edu" }
    val initials = name.split(" ").mapNotNull { it.firstOrNull()?.toString() }.joinToString("").take(2).uppercase()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Settings",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Manage your preferences and focus parameters.",
                fontSize = 13.sp,
                color = Color.Gray,
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
            )
        }

        // Profile Section with Sign Out Button
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(25.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = initials, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(text = name, fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 1)
                            Text(text = email, fontSize = 12.sp, color = Color.Gray, maxLines = 1)
                        }
                    }
                    TextButton(
                        onClick = onLogout,
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Sign Out", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    }
                }
            }
        }

        // Pomodoro Preferences Section
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "Pomodoro Preferences", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Study Session (minutes)", fontSize = 13.sp)
                        OutlinedTextField(
                            value = studyTime,
                            onValueChange = { 
                                studyTime = it
                                it.toIntOrNull()?.let { minutes -> 
                                    repository.updateSettings(settings.copy(pomodoroStudy = minutes))
                                }
                            },
                            modifier = Modifier.width(70.dp),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Break Session (minutes)", fontSize = 13.sp)
                        OutlinedTextField(
                            value = breakTime,
                            onValueChange = { 
                                breakTime = it
                                it.toIntOrNull()?.let { minutes -> 
                                    repository.updateSettings(settings.copy(pomodoroBreak = minutes))
                                }
                            },
                            modifier = Modifier.width(70.dp),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }
            }
        }

        // Notification Preferences Section
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "Notifications & Reminders", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Push Notifications", fontSize = 13.sp)
                        Switch(
                            checked = notificationsEnabled,
                            onCheckedChange = { 
                                notificationsEnabled = it
                                repository.updateSettings(settings.copy(notifications = it))
                            }
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Daily Goals Reminders", fontSize = 13.sp)
                        Switch(
                            checked = dailyGoalReminder,
                            onCheckedChange = { 
                                dailyGoalReminder = it
                                repository.updateSettings(settings.copy(dailyGoalReminder = it))
                            }
                        )
                    }
                }
            }
        }

        // Theme Configuration Section
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "Theme & Appearance", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("light", "dark").forEach { theme ->
                            val isSelected = themeSelection == theme
                            Button(
                                onClick = { 
                                    themeSelection = theme
                                    repository.updateSettings(settings.copy(theme = theme))
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(text = theme.uppercase(), fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
