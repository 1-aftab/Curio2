package com.aftab.curioalarm.ui

import android.text.format.DateFormat
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aftab.curioalarm.Graph
import com.aftab.curioalarm.data.Alarm
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    alarms: List<Alarm>,
    perms: PermissionStatus,
    onFix: (PermissionKind) -> Unit,
    onAdd: () -> Unit,
    onEdit: (Alarm) -> Unit,
    onToggle: (Alarm, Boolean) -> Unit,
    onRestorePresets: () -> Unit
) {
    val ctx = LocalContext.current
    val is24 = remember { DateFormat.is24HourFormat(ctx) }

    Scaffold(
        containerColor = Palette.Black,
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAdd,
                containerColor = Palette.Silver,
                contentColor = Palette.Black,
                shape = CircleShape
            ) { Icon(Icons.Default.Add, contentDescription = "Add alarm") }
        },
        bottomBar = { CuriosityFooter() }
    ) { inner ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 20.dp,
                end = 20.dp,
                top = inner.calculateTopPadding() + 28.dp,
                bottom = inner.calculateBottomPadding() + 96.dp
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item { Header(alarms) }
            if (!perms.allGood) item { PermissionCard(perms, onFix) }
            if (alarms.isEmpty()) item { EmptyState(onRestorePresets) }
            items(alarms, key = { it.id }) { alarm ->
                AlarmCard(
                    alarm = alarm,
                    is24 = is24,
                    onClick = { onEdit(alarm) },
                    onToggle = { onToggle(alarm, it) }
                )
            }
        }
    }
}

@Composable
private fun Header(alarms: List<Alarm>) {
    val now by produceState(System.currentTimeMillis()) {
        while (true) {
            delay(20_000)
            value = System.currentTimeMillis()
        }
    }
    val subtitle = remember(alarms, now) {
        val next = alarms.filter { it.enabled }
            .minOfOrNull { Graph.scheduler.nextTrigger(it).toInstant().toEpochMilli() }
        if (next == null) "All alarms are off" else {
            val mins = ((next - now).coerceAtLeast(0) / 60_000L) + 1
            val h = mins / 60
            val m = mins % 60
            when {
                h >= 24 -> "Next alarm in ${h / 24} d ${h % 24} h"
                h > 0 -> "Next alarm in $h h $m min"
                else -> "Next alarm in $m min"
            }
        }
    }
    Column(Modifier.padding(bottom = 8.dp)) {
        Text(
            "Alarms",
            color = Palette.Text,
            fontSize = 42.sp,
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Normal,
            letterSpacing = (-0.35).sp
        )
        Spacer(Modifier.height(4.dp))
        Text(subtitle, color = Palette.Muted, fontSize = 14.sp, letterSpacing = 0.3.sp)
    }
}

@Composable
private fun AlarmCard(alarm: Alarm, is24: Boolean, onClick: () -> Unit, onToggle: (Boolean) -> Unit) {
    val alpha by animateFloatAsState(if (alarm.enabled) 1f else 0.5f, tween(300), label = "alpha")
    val (time, suffix) = Fmt.time(alarm.hour, alarm.minute, is24)
    val shape = RoundedCornerShape(28.dp)
    Surface(
        shape = shape,
        color = Palette.Surface,
        border = BorderStroke(1.dp, Palette.Outline),
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .clickable(onClick = onClick)
            .animateContentSize()
    ) {
        Row(
            modifier = Modifier.padding(start = 24.dp, end = 20.dp, top = 20.dp, bottom = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f).alpha(alpha)) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(time, color = Palette.Text, fontSize = 48.sp, fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Light, letterSpacing = (-1.2).sp)
                    if (suffix.isNotEmpty()) {
                        Text(
                            suffix,
                            color = Palette.Muted,
                            fontSize = 15.sp,
                            modifier = Modifier.padding(start = 8.dp, bottom = 10.dp)
                        )
                    }
                }
                Text(
                    alarm.label.ifBlank { "Alarm" } + "  ·  " + Fmt.days(alarm.days),
                    color = Palette.SilverDim,
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp,
                    letterSpacing = 0.1.sp
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    Fmt.challengeSummary(alarm) + "  ·  " + (alarm.soundName ?: "Default tone"),
                    color = Palette.Muted,
                    fontFamily = FontFamily.SansSerif,
                    fontSize = 11.sp,
                    letterSpacing = 0.1.sp,
                    maxLines = 1
                )
            }
            Switch(checked = alarm.enabled, onCheckedChange = onToggle, colors = curioSwitchColors())
        }
    }
}

@Composable
private fun PermissionCard(perms: PermissionStatus, onFix: (PermissionKind) -> Unit) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = Palette.Surface,
        border = BorderStroke(1.dp, Palette.Outline2),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            SectionLabel("Needs attention")
            perms.missing.forEach { kind ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f).padding(end = 12.dp)) {
                        Text(kind.title, color = Palette.Text, fontSize = 15.sp)
                        Text(kind.detail, color = Palette.Muted, fontSize = 12.sp)
                    }
                    GhostButton("Allow", onClick = { onFix(kind) })
                }
            }
        }
    }
}

@Composable
private fun EmptyState(onRestorePresets: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("No alarms yet", color = Palette.Muted, fontSize = 16.sp)
        GhostButton("Restore Morning · Afternoon · Night", onClick = onRestorePresets)
    }
}
