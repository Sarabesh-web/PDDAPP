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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartplannerapp.data.DataRepository

@Composable
fun AnalyticsScreen(
    repository: DataRepository,
    modifier: Modifier = Modifier
) {
    val tasks by repository.tasks.collectAsState()
    val schedules by repository.schedules.collectAsState()

    val totalStudyHours = schedules.filter { it.isCompleted }.sumOf { 1.5 }
    val completedTasks = tasks.count { it.status == "completed" }
    val totalTasks = tasks.size
    val completionRate = if (totalTasks > 0) (completedTasks * 100 / totalTasks) else 0

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
                color = Color.Gray,
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
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
                    
                    MetricRow(label = "Total Study Hours", value = "${totalStudyHours}h", color = Color(0xFF3B82F6))
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    MetricRow(label = "Task Completion Rate", value = "$completionRate%", color = Color(0xFF10B981))
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    MetricRow(label = "Productivity Grade", value = "A-", color = Color(0xFF8B5CF6))
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

                    SubjectProgressItem(name = "Mathematics", fraction = 0.45f, color = Color(0xFF6366F1), hours = "5.5h")
                    SubjectProgressItem(name = "Physics", fraction = 0.25f, color = Color(0xFF3B82F6), hours = "3.0h")
                    SubjectProgressItem(name = "Computer Science", fraction = 0.20f, color = Color(0xFF8B5CF6), hours = "2.5h")
                    SubjectProgressItem(name = "English Literature", fraction = 0.10f, color = Color(0xFFEC4899), hours = "1.0h")
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
                    Text(
                        text = "You are most active in the mornings. Harder subjects like Calculus should be studied before 11 AM to optimize absorption.",
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
        Text(text = label, fontSize = 14.sp, color = Color.Gray)
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
            Text(text = name, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Text(text = hours, fontSize = 11.sp, color = Color.Gray)
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { fraction },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp),
            color = color,
            trackColor = Color.Gray.copy(alpha = 0.2f),
            strokeCap = StrokeCap.Round
        )
    }
}

