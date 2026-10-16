package com.aftab.curioalarm

import android.app.Application
import com.aftab.curioalarm.alarm.Notifier

class CurioApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Graph.init(this)
        Notifier.ensureChannels(this)
        Graph.ioScope.launchCleanup()
    }
}
