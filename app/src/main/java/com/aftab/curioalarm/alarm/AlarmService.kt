package com.aftab.curioalarm.alarm

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.aftab.curioalarm.Graph
import com.aftab.curioalarm.R
import com.aftab.curioalarm.data.Alarm
import com.aftab.curioalarm.tools.TimerController
import com.aftab.curioalarm.ui.AlarmActivity
import java.io.File
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Foreground service that owns the audio, vibration and notification of a ringing alarm.
 * It only stops when [dismiss] is called (after the user solves the challenge, or taps
 * Dismiss on a timer). The power button, Home, and a killed process do not count as a dismiss:
 * the screen is brought back and a watchdog restarts the service if the process dies.
 * If several alarms fire together they are queued and dismissed one after another.
 */
class AlarmService : Service() {

    private var player: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var focusRequest: AudioFocusRequest? = null
    private val handler = Handler(Looper.getMainLooper())
    private var currentAlarmId = -1
    private var triedFallback = false
    private var ramping = false
    private var rampToken = 0
    private var lastNudge = 0L
    private var lastSoundStart = 0L
    private var fsiCancel: Runnable? = null
    private val queue = ArrayDeque<Int>()

    private val alarmAttributes: AudioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_ALARM)
        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
        .build()

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (currentAlarmId < 0) return
            when (intent?.action) {
                Intent.ACTION_SCREEN_OFF, Intent.ACTION_SCREEN_ON -> nudgeUi()
            }
        }
    }

    private val playbackWatch = object : Runnable {
        override fun run() {
            if (currentAlarmId < 0) return
            ensureStillPlaying(restartVibration = false)
            handler.postDelayed(this, 2_000)
        }
    }

    private fun startPlaybackWatch() {
        handler.removeCallbacks(playbackWatch)
        handler.postDelayed(playbackWatch, 2_000)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        val filter = IntentFilter(Intent.ACTION_SCREEN_OFF).apply {
            addAction(Intent.ACTION_SCREEN_ON)
        }
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(screenReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            registerReceiver(screenReceiver, filter)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_DISMISS -> {
                dismissCurrent()
                return START_NOT_STICKY
            }
            ACTION_NUDGE -> {
                ensureAliveFromSession()
                if (currentAlarmId >= 0) nudgeUi()
                return if (currentAlarmId >= 0) START_REDELIVER_INTENT else START_NOT_STICKY
            }
            ACTION_REPOST -> {
                ensureAliveFromSession()
                if (currentAlarmId < 0) {
                    quietStop()
                    return START_NOT_STICKY
                }
                // A swipe removes the foreground notification. Put it back, then bring
                // the challenge forward. This is not a dismiss.
                postForeground(resolve(currentAlarmId), currentAlarmId)
                nudgeUi()
                return START_REDELIVER_INTENT
            }
        }

        val extraId = intent?.getIntExtra(EXTRA_ALARM_ID, -1) ?: -1
        val id = if (extraId >= 0) extraId else RingSession.currentId(this) ?: -1
        return startRing(id)
    }

    /** Stored alarm, or the synthetic timer alarm for the reserved timer id. */
    private fun resolve(id: Int): Alarm? =
        if (id == TimerController.TIMER_ID) Graph.timer.ringingAlarm() else Graph.repo.get(id)

    private fun startRing(id: Int): Int {
        val alarm = resolve(id)

        // Phantom trigger: the alarm was deleted (or the intent is malformed / redelivered
        // without data). Never ring for a non-existent alarm — it could never be dismissed
        // from Home and would ring forever.
        if (alarm == null) {
            if (currentAlarmId >= 0) {
                // Keep the current alarm ringing; still re-promote to satisfy the
                // startForegroundService() contract for this start.
                postForeground(resolve(currentAlarmId), currentAlarmId)
                return START_REDELIVER_INTENT
            }
            RingSession.clear(this)
            quietStop()
            return START_NOT_STICKY
        }

        // Something is already ringing.
        if (currentAlarmId >= 0 && _ringing.value == currentAlarmId) {
            // A *different* alarm: queue it instead of cutting the current one off.
            // (Cutting it off silently lost the first alarm — its challenge was never shown.)
            // A duplicate of the current one: never queue it, or it would ring again on dismiss.
            if (id != currentAlarmId && id !in queue) queue.addLast(id)
            // re-promote with the *current* alarm's info: startForegroundService() requires
            // startForeground() on every start, even for duplicates and queued alarms.
            postForeground(resolve(currentAlarmId), currentAlarmId)
            return START_REDELIVER_INTENT
        }

        // startForegroundService() requires startForeground() every time.
        postForeground(alarm, id)
        beginRinging(alarm)
        return START_REDELIVER_INTENT
    }

    /** After process death the in-memory id is gone. The persisted session is the source of truth. */
    private fun ensureAliveFromSession() {
        if (currentAlarmId >= 0 && _ringing.value == currentAlarmId) return
        val saved = RingSession.currentId(this) ?: return
        val alarm = resolve(saved) ?: run {
            RingSession.clear(this)
            return
        }
        postForeground(alarm, saved)
        beginRinging(alarm)
    }

    private fun beginRinging(alarm: Alarm) {
        stopPlayback()
        cancelCompanion()
        currentAlarmId = alarm.id
        _ringing.value = alarm.id
        RingSession.mark(this, alarm.id)
        acquireWakeLock()
        if (alarm.alertMode.playsSound) startSound(alarm)
        if (alarm.alertMode.vibrates) startVibration(alarm)
        startPlaybackWatch()
        if (alarm.hardMode) showAlarmScreen(alarm.id)
    }

    /** Solves the current alarm; rings the next queued one, or stops when the queue is empty. */
    private fun dismissCurrent() {
        stopPlayback()
        if (currentAlarmId == TimerController.TIMER_ID) Graph.timer.onRingDismissed()
        while (queue.isNotEmpty()) {
            val next = resolve(queue.removeFirst())
            if (next != null) {
                postForeground(next, next.id)
                beginRinging(next)
                return
            }
        }
        // Clear the session before publishing "not ringing", so the activity leaves
        // instead of treating a null id as "service still coming back".
        RingSession.clear(this)
        currentAlarmId = -1
        _ringing.value = null
        cancelCompanion()
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        runCatching { unregisterReceiver(screenReceiver) }
        stopPlayback()
        queue.clear()
        currentAlarmId = -1
        _ringing.value = null
        // Do not clear RingSession here. A real dismiss already cleared it. If the process
        // is being killed, the watchdog still has to revive the alarm.
        super.onDestroy()
    }

    /**
     * Power / Home / notification swipe. Turns the screen back on and re-posts a full-screen
     * intent. Android does not deliver the power key to apps, so this is the supported way
     * to refuse that button as a dismiss.
     */
    private fun nudgeUi() {
        if (currentAlarmId < 0) return
        val now = SystemClock.elapsedRealtime()
        if (now - lastNudge < 500) return
        lastNudge = now
        val alarm = resolve(currentAlarmId)
        wakeScreen()
        fireFullScreen(alarm, currentAlarmId)
        showAlarmScreen(currentAlarmId)
        ensureStillPlaying(restartVibration = true)
        // If this press is about to kill the process, revive quickly.
        RingSession.arm(this, 2_000L)
    }

    private fun ensureStillPlaying(restartVibration: Boolean) {
        val alarm = resolve(currentAlarmId) ?: return
        if (alarm.alertMode.playsSound) {
            ensureAudible()
            val playing = runCatching { player?.isPlaying == true }.getOrDefault(false)
            if (!playing) startSound(alarm)
        }
        if (alarm.alertMode.vibrates && (restartVibration || vibrator == null)) {
            runCatching { vibrator?.cancel() }
            vibrator = null
            startVibration(alarm)
        }
    }

    /**
     * Opens the task screen right away. Android only allows this when the app is already visible
     * (or briefly after an alarm fires); otherwise the call is ignored and the full-screen
     * notification posted by [fireFullScreen] remains the supported fallback.
     */
    private fun showAlarmScreen(id: Int) {
        try {
            startActivity(
                Intent(this, AlarmActivity::class.java)
                    .addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TOP or
                            Intent.FLAG_ACTIVITY_SINGLE_TOP or
                            Intent.FLAG_ACTIVITY_NO_USER_ACTION
                    )
                    .putExtra(EXTRA_ALARM_ID, id)
            )
        } catch (_: Exception) {
            // Background launch not permitted: the full-screen intent covers it.
        }
    }

    // ---------------------------------------------------------------- notification

    private fun postForeground(alarm: Alarm?, id: Int) {
        Notifier.ensureChannels(this)
        val notification = buildNotification(alarm, id, withDeleteIntent = true)
        try {
            ServiceCompat.startForeground(
                this, NOTIF_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
        } catch (_: Exception) {
            // Keep ringing even if the OS refuses the foreground promotion.
        }
    }

    /**
     * Updating the foreground notification does not fire its full-screen intent again.
     * A second notification, cancelled and posted fresh, does. It has no delete intent,
     * so cancelling it cannot be mistaken for the user swiping the alarm away.
     */
    private fun fireFullScreen(alarm: Alarm?, id: Int) {
        val nm = getSystemService(NotificationManager::class.java) ?: return
        val notification = buildNotification(alarm, id, withDeleteIntent = false)
        fsiCancel?.let { handler.removeCallbacks(it) }
        runCatching { nm.cancel(NOTIF_FSI) }
        runCatching { nm.notify(NOTIF_FSI, notification) }
        val cancel = Runnable {
            fsiCancel = null
            runCatching { nm.cancel(NOTIF_FSI) }
        }
        fsiCancel = cancel
        handler.postDelayed(cancel, 8_000)
    }

    private fun cancelCompanion() {
        runCatching { getSystemService(NotificationManager::class.java)?.cancel(NOTIF_FSI) }
    }

    private fun quietStop() {
        postForeground(null, -1)
        _ringing.value = null
        cancelCompanion()
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun buildNotification(alarm: Alarm?, id: Int, withDeleteIntent: Boolean): Notification {
        val open = PendingIntent.getActivity(
            this, 1,
            Intent(this, AlarmActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .putExtra(EXTRA_ALARM_ID, id),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val time = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).format(
            if (alarm != null) LocalTime.of(alarm.hour, alarm.minute) else LocalTime.now()
        )
        val builder = NotificationCompat.Builder(this, Notifier.CH_RING)
            .setSmallIcon(R.drawable.ic_stat_alarm)
            .setContentTitle(alarm?.label?.ifBlank { null } ?: "Alarm")
            .setContentText(
                if (alarm != null && alarm.challenges.isEmpty()) "$time · Time's up — tap to open"
                else "$time · Solve the challenge to dismiss"
            )
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(withDeleteIntent)
            .setOnlyAlertOnce(false)
            .setContentIntent(open)
            .setFullScreenIntent(open, true)
        if (withDeleteIntent) {
            val repost = PendingIntent.getService(
                this, 2,
                Intent(this, AlarmService::class.java).setAction(ACTION_REPOST),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.setDeleteIntent(repost)
        }
        return builder.build()
    }

    // ---------------------------------------------------------------- audio

    private fun startSound(alarm: Alarm?) {
        if (alarm != null && !alarm.alertMode.playsSound) return
        val now = SystemClock.elapsedRealtime()
        // Focus-loss can call back into here immediately. Don't tear down a player we just started.
        if (player != null && now - lastSoundStart < 800) return
        lastSoundStart = now
        triedFallback = false
        ramping = false
        rampToken++
        player?.let { runCatching { it.stop() }; runCatching { it.release() } }
        player = null
        requestAudioFocus()
        ensureAudible()
        val customPath = alarm?.soundPath?.takeIf { File(it).exists() }
        val mp: MediaPlayer = (if (customPath != null) prepare { setDataSource(customPath) } else null)
            ?: prepareFallback()
            ?: return   // vibration-only as the very last resort
        mp.setOnErrorListener { _, _, _ ->
            if (!triedFallback) {
                triedFallback = true
                playerSwap(prepareFallback())
                if (alarm != null && alarm.gradualVolume) rampVolume() else player?.setVolume(1f, 1f)
            }
            true
        }
        player = mp
        mp.start()
        if (alarm != null && alarm.gradualVolume) rampVolume() else mp.setVolume(1f, 1f)
    }

    private fun playerSwap(next: MediaPlayer?) {
        player?.let { runCatching { it.stop() }; runCatching { it.release() } }
        player = next
        next?.start()
    }

    private fun prepareFallback(): MediaPlayer? {
        val candidates = listOf(
            RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_ALARM),
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM),
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE),
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        )
        for (uri in candidates) {
            if (uri == null) continue
            val mp = prepare { setDataSource(this@AlarmService, uri) }
            if (mp != null) return mp
        }
        return null
    }

    private fun prepare(setup: MediaPlayer.() -> Unit): MediaPlayer? {
        val mp = MediaPlayer()
        return try {
            mp.setAudioAttributes(alarmAttributes)
            mp.setWakeMode(applicationContext, PowerManager.PARTIAL_WAKE_LOCK)
            mp.setup()
            mp.isLooping = true
            mp.prepare()
            mp
        } catch (_: Exception) {
            runCatching { mp.release() }
            null
        }
    }

    /**
     * Reads the *current* player on every tick: after an error-fallback swap the ramp
     * continues on the new player instead of poking a released one.
     */
    private fun rampVolume() {
        if (ramping) return
        ramping = true
        val token = rampToken
        var step = 0
        val steps = 40  // 40 * 750 ms = 30 s
        runCatching { player?.setVolume(0.05f, 0.05f) }
        val tick = object : Runnable {
            override fun run() {
                if (token != rampToken) return
                step++
                val v = (0.05f + 0.95f * step / steps).coerceAtMost(1f)
                runCatching { player?.setVolume(v, v) }
                if (step < steps) handler.postDelayed(this, 750) else ramping = false
            }
        }
        handler.postDelayed(tick, 750)
    }

    private fun ensureAudible() {
        try {
            val am = getSystemService(Context.AUDIO_SERVICE) as AudioManager
            if (am.getStreamVolume(AudioManager.STREAM_ALARM) == 0) {
                val max = am.getStreamMaxVolume(AudioManager.STREAM_ALARM)
                am.setStreamVolume(AudioManager.STREAM_ALARM, (max * 0.7f).toInt().coerceAtLeast(1), 0)
            }
        } catch (_: Exception) {
        }
    }

    private fun requestAudioFocus() {
        try {
            val am = getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val req = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(alarmAttributes)
                .setOnAudioFocusChangeListener { change ->
                    if (change != AudioManager.AUDIOFOCUS_GAIN && currentAlarmId >= 0) {
                        handler.post { ensureStillPlaying(restartVibration = false) }
                    }
                }
                .build()
            am.requestAudioFocus(req)
            focusRequest = req
        } catch (_: Exception) {
        }
    }

    // ---------------------------------------------------------------- vibration / wake lock

    @Suppress("DEPRECATION")
    private fun startVibration(alarm: Alarm?) {
        if (alarm != null && !alarm.alertMode.vibrates) return
        try {
            val v: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                getSystemService(VibratorManager::class.java)?.defaultVibrator
            } else {
                getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
            if (v != null && v.hasVibrator()) {
                v.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 700, 500), 0), alarmAttributes)
                vibrator = v
            }
        } catch (_: Exception) {
        }
    }

    private fun acquireWakeLock() {
        if (wakeLock?.isHeld == true) return
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "curioalarm:ring").apply {
            acquire(2 * 60 * 60 * 1000L)
        }
    }

    /**
     * The power button always wins against FLAG_KEEP_SCREEN_ON. This lock turns the screen
     * back on so the challenge is in front again. Deprecated since API 17, but still the
     * only call a service can make to force the display on; the activity then holds
     * FLAG_KEEP_SCREEN_ON.
     */
    @Suppress("DEPRECATION")
    private fun wakeScreen() {
        try {
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            val screen = pm.newWakeLock(
                PowerManager.SCREEN_BRIGHT_WAKE_LOCK or
                    PowerManager.ACQUIRE_CAUSES_WAKEUP or
                    PowerManager.ON_AFTER_RELEASE,
                "curioalarm:screen"
            )
            screen.acquire(5_000L)
            handler.postDelayed({ runCatching { if (screen.isHeld) screen.release() } }, 4_000)
        } catch (_: Exception) {
        }
    }

    // ---------------------------------------------------------------- stopping

    private fun stopPlayback() {
        handler.removeCallbacksAndMessages(null)
        ramping = false
        player?.let { runCatching { it.stop() }; runCatching { it.release() } }
        player = null
        runCatching { vibrator?.cancel() }
        vibrator = null
        runCatching {
            focusRequest?.let {
                (getSystemService(Context.AUDIO_SERVICE) as AudioManager).abandonAudioFocusRequest(it)
            }
        }
        focusRequest = null
        runCatching { if (wakeLock?.isHeld == true) wakeLock?.release() }
        wakeLock = null
    }

    companion object {
        const val ACTION_START = "com.aftab.curioalarm.START"
        const val ACTION_DISMISS = "com.aftab.curioalarm.DISMISS"
        const val ACTION_REPOST = "com.aftab.curioalarm.REPOST"
        const val ACTION_NUDGE = "com.aftab.curioalarm.NUDGE"
        const val EXTRA_ALARM_ID = "alarm_id"
        private const val NOTIF_ID = 4242
        private const val NOTIF_FSI = 4243

        private val _ringing = MutableStateFlow<Int?>(null)

        /** Id of the alarm that is ringing right now, or null. */
        val ringing: StateFlow<Int?> = _ringing.asStateFlow()

        fun start(context: Context, alarmId: Int) {
            val intent = Intent(context, AlarmService::class.java)
                .setAction(ACTION_START)
                .putExtra(EXTRA_ALARM_ID, alarmId)
            ContextCompat.startForegroundService(context, intent)
        }

        /** Bring the challenge screen back. Does not stop the alarm. */
        fun nudge(context: Context) {
            runCatching {
                context.startService(
                    Intent(context, AlarmService::class.java).setAction(ACTION_NUDGE)
                )
            }
        }

        fun dismiss(context: Context) {
            // The service updates [ringing] itself — to the next queued alarm, or to null.
            // If delivery fails the alarm is still ringing, so keep the state (and the
            // challenge screen) up rather than pretending it was dismissed.
            runCatching {
                context.startService(
                    Intent(context, AlarmService::class.java).setAction(ACTION_DISMISS)
                )
            }
        }
    }
}
