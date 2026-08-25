package com.quietinbox.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.quietinbox.ui.theme.QuietInboxTheme
import com.quietinbox.ui.theme.QuietTheme

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    trailing: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = QuietTheme.tokens.screenHorizontalPadding,
                vertical = QuietTheme.tokens.space8
            ),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = QuietTheme.typography.titleMedium,
            color = QuietTheme.colors.onSurface
        )
        trailing?.invoke()
    }
}

@Preview(name = "SectionHeader - Dark")
@Composable
private fun SectionHeaderDarkPreview() {
    QuietInboxTheme(darkTheme = true) {
        SectionHeader(
            title = "Always Allowed Apps",
            trailing = {
                Text(
                    text = "3 apps",
                    style = QuietTheme.typography.label,
                    color = QuietTheme.colors.primary
                )
            }
        )
    }
}
