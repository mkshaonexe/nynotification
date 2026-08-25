package com.quietinbox.feature.schedules

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quietinbox.core.firewall.ScheduleEvaluator
import com.quietinbox.core.time.Clock
import com.quietinbox.data.db.entity.ScheduleEntity
import com.quietinbox.feature.schedules.data.SchedulesUiDao
import com.quietinbox.feature.schedules.model.PresetType
import com.quietinbox.feature.schedules.model.ScheduleItemUi
import com.quietinbox.feature.schedules.model.SchedulePresets
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UI state for the schedules list screen.
 */
data class SchedulesUiState(
    val schedules: List<ScheduleItemUi> = emptyList(),
    val activeScheduleId: Long? = null,
    val isLoading: Boolean = false
)

/**
 * ViewModel managing the schedules list screen, active status tracking, and quick presets.
 */
@HiltViewModel
class SchedulesViewModel @Inject constructor(
    private val schedulesUiDao: SchedulesUiDao,
    private val scheduleEvaluator: ScheduleEvaluator,
    private val clock: Clock
) : ViewModel() {

    private val _refreshTrigger = MutableStateFlow(0L)

    val uiState: StateFlow<SchedulesUiState> = combine(
        schedulesUiDao.observeAllWithApps(),
        _refreshTrigger
    ) { schedulesWithApps, _ ->
        val active = scheduleEvaluator.activeAt(clock.now())
        val items = schedulesWithApps.map { item ->
            ScheduleItemUi(
                schedule = item.schedule,
                extraAllowedApps = item.extraAllowedApps,
                isActiveNow = active?.schedule?.id == item.schedule.id
            )
        }
        SchedulesUiState(
            schedules = items,
            activeScheduleId = active?.schedule?.id,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SchedulesUiState(isLoading = true)
    )

    /**
     * Toggles the enabled state of a schedule.
     */
    fun toggleSchedule(scheduleId: Long, enabled: Boolean) {
        viewModelScope.launch {
            schedulesUiDao.setEnabled(scheduleId, enabled)
            _refreshTrigger.value = clock.now()
        }
    }

    /**
     * Deletes a schedule by its ID.
     */
    fun deleteSchedule(scheduleId: Long) {
        viewModelScope.launch {
            schedulesUiDao.deleteScheduleById(scheduleId)
            _refreshTrigger.value = clock.now()
        }
    }

    /**
     * Creates and saves a preset schedule (Sleep, Work, or Prayer) into the database.
     */
    fun applyPreset(presetType: PresetType) {
        viewModelScope.launch {
            val now = clock.now()
            val preset = when (presetType) {
                PresetType.SLEEP -> SchedulePresets.createSleepPreset(now)
                PresetType.WORK -> SchedulePresets.createWorkPreset(now)
                PresetType.PRAYER -> SchedulePresets.createPrayerPreset(now)
            }
            schedulesUiDao.insertSchedule(preset)
            _refreshTrigger.value = now
        }
    }

    /**
     * Refreshes active status computation against the clock.
     */
    fun refresh() {
        _refreshTrigger.value = clock.now()
    }
}
