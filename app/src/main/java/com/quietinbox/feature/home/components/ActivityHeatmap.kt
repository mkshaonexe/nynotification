package com.quietinbox.feature.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quietinbox.R
import com.quietinbox.feature.home.domain.HeatmapCell
import com.quietinbox.feature.home.domain.HeatmapData

/**
 * Activity Heatmap displaying 7 rows (Mon-Sun) by up to 26 week columns.
 * Tapping a cell navigates to filtered inbox for that day.
 * Owned by Phase 5.
 */
@Composable
fun ActivityHeatmap(
    heatmapData: HeatmapData,
    onDayClicked: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val isDark = isSystemInDarkTheme()

    // Auto-scroll to the end (most recent weeks) on initial load
    LaunchedEffect(heatmapData.weeks.size) {
        if (heatmapData.weeks.isNotEmpty()) {
            scrollState.scrollTo(scrollState.maxValue)
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(14.dp)
    ) {
        Text(
            text = stringResource(R.string.activity_heatmap_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Day of week labels on the left: Mon, Wed, Fri, Sun
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(end = 6.dp)
            ) {
                DayLabel(stringResource(R.string.heatmap_day_mon))
                DayLabel("")
                DayLabel(stringResource(R.string.heatmap_day_wed))
                DayLabel("")
                DayLabel(stringResource(R.string.heatmap_day_fri))
                DayLabel("")
                DayLabel(stringResource(R.string.heatmap_day_sun))
            }

            // Scrollable grid of week columns
            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(scrollState),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                heatmapData.weeks.forEach { week ->
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        week.days.forEach { cell ->
                            HeatmapCellView(
                                cell = cell,
                                isDark = isDark,
                                onDayClicked = onDayClicked
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayLabel(text: String) {
    Box(
        modifier = Modifier.size(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 9.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun HeatmapCellView(
    cell: HeatmapCell?,
    isDark: Boolean,
    onDayClicked: (Int) -> Unit
) {
    if (cell == null) {
        // Future date or filler cell
        Box(
            modifier = Modifier
                .size(16.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(Color.Transparent)
        )
    } else {
        val cellColor = getIntensityColor(cell.level, isDark)
        val cd = cell.contentDescription

        Box(
            modifier = Modifier
                .size(16.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(cellColor)
                .clickable(
                    role = Role.Button,
                    onClickLabel = cd
                ) {
                    onDayClicked(cell.dayInt)
                }
                .semantics {
                    contentDescription = cd
                }
        )
    }
}

/**
 * 5-step intensity color ramp derived from accent color tokens (design.md §8).
 */
private fun getIntensityColor(level: Int, isDark: Boolean): Color {
    return if (isDark) {
        when (level) {
            1 -> Color(0xFF2A3565)
            2 -> Color(0xFF3F51A8)
            3 -> Color(0xFF5A72DB)
            4 -> Color(0xFF7B93FF)
            else -> Color(0xFF1F2125) // Level 0: empty / surfaceVariant
        }
    } else {
        when (level) {
            1 -> Color(0xFFD4DEFF)
            2 -> Color(0xFF9EB3FF)
            3 -> Color(0xFF5E80F7)
            4 -> Color(0xFF3B5BDB)
            else -> Color(0xFFF1F2F4) // Level 0: empty / surfaceVariant
        }
    }
}
