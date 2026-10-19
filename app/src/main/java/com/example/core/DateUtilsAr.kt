package com.example.core

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Central Arabic date, time, and duration formatting utility.
 * Strictly offline, localized, and handles day boundaries, DST, and Arabic grammatical duration rules.
 */
object DateUtilsAr {

    val ZONE_ID: ZoneId = ZoneId.systemDefault()

    fun todayEpochDay(): Long {
        return LocalDate.now(ZONE_ID).toEpochDay()
    }

    fun currentMinuteOfDay(): Int {
        val now = LocalTime.now(ZONE_ID)
        return now.hour * 60 + now.minute
    }

    fun epochDayToLocalDate(epochDay: Long): LocalDate {
        return LocalDate.ofEpochDay(epochDay)
    }

    fun localDateToEpochDay(date: LocalDate): Long {
        return date.toEpochDay()
    }

    fun getDayOfWeekArabic(dayOfWeek: DayOfWeek): String {
        return when (dayOfWeek) {
            DayOfWeek.SATURDAY -> "السبت"
            DayOfWeek.SUNDAY -> "الأحد"
            DayOfWeek.MONDAY -> "الإثنين"
            DayOfWeek.TUESDAY -> "الثلاثاء"
            DayOfWeek.WEDNESDAY -> "الأربعاء"
            DayOfWeek.THURSDAY -> "الخميس"
            DayOfWeek.FRIDAY -> "الجمعة"
        }
    }

    fun getMonthNameArabic(monthValue: Int): String {
        return when (monthValue) {
            1 -> "يناير"
            2 -> "فبراير"
            3 -> "مارس"
            4 -> "أبريل"
            5 -> "مايو"
            6 -> "يونيو"
            7 -> "يوليو"
            8 -> "أغسطس"
            9 -> "سبتمبر"
            10 -> "أكتوبر"
            11 -> "نوفمبر"
            12 -> "ديسمبر"
            else -> ""
        }
    }

    /**
     * Formats date as: "السبت 21 سبتمبر 2026"
     */
    fun formatFullDateArabic(epochDay: Long): String {
        val date = epochDayToLocalDate(epochDay)
        val dayName = getDayOfWeekArabic(date.dayOfWeek)
        val monthName = getMonthNameArabic(date.monthValue)
        return "$dayName ${date.dayOfMonth} $monthName ${date.year}"
    }

    /**
     * Formats relative date or full date: "اليوم"، "غداً"، "أمس"
     */
    fun formatRelativeDateArabic(epochDay: Long): String {
        val today = todayEpochDay()
        return when (epochDay) {
            today -> "اليوم (${formatFullDateArabic(epochDay)})"
            today + 1 -> "غداً (${formatFullDateArabic(epochDay)})"
            today - 1 -> "أمس (${formatFullDateArabic(epochDay)})"
            else -> formatFullDateArabic(epochDay)
        }
    }

    /**
     * Formats minute of day (0..1439 or 1440) as "HH:mm" (24h) or "hh:mm a" (12h)
     */
    fun formatTime(minuteOfDay: Int, is24Hour: Boolean = true): String {
        val safeMinute = minuteOfDay.coerceIn(0, 1440)
        if (safeMinute == 1440) {
            return if (is24Hour) "24:00" else "12:00 ص (نهاية اليوم)"
        }
        val hour = safeMinute / 60
        val minute = safeMinute % 60
        return if (is24Hour) {
            String.format(Locale.ROOT, "%02d:%02d", hour, minute)
        } else {
            val period = if (hour < 12) "ص" else "م"
            val displayHour = when (val h = hour % 12) {
                0 -> 12
                else -> h
            }
            String.format(Locale.ROOT, "%02d:%02d %s", displayHour, minute, period)
        }
    }

    fun formatTimeRange(startMinute: Int, endMinute: Int, is24Hour: Boolean = true): String {
        return "${formatTime(startMinute, is24Hour)} → ${formatTime(endMinute, is24Hour)}"
    }

    /**
     * Correct Arabic duration rules:
     * 1 = دقيقة
     * 2 = دقيقتان
     * 3-10 = X دقائق
     * 11+ = X دقيقة
     * 60 = ساعة
     * 90 = ساعة ونصف
     * 120 = ساعتان
     * composite = X ساعات / ساعة و Y دقيقة / دقائق
     */
    fun formatDurationArabic(minutes: Int): String {
        if (minutes <= 0) return "0 دقيقة"
        if (minutes == 60) return "ساعة"
        if (minutes == 90) return "ساعة ونصف"
        if (minutes == 120) return "ساعتان"

        val hours = minutes / 60
        val mins = minutes % 60

        val minsString = when {
            mins == 0 -> ""
            mins == 1 -> "دقيقة"
            mins == 2 -> "دقيقتان"
            mins in 3..10 -> "$mins دقائق"
            else -> "$mins دقيقة"
        }

        val hoursString = when {
            hours == 0 -> ""
            hours == 1 -> "ساعة"
            hours == 2 -> "ساعتان"
            hours in 3..10 -> "$hours ساعات"
            else -> "$hours ساعة"
        }

        return when {
            hours == 0 -> minsString
            mins == 0 -> hoursString
            else -> "$hoursString و $minsString"
        }
    }

    /**
     * Converts epochDay + minuteOfDay to epoch millis for exact AlarmManager triggers
     */
    fun toEpochMillis(epochDay: Long, minuteOfDay: Int): Long {
        val safeMinute = minuteOfDay.coerceIn(0, 1439)
        val date = epochDayToLocalDate(epochDay)
        val time = LocalTime.of(safeMinute / 60, safeMinute % 60)
        val localDateTime = LocalDateTime.of(date, time)
        return localDateTime.atZone(ZONE_ID).toInstant().toEpochMilli()
    }
}
