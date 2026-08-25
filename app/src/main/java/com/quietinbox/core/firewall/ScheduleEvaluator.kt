package com.quietinbox.core.firewall

import com.quietinbox.data.db.entity.ScheduleEntity

/**
 * An active schedule window along with its extra allowed apps for the current time.
 *
 * @property schedule The active [ScheduleEntity].
 * @property extraAllowedApps The set of package names allowed specifically during this schedule.
 */
data class ActiveSchedule(
    val schedule: ScheduleEntity,
    val extraAllowedApps: Set<String>
)

/**
 * Interface for evaluating whether any quiet/open schedule window is currently active.
 */
interface ScheduleEvaluator {
    /**
     * Evaluates which schedule is active at [nowEpochMs].
     *
     * @param nowEpochMs Wall clock epoch milliseconds. Handles windows that cross midnight.
     * @return The [ActiveSchedule] if a schedule is currently active, or null otherwise.
     */
    suspend fun activeAt(nowEpochMs: Long): ActiveSchedule?
}
