package com.mercymayagames.taskr.ui.login

import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.mercymayagames.taskr.R
import com.mercymayagames.taskr.network.ApiClient
import com.google.gson.JsonObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

/**
 * In the following lines, we manage user registration, including
 * dynamic password requirement checks and toggling visibility.
 */
class RegisterActivity : AppCompatActivity() {

    private lateinit var etUsername: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var ivShowPassword: ImageView
    private lateinit var btnRegister: Button

    // Password requirement indicators
    private lateinit var tvReqLength: TextView
    private lateinit var tvReqUpper: TextView
    private lateinit var tvReqSpecial: TextView

    private var isPasswordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        // Initialize views
        etUsername = findViewById(R.id.etUsername)
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
      //  ivShowPassword = findViewById(R.id.ivShowPassword)
        btnRegister = findViewById(R.id.btnRegister)

        tvReqLength = findViewById(R.id.tvReqLength)
        tvReqUpper = findViewById(R.id.tvReqUpper)
        tvReqSpecial = findViewById(R.id.tvReqSpecial)

        /**
         * In the following lines, we handle toggling password visibility for better accessibility.
         * Yup, here too. We never needed to write this out.
        ivShowPassword.setOnClickListener {
            isPasswordVisible = !isPasswordVisible
            if (isPasswordVisible) {
                etPassword.inputType = InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                ivShowPassword.setImageResource(R.drawable.ic_eye_off)
            } else {
                etPassword.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                ivShowPassword.setImageResource(R.drawable.ic_eye)
            }
            etPassword.setSelection(etPassword.text.length)
        }
         */

        /**
         * In the following lines, we add a text watcher to dynamically check password requirements.
         */
        etPassword.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val password = s.toString()
                updatePasswordRequirements(password)
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        btnRegister.setOnClickListener {
            val username = etUsername.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (username.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!isAllRequirementsMet(password)) {
                Toast.makeText(this, "Password requirements not met", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            registerUser(username, email, password)
        }
    }

    private fun updatePasswordRequirements(password: String) {
        // Requirement #1: length >= 8
        if (password.length >= 8) {
            tvReqLength.setTextColor(getColor(R.color.requirementMet))
        } else {
            tvReqLength.setTextColor(getColor(R.color.requirementNotMet))
        }
        // Requirement #2: at least one uppercase
        if (password.any { it.isUpperCase() }) {
            tvReqUpper.setTextColor(getColor(R.color.requirementMet))
        } else {
            tvReqUpper.setTextColor(getColor(R.color.requirementNotMet))
        }
        // Requirement #3: at least one special character
        if (password.contains(Regex("[^A-Za-z0-9 ]"))) {
            tvReqSpecial.setTextColor(getColor(R.color.requirementMet))
        } else {
            tvReqSpecial.setTextColor(getColor(R.color.requirementNotMet))
        }
    }

    private fun isAllRequirementsMet(password: String): Boolean {
        val isLengthOk = password.length >= 8
        val hasUpper = password.any { it.isUpperCase() }
        val hasSpecial = password.contains(Regex("[^A-Za-z0-9 ]"))

        return isLengthOk && hasUpper && hasSpecial
    }

    private fun registerUser(username: String, email: String, password: String) {
        val call = ApiClient.apiInterface.registerUser(username, email, password)
        call.enqueue(object : Callback<JsonObject> {
            override fun onResponse(call: Call<JsonObject>, response: Response<JsonObject>) {
                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    if (body.get("success").asBoolean) {
                        Toast.makeText(this@RegisterActivity, "Registration successful", Toast.LENGTH_SHORT).show()
                        finish()
                    } else {
                        val message = body.get("message").asString
                        Toast.makeText(this@RegisterActivity, message, Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this@RegisterActivity, "Response not successful", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<JsonObject>, t: Throwable) {
                Toast.makeText(this@RegisterActivity, "Network error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
