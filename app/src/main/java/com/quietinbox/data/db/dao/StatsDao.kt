package com.quietinbox.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.quietinbox.data.db.entity.DailyStatEntity
import com.quietinbox.data.db.entity.FirewallDecisionEntity
import kotlinx.coroutines.flow.Flow

data class TopAppCapture(
    val packageName: String,
    val count: Int
)

@Dao
interface StatsDao {

    @Query("SELECT * FROM daily_stats WHERE day = :day LIMIT 1")
    suspend fun getStats(day: Int): DailyStatEntity?

    @Query("SELECT * FROM daily_stats WHERE day BETWEEN :fromDay AND :toDay ORDER BY day ASC")
    fun getRange(fromDay: Int, toDay: Int): Flow<List<DailyStatEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplace(stat: DailyStatEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplaceAll(stats: List<DailyStatEntity>)

    // Decision logging
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun logDecision(decision: FirewallDecisionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun logDecisions(decisions: List<FirewallDecisionEntity>)

    @Query("SELECT COUNT(*) FROM firewall_decisions WHERE action = 'SILENCE' AND at >= :sinceEpochMs")
    suspend fun getSilencedCountSince(sinceEpochMs: Long): Int

    @Query("SELECT COUNT(*) FROM firewall_decisions WHERE action = 'ALLOW' AND at >= :sinceEpochMs")
    suspend fun getAllowedCountSince(sinceEpochMs: Long): Int

    @Query("""
        SELECT packageName, COUNT(*) as count 
        FROM notifications 
        WHERE firstSeenAt >= :sinceEpochMs 
        GROUP BY packageName 
        ORDER BY count DESC 
        LIMIT :limit
    """)
    suspend fun getTopNotifyingApps(sinceEpochMs: Long, limit: Int = 5): List<TopAppCapture>
}
