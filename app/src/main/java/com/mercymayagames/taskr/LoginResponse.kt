package com.mercymayagames.taskr

data class LoginResponse(
    val success: Boolean,
    val message: String,
    val user: User? // Optional: Include user data if required
)

data class User(
    val id: Int,
    val name: String,
    val email: String
)
