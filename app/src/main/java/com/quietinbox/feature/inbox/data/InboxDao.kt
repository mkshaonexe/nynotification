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
        SELECT * FROM notifications
        WHERE (:stateFilter = 'ALL' 
               OR (:stateFilter = 'UNSEEN' AND isSeen = 0)
               OR (:stateFilter = 'SEEN' AND isSeen = 1)
               OR (:stateFilter = 'STARRED' AND isStarred = 1))
          AND (:packageName IS NULL OR packageName = :packageName)
          AND (:startEpochMs IS NULL OR firstSeenAt >= :startEpochMs)
          AND (:endEpochMs IS NULL OR firstSeenAt <= :endEpochMs)
          AND (:includeTransport = 1 OR signalClass != 'TRANSPORT')
          AND (:query IS NULL OR id IN (SELECT rowid FROM notifications_fts WHERE notifications_fts MATCH :query) OR (:digitsQuery IS NOT NULL AND senderDigits LIKE '%' || :digitsQuery || '%'))
        ORDER BY firstSeenAt DESC
    """)
    fun pagingSource(
        stateFilter: String,
        packageName: String?,
        startEpochMs: Long?,
        endEpochMs: Long?,
        includeTransport: Boolean,
        query: String?,
        digitsQuery: String?
    ): PagingSource<Int, NotificationEntity>

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
