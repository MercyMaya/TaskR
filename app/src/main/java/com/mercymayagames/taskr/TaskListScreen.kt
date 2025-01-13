package com.mercymayagames.taskr

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
                    // Group tasks by priority, but skip those that are completed
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
                                        .heightIn(max = 400.dp) // optional height limit
                                ) {
                                    PriorityGroup(
                                        priority = priority,
                                        tasks = tasks,
                                        onTasksChanged = { updatedTasks -> tasks = updatedTasks },
                                        onShowDialog = { showDialog = it },
                                        onSetEditingTask = { editingTask = it },
                                        onSwipeComplete = { swipedTask ->
                                            // This triggers after a successful swipe completion
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

            // The "Undo" message for a swiped/completed task
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
                                    // Mark the task as incomplete again on the server
                                    taskService.updateTaskStatus(
                                        taskId = swiped.id,
                                        isCompleted = 0 // 0 = false
                                    )
                                    // Update local state
                                    tasks = tasks.map { t ->
                                        if (t.id == swiped.id) t.copy(isCompleted = false) else t
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                        }
                        showUndoMessage = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Text("Undo")
                }
            }
        }

        // Dialog for adding/editing a task
        if (showDialog) {
            AddEditTaskDialog(
                task = editingTask,
                onDismiss = { showDialog = false },
                onSave = { newOrUpdated ->
                    scope.launch {
                        try {
                            if (editingTask == null) {
                                // If it's a brand new task
                                taskService.addTask(
                                    userId = 1,
                                    title = newOrUpdated.title,
                                    description = newOrUpdated.description,
                                    priority = newOrUpdated.priority.name,
                                    dueDate = "2025-01-12"
                                )
                                // Re-fetch tasks from the server to get the actual task ID
                                val updatedTasks = taskService.getTasks(userId = 1)
                                tasks = updatedTasks.map {
                                    LocalTask(
                                        id = it.id,
                                        title = it.title,
                                        description = it.description,
                                        priority = Priority.valueOf(it.priority.uppercase()),
                                        order = it.order,
                                        isCompleted = it.isCompleted
                                    )
                                }
                            } else {
                                // If editing an existing task, update only locally for now
                                tasks = tasks.map {
                                    if (it.id == newOrUpdated.id) newOrUpdated else it
                                }
                                // Optionally, call a real "updateTask" endpoint if you have one
                            }
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
    // Filter tasks for this priority, excluding completed tasks
    val tasksForPriority = tasks.filter { it.priority == priority && !it.isCompleted }
        .sortedBy { it.order }

    if (tasksForPriority.isEmpty()) return

    val reorderState = rememberReorderableLazyListState(
        onMove = { from, to ->
            val currentList = tasks.toMutableList()
            val subIndices = currentList
                .withIndex()
                .filter { it.value.priority == priority && !it.value.isCompleted }
                .map { it.index }

            if (from.index in subIndices.indices && to.index in subIndices.indices) {
                val fromGlobalIndex = subIndices[from.index]
                val toGlobalIndex = subIndices[to.index]

                val movingTask = currentList.removeAt(fromGlobalIndex)
                currentList.add(toGlobalIndex, movingTask)

                // Reassign 'order' for tasks of this priority
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
                        // Toggle expansion
                        onTasksChanged(
                            tasks.map {
                                if (it.id == task.id) it.copy(isExpanded = !it.isExpanded)
                                else it
                            }
                        )
                    },
                    onEdit = {
                        onSetEditingTask(it)
                        onShowDialog(true)
                    },
                    onComplete = { completed ->
                        // Update the local list
                        onTasksChanged(
                            tasks.map {
                                if (it.id == task.id) it.copy(isCompleted = completed) else it
                            }
                        )
                        if (completed) onSwipeComplete(task)
                    },
                    isDragged = isDragging,
                    modifier = Modifier.fillMaxWidth()
                )
            }
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
    task: LocalTask,
    onExpand: () -> Unit,
    onEdit: (LocalTask) -> Unit,
    onComplete: (Boolean) -> Unit,
    isDragged: Boolean,
    modifier: Modifier = Modifier
) {
    val swipeThreshold = 50f
    val taskService = RetrofitClient.instance
    val scope = rememberCoroutineScope()

    Card(
        modifier = modifier
            // Detect horizontal drag (swipe to complete)
            .pointerInput(Unit) {
                detectHorizontalDragGestures { _, dragAmount ->
                    // Swipe right beyond threshold => mark complete
                    if (dragAmount > swipeThreshold && !task.isCompleted) {
                        scope.launch {
                            try {
                                // Mark as completed (1) on the server
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
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
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
                        onCheckedChange = { checked ->
                            scope.launch {
                                try {
                                    // Update the server with 0 or 1
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
                }
                if (task.isExpanded) {
                    Spacer(modifier = Modifier.height(8.dp))
                    task.description?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
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
}

@Composable
fun AddEditTaskDialog(
    task: LocalTask?,
    onDismiss: () -> Unit,
    onSave: (LocalTask) -> Unit
) {
    var title by remember { mutableStateOf(task?.title ?: "") }
    var description by remember { mutableStateOf(task?.description ?: "") }
    var priority by remember { mutableStateOf(task?.priority ?: Priority.MEDIUM) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
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
                    Priority.entries.forEach { p ->
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
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
fun TaskListScreenPreview() {
    TaskRTheme {
        TaskListScreen()
    }
}
