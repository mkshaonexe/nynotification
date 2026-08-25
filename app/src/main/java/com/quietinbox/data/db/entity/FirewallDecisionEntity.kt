package com.quietinbox.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "firewall_decisions",
    indices = [
        Index(value = ["at"])
    ]
)
data class FirewallDecisionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val sbnKey: String,
    val action: String, // ALLOW | SILENCE
    val ruleId: Int,
    val ruleLabel: String,
    val at: Long,
)
