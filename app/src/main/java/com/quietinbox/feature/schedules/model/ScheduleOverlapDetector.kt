package com.quietinbox.feature.schedules.model

import com.quietinbox.core.firewall.DefaultScheduleEvaluator
import com.quietinbox.data.db.entity.ScheduleEntity
import java.time.DayOfWeek

/**
 * Result of checking a candidate schedule for overlaps with existing enabled schedules.
 */
data class OverlapResult(
    val conflictingSchedule: ScheduleEntity,
    val explanation: String,
    val openWins: Boolean
)

/**
 * Helper to detect schedule conflicts and compute clear, plain-language precedence warnings.
 */
object ScheduleOverlapDetector {

    /**
     * Checks if [candidate] overlaps with any enabled schedule in [existingSchedules].
     *
     * @param candidate The schedule being configured or edited.
     * @param existingSchedules The list of currently existing schedules in the database.
     * @return [OverlapResult] if a conflict exists, or null otherwise.
     */
    fun findOverlap(
        candidate: ScheduleEntity,
        existingSchedules: List<ScheduleEntity>
    ): OverlapResult? {
        for (existing in existingSchedules) {
            if (existing.id == candidate.id || !existing.enabled) continue
            if (doSchedulesOverlap(candidate, existing)) {
                val candidateIsOpen = candidate.policy.equals("OPEN", ignoreCase = true)
                val existingIsOpen = existing.policy.equals("OPEN", ignoreCase = true)

                val candidateName = candidate.name.ifBlank { "This schedule" }
                val existingName = existing.name.ifBlank { "Existing schedule" }

                val explanation = if (candidateIsOpen && !existingIsOpen) {
                    "“$candidateName” (Let everything through) will take precedence over “$existingName” (Quiet) during overlapping times."
                } else if (!candidateIsOpen && existingIsOpen) {
                    "“$existingName” (Let everything through) will take precedence over “$candidateName” (Quiet) during overlapping times."
                } else {
                    "Overlaps with “$existingName” (${ScheduleDaysHelper.formatTimeRange(existing.startMinute, existing.endMinute)})."
                }

                return OverlapResult(
                    conflictingSchedule = existing,
                    explanation = explanation,
                    openWins = candidateIsOpen || existingIsOpen
                )
            }
        }
        return null
    }

    /**
     * Tests whether two schedule entities overlap on any minute across active days.
     */
    fun doSchedulesOverlap(s1: ScheduleEntity, s2: ScheduleEntity): Boolean {
        for (day in ScheduleDaysHelper.ALL_DAYS) {
            // Check minute overlaps in 15-minute increments for efficiency
            for (m in 0 until 1440 step 15) {
                val s1Active = isMinuteActive(s1, m, day)
                val s2Active = isMinuteActive(s2, m, day)
                if (s1Active && s2Active) {
                    return true
                }
            }
        }
        return false
    }

    private fun isMinuteActive(s: ScheduleEntity, minute: Int, day: DayOfWeek): Boolean {
        return DefaultScheduleEvaluator.isScheduleActive(
            startMinute = s.startMinute,
            endMinute = s.endMinute,
            daysMask = s.daysMask,
            currentMinuteOfDay = minute,
            currentDayOfWeek = day
        )
    }
}
