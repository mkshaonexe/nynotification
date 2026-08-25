package com.quietinbox.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "allow_rules")
data class AllowRuleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val type: String, // APP | SENDER | WORD
    val value: String,
    val matchMode: String, // EXACT | CONTAINS | DIGITS
    val enabled: Boolean = true,
    val createdAt: Long,
)
