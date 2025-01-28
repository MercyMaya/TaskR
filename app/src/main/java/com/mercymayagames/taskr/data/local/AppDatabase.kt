package com.mercymayagames.taskr.data.local

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import android.content.Context
import com.mercymayagames.taskr.data.model.Task

/**
 * In the following lines, we create our Room database class,
 * specifying our entities and version number.
 */
@Database(entities = [Task::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun taskDao(): TaskDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                val newInstance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "taskr_db"
                ).build()
                instance = newInstance
                newInstance
            }
        }
    }
}
