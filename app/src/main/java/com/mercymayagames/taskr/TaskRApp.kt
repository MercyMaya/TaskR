package com.mercymayagames.taskr

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.mercymayagames.taskr.util.SharedPrefManager

/**
 * TaskRApp is our custom Application class.
 * We use it to force the app to use the user's saved dark/light mode
 * immediately on launch, with light as default if no preference is found.
 */
class TaskRApp : Application() {

    override fun onCreate() {
        super.onCreate()

        // Step 1: Initialize your SharedPreferences manager
        val sharedPrefManager = SharedPrefManager(this)

        // Step 2: Check if user selected dark mode previously
        if (sharedPrefManager.isDarkMode()) {
            // If they set dark mode in Settings, use dark
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            // Otherwise, default to light
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
    }
}
