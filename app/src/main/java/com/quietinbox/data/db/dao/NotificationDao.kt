package com.quietinbox.data.db.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.quietinbox.data.db.entity.NotificationEntity
import kotlinx.coroutines.flow.Flow

data class PackageNotificationCount(
    val packageName: String,
    val count: Int
)

@Dao
interface NotificationDao {

    @Query("SELECT * FROM notifications WHERE sbnKey = :sbnKey LIMIT 1")
    suspend fun findByKey(sbnKey: String): NotificationEntity?

    @Query("SELECT * FROM notifications WHERE sbnKey = :sbnKey ORDER BY firstSeenAt DESC LIMIT 1")
    suspend fun latestForKey(sbnKey: String): NotificationEntity?

    @Query("SELECT * FROM notifications WHERE id = :id LIMIT 1")
    suspend fun findById(id: Long): NotificationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(notification: NotificationEntity): Long

    @Query("UPDATE notifications SET lastSeenAt = :lastSeenAt, title = :title, body = :body, updateCount = :updateCount WHERE id = :rowId")
    suspend fun updateSession(rowId: Long, lastSeenAt: Long, title: String?, body: String?, updateCount: Int)

    @Query("UPDATE notifications SET endedAt = :endedAt, removalReason = :reason WHERE sbnKey = :sbnKey AND endedAt IS NULL")
    suspend fun markEnded(sbnKey: String, endedAt: Long, reason: Int?)

    @Query("SELECT * FROM notifications ORDER BY firstSeenAt DESC")
    fun pagingSource(): PagingSource<Int, NotificationEntity>

    @Query("""
        SELECT * FROM notifications 
        WHERE (:isSeen IS NULL OR isSeen = :isSeen)
          AND (:isStarred IS NULL OR isStarred = :isStarred)
          AND (:packageName IS NULL OR packageName = :packageName)
        ORDER BY firstSeenAt DESC
    """)
    fun pagingSourceFiltered(
        isSeen: Boolean? = null,
        isStarred: Boolean? = null,
        packageName: String? = null
    ): PagingSource<Int, NotificationEntity>

    @Query("""
        SELECT n.* FROM notifications n
        JOIN notifications_fts fts ON n.id = fts.rowid
        WHERE notifications_fts MATCH :query
           OR (:digits IS NOT NULL AND n.senderDigits LIKE '%' || :digits || '%')
        ORDER BY n.firstSeenAt DESC
    """)
    fun searchPaging(query: String, digits: String?): PagingSource<Int, NotificationEntity>

    @Query("UPDATE notifications SET isSeen = :seen WHERE id = :id")
    suspend fun setSeen(id: Long, seen: Boolean = true)

    @Query("UPDATE notifications SET isSeen = :seen WHERE id IN (:ids)")
    suspend fun setSeenByIds(ids: List<Long>, seen: Boolean = true)

    @Query("UPDATE notifications SET isStarred = :starred WHERE id = :id")
    suspend fun setStarred(id: Long, starred: Boolean)

    @Query("DELETE FROM notifications WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)

    @Query("DELETE FROM notifications WHERE firstSeenAt < :cutoff AND isStarred = 0")
    suspend fun purgeOlderThan(cutoff: Long): Int

    @Query("SELECT COUNT(*) FROM notifications")
    suspend fun count(): Int

    @Query("SELECT COUNT(*) FROM notifications WHERE firstSeenAt >= :from")
    suspend fun countsSince(from: Long): Int

    @Query("SELECT COUNT(*) FROM notifications WHERE packageName = :packageName")
    suspend fun countByPackage(packageName: String): Int

    @Query("SELECT packageName, COUNT(*) as count FROM notifications GROUP BY packageName")
    suspend fun getPackageCounts(): List<PackageNotificationCount>

    @Query("""
        DELETE FROM notifications 
        WHERE id IN (
            SELECT id FROM notifications 
            WHERE isStarred = 0 
            ORDER BY firstSeenAt ASC 
            LIMIT :count
        )
    """)
    suspend fun purgeOldestUnstarred(count: Int): Int
}
