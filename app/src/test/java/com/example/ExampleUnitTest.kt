package com.example

import com.example.core.DateUtilsAr
import com.example.data.local.TaskEntity
import com.example.domain.ConflictEngine
import com.example.domain.RecurrenceEngine
import com.example.domain.RecurrenceType
import com.example.domain.TaskOccurrence
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class ExampleUnitTest {

    @Test
    fun testArabicDurationFormatting() {
        assertEquals("دقيقة", DateUtilsAr.formatDurationArabic(1))
        assertEquals("دقيقتان", DateUtilsAr.formatDurationArabic(2))
        assertEquals("5 دقائق", DateUtilsAr.formatDurationArabic(5))
        assertEquals("15 دقيقة", DateUtilsAr.formatDurationArabic(15))
        assertEquals("ساعة", DateUtilsAr.formatDurationArabic(60))
        assertEquals("ساعة ونصف", DateUtilsAr.formatDurationArabic(90))
        assertEquals("ساعتان", DateUtilsAr.formatDurationArabic(120))
        assertEquals("ساعتان و 15 دقيقة", DateUtilsAr.formatDurationArabic(135))
    }

    @Test
    fun testConflictEngineOverlapRules() {
        // Overlap: 16:00-17:00 (960..1020) and 16:30-17:30 (990..1050) -> TRUE
        assertTrue(ConflictEngine.isOverlapping(960, 1020, 990, 1050))

        // Adjacent: 16:00-17:00 (960..1020) and 17:00-18:00 (1020..1080) -> FALSE
        assertFalse(ConflictEngine.isOverlapping(960, 1020, 1020, 1080))

        // Disjoint: 10:00-11:00 (600..660) and 14:00-15:00 (840..900) -> FALSE
        assertFalse(ConflictEngine.isOverlapping(600, 660, 840, 900))

        // Enclosed: 10:00-12:00 (600..720) and 10:30-11:30 (630..690) -> TRUE
        assertTrue(ConflictEngine.isOverlapping(600, 720, 630, 690))
    }

    @Test
    fun testFreeGapsCalculation() {
        val existing = listOf(
            TaskOccurrence(
                taskId = "1",
                epochDay = 20000L,
                title = "مهمة 1",
                startMinute = 600,  // 10:00
                endMinute = 720,    // 12:00
                durationMinutes = 120
            ),
            TaskOccurrence(
                taskId = "2",
                epochDay = 20000L,
                title = "مهمة 2",
                startMinute = 840,  // 14:00
                endMinute = 960,    // 16:00
                durationMinutes = 120
            )
        )

        val gaps = ConflictEngine.calculateFreeGaps(existing)
        // Expected gaps: 0..600 (morning), 720..840 (lunch), 960..1440 (evening)
        assertEquals(3, gaps.size)
        assertEquals(0, gaps[0].startMinute)
        assertEquals(600, gaps[0].endMinute)
        assertEquals(720, gaps[1].startMinute)
        assertEquals(840, gaps[1].endMinute)
        assertEquals(960, gaps[2].startMinute)
        assertEquals(1440, gaps[2].endMinute)
    }

    @Test
    fun testRecurrenceEngineRules() {
        val baseDate = LocalDate.of(2026, 9, 21) // Monday
        val epochDay = baseDate.toEpochDay()

        // Daily task matches baseDate and baseDate + 5
        val dailyTask = TaskEntity(
            id = "daily_1",
            title = "يومياً",
            startMinute = 600,
            endMinute = 660,
            durationMinutes = 60,
            startEpochDay = epochDay,
            recurrenceType = "daily"
        )
        assertTrue(RecurrenceEngine.doesTaskOccurOn(dailyTask, epochDay))
        assertTrue(RecurrenceEngine.doesTaskOccurOn(dailyTask, epochDay + 5))
        assertFalse(RecurrenceEngine.doesTaskOccurOn(dailyTask, epochDay - 1))

        // Weekly task matches same day of week (Monday)
        val weeklyTask = TaskEntity(
            id = "weekly_1",
            title = "أسبوعياً",
            startMinute = 600,
            endMinute = 660,
            durationMinutes = 60,
            startEpochDay = epochDay,
            recurrenceType = "weekly"
        )
        assertTrue(RecurrenceEngine.doesTaskOccurOn(weeklyTask, epochDay + 7))
        assertFalse(RecurrenceEngine.doesTaskOccurOn(weeklyTask, epochDay + 3))

        // Days of week bitmask (Sat=1, Sun=2, Mon=4) -> 1 | 4 = 5
        val daysMask = RecurrenceEngine.dayOfWeekToBitmask(DayOfWeek.SATURDAY) or
                RecurrenceEngine.dayOfWeekToBitmask(DayOfWeek.MONDAY)
        val customDaysTask = TaskEntity(
            id = "custom_1",
            title = "محدد",
            startMinute = 600,
            endMinute = 660,
            durationMinutes = 60,
            startEpochDay = epochDay,
            recurrenceType = "daysOfWeek",
            recurrenceDaysMask = daysMask
        )
        assertTrue(RecurrenceEngine.doesTaskOccurOn(customDaysTask, epochDay)) // Monday
        assertFalse(RecurrenceEngine.doesTaskOccurOn(customDaysTask, epochDay + 1)) // Tuesday
    }
}
