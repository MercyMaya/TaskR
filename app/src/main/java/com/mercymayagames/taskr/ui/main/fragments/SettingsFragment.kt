package com.mercymayagames.taskr.ui.main.fragments

import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import com.mercymayagames.taskr.R
import com.mercymayagames.taskr.databinding.FragmentSettingsBinding
import com.mercymayagames.taskr.ui.login.LoginActivity
import com.mercymayagames.taskr.util.SharedPrefManager

/**
 * In the following fragment, we allow the user to:
 * - Toggle between Dark Mode and Light Mode (locally stored, no server calls)
 * - Adjust text size
 * - Logout
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

        // Read the locally stored dark mode value and set the app theme accordingly
        applyDarkMode(sharedPrefManager.isDarkMode())

        // Initialize dark mode switch from prefs
        binding.switchDarkMode.isChecked = sharedPrefManager.isDarkMode()

        // Initialize text size from prefs
        binding.seekBarTextSize.progress = (sharedPrefManager.getTextSize() - 10).toInt()
        binding.tvSampleText.textSize = sharedPrefManager.getTextSize()

        /**
         * When the dark mode switch is toggled, we:
         * 1) Save the preference
         * 2) Call applyDarkMode(...) with the new setting
         * 3) Optionally recreate the activity so theme changes take immediate effect
         */
        binding.switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            sharedPrefManager.setDarkMode(isChecked)
            applyDarkMode(isChecked)
            // Recreate the activity so the new theme is applied instantly
            requireActivity().recreate()
        }

        // Text Size SeekBar
        binding.seekBarTextSize.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val textSize = progress + 10f
                sharedPrefManager.setTextSize(textSize)
                binding.tvSampleText.textSize = textSize
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Logout button
        binding.btnLogout.setOnClickListener {
            sharedPrefManager.logout()
            val intent = Intent(requireContext(), LoginActivity::class.java)
            startActivity(intent)
            requireActivity().finish()
        }
    }

    /**
     * In the following method, we set the app's default night mode
     * according to the boolean parameter.
     * This does not involve any server calls—just local storage/logic.
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
