package com.mercymayagames.taskr.network

import com.google.gson.annotations.SerializedName
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*

// 1) Updated data class that reflects the DB columns.
//    - "is_completed" comes back as 0 or 1. We'll store that
//      in isCompletedInt, and then isCompleted is derived.
data class Task(
    val id: Int,

    @SerializedName("user_id")
    val userId: Int,

    val title: String,
    val description: String?,        // text can be null if desired
    val priority: String,           // e.g. "HIGH", "MEDIUM", or "LOW"

    @SerializedName("due_date")
    val dueDate: String?,           // can be null if you don't always set it

    @SerializedName("order")
    val order: Int,                 // the task's ordering

    @SerializedName("is_completed")
    val isCompletedInt: Int,        // 0 or 1 coming from the server

    @SerializedName("created_at")
    val createdAt: String?          // might be something like "2025-01-13 03:59:40"
) {
    // This derived property makes it easier to handle
    // the "isCompleted" concept in your UI code.
    val isCompleted: Boolean
        get() = (isCompletedInt == 1)
}

// 2) Your Retrofit interface with updated calls.
interface TaskApiService {

    // GET tasks for a particular user
    @GET("get_tasks.php")
    suspend fun getTasks(
        @Query("user_id") userId: Int
    ): List<Task>

    // Add a new task
    @FormUrlEncoded
    @POST("add_task.php")
    suspend fun addTask(
        @Field("user_id") userId: Int,
        @Field("title") title: String,
        @Field("description") description: String?,
        @Field("priority") priority: String,
        @Field("due_date") dueDate: String?  // e.g. "2025-01-12"
    )

    // Update completion status
    @FormUrlEncoded
    @POST("update_task_status.php")
    suspend fun updateTaskStatus(
        @Field("task_id") taskId: Int,
        @Field("is_completed") isCompleted: Int
    )
}

// 3) The Retrofit singleton with logging and JSON conversion.
object RetrofitClient {
    private const val BASE_URL = "https://voxursa.com/TaskR/"

    val instance: TaskApiService by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()

        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(TaskApiService::class.java)
    }
}