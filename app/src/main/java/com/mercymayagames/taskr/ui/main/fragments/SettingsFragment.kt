package com.mercymayagames.taskr.ui.main.fragments

import android.content.Intent
import android.os.Bundle
import android.view.*
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import com.mercymayagames.taskr.databinding.FragmentSettingsBinding
import com.mercymayagames.taskr.ui.login.LoginActivity
import com.mercymayagames.taskr.util.SharedPrefManager

/**
 * This fragment now only handles:
 * - Dark Mode toggling (stored locally)
 * - Logout
 *
 * All text-size functionality has been removed.
 */
class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private lateinit var sharedPrefManager: SharedPrefManager

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        sharedPrefManager = SharedPrefManager(requireContext())

        // Initialize the dark mode switch from user preferences
        binding.switchDarkMode.isChecked = sharedPrefManager.isDarkMode()

        // When the user toggles Dark Mode
        binding.switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            sharedPrefManager.setDarkMode(isChecked)
            applyDarkMode(isChecked)
            // Recreate activity so theme changes apply immediately
            requireActivity().recreate()
        }

        // Logout button
        binding.btnLogout.setOnClickListener {
            sharedPrefManager.logout()
            val intent = Intent(requireContext(), LoginActivity::class.java)
            startActivity(intent)
            requireActivity().finish()
        }
    }

    /**
     * In the following method, we set the app's night mode
     * according to the boolean parameter (Dark or Light).
     */
    private fun applyDarkMode(enableDarkMode: Boolean) {
        if (enableDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
