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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aftab.curioalarm.data.Alarm
import com.aftab.curioalarm.data.ChallengeType
import com.aftab.curioalarm.data.Difficulty
import com.aftab.curioalarm.data.SoundStore
import java.io.File
import kotlinx.coroutines.Dispatchers
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

    var label by remember { mutableStateOf(initial.label) }
    var days by remember { mutableStateOf(initial.days) }
    var challenges by remember { mutableStateOf(initial.challenges) }
    var difficulty by remember { mutableStateOf(initial.difficulty) }
    var vibrate by remember { mutableStateOf(initial.vibrate) }
    var gradual by remember { mutableStateOf(initial.gradualVolume) }
    var hardMode by remember { mutableStateOf(initial.hardMode) }
    var soundPath by remember { mutableStateOf(initial.soundPath) }
    var soundName by remember { mutableStateOf(initial.soundName) }
    var confirmDelete by remember { mutableStateOf(false) }
    var importing by remember { mutableStateOf(false) }

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

    Surface(color = Palette.Black, modifier = Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
        ) {
            // ---- top bar
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onClose) { Text("Cancel", color = Palette.Muted) }
                Text(
                    if (isNew) "New alarm" else "Edit alarm",
                    color = Palette.Text,
                    fontFamily = FontFamily.Serif,
                    fontSize = 20.sp,
                    modifier = Modifier.weight(1f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                TextButton(onClick = {
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
                            challenges = challenges,
                            difficulty = difficulty,
                            vibrate = vibrate,
                            gradualVolume = gradual,
                            hardMode = hardMode
                        )
                    )
                }) { Text("Save", color = Palette.Silver, fontWeight = FontWeight.Medium) }
            }

            // ---- time
            Box(Modifier.fillMaxWidth().padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
                TimePicker(state = timeState)
            }

            Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(28.dp)) {

                // ---- label
                OutlinedTextField(
                    value = label,
                    onValueChange = { if (it.length <= 40) label = it },
                    label = { Text("Label") },
                    singleLine = true,
                    shape = RoundedCornerShape(18.dp),
                    colors = curioFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )

                // ---- repeat
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SectionLabel("Repeat")
                    val names = listOf("M", "T", "W", "T", "F", "S", "S")
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        for (day in 1..7) {
                            val selected = day in days
                            val bg by animateColorAsState(
                                if (selected) Palette.Silver else Palette.Surface, label = "day"
                            )
                            Box(
                                Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(bg)
                                    .border(1.dp, if (selected) Palette.Silver else Palette.Outline2, CircleShape)
                                    .clickable { days = if (selected) days - day else days + day },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    names[day - 1],
                                    color = if (selected) Palette.Black else Palette.Muted,
                                    fontWeight = FontWeight.Medium
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
                        shape = RoundedCornerShape(20.dp),
                        color = Palette.Surface,
                        border = BorderStroke(1.dp, Palette.Outline),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                if (importing) "Importing…" else (soundName ?: "Default alarm tone"),
                                color = Palette.Text,
                                fontSize = 15.sp,
                                maxLines = 1
                            )
                            if (soundMissing) {
                                Text(
                                    "This file is unavailable. The default tone will play instead.",
                                    color = Palette.Muted,
                                    fontSize = 12.sp
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                GhostButton(
                                    "Choose file",
                                    onClick = { picker.launch(arrayOf("audio/*", "application/ogg")) },
                                    modifier = Modifier.weight(1f)
                                )
                                GhostButton(
                                    if (preview.playing) "Stop" else "Preview",
                                    onClick = {
                                        if (!preview.toggle(soundPath)) {
                                            Toast.makeText(ctx, "Can't play that sound", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            if (soundPath != null) {
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
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ChallengeType.values().forEach { type ->
                            FilterChip(
                                selected = type in challenges,
                                onClick = {
                                    val selected = type in challenges
                                    if (!(selected && challenges.size == 1)) {
                                        challenges = if (selected) challenges - type else challenges + type
                                    }
                                },
                                label = { Text(type.title) },
                                shape = RoundedCornerShape(14.dp),
                                colors = curioChipColors()
                            )
                        }
                    }
                    Text(
                        if (challenges.size == 1) challenges.first().blurb
                        else "One is picked at random each time the alarm rings",
                        color = Palette.Muted,
                        fontSize = 12.sp
                    )
                    SectionLabel("Difficulty", Modifier.padding(top = 8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Difficulty.values().forEach { d ->
                            FilterChip(
                                selected = d == difficulty,
                                onClick = { difficulty = d },
                                label = { Text(d.title) },
                                shape = RoundedCornerShape(14.dp),
                                colors = curioChipColors()
                            )
                        }
                    }
                }

                // ---- mode
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SectionLabel("Mode")
                    ToggleRow("Hard mode", hardMode) { hardMode = it }
                    Text(
                        if (hardMode) "Opens the task screen straight away and tries to lock the phone to it until the task is done"
                        else "Normal: the alarm rings with the task screen as usual",
                        color = Palette.Muted,
                        fontSize = 12.sp
                    )
                }

                if (!isNew) {
                    GhostButton("Delete alarm", onClick = { confirmDelete = true }, modifier = Modifier.fillMaxWidth())
                }
                Spacer(Modifier.height(40.dp))
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            containerColor = Palette.Surface,
            titleContentColor = Palette.Text,
            textContentColor = Palette.Muted,
            title = { Text("Delete this alarm?") },
            text = { Text("This can't be undone.") },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; preview.stop(); onDelete() }) {
                    Text("Delete", color = Palette.Text)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Keep", color = Palette.Muted) }
            }
        )
    }
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
