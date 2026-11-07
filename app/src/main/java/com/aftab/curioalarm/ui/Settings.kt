package com.aftab.curioalarm.ui

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Settings section. Appearance now; a natural home for future preferences. */
@Composable
fun SettingsScreen(
    themeMode: ThemeMode,
    onThemeChange: (ThemeMode) -> Unit,
    contentPadding: PaddingValues
) {
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
                    "Settings",
                    color = Palette.Text,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = (-0.5).sp
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Make it yours.",
                    color = Palette.Muted,
                    fontSize = 13.sp,
                    letterSpacing = 0.3.sp
                )
            }
        }

        item { SectionLabel("Appearance", Modifier.padding(top = 8.dp)) }

        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Palette.Surface,
                border = BorderStroke(1.dp, Palette.Outline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(8.dp)) {
                    ThemeMode.entries.forEach { mode ->
                        ThemeOptionRow(
                            mode = mode,
                            selected = mode == themeMode,
                            onSelect = { onThemeChange(mode) }
                        )
                    }
                }
            }
        }

        item {
            Text(
                "System follows your device's light/dark setting. The choice applies everywhere, including the ringing screen.",
                color = Palette.Muted,
                fontSize = 12.sp,
                lineHeight = 17.sp,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }

        item { SectionLabel("About", Modifier.padding(top = 8.dp)) }

        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Palette.Surface,
                border = BorderStroke(1.dp, Palette.Outline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Curio Alarm", color = Palette.Text, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                    Text("Version 1.0", color = Palette.Muted, fontSize = 12.sp)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "An alarm clock that wakes your brain, not just your hand. Alarms ring until you solve a challenge; timers and the stopwatch live one tap away.",
                        color = Palette.Muted,
                        fontSize = 12.5.sp,
                        lineHeight = 18.sp
                    )
                }
            }
        }

        item { CuriosityFooter(Modifier.padding(top = 6.dp)) }
    }
}

@Composable
private fun ThemeOptionRow(mode: ThemeMode, selected: Boolean, onSelect: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .selectable(selected = selected, role = Role.RadioButton, onClick = onSelect)
            .padding(horizontal = 12.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Minimal radio indicator drawn with surfaces — no Material color surprises.
        Surface(
            shape = CircleShape,
            color = if (selected) Palette.Accent else Palette.Surface,
            border = BorderStroke(1.5.dp, if (selected) Palette.Accent else Palette.Outline2),
            modifier = Modifier.size(22.dp)
        ) {
            if (selected) {
                Row(
                    Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Palette.OnAccent,
                        modifier = Modifier.size(8.dp)
                    ) {}
                }
            }
        }
        Column(Modifier.weight(1f)) {
            Text(mode.title, color = Palette.Text, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            Text(mode.subtitle, color = Palette.Muted, fontSize = 12.sp)
        }
    }
}
