package com.emergencysupply.rations.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pending_sync_actions")
data class PendingSyncAction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val actionType: String, // "ADJUST_STOCK", "ADD_ITEM", "ADD_GROCERY", "TOGGLE_GROCERY", "DELETE_GROCERY"
    val targetId: String,
    val payloadJson: String,
    val createdAt: Long = System.currentTimeMillis()
)
