package com.aftab.curioalarm.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.aftab.curioalarm.Graph
import com.aftab.curioalarm.tools.TimerController

/** Fires when a countdown timer reaches zero; rings through the regular alarm pipeline. */
class TimerReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != TimerController.ACTION_FIRE) return
        val alarm = Graph.timer.onFired() ?: return   // stale broadcast: timer was cancelled/paused
        try {
            AlarmService.start(context, alarm.id)
        } catch (e: Exception) {
            Graph.timer.onRingDismissed()
            Notifier.showMissedTimer(context, alarm.label)
        }
    }
}
