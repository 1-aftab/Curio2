package com.aftab.curioalarm.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.aftab.curioalarm.data.Alarm
import com.aftab.curioalarm.ui.MainActivity
import java.time.LocalTime
import java.time.ZonedDateTime

class AlarmScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun canScheduleExact(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()

    /** Next moment (strictly after [now]) this alarm should ring. */
    fun nextTrigger(alarm: Alarm, now: ZonedDateTime = ZonedDateTime.now()): ZonedDateTime {
        val time = LocalTime.of(alarm.hour, alarm.minute)
        for (offset in 0..8) {
            val candidate = ZonedDateTime.of(now.toLocalDate().plusDays(offset.toLong()), time, now.zone)
            if (!candidate.isAfter(now)) continue
            if (alarm.days.isEmpty() || candidate.dayOfWeek.value in alarm.days) return candidate
        }
        return now.plusDays(1)
    }

    /** Registers the next occurrence. Returns the trigger time in epoch millis. */
    fun schedule(alarm: Alarm): Long {
        val at = nextTrigger(alarm).toInstant().toEpochMilli()
        val operation = firePendingIntent(alarm.id, at)
        val showIntent = PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        try {
            // setAlarmClock: exact, survives Doze, shown in the system UI as the next alarm.
            alarmManager.setAlarmClock(AlarmManager.AlarmClockInfo(at, showIntent), operation)
        } catch (e: SecurityException) {
            // Exact-alarm permission revoked: fall back to best-effort so the alarm still rings.
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, operation)
        }
        return at
    }

    fun cancel(alarm: Alarm) {
        // FLAG_NO_CREATE: cancelling must never create a fresh PendingIntent nor overwrite
        // the trigger-time extras of one that is still scheduled.
        val existing = PendingIntent.getBroadcast(
            context, alarm.id,
            Intent(context, AlarmReceiver::class.java).setAction(ACTION_FIRE),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (existing != null) {
            alarmManager.cancel(existing)
            existing.cancel()
        }
    }

    private fun firePendingIntent(alarmId: Int, triggerAt: Long): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java)
            .setAction(ACTION_FIRE)
            .putExtra(EXTRA_ALARM_ID, alarmId)
            .putExtra(EXTRA_TRIGGER_AT, triggerAt)
        return PendingIntent.getBroadcast(
            context, alarmId, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    companion object {
        const val ACTION_FIRE = "com.aftab.curioalarm.ACTION_FIRE"
        const val EXTRA_ALARM_ID = "alarm_id"
        const val EXTRA_TRIGGER_AT = "trigger_at"
    }
}
