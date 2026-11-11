package com.example.weighttrackerapp.data

// The way the rest of the app reads or writes stored data.

interface WeightRepository {
    // Returns the new user's id, or null if the username is already taken
    fun createUser(username: String, passwordHash: String): Long?
    fun findUser(username: String): UserRecord?
    fun updatePasswordHash(userId: Long, passwordHash: String)

    fun addWeight(userId: Long, date: String, weight: Double): Boolean
    // Entries for userId
    fun getWeights(userId: Long): List<WeightEntry>
    fun updateWeight(userId: Long, entryId: Long, date: String, weight: Double): Boolean
    fun deleteWeight(userId: Long, entryId: Long): Boolean

    fun getGoal(userId: Long): Goal?
    fun saveGoal(userId: Long, goal: Goal)
}