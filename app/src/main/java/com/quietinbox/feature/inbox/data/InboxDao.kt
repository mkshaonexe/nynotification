package com.quietinbox.feature.inbox.data

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.quietinbox.data.db.entity.AllowRuleEntity
import com.quietinbox.data.db.entity.MutedAppEntity
import com.quietinbox.data.db.entity.NotificationEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Inbox operations, paging, filtering, and row interactions.
 */
@Dao
interface InboxDao {

    @Query("SELECT COUNT(*) FROM notifications")
    fun observeNotificationCount(): Flow<Int>

    @Query("""
        SELECT 
            n.id, n.sbnKey, n.packageName, n.appLabel, n.title, n.body, n.subText,
            n.senderName, n.senderDigits, n.channelId, n.androidCategory, n.importance,
            n.signalClass, n.contentHash, n.firstSeenAt, n.lastSeenAt, n.endedAt,
            n.updateCount, n.wasRateLimited, n.isSeen, n.isStarred, n.removalReason,
            fd.action AS firewallAction, fd.ruleId AS firewallRuleId, fd.ruleLabel AS firewallRuleLabel
        FROM notifications n
        LEFT JOIN firewall_decisions fd ON n.sbnKey = fd.sbnKey
        WHERE (:stateFilter = 'ALL' 
               OR (:stateFilter = 'UNSEEN' AND n.isSeen = 0)
               OR (:stateFilter = 'SEEN' AND n.isSeen = 1)
               OR (:stateFilter = 'STARRED' AND n.isStarred = 1))
          AND (:packageName IS NULL OR n.packageName = :packageName)
          AND (:startEpochMs IS NULL OR n.firstSeenAt >= :startEpochMs)
          AND (:endEpochMs IS NULL OR n.firstSeenAt <= :endEpochMs)
          AND (:includeTransport = 1 OR n.signalClass != 'TRANSPORT')
          AND (:query IS NULL OR n.id IN (SELECT rowid FROM notifications_fts WHERE notifications_fts MATCH :query) OR (:digitsQuery IS NOT NULL AND n.senderDigits LIKE '%' || :digitsQuery || '%'))
        ORDER BY n.firstSeenAt DESC
    """)
    fun pagingSource(
        stateFilter: String,
        packageName: String?,
        startEpochMs: Long?,
        endEpochMs: Long?,
        includeTransport: Boolean,
        query: String?,
        digitsQuery: String?
    ): PagingSource<Int, NotificationWithVerdict>

    @Query("UPDATE notifications SET isStarred = :isStarred WHERE id = :id")
    suspend fun setStarred(id: Long, isStarred: Boolean)

    @Query("UPDATE notifications SET isSeen = :isSeen WHERE id = :id")
    suspend fun setSeen(id: Long, isSeen: Boolean)

    @Query("UPDATE notifications SET isSeen = 1")
    suspend fun markAllSeen()

    @Query("UPDATE notifications SET isSeen = :isSeen WHERE id IN (:ids)")
    suspend fun setSeenByIds(ids: List<Long>, isSeen: Boolean)

    @Query("UPDATE notifications SET isStarred = :isStarred WHERE id IN (:ids)")
    suspend fun setStarredByIds(ids: List<Long>, isStarred: Boolean)

    @Query("SELECT * FROM notifications WHERE id = :id LIMIT 1")
    suspend fun getNotificationById(id: Long): NotificationEntity?

    @Query("DELETE FROM notifications WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM notifications WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotifications(notifications: List<NotificationEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun muteApp(mutedApp: MutedAppEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addAllowRule(rule: AllowRuleEntity)
}
