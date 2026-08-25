package com.quietinbox.feature.schedules

import androidx.lifecycle.SavedStateHandle
import com.quietinbox.core.apps.InstalledApp
import com.quietinbox.core.apps.InstalledAppsProvider
import com.quietinbox.core.time.Clock
import com.quietinbox.data.db.entity.ScheduleAppEntity
import com.quietinbox.data.db.entity.ScheduleEntity
import com.quietinbox.feature.schedules.data.ScheduleWithApps
import com.quietinbox.feature.schedules.data.SchedulesUiDao
import com.quietinbox.feature.schedules.model.SchedulePresets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.time.DayOfWeek

@OptIn(ExperimentalCoroutinesApi::class)
class ScheduleEditViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private class FakeSchedulesUiDao : SchedulesUiDao {
        val schedules = mutableListOf<ScheduleEntity>()
        val scheduleApps = mutableListOf<ScheduleAppEntity>()

        override fun observeAllWithApps(): Flow<List<ScheduleWithApps>> = flowOf(emptyList())
        override suspend fun getEnabledWithApps(): List<ScheduleWithApps> = schedules.filter { it.enabled }.map { ScheduleWithApps(it, emptyList()) }
        override suspend fun getByIdWithApps(id: Long): ScheduleWithApps? {
            val entity = schedules.firstOrNull { it.id == id } ?: return null
            val apps = scheduleApps.filter { it.scheduleId == id }
            return ScheduleWithApps(entity, apps)
        }
        override suspend fun getById(id: Long): ScheduleEntity? = schedules.firstOrNull { it.id == id }
        override suspend fun insertSchedule(schedule: ScheduleEntity): Long {
            val id = if (schedule.id == 0L) (schedules.size + 1).toLong() else schedule.id
            schedules.add(schedule.copy(id = id))
            return id
        }
        override suspend fun updateSchedule(schedule: ScheduleEntity) {
            val index = schedules.indexOfFirst { it.id == schedule.id }
            if (index >= 0) schedules[index] = schedule
        }
        override suspend fun setEnabled(id: Long, enabled: Boolean) {}
        override suspend fun deleteSchedule(schedule: ScheduleEntity) {}
        override suspend fun deleteScheduleById(id: Long) {
            schedules.removeAll { it.id == id }
            scheduleApps.removeAll { it.scheduleId == id }
        }
        override suspend fun insertApps(apps: List<ScheduleAppEntity>) {
            scheduleApps.addAll(apps)
        }
        override suspend fun deleteAppsForSchedule(scheduleId: Long) {
            scheduleApps.removeAll { it.scheduleId == scheduleId }
        }
        override suspend fun getAppsForSchedule(scheduleId: Long): List<String> = scheduleApps.filter { it.scheduleId == scheduleId }.map { it.packageName }
    }

    private class FakeInstalledAppsProvider : InstalledAppsProvider {
        override suspend fun all(includeSystemComponents: Boolean): List<InstalledApp> {
            return listOf(
                InstalledApp("com.whatsapp", "WhatsApp", isLaunchable = true, isPreinstalled = false, wasUpdatedSystemApp = false, notificationCount = 10),
                InstalledApp("com.slack", "Slack", isLaunchable = true, isPreinstalled = false, wasUpdatedSystemApp = false, notificationCount = 5)
            )
        }
        override suspend fun label(packageName: String): String = packageName
        override fun iconFile(packageName: String): File = File("/tmp/$packageName.webp")
    }

    private class FakeClock(var timeMs: Long = 1000L) : Clock {
        override fun now(): Long = timeMs
        override fun elapsedRealtime(): Long = timeMs
    }

    private lateinit var fakeDao: FakeSchedulesUiDao
    private lateinit var fakeAppsProvider: FakeInstalledAppsProvider
    private lateinit var fakeClock: FakeClock

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeDao = FakeSchedulesUiDao()
        fakeAppsProvider = FakeInstalledAppsProvider()
        fakeClock = FakeClock()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun createNewSchedule_save_writesToDao() = runTest(testDispatcher) {
        val savedStateHandle = SavedStateHandle(mapOf("id" to 0L))
        val viewModel = ScheduleEditViewModel(fakeDao, fakeAppsProvider, fakeClock, savedStateHandle)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.updateName("Deep Focus")
        viewModel.updateTimeRange(9 * 60, 12 * 60)
        viewModel.setDaysMask(SchedulePresets.MASK_WEEKDAYS)
        viewModel.updatePolicy("QUIET")
        viewModel.toggleExtraApp("com.slack")

        viewModel.save()
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isSaved)
        assertEquals(1, fakeDao.schedules.size)
        val saved = fakeDao.schedules.first()
        assertEquals("Deep Focus", saved.name)
        assertEquals(9 * 60, saved.startMinute)
        assertEquals(12 * 60, saved.endMinute)
        assertEquals(SchedulePresets.MASK_WEEKDAYS, saved.daysMask)
        assertEquals("QUIET", saved.policy)

        assertEquals(1, fakeDao.scheduleApps.size)
        assertEquals("com.slack", fakeDao.scheduleApps.first().packageName)
    }

    @Test
    fun editExistingSchedule_loadsAndUpdates() = runTest(testDispatcher) {
        val existingEntity = ScheduleEntity(
            id = 42L,
            name = "Old Name",
            startMinute = 10 * 60,
            endMinute = 11 * 60,
            daysMask = SchedulePresets.MASK_ALL_DAYS,
            policy = "QUIET",
            enabled = true,
            createdAt = 1000L
        )
        fakeDao.schedules.add(existingEntity)
        fakeDao.scheduleApps.add(ScheduleAppEntity(42L, "com.whatsapp"))

        val savedStateHandle = SavedStateHandle(mapOf("id" to 42L))
        val viewModel = ScheduleEditViewModel(fakeDao, fakeAppsProvider, fakeClock, savedStateHandle)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("Old Name", viewModel.uiState.value.name)
        assertEquals(10 * 60, viewModel.uiState.value.startMinute)
        assertTrue(viewModel.uiState.value.extraAllowedApps.contains("com.whatsapp"))

        viewModel.updateName("Updated Name")
        viewModel.save()
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isSaved)
        val updated = fakeDao.schedules.first { it.id == 42L }
        assertEquals("Updated Name", updated.name)
    }

    @Test
    fun toggleDay_modifiesDaysMask() = runTest(testDispatcher) {
        val savedStateHandle = SavedStateHandle(mapOf("id" to 0L))
        val viewModel = ScheduleEditViewModel(fakeDao, fakeAppsProvider, fakeClock, savedStateHandle)
        testDispatcher.scheduler.advanceUntilIdle()

        // Start with all days (127)
        viewModel.setDaysMask(SchedulePresets.MASK_ALL_DAYS)
        assertEquals(127, viewModel.uiState.value.daysMask)

        // Toggle Monday (bit 0 = 1) -> 126
        viewModel.toggleDay(DayOfWeek.MONDAY)
        assertEquals(126, viewModel.uiState.value.daysMask)

        // Toggle Monday again -> 127
        viewModel.toggleDay(DayOfWeek.MONDAY)
        assertEquals(127, viewModel.uiState.value.daysMask)
    }
}
