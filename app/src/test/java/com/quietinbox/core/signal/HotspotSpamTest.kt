package com.quietinbox.core.signal

import com.quietinbox.core.model.SignalClass
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Verifies that 600 rapid updates from a mobile hotspot notification collapse into exactly 1 database row.
 * This is the critical acceptance test for the hotspot spam bug reported in design.md §5.2.
 */
class HotspotSpamTest {

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
    fun hotspot_600Updates_producesExactlyOneInsertAnd599Updates() = runBlocking {
        val sbnKey = "android|hotspot|0|1000"
        var insertCount = 0
        var updateCount = 0

        // Simulate 600 notifications across 10 minutes (1 second apart)
        for (i in 1..600) {
            val mb = String.format(java.util.Locale.US, "%.2f", 3.00 + (i * 0.01))
            val notification = createTestNotification(
                sbnKey = sbnKey,
                packageName = "com.android.settings",
                appLabel = "Android System",
                title = "Hotspot active",
                body = "1 device connected, $mb MB used",
                isOngoing = true,
                isClearable = false,
                postedAt = clock.nowEpochMs()
            )

            val decision = classifier.decide(notification)
            val hash = classifier.contentHash(notification)

            when (decision) {
                is IngestDecision.Insert -> {
                    insertCount++
                    assertEquals(SignalClass.ONGOING, decision.signalClass)
                }
                is IngestDecision.UpdateExisting -> {
                    updateCount++
                    assertEquals(1L, decision.rowId)
                    assertTrue(decision.bumpCount)
                }
                is IngestDecision.Drop -> {
                    org.junit.Assert.fail("Hotspot notification should never be dropped")
                }
            }

            // Apply decision to database
            dao.applyDecision(decision, notification, hash, clock.nowEpochMs())

            // Advance clock by 1 second
            clock.advance(1000L)
        }

        assertEquals(1, insertCount)
        assertEquals(599, updateCount)
        assertEquals(1, dao.rows.size)

        val sessionRow = dao.rows.first()
        assertEquals(sbnKey, sessionRow.sbnKey)
        assertEquals("ONGOING", sessionRow.signalClass)
        assertEquals(600, sessionRow.updateCount)
        assertEquals("1 device connected, 9.00 MB used", sessionRow.body)
    }

    @Test
    fun hotspot_afterSessionEnded_newHotspotCreatesNewSession() = runBlocking {
        val sbnKey = "android|hotspot|0|1000"

        // 1st hotspot session: 10 updates
        for (i in 1..10) {
            val n = createTestNotification(
                sbnKey = sbnKey,
                title = "Hotspot active",
                body = "$i MB",
                isOngoing = true,
                isClearable = false
            )
            val decision = classifier.decide(n)
            dao.applyDecision(decision, n, classifier.contentHash(n), clock.nowEpochMs())
            clock.advance(1000L)
        }

        assertEquals(1, dao.rows.size)

        // Hotspot turned off -> session marked ended
        dao.markEnded(sbnKey, clock.nowEpochMs(), 1)

        // 2 hours later, hotspot turned on again
        clock.advance(2 * 60 * 60 * 1000L)
        val n2 = createTestNotification(
            sbnKey = sbnKey,
            title = "Hotspot active",
            body = "0.01 MB",
            isOngoing = true,
            isClearable = false
        )
        val decision2 = classifier.decide(n2)
        assertTrue(decision2 is IngestDecision.Insert)
        dao.applyDecision(decision2, n2, classifier.contentHash(n2), clock.nowEpochMs())

        assertEquals(2, dao.rows.size)
    }
}
