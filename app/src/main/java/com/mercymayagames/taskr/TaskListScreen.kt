package com.mercymayagames.taskr


import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mercymayagames.taskr.ui.theme.TaskRTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.foundation.clickable
import androidx.compose.ui.input.pointer.positionChange

data class Task(
    val id: Int,
    var title: String,
    var description: String,
    var priority: Priority,
    var order: Int,
    var isCompleted: Boolean,
    var isExpanded: Boolean = false
)

enum class Priority { HIGH, MEDIUM, LOW }

@Composable
fun TaskListScreen(modifier: Modifier = Modifier) {
    var tasks by remember { mutableStateOf(sampleTasks().sortedBy { it.order }) }
    var showDialog by remember { mutableStateOf(false) }
    var editingTask by remember { mutableStateOf<Task?>(null) }
    var showUndoMessage by remember { mutableStateOf(false) }
    var lastSwipedTask by remember { mutableStateOf<Task?>(null) }
    val scope = rememberCoroutineScope()

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = {
                editingTask = null
                showDialog = true
            }) {
                Icon(Icons.Filled.Add, contentDescription = "Add Task")
            }
        },
        modifier = modifier
    ) { innerPadding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(), // Make the entire column scrollable
                contentPadding = PaddingValues(bottom = 120.dp) // Space for the undo button
            ) {
                Priority.entries.forEach { priority ->
                    val tasksForPriority = tasks.filter { it.priority == priority && !it.isCompleted }
                    if (tasksForPriority.isNotEmpty()) {
                        item {
                            PriorityHeader(priority) // Add priority header
                        }
                        itemsIndexed(tasksForPriority, key = { _, task -> task.id }) { _, task ->
                            TaskCard(
                                task = task,
                                onExpand = {
                                    tasks = tasks.map {
                                        if (it.id == task.id) it.copy(isExpanded = !it.isExpanded) else it
                                    }
                                },
                                onEdit = {
                                    editingTask = it
                                    showDialog = true
                                },
                                onComplete = { completed ->
                                    tasks = tasks.map {
                                        if (it.id == task.id) it.copy(isCompleted = completed) else it
                                    }
                                    lastSwipedTask = if (completed) task else null
                                    showUndoMessage = completed
                                    if (completed) {
                                        scope.launch {
                                            delay(5000)
                                            showUndoMessage = false
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().padding(8.dp)
                            )
                        }
                    }
                }
            }

            // Undo Button
            AnimatedVisibility(
                visible = showUndoMessage,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter) // Position at the bottom center
                    .padding(bottom = 72.dp) // Ensure it's above the bottom navigation bar
            ) {
                Button(
                    onClick = {
                        lastSwipedTask?.let {
                            tasks = tasks.map { t ->
                                if (t.id == it.id) t.copy(isCompleted = false) else t
                            }
                        }
                        showUndoMessage = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary // Contrasting color
                    )
                ) {
                    Text("Undo")
                }
            }
        }

        if (showDialog) {
            AddEditTaskDialog(
                task = editingTask,
                onDismiss = { showDialog = false },
                onSave = { task ->
                    tasks = if (editingTask == null) {
                        tasks + task.copy(
                            id = tasks.size + 1,
                            order = tasks.count { it.priority == task.priority }
                        )
                    } else {
                        tasks.map { if (it.id == task.id) task else it }
                    }
                    showDialog = false
                }
            )
        }
    }
}


@Composable
fun PriorityHeader(priority: Priority) {
    Text(
        text = when (priority) {
            Priority.HIGH -> "High Priority"
            Priority.MEDIUM -> "Medium Priority"
            Priority.LOW -> "Low Priority"
        },
        style = MaterialTheme.typography.titleLarge,
        modifier = Modifier.padding(16.dp)
    )
}

@Composable
fun TaskCard(
    task: Task,
    onExpand: () -> Unit,
    onEdit: (Task) -> Unit,
    onComplete: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        val dragAmount = event.changes.first().positionChange().x
                        println("Pointer event detected: $dragAmount") // Debugging
                        if (dragAmount > 30) { // Swipe right
                            onComplete(true)
                        } else if (dragAmount < -30) { // Swipe left
                            onComplete(false)
                        }
                    }
                }
            }
            .clickable { onExpand() } // Ensure this is below pointerInput
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = if (task.isExpanded) Int.MAX_VALUE else 1,
                    overflow = if (task.isExpanded) TextOverflow.Visible else TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Checkbox(
                    checked = task.isCompleted,
                    onCheckedChange = { isChecked ->
                        onComplete(isChecked)
                    }
                )
            }

            if (task.isExpanded) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = task.description,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    IconButton(onClick = { onEdit(task) }) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = "Edit Task"
                        )
                    }
                }
            }
        }
    }
}



@Composable
fun AddEditTaskDialog(
    task: Task?,
    onDismiss: () -> Unit,
    onSave: (Task) -> Unit
) {
    var title by remember { mutableStateOf(task?.title ?: "") }
    var description by remember { mutableStateOf(task?.description ?: "") }
    var priority by remember { mutableStateOf(task?.priority ?: Priority.MEDIUM) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                onSave(
                    Task(
                        id = task?.id ?: 0,
                        title = title,
                        description = description,
                        priority = priority,
                        order = task?.order ?: 0,
                        isCompleted = task?.isCompleted ?: false
                    )
                )
            }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        title = { Text(text = if (task == null) "Add Task" else "Edit Task") },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Priority:")
                    Priority.entries.forEach { p ->
                        TextButton(onClick = { priority = p }) {
                            Text(
                                text = p.name,
                                style = if (priority == p) MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.primary)
                                else MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            }
        }
    )
}

fun sampleTasks() = listOf(
    Task(1, "High Priority Task 1", "Description of High Task 1", Priority.HIGH, 0, false),
    Task(2, "High Priority Task 2", "This is a very long description of High Priority Task 2 meant to test text overflow and expandability", Priority.HIGH, 1, false),
    Task(3, "High Priority Task 3 with a Very Long Title for Testing", "Description of High Task 3", Priority.HIGH, 2, false),
    Task(4, "Medium Priority Task 1", "Description of Medium Task 1", Priority.MEDIUM, 0, false),
    Task(5, "Medium Priority Task 2", "This is a very long description of Medium Priority Task 2 meant to test text overflow and expandability", Priority.MEDIUM, 1, false),
    Task(6, "Medium Priority Task 3 with a Very Long Title for Testing", "Description of Medium Task 3", Priority.MEDIUM, 2, false),
    Task(7, "Low Priority Task 1", "Description of Low Task 1", Priority.LOW, 0, false),
    Task(8, "Low Priority Task 2", "This is a very long description of Low Priority Task 2 meant to test text overflow and expandability", Priority.LOW, 1, false),
    Task(9, "Low Priority Task 3 with a Very Long Title for Testing", "Description of Low Task 3", Priority.LOW, 2, false)
)

@Preview(showBackground = true)
@Composable
fun TaskListScreenPreview() {
    TaskRTheme {
        TaskListScreen()
    }
}
