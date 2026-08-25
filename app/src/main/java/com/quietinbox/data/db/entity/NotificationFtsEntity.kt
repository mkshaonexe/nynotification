package com.quietinbox.data.db.entity

import androidx.room.Entity
import androidx.room.Fts4

@Entity(tableName = "notifications_fts")
@Fts4(contentEntity = NotificationEntity::class)
data class NotificationFtsEntity(
    val title: String?,
    val body: String?,
    val appLabel: String,
    val senderName: String?,
)
