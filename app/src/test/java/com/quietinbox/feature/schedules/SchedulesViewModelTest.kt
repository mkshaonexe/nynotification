package com.quietinbox.feature.schedules

import com.quietinbox.core.firewall.ActiveSchedule
import com.quietinbox.core.firewall.ScheduleEvaluator
import com.quietinbox.core.time.Clock
import com.quietinbox.data.db.entity.ScheduleAppEntity
import com.quietinbox.data.db.entity.ScheduleEntity
import com.quietinbox.feature.schedules.data.ScheduleWithApps
import com.quietinbox.feature.schedules.data.SchedulesUiDao
import com.quietinbox.feature.schedules.model.PresetType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
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

@OptIn(ExperimentalCoroutinesApi::class)
class SchedulesViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private class FakeSchedulesUiDao : SchedulesUiDao {
        val itemsFlow = MutableStateFlow<List<ScheduleWithApps>>(emptyList())
        val savedSchedules = mutableListOf<ScheduleEntity>()

        override fun observeAllWithApps(): Flow<List<ScheduleWithApps>> = itemsFlow
        override suspend fun getEnabledWithApps(): List<ScheduleWithApps> = itemsFlow.value.filter { it.schedule.enabled }
        override suspend fun getByIdWithApps(id: Long): ScheduleWithApps? = itemsFlow.value.firstOrNull { it.schedule.id == id }
        override suspend fun getById(id: Long): ScheduleEntity? = itemsFlow.value.firstOrNull { it.schedule.id == id }?.schedule
        override suspend fun insertSchedule(schedule: ScheduleEntity): Long {
            val generatedId = if (schedule.id == 0L) (savedSchedules.size + 1).toLong() else schedule.id
            val updated = schedule.copy(id = generatedId)
            savedSchedules.add(updated)
            itemsFlow.value = savedSchedules.map { ScheduleWithApps(it, emptyList()) }
            return generatedId
        }
        override suspend fun updateSchedule(schedule: ScheduleEntity) {
            val index = savedSchedules.indexOfFirst { it.id == schedule.id }
            if (index >= 0) {
                savedSchedules[index] = schedule
                itemsFlow.value = savedSchedules.map { ScheduleWithApps(it, emptyList()) }
            }
        }
        override suspend fun setEnabled(id: Long, enabled: Boolean) {
            val index = savedSchedules.indexOfFirst { it.id == id }
            if (index >= 0) {
                savedSchedules[index] = savedSchedules[index].copy(enabled = enabled)
                itemsFlow.value = savedSchedules.map { ScheduleWithApps(it, emptyList()) }
            }
        }
        override suspend fun deleteSchedule(schedule: ScheduleEntity) {
            deleteScheduleById(schedule.id)
        }
        override suspend fun deleteScheduleById(id: Long) {
            savedSchedules.removeAll { it.id == id }
            itemsFlow.value = savedSchedules.map { ScheduleWithApps(it, emptyList()) }
        }
        override suspend fun insertApps(apps: List<ScheduleAppEntity>) {}
        override suspend fun deleteAppsForSchedule(scheduleId: Long) {}
        override suspend fun getAppsForSchedule(scheduleId: Long): List<String> = emptyList()
    }

    private class FakeScheduleEvaluator(var activeSchedule: ActiveSchedule? = null) : ScheduleEvaluator {
        override suspend fun activeAt(nowEpochMs: Long): ActiveSchedule? = activeSchedule
    }

    private class FakeClock(var timeMs: Long = 1000L) : Clock {
        override fun now(): Long = timeMs
        override fun elapsedRealtime(): Long = timeMs
    }

    private lateinit var fakeDao: FakeSchedulesUiDao
    private lateinit var fakeEvaluator: FakeScheduleEvaluator
    private lateinit var fakeClock: FakeClock
    private lateinit var viewModel: SchedulesViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeDao = FakeSchedulesUiDao()
        fakeEvaluator = FakeScheduleEvaluator()
        fakeClock = FakeClock()
        viewModel = SchedulesViewModel(fakeDao, fakeEvaluator, fakeClock)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun applyPreset_insertsPresetIntoDatabase() = runTest(testDispatcher) {
        viewModel.applyPreset(PresetType.SLEEP)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.first()
        assertEquals(1, state.schedules.size)
        assertEquals("Sleep", state.schedules.first().schedule.name)
        assertEquals(22 * 60, state.schedules.first().schedule.startMinute)
        assertEquals(7 * 60, state.schedules.first().schedule.endMinute)
    }

    @Test
    fun toggleSchedule_updatesEnabledState() = runTest(testDispatcher) {
        viewModel.applyPreset(PresetType.WORK)
        testDispatcher.scheduler.advanceUntilIdle()

        val scheduleId = fakeDao.savedSchedules.first().id
        viewModel.toggleSchedule(scheduleId, false)
        testDispatcher.scheduler.advanceUntilIdle()

        val updated = fakeDao.savedSchedules.first()
        assertFalse(updated.enabled)
    }

    @Test
    fun deleteSchedule_removesSchedule() = runTest(testDispatcher) {
        viewModel.applyPreset(PresetType.PRAYER)
        testDispatcher.scheduler.advanceUntilIdle()

        val scheduleId = fakeDao.savedSchedules.first().id
        viewModel.deleteSchedule(scheduleId)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(fakeDao.savedSchedules.isEmpty())
    }
}
