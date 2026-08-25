package com.quietinbox.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.quietinbox.ui.theme.QuietInboxTheme
import com.quietinbox.ui.theme.QuietTheme

@Composable
fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Search notifications, apps, senders…"
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = QuietTheme.tokens.minTouchTarget)
            .clip(QuietTheme.shapes.chip)
            .background(QuietTheme.colors.surfaceVariant)
            .padding(horizontal = QuietTheme.tokens.space12),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = "Search",
            tint = QuietTheme.colors.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )

        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = QuietTheme.tokens.space8),
            textStyle = QuietTheme.typography.bodyLarge.copy(color = QuietTheme.colors.onSurface),
            cursorBrush = SolidColor(QuietTheme.colors.primary),
            singleLine = true,
            decorationBox = { innerTextField ->
                if (query.isEmpty()) {
                    Text(
                        text = placeholder,
                        style = QuietTheme.typography.bodyLarge,
                        color = QuietTheme.colors.onSurfaceVariant
                    )
                }
                innerTextField()
            }
        )

        if (query.isNotEmpty()) {
            IconButton(
                onClick = { onQueryChange("") },
                modifier = Modifier.size(QuietTheme.tokens.minTouchTarget)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Clear search",
                    tint = QuietTheme.colors.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Preview(name = "SearchField - Dark Empty")
@Composable
private fun SearchFieldDarkEmptyPreview() {
    QuietInboxTheme(darkTheme = true) {
        SearchField(
            query = "",
            onQueryChange = {}
        )
    }
}

@Preview(name = "SearchField - Light Filled")
@Composable
private fun SearchFieldLightFilledPreview() {
    QuietInboxTheme(darkTheme = false) {
        SearchField(
            query = "Telegram OTP",
            onQueryChange = {}
        )
    }
}
