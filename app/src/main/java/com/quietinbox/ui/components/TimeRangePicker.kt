package com.quietinbox.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import com.quietinbox.ui.theme.QuietInboxTheme
import com.quietinbox.ui.theme.QuietTheme
import java.util.Locale

fun formatMinuteOfDay(minuteOfDay: Int): String {
    val totalMinutes = ((minuteOfDay % 1440) + 1440) % 1440
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    val amPm = if (hours < 12) "AM" else "PM"
    val displayHours = when {
        hours == 0 -> 12
        hours > 12 -> hours - 12
        else -> hours
    }
    return String.format(Locale.US, "%d:%02d %s", displayHours, minutes, amPm)
}

@Composable
fun TimeRangePicker(
    startMinuteOfDay: Int,
    endMinuteOfDay: Int,
    onStartTimeClick: () -> Unit,
    onEndTimeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val crossesMidnight = startMinuteOfDay > endMinuteOfDay

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(QuietTheme.shapes.card)
            .background(QuietTheme.colors.surface)
            .padding(QuietTheme.tokens.space16),
        verticalArrangement = Arrangement.spacedBy(QuietTheme.tokens.space12)
    ) {
        Text(
            text = "Time Range",
            style = QuietTheme.typography.titleMedium,
            color = QuietTheme.colors.onSurface
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(QuietTheme.tokens.space12)
        ) {
            // Start Time Card
            Box(
                modifier = Modifier
                    .weight(1f)
                    .defaultMinSize(minHeight = QuietTheme.tokens.minTouchTarget)
                    .clip(QuietTheme.shapes.chip)
                    .background(QuietTheme.colors.surfaceVariant)
                    .clickable(onClick = onStartTimeClick)
                    .padding(QuietTheme.tokens.space12),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Start",
                        style = QuietTheme.typography.label,
                        color = QuietTheme.colors.onSurfaceVariant
                    )
                    Text(
                        text = formatMinuteOfDay(startMinuteOfDay),
                        style = QuietTheme.typography.titleLarge,
                        color = QuietTheme.colors.onSurface
                    )
                }
            }

            // End Time Card
            Box(
                modifier = Modifier
                    .weight(1f)
                    .defaultMinSize(minHeight = QuietTheme.tokens.minTouchTarget)
                    .clip(QuietTheme.shapes.chip)
                    .background(QuietTheme.colors.surfaceVariant)
                    .clickable(onClick = onEndTimeClick)
                    .padding(QuietTheme.tokens.space12),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "End",
                        style = QuietTheme.typography.label,
                        color = QuietTheme.colors.onSurfaceVariant
                    )
                    Text(
                        text = formatMinuteOfDay(endMinuteOfDay),
                        style = QuietTheme.typography.titleLarge,
                        color = QuietTheme.colors.onSurface
                    )
                }
            }
        }

        if (crossesMidnight) {
            Text(
                text = "Runs overnight (ends the following morning)",
                style = QuietTheme.typography.label,
                color = QuietTheme.colors.primary
            )
        }
    }
}

@Preview(name = "TimeRangePicker - Overnight Dark")
@Composable
private fun TimeRangePickerOvernightDarkPreview() {
    QuietInboxTheme(darkTheme = true) {
        TimeRangePicker(
            startMinuteOfDay = 22 * 60, // 10:00 PM
            endMinuteOfDay = 7 * 60,   // 7:00 AM
            onStartTimeClick = {},
            onEndTimeClick = {}
        )
    }
}

@Preview(name = "TimeRangePicker - Daytime Light")
@Composable
private fun TimeRangePickerDaytimeLightPreview() {
    QuietInboxTheme(darkTheme = false) {
        TimeRangePicker(
            startMinuteOfDay = 9 * 60,  // 9:00 AM
            endMinuteOfDay = 17 * 60,  // 5:00 PM
            onStartTimeClick = {},
            onEndTimeClick = {}
        )
    }
}
