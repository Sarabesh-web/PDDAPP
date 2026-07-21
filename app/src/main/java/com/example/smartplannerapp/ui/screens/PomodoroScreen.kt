package com.example.smartplannerapp.ui.screens

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.ToneGenerator
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
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
import kotlinx.coroutines.launch
import kotlin.concurrent.thread
import kotlin.math.sin

class AmbientNoiseSynthesizer {
    private var audioTrack: AudioTrack? = null
    private var isPlaying = false
    private var type = "none" // rain, waves, lofi
    private var volume = 0.5f

    fun start(soundType: String) {
        stop()
        type = soundType
        isPlaying = true
        val sampleRate = 44100
        val minBufferSize = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )

        try {
            audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(minBufferSize * 2)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            audioTrack?.setVolume(volume)
            audioTrack?.play()

            thread(start = true) {
                val buffer = ShortArray(2048)
                var phase = 0.0
                var waveLfo = 0.0
                var lastOut = 0.0

                while (isPlaying) {
                    val currentTrack = audioTrack
                    if (currentTrack == null || !isPlaying) break
                    for (i in buffer.indices) {
                        val sample = when (type) {
                            "rain" -> {
                                val noise = (Math.random() * 2.0 - 1.0)
                                lastOut = 0.95 * lastOut + 0.05 * noise
                                lastOut
                            }
                            "waves" -> {
                                val noise = (Math.random() * 2.0 - 1.0)
                                waveLfo += 2.0 * Math.PI * 0.1 / sampleRate
                                val amp = 0.4 + 0.4 * sin(waveLfo)
                                lastOut = 0.9 * lastOut + 0.1 * noise
                                lastOut * amp
                            }
                            "lofi" -> {
                                phase += 2.0 * Math.PI / sampleRate
                                val tone1 = sin(phase * 130.81) // C3
                                val tone2 = sin(phase * 164.81) // E3
                                val tone3 = sin(phase * 196.00) // G3
                                val tone4 = sin(phase * 246.94) // B3
                                (tone1 + tone2 + tone3 + tone4) * 0.25
                            }
                            else -> 0.0
                        }
                        val shortVal = (sample.coerceIn(-1.0, 1.0) * 32767.0).toInt().toShort()
                        buffer[i] = shortVal
                    }
                    try {
                        currentTrack.write(buffer, 0, buffer.size)
                    } catch (e: Exception) {
                        break
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun stop() {
        isPlaying = false
        audioTrack?.apply {
            try {
                stop()
                release()
            } catch (e: Exception) {}
        }
        audioTrack = null
    }

    fun setVolume(vol: Float) {
        volume = vol.coerceIn(0f, 1f)
        try {
            audioTrack?.setVolume(volume)
        } catch (e: Exception) {}
    }
}

@Composable
fun PomodoroScreen(
    repository: DataRepository,
    modifier: Modifier = Modifier
) {
    val tasks by repository.tasks.collectAsState()
    val subjects by repository.subjects.collectAsState()
    val scope = rememberCoroutineScope()

    val settings by repository.settings.collectAsState()
    var focusMode by remember { mutableStateOf("study") } // study, shortBreak, longBreak

    val totalTimeSeconds = when (focusMode) {
        "shortBreak" -> settings.pomodoroBreak * 60
        "longBreak" -> 15 * 60
        else -> settings.pomodoroStudy * 60
    }

    var timeLeftSeconds by remember { mutableStateOf(totalTimeSeconds) }
    var isRunning by remember { mutableStateOf(false) }

    // Subject Selector state
    var selectedSubject by remember { mutableStateOf("General") }
    var subjectDropdownExpanded by remember { mutableStateOf(false) }

    // Ambient Sound Synthesizer instance
    val synthesizer = remember { AmbientNoiseSynthesizer() }
    var selectedSound by remember { mutableStateOf("none") } // none, rain, waves, lofi
    var ambientVolume by remember { mutableStateOf(0.5f) }

    DisposableEffect(Unit) {
        onDispose {
            synthesizer.stop()
        }
    }

    // Sync timer when tab or setting switches
    LaunchedEffect(focusMode, settings) {
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
            // Play alert tone
            try {
                val toneGenerator = ToneGenerator(AudioManager.STREAM_ALARM, 100)
                toneGenerator.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 500)
            } catch (e: Exception) {}

            // Log Focus Time if completed study mode
            if (focusMode == "study") {
                val durationMinutes = totalTimeSeconds / 60
                scope.launch {
                    repository.logStudySession(selectedSubject, durationMinutes, "Pomodoro study session")
                }
            }
        }
    }

    val minutes = timeLeftSeconds / 60
    val seconds = timeLeftSeconds % 60
    val formattedTime = String.format("%02d:%02d", minutes, seconds)
    val progressFraction = timeLeftSeconds.toFloat() / totalTimeSeconds.toFloat()

    val activeTask = tasks.firstOrNull { it.status == "pending" && it.subject.equals(selectedSubject, ignoreCase = true) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Pomodoro Timer",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Boost your study sessions with ambient sounds.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
                Text(
                    text = "AETHERFLOW",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.sp
                )
            }
        }

        // Subject Selector
        item {
            Box {
                OutlinedButton(
                    onClick = { subjectDropdownExpanded = true },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Focus Subject: $selectedSubject", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Icon(Icons.Default.ArrowDropDown, contentDescription = "Dropdown")
                    }
                }
                DropdownMenu(
                    expanded = subjectDropdownExpanded,
                    onDismissRequest = { subjectDropdownExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("General") },
                        onClick = {
                            selectedSubject = "General"
                            subjectDropdownExpanded = false
                        }
                    )
                    subjects.forEach { subject ->
                        DropdownMenuItem(
                            text = { Text(subject.name) },
                            onClick = {
                                selectedSubject = subject.name
                                subjectDropdownExpanded = false
                            }
                        )
                    }
                }
            }
        }

        // Mode Toggles
        item {
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
                            contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(vertical = 10.dp)
                    ) {
                        Text(text = label, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Circular Timer Canvas
        item {
            Box(
                modifier = Modifier.size(220.dp).padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                val progressColor = when (focusMode) {
                    "shortBreak" -> MaterialTheme.colorScheme.tertiary
                    "longBreak" -> MaterialTheme.colorScheme.secondary
                    else -> MaterialTheme.colorScheme.primary
                }

                val trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)

                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(
                        color = trackColor,
                        radius = size.minDimension / 2 - 8.dp.toPx(),
                        style = Stroke(width = 8.dp.toPx())
                    )
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
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Bold,
                        color = progressColor
                    )
                    Text(
                        text = when (focusMode) {
                            "shortBreak" -> "SHORT BREAK"
                            "longBreak" -> "LONG BREAK"
                            else -> "STUDY FOCUS"
                        },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }

        // Action Controls
        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = { isRunning = !isRunning },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = when (focusMode) {
                            "shortBreak" -> MaterialTheme.colorScheme.tertiary
                            "longBreak" -> MaterialTheme.colorScheme.secondary
                            else -> MaterialTheme.colorScheme.primary
                        }
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.width(120.dp).height(45.dp)
                ) {
                    Text(text = if (isRunning) "Pause" else "Start", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }

                OutlinedButton(
                    onClick = {
                        isRunning = false
                        timeLeftSeconds = totalTimeSeconds
                    },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.width(120.dp).height(45.dp)
                ) {
                    Text(text = "Reset", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Ambient Sound Controls
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(text = "Sound Space Generator", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("none", "rain", "waves", "lofi").forEach { sound ->
                            val isSelected = selectedSound == sound
                            val label = sound.uppercase()
                            Button(
                                onClick = {
                                    selectedSound = sound
                                    if (sound == "none") {
                                        synthesizer.stop()
                                    } else {
                                        synthesizer.start(sound)
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(vertical = 8.dp)
                            ) {
                                Text(text = label, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    if (selectedSound != "none") {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                            Text(text = "Volume", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(50.dp))
                            Slider(
                                value = ambientVolume,
                                onValueChange = {
                                    ambientVolume = it
                                    synthesizer.setVolume(it)
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }

        // Active Study Focus Target
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Focused Target",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    if (activeTask != null) {
                        Text(
                            text = activeTask.title,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${activeTask.subject} • Priority: ${activeTask.priority.uppercase()}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    } else {
                        Text(
                            text = "No pending tasks for $selectedSubject",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Log study session when the Pomodoro completes.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }
        }
    }
}
