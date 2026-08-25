package com.quietinbox.feature.schedules

import com.quietinbox.data.db.entity.ScheduleEntity
import com.quietinbox.feature.schedules.model.ScheduleOverlapDetector
import com.quietinbox.feature.schedules.model.SchedulePresets
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ScheduleOverlapDetectorTest {

    @Test
    fun sameDay_overlappingHours_detected() {
        val s1 = ScheduleEntity(
            id = 1L,
            name = "Morning Work",
            startMinute = 9 * 60,  // 09:00
            endMinute = 13 * 60,   // 13:00
            daysMask = SchedulePresets.MASK_WEEKDAYS,
            policy = "QUIET",
            enabled = true,
            createdAt = 1000L
        )

        val s2 = ScheduleEntity(
            id = 2L,
            name = "Lunch Window",
            startMinute = 12 * 60, // 12:00 (overlaps from 12:00 to 13:00)
            endMinute = 14 * 60,   // 14:00
            daysMask = SchedulePresets.MASK_WEEKDAYS,
            policy = "OPEN",
            enabled = true,
            createdAt = 2000L
        )

        assertTrue(ScheduleOverlapDetector.doSchedulesOverlap(s1, s2))

        val overlap = ScheduleOverlapDetector.findOverlap(s2, listOf(s1))
        assertNotNull(overlap)
        assertTrue(overlap?.openWins == true)
        assertTrue(overlap?.explanation?.contains("Let everything through") == true)
    }

    @Test
    fun sameDay_distinctHours_noOverlap() {
        val s1 = ScheduleEntity(
            id = 1L,
            name = "Morning",
            startMinute = 9 * 60,
            endMinute = 12 * 60,
            daysMask = SchedulePresets.MASK_ALL_DAYS,
            policy = "QUIET",
            enabled = true,
            createdAt = 1000L
        )

        val s2 = ScheduleEntity(
            id = 2L,
            name = "Afternoon",
            startMinute = 14 * 60,
            endMinute = 18 * 60,
            daysMask = SchedulePresets.MASK_ALL_DAYS,
            policy = "QUIET",
            enabled = true,
            createdAt = 2000L
        )

        assertFalse(ScheduleOverlapDetector.doSchedulesOverlap(s1, s2))
        assertNull(ScheduleOverlapDetector.findOverlap(s2, listOf(s1)))
    }

    @Test
    fun sameHours_differentDays_noOverlap() {
        val s1 = ScheduleEntity(
            id = 1L,
            name = "Weekday Schedule",
            startMinute = 9 * 60,
            endMinute = 18 * 60,
            daysMask = SchedulePresets.MASK_WEEKDAYS, // Mon-Fri
            policy = "QUIET",
            enabled = true,
            createdAt = 1000L
        )

        val s2 = ScheduleEntity(
            id = 2L,
            name = "Weekend Schedule",
            startMinute = 9 * 60,
            endMinute = 18 * 60,
            daysMask = SchedulePresets.MASK_WEEKENDS, // Sat-Sun
            policy = "QUIET",
            enabled = true,
            createdAt = 2000L
        )

        assertFalse(ScheduleOverlapDetector.doSchedulesOverlap(s1, s2))
        assertNull(ScheduleOverlapDetector.findOverlap(s2, listOf(s1)))
    }

    @Test
    fun overnightSchedule_overlapsMorningSchedule() {
        // Sleep schedule: 22:00 to 07:00 Daily
        val sleep = ScheduleEntity(
            id = 1L,
            name = "Sleep",
            startMinute = 22 * 60,
            endMinute = 7 * 60,
            daysMask = SchedulePresets.MASK_ALL_DAYS,
            policy = "QUIET",
            enabled = true,
            createdAt = 1000L
        )

        // Early morning routine: 06:00 to 08:00 Daily (overlaps 06:00 to 07:00)
        val earlyMorning = ScheduleEntity(
            id = 2L,
            name = "Early Workout",
            startMinute = 6 * 60,
            endMinute = 8 * 60,
            daysMask = SchedulePresets.MASK_ALL_DAYS,
            policy = "OPEN",
            enabled = true,
            createdAt = 2000L
        )

        assertTrue(ScheduleOverlapDetector.doSchedulesOverlap(sleep, earlyMorning))
        val overlap = ScheduleOverlapDetector.findOverlap(earlyMorning, listOf(sleep))
        assertNotNull(overlap)
        assertTrue(overlap?.openWins == true)
    }
}
