package com.aftab.curioalarm.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.aftab.curioalarm.Graph

/** Restores scheduled alarms after reboot, app update, and clock/timezone changes. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED -> {
                Graph.controller.restoreAll()
                Graph.timer.restore()
                SnoozeStore.allPending(context).forEach { pending ->
                    if (pending.atMillis <= System.currentTimeMillis()) {
                        SnoozeStore.clearPending(context, pending.alarmId)
                        runCatching { AlarmService.start(context, pending.alarmId) }
                    } else {
                        Graph.scheduler.scheduleSnooze(pending.alarmId, pending.atMillis)
                    }
                }
                // A reboot is not a dismiss. If a challenge was still owed, ring again.
                RingSession.currentId(context)?.let { id ->
                    runCatching { AlarmService.start(context, id) }
                }
            }
        }
    }
}
