package com.mercymayagames.taskr.network

import com.google.gson.JsonObject
import retrofit2.Call
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

/**
 * In the following interface, we define our Retrofit endpoints.
 * Each function corresponds to a PHP script or an action.
 */
interface ApiInterface {

    @FormUrlEncoded
    @POST("register.php")
    fun registerUser(
        @Field("username") username: String,
        @Field("email") email: String,
        @Field("password") password: String
    ): Call<JsonObject>

    @FormUrlEncoded
    @POST("login.php")
    fun loginUser(
        @Field("email") email: String,
        @Field("password") password: String
    ): Call<JsonObject>

    @FormUrlEncoded
    @POST("tasks.php")
    fun fetchTasks(
        @Field("action") action: String,
        @Field("user_id") userId: Int
    ): Call<JsonObject>

    @FormUrlEncoded
    @POST("tasks.php")
    fun fetchCompletedTasks(
        @Field("action") action: String,
        @Field("user_id") userId: Int
    ): Call<JsonObject>

    @FormUrlEncoded
    @POST("tasks.php")
    fun addTask(
        @Field("action") action: String,
        @Field("user_id") userId: Int,
        @Field("task_title") title: String,
        @Field("task_description") description: String,
        @Field("priority") priority: String
    ): Call<JsonObject>

    @FormUrlEncoded
    @POST("tasks.php")
    fun updateTask(
        @Field("action") action: String,
        @Field("user_id") userId: Int,
        @Field("task_id") taskId: Int,
        @Field("task_title") title: String,
        @Field("task_description") description: String,
        @Field("priority") priority: String,
        @Field("is_completed") isCompleted: Int
    ): Call<JsonObject>

    @FormUrlEncoded
    @POST("tasks.php")
    fun softDeleteTask(
        @Field("action") action: String,
        @Field("user_id") userId: Int,
        @Field("task_id") taskId: Int
    ): Call<JsonObject>
}
