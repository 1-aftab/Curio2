package com.aftab.curioalarm.data

import java.time.LocalDate

/**
 * Consecutive local days on which a challenge was solved. Solving twice on the same day
 * does not increase the count. A missed day resets it to 1 on the next solve.
 */
object WakeStreak {
    fun apply(last: LocalDate?, streak: Int, today: LocalDate): Pair<LocalDate, Int> {
        if (last == today) return today to streak.coerceAtLeast(1)
        val next = if (last != null && last.plusDays(1) == today) streak + 1 else 1
        return today to next.coerceAtLeast(1)
    }
}
