package com.quietinbox.feature.schedules.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.quietinbox.R
import com.quietinbox.feature.schedules.model.ScheduleDaysHelper
import com.quietinbox.feature.schedules.model.SchedulePresets
import java.time.DayOfWeek

/**
 * Interactive day-of-week selector with quick preset chips (Daily, Weekdays, Weekends, Sun–Thu)
 * and individual circular day buttons adhering to 48dp minimum touch targets.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DaySelector(
    daysMask: Int,
    onDayToggle: (DayOfWeek) -> Unit,
    onMaskSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Quick preset chips
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            QuickDayChip(
                label = stringResource(R.string.schedule_days_all),
                isSelected = daysMask == SchedulePresets.MASK_ALL_DAYS,
                onClick = { onMaskSelect(SchedulePresets.MASK_ALL_DAYS) }
            )
            QuickDayChip(
                label = stringResource(R.string.schedule_days_weekdays),
                isSelected = daysMask == SchedulePresets.MASK_WEEKDAYS,
                onClick = { onMaskSelect(SchedulePresets.MASK_WEEKDAYS) }
            )
            QuickDayChip(
                label = stringResource(R.string.schedule_days_weekends),
                isSelected = daysMask == SchedulePresets.MASK_WEEKENDS,
                onClick = { onMaskSelect(SchedulePresets.MASK_WEEKENDS) }
            )
            QuickDayChip(
                label = stringResource(R.string.schedule_days_sun_thu),
                isSelected = daysMask == SchedulePresets.MASK_SUN_THU,
                onClick = { onMaskSelect(SchedulePresets.MASK_SUN_THU) }
            )
        }

        // 7 individual day circles
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val dayLabels = listOf(
                DayOfWeek.MONDAY to stringResource(R.string.schedule_day_m),
                DayOfWeek.TUESDAY to stringResource(R.string.schedule_day_t),
                DayOfWeek.WEDNESDAY to stringResource(R.string.schedule_day_w),
                DayOfWeek.THURSDAY to stringResource(R.string.schedule_day_th),
                DayOfWeek.FRIDAY to stringResource(R.string.schedule_day_f),
                DayOfWeek.SATURDAY to stringResource(R.string.schedule_day_s),
                DayOfWeek.SUNDAY to stringResource(R.string.schedule_day_su)
            )

            dayLabels.forEach { (day, label) ->
                val isSelected = ScheduleDaysHelper.isDaySelected(daysMask, day)
                DayCircleButton(
                    dayOfWeek = day,
                    label = label,
                    isSelected = isSelected,
                    onClick = { onDayToggle(day) }
                )
            }
        }
    }
}

@Composable
private fun QuickDayChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = isSelected,
        onClick = onClick,
        label = { Text(text = label, style = MaterialTheme.typography.labelMedium) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
        ),
        modifier = Modifier.heightIn(min = 48.dp)
    )
}

@Composable
private fun DayCircleButton(
    dayOfWeek: DayOfWeek,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val dayName = dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() }

    Box(
        modifier = Modifier
            .size(48.dp)
            .semantics {
                role = Role.Checkbox
                contentDescription = "$dayName ${if (isSelected) "selected" else "not selected"}"
            }
            .clip(CircleShape)
            .background(
                if (isSelected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surfaceVariant
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (isSelected) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
