package com.quietinbox.core.signal

import com.quietinbox.core.model.SignalClass
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Verifies that 400 download progress updates with the same key collapse into a single database row.
 */
class DownloadProgressTest {

    private lateinit var clock: TestClock
    private lateinit var dao: FakeNotificationDao
    private lateinit var classifier: DefaultSignalClassifier

    @Before
    fun setUp() {
        clock = TestClock(currentEpochMs = 1_700_000_000_000L)
        dao = FakeNotificationDao()
        classifier = DefaultSignalClassifier(dao, clock)
    }

    @Test
    fun downloadProgress_400Ticks_producesExactlyOneInsertAnd399Updates() = runBlocking {
        val sbnKey = "com.android.providers.downloads|download|100|42"
        var insertCount = 0
        var updateCount = 0

        // 400 progress notifications
        for (i in 1..400) {
            val percent = (i * 100) / 400
            val notification = createTestNotification(
                sbnKey = sbnKey,
                packageName = "com.android.providers.downloads",
                appLabel = "Download Manager",
                title = "Downloading file.zip",
                body = "$percent% ($i/400 MB)",
                hasProgress = true,
                isOngoing = false,
                isClearable = true,
                postedAt = clock.nowEpochMs()
            )

            val decision = classifier.decide(notification)
            val hash = classifier.contentHash(notification)

            when (decision) {
                is IngestDecision.Insert -> {
                    insertCount++
                    assertEquals(SignalClass.PROGRESS, decision.signalClass)
                }
                is IngestDecision.UpdateExisting -> {
                    updateCount++
                    assertEquals(1L, decision.rowId)
                    assertTrue(decision.bumpCount)
                }
                is IngestDecision.Drop -> {
                    org.junit.Assert.fail("Download progress should never be dropped")
                }
            }

            dao.applyDecision(decision, notification, hash, clock.nowEpochMs())
            clock.advance(500L)
        }

        assertEquals(1, insertCount)
        assertEquals(399, updateCount)
        assertEquals(1, dao.rows.size)

        val row = dao.rows.first()
        assertEquals(sbnKey, row.sbnKey)
        assertEquals("PROGRESS", row.signalClass)
        assertEquals(400, row.updateCount)
        assertEquals("100% (400/400 MB)", row.body)
    }

    @Test
    fun downloadProgress_withCategoryProgress_classifiesAsProgress() = runBlocking {
        val notification = createTestNotification(
            sbnKey = "com.example.browser|download|1",
            androidCategory = "progress",
            hasProgress = false
        )
        assertEquals(SignalClass.PROGRESS, classifier.classify(notification))
    }
}
