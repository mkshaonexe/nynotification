package com.quietinbox.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "muted_apps")
data class MutedAppEntity(
    @PrimaryKey
    val packageName: String,
    val mutedAt: Long,
)
