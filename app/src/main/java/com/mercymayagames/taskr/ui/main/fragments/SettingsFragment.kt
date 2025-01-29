package com.mercymayagames.taskr.ui.main.fragments

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.*
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import com.mercymayagames.taskr.databinding.FragmentSettingsBinding
import com.mercymayagames.taskr.ui.login.LoginActivity
import com.mercymayagames.taskr.util.SharedPrefManager

/**
 * This fragment now handles:
 * - Dark Mode toggling (stored locally)
 * - Logout
 * - Credits display
 * - Tip Jar with Venmo links
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
        // Initialize shared prefs
        sharedPrefManager = SharedPrefManager(requireContext())

        // Dark Mode Switch
        binding.switchDarkMode.isChecked = sharedPrefManager.isDarkMode()

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

        /**
         * In the following lines, we set up the "tip jar" buttons.
         * Each button calls openVenmo(...) with a different amount.
         */
        binding.btnTipFive.setOnClickListener {
            openVenmo(5.0)
        }
        binding.btnTipTen.setOnClickListener {
            openVenmo(10.0)
        }
        binding.btnTipTwenty.setOnClickListener {
            openVenmo(20.0)
        }
        binding.btnTipCustom.setOnClickListener {
            // If user wants a custom tip, we can open Venmo with no amount
            // so they can type it in manually.
            openVenmo(null)
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

    /**
     * This helper method constructs a Venmo link and starts an ACTION_VIEW intent.
     * If amount == null, we omit the &amount= param so the user can enter a custom tip.
     */
    private fun openVenmo(amount: Double?) {
        // Basic Venmo URL approach.
        // You could do "venmo://paycharge?txn=pay&..." but many rely on https://venmo.com/...
        val baseUrl = "https://venmo.com/u/Mercy-Maya?txn=pay"

        // If an amount was specified, add it to the query
        val fullUrl = if (amount != null) {
            "$baseUrl&amount=$amount"
        } else {
            // Custom tip => no amount param
            baseUrl
        }

        val venmoUri = Uri.parse(fullUrl)
        val intent = Intent(Intent.ACTION_VIEW, venmoUri)
        // Attempt to open the Venmo app or a browser fallback
        startActivity(intent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
