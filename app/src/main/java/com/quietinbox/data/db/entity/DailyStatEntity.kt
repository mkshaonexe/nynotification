package com.quietinbox.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_stats")
data class DailyStatEntity(
    @PrimaryKey
    val day: Int, // yyyymmdd
    val captured: Int,
    val silenced: Int,
    val allowed: Int,
    val quietMinutes: Int,
    val distinctApps: Int,
)
