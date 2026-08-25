package com.quietinbox.service.ingest

import android.content.Context
import android.service.notification.NotificationListenerService
import androidx.paging.PagingSource
import androidx.test.core.app.ApplicationProvider
import com.quietinbox.core.firewall.FirewallAction
import com.quietinbox.core.firewall.FirewallEngine
import com.quietinbox.core.firewall.FirewallVerdict
import com.quietinbox.core.health.DefaultListenerHealth
import com.quietinbox.core.model.CapturedNotification
import com.quietinbox.core.model.SignalClass
import com.quietinbox.core.signal.IngestDecision
import com.quietinbox.core.signal.SignalClassifier
import com.quietinbox.core.time.Clock
import com.quietinbox.data.db.dao.NotificationDao
import com.quietinbox.data.db.dao.PackageNotificationCount
import com.quietinbox.data.db.dao.StatsDao
import com.quietinbox.data.db.dao.TopAppCapture
import com.quietinbox.data.db.entity.DailyStatEntity
import com.quietinbox.data.db.entity.FirewallDecisionEntity
import com.quietinbox.data.db.entity.NotificationEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class IngestPipelineTest {

    private lateinit var context: Context
    private lateinit var fakeNotificationDao: FakeNotificationDao
    private lateinit var fakeStatsDao: FakeStatsDao
    private lateinit var fakeSignalClassifier: FakeSignalClassifier
    private lateinit var fakeFirewallEngine: FakeFirewallEngine
    private lateinit var fakeClock: FakeClock
    private lateinit var listenerHealth: DefaultListenerHealth
    private lateinit var pipeline: DefaultIngestPipeline

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        fakeNotificationDao = FakeNotificationDao()
        fakeStatsDao = FakeStatsDao()
        fakeSignalClassifier = FakeSignalClassifier()
        fakeFirewallEngine = FakeFirewallEngine()
        fakeClock = FakeClock(10_000L)
        listenerHealth = DefaultListenerHealth(context, fakeClock)

        pipeline = DefaultIngestPipeline(
            notificationDao = fakeNotificationDao,
            statsDao = fakeStatsDao,
            signalClassifier = fakeSignalClassifier,
            firewallEngine = fakeFirewallEngine,
            listenerHealth = listenerHealth,
            clock = fakeClock
        )
    }

    private fun sampleNotification(
        key: String = "0|com.example.app|1|null|1000",
        packageName: String = "com.example.app",
        title: String? = "Test Title",
        body: String? = "Test Body"
    ): CapturedNotification {
        return CapturedNotification(
            sbnKey = key,
            packageName = packageName,
            appLabel = "Test App",
            title = title,
            body = body,
            subText = null,
            senderName = "Sender",
            senderDigits = "123456789",
            channelId = "channel_1",
            androidCategory = null,
            importance = 3,
            postedAt = 10_000L,
            isOngoing = false,
            isForegroundService = false,
            isGroupSummary = false,
            isClearable = true,
            hasProgress = false,
            groupKey = null
        )
    }

    @Test
    fun onPosted_whenDecisionIsDrop_doesNotInsertAndReturnsNull() = runTest {
        val notification = sampleNotification()
        fakeSignalClassifier.decideResult = IngestDecision.Drop("Duplicate content")

        val result = pipeline.onPosted(notification)

        assertNull(result)
        assertEquals(0, fakeNotificationDao.insertedEntities.size)
        assertEquals(0, fakeStatsDao.loggedDecisions.size)
    }

    @Test
    fun onPosted_whenDecisionIsUpdateExisting_updatesSessionAndReturnsNull() = runTest {
        val notification = sampleNotification(title = "Updated Title", body = "Updated Body")
        val existingEntity = NotificationEntity(
            id = 42L,
            sbnKey = notification.sbnKey,
            packageName = notification.packageName,
            appLabel = notification.appLabel,
            title = "Old Title",
            body = "Old Body",
            subText = null,
            senderName = null,
            senderDigits = null,
            channelId = null,
            androidCategory = null,
            importance = 3,
            signalClass = SignalClass.ALERT.name,
            contentHash = "hash1",
            firstSeenAt = 5_000L,
            lastSeenAt = 5_000L,
            updateCount = 2
        )
        fakeNotificationDao.insertedEntities.add(existingEntity)

        fakeSignalClassifier.decideResult = IngestDecision.UpdateExisting(rowId = 42L, bumpCount = true)
        fakeClock.currentTime = 15_000L

        val result = pipeline.onPosted(notification)

        assertNull(result)
        val updated = fakeNotificationDao.findById(42L)
        assertEquals("Updated Title", updated?.title)
        assertEquals("Updated Body", updated?.body)
        assertEquals(3, updated?.updateCount)
        assertEquals(15_000L, updated?.lastSeenAt)
    }

    @Test
    fun onPosted_whenDecisionIsInsertAndFirewallAllows_writesDbBeforeFirewallAndReturnsNull() = runTest {
        val notification = sampleNotification()
        fakeSignalClassifier.decideResult = IngestDecision.Insert(SignalClass.ALERT)
        fakeFirewallEngine.verdict = FirewallVerdict(FirewallAction.ALLOW, 0, "Default Allow")

        var dbInsertedBeforeFirewall = false
        fakeFirewallEngine.onEvaluate = {
            dbInsertedBeforeFirewall = fakeNotificationDao.insertedEntities.isNotEmpty()
        }

        val result = pipeline.onPosted(notification)

        assertNull(result)
        assertTrue("Row must be in DB before FirewallEngine evaluates", dbInsertedBeforeFirewall)
        assertEquals(1, fakeNotificationDao.insertedEntities.size)
        assertEquals(1, fakeStatsDao.loggedDecisions.size)
        assertEquals("ALLOW", fakeStatsDao.loggedDecisions.first().action)
    }

    @Test
    fun onPosted_whenDecisionIsInsertAndFirewallSilences_returnsSbnKey() = runTest {
        val notification = sampleNotification(key = "silenced_key_123")
        fakeSignalClassifier.decideResult = IngestDecision.Insert(SignalClass.ALERT)
        fakeFirewallEngine.verdict = FirewallVerdict(FirewallAction.SILENCE, 1, "Muted App Rule")

        val result = pipeline.onPosted(notification)

        assertEquals("silenced_key_123", result)
        assertEquals(1, fakeNotificationDao.insertedEntities.size)
        assertEquals(1, fakeStatsDao.loggedDecisions.size)
        assertEquals("SILENCE", fakeStatsDao.loggedDecisions.first().action)
    }

    @Test
    fun onRemoved_whenDismissedInShade_marksEndedAndMarksSeen() = runTest {
        val entity = NotificationEntity(
            id = 99L,
            sbnKey = "shade_dismissed_key",
            packageName = "com.example.chat",
            appLabel = "Chat",
            title = "Hello",
            body = "World",
            subText = null,
            senderName = null,
            senderDigits = null,
            channelId = null,
            androidCategory = null,
            importance = 3,
            signalClass = SignalClass.ALERT.name,
            contentHash = "hash2",
            firstSeenAt = 1_000L,
            lastSeenAt = 1_000L,
            isSeen = false
        )
        fakeNotificationDao.insertedEntities.add(entity)
        fakeClock.currentTime = 20_000L

        pipeline.onRemoved("shade_dismissed_key", NotificationListenerService.REASON_CANCEL)

        val updated = fakeNotificationDao.findById(99L)
        assertEquals(20_000L, updated?.endedAt)
        assertEquals(NotificationListenerService.REASON_CANCEL, updated?.removalReason)
        assertTrue("Notification dismissed in system shade must be marked as seen", updated?.isSeen == true)
    }

    @Test
    fun onRemoved_whenSilencedByListener_marksEndedWithoutMarkingSeen() = runTest {
        val entity = NotificationEntity(
            id = 101L,
            sbnKey = "silenced_by_app_key",
            packageName = "com.example.promo",
            appLabel = "Promo",
            title = "Sale",
            body = "50% off",
            subText = null,
            senderName = null,
            senderDigits = null,
            channelId = null,
            androidCategory = null,
            importance = 3,
            signalClass = SignalClass.ALERT.name,
            contentHash = "hash3",
            firstSeenAt = 1_000L,
            lastSeenAt = 1_000L,
            isSeen = false
        )
        fakeNotificationDao.insertedEntities.add(entity)
        fakeClock.currentTime = 25_000L

        pipeline.onRemoved("silenced_by_app_key", NotificationListenerService.REASON_LISTENER_CANCEL)

        val updated = fakeNotificationDao.findById(101L)
        assertEquals(25_000L, updated?.endedAt)
        assertFalse("Notification silenced by app must NOT be marked seen", updated?.isSeen == true)
    }

    @Test
    fun backfill_insertsMissingActiveNotifications() = runTest {
        val n1 = sampleNotification(key = "active_key_1")
        val n2 = sampleNotification(key = "active_key_2")

        fakeSignalClassifier.classifyResult = SignalClass.ALERT
        fakeSignalClassifier.contentHashResult = "backfill_hash"

        pipeline.backfill(listOf(n1, n2))

        assertEquals(2, fakeNotificationDao.insertedEntities.size)
        assertEquals("active_key_1", fakeNotificationDao.insertedEntities[0].sbnKey)
        assertEquals("active_key_2", fakeNotificationDao.insertedEntities[1].sbnKey)
    }

    // --- Test Doubles ---

    private class FakeClock(var currentTime: Long) : Clock {
        override fun now(): Long = currentTime
        override fun elapsedRealtime(): Long = currentTime
    }

    private class FakeSignalClassifier : SignalClassifier {
        var decideResult: IngestDecision = IngestDecision.Insert(SignalClass.ALERT)
        var classifyResult: SignalClass = SignalClass.ALERT
        var contentHashResult: String = "dummy_hash"

        override fun classify(n: CapturedNotification): SignalClass = classifyResult
        override fun contentHash(n: CapturedNotification): String = contentHashResult
        override suspend fun decide(n: CapturedNotification): IngestDecision = decideResult
    }

    private class FakeFirewallEngine : FirewallEngine {
        var verdict = FirewallVerdict(FirewallAction.ALLOW, 0, "Default")
        var onEvaluate: (() -> Unit)? = null

        override suspend fun evaluate(n: CapturedNotification, signalClass: SignalClass): FirewallVerdict {
            onEvaluate?.invoke()
            return verdict
        }
    }

    private class FakeNotificationDao : NotificationDao {
        val insertedEntities = mutableListOf<NotificationEntity>()
        private var nextId = 1L

        override suspend fun findByKey(sbnKey: String): NotificationEntity? {
            return insertedEntities.firstOrNull { it.sbnKey == sbnKey }
        }

        override suspend fun latestForKey(sbnKey: String): NotificationEntity? {
            return insertedEntities.filter { it.sbnKey == sbnKey }.maxByOrNull { it.firstSeenAt }
        }

        override suspend fun findById(id: Long): NotificationEntity? {
            return insertedEntities.firstOrNull { it.id == id }
        }

        override suspend fun insert(notification: NotificationEntity): Long {
            val id = if (notification.id == 0L) nextId++ else notification.id
            val stored = notification.copy(id = id)
            insertedEntities.removeAll { it.id == id }
            insertedEntities.add(stored)
            return id
        }

        override suspend fun updateSession(
            rowId: Long,
            lastSeenAt: Long,
            title: String?,
            body: String?,
            updateCount: Int
        ) {
            val index = insertedEntities.indexOfFirst { it.id == rowId }
            if (index >= 0) {
                val current = insertedEntities[index]
                insertedEntities[index] = current.copy(
                    lastSeenAt = lastSeenAt,
                    title = title,
                    body = body,
                    updateCount = updateCount
                )
            }
        }

        override suspend fun markEnded(sbnKey: String, endedAt: Long, reason: Int?) {
            for (i in insertedEntities.indices) {
                if (insertedEntities[i].sbnKey == sbnKey && insertedEntities[i].endedAt == null) {
                    insertedEntities[i] = insertedEntities[i].copy(
                        endedAt = endedAt,
                        removalReason = reason
                    )
                }
            }
        }

        override suspend fun setSeen(id: Long, seen: Boolean) {
            val index = insertedEntities.indexOfFirst { it.id == id }
            if (index >= 0) {
                insertedEntities[index] = insertedEntities[index].copy(isSeen = seen)
            }
        }

        override suspend fun setSeenByIds(ids: List<Long>, seen: Boolean) {
            for (i in insertedEntities.indices) {
                if (insertedEntities[i].id in ids) {
                    insertedEntities[i] = insertedEntities[i].copy(isSeen = seen)
                }
            }
        }

        override suspend fun setStarred(id: Long, starred: Boolean) {
            val index = insertedEntities.indexOfFirst { it.id == id }
            if (index >= 0) {
                insertedEntities[index] = insertedEntities[index].copy(isStarred = starred)
            }
        }

        override suspend fun deleteByIds(ids: List<Long>) {
            insertedEntities.removeAll { it.id in ids }
        }

        override suspend fun purgeOlderThan(cutoff: Long): Int {
            val countBefore = insertedEntities.size
            insertedEntities.removeAll { it.firstSeenAt < cutoff && !it.isStarred }
            return countBefore - insertedEntities.size
        }

        override suspend fun count(): Int = insertedEntities.size
        override suspend fun countsSince(from: Long): Int = insertedEntities.count { it.firstSeenAt >= from }
        override suspend fun countByPackage(packageName: String): Int = insertedEntities.count { it.packageName == packageName }
        override suspend fun getPackageCounts(): List<PackageNotificationCount> = emptyList()
        override suspend fun purgeOldestUnstarred(count: Int): Int = 0

        override fun pagingSource(): PagingSource<Int, NotificationEntity> = throw NotImplementedError()
        override fun pagingSourceFiltered(
            isSeen: Boolean?,
            isStarred: Boolean?,
            packageName: String?
        ): PagingSource<Int, NotificationEntity> = throw NotImplementedError()

        override fun searchPaging(query: String, digits: String?): PagingSource<Int, NotificationEntity> = throw NotImplementedError()
    }

    private class FakeStatsDao : StatsDao {
        val loggedDecisions = mutableListOf<FirewallDecisionEntity>()

        override suspend fun getStats(day: Int): DailyStatEntity? = null
        override fun getRange(fromDay: Int, toDay: Int): Flow<List<DailyStatEntity>> = emptyFlow()
        override suspend fun insertOrReplace(stat: DailyStatEntity) {}
        override suspend fun insertOrReplaceAll(stats: List<DailyStatEntity>) {}
        override suspend fun logDecision(decision: FirewallDecisionEntity): Long {
            loggedDecisions.add(decision)
            return loggedDecisions.size.toLong()
        }
        override suspend fun logDecisions(decisions: List<FirewallDecisionEntity>) {
            loggedDecisions.addAll(decisions)
        }
        override suspend fun getSilencedCountSince(sinceEpochMs: Long): Int = loggedDecisions.count { it.action == "SILENCE" && it.at >= sinceEpochMs }
        override suspend fun getAllowedCountSince(sinceEpochMs: Long): Int = loggedDecisions.count { it.action == "ALLOW" && it.at >= sinceEpochMs }
        override suspend fun getTopNotifyingApps(sinceEpochMs: Long, limit: Int): List<TopAppCapture> = emptyList()
    }
}
