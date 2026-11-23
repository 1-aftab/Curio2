package com.aftab.curioalarm.ui

import androidx.lifecycle.ViewModel
import com.aftab.curioalarm.Graph
import com.aftab.curioalarm.data.Alarm
import java.time.LocalTime
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AlarmViewModel : ViewModel() {
    val alarms: StateFlow<List<Alarm>> = Graph.repo.alarms

    fun save(alarm: Alarm) {
        Graph.ioScope.launch { Graph.controller.save(alarm); Graph.repo.cleanupSounds() }
    }

    fun toggle(alarm: Alarm, enabled: Boolean) {
        Graph.ioScope.launch { Graph.controller.setEnabled(alarm.id, enabled) }
    }

    fun delete(id: Int) {
        Graph.ioScope.launch { Graph.controller.delete(id); Graph.repo.cleanupSounds() }
    }

    /** One-shot alarm a few minutes from now. A single 9-minute snooze, then the challenge. */
    fun quick(minutes: Int) {
        if (minutes <= 0) return
        val t = LocalTime.now().plusMinutes(minutes.toLong())
        Graph.ioScope.launch {
            Graph.controller.save(
                Alarm(
                    hour = t.hour,
                    minute = t.minute,
                    label = "Quick",
                    enabled = true,
                    snoozeMinutes = 9,
                    snoozeLimit = 1
                )
            )
        }
    }

    fun restorePresets() {
        Graph.ioScope.launch { Graph.controller.restorePresets() }
    }

    fun cleanupSounds() {
        Graph.ioScope.launch { Graph.repo.cleanupSounds() }
    }
}
