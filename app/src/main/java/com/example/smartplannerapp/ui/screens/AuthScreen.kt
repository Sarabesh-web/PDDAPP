package com.example.smartplannerapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartplannerapp.data.DataRepository
import kotlinx.coroutines.launch

@Composable
fun AuthScreen(
    repository: DataRepository,
    onAuthSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sessionManager = repository.getSessionManager()
    val scope = rememberCoroutineScope()

    var isLoginMode by remember { mutableStateOf(true) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var apiUrl by remember { mutableStateOf(sessionManager.getBaseUrl()) }

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0F172A), // Slate 900
                        Color(0xFF1E1B4B)  // Indigo 950
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF1E293B).copy(alpha = 0.85f) // Slate 800 with transparency
            )
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Text(
                    text = if (isLoginMode) "Welcome Back" else "Create Account",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = if (isLoginMode) "Log in to sync your productivity database" else "Sign up to track your academic schedules",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // Error Message block
                errorMessage?.let { msg ->
                    Text(
                        text = msg,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }

                // Name field (Signup only)
                if (!isLoginMode) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Name", color = Color.LightGray) },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = "Name", tint = Color.Gray) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = Color.Gray
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Email Field
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email Address", color = Color.LightGray) },
                    leadingIcon = { Icon(Icons.Default.MailOutline, contentDescription = "Email", tint = Color.Gray) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = Color.Gray
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Password Field
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password", color = Color.LightGray) },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = "Password", tint = Color.Gray) },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = Color.Gray
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Backend Endpoint URL Field
                OutlinedTextField(
                    value = apiUrl,
                    onValueChange = { apiUrl = it },
                    label = { Text("Backend Server URL", color = Color.LightGray) },
                    leadingIcon = { Icon(Icons.Default.Settings, contentDescription = "Server URL", tint = Color.Gray) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = Color.Gray
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Submit Button
                Button(
                    onClick = {
                        if (email.isBlank() || password.isBlank() || (!isLoginMode && name.isBlank())) {
                            errorMessage = "Please fill in all details."
                            return@Button
                        }
                        isLoading = true
                        errorMessage = null
                        sessionManager.saveBaseUrl(apiUrl.trim())
                        repository.rebuildService()

                        scope.launch {
                            val error = if (isLoginMode) {
                                repository.login(email.trim(), password)
                            } else {
                                repository.signup(email.trim(), password, name.trim())
                            }
                            isLoading = false
                            if (error == null) {
                                onAuthSuccess()
                            } else {
                                errorMessage = error
                            }
                        }
                    },
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Text(text = if (isLoginMode) "Sign In" else "Create Account")
                    }
                }

                // Switch Mode click text
                Text(
                    text = if (isLoginMode) "Don't have an account? Sign Up" else "Already have an account? Sign In",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 13.sp,
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .clickable {
                            isLoginMode = !isLoginMode
                            errorMessage = null
                        }
                )
            }
        }
    }
}
