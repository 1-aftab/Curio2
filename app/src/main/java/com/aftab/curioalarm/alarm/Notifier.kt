package com.aftab.curioalarm.alarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.aftab.curioalarm.R
import com.aftab.curioalarm.data.Alarm
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

object Notifier {
    const val CH_RING = "alarm_ring"
    const val CH_INFO = "alarm_info"

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(NotificationManager::class.java)
        val ring = NotificationChannel(CH_RING, "Ringing alarms", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Shown while an alarm is ringing"
            setSound(null, null)      // audio is played by the alarm service itself
            enableVibration(false)    // vibration is handled by the alarm service
            lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
        }
        val info = NotificationChannel(CH_INFO, "Alarm info", NotificationManager.IMPORTANCE_DEFAULT).apply {
            description = "Missed alarms and problems"
        }
        nm.createNotificationChannel(ring)
        nm.createNotificationChannel(info)
    }

    fun showMissedTimer(context: Context, label: String) {
        ensureChannels(context)
        val n = NotificationCompat.Builder(context, CH_INFO)
            .setSmallIcon(R.drawable.ic_stat_alarm)
            .setContentTitle("Timer finished")
            .setContentText("${label.ifBlank { "Your timer" }} reached zero but could not ring.")
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(20_001, n)
        } catch (_: SecurityException) {
        }
    }

    fun showMissed(context: Context, alarm: Alarm) {
        ensureChannels(context)
        val time = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
            .format(LocalTime.of(alarm.hour, alarm.minute))
        val n = NotificationCompat.Builder(context, CH_INFO)
            .setSmallIcon(R.drawable.ic_stat_alarm)
            .setContentTitle("Missed alarm")
            .setContentText("${alarm.label.ifBlank { "Alarm" }} at $time did not ring (device was off).")
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(10_000 + alarm.id, n)
        } catch (_: SecurityException) {
        }
    }
}
