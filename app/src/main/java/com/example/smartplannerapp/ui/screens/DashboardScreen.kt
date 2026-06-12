package com.example.smartplannerapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartplannerapp.data.DataRepository
import com.example.smartplannerapp.data.Task
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    repository: DataRepository,
    onNavigateToTasks: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tasks by repository.tasks.collectAsState()
    val schedules by repository.schedules.collectAsState()
    
    val totalStudyHours = schedules.filter { it.isCompleted }.sumOf { 1.5 } // mock hours per class
    val totalTasks = tasks.size
    val completedTasks = tasks.count { it.status == "completed" }
    val completionRate = if (totalTasks > 0) (completedTasks * 100 / totalTasks) else 0

    val todayDate = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(Date())

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Welcome Header
        item {
            Column {
                Text(
                    text = "Welcome back, Student",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = todayDate,
                    fontSize = 14.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        // Daily Motivation Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Star",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Daily Motivation",
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "\"Success is the sum of small efforts, repeated day in and day out.\"",
                        fontSize = 16.sp,
                        fontStyle = FontStyle.Italic,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "— Robert Collier",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Light,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }

        // Stat Cards Grid (2x2 structure)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                StatCard(
                    title = "Study Hours",
                    value = "${totalStudyHours}h",
                    subtitle = "This week",
                    color = Color(0xFF3B82F6),
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "Streak",
                    value = "12 days",
                    subtitle = "Keep it up!",
                    color = Color(0xFFF59E0B),
                    modifier = Modifier.weight(1f)
                )
            }
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                StatCard(
                    title = "Tasks Done",
                    value = "$completedTasks/$totalTasks",
                    subtitle = "$completionRate% completed",
                    color = Color(0xFF10B981),
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "Focus Time",
                    value = "4.2h",
                    subtitle = "Today's goal: 6h",
                    color = Color(0xFF8B5CF6),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Upcoming Deadlines Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Upcoming Deadlines",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                TextButton(onClick = onNavigateToTasks) {
                    Text("View All")
                }
            }
        }

        // Upcoming Deadlines List
        val upcomingTasks = tasks.filter { it.status == "pending" }.take(3)
        if (upcomingTasks.isEmpty()) {
            item {
                Text(
                    text = "No upcoming deadlines!",
                    color = Color.Gray,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
        } else {
            items(upcomingTasks) { task ->
                DeadlineItem(task = task)
            }
        }
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    subtitle: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(110.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
            Text(
                text = value,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
fun DeadlineItem(task: Task) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(
                            color = when (task.priority) {
                                "high" -> Color.Red
                                "medium" -> Color.Yellow
                                else -> Color.Green
                            },
                            shape = RoundedCornerShape(4.dp)
                        )
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = task.title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${task.subject} • Due ${task.dueDate}",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }
            }
            Badge(
                containerColor = when (task.priority) {
                    "high" -> Color(0xFFFEE2E2)
                    "medium" -> Color(0xFFFEF3C7)
                    else -> Color(0xFFD1FAE5)
                },
                contentColor = when (task.priority) {
                    "high" -> Color(0xFFB91C1C)
                    "medium" -> Color(0xFFB45309)
                    else -> Color(0xFF047857)
                }
            ) {
                Text(
                    text = task.priority.uppercase(),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}
