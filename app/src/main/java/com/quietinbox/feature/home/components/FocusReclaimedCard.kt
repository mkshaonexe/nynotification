package com.quietinbox.feature.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.quietinbox.R
import com.quietinbox.feature.home.StatsTuning

/**
 * Card displaying the estimated focus reclaimed from silenced interruptions.
 * Displayed only when silenced >= 50 and not hidden by user.
 * Owned by Phase 5.
 */
@Composable
fun FocusReclaimedCard(
    silencedCount: Int,
    secondsPerInterruption: Int,
    onSecondsPerInterruptionChanged: (Int) -> Unit,
    onHideEstimate: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (silencedCount < StatsTuning.FOCUS_RECLAIMED_MIN_SILENCED) return

    var showDialog by remember { mutableStateOf(false) }

    // Calculate time saved
    val totalSeconds = (silencedCount.toLong() * secondsPerInterruption)
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60

    val timeReclaimedString = when {
        hours > 0 -> stringResource(R.string.focus_reclaimed_hours_mins, hours, minutes)
        minutes > 0 -> stringResource(R.string.focus_reclaimed_mins, minutes)
        else -> stringResource(R.string.focus_reclaimed_seconds, seconds)
    }

    val title = stringResource(R.string.focus_reclaimed_title, timeReclaimedString)
    val assumption = stringResource(R.string.focus_reclaimed_assumption, secondsPerInterruption)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(role = Role.Button) {
                showDialog = true
            }
            .padding(16.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = assumption,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    if (showDialog) {
        FocusReclaimedConfigDialog(
            currentSeconds = secondsPerInterruption,
            onOptionSelected = {
                onSecondsPerInterruptionChanged(it)
                showDialog = false
            },
            onHideSelected = {
                onHideEstimate()
                showDialog = false
            },
            onDismiss = { showDialog = false }
        )
    }
}

@Composable
private fun FocusReclaimedConfigDialog(
    currentSeconds: Int,
    onOptionSelected: (Int) -> Unit,
    onHideSelected: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.focus_reclaimed_dialog_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                FocusOptionRow(
                    label = stringResource(R.string.focus_reclaimed_4s),
                    selected = currentSeconds == 4,
                    onClick = { onOptionSelected(4) }
                )
                FocusOptionRow(
                    label = stringResource(R.string.focus_reclaimed_8s),
                    selected = currentSeconds == 8,
                    onClick = { onOptionSelected(8) }
                )
                FocusOptionRow(
                    label = stringResource(R.string.focus_reclaimed_15s),
                    selected = currentSeconds == 15,
                    onClick = { onOptionSelected(15) }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onHideSelected) {
                Text(
                    text = stringResource(R.string.focus_reclaimed_hide),
                    color = MaterialTheme.colorScheme.error
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Close")
            }
        }
    )
}

@Composable
private fun FocusOptionRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(
                selectedColor = MaterialTheme.colorScheme.primary
            )
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
