package com.quietinbox.feature.schedules.model

import com.quietinbox.data.db.entity.ScheduleEntity

/**
 * Types of quick starter presets available for one-tap creation.
 */
enum class PresetType {
    SLEEP,
    WORK,
    PRAYER
}

/**
 * Preset definitions matching design.md §5.5 and TASKS.md §7.5.
 *
 * Presets are ordinary editable schedules, created on user tap and never seeded.
 */
object SchedulePresets {
    const val MASK_ALL_DAYS = 127 // 0b1111111 (Mon-Sun)
    const val MASK_SUN_THU = 79   // 0b1001111 (Sun, Mon, Tue, Wed, Thu)
    const val MASK_WEEKDAYS = 31  // 0b0011111 (Mon, Tue, Wed, Thu, Fri)
    const val MASK_WEEKENDS = 96  // 0b1100000 (Sat, Sun)

    /**
     * Sleep preset: 22:00 – 07:00 Daily, QUIET.
     */
    fun createSleepPreset(createdAt: Long): ScheduleEntity = ScheduleEntity(
        id = 0L,
        name = "Sleep",
        startMinute = 22 * 60, // 1320 (22:00)
        endMinute = 7 * 60,    // 420 (07:00)
        daysMask = MASK_ALL_DAYS,
        policy = "QUIET",
        enabled = true,
        createdAt = createdAt
    )

    /**
     * Work preset: 09:00 – 18:00 Sun–Thu, QUIET.
     */
    fun createWorkPreset(createdAt: Long): ScheduleEntity = ScheduleEntity(
        id = 0L,
        name = "Work",
        startMinute = 9 * 60,  // 540 (09:00)
        endMinute = 18 * 60,   // 1080 (18:00)
        daysMask = MASK_SUN_THU,
        policy = "QUIET",
        enabled = true,
        createdAt = createdAt
    )

    /**
     * Prayer preset: 13:00 – 13:30 Daily, QUIET.
     */
    fun createPrayerPreset(createdAt: Long): ScheduleEntity = ScheduleEntity(
        id = 0L,
        name = "Prayer",
        startMinute = 13 * 60,      // 780 (13:00)
        endMinute = 13 * 60 + 30,  // 810 (13:30)
        daysMask = MASK_ALL_DAYS,
        policy = "QUIET",
        enabled = true,
        createdAt = createdAt
    )
}
