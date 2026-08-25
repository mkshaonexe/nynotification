package com.quietinbox.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quietinbox.core.health.HealthState
import com.quietinbox.core.health.ListenerHealth
import com.quietinbox.core.time.Clock
import com.quietinbox.data.db.entity.DailyStatEntity
import com.quietinbox.data.db.entity.MutedAppEntity
import com.quietinbox.data.prefs.SettingsDataStore
import com.quietinbox.feature.home.components.HomeMetrics
import com.quietinbox.feature.home.components.StatsRange
import com.quietinbox.feature.home.data.AppNotificationCount
import com.quietinbox.feature.home.data.HomeStatsDao
import com.quietinbox.feature.home.domain.HeatmapBucketer
import com.quietinbox.feature.home.domain.HeatmapData
import com.quietinbox.feature.home.domain.StreakCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

/**
 * ViewModel for the Home & Statistics feature.
 * Coordinates metrics computation, live health binding, Quiet Mode toggle, pause state, and app muting.
 * Owned by Phase 5.
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val homeStatsDao: HomeStatsDao,
    private val settingsDataStore: SettingsDataStore,
    private val listenerHealth: ListenerHealth,
    private val clock: Clock
) : ViewModel() {

    private val selectedRangeFlow = MutableStateFlow(StatsRange.DAYS_30)
    private val secondsPerInterruptionFlow = MutableStateFlow(StatsTuning.SECONDS_PER_INTERRUPTION)
    private val isFocusReclaimedHiddenFlow = MutableStateFlow(false)

    private val timeFormatter = DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault())

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<HomeUiState> = combine(
        settingsDataStore.settingsFlow,
        listenerHealth.snapshot,
        selectedRangeFlow,
        secondsPerInterruptionFlow,
        isFocusReclaimedHiddenFlow
    ) { settings, healthSnapshot, range, secondsPerInterruption, isFocusHidden ->
        val now = clock.now()
        val isPaused = settings.pausedUntilEpochMs > now
        val isQuietActive = settings.quietModeEnabled && !isPaused

        val subtitle = when {
            isPaused -> {
                val remainingMinutes = ((settings.pausedUntilEpochMs - now) / 60000L).coerceAtLeast(1)
                "Paused · ${remainingMinutes}m remaining"
            }
            isQuietActive -> {
                // If there's an enabled timestamp in settings, format it; otherwise "On"
                "On"
            }
            else -> "Off"
        }

        BaseSettingsState(
            quietModeEnabled = settings.quietModeEnabled,
            pausedUntilEpochMs = settings.pausedUntilEpochMs,
            isPaused = isPaused,
            statusSubtitle = subtitle,
            healthState = healthSnapshot.state,
            selectedRange = range,
            secondsPerInterruption = secondsPerInterruption,
            isFocusReclaimedHidden = isFocusHidden
        )
    }.flatMapLatest { base ->
        val fromEpochMs = base.selectedRange.days?.let { days ->
            clock.now() - (days * 24 * 60 * 60 * 1000L)
        }
        val fromDayInt = base.selectedRange.days?.let { days ->
            val fromDate = LocalDate.now().minusDays(days.toLong())
            StreakCalculator.toDayInt(fromDate)
        }

        combine(
            listOf(
                homeStatsDao.getCapturedCount(fromEpochMs),
                homeStatsDao.getSilencedCount(fromEpochMs),
                homeStatsDao.getAllowedCount(fromEpochMs),
                homeStatsDao.getDistinctAppsCount(fromEpochMs),
                homeStatsDao.getQuietDaysCount(fromDayInt),
                homeStatsDao.getAllDailyStats(),
                homeStatsDao.getBusiestHour(fromEpochMs),
                homeStatsDao.getTopAppsByCount(fromEpochMs, 5),
                homeStatsDao.getMutedPackages(),
                homeStatsDao.getDailySilencedFromFirewall(fromEpochMs)
            )
        ) { array ->
            @Suppress("UNCHECKED_CAST")
            val captured = array[0] as Int
            @Suppress("UNCHECKED_CAST")
            val silenced = array[1] as Int
            @Suppress("UNCHECKED_CAST")
            val allowed = array[2] as Int
            @Suppress("UNCHECKED_CAST")
            val apps = array[3] as Int
            @Suppress("UNCHECKED_CAST")
            val quietDays = array[4] as Int
            @Suppress("UNCHECKED_CAST")
            val allStats = array[5] as List<DailyStatEntity>
            @Suppress("UNCHECKED_CAST")
            val busiestHour = array[6] as HourCount?
            @Suppress("UNCHECKED_CAST")
            val topApps = array[7] as List<AppNotificationCount>
            @Suppress("UNCHECKED_CAST")
            val mutedList = array[8] as List<String>
            @Suppress("UNCHECKED_CAST")
            val dailySilenced = array[9] as List<DaySilencedCount>

            val today = LocalDate.now()
            val streakResult = StreakCalculator.calculate(allStats, today)

            val busiestHourFormatted = busiestHour?.let { formatHour(it.hour) } ?: "—"
            val noisiestAppLabel = topApps.firstOrNull()?.appLabel ?: "—"

            val metrics = HomeMetrics(
                silencedCount = silenced,
                capturedCount = captured,
                letThroughCount = allowed,
                appsCount = apps,
                quietDaysCount = quietDays,
                currentStreakDays = streakResult.currentStreak,
                longestStreakDays = streakResult.longestStreak,
                busiestHourFormatted = busiestHourFormatted,
                noisiestAppLabel = noisiestAppLabel
            )

            // Convert daily silenced list into map for heatmap bucketer
            val silencedMap = dailySilenced.associate { it.day to it.count }
            val heatmapData = HeatmapBucketer.buildHeatmap(
                dayCounts = silencedMap,
                today = today,
                weeks = StatsTuning.HEATMAP_WEEKS
            )

            HomeUiState(
                quietModeEnabled = base.quietModeEnabled,
                pausedUntilEpochMs = base.pausedUntilEpochMs,
                isPaused = base.isPaused,
                statusSubtitle = base.statusSubtitle,
                healthState = base.healthState,
                selectedRange = base.selectedRange,
                metrics = metrics,
                heatmapData = heatmapData,
                topApps = topApps,
                mutedPackages = mutedList.toSet(),
                secondsPerInterruption = base.secondsPerInterruption,
                isFocusReclaimedHidden = base.isFocusReclaimedHidden,
                isLoading = false
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState(isLoading = true)
    )

    /**
     * Toggles the master Quiet Mode switch and updates DataStore.
     */
    fun toggleQuietMode(enabled: Boolean) {
        viewModelScope.launch {
            settingsDataStore.setQuietModeEnabled(enabled)
            if (enabled) {
                // Clear any lingering pause when explicitly turning on
                settingsDataStore.setPausedUntil(0L)
            }
        }
    }

    /**
     * Sets a pause duration from now in milliseconds.
     */
    fun pauseFor(durationMs: Long) {
        viewModelScope.launch {
            val pausedUntil = clock.now() + durationMs
            settingsDataStore.setPausedUntil(pausedUntil)
        }
    }

    /**
     * Resumes Quiet Mode immediately by clearing pause state.
     */
    fun resumeNow() {
        viewModelScope.launch {
            settingsDataStore.setPausedUntil(0L)
            settingsDataStore.setQuietModeEnabled(true)
        }
    }

    /**
     * Turns Quiet Mode completely off.
     */
    fun turnOff() {
        viewModelScope.launch {
            settingsDataStore.setPausedUntil(0L)
            settingsDataStore.setQuietModeEnabled(false)
        }
    }

    /**
     * Selects the active statistics range (All, 30d, 7d).
     */
    fun setRange(range: StatsRange) {
        selectedRangeFlow.value = range
    }

    /**
     * Updates the seconds per interruption for focus reclaimed estimation.
     */
    fun setSecondsPerInterruption(seconds: Int) {
        secondsPerInterruptionFlow.value = seconds
    }

    /**
     * Hides the focus reclaimed card.
     */
    fun hideFocusReclaimed() {
        isFocusReclaimedHiddenFlow.value = true
    }

    /**
     * Toggles muting for a specific package directly from the Home screen.
     */
    fun toggleMuteApp(packageName: String, isCurrentlyMuted: Boolean) {
        viewModelScope.launch {
            if (isCurrentlyMuted) {
                homeStatsDao.unmuteApp(packageName)
            } else {
                homeStatsDao.muteApp(
                    MutedAppEntity(
                        packageName = packageName,
                        mutedAt = clock.now()
                    )
                )
            }
        }
    }

    private fun formatHour(hour: Int): String {
        return when {
            hour == 0 -> "12 AM"
            hour < 12 -> "$hour AM"
            hour == 12 -> "12 PM"
            else -> "${hour - 12} PM"
        }
    }

    private data class BaseSettingsState(
        val quietModeEnabled: Boolean,
        val pausedUntilEpochMs: Long,
        val isPaused: Boolean,
        val statusSubtitle: String,
        val healthState: HealthState,
        val selectedRange: StatsRange,
        val secondsPerInterruption: Int,
        val isFocusReclaimedHidden: Boolean
    )
}
