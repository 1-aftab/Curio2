package com.aftab.curioalarm.data

import android.content.Context
import java.time.LocalDate

/** Persists [WakeStreak] in the same prefs file as the rest of the UI. */
object WakeStreakStore {
    private const val PREFS = "curio_ui"
    private const val KEY_DAY = "streak_day"
    private const val KEY_COUNT = "streak_count"

    fun current(context: Context): Int {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val day = p.getLong(KEY_DAY, Long.MIN_VALUE)
        if (day == Long.MIN_VALUE) return 0
        val last = LocalDate.ofEpochDay(day)
        val today = LocalDate.now()
        // Still "in a row" through today or yesterday; otherwise the display would lie.
        return if (last == today || last.plusDays(1) == today) p.getInt(KEY_COUNT, 0) else 0
    }

    fun recordToday(context: Context) {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val today = LocalDate.now()
        val last = p.getLong(KEY_DAY, Long.MIN_VALUE).takeIf { it != Long.MIN_VALUE }?.let(LocalDate::ofEpochDay)
        val (day, count) = WakeStreak.apply(last, p.getInt(KEY_COUNT, 0), today)
        p.edit().putLong(KEY_DAY, day.toEpochDay()).putInt(KEY_COUNT, count).commit()
    }
}
