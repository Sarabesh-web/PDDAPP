package com.example.smartplannerapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartplannerapp.data.DataRepository
import com.example.smartplannerapp.data.Task
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksScreen(
    repository: DataRepository,
    modifier: Modifier = Modifier
) {
    val tasks by repository.tasks.collectAsState()
    
    var searchQuery by remember { mutableStateOf("") }
    var filterSelected by remember { mutableStateOf("all") } // all, pending, completed
    var showAddDialog by remember { mutableStateOf(false) }

    // Dialog state fields
    var newTitle by remember { mutableStateOf("") }
    var newDesc by remember { mutableStateOf("") }
    var newSubject by remember { mutableStateOf("") }
    var newPriority by remember { mutableStateOf("medium") } // low, medium, high
    var newDueDate by remember { mutableStateOf("") }

    val filteredTasks = tasks.filter { task ->
        val matchesSearch = task.title.contains(searchQuery, ignoreCase = true) || 
                            task.description.contains(searchQuery, ignoreCase = true)
        val matchesFilter = when (filterSelected) {
            "pending" -> task.status == "pending"
            "completed" -> task.status == "completed"
            else -> true
        }
        matchesSearch && matchesFilter
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { 
                    newTitle = ""
                    newDesc = ""
                    newSubject = "General"
                    newPriority = "medium"
                    newDueDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                    showAddDialog = true 
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Task")
            }
        },
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Text(
                text = "Task Management",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Track your assignments, homework, and exams.",
                fontSize = 13.sp,
                color = Color.Gray,
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
            )

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search tasks...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Filter Tabs
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("all", "pending", "completed").forEach { filter ->
                    val isSelected = filterSelected == filter
                    val label = when (filter) {
                        "pending" -> "Pending"
                        "completed" -> "Completed"
                        else -> "All"
                    }
                    Button(
                        onClick = { filterSelected = filter },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(text = label, fontSize = 12.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Task List
            if (filteredTasks.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "No tasks found.", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredTasks) { task ->
                        TaskItem(
                            task = task,
                            onToggle = { repository.toggleTask(task.id) },
                            onDelete = { repository.deleteTask(task.id) }
                        )
                    }
                }
            }
        }
    }

    // Add Task Dialog
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Create New Task") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newTitle,
                        onValueChange = { newTitle = it },
                        label = { Text("Title") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newDesc,
                        onValueChange = { newDesc = it },
                        label = { Text("Description") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newSubject,
                        onValueChange = { newSubject = it },
                        label = { Text("Subject") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newDueDate,
                        onValueChange = { newDueDate = it },
                        label = { Text("Due Date (YYYY-MM-DD)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Text("Priority", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(top = 4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("low", "medium", "high").forEach { prio ->
                            val isSelected = newPriority == prio
                            Button(
                                onClick = { newPriority = prio },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(prio.uppercase(), fontSize = 10.sp)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newTitle.isNotBlank()) {
                            repository.addTask(
                                title = newTitle,
                                description = newDesc,
                                subject = newSubject,
                                priority = newPriority,
                                dueDate = newDueDate
                            )
                            showAddDialog = false
                        }
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun TaskItem(
    task: Task,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    val isCompleted = task.status == "completed"

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCompleted) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Checkbox(
                    checked = isCompleted,
                    onCheckedChange = { onToggle() }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = task.title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        textDecoration = if (isCompleted) TextDecoration.LineThrough else null,
                        color = if (isCompleted) Color.Gray else MaterialTheme.colorScheme.onSurface
                    )
                    if (task.description.isNotBlank()) {
                        Text(
                            text = task.description,
                            fontSize = 12.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                    Text(
                        text = "${task.subject} • Due ${task.dueDate}",
                        fontSize = 11.sp,
                        color = Color.LightGray,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
            
            Row(verticalAlignment = Alignment.CenterVertically) {
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
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = Color.Gray
                    )
                }
            }
        }
    }
}
