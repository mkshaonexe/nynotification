package com.quietinbox.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.quietinbox.core.health.HealthState
import com.quietinbox.ui.theme.QuietInboxTheme
import com.quietinbox.ui.theme.QuietTheme

@Composable
fun HealthBanner(
    healthState: HealthState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (statusText, dotColor) = when (healthState) {
        HealthState.CONNECTED -> "Connected · capturing" to QuietTheme.colors.positive
        HealthState.STALE -> "Connected, but nothing captured in 24h" to QuietTheme.colors.warning
        HealthState.NO_ACCESS -> "Notification access is off · Tap to fix" to QuietTheme.colors.danger
        HealthState.UNKNOWN -> "Checking listener status…" to QuietTheme.colors.muted
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(QuietTheme.shapes.card)
            .background(QuietTheme.colors.surface)
            .clickable(onClick = onClick)
            .padding(
                horizontal = QuietTheme.tokens.screenHorizontalPadding,
                vertical = QuietTheme.tokens.space12
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(QuietTheme.tokens.space8)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(dotColor)
        )
        Text(
            text = statusText,
            style = QuietTheme.typography.bodyMedium,
            color = QuietTheme.colors.onSurface
        )
    }
}

@Preview(name = "HealthBanner - Dark")
@Composable
private fun HealthBannerDarkPreview() {
    QuietInboxTheme(darkTheme = true) {
        HealthBanner(
            healthState = HealthState.CONNECTED,
            onClick = {}
        )
    }
}

@Preview(name = "HealthBanner - Warning Light")
@Composable
private fun HealthBannerLightPreview() {
    QuietInboxTheme(darkTheme = false) {
        HealthBanner(
            healthState = HealthState.STALE,
            onClick = {}
        )
    }
}
