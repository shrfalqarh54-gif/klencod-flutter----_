package com.example.domain

import com.example.core.DateUtilsAr

enum class NowStateKind {
    ACTIVE_TASK,          // Has an ongoing task right now
    FREE_TIME_WITH_NEXT,  // Free time right now, with an upcoming task later today
    NO_MORE_TASKS_TODAY,  // Completed or passed all tasks today
    EMPTY_DAY             // No tasks scheduled for today at all
}

data class NowMomentAnalysis(
    val kind: NowStateKind,
    val currentTask: TaskOccurrence?,
    val remainingMinutesInCurrent: Int?,
    val nextTask: TaskOccurrence?,
    val freeMinutesUntilNext: Int?,
    val missedTasksCount: Int,
    val completedTasksCount: Int,
    val totalTasksCount: Int,
    val todayTasks: List<TaskOccurrence>
)

object SmartEngine {

    fun analyzeNowMoment(
        occurrences: List<TaskOccurrence>,
        currentMinute: Int = DateUtilsAr.currentMinuteOfDay()
    ): NowMomentAnalysis {
        val todayTasks = occurrences.sortedBy { it.startMinute }

        if (todayTasks.isEmpty()) {
            return NowMomentAnalysis(
                kind = NowStateKind.EMPTY_DAY,
                currentTask = null,
                remainingMinutesInCurrent = null,
                nextTask = null,
                freeMinutesUntilNext = (1440 - currentMinute).coerceAtLeast(0),
                missedTasksCount = 0,
                completedTasksCount = 0,
                totalTasksCount = 0,
                todayTasks = emptyList()
            )
        }

        // Active task running right now
        val current = todayTasks.firstOrNull { occ ->
            currentMinute in occ.startMinute until occ.endMinute &&
                    occ.status != TaskStatus.DONE &&
                    occ.status != TaskStatus.SKIPPED
        }

        // Next upcoming task starting after current minute
        val next = todayTasks.firstOrNull { it.startMinute > currentMinute && it.status != TaskStatus.DONE }

        val missed = todayTasks.count {
            it.endMinute <= currentMinute && it.status == TaskStatus.MISSED
        }
        val completed = todayTasks.count { it.status == TaskStatus.DONE }

        val kind = when {
            current != null -> NowStateKind.ACTIVE_TASK
            next != null -> NowStateKind.FREE_TIME_WITH_NEXT
            else -> NowStateKind.NO_MORE_TASKS_TODAY
        }

        val remainingMinutes = current?.let { (it.endMinute - currentMinute).coerceAtLeast(0) }
        val freeUntilNext = when {
            current != null -> null
            next != null -> (next.startMinute - currentMinute).coerceAtLeast(0)
            else -> (1440 - currentMinute).coerceAtLeast(0)
        }

        return NowMomentAnalysis(
            kind = kind,
            currentTask = current,
            remainingMinutesInCurrent = remainingMinutes,
            nextTask = next,
            freeMinutesUntilNext = freeUntilNext,
            missedTasksCount = missed,
            completedTasksCount = completed,
            totalTasksCount = todayTasks.size,
            todayTasks = todayTasks
        )
    }
}
