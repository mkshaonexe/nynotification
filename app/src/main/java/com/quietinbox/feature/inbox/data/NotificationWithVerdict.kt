package com.quietinbox.feature.inbox.data

/**
 * Composite model representing a notification row joined with its optional firewall decision verdict.
 */
data class NotificationWithVerdict(
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
    val signalClass: String,
    val contentHash: String,
    val firstSeenAt: Long,
    val lastSeenAt: Long,
    val endedAt: Long?,
    val updateCount: Int,
    val wasRateLimited: Boolean,
    val isSeen: Boolean,
    val isStarred: Boolean,
    val removalReason: Int?,
    val firewallAction: String? = null,
    val firewallRuleId: Int? = null,
    val firewallRuleLabel: String? = null
)
