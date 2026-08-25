package com.quietinbox.core.signal

import androidx.paging.PagingSource
import com.quietinbox.core.model.CapturedNotification
import com.quietinbox.core.time.Clock
import com.quietinbox.data.db.dao.NotificationDao
import com.quietinbox.data.db.dao.PackageNotificationCount
import com.quietinbox.data.db.entity.NotificationEntity

/**
 * Mutable clock for deterministic time simulation in unit tests.
 */
class TestClock(var currentEpochMs: Long = 1_700_000_000_000L) : Clock {
    override fun now(): Long = currentEpochMs

    override fun elapsedRealtime(): Long = currentEpochMs

    fun nowEpochMs(): Long = currentEpochMs

    fun advance(durationMs: Long) {
        currentEpochMs += durationMs
    }

    fun set(epochMs: Long) {
        currentEpochMs = epochMs
    }
}

/**
 * In-memory test fake for [NotificationDao] supporting session queries,
 * group queries, and hourly rate counting.
 */
class FakeNotificationDao : NotificationDao {
    val rows = mutableListOf<NotificationEntity>()
    private var nextId = 1L

    override suspend fun findByKey(sbnKey: String): NotificationEntity? {
        return rows.firstOrNull { it.sbnKey == sbnKey }
    }

    override suspend fun latestForKey(sbnKey: String): NotificationEntity? {
        return rows.filter { it.sbnKey == sbnKey || (it.groupKey != null && it.groupKey == sbnKey) }.maxByOrNull { it.firstSeenAt }
    }

    override suspend fun findById(id: Long): NotificationEntity? {
        return rows.firstOrNull { it.id == id }
    }

    override suspend fun insert(notification: NotificationEntity): Long {
        val id = if (notification.id == 0L) nextId++ else notification.id
        val row = notification.copy(id = id)
        rows.add(row)
        return id
    }

    override suspend fun updateSession(
        rowId: Long,
        lastSeenAt: Long,
        title: String?,
        body: String?,
        updateCount: Int
    ) {
        val index = rows.indexOfFirst { it.id == rowId }
        if (index != -1) {
            val old = rows[index]
            rows[index] = old.copy(
                lastSeenAt = lastSeenAt,
                title = title,
                body = body,
                updateCount = updateCount
            )
        }
    }

    override suspend fun markEnded(sbnKey: String, endedAt: Long, reason: Int?) {
        rows.indices.filter { rows[it].sbnKey == sbnKey && rows[it].endedAt == null }.forEach { i ->
            rows[i] = rows[i].copy(endedAt = endedAt, removalReason = reason)
        }
    }

    override fun pagingSource(): PagingSource<Int, NotificationEntity> {
        throw UnsupportedOperationException("Not needed in signal engine tests")
    }

    override fun pagingSourceFiltered(
        isSeen: Boolean?,
        isStarred: Boolean?,
        packageName: String?
    ): PagingSource<Int, NotificationEntity> {
        throw UnsupportedOperationException("Not needed in signal engine tests")
    }

    override fun searchPaging(query: String, digits: String?): PagingSource<Int, NotificationEntity> {
        throw UnsupportedOperationException("Not needed in signal engine tests")
    }

    override suspend fun setSeen(id: Long, seen: Boolean) {
        val index = rows.indexOfFirst { it.id == id }
        if (index != -1) {
            rows[index] = rows[index].copy(isSeen = seen)
        }
    }

    override suspend fun setSeenByIds(ids: List<Long>, seen: Boolean) {
        rows.indices.filter { rows[it].id in ids }.forEach { i ->
            rows[i] = rows[i].copy(isSeen = seen)
        }
    }

    override suspend fun setStarred(id: Long, starred: Boolean) {
        val index = rows.indexOfFirst { it.id == id }
        if (index != -1) {
            rows[index] = rows[index].copy(isStarred = starred)
        }
    }

    override suspend fun deleteByIds(ids: List<Long>) {
        rows.removeAll { it.id in ids }
    }

    override suspend fun purgeOlderThan(cutoff: Long): Int {
        val count = rows.count { it.firstSeenAt < cutoff && !it.isStarred }
        rows.removeAll { it.firstSeenAt < cutoff && !it.isStarred }
        return count
    }

    override suspend fun count(): Int = rows.size

    override suspend fun countsSince(from: Long): Int {
        return rows.count { it.firstSeenAt >= from }
    }

    override suspend fun countByPackage(packageName: String): Int {
        return rows.count { it.packageName == packageName }
    }

    override suspend fun getPackageCounts(): List<PackageNotificationCount> {
        return rows.groupBy { it.packageName }.map { (pkg, list) ->
            PackageNotificationCount(pkg, list.size)
        }
    }

    override suspend fun purgeOldestUnstarred(count: Int): Int {
        val unstarred = rows.filter { !it.isStarred }.sortedBy { it.firstSeenAt }.take(count)
        rows.removeAll(unstarred)
        return unstarred.size
    }

    fun applyDecision(decision: IngestDecision, n: CapturedNotification, hash: String, now: Long) {
        when (decision) {
            is IngestDecision.Insert -> {
                val id = nextId++
                rows.add(
                    NotificationEntity(
                        id = id,
                        sbnKey = n.sbnKey,
                        packageName = n.packageName,
                        appLabel = n.appLabel,
                        title = n.title,
                        body = n.body,
                        subText = n.subText,
                        senderName = n.senderName,
                        senderDigits = n.senderDigits,
                        channelId = n.channelId,
                        androidCategory = n.androidCategory,
                        importance = n.importance,
                        signalClass = decision.signalClass.name,
                        contentHash = hash,
                        firstSeenAt = now,
                        lastSeenAt = now,
                        endedAt = null,
                        updateCount = 1,
                        wasRateLimited = false,
                        isSeen = false,
                        isStarred = false,
                        removalReason = null
                    )
                )
            }
            is IngestDecision.UpdateExisting -> {
                val index = rows.indexOfFirst { it.id == decision.rowId }
                if (index != -1) {
                    val old = rows[index]
                    rows[index] = old.copy(
                        lastSeenAt = now,
                        title = n.title,
                        body = n.body,
                        updateCount = if (decision.bumpCount) old.updateCount + 1 else old.updateCount
                    )
                }
            }
            is IngestDecision.Drop -> {
                // Drop: no DB change
            }
        }
    }
}

/**
 * Creates a test [CapturedNotification] instance with sensible defaults.
 */
fun createTestNotification(
    sbnKey: String = "com.example.app|1|tag|100",
    packageName: String = "com.example.app",
    appLabel: String = "Example App",
    title: String? = "Notification Title",
    body: String? = "Notification Body",
    subText: String? = null,
    senderName: String? = null,
    senderDigits: String? = null,
    channelId: String? = "default_channel",
    androidCategory: String? = null,
    importance: Int = 3,
    postedAt: Long = 1_700_000_000_000L,
    isOngoing: Boolean = false,
    isForegroundService: Boolean = false,
    isGroupSummary: Boolean = false,
    isClearable: Boolean = true,
    hasProgress: Boolean = false,
    groupKey: String? = null,
): CapturedNotification {
    return CapturedNotification(
        sbnKey = sbnKey,
        packageName = packageName,
        appLabel = appLabel,
        title = title,
        body = body,
        subText = subText,
        senderName = senderName,
        senderDigits = senderDigits,
        channelId = channelId,
        androidCategory = androidCategory,
        importance = importance,
        postedAt = postedAt,
        isOngoing = isOngoing,
        isForegroundService = isForegroundService,
        isGroupSummary = isGroupSummary,
        isClearable = isClearable,
        hasProgress = hasProgress,
        groupKey = groupKey,
    )
}
