package com.example.domain

import com.example.core.DateUtilsAr
import com.example.data.local.OccurrenceStateEntity
import com.example.data.local.SubtaskEntity
import com.example.data.local.SubtaskStateEntity
import com.example.data.local.TaskEntity
import com.example.data.local.TaskExceptionEntity
import java.time.DayOfWeek
import java.time.LocalDate

object RecurrenceEngine {

    /**
     * Map DayOfWeek to bitmask:
     * Sat = 1, Sun = 2, Mon = 4, Tue = 8, Wed = 16, Thu = 32, Fri = 64
     */
    fun dayOfWeekToBitmask(dayOfWeek: DayOfWeek): Int {
        return when (dayOfWeek) {
            DayOfWeek.SATURDAY -> 1
            DayOfWeek.SUNDAY -> 2
            DayOfWeek.MONDAY -> 4
            DayOfWeek.TUESDAY -> 8
            DayOfWeek.WEDNESDAY -> 16
            DayOfWeek.THURSDAY -> 32
            DayOfWeek.FRIDAY -> 64
        }
    }

    /**
     * Checks if task matches the given epochDay based on recurrence rules.
     */
    fun doesTaskOccurOn(task: TaskEntity, epochDay: Long): Boolean {
        if (epochDay < task.startEpochDay) return false

        task.recurrenceEndEpochDay?.let { endDay ->
            if (epochDay > endDay) return false
        }

        val startDate = DateUtilsAr.epochDayToLocalDate(task.startEpochDay)
        val targetDate = DateUtilsAr.epochDayToLocalDate(epochDay)

        return when (RecurrenceType.fromCode(task.recurrenceType)) {
            RecurrenceType.ONCE -> epochDay == task.startEpochDay
            RecurrenceType.DAILY -> true
            RecurrenceType.WEEKLY -> targetDate.dayOfWeek == startDate.dayOfWeek
            RecurrenceType.DAYS_OF_WEEK -> {
                val bit = dayOfWeekToBitmask(targetDate.dayOfWeek)
                (task.recurrenceDaysMask and bit) != 0
            }
            RecurrenceType.MONTHLY -> {
                val targetDayOfMonth = targetDate.dayOfMonth
                val expectedDay = startDate.dayOfMonth
                val maxDayInTargetMonth = targetDate.lengthOfMonth()
                if (expectedDay > maxDayInTargetMonth) {
                    targetDayOfMonth == maxDayInTargetMonth
                } else {
                    targetDayOfMonth == expectedDay
                }
            }
        }
    }

    /**
     * Expands all occurrences for a specific day given tasks, exceptions, and states.
     */
    fun expandOccurrencesForDay(
        epochDay: Long,
        tasks: List<TaskEntity>,
        exceptions: List<TaskExceptionEntity>,
        occurrenceStates: List<OccurrenceStateEntity>,
        subtasks: List<SubtaskEntity> = emptyList(),
        subtaskStates: List<SubtaskStateEntity> = emptyList(),
        currentMinute: Int = DateUtilsAr.currentMinuteOfDay(),
        isToday: Boolean = (epochDay == DateUtilsAr.todayEpochDay())
    ): List<TaskOccurrence> {
        val exceptionsByTask = exceptions.filter { it.originalEpochDay == epochDay }
            .associateBy { it.taskId }
        val statesByTask = occurrenceStates.filter { it.epochDay == epochDay }
            .associateBy { it.taskId }
        val subtasksByTask = subtasks.groupBy { it.taskId }
        val subtaskStatesBySubtask = subtaskStates.filter { it.epochDay == epochDay }
            .associateBy { it.subtaskId }

        val occurrences = mutableListOf<TaskOccurrence>()

        for (task in tasks) {
            val exception = exceptionsByTask[task.id]
            if (exception?.type == "cancelled") {
                continue
            }

            if (!doesTaskOccurOn(task, epochDay) && exception == null) {
                continue
            }

            // Apply overrides if present
            val title = exception?.overrideTitle ?: task.title
            val startMinute = exception?.overrideStart ?: task.startMinute
            val endMinute = exception?.overrideEnd ?: task.endMinute
            val note = exception?.overrideNote ?: task.note
            val alarmStart = exception?.overrideAlarmStart ?: task.alarmStartEnabled
            val alarmEnd = exception?.overrideAlarmEnd ?: task.alarmEndEnabled
            val tone = exception?.overrideTone ?: task.toneType

            val duration = (endMinute - startMinute).coerceAtLeast(1)

            val rawStatus = statesByTask[task.id]?.status ?: "pending"
            val status = when (rawStatus) {
                "done" -> TaskStatus.DONE
                "skipped" -> TaskStatus.SKIPPED
                "moved" -> TaskStatus.MOVED
                "missed" -> TaskStatus.MISSED
                else -> {
                    if (isToday) {
                        when {
                            currentMinute in startMinute until endMinute -> TaskStatus.RUNNING
                            currentMinute >= endMinute -> TaskStatus.MISSED
                            else -> TaskStatus.PENDING
                        }
                    } else if (epochDay < DateUtilsAr.todayEpochDay()) {
                        TaskStatus.MISSED
                    } else {
                        TaskStatus.PENDING
                    }
                }
            }

            val taskSubs = subtasksByTask[task.id] ?: emptyList()
            val totalSubs = taskSubs.size
            val doneSubs = taskSubs.count { sub ->
                subtaskStatesBySubtask[sub.id]?.done == true
            }

            occurrences.add(
                TaskOccurrence(
                    taskId = task.id,
                    epochDay = epochDay,
                    title = title,
                    startMinute = startMinute,
                    endMinute = endMinute,
                    durationMinutes = duration,
                    status = status,
                    categoryId = task.categoryId,
                    note = note,
                    isRecurring = task.recurrenceType != "once",
                    recurrenceType = RecurrenceType.fromCode(task.recurrenceType),
                    hasConflict = false, // will be evaluated by ConflictEngine
                    completedSubtasks = doneSubs,
                    totalSubtasks = totalSubs,
                    alarmStartEnabled = alarmStart,
                    alarmEndEnabled = alarmEnd,
                    toneType = tone,
                    toneRef = task.toneRef
                )
            )
        }

        return occurrences.sortedBy { it.startMinute }
    }
}
