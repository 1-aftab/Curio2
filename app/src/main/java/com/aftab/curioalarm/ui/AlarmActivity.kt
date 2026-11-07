package com.aftab.curioalarm.ui

import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aftab.curioalarm.Graph
import com.aftab.curioalarm.alarm.AlarmService
import com.aftab.curioalarm.challenge.ChallengeEngine
import com.aftab.curioalarm.challenge.Puzzle
import com.aftab.curioalarm.data.Alarm
import com.aftab.curioalarm.data.ChallengeType
import com.aftab.curioalarm.data.Difficulty
import com.aftab.curioalarm.tools.TimerController
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlinx.coroutines.delay

/** Full-screen ringing UI. The alarm only stops after the challenge is solved. */
class AlarmActivity : ComponentActivity() {

    private var lockTaskStarted = false

    /**
     * Kiosk mode only. Asks Android to pin this screen. On an ordinary phone this is screen
     * pinning: it only works if "App pinning" is enabled in Settings, and Android may ask the
     * user to confirm. If the app was allowlisted by a device owner, it locks without a prompt.
     * If neither applies, nothing happens. Users can always exit pinning with the system gesture.
     */
    private fun tryLockTask() {
        if (lockTaskStarted) return
        try {
            startLockTask()
            lockTaskStarted = true
        } catch (_: Exception) {
        }
    }

    /** Releases the pin as soon as the alarm is over (safe to call more than once). */
    private fun releaseLockTask() {
        if (lockTaskStarted) {
            runCatching { stopLockTask() }
            lockTaskStarted = false
        }
    }

    override fun onDestroy() {
        releaseLockTask()
        super.onDestroy()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        enableEdgeToEdge()

        if (AlarmService.ringing.value == null) {
            finish()
            return
        }

        val themeMode = ThemePrefs.load(this)
        setContent {
            CurioTheme(themeMode) {
                val ringingId by AlarmService.ringing.collectAsState()
                // React to every transition: dismissed → finish; next queued alarm → new
                // challenge; kiosk flag of the *current* alarm decides pinning.
                LaunchedEffect(ringingId) {
                    val id = ringingId
                    if (id == null) {
                        releaseLockTask()
                        finish()
                    } else if (resolveAlarm(id)?.hardMode == true) {
                        tryLockTask()
                    } else {
                        releaseLockTask()
                    }
                }
                ringingId?.let { id ->
                    key(id) {
                        RingScreen(
                            alarm = resolveAlarm(id),
                            onSolved = { AlarmService.dismiss(applicationContext) }
                        )
                    }
                }
            }
        }
    }
}

/** Stored alarm, or the synthetic timer alarm for the reserved timer id. */
private fun resolveAlarm(id: Int): Alarm? =
    if (id == TimerController.TIMER_ID) Graph.timer.ringingAlarm() else Graph.repo.get(id)

@Composable
private fun RingScreen(alarm: Alarm?, onSolved: () -> Unit) {
    val ctx = LocalContext.current
    val isTimer = alarm != null && alarm.challenges.isEmpty()
    val types = alarm?.challenges?.takeIf { it.isNotEmpty() } ?: setOf(ChallengeType.MATH)
    val difficulty = alarm?.difficulty ?: Difficulty.MEDIUM

    // Picked once per occurrence and survives recreation.
    var typeName by rememberSaveable { mutableStateOf(ChallengeEngine.pick(types).name) }
    var round by rememberSaveable { mutableIntStateOf(0) }
    val type = ChallengeType.valueOf(typeName)
    val puzzle = remember(typeName, round) { ChallengeEngine.generate(ctx, type, difficulty, round) }

    // Block the back button: the only way out is solving the challenge.
    BackHandler(enabled = true) { }

    val clock by produceState(LocalTime.now()) {
        while (true) {
            delay(1_000)
            value = LocalTime.now()
        }
    }

    Surface(color = Palette.Ink, modifier = Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(Modifier.size(220.dp), contentAlignment = Alignment.Center) {
                PulseRings()
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).format(clock),
                        color = Palette.Text,
                        fontSize = 38.sp,
                        fontWeight = FontWeight.Light
                    )
                    Text(
                        alarm?.label?.ifBlank { null } ?: "Alarm",
                        color = Palette.AccentDim,
                        fontSize = 14.sp,
                        letterSpacing = 0.4.sp
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (isTimer) {
                    MetaChip("Timer")
                } else {
                    MetaChip(type.title)
                    MetaChip(difficulty.title)
                    if (round > 0) MetaChip("Attempt ${round + 1}")
                }
            }
            Spacer(Modifier.height(16.dp))
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Palette.Surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, Palette.Outline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(Modifier.padding(24.dp)) {
                    if (isTimer) {
                        TimerDoneView(onDismiss = onSolved)
                    } else {
                        when (puzzle) {
                            is Puzzle.Math -> MathChallenge(puzzle, onSolved)
                            is Puzzle.Phrase -> PhraseChallenge(puzzle, onSolved)
                            is Puzzle.Memory -> MemoryChallenge(puzzle, onSolved, onWrong = { round++ })
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Text(
                if (isTimer) "Tap dismiss to stop the ringing."
                else "The alarm will keep ringing until the challenge is complete.",
                color = Palette.Muted,
                fontSize = 12.sp
            )
        }
    }
}

/** Timers need no challenge — just an explicit dismiss. */
@Composable
private fun TimerDoneView(onDismiss: () -> Unit) {
    Column(
        Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        SectionLabel("Time's up")
        Text(
            "00:00",
            color = Palette.Accent,
            fontSize = 44.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Light
        )
        Text("Your countdown reached zero.", color = Palette.Muted, fontSize = 13.sp)
        PrimaryButton("Dismiss", onClick = onDismiss, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun PulseRings() {
    val transition = rememberInfiniteTransition(label = "pulse")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2600, easing = LinearEasing), RepeatMode.Restart),
        label = "t"
    )
    val accent = Palette.Accent
    Canvas(Modifier.fillMaxSize()) {
        for (k in 0..1) {
            val p = (t + k * 0.5f) % 1f
            drawCircle(
                color = accent.copy(alpha = (1f - p) * 0.35f),
                radius = size.minDimension / 2f * (0.55f + 0.45f * p),
                style = Stroke(width = 1.5.dp.toPx())
            )
        }
    }
}
