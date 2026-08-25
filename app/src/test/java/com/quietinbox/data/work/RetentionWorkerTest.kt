package com.quietinbox.data.work

import com.quietinbox.core.time.Clock
import com.quietinbox.data.db.entity.DailyStatEntity
import com.quietinbox.data.db.entity.NotificationEntity
import com.quietinbox.feature.settings.data.SettingsDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RetentionWorkerTest {

    private class FakeClock(var timeMs: Long = 1700000000000L) : Clock {
        override fun now(): Long = timeMs
        override fun elapsedRealtime(): Long = timeMs
    }

    private class InMemorySettingsDao : SettingsDao {
        val notifications = mutableListOf<NotificationEntity>()
        val dailyStats = mutableListOf<DailyStatEntity>()
        var vacuumCount = 0

        override fun observeNotificationCount(): Flow<Int> = flowOf(notifications.size)
        override suspend fun getNotificationCount(): Int = notifications.size
        override fun observeStarredCount(): Flow<Int> = flowOf(notifications.count { it.isStarred })
        override suspend fun getStarredCount(): Int = notifications.count { it.isStarred }
        override suspend fun getUnstarredCount(): Int = notifications.count { !it.isStarred }
        override fun observeOldestTimestamp(): Flow<Long?> = flowOf(notifications.minOfOrNull { it.firstSeenAt })
        override fun observeNewestTimestamp(): Flow<Long?> = flowOf(notifications.maxOfOrNull { it.firstSeenAt })
        override fun observeDistinctAppsCount(): Flow<Int> = flowOf(notifications.map { it.packageName }.distinct().size)
        override fun observeSilencedCount(): Flow<Int> = flowOf(0)
        override fun observeAllowedCount(): Flow<Int> = flowOf(0)

        override suspend fun deleteUnstarredOlderThan(cutoffEpochMs: Long): Int {
            val before = notifications.size
            notifications.removeAll { it.firstSeenAt < cutoffEpochMs && !it.isStarred }
            return before - notifications.size
        }

        override suspend fun deleteOldestUnstarred(limit: Int): Int {
            val unstarred = notifications.filter { !it.isStarred }.sortedBy { it.firstSeenAt }.take(limit)
            notifications.removeAll(unstarred)
            return unstarred.size
        }

        override suspend fun deleteAllNotifications(): Int {
            val size = notifications.size
            notifications.clear()
            return size
        }

        override suspend fun deleteAllFirewallDecisions(): Int = 0

        override suspend fun deleteAllDailyStats(): Int {
            val size = dailyStats.size
            dailyStats.clear()
            return size
        }

        override suspend fun getNotificationsChunk(limit: Int, offset: Int): List<NotificationEntity> {
            if (offset >= notifications.size) return emptyList()
            return notifications.subList(offset, (offset + limit).coerceAtMost(notifications.size))
        }

        override suspend fun getAllNotificationsForExport(): List<NotificationEntity> = notifications.toList()

        override suspend fun getDistinctNotificationDays(): List<Int> {
            return listOf(20260826)
        }

        override suspend fun getCapturedCountInRange(startEpochMs: Long, endEpochMs: Long): Int {
            return notifications.count { it.firstSeenAt in startEpochMs..endEpochMs }
        }

        override suspend fun getDistinctAppsInRange(startEpochMs: Long, endEpochMs: Long): Int {
            return notifications.filter { it.firstSeenAt in startEpochMs..endEpochMs }.map { it.packageName }.distinct().size
        }

        override suspend fun getSilencedCountInRange(startEpochMs: Long, endEpochMs: Long): Int = 0
        override suspend fun getAllowedCountInRange(startEpochMs: Long, endEpochMs: Long): Int = 0

        override suspend fun insertOrUpdateDailyStat(stat: DailyStatEntity) {
            dailyStats.removeAll { it.day == stat.day }
            dailyStats.add(stat)
        }

        override suspend fun deleteDailyStatsNotIn(days: List<Int>) {
            dailyStats.removeAll { it.day !in days }
        }

        override suspend fun executeRaw(query: androidx.sqlite.db.SupportSQLiteQuery): Int {
            vacuumCount++
            return 0
        }
    }

    private lateinit var fakeDao: InMemorySettingsDao
    private lateinit var fakeClock: FakeClock

    @Before
    fun setup() {
        fakeDao = InMemorySettingsDao()
        fakeClock = FakeClock(timeMs = 1700000000000L) // Reference now
    }

    private fun createNotification(id: Long, ageDays: Long, isStarred: Boolean): NotificationEntity {
        val firstSeenAt = fakeClock.now() - (ageDays * 24L * 60 * 60 * 1000)
        return NotificationEntity(
            id = id,
            sbnKey = "key_$id",
            packageName = "com.test.app",
            appLabel = "Test App",
            title = "Title $id",
            body = "Body $id",
            subText = null,
            senderName = null,
            senderDigits = null,
            channelId = null,
            androidCategory = null,
            importance = null,
            signalClass = "ALERT",
            contentHash = "hash_$id",
            firstSeenAt = firstSeenAt,
            lastSeenAt = firstSeenAt,
            endedAt = null,
            updateCount = 1,
            wasRateLimited = false,
            isSeen = true,
            isStarred = isStarred,
            removalReason = null
        )
    }

    @Test
    fun testStarredNotificationsSurviveRetentionPurge() = runBlocking {
        // Add a 100-day old unstarred notification (should be purged under 90-day retention)
        fakeDao.notifications.add(createNotification(id = 1, ageDays = 100, isStarred = false))

        // Add a 100-day old starred notification (must survive)
        fakeDao.notifications.add(createNotification(id = 2, ageDays = 100, isStarred = true))

        // Add a 10-day old unstarred notification (must survive)
        fakeDao.notifications.add(createNotification(id = 3, ageDays = 10, isStarred = false))

        val cutoff = fakeClock.now() - (90L * 24 * 60 * 60 * 1000)
        val deleted = fakeDao.deleteUnstarredOlderThan(cutoff)

        assertEquals(1, deleted)
        assertEquals(2, fakeDao.notifications.size)
        assertTrue("Starred notification must survive purge", fakeDao.notifications.any { it.id == 2L && it.isStarred })
        assertTrue("Recent unstarred notification must survive purge", fakeDao.notifications.any { it.id == 3L })
    }

    @Test
    fun testCapEnforcementDropsOldestUnstarredOnly() = runBlocking {
        // Add 5 notifications: 2 starred (older and newer), 3 unstarred (oldest, middle, newest)
        fakeDao.notifications.add(createNotification(id = 1, ageDays = 50, isStarred = true))  // Starred old
        fakeDao.notifications.add(createNotification(id = 2, ageDays = 40, isStarred = false)) // Unstarred oldest
        fakeDao.notifications.add(createNotification(id = 3, ageDays = 30, isStarred = false)) // Unstarred middle
        fakeDao.notifications.add(createNotification(id = 4, ageDays = 20, isStarred = false)) // Unstarred newest
        fakeDao.notifications.add(createNotification(id = 5, ageDays = 10, isStarred = true))  // Starred new

        // Simulate cap excess = 2 (need to drop 2 oldest unstarred)
        val deleted = fakeDao.deleteOldestUnstarred(limit = 2)

        assertEquals(2, deleted)
        assertEquals(3, fakeDao.notifications.size)

        // Starred items #1 and #5 must survive
        assertTrue(fakeDao.notifications.any { it.id == 1L })
        assertTrue(fakeDao.notifications.any { it.id == 5L })

        // Newest unstarred #4 must survive, #2 and #3 dropped
        assertTrue(fakeDao.notifications.any { it.id == 4L })
    }
}
