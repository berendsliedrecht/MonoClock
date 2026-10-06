package com.monoapps.monoclock

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.format.DateFormat
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mudita.mmd.ThemeMMD
import com.mudita.mmd.components.buttons.OutlinedButtonMMD
import com.mudita.mmd.components.text.TextMMD
import java.util.Date

/** Full-screen alarm/timer ring screen, shown over the lock screen. */
class RingActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setShowWhenLocked(true)
        setTurnScreenOn(true)
        (getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager)
            .requestDismissKeyguard(this, null)
        setContent { ThemeMMD { RingScreen() } }
    }

    private fun serviceAction(action: String) {
        startService(Intent(this, AlarmService::class.java).setAction(action))
    }

    @Composable
    private fun RingScreen() {
        val ring by AlarmService.ringing.collectAsState()
        // Finish once the ring stops, also when dismissed from the notification.
        if (ring == null) {
            finish()
            return
        }
        val isTimer = ring!!.isTimer
        val pattern = if (DateFormat.is24HourFormat(this)) "HH:mm" else "h:mm a"

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(120.dp))
            TextMMD(if (isTimer) "Timer" else "Alarm", fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(24.dp))
            TextMMD(
                if (isTimer) "0:00" else DateFormat.format(pattern, Date()).toString(),
                fontSize = 72.sp,
                fontWeight = FontWeight.Black,
            )
            Spacer(Modifier.height(80.dp))
            OutlinedButtonMMD(
                onClick = { serviceAction(AlarmService.ACTION_DISMISS) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
            ) { TextMMD("Dismiss", fontSize = 22.sp, fontWeight = FontWeight.Bold) }
            if (!isTimer) {
                Spacer(Modifier.height(20.dp))
                OutlinedButtonMMD(
                    onClick = { serviceAction(AlarmService.ACTION_SNOOZE) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp),
                ) { TextMMD("Snooze 10 min", fontSize = 22.sp) }
            }
        }
    }
}
