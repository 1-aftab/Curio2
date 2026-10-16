package com.aftab.curioalarm.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.aftab.curioalarm.Graph

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != AlarmScheduler.ACTION_FIRE) return
        val id = intent.getIntExtra(AlarmScheduler.EXTRA_ALARM_ID, -1)
        val triggerAt = intent.getLongExtra(AlarmScheduler.EXTRA_TRIGGER_AT, 0L)
        val alarm = Graph.controller.onFired(id, triggerAt) ?: return
        try {
            AlarmService.start(context, alarm.id)
        } catch (e: Exception) {
            Notifier.showMissed(context, alarm)
        }
    }
}
