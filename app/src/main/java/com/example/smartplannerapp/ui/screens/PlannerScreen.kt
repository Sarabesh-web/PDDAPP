package com.example.smartplannerapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartplannerapp.data.DataRepository
import com.example.smartplannerapp.data.ScheduleItem
import com.example.smartplannerapp.data.isCompleted

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlannerScreen(
    repository: DataRepository,
    modifier: Modifier = Modifier
) {
    val schedules by repository.schedules.collectAsState()
    val subjects by repository.subjects.collectAsState()

    val daysOfWeek = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
    var selectedDay by remember { mutableStateOf("Monday") }
    var showAddDialog by remember { mutableStateOf(false) }

    var newTitle by remember { mutableStateOf("") }
    var newStartTime by remember { mutableStateOf("09:00") }
    var newEndTime by remember { mutableStateOf("10:00") }
    var newPriority by remember { mutableStateOf("medium") }
    var newSubject by remember { mutableStateOf("") }

    if (newSubject.isEmpty() && subjects.isNotEmpty()) {
        newSubject = subjects.first().name
    }

    val daySchedules = schedules.filter { it.day == selectedDay }
        .sortedBy { it.startTime }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    newTitle = ""
                    newStartTime = "09:00"
                    newEndTime = "10:00"
                    newPriority = "medium"
                    if (subjects.isNotEmpty()) newSubject = subjects.first().name
                    showAddDialog = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Session")
            }
        },
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Text(
                text = "Study Planner",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Organize your study sessions throughout the week.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
            )

            // Horizontal Days Row
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(daysOfWeek) { day ->
                    val isSelected = selectedDay == day
                    Button(
                        onClick = { selectedDay = day },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(day.substring(0, 3), fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Schedules list
            if (daySchedules.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "No study sessions scheduled for today.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(daySchedules) { item ->
                        ScheduleItemRow(
                            item = item,
                            onToggle = { repository.toggleSchedule(item.id) },
                            onDelete = { repository.deleteSchedule(item.id) }
                        )
                    }
                }
            }
        }
    }

    // Add Session Dialog
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add Study Session") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newTitle,
                        onValueChange = { newTitle = it },
                        label = { Text("Session Title") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("Subject", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    // Dropdown simulation with buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        subjects.take(3).forEach { sub ->
                            val isSelected = newSubject == sub.name
                            Button(
                                onClick = { newSubject = sub.name },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = if (isSelected) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(vertical = 4.dp, horizontal = 2.dp)
                            ) {
                                Text(sub.name, fontSize = 9.sp)
                            }
                        }
                    }

                    OutlinedTextField(
                        value = newStartTime,
                        onValueChange = { newStartTime = it },
                        label = { Text("Start Time (e.g. 09:00)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newEndTime,
                        onValueChange = { newEndTime = it },
                        label = { Text("End Time (e.g. 10:30)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("Priority", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("low", "medium", "high").forEach { prio ->
                            val isSelected = newPriority == prio
                            Button(
                                onClick = { newPriority = prio },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = if (isSelected) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(prio.uppercase(), fontSize = 10.sp)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newTitle.isNotBlank()) {
                            repository.addSchedule(
                                title = newTitle,
                                startTime = newStartTime,
                                endTime = newEndTime,
                                day = selectedDay,
                                priority = newPriority,
                                subjectName = newSubject
                            )
                            showAddDialog = false
                        }
                    }
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun ScheduleItemRow(
    item: ScheduleItem,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (item.isCompleted) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Checkbox(
                    checked = item.isCompleted,
                    onCheckedChange = { onToggle() }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = item.title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        textDecoration = if (item.isCompleted) TextDecoration.LineThrough else null,
                        color = if (item.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${item.startTime} - ${item.endTime} • ${item.subjectName}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Badge(
                    containerColor = when (item.priority) {
                        "high" -> MaterialTheme.colorScheme.errorContainer
                        "medium" -> Color(0xFFFEF3C7)
                        else -> MaterialTheme.colorScheme.primaryContainer
                    },
                    contentColor = when (item.priority) {
                        "high" -> MaterialTheme.colorScheme.onErrorContainer
                        "medium" -> Color(0xFFB45309)
                        else -> MaterialTheme.colorScheme.onPrimaryContainer
                    }
                ) {
                    Text(
                        text = item.priority.uppercase(),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Schedule",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
