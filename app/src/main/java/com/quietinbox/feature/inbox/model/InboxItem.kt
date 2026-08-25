package com.quietinbox.feature.inbox.model

import com.quietinbox.core.firewall.FirewallAction
import com.quietinbox.core.model.SignalClass

/**
 * Filter options for the inbox state selector.
 */
enum class InboxStateFilter {
    ALL,
    UNSEEN,
    SEEN,
    STARRED
}

/**
 * Sealed interface representing items in the Inbox feed.
 */
sealed interface InboxItem {

    /**
     * Sticky day header separating notifications by local calendar day.
     */
    data class Header(
        val title: String,
        val epochDay: Long
    ) : InboxItem

    /**
     * Notification row item.
     */
    data class Row(
        val id: Long,
        val sbnKey: String,
        val packageName: String,
        val appLabel: String,
        val title: String?,
        val body: String?,
        val subText: String?,
        val senderName: String?,
        val senderDigits: String?,
        val channelId: String?,
        val androidCategory: String?,
        val importance: Int?,
        val signalClass: SignalClass,
        val contentHash: String,
        val firstSeenAt: Long,
        val lastSeenAt: Long,
        val endedAt: Long?,
        val updateCount: Int,
        val wasRateLimited: Boolean,
        val isSeen: Boolean,
        val isStarred: Boolean,
        val removalReason: Int?,
        val isSilenced: Boolean,
        val firewallAction: FirewallAction?,
        val firewallRuleId: Int?,
        val firewallRuleLabel: String?
    ) : InboxItem {
        val isSession: Boolean
            get() = signalClass == SignalClass.ONGOING ||
                    signalClass == SignalClass.PROGRESS ||
                    signalClass == SignalClass.TRANSPORT ||
                    signalClass == SignalClass.SERVICE
    }
}
