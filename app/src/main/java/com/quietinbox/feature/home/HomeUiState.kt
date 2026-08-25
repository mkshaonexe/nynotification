package com.quietinbox.feature.home

import com.quietinbox.core.health.HealthState
import com.quietinbox.feature.home.components.HomeMetrics
import com.quietinbox.feature.home.components.StatsRange
import com.quietinbox.feature.home.data.AppNotificationCount
import com.quietinbox.feature.home.domain.HeatmapData

/**
 * UI State for the Home & Statistics dashboard.
 * Owned by Phase 5.
 */
data class HomeUiState(
    val quietModeEnabled: Boolean = false,
    val pausedUntilEpochMs: Long = 0L,
    val isPaused: Boolean = false,
    val statusSubtitle: String = "Off",
    val healthState: HealthState = HealthState.CONNECTED,
    val selectedRange: StatsRange = StatsRange.DAYS_30,
    val metrics: HomeMetrics = HomeMetrics(
        silencedCount = 0,
        capturedCount = 0,
        letThroughCount = 0,
        appsCount = 0,
        quietDaysCount = 0,
        currentStreakDays = 0,
        longestStreakDays = 0,
        busiestHourFormatted = "—",
        noisiestAppLabel = "—"
    ),
    val heatmapData: HeatmapData = HeatmapData(weeks = emptyList(), p90 = 0.0),
    val topApps: List<AppNotificationCount> = emptyList(),
    val mutedPackages: Set<String> = emptySet(),
    val secondsPerInterruption: Int = StatsTuning.SECONDS_PER_INTERRUPTION,
    val isFocusReclaimedHidden: Boolean = false,
    val isLoading: Boolean = false
)
