package com.quietinbox.feature.schedules.data

import com.quietinbox.data.db.entity.ScheduleEntity

data class ScheduleWithApps(
    val schedule: ScheduleEntity,
    val extraAllowedApps: Set<String> = emptySet()
)
