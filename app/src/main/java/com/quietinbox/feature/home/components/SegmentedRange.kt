package com.quietinbox.feature.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.quietinbox.R

/**
 * Statistical range filters available on the Home dashboard.
 */
enum class StatsRange(val days: Int?, val labelRes: Int) {
    ALL(null, R.string.range_all),
    DAYS_30(30, R.string.range_30d),
    DAYS_7(7, R.string.range_7d)
}

/**
 * Segmented range control for All · 30d · 7d selection.
 * Owned by Phase 5.
 */
@Composable
fun SegmentedRange(
    selectedRange: StatsRange,
    onRangeSelected: (StatsRange) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(4.dp)
    ) {
        StatsRange.entries.forEach { range ->
            val isSelected = range == selectedRange
            val label = stringResource(range.labelRes)

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (isSelected) {
                            MaterialTheme.colorScheme.surface
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        }
                    )
                    .clickable(role = Role.Tab) {
                        onRangeSelected(range)
                    }
                    .semantics {
                        selected = isSelected
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        }
    }
}
