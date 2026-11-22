package com.aftab.curioalarm.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Fires while a ring session is owed. If the service is gone (power button, OEM kill),
 * start it again. If it is still playing, just keep the watchdog armed.
 */
class RingWatchdogReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val id = RingSession.currentId(context) ?: return
        if (RingGuard.needsRevive(id, AlarmService.ringing.value)) {
            runCatching { AlarmService.start(context, id) }
        }
        // start() re-arms on success; arm here too so a failed start is retried.
        if (RingSession.currentId(context) != null) RingSession.arm(context)
    }
}
