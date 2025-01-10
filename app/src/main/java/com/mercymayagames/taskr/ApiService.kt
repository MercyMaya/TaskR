package com.mercymayagames.taskr

import retrofit2.Call
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

interface ApiService {

    // Existing loginUser function
    @POST("login.php")
    @FormUrlEncoded
    fun loginUser(
        @Field("email") email: String,
        @Field("password") password: String
    ): Call<LoginResponse>

    // New registerUser function
    @POST("register.php")
    @FormUrlEncoded
    fun registerUser(
        @Field("email") email: String,
        @Field("password") password: String
    ): Call<ApiResponse>
}
