package com.example.weighttrackerapp.domain

import com.example.weighttrackerapp.data.Goal
import com.example.weighttrackerapp.data.WeightEntry
import com.example.weighttrackerapp.data.WeightRepository
import java.time.LocalDate

// Weight tracking rules: validation, per-user access, and the goal-reached check
class WeightService(
    private val repository: WeightRepository,
    private val notifier: GoalNotifier,
    private val today: () -> LocalDate = { LocalDate.now() }
) {
    // goalReached is true only when this entry is the one that newly reached the goal
    data class AddResult(val goalReached: Boolean, val notificationSent: Boolean)

    fun listEntries(userId: Long): List<WeightEntry> = repository.getWeights(userId)

    fun getGoal(userId: Long): Goal? = repository.getGoal(userId)

    fun addEntry(userId: Long, dateText: String, weightText: String): Outcome<AddResult> {
        val date = when (val r = InputValidator.parseDate(dateText, today())) {
            is Outcome.Success -> r.value
            is Outcome.Failure -> return r
        }
        val weight = when (val r = InputValidator.parseWeight(weightText)) {
            is Outcome.Success -> r.value
            is Outcome.Failure -> return r
        }


        if (!repository.addWeight(userId, date.toString(), weight)) {
            return Outcome.Failure("Could not save the entry")
        }

        val goal = repository.getGoal(userId) ?: return Outcome.Success(AddResult(false, false))
        // this entry reached the goal if it is the newest one and the goal was crossed on its date
        val points = WeightAnalytics.clean(repository.getWeights(userId)).points
        val reachedNow = points.lastOrNull()?.date == date &&
                WeightAnalytics.findCrossings(points, goal.weight).any { it.date == date }
        if (!reachedNow) return Outcome.Success(AddResult(false, false))

        val phone = goal.phoneNumber
        val sent = phone != null && notifier.notifyGoalReached(phone, goal.weight)
        return Outcome.Success(AddResult(goalReached = true, notificationSent = sent))
    }

    fun updateEntry(userId: Long, entryId: Long, dateText: String, weightText: String): Outcome<Unit> {
        val date = when (val r = InputValidator.parseDate(dateText, today())) {
            is Outcome.Success -> r.value
            is Outcome.Failure -> return r
        }
        val weight = when (val r = InputValidator.parseWeight(weightText)) {
            is Outcome.Success -> r.value
            is Outcome.Failure -> return r
        }
        return if (repository.updateWeight(userId, entryId, date.toString(), weight)) {
            Outcome.Success(Unit)
        } else {
            Outcome.Failure("Entry not found")
        }
    }

    fun deleteEntry(userId: Long, entryId: Long): Outcome<Unit> =
        if (repository.deleteWeight(userId, entryId)) Outcome.Success(Unit) else Outcome.Failure("Entry not found")

    fun saveGoal(userId: Long, goalText: String, phoneText: String): Outcome<Goal> {
        val weight = when (val r = InputValidator.parseWeight(goalText)) {
            is Outcome.Success -> r.value
            is Outcome.Failure -> return r
        }
        val phone = when (val r = InputValidator.parsePhone(phoneText)) {
            is Outcome.Success -> r.value
            is Outcome.Failure -> return r
        }
        val goal = Goal(weight, phone)
        repository.saveGoal(userId, goal)
        return Outcome.Success(goal)
    }
}