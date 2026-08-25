package com.quietinbox.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import com.quietinbox.ui.theme.QuietInboxTheme
import com.quietinbox.ui.theme.QuietTheme

enum class PermissionStatus {
    GRANTED,
    REQUIRED_MISSING,
    RECOMMENDED_MISSING
}

@Composable
fun PermissionCard(
    title: String,
    description: String,
    status: PermissionStatus,
    actionLabel: String,
    onActionClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(QuietTheme.shapes.card)
            .background(QuietTheme.colors.surface)
            .padding(QuietTheme.tokens.space16),
        verticalArrangement = Arrangement.spacedBy(QuietTheme.tokens.space8)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = QuietTheme.typography.titleMedium,
                color = QuietTheme.colors.onSurface
            )

            val (badgeText, badgeColor) = when (status) {
                PermissionStatus.GRANTED -> "Granted" to QuietTheme.colors.positive
                PermissionStatus.REQUIRED_MISSING -> "Required" to QuietTheme.colors.danger
                PermissionStatus.RECOMMENDED_MISSING -> "Recommended" to QuietTheme.colors.warning
            }

            Text(
                text = badgeText,
                style = QuietTheme.typography.label,
                color = badgeColor
            )
        }

        Text(
            text = description,
            style = QuietTheme.typography.bodyMedium,
            color = QuietTheme.colors.onSurfaceVariant
        )

        if (status != PermissionStatus.GRANTED) {
            Button(
                onClick = onActionClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = QuietTheme.tokens.minTouchTarget),
                shape = QuietTheme.shapes.chip,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (status == PermissionStatus.REQUIRED_MISSING) QuietTheme.colors.danger else QuietTheme.colors.primary,
                    contentColor = QuietTheme.colors.surface
                )
            ) {
                Text(text = actionLabel, style = QuietTheme.typography.label)
            }
        }
    }
}

@Preview(name = "PermissionCard - Dark")
@Composable
private fun PermissionCardDarkPreview() {
    QuietInboxTheme(darkTheme = true) {
        PermissionCard(
            title = "Notification Access",
            description = "Allows Quiet Inbox to capture and silence notifications on device.",
            status = PermissionStatus.REQUIRED_MISSING,
            actionLabel = "Grant Permission",
            onActionClick = {}
        )
    }
}

@Preview(name = "PermissionCard - Light")
@Composable
private fun PermissionCardLightPreview() {
    QuietInboxTheme(darkTheme = false) {
        PermissionCard(
            title = "Battery Optimization",
            description = "Ensures background capture service stays reliable across app sleep cycles.",
            status = PermissionStatus.GRANTED,
            actionLabel = "Ignore",
            onActionClick = {}
        )
    }
}
