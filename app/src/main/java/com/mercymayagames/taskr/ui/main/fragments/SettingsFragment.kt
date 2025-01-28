package com.mercymayagames.taskr.ui.main.fragments

import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import com.mercymayagames.taskr.databinding.FragmentSettingsBinding
import com.mercymayagames.taskr.util.SharedPrefManager

/**
 * In the following lines, we allow the user to adjust text size,
 * enable/disable dark mode, etc. for accessibility.
 */
class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private lateinit var sharedPrefManager: SharedPrefManager

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        sharedPrefManager = SharedPrefManager(requireContext())

        // Initialize switch and slider
        binding.switchDarkMode.isChecked = sharedPrefManager.isDarkMode()
        binding.seekBarTextSize.progress = (sharedPrefManager.getTextSize() - 10).toInt()

        binding.switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            sharedPrefManager.setDarkMode(isChecked)
            // For a real app, you'd update the theme dynamically or require an activity restart
            Toast.makeText(requireContext(), "Dark Mode: $isChecked", Toast.LENGTH_SHORT).show()
        }

        binding.seekBarTextSize.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val textSize = progress + 10f
                sharedPrefManager.setTextSize(textSize)
                binding.tvSampleText.textSize = textSize
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
