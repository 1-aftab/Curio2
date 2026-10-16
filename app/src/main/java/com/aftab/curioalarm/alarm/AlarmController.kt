package com.aftab.curioalarm.alarm

import android.content.Context
import com.aftab.curioalarm.data.Alarm
import com.aftab.curioalarm.data.AlarmRepository

/** The single place that keeps stored alarms and AlarmManager registrations consistent. */
class AlarmController(
    private val context: Context,
    private val repo: AlarmRepository,
    private val scheduler: AlarmScheduler
) {

    fun save(alarm: Alarm): Alarm = applySchedule(repo.upsert(alarm))

    fun setEnabled(id: Int, enabled: Boolean) {
        val alarm = repo.get(id) ?: return
        applySchedule(repo.upsert(alarm.copy(enabled = enabled)))
    }

    fun delete(id: Int) {
        repo.get(id)?.let { scheduler.cancel(it) }
        repo.delete(id)
    }

    fun restorePresets() = repo.addPresets()

    /**
     * Called by [AlarmReceiver]. Returns the alarm if it should ring now, or null when it was
     * disabled/deleted or this exact occurrence has already been handled (duplicate trigger).
     */
    @Synchronized
    fun onFired(id: Int, triggerAt: Long): Alarm? {
        val alarm = repo.get(id) ?: return null
        if (!alarm.enabled) return null
        if (triggerAt != 0L && alarm.lastFiredAt == triggerAt) return null
        val updated = repo.upsert(alarm.copy(lastFiredAt = triggerAt, enabled = alarm.isRepeating))
        if (updated.enabled) applySchedule(updated) else repo.upsert(updated.copy(scheduledAt = 0L))
        return alarm
    }

    /** Re-registers every enabled alarm (boot, app update, time/timezone change). */
    @Synchronized
    fun restoreAll() {
        val now = System.currentTimeMillis()
        for (alarm in repo.snapshot()) {
            if (!alarm.enabled) {
                scheduler.cancel(alarm)
                continue
            }
            val missed = !alarm.isRepeating && alarm.scheduledAt in 1 until now &&
                alarm.lastFiredAt != alarm.scheduledAt
            if (missed) {
                repo.upsert(alarm.copy(enabled = false, scheduledAt = 0L))
                Notifier.showMissed(context, alarm)
            } else {
                applySchedule(alarm)
            }
        }
    }

    private fun applySchedule(alarm: Alarm): Alarm {
        val at = if (alarm.enabled) scheduler.schedule(alarm) else {
            scheduler.cancel(alarm)
            0L
        }
        return repo.upsert(alarm.copy(scheduledAt = at))
    }
}
