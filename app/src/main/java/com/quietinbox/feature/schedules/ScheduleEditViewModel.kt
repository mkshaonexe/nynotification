package com.quietinbox.feature.schedules

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quietinbox.core.apps.InstalledApp
import com.quietinbox.core.apps.InstalledAppsProvider
import com.quietinbox.core.time.Clock
import com.quietinbox.data.db.entity.ScheduleEntity
import com.quietinbox.feature.schedules.data.SchedulesUiDao
import com.quietinbox.feature.schedules.model.OverlapResult
import com.quietinbox.feature.schedules.model.ScheduleDaysHelper
import com.quietinbox.feature.schedules.model.ScheduleOverlapDetector
import com.quietinbox.feature.schedules.model.SchedulePresets
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import javax.inject.Inject

/**
 * UI State for the schedule create/edit screen.
 */
data class ScheduleEditUiState(
    val id: Long = 0L,
    val name: String = "",
    val startMinute: Int = 22 * 60, // 22:00
    val endMinute: Int = 7 * 60,    // 07:00
    val daysMask: Int = SchedulePresets.MASK_ALL_DAYS,
    val policy: String = "QUIET",
    val enabled: Boolean = true,
    val extraAllowedApps: Set<String> = emptySet(),
    val availableApps: List<InstalledApp> = emptyList(),
    val showSystemComponents: Boolean = false,
    val overlapWarning: OverlapResult? = null,
    val isLoading: Boolean = false,
    val isSaved: Boolean = false,
    val isDeleted: Boolean = false
)

/**
 * ViewModel managing the creation and editing of a schedule, app selection, and overlap detection.
 */
@HiltViewModel
class ScheduleEditViewModel @Inject constructor(
    private val schedulesUiDao: SchedulesUiDao,
    private val installedAppsProvider: InstalledAppsProvider,
    private val clock: Clock,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val scheduleId: Long = savedStateHandle.get<Long>("id") ?: 0L

    private val _uiState = MutableStateFlow(ScheduleEditUiState(id = scheduleId, isLoading = true))
    val uiState: StateFlow<ScheduleEditUiState> = _uiState.asStateFlow()

    private var allExistingSchedules: List<ScheduleEntity> = emptyList()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            val existing = schedulesUiDao.getEnabledWithApps().map { it.schedule }
            allExistingSchedules = existing

            val apps = installedAppsProvider.all(includeSystemComponents = _uiState.value.showSystemComponents)

            if (scheduleId > 0L) {
                val item = schedulesUiDao.getByIdWithApps(scheduleId)
                if (item != null) {
                    val entity = item.schedule
                    _uiState.update {
                        it.copy(
                            id = entity.id,
                            name = entity.name,
                            startMinute = entity.startMinute,
                            endMinute = entity.endMinute,
                            daysMask = entity.daysMask,
                            policy = entity.policy,
                            enabled = entity.enabled,
                            extraAllowedApps = item.extraAllowedApps,
                            availableApps = apps,
                            isLoading = false
                        )
                    }
                    checkOverlap()
                    return@launch
                }
            }

            _uiState.update {
                it.copy(
                    availableApps = apps,
                    isLoading = false
                )
            }
            checkOverlap()
        }
    }

    fun updateName(name: String) {
        _uiState.update { it.copy(name = name) }
        checkOverlap()
    }

    fun updateTimeRange(startMinute: Int, endMinute: Int) {
        _uiState.update { it.copy(startMinute = startMinute, endMinute = endMinute) }
        checkOverlap()
    }

    fun toggleDay(day: DayOfWeek) {
        _uiState.update {
            val newMask = ScheduleDaysHelper.toggleDay(it.daysMask, day)
            it.copy(daysMask = newMask)
        }
        checkOverlap()
    }

    fun setDaysMask(mask: Int) {
        _uiState.update { it.copy(daysMask = mask) }
        checkOverlap()
    }

    fun updatePolicy(policy: String) {
        _uiState.update { it.copy(policy = policy) }
        checkOverlap()
    }

    fun toggleExtraApp(packageName: String) {
        _uiState.update {
            val updated = it.extraAllowedApps.toMutableSet()
            if (updated.contains(packageName)) {
                updated.remove(packageName)
            } else {
                updated.add(packageName)
            }
            it.copy(extraAllowedApps = updated)
        }
    }

    fun setShowSystemComponents(show: Boolean) {
        _uiState.update { it.copy(showSystemComponents = show) }
        viewModelScope.launch {
            val apps = installedAppsProvider.all(includeSystemComponents = show)
            _uiState.update { it.copy(availableApps = apps) }
        }
    }

    private fun checkOverlap() {
        val current = _uiState.value
        val candidate = ScheduleEntity(
            id = current.id,
            name = current.name,
            startMinute = current.startMinute,
            endMinute = current.endMinute,
            daysMask = current.daysMask,
            policy = current.policy,
            enabled = current.enabled,
            createdAt = clock.now()
        )
        val overlap = ScheduleOverlapDetector.findOverlap(candidate, allExistingSchedules)
        _uiState.update { it.copy(overlapWarning = overlap) }
    }

    fun save() {
        viewModelScope.launch {
            val current = _uiState.value
            val entity = ScheduleEntity(
                id = current.id,
                name = current.name.ifBlank { "Schedule" },
                startMinute = current.startMinute,
                endMinute = current.endMinute,
                daysMask = current.daysMask,
                policy = current.policy,
                enabled = current.enabled,
                createdAt = clock.now()
            )
            schedulesUiDao.saveScheduleWithApps(entity, current.extraAllowedApps)
            _uiState.update { it.copy(isSaved = true) }
        }
    }

    fun delete() {
        if (scheduleId <= 0L) return
        viewModelScope.launch {
            schedulesUiDao.deleteScheduleById(scheduleId)
            _uiState.update { it.copy(isDeleted = true) }
        }
    }
}
