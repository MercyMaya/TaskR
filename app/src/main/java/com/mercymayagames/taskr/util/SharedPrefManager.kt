package com.mercymayagames.taskr.util

import android.content.Context
import android.content.SharedPreferences

/**
 * In the following lines, we manage user sessions and basic settings (like text size or dark mode).
 */
class SharedPrefManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("TaskRPrefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_USER_ID = "KEY_USER_ID"
        private const val KEY_USERNAME = "KEY_USERNAME"
        private const val KEY_EMAIL = "KEY_EMAIL"
        private const val KEY_LOGGED_IN = "KEY_LOGGED_IN"

        private const val KEY_TEXT_SIZE = "KEY_TEXT_SIZE"
        private const val KEY_DARK_MODE = "KEY_DARK_MODE"
    }

    fun saveLoginData(userId: Int, username: String, email: String) {
        prefs.edit().apply {
            putInt(KEY_USER_ID, userId)
            putString(KEY_USERNAME, username)
            putString(KEY_EMAIL, email)
            putBoolean(KEY_LOGGED_IN, true)
            apply()
        }
    }

    fun isLoggedIn(): Boolean = prefs.getBoolean(KEY_LOGGED_IN, false)

    fun getUserId(): Int = prefs.getInt(KEY_USER_ID, 0)
    fun getUsername(): String? = prefs.getString(KEY_USERNAME, null)
    fun getEmail(): String? = prefs.getString(KEY_EMAIL, null)

    fun logout() {
        prefs.edit().clear().apply()
    }

    // Accessibility / UI preferences
    fun setTextSize(size: Float) {
        prefs.edit().putFloat(KEY_TEXT_SIZE, size).apply()
    }
    fun getTextSize(): Float = prefs.getFloat(KEY_TEXT_SIZE, 16f)

    fun setDarkMode(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_DARK_MODE, enabled).apply()
    }
    fun isDarkMode(): Boolean = prefs.getBoolean(KEY_DARK_MODE, false)
}
