package com.aftab.curioalarm.data

enum class ChallengeType(val title: String, val blurb: String) {
    MATH("Math", "Solve an arithmetic problem"),
    MEMORY("Memory", "Reproduce a sequence"),
    PHRASE("Phrase", "Type a phrase exactly")
}

enum class Difficulty(val title: String) {
    EASY("Easy"),
    MEDIUM("Medium"),
    HARD("Hard")
}

/**
 * @param days java.time.DayOfWeek values (1 = Monday ... 7 = Sunday). Empty = one-time alarm.
 * @param scheduledAt epoch millis of the next trigger registered with AlarmManager (0 = none).
 * @param lastFiredAt epoch millis of the last trigger that rang; used to block duplicate triggers.
 */
data class Alarm(
    val id: Int = 0,
    val hour: Int = 7,
    val minute: Int = 0,
    val label: String = "",
    val enabled: Boolean = true,
    val days: Set<Int> = emptySet(),
    val soundPath: String? = null,
    val soundName: String? = null,
    val challenges: Set<ChallengeType> = setOf(ChallengeType.MATH),
    val difficulty: Difficulty = Difficulty.MEDIUM,
    val vibrate: Boolean = true,
    val gradualVolume: Boolean = true,
    val scheduledAt: Long = 0L,
    val lastFiredAt: Long = 0L,
    val hardMode: Boolean = false
) {
    val isRepeating: Boolean get() = days.isNotEmpty()
}
