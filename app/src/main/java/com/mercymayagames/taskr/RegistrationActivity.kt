package com.mercymayagames.taskr

import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class RegistrationActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registration)

        val emailField: EditText = findViewById(R.id.etEmail)
        val passwordField: EditText = findViewById(R.id.etPassword)
        val registerButton: Button = findViewById(R.id.btnRegister)

        val requirementLength: TextView = findViewById(R.id.tvRequirementLength)
        val requirementUppercase: TextView = findViewById(R.id.tvRequirementUppercase)
        val requirementLowercase: TextView = findViewById(R.id.tvRequirementLowercase)
        val requirementNumber: TextView = findViewById(R.id.tvRequirementNumber)

        passwordField.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val password = s.toString()

                updateRequirement(requirementLength, password.length >= 8)
                updateRequirement(requirementUppercase, password.any { it.isUpperCase() })
                updateRequirement(requirementLowercase, password.any { it.isLowerCase() })
                updateRequirement(requirementNumber, password.any { it.isDigit() })

                val isPasswordValid = password.length >= 8 &&
                        password.any { it.isUpperCase() } &&
                        password.any { it.isLowerCase() } &&
                        password.any { it.isDigit() }
                val isEmailValid = isValidEmail(emailField.text.toString())

                registerButton.isEnabled = isPasswordValid && isEmailValid
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        registerButton.setOnClickListener {
            val email = emailField.text.toString().trim()
            val password = passwordField.text.toString()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            registerUser(email, password)
        }
    }

    private fun updateRequirement(textView: TextView, isValid: Boolean) {
        textView.setTextColor(if (isValid) Color.GREEN else Color.RED)
    }

    private fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }
    private fun registerUser(email: String, password: String) {
        val apiService = ApiClient.getApiService()
        val call = apiService.registerUser(email, password)

        // Log the data being sent
        Log.d("RegistrationActivity", "Sending registration request: email=$email, password=$password")

        call.enqueue(object : Callback<ApiResponse> {
            override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                if (response.isSuccessful) {
                    val apiResponse = response.body()
                    if (apiResponse != null) {
                        Log.d("RegistrationActivity", "Response: success=${apiResponse.success}, message=${apiResponse.message}")
                        Toast.makeText(
                            this@RegistrationActivity,
                            apiResponse.message,
                            Toast.LENGTH_LONG
                        ).show()

                        if (apiResponse.success) {
                            finish() // Close registration screen on success
                        }
                    } else {
                        Log.e("RegistrationActivity", "Response body is null")
                        Toast.makeText(
                            this@RegistrationActivity,
                            "Failed to register. Please try again.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                } else {
                    Log.e("RegistrationActivity", "Response error: code=${response.code()}, message=${response.message()}")
                    Toast.makeText(
                        this@RegistrationActivity,
                        "Failed to register. Please try again.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }

            override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                Log.e("RegistrationActivity", "API call failed: ${t.message}", t)
                Toast.makeText(
                    this@RegistrationActivity,
                    "An error occurred: ${t.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        })
    }

}
