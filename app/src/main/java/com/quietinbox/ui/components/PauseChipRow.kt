package com.quietinbox.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import com.quietinbox.ui.theme.QuietInboxTheme
import com.quietinbox.ui.theme.QuietTheme

enum class PauseDuration(val label: String, val minutes: Int) {
    MIN_5("5 min", 5),
    MIN_15("15 min", 15),
    HOUR_1("1 hour", 60),
    UNTIL_OFF("Until off", -1)
}

@Composable
fun PauseChipRow(
    onSelectDuration: (PauseDuration) -> Unit,
    modifier: Modifier = Modifier,
    selectedDuration: PauseDuration? = null
) {
    val scrollState = rememberScrollState()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(horizontal = QuietTheme.tokens.screenHorizontalPadding),
        horizontalArrangement = Arrangement.spacedBy(QuietTheme.tokens.space8)
    ) {
        PauseDuration.entries.forEach { duration ->
            val isSelected = duration == selectedDuration
            Box(
                modifier = Modifier
                    .defaultMinSize(minHeight = QuietTheme.tokens.minTouchTarget)
                    .clip(QuietTheme.shapes.chip)
                    .background(if (isSelected) QuietTheme.colors.primary else QuietTheme.colors.surfaceVariant)
                    .clickable(role = Role.Button) { onSelectDuration(duration) }
                    .padding(
                        horizontal = QuietTheme.tokens.space16,
                        vertical = QuietTheme.tokens.space8
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = duration.label,
                    style = QuietTheme.typography.label,
                    color = if (isSelected) QuietTheme.colors.background else QuietTheme.colors.onSurface
                )
            }
        }
    }
}

@Preview(name = "PauseChipRow - Dark")
@Composable
private fun PauseChipRowDarkPreview() {
    QuietInboxTheme(darkTheme = true) {
        PauseChipRow(
            selectedDuration = PauseDuration.MIN_15,
            onSelectDuration = {}
        )
    }
}

@Preview(name = "PauseChipRow - Light")
@Composable
private fun PauseChipRowLightPreview() {
    QuietInboxTheme(darkTheme = false) {
        PauseChipRow(
            selectedDuration = null,
            onSelectDuration = {}
        )
    }
}
