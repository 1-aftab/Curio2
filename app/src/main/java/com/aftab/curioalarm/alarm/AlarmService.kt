package com.aftab.curioalarm.alarm

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
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
 * It only stops when [dismiss] is called (after the user solves the challenge).
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
    private val queue = ArrayDeque<Int>()

    private val alarmAttributes: AudioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_ALARM)
        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
        .build()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_DISMISS -> {
                dismissCurrent()
                return START_NOT_STICKY
            }
            ACTION_REPOST -> {
                if (currentAlarmId < 0) {
                    stopSelf()
                    return START_NOT_STICKY
                }
                postForeground(resolve(currentAlarmId), currentAlarmId)
                return START_REDELIVER_INTENT
            }
        }

        val id = intent?.getIntExtra(EXTRA_ALARM_ID, -1) ?: -1
        val alarm = resolve(id)

        // Phantom trigger: the alarm was deleted (or the intent is malformed / redelivered
        // without data). Never ring for a non-existent alarm — it could never be dismissed
        // from Home and would ring forever.
        if (alarm == null) {
            if (currentAlarmId >= 0) {
                // Keep the current alarm ringing; still re-promote to satisfy the
                // startForegroundService() contract for this start.
                postForeground(resolve(currentAlarmId), currentAlarmId)
            } else {
                // Promote first, then stop — some OEM builds crash otherwise even when
                // stopSelf() is immediate.
                postForeground(null, -1)
                _ringing.value = null
                ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
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

    /** Stored alarm, or the synthetic timer alarm for the reserved timer id. */
    private fun resolve(id: Int): Alarm? =
        if (id == TimerController.TIMER_ID) Graph.timer.ringingAlarm() else Graph.repo.get(id)

    private fun beginRinging(alarm: Alarm) {
        stopPlayback()
        currentAlarmId = alarm.id
        _ringing.value = alarm.id
        acquireWakeLock()
        startSound(alarm)
        startVibration(alarm)
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
        currentAlarmId = -1
        _ringing.value = null
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        stopPlayback()
        queue.clear()
        currentAlarmId = -1
        _ringing.value = null
        super.onDestroy()
    }

    /**
     * Opens the task screen right away. Android only allows this when the app is already visible
     * (or briefly after an alarm fires); otherwise the call is ignored and the full-screen
     * notification posted by [postForeground] remains the supported fallback.
     */
    private fun showAlarmScreen(id: Int) {
        try {
            startActivity(
                Intent(this, AlarmActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    .putExtra(EXTRA_ALARM_ID, id)
            )
        } catch (_: Exception) {
            // Background launch not permitted: the full-screen intent / heads-up notification covers it.
        }
    }

    // ---------------------------------------------------------------- notification

    private fun postForeground(alarm: Alarm?, id: Int) {
        Notifier.ensureChannels(this)
        val notification = buildNotification(alarm, id)
        try {
            ServiceCompat.startForeground(
                this, NOTIF_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
        } catch (_: Exception) {
            // Keep ringing even if the OS refuses the foreground promotion.
        }
    }

    private fun buildNotification(alarm: Alarm?, id: Int): Notification {
        val open = PendingIntent.getActivity(
            this, 1,
            Intent(this, AlarmActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .putExtra(EXTRA_ALARM_ID, id),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val repost = PendingIntent.getService(
            this, 2,
            Intent(this, AlarmService::class.java).setAction(ACTION_REPOST),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val time = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).format(
            if (alarm != null) LocalTime.of(alarm.hour, alarm.minute) else LocalTime.now()
        )
        return NotificationCompat.Builder(this, Notifier.CH_RING)
            .setSmallIcon(R.drawable.ic_stat_alarm)
            .setContentTitle(alarm?.label?.ifBlank { null } ?: "Alarm")
            .setContentText(if (alarm != null && alarm.challenges.isEmpty()) "$time · Time's up — tap to open" else "$time · Solve the challenge to dismiss")
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(open)
            .setFullScreenIntent(open, true)
            .setDeleteIntent(repost)
            .build()
    }

    // ---------------------------------------------------------------- audio

    private fun startSound(alarm: Alarm?) {
        triedFallback = false
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
        var step = 0
        val steps = 40  // 40 * 750 ms = 30 s
        runCatching { player?.setVolume(0.05f, 0.05f) }
        val tick = object : Runnable {
            override fun run() {
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
            val req = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                .setAudioAttributes(alarmAttributes)
                .setOnAudioFocusChangeListener { }
                .build()
            am.requestAudioFocus(req)
            focusRequest = req
        } catch (_: Exception) {
        }
    }

    // ---------------------------------------------------------------- vibration / wake lock

    @Suppress("DEPRECATION")
    private fun startVibration(alarm: Alarm?) {
        if (alarm?.vibrate == false) return
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
        const val EXTRA_ALARM_ID = "alarm_id"
        private const val NOTIF_ID = 4242

        private val _ringing = MutableStateFlow<Int?>(null)

        /** Id of the alarm that is ringing right now, or null. */
        val ringing: StateFlow<Int?> = _ringing.asStateFlow()

        fun start(context: Context, alarmId: Int) {
            val intent = Intent(context, AlarmService::class.java)
                .setAction(ACTION_START)
                .putExtra(EXTRA_ALARM_ID, alarmId)
            ContextCompat.startForegroundService(context, intent)
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
