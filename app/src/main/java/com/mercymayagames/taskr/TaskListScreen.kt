package com.mercymayagames.taskr

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mercymayagames.taskr.ui.theme.TaskRTheme
import com.mercymayagames.taskr.network.RetrofitClient
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.burnoutcrew.reorderable.ReorderableItem
import org.burnoutcrew.reorderable.detectReorderAfterLongPress
import org.burnoutcrew.reorderable.reorderable
import org.burnoutcrew.reorderable.rememberReorderableLazyListState

data class LocalTask(
    val id: Int,
    var title: String,
    var description: String?,
    var priority: Priority,
    var order: Int,
    var isCompleted: Boolean,
    val isDeleted: Boolean = false,
    val completedAt: String? = null,
    var isExpanded: Boolean = false
)

enum class Priority { HIGH, MEDIUM, LOW }

@Composable
fun TaskListScreen(modifier: Modifier = Modifier) {
    val taskService = RetrofitClient.instance
    var tasks by remember { mutableStateOf(emptyList<LocalTask>()) }
    var isLoading by remember { mutableStateOf(true) }

    var showDialog by remember { mutableStateOf(false) }
    var editingTask by remember { mutableStateOf<LocalTask?>(null) }

    var showUndoMessage by remember { mutableStateOf(false) }
    var lastSwipedTask by remember { mutableStateOf<LocalTask?>(null) }
    val scope = rememberCoroutineScope()

    // Fetch tasks when the screen first launches
    LaunchedEffect(Unit) {
        try {
            val remoteTasks = taskService.getTasks(userId = 1)
            tasks = remoteTasks.map {
                LocalTask(
                    id = it.id,
                    title = it.title,
                    description = it.description,
                    priority = Priority.valueOf(it.priority.uppercase()),
                    order = it.order,
                    isCompleted = it.isCompleted
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            isLoading = false
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingTask = null
                    showDialog = true
                }
            ) {
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
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 120.dp)
                ) {
                    Priority.entries.toList().forEach { priority ->
                        val tasksForPriority = tasks.filter {
                            it.priority == priority && !it.isCompleted
                        }
                        if (tasksForPriority.isNotEmpty()) {
                            item {
                                PriorityHeader(priority)
                            }
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(max = 400.dp)
                                ) {
                                    PriorityGroup(
                                        priority = priority,
                                        tasks = tasks,
                                        onTasksChanged = { updatedTasks -> tasks = updatedTasks },
                                        onShowDialog = { showDialog = it },
                                        onSetEditingTask = { editingTask = it },
                                        onSwipeComplete = { swipedTask ->
                                            lastSwipedTask = swipedTask
                                            showUndoMessage = true
                                            scope.launch {
                                                delay(5000)
                                                showUndoMessage = false
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = showUndoMessage,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 72.dp)
            ) {
                Button(
                    onClick = {
                        lastSwipedTask?.let { swiped ->
                            scope.launch {
                                try {
                                    taskService.updateTaskStatus(
                                        taskId = swiped.id,
                                        isCompleted = 0
                                    )
                                    tasks = tasks.map {
                                        if (it.id == swiped.id) it.copy(isCompleted = false) else it
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                        }
                        showUndoMessage = false
                    }
                ) {
                    Text("Undo")
                }
            }
        }

        if (showDialog) {
            AddEditTaskDialog(
                task = editingTask,
                onDismiss = { showDialog = false },
                onSave = { newTask ->
                    // Add Debug Logging Here
                    scope.launch {
                        try {
                            if (editingTask == null) {
                                // Log for Adding a Task
                                Log.d("AddEditTaskDialog", "Adding Task: $newTask")
                                taskService.addTask(
                                    userId = 1,
                                    title = newTask.title,
                                    description = newTask.description,
                                    priority = newTask.priority.name,
                                    dueDate = "2025-01-12"
                                )
                            } else {
                                // Log for Updating a Task
                                Log.d("AddEditTaskDialog", "Updating Task: $newTask")
                                taskService.updateTask(
                                    taskId = newTask.id,
                                    title = newTask.title,
                                    description = newTask.description,
                                    priority = newTask.priority.name
                                )
                            }
                            // Refresh Task List
                            tasks = taskService.getTasks(userId = 1).map {
                                LocalTask(
                                    id = it.id,
                                    title = it.title,
                                    description = it.description,
                                    priority = Priority.valueOf(it.priority.uppercase()),
                                    order = it.order,
                                    isCompleted = it.isCompleted
                                )
                            }
                        } catch (e: Exception) {
                            Log.e("AddEditTaskDialog", "Error in onSave", e)
                        } finally {
                            showDialog = false
                        }
                    }
                },
                onDelete = {
                    scope.launch {
                        try {
                            taskService.deleteTask(taskId = editingTask?.id ?: 0)
                            tasks = tasks.filter { it.id != editingTask?.id }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        } finally {
                            showDialog = false
                        }
                    }
                }
            )
        }

    }
}

@Composable
fun PriorityGroup(
    priority: Priority,
    tasks: List<LocalTask>,
    onTasksChanged: (List<LocalTask>) -> Unit,
    onShowDialog: (Boolean) -> Unit,
    onSetEditingTask: (LocalTask?) -> Unit,
    onSwipeComplete: (LocalTask) -> Unit
) {
    val tasksForPriority = tasks.filter { it.priority == priority && !it.isCompleted }
        .sortedBy { it.order }

    if (tasksForPriority.isEmpty()) return

    val reorderState = rememberReorderableLazyListState(
        onMove = { from, to ->
            val currentList = tasks.toMutableList()
            val subIndices = currentList
                .withIndex()
                .filter { indexedTask -> indexedTask.value.priority == priority && !indexedTask.value.isCompleted }
                .map { it.index }

            if (from.index in subIndices.indices && to.index in subIndices.indices) {
                val fromGlobalIndex = subIndices[from.index]
                val toGlobalIndex = subIndices[to.index]

                val movingTask = currentList.removeAt(fromGlobalIndex)
                currentList.add(toGlobalIndex, movingTask)

                currentList
                    .filter { it.priority == priority && !it.isCompleted }
                    .forEachIndexed { i, task -> task.order = i }

                onTasksChanged(currentList.toList())
            }
        }
    )

    LazyColumn(
        state = reorderState.listState,
        modifier = Modifier
            .fillMaxWidth()
            .reorderable(reorderState)
            .detectReorderAfterLongPress(reorderState)
    ) {
        itemsIndexed(tasksForPriority, key = { _, t -> t.id }) { _, task ->
            ReorderableItem(reorderState, key = task.id) { isDragging ->
                TaskCard(
                    task = task,
                    onExpand = {
                        val updatedTasks = tasks.map { t ->
                            t.copy(isExpanded = t.id == task.id && !t.isExpanded)
                        }
                        onTasksChanged(updatedTasks)
                    },
                    onEdit = { onSetEditingTask(it); onShowDialog(true) },
                    onComplete = { completed ->
                        val updatedTasks = tasks.map {
                            if (it.id == task.id) it.copy(isCompleted = completed) else it
                        }
                        onTasksChanged(updatedTasks)
                        if (completed) onSwipeComplete(task)
                    },
                    isDragged = isDragging,
                    modifier = Modifier.fillMaxWidth(),
                    isCompletedScreen = false
                )
            }
        }
    }
}


@Composable
fun TaskCard(
    modifier: Modifier = Modifier,
    task: LocalTask,
    onExpand: () -> Unit,
    onEdit: (LocalTask) -> Unit = {},
    onComplete: (Boolean) -> Unit = {},
    onDelete: () -> Unit = {},
    isDragged: Boolean = false,
    isCompletedScreen: Boolean = false
) {
    val swipeThreshold = 50f
    val taskService = RetrofitClient.instance
    val scope = rememberCoroutineScope()

    Card(
        modifier = modifier
            .padding(8.dp)
            .pointerInput(Unit) {
                if (!isCompletedScreen) {
                    detectHorizontalDragGestures { _, dragAmount ->
                        if (dragAmount > swipeThreshold && !task.isCompleted) {
                            scope.launch {
                                try {
                                    taskService.updateTaskStatus(
                                        taskId = task.id,
                                        isCompleted = 1
                                    )
                                    onComplete(true)
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                        }
                    }
                }
            }
            .graphicsLayer {
                if (isDragged) {
                    scaleX = 0.95f
                    scaleY = 0.95f
                    alpha = 0.7f
                }
            }
            .shadow(if (isDragged) 16.dp else 0.dp)
            .clickable { onExpand() }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.DragIndicator,
                contentDescription = "Reorder",
                modifier = Modifier.padding(end = 8.dp)
            )
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = task.isCompleted,
                        onCheckedChange = { checked ->
                            scope.launch {
                                try {
                                    taskService.updateTaskStatus(
                                        taskId = task.id,
                                        isCompleted = if (checked) 1 else 0
                                    )
                                    onComplete(checked)
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                        }
                    )
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = if (task.isExpanded) Int.MAX_VALUE else 1,
                        overflow = TextOverflow.Ellipsis
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
                        IconButton(onClick = { onEdit(task) }) {
                            Icon(
                                imageVector = Icons.Filled.Edit,
                                contentDescription = "Edit Task"
                            )
                        }
                        IconButton(onClick = { onDelete() }) {
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

@Composable
fun AddEditTaskDialog(
    task: LocalTask?,
    onDismiss: () -> Unit,
    onSave: (LocalTask) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    var title by remember { mutableStateOf(task?.title ?: "") }
    var description by remember { mutableStateOf(task?.description ?: "") }
    var priority by remember { mutableStateOf(task?.priority ?: Priority.MEDIUM) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            confirmButton = {
                TextButton(onClick = {
                    onDelete?.invoke()
                    showDeleteConfirmation = false
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

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                // Add Save Logic Here
                onSave(
                    LocalTask(
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
        title = { Text(if (task == null) "Add Task" else "Edit Task") },
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Priority:")
                    Priority.values().forEach { p ->
                        TextButton(onClick = { priority = p }) {
                            Text(
                                text = p.name,
                                style = if (priority == p)
                                    MaterialTheme.typography.bodyLarge.copy(
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                else
                                    MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
                if (task != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { showDeleteConfirmation = true },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Delete Task")
                    }
                }
            }
        }
    )
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

@Preview(showBackground = true)
@Composable
fun TaskListScreenPreview() {
    TaskRTheme {
        TaskListScreen()
    }
}
