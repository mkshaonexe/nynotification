package com.quietinbox.data.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.PeriodicWorkRequest
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkerParameters
import com.quietinbox.core.health.HealthState
import com.quietinbox.core.health.ListenerHealth
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

/**
 * Periodic background worker that checks whether the NotificationListenerService
 * is still bound and active. If disconnected or stale, forces a rebind via [ListenerHealth].
 */
@HiltWorker
class ListenerHealthWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val listenerHealth: ListenerHealth
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            listenerHealth.refresh()
            val state = listenerHealth.snapshot.value.state
            if (state != HealthState.CONNECTED) {
                listenerHealth.forceRebind()
            }
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "quietinbox_listener_health_worker"
        const val REPEAT_INTERVAL_HOURS = 6L

        /**
         * Builds a 6-hour periodic work request with battery not low constraint.
         */
        fun buildPeriodicRequest(): PeriodicWorkRequest {
            val constraints = Constraints.Builder()
                .setRequiresBatteryNotLow(true)
                .build()

            return PeriodicWorkRequestBuilder<ListenerHealthWorker>(
                REPEAT_INTERVAL_HOURS,
                TimeUnit.HOURS
            )
                .setConstraints(constraints)
                .build()
        }
    }
}
