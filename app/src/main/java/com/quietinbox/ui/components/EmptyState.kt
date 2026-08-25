package com.quietinbox.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.quietinbox.ui.theme.QuietInboxTheme
import com.quietinbox.ui.theme.QuietTheme

@Composable
fun EmptyState(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Default.Inbox,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(QuietTheme.tokens.space24),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(QuietTheme.tokens.space12)
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(QuietTheme.colors.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = QuietTheme.colors.onSurfaceVariant,
                modifier = Modifier.size(28.dp)
            )
        }

        Text(
            text = title,
            style = QuietTheme.typography.titleLarge,
            color = QuietTheme.colors.onSurface,
            textAlign = TextAlign.Center
        )

        Text(
            text = subtitle,
            style = QuietTheme.typography.bodyMedium,
            color = QuietTheme.colors.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        if (actionLabel != null && onActionClick != null) {
            Button(
                onClick = onActionClick,
                modifier = Modifier
                    .padding(top = QuietTheme.tokens.space8)
                    .defaultMinSize(minHeight = QuietTheme.tokens.minTouchTarget),
                shape = QuietTheme.shapes.chip,
                colors = ButtonDefaults.buttonColors(
                    containerColor = QuietTheme.colors.primary,
                    contentColor = QuietTheme.colors.background
                )
            ) {
                Text(
                    text = actionLabel,
                    style = QuietTheme.typography.label
                )
            }
        }
    }
}

@Preview(name = "EmptyState - Dark")
@Composable
private fun EmptyStateDarkPreview() {
    QuietInboxTheme(darkTheme = true) {
        EmptyState(
            title = "Nothing captured yet",
            subtitle = "Notifications will show up here once received.",
            actionLabel = "Grant Access",
            onActionClick = {}
        )
    }
}

@Preview(name = "EmptyState - Light")
@Composable
private fun EmptyStateLightPreview() {
    QuietInboxTheme(darkTheme = false) {
        EmptyState(
            title = "No results found",
            subtitle = "Try adjusting your search query or filters.",
            actionLabel = "Clear Filter",
            onActionClick = {}
        )
    }
}
