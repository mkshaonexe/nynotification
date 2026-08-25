package com.quietinbox.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import com.quietinbox.core.apps.InstalledApp
import com.quietinbox.ui.theme.QuietInboxTheme
import com.quietinbox.ui.theme.QuietTheme

@Composable
fun AppPickerSheet(
    apps: List<InstalledApp>,
    selectedPackages: Set<String>,
    onTogglePackage: (String) -> Unit,
    onDone: () -> Unit,
    showSystemComponents: Boolean,
    onToggleSystemComponents: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }

    val filteredApps = remember(apps, searchQuery, showSystemComponents) {
        apps.filter { app ->
            (showSystemComponents || app.isUserFacing) &&
                    (searchQuery.isBlank() || app.label.contains(searchQuery, ignoreCase = true) || app.packageName.contains(searchQuery, ignoreCase = true))
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight(0.85f)
            .clip(QuietTheme.shapes.sheet)
            .background(QuietTheme.colors.surface)
            .padding(QuietTheme.tokens.space16),
        verticalArrangement = Arrangement.spacedBy(QuietTheme.tokens.space12)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Select Apps",
                style = QuietTheme.typography.titleLarge,
                color = QuietTheme.colors.onSurface
            )

            Button(
                onClick = onDone,
                shape = QuietTheme.shapes.chip,
                colors = ButtonDefaults.buttonColors(
                    containerColor = QuietTheme.colors.primary,
                    contentColor = QuietTheme.colors.surface
                )
            ) {
                Text(text = "Done", style = QuietTheme.typography.label)
            }
        }

        SearchField(
            query = searchQuery,
            onQueryChange = { searchQuery = it },
            placeholder = "Search installed apps…"
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Show system components",
                style = QuietTheme.typography.bodyMedium,
                color = QuietTheme.colors.onSurfaceVariant
            )
            Switch(
                checked = showSystemComponents,
                onCheckedChange = onToggleSystemComponents,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = QuietTheme.colors.surface,
                    checkedTrackColor = QuietTheme.colors.primary,
                    uncheckedThumbColor = QuietTheme.colors.onSurfaceVariant,
                    uncheckedTrackColor = QuietTheme.colors.surfaceVariant
                )
            )
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(QuietTheme.tokens.space8)
        ) {
            items(filteredApps, key = { it.packageName }) { app ->
                AppRow(
                    label = app.label,
                    packageName = app.packageName,
                    notificationCount = app.notificationCount,
                    isChecked = selectedPackages.contains(app.packageName),
                    onCheckedChange = { onTogglePackage(app.packageName) }
                )
            }
        }
    }
}

@Preview(name = "AppPickerSheet - Dark")
@Composable
private fun AppPickerSheetDarkPreview() {
    val sampleApps = listOf(
        InstalledApp("com.whatsapp", "WhatsApp", isLaunchable = true, isPreinstalled = false, wasUpdatedSystemApp = false, notificationCount = 120),
        InstalledApp("com.google.android.youtube", "YouTube", isLaunchable = true, isPreinstalled = true, wasUpdatedSystemApp = true, notificationCount = 45),
        InstalledApp("com.android.chrome", "Chrome", isLaunchable = true, isPreinstalled = true, wasUpdatedSystemApp = false, notificationCount = 10),
        InstalledApp("com.android.systemui", "System UI", isLaunchable = false, isPreinstalled = true, wasUpdatedSystemApp = false, notificationCount = 0)
    )

    QuietInboxTheme(darkTheme = true) {
        AppPickerSheet(
            apps = sampleApps,
            selectedPackages = setOf("com.whatsapp"),
            onTogglePackage = {},
            onDone = {},
            showSystemComponents = false,
            onToggleSystemComponents = {}
        )
    }
}
