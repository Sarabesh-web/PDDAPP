package com.example.smartplannerapp.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class Subject(
    val id: String = "",
    val name: String,
    @SerialName("color") val colorHex: String,
    val icon: String = "📐"
)

@Serializable
data class Task(
    val id: String = "",
    val title: String,
    val description: String = "",
    val subject: String = "General",
    val priority: String = "medium", // low, medium, high
    val status: String = "pending",   // pending, completed
    val dueDate: String = "",
    val completedAt: String = ""
)

@Serializable
data class Note(
    val id: String = "",
    val title: String,
    val content: String,
    val subject: String = "General",
    val date: String = ""
)

@Serializable
data class ScheduleItem(
    val id: String = "",
    val title: String,
    val startTime: String,
    val endTime: String,
    val day: String,
    val priority: String = "medium", // low, medium, high
    val completed: String = "false", // Mongoose schema has completed as String ('true'/'false')
    val subjectId: String = "",
    val subjectName: String = ""
)

@Serializable
data class AppSettings(
    val theme: String = "dark",
    val notifications: Boolean = true,
    val pomodoroStudy: Int = 25,
    val pomodoroBreak: Int = 5,
    val dailyGoalReminder: Boolean = true,
    val fontSize: String = "medium"
)

val ScheduleItem.isCompleted: Boolean
    get() = completed == "true"

