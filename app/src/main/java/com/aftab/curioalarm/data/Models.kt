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
 * How a ringing alarm makes itself heard.
 * Older saves only stored a vibrate on/off flag and always played sound — see [fromStored].
 */
enum class AlertMode(val title: String, val blurb: String) {
    SOUND_AND_VIBRATION("Both", "Plays the tone and vibrates"),
    SOUND("Sound", "Plays the tone. No vibration"),
    VIBRATION("Vibrate", "Silent — vibration only, no tone");

    val playsSound: Boolean get() = this != VIBRATION
    val vibrates: Boolean get() = this != SOUND

    companion object {
        fun parse(name: String?): AlertMode =
            entries.firstOrNull { it.name == name } ?: SOUND_AND_VIBRATION

        /** [hasAlertMode] is false for alarms saved before this choice existed. */
        fun fromStored(alertModeName: String?, hasAlertMode: Boolean, legacyVibrate: Boolean): AlertMode =
            if (hasAlertMode) parse(alertModeName)
            else if (legacyVibrate) SOUND_AND_VIBRATION else SOUND
    }
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
    val alertMode: AlertMode = AlertMode.SOUND_AND_VIBRATION,
    val gradualVolume: Boolean = true,
    val scheduledAt: Long = 0L,
    val lastFiredAt: Long = 0L,
    val hardMode: Boolean = false
) {
    val isRepeating: Boolean get() = days.isNotEmpty()
}
