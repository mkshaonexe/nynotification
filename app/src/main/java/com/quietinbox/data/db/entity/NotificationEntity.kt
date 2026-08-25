package com.quietinbox.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "notifications",
    indices = [
        Index(value = ["sbnKey"]),
        Index(value = ["packageName"]),
        Index(value = ["senderName"]),
        Index(value = ["senderDigits"]),
        Index(value = ["firstSeenAt"]),
        Index(value = ["isSeen"]),
        Index(value = ["isStarred"]),
        Index(value = ["sbnKey", "contentHash", "firstSeenAt"], unique = true)
    ]
)
data class NotificationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
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
    val importance: Int,
    val signalClass: String,
    val contentHash: String,
    val firstSeenAt: Long,
    val lastSeenAt: Long,
    val endedAt: Long? = null,
    val updateCount: Int = 1,
    val wasRateLimited: Boolean = false,
    val isSeen: Boolean = false,
    val isStarred: Boolean = false,
    val removalReason: Int? = null,
)
