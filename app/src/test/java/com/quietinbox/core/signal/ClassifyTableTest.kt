package com.quietinbox.core.signal

import com.quietinbox.core.model.SignalClass
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * Verifies every row of the classification table in design.md §5.2 and TASKS.md 2.2,
 * including exact precedence ordering:
 * ONGOING -> PROGRESS -> TRANSPORT -> SERVICE -> GROUP_SUMMARY -> ALERT.
 */
class ClassifyTableTest {

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
    fun classify_ongoingEvent_returnsOngoing() {
        val n = createTestNotification(isOngoing = true, isClearable = true)
        assertEquals(SignalClass.ONGOING, classifier.classify(n))
    }

    @Test
    fun classify_foregroundService_returnsOngoing() {
        val n = createTestNotification(isForegroundService = true, isClearable = true)
        assertEquals(SignalClass.ONGOING, classifier.classify(n))
    }

    @Test
    fun classify_nonClearable_returnsOngoing() {
        val n = createTestNotification(isClearable = false)
        assertEquals(SignalClass.ONGOING, classifier.classify(n))
    }

    @Test
    fun classify_hasProgress_returnsProgress() {
        val n = createTestNotification(hasProgress = true, isOngoing = false, isClearable = true)
        assertEquals(SignalClass.PROGRESS, classifier.classify(n))
    }

    @Test
    fun classify_categoryProgress_returnsProgress() {
        val n = createTestNotification(androidCategory = "progress", hasProgress = false)
        assertEquals(SignalClass.PROGRESS, classifier.classify(n))
    }

    @Test
    fun classify_categoryTransport_returnsTransport() {
        val n = createTestNotification(androidCategory = "transport")
        assertEquals(SignalClass.TRANSPORT, classifier.classify(n))
    }

    @Test
    fun classify_categoryService_returnsService() {
        val n = createTestNotification(androidCategory = "service")
        assertEquals(SignalClass.SERVICE, classifier.classify(n))
    }

    @Test
    fun classify_categoryStatus_returnsService() {
        val n = createTestNotification(androidCategory = "status")
        assertEquals(SignalClass.SERVICE, classifier.classify(n))
    }

    @Test
    fun classify_categorySys_returnsService() {
        val n = createTestNotification(androidCategory = "sys")
        assertEquals(SignalClass.SERVICE, classifier.classify(n))
    }

    @Test
    fun classify_categorySystem_returnsService() {
        val n = createTestNotification(androidCategory = "system")
        assertEquals(SignalClass.SERVICE, classifier.classify(n))
    }

    @Test
    fun classify_categoryNavigation_returnsService() {
        val n = createTestNotification(androidCategory = "navigation")
        assertEquals(SignalClass.SERVICE, classifier.classify(n))
    }

    @Test
    fun classify_categoryLocationSharing_returnsService() {
        val n = createTestNotification(androidCategory = "location_sharing")
        assertEquals(SignalClass.SERVICE, classifier.classify(n))
    }

    @Test
    fun classify_groupSummary_returnsGroupSummary() {
        val n = createTestNotification(isGroupSummary = true)
        assertEquals(SignalClass.GROUP_SUMMARY, classifier.classify(n))
    }

    @Test
    fun classify_standardAlert_returnsAlert() {
        val n = createTestNotification(
            isOngoing = false,
            isForegroundService = false,
            isClearable = true,
            hasProgress = false,
            androidCategory = "msg",
            isGroupSummary = false
        )
        assertEquals(SignalClass.ALERT, classifier.classify(n))
    }

    @Test
    fun classify_priority_ongoingBeatsProgress() {
        val n = createTestNotification(isOngoing = true, hasProgress = true)
        assertEquals(SignalClass.ONGOING, classifier.classify(n))
    }

    @Test
    fun classify_priority_progressBeatsTransport() {
        val n = createTestNotification(hasProgress = true, androidCategory = "transport")
        assertEquals(SignalClass.PROGRESS, classifier.classify(n))
    }

    @Test
    fun classify_priority_transportBeatsService() {
        val n = createTestNotification(androidCategory = "transport")
        assertEquals(SignalClass.TRANSPORT, classifier.classify(n))
    }

    @Test
    fun classify_priority_serviceBeatsGroupSummary() {
        val n = createTestNotification(androidCategory = "service", isGroupSummary = true)
        assertEquals(SignalClass.SERVICE, classifier.classify(n))
    }

    @Test
    fun classify_priority_groupSummaryBeatsAlert() {
        val n = createTestNotification(isGroupSummary = true)
        assertEquals(SignalClass.GROUP_SUMMARY, classifier.classify(n))
    }
}
