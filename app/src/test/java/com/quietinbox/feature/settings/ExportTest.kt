package com.quietinbox.feature.settings

import com.quietinbox.data.db.entity.DailyStatEntity
import com.quietinbox.data.db.entity.NotificationEntity
import com.quietinbox.feature.settings.data.ExportManager
import com.quietinbox.feature.settings.data.SettingsDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream

class ExportTest {

    private class FakeSettingsDao(
        private val notifications: List<NotificationEntity>
    ) : SettingsDao {
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
        override suspend fun deleteUnstarredOlderThan(cutoffEpochMs: Long): Int = 0
        override suspend fun deleteOldestUnstarred(limit: Int): Int = 0
        override suspend fun deleteAllNotifications(): Int = 0
        override suspend fun deleteAllFirewallDecisions(): Int = 0
        override suspend fun deleteAllDailyStats(): Int = 0

        override suspend fun getNotificationsChunk(limit: Int, offset: Int): List<NotificationEntity> {
            if (offset >= notifications.size) return emptyList()
            val toIndex = (offset + limit).coerceAtMost(notifications.size)
            return notifications.subList(offset, toIndex)
        }

        override suspend fun getAllNotificationsForExport(): List<NotificationEntity> = notifications
        override suspend fun getDistinctNotificationDays(): List<Int> = emptyList()
        override suspend fun getCapturedCountInRange(startEpochMs: Long, endEpochMs: Long): Int = 0
        override suspend fun getDistinctAppsInRange(startEpochMs: Long, endEpochMs: Long): Int = 0
        override suspend fun getSilencedCountInRange(startEpochMs: Long, endEpochMs: Long): Int = 0
        override suspend fun getAllowedCountInRange(startEpochMs: Long, endEpochMs: Long): Int = 0
        override suspend fun insertOrUpdateDailyStat(stat: DailyStatEntity) {}
        override suspend fun deleteDailyStatsNotIn(days: List<Int>) {}
        override suspend fun executeRaw(query: androidx.sqlite.db.SupportSQLiteQuery): Int = 0
    }

    private fun createSampleNotification(
        id: Long,
        title: String = "Test Title $id",
        body: String = "Test Body $id",
        packageName: String = "com.example.app",
        isStarred: Boolean = false
    ): NotificationEntity {
        return NotificationEntity(
            id = id,
            sbnKey = "0|com.example.app|$id|null|1000",
            packageName = packageName,
            appLabel = "Sample App",
            title = title,
            body = body,
            subText = "subText",
            senderName = "Sender $id",
            senderDigits = "123456789",
            channelId = "channel_general",
            androidCategory = "msg",
            importance = 3,
            signalClass = "ALERT",
            contentHash = "hash_$id",
            firstSeenAt = 1000000L + id * 1000L,
            lastSeenAt = 1000000L + id * 1000L,
            endedAt = null,
            updateCount = 1,
            wasRateLimited = false,
            isSeen = false,
            isStarred = isStarred,
            removalReason = null
        )
    }

    @Test
    fun testCsvEscapeHelper() {
        val fakeDao = FakeSettingsDao(emptyList())
        val exportManager = ExportManager(fakeDao)

        assertEquals("", exportManager.escapeCsv(null))
        assertEquals("Simple text", exportManager.escapeCsv("Simple text"))
        assertEquals("\"Text with, comma\"", exportManager.escapeCsv("Text with, comma"))
        assertEquals("\"Text with \"\"quotes\"\"\"", exportManager.escapeCsv("Text with \"quotes\""))
        assertEquals("\"Text with\nnewline\"", exportManager.escapeCsv("Text with\nnewline"))
        assertEquals("\"Text with, comma and \"\"quotes\"\" and\nnewline\"", exportManager.escapeCsv("Text with, comma and \"quotes\" and\nnewline"))
    }

    @Test
    fun testStreamingCsvExportWithEscaping() = runBlocking {
        val specialNotifications = listOf(
            createSampleNotification(
                id = 1,
                title = "Meeting, Urgent!",
                body = "Here is a \"special\" quote and a\nnew line.",
                packageName = "com.chat.app"
            ),
            createSampleNotification(
                id = 2,
                title = "Simple Title",
                body = "Normal body without special characters"
            )
        )

        val fakeDao = FakeSettingsDao(specialNotifications)
        val exportManager = ExportManager(fakeDao)
        val outputStream = ByteArrayOutputStream()

        val exportedCount = exportManager.exportCsvToStream(outputStream)
        assertEquals(2, exportedCount)

        val csvString = outputStream.toString("UTF-8")
        assertTrue(csvString.startsWith("id,sbnKey,packageName"))
        assertTrue(csvString.contains("\"Meeting, Urgent!\""))
        assertTrue(csvString.contains("\"Here is a \"\"special\"\" quote and a\nnew line.\""))
        assertTrue(csvString.contains("Simple Title"))
    }

    @Test
    fun testStreamingJsonExport10000Rows() = runBlocking {
        val largeList = (1..10_000).map { id ->
            createSampleNotification(id = id.toLong())
        }

        val fakeDao = FakeSettingsDao(largeList)
        val exportManager = ExportManager(fakeDao)
        val outputStream = ByteArrayOutputStream()

        val exportedCount = exportManager.exportJsonToStream(outputStream)
        assertEquals(10_000, exportedCount)

        val jsonString = outputStream.toString("UTF-8")
        assertTrue(jsonString.startsWith("[\n"))
        assertTrue(jsonString.endsWith("\n]\n"))
        assertTrue(jsonString.contains("\"id\": 1"))
        assertTrue(jsonString.contains("\"id\": 10000"))
        assertTrue(jsonString.contains("\"packageName\": \"com.example.app\""))
    }
}
