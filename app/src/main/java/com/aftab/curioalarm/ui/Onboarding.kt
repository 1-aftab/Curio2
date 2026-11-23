package com.aftab.curioalarm.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class IntroMode { FIRST_RUN, TOUR, PERMISSIONS }

/**
 * First launch: welcome, a real consent step, then a short tour.
 * Settings can reopen the tour or the permission list on their own.
 */
@Composable
fun WelcomeFlow(
    mode: IntroMode,
    perms: PermissionStatus,
    onRequestNotifications: () -> Unit,
    onOpenPermission: (PermissionKind) -> Unit,
    onFinished: () -> Unit
) {
    var step by rememberSaveable {
        mutableStateOf(
            when (mode) {
                IntroMode.FIRST_RUN -> Step.WELCOME.name
                IntroMode.TOUR -> Step.TOUR_ALARMS.name
                IntroMode.PERMISSIONS -> Step.PERMISSIONS.name
            }
        )
    }
    var agreed by rememberSaveable { mutableStateOf(false) }
    val current = Step.entries.firstOrNull { it.name == step } ?: Step.WELCOME

    fun go(next: Step) { step = next.name }

    fun back() {
        val prev = when (current) {
            Step.WELCOME -> null
            Step.CONSENT -> Step.WELCOME
            Step.PERMISSIONS -> if (mode == IntroMode.PERMISSIONS) null else Step.CONSENT
            Step.TOUR_ALARMS -> if (mode == IntroMode.TOUR) null else Step.CONSENT
            Step.TOUR_TOOLS -> Step.TOUR_ALARMS
            Step.TOUR_YOURS -> Step.TOUR_TOOLS
        }
        if (prev != null) go(prev)
        else if (mode != IntroMode.FIRST_RUN) onFinished()
    }

    BackHandler { back() }

    Surface(color = Palette.Ink, modifier = Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 20.dp)
        ) {
            Text(
                current.kicker,
                color = Palette.AccentDim,
                fontSize = 11.sp,
                letterSpacing = 1.8.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(18.dp))
            Text(
                current.title,
                color = Palette.Text,
                fontSize = 30.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.5).sp,
                lineHeight = 36.sp
            )
            Spacer(Modifier.height(12.dp))
            Text(
                current.body,
                color = Palette.Muted,
                fontSize = 15.sp,
                lineHeight = 22.sp
            )
            Spacer(Modifier.height(22.dp))

            when (current) {
                Step.WELCOME -> {
                    Point("A challenge stops the alarm. A button does not.")
                    Point("Timers and the stopwatch live in the bottom bar.")
                    Point("Sound, vibration, or both — chosen per alarm.")
                }
                Step.CONSENT -> {
                    PermissionKind.entries.forEach { kind ->
                        ConsentRow(kind.title, kind.detail)
                    }
                    Spacer(Modifier.height(8.dp))
                    AgreeRow(agreed) { agreed = it }
                }
                Step.PERMISSIONS -> {
                    PermissionKind.entries.forEach { kind ->
                        val ok = when (kind) {
                            PermissionKind.NOTIFICATIONS -> perms.notifications
                            PermissionKind.EXACT_ALARM -> perms.exactAlarm
                            PermissionKind.FULL_SCREEN -> perms.fullScreen
                            PermissionKind.BATTERY -> perms.battery
                        }
                        StatusRow(
                            title = kind.title,
                            ok = ok,
                            optional = kind == PermissionKind.BATTERY,
                            onAllow = {
                                if (kind == PermissionKind.NOTIFICATIONS) onRequestNotifications()
                                else onOpenPermission(kind)
                            }
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Battery is recommended, not required. You can change any of these later from Settings.",
                        color = Palette.Muted,
                        fontSize = 12.sp,
                        lineHeight = 17.sp
                    )
                }
                Step.TOUR_ALARMS -> {
                    Point("New alarm sits above the bar. It fires on the minute, app open or not.")
                    Point("Solve the task on the ring screen. The power button brings that screen back.")
                    Point("Snooze is off until you turn it on. Even then it is limited, and kiosk mode never snoozes.")
                }
                Step.TOUR_TOOLS -> {
                    Point("Stopwatch keeps laps, including while the app is in the background.")
                    Point("Timer rings through the same pipeline as an alarm, then stops with one tap.")
                    Point("Quick alarms on the home screen are one-shot, with a single 9-minute snooze.")
                }
                Step.TOUR_YOURS -> {
                    Point("In the editor: Both, Sound, or Vibrate.")
                    Point("Settings switches light, dark, or system, including the ring screen.")
                    Point("This tour can be opened again from Settings.")
                }
            }

            Spacer(Modifier.height(28.dp))

            when (current) {
                Step.WELCOME -> PrimaryButton("Continue", { go(Step.CONSENT) }, Modifier.fillMaxWidth())
                Step.CONSENT -> {
                    PrimaryButton(
                        "Set these up",
                        { go(Step.PERMISSIONS) },
                        Modifier.fillMaxWidth(),
                        enabled = agreed
                    )
                    TextButton(
                        onClick = { go(Step.TOUR_ALARMS) },
                        enabled = agreed,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Skip for now", color = if (agreed) Palette.Muted else Palette.Outline2) }
                }
                Step.PERMISSIONS -> PrimaryButton(
                    "Continue",
                    { if (mode == IntroMode.PERMISSIONS) onFinished() else go(Step.TOUR_ALARMS) },
                    Modifier.fillMaxWidth()
                )
                Step.TOUR_ALARMS -> {
                    PrimaryButton("Next", { go(Step.TOUR_TOOLS) }, Modifier.fillMaxWidth())
                    if (mode == IntroMode.TOUR) {
                        TextButton(onClick = onFinished, modifier = Modifier.fillMaxWidth()) {
                            Text("Close", color = Palette.Muted)
                        }
                    }
                }
                Step.TOUR_TOOLS -> PrimaryButton("Next", { go(Step.TOUR_YOURS) }, Modifier.fillMaxWidth())
                Step.TOUR_YOURS -> PrimaryButton(
                    if (mode == IntroMode.TOUR) "Done" else "Get started",
                    onFinished,
                    Modifier.fillMaxWidth()
                )
            }
        }
    }
}

private enum class Step(val kicker: String, val title: String, val body: String) {
    WELCOME(
        "CURIO",
        "Wake up on purpose.",
        "An alarm you can silence half-asleep is not an alarm. Curio rings until you solve a short challenge."
    ),
    CONSENT(
        "BEFORE ANYTHING IS ASKED",
        "These are only for ringing.",
        "Nothing here is used for an account, ads, or tracking. You can refuse any of them. Alarms are less reliable without the first three."
    ),
    PERMISSIONS(
        "SETUP",
        "Allow what you want.",
        "Each one opens the system screen. Curio does not grant them for you."
    ),
    TOUR_ALARMS(
        "1 OF 3",
        "Alarms",
        "The home screen is the list. The ring screen is the only way out."
    ),
    TOUR_TOOLS(
        "2 OF 3",
        "Stopwatch and timer",
        "The bottom bar switches sections. Nothing else moves."
    ),
    TOUR_YOURS(
        "3 OF 3",
        "Sound, or just vibration.",
        "Set it per alarm. The look of the app is in Settings."
    )
}

@Composable
private fun Point(text: String) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 7.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("—", color = Palette.AccentDim, fontSize = 14.sp)
        Text(text, color = Palette.Text, fontSize = 15.sp, lineHeight = 21.sp)
    }
}

@Composable
private fun ConsentRow(title: String, detail: String) {
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(title, color = Palette.Text, fontSize = 15.sp, fontWeight = FontWeight.Medium)
        Text(detail, color = Palette.Muted, fontSize = 13.sp, lineHeight = 18.sp)
    }
}

@Composable
private fun AgreeRow(agreed: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .toggleable(value = agreed, role = Role.Checkbox, onValueChange = onChange)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = if (agreed) Palette.Accent else Palette.Surface,
            border = BorderStroke(1.5.dp, if (agreed) Palette.Accent else Palette.Outline2),
            modifier = Modifier.size(22.dp)
        ) {
            if (agreed) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Surface(shape = CircleShape, color = Palette.OnAccent, modifier = Modifier.size(8.dp)) {}
                }
            }
        }
        Text(
            "I understand. These permissions are only so alarms can ring on time.",
            color = Palette.Text,
            fontSize = 14.sp,
            lineHeight = 19.sp,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun StatusRow(title: String, ok: Boolean, optional: Boolean, onAllow: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Palette.Surface,
        border = BorderStroke(1.dp, Palette.Outline),
        modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp)
    ) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, color = Palette.Text, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                Text(
                    when {
                        ok -> "Allowed"
                        optional -> "Recommended"
                        else -> "Not allowed yet"
                    },
                    color = Palette.Muted,
                    fontSize = 12.sp
                )
            }
            if (!ok) {
                TextButton(onClick = onAllow) { Text("Allow", color = Palette.Text) }
            }
        }
    }
}
