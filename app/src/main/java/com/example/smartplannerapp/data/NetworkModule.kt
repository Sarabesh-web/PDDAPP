package com.example.smartplannerapp.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import kotlinx.serialization.Serializable
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Response
import retrofit2.http.*

class SessionManager(context: Context) {
    private val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
    private val prefs: SharedPreferences = EncryptedSharedPreferences.create(
        "smart_planner_secure_session",
        masterKeyAlias,
        context,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    companion object {
        private const val KEY_TOKEN = "auth_token"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_USER_ROLE = "user_role"
        private const val KEY_BASE_URL = "api_base_url"
        const val DEFAULT_BASE_URL = "http://192.168.1.33:3001" // PC local IP address on Wi-Fi
    }

    fun saveSession(token: String, name: String, email: String, role: String = "student") {
        prefs.edit().apply {
            putString(KEY_TOKEN, token)
            putString(KEY_USER_NAME, name)
            putString(KEY_USER_EMAIL, email)
            putString(KEY_USER_ROLE, role)
            apply()
        }
    }

    fun clearSession() {
        prefs.edit().apply {
            remove(KEY_TOKEN)
            remove(KEY_USER_NAME)
            remove(KEY_USER_EMAIL)
            remove(KEY_USER_ROLE)
            apply()
        }
    }

    fun getToken(): String? = prefs.getString(KEY_TOKEN, null)
    fun getUserName(): String = prefs.getString(KEY_USER_NAME, "") ?: ""
    fun getUserEmail(): String = prefs.getString(KEY_USER_EMAIL, "") ?: ""
    fun getUserRole(): String = prefs.getString(KEY_USER_ROLE, "student") ?: "student"

    fun getBaseUrl(): String = prefs.getString(KEY_BASE_URL, DEFAULT_BASE_URL) ?: DEFAULT_BASE_URL
    fun saveBaseUrl(url: String) {
        prefs.edit().putString(KEY_BASE_URL, url).apply()
    }
}

@Serializable
data class LoginRequest(
    val email: String,
    val password: String
)

@Serializable
data class SignupRequest(
    val email: String,
    val password: String,
    val name: String,
    val role: String = "student"
)

@Serializable
data class UserResponse(
    val id: String = "",
    val email: String,
    val name: String,
    val role: String = "student",
    val avatar: String = ""
)

@Serializable
data class AuthResponse(
    val token: String,
    val user: UserResponse
)

@Serializable
data class ProfileResponse(
    val bio: String = "",
    val university: String = "",
    val major: String = "",
    val year: String = "",
    val theme: String = "dark",
    val notifications: String = "true"
)

@Serializable
data class MeResponse(
    val user: UserResponse,
    val profile: ProfileResponse
)

@Serializable
data class ProfileUpdateRequest(
    val name: String? = null,
    val bio: String? = null,
    val university: String? = null,
    val major: String? = null,
    val year: String? = null,
    val theme: String? = null,
    val notifications: String? = null
)

@Serializable
data class ProfileUpdateResponse(
    val profile: ProfileResponse
)

@Serializable
data class StudentProgress(
    val id: String = "",
    val name: String,
    val email: String,
    val major: String = "",
    val university: String = "",
    val bio: String = "",
    val completedTasks: Int = 0,
    val totalTasks: Int = 0,
    val studyHours: Double = 0.0,
    val attendanceRate: Int = 100,
    val streak: Int = 0
)

@Serializable
data class LogStudySessionRequest(
    val subjectName: String,
    val durationMinutes: Int,
    val title: String = ""
)

interface ApiService {
    @POST("/api/auth/login")
    suspend fun login(@Body body: LoginRequest): Response<AuthResponse>

    @POST("/api/auth/signup")
    suspend fun signup(@Body body: SignupRequest): Response<AuthResponse>

    @GET("/api/auth/me")
    suspend fun getMe(): Response<MeResponse>

    @PUT("/api/auth/profile")
    suspend fun updateProfile(@Body body: ProfileUpdateRequest): Response<ProfileUpdateResponse>

    @GET("/api/data/admin/progress")
    suspend fun getAdminProgress(): Response<List<StudentProgress>>

    @POST("/api/data/schedules/log")
    suspend fun logStudySession(@Body body: LogStudySessionRequest): Response<ScheduleItem>

    @GET("/api/data/subjects")
    suspend fun getSubjects(): Response<List<Subject>>

    @POST("/api/data/subjects")
    suspend fun createSubject(@Body body: Subject): Response<Subject>

    @DELETE("/api/data/subjects/{id}")
    suspend fun deleteSubject(@Path("id") id: String): Response<Unit>

    @GET("/api/data/schedules")
    suspend fun getSchedules(): Response<List<ScheduleItem>>

    @POST("/api/data/schedules")
    suspend fun createSchedule(@Body body: ScheduleItem): Response<ScheduleItem>

    @PUT("/api/data/schedules/{id}")
    suspend fun updateSchedule(@Path("id") id: String, @Body body: ScheduleItem): Response<ScheduleItem>

    @DELETE("/api/data/schedules/{id}")
    suspend fun deleteSchedule(@Path("id") id: String): Response<Unit>

    @GET("/api/data/tasks")
    suspend fun getTasks(): Response<List<Task>>

    @POST("/api/data/tasks")
    suspend fun createTask(@Body body: Task): Response<Task>

    @PUT("/api/data/tasks/{id}")
    suspend fun updateTask(@Path("id") id: String, @Body body: Task): Response<Task>

    @DELETE("/api/data/tasks/{id}")
    suspend fun deleteTask(@Path("id") id: String): Response<Unit>

    @GET("/api/data/notes")
    suspend fun getNotes(): Response<List<Note>>

    @POST("/api/data/notes")
    suspend fun createNote(@Body body: Note): Response<Note>

    @PUT("/api/data/notes/{id}")
    suspend fun updateNote(@Path("id") id: String, @Body body: Note): Response<Note>

    @DELETE("/api/data/notes/{id}")
    suspend fun deleteNote(@Path("id") id: String): Response<Unit>
}

object NetworkModule {
    private val json = Json { ignoreUnknownKeys = true }

    fun getApiService(sessionManager: SessionManager): ApiService {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                sessionManager.getToken()?.let { token ->
                    request.addHeader("Authorization", "Bearer $token")
                }
                chain.proceed(request.build())
            }
            .build()

        val contentType = "application/json".toMediaType()
        return Retrofit.Builder()
            .baseUrl(sessionManager.getBaseUrl())
            .client(client)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
            .create(ApiService::class.java)
    }
}
