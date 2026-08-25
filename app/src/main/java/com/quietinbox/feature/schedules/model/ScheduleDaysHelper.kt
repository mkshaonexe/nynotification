package com.quietinbox.feature.schedules.model

import java.time.DayOfWeek
import java.util.Locale

/**
 * Utility functions for day-of-week bitmasks and time formatting.
 */
object ScheduleDaysHelper {

    val ALL_DAYS: List<DayOfWeek> = listOf(
        DayOfWeek.MONDAY,
        DayOfWeek.TUESDAY,
        DayOfWeek.WEDNESDAY,
        DayOfWeek.THURSDAY,
        DayOfWeek.FRIDAY,
        DayOfWeek.SATURDAY,
        DayOfWeek.SUNDAY
    )

    /**
     * Checks if [dayOfWeek] is selected in [daysMask].
     */
    fun isDaySelected(daysMask: Int, dayOfWeek: DayOfWeek): Boolean {
        val bit = 1 shl (dayOfWeek.value - 1)
        return (daysMask and bit) != 0
    }

    /**
     * Toggles [dayOfWeek] in [daysMask] and returns the updated mask.
     */
    fun toggleDay(daysMask: Int, dayOfWeek: DayOfWeek): Int {
        val bit = 1 shl (dayOfWeek.value - 1)
        return daysMask xor bit
    }

    /**
     * Returns a human-readable summary of the active days (e.g. "Daily", "Sun–Thu", "Mon, Wed").
     */
    fun formatDaysSummary(daysMask: Int): String {
        return when (daysMask) {
            SchedulePresets.MASK_ALL_DAYS -> "Daily"
            SchedulePresets.MASK_WEEKDAYS -> "Weekdays"
            SchedulePresets.MASK_WEEKENDS -> "Weekends"
            SchedulePresets.MASK_SUN_THU -> "Sun–Thu"
            0 -> "Never"
            else -> {
                val dayNames = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                val activeNames = ALL_DAYS.mapIndexedNotNull { index, day ->
                    if (isDaySelected(daysMask, day)) dayNames[index] else null
                }
                activeNames.joinToString(", ")
            }
        }
    }

    /**
     * Formats a minute-of-day [0..1439] as "HH:mm".
     */
    fun formatTime(minuteOfDay: Int): String {
        val m = ((minuteOfDay % 1440) + 1440) % 1440
        val hour = m / 60
        val minute = m % 60
        return String.format(Locale.ROOT, "%02d:%02d", hour, minute)
    }

    /**
     * Formats start and end minutes as "HH:mm – HH:mm".
     */
    fun formatTimeRange(startMinute: Int, endMinute: Int): String {
        return "${formatTime(startMinute)} – ${formatTime(endMinute)}"
    }
}
