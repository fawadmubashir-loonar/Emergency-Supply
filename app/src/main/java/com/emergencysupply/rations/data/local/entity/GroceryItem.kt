package com.emergencysupply.rations.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "groceries")
data class GroceryItem(
    @PrimaryKey val id: String,
    val name: String,
    val qty: Double = 1.0,
    val unit: String = "",
    val aisle: String = "",
    val store: String = "Grocery Store",
    val checked: Boolean = false,
    val favorite: Boolean = false,
    val addedBy: String = "",
    val updatedAt: String = "",
    val syncPending: Boolean = false
)
