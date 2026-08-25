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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.quietinbox.ui.theme.QuietInboxTheme
import com.quietinbox.ui.theme.QuietTheme

@Composable
fun AppRow(
    label: String,
    packageName: String,
    modifier: Modifier = Modifier,
    notificationCount: Int = 0,
    isChecked: Boolean? = null,
    onCheckedChange: ((Boolean) -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(QuietTheme.shapes.card)
            .background(QuietTheme.colors.surface)
            .then(
                if (onClick != null) Modifier.clickable(onClick = onClick)
                else if (onCheckedChange != null && isChecked != null) Modifier.clickable { onCheckedChange(!isChecked) }
                else Modifier
            )
            .padding(
                horizontal = QuietTheme.tokens.screenHorizontalPadding,
                vertical = QuietTheme.tokens.rowVerticalPadding
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(QuietTheme.tokens.space12)
    ) {
        // App Icon
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(QuietTheme.tokens.radiusChip))
                .background(QuietTheme.colors.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label.take(1).uppercase(),
                style = QuietTheme.typography.titleMedium,
                color = QuietTheme.colors.primary
            )
        }

        // Info
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(QuietTheme.tokens.space4)
        ) {
            Text(
                text = label,
                style = QuietTheme.typography.titleMedium,
                color = QuietTheme.colors.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = packageName,
                style = QuietTheme.typography.label,
                color = QuietTheme.colors.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (notificationCount > 0) {
            Text(
                text = "$notificationCount",
                style = QuietTheme.typography.mono,
                color = QuietTheme.colors.onSurfaceVariant
            )
        }

        if (isChecked != null && onCheckedChange != null) {
            Checkbox(
                checked = isChecked,
                onCheckedChange = onCheckedChange,
                colors = CheckboxDefaults.colors(
                    checkedColor = QuietTheme.colors.primary,
                    uncheckedColor = QuietTheme.colors.outline,
                    checkmarkColor = QuietTheme.colors.background
                )
            )
        }
    }
}

@Preview(name = "AppRow - Dark")
@Composable
private fun AppRowDarkPreview() {
    QuietInboxTheme(darkTheme = true) {
        AppRow(
            label = "WhatsApp",
            packageName = "com.whatsapp",
            notificationCount = 142,
            isChecked = true,
            onCheckedChange = {}
        )
    }
}

@Preview(name = "AppRow - Light")
@Composable
private fun AppRowLightPreview() {
    QuietInboxTheme(darkTheme = false) {
        AppRow(
            label = "Instagram",
            packageName = "com.instagram.android",
            notificationCount = 312,
            isChecked = false,
            onCheckedChange = {}
        )
    }
}
