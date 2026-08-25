package com.quietinbox.data.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.quietinbox.core.time.Clock
import com.quietinbox.data.db.entity.DailyStatEntity
import com.quietinbox.data.prefs.SettingsDataStore
import com.quietinbox.feature.settings.data.SettingsDao
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.ZoneId

/**
 * Background worker responsible for:
 * 1. Purging unstarred notifications older than the user's retention setting (30/90/365 days).
 * 2. Enforcing the 50,000 row cap by dropping the oldest unstarred notifications.
 * 3. Preserving all starred notifications indefinitely.
 * 4. Rebuilding daily_stats for active days.
 * 5. Running SQLite VACUUM at most once a week.
 */
@HiltWorker
class RetentionWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val settingsDao: SettingsDao,
    private val settingsDataStore: SettingsDataStore,
    private val clock: Clock
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val WORK_NAME = "com.quietinbox.data.work.RetentionWorker"
        const val MAX_NOTIFICATION_CAP = 50_000
        const val VACUUM_INTERVAL_MS = 7L * 24 * 60 * 60 * 1000 // 7 days
        const val MS_PER_DAY = 24L * 60 * 60 * 1000
        private const val PREFS_NAME = "quiet_inbox_retention_prefs"
        private const val KEY_LAST_VACUUM_MS = "last_vacuum_epoch_ms"
    }

    override suspend fun doWork(): Result {
        return runCatching {
            val settings = settingsDataStore.settings.first()
            val nowMs = clock.now()

            // 1. Purge older than retention window (if not Forever / 0)
            val retentionDays = settings.retentionDays
            if (retentionDays > 0) {
                val cutoffMs = nowMs - (retentionDays.toLong() * MS_PER_DAY)
                settingsDao.deleteUnstarredOlderThan(cutoffMs)
            }

            // 2. Enforce 50,000 hard row cap (oldest unstarred dropped first)
            val currentCount = settingsDao.getNotificationCount()
            if (currentCount > MAX_NOTIFICATION_CAP) {
                val excess = currentCount - MAX_NOTIFICATION_CAP
                settingsDao.deleteOldestUnstarred(excess)
            }

            // 3. Rebuild daily_stats for existing days
            rebuildDailyStats()

            // 4. Run VACUUM at most once a week
            val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val lastVacuumMs = prefs.getLong(KEY_LAST_VACUUM_MS, 0L)
            if (nowMs - lastVacuumMs >= VACUUM_INTERVAL_MS) {
                runCatching {
                    settingsDao.executeRaw(SimpleSQLiteQuery("VACUUM"))
                    prefs.edit().putLong(KEY_LAST_VACUUM_MS, nowMs).apply()
                }
            }

            Result.success()
        }.getOrElse {
            Result.retry()
        }
    }

    /**
     * Rebuilds aggregated daily statistics from captured notifications.
     */
    suspend fun rebuildDailyStats() {
        val days = settingsDao.getDistinctNotificationDays()
        if (days.isEmpty()) {
            settingsDao.deleteAllDailyStats()
            return
        }

        // Remove old stats for days that no longer have any records
        settingsDao.deleteDailyStatsNotIn(days)

        val zoneId = ZoneId.systemDefault()
        for (dayInt in days) {
            val year = dayInt / 10000
            val month = (dayInt % 10000) / 100
            val day = dayInt % 100

            val localDate = runCatching { LocalDate.of(year, month, day) }.getOrNull() ?: continue
            val startEpochMs = localDate.atStartOfDay(zoneId).toInstant().toEpochMilli()
            val endEpochMs = localDate.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli() - 1

            val captured = settingsDao.getCapturedCountInRange(startEpochMs, endEpochMs)
            val silenced = settingsDao.getSilencedCountInRange(startEpochMs, endEpochMs)
            val allowed = settingsDao.getAllowedCountInRange(startEpochMs, endEpochMs)
            val distinctApps = settingsDao.getDistinctAppsInRange(startEpochMs, endEpochMs)

            val stat = DailyStatEntity(
                day = dayInt,
                captured = captured,
                silenced = silenced,
                allowed = allowed,
                quietMinutes = 0,
                distinctApps = distinctApps
            )
            settingsDao.insertOrUpdateDailyStat(stat)
        }
    }
}
