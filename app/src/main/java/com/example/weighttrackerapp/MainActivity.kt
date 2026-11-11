package com.example.weighttrackerapp

import android.os.Bundle
import android.content.Intent
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var db: DatabaseHelper
    private lateinit var usernameInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var loginButton: Button
    private lateinit var createButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // uses login layout
        setContentView(R.layout.activity_login)

        // initialize database
        db = DatabaseHelper(this)

        // link UI elements
        usernameInput = findViewById(R.id.usernameinputbox)
        passwordInput = findViewById(R.id.passwordinputbox)
        loginButton = findViewById(R.id.loginbutton)
        createButton = findViewById(R.id.createnewaccountbutton)

        // login button
        loginButton.setOnClickListener {

            val username = usernameInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()

            // validate input
            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Enter username and password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // check database
            if (db.checkUser(username, password)) {
                Toast.makeText(this, "Login Successful", Toast.LENGTH_SHORT).show()

                // check if sms prompt has been shown before
                val prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE)
                val smsPromptShown = prefs.getBoolean("sms_prompt_shown", false)

                if (!smsPromptShown) {
                    // first login shows sms permission screen
                    startActivity(Intent(this, SmsActivity::class.java))
                } else {
                    // subsequent logins go to main app
                    startActivity(Intent(this, DataActivity::class.java))
                }
                // prevents going back to login screen
                finish()

            } else {
                Toast.makeText(this, "Invalid username or password", Toast.LENGTH_SHORT).show()
            }
        }

        // create account button
        createButton.setOnClickListener {

            val username = usernameInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()

            // Validate input
            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Enter username and password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // try to create account
            val success = db.registerUser(username, password)

            if (success) {
                Toast.makeText(this, "Account created successfully", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "User already exists", Toast.LENGTH_SHORT).show()
            }
        }
    }
}