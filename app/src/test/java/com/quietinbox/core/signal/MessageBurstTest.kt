package com.quietinbox.core.signal

import com.quietinbox.core.model.SignalClass
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Verifies burst coalescing for ALERT notifications:
 * Rapid messages within the 10-second burst window coalesce into 1 row;
 * spaced messages (>10s apart) create separate rows.
 */
class MessageBurstTest {

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
    fun messageBurst_within10Seconds_coalescesIntoSingleRow() = runBlocking {
        val sbnKey = "com.whatsapp|msg|user123"
        val messages = listOf("typing…", "1 new message", "2 new messages")

        // 1st message at t=0s
        val n1 = createTestNotification(sbnKey = sbnKey, body = messages[0])
        val d1 = classifier.decide(n1)
        assertTrue(d1 is IngestDecision.Insert)
        assertEquals(SignalClass.ALERT, (d1 as IngestDecision.Insert).signalClass)
        dao.applyDecision(d1, n1, classifier.contentHash(n1), clock.nowEpochMs())

        // 2nd message at t=2s (within 10s burst window)
        clock.advance(2_000L)
        val n2 = createTestNotification(sbnKey = sbnKey, body = messages[1])
        val d2 = classifier.decide(n2)
        assertTrue(d2 is IngestDecision.UpdateExisting)
        assertEquals(1L, (d2 as IngestDecision.UpdateExisting).rowId)
        dao.applyDecision(d2, n2, classifier.contentHash(n2), clock.nowEpochMs())

        // 3rd message at t=5s (within 10s burst window from last seen)
        clock.advance(3_000L)
        val n3 = createTestNotification(sbnKey = sbnKey, body = messages[2])
        val d3 = classifier.decide(n3)
        assertTrue(d3 is IngestDecision.UpdateExisting)
        assertEquals(1L, (d3 as IngestDecision.UpdateExisting).rowId)
        dao.applyDecision(d3, n3, classifier.contentHash(n3), clock.nowEpochMs())

        assertEquals(1, dao.rows.size)
        val row = dao.rows.first()
        assertEquals("2 new messages", row.body)
        assertEquals(3, row.updateCount)
    }

    @Test
    fun messageBurst_sixtySecondsApart_createsThreeSeparateRows() = runBlocking {
        val sbnKey = "com.whatsapp|msg|user123"
        val messages = listOf("typing…", "1 new message", "2 new messages")

        // 1st message at t=0s
        val n1 = createTestNotification(sbnKey = sbnKey, body = messages[0])
        val d1 = classifier.decide(n1)
        assertTrue(d1 is IngestDecision.Insert)
        dao.applyDecision(d1, n1, classifier.contentHash(n1), clock.nowEpochMs())

        // 2nd message at t=60s (>10s window)
        clock.advance(60_000L)
        val n2 = createTestNotification(sbnKey = sbnKey, body = messages[1])
        val d2 = classifier.decide(n2)
        assertTrue(d2 is IngestDecision.Insert)
        dao.applyDecision(d2, n2, classifier.contentHash(n2), clock.nowEpochMs())

        // 3rd message at t=120s (>10s window)
        clock.advance(60_000L)
        val n3 = createTestNotification(sbnKey = sbnKey, body = messages[2])
        val d3 = classifier.decide(n3)
        assertTrue(d3 is IngestDecision.Insert)
        dao.applyDecision(d3, n3, classifier.contentHash(n3), clock.nowEpochMs())

        assertEquals(3, dao.rows.size)
        assertEquals("typing…", dao.rows[0].body)
        assertEquals("1 new message", dao.rows[1].body)
        assertEquals("2 new messages", dao.rows[2].body)
    }
}
