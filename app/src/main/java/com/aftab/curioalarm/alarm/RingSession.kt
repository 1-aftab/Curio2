package com.aftab.curioalarm.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.SystemClock

/**
 * Persists "an alarm is still owed" across process death. The power button on some phones
 * kills the ringing process; this is what brings it back. Cleared only when the user
 * actually dismisses the alarm.
 */
object RingSession {
    private const val PREFS = "curio_ring"
    private const val KEY_ACTIVE = "active"
    private const val KEY_ID = "alarm_id"
    private const val REQUEST = 91_001
    private const val DEFAULT_DELAY_MS = 5_000L

    fun mark(context: Context, alarmId: Int) {
        prefs(context).edit()
            .putBoolean(KEY_ACTIVE, true)
            .putInt(KEY_ID, alarmId)
            .commit()
        arm(context)
    }

    fun clear(context: Context) {
        prefs(context).edit()
            .putBoolean(KEY_ACTIVE, false)
            .remove(KEY_ID)
            .commit()
        disarm(context)
    }

    /** Id still owed, including the reserved timer id 0. Null if nothing should be ringing. */
    fun currentId(context: Context): Int? {
        val p = prefs(context)
        if (!p.getBoolean(KEY_ACTIVE, false)) return null
        return p.getInt(KEY_ID, -1).takeIf { it >= 0 }
    }

    fun arm(context: Context, delayMs: Long = DEFAULT_DELAY_MS) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val at = SystemClock.elapsedRealtime() + delayMs
        val pi = pending(context)
        try {
            am.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, at, pi)
        } catch (_: SecurityException) {
            am.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, at, pi)
        }
    }

    fun disarm(context: Context) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = pending(context)
        am.cancel(pi)
        pi.cancel()
    }

    private fun pending(context: Context): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            REQUEST,
            Intent(context, RingWatchdogReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
