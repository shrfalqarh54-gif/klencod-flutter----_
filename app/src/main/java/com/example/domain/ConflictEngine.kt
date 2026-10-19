package com.example.domain

object ConflictEngine {

    /**
     * Checks if two time intervals overlap.
     * Overlap occurs ONLY if startA < endB AND startB < endA.
     * Adjacent intervals (e.g. 16:00-17:00 and 17:00-18:00) do NOT overlap.
     */
    fun isOverlapping(startA: Int, endA: Int, startB: Int, endB: Int): Boolean {
        return startA < endB && startB < endA
    }

    /**
     * Finds all occurrences on the specified day that conflict with [start, end].
     */
    fun findConflicts(
        start: Int,
        end: Int,
        existingOccurrences: List<TaskOccurrence>,
        excludeTaskId: String? = null
    ): List<TaskOccurrence> {
        return existingOccurrences.filter { occ ->
            if (excludeTaskId != null && occ.taskId == excludeTaskId) {
                false
            } else {
                isOverlapping(start, end, occ.startMinute, occ.endMinute)
            }
        }.sortedBy { it.startMinute }
    }

    /**
     * Finds all free gaps in the 24-hour day (0..1440).
     */
    fun calculateFreeGaps(
        existingOccurrences: List<TaskOccurrence>,
        excludeTaskId: String? = null
    ): List<FreeGap> {
        val filtered = existingOccurrences
            .filter { excludeTaskId == null || it.taskId != excludeTaskId }
            .sortedBy { it.startMinute }

        val gaps = mutableListOf<FreeGap>()
        var cursor = 0

        for (occ in filtered) {
            if (occ.startMinute > cursor) {
                val gapDur = occ.startMinute - cursor
                if (gapDur > 0) {
                    gaps.add(FreeGap(cursor, occ.startMinute, gapDur))
                }
            }
            cursor = maxOf(cursor, occ.endMinute)
        }

        if (cursor < 1440) {
            gaps.add(FreeGap(cursor, 1440, 1440 - cursor))
        }

        return gaps
    }

    /**
     * Suggests up to 5 free gaps that can fit [requiredDuration].
     */
    fun suggestAlternativeGaps(
        requiredDuration: Int,
        existingOccurrences: List<TaskOccurrence>,
        excludeTaskId: String? = null,
        limit: Int = 5
    ): List<FreeGap> {
        val gaps = calculateFreeGaps(existingOccurrences, excludeTaskId)
        return gaps.filter { it.durationMinutes >= requiredDuration }.take(limit)
    }

    /**
     * Comprehensive conflict evaluation.
     */
    fun checkConflict(
        start: Int,
        end: Int,
        existingOccurrences: List<TaskOccurrence>,
        excludeTaskId: String? = null
    ): ConflictCheckResult {
        val conflicts = findConflicts(start, end, existingOccurrences, excludeTaskId)
        val hasConflict = conflicts.isNotEmpty()
        val duration = (end - start).coerceAtLeast(1)

        val suggestedGaps = if (hasConflict) {
            suggestAlternativeGaps(duration, existingOccurrences, excludeTaskId)
        } else {
            emptyList()
        }

        // Propose shortened end: first conflicting occurrence starting after start
        val firstConflictAfterStart = conflicts.firstOrNull { it.startMinute > start }
        val proposedShortenedEnd = if (firstConflictAfterStart != null && firstConflictAfterStart.startMinute > start) {
            firstConflictAfterStart.startMinute
        } else {
            null
        }

        return ConflictCheckResult(
            hasConflict = hasConflict,
            conflictingOccurrences = conflicts,
            suggestedGaps = suggestedGaps,
            proposedShortenedEnd = proposedShortenedEnd
        )
    }

    /**
     * Marks occurrences in a day list that overlap with any other occurrence in the day.
     */
    fun markAllConflictsInDay(occurrences: List<TaskOccurrence>): List<TaskOccurrence> {
        val sorted = occurrences.sortedBy { it.startMinute }
        return sorted.map { current ->
            val hasOverlap = sorted.any { other ->
                other.taskId != current.taskId && isOverlapping(current.startMinute, current.endMinute, other.startMinute, other.endMinute)
            }
            current.copy(hasConflict = hasOverlap)
        }
    }
}
