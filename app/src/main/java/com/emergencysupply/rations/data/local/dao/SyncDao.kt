package com.emergencysupply.rations.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.emergencysupply.rations.data.local.entity.PendingSyncAction

@Dao
interface SyncDao {
    @Query("SELECT * FROM pending_sync_actions ORDER BY createdAt ASC")
    suspend fun getAllPendingActions(): List<PendingSyncAction>

    @Query("SELECT COUNT(*) FROM pending_sync_actions")
    suspend fun getPendingCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAction(action: PendingSyncAction): Long

    @Query("DELETE FROM pending_sync_actions WHERE id = :id")
    suspend fun deleteAction(id: Long)

    @Query("DELETE FROM pending_sync_actions")
    suspend fun clearAll()
}
