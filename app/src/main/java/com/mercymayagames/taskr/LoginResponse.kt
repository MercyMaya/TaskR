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

data class ApiResponse(
    val success: Boolean, // Indicates whether the operation was successful
    val message: String,  // Provides a message for the user
    val data: Any? = null // Optional field for additional data (e.g., user details, token)
)