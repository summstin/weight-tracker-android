package com.example.weighttrackerapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

// One-time screen that asks for SMS permission, then continues to the main screen
class SmsActivity : AppCompatActivity() {

    private var userId = AppConstants.NO_USER

    // Result API replaces the deprecated requestPermissions / onRequestPermissionsResult pair
    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            onPermissionHandled(granted)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sms)

        userId = intent.getLongExtra(AppConstants.EXTRA_USER_ID, AppConstants.NO_USER)
        if (userId == AppConstants.NO_USER) {
            // Opened without a logged-in user: send them back to log in.
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        findViewById<Button>(R.id.allowsmsbutton).setOnClickListener { checkPermission() }
    }

    private fun checkPermission() {
        val granted = ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) ==
                PackageManager.PERMISSION_GRANTED
        if (granted) onPermissionHandled(true) else permissionLauncher.launch(Manifest.permission.SEND_SMS)
    }

    // Saves that the prompt was shown and moves on to the main app.
    private fun onPermissionHandled(granted: Boolean) {
        getSharedPreferences(AppConstants.PREFS_NAME, MODE_PRIVATE)
            .edit()
            .putBoolean(AppConstants.PREF_SMS_PROMPT_SHOWN, true)
            .apply()

        Toast.makeText(
            this,
            if (granted) "SMS Permission Granted" else "SMS Permission Denied",
            Toast.LENGTH_SHORT
        ).show()

        startActivity(Intent(this, DataActivity::class.java).putExtra(AppConstants.EXTRA_USER_ID, userId))
        finish()
    }
}