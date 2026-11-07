package com.aftab.curioalarm.tools

import android.content.Context
import android.os.SystemClock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

/**
 * Stopwatch that keeps correct time while the app is backgrounded (based on the monotonic
 * [SystemClock.elapsedRealtime] clock) and survives process death via SharedPreferences.
 * Lives in Graph, so switching tabs never loses the running state.
 */
class StopwatchController(context: Context) {

    enum class Mode { IDLE, RUNNING, PAUSED }

    data class State(
        val mode: Mode = Mode.IDLE,
        val startElapsed: Long = 0L,      // elapsedRealtime at (re)start
        val startWall: Long = 0L,         // wall clock at (re)start, for reboot recovery
        val accumulated: Long = 0L,       // millis from previous run segments
        val laps: List<Long> = emptyList() // cumulative elapsed at each lap press
    )

    private val prefs = context.getSharedPreferences("curio_stopwatch", Context.MODE_PRIVATE)
    private val _state = MutableStateFlow(load())
    val state: StateFlow<State> = _state.asStateFlow()

    fun elapsedNow(): Long {
        val s = _state.value
        return when (s.mode) {
            Mode.RUNNING -> s.accumulated + (SystemClock.elapsedRealtime() - s.startElapsed).coerceAtLeast(0L)
            Mode.PAUSED -> s.accumulated
            Mode.IDLE -> 0L
        }
    }

    fun start() {
        val s = _state.value
        if (s.mode == Mode.RUNNING) return
        setState(
            s.copy(
                mode = Mode.RUNNING,
                startElapsed = SystemClock.elapsedRealtime(),
                startWall = System.currentTimeMillis()
            )
        )
    }

    fun pause() {
        val s = _state.value
        if (s.mode != Mode.RUNNING) return
        setState(s.copy(mode = Mode.PAUSED, accumulated = elapsedNow()))
    }

    fun lap() {
        val s = _state.value
        if (s.mode != Mode.RUNNING) return
        setState(s.copy(laps = s.laps + elapsedNow()))
    }

    fun reset() {
        setState(State())
    }

    // ---- internals ----

    private fun setState(s: State) {
        _state.value = s
        prefs.edit().putString(KEY, toJson(s).toString()).apply()
    }

    private fun load(): State = runCatching {
        val o = JSONObject(prefs.getString(KEY, null) ?: return State())
        var s = State(
            mode = runCatching { Mode.valueOf(o.optString("mode", "IDLE")) }.getOrDefault(Mode.IDLE),
            startElapsed = o.optLong("startElapsed"),
            startWall = o.optLong("startWall"),
            accumulated = o.optLong("accumulated"),
            laps = o.optJSONArray("laps")?.let { a -> (0 until a.length()).map { a.getLong(it) } } ?: emptyList()
        )
        // The device rebooted while running: elapsedRealtime restarted from zero, so settle
        // the time using the wall clock and leave it paused for the user to review.
        if (s.mode == Mode.RUNNING && SystemClock.elapsedRealtime() < s.startElapsed) {
            s = s.copy(
                mode = Mode.PAUSED,
                accumulated = s.accumulated + (System.currentTimeMillis() - s.startWall).coerceAtLeast(0L)
            )
        }
        s
    }.getOrDefault(State())

    private fun toJson(s: State) = JSONObject().apply {
        put("mode", s.mode.name)
        put("startElapsed", s.startElapsed)
        put("startWall", s.startWall)
        put("accumulated", s.accumulated)
        put("laps", JSONArray(s.laps))
    }

    private companion object {
        const val KEY = "state_json"
    }
}
