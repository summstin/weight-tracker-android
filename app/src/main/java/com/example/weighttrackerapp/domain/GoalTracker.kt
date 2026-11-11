package com.example.weighttrackerapp.domain

import kotlin.math.abs

// Decides whether a weight counts as having reached the goal.
object GoalTracker {
    const val TOLERANCE = 0.05

    // Reached when the weight matches the goal, or has passed it in the direction of progress
    fun isGoalReached(weight: Double, goal: Double, startWeight: Double?): Boolean {
        if (abs(weight - goal) <= TOLERANCE) return true
        if (startWeight == null) return false
        return when {
            startWeight > goal -> weight <= goal
            startWeight < goal -> weight >= goal
            else -> false
        }
    }
}