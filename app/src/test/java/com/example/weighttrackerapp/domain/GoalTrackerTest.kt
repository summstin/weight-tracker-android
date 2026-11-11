package com.example.weighttrackerapp.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GoalTrackerTest {
    @Test
    fun losingWeightGoalIsReachedAtOrBelowGoal() {
        assertTrue(GoalTracker.isGoalReached(150.0, 150.0, 180.0))
        assertTrue(GoalTracker.isGoalReached(149.0, 150.0, 180.0))
        assertFalse(GoalTracker.isGoalReached(151.0, 150.0, 180.0))
    }

    @Test
    fun gainingWeightGoalIsReachedAtOrAboveGoal() {
        assertTrue(GoalTracker.isGoalReached(151.0, 150.0, 120.0))
        assertFalse(GoalTracker.isGoalReached(149.0, 150.0, 120.0))
    }

    @Test
    fun withoutStartWeightOnlyAnExactMatchCounts() {
        assertTrue(GoalTracker.isGoalReached(150.04, 150.0, null))
        assertFalse(GoalTracker.isGoalReached(149.0, 150.0, null))
    }
}