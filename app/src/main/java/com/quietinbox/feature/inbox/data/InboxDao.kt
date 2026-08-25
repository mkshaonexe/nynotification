package com.quietinbox.feature.inbox.data

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.quietinbox.data.db.entity.AllowRuleEntity
import com.quietinbox.data.db.entity.FirewallDecisionEntity
import com.quietinbox.data.db.entity.MutedAppEntity
import com.quietinbox.data.db.entity.NotificationEntity
import kotlinx.coroutines.flow.Flow

/**
 * Composite projection of a notification row with its latest firewall decision.
 */
data class NotificationWithVerdict(
    val id: Long,
    val sbnKey: String,
    val packageName: String,
    val appLabel: String,
    val title: String?,
    val body: String?,
    val subText: String?,
    val senderName: String?,
    val senderDigits: String?,
    val channelId: String?,
    val androidCategory: String?,
    val importance: Int?,
    val signalClass: String,
    val contentHash: String,
    val firstSeenAt: Long,
    val lastSeenAt: Long,
    val endedAt: Long?,
    val updateCount: Int,
    val wasRateLimited: Boolean,
    val isSeen: Boolean,
    val isStarred: Boolean,
    val removalReason: Int?,
    val firewallAction: String?,
    val firewallRuleId: Int?,
    val firewallRuleLabel: String?
)

/**
 * Inbox Data Access Object providing filtered and paged queries for the notification inbox.
 */
@Dao
interface InboxDao {

    /**
     * Unified PagingSource query supporting state filter, app filter, day window filter,
     * transport inclusion toggle, and text / senderDigits search.
     */
    @Query("""
        SELECT n.id, n.sbnKey, n.packageName, n.appLabel, n.title, n.body, n.subText,
               n.senderName, n.senderDigits, n.channelId, n.androidCategory, n.importance,
               n.signalClass, n.contentHash, n.firstSeenAt, n.lastSeenAt, n.endedAt,
               n.updateCount, n.wasRateLimited, n.isSeen, n.isStarred, n.removalReason,
               f.action AS firewallAction, f.ruleId AS firewallRuleId, f.ruleLabel AS firewallRuleLabel
        FROM notifications n
        LEFT JOIN (
            SELECT sbnKey, action, ruleId, ruleLabel, MAX(at) as maxAt
            FROM firewall_decisions
            GROUP BY sbnKey
        ) f ON n.sbnKey = f.sbnKey
        WHERE (:includeTransport = 1 OR n.signalClass != 'TRANSPORT')
          AND (
              :stateFilter = 'ALL'
              OR (:stateFilter = 'UNSEEN' AND n.isSeen = 0)
              OR (:stateFilter = 'SEEN' AND n.isSeen = 1)
              OR (:stateFilter = 'STARRED' AND n.isStarred = 1)
          )
          AND (:packageName IS NULL OR n.packageName = :packageName)
          AND (:startEpochMs IS NULL OR (n.firstSeenAt >= :startEpochMs AND n.firstSeenAt <= :endEpochMs))
          AND (
              :query IS NULL OR :query = ''
              OR n.title LIKE '%' || :query || '%'
              OR n.body LIKE '%' || :query || '%'
              OR n.appLabel LIKE '%' || :query || '%'
              OR n.senderName LIKE '%' || :query || '%'
              OR (:digitsQuery IS NOT NULL AND n.senderDigits LIKE '%' || :digitsQuery || '%')
          )
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

    @Query("SELECT * FROM notifications WHERE id = :id LIMIT 1")
    suspend fun getNotificationById(id: Long): NotificationEntity?

    @Query("SELECT * FROM notifications WHERE id = :id LIMIT 1")
    fun observeNotificationById(id: Long): Flow<NotificationEntity?>

    @Query("""
        SELECT * FROM firewall_decisions 
        WHERE sbnKey = :sbnKey 
        ORDER BY at DESC, id DESC 
        LIMIT 1
    """)
    suspend fun getLatestFirewallDecision(sbnKey: String): FirewallDecisionEntity?

    @Query("SELECT COUNT(*) FROM notifications")
    fun observeNotificationCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM notifications")
    suspend fun getNotificationCount(): Int

    @Query("UPDATE notifications SET isSeen = :isSeen WHERE id = :id")
    suspend fun setSeen(id: Long, isSeen: Boolean)

    @Query("UPDATE notifications SET isSeen = :isSeen WHERE id IN (:ids)")
    suspend fun setSeenByIds(ids: List<Long>, isSeen: Boolean)

    @Query("UPDATE notifications SET isSeen = 1 WHERE isSeen = 0")
    suspend fun markAllSeen()

    @Query("UPDATE notifications SET isStarred = :isStarred WHERE id = :id")
    suspend fun setStarred(id: Long, isStarred: Boolean)

    @Query("UPDATE notifications SET isStarred = :isStarred WHERE id IN (:ids)")
    suspend fun setStarredByIds(ids: List<Long>, isStarred: Boolean)

    @Query("DELETE FROM notifications WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM notifications WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotifications(notifications: List<NotificationEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: NotificationEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun muteApp(entity: MutedAppEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addAllowRule(rule: AllowRuleEntity)

    @Query("SELECT DISTINCT packageName FROM notifications ORDER BY appLabel ASC")
    fun observeDistinctApps(): Flow<List<String>>
}
