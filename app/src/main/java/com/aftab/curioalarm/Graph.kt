package com.aftab.curioalarm

import android.content.Context
import com.aftab.curioalarm.alarm.AlarmController
import com.aftab.curioalarm.alarm.AlarmScheduler
import com.aftab.curioalarm.data.AlarmRepository
import com.aftab.curioalarm.tools.StopwatchController
import com.aftab.curioalarm.tools.TimerController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Tiny service locator. Initialised in [CurioApp.onCreate], so receivers/services can rely on it. */
object Graph {
    lateinit var repo: AlarmRepository
        private set
    lateinit var scheduler: AlarmScheduler
        private set
    lateinit var controller: AlarmController
        private set
    lateinit var stopwatch: StopwatchController
        private set
    lateinit var timer: TimerController
        private set

    val ioScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun init(context: Context) {
        val app = context.applicationContext
        repo = AlarmRepository(app)
        scheduler = AlarmScheduler(app)
        controller = AlarmController(app, repo, scheduler)
        stopwatch = StopwatchController(app)
        timer = TimerController(app)
    }
}

fun CoroutineScope.launchCleanup() {
    launch { Graph.repo.cleanupSounds() }
}
