package com.quietinbox.core.firewall

import com.quietinbox.core.time.Clock
import com.quietinbox.data.db.entity.ScheduleAppEntity
import com.quietinbox.data.db.entity.ScheduleEntity
import com.quietinbox.feature.schedules.data.ScheduleWithApps
import com.quietinbox.feature.schedules.data.SchedulesUiDao
import com.quietinbox.feature.schedules.model.SchedulePresets
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.ZoneId

class ScheduleEvaluatorTest {

    private val zoneId = ZoneId.of("UTC")

    private fun localDateTimeToEpochMs(ldt: LocalDateTime): Long {
        return ldt.atZone(zoneId).toInstant().toEpochMilli()
    }

    private class FakeSchedulesUiDao(
        var schedules: List<ScheduleWithApps> = emptyList()
    ) : SchedulesUiDao {
        override fun observeAllWithApps(): Flow<List<ScheduleWithApps>> = flowOf(schedules)
        override suspend fun getEnabledWithApps(): List<ScheduleWithApps> = schedules.filter { it.schedule.enabled }
        override suspend fun getByIdWithApps(id: Long): ScheduleWithApps? = schedules.firstOrNull { it.schedule.id == id }
        override suspend fun getById(id: Long): ScheduleEntity? = schedules.firstOrNull { it.schedule.id == id }?.schedule
        override suspend fun insertSchedule(schedule: ScheduleEntity): Long = schedule.id
        override suspend fun updateSchedule(schedule: ScheduleEntity) {}
        override suspend fun setEnabled(id: Long, enabled: Boolean) {}
        override suspend fun deleteSchedule(schedule: ScheduleEntity) {}
        override suspend fun deleteScheduleById(id: Long) {}
        override suspend fun insertApps(apps: List<ScheduleAppEntity>) {}
        override suspend fun deleteAppsForSchedule(scheduleId: Long) {}
        override suspend fun getAppsForSchedule(scheduleId: Long): List<String> = emptyList()
    }

    private class FakeClock(var currentEpochMs: Long = 0L) : Clock {
        override fun now(): Long = currentEpochMs
        override fun elapsedRealtime(): Long = currentEpochMs
    }

    @Test
    fun overnightWindow_activeDuringNightAndMorning_notDuringDay() = runTest {
        // 22:00 to 07:00 Daily
        val sleepSchedule = ScheduleEntity(
            id = 1L,
            name = "Sleep",
            startMinute = 22 * 60, // 1320
            endMinute = 7 * 60,    // 420
            daysMask = SchedulePresets.MASK_ALL_DAYS,
            policy = "QUIET",
            enabled = true,
            createdAt = 1000L
        )

        val scheduleWithApps = ScheduleWithApps(
            schedule = sleepSchedule,
            appEntities = listOf(ScheduleAppEntity(1L, "com.whatsapp"))
        )

        val fakeDao = FakeSchedulesUiDao(listOf(scheduleWithApps))
        val evaluator = DefaultScheduleEvaluator(fakeDao)

        // Monday 21:59 (before start) -> inactive
        val t2159 = localDateTimeToEpochMs(LocalDateTime.of(2026, 8, 24, 21, 59)) // Monday
        assertNull(DefaultScheduleEvaluator.evaluateSchedulesAt(fakeDao.getEnabledWithApps(), t2159, zoneId))

        // Monday 22:00 (exact start boundary) -> active
        val t2200 = localDateTimeToEpochMs(LocalDateTime.of(2026, 8, 24, 22, 0))
        val active2200 = DefaultScheduleEvaluator.evaluateSchedulesAt(fakeDao.getEnabledWithApps(), t2200, zoneId)
        assertNotNull(active2200)
        assertEquals(1L, active2200?.schedule?.id)
        assertTrue(active2200?.extraAllowedApps?.contains("com.whatsapp") == true)

        // Monday 23:30 -> active
        val t2330 = localDateTimeToEpochMs(LocalDateTime.of(2026, 8, 24, 23, 30))
        assertNotNull(DefaultScheduleEvaluator.evaluateSchedulesAt(fakeDao.getEnabledWithApps(), t2330, zoneId))

        // Tuesday 00:00 -> active
        val t0000 = localDateTimeToEpochMs(LocalDateTime.of(2026, 8, 25, 0, 0))
        assertNotNull(DefaultScheduleEvaluator.evaluateSchedulesAt(fakeDao.getEnabledWithApps(), t0000, zoneId))

        // Tuesday 06:30 -> active
        val t0630 = localDateTimeToEpochMs(LocalDateTime.of(2026, 8, 25, 6, 30))
        assertNotNull(DefaultScheduleEvaluator.evaluateSchedulesAt(fakeDao.getEnabledWithApps(), t0630, zoneId))

        // Tuesday 06:59 -> active
        val t0659 = localDateTimeToEpochMs(LocalDateTime.of(2026, 8, 25, 6, 59))
        assertNotNull(DefaultScheduleEvaluator.evaluateSchedulesAt(fakeDao.getEnabledWithApps(), t0659, zoneId))

        // Tuesday 07:00 (exact end boundary) -> inactive
        val t0700 = localDateTimeToEpochMs(LocalDateTime.of(2026, 8, 25, 7, 0))
        assertNull(DefaultScheduleEvaluator.evaluateSchedulesAt(fakeDao.getEnabledWithApps(), t0700, zoneId))

        // Tuesday 08:00 -> inactive
        val t0800 = localDateTimeToEpochMs(LocalDateTime.of(2026, 8, 25, 8, 0))
        assertNull(DefaultScheduleEvaluator.evaluateSchedulesAt(fakeDao.getEnabledWithApps(), t0800, zoneId))
    }

    @Test
    fun overnightWindow_dayMaskEvaluatedAgainstStartDay() {
        // Friday-only overnight schedule: 22:00 to 07:00 (Friday mask = 1 shl 4 = 16)
        val fridayMask = 1 shl (DayOfWeek.FRIDAY.value - 1) // 16

        // Friday 23:00 -> Start day is Friday -> Active
        val friday2300Active = DefaultScheduleEvaluator.isScheduleActive(
            startMinute = 1320,
            endMinute = 420,
            daysMask = fridayMask,
            currentMinuteOfDay = 23 * 60,
            currentDayOfWeek = DayOfWeek.FRIDAY
        )
        assertTrue(friday2300Active)

        // Saturday 05:00 -> Start day was Friday -> Active
        val saturday0500Active = DefaultScheduleEvaluator.isScheduleActive(
            startMinute = 1320,
            endMinute = 420,
            daysMask = fridayMask,
            currentMinuteOfDay = 5 * 60,
            currentDayOfWeek = DayOfWeek.SATURDAY
        )
        assertTrue(saturday0500Active)

        // Saturday 23:00 -> Start day is Saturday -> Inactive
        val saturday2300Active = DefaultScheduleEvaluator.isScheduleActive(
            startMinute = 1320,
            endMinute = 420,
            daysMask = fridayMask,
            currentMinuteOfDay = 23 * 60,
            currentDayOfWeek = DayOfWeek.SATURDAY
        )
        assertTrue(!saturday2300Active)

        // Sunday 05:00 -> Start day was Saturday -> Inactive
        val sunday0500Active = DefaultScheduleEvaluator.isScheduleActive(
            startMinute = 1320,
            endMinute = 420,
            daysMask = fridayMask,
            currentMinuteOfDay = 5 * 60,
            currentDayOfWeek = DayOfWeek.SUNDAY
        )
        assertTrue(!sunday0500Active)
    }

    @Test
    fun precedence_openWinsOverQuiet() {
        val quietSchedule = ScheduleEntity(
            id = 1L,
            name = "Quiet Work",
            startMinute = 9 * 60,
            endMinute = 18 * 60,
            daysMask = SchedulePresets.MASK_ALL_DAYS,
            policy = "QUIET",
            enabled = true,
            createdAt = 1000L
        )

        val openSchedule = ScheduleEntity(
            id = 2L,
            name = "Lunch Break (Open)",
            startMinute = 12 * 60,
            endMinute = 13 * 60,
            daysMask = SchedulePresets.MASK_ALL_DAYS,
            policy = "OPEN",
            enabled = true,
            createdAt = 2000L
        )

        val list = listOf(
            ScheduleWithApps(quietSchedule, emptyList()),
            ScheduleWithApps(openSchedule, emptyList())
        )

        // At 12:30 (during lunch break, both schedules active): OPEN must win!
        val t1230 = localDateTimeToEpochMs(LocalDateTime.of(2026, 8, 24, 12, 30))
        val active = DefaultScheduleEvaluator.evaluateSchedulesAt(list, t1230, zoneId)

        assertNotNull(active)
        assertEquals(2L, active?.schedule?.id)
        assertEquals("OPEN", active?.schedule?.policy)

        // At 10:00 (only quiet active): QUIET active
        val t1000 = localDateTimeToEpochMs(LocalDateTime.of(2026, 8, 24, 10, 0))
        val active1000 = DefaultScheduleEvaluator.evaluateSchedulesAt(list, t1000, zoneId)

        assertNotNull(active1000)
        assertEquals(1L, active1000?.schedule?.id)
        assertEquals("QUIET", active1000?.schedule?.policy)
    }

    @Test
    fun disabledSchedule_isIgnored() {
        val disabledSchedule = ScheduleEntity(
            id = 1L,
            name = "Disabled Sleep",
            startMinute = 22 * 60,
            endMinute = 7 * 60,
            daysMask = SchedulePresets.MASK_ALL_DAYS,
            policy = "QUIET",
            enabled = false,
            createdAt = 1000L
        )

        val list = listOf(ScheduleWithApps(disabledSchedule, emptyList())).filter { it.schedule.enabled }
        val t2300 = localDateTimeToEpochMs(LocalDateTime.of(2026, 8, 24, 23, 0))
        val active = DefaultScheduleEvaluator.evaluateSchedulesAt(list, t2300, zoneId)

        assertNull(active)
    }

    @Test
    fun multipleQuietSchedules_unionsAllowedApps() {
        val quiet1 = ScheduleEntity(
            id = 1L,
            name = "Quiet 1",
            startMinute = 9 * 60,
            endMinute = 18 * 60,
            daysMask = SchedulePresets.MASK_ALL_DAYS,
            policy = "QUIET",
            enabled = true,
            createdAt = 1000L
        )
        val quiet2 = ScheduleEntity(
            id = 2L,
            name = "Quiet 2",
            startMinute = 10 * 60,
            endMinute = 12 * 60,
            daysMask = SchedulePresets.MASK_ALL_DAYS,
            policy = "QUIET",
            enabled = true,
            createdAt = 2000L
        )

        val list = listOf(
            ScheduleWithApps(quiet1, listOf(ScheduleAppEntity(1L, "com.slack"))),
            ScheduleWithApps(quiet2, listOf(ScheduleAppEntity(2L, "com.telegram")))
        )

        val t1100 = localDateTimeToEpochMs(LocalDateTime.of(2026, 8, 24, 11, 0))
        val active = DefaultScheduleEvaluator.evaluateSchedulesAt(list, t1100, zoneId)

        assertNotNull(active)
        assertTrue(active?.extraAllowedApps?.contains("com.slack") == true)
        assertTrue(active?.extraAllowedApps?.contains("com.telegram") == true)
    }
}
