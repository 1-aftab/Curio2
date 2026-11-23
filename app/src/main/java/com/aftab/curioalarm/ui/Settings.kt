package com.aftab.curioalarm.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlinx.coroutines.delay

/** Settings section. Appearance now; a natural home for future preferences. */
@Composable
fun SettingsScreen(
    themeMode: ThemeMode,
    onThemeChange: (ThemeMode) -> Unit,
    onShowTour: () -> Unit,
    onReviewPermissions: () -> Unit,
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

        item { SectionLabel("Setup", Modifier.padding(top = 8.dp)) }

        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Palette.Surface,
                border = BorderStroke(1.dp, Palette.Outline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
                    TextButton(onClick = onReviewPermissions, modifier = Modifier.fillMaxWidth()) {
                        Text("Review permissions", color = Palette.Text, modifier = Modifier.fillMaxWidth())
                    }
                    TextButton(onClick = onShowTour, modifier = Modifier.fillMaxWidth()) {
                        Text("Show the tour", color = Palette.Text, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        }

        item { SectionLabel("About", Modifier.padding(top = 8.dp)) }

        item { AboutCard() }

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

private enum class AboutTab(val label: String, val body: String) {
    LORE(
        "the lore",
        "ok so. most alarms are a suggestion. you slap the side button, roll over, and the morning is cooked before it starts. curio does not do that. it stays up. it asks you to use your actual brain before it lets you go.\n\nnot a sleep score. not a pastel sunrise. not a streak app pretending to be your friend. a door that only opens if you're awake. the black and white thing is on purpose. color was doing too much, and a beige gradient has never gotten anyone out of bed."
    ),
    RULES(
        "the rules",
        "power button brings the screen back. home doesn't dismiss it. swipe the notification and it comes back anyway. the only real exit is the challenge.\n\nsnooze is limited on purpose, and kiosk mode doesn't snooze at all. timers get a tap. alarms don't. quick alarms from home follow the same rules as the ones you schedule. if you wanted easy, this was the wrong download. if something feels broken, say version 1.3. that's the receipt."
    ),
    BUILDER(
        "the builder",
        "aftab. one person. no team, no growth hack, no 'we value your privacy' page that actually means the opposite. permissions are for ringing. that's the whole list. no account, no ads, no tracking.\n\nbuilt this because being late to your own life is embarrassing, and because a normal alarm is too easy to silence half-asleep. the challenge is the product. if you found this tab, you're already more awake than the alarm expected."
    )
}

@Composable
private fun AboutCard() {
    val ctx = LocalContext.current
    val clock by produceState(LocalTime.now()) {
        while (true) {
            delay(1_000)
            value = LocalTime.now()
        }
    }
    var tab by remember { mutableStateOf(AboutTab.LORE) }
    var taps by remember { mutableIntStateOf(0) }
    var line by remember { mutableIntStateOf(0) }
    val hooks = listOf(
        "not a vibe alarm. a menace alarm.",
        "your morning's problem, on purpose.",
        "if you can silence it half-asleep, it failed.",
        "tap the pills. they talk back."
    )
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Palette.Surface,
        border = BorderStroke(1.dp, Palette.Outline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(20.dp)) {
            Text(
                DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).format(clock),
                color = Palette.AccentDim,
                fontSize = 12.sp,
                letterSpacing = 1.2.sp
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "curio",
                color = Palette.Text,
                fontSize = 32.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.8).sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { line = (line + 1) % hooks.size }
            )
            Text(
                hooks[line],
                color = Palette.Muted,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
            Text("version 1.3", color = Palette.Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))

            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AboutTab.entries.forEach { item ->
                    val on = item == tab
                    Surface(
                        shape = CircleShape,
                        color = if (on) Palette.Accent else Palette.SurfaceHigh,
                        border = BorderStroke(1.dp, if (on) Palette.Accent else Palette.Outline2),
                        modifier = Modifier
                            .weight(1f)
                            .clip(CircleShape)
                            .clickable { tab = item }
                    ) {
                        Text(
                            item.label,
                            color = if (on) Palette.OnAccent else Palette.Muted,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 6.dp, vertical = 7.dp)
                        )
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            AnimatedContent(
                targetState = tab,
                transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(120)) },
                label = "about"
            ) { page ->
                Text(
                    page.body,
                    color = Palette.Text,
                    fontSize = 14.sp,
                    lineHeight = 21.sp
                )
            }

            Spacer(Modifier.height(16.dp))
            Text(
                if (taps >= 5) "ok you found it. still not letting you skip the challenge."
                else "aftab",
                color = Palette.Text,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable { taps++ }
            )
            Text(
                if (taps in 1..4) "tap that ${5 - taps} more time${if (5 - taps == 1) "" else "s"}."
                else "made this because snoozing through life is a skill issue.",
                color = Palette.Muted,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 2.dp)
            )
            if (OnboardingPrefs.hasConsent(ctx)) {
                Spacer(Modifier.height(12.dp))
                Text(
                    "you already said the permissions are only for ringing. we didn't forget. we also didn't sell that sentence to anyone.",
                    color = Palette.Muted,
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )
            }
            Text(
                "tap curio if the line gets boring.",
                color = Palette.Outline2,
                fontSize = 11.sp,
                textAlign = TextAlign.Start,
                modifier = Modifier.padding(top = 14.dp)
            )
        }
    }
}
