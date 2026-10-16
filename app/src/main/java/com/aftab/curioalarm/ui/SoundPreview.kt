package com.aftab.curioalarm.ui

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.io.File

/** Plays a one-shot preview of an alarm sound inside the editor. */
class SoundPreview(private val context: Context) {
    private var player: MediaPlayer? = null

    var playing by mutableStateOf(false)
        private set

    /** Starts or stops the preview. Returns false when the sound cannot be played. */
    fun toggle(path: String?): Boolean {
        if (playing) {
            stop()
            return true
        }
        return start(path)
    }

    private fun start(path: String?): Boolean {
        stop()
        val mp = MediaPlayer()
        return try {
            mp.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            if (path != null && File(path).exists()) {
                mp.setDataSource(path)
            } else {
                mp.setDataSource(context, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM))
            }
            mp.setOnCompletionListener { stop() }
            mp.prepare()
            mp.start()
            player = mp
            playing = true
            true
        } catch (_: Exception) {
            runCatching { mp.release() }
            false
        }
    }

    fun stop() {
        player?.let {
            runCatching { it.stop() }
            runCatching { it.release() }
        }
        player = null
        playing = false
    }
}
