package com.quietinbox.core.signal

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Tests for deterministic content hashing and identical-content duplicate suppression.
 */
class ContentHashTest {

    private lateinit var clock: TestClock
    private lateinit var dao: FakeNotificationDao
    private lateinit var classifier: DefaultSignalClassifier

    @Before
    fun setUp() {
        clock = TestClock()
        dao = FakeNotificationDao()
        classifier = DefaultSignalClassifier(dao, clock)
    }

    @Test
    fun contentHash_returns64CharHex() {
        val n = createTestNotification(title = "Title", body = "Body", subText = "Sub")
        val hash = classifier.contentHash(n)
        assertEquals(64, hash.length)
        assertTrue(hash.matches(Regex("^[0-9a-f]{64}$")))
    }

    @Test
    fun contentHash_handlesNullsGracefully() {
        val n1 = createTestNotification(title = null, body = null, subText = null)
        val hash1 = classifier.contentHash(n1)
        assertEquals(64, hash1.length)

        val n2 = createTestNotification(title = "", body = "", subText = "")
        val hash2 = classifier.contentHash(n2)
        assertEquals(hash1, hash2)
    }

    @Test
    fun contentHash_normalizesWhitespace() {
        val n1 = createTestNotification(
            title = "  Important   Notice  ",
            body = "Line 1\n\nLine  2   with   spaces",
            subText = "  sub  "
        )
        val n2 = createTestNotification(
            title = "Important Notice",
            body = "Line 1 Line 2 with spaces",
            subText = "sub"
        )

        val hash1 = classifier.contentHash(n1)
        val hash2 = classifier.contentHash(n2)
        assertEquals(hash1, hash2)
    }

    @Test
    fun contentHash_truncatesBodyTo4000Chars() {
        val base4000 = "a".repeat(4000)
        val long4500 = base4000 + "b".repeat(500)
        val long5000 = base4000 + "c".repeat(1000)

        val n1 = createTestNotification(title = "Header", body = long4500)
        val n2 = createTestNotification(title = "Header", body = long5000)

        val hash1 = classifier.contentHash(n1)
        val hash2 = classifier.contentHash(n2)
        // Since body is truncated to 4000 characters first, both have "a".repeat(4000) as body
        assertEquals(hash1, hash2)

        val n3 = createTestNotification(title = "Header", body = "a".repeat(3999) + "z")
        assertNotEquals(hash1, classifier.contentHash(n3))
    }

    @Test
    fun identicalContentSuppression_sameKeyAndContent_updatesExistingRow() = runBlocking {
        val sbnKey = "com.battery.monitor|charge|1"
        val n1 = createTestNotification(
            sbnKey = sbnKey,
            title = "Battery 20%",
            body = "Plug in charger"
        )

        // 1st notification -> insert
        val d1 = classifier.decide(n1)
        assertTrue(d1 is IngestDecision.Insert)
        dao.applyDecision(d1, n1, classifier.contentHash(n1), clock.nowEpochMs())

        // 5 minutes later (> 10s burst window), identical content posted again
        clock.advance(5 * 60 * 1000L)
        val n2 = createTestNotification(
            sbnKey = sbnKey,
            title = "Battery 20%",
            body = "Plug in charger"
        )

        val d2 = classifier.decide(n2)
        assertTrue(d2 is IngestDecision.UpdateExisting)
        assertEquals(1L, (d2 as IngestDecision.UpdateExisting).rowId)
        assertTrue(d2.bumpCount)
    }
}
