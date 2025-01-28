package com.mercymayagames.taskr.data.local

import androidx.room.*
import com.mercymayagames.taskr.data.model.Task

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks WHERE isSoftDeleted = 0")
    fun getAllActiveTasks(): List<Task>

    @Query("SELECT * FROM tasks WHERE isCompleted = 1 AND isSoftDeleted = 0")
    fun getCompletedTasks(): List<Task>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertOrUpdateTask(task: Task)

    @Update
    fun updateTask(task: Task)

    @Delete
    fun deleteTask(task: Task)
}
