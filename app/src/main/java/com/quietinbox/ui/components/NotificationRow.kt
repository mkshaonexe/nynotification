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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Star
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
fun NotificationRow(
    appName: String,
    title: String?,
    body: String?,
    timeAgo: String,
    isUnread: Boolean,
    isSilenced: Boolean,
    isStarred: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val semanticsDesc = "$appName · ${title ?: ""} · $timeAgo ${if (isUnread) "· unread" else ""}"

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
        // App Icon Fallback Avatar
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(QuietTheme.tokens.radiusChip))
                .background(QuietTheme.colors.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = appName.take(1).uppercase(),
                style = QuietTheme.typography.titleMedium,
                color = QuietTheme.colors.primary
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(QuietTheme.tokens.space4)
                ) {
                    if (isSilenced) {
                        Icon(
                            imageVector = Icons.Default.NotificationsOff,
                            contentDescription = "Silenced",
                            tint = QuietTheme.colors.muted,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    if (isStarred) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Starred",
                            tint = QuietTheme.colors.warning,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Text(
                        text = timeAgo,
                        style = QuietTheme.typography.label,
                        color = QuietTheme.colors.onSurfaceVariant
                    )
                }
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

            if (!body.isNullOrBlank()) {
                Text(
                    text = body,
                    style = QuietTheme.typography.bodyLarge,
                    color = QuietTheme.colors.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        if (isUnread) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(QuietTheme.colors.primary)
            )
        }
    }
}

@Preview(name = "NotificationRow - Dark")
@Composable
private fun NotificationRowDarkPreview() {
    QuietInboxTheme(darkTheme = true) {
        NotificationRow(
            appName = "Telegram",
            title = "Alex Chen",
            body = "Hey, are we still meeting at 3 PM today?",
            timeAgo = "12m",
            isUnread = true,
            isSilenced = true,
            isStarred = false,
            onClick = {}
        )
    }
}

@Preview(name = "NotificationRow - Light")
@Composable
private fun NotificationRowLightPreview() {
    QuietInboxTheme(darkTheme = false) {
        NotificationRow(
            appName = "Slack",
            title = "#engineering",
            body = "Release build v1.0.0 is now live on staging.",
            timeAgo = "1h",
            isUnread = false,
            isSilenced = false,
            isStarred = true,
            onClick = {}
        )
    }
}
