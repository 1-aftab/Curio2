package com.aftab.curioalarm.ui

import android.os.SystemClock
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aftab.curioalarm.Graph
import com.aftab.curioalarm.tools.StopwatchController.Mode
import kotlinx.coroutines.delay

/** Stopwatch section. Timekeeping lives in [Graph.stopwatch], so it survives tab switches and backgrounding. */
@Composable
fun StopwatchScreen(contentPadding: PaddingValues) {
    val controller = remember { Graph.stopwatch }
    val state by controller.state.collectAsState()

    // Tick the display while running; the controller itself needs no ticking.
    var now by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    LaunchedEffect(state.mode) {
        while (state.mode == Mode.RUNNING) {
            now = SystemClock.elapsedRealtime()
            delay(33)
        }
    }
    val elapsed = when (state.mode) {
        Mode.RUNNING -> state.accumulated + (now - state.startElapsed).coerceAtLeast(0L)
        Mode.PAUSED -> state.accumulated
        Mode.IDLE -> 0L
    }

    val deltas = remember(state.laps) {
        state.laps.mapIndexed { i, t -> t - (state.laps.getOrNull(i - 1) ?: 0L) }
    }
    val fastest = if (deltas.size >= 2) deltas.minOrNull() else null
    val slowest = if (deltas.size >= 2) deltas.maxOrNull() else null

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 20.dp,
            end = 20.dp,
            top = contentPadding.calculateTopPadding() + 24.dp,
            bottom = contentPadding.calculateBottomPadding() + 24.dp
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column(Modifier.padding(bottom = 4.dp)) {
                Text(
                    "Stopwatch",
                    color = Palette.Text,
                    fontSize = 34.sp,
                    fontFamily = FontFamily.Serif,
                    letterSpacing = (-0.3).sp
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Keeps running while you're away.",
                    color = Palette.Muted,
                    fontSize = 13.sp,
                    letterSpacing = 0.3.sp
                )
            }
        }

        item {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = Palette.Surface,
                border = BorderStroke(1.dp, Palette.Outline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    Modifier.fillMaxWidth().padding(vertical = 36.dp, horizontal = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    SectionLabel(
                        when (state.mode) {
                            Mode.RUNNING -> "Running"
                            Mode.PAUSED -> "Paused"
                            Mode.IDLE -> "Ready"
                        }
                    )
                    Spacer(Modifier.height(14.dp))
                    Text(
                        Fmt.stopwatch(elapsed),
                        color = if (state.mode == Mode.RUNNING) Palette.Accent else Palette.Text,
                        fontSize = 46.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Light,
                        maxLines = 1
                    )
                }
            }
        }

        item {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                when (state.mode) {
                    Mode.IDLE -> PrimaryButton(
                        "Start",
                        onClick = { controller.start() },
                        modifier = Modifier.weight(1f)
                    )
                    Mode.RUNNING -> {
                        GhostButton(
                            "Lap",
                            onClick = { controller.lap() },
                            modifier = Modifier.weight(1f).height(54.dp)
                        )
                        PrimaryButton(
                            "Pause",
                            onClick = { controller.pause() },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Mode.PAUSED -> {
                        GhostButton(
                            "Reset",
                            onClick = { controller.reset() },
                            modifier = Modifier.weight(1f).height(54.dp)
                        )
                        PrimaryButton(
                            "Resume",
                            onClick = { controller.start() },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        if (state.laps.isNotEmpty()) {
            item {
                Row(
                    Modifier.fillMaxWidth().padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SectionLabel("Laps", Modifier.weight(1f))
                    Text(
                        "${state.laps.size} recorded",
                        color = Palette.Muted,
                        fontSize = 11.sp,
                        letterSpacing = 0.4.sp
                    )
                }
            }
            val last = state.laps.lastIndex
            itemsIndexed(state.laps.asReversed(), key = { i, _ -> last - i }) { revIndex, cumulative ->
                val i = last - revIndex
                val delta = deltas[i]
                val lapColor = when {
                    deltas.size >= 2 && fastest != slowest && delta == fastest -> Palette.Accent
                    deltas.size >= 2 && fastest != slowest && delta == slowest -> Palette.Danger
                    else -> Palette.Text
                }
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Palette.Surface,
                    border = BorderStroke(1.dp, Palette.Outline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Lap ${(i + 1).toString().padStart(2, '0')}",
                            color = Palette.Muted,
                            fontSize = 13.sp,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            Fmt.stopwatch(delta),
                            color = lapColor,
                            fontSize = 15.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(Modifier.weight(0.4f))
                        Text(
                            Fmt.stopwatch(cumulative),
                            color = Palette.Muted,
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        item { CuriosityFooter(Modifier.padding(top = 6.dp)) }
    }
}
