package com.quietinbox.core.model

/** A parsed, engine-ready notification. Free of Android types so it is unit-testable. */
data class CapturedNotification(
    val sbnKey: String,
    val packageName: String,
    val appLabel: String,
    val title: String?,
    val body: String?,
    val subText: String?,
    val senderName: String?,
    val senderDigits: String?,      // digits only, may be null
    val channelId: String?,
    val androidCategory: String?,   // Notification.CATEGORY_*
    val importance: Int,
    val postedAt: Long,
    val isOngoing: Boolean,
    val isForegroundService: Boolean,
    val isGroupSummary: Boolean,
    val isClearable: Boolean,
    val hasProgress: Boolean,
    val groupKey: String?,
)
