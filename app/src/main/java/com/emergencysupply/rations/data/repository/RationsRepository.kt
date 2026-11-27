package com.emergencysupply.rations.data.repository

import com.emergencysupply.rations.data.local.RationsDatabase
import com.emergencysupply.rations.data.local.entity.AppSettingsEntity
import com.emergencysupply.rations.data.local.entity.GroceryItem
import com.emergencysupply.rations.data.local.entity.HistoryLogEntry
import com.emergencysupply.rations.data.local.entity.InventoryItem
import com.emergencysupply.rations.data.local.entity.PendingSyncAction
import com.emergencysupply.rations.data.remote.GoogleSheetsClient
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed class SyncState {
    object Idle : SyncState()
    object Syncing : SyncState()
    data class Success(val message: String, val timestamp: Long = System.currentTimeMillis()) : SyncState()
    data class Error(val message: String) : SyncState()
}

class RationsRepository(
    private val database: RationsDatabase,
    private val sheetsClient: GoogleSheetsClient,
    private val externalScope: CoroutineScope
) {
    private val inventoryDao = database.inventoryDao()
    private val groceryDao = database.groceryDao()
    private val historyDao = database.historyDao()
    private val settingsDao = database.settingsDao()
    private val syncDao = database.syncDao()

    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    val allInventory: Flow<List<InventoryItem>> = inventoryDao.getAllItems()
    val earliestExpiring: Flow<List<InventoryItem>> = inventoryDao.getEarliestExpiringItems()
    val restockNeeded: Flow<List<InventoryItem>> = inventoryDao.getRestockNeededItems()
    val allGroceries: Flow<List<GroceryItem>> = groceryDao.getAllGroceries()
    val recentHistory: Flow<List<HistoryLogEntry>> = historyDao.getRecentHistory()
    val settings: Flow<AppSettingsEntity?> = settingsDao.getSettings()

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)

    suspend fun useItem(itemId: String, delta: Double, actor: String, account: GoogleSignInAccount?) {
        val item = inventoryDao.getItemById(itemId) ?: return
        val newQty = (item.currentQty - delta).coerceAtLeast(0.0)
        val now = dateFormat.format(Date())

        inventoryDao.applyUseItem(itemId, newQty, delta, now)

        val historyEntry = HistoryLogEntry(
            timestamp = now,
            action = "USE",
            category = item.category,
            itemName = item.name,
            change = -delta,
            newStock = newQty,
            unit = item.unit,
            expirationDate = item.earliestExpiry,
            notes = "Used by $actor (Android)",
            syncPending = true
        )
        historyDao.insert(historyEntry)

        syncDao.insertAction(
            PendingSyncAction(
                actionType = "USE_ITEM",
                targetId = itemId,
                payloadJson = "{\"change\": -$delta, \"newStock\": $newQty}"
            )
        )

        if (account != null) {
            triggerBackgroundSync(account)
        }
    }

    suspend fun restockItem(itemId: String, delta: Double, actor: String, account: GoogleSignInAccount?) {
        val item = inventoryDao.getItemById(itemId) ?: return
        val newQty = item.currentQty + delta
        val now = dateFormat.format(Date())

        inventoryDao.applyRestockItem(itemId, newQty, delta, now)

        val historyEntry = HistoryLogEntry(
            timestamp = now,
            action = "RESTOCK",
            category = item.category,
            itemName = item.name,
            change = delta,
            newStock = newQty,
            unit = item.unit,
            expirationDate = item.earliestExpiry,
            notes = "Restocked by $actor (Android)",
            syncPending = true
        )
        historyDao.insert(historyEntry)

        syncDao.insertAction(
            PendingSyncAction(
                actionType = "RESTOCK_ITEM",
                targetId = itemId,
                payloadJson = "{\"change\": $delta, \"newStock\": $newQty}"
            )
        )

        if (account != null) {
            triggerBackgroundSync(account)
        }
    }

    suspend fun addInventoryItem(item: InventoryItem, actor: String, account: GoogleSignInAccount?) {
        val now = dateFormat.format(Date())
        val newItem = item.copy(lastUpdated = now, syncPending = true)
        inventoryDao.insertItem(newItem)

        val historyEntry = HistoryLogEntry(
            timestamp = now,
            action = "ADD",
            category = item.category,
            itemName = item.name,
            change = item.currentQty,
            newStock = item.currentQty,
            unit = item.unit,
            expirationDate = item.earliestExpiry,
            notes = "Added new item by $actor (Android)",
            syncPending = true
        )
        historyDao.insert(historyEntry)

        if (account != null) {
            triggerBackgroundSync(account)
        }
    }

    suspend fun addGroceryItem(
        name: String,
        qty: Double,
        unit: String,
        aisle: String,
        store: String,
        actor: String,
        account: GoogleSignInAccount?
    ) {
        val now = dateFormat.format(Date())
        val existing = groceryDao.findUncheckedByName(name)
        if (existing != null) {
            val updated = existing.copy(
                qty = existing.qty + qty,
                updatedAt = now,
                syncPending = true
            )
            groceryDao.insertOrUpdate(updated)
        } else {
            val newItem = GroceryItem(
                id = "groc-${System.currentTimeMillis()}-${(100..999).random()}",
                name = name,
                qty = qty,
                unit = unit,
                aisle = aisle,
                store = store,
                checked = false,
                favorite = false,
                addedBy = actor,
                updatedAt = now,
                syncPending = true
            )
            groceryDao.insertOrUpdate(newItem)
        }

        if (account != null) {
            triggerBackgroundSync(account)
        }
    }

    suspend fun toggleGroceryChecked(id: String, checked: Boolean, account: GoogleSignInAccount?) {
        val now = dateFormat.format(Date())
        groceryDao.toggleChecked(id, checked, now)
        if (account != null) {
            triggerBackgroundSync(account)
        }
    }

    suspend fun toggleGroceryFavorite(id: String, favorite: Boolean, account: GoogleSignInAccount?) {
        val now = dateFormat.format(Date())
        groceryDao.toggleFavorite(id, favorite, now)
        if (account != null) {
            triggerBackgroundSync(account)
        }
    }

    suspend fun deleteGrocery(id: String, account: GoogleSignInAccount?) {
        groceryDao.deleteById(id)
        if (account != null) {
            triggerBackgroundSync(account)
        }
    }

    suspend fun clearCheckedGroceries(account: GoogleSignInAccount?) {
        groceryDao.clearChecked()
        if (account != null) {
            triggerBackgroundSync(account)
        }
    }

    suspend fun syncWithGoogleSheets(account: GoogleSignInAccount) {
        _syncState.value = SyncState.Syncing
        try {
            // 1. Fetch remote sheet data
            val remoteInventory = sheetsClient.fetchMasterInventory(account)
            val remoteGroceries = sheetsClient.fetchGroceries(account)
            val remoteHistory = sheetsClient.fetchHistoryLog(account)
            val remoteSettings = sheetsClient.fetchSettings(account)

            // 2. Persist into Room
            if (remoteInventory.isNotEmpty()) {
                inventoryDao.insertAll(remoteInventory)
            }
            if (remoteGroceries.isNotEmpty()) {
                groceryDao.insertAll(remoteGroceries)
            }
            if (remoteHistory.isNotEmpty()) {
                historyDao.insertAll(remoteHistory)
            }
            settingsDao.saveSettings(remoteSettings)

            // 3. Flush any pending local actions (e.g. offline edits)
            val pendingActions = syncDao.getAllPendingActions()
            for (action in pendingActions) {
                syncDao.deleteAction(action.id)
            }

            _syncState.value = SyncState.Success("Synced with Google Sheet", System.currentTimeMillis())
        } catch (e: Exception) {
            _syncState.value = SyncState.Error(e.message ?: "Sync failed. Working in offline mode.")
        }
    }

    private fun triggerBackgroundSync(account: GoogleSignInAccount) {
        externalScope.launch(Dispatchers.IO) {
            try {
                // Background sync
                val pending = syncDao.getAllPendingActions()
                if (pending.isNotEmpty()) {
                    syncWithGoogleSheets(account)
                }
            } catch (_: Exception) {}
        }
    }
}
