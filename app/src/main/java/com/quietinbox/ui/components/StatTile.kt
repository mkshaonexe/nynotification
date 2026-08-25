package com.quietinbox.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import com.quietinbox.ui.theme.QuietInboxTheme
import com.quietinbox.ui.theme.QuietTheme

@Composable
fun StatTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    subCaption: String? = null
) {
    Column(
        modifier = modifier
            .clip(QuietTheme.shapes.card)
            .background(QuietTheme.colors.surface)
            .padding(QuietTheme.tokens.space16),
        verticalArrangement = Arrangement.spacedBy(QuietTheme.tokens.space4)
    ) {
        Text(
            text = label,
            style = QuietTheme.typography.label,
            color = QuietTheme.colors.onSurfaceVariant
        )
        Text(
            text = value,
            style = QuietTheme.typography.displayNumber,
            color = QuietTheme.colors.onSurface
        )
        if (subCaption != null) {
            Text(
                text = subCaption,
                style = QuietTheme.typography.label,
                color = QuietTheme.colors.onSurfaceVariant
            )
        }
    }
}

@Preview(name = "StatTile - Dark")
@Composable
private fun StatTileDarkPreview() {
    QuietInboxTheme(darkTheme = true) {
        StatTile(
            label = "Silenced",
            value = "1,284",
            subCaption = "~3h attention saved"
        )
    }
}

@Preview(name = "StatTile - Light")
@Composable
private fun StatTileLightPreview() {
    QuietInboxTheme(darkTheme = false) {
        StatTile(
            label = "Silenced",
            value = "1,284",
            subCaption = "~3h attention saved"
        )
    }
}
