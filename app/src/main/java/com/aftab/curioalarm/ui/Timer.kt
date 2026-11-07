package com.aftab.curioalarm.ui

import android.os.SystemClock
import android.text.format.DateFormat
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aftab.curioalarm.Graph
import com.aftab.curioalarm.alarm.AlarmService
import com.aftab.curioalarm.tools.TimerController
import com.aftab.curioalarm.tools.TimerController.Mode
import java.util.Date
import kotlinx.coroutines.delay

/**
 * Timer section. The finish moment is registered with AlarmManager by [Graph.timer], so it
 * rings through the alarm pipeline even with the app closed or the phone locked.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimerScreen(contentPadding: PaddingValues) {
    val ctx = LocalContext.current
    val controller = remember { Graph.timer }
    val state by controller.state.collectAsState()
    val ringingId by AlarmService.ringing.collectAsState()

    var now by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    LaunchedEffect(state.mode, state.endElapsed) {
        while (state.mode == Mode.RUNNING) {
            now = SystemClock.elapsedRealtime()
            delay(100)
        }
    }
    val remaining = when (state.mode) {
        Mode.RUNNING -> (state.endElapsed - now).coerceAtLeast(0L)
        Mode.PAUSED -> state.remainingMillis
        else -> 0L
    }

    // The device rebooted but BootReceiver never ran for us (app was force-stopped before
    // the reboot): elapsedRealtime restarted below the saved marker. Heal from wall clock.
    LaunchedEffect(state.mode, state.savedAtElapsed) {
        if (state.mode == Mode.RUNNING && SystemClock.elapsedRealtime() < state.savedAtElapsed) {
            controller.restore()
        }
    }

    // Safety net: if the exact broadcast never arrived (e.g. the app was force-stopped,
    // which clears AlarmManager registrations), ring from the UI once the display hits zero.
    // AlarmService dedupes, so a late broadcast afterwards is harmless.
    LaunchedEffect(state.mode, remaining == 0L) {
        if (state.mode == Mode.RUNNING && remaining == 0L && ringingId != TimerController.TIMER_ID) {
            delay(2_500)
            controller.onFired()?.let { AlarmService.start(ctx, it.id) }
        }
    }

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
                    "Timer",
                    color = Palette.Text,
                    fontSize = 34.sp,
                    fontFamily = FontFamily.Serif,
                    letterSpacing = (-0.3).sp
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Rings like an alarm — even with the app closed.",
                    color = Palette.Muted,
                    fontSize = 13.sp,
                    letterSpacing = 0.3.sp
                )
            }
        }

        when (state.mode) {
            Mode.IDLE -> {
                item { DurationPicker(onStart = { ms, label -> controller.start(ms, label) }) }
                if (state.lastTotalMillis > 0) {
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            SectionLabel("Repeat last", Modifier.weight(1f))
                            FilterChip(
                                selected = false,
                                onClick = { controller.start(state.lastTotalMillis, state.label) },
                                label = { Text(Fmt.countdown(state.lastTotalMillis)) },
                                colors = curioChipColors(),
                                border = BorderStroke(1.dp, Palette.Outline2)
                            )
                        }
                    }
                }
                if (!Graph.scheduler.canScheduleExact()) {
                    item {
                        Text(
                            "Exact alarms are off in system settings, so the timer may fire a little late. Enable them from the permission card on the Alarms tab for on-time ringing.",
                            color = Palette.Danger,
                            fontSize = 12.sp,
                            lineHeight = 17.sp
                        )
                    }
                }
            }

            Mode.RUNNING, Mode.PAUSED -> {
                item {
                    Surface(
                        shape = RoundedCornerShape(28.dp),
                        color = Palette.Surface,
                        border = BorderStroke(1.dp, Palette.Outline),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            Modifier.fillMaxWidth().padding(vertical = 28.dp, horizontal = 20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(Modifier.size(250.dp), contentAlignment = Alignment.Center) {
                                CountdownArc(
                                    fraction = if (state.totalMillis > 0)
                                        remaining.toFloat() / state.totalMillis.toFloat() else 0f,
                                    paused = state.mode == Mode.PAUSED
                                )
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    SectionLabel(if (state.mode == Mode.PAUSED) "Paused" else "Remaining")
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        Fmt.countdown(remaining),
                                        color = if (state.mode == Mode.PAUSED) Palette.Text else Palette.Accent,
                                        fontSize = 42.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Light,
                                        maxLines = 1
                                    )
                                    if (state.label.isNotBlank()) {
                                        Spacer(Modifier.height(6.dp))
                                        Text(state.label, color = Palette.Muted, fontSize = 13.sp)
                                    }
                                }
                            }
                            Spacer(Modifier.height(10.dp))
                            Text(
                                if (state.mode == Mode.RUNNING)
                                    "Ends at " + DateFormat.getTimeFormat(ctx).format(Date(state.endWall))
                                else "Resume to keep counting.",
                                color = Palette.Muted,
                                fontSize = 12.5.sp
                            )
                        }
                    }
                }
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        GhostButton(
                            "+1 min",
                            onClick = { controller.addMinute() },
                            modifier = Modifier.weight(1f).height(54.dp)
                        )
                        if (state.mode == Mode.RUNNING) {
                            PrimaryButton(
                                "Pause",
                                onClick = { controller.pause() },
                                modifier = Modifier.weight(1f)
                            )
                        } else {
                            PrimaryButton(
                                "Resume",
                                onClick = { controller.resume() },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
                item {
                    GhostButton(
                        "Cancel timer",
                        onClick = { controller.cancel() },
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    )
                }
            }

            Mode.RINGING -> {
                item {
                    Surface(
                        shape = RoundedCornerShape(28.dp),
                        color = Palette.AccentSoft,
                        border = BorderStroke(1.dp, Palette.AccentDim.copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            Modifier.fillMaxWidth().padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            SectionLabel("Time's up")
                            Text(
                                "00:00",
                                color = Palette.Accent,
                                fontSize = 46.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Light
                            )
                            if (ringingId == TimerController.TIMER_ID) {
                                PrimaryButton(
                                    "Dismiss",
                                    onClick = { AlarmService.dismiss(ctx) },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            } else {
                                Text(
                                    "Waiting for the ringing screen…",
                                    color = Palette.Muted,
                                    fontSize = 13.sp,
                                    textAlign = TextAlign.Center
                                )
                                // Escape hatch: the app was force-stopped while ringing, so the
                                // service is gone for good. Clearing the state is safe even in the
                                // split second before a normal ring starts (the service treats a
                                // vanished timer as a phantom trigger and stops).
                                GhostButton(
                                    "Reset timer",
                                    onClick = { controller.onRingDismissed() },
                                    modifier = Modifier.fillMaxWidth().height(48.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        item { CuriosityFooter(Modifier.padding(top = 6.dp)) }
    }
}

/** Idle state: hour/minute/second fields, quick presets and an optional label. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DurationPicker(onStart: (Long, String) -> Unit) {
    var hours by rememberSaveable { mutableStateOf("0") }
    var minutes by rememberSaveable { mutableStateOf("5") }
    var seconds by rememberSaveable { mutableStateOf("0") }
    var label by rememberSaveable { mutableStateOf("") }

    fun sanitized(v: String, max: Int): String {
        val digits = v.filter { it.isDigit() }.take(2)
        val n = digits.toIntOrNull() ?: return if (digits.isEmpty()) "" else digits
        return n.coerceAtMost(max).toString()
    }
    val total = ((hours.toIntOrNull() ?: 0) * 3600L +
        (minutes.toIntOrNull() ?: 0) * 60L +
        (seconds.toIntOrNull() ?: 0)) * 1000L

    Surface(
        shape = RoundedCornerShape(28.dp),
        color = Palette.Surface,
        border = BorderStroke(1.dp, Palette.Outline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            SectionLabel("Duration")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                DurationField(hours, { hours = sanitized(it, 23) }, "Hours", Modifier.weight(1f))
                DurationField(minutes, { minutes = sanitized(it, 59) }, "Min", Modifier.weight(1f))
                DurationField(seconds, { seconds = sanitized(it, 59) }, "Sec", Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(1, 5, 10).forEach { m ->
                    FilterChip(
                        selected = false,
                        onClick = { hours = "0"; minutes = m.toString(); seconds = "0" },
                        label = { Text("$m min") },
                        colors = curioChipColors(),
                        border = BorderStroke(1.dp, Palette.Outline2)
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = false,
                    onClick = { hours = "0"; minutes = "15"; seconds = "0" },
                    label = { Text("15 min") },
                    colors = curioChipColors(),
                    border = BorderStroke(1.dp, Palette.Outline2)
                )
                FilterChip(
                    selected = false,
                    onClick = { hours = "0"; minutes = "30"; seconds = "0" },
                    label = { Text("30 min") },
                    colors = curioChipColors(),
                    border = BorderStroke(1.dp, Palette.Outline2)
                )
                FilterChip(
                    selected = false,
                    onClick = { hours = "1"; minutes = "0"; seconds = "0" },
                    label = { Text("1 h") },
                    colors = curioChipColors(),
                    border = BorderStroke(1.dp, Palette.Outline2)
                )
            }
            OutlinedTextField(
                value = label,
                onValueChange = { label = it.take(40) },
                label = { Text("Label (optional)") },
                singleLine = true,
                colors = curioFieldColors(),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )
            PrimaryButton(
                if (total > 0) "Start · ${Fmt.countdown(total)}" else "Start",
                onClick = { onStart(total, label) },
                enabled = total > 0,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun DurationField(value: String, onChange: (String) -> Unit, label: String, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label, fontSize = 11.sp) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        textStyle = androidx.compose.ui.text.TextStyle(
            fontSize = 24.sp,
            fontFamily = FontFamily.Monospace,
            textAlign = TextAlign.Center,
            color = Palette.Text
        ),
        colors = curioFieldColors(),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
    )
}

/** Gold arc shrinking with the remaining fraction; full hairline circle behind it. */
@Composable
private fun CountdownArc(fraction: Float, paused: Boolean) {
    val track = Palette.Outline
    val gold = if (paused) Palette.Muted else Palette.Accent
    Canvas(Modifier.fillMaxSize()) {
        val stroke = 7.dp.toPx()
        val inset = stroke / 2f
        val arcSize = Size(size.width - stroke, size.height - stroke)
        drawArc(
            color = track,
            startAngle = -90f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = Offset(inset, inset),
            size = arcSize,
            style = Stroke(width = 2.dp.toPx())
        )
        drawArc(
            color = gold,
            startAngle = -90f,
            sweepAngle = (fraction.coerceIn(0f, 1f) * 360f),
            useCenter = false,
            topLeft = Offset(inset, inset),
            size = arcSize,
            style = Stroke(width = stroke, cap = StrokeCap.Round)
        )
    }
}
