package com.example.weighttrackerapp

// App-wide constants so keys and codes are defined once instead of repeated as literals.
object AppConstants {
    const val PREFS_NAME = "AppPrefs"
    const val PREF_SMS_PROMPT_SHOWN = "sms_prompt_shown"

    // Intent extra that carries the logged-in user's id between screens.
    const val EXTRA_USER_ID = "extra_user_id"
    const val NO_USER = -1L
}