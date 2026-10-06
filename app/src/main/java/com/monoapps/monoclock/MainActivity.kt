package com.monoapps.monoclock

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.AlarmClock
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.HourglassEmpty
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.monoapps.monoclock.data.Alarm
import com.monoapps.monoclock.ui.AlarmEditScreen
import com.monoapps.monoclock.ui.AlarmTab
import com.monoapps.monoclock.ui.StopwatchTab
import com.monoapps.monoclock.ui.TimerTab
import com.mudita.mmd.ThemeMMD
import com.mudita.mmd.components.nav_bar.NavigationBarItemMMD
import com.mudita.mmd.components.nav_bar.NavigationBarMMD
import com.mudita.mmd.components.text.TextMMD
import com.mudita.mmd.components.top_app_bar.TopAppBarMMD

enum class Tab(val title: String, val icon: ImageVector) {
    Alarms("Alarm", Icons.Outlined.Alarm),
    Timer("Timer", Icons.Outlined.HourglassEmpty),
    Stopwatch("Stopwatch", Icons.Outlined.Timer),
}

class MainActivity : ComponentActivity() {
    private val viewModel: ClockViewModel by viewModels()
    private val tabState = mutableStateOf(Tab.Alarms)
    private val editorState = mutableStateOf<AlarmEditor?>(null)

    /** Open alarm editor; [alarm] null means a new alarm. */
    data class AlarmEditor(val alarm: Alarm?)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleClockIntent(intent)
        setContent { ThemeMMD { App() } }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleClockIntent(intent)
    }

    /** The standard Android clock intents other apps use to set alarms/timers. */
    private fun handleClockIntent(intent: Intent?) {
        when (intent?.action) {
            AlarmClock.ACTION_SET_ALARM -> {
                tabState.value = Tab.Alarms
                if (intent.hasExtra(AlarmClock.EXTRA_HOUR)) {
                    val days = intent.getIntegerArrayListExtra(AlarmClock.EXTRA_DAYS)?.toSet()
                    viewModel.addAlarm(
                        hour = intent.getIntExtra(AlarmClock.EXTRA_HOUR, 7),
                        minute = intent.getIntExtra(AlarmClock.EXTRA_MINUTES, 0),
                        days = days ?: emptySet(),
                    )
                } else {
                    editorState.value = AlarmEditor(null)
                }
            }
            AlarmClock.ACTION_SET_TIMER -> {
                tabState.value = Tab.Timer
                if (intent.hasExtra(AlarmClock.EXTRA_LENGTH)) {
                    val seconds = intent.getIntExtra(AlarmClock.EXTRA_LENGTH, 0)
                    if (seconds > 0) {
                        viewModel.updateTimerDuration(seconds * 1000L)
                        viewModel.startTimer(seconds * 1000L)
                    }
                }
            }
            AlarmClock.ACTION_SHOW_ALARMS -> tabState.value = Tab.Alarms
            AlarmClock.ACTION_SHOW_TIMERS -> tabState.value = Tab.Timer
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun App() {
        var tab by tabState
        var editor by editorState

        // Physical back button: step back inside the app (editor, then the
        // main tab) before letting the system close it.
        BackHandler(enabled = editor != null || tab != Tab.Alarms) {
            when {
                editor != null -> editor = null
                else -> tab = Tab.Alarms
            }
        }

        // Alarms can ring and reschedule while the app is open or backgrounded.
        val lifecycleOwner = LocalLifecycleOwner.current
        DisposableEffect(lifecycleOwner) {
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) viewModel.refresh()
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
        }

        NotificationPermissionRequest()

        Scaffold(
            containerColor = Color.White,
            topBar = {
                val editing = editor
                TopAppBarMMD(
                    title = {
                        TextMMD(
                            when {
                                editing == null -> tab.title
                                editing.alarm == null -> "New alarm"
                                else -> "Edit alarm"
                            },
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    },
                    navigationIcon = {
                        if (editing != null) {
                            IconButton(onClick = { editor = null }) {
                                Icon(Icons.Outlined.ArrowBack, contentDescription = "Back")
                            }
                        }
                    },
                )
            },
            bottomBar = {
                if (editor == null) {
                    NavigationBarMMD {
                        for (t in Tab.entries) {
                            NavigationBarItemMMD(
                                selected = tab == t,
                                onClick = { tab = t },
                                icon = { Icon(t.icon, contentDescription = t.title) },
                                label = { TextMMD(t.title, fontSize = 12.sp) },
                            )
                        }
                    }
                }
            },
        ) { padding ->
            val modifier = Modifier
                .fillMaxSize()
                .padding(padding)
            val editing = editor
            if (editing != null) {
                AlarmEditScreen(viewModel, editing.alarm, modifier, onDone = { editor = null })
            } else when (tab) {
                Tab.Alarms -> AlarmTab(viewModel, modifier, onEdit = { editor = AlarmEditor(it) })
                Tab.Timer -> TimerTab(viewModel, modifier)
                Tab.Stopwatch -> StopwatchTab(viewModel, modifier)
            }
        }
    }

    @Composable
    private fun NotificationPermissionRequest() {
        if (Build.VERSION.SDK_INT < 33) return
        val launcher = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) {}
        LaunchedEffect(Unit) {
            if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}
