package com.emergencysupply.rations.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "history_log")
data class HistoryLogEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: String,
    val action: String,
    val category: String,
    val itemName: String,
    val change: Double,
    val newStock: Double,
    val unit: String,
    val expirationDate: String? = null,
    val notes: String = "",
    val syncPending: Boolean = false
)
