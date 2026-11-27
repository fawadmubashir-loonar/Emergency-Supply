package com.emergencysupply.rations.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "master_inventory")
data class InventoryItem(
    @PrimaryKey val id: String,
    val name: String,
    val category: String,
    val unit: String = "",
    val location: String = "",
    val initialQty: Double = 0.0,
    val stockIn: Double = 0.0,
    val stockOut: Double = 0.0,
    val currentQty: Double = 0.0,
    val parLevel: Double = 0.0,
    val restockNeeded: Double = 0.0,
    val earliestExpiry: String? = null,
    val daysUntilExpiry: Int? = null,
    val expirationStatus: String = "UNKNOWN", // EXPIRED, SOON, GOOD, UNKNOWN
    val stockStatus: String = "OK",           // LOW, OK, OVER
    val servingsPerUnit: Double = 1.0,
    val totalServings: Double = 0.0,
    val notes: String = "",
    val lastUpdated: String = "",
    val syncPending: Boolean = false
) {
    val isLowStock: Boolean
        get() = currentQty < parLevel || stockStatus.equals("LOW", ignoreCase = true)

    val isExpiringSoon: Boolean
        get() = (daysUntilExpiry != null && daysUntilExpiry in 0..180) ||
                expirationStatus.equals("SOON", ignoreCase = true)

    val isExpired: Boolean
        get() = (daysUntilExpiry != null && daysUntilExpiry < 0) ||
                expirationStatus.equals("EXPIRED", ignoreCase = true)
}
