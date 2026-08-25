package com.quietinbox.core.signal

import com.quietinbox.core.model.SignalClass
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Tests hourly rate ceiling enforcement (design.md §5.2 Task 2.8):
 * More than 60 inserts for one key inside one hour forces session behaviour (UpdateExisting)
 * for the remainder of the hour.
 */
class RateCeilingTest {

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
    fun rateCeiling_exceeding60EventsInOneHour_forcesSessionCollapse() = runBlocking {
        val sbnKey = "com.noisy.app|alert|channel"

        // Post 60 distinct notifications, spaced 30 seconds apart (outside 10s burst window)
        // 60 * 30s = 1800s (30 minutes total, well within 1 hour)
        for (i in 1..60) {
            val notification = createTestNotification(
                sbnKey = sbnKey,
                title = "Alert #$i",
                body = "Unique message body $i"
            )
            val decision = classifier.decide(notification)
            assertTrue("Notification $i should be Insert", decision is IngestDecision.Insert)
            assertEquals(SignalClass.ALERT, (decision as IngestDecision.Insert).signalClass)

            dao.applyDecision(decision, notification, classifier.contentHash(notification), clock.nowEpochMs())
            clock.advance(30_000L)
        }

        assertEquals(60, dao.rows.size)

        // 61st notification in the same hour with unique content and > 10s interval
        val n61 = createTestNotification(
            sbnKey = sbnKey,
            title = "Alert #61",
            body = "Unique message body 61"
        )
        val decision61 = classifier.decide(n61)
        assertTrue("61st notification should be UpdateExisting due to rate ceiling", decision61 is IngestDecision.UpdateExisting)
        val update = decision61 as IngestDecision.UpdateExisting
        assertEquals(60L, update.rowId) // updates latest row
        assertTrue(update.bumpCount)

        dao.applyDecision(decision61, n61, classifier.contentHash(n61), clock.nowEpochMs())
        assertEquals(60, dao.rows.size) // No new row inserted

        // Advance time by 61 minutes (so older events fall outside the 1-hour window)
        clock.advance(61 * 60 * 1000L)

        // 62nd notification should now be inserted as rate limit window rolled over
        val n62 = createTestNotification(
            sbnKey = sbnKey,
            title = "Alert #62",
            body = "Unique message body 62"
        )
        val decision62 = classifier.decide(n62)
        assertTrue("After 1 hour window resets, new insert should succeed", decision62 is IngestDecision.Insert)
    }
}
