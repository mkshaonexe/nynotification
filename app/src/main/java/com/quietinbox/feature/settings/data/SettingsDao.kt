package com.quietinbox.feature.settings.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RawQuery
import androidx.sqlite.db.SupportSQLiteQuery
import com.quietinbox.data.db.entity.DailyStatEntity
import com.quietinbox.data.db.entity.NotificationEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Settings, Diagnostics, Storage Retention and Export operations.
 */
@Dao
interface SettingsDao {

    @Query("SELECT COUNT(*) FROM notifications")
    fun observeNotificationCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM notifications")
    suspend fun getNotificationCount(): Int

    @Query("SELECT COUNT(*) FROM notifications WHERE isStarred = 1")
    fun observeStarredCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM notifications WHERE isStarred = 1")
    suspend fun getStarredCount(): Int

    @Query("SELECT COUNT(*) FROM notifications WHERE isStarred = 0")
    suspend fun getUnstarredCount(): Int

    @Query("SELECT MIN(firstSeenAt) FROM notifications")
    fun observeOldestTimestamp(): Flow<Long?>

    @Query("SELECT MAX(firstSeenAt) FROM notifications")
    fun observeNewestTimestamp(): Flow<Long?>

    @Query("SELECT COUNT(DISTINCT packageName) FROM notifications")
    fun observeDistinctAppsCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM firewall_decisions WHERE action = 'SILENCE'")
    fun observeSilencedCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM firewall_decisions WHERE action = 'ALLOW'")
    fun observeAllowedCount(): Flow<Int>

    @Query("DELETE FROM notifications WHERE isStarred = 0 AND firstSeenAt < :cutoffEpochMs")
    suspend fun deleteUnstarredOlderThan(cutoffEpochMs: Long): Int

    @Query("DELETE FROM notifications WHERE id IN (SELECT id FROM notifications WHERE isStarred = 0 ORDER BY firstSeenAt ASC LIMIT :limit)")
    suspend fun deleteOldestUnstarred(limit: Int): Int

    @Query("DELETE FROM notifications")
    suspend fun deleteAllNotifications(): Int

    @Query("DELETE FROM firewall_decisions")
    suspend fun deleteAllFirewallDecisions(): Int

    @Query("DELETE FROM daily_stats")
    suspend fun deleteAllDailyStats(): Int

    @Query("SELECT * FROM notifications ORDER BY firstSeenAt ASC LIMIT :limit OFFSET :offset")
    suspend fun getNotificationsChunk(limit: Int, offset: Int): List<NotificationEntity>

    @Query("SELECT * FROM notifications ORDER BY firstSeenAt ASC")
    suspend fun getAllNotificationsForExport(): List<NotificationEntity>

    @Query("SELECT DISTINCT (CAST(strftime('%Y%m%d', firstSeenAt / 1000, 'unixepoch', 'localtime') AS INTEGER)) FROM notifications")
    suspend fun getDistinctNotificationDays(): List<Int>

    @Query("SELECT COUNT(*) FROM notifications WHERE firstSeenAt BETWEEN :startEpochMs AND :endEpochMs")
    suspend fun getCapturedCountInRange(startEpochMs: Long, endEpochMs: Long): Int

    @Query("SELECT COUNT(DISTINCT packageName) FROM notifications WHERE firstSeenAt BETWEEN :startEpochMs AND :endEpochMs")
    suspend fun getDistinctAppsInRange(startEpochMs: Long, endEpochMs: Long): Int

    @Query("SELECT COUNT(*) FROM firewall_decisions WHERE action = 'SILENCE' AND at BETWEEN :startEpochMs AND :endEpochMs")
    suspend fun getSilencedCountInRange(startEpochMs: Long, endEpochMs: Long): Int

    @Query("SELECT COUNT(*) FROM firewall_decisions WHERE action = 'ALLOW' AND at BETWEEN :startEpochMs AND :endEpochMs")
    suspend fun getAllowedCountInRange(startEpochMs: Long, endEpochMs: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateDailyStat(stat: DailyStatEntity)

    @Query("DELETE FROM daily_stats WHERE day NOT IN (:days)")
    suspend fun deleteDailyStatsNotIn(days: List<Int>)

    @RawQuery
    suspend fun executeRaw(query: SupportSQLiteQuery): Int
}
