package com.aftab.curioalarm.ui

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import com.aftab.curioalarm.Graph

enum class PermissionKind(val title: String, val detail: String) {
    NOTIFICATIONS("Notifications", "Needed to show the ringing alarm."),
    EXACT_ALARM("Exact alarms", "Lets alarms fire on the exact minute."),
    FULL_SCREEN("Full-screen alerts", "Shows the alarm over the lock screen."),
    BATTERY("Unrestricted battery (recommended)", "Keeps alarms reliable on aggressive power-saving phones.")
}

data class PermissionStatus(
    val notifications: Boolean,
    val exactAlarm: Boolean,
    val fullScreen: Boolean,
    val battery: Boolean
) {
    val missing: List<PermissionKind>
        get() = buildList {
            if (!notifications) add(PermissionKind.NOTIFICATIONS)
            if (!exactAlarm) add(PermissionKind.EXACT_ALARM)
            if (!fullScreen) add(PermissionKind.FULL_SCREEN)
            if (!battery) add(PermissionKind.BATTERY)
        }
    val allGood: Boolean get() = missing.isEmpty()
}

object Permissions {

    fun status(context: Context): PermissionStatus {
        val fullScreen = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            context.getSystemService(NotificationManager::class.java).canUseFullScreenIntent()
        } else true
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return PermissionStatus(
            notifications = NotificationManagerCompat.from(context).areNotificationsEnabled(),
            exactAlarm = Graph.scheduler.canScheduleExact(),
            fullScreen = fullScreen,
            battery = pm.isIgnoringBatteryOptimizations(context.packageName)
        )
    }

    fun open(context: Context, kind: PermissionKind) {
        val pkg = Uri.parse("package:${context.packageName}")
        val details = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, pkg)
        val intent = when (kind) {
            PermissionKind.NOTIFICATIONS ->
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            PermissionKind.EXACT_ALARM ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                    Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, pkg) else details
            PermissionKind.FULL_SCREEN ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
                    Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, pkg) else details
            PermissionKind.BATTERY ->
                Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, pkg)
        }
        // Some OEM builds throw SecurityException or other RuntimeExceptions for these
        // settings intents — a permission helper must never crash the app.
        try {
            context.startActivity(intent)
        } catch (_: Exception) {
            runCatching { context.startActivity(details) }
        }
    }
}
