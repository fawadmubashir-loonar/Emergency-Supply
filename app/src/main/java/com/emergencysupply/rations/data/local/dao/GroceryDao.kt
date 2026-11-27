package com.emergencysupply.rations.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.emergencysupply.rations.data.local.entity.GroceryItem
import kotlinx.coroutines.flow.Flow

@Dao
interface GroceryDao {
    @Query("SELECT * FROM groceries ORDER BY checked ASC, store ASC, name ASC")
    fun getAllGroceries(): Flow<List<GroceryItem>>

    @Query("SELECT * FROM groceries WHERE checked = 0 ORDER BY store ASC, name ASC")
    fun getUncheckedGroceries(): Flow<List<GroceryItem>>

    @Query("SELECT * FROM groceries WHERE id = :id LIMIT 1")
    suspend fun getGroceryById(id: String): GroceryItem?

    @Query("SELECT * FROM groceries WHERE LOWER(name) = LOWER(:name) AND checked = 0 LIMIT 1")
    suspend fun findUncheckedByName(name: String): GroceryItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<GroceryItem>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(item: GroceryItem)

    @Update
    suspend fun update(item: GroceryItem)

    @Query("UPDATE groceries SET checked = :checked, updatedAt = :updatedAt, syncPending = 1 WHERE id = :id")
    suspend fun toggleChecked(id: String, checked: Boolean, updatedAt: String)

    @Query("UPDATE groceries SET favorite = :favorite, updatedAt = :updatedAt, syncPending = 1 WHERE id = :id")
    suspend fun toggleFavorite(id: String, favorite: Boolean, updatedAt: String)

    @Query("DELETE FROM groceries WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM groceries WHERE checked = 1")
    suspend fun clearChecked()

    @Query("DELETE FROM groceries")
    suspend fun clearAll()
}
