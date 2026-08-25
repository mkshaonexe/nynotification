package com.quietinbox.service.ingest

import android.app.Notification
import android.app.NotificationManager
import android.os.Build
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import com.quietinbox.core.model.CapturedNotification

/**
 * Parses an Android [StatusBarNotification] and [NotificationListenerService.RankingMap]
 * into a pure Kotlin [CapturedNotification] that is detached from Android platform lifecycles.
 */
object NotificationParser {

    fun parse(
        sbn: StatusBarNotification,
        rankingMap: NotificationListenerService.RankingMap? = null,
        appLabel: String? = null
    ): CapturedNotification {
        val notification = sbn.notification
        val extras: Bundle? = notification.extras

        val rawTitle = extras?.getCharSequence(Notification.EXTRA_TITLE)?.toString()
            ?: extras?.getCharSequence(NotificationCompat.EXTRA_TITLE_BIG)?.toString()

        val rawBody = extractBody(extras)
        val rawSubText = extras?.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()

        val senderName = extractSenderName(extras, rawTitle)
        val senderDigits = extractSenderDigits(senderName, rawTitle)

        val channelId = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notification.channelId
        } else {
            null
        }

        val androidCategory = notification.category
        val importance = extractImportance(sbn.key, rankingMap)

        val isOngoing = sbn.isOngoing || ((notification.flags and Notification.FLAG_ONGOING_EVENT) != 0)
        val isForegroundService = (notification.flags and Notification.FLAG_FOREGROUND_SERVICE) != 0
        val isGroupSummary = (notification.flags and Notification.FLAG_GROUP_SUMMARY) != 0
        val isClearable = sbn.isClearable

        val hasProgress = extras?.let {
            it.containsKey(Notification.EXTRA_PROGRESS) && it.getInt(Notification.EXTRA_PROGRESS_MAX, 0) > 0
        } ?: false

        val groupKey = sbn.groupKey

        return CapturedNotification(
            sbnKey = sbn.key,
            packageName = sbn.packageName,
            appLabel = appLabel?.ifBlank { null } ?: sbn.packageName,
            title = rawTitle?.trim()?.ifEmpty { null },
            body = rawBody?.trim()?.ifEmpty { null },
            subText = rawSubText?.trim()?.ifEmpty { null },
            senderName = senderName?.trim()?.ifEmpty { null },
            senderDigits = senderDigits,
            channelId = channelId,
            androidCategory = androidCategory,
            importance = importance,
            postedAt = sbn.postTime,
            isOngoing = isOngoing,
            isForegroundService = isForegroundService,
            isGroupSummary = isGroupSummary,
            isClearable = isClearable,
            hasProgress = hasProgress,
            groupKey = groupKey
        )
    }

    private fun extractBody(extras: Bundle?): String? {
        if (extras == null) return null

        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
        if (!bigText.isNullOrBlank()) {
            return bigText
        }

        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
        if (!text.isNullOrBlank()) {
            return text
        }

        // Handle InboxStyle lines
        val textLines = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)
        if (!textLines.isNullOrEmpty()) {
            return textLines.filterNotNull().joinToString("\n")
        }

        return null
    }

    private fun extractSenderName(extras: Bundle?, fallbackTitle: String?): String? {
        if (extras == null) return fallbackTitle

        // Check for MessagingStyle conversation title or messaging person
        val conversationTitle = extras.getCharSequence(NotificationCompat.EXTRA_CONVERSATION_TITLE)?.toString()
        if (!conversationTitle.isNullOrBlank()) {
            return conversationTitle
        }

        val selfDisplayName = extras.getCharSequence(NotificationCompat.EXTRA_SELF_DISPLAY_NAME)?.toString()
        if (!selfDisplayName.isNullOrBlank()) {
            return selfDisplayName
        }

        return fallbackTitle
    }

    private fun extractSenderDigits(senderName: String?, title: String?): String? {
        val target = when {
            !senderName.isNullOrBlank() && PhoneNormalizer.isPotentialPhoneNumber(senderName) -> senderName
            !title.isNullOrBlank() && PhoneNormalizer.isPotentialPhoneNumber(title) -> title
            else -> senderName ?: title
        }
        return PhoneNormalizer.normalize(target)
    }

    private fun extractImportance(
        key: String,
        rankingMap: NotificationListenerService.RankingMap?
    ): Int {
        if (rankingMap != null) {
            val ranking = NotificationListenerService.Ranking()
            if (rankingMap.getRanking(key, ranking)) {
                return ranking.importance
            }
        }
        return NotificationManager.IMPORTANCE_DEFAULT
    }
}
