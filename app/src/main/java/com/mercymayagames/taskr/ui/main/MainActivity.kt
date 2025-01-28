package com.mercymayagames.taskr.ui.main

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.mercymayagames.taskr.R
import com.mercymayagames.taskr.databinding.ActivityMainBinding
import com.mercymayagames.taskr.ui.main.fragments.CompletedTasksFragment
import com.mercymayagames.taskr.ui.main.fragments.SettingsFragment
import com.mercymayagames.taskr.ui.main.fragments.TasksFragment
import com.google.android.material.bottomnavigation.BottomNavigationView

/**
 * In the following activity, we host 3 fragments (Tasks, Completed, Settings)
 * using a BottomNavigationView. We ensure that toggling dark mode
 * in Settings won't force us back to Tasks upon recreation.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    // Key for saving/restoring which menu item was selected
    private val SELECTED_ITEM_KEY = "SELECTED_ITEM_KEY"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Use ViewBinding for activity_main.xml
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // If there's no previously selected fragment, load the default (Tasks)
        // If we do have a savedInstanceState, onRestoreInstanceState() will handle it
        if (savedInstanceState == null) {
            loadFragment(TasksFragment())
        }

        /**
         * In the following lines, we handle bottom nav item clicks
         * to swap fragments accordingly.
         */
        binding.bottomNav.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_tasks -> {
                    loadFragment(TasksFragment())
                    true
                }
                R.id.nav_completed -> {
                    loadFragment(CompletedTasksFragment())
                    true
                }
                R.id.nav_settings -> {
                    loadFragment(SettingsFragment())
                    true
                }
                else -> false
            }
        }
    }

    /**
     * In the following lines, we save which nav item is selected
     * so that after toggling dark mode (which recreates the activity),
     * we can restore the same fragment.
     */
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        val selectedItemId = binding.bottomNav.selectedItemId
        outState.putInt(SELECTED_ITEM_KEY, selectedItemId)
    }

    /**
     * Once the activity is recreated, we reselect the same menu item
     * that was previously chosen, so the user remains in the correct screen.
     */
    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        val selectedId = savedInstanceState.getInt(SELECTED_ITEM_KEY, R.id.nav_tasks)
        // Force the bottom nav to show the saved item
        binding.bottomNav.selectedItemId = selectedId
    }

    /**
     * A helper function to swap the fragment displayed in the container.
     */
    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }
}
