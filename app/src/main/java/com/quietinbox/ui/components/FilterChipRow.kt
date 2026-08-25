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

enum class InboxFilter(val label: String) {
    ALL("All"),
    UNSEEN("Unseen"),
    SEEN("Seen"),
    STARRED("Starred")
}

@Composable
fun FilterChipRow(
    selectedFilter: InboxFilter,
    onFilterSelected: (InboxFilter) -> Unit,
    modifier: Modifier = Modifier,
    trailingItem: (@Composable () -> Unit)? = null
) {
    val scrollState = rememberScrollState()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(horizontal = QuietTheme.tokens.screenHorizontalPadding),
        horizontalArrangement = Arrangement.spacedBy(QuietTheme.tokens.space8),
        verticalAlignment = Alignment.CenterVertically
    ) {
        InboxFilter.entries.forEach { filter ->
            val isSelected = filter == selectedFilter
            Box(
                modifier = Modifier
                    .defaultMinSize(minHeight = QuietTheme.tokens.minTouchTarget)
                    .clip(QuietTheme.shapes.chip)
                    .background(if (isSelected) QuietTheme.colors.primary else QuietTheme.colors.surfaceVariant)
                    .clickable(role = Role.RadioButton) { onFilterSelected(filter) }
                    .padding(
                        horizontal = QuietTheme.tokens.space16,
                        vertical = QuietTheme.tokens.space8
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = filter.label,
                    style = QuietTheme.typography.label,
                    color = if (isSelected) QuietTheme.colors.background else QuietTheme.colors.onSurface
                )
            }
        }
        trailingItem?.invoke()
    }
}

@Preview(name = "FilterChipRow - Dark")
@Composable
private fun FilterChipRowDarkPreview() {
    QuietInboxTheme(darkTheme = true) {
        FilterChipRow(
            selectedFilter = InboxFilter.ALL,
            onFilterSelected = {}
        )
    }
}

@Preview(name = "FilterChipRow - Light")
@Composable
private fun FilterChipRowLightPreview() {
    QuietInboxTheme(darkTheme = false) {
        FilterChipRow(
            selectedFilter = InboxFilter.UNSEEN,
            onFilterSelected = {}
        )
    }
}
