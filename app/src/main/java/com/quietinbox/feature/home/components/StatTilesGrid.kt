package com.quietinbox.feature.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFeature
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quietinbox.R
import java.text.NumberFormat
import java.util.Locale

/**
 * Data holder for the 9 home dashboard metrics.
 */
data class HomeMetrics(
    val silencedCount: Int,
    val capturedCount: Int,
    val letThroughCount: Int,
    val appsCount: Int,
    val quietDaysCount: Int,
    val currentStreakDays: Int,
    val longestStreakDays: Int,
    val busiestHourFormatted: String,
    val noisiestAppLabel: String
)

/**
 * 3x3 Grid displaying all 9 statistical tiles.
 * Tabular figures (tnum) used on all numeric values to prevent jitter.
 * Owned by Phase 5.
 */
@Composable
fun StatTilesGrid(
    metrics: HomeMetrics,
    modifier: Modifier = Modifier
) {
    val numberFormatter = NumberFormat.getNumberInstance(Locale.getDefault())

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Row 1: Silenced, Captured, Let through
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StatTile(
                title = stringResource(R.string.stat_silenced),
                value = numberFormatter.format(metrics.silencedCount),
                modifier = Modifier.weight(1f)
            )
            StatTile(
                title = stringResource(R.string.stat_captured),
                value = numberFormatter.format(metrics.capturedCount),
                modifier = Modifier.weight(1f)
            )
            StatTile(
                title = stringResource(R.string.stat_let_through),
                value = numberFormatter.format(metrics.letThroughCount),
                modifier = Modifier.weight(1f)
            )
        }

        // Row 2: Apps, Quiet days, Current streak
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StatTile(
                title = stringResource(R.string.stat_apps),
                value = numberFormatter.format(metrics.appsCount),
                modifier = Modifier.weight(1f)
            )
            StatTile(
                title = stringResource(R.string.stat_quiet_days),
                value = stringResource(R.string.stat_days_unit, metrics.quietDaysCount),
                modifier = Modifier.weight(1f)
            )
            StatTile(
                title = stringResource(R.string.stat_current_streak),
                value = stringResource(R.string.stat_days_unit, metrics.currentStreakDays),
                modifier = Modifier.weight(1f)
            )
        }

        // Row 3: Longest streak, Busiest hour, Noisiest app
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StatTile(
                title = stringResource(R.string.stat_longest_streak),
                value = stringResource(R.string.stat_days_unit, metrics.longestStreakDays),
                modifier = Modifier.weight(1f)
            )
            StatTile(
                title = stringResource(R.string.stat_busiest_hour),
                value = metrics.busiestHourFormatted.ifBlank { stringResource(R.string.stat_none) },
                modifier = Modifier.weight(1f)
            )
            StatTile(
                title = stringResource(R.string.stat_noisiest_app),
                value = metrics.noisiestAppLabel.ifBlank { stringResource(R.string.stat_none) },
                isTextValue = true,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * Individual metric tile with 12dp rounded corners and tnum typography.
 */
@Composable
fun StatTile(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    isTextValue: Boolean = false
) {
    val cd = "$title: $value"

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(12.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = cd
            },
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = value,
            style = if (isTextValue && value.length > 5) {
                MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold
                )
            } else {
                MaterialTheme.typography.titleLarge.copy(
                    fontSize = 24.sp,
                    lineHeight = 28.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFeatureSettings = "tnum"
                )
            },
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
