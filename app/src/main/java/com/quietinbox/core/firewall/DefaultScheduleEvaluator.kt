package com.quietinbox.core.firewall

import com.quietinbox.core.time.Clock
import com.quietinbox.data.db.entity.ScheduleEntity
import com.quietinbox.feature.schedules.data.ScheduleWithApps
import com.quietinbox.feature.schedules.data.SchedulesUiDao
import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Production implementation of [ScheduleEvaluator] handling modulo-1440 time windows,
 * overnight boundaries, day-of-week bitmasks evaluated on the window's start day,
 * and precedence where OPEN policy wins over QUIET.
 */
@Singleton
class DefaultScheduleEvaluator @Inject constructor(
    private val schedulesUiDao: SchedulesUiDao
) : ScheduleEvaluator {

    /**
     * Evaluates which schedule is active at [nowEpochMs].
     *
     * @param nowEpochMs Wall clock epoch milliseconds.
     * @return The active [ActiveSchedule] (OPEN policy winning over QUIET), or null if none active.
     */
    override suspend fun activeAt(nowEpochMs: Long): ActiveSchedule? {
        val enabledSchedules = schedulesUiDao.getEnabledWithApps()
        if (enabledSchedules.isEmpty()) return null

        val zoneId = ZoneId.systemDefault()
        return evaluateSchedulesAt(enabledSchedules, nowEpochMs, zoneId)
    }

    companion object {
        /**
         * Evaluates a list of enabled [ScheduleWithApps] at the given timestamp and timezone.
         */
        fun evaluateSchedulesAt(
            schedules: List<ScheduleWithApps>,
            nowEpochMs: Long,
            zoneId: ZoneId
        ): ActiveSchedule? {
            val zdt = Instant.ofEpochMilli(nowEpochMs).atZone(zoneId)
            val currentMinuteOfDay = zdt.hour * 60 + zdt.minute
            val currentDayOfWeek = zdt.dayOfWeek

            val activeList = schedules.filter { item ->
                isScheduleActive(
                    startMinute = item.schedule.startMinute,
                    endMinute = item.schedule.endMinute,
                    daysMask = item.schedule.daysMask,
                    currentMinuteOfDay = currentMinuteOfDay,
                    currentDayOfWeek = currentDayOfWeek
                )
            }

            if (activeList.isEmpty()) return null

            // Precedence (TASKS.md §7.2): OPEN policy wins over QUIET
            val openSchedule = activeList.firstOrNull { it.schedule.policy.equals("OPEN", ignoreCase = true) }
            if (openSchedule != null) {
                return ActiveSchedule(
                    schedule = openSchedule.schedule,
                    extraAllowedApps = openSchedule.extraAllowedApps
                )
            }

            // All matching are QUIET: combine allowed apps from all active quiet schedules
            val primaryQuiet = activeList.first()
            val allExtraApps = activeList.flatMap { it.extraAllowedApps }.toSet()
            return ActiveSchedule(
                schedule = primaryQuiet.schedule,
                extraAllowedApps = allExtraApps
            )
        }

        /**
         * Checks whether a schedule window is active at [currentMinuteOfDay] and [currentDayOfWeek].
         *
         * Days bitmask format (ISO DayOfWeek 1..7):
         * Mon = bit 0 (1), Tue = bit 1 (2), Wed = bit 2 (4), Thu = bit 3 (8),
         * Fri = bit 4 (16), Sat = bit 5 (32), Sun = bit 6 (64).
         */
        fun isScheduleActive(
            startMinute: Int,
            endMinute: Int,
            daysMask: Int,
            currentMinuteOfDay: Int,
            currentDayOfWeek: DayOfWeek
        ): Boolean {
            val start = normalizeMinute(startMinute)
            val end = normalizeMinute(endMinute)
            val current = normalizeMinute(currentMinuteOfDay)

            if (start == end) {
                // 24-hour schedule
                return isDayEnabled(daysMask, currentDayOfWeek)
            }

            return if (start < end) {
                // Same-day window (e.g. 09:00 to 18:00)
                current in start until end && isDayEnabled(daysMask, currentDayOfWeek)
            } else {
                // Overnight window (e.g. 22:00 to 07:00)
                // Window starts either:
                // 1) Today evening (current >= start) -> start day is today
                // 2) Yesterday evening (current < end) -> start day was yesterday
                if (current >= start) {
                    isDayEnabled(daysMask, currentDayOfWeek)
                } else if (current < end) {
                    val yesterday = currentDayOfWeek.minus(1)
                    isDayEnabled(daysMask, yesterday)
                } else {
                    false
                }
            }
        }

        /**
         * Checks if [dayOfWeek] bit is set in [daysMask].
         */
        fun isDayEnabled(daysMask: Int, dayOfWeek: DayOfWeek): Boolean {
            val bitIndex = dayOfWeek.value - 1
            return (daysMask and (1 shl bitIndex)) != 0
        }

        /**
         * Normalizes minute of day to [0, 1439] range modulo 1440.
         */
        fun normalizeMinute(minuteOfDay: Int): Int {
            return ((minuteOfDay % 1440) + 1440) % 1440
        }
    }
}
