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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import com.quietinbox.ui.theme.QuietInboxTheme
import com.quietinbox.ui.theme.QuietTheme

@Composable
fun ConfirmSheet(
    title: String,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    isDangerous: Boolean = false,
    cancelLabel: String = "Cancel"
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(QuietTheme.shapes.sheet)
            .background(QuietTheme.colors.surface)
            .padding(QuietTheme.tokens.space24),
        verticalArrangement = Arrangement.spacedBy(QuietTheme.tokens.space16)
    ) {
        Text(
            text = title,
            style = QuietTheme.typography.titleLarge,
            color = QuietTheme.colors.onSurface
        )

        Text(
            text = message,
            style = QuietTheme.typography.bodyLarge,
            color = QuietTheme.colors.onSurfaceVariant
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(QuietTheme.tokens.space12),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier
                    .weight(1f)
                    .defaultMinSize(minHeight = QuietTheme.tokens.minTouchTarget),
                shape = QuietTheme.shapes.chip
            ) {
                Text(
                    text = cancelLabel,
                    style = QuietTheme.typography.label,
                    color = QuietTheme.colors.onSurface
                )
            }

            Button(
                onClick = onConfirm,
                modifier = Modifier
                    .weight(1f)
                    .defaultMinSize(minHeight = QuietTheme.tokens.minTouchTarget),
                shape = QuietTheme.shapes.chip,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDangerous) QuietTheme.colors.danger else QuietTheme.colors.primary,
                    contentColor = QuietTheme.colors.surface
                )
            ) {
                Text(
                    text = confirmLabel,
                    style = QuietTheme.typography.label
                )
            }
        }
    }
}

@Preview(name = "ConfirmSheet - Dangerous Dark")
@Composable
private fun ConfirmSheetDangerousDarkPreview() {
    QuietInboxTheme(darkTheme = true) {
        ConfirmSheet(
            title = "Delete Notification",
            message = "This action is permanent and cannot be undone. Are you sure?",
            confirmLabel = "Delete",
            isDangerous = true,
            onConfirm = {},
            onDismiss = {}
        )
    }
}

@Preview(name = "ConfirmSheet - Light")
@Composable
private fun ConfirmSheetLightPreview() {
    QuietInboxTheme(darkTheme = false) {
        ConfirmSheet(
            title = "Reset Rules",
            message = "Do you want to reset all always-allow rules to default settings?",
            confirmLabel = "Reset",
            isDangerous = false,
            onConfirm = {},
            onDismiss = {}
        )
    }
}
