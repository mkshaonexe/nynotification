package com.quietinbox.feature.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quietinbox.R
import com.quietinbox.feature.home.data.AppNotificationCount
import java.text.NumberFormat
import java.util.Locale

/**
 * Section displaying the top 5 noisiest apps with volume bars and inline mute actions.
 * Owned by Phase 5.
 */
@Composable
fun NoisiestAppsSection(
    topApps: List<AppNotificationCount>,
    mutedPackageNames: Set<String>,
    onToggleMute: (packageName: String, isMuted: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    if (topApps.isEmpty()) return

    val maxCount = topApps.maxOfOrNull { it.count }?.coerceAtLeast(1) ?: 1
    val numberFormatter = NumberFormat.getNumberInstance(Locale.getDefault())

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(16.dp)
    ) {
        Text(
            text = stringResource(R.string.noisiest_apps_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(14.dp))

        Column(
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            topApps.forEach { app ->
                val isMuted = mutedPackageNames.contains(app.packageName)
                val progress = (app.count.toFloat() / maxCount).coerceIn(0.05f, 1f)
                val formattedCount = numberFormatter.format(app.count)

                NoisiestAppRow(
                    appLabel = app.appLabel,
                    packageName = app.packageName,
                    count = formattedCount,
                    progress = progress,
                    isMuted = isMuted,
                    onToggleMute = { onToggleMute(app.packageName, isMuted) }
                )
            }
        }
    }
}

@Composable
private fun NoisiestAppRow(
    appLabel: String,
    packageName: String,
    count: String,
    progress: Float,
    isMuted: Boolean,
    onToggleMute: () -> Unit
) {
    val cdIcon = stringResource(R.string.cd_app_icon, appLabel)
    val cdAction = if (isMuted) {
        stringResource(R.string.noisiest_apps_action_unmute, appLabel)
    } else {
        stringResource(R.string.noisiest_apps_action_mute, appLabel)
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // App avatar / letter indicator
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface)
                .semantics {
                    contentDescription = cdIcon
                },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = appLabel.firstOrNull()?.uppercase() ?: "?",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // App Name, Progress Bar, Count
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = appLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = count,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFeatureSettings = "tnum",
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Horizontal volume bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(3.dp))
                        .background(
                            if (isMuted) {
                                MaterialTheme.colorScheme.outline
                            } else {
                                MaterialTheme.colorScheme.primary
                            }
                        )
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Inline Mute Button (min 48dp touch target)
        if (isMuted) {
            OutlinedButton(
                onClick = onToggleMute,
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .semantics {
                        contentDescription = cdAction
                    },
                shape = RoundedCornerShape(8.dp),
                border = ButtonDefaults.outlinedButtonBorder.copy(
                    brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.outline)
                ),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp)
            ) {
                Text(
                    text = stringResource(R.string.noisiest_apps_muted_badge),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        } else {
            TextButton(
                onClick = onToggleMute,
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .semantics {
                        contentDescription = cdAction
                    },
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary
                ),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp)
            ) {
                Text(
                    text = stringResource(R.string.noisiest_apps_mute_btn),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
