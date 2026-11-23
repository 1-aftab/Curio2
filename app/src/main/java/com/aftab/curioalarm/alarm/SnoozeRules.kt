package com.aftab.curioalarm.alarm

/** Pure snooze decisions. Kiosk mode never snoozes, and a zero length means the option is off. */
object SnoozeRules {
    val MINUTES = intArrayOf(0, 5, 9, 15)
    val LIMITS = intArrayOf(1, 2, 3)

    fun sanitizeMinutes(minutes: Int): Int = if (minutes in MINUTES) minutes else 0

    fun sanitizeLimit(limit: Int): Int = when {
        limit < 1 -> 1
        limit > 3 -> 3
        else -> limit
    }

    fun left(limit: Int, used: Int): Int = (sanitizeLimit(limit) - used.coerceAtLeast(0)).coerceAtLeast(0)

    fun canSnooze(minutes: Int, limit: Int, used: Int, hardMode: Boolean): Boolean =
        sanitizeMinutes(minutes) > 0 && !hardMode && left(limit, used) > 0

    fun label(minutes: Int): String = if (minutes <= 0) "Off" else "$minutes min"
}
