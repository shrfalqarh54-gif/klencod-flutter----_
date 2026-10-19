package com.example.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.core.DateUtilsAr
import com.example.data.local.AppDatabase
import com.example.data.local.ScheduledAlarmEntity
import com.example.domain.RecurrenceEngine
import com.example.domain.TaskOccurrence
import com.example.domain.TaskStatus
import com.example.receiver.AlarmBroadcastReceiver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class PendingAlarmEvent(
    val requestCode: Int,
    val taskId: String,
    val epochDay: Long,
    val kind: String, // "start" or "end"
    val title: String,
    val timeRange: String,
    val toneId: String,
    val triggerAtMillis: Long
)

object AlarmScheduler {

    fun generateRequestCode(taskId: String, epochDay: Long, kind: String): Int {
        return ((taskId.hashCode() * 31 + epochDay.toInt() * 17 + kind.hashCode()) and 0x7FFFFFFF)
    }

    suspend fun rescheduleRollingWindow(context: Context) = withContext(Dispatchers.IO) {
        val db = AppDatabase.getInstance(context)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // Cancel previously recorded alarms
        val existingAlarms = db.alarmDao().getAllScheduledAlarms()
        for (alarm in existingAlarms) {
            cancelAlarm(context, alarmManager, alarm.requestCode)
        }
        db.alarmDao().clearAll()

        val tasks = db.taskDao().getAllTasks()
        val exceptions = db.taskExceptionDao().getAllExceptions()
        val states = db.occurrenceDao().getAllOccurrenceStates()

        val nowMillis = System.currentTimeMillis()
        val todayEpochDay = DateUtilsAr.todayEpochDay()
        val maxDaysAhead = 400L // 400 days horizon per spec
        val maxAlarmsCount = 100 // rolling window of 100 alarms

        val candidateEvents = mutableListOf<PendingAlarmEvent>()

        for (dayOffset in 0L..maxDaysAhead) {
            val epochDay = todayEpochDay + dayOffset
            val occurrences = RecurrenceEngine.expandOccurrencesForDay(
                epochDay = epochDay,
                tasks = tasks,
                exceptions = exceptions,
                occurrenceStates = states,
                currentMinute = DateUtilsAr.currentMinuteOfDay(),
                isToday = (epochDay == todayEpochDay)
            )

            for (occ in occurrences) {
                // If occurrence is done or skipped, do not ring
                if (occ.status == TaskStatus.DONE || occ.status == TaskStatus.SKIPPED) {
                    continue
                }

                val timeRangeStr = DateUtilsAr.formatTimeRange(occ.startMinute, occ.endMinute)

                // Start alarm
                if (occ.alarmStartEnabled) {
                    val startMillis = DateUtilsAr.toEpochMillis(epochDay, occ.startMinute)
                    if (startMillis > nowMillis) {
                        val reqCode = generateRequestCode(occ.taskId, epochDay, "start")
                        candidateEvents.add(
                            PendingAlarmEvent(
                                requestCode = reqCode,
                                taskId = occ.taskId,
                                epochDay = epochDay,
                                kind = "start",
                                title = occ.title,
                                timeRange = timeRangeStr,
                                toneId = occ.toneType,
                                triggerAtMillis = startMillis
                            )
                        )
                    }
                }

                // End alarm
                if (occ.alarmEndEnabled) {
                    val endMillis = DateUtilsAr.toEpochMillis(epochDay, occ.endMinute)
                    if (endMillis > nowMillis) {
                        val reqCode = generateRequestCode(occ.taskId, epochDay, "end")
                        candidateEvents.add(
                            PendingAlarmEvent(
                                requestCode = reqCode,
                                taskId = occ.taskId,
                                epochDay = epochDay,
                                kind = "end",
                                title = occ.title,
                                timeRange = timeRangeStr,
                                toneId = occ.toneType,
                                triggerAtMillis = endMillis
                            )
                        )
                    }
                }
            }

            if (candidateEvents.size >= maxAlarmsCount * 2) {
                break
            }
        }

        // Sort by time and take nearest 100
        val nearestAlarms = candidateEvents.sortedBy { it.triggerAtMillis }.take(maxAlarmsCount)

        for (event in nearestAlarms) {
            scheduleExactAlarm(context, alarmManager, event)
            db.alarmDao().insertAlarm(
                ScheduledAlarmEntity(
                    requestCode = event.requestCode,
                    taskId = event.taskId,
                    epochDay = event.epochDay,
                    kind = event.kind,
                    triggerAtMillis = event.triggerAtMillis
                )
            )
        }
    }

    private fun scheduleExactAlarm(
        context: Context,
        alarmManager: AlarmManager,
        event: PendingAlarmEvent
    ) {
        val intent = Intent(context, AlarmBroadcastReceiver::class.java).apply {
            action = AlarmBroadcastReceiver.ACTION_TRIGGER_ALARM
            putExtra("taskId", event.taskId)
            putExtra("epochDay", event.epochDay)
            putExtra("kind", event.kind)
            putExtra("title", event.title)
            putExtra("timeRange", event.timeRange)
            putExtra("toneId", event.toneId)
            putExtra("scheduledTime", event.triggerAtMillis)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            event.requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val canScheduleExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }

        try {
            if (canScheduleExact) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    event.triggerAtMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    event.triggerAtMillis,
                    pendingIntent
                )
            }
        } catch (_: SecurityException) {
            // In case of permission denial on newer Android, use inexact fallback without crashing
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                event.triggerAtMillis,
                pendingIntent
            )
        }
    }

    fun cancelAlarm(context: Context, alarmManager: AlarmManager, requestCode: Int) {
        val intent = Intent(context, AlarmBroadcastReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    /**
     * Test Alarm button in Settings: triggers real alarm test after [delaySeconds]
     */
    fun scheduleTestAlarm(context: Context, delaySeconds: Int = 10) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val triggerTime = System.currentTimeMillis() + (delaySeconds * 1000L)
        val reqCode = 999999

        val intent = Intent(context, AlarmBroadcastReceiver::class.java).apply {
            action = AlarmBroadcastReceiver.ACTION_TRIGGER_ALARM
            putExtra("taskId", "test_task")
            putExtra("epochDay", DateUtilsAr.todayEpochDay())
            putExtra("kind", "start")
            putExtra("title", "اختبار منبه قوة الآن 💪")
            putExtra("timeRange", "الآن")
            putExtra("toneId", "tone_default")
            putExtra("scheduledTime", triggerTime)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reqCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            }
        } catch (_: Exception) {
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
        }
    }
}
