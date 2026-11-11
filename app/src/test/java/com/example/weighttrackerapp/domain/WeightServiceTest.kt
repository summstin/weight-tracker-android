package com.example.weighttrackerapp.domain

import com.example.weighttrackerapp.data.Goal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class WeightServiceTest {
    private val repo = FakeWeightRepository()
    private val notifier = FakeGoalNotifier()
    private val service = WeightService(repo, notifier, today = { LocalDate.of(2026, 11, 8) })

    private val alice = 1L
    private val bob = 2L

    @Test
    fun addedEntriesAreListedNewestFirst() {
        service.addEntry(alice, "2026-11-01", "180")
        service.addEntry(alice, "2026-11-03", "178.5")
        val entries = service.listEntries(alice)
        assertEquals(listOf("2026-11-03", "2026-11-01"), entries.map { it.date })
        assertEquals(178.5, entries.first().weight, 0.0)
    }

    @Test
    fun invalidEntriesAreRejectedAndNotSaved() {
        assertTrue(service.addEntry(alice, "2026-11-01", "abc") is Outcome.Failure)
        assertTrue(service.addEntry(alice, "11/01/2026", "180") is Outcome.Failure)
        assertTrue(service.addEntry(alice, "2026-12-25", "180") is Outcome.Failure)
        assertTrue(service.listEntries(alice).isEmpty())
    }

    @Test
    fun usersOnlySeeTheirOwnEntries() {
        service.addEntry(alice, "2026-11-01", "180")
        assertTrue(service.listEntries(bob).isEmpty())
    }

    @Test
    fun usersCannotEditOrDeleteEachOthersEntries() {
        service.addEntry(alice, "2026-11-01", "180")
        val aliceEntryId = service.listEntries(alice).single().id

        assertTrue(service.updateEntry(bob, aliceEntryId, "2026-11-02", "100") is Outcome.Failure)
        assertTrue(service.deleteEntry(bob, aliceEntryId) is Outcome.Failure)

        val untouched = service.listEntries(alice).single()
        assertEquals("2026-11-01", untouched.date)
        assertEquals(180.0, untouched.weight, 0.0)
    }

    @Test
    fun ownersCanEditAndDelete() {
        service.addEntry(alice, "2026-11-01", "180")
        val id = service.listEntries(alice).single().id

        assertTrue(service.updateEntry(alice, id, "2026-11-02", "179") is Outcome.Success)
        assertEquals(179.0, service.listEntries(alice).single().weight, 0.0)

        assertTrue(service.deleteEntry(alice, id) is Outcome.Success)
        assertTrue(service.listEntries(alice).isEmpty())
    }

    @Test
    fun goalsAreStoredPerUser() {
        service.saveGoal(alice, "150", "")
        assertEquals(Goal(150.0, null), service.getGoal(alice))
        assertEquals(null, service.getGoal(bob))
    }

    @Test
    fun goalInputIsValidated() {
        assertTrue(service.saveGoal(alice, "abc", "") is Outcome.Failure)
        assertTrue(service.saveGoal(alice, "150", "not a number") is Outcome.Failure)
        assertEquals(null, service.getGoal(alice))
    }

    @Test
    fun reachingTheGoalSendsOneTextAndNotRepeats() {
        service.saveGoal(alice, "150", "5025550123")
        service.addEntry(alice, "2026-11-01", "160")

        val reached = service.addEntry(alice, "2026-11-02", "149") as Outcome.Success
        assertTrue(reached.value.goalReached)
        assertTrue(reached.value.notificationSent)
        assertEquals(listOf("5025550123" to 150.0), notifier.calls)

        val later = service.addEntry(alice, "2026-11-03", "148") as Outcome.Success
        assertFalse(later.value.goalReached)
        assertEquals(1, notifier.calls.size)
    }

    @Test
    fun noTextIsSentWithoutAPhoneNumber() {
        service.saveGoal(alice, "150", "")
        service.addEntry(alice, "2026-11-01", "160")

        val reached = service.addEntry(alice, "2026-11-02", "150") as Outcome.Success
        assertTrue(reached.value.goalReached)
        assertFalse(reached.value.notificationSent)
        assertTrue(notifier.calls.isEmpty())
    }

    @Test
    fun failedSendIsReportedNotHidden() {
        notifier.succeeds = false
        service.saveGoal(alice, "150", "5025550123")
        service.addEntry(alice, "2026-11-01", "160")

        val reached = service.addEntry(alice, "2026-11-02", "150") as Outcome.Success
        assertTrue(reached.value.goalReached)
        assertFalse(reached.value.notificationSent)
    }

    @Test
    fun otherUsersGoalsDoNotTriggerTexts() {
        service.saveGoal(alice, "150", "5025550123")
        service.addEntry(bob, "2026-11-01", "160")
        service.addEntry(bob, "2026-11-02", "149")
        assertTrue(notifier.calls.isEmpty())
    }
}