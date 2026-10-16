package com.aftab.curioalarm.ui

import android.Manifest
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
        if (AlarmService.ringing.value != null) {
            startActivity(
                Intent(this, AlarmActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            )
        }
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

    // Notification permission (Android 13+). Opens settings if the user taps "Allow" after a denial.
    var openSettingsOnDeny by remember { mutableStateOf(false) }
    val notificationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        perms = Permissions.status(ctx)
        if (!granted && openSettingsOnDeny) Permissions.open(ctx, PermissionKind.NOTIFICATIONS)
        openSettingsOnDeny = false
    }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !perms.notifications) {
            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    var editorAlarm by remember { mutableStateOf<Alarm?>(null) }
    var editorVisible by remember { mutableStateOf(false) }

    fun openEditor(alarm: Alarm) {
        editorAlarm = alarm
        editorVisible = true
    }

    fun closeEditor() {
        editorVisible = false
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
            editorAlarm?.let { alarm ->
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
