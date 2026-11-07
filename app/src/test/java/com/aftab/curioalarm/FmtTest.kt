package com.aftab.curioalarm

import com.aftab.curioalarm.data.Alarm
import com.aftab.curioalarm.data.ChallengeType
import com.aftab.curioalarm.ui.Fmt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FmtTest {

    @Test
    fun time12h() {
        assertEquals(Pair("7:05", "AM"), Fmt.time(7, 5, is24 = false))
        assertEquals(Pair("12:00", "AM"), Fmt.time(0, 0, is24 = false))
        assertEquals(Pair("12:30", "PM"), Fmt.time(12, 30, is24 = false))
        assertEquals(Pair("11:59", "PM"), Fmt.time(23, 59, is24 = false))
    }

    @Test
    fun time24h() {
        assertEquals(Pair("07:05", ""), Fmt.time(7, 5, is24 = true))
        assertEquals(Pair("00:00", ""), Fmt.time(0, 0, is24 = true))
        assertEquals(Pair("23:59", ""), Fmt.time(23, 59, is24 = true))
    }

    @Test
    fun dayNames() {
        assertEquals("Once", Fmt.days(emptySet()))
        assertEquals("Every day", Fmt.days(setOf(1, 2, 3, 4, 5, 6, 7)))
        assertEquals("Weekdays", Fmt.days(setOf(1, 2, 3, 4, 5)))
        assertEquals("Weekends", Fmt.days(setOf(6, 7)))
    }

    @Test
    fun untilTextRoundsUpAndNeverShowsZero() {
        // Regression: the old header showed "0 min" / off-by-one countdowns.
        assertEquals("under a minute", Fmt.untilText(0))
        assertEquals("1 min", Fmt.untilText(1_000))
        assertEquals("1 min", Fmt.untilText(59_999))
        assertEquals("1 min", Fmt.untilText(60_000))
        assertEquals("2 min", Fmt.untilText(60_001))
        assertEquals("1 h 0 min", Fmt.untilText(60 * 60_000))
        assertEquals("1 h 1 min", Fmt.untilText(61 * 60_000))
        assertEquals("1 d 0 h", Fmt.untilText(24 * 60 * 60_000))
        assertEquals("2 d 1 h", Fmt.untilText(49 * 60 * 60_000))
    }

    @Test
    fun untilTextClampsNegative() {
        assertEquals("under a minute", Fmt.untilText(-5_000))
    }

    @Test
    fun challengeShortLabels() {
        val single = Alarm(challenges = setOf(ChallengeType.MATH))
        assertEquals("Math", Fmt.challengeShort(single))
        val multi = Alarm(challenges = setOf(ChallengeType.MATH, ChallengeType.MEMORY))
        assertEquals("Random · 2 tasks", Fmt.challengeShort(multi))
    }
}

class ChallengeEngineTest {

    @Test
    fun normalizeCollapsesWhitespaceAndCase() {
        assertEquals(
            "the quiet morning light",
            com.aftab.curioalarm.challenge.ChallengeEngine.normalize("  The  QUIET\tmorning  light \n")
        )
    }

    @Test
    fun pickNeverReturnsOutsideTheRequestedSet() {
        val types = setOf(ChallengeType.PHRASE)
        repeat(50) {
            assertEquals(ChallengeType.PHRASE, com.aftab.curioalarm.challenge.ChallengeEngine.pick(types))
        }
    }

    @Test
    fun pickFallsBackToMathOnEmptySet() {
        repeat(20) {
            assertTrue(
                com.aftab.curioalarm.challenge.ChallengeEngine.pick(emptySet()) == ChallengeType.MATH
            )
        }
    }
}
