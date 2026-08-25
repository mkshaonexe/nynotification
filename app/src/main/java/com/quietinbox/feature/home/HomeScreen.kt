package com.quietinbox.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.quietinbox.feature.home.components.ActivityHeatmap
import com.quietinbox.feature.home.components.FocusReclaimedCard
import com.quietinbox.feature.home.components.HealthBanner
import com.quietinbox.feature.home.components.NoisiestAppsSection
import com.quietinbox.feature.home.components.PauseChipRow
import com.quietinbox.feature.home.components.QuietSwitch
import com.quietinbox.feature.home.components.SegmentedRange
import com.quietinbox.feature.home.components.StatTilesGrid

/**
 * Stateful entry point for the Home screen.
 * Owned by Phase 5.
 */
@Composable
fun HomeScreen(
    onNavigateToPermissionsHealth: () -> Unit,
    onNavigateToInboxDay: (Int) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    HomeScreenContent(
        uiState = uiState,
        onToggleQuietMode = viewModel::toggleQuietMode,
        onPauseDurationSelected = viewModel::pauseFor,
        onTurnOff = viewModel::turnOff,
        onResumeNow = viewModel::resumeNow,
        onRangeSelected = viewModel::setRange,
        onSecondsPerInterruptionChanged = viewModel::setSecondsPerInterruption,
        onHideFocusEstimate = viewModel::hideFocusReclaimed,
        onToggleMuteApp = viewModel::toggleMuteApp,
        onNavigateToPermissionsHealth = onNavigateToPermissionsHealth,
        onNavigateToInboxDay = onNavigateToInboxDay,
        modifier = modifier
    )
}

/**
 * Stateless Home screen content composable.
 */
@Composable
fun HomeScreenContent(
    uiState: HomeUiState,
    onToggleQuietMode: (Boolean) -> Unit,
    onPauseDurationSelected: (Long) -> Unit,
    onTurnOff: () -> Unit,
    onResumeNow: () -> Unit,
    onRangeSelected: (com.quietinbox.feature.home.components.StatsRange) -> Unit,
    onSecondsPerInterruptionChanged: (Int) -> Unit,
    onHideFocusEstimate: () -> Unit,
    onToggleMuteApp: (packageName: String, isMuted: Boolean) -> Unit,
    onNavigateToPermissionsHealth: () -> Unit,
    onNavigateToInboxDay: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Quiet Mode Switch Hero Card
            item {
                QuietSwitch(
                    isEnabled = uiState.quietModeEnabled,
                    isPaused = uiState.isPaused,
                    statusSubtitle = uiState.statusSubtitle,
                    onToggle = onToggleQuietMode,
                    onResumeNow = onResumeNow
                )
            }

            // 2. Pause Chip Presets
            item {
                PauseChipRow(
                    onPauseDurationSelected = onPauseDurationSelected,
                    onTurnOff = onTurnOff
                )
            }

            // 3. Listener Health Status Banner
            item {
                HealthBanner(
                    state = uiState.healthState,
                    onNavigateToPermissionsHealth = onNavigateToPermissionsHealth
                )
            }

            // 4. Statistics Range Selector (All · 30d · 7d)
            item {
                SegmentedRange(
                    selectedRange = uiState.selectedRange,
                    onRangeSelected = onRangeSelected
                )
            }

            // 5. 9-Metric Stat Tiles Grid
            item {
                StatTilesGrid(
                    metrics = uiState.metrics
                )
            }

            // 6. 7x26 Activity Heatmap
            item {
                ActivityHeatmap(
                    heatmapData = uiState.heatmapData,
                    onDayClicked = onNavigateToInboxDay
                )
            }

            // 7. Focus Reclaimed Card (shown when silenced >= 50)
            if (!uiState.isFocusReclaimedHidden && uiState.metrics.silencedCount >= StatsTuning.FOCUS_RECLAIMED_MIN_SILENCED) {
                item {
                    FocusReclaimedCard(
                        silencedCount = uiState.metrics.silencedCount,
                        secondsPerInterruption = uiState.secondsPerInterruption,
                        onSecondsPerInterruptionChanged = onSecondsPerInterruptionChanged,
                        onHideEstimate = onHideFocusEstimate
                    )
                }
            }

            // 8. Noisiest Apps Section (Top 5)
            if (uiState.topApps.isNotEmpty()) {
                item {
                    NoisiestAppsSection(
                        topApps = uiState.topApps,
                        mutedPackageNames = uiState.mutedPackages,
                        onToggleMute = onToggleMuteApp
                    )
                }
            }

            // Bottom spacer for scroll comfort
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
