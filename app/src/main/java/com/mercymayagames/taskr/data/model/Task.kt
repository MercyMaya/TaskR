package com.mercymayagames.taskr.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * The Task entity for offline storage.
 * In the following lines, we define fields matching or closely matching
 * our MySQL table for easy synchronization.
 */
@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true)
    val localId: Int = 0,
    val id: Int? = null,  // Remote ID
    val userId: Int,
    val taskTitle: String,
    val taskDescription: String,
    val priority: String = "Normal",
    val isCompleted: Boolean = false,
    val isSoftDeleted: Boolean = false,
    val createdAt: String? = null,
    val updatedAt: String? = null
)
