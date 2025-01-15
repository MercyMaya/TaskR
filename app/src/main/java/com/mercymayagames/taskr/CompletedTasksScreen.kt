package com.mercymayagames.taskr

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mercymayagames.taskr.network.RetrofitClient
import com.mercymayagames.taskr.ui.theme.TaskRTheme
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun CompletedTasksScreen(modifier: Modifier = Modifier) {
    val taskService = RetrofitClient.instance
    var completedTasks by remember { mutableStateOf(emptyList<LocalTask>()) }
    var isLoading by remember { mutableStateOf(true) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)

    // Fetch completed tasks when the screen first launches
    LaunchedEffect(Unit) {
        try {
            val remoteTasks = taskService.getTasks(userId = 1)
            completedTasks = remoteTasks
                .filter { it.isCompleted && it.isDeleted == 0 }
                .sortedByDescending { task ->
                    task.completedAt?.let { dateFormat.parse(it)?.time } ?: Long.MIN_VALUE
                }
                .map {
                    LocalTask(
                        id = it.id,
                        title = it.title,
                        description = it.description,
                        priority = Priority.valueOf(it.priority.uppercase()),
                        order = it.order,
                        isCompleted = it.isCompleted,
                        completedAt = it.completedAt
                    )
                }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            isLoading = false
        }
    }

    Scaffold(modifier = modifier) { innerPadding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    items(completedTasks, key = { it.id }) { task ->
                        TaskCard(
                            task = task,
                            onExpand = {
                                completedTasks = completedTasks.map {
                                    it.copy(isExpanded = it.id == task.id && !it.isExpanded)
                                }
                            },
                            onComplete = { completed ->
                                if (!completed) {
                                    scope.launch {
                                        try {
                                            taskService.updateTaskStatus(
                                                taskId = task.id,
                                                isCompleted = 0
                                            )
                                            completedTasks = completedTasks.filter { it.id != task.id }
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                        }
                                    }
                                }
                            },
                            onDelete = {
                                showDeleteConfirmation = true
                            },
                            isCompletedScreen = true
                        )

                        if (showDeleteConfirmation) {
                            AlertDialog(
                                onDismissRequest = { showDeleteConfirmation = false },
                                confirmButton = {
                                    TextButton(onClick = {
                                        scope.launch {
                                            try {
                                                taskService.deleteTask(taskId = task.id)
                                                completedTasks = completedTasks.filter { it.id != task.id }
                                            } catch (e: Exception) {
                                                e.printStackTrace()
                                            } finally {
                                                showDeleteConfirmation = false
                                            }
                                        }
                                    }) {
                                        Text("Delete")
                                    }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showDeleteConfirmation = false }) {
                                        Text("Cancel")
                                    }
                                },
                                title = { Text("Confirm Delete") },
                                text = { Text("Are you sure you want to delete this task? This action cannot be undone.") }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TaskCard(
    modifier: Modifier = Modifier,
    task: LocalTask,
    onExpand: () -> Unit,
    onComplete: (Boolean) -> Unit = {},
    onDelete: () -> Unit = {},
    isCompletedScreen: Boolean = false // Used to differentiate screens
) {
    val scope = rememberCoroutineScope()

    Card(
        modifier = modifier
            .padding(8.dp)
            .clickable { onExpand() }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(16.dp)
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = if (task.isExpanded) Int.MAX_VALUE else 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    // Checkbox on the far right
                    Checkbox(
                        checked = task.isCompleted,
                        onCheckedChange = { checked ->
                            scope.launch {
                                onComplete(checked)
                            }
                        }
                    )
                }

                if (task.isExpanded) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = task.description ?: "No Description",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Row(
                        horizontalArrangement = Arrangement.End,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isCompletedScreen) {
                            IconButton(onClick = onDelete) {
                                Icon(
                                    imageVector = Icons.Filled.Delete,
                                    contentDescription = "Delete Task"
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CompletedTasksScreenPreview() {
    TaskRTheme {
        CompletedTasksScreen()
    }
}
