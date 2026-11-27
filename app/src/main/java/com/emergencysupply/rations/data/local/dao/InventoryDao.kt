package com.emergencysupply.rations.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.emergencysupply.rations.data.local.entity.InventoryItem
import kotlinx.coroutines.flow.Flow

@Dao
interface InventoryDao {
    @Query("SELECT * FROM master_inventory ORDER BY name ASC")
    fun getAllItems(): Flow<List<InventoryItem>>

    @Query("SELECT * FROM master_inventory WHERE id = :id LIMIT 1")
    suspend fun getItemById(id: String): InventoryItem?

    @Query("SELECT * FROM master_inventory WHERE daysUntilExpiry IS NOT NULL AND daysUntilExpiry >= 0 ORDER BY daysUntilExpiry ASC LIMIT 10")
    fun getEarliestExpiringItems(): Flow<List<InventoryItem>>

    @Query("SELECT * FROM master_inventory WHERE currentQty < parLevel OR stockStatus = 'LOW' ORDER BY (currentQty / CASE WHEN parLevel > 0 THEN parLevel ELSE 1.0 END) ASC")
    fun getRestockNeededItems(): Flow<List<InventoryItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<InventoryItem>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: InventoryItem)

    @Update
    suspend fun updateItem(item: InventoryItem)

    @Query("UPDATE master_inventory SET currentQty = :newQty, stockOut = stockOut + :usedDelta, lastUpdated = :timestamp, syncPending = 1 WHERE id = :itemId")
    suspend fun applyUseItem(itemId: String, newQty: Double, usedDelta: Double, timestamp: String)

    @Query("UPDATE master_inventory SET currentQty = :newQty, stockIn = stockIn + :addedDelta, lastUpdated = :timestamp, syncPending = 1 WHERE id = :itemId")
    suspend fun applyRestockItem(itemId: String, newQty: Double, addedDelta: Double, timestamp: String)

    @Query("DELETE FROM master_inventory")
    suspend fun clearAll()
}
