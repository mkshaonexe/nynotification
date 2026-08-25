package com.quietinbox.feature.home.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.quietinbox.R

/**
 * Pause chip options: 5 min, 15 min, 1 hour, Until I turn it back on.
 * Touch targets >= 48dp. Owned by Phase 5.
 */
@Composable
fun PauseChipRow(
    onPauseDurationSelected: (Long) -> Unit,
    onTurnOff: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 5 min pause chip (5 * 60 * 1000 ms)
        FilterChip(
            selected = false,
            onClick = { onPauseDurationSelected(5 * 60 * 1000L) },
            label = { Text(stringResource(R.string.pause_5_min)) },
            modifier = Modifier.heightIn(min = 48.dp),
            colors = FilterChipDefaults.filterChipColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                labelColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )

        // 15 min pause chip (15 * 60 * 1000 ms)
        FilterChip(
            selected = false,
            onClick = { onPauseDurationSelected(15 * 60 * 1000L) },
            label = { Text(stringResource(R.string.pause_15_min)) },
            modifier = Modifier.heightIn(min = 48.dp),
            colors = FilterChipDefaults.filterChipColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                labelColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )

        // 1 hour pause chip (60 * 60 * 1000 ms)
        FilterChip(
            selected = false,
            onClick = { onPauseDurationSelected(60 * 60 * 1000L) },
            label = { Text(stringResource(R.string.pause_1_hour)) },
            modifier = Modifier.heightIn(min = 48.dp),
            colors = FilterChipDefaults.filterChipColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                labelColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )

        // Until I turn it back on
        FilterChip(
            selected = false,
            onClick = onTurnOff,
            label = { Text(stringResource(R.string.pause_until_off)) },
            modifier = Modifier.heightIn(min = 48.dp),
            colors = FilterChipDefaults.filterChipColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                labelColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )
    }
}
