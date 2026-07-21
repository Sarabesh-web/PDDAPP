package com.example.smartplannerapp.ui.screens

import android.content.Context
import android.content.Intent
import android.graphics.pdf.PdfDocument
import android.os.Environment
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartplannerapp.data.DataRepository
import com.example.smartplannerapp.data.Task
import com.example.smartplannerapp.data.ScheduleItem
import com.example.smartplannerapp.data.isCompleted
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

fun calculateStreak(tasks: List<Task>): Int {
    if (tasks.isEmpty()) return 0
    val completedDates = tasks
        .filter { it.status == "completed" && it.completedAt.isNotBlank() }
        .map { it.completedAt.split("T")[0] }
        .toSet()
    if (completedDates.isEmpty()) return 0

    var currentStreak = 0
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val calendar = Calendar.getInstance()

    while (true) {
        val dateStr = sdf.format(calendar.time)
        if (completedDates.contains(dateStr)) {
            currentStreak++
            calendar.add(Calendar.DATE, -1)
        } else {
            if (currentStreak == 0) {
                calendar.add(Calendar.DATE, -1)
                val yesterdayStr = sdf.format(calendar.time)
                if (completedDates.contains(yesterdayStr)) {
                    currentStreak++
                    calendar.add(Calendar.DATE, -1)
                    continue
                }
            }
            break
        }
    }
    return currentStreak
}

fun calculateStudyHours(schedules: List<ScheduleItem>): Double {
    var totalHours = 0.0
    schedules.filter { it.isCompleted }.forEach { s ->
        val startParts = s.startTime.split(":")
        val endParts = s.endTime.split(":")
        if (startParts.size >= 2 && endParts.size >= 2) {
            val startH = startParts[0].toIntOrNull() ?: 0
            val startM = startParts[1].toIntOrNull() ?: 0
            val endH = endParts[0].toIntOrNull() ?: 0
            val endM = endParts[1].toIntOrNull() ?: 0
            val duration = (endH * 60 + endM) - (startH * 60 + startM)
            if (duration > 0) {
                totalHours += duration.toDouble() / 60.0
            }
        }
    }
    return totalHours
}

fun generateAndSharePdf(context: Context, userName: String, tasks: List<Task>, schedules: List<ScheduleItem>) {
    try {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas
        val paint = android.graphics.Paint()

        // Title
        paint.color = android.graphics.Color.parseColor("#0284C7")
        paint.textSize = 22f
        paint.isFakeBoldText = true
        canvas.drawText("AetherFlow — Academic Performance Report", 40f, 50f, paint)

        // Subheader
        paint.color = android.graphics.Color.DKGRAY
        paint.textSize = 14f
        paint.isFakeBoldText = false
        val todayDate = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault()).format(Date())
        canvas.drawText("Student: $userName | Date: $todayDate", 40f, 75f, paint)

        // Divider
        paint.color = android.graphics.Color.LTGRAY
        canvas.drawLine(40f, 90f, 555f, 90f, paint)

        // Metrics Summary
        paint.color = android.graphics.Color.BLACK
        paint.textSize = 14f
        paint.isFakeBoldText = true
        canvas.drawText("Summary Stats:", 40f, 120f, paint)

        paint.textSize = 12f
        paint.isFakeBoldText = false
        val totalStudyHours = calculateStudyHours(schedules)
        val completedTasks = tasks.count { it.status == "completed" }
        val streak = calculateStreak(tasks)

        canvas.drawText("• Total Study Hours: ${String.format(Locale.US, "%.1fh", totalStudyHours)}", 50f, 145f, paint)
        canvas.drawText("• Tasks Completed: $completedTasks / ${tasks.size}", 50f, 165f, paint)
        canvas.drawText("• Active Streak: $streak days", 50f, 185f, paint)

        // Tasks section
        paint.textSize = 14f
        paint.isFakeBoldText = true
        canvas.drawText("Task Checklist:", 40f, 225f, paint)

        paint.textSize = 11f
        paint.isFakeBoldText = false
        var yPos = 250f
        tasks.take(15).forEach { task ->
            val statusStr = if (task.status == "completed") "[✓]" else "[ ]"
            canvas.drawText("$statusStr ${task.title} (${task.subject})", 50f, yPos, paint)
            yPos += 20f
        }

        pdfDocument.finishPage(page)

        val file = File(context.cacheDir, "AetherFlow_Report.pdf")
        pdfDocument.writeTo(FileOutputStream(file))
        pdfDocument.close()

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Export Report PDF"))
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "Failed to generate PDF: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    repository: DataRepository,
    onNavigateToTasks: () -> Unit,
    onNavigateToStats: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToAdmin: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val userProfile by repository.userProfile.collectAsState()
    val userRole = userProfile?.user?.role ?: repository.getSessionManager().getUserRole()

    val userName = userProfile?.user?.name ?: repository.getSessionManager().getUserName().ifBlank { "Student" }

    val tasks by repository.tasks.collectAsState()
    val schedules by repository.schedules.collectAsState()

    val totalStudyHours = calculateStudyHours(schedules)
    val totalTasks = tasks.size
    val completedTasks = tasks.count { it.status == "completed" }
    val completionRate = if (totalTasks > 0) (completedTasks * 100 / totalTasks) else 0
    val currentStreak = calculateStreak(tasks)

    val todayDate = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(Date())
    var menuExpanded by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // Top Bar — App name in tiny muted uppercase top corner + Profile/More Menu button
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Top-Left Branding label
                Text(
                    text = "AETHERFLOW • SMART PLANNER",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.5.sp
                )

                // Top-Right Actions Menu
                Box {
                    IconButton(
                        onClick = { menuExpanded = true },
                        modifier = Modifier
                            .size(36.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Menu",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                        modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                    ) {
                        DropdownMenuItem(
                            text = { Text("Analytics & Stats") },
                            leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                onNavigateToStats()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Settings") },
                            leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                onNavigateToSettings()
                            }
                        )
                        if (userRole.equals("admin", ignoreCase = true)) {
                            DropdownMenuItem(
                                text = { Text("Admin Panel") },
                                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                                onClick = {
                                    menuExpanded = false
                                    onNavigateToAdmin()
                                }
                            )
                        }
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("Export PDF Report") },
                            leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                generateAndSharePdf(context, userName, tasks, schedules)
                            }
                        )
                    }
                }
            }
        }

        // Welcome Header — "Welcome back," small, "Sarabesh" big and prominent
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Welcome back,",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = userName,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = todayDate,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.padding(top = 2.dp)
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
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontSize = 14.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "\"Success is the sum of small efforts, repeated day in and day out.\"",
                        fontSize = 14.sp,
                        fontStyle = FontStyle.Italic,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "— Robert Collier",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Light,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }

        // Stat Cards Grid
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                StatCard(
                    title = "Study Hours",
                    value = String.format(Locale.US, "%.1fh", totalStudyHours),
                    subtitle = "Schedules done",
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "Active Streak",
                    value = if (currentStreak > 0) "$currentStreak d" else "0 d",
                    subtitle = if (currentStreak > 0) "Streak burning! 🔥" else "Study to start",
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                StatCard(
                    title = "Tasks Done",
                    value = "$completedTasks/$totalTasks",
                    subtitle = "$completionRate% completed",
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "Progress Goal",
                    value = "$completionRate%",
                    subtitle = "Rating score",
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Daily Study Checklist Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Daily Study Checklist",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                TextButton(onClick = onNavigateToTasks) {
                    Text("View All", fontSize = 12.sp)
                }
            }
        }

        val pendingTasks = tasks.filter { it.status != "completed" }
        if (pendingTasks.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No pending tasks! All caught up.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp,
                            fontStyle = FontStyle.Italic
                        )
                    }
                }
            }
        } else {
            items(pendingTasks.take(4)) { task ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp)
                        .clickable { repository.toggleTask(task.id) },
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
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Check",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
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
                                    text = "${task.subject} • Priority: ${task.priority.uppercase()}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // Upcoming Deadlines Header
        item {
            Text(
                text = "Upcoming Deadlines",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        // Upcoming Deadlines List
        val upcomingTasks = tasks.filter { it.status == "pending" }.sortedBy { it.dueDate }.take(3)
        if (upcomingTasks.isEmpty()) {
            item {
                Text(
                    text = "No upcoming deadlines!",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(vertical = 4.dp)
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
        modifier = modifier.height(105.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
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
                                "high" -> MaterialTheme.colorScheme.error
                                "medium" -> Color(0xFFF59E0B)
                                else -> MaterialTheme.colorScheme.tertiary
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
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Badge(
                containerColor = when (task.priority) {
                    "high" -> MaterialTheme.colorScheme.errorContainer
                    "medium" -> Color(0xFFFEF3C7)
                    else -> MaterialTheme.colorScheme.primaryContainer
                },
                contentColor = when (task.priority) {
                    "high" -> MaterialTheme.colorScheme.onErrorContainer
                    "medium" -> Color(0xFFB45309)
                    else -> MaterialTheme.colorScheme.onPrimaryContainer
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
