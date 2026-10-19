package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.local.AppDatabase
import com.example.data.local.OccurrenceStateEntity
import com.example.service.AlarmRingtoneService
import com.example.service.AlarmScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlarmBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent == null) return

        val taskId = intent.getStringExtra("taskId") ?: ""
        val epochDay = intent.getLongExtra("epochDay", 0L)
        val kind = intent.getStringExtra("kind") ?: "start"
        val title = intent.getStringExtra("title") ?: "المهمة"
        val timeRange = intent.getStringExtra("timeRange") ?: ""
        val toneId = intent.getStringExtra("toneId") ?: "tone_default"
        val scheduledTime = intent.getLongExtra("scheduledTime", 0L)

        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getInstance(context)

                when (intent.action) {
                    ACTION_TRIGGER_ALARM -> {
                        val now = System.currentTimeMillis()
                        val diffMinutes = if (scheduledTime > 0) (now - scheduledTime) / (60 * 1000) else 0

                        // Rule: Missed by > 5 minutes (e.g. phone was off) -> do NOT ring, mark missed
                        if (diffMinutes > 5 && taskId != "test_task") {
                            db.occurrenceDao().setOccurrenceState(
                                OccurrenceStateEntity(
                                    taskId = taskId,
                                    epochDay = epochDay,
                                    status = "missed"
                                )
                            )
                        } else {
                            // Ring and show full-screen notification/activity
                            AlarmRingtoneService.startService(
                                context = context,
                                taskId = taskId,
                                epochDay = epochDay,
                                title = title,
                                kind = kind,
                                timeRange = timeRange,
                                toneId = toneId
                            )
                        }

                        // Refill rolling window
                        AlarmScheduler.rescheduleRollingWindow(context)
                    }

                    ACTION_STOP_ALARM -> {
                        AlarmRingtoneService.stopService(context)
                    }

                    ACTION_MARK_DONE -> {
                        AlarmRingtoneService.stopService(context)
                        if (taskId.isNotEmpty()) {
                            db.occurrenceDao().setOccurrenceState(
                                OccurrenceStateEntity(
                                    taskId = taskId,
                                    epochDay = epochDay,
                                    status = "done",
                                    completedAt = System.currentTimeMillis()
                                )
                            )
                            AlarmScheduler.rescheduleRollingWindow(context)
                        }
                    }

                    ACTION_MARK_MISSED -> {
                        AlarmRingtoneService.stopService(context)
                        if (taskId.isNotEmpty()) {
                            db.occurrenceDao().setOccurrenceState(
                                OccurrenceStateEntity(
                                    taskId = taskId,
                                    epochDay = epochDay,
                                    status = "missed"
                                )
                            )
                            AlarmScheduler.rescheduleRollingWindow(context)
                        }
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_TRIGGER_ALARM = "com.aistudio.powerofnow.ACTION_TRIGGER_ALARM"
        const val ACTION_STOP_ALARM = "com.aistudio.powerofnow.ACTION_STOP_ALARM"
        const val ACTION_MARK_DONE = "com.aistudio.powerofnow.ACTION_MARK_DONE"
        const val ACTION_MARK_MISSED = "com.aistudio.powerofnow.ACTION_MARK_MISSED"
    }
}
