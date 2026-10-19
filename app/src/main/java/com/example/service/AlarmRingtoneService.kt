package com.example.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.MediaPlayer
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.example.data.preferences.PreferencesManager

class AlarmRingtoneService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private val handler = Handler(Looper.getMainLooper())
    private var stopRunnable: Runnable? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) return START_NOT_STICKY

        if (intent.action == ACTION_STOP) {
            stopAlarm()
            stopSelf()
            return START_NOT_STICKY
        }

        val taskId = intent.getStringExtra("taskId") ?: ""
        val epochDay = intent.getLongExtra("epochDay", 0L)
        val title = intent.getStringExtra("title") ?: "المهمة"
        val kind = intent.getStringExtra("kind") ?: "start"
        val timeRangeStr = intent.getStringExtra("timeRange") ?: ""
        val toneId = intent.getStringExtra("toneId") ?: "tone_default"

        val notification = NotificationHelper.buildAlarmNotification(
            this,
            taskId,
            epochDay,
            title,
            kind,
            timeRangeStr
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        playAudioAndVibrate(toneId)

        // Auto-stop after configured duration (default 60 seconds)
        val prefs = PreferencesManager(this).preferencesFlow.value
        val durationMs = prefs.ringDurationSeconds * 1000L

        stopRunnable?.let { handler.removeCallbacks(it) }
        stopRunnable = Runnable {
            stopAlarm()
            stopSelf()
        }
        handler.postDelayed(stopRunnable!!, durationMs)

        return START_STICKY
    }

    private fun playAudioAndVibrate(toneId: String) {
        stopAudioAndVibration()
        val prefs = PreferencesManager(this).preferencesFlow.value

        // Audio
        try {
            val player = ToneManager.createMediaPlayerForTone(this, toneId)
            player.isLooping = true
            mediaPlayer = player
            player.start()
        } catch (_: Exception) {
            // Audio failure should not crash alarm
        }

        // Vibration
        if (prefs.vibrationEnabled) {
            try {
                vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val manager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                    manager.defaultVibrator
                } else {
                    @Suppress("DEPRECATION")
                    getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                }

                val pattern = longArrayOf(0, 1000, 800)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(pattern, 0)
                }
            } catch (_: Exception) {
                // Vibration failure ignored
            }
        }
    }

    private fun stopAudioAndVibration() {
        try {
            mediaPlayer?.let {
                if (it.isPlaying) it.stop()
                it.release()
            }
        } catch (_: Exception) {}
        mediaPlayer = null

        try {
            vibrator?.cancel()
        } catch (_: Exception) {}
        vibrator = null
    }

    private fun stopAlarm() {
        stopRunnable?.let { handler.removeCallbacks(it) }
        stopAudioAndVibration()
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    override fun onDestroy() {
        stopAlarm()
        super.onDestroy()
    }

    companion object {
        const val NOTIFICATION_ID = 9001
        const val ACTION_START = "com.aistudio.powerofnow.ACTION_START_RINGING"
        const val ACTION_STOP = "com.aistudio.powerofnow.ACTION_STOP_RINGING"

        fun startService(
            context: Context,
            taskId: String,
            epochDay: Long,
            title: String,
            kind: String,
            timeRange: String,
            toneId: String
        ) {
            val intent = Intent(context, AlarmRingtoneService::class.java).apply {
                action = ACTION_START
                putExtra("taskId", taskId)
                putExtra("epochDay", epochDay)
                putExtra("title", title)
                putExtra("kind", kind)
                putExtra("timeRange", timeRange)
                putExtra("toneId", toneId)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, AlarmRingtoneService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }
}
