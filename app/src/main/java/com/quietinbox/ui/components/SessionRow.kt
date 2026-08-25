package com.quietinbox.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.quietinbox.ui.theme.QuietInboxTheme
import com.quietinbox.ui.theme.QuietTheme

@Composable
fun SessionRow(
    appName: String,
    title: String?,
    sessionSummary: String,
    updateCount: Int,
    timeRangeText: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val semanticsDesc = "$appName · ${title ?: ""} · $timeRangeText · $updateCount updates"

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(QuietTheme.shapes.card)
            .background(QuietTheme.colors.surface)
            .clickable(onClick = onClick)
            .padding(
                horizontal = QuietTheme.tokens.screenHorizontalPadding,
                vertical = QuietTheme.tokens.rowVerticalPadding
            )
            .semantics(mergeDescendants = true) {
                contentDescription = semanticsDesc
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(QuietTheme.tokens.space12)
    ) {
        // Session Icon
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(QuietTheme.tokens.radiusChip))
                .background(QuietTheme.colors.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Sync,
                contentDescription = "Session",
                tint = QuietTheme.colors.primary,
                modifier = Modifier.size(20.dp)
            )
        }

        // Content
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(QuietTheme.tokens.space4)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = appName,
                    style = QuietTheme.typography.label,
                    color = QuietTheme.colors.onSurfaceVariant
                )
                Text(
                    text = "$updateCount updates",
                    style = QuietTheme.typography.mono,
                    color = QuietTheme.colors.primary
                )
            }

            if (!title.isNullOrBlank()) {
                Text(
                    text = title,
                    style = QuietTheme.typography.titleMedium,
                    color = QuietTheme.colors.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Text(
                text = "$timeRangeText · $sessionSummary",
                style = QuietTheme.typography.bodyMedium,
                color = QuietTheme.colors.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Preview(name = "SessionRow - Dark")
@Composable
private fun SessionRowDarkPreview() {
    QuietInboxTheme(darkTheme = true) {
        SessionRow(
            appName = "Android System",
            title = "Mobile Hotspot active",
            sessionSummary = "1 device · 3.1 MB",
            updateCount = 612,
            timeRangeText = "10:17 PM – 10:27 PM (10 min)",
            onClick = {}
        )
    }
}

@Preview(name = "SessionRow - Light")
@Composable
private fun SessionRowLightPreview() {
    QuietInboxTheme(darkTheme = false) {
        SessionRow(
            appName = "Google Play Store",
            title = "Downloading updates…",
            sessionSummary = "45 MB of 120 MB (100%)",
            updateCount = 420,
            timeRangeText = "3:00 PM – 3:04 PM",
            onClick = {}
        )
    }
}
