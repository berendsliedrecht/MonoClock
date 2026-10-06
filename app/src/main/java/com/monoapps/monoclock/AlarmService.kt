package com.monoapps.monoclock

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.app.NotificationCompat
import com.monoapps.monoclock.data.ClockStore
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Foreground service that rings an alarm or the timer: plays the system
 * default alarm sound at alarm volume, vibrates, and posts a full-screen
 * notification that opens [RingActivity].
 */
class AlarmService : Service() {

    data class Ring(val alarmId: Int, val isTimer: Boolean)

    companion object {
        const val ACTION_RING = "com.monoapps.monoclock.RING"
        const val ACTION_DISMISS = "com.monoapps.monoclock.DISMISS"
        const val ACTION_SNOOZE = "com.monoapps.monoclock.SNOOZE"
        const val EXTRA_ALARM_ID = "alarm_id"
        const val EXTRA_IS_TIMER = "is_timer"

        const val SNOOZE_MILLIS = 10 * 60_000L
        private const val SILENCE_AFTER_MILLIS = 10 * 60_000L
        private const val CHANNEL_ID = "alarms"
        private const val NOTIFICATION_ID = 1

        /** What is currently ringing, for [RingActivity] to observe. */
        val ringing = MutableStateFlow<Ring?>(null)
    }

    private var ringtone: Ringtone? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private val handler = Handler(Looper.getMainLooper())
    private val silence = Runnable { stopRinging() }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_RING -> startRinging(
                Ring(
                    alarmId = intent.getIntExtra(EXTRA_ALARM_ID, -1),
                    isTimer = intent.getBooleanExtra(EXTRA_IS_TIMER, false),
                )
            )
            ACTION_SNOOZE -> {
                val ring = ringing.value
                if (ring != null && !ring.isTimer) {
                    val store = ClockStore(this)
                    store.alarms().find { it.id == ring.alarmId }?.let { alarm ->
                        // One-shots were disabled on fire; re-enable so the
                        // snoozed ring passes the receiver's enabled check.
                        if (!alarm.repeating) store.updateAlarm(alarm.copy(enabled = true))
                        AlarmScheduler.scheduleAlarm(
                            this, alarm,
                            triggerAt = System.currentTimeMillis() + SNOOZE_MILLIS,
                        )
                    }
                }
                stopRinging()
            }
            ACTION_DISMISS -> stopRinging()
        }
        return START_NOT_STICKY
    }

    private fun startRinging(ring: Ring) {
        ringing.value = ring
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "monoclock:ring").apply {
            acquire(SILENCE_AFTER_MILLIS)
        }

        val notification = buildNotification(ring)
        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
        ringtone = RingtoneManager.getRingtone(this, uri)?.apply {
            audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            isLooping = true
            play()
        }

        @Suppress("DEPRECATION")
        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        vibrator.vibrate(
            VibrationEffect.createWaveform(longArrayOf(0, 600, 800), 0),
            AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build(),
        )

        handler.postDelayed(silence, SILENCE_AFTER_MILLIS)
    }

    private fun stopRinging() {
        handler.removeCallbacks(silence)
        ringtone?.stop()
        ringtone = null
        @Suppress("DEPRECATION")
        (getSystemService(Context.VIBRATOR_SERVICE) as Vibrator).cancel()
        wakeLock?.takeIf { it.isHeld }?.release()
        wakeLock = null
        ringing.value = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun buildNotification(ring: Ring): android.app.Notification {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Alarms", NotificationManager.IMPORTANCE_HIGH).apply {
                setSound(null, null) // the service plays the sound itself
                enableVibration(false)
            }
        )

        val fullScreen = PendingIntent.getActivity(
            this, 0,
            Intent(this, RingActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        fun action(act: String): PendingIntent = PendingIntent.getService(
            this, act.hashCode(),
            Intent(this, AlarmService::class.java).setAction(act),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(if (ring.isTimer) "Timer" else "Alarm")
            .setContentText(if (ring.isTimer) "Time is up" else "Wake up")
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setOngoing(true)
            .setFullScreenIntent(fullScreen, true)
            .setContentIntent(fullScreen)
            .addAction(0, "Dismiss", action(ACTION_DISMISS))
        if (!ring.isTimer) builder.addAction(0, "Snooze", action(ACTION_SNOOZE))
        return builder.build()
    }
}
