package com.aftab.curioalarm.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

/** Local persistence: all alarms live as one JSON array in SharedPreferences. */
class AlarmRepository(context: Context) {

    private val appContext = context.applicationContext
    private val prefs: SharedPreferences =
        appContext.getSharedPreferences("curio_alarms", Context.MODE_PRIVATE)
    private val lock = Any()
    private val _alarms = MutableStateFlow<List<Alarm>>(emptyList())
    val alarms: StateFlow<List<Alarm>> = _alarms.asStateFlow()

    init {
        synchronized(lock) {
            if (!prefs.getBoolean(KEY_SEEDED, false)) {
                val seeded = presets().map { it.copy(id = takeId()) }
                write(seeded)
                prefs.edit().putBoolean(KEY_SEEDED, true).commit()
            } else {
                _alarms.value = readAll()
            }
        }
    }

    fun snapshot(): List<Alarm> = _alarms.value

    fun get(id: Int): Alarm? = _alarms.value.firstOrNull { it.id == id }

    fun upsert(alarm: Alarm): Alarm = synchronized(lock) {
        val stored = if (alarm.id == 0) alarm.copy(id = takeId()) else alarm
        val list = _alarms.value.toMutableList()
        val index = list.indexOfFirst { it.id == stored.id }
        if (index >= 0) list[index] = stored else list.add(stored)
        write(sorted(list))
        stored
    }

    fun delete(id: Int) = synchronized(lock) {
        write(_alarms.value.filterNot { it.id == id })
    }

    fun addPresets() = synchronized(lock) {
        val list = _alarms.value.toMutableList()
        presets().forEach { list.add(it.copy(id = takeId())) }
        write(sorted(list))
    }

    /** Deletes imported sound files no alarm references any more. */
    fun cleanupSounds() {
        val keep = _alarms.value.mapNotNull { it.soundPath }.toSet()
        SoundStore.cleanup(appContext, keep)
    }

    // ---- internals ----

    private fun sorted(list: List<Alarm>) =
        list.sortedWith(compareBy<Alarm>({ it.hour }, { it.minute }, { it.id }))

    private fun takeId(): Int {
        val id = prefs.getInt(KEY_NEXT_ID, 1)
        prefs.edit().putInt(KEY_NEXT_ID, id + 1).commit()
        return id
    }

    private fun write(list: List<Alarm>) {
        val array = JSONArray()
        list.forEach { array.put(it.toJson()) }
        prefs.edit().putString(KEY_ALARMS, array.toString()).commit()
        _alarms.value = list
    }

    private fun readAll(): List<Alarm> {
        val raw = prefs.getString(KEY_ALARMS, null) ?: return emptyList()
        val out = mutableListOf<Alarm>()
        try {
            val array = JSONArray(raw)
            for (i in 0 until array.length()) {
                try {
                    out.add(array.getJSONObject(i).toAlarm())
                } catch (_: Exception) {
                    // skip a corrupt entry rather than losing every alarm
                }
            }
        } catch (_: Exception) {
        }
        return sorted(out)
    }

    private fun presets(): List<Alarm> {
        val everyDay = setOf(1, 2, 3, 4, 5, 6, 7)
        return listOf(
            Alarm(
                hour = 7, minute = 0, label = "Morning", enabled = false,
                days = setOf(1, 2, 3, 4, 5),
                challenges = setOf(ChallengeType.MATH), difficulty = Difficulty.MEDIUM
            ),
            Alarm(
                hour = 13, minute = 0, label = "Afternoon", enabled = false,
                days = everyDay,
                challenges = setOf(ChallengeType.MEMORY), difficulty = Difficulty.MEDIUM
            ),
            Alarm(
                hour = 21, minute = 0, label = "Night", enabled = false,
                days = everyDay,
                challenges = setOf(ChallengeType.PHRASE), difficulty = Difficulty.EASY
            )
        )
    }

    private fun Alarm.toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("hour", hour)
        put("minute", minute)
        put("label", label)
        put("enabled", enabled)
        put("days", JSONArray(days.sorted()))
        put("soundPath", soundPath ?: "")
        put("soundName", soundName ?: "")
        put("challenges", JSONArray(challenges.map { it.name }))
        put("difficulty", difficulty.name)
        put("vibrate", vibrate)
        put("gradualVolume", gradualVolume)
        put("scheduledAt", scheduledAt)
        put("lastFiredAt", lastFiredAt)
        put("hardMode", hardMode)
    }

    private fun JSONObject.toAlarm(): Alarm {
        val dayArray = optJSONArray("days") ?: JSONArray()
        val days = (0 until dayArray.length()).map { dayArray.getInt(it) }.filter { it in 1..7 }.toSet()
        val chArray = optJSONArray("challenges") ?: JSONArray()
        val challenges = (0 until chArray.length()).mapNotNull {
            runCatching { ChallengeType.valueOf(chArray.getString(it)) }.getOrNull()
        }.toSet().ifEmpty { setOf(ChallengeType.MATH) }
        val id = getInt("id")
        val hour = getInt("hour")
        val minute = getInt("minute")
        // Reject corrupt rows here; readAll() skips thrown entries instead of crashing
        // later inside LocalTime.of() when scheduling or formatting.
        require(id > 0 && hour in 0..23 && minute in 0..59) { "corrupt alarm row" }
        return Alarm(
            id = id,
            hour = hour,
            minute = minute,
            label = optString("label", ""),
            enabled = optBoolean("enabled", false),
            days = days,
            soundPath = optString("soundPath", "").ifBlank { null },
            soundName = optString("soundName", "").ifBlank { null },
            challenges = challenges,
            difficulty = runCatching { Difficulty.valueOf(optString("difficulty", "MEDIUM")) }
                .getOrDefault(Difficulty.MEDIUM),
            vibrate = optBoolean("vibrate", true),
            gradualVolume = optBoolean("gradualVolume", true),
            scheduledAt = optLong("scheduledAt", 0L),
            lastFiredAt = optLong("lastFiredAt", 0L),
            hardMode = optBoolean("hardMode", false)
        )
    }

    private companion object {
        const val KEY_ALARMS = "alarms_json"
        const val KEY_NEXT_ID = "next_id"
        const val KEY_SEEDED = "seeded"
    }
}
