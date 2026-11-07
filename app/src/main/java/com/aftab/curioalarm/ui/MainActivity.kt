package com.aftab.curioalarm.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aftab.curioalarm.alarm.AlarmService
import com.aftab.curioalarm.data.Alarm
import java.time.LocalTime

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        setContent { CurioTheme { CurioRoot() } }
    }

    override fun onResume() {
        super.onResume()
        // If an alarm is ringing, always bring the challenge screen back.
        if (AlarmService.ringing.value != null) openRingScreen()
    }

    private fun openRingScreen() {
        startActivity(
            Intent(this, AlarmActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        )
    }
}

@Composable
private fun CurioRoot(vm: AlarmViewModel = viewModel()) {
    val ctx = LocalContext.current
    val activity = ctx as ComponentActivity
    val alarms by vm.alarms.collectAsState()

    var perms by remember { mutableStateOf(Permissions.status(ctx)) }
    DisposableEffect(activity) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) perms = Permissions.status(ctx)
        }
        activity.lifecycle.addObserver(observer)
        onDispose { activity.lifecycle.removeObserver(observer) }
    }

    // Notification permission (Android 13+): ask once automatically; afterwards the
    // permission card on Home is the way in (no dialog spam on every launch).
    val prefs = remember { ctx.getSharedPreferences("curio_ui", Context.MODE_PRIVATE) }
    var openSettingsOnDeny by remember { mutableStateOf(false) }
    val notificationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        perms = Permissions.status(ctx)
        if (!granted && openSettingsOnDeny) Permissions.open(ctx, PermissionKind.NOTIFICATIONS)
        openSettingsOnDeny = false
    }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !perms.notifications &&
            !prefs.getBoolean("notif_asked", false)
        ) {
            prefs.edit().putBoolean("notif_asked", true).apply()
            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // If an alarm starts ringing while the app is open, jump straight to the challenge.
    val ringingId by AlarmService.ringing.collectAsState()
    LaunchedEffect(ringingId) {
        if (ringingId != null) {
            ctx.startActivity(
                Intent(ctx, AlarmActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            )
        }
    }

    // Editor state lives here so it survives process death:
    //  -1 = closed, 0 = new alarm, >0 = editing that stored alarm.
    var editorAlarmId by rememberSaveable { mutableIntStateOf(-1) }
    var editorSession by rememberSaveable { mutableIntStateOf(0) }
    var newHour by rememberSaveable { mutableIntStateOf(7) }
    var newMinute by rememberSaveable { mutableIntStateOf(0) }

    val editorInitial: Alarm? = when {
        editorAlarmId > 0 -> alarms.firstOrNull { it.id == editorAlarmId }
        editorAlarmId == 0 -> Alarm(hour = newHour, minute = newMinute)
        else -> null
    }
    val editorVisible = editorInitial != null

    // Keep the last shown alarm around so the exit animation animates the real content
    // instead of a blank surface.
    var lastShown by remember { mutableStateOf<Alarm?>(null) }
    LaunchedEffect(editorInitial) { if (editorInitial != null) lastShown = editorInitial }
    val displayAlarm = editorInitial ?: lastShown

    fun openEditor(alarm: Alarm) {
        if (alarm.id == 0) {
            newHour = alarm.hour
            newMinute = alarm.minute
            editorAlarmId = 0
        } else {
            editorAlarmId = alarm.id
        }
        editorSession++   // fresh editor state per opening, even for the same alarm
    }

    fun closeEditor() {
        editorAlarmId = -1
        vm.cleanupSounds()
    }

    BackHandler(enabled = editorVisible) { closeEditor() }

    Box(Modifier.fillMaxSize()) {
        HomeScreen(
            alarms = alarms,
            perms = perms,
            onFix = { kind ->
                if (kind == PermissionKind.NOTIFICATIONS && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    openSettingsOnDeny = true
                    notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    Permissions.open(ctx, kind)
                }
            },
            onAdd = {
                val t = LocalTime.now().plusHours(1)
                openEditor(Alarm(hour = t.hour, minute = 0))
            },
            onEdit = { openEditor(it) },
            onToggle = { alarm, on -> vm.toggle(alarm, on) },
            onRestorePresets = { vm.restorePresets() }
        )

        AnimatedVisibility(
            visible = editorVisible,
            enter = slideInVertically(tween(320)) { it / 8 } + fadeIn(tween(280)),
            exit = slideOutVertically(tween(240)) { it / 8 } + fadeOut(tween(200))
        ) {
            displayAlarm?.let { alarm ->
                // key(): every editor opening gets its own state — fields from a previously
                // edited alarm can never leak into the next one.
                key(editorSession) {
                    AlarmEditor(
                        initial = alarm,
                        isNew = alarm.id == 0,
                        onSave = { vm.save(it); closeEditor() },
                        onDelete = { vm.delete(alarm.id); closeEditor() },
                        onClose = { closeEditor() }
                    )
                }
            }
        }
    }
}
