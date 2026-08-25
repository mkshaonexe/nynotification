package com.quietinbox.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.quietinbox.ui.theme.QuietInboxTheme
import com.quietinbox.ui.theme.QuietTheme

data class HeatmapCell(
    val dayKey: Int, // yyyymmdd
    val dayOfWeek: Int, // 0 = Mon, 6 = Sun
    val weekIndex: Int,
    val count: Int,
    val dateLabel: String,
    val intensityLevel: Int // 0 to 4
)

@Composable
fun ActivityHeatmap(
    cells: List<HeatmapCell>,
    onCellClick: (dayKey: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val ramp = QuietTheme.colors.heatmapRamp

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(QuietTheme.shapes.card)
            .background(QuietTheme.colors.surface)
            .padding(QuietTheme.tokens.space16),
        verticalArrangement = Arrangement.spacedBy(QuietTheme.tokens.space8)
    ) {
        Text(
            text = "Activity",
            style = QuietTheme.typography.titleMedium,
            color = QuietTheme.colors.onSurface
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val maxWeeks = (cells.maxOfOrNull { it.weekIndex } ?: 0) + 1
            for (week in 0 until maxWeeks) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    for (dayOfWeek in 0..6) {
                        val cell = cells.find { it.weekIndex == week && it.dayOfWeek == dayOfWeek }
                        val level = cell?.intensityLevel?.coerceIn(0, 4) ?: 0
                        val color = ramp.getOrElse(level) { ramp.first() }
                        val description = cell?.let { "${it.dateLabel}, ${it.count} silenced" } ?: "No data"

                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(color)
                                .semantics { contentDescription = description }
                                .then(
                                    if (cell != null) {
                                        Modifier.clickable { onCellClick(cell.dayKey) }
                                    } else Modifier
                                )
                        )
                    }
                }
            }
        }
    }
}

@Preview(name = "ActivityHeatmap - Dark")
@Composable
private fun ActivityHeatmapDarkPreview() {
    val sampleCells = (0 until 182).map { i ->
        val week = i / 7
        val day = i % 7
        HeatmapCell(
            dayKey = 20260101 + i,
            dayOfWeek = day,
            weekIndex = week,
            count = (i * 3) % 50,
            dateLabel = "Day $i",
            intensityLevel = (i % 5)
        )
    }
    QuietInboxTheme(darkTheme = true) {
        ActivityHeatmap(cells = sampleCells, onCellClick = {})
    }
}

@Preview(name = "ActivityHeatmap - Light")
@Composable
private fun ActivityHeatmapLightPreview() {
    val sampleCells = (0 until 182).map { i ->
        val week = i / 7
        val day = i % 7
        HeatmapCell(
            dayKey = 20260101 + i,
            dayOfWeek = day,
            weekIndex = week,
            count = (i * 3) % 50,
            dateLabel = "Day $i",
            intensityLevel = (i % 5)
        )
    }
    QuietInboxTheme(darkTheme = false) {
        ActivityHeatmap(cells = sampleCells, onCellClick = {})
    }
}
