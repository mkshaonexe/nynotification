package com.quietinbox.feature.home.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.quietinbox.data.db.entity.DailyStatEntity
import com.quietinbox.data.db.entity.MutedAppEntity
import kotlinx.coroutines.flow.Flow

data class AppNotificationCount(
    val packageName: String,
    val appLabel: String,
    val count: Int
)

data class HourCount(
    val hour: Int,
    val count: Int
)

data class DaySilencedCount(
    val day: Int,
    val count: Int
)

/**
 * Data Access Object for the Home & Statistics dashboard.
 */
@Dao
interface HomeStatsDao {

    @Query("SELECT COUNT(*) FROM notifications WHERE (:fromEpochMs IS NULL OR firstSeenAt >= :fromEpochMs)")
    fun getCapturedCount(fromEpochMs: Long?): Flow<Int>

    @Query("SELECT COUNT(*) FROM firewall_decisions WHERE action = 'SILENCE' AND (:fromEpochMs IS NULL OR at >= :fromEpochMs)")
    fun getSilencedCount(fromEpochMs: Long?): Flow<Int>

    @Query("SELECT COUNT(*) FROM firewall_decisions WHERE action = 'ALLOW' AND (:fromEpochMs IS NULL OR at >= :fromEpochMs)")
    fun getAllowedCount(fromEpochMs: Long?): Flow<Int>

    @Query("SELECT COUNT(DISTINCT packageName) FROM notifications WHERE (:fromEpochMs IS NULL OR firstSeenAt >= :fromEpochMs)")
    fun getDistinctAppsCount(fromEpochMs: Long?): Flow<Int>

    @Query("SELECT COUNT(*) FROM daily_stats WHERE quietMinutes >= 240 AND (:fromDayInt IS NULL OR day >= :fromDayInt)")
    fun getQuietDaysCount(fromDayInt: Int?): Flow<Int>

    @Query("SELECT * FROM daily_stats ORDER BY day ASC")
    fun getAllDailyStats(): Flow<List<DailyStatEntity>>

    @Query("""
        SELECT CAST(strftime('%H', firstSeenAt / 1000, 'unixepoch', 'localtime') AS INTEGER) as hour, COUNT(*) as count 
        FROM notifications 
        WHERE (:fromEpochMs IS NULL OR firstSeenAt >= :fromEpochMs)
        GROUP BY hour 
        ORDER BY count DESC 
        LIMIT 1
    """)
    fun getBusiestHour(fromEpochMs: Long?): Flow<HourCount?>

    @Query("""
        SELECT packageName, appLabel, COUNT(*) as count 
        FROM notifications 
        WHERE (:fromEpochMs IS NULL OR firstSeenAt >= :fromEpochMs)
        GROUP BY packageName 
        ORDER BY count DESC 
        LIMIT :limit
    """)
    fun getTopAppsByCount(fromEpochMs: Long?, limit: Int): Flow<List<AppNotificationCount>>

    @Query("SELECT packageName FROM muted_apps")
    fun getMutedPackages(): Flow<List<String>>

    @Query("""
        SELECT CAST(strftime('%Y%m%d', at / 1000, 'unixepoch', 'localtime') AS INTEGER) as day, COUNT(*) as count 
        FROM firewall_decisions 
        WHERE action = 'SILENCE' AND (:fromEpochMs IS NULL OR at >= :fromEpochMs)
        GROUP BY day
    """)
    fun getDailySilencedFromFirewall(fromEpochMs: Long?): Flow<List<DaySilencedCount>>

    @Query("DELETE FROM muted_apps WHERE packageName = :packageName")
    suspend fun unmuteApp(packageName: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun muteApp(entity: MutedAppEntity)
}
