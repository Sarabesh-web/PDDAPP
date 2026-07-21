package com.example.smartplannerapp.data

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DataRepository(context: Context) {
    private val sessionManager = SessionManager(context)
    private var apiService = NetworkModule.getApiService(sessionManager)
    private val scope = CoroutineScope(Dispatchers.IO)

    private val _subjects = MutableStateFlow<List<Subject>>(emptyList())
    val subjects: StateFlow<List<Subject>> = _subjects.asStateFlow()

    private val _tasks = MutableStateFlow<List<Task>>(emptyList())
    val tasks: StateFlow<List<Task>> = _tasks.asStateFlow()

    private val _notes = MutableStateFlow<List<Note>>(emptyList())
    val notes: StateFlow<List<Note>> = _notes.asStateFlow()

    private val _schedules = MutableStateFlow<List<ScheduleItem>>(emptyList())
    val schedules: StateFlow<List<ScheduleItem>> = _schedules.asStateFlow()

    private val _settings = MutableStateFlow(AppSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    private val _userProfile = MutableStateFlow<MeResponse?>(null)
    val userProfile: StateFlow<MeResponse?> = _userProfile.asStateFlow()

    init {
        rebuildService()
        if (isLoggedIn()) {
            refreshData()
        }
    }

    fun isLoggedIn(): Boolean = sessionManager.getToken() != null

    fun rebuildService() {
        apiService = NetworkModule.getApiService(sessionManager)
    }

    fun getSessionManager(): SessionManager = sessionManager

    fun logout() {
        sessionManager.clearSession()
        _userProfile.value = null
        _subjects.value = emptyList()
        _tasks.value = emptyList()
        _notes.value = emptyList()
        _schedules.value = emptyList()
    }

    suspend fun login(email: String, password: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.login(LoginRequest(email, password))
                if (response.isSuccessful) {
                    val auth = response.body()
                    if (auth != null) {
                        sessionManager.saveSession(auth.token, auth.user.name, auth.user.email, auth.user.role)
                        rebuildService()
                        refreshData()
                        null // Success
                    } else "Empty response payload"
                } else {
                    response.errorBody()?.string() ?: "Login failed"
                }
            } catch (e: Exception) {
                e.message ?: "Network error"
            }
        }
    }

    suspend fun signup(email: String, password: String, name: String, role: String = "student"): String? {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.signup(SignupRequest(email, password, name, role))
                if (response.isSuccessful) {
                    val auth = response.body()
                    if (auth != null) {
                        sessionManager.saveSession(auth.token, auth.user.name, auth.user.email, auth.user.role)
                        rebuildService()
                        refreshData()
                        null // Success
                    } else "Empty response payload"
                } else {
                    response.errorBody()?.string() ?: "Signup failed"
                }
            } catch (e: Exception) {
                e.message ?: "Network error"
            }
        }
    }

    fun refreshData() {
        if (!isLoggedIn()) return
        scope.launch {
            try {
                val meRes = apiService.getMe()
                if (meRes.isSuccessful) {
                    _userProfile.value = meRes.body()
                    meRes.body()?.profile?.let { profile ->
                        _settings.value = _settings.value.copy(
                            theme = profile.theme,
                            notifications = profile.notifications == "true"
                        )
                    }
                }

                val subRes = apiService.getSubjects()
                val activeSubjects = if (subRes.isSuccessful) subRes.body() ?: emptyList() else emptyList()
                _subjects.value = activeSubjects

                val taskRes = apiService.getTasks()
                if (taskRes.isSuccessful) {
                    _tasks.value = taskRes.body() ?: emptyList()
                }

                val notesRes = apiService.getNotes()
                if (notesRes.isSuccessful) {
                    _notes.value = notesRes.body() ?: emptyList()
                }

                val schedRes = apiService.getSchedules()
                if (schedRes.isSuccessful) {
                    val rawSchedules = schedRes.body() ?: emptyList()
                    // Map subjectId to subjectName dynamically to align with UI expectations
                    val mappedSchedules = rawSchedules.map { schedule ->
                        val subName = activeSubjects.find { it.id == schedule.subjectId }?.name ?: "General"
                        schedule.copy(subjectName = subName)
                    }
                    _schedules.value = mappedSchedules
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // --- CRUD Actions ---

    fun addTask(title: String, description: String, subject: String, priority: String, dueDate: String) {
        scope.launch {
            try {
                val response = apiService.createTask(
                    Task(
                        title = title,
                        description = description,
                        subject = subject,
                        priority = priority,
                        dueDate = dueDate,
                        status = "pending"
                    )
                )
                if (response.isSuccessful) {
                    refreshData()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun toggleTask(id: String) {
        scope.launch {
            try {
                val task = _tasks.value.find { it.id == id } ?: return@launch
                val newStatus = if (task.status == "completed") "pending" else "completed"
                val response = apiService.updateTask(id, task.copy(status = newStatus))
                if (response.isSuccessful) {
                    refreshData()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun deleteTask(id: String) {
        scope.launch {
            try {
                val response = apiService.deleteTask(id)
                if (response.isSuccessful) {
                    refreshData()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun addNote(title: String, content: String, subject: String) {
        scope.launch {
            try {
                val response = apiService.createNote(
                    Note(
                        title = title,
                        content = content,
                        subject = subject,
                        date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                    )
                )
                if (response.isSuccessful) {
                    refreshData()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun deleteNote(id: String) {
        scope.launch {
            try {
                val response = apiService.deleteNote(id)
                if (response.isSuccessful) {
                    refreshData()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun addSchedule(title: String, startTime: String, endTime: String, day: String, priority: String, subjectName: String) {
        scope.launch {
            try {
                val subjectId = _subjects.value.find { it.name == subjectName }?.id ?: ""
                val response = apiService.createSchedule(
                    ScheduleItem(
                        title = title,
                        startTime = startTime,
                        endTime = endTime,
                        day = day,
                        priority = priority,
                        completed = "false",
                        subjectId = subjectId
                    )
                )
                if (response.isSuccessful) {
                    refreshData()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun toggleSchedule(id: String) {
        scope.launch {
            try {
                val schedule = _schedules.value.find { it.id == id } ?: return@launch
                val newCompleted = if (schedule.completed == "true") "false" else "true"
                val response = apiService.updateSchedule(id, schedule.copy(completed = newCompleted))
                if (response.isSuccessful) {
                    refreshData()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun deleteSchedule(id: String) {
        scope.launch {
            try {
                val response = apiService.deleteSchedule(id)
                if (response.isSuccessful) {
                    refreshData()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun updateSettings(newSettings: AppSettings) {
        _settings.value = newSettings
        scope.launch {
            try {
                apiService.updateProfile(
                    ProfileUpdateRequest(
                        theme = newSettings.theme,
                        notifications = if (newSettings.notifications) "true" else "false"
                    )
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun updateProfile(name: String?, bio: String?, university: String?, major: String?, year: String?, theme: String? = null, notifications: String? = null): String? {
        return withContext(Dispatchers.IO) {
            try {
                val activeTheme = theme ?: _settings.value.theme
                val response = apiService.updateProfile(ProfileUpdateRequest(name, bio, university, major, year, activeTheme, notifications))
                if (response.isSuccessful) {
                    refreshData()
                    null
                } else {
                    response.errorBody()?.string() ?: "Update failed"
                }
            } catch (e: Exception) {
                e.message ?: "Network error"
            }
        }
    }

    suspend fun logStudySession(subjectName: String, durationMinutes: Int, title: String = ""): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.logStudySession(LogStudySessionRequest(subjectName, durationMinutes, title))
                if (response.isSuccessful) {
                    refreshData()
                    true
                } else false
            } catch (e: Exception) {
                false
            }
        }
    }

    suspend fun getAdminProgress(): List<StudentProgress>? {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getAdminProgress()
                if (response.isSuccessful) response.body() else null
            } catch (e: Exception) {
                null
            }
        }
    }
}
