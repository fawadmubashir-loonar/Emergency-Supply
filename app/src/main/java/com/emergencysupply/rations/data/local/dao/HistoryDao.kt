package com.emergencysupply.rations.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.emergencysupply.rations.data.local.entity.HistoryLogEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {
    @Query("SELECT * FROM history_log ORDER BY id DESC LIMIT :limit")
    fun getRecentHistory(limit: Int = 50): Flow<List<HistoryLogEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: HistoryLogEntry)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entries: List<HistoryLogEntry>)

    @Query("DELETE FROM history_log")
    suspend fun clearAll()
}
