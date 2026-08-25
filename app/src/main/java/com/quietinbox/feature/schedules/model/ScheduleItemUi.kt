package com.quietinbox.feature.schedules.model

import com.quietinbox.data.db.entity.ScheduleEntity

/**
 * UI representation of a schedule item displayed on the list screen.
 */
data class ScheduleItemUi(
    val schedule: ScheduleEntity,
    val extraAllowedApps: Set<String> = emptySet(),
    val isActiveNow: Boolean = false
)
