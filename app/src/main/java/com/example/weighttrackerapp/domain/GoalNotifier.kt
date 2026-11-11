package com.example.weighttrackerapp.domain

// Tells the user their goal was reached
interface GoalNotifier {
    // Returns true if the notification was handed off successfully
    fun notifyGoalReached(phoneNumber: String, goalWeight: Double): Boolean
}