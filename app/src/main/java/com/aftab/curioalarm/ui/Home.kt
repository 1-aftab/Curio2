package com.aftab.curioalarm.ui

import android.text.format.DateFormat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aftab.curioalarm.Graph
import com.aftab.curioalarm.data.Alarm
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.delay

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
        containerColor = Palette.Ink,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAdd,
                containerColor = Palette.Accent,
                contentColor = Palette.Ink,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("New alarm", fontWeight = FontWeight.SemiBold, letterSpacing = 0.3.sp) }
            )
        },
        bottomBar = { CuriosityFooter() }
    ) { inner ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 20.dp,
                end = 20.dp,
                top = inner.calculateTopPadding() + 24.dp,
                bottom = inner.calculateBottomPadding() + 96.dp
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item { Greeting() }
            item { NextAlarmHero(alarms, is24) }
            if (!perms.allGood) item { PermissionCard(perms, onFix) }
            if (alarms.isEmpty()) {
                item { EmptyState(onRestorePresets) }
            } else {
                item {
                    Row(
                        Modifier.fillMaxWidth().padding(top = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SectionLabel("Your alarms", Modifier.weight(1f))
                        Text(
                            "${alarms.count { it.enabled }} on",
                            color = Palette.Muted,
                            fontSize = 11.sp,
                            letterSpacing = 0.4.sp
                        )
                    }
                }
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
}

// ---------------------------------------------------------------- header

@Composable
private fun Greeting() {
    val now by produceState(LocalDateTime.now()) {
        while (true) {
            delay(30_000)
            value = LocalDateTime.now()
        }
    }
    val greeting = when (now.hour) {
        in 5..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        in 17..20 -> "Good evening"
        else -> "Good night"
    }
    val dateFmt = remember { DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.getDefault()) }
    Column(Modifier.padding(bottom = 4.dp)) {
        Text(
            "CURIO",
            color = Palette.AccentDim,
            fontSize = 12.sp,
            letterSpacing = 4.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(10.dp))
        Text(
            greeting,
            color = Palette.Text,
            fontSize = 34.sp,
            fontFamily = FontFamily.Serif,
            letterSpacing = (-0.3).sp
        )
        Spacer(Modifier.height(4.dp))
        Text(dateFmt.format(now), color = Palette.Muted, fontSize = 13.sp, letterSpacing = 0.3.sp)
    }
}

@Composable
private fun NextAlarmHero(alarms: List<Alarm>, is24: Boolean) {
    val now by produceState(System.currentTimeMillis()) {
        while (true) {
            delay(15_000)
            value = System.currentTimeMillis()
        }
    }
    val next = remember(alarms, now) {
        alarms.filter { it.enabled }
            .map { it to Graph.scheduler.nextTrigger(it).toInstant().toEpochMilli() }
            .minByOrNull { it.second }
    }
    val shape = RoundedCornerShape(28.dp)
    if (next == null) {
        Surface(
            shape = shape,
            color = Palette.Surface,
            border = BorderStroke(1.dp, Palette.Outline),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                Modifier.padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Icon(Icons.Default.Notifications, contentDescription = null, tint = Palette.Muted)
                Column {
                    Text("No alarms scheduled", color = Palette.Text, fontSize = 15.sp)
                    Text(
                        "Turn one on below, or create a new alarm.",
                        color = Palette.Muted,
                        fontSize = 12.sp
                    )
                }
            }
        }
    } else {
        val (alarm, at) = next
        val (time, suffix) = Fmt.time(alarm.hour, alarm.minute, is24)
        Surface(
            shape = shape,
            color = Palette.AccentSoft,
            border = BorderStroke(1.dp, Palette.AccentDim.copy(alpha = 0.35f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(22.dp)) {
                Text(
                    "NEXT ALARM",
                    color = Palette.AccentDim,
                    fontSize = 10.5.sp,
                    letterSpacing = 2.2.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "in " + Fmt.untilText(at - now),
                    color = Palette.Accent,
                    fontSize = 34.sp,
                    fontFamily = FontFamily.Serif,
                    letterSpacing = (-0.3).sp
                )
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        time + if (suffix.isEmpty()) "" else " $suffix",
                        color = Palette.Text,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        "  ·  " + alarm.label.ifBlank { "Alarm" } + "  ·  " + Fmt.days(alarm.days),
                        color = Palette.Muted,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------- alarm card

@Composable
private fun AlarmCard(alarm: Alarm, is24: Boolean, onClick: () -> Unit, onToggle: (Boolean) -> Unit) {
    val contentAlpha by animateFloatAsState(if (alarm.enabled) 1f else 0.45f, tween(300), label = "alpha")
    val (time, suffix) = Fmt.time(alarm.hour, alarm.minute, is24)
    val shape = RoundedCornerShape(26.dp)
    Surface(
        shape = shape,
        color = Palette.Surface,
        border = BorderStroke(1.dp, if (alarm.enabled) Palette.Outline else Palette.Outline.copy(alpha = 0.6f)),
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .clickable(onClick = onClick)
    ) {
        Column(Modifier.padding(start = 22.dp, end = 18.dp, top = 18.dp, bottom = 18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Row(
                    Modifier.weight(1f).alpha(contentAlpha),
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        time,
                        color = Palette.Text,
                        fontSize = 40.sp,
                        fontWeight = FontWeight.Light,
                        letterSpacing = (-1).sp
                    )
                    if (suffix.isNotEmpty()) {
                        Text(
                            suffix,
                            color = Palette.Muted,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(start = 6.dp, bottom = 7.dp)
                        )
                    }
                }
                Switch(checked = alarm.enabled, onCheckedChange = onToggle, colors = curioSwitchColors())
            }
            Text(
                alarm.label.ifBlank { "Alarm" },
                color = Palette.Muted,
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp,
                letterSpacing = 0.1.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.alpha(contentAlpha)
            )
            Spacer(Modifier.height(12.dp))
            DayStrip(alarm.days, Modifier.alpha(contentAlpha))
            Spacer(Modifier.height(12.dp))
            Row(
                Modifier.alpha(contentAlpha),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                MetaChip(Fmt.challengeShort(alarm))
                MetaChip(alarm.soundName ?: "Default tone", Modifier.weight(1f, fill = false))
                if (alarm.hardMode) MetaChip("Kiosk", icon = Icons.Default.Lock)
            }
        }
    }
}

@Composable
private fun DayStrip(days: Set<Int>, modifier: Modifier = Modifier) {
    if (days.isEmpty()) {
        MetaChip("Rings once", modifier)
        return
    }
    val letters = listOf("M", "T", "W", "T", "F", "S", "S")
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        for (day in 1..7) {
            val on = day in days
            Box(
                Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(if (on) Palette.AccentSoft else Palette.Ink)
                    .border(
                        1.dp,
                        if (on) Palette.AccentDim.copy(alpha = 0.55f) else Palette.Outline,
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    letters[day - 1],
                    color = if (on) Palette.Accent else Palette.Muted.copy(alpha = 0.55f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

// ---------------------------------------------------------------- permissions

@Composable
private fun PermissionCard(perms: PermissionStatus, onFix: (PermissionKind) -> Unit) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val missing = perms.missing
    val shape = RoundedCornerShape(24.dp)
    Surface(
        shape = shape,
        color = Palette.Surface,
        border = BorderStroke(1.dp, Palette.Danger.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth().clip(shape).clickable { expanded = !expanded }
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = Palette.Danger,
                    modifier = Modifier.size(20.dp)
                )
                Column(Modifier.weight(1f).padding(start = 12.dp)) {
                    Text("Needs attention", color = Palette.Text, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                    Text(
                        if (missing.size == 1) "1 permission missing" else "${missing.size} permissions missing",
                        color = Palette.Muted,
                        fontSize = 12.sp
                    )
                }
                Icon(
                    if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = Palette.Muted
                )
            }
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(tween(220)) + fadeIn(tween(220)),
                exit = shrinkVertically(tween(180)) + fadeOut(tween(160))
            ) {
                Column(Modifier.padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    missing.forEach { kind ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f).padding(end = 12.dp)) {
                                Text(kind.title, color = Palette.Text, fontSize = 14.sp)
                                Text(kind.detail, color = Palette.Muted, fontSize = 12.sp)
                            }
                            GhostButton("Fix", onClick = { onFix(kind) })
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------- empty state

@Composable
private fun EmptyState(onRestorePresets: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            Modifier.size(72.dp).clip(CircleShape).background(Palette.AccentSoft),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Notifications,
                contentDescription = null,
                tint = Palette.Accent,
                modifier = Modifier.size(30.dp)
            )
        }
        Text("No alarms yet", color = Palette.Text, fontSize = 22.sp, fontFamily = FontFamily.Serif)
        Text(
            "Start from a preset, or create your own.",
            color = Palette.Muted,
            fontSize = 13.sp
        )
        GhostButton("Restore presets", onClick = onRestorePresets)
    }
}
