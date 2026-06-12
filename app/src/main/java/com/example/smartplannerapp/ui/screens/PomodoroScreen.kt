package com.example.smartplannerapp.ui.screens

import android.media.AudioManager
import android.media.ToneGenerator
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartplannerapp.data.DataRepository
import kotlinx.coroutines.delay

@Composable
fun PomodoroScreen(
    repository: DataRepository,
    modifier: Modifier = Modifier
) {
    val tasks by repository.tasks.collectAsState()
    
    var focusMode by remember { mutableStateOf("study") } // study, shortBreak, longBreak
    val totalTimeSeconds = when (focusMode) {
        "shortBreak" -> 5 * 60
        "longBreak" -> 15 * 60
        else -> 25 * 60
    }
    
    var timeLeftSeconds by remember { mutableStateOf(totalTimeSeconds) }
    var isRunning by remember { mutableStateOf(false) }
    
    // Sync timer when tab switches
    LaunchedEffect(focusMode) {
        isRunning = false
        timeLeftSeconds = totalTimeSeconds
    }
    
    // Timer Tick Effect
    LaunchedEffect(isRunning, timeLeftSeconds) {
        if (isRunning && timeLeftSeconds > 0) {
            delay(1000L)
            timeLeftSeconds -= 1
        } else if (timeLeftSeconds == 0) {
            isRunning = false
            // Play alert sound
            try {
                val toneGenerator = ToneGenerator(AudioManager.STREAM_ALARM, 100)
                toneGenerator.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 500)
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    val minutes = timeLeftSeconds / 60
    val seconds = timeLeftSeconds % 60
    val formattedTime = String.format("%02d:%02d", minutes, seconds)
    val progressFraction = timeLeftSeconds.toFloat() / totalTimeSeconds.toFloat()

    val activeTask = tasks.firstOrNull { it.status == "pending" && it.priority == "high" }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Header
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Pomodoro Focus Timer",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Boost your study sessions with structural breaks.",
                fontSize = 13.sp,
                color = Color.Gray,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        // Mode Toggles
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(24.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            listOf("study", "shortBreak", "longBreak").forEach { mode ->
                val isSelected = focusMode == mode
                val label = when (mode) {
                    "shortBreak" -> "Short Break"
                    "longBreak" -> "Long Break"
                    else -> "Focus Session"
                }
                Button(
                    onClick = { focusMode = mode },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                        contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    Text(text = label, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Circular Timer Canvas
        Box(
            modifier = Modifier.size(240.dp),
            contentAlignment = Alignment.Center
        ) {
            val progressColor = when (focusMode) {
                "shortBreak" -> Color(0xFF10B981)
                "longBreak" -> Color(0xFF8B5CF6)
                else -> Color(0xFF3B82F6)
            }
            
            Canvas(modifier = Modifier.fillMaxSize()) {
                // Background Track
                drawCircle(
                    color = Color.Gray.copy(alpha = 0.2f),
                    radius = size.minDimension / 2 - 8.dp.toPx(),
                    style = Stroke(width = 8.dp.toPx())
                )
                // Active Sweep Progress
                drawArc(
                    color = progressColor,
                    startAngle = -90f,
                    sweepAngle = 360f * progressFraction,
                    useCenter = false,
                    style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                )
            }
            
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = formattedTime,
                    fontSize = 54.sp,
                    fontWeight = FontWeight.Bold,
                    color = progressColor
                )
                Text(
                    text = when (focusMode) {
                        "shortBreak" -> "SHORT BREAK"
                        "longBreak" -> "LONG BREAK"
                        else -> "STUDY FOCUS"
                    },
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        // Action Controls
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = { isRunning = !isRunning },
                colors = ButtonDefaults.buttonColors(
                    containerColor = when (focusMode) {
                        "shortBreak" -> Color(0xFF10B981)
                        "longBreak" -> Color(0xFF8B5CF6)
                        else -> Color(0xFF3B82F6)
                    }
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .width(130.dp)
                    .height(50.dp)
            ) {
                Text(text = if (isRunning) "Pause" else "Start", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
            
            OutlinedButton(
                onClick = {
                    isRunning = false
                    timeLeftSeconds = totalTimeSeconds
                },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .width(130.dp)
                    .height(50.dp)
            ) {
                Text(text = "Reset", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Active Study Focus Area
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Focused Target",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                if (activeTask != null) {
                    Text(
                        text = activeTask.title,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    )
                    Text(
                        text = "${activeTask.subject} • Priority: ${activeTask.priority.uppercase()}",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                } else {
                    Text(
                        text = "Complete Physics Lab Report", // fallback default
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    )
                    Text(
                        text = "Physics • Priority: HIGH",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }
    }
}
