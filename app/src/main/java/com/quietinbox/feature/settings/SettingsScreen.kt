package com.quietinbox.feature.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.quietinbox.R

/**
 * Root Settings screen displaying a flat list of eight configuration destinations,
 * appearance preferences, and digest settings.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateToPermissionsHealth: () -> Unit = {},
    onNavigateToAllowRules: () -> Unit = {},
    onNavigateToSchedules: () -> Unit = {},
    onNavigateToMutedApps: () -> Unit = {},
    onNavigateToStorage: () -> Unit = {},
    onNavigateToAppLock: () -> Unit = {},
    onNavigateToPrivacy: () -> Unit = {},
    onNavigateToAbout: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val settings by viewModel.settings.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.settings_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // General & Rules Section
            SettingsSectionHeader(stringResource(R.string.settings_section_general))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column {
                    SettingsNavigationRow(
                        title = stringResource(R.string.settings_row_permissions_title),
                        subtitle = stringResource(R.string.settings_row_permissions_subtitle),
                        icon = Icons.Default.CheckCircle,
                        onClick = onNavigateToPermissionsHealth
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    SettingsNavigationRow(
                        title = stringResource(R.string.settings_row_allow_rules_title),
                        subtitle = stringResource(R.string.settings_row_allow_rules_subtitle),
                        icon = Icons.Default.NotificationsActive,
                        onClick = onNavigateToAllowRules
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    SettingsNavigationRow(
                        title = stringResource(R.string.settings_row_schedules_title),
                        subtitle = stringResource(R.string.settings_row_schedules_subtitle),
                        icon = Icons.Default.Schedule,
                        onClick = onNavigateToSchedules
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    SettingsNavigationRow(
                        title = stringResource(R.string.settings_row_muted_apps_title),
                        subtitle = stringResource(R.string.settings_row_muted_apps_subtitle),
                        icon = Icons.Default.Block,
                        onClick = onNavigateToMutedApps
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Data & Security Section
            SettingsSectionHeader(stringResource(R.string.settings_section_data_security))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column {
                    SettingsNavigationRow(
                        title = stringResource(R.string.settings_row_storage_title),
                        subtitle = stringResource(R.string.settings_row_storage_subtitle),
                        icon = Icons.Default.Storage,
                        onClick = onNavigateToStorage
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    SettingsNavigationRow(
                        title = stringResource(R.string.settings_row_app_lock_title),
                        subtitle = stringResource(R.string.settings_row_app_lock_subtitle),
                        icon = Icons.Default.Fingerprint,
                        onClick = onNavigateToAppLock
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    SettingsNavigationRow(
                        title = stringResource(R.string.settings_row_privacy_title),
                        subtitle = stringResource(R.string.settings_row_privacy_subtitle),
                        icon = Icons.Default.Policy,
                        onClick = onNavigateToPrivacy
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Appearance Section
            SettingsSectionHeader(stringResource(R.string.settings_section_appearance))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.settings_theme_mode_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = settings.themeMode == "SYSTEM",
                            onClick = { viewModel.setThemeMode("SYSTEM") },
                            label = { Text(stringResource(R.string.settings_theme_mode_system)) }
                        )
                        FilterChip(
                            selected = settings.themeMode == "LIGHT",
                            onClick = { viewModel.setThemeMode("LIGHT") },
                            label = { Text(stringResource(R.string.settings_theme_mode_light)) }
                        )
                        FilterChip(
                            selected = settings.themeMode == "DARK",
                            onClick = { viewModel.setThemeMode("DARK") },
                            label = { Text(stringResource(R.string.settings_theme_mode_dark)) }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.settings_dynamic_color_title),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = stringResource(R.string.settings_dynamic_color_subtitle),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = settings.dynamicColorEnabled,
                            onCheckedChange = { viewModel.setDynamicColorEnabled(it) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // About Section
            SettingsSectionHeader(stringResource(R.string.settings_section_about))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                SettingsNavigationRow(
                    title = stringResource(R.string.settings_row_about_title),
                    subtitle = stringResource(R.string.settings_row_about_subtitle),
                    icon = Icons.Default.Info,
                    onClick = onNavigateToAbout
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun SettingsSectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
    )
}

@Composable
fun SettingsNavigationRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(16.dp)
        )
    }
}
