package com.example.weighttrackerapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.weighttrackerapp.domain.AuthService
import com.example.weighttrackerapp.domain.Outcome
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// Login and account creation
class MainActivity : AppCompatActivity() {

    private lateinit var auth: AuthService
    private lateinit var usernameInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var loginButton: Button
    private lateinit var createButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = ServiceLocator.authService(this)
        usernameInput = findViewById(R.id.usernameinputbox)
        passwordInput = findViewById(R.id.passwordinputbox)
        loginButton = findViewById(R.id.loginbutton)
        createButton = findViewById(R.id.createnewaccountbutton)

        loginButton.setOnClickListener { submit(register = false) }
        createButton.setOnClickListener { submit(register = true) }
    }

    private fun submit(register: Boolean) {
        val username = usernameInput.text.toString().trim()
        val password = passwordInput.text.toString()   // never trimmed

        setBusy(true)
        lifecycleScope.launch {
            val outcome = withContext(Dispatchers.IO) {
                if (register) auth.register(username, password) else auth.login(username, password)
            }
            setBusy(false)

            when (outcome) {
                is Outcome.Failure -> toast(outcome.message)
                is Outcome.Success ->
                    if (register) toast("Account created. You can log in now.") else onLoginSuccess(outcome.value)
            }
        }
    }

    private fun onLoginSuccess(userId: Long) {
        passwordInput.text.clear()
        val prefs = getSharedPreferences(AppConstants.PREFS_NAME, MODE_PRIVATE)
        val smsPromptShown = prefs.getBoolean(AppConstants.PREF_SMS_PROMPT_SHOWN, false)

        // First login shows the SMS permission screen; later logins go straight to the main screen.
        val next = if (smsPromptShown) DataActivity::class.java else SmsActivity::class.java
        startActivity(Intent(this, next).putExtra(AppConstants.EXTRA_USER_ID, userId))
        finish()
    }

    private fun setBusy(busy: Boolean) {
        loginButton.isEnabled = !busy
        createButton.isEnabled = !busy
    }

    private fun toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
}