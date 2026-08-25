package com.quietinbox.core.signal

import com.quietinbox.core.model.SignalClass
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Verifies handling of group summary notifications:
 * Dropped when non-summary children exist in the same group within the last 5 minutes;
 * Treated as ALERT when no children exist or children are older than 5 minutes.
 */
class GroupSummaryTest {

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
    fun groupSummary_withRecentChild_dropsSummary() = runBlocking {
        val groupKey = "com.google.android.gm|emails|user@gmail.com"

        // Child email arrived 1 minute ago
        val child = createTestNotification(
            sbnKey = "gmail|msg|1",
            title = "New Email",
            body = "Meeting tomorrow",
            isGroupSummary = false,
            groupKey = groupKey
        )
        val childDecision = classifier.decide(child)
        dao.applyDecision(childDecision, child, classifier.contentHash(child), clock.nowEpochMs())

        clock.advance(60_000L)

        // Summary notification arrives
        val summary = createTestNotification(
            sbnKey = "gmail|summary|1",
            title = "Gmail",
            body = "1 new message",
            isGroupSummary = true,
            groupKey = groupKey
        )

        val summaryDecision = classifier.decide(summary)
        assertTrue(summaryDecision is IngestDecision.Drop)
        assertEquals("group summary", (summaryDecision as IngestDecision.Drop).reason)
    }

    @Test
    fun groupSummary_withoutChild_treatedAsAlertAndInserted() = runBlocking {
        val summary = createTestNotification(
            sbnKey = "some.app|summary|1",
            title = "Some App",
            body = "3 updates",
            isGroupSummary = true,
            groupKey = "some.app.group"
        )

        val decision = classifier.decide(summary)
        assertTrue(decision is IngestDecision.Insert)
        assertEquals(SignalClass.ALERT, (decision as IngestDecision.Insert).signalClass)
    }

    @Test
    fun groupSummary_childOlderThan5Minutes_treatedAsAlert() = runBlocking {
        val groupKey = "com.chat|group|123"

        // Child arrived 6 minutes ago
        val child = createTestNotification(
            sbnKey = "chat|msg|1",
            title = "Old msg",
            body = "Hi",
            isGroupSummary = false,
            groupKey = groupKey
        )
        val childDecision = classifier.decide(child)
        dao.applyDecision(childDecision, child, classifier.contentHash(child), clock.nowEpochMs())

        clock.advance(6 * 60 * 1000L) // 6 minutes later

        val summary = createTestNotification(
            sbnKey = "chat|summary|1",
            title = "Chat",
            body = "New message",
            isGroupSummary = true,
            groupKey = groupKey
        )

        val summaryDecision = classifier.decide(summary)
        assertTrue(summaryDecision is IngestDecision.Insert)
    }

    @Test
    fun groupSummary_nullGroupKey_treatedAsAlert() = runBlocking {
        val summary = createTestNotification(
            sbnKey = "chat|summary|null_group",
            isGroupSummary = true,
            groupKey = null
        )

        val decision = classifier.decide(summary)
        assertTrue(decision is IngestDecision.Insert)
    }
}
