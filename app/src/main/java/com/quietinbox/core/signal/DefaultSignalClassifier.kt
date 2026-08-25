package com.quietinbox.core.signal

import com.quietinbox.core.model.CapturedNotification
import com.quietinbox.core.model.SignalClass
import com.quietinbox.core.time.Clock
import com.quietinbox.data.db.dao.NotificationDao
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Production implementation of [SignalClassifier] responsible for collapsing notification spam,
 * tracking ongoing sessions, coalescing rapid message bursts, suppressing identical duplicate posts,
 * and enforcing hourly rate ceilings per notification key.
 */
@Singleton
class DefaultSignalClassifier @Inject constructor(
    private val notificationDao: NotificationDao,
    private val clock: Clock,
) : SignalClassifier {

    companion object {
        private val SERVICE_CATEGORIES = setOf(
            "service",
            "status",
            "sys",
            "system",
            "navigation",
            "location_sharing"
        )
        private val WHITESPACE_REGEX = Regex("\\s+")
    }

    override fun classify(n: CapturedNotification): SignalClass {
        return when {
            // Ongoing session: active ongoing flag, foreground service flag, or non-clearable flag
            n.isOngoing || n.isForegroundService || !n.isClearable -> SignalClass.ONGOING

            // Progress session: has progress indicator or category progress
            n.hasProgress || n.androidCategory.equals("progress", ignoreCase = true) -> SignalClass.PROGRESS

            // Transport session: media playback controls
            n.androidCategory.equals("transport", ignoreCase = true) -> SignalClass.TRANSPORT

            // Service session: background services, status, system, navigation, location sharing
            n.androidCategory != null && n.androidCategory.lowercase() in SERVICE_CATEGORIES -> SignalClass.SERVICE

            // Group summary: system bundle summary notification
            n.isGroupSummary -> SignalClass.GROUP_SUMMARY

            // Alert: standard event notification
            else -> SignalClass.ALERT
        }
    }

    override fun contentHash(n: CapturedNotification): String {
        val normTitle = normalize(n.title)
        val truncatedBody = n.body?.take(SignalTuning.BODY_MAX_CHARS)
        val normBody = normalize(truncatedBody)
        val normSubText = normalize(n.subText)
        val rawPayload = "$normTitle|$normBody|$normSubText"
        return sha256(rawPayload)
    }

    override suspend fun decide(n: CapturedNotification): IngestDecision {
        val signalClass = classify(n)
        val now = clock.now()

        return when (signalClass) {
            SignalClass.ONGOING,
            SignalClass.PROGRESS,
            SignalClass.TRANSPORT,
            SignalClass.SERVICE -> {
                // Session collapse: if an active session row exists (endedAt == null), update in place.
                val latestRow = notificationDao.latestForKey(n.sbnKey)
                if (latestRow != null && latestRow.endedAt == null) {
                    IngestDecision.UpdateExisting(rowId = latestRow.id, bumpCount = true)
                } else {
                    IngestDecision.Insert(signalClass)
                }
            }

            SignalClass.GROUP_SUMMARY -> {
                // Group summary drop: drop if at least one non-summary child exists in the group in the last 5 minutes.
                val groupKey = n.groupKey
                if (!groupKey.isNullOrBlank()) {
                    val windowStart = now - SignalTuning.GROUP_SUMMARY_WINDOW_MS
                    val latestForGroup = notificationDao.latestForKey(groupKey)
                    val latestForSummary = notificationDao.latestForKey(n.sbnKey)
                    val candidate = latestForGroup ?: latestForSummary
                    if (candidate != null && candidate.lastSeenAt >= windowStart) {
                        return IngestDecision.Drop("group summary")
                    }
                }
                // Otherwise treat it as ALERT (some apps post only the summary).
                decideAlert(n, now)
            }

            SignalClass.ALERT -> {
                decideAlert(n, now)
            }
        }
    }

    private suspend fun decideAlert(n: CapturedNotification, now: Long): IngestDecision {
        val currentHash = contentHash(n)
        val latestRow = notificationDao.latestForKey(n.sbnKey)

        // 1. Identical-content suppression: same key + same contentHash as latest row
        if (latestRow != null && latestRow.contentHash == currentHash) {
            return IngestDecision.UpdateExisting(rowId = latestRow.id, bumpCount = true)
        }

        // 2. Burst coalescing: same key, class ALERT, posted within burst window (10s) of latest update
        if (latestRow != null && (now - latestRow.lastSeenAt) <= SignalTuning.BURST_WINDOW_MS) {
            return IngestDecision.UpdateExisting(rowId = latestRow.id, bumpCount = true)
        }

        // 3. Rate ceiling: if more than 60 events occurred for this key within the last hour
        if (latestRow != null && latestRow.updateCount >= SignalTuning.MAX_EVENTS_PER_KEY_PER_HOUR && (now - latestRow.firstSeenAt) <= SignalTuning.ONE_HOUR_MS) {
            return IngestDecision.UpdateExisting(rowId = latestRow.id, bumpCount = true)
        }

        // Standard new event insert
        return IngestDecision.Insert(SignalClass.ALERT)
    }

    private fun normalize(text: String?): String {
        if (text == null) return ""
        return text.trim().replace(WHITESPACE_REGEX, " ")
    }

    private fun sha256(input: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(input.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }
}
