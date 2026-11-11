package com.example.weighttrackerapp.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class InputValidatorTest {
    private val today = LocalDate.of(2026, 11, 8)

    @Test
    fun weightAcceptsNumbersAndRoundsToOneDecimal() {
        assertEquals(150.0, (InputValidator.parseWeight("150") as Outcome.Success).value, 0.0)
        assertEquals(150.3, (InputValidator.parseWeight(" 150.26 ") as Outcome.Success).value, 0.0)
    }

    @Test
    fun weightRejectsBadInput() {
        listOf("", "abc", "NaN", "Infinity", "-5", "0", "19.9", "1000.1", "12,5").forEach {
            assertTrue("'$it' should be rejected", InputValidator.parseWeight(it) is Outcome.Failure)
        }
    }

    @Test
    fun dateMustBeRealIsoAndNotInTheFuture() {
        assertEquals(today, (InputValidator.parseDate("2026-11-08", today) as Outcome.Success).value)
        listOf("11/08/2026", "2026-13-01", "2026-02-30", "today", "").forEach {
            assertTrue("'$it' should be rejected", InputValidator.parseDate(it, today) is Outcome.Failure)
        }
        assertTrue(InputValidator.parseDate("2026-11-09", today) is Outcome.Failure)
    }

    @Test
    fun usernameRules() {
        assertNull(InputValidator.validateUsername("tyler_99"))
        assertNotNull(InputValidator.validateUsername("ab"))
        assertNotNull(InputValidator.validateUsername("has space"))
        assertNotNull(InputValidator.validateUsername("a".repeat(33)))
    }

    @Test
    fun passwordRules() {
        assertNull(InputValidator.validatePassword("12345678"))
        assertNotNull(InputValidator.validatePassword("short"))
        assertNotNull(InputValidator.validatePassword("a".repeat(129)))
    }

    @Test
    fun phoneIsOptionalAndNormalized() {
        assertNull((InputValidator.parsePhone("  ") as Outcome.Success).value)
        assertEquals("+15025550123", (InputValidator.parsePhone("+1 (502) 555-0123") as Outcome.Success).value)
        assertTrue(InputValidator.parsePhone("12345") is Outcome.Failure)
        assertTrue(InputValidator.parsePhone("call me") is Outcome.Failure)
    }
}