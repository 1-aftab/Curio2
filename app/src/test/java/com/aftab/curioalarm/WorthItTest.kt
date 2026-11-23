package com.aftab.curioalarm

import com.aftab.curioalarm.alarm.SnoozeRules
import com.aftab.curioalarm.data.WakeStreak
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WakeStreakTest {
    private val today = LocalDate.of(2026, 11, 22)

    @Test
    fun firstSolveStartsAtOne() {
        assertEquals(today to 1, WakeStreak.apply(null, 0, today))
    }

    @Test
    fun sameDayDoesNotGrow() {
        assertEquals(today to 3, WakeStreak.apply(today, 3, today))
    }

    @Test
    fun nextDayGrows() {
        assertEquals(today to 4, WakeStreak.apply(today.minusDays(1), 3, today))
    }

    @Test
    fun aGapResets() {
        assertEquals(today to 1, WakeStreak.apply(today.minusDays(2), 9, today))
    }
}

class SnoozeRulesTest {
    @Test
    fun offAndKioskNeverSnooze() {
        assertFalse(SnoozeRules.canSnooze(0, 1, 0, hardMode = false))
        assertFalse(SnoozeRules.canSnooze(9, 1, 0, hardMode = true))
    }

    @Test
    fun limitIsHonored() {
        assertTrue(SnoozeRules.canSnooze(9, 2, 1, hardMode = false))
        assertFalse(SnoozeRules.canSnooze(9, 2, 2, hardMode = false))
        assertEquals(1, SnoozeRules.left(2, 1))
        assertEquals(0, SnoozeRules.left(1, 4))
    }

    @Test
    fun unknownLengthsAreOff() {
        assertEquals(0, SnoozeRules.sanitizeMinutes(7))
        assertEquals(9, SnoozeRules.sanitizeMinutes(9))
        assertEquals(1, SnoozeRules.sanitizeLimit(0))
        assertEquals(3, SnoozeRules.sanitizeLimit(9))
    }
}
