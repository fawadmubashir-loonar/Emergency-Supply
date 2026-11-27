package com.emergencysupply.rations.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.emergencysupply.rations.data.local.dao.GroceryDao
import com.emergencysupply.rations.data.local.dao.HistoryDao
import com.emergencysupply.rations.data.local.dao.InventoryDao
import com.emergencysupply.rations.data.local.dao.SettingsDao
import com.emergencysupply.rations.data.local.dao.SyncDao
import com.emergencysupply.rations.data.local.entity.AppSettingsEntity
import com.emergencysupply.rations.data.local.entity.GroceryItem
import com.emergencysupply.rations.data.local.entity.HistoryLogEntry
import com.emergencysupply.rations.data.local.entity.InventoryItem
import com.emergencysupply.rations.data.local.entity.PendingSyncAction

@Database(
    entities = [
        InventoryItem::class,
        GroceryItem::class,
        HistoryLogEntry::class,
        AppSettingsEntity::class,
        PendingSyncAction::class
    ],
    version = 1,
    exportSchema = false
)
abstract class RationsDatabase : RoomDatabase() {
    abstract fun inventoryDao(): InventoryDao
    abstract fun groceryDao(): GroceryDao
    abstract fun historyDao(): HistoryDao
    abstract fun settingsDao(): SettingsDao
    abstract fun syncDao(): SyncDao

    companion object {
        @Volatile
        private var INSTANCE: RationsDatabase? = null

        fun getInstance(context: Context): RationsDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    RationsDatabase::class.java,
                    "emergency_rations.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
