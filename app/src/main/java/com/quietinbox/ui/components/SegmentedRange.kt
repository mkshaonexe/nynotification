package com.quietinbox.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.quietinbox.ui.theme.QuietInboxTheme
import com.quietinbox.ui.theme.QuietTheme

enum class TimeRange(val label: String, val days: Int) {
    ALL("All", 0),
    THIRTY_DAYS("30d", 30),
    SEVEN_DAYS("7d", 7)
}

@Composable
fun SegmentedRange(
    selectedRange: TimeRange,
    onRangeSelected: (TimeRange) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(QuietTheme.tokens.radiusChip))
            .background(QuietTheme.colors.surfaceVariant)
            .padding(QuietTheme.tokens.space4),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        TimeRange.entries.forEach { range ->
            val isSelected = range == selectedRange
            Box(
                modifier = Modifier
                    .weight(1f)
                    .defaultMinSize(minHeight = QuietTheme.tokens.minTouchTarget)
                    .clip(RoundedCornerShape(QuietTheme.tokens.radiusChip))
                    .background(if (isSelected) QuietTheme.colors.surface else QuietTheme.colors.surfaceVariant)
                    .clickable(role = Role.RadioButton) { onRangeSelected(range) }
                    .padding(vertical = QuietTheme.tokens.space8),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = range.label,
                    style = QuietTheme.typography.label,
                    color = if (isSelected) QuietTheme.colors.onSurface else QuietTheme.colors.onSurfaceVariant
                )
            }
        }
    }
}

@Preview(name = "SegmentedRange - Dark")
@Composable
private fun SegmentedRangeDarkPreview() {
    QuietInboxTheme(darkTheme = true) {
        SegmentedRange(
            selectedRange = TimeRange.THIRTY_DAYS,
            onRangeSelected = {}
        )
    }
}

@Preview(name = "SegmentedRange - Light")
@Composable
private fun SegmentedRangeLightPreview() {
    QuietInboxTheme(darkTheme = false) {
        SegmentedRange(
            selectedRange = TimeRange.ALL,
            onRangeSelected = {}
        )
    }
}
