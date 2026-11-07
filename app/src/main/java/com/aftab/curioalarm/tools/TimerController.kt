package com.aftab.curioalarm.tools

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import com.aftab.curioalarm.alarm.Notifier
import com.aftab.curioalarm.alarm.TimerReceiver
import com.aftab.curioalarm.data.Alarm
import com.aftab.curioalarm.ui.MainActivity
import java.time.LocalTime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject

/**
 * Countdown timer. The finish moment is registered with AlarmManager (exact, Doze-proof),
 * so the timer rings even when the app is closed or the phone is locked — the ringing itself
 * is handled by the existing AlarmService pipeline using the reserved id [TIMER_ID].
 * The displayed countdown uses the monotonic clock; wall-clock changes and reboots are
 * healed by [restore] (called from BootReceiver).
 */
class TimerController(private val context: Context) {

    enum class Mode { IDLE, RUNNING, PAUSED, RINGING }

    data class State(
        val mode: Mode = Mode.IDLE,
        val endElapsed: Long = 0L,        // elapsedRealtime when the timer ends
        val endWall: Long = 0L,           // wall clock when the timer ends (for AlarmManager/reboot)
        val savedAtElapsed: Long = 0L,    // markers to detect a reboot after process death
        val savedAtWall: Long = 0L,
        val totalMillis: Long = 0L,
        val remainingMillis: Long = 0L,   // valid while PAUSED
        val label: String = "",
        val lastTotalMillis: Long = 0L    // for the "repeat last" shortcut
    )

    private val prefs = context.getSharedPreferences("curio_timer", Context.MODE_PRIVATE)
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val _state = MutableStateFlow(load())
    val state: StateFlow<State> = _state.asStateFlow()

    fun remainingNow(): Long {
        val s = _state.value
        return when (s.mode) {
            Mode.RUNNING -> (s.endElapsed - SystemClock.elapsedRealtime()).coerceAtLeast(0L)
            Mode.PAUSED -> s.remainingMillis
            else -> 0L
        }
    }

    fun start(totalMillis: Long, label: String) {
        if (totalMillis <= 0 || _state.value.mode == Mode.RINGING) return
        val nowE = SystemClock.elapsedRealtime()
        val nowW = System.currentTimeMillis()
        setState(
            State(
                mode = Mode.RUNNING,
                endElapsed = nowE + totalMillis,
                endWall = nowW + totalMillis,
                savedAtElapsed = nowE,
                savedAtWall = nowW,
                totalMillis = totalMillis,
                label = label.trim().take(40),
                lastTotalMillis = totalMillis
            )
        )
        schedule(nowW + totalMillis)
    }

    fun pause() {
        val s = _state.value
        if (s.mode != Mode.RUNNING) return
        cancelSchedule()
        setState(s.copy(mode = Mode.PAUSED, remainingMillis = remainingNow()))
    }

    fun resume() {
        val s = _state.value
        if (s.mode != Mode.PAUSED || s.remainingMillis <= 0) return
        val nowE = SystemClock.elapsedRealtime()
        val nowW = System.currentTimeMillis()
        setState(
            s.copy(
                mode = Mode.RUNNING,
                endElapsed = nowE + s.remainingMillis,
                endWall = nowW + s.remainingMillis,
                savedAtElapsed = nowE,
                savedAtWall = nowW
            )
        )
        schedule(nowW + s.remainingMillis)
    }

    /** Adds one minute to a running or paused timer. */
    fun addMinute() {
        val s = _state.value
        when (s.mode) {
            Mode.RUNNING -> {
                val nowE = SystemClock.elapsedRealtime()
                val nowW = System.currentTimeMillis()
                val remaining = (s.endElapsed - nowE).coerceAtLeast(0L) + 60_000L
                setState(
                    s.copy(
                        endElapsed = nowE + remaining,
                        endWall = nowW + remaining,
                        savedAtElapsed = nowE,
                        savedAtWall = nowW,
                        totalMillis = s.totalMillis + 60_000L
                    )
                )
                schedule(nowW + remaining)
            }
            Mode.PAUSED -> setState(
                s.copy(remainingMillis = s.remainingMillis + 60_000L, totalMillis = s.totalMillis + 60_000L)
            )
            else -> Unit
        }
    }

    fun cancel() {
        val s = _state.value
        if (s.mode == Mode.RINGING) return   // ringing is stopped via AlarmService.dismiss
        cancelSchedule()
        setState(State(lastTotalMillis = maxOf(s.lastTotalMillis, s.totalMillis)))
    }

    /**
     * Called by [TimerReceiver] when the scheduled moment arrives. Returns the synthetic
     * alarm to ring, or null for a stale broadcast (timer was cancelled/paused meanwhile).
     */
    @Synchronized
    fun onFired(): Alarm? {
        val s = _state.value
        if (s.mode != Mode.RUNNING && s.mode != Mode.RINGING) return null
        if (s.mode == Mode.RUNNING) setState(s.copy(mode = Mode.RINGING))
        return ringingAlarm()
    }

    /** The synthetic alarm the AlarmService/AlarmActivity ring for [TIMER_ID], or null. */
    fun ringingAlarm(): Alarm? {
        val s = _state.value
        if (s.mode != Mode.RINGING) return null
        val now = LocalTime.now()
        return Alarm(
            id = TIMER_ID,
            hour = now.hour,
            minute = now.minute,
            label = s.label.ifBlank { "Timer" },
            challenges = emptySet(),   // no challenge: the ring screen shows a Dismiss button
            vibrate = true,
            gradualVolume = false,
            hardMode = false
        )
    }

    /** Called when the timer's ringing segment is dismissed. */
    @Synchronized
    fun onRingDismissed() {
        val s = _state.value
        if (s.mode == Mode.RINGING) {
            setState(State(lastTotalMillis = maxOf(s.lastTotalMillis, s.totalMillis)))
        }
    }

    /**
     * Re-registers or settles the timer after reboot, app update, or a wall-clock change.
     * Uses the monotonic markers to detect reboots (elapsedRealtime restarts at 0).
     */
    @Synchronized
    fun restore() {
        val s = _state.value
        if (s.mode != Mode.RUNNING) return
        val nowE = SystemClock.elapsedRealtime()
        val nowW = System.currentTimeMillis()
        val remaining = if (nowE >= s.savedAtElapsed) s.endElapsed - nowE else s.endWall - nowW
        if (remaining <= 0) {
            cancelSchedule()
            Notifier.showMissedTimer(context, s.label)
            setState(State(lastTotalMillis = maxOf(s.lastTotalMillis, s.totalMillis)))
        } else {
            setState(s.copy(endElapsed = nowE + remaining, endWall = nowW + remaining, savedAtElapsed = nowE, savedAtWall = nowW))
            schedule(nowW + remaining)
        }
    }

    // ---- scheduling ----

    private fun schedule(wallAt: Long) {
        val fire = PendingIntent.getBroadcast(
            context, TIMER_REQUEST,
            Intent(context, TimerReceiver::class.java).setAction(ACTION_FIRE),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val show = PendingIntent.getActivity(
            context, TIMER_REQUEST + 1, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        try {
            alarmManager.setAlarmClock(AlarmManager.AlarmClockInfo(wallAt, show), fire)
        } catch (_: SecurityException) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, wallAt, fire)
        }
    }

    private fun cancelSchedule() {
        val existing = PendingIntent.getBroadcast(
            context, TIMER_REQUEST,
            Intent(context, TimerReceiver::class.java).setAction(ACTION_FIRE),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (existing != null) {
            alarmManager.cancel(existing)
            existing.cancel()
        }
    }

    // ---- persistence ----

    private fun setState(s: State) {
        _state.value = s
        prefs.edit().putString(KEY, JSONObject().apply {
            put("mode", s.mode.name)
            put("endElapsed", s.endElapsed)
            put("endWall", s.endWall)
            put("savedAtElapsed", s.savedAtElapsed)
            put("savedAtWall", s.savedAtWall)
            put("totalMillis", s.totalMillis)
            put("remainingMillis", s.remainingMillis)
            put("label", s.label)
            put("lastTotalMillis", s.lastTotalMillis)
        }.toString()).apply()
    }

    private fun load(): State = runCatching {
        val o = JSONObject(prefs.getString(KEY, null) ?: return State())
        State(
            mode = runCatching { Mode.valueOf(o.optString("mode", "IDLE")) }.getOrDefault(Mode.IDLE),
            endElapsed = o.optLong("endElapsed"),
            endWall = o.optLong("endWall"),
            savedAtElapsed = o.optLong("savedAtElapsed"),
            savedAtWall = o.optLong("savedAtWall"),
            totalMillis = o.optLong("totalMillis"),
            remainingMillis = o.optLong("remainingMillis"),
            label = o.optString("label", ""),
            lastTotalMillis = o.optLong("lastTotalMillis")
        )
    }.getOrDefault(State())

    companion object {
        /** Reserved ringing id for the timer. Stored alarm ids start at 1, so 0 is safe. */
        const val TIMER_ID = 0
        const val ACTION_FIRE = "com.aftab.curioalarm.TIMER_FIRE"
        private const val TIMER_REQUEST = 77_001
        private const val KEY = "state_json"
    }
}
