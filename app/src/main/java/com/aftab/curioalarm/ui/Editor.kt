package com.aftab.curioalarm.ui

import android.text.format.DateFormat
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aftab.curioalarm.Graph
import com.aftab.curioalarm.data.Alarm
import com.aftab.curioalarm.data.ChallengeType
import com.aftab.curioalarm.data.Difficulty
import com.aftab.curioalarm.data.SoundStore
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmEditor(
    initial: Alarm,
    isNew: Boolean,
    onSave: (Alarm) -> Unit,
    onDelete: () -> Unit,
    onClose: () -> Unit
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val is24 = remember { DateFormat.is24HourFormat(ctx) }
    val timeState = rememberTimePickerState(initial.hour, initial.minute, is24)

    // Saveable state: the editor survives process death while open.
    var label by rememberSaveable { mutableStateOf(initial.label) }
    var dayList by rememberSaveable { mutableStateOf(initial.days.sorted()) }
    var challengeNames by rememberSaveable { mutableStateOf(initial.challenges.map { it.name }) }
    var difficultyName by rememberSaveable { mutableStateOf(initial.difficulty.name) }
    var vibrate by rememberSaveable { mutableStateOf(initial.vibrate) }
    var gradual by rememberSaveable { mutableStateOf(initial.gradualVolume) }
    var kiosk by rememberSaveable { mutableStateOf(initial.hardMode) }
    var soundPath by rememberSaveable { mutableStateOf(initial.soundPath) }
    var soundName by rememberSaveable { mutableStateOf(initial.soundName) }
    var dialMode by rememberSaveable { mutableStateOf(true) }
    var confirmDelete by remember { mutableStateOf(false) }
    var importing by remember { mutableStateOf(false) }

    val days = dayList.toSet()
    val challenges = challengeNames.mapNotNull { n -> ChallengeType.entries.firstOrNull { it.name == n } }.toSet()
    val difficulty = Difficulty.entries.firstOrNull { it.name == difficultyName } ?: Difficulty.MEDIUM

    val preview = remember { SoundPreview(ctx) }
    DisposableEffect(Unit) { onDispose { preview.stop() } }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            importing = true
            preview.stop()
            scope.launch {
                val result = withContext(Dispatchers.IO) { SoundStore.importFrom(ctx, uri) }
                importing = false
                result.onSuccess {
                    soundPath = it.path
                    soundName = it.displayName
                }.onFailure {
                    Toast.makeText(ctx, it.message ?: "Could not use that file", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    val soundMissing = soundPath != null && !File(soundPath!!).exists()

    fun save() {
        preview.stop()
        onSave(
            initial.copy(
                hour = timeState.hour,
                minute = timeState.minute,
                label = label.trim(),
                enabled = true,
                days = days,
                soundPath = soundPath,
                soundName = soundName,
                challenges = challenges.ifEmpty { setOf(ChallengeType.MATH) },
                difficulty = difficulty,
                vibrate = vibrate,
                gradualVolume = gradual,
                hardMode = kiosk
            )
        )
    }

    Surface(color = Palette.Ink, modifier = Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
        ) {
            // ---- top bar
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onClose) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Close", tint = Palette.Muted)
                }
                Text(
                    if (isNew) "New alarm" else "Edit alarm",
                    color = Palette.Text,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 20.sp,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
                TextButton(onClick = { save() }, enabled = !importing) {
                    Text(
                        "Save",
                        color = if (importing) Palette.Muted else Palette.Accent,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // ---- time
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Palette.Surface,
                border = BorderStroke(1.dp, Palette.Outline),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp)
            ) {
                Column(
                    Modifier.padding(vertical = 20.dp, horizontal = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = dialMode,
                            onClick = { dialMode = true },
                            label = { Text("Dial") },
                            shape = RoundedCornerShape(12.dp),
                            colors = curioChipColors()
                        )
                        FilterChip(
                            selected = !dialMode,
                            onClick = { dialMode = false },
                            label = { Text("Keyboard") },
                            shape = RoundedCornerShape(12.dp),
                            colors = curioChipColors()
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    if (dialMode) TimePicker(state = timeState) else TimeInput(state = timeState)
                    Spacer(Modifier.height(12.dp))
                    RingsInHint(timeState.hour, timeState.minute, days)
                }
            }

            Column(
                Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(26.dp)
            ) {

                // ---- label
                OutlinedTextField(
                    value = label,
                    onValueChange = { if (it.length <= 40) label = it },
                    label = { Text("Label") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = curioFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )

                // ---- repeat
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SectionLabel("Repeat")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        RepeatPreset("Once", emptyList(), dayList) { dayList = it }
                        RepeatPreset("Weekdays", listOf(1, 2, 3, 4, 5), dayList) { dayList = it }
                        RepeatPreset("Weekends", listOf(6, 7), dayList) { dayList = it }
                        RepeatPreset("Daily", listOf(1, 2, 3, 4, 5, 6, 7), dayList) { dayList = it }
                    }
                    val names = listOf("M", "T", "W", "T", "F", "S", "S")
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        for (day in 1..7) {
                            val selected = day in days
                            val bg by animateColorAsState(
                                if (selected) Palette.Accent else Palette.Surface, label = "day"
                            )
                            Box(
                                Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(bg)
                                    .border(1.dp, if (selected) Palette.Accent else Palette.Outline2, CircleShape)
                                    .clickable {
                                        dayList = if (selected) dayList - day else (dayList + day).sorted()
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    names[day - 1],
                                    color = if (selected) Palette.OnAccent else Palette.Muted,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                    Text(
                        if (days.isEmpty()) "Rings once, then turns itself off" else "Repeats: " + Fmt.days(days),
                        color = Palette.Muted,
                        fontSize = 12.sp
                    )
                }

                // ---- sound
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SectionLabel("Sound")
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Palette.Surface,
                        border = BorderStroke(1.dp, Palette.Outline),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                GlyphBadge("♫")
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        if (importing) "Importing…" else (soundName ?: "Default alarm tone"),
                                        color = Palette.Text,
                                        fontSize = 15.sp,
                                        maxLines = 1
                                    )
                                    Text(
                                        when {
                                            importing -> "Copying into app storage"
                                            soundMissing -> "File unavailable — the default tone will play"
                                            soundPath != null -> "Custom sound"
                                            else -> "Built-in"
                                        },
                                        color = if (soundMissing) Palette.Danger else Palette.Muted,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                            if (importing) {
                                LinearProgressIndicator(
                                    modifier = Modifier.fillMaxWidth(),
                                    color = Palette.Accent,
                                    trackColor = Palette.SurfaceHigh
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                GhostButton(
                                    "Choose file",
                                    onClick = { picker.launch(arrayOf("audio/*", "application/ogg")) },
                                    modifier = Modifier.weight(1f),
                                    enabled = !importing
                                )
                                GhostButton(
                                    if (preview.playing) "Stop" else "Preview",
                                    onClick = {
                                        if (!preview.toggle(soundPath)) {
                                            Toast.makeText(ctx, "Can't play that sound", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    enabled = !importing
                                )
                            }
                            if (soundPath != null && !importing) {
                                TextButton(onClick = {
                                    preview.stop()
                                    soundPath = null
                                    soundName = null
                                }) { Text("Use default tone", color = Palette.Muted) }
                            }
                        }
                    }
                    ToggleRow("Vibrate", vibrate) { vibrate = it }
                    ToggleRow("Gradually raise volume", gradual) { gradual = it }
                }

                // ---- challenge
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SectionLabel("Dismissal challenge")
                    val glyphs = mapOf(
                        ChallengeType.MATH to "×",
                        ChallengeType.MEMORY to "◆",
                        ChallengeType.PHRASE to "Aa"
                    )
                    ChallengeType.entries.forEach { type ->
                        val selected = type in challenges
                        val border by animateColorAsState(
                            if (selected) Palette.AccentDim.copy(alpha = 0.6f) else Palette.Outline,
                            label = "challengeBorder"
                        )
                        val shape = RoundedCornerShape(14.dp)
                        Surface(
                            shape = shape,
                            color = if (selected) Palette.AccentSoft else Palette.Surface,
                            border = BorderStroke(1.dp, border),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(shape)
                                .clickable {
                                    challengeNames = if (selected) {
                                        if (challengeNames.size > 1) challengeNames - type.name else challengeNames
                                    } else {
                                        challengeNames + type.name
                                    }
                                }
                        ) {
                            Row(
                                Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                GlyphBadge(glyphs[type] ?: "?")
                                Column(Modifier.weight(1f)) {
                                    Text(type.title, color = Palette.Text, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                                    Text(type.blurb, color = Palette.Muted, fontSize = 12.sp)
                                }
                                Box(
                                    Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(if (selected) Palette.Accent else Palette.SurfaceHigh)
                                        .border(1.dp, if (selected) Palette.Accent else Palette.Outline2, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (selected) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = null,
                                            tint = Palette.OnAccent,
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                    Text(
                        if (challenges.size <= 1) "At least one challenge is always kept selected"
                        else "One of the ${challenges.size} is picked at random each time the alarm rings",
                        color = Palette.Muted,
                        fontSize = 12.sp
                    )
                    SectionLabel("Difficulty", Modifier.padding(top = 4.dp))
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        Difficulty.entries.forEachIndexed { index, d ->
                            SegmentedButton(
                                selected = d == difficulty,
                                onClick = { difficultyName = d.name },
                                shape = SegmentedButtonDefaults.itemShape(index = index, count = Difficulty.entries.size),
                                colors = SegmentedButtonDefaults.colors(
                                    activeContainerColor = Palette.Accent,
                                    activeContentColor = Palette.OnAccent,
                                    inactiveContainerColor = Palette.Surface,
                                    inactiveContentColor = Palette.Muted,
                                    activeBorderColor = Palette.Accent,
                                    inactiveBorderColor = Palette.Outline2
                                ),
                                icon = {}
                            ) { Text(d.title, fontSize = 13.sp) }
                        }
                    }
                }

                // ---- kiosk mode
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SectionLabel("Mode")
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Palette.Surface,
                        border = BorderStroke(1.dp, Palette.Outline),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = if (kiosk) Palette.Accent else Palette.Muted)
                            Column(Modifier.weight(1f)) {
                                Text("Kiosk mode", color = Palette.Text, fontSize = 15.sp)
                                Text(
                                    if (kiosk) "Opens the task screen immediately and pins it where Android allows"
                                    else "Normal: the alarm rings with the task screen as usual",
                                    color = Palette.Muted,
                                    fontSize = 12.sp
                                )
                            }
                            Switch(checked = kiosk, onCheckedChange = { kiosk = it }, colors = curioSwitchColors())
                        }
                    }
                }

                if (!isNew) {
                    androidx.compose.material3.OutlinedButton(
                        onClick = { confirmDelete = true },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Palette.Danger.copy(alpha = 0.45f)),
                        colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(contentColor = Palette.Danger)
                    ) { Text("Delete alarm") }
                }
                Spacer(Modifier.navigationBarsPadding().height(32.dp))
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            containerColor = Palette.SurfaceHigh,
            titleContentColor = Palette.Text,
            textContentColor = Palette.Muted,
            title = { Text("Delete this alarm?") },
            text = { Text("This can't be undone.") },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; preview.stop(); onDelete() }) {
                    Text("Delete", color = Palette.Danger)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Keep", color = Palette.Muted) }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RepeatPreset(title: String, preset: List<Int>, current: List<Int>, onPick: (List<Int>) -> Unit) {
    FilterChip(
        selected = current == preset,
        onClick = { onPick(preset) },
        label = { Text(title, fontSize = 12.sp) },
        shape = RoundedCornerShape(12.dp),
        colors = curioChipColors()
    )
}

@Composable
private fun RingsInHint(hour: Int, minute: Int, days: Set<Int>) {
    val now by produceState(System.currentTimeMillis()) {
        while (true) {
            delay(30_000)
            value = System.currentTimeMillis()
        }
    }
    val trigger = remember(hour, minute, days, now) {
        Graph.scheduler.nextTrigger(
            Alarm(hour = hour, minute = minute, days = days),
            ZonedDateTime.ofInstant(Instant.ofEpochMilli(now), ZoneId.systemDefault())
        ).toInstant().toEpochMilli()
    }
    Text(
        "Rings in " + Fmt.untilText(trigger - now),
        color = Palette.AccentDim,
        fontSize = 13.sp,
        letterSpacing = 0.3.sp
    )
}

@Composable
private fun ToggleRow(title: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, color = Palette.Text, fontSize = 15.sp, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange, colors = curioSwitchColors())
    }
}
