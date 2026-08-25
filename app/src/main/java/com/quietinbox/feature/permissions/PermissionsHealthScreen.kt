package com.quietinbox.feature.permissions

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quietinbox.R
import com.quietinbox.core.health.HealthState
import com.quietinbox.feature.permissions.components.AutostartInstructionsDialog
import com.quietinbox.feature.permissions.components.HealthStatusType
import com.quietinbox.feature.permissions.components.OemNoticeCard
import com.quietinbox.feature.permissions.components.PermissionHealthRow
import com.quietinbox.feature.permissions.util.AutostartHelper

/**
 * Screen displaying live status for all system permissions, battery optimization,
 * OEM autostart, and listener service health.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionsHealthScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PermissionsHealthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Request runtime notification permission (Android 13+)
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = {
            viewModel.refreshAll()
        }
    )

    // Refresh status when returning to the screen
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshAll()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.permissions_health_title),
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // 1. Aggressive OEM Notice (if detected and not dismissed)
            if (uiState.oemBrand.isAggressive && !uiState.isOemNoticeDismissed) {
                item(key = "oem_notice") {
                    OemNoticeCard(
                        oemName = uiState.oemBrand.displayName,
                        onFixClick = {
                            val opened = AutostartHelper.openAutostartSettings(context)
                            if (!opened) {
                                viewModel.showAutostartDialog()
                            }
                        },
                        onDismissClick = {
                            viewModel.dismissOemNotice()
                        }
                    )
                }
            }

            // 2. Notification Listener Permission (Required)
            item(key = "perm_listener") {
                PermissionHealthRow(
                    title = stringResource(R.string.perm_listener_title),
                    description = stringResource(R.string.perm_listener_desc),
                    statusText = if (uiState.hasNotificationAccess) {
                        stringResource(R.string.permissions_status_granted)
                    } else {
                        stringResource(R.string.permissions_status_missing)
                    },
                    statusType = if (uiState.hasNotificationAccess) HealthStatusType.SUCCESS else HealthStatusType.ERROR,
                    icon = Icons.Default.Security,
                    actionText = if (!uiState.hasNotificationAccess) stringResource(R.string.perm_listener_fix) else null,
                    onActionClick = {
                        try {
                            val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                            context.startActivity(intent)
                        } catch (_: Exception) {
                            try {
                                context.startActivity(Intent(Settings.ACTION_SETTINGS))
                            } catch (_: Exception) {}
                        }
                    }
                )
            }

            // 3. Post Notifications (Optional, Android 13+)
            item(key = "perm_post_notif") {
                PermissionHealthRow(
                    title = stringResource(R.string.perm_post_notif_title),
                    description = stringResource(R.string.perm_post_notif_desc),
                    statusText = if (uiState.hasPostNotificationPermission) {
                        stringResource(R.string.permissions_status_granted)
                    } else {
                        stringResource(R.string.permissions_status_missing)
                    },
                    statusType = if (uiState.hasPostNotificationPermission) HealthStatusType.SUCCESS else HealthStatusType.WARNING,
                    icon = Icons.Default.Notifications,
                    actionText = if (!uiState.hasPostNotificationPermission) stringResource(R.string.perm_post_notif_fix) else null,
                    onActionClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                )
            }

            // 4. Battery Optimization Exemption
            item(key = "perm_battery") {
                PermissionHealthRow(
                    title = stringResource(R.string.perm_battery_title),
                    description = stringResource(R.string.perm_battery_desc),
                    statusText = if (uiState.isIgnoringBatteryOptimizations) {
                        stringResource(R.string.permissions_status_enabled)
                    } else {
                        stringResource(R.string.permissions_status_disabled)
                    },
                    statusType = if (uiState.isIgnoringBatteryOptimizations) HealthStatusType.SUCCESS else HealthStatusType.WARNING,
                    icon = Icons.Default.BatteryAlert,
                    actionText = if (!uiState.isIgnoringBatteryOptimizations) stringResource(R.string.perm_battery_fix) else null,
                    onActionClick = {
                        try {
                            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                data = Uri.parse("package:${context.packageName}")
                            }
                            context.startActivity(intent)
                        } catch (_: Exception) {
                            try {
                                context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                            } catch (_: Exception) {}
                        }
                    }
                )
            }

            // 5. OEM Autostart (Xiaomi / Huawei / Oppo / Vivo / Samsung)
            if (uiState.oemBrand.isAggressive) {
                item(key = "perm_autostart") {
                    PermissionHealthRow(
                        title = stringResource(R.string.perm_autostart_title),
                        description = stringResource(R.string.perm_autostart_desc, uiState.oemBrand.displayName),
                        statusText = stringResource(R.string.perm_autostart_fix),
                        statusType = HealthStatusType.INFO,
                        icon = Icons.Default.PowerSettingsNew,
                        actionText = stringResource(R.string.perm_autostart_fix),
                        onActionClick = {
                            val success = AutostartHelper.openAutostartSettings(context)
                            if (!success) {
                                viewModel.showAutostartDialog()
                            }
                        }
                    )
                }
            }

            // 6. Listener Service Live Connection State
            item(key = "perm_service_connection") {
                val state = uiState.healthSnapshot.state
                val (statusText, statusType) = when (state) {
                    HealthState.CONNECTED -> stringResource(R.string.permissions_status_active) to HealthStatusType.SUCCESS
                    HealthState.STALE -> stringResource(R.string.permissions_status_stale) to HealthStatusType.WARNING
                    HealthState.NO_ACCESS -> stringResource(R.string.permissions_status_disconnected) to HealthStatusType.ERROR
                    HealthState.UNKNOWN -> stringResource(R.string.permissions_status_unknown) to HealthStatusType.INFO
                }

                PermissionHealthRow(
                    title = stringResource(R.string.perm_service_connection_title),
                    description = stringResource(R.string.perm_service_connection_desc),
                    statusText = statusText,
                    statusType = statusType,
                    icon = Icons.Default.NotificationsActive,
                    actionText = stringResource(R.string.perm_service_reconnect_btn),
                    onActionClick = {
                        viewModel.reconnectListener()
                    }
                )
            }

            // 7. Last Notification Captured & Diagnostics
            item(key = "perm_diagnostics") {
                val formattedTime = viewModel.formatLastCapture(uiState.healthSnapshot.lastCaptureAt)

                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = stringResource(R.string.perm_last_capture_title),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = formattedTime,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column {
                                Text(
                                    text = stringResource(R.string.perm_total_captured_title),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = uiState.healthSnapshot.totalCaptured.toString(),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = stringResource(R.string.perm_total_errors_title),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = uiState.healthSnapshot.ingestErrors.toString(),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = if (uiState.healthSnapshot.ingestErrors > 0) {
                                        MaterialTheme.colorScheme.error
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (uiState.showAutostartDialog) {
        AutostartInstructionsDialog(
            brand = uiState.oemBrand,
            onDismissRequest = { viewModel.hideAutostartDialog() }
        )
    }
}
