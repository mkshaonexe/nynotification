package com.quietinbox.core.firewall

import com.quietinbox.data.db.entity.ScheduleEntity

data class ActiveSchedule(val schedule: ScheduleEntity, val extraAllowedApps: Set<String>)

interface ScheduleEvaluator {
    /** @param nowEpochMs wall clock. Must handle windows that cross midnight. */
    suspend fun activeAt(nowEpochMs: Long): ActiveSchedule?
}
