package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.feature.alarm.AlarmRingActivity
import com.example.receiver.AlarmBroadcastReceiver

object NotificationHelper {

    const val CHANNEL_ID = "power_of_now_alarms"
    const val CHANNEL_NAME = "منبهات قوة الآن"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "قناة إطلاق منبهات المهام في الوقت الفعلي"
                enableVibration(true)
                setBypassDnd(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    fun buildAlarmNotification(
        context: Context,
        taskId: String,
        epochDay: Long,
        title: String,
        kind: String, // "start" or "end"
        timeRangeStr: String
    ): Notification {
        createNotificationChannel(context)

        // Full Screen Intent to AlarmRingActivity
        val fullScreenIntent = Intent(context, AlarmRingActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("taskId", taskId)
            putExtra("epochDay", epochDay)
            putExtra("title", title)
            putExtra("kind", kind)
            putExtra("timeRange", timeRangeStr)
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            context,
            (taskId.hashCode() * 31 + epochDay.toInt() * 7 + kind.hashCode()) and 0x7FFFFFFF,
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Stop Intent (dismiss alarm service)
        val stopIntent = Intent(context, AlarmBroadcastReceiver::class.java).apply {
            action = AlarmBroadcastReceiver.ACTION_STOP_ALARM
            putExtra("taskId", taskId)
            putExtra("epochDay", epochDay)
        }
        val stopPendingIntent = PendingIntent.getBroadcast(
            context,
            (taskId.hashCode() + 101) and 0x7FFFFFFF,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val headerText = if (kind == "start") "🔔 الآن وقت:" else "⏰ انتهت المهمة:"
        val bodyText = if (kind == "start") "«$title» — $timeRangeStr" else "«$title»"

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(headerText)
            .setContentText(bodyText)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setContentIntent(fullScreenPendingIntent)

        if (kind == "start") {
            builder.addAction(0, "إيقاف المنبه", stopPendingIntent)
        } else {
            // "تم ✅" action
            val doneIntent = Intent(context, AlarmBroadcastReceiver::class.java).apply {
                action = AlarmBroadcastReceiver.ACTION_MARK_DONE
                putExtra("taskId", taskId)
                putExtra("epochDay", epochDay)
            }
            val donePendingIntent = PendingIntent.getBroadcast(
                context,
                (taskId.hashCode() + 202) and 0x7FFFFFFF,
                doneIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // "لم أنجزها" action
            val missedIntent = Intent(context, AlarmBroadcastReceiver::class.java).apply {
                action = AlarmBroadcastReceiver.ACTION_MARK_MISSED
                putExtra("taskId", taskId)
                putExtra("epochDay", epochDay)
            }
            val missedPendingIntent = PendingIntent.getBroadcast(
                context,
                (taskId.hashCode() + 303) and 0x7FFFFFFF,
                missedIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            builder.addAction(0, "تم ✅", donePendingIntent)
            builder.addAction(0, "لم أنجزها", missedPendingIntent)
        }

        return builder.build()
    }
}
