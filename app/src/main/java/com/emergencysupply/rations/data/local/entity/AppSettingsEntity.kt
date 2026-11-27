package com.emergencysupply.rations.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_settings")
data class AppSettingsEntity(
    @PrimaryKey val key: String = "app_settings",
    val householdSize: Int = 5,
    val adults: Int = 2,
    val kidAgesJson: String = "[9, 5, 0]",
    val categoriesJson: String = "[]",
    val locationsJson: String = "[]",
    val targetYears: Double = 2.0,
    val lowStockPct: Int = 40,
    val useSoonDays: Int = 90,
    val lastSyncedAt: Long = 0L,
    val userEmail: String = ""
)
