package com.quietinbox.feature.permissions

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quietinbox.core.health.HealthSnapshot
import com.quietinbox.core.health.HealthState
import com.quietinbox.core.health.ListenerHealth
import com.quietinbox.core.time.Clock
import com.quietinbox.feature.onboarding.util.OemBrand
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject

/**
 * UI State for the Permissions & Health screen.
 */
data class PermissionsHealthUiState(
    val hasNotificationAccess: Boolean = false,
    val hasPostNotificationPermission: Boolean = false,
    val isIgnoringBatteryOptimizations: Boolean = false,
    val oemBrand: OemBrand = OemBrand.fromManufacturer(),
    val isOemNoticeDismissed: Boolean = false,
    val showAutostartDialog: Boolean = false,
    val healthSnapshot: HealthSnapshot = HealthSnapshot(
        state = HealthState.UNKNOWN,
        lastCaptureAt = null,
        totalCaptured = 0L,
        ingestErrors = 0L
    )
)

/**
 * ViewModel managing live status detection for permissions, battery optimization,
 * listener service health, and diagnostics.
 */
@HiltViewModel
class PermissionsHealthViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val listenerHealth: ListenerHealth,
    private val clock: Clock
) : ViewModel() {

    private val _localState = MutableStateFlow(
        PermissionsHealthUiState(
            hasNotificationAccess = checkNotificationAccess(),
            hasPostNotificationPermission = checkPostNotificationPermission(),
            isIgnoringBatteryOptimizations = checkBatteryOptimizations(),
            oemBrand = OemBrand.fromManufacturer()
        )
    )

    val uiState: StateFlow<PermissionsHealthUiState> = combine(
        _localState,
        listenerHealth.snapshot
    ) { local, snapshot ->
        local.copy(healthSnapshot = snapshot)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = _localState.value
    )

    fun refreshAll() {
        listenerHealth.refresh()
        _localState.update { current ->
            current.copy(
                hasNotificationAccess = checkNotificationAccess(),
                hasPostNotificationPermission = checkPostNotificationPermission(),
                isIgnoringBatteryOptimizations = checkBatteryOptimizations()
            )
        }
    }

    fun reconnectListener() {
        listenerHealth.forceRebind()
        refreshAll()
    }

    fun dismissOemNotice() {
        _localState.update { it.copy(isOemNoticeDismissed = true) }
    }

    fun showAutostartDialog() {
        _localState.update { it.copy(showAutostartDialog = true) }
    }

    fun hideAutostartDialog() {
        _localState.update { it.copy(showAutostartDialog = false) }
    }

    fun formatLastCapture(lastCaptureAt: Long?): String {
        if (lastCaptureAt == null || lastCaptureAt <= 0L) {
            return "Never"
        }
        val now = clock.nowEpochMs()
        val diffMs = now - lastCaptureAt
        if (diffMs < 0) return "Just now"

        val seconds = diffMs / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24

        return when {
            seconds < 60 -> "Just now"
            minutes < 60 -> "${minutes}m ago"
            hours < 24 -> "${hours}h ago"
            days == 1L -> "Yesterday"
            else -> "${days}d ago"
        }
    }

    private fun checkNotificationAccess(): Boolean {
        return try {
            val enabled = NotificationManagerCompat.getEnabledListenerPackages(context)
            enabled.contains(context.packageName)
        } catch (_: Exception) {
            false
        }
    }

    private fun checkPostNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    private fun checkBatteryOptimizations(): Boolean {
        return try {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            powerManager?.isIgnoringBatteryOptimizations(context.packageName) ?: false
        } catch (_: Exception) {
            false
        }
    }
}
