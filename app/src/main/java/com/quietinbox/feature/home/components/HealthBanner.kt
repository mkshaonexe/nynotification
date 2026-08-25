package com.quietinbox.feature.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.quietinbox.R
import com.quietinbox.core.health.HealthState

/**
 * Health indicator banner showing live status of the notification listener.
 * Green (CONNECTED), Amber (STALE), Red (NO_ACCESS/UNKNOWN).
 * Owned by Phase 5.
 */
@Composable
fun HealthBanner(
    state: HealthState,
    onNavigateToPermissionsHealth: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (statusText, dotColor, containerColor, outlineColor, isActionable) = when (state) {
        HealthState.CONNECTED -> {
            BannerConfig(
                text = stringResource(R.string.health_connected_capturing),
                dotColor = Color(0xFF4ADE80), // Positive Green
                containerColor = Color(0xFF132D1F).copy(alpha = 0.4f),
                outlineColor = Color(0xFF1B4D2E),
                isActionable = false
            )
        }
        HealthState.STALE -> {
            BannerConfig(
                text = stringResource(R.string.health_stale_warning),
                dotColor = Color(0xFFFBBF24), // Warning Amber
                containerColor = Color(0xFF3B2D11).copy(alpha = 0.4f),
                outlineColor = Color(0xFF6B4E1B),
                isActionable = true
            )
        }
        HealthState.NO_ACCESS, HealthState.UNKNOWN -> {
            BannerConfig(
                text = stringResource(R.string.health_no_access_error),
                dotColor = Color(0xFFF87171), // Danger Red
                containerColor = Color(0xFF381515).copy(alpha = 0.4f),
                outlineColor = Color(0xFF661E1E),
                isActionable = true
            )
        }
    }

    val cdStatus = stringResource(R.string.cd_health_status, statusText)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(containerColor)
            .border(1.dp, outlineColor, RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = cdStatus
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = statusText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium
            )
        }

        if (isActionable) {
            Spacer(modifier = Modifier.width(8.dp))
            TextButton(
                onClick = onNavigateToPermissionsHealth,
                modifier = Modifier.heightIn(min = 48.dp),
                colors = ButtonDefaults.textButtonColors(contentColor = dotColor)
            ) {
                Text(
                    text = stringResource(R.string.health_fix_action),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

private data class BannerConfig(
    val text: String,
    val dotColor: Color,
    val containerColor: Color,
    val outlineColor: Color,
    val isActionable: Boolean
)
