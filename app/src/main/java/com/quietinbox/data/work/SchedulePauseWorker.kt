package com.quietinbox.data.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.quietinbox.core.firewall.ScheduleEvaluator
import com.quietinbox.core.time.Clock
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

/**
 * One-shot background worker executed when a schedule window opens or closes.
 * Refreshes active status and UI state. Correctness never relies on this worker
 * as [ScheduleEvaluator] reads the clock directly at evaluation time.
 */
@HiltWorker
class SchedulePauseWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val scheduleEvaluator: ScheduleEvaluator,
    private val clock: Clock
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        // Query current active schedule to refresh any cached status or notify components
        scheduleEvaluator.activeAt(clock.now())
        return Result.success()
    }

    companion object {
        const val WORK_NAME = "schedule_pause_worker"

        /**
         * Enqueues a one-shot work request with a delay in milliseconds.
         *
         * @param context Application context.
         * @param delayMs Delay in milliseconds until schedule boundary.
         */
        fun scheduleTransition(context: Context, delayMs: Long) {
            if (delayMs <= 0) return
            val request = OneTimeWorkRequestBuilder<SchedulePauseWorker>()
                .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                request
            )
        }
    }
}
