package com.example.data.preferences

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.DayOfWeek

data class UserPreferences(
    val defaultTone: String = "tone_default",
    val ringDurationSeconds: Int = 60,
    val vibrationEnabled: Boolean = true,
    val allowConflictOverride: Boolean = false,
    val weekStartDay: DayOfWeek = DayOfWeek.SATURDAY,
    val is24HourFormat: Boolean = true,
    val quietHoursEnabled: Boolean = false,
    val quietHoursStartMinute: Int = 23 * 60, // 23:00
    val quietHoursEndMinute: Int = 6 * 60    // 06:00
)

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("power_of_now_settings", Context.MODE_PRIVATE)

    private val _preferencesFlow = MutableStateFlow(loadPreferences())
    val preferencesFlow: StateFlow<UserPreferences> = _preferencesFlow.asStateFlow()

    private fun loadPreferences(): UserPreferences {
        val weekStartStr = prefs.getString("week_start_day", DayOfWeek.SATURDAY.name) ?: DayOfWeek.SATURDAY.name
        val weekStart = try {
            DayOfWeek.valueOf(weekStartStr)
        } catch (_: Exception) {
            DayOfWeek.SATURDAY
        }

        return UserPreferences(
            defaultTone = prefs.getString("default_tone", "tone_default") ?: "tone_default",
            ringDurationSeconds = prefs.getInt("ring_duration_seconds", 60),
            vibrationEnabled = prefs.getBoolean("vibration_enabled", true),
            allowConflictOverride = prefs.getBoolean("allow_conflict_override", false),
            weekStartDay = weekStart,
            is24HourFormat = prefs.getBoolean("is_24_hour_format", true),
            quietHoursEnabled = prefs.getBoolean("quiet_hours_enabled", false),
            quietHoursStartMinute = prefs.getInt("quiet_hours_start", 23 * 60),
            quietHoursEndMinute = prefs.getInt("quiet_hours_end", 6 * 60)
        )
    }

    fun updatePreferences(update: (UserPreferences) -> UserPreferences) {
        val newPrefs = update(_preferencesFlow.value)
        prefs.edit().apply {
            putString("default_tone", newPrefs.defaultTone)
            putInt("ring_duration_seconds", newPrefs.ringDurationSeconds)
            putBoolean("vibration_enabled", newPrefs.vibrationEnabled)
            putBoolean("allow_conflict_override", newPrefs.allowConflictOverride)
            putString("week_start_day", newPrefs.weekStartDay.name)
            putBoolean("is_24_hour_format", newPrefs.is24HourFormat)
            putBoolean("quiet_hours_enabled", newPrefs.quietHoursEnabled)
            putInt("quiet_hours_start", newPrefs.quietHoursStartMinute)
            putInt("quiet_hours_end", newPrefs.quietHoursEndMinute)
            apply()
        }
        _preferencesFlow.value = newPrefs
    }

    fun clearAll() {
        prefs.edit().clear().apply()
        _preferencesFlow.value = UserPreferences()
    }
}
