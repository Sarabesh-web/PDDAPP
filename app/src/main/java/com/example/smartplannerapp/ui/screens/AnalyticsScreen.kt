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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartplannerapp.data.DataRepository
import com.example.smartplannerapp.data.isCompleted
import java.util.Locale

@Composable
fun AnalyticsScreen(
    repository: DataRepository,
    modifier: Modifier = Modifier
) {
    val tasks by repository.tasks.collectAsState()
    val schedules by repository.schedules.collectAsState()

    // Calculate study hours dynamically
    var totalStudyHours = 0.0
    val subjectHoursMap = mutableMapOf<String, Double>()

    schedules.filter { it.isCompleted }.forEach { item ->
        val subjectName = item.subjectName.ifBlank { "General" }
        val startParts = item.startTime.split(":")
        val endParts = item.endTime.split(":")
        if (startParts.size >= 2 && endParts.size >= 2) {
            val startH = startParts[0].toIntOrNull() ?: 0
            val startM = startParts[1].toIntOrNull() ?: 0
            val endH = endParts[0].toIntOrNull() ?: 0
            val endM = endParts[1].toIntOrNull() ?: 0
            val duration = (endH * 60 + endM) - (startH * 60 + startM)
            if (duration > 0) {
                val hrs = duration.toDouble() / 60.0
                totalStudyHours += hrs
                subjectHoursMap[subjectName] = (subjectHoursMap[subjectName] ?: 0.0) + hrs
            }
        }
    }

    val completedTasks = tasks.count { it.status == "completed" }
    val totalTasks = tasks.size
    val completionRate = if (totalTasks > 0) (completedTasks * 100 / totalTasks) else 0

    // Dynamic Productivity Grade
    val productivityGrade = when {
        completionRate >= 90 -> "A+"
        completionRate >= 80 -> "A"
        completionRate >= 75 -> "A-"
        completionRate >= 65 -> "B+"
        completionRate >= 50 -> "B"
        completionRate >= 35 -> "C"
        else -> "D"
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Productivity Analytics",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Track and analyze your study habits over time.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
            )
        }

        // Summary Statistics Cards
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Performance Metrics",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    MetricRow(label = "Total Study Hours", value = String.format(Locale.US, "%.1fh", totalStudyHours), color = MaterialTheme.colorScheme.primary)
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    MetricRow(label = "Task Completion Rate", value = "$completionRate%", color = MaterialTheme.colorScheme.tertiary)
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    MetricRow(label = "Productivity Grade", value = productivityGrade, color = MaterialTheme.colorScheme.secondary)
                }
            }
        }

        // Subject Distribution Progress Bars
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Subject-wise Distribution",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    if (subjectHoursMap.isEmpty()) {
                        Text(
                            text = "No study logs recorded yet. Complete focus slots or planner items to log study hours.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontStyle = FontStyle.Italic,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    } else {
                        val maxHours = subjectHoursMap.values.maxOrNull() ?: 1.0
                        val colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.secondary,
                            MaterialTheme.colorScheme.tertiary,
                            MaterialTheme.colorScheme.error
                        )

                        subjectHoursMap.entries.sortedByDescending { it.value }.forEachIndexed { index, entry ->
                            val color = colors[index % colors.size]
                            val fraction = (entry.value / maxHours).toFloat()
                            SubjectProgressItem(
                                name = entry.key,
                                fraction = fraction,
                                color = color,
                                hours = String.format(Locale.US, "%.1fh", entry.value)
                            )
                        }
                    }
                }
            }
        }

        // Productivity Tips
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "AI Study Insight",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    val insightMessage = if (totalStudyHours > 0) {
                        val topSubject = subjectHoursMap.entries.maxByOrNull { it.value }?.key ?: "your subjects"
                        "Excellent job! You are focusing heavily on $topSubject. Remember to balance your schedule and take regular breaks to optimize long-term cognitive absorption."
                    } else {
                        "No data yet. Set a 25-minute Pomodoro timer or schedule study slots to train the AetherFlow planning engine."
                    }

                    Text(
                        text = insightMessage,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun MetricRow(label: String, value: String, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = color)
    }
}

@Composable
fun SubjectProgressItem(name: String, fraction: Float, color: Color, hours: String) {
    Column(modifier = Modifier.padding(vertical = 6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = name, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
            Text(text = hours, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { fraction },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp),
            color = color,
            trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
            strokeCap = StrokeCap.Round
        )
    }
}
