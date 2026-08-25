package com.quietinbox.core.health

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.service.notification.NotificationListenerService
import androidx.core.app.NotificationManagerCompat
import com.quietinbox.core.time.Clock
import com.quietinbox.service.QuietListenerService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultListenerHealth @Inject constructor(
    @ApplicationContext private val context: Context,
    private val clock: Clock
) : ListenerHealth {

    companion object {
        const val STALE_THRESHOLD_MS: Long = 24 * 60 * 60 * 1000L // 24 hours
    }

    private val _snapshot = MutableStateFlow(
        HealthSnapshot(
            state = HealthState.UNKNOWN,
            lastCaptureAt = null,
            totalCaptured = 0L,
            ingestErrors = 0L
        )
    )

    override val snapshot: StateFlow<HealthSnapshot> = _snapshot.asStateFlow()

    fun recordCapture(at: Long) {
        _snapshot.update { current ->
            val hasAccess = isListenerAccessGranted()
            val state = if (!hasAccess) {
                HealthState.NO_ACCESS
            } else {
                HealthState.CONNECTED
            }
            current.copy(
                state = state,
                lastCaptureAt = at,
                totalCaptured = current.totalCaptured + 1
            )
        }
    }

    fun recordError() {
        _snapshot.update { current ->
            current.copy(ingestErrors = current.ingestErrors + 1)
        }
    }

    fun recordConnectionState(connected: Boolean) {
        _snapshot.update { current ->
            val hasAccess = isListenerAccessGranted()
            val state = when {
                !hasAccess -> HealthState.NO_ACCESS
                connected -> evaluateConnectedState(current.lastCaptureAt)
                else -> HealthState.UNKNOWN
            }
            current.copy(state = state)
        }
    }

    override fun refresh() {
        _snapshot.update { current ->
            val hasAccess = isListenerAccessGranted()
            val state = if (!hasAccess) {
                HealthState.NO_ACCESS
            } else {
                evaluateConnectedState(current.lastCaptureAt)
            }
            current.copy(state = state)
        }
    }

    override fun forceRebind() {
        runCatching {
            val component = ComponentName(context, QuietListenerService::class.java)
            val pm = context.packageManager

            // Toggle component enabled state to force Android framework to reset listener
            pm.setComponentEnabledSetting(
                component,
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP
            )
            pm.setComponentEnabledSetting(
                component,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP
            )

            NotificationListenerService.requestRebind(component)
            refresh()
        }.onFailure {
            recordError()
        }
    }

    private fun isListenerAccessGranted(): Boolean {
        return runCatching {
            NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)
        }.getOrDefault(false)
    }

    private fun evaluateConnectedState(lastCaptureAt: Long?): HealthState {
        if (lastCaptureAt != null && (clock.now() - lastCaptureAt > STALE_THRESHOLD_MS)) {
            return HealthState.STALE
        }
        return HealthState.CONNECTED
    }
}
