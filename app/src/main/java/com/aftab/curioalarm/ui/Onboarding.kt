package com.aftab.curioalarm.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aftab.curioalarm.data.AlertMode
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlin.math.roundToInt
import kotlinx.coroutines.delay

enum class IntroMode { FIRST_RUN, TOUR, PERMISSIONS }

// Consent is the gate. Continuing or skipping without the checkbox stays put.
object OnboardingFlow {
    fun next(current: String, mode: IntroMode, agreed: Boolean): String {
        val step = runCatching { Step.valueOf(current) }.getOrDefault(Step.WELCOME)
        val next = when (step) {
            Step.WELCOME -> Step.CONSENT
            Step.CONSENT -> if (agreed) Step.PERMISSIONS else Step.CONSENT
            Step.PERMISSIONS -> if (mode == IntroMode.PERMISSIONS) null else Step.TOUR_ALARMS
            Step.TOUR_ALARMS -> Step.TOUR_TOOLS
            Step.TOUR_TOOLS -> Step.TOUR_YOURS
            Step.TOUR_YOURS -> null
        }
        return next?.name ?: current
    }

    fun skipConsent(agreed: Boolean): String =
        if (agreed) Step.TOUR_ALARMS.name else Step.CONSENT.name

    fun finishes(current: String, mode: IntroMode): Boolean = when (mode) {
        IntroMode.PERMISSIONS -> current == Step.PERMISSIONS.name
        else -> current == Step.TOUR_YOURS.name
    }
}

/**
 * First launch: welcome, a real consent step, then a short tour.
 * The screens are toys on purpose — you poke them, they answer.
 * Settings can reopen the tour or the permission list on their own.
 */
@Composable
fun WelcomeFlow(
    mode: IntroMode,
    perms: PermissionStatus,
    onRequestNotifications: () -> Unit,
    onOpenPermission: (PermissionKind) -> Unit,
    onFinished: () -> Unit,
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    onThemeChange: (ThemeMode) -> Unit = {}
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
    var nag by rememberSaveable { mutableIntStateOf(0) }
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
            StepDots(current, mode) { tapped ->
                val tour = tapped == Step.TOUR_ALARMS || tapped == Step.TOUR_TOOLS || tapped == Step.TOUR_YOURS
                val hereIsTour = current == Step.TOUR_ALARMS || current == Step.TOUR_TOOLS || current == Step.TOUR_YOURS
                if (tour && (hereIsTour || mode == IntroMode.TOUR)) go(tapped)
            }
            Spacer(Modifier.height(18.dp))
            Text(
                current.kicker,
                color = Palette.AccentDim,
                fontSize = 11.sp,
                letterSpacing = 1.8.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(14.dp))
            Text(
                current.title,
                color = Palette.Text,
                fontSize = 30.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.5).sp,
                lineHeight = 36.sp
            )
            Spacer(Modifier.height(10.dp))
            Text(
                current.body,
                color = Palette.Muted,
                fontSize = 15.sp,
                lineHeight = 22.sp
            )
            Spacer(Modifier.height(20.dp))

            when (current) {
                Step.WELCOME -> WelcomePlayground()
                Step.CONSENT -> ConsentPlayground(agreed, nag) { agreed = it }
                Step.PERMISSIONS -> PermissionPlayground(
                    perms = perms,
                    onRequestNotifications = onRequestNotifications,
                    onOpenPermission = onOpenPermission
                )
                Step.TOUR_ALARMS -> AlarmPlayground()
                Step.TOUR_TOOLS -> ToolsPlayground()
                Step.TOUR_YOURS -> YoursPlayground(themeMode, onThemeChange)
            }

            Spacer(Modifier.height(28.dp))

            when (current) {
                Step.WELCOME -> PrimaryButton(
                    "Continue",
                    { go(Step.valueOf(OnboardingFlow.next(current.name, mode, agreed))) },
                    Modifier.fillMaxWidth()
                )
                Step.CONSENT -> {
                    PrimaryButton(
                        "Set these up",
                        {
                            val next = OnboardingFlow.next(current.name, mode, agreed)
                            if (next == current.name) nag++ else go(Step.valueOf(next))
                        },
                        Modifier.fillMaxWidth()
                    )
                    TextButton(
                        onClick = {
                            val next = OnboardingFlow.skipConsent(agreed)
                            if (next == current.name) nag++ else go(Step.valueOf(next))
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            if (agreed) "Skip for now" else "Check the box first",
                            color = if (agreed) Palette.Muted else Palette.Danger
                        )
                    }
                }
                Step.PERMISSIONS -> PrimaryButton(
                    "Continue",
                    {
                        if (OnboardingFlow.finishes(current.name, mode)) onFinished()
                        else go(Step.valueOf(OnboardingFlow.next(current.name, mode, agreed)))
                    },
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
                    { if (OnboardingFlow.finishes(current.name, mode)) onFinished() },
                    Modifier.fillMaxWidth()
                )
            }
        }
    }
}

private enum class Step(val kicker: String, val title: String, val body: String) {
    WELCOME(
        "CURIO",
        "Try to turn it off.",
        "Tap the power button. Then solve the tiny problem. That's the whole app."
    ),
    CONSENT(
        "BEFORE ANYTHING IS ASKED",
        "Tap each one. Then agree.",
        "Nothing here is for an account, ads, or tracking. Open a card if you want the plain version."
    ),
    PERMISSIONS(
        "SETUP",
        "Flip the ones you want.",
        "Tap a card. It opens the system screen. Curio cannot grant these for you."
    ),
    TOUR_ALARMS(
        "1 OF 3",
        "Make an alarm exist.",
        "Toggle it. Then answer the question. Wrong answers don't get to leave."
    ),
    TOUR_TOOLS(
        "2 OF 3",
        "The other two tabs.",
        "Switch the preview. The stopwatch actually runs. The timer chips are a taste."
    ),
    TOUR_YOURS(
        "3 OF 3",
        "Make it yours. For real.",
        "These aren't pictures. The theme chips change the app right now."
    )
}

@Composable
private fun StepDots(current: Step, mode: IntroMode, onTour: (Step) -> Unit) {
    val steps = if (mode == IntroMode.PERMISSIONS) listOf(Step.PERMISSIONS)
    else if (mode == IntroMode.TOUR) listOf(Step.TOUR_ALARMS, Step.TOUR_TOOLS, Step.TOUR_YOURS)
    else Step.entries
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        steps.forEach { step ->
            val on = step == current
            val tour = step == Step.TOUR_ALARMS || step == Step.TOUR_TOOLS || step == Step.TOUR_YOURS
            Box(
                Modifier
                    .size(if (on) 22.dp else 8.dp, 8.dp)
                    .clip(CircleShape)
                    .background(if (on) Palette.Accent else Palette.Outline2)
                    .then(if (tour) Modifier.clickable { onTour(step) } else Modifier)
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            "${steps.indexOf(current).coerceAtLeast(0) + 1} / ${steps.size}",
            color = Palette.Muted,
            fontSize = 12.sp
        )
    }
}

@Composable
private fun WelcomePlayground() {
    val clock by produceState(LocalTime.now()) {
        while (true) {
            delay(1_000)
            value = LocalTime.now()
        }
    }
    var tries by rememberSaveable { mutableIntStateOf(0) }
    var solved by rememberSaveable { mutableStateOf(false) }
    var wrong by rememberSaveable { mutableIntStateOf(0) }
    val shake = rememberShake(tries + wrong)
    val roasts = listOf(
        "yeah. no.",
        "it came back.",
        "that's the point.",
        "you can keep tapping."
    )
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Palette.Surface,
            border = BorderStroke(1.dp, Palette.Outline),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).format(clock),
                    color = Palette.Text,
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Light
                )
                Text("still ringing", color = Palette.Muted, fontSize = 12.sp)
                Spacer(Modifier.height(16.dp))
                Surface(
                    shape = CircleShape,
                    color = Palette.Accent,
                    modifier = Modifier
                        .size(72.dp)
                        .offset { IntOffset(shake.value.roundToInt(), 0) }
                        .clip(CircleShape)
                        .clickable { tries++ }
                ) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("pwr", color = Palette.OnAccent, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    if (tries == 0) "tap it. go on." else roasts[(tries - 1) % roasts.size],
                    color = Palette.Text,
                    fontSize = 14.sp
                )
            }
        }
        Text("now actually stop it", color = Palette.Muted, fontSize = 12.sp, letterSpacing = 0.4.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(12, 14, 15).forEach { n ->
                val picked = solved && n == 14
                AnswerChip(
                    label = n.toString(),
                    selected = picked,
                    modifier = Modifier.weight(1f),
                    onClick = { if (n == 14) solved = true else wrong++ }
                )
            }
        }
        Text(
            if (solved) "8 + 6 is 14. you're up. that's the product."
            else if (wrong > 0) "wrong. the alarm would still be going."
            else "8 + 6. pick the one that lets you leave.",
            color = if (wrong > 0 && !solved) Palette.Danger else Palette.Muted,
            fontSize = 13.sp
        )
    }
}

@Composable
private fun ConsentPlayground(agreed: Boolean, nag: Int, onAgree: (Boolean) -> Unit) {
    var open by rememberSaveable { mutableStateOf(PermissionKind.NOTIFICATIONS.name) }
    val shake = rememberShake(nag)
    Column(
        Modifier.offset { IntOffset(if (agreed) 0 else shake.value.roundToInt(), 0) },
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        PermissionKind.entries.forEach { kind ->
            val expanded = open == kind.name
            val next = if (expanded) "" else kind.name
            val card = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).clickable(onClick = { open = next })
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (expanded) Palette.AccentSoft else Palette.Surface,
                border = BorderStroke(1.dp, if (expanded) Palette.AccentDim else Palette.Outline),
                modifier = card
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(kind.title, color = Palette.Text, fontSize = 15.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                        Text(if (expanded) "-" else "+", color = Palette.Muted, fontSize = 18.sp)
                    }
                    if (expanded) {
                        Spacer(Modifier.height(8.dp))
                        Text(kind.detail, color = Palette.Text, fontSize = 13.sp, lineHeight = 18.sp)
                        Spacer(Modifier.height(4.dp))
                        Text(plainVersion(kind), color = Palette.Muted, fontSize = 12.sp, lineHeight = 17.sp)
                    }
                }
            }
        }
        if (nag > 0 && !agreed) {
            Text("the circle. then you can move.", color = Palette.Danger, fontSize = 12.sp)
        }
        AgreeRow(agreed, onAgree)
    }
}

private fun plainVersion(kind: PermissionKind): String = when (kind) {
    PermissionKind.NOTIFICATIONS -> "Without this, the phone can hide the alarm. We don't send anything else."
    PermissionKind.EXACT_ALARM -> "Otherwise Android may ring you 'sometime soon'. That's not an alarm."
    PermissionKind.FULL_SCREEN -> "So the challenge can sit on the lock screen, not as a tiny banner you swipe."
    PermissionKind.BATTERY -> "Optional. Some phones kill alarms to save power. This tells them not to."
}

@Composable
private fun PermissionPlayground(
    perms: PermissionStatus,
    onRequestNotifications: () -> Unit,
    onOpenPermission: (PermissionKind) -> Unit
) {
    val rows = PermissionKind.entries.map { kind ->
        kind to when (kind) {
            PermissionKind.NOTIFICATIONS -> perms.notifications
            PermissionKind.EXACT_ALARM -> perms.exactAlarm
            PermissionKind.FULL_SCREEN -> perms.fullScreen
            PermissionKind.BATTERY -> perms.battery
        }
    }
    val on = rows.count { it.second }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "$on of ${rows.size} on",
            color = Palette.Text,
            fontSize = 22.sp,
            fontWeight = FontWeight.SemiBold
        )
        rows.forEach { (kind, ok) ->
            val border by animateColorAsState(if (ok) Palette.Accent else Palette.Outline, label = "perm")
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Palette.Surface,
                border = BorderStroke(1.dp, border),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .clickable {
                        if (!ok) {
                            if (kind == PermissionKind.NOTIFICATIONS) onRequestNotifications()
                            else onOpenPermission(kind)
                        }
                    }
            ) {
                Row(
                    Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(kind.title, color = Palette.Text, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                        Text(
                            when {
                                ok -> "Allowed. Tap won't nag."
                                kind == PermissionKind.BATTERY -> "Recommended. Tap to open."
                                else -> "Not yet. Tap to open."
                            },
                            color = Palette.Muted,
                            fontSize = 12.sp
                        )
                    }
                    Text(
                        if (ok) "On" else "Allow",
                        color = if (ok) Palette.Accent else Palette.Text,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun AlarmPlayground() {
    var enabled by rememberSaveable { mutableStateOf(true) }
    var solved by rememberSaveable { mutableStateOf(false) }
    var wrong by rememberSaveable { mutableIntStateOf(0) }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = Palette.Surface,
            border = BorderStroke(1.dp, Palette.Outline),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("7:00", color = Palette.Text, fontSize = 28.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        if (enabled) "Morning · on" else "off. it will not save you.",
                        color = Palette.Muted,
                        fontSize = 13.sp
                    )
                }
                Switch(checked = enabled, onCheckedChange = { enabled = it }, colors = curioSwitchColors())
            }
        }
        Text(
            if (!enabled) "Turn it back on. A dead alarm is just a clock."
            else if (solved) "Solved. That's the only real exit."
            else "It's ringing. 9 × 3.",
            color = Palette.Muted,
            fontSize = 13.sp
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(27, 24, 18).forEach { n ->
                AnswerChip(
                    label = n.toString(),
                    selected = solved && n == 27,
                    modifier = Modifier.weight(1f),
                    enabled = enabled && !solved
                ) {
                    if (n == 27) solved = true else wrong++
                }
            }
        }
        if (wrong > 0 && !solved) {
            Text("Nope. Attempt ${wrong + 1}.", color = Palette.Danger, fontSize = 12.sp)
        }
    }
}

@Composable
private fun ToolsPlayground() {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var running by rememberSaveable { mutableStateOf(false) }
    var elapsed by rememberSaveable { mutableLongStateOf(0L) }
    var laps by rememberSaveable { mutableIntStateOf(0) }
    var timerMin by rememberSaveable { mutableIntStateOf(15) }
    LaunchedEffect(running) {
        while (running) {
            delay(100)
            elapsed += 100
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Stopwatch", "Timer").forEachIndexed { i, label ->
                val on = tab == i
                Surface(
                    shape = CircleShape,
                    color = if (on) Palette.Accent else Palette.Surface,
                    border = BorderStroke(1.dp, if (on) Palette.Accent else Palette.Outline2),
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable { tab = i }
                ) {
                    Text(
                        label,
                        color = if (on) Palette.OnAccent else Palette.Muted,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }
            }
        }
        AnimatedContent(
            targetState = tab,
            transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(120)) },
            label = "tools"
        ) { page ->
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = Palette.Surface,
                border = BorderStroke(1.dp, Palette.Outline),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (page == 0) {
                    Column(Modifier.padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            Fmt.stopwatch(elapsed),
                            color = Palette.Text,
                            fontSize = 36.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Light
                        )
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            GhostButton(if (running) "Pause" else "Start", { running = !running }, Modifier.weight(1f))
                            GhostButton(
                                text = "Lap",
                                onClick = { if (running) laps++ },
                                modifier = Modifier.weight(1f),
                                enabled = running || laps > 0
                            )
                        }
                        if (laps > 0) {
                            Spacer(Modifier.height(8.dp))
                            Text("$laps lap${if (laps == 1) "" else "s"}", color = Palette.Muted, fontSize = 12.sp)
                        }
                    }
                } else {
                    Column(Modifier.padding(18.dp)) {
                        Text("Rings even if the app is closed.", color = Palette.Muted, fontSize = 13.sp)
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(5, 15, 30).forEach { m ->
                                AnswerChip(
                                    label = "$m min",
                                    selected = timerMin == m,
                                    modifier = Modifier.weight(1f),
                                    onClick = { timerMin = m }
                                )
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        Text(
                            "A $timerMin min timer stops with one tap. No challenge. Alarms do not get that mercy.",
                            color = Palette.Text,
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun YoursPlayground(themeMode: ThemeMode, onThemeChange: (ThemeMode) -> Unit) {
    var alert by rememberSaveable { mutableStateOf(AlertMode.SOUND_AND_VIBRATION.name) }
    val mode = AlertMode.parse(alert)
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Alert", color = Palette.Muted, fontSize = 12.sp, letterSpacing = 0.6.sp)
        AlertModePicker(mode) { alert = it.name }
        Text(reaction(mode), color = Palette.Text, fontSize = 14.sp)
        Text("Theme", color = Palette.Muted, fontSize = 12.sp, letterSpacing = 0.6.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ThemeMode.entries.forEach { theme ->
                AnswerChip(
                    theme.title,
                    theme == themeMode,
                    Modifier.weight(1f)
                ) { onThemeChange(theme) }
            }
        }
        Text(
            when (themeMode) {
                ThemeMode.SYSTEM -> "Follows the phone. The ring screen follows too."
                ThemeMode.LIGHT -> "White. Buttons go black. You just did that."
                ThemeMode.DARK -> "Black. Buttons go white. Same app, inverted."
            },
            color = Palette.Muted,
            fontSize = 13.sp,
            lineHeight = 18.sp
        )
    }
}

private fun reaction(mode: AlertMode): String = when (mode) {
    AlertMode.SOUND_AND_VIBRATION -> "Tone and vibration. No 'I didn't hear it'."
    AlertMode.SOUND -> "Tone only. The pocket stays still."
    AlertMode.VIBRATION -> "Silent. The table does the talking."
}

@Composable
private fun AnswerChip(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val bg by animateColorAsState(if (selected) Palette.Accent else Palette.Surface, label = "chip")
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = bg,
        border = BorderStroke(1.dp, if (selected) Palette.Accent else Palette.Outline2),
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = enabled, onClick = onClick)
    ) {
        Text(
            label,
            color = if (selected) Palette.OnAccent else Palette.Text,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
        )
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
            "I get it. These are only so the alarm can actually ring.",
            color = Palette.Text,
            fontSize = 14.sp,
            lineHeight = 19.sp,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun rememberShake(key: Int): Animatable<Float, androidx.compose.animation.core.AnimationVector1D> {
    val shake = remember { Animatable(0f) }
    LaunchedEffect(key) {
        if (key == 0) return@LaunchedEffect
        repeat(3) {
            shake.animateTo(10f, tween(40))
            shake.animateTo(-10f, tween(40))
        }
        shake.animateTo(0f, tween(40))
    }
    return shake
}
