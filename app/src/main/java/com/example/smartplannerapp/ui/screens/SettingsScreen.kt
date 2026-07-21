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
import com.example.smartplannerapp.data.DataRepository
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    repository: DataRepository,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val settings by repository.settings.collectAsState()
    val userProfile by repository.userProfile.collectAsState()
    val scope = rememberCoroutineScope()

    var notificationsEnabled by remember { mutableStateOf(settings.notifications) }
    var dailyGoalReminder by remember { mutableStateOf(settings.dailyGoalReminder) }
    var studyTime by remember { mutableStateOf(settings.pomodoroStudy.toString()) }
    var breakTime by remember { mutableStateOf(settings.pomodoroBreak.toString()) }
    var themeSelection by remember { mutableStateOf(settings.theme) }

    var nameText by remember { mutableStateOf("") }
    var bioText by remember { mutableStateOf("") }
    var universityText by remember { mutableStateOf("") }
    var majorText by remember { mutableStateOf("") }
    var yearText by remember { mutableStateOf("") }

    LaunchedEffect(userProfile) {
        userProfile?.let { me ->
            nameText = me.user.name
            bioText = me.profile.bio
            universityText = me.profile.university
            majorText = me.profile.major
            yearText = me.profile.year
            if (me.profile.theme.isNotBlank()) {
                themeSelection = me.profile.theme
            }
        }
    }

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
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
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
                            Text(text = initials, color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(text = name, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
                            Text(text = email, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
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

        // Profile Editing Form Section
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(text = "Edit Academic Profile", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)

                    OutlinedTextField(
                        value = nameText,
                        onValueChange = { nameText = it },
                        label = { Text("Display Name", fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = universityText,
                        onValueChange = { universityText = it },
                        label = { Text("University / School", fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = majorText,
                        onValueChange = { majorText = it },
                        label = { Text("Major / Field of Study", fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = yearText,
                        onValueChange = { yearText = it },
                        label = { Text("Academic Year", fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = bioText,
                        onValueChange = { bioText = it },
                        label = { Text("Short Biography", fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )

                    var isSaving by remember { mutableStateOf(false) }
                    var feedbackMessage by remember { mutableStateOf("") }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (feedbackMessage.isNotEmpty()) {
                            Text(text = feedbackMessage, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.primary)
                        } else {
                            Spacer(modifier = Modifier.weight(1f))
                        }

                        Button(
                            onClick = {
                                isSaving = true
                                feedbackMessage = ""
                                scope.launch {
                                    val err = repository.updateProfile(
                                        name = nameText.trim(),
                                        bio = bioText.trim(),
                                        university = universityText.trim(),
                                        major = majorText.trim(),
                                        year = yearText.trim()
                                    )
                                    isSaving = false
                                    if (err == null) {
                                        feedbackMessage = "Profile updated!"
                                    } else {
                                        feedbackMessage = "Update failed: $err"
                                    }
                                }
                            },
                            enabled = !isSaving,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            if (isSaving) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                            } else {
                                Text("Save Profile", fontSize = 13.sp)
                            }
                        }
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
                    Text(text = "Pomodoro Preferences", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Study Session (minutes)", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
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
                        Text(text = "Break Session (minutes)", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
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
                    Text(text = "Notifications & Reminders", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Push Notifications", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
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
                        Text(text = "Daily Goals Reminders", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
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
                    Text(text = "Theme & Appearance", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
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
                                    contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(text = theme.uppercase(), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
