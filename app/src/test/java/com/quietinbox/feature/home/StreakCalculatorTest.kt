package com.quietinbox.feature.home

import com.quietinbox.data.db.entity.DailyStatEntity
import com.quietinbox.feature.home.domain.StreakCalculator
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class StreakCalculatorTest {

    @Test
    fun `empty stats returns zero streaks`() {
        val result = StreakCalculator.calculate(
            stats = emptyList(),
            today = LocalDate.of(2026, 8, 26)
        )
        assertEquals(0, result.currentStreak)
        assertEquals(0, result.longestStreak)
    }

    @Test
    fun `days below 240 minutes do not qualify as quiet days`() {
        val stats = listOf(
            DailyStatEntity(day = 20260824, captured = 10, silenced = 5, allowed = 5, quietMinutes = 239, distinctApps = 3),
            DailyStatEntity(day = 20260825, captured = 10, silenced = 5, allowed = 5, quietMinutes = 100, distinctApps = 2),
            DailyStatEntity(day = 20260826, captured = 10, silenced = 5, allowed = 5, quietMinutes = 0, distinctApps = 1)
        )
        val result = StreakCalculator.calculate(
            stats = stats,
            today = LocalDate.of(2026, 8, 26)
        )
        assertEquals(0, result.currentStreak)
        assertEquals(0, result.longestStreak)
    }

    @Test
    fun `today only quiet day returns streak of 1`() {
        val today = LocalDate.of(2026, 8, 26)
        val stats = listOf(
            DailyStatEntity(day = 20260826, captured = 15, silenced = 10, allowed = 5, quietMinutes = 240, distinctApps = 4)
        )
        val result = StreakCalculator.calculate(
            stats = stats,
            today = today
        )
        assertEquals(1, result.currentStreak)
        assertEquals(1, result.longestStreak)
    }

    @Test
    fun `gaps between quiet days computes correct longest and current streaks`() {
        val today = LocalDate.of(2026, 8, 26)
        val stats = listOf(
            // 3-day streak: Aug 18, 19, 20
            DailyStatEntity(day = 20260818, captured = 10, silenced = 8, allowed = 2, quietMinutes = 300, distinctApps = 2),
            DailyStatEntity(day = 20260819, captured = 10, silenced = 8, allowed = 2, quietMinutes = 250, distinctApps = 2),
            DailyStatEntity(day = 20260820, captured = 10, silenced = 8, allowed = 2, quietMinutes = 240, distinctApps = 2),
            // Gap on Aug 21 (only 120 mins)
            DailyStatEntity(day = 20260821, captured = 10, silenced = 8, allowed = 2, quietMinutes = 120, distinctApps = 2),
            // 2-day streak ending today: Aug 25, 26
            DailyStatEntity(day = 20260825, captured = 10, silenced = 8, allowed = 2, quietMinutes = 400, distinctApps = 2),
            DailyStatEntity(day = 20260826, captured = 10, silenced = 8, allowed = 2, quietMinutes = 260, distinctApps = 2)
        )
        val result = StreakCalculator.calculate(
            stats = stats,
            today = today
        )
        assertEquals(2, result.currentStreak)
        assertEquals(3, result.longestStreak)
    }

    @Test
    fun `a 13-day run computes 13 for current and longest streak`() {
        val today = LocalDate.of(2026, 8, 26)
        val stats = (0..12).map { offset ->
            val date = today.minusDays(offset.toLong())
            val dayInt = StreakCalculator.toDayInt(date)
            DailyStatEntity(day = dayInt, captured = 20, silenced = 15, allowed = 5, quietMinutes = 300, distinctApps = 5)
        }
        val result = StreakCalculator.calculate(
            stats = stats,
            today = today
        )
        assertEquals(13, result.currentStreak)
        assertEquals(13, result.longestStreak)
    }

    @Test
    fun `today in progress inherits streak ending yesterday`() {
        val today = LocalDate.of(2026, 8, 26)
        val stats = listOf(
            DailyStatEntity(day = 20260824, captured = 20, silenced = 15, allowed = 5, quietMinutes = 300, distinctApps = 3),
            DailyStatEntity(day = 20260825, captured = 20, silenced = 15, allowed = 5, quietMinutes = 250, distinctApps = 3),
            DailyStatEntity(day = 20260826, captured = 5, silenced = 2, allowed = 3, quietMinutes = 60, distinctApps = 1) // today in progress (< 240)
        )
        val result = StreakCalculator.calculate(
            stats = stats,
            today = today
        )
        assertEquals(2, result.currentStreak)
        assertEquals(2, result.longestStreak)
    }

    @Test
    fun `month and year transitions compute seamlessly`() {
        val today = LocalDate.of(2027, 1, 2)
        val stats = listOf(
            DailyStatEntity(day = 20261230, captured = 10, silenced = 5, allowed = 5, quietMinutes = 300, distinctApps = 2),
            DailyStatEntity(day = 20261231, captured = 10, silenced = 5, allowed = 5, quietMinutes = 300, distinctApps = 2),
            DailyStatEntity(day = 20270101, captured = 10, silenced = 5, allowed = 5, quietMinutes = 300, distinctApps = 2),
            DailyStatEntity(day = 20270102, captured = 10, silenced = 5, allowed = 5, quietMinutes = 300, distinctApps = 2)
        )
        val result = StreakCalculator.calculate(
            stats = stats,
            today = today
        )
        assertEquals(4, result.currentStreak)
        assertEquals(4, result.longestStreak)
    }

    @Test
    fun `leap year feb 28 to feb 29 to mar 1 transition`() {
        val today = LocalDate.of(2028, 3, 1)
        val stats = listOf(
            DailyStatEntity(day = 20280228, captured = 10, silenced = 5, allowed = 5, quietMinutes = 300, distinctApps = 2),
            DailyStatEntity(day = 20280229, captured = 10, silenced = 5, allowed = 5, quietMinutes = 300, distinctApps = 2),
            DailyStatEntity(day = 20280301, captured = 10, silenced = 5, allowed = 5, quietMinutes = 300, distinctApps = 2)
        )
        val result = StreakCalculator.calculate(
            stats = stats,
            today = today
        )
        assertEquals(3, result.currentStreak)
        assertEquals(3, result.longestStreak)
    }
}
