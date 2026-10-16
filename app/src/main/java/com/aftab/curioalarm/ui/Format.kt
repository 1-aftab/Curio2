package com.aftab.curioalarm.ui

import com.aftab.curioalarm.data.Alarm
import java.time.DayOfWeek
import java.time.format.TextStyle as JTextStyle
import java.util.Locale

object Fmt {
    /** Returns ("7:00", "AM") or ("07:00", "") for 24-hour clocks. */
    fun time(hour: Int, minute: Int, is24: Boolean): Pair<String, String> {
        val mm = minute.toString().padStart(2, '0')
        return if (is24) {
            Pair(hour.toString().padStart(2, '0') + ":" + mm, "")
        } else {
            val h12 = if (hour % 12 == 0) 12 else hour % 12
            Pair("$h12:$mm", if (hour < 12) "AM" else "PM")
        }
    }

    fun days(days: Set<Int>): String = when {
        days.isEmpty() -> "Once"
        days.size == 7 -> "Every day"
        days == setOf(1, 2, 3, 4, 5) -> "Weekdays"
        days == setOf(6, 7) -> "Weekends"
        else -> days.sorted().joinToString(", ") {
            DayOfWeek.of(it).getDisplayName(JTextStyle.SHORT, Locale.getDefault())
        }
    }

    fun challengeSummary(alarm: Alarm): String {
        val names = alarm.challenges.sortedBy { it.ordinal }.joinToString(" · ") { it.title }
        return if (alarm.challenges.size > 1) "Random: $names" else names
    }
}
