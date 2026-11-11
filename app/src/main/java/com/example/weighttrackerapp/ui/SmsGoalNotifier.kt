package com.example.weighttrackerapp.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.telephony.SmsManager
import androidx.core.content.ContextCompat
import com.example.weighttrackerapp.domain.GoalNotifier
import com.example.weighttrackerapp.domain.formatWeight

// Sends the goal-reached text message. Returns false instead of crashing if it can't send
class SmsGoalNotifier(private val context: Context) : GoalNotifier {

    override fun notifyGoalReached(phoneNumber: String, goalWeight: Double): Boolean {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) ==
                PackageManager.PERMISSION_GRANTED
        if (!granted) return false

        return try {
            // SmsManager.getDefault() is deprecated; the system service is the supported way.
            val smsManager = context.getSystemService(SmsManager::class.java) ?: return false
            smsManager.sendTextMessage(
                phoneNumber,
                null,
                "Weight Tracker: you reached your goal weight of ${formatWeight(goalWeight)} lbs!",
                null,
                null
            )
            true
        } catch (e: SecurityException) {
            false
        } catch (e: IllegalArgumentException) {
            false
        }
    }
}