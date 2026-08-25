package com.quietinbox.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.quietinbox.ui.theme.QuietInboxTheme
import com.quietinbox.ui.theme.QuietTheme

@Composable
fun QuietSwitch(
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 56.dp)
            .clip(QuietTheme.shapes.card)
            .background(QuietTheme.colors.surface)
            .clickable(role = Role.Switch) { onEnabledChange(!enabled) }
            .padding(
                horizontal = QuietTheme.tokens.screenHorizontalPadding,
                vertical = QuietTheme.tokens.space12
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(QuietTheme.tokens.space4)
        ) {
            Text(
                text = "Quiet Mode",
                style = QuietTheme.typography.titleLarge,
                color = QuietTheme.colors.onSurface
            )
            Text(
                text = subtitle,
                style = QuietTheme.typography.bodyMedium,
                color = if (enabled) QuietTheme.colors.primary else QuietTheme.colors.onSurfaceVariant
            )
        }

        Switch(
            checked = enabled,
            onCheckedChange = onEnabledChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = QuietTheme.colors.surface,
                checkedTrackColor = QuietTheme.colors.primary,
                uncheckedThumbColor = QuietTheme.colors.onSurfaceVariant,
                uncheckedTrackColor = QuietTheme.colors.surfaceVariant
            )
        )
    }
}

@Preview(name = "QuietSwitch - On Dark")
@Composable
private fun QuietSwitchOnDarkPreview() {
    QuietInboxTheme(darkTheme = true) {
        QuietSwitch(
            enabled = true,
            onEnabledChange = {},
            subtitle = "On since 9:14 AM"
        )
    }
}

@Preview(name = "QuietSwitch - Off Light")
@Composable
private fun QuietSwitchOffLightPreview() {
    QuietInboxTheme(darkTheme = false) {
        QuietSwitch(
            enabled = false,
            onEnabledChange = {},
            subtitle = "Off · All notifications deliver"
        )
    }
}
