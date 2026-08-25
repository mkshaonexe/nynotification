package com.quietinbox.core.health

import kotlinx.coroutines.flow.StateFlow

enum class HealthState { CONNECTED, NO_ACCESS, STALE, UNKNOWN }

data class HealthSnapshot(
    val state: HealthState,
    val lastCaptureAt: Long?,
    val totalCaptured: Long,
    val ingestErrors: Long,
)

interface ListenerHealth {
    val snapshot: StateFlow<HealthSnapshot>
    fun refresh()
    /** Disable + re-enable the component and requestRebind. */
    fun forceRebind()
}
