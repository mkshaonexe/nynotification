package com.quietinbox.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "schedules")
data class ScheduleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val name: String,
    val startMinute: Int,
    val endMinute: Int,
    val daysMask: Int,
    val policy: String, // QUIET | OPEN
    val enabled: Boolean = true,
    val createdAt: Long,
)
