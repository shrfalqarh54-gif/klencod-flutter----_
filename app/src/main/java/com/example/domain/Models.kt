package com.example.domain

enum class TaskStatus {
    PENDING,
    RUNNING,
    DONE,
    MISSED,
    SKIPPED,
    MOVED
}

enum class RecurrenceType(val code: String, val titleAr: String) {
    ONCE("once", "مرة واحدة"),
    DAILY("daily", "يومياً"),
    WEEKLY("weekly", "أسبوعياً"),
    DAYS_OF_WEEK("daysOfWeek", "أيام محددة"),
    MONTHLY("monthly", "شهرياً");

    companion object {
        fun fromCode(code: String): RecurrenceType {
            return entries.find { it.code == code } ?: ONCE
        }
    }
}

data class TaskOccurrence(
    val taskId: String,
    val epochDay: Long,
    val title: String,
    val startMinute: Int,
    val endMinute: Int,
    val durationMinutes: Int,
    val status: TaskStatus = TaskStatus.PENDING,
    val categoryId: String? = null,
    val note: String? = null,
    val isRecurring: Boolean = false,
    val recurrenceType: RecurrenceType = RecurrenceType.ONCE,
    val hasConflict: Boolean = false,
    val completedSubtasks: Int = 0,
    val totalSubtasks: Int = 0,
    val alarmStartEnabled: Boolean = true,
    val alarmEndEnabled: Boolean = true,
    val toneType: String = "defaultTone",
    val toneRef: String? = null
)

data class FreeGap(
    val startMinute: Int,
    val endMinute: Int,
    val durationMinutes: Int
)

data class ConflictCheckResult(
    val hasConflict: Boolean,
    val conflictingOccurrences: List<TaskOccurrence>,
    val suggestedGaps: List<FreeGap>,
    val proposedShortenedEnd: Int? = null
)
