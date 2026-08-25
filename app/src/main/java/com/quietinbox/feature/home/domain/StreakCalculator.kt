package com.quietinbox.feature.home.domain

import com.quietinbox.data.db.entity.DailyStatEntity
import com.quietinbox.feature.home.StatsTuning
import java.time.LocalDate

/**
 * Result data class containing the current and longest consecutive quiet streaks in days.
 */
data class StreakResult(
    val currentStreak: Int,
    val longestStreak: Int
)

/**
 * Calculator for computing current and all-time longest streaks of quiet days.
 * A quiet day is defined as having at least [StatsTuning.QUIET_DAY_MINIMUM_MINUTES] (4 hours)
 * of Quiet Mode enabled.
 */
object StreakCalculator {

    /**
     * Calculates the current and longest streaks from a list of [DailyStatEntity] records.
     *
     * @param stats List of daily statistics records.
     * @param today The reference local date representing today.
     * @return [StreakResult] containing the current and longest streak counts.
     */
    fun calculate(
        stats: List<DailyStatEntity>,
        today: LocalDate = LocalDate.now()
    ): StreakResult {
        if (stats.isEmpty()) {
            return StreakResult(currentStreak = 0, longestStreak = 0)
        }

        // Collect all distinct local dates that qualify as quiet days
        val quietDates = stats
            .filter { it.quietMinutes >= StatsTuning.QUIET_DAY_MINIMUM_MINUTES }
            .mapNotNull { parseDate(it.day) }
            .toSortedSet()

        if (quietDates.isEmpty()) {
            return StreakResult(currentStreak = 0, longestStreak = 0)
        }

        // Calculate all-time longest streak
        var longestStreak = 0
        var currentSequence = 0
        var previousDate: LocalDate? = null

        for (date in quietDates) {
            if (previousDate == null || date == previousDate.plusDays(1)) {
                currentSequence++
            } else {
                currentSequence = 1
            }
            if (currentSequence > longestStreak) {
                longestStreak = currentSequence
            }
            previousDate = date
        }

        // Calculate current streak counting back from today
        var currentStreak = 0
        var checkDate = today

        // If today is a quiet day, count today and walk backward
        if (quietDates.contains(today)) {
            while (quietDates.contains(checkDate)) {
                currentStreak++
                checkDate = checkDate.minusDays(1)
            }
        } else {
            // If today is still in progress (not yet a quiet day), check if yesterday was a quiet day
            val yesterday = today.minusDays(1)
            if (quietDates.contains(yesterday)) {
                checkDate = yesterday
                while (quietDates.contains(checkDate)) {
                    currentStreak++
                    checkDate = checkDate.minusDays(1)
                }
            }
        }

        return StreakResult(
            currentStreak = currentStreak,
            longestStreak = longestStreak
        )
    }

    /**
     * Converts a YYYYMMDD integer into a [LocalDate].
     */
    fun parseDate(dayInt: Int): LocalDate? {
        return try {
            val year = dayInt / 10000
            val month = (dayInt % 10000) / 100
            val day = dayInt % 100
            LocalDate.of(year, month, day)
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Converts a [LocalDate] into a YYYYMMDD integer.
     */
    fun toDayInt(date: LocalDate): Int {
        return date.year * 10000 + date.monthValue * 100 + date.dayOfMonth
    }
}
