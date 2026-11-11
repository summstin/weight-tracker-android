package com.example.weighttrackerapp.domain

import com.example.weighttrackerapp.data.Goal
import com.example.weighttrackerapp.data.UserRecord
import com.example.weighttrackerapp.data.WeightEntry
import com.example.weighttrackerapp.data.WeightRepository

// In-memory repository so the service logic can be tested without a database or an emulator
class FakeWeightRepository : WeightRepository {
    private data class Row(val userId: Long, val entry: WeightEntry)

    private val users = mutableListOf<UserRecord>()
    private val rows = mutableListOf<Row>()
    private val goals = mutableMapOf<Long, Goal>()
    private var nextId = 1L

    override fun createUser(username: String, passwordHash: String): Long? {
        if (users.any { it.username.equals(username, ignoreCase = true) }) return null
        val id = nextId++
        users.add(UserRecord(id, username, passwordHash))
        return id
    }

    override fun findUser(username: String): UserRecord? =
        users.firstOrNull { it.username.equals(username, ignoreCase = true) }

    override fun updatePasswordHash(userId: Long, passwordHash: String) {
        val index = users.indexOfFirst { it.id == userId }
        if (index >= 0) users[index] = users[index].copy(passwordHash = passwordHash)
    }

    override fun addWeight(userId: Long, date: String, weight: Double): Boolean {
        rows.add(Row(userId, WeightEntry(nextId++, date, weight)))
        return true
    }

    override fun getWeights(userId: Long): List<WeightEntry> =
        rows.filter { it.userId == userId }
            .map { it.entry }
            .sortedWith(compareByDescending<WeightEntry> { it.date }.thenByDescending { it.id })

    override fun updateWeight(userId: Long, entryId: Long, date: String, weight: Double): Boolean {
        val index = rows.indexOfFirst { it.userId == userId && it.entry.id == entryId }
        if (index < 0) return false
        rows[index] = rows[index].copy(entry = WeightEntry(entryId, date, weight))
        return true
    }

    override fun deleteWeight(userId: Long, entryId: Long): Boolean =
        rows.removeAll { it.userId == userId && it.entry.id == entryId }

    override fun getGoal(userId: Long): Goal? = goals[userId]

    override fun saveGoal(userId: Long, goal: Goal) {
        goals[userId] = goal
    }
}

// Records goal notifications instead of sending text messages
class FakeGoalNotifier(var succeeds: Boolean = true) : GoalNotifier {
    val calls = mutableListOf<Pair<String, Double>>()

    override fun notifyGoalReached(phoneNumber: String, goalWeight: Double): Boolean {
        calls.add(phoneNumber to goalWeight)
        return succeeds
    }
}