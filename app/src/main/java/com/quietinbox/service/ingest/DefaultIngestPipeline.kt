package com.quietinbox.service.ingest

import android.service.notification.NotificationListenerService
import com.quietinbox.core.firewall.FirewallAction
import com.quietinbox.core.firewall.FirewallEngine
import com.quietinbox.core.health.DefaultListenerHealth
import com.quietinbox.core.model.CapturedNotification
import com.quietinbox.core.signal.IngestDecision
import com.quietinbox.core.signal.SignalClassifier
import com.quietinbox.core.time.Clock
import com.quietinbox.data.db.dao.NotificationDao
import com.quietinbox.data.db.dao.StatsDao
import com.quietinbox.data.db.entity.FirewallDecisionEntity
import com.quietinbox.data.db.entity.NotificationEntity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultIngestPipeline @Inject constructor(
    private val notificationDao: NotificationDao,
    private val statsDao: StatsDao,
    private val signalClassifier: SignalClassifier,
    private val firewallEngine: FirewallEngine,
    private val listenerHealth: DefaultListenerHealth,
    private val clock: Clock,
) : IngestPipeline {

    override suspend fun onPosted(n: CapturedNotification): String? {
        val decision = signalClassifier.decide(n)
        return when (decision) {
            is IngestDecision.Drop -> {
                null
            }
            is IngestDecision.UpdateExisting -> {
                val existing = notificationDao.findById(decision.rowId)
                val updateCount = if (decision.bumpCount) {
                    (existing?.updateCount ?: 1) + 1
                } else {
                    existing?.updateCount ?: 1
                }
                val now = clock.now()
                notificationDao.updateSession(
                    rowId = decision.rowId,
                    lastSeenAt = now,
                    title = n.title,
                    body = n.body,
                    updateCount = updateCount
                )
                null
            }
            is IngestDecision.Insert -> {
                val now = clock.now()
                val contentHash = signalClassifier.contentHash(n)

                val entity = NotificationEntity(
                    id = 0L,
                    sbnKey = n.sbnKey,
                    packageName = n.packageName,
                    appLabel = n.appLabel,
                    title = n.title,
                    body = n.body,
                    subText = n.subText,
                    senderName = n.senderName,
                    senderDigits = n.senderDigits,
                    channelId = n.channelId,
                    androidCategory = n.androidCategory,
                    importance = n.importance,
                    signalClass = decision.signalClass.name,
                    contentHash = contentHash,
                    firstSeenAt = n.postedAt,
                    lastSeenAt = n.postedAt,
                    endedAt = null,
                    updateCount = 1,
                    wasRateLimited = false,
                    isSeen = false,
                    isStarred = false,
                    removalReason = null
                )

                // CRITICAL (design.md principle 2): Ingest writes row to Room DB BEFORE firewall evaluates
                notificationDao.insert(entity)

                // Firewall evaluation
                val verdict = firewallEngine.evaluate(n, decision.signalClass)

                // Log firewall decision to stats
                statsDao.logDecision(
                    FirewallDecisionEntity(
                        id = 0L,
                        sbnKey = n.sbnKey,
                        action = verdict.action.name,
                        ruleId = verdict.ruleId,
                        ruleLabel = verdict.ruleLabel,
                        at = now
                    )
                )

                listenerHealth.recordCapture(now)

                if (verdict.action == FirewallAction.SILENCE) {
                    n.sbnKey
                } else {
                    null
                }
            }
        }
    }

    override suspend fun onRemoved(sbnKey: String, reason: Int) {
        val now = clock.now()
        notificationDao.markEnded(sbnKey, endedAt = now, reason = reason)

        // User dismissed notification in shade -> mark as seen in Inbox
        if (reason == NotificationListenerService.REASON_CANCEL ||
            reason == NotificationListenerService.REASON_CANCEL_ALL
        ) {
            val latest = notificationDao.latestForKey(sbnKey)
            if (latest != null) {
                notificationDao.setSeen(latest.id, true)
            }
        }
        // If reason == REASON_LISTENER_CANCEL, Quiet Inbox silenced it. DO NOT mark seen.
    }

    override suspend fun backfill(active: List<CapturedNotification>) {
        for (n in active) {
            val existing = notificationDao.findByKey(n.sbnKey)
            if (existing == null) {
                val signalClass = signalClassifier.classify(n)
                val contentHash = signalClassifier.contentHash(n)
                val entity = NotificationEntity(
                    id = 0L,
                    sbnKey = n.sbnKey,
                    packageName = n.packageName,
                    appLabel = n.appLabel,
                    title = n.title,
                    body = n.body,
                    subText = n.subText,
                    senderName = n.senderName,
                    senderDigits = n.senderDigits,
                    channelId = n.channelId,
                    androidCategory = n.androidCategory,
                    importance = n.importance,
                    signalClass = signalClass.name,
                    contentHash = contentHash,
                    firstSeenAt = n.postedAt,
                    lastSeenAt = n.postedAt,
                    endedAt = null,
                    updateCount = 1,
                    wasRateLimited = false,
                    isSeen = false,
                    isStarred = false,
                    removalReason = null
                )
                notificationDao.insert(entity)
            }
        }
    }
}
