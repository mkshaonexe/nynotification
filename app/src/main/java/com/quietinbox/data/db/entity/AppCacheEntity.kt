package com.quietinbox.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_cache")
data class AppCacheEntity(
    @PrimaryKey
    val packageName: String,
    val label: String,
    val isLaunchable: Boolean,
    val isPreinstalled: Boolean,
    val wasUpdated: Boolean,
    val iconUpdatedAt: Long,
)
