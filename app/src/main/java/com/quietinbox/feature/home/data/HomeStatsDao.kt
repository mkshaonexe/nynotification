package com.quietinbox.feature.home.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.quietinbox.data.db.entity.DailyStatEntity
import com.quietinbox.data.db.entity.MutedAppEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data class representing the busiest hour and its notification count.
 */
data class HourCount(
    val hour: Int,
    val count: Int
)

/**
 * Data class representing an app's notification count in a given range.
 */
data class AppNotificationCount(
    val packageName: String,
    val appLabel: String,
    val count: Int
)

/**
 * Data class representing a single day's count of silenced notifications.
 */
data class DailySilencedCount(
    val day: Int,
    val count: Int
)

/**
 * DAO interface for Home screen statistical queries and aggregations.
 * Owned by Phase 5.
 */
@Dao
interface HomeStatsDao {

    /**
     * Returns a reactive flow of total notifications captured since [fromEpochMs],
     * or all notifications if [fromEpochMs] is null.
     */
    @Query("SELECT COUNT(*) FROM notifications WHERE (:fromEpochMs IS NULL OR firstSeenAt >= :fromEpochMs)")
    fun getCapturedCount(fromEpochMs: Long?): Flow<Int>

    /**
     * Returns a reactive flow of total notifications silenced since [fromEpochMs],
     * or all silenced notifications if [fromEpochMs] is null.
     */
    @Query("SELECT COUNT(*) FROM firewall_decisions WHERE action = 'SILENCE' AND (:fromEpochMs IS NULL OR at >= :fromEpochMs)")
    fun getSilencedCount(fromEpochMs: Long?): Flow<Int>

    /**
     * Returns a reactive flow of total notifications allowed since [fromEpochMs],
     * or all allowed notifications if [fromEpochMs] is null.
     */
    @Query("SELECT COUNT(*) FROM firewall_decisions WHERE action = 'ALLOW' AND (:fromEpochMs IS NULL OR at >= :fromEpochMs)")
    fun getAllowedCount(fromEpochMs: Long?): Flow<Int>

    /**
     * Returns a reactive flow of the number of distinct apps with notifications since [fromEpochMs],
     * or all distinct apps if [fromEpochMs] is null.
     */
    @Query("SELECT COUNT(DISTINCT packageName) FROM notifications WHERE (:fromEpochMs IS NULL OR firstSeenAt >= :fromEpochMs)")
    fun getDistinctAppsCount(fromEpochMs: Long?): Flow<Int>

    /**
     * Returns a reactive flow of quiet days count (days where quietMinutes >= 240) since [fromDayInt],
     * or all quiet days if [fromDayInt] is null.
     */
    @Query("SELECT COUNT(*) FROM daily_stats WHERE quietMinutes >= 240 AND (:fromDayInt IS NULL OR day >= :fromDayInt)")
    fun getQuietDaysCount(fromDayInt: Int?): Flow<Int>

    /**
     * Returns all daily statistics ordered chronologically by day.
     */
    @Query("SELECT * FROM daily_stats ORDER BY day ASC")
    fun getAllDailyStats(): Flow<List<DailyStatEntity>>

    /**
     * Calculates the hour of the day (0-23) with the highest volume of notification captures
     * since [fromEpochMs].
     */
    @Query("""
        SELECT CAST(strftime('%H', firstSeenAt / 1000, 'unixepoch', 'localtime') AS INTEGER) AS hour,
               COUNT(*) AS count
        FROM notifications
        WHERE (:fromEpochMs IS NULL OR firstSeenAt >= :fromEpochMs)
        GROUP BY hour
        ORDER BY count DESC
        LIMIT 1
    """)
    fun getBusiestHour(fromEpochMs: Long?): Flow<HourCount?>

    /**
     * Returns the top apps by notification volume since [fromEpochMs], limited to [limit].
     */
    @Query("""
        SELECT packageName, appLabel, COUNT(*) AS count
        FROM notifications
        WHERE (:fromEpochMs IS NULL OR firstSeenAt >= :fromEpochMs)
        GROUP BY packageName
        ORDER BY count DESC
        LIMIT :limit
    """)
    fun getTopAppsByCount(fromEpochMs: Long?, limit: Int = 5): Flow<List<AppNotificationCount>>

    /**
     * Returns a reactive flow of all muted app package names.
     */
    @Query("SELECT packageName FROM muted_apps")
    fun getMutedPackages(): Flow<List<String>>

    /**
     * Mutes an app by inserting into muted_apps.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun muteApp(mutedApp: MutedAppEntity)

    /**
     * Unmutes an app by deleting from muted_apps.
     */
    @Query("DELETE FROM muted_apps WHERE packageName = :packageName")
    suspend fun unmuteApp(packageName: String)

    /**
     * Returns the daily silenced counts from the pre-aggregated daily_stats table.
     */
    @Query("""
        SELECT day, silenced AS count
        FROM daily_stats
        WHERE (:fromDayInt IS NULL OR day >= :fromDayInt)
        ORDER BY day ASC
    """)
    fun getDailySilencedFromStats(fromDayInt: Int?): Flow<List<DailySilencedCount>>

    /**
     * Returns the daily silenced counts aggregated dynamically from firewall_decisions.
     */
    @Query("""
        SELECT CAST(strftime('%Y%m%d', at / 1000, 'unixepoch', 'localtime') AS INTEGER) AS day,
               COUNT(*) AS count
        FROM firewall_decisions
        WHERE action = 'SILENCE' AND (:fromEpochMs IS NULL OR at >= :fromEpochMs)
        GROUP BY day
        ORDER BY day ASC
    """)
    fun getDailySilencedFromFirewall(fromEpochMs: Long?): Flow<List<DailySilencedCount>>
}
