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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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

    private val syncMutex = Mutex()

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
        val stockOut = item.stockOut + delta
        val restockNeeded = maxOf(0.0, item.parLevel - newQty)
        val stockStatus = if (newQty < item.parLevel) "LOW" else "OK"
        val totalServings = newQty * item.servingsPerUnit

        val updatedItem = item.copy(
            currentQty = newQty,
            stockOut = stockOut,
            restockNeeded = restockNeeded,
            stockStatus = stockStatus,
            totalServings = totalServings,
            lastUpdated = now,
            syncPending = true
        )
        inventoryDao.updateItem(updatedItem)

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
        val stockIn = item.stockIn + delta
        val restockNeeded = maxOf(0.0, item.parLevel - newQty)
        val stockStatus = if (newQty < item.parLevel) "LOW" else "OK"
        val totalServings = newQty * item.servingsPerUnit

        val updatedItem = item.copy(
            currentQty = newQty,
            stockIn = stockIn,
            restockNeeded = restockNeeded,
            stockStatus = stockStatus,
            totalServings = totalServings,
            lastUpdated = now,
            syncPending = true
        )
        inventoryDao.updateItem(updatedItem)

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
        val restockNeeded = maxOf(0.0, item.parLevel - item.currentQty)
        val stockStatus = if (item.currentQty < item.parLevel) "LOW" else "OK"
        val totalServings = item.currentQty * item.servingsPerUnit
        val newItem = item.copy(
            restockNeeded = restockNeeded,
            stockStatus = stockStatus,
            totalServings = totalServings,
            lastUpdated = now,
            syncPending = true
        )
        inventoryDao.insertItem(newItem)

        val historyEntry = HistoryLogEntry(
            timestamp = now,
            action = "ADD",
            category = newItem.category,
            itemName = newItem.name,
            change = newItem.currentQty,
            newStock = newItem.currentQty,
            unit = newItem.unit,
            expirationDate = newItem.earliestExpiry,
            notes = "Added new item by $actor (Android)",
            syncPending = true
        )
        historyDao.insert(historyEntry)

        syncDao.insertAction(
            PendingSyncAction(
                actionType = "ADD_ITEM",
                targetId = newItem.id,
                payloadJson = "{\"name\": \"${newItem.name}\"}"
            )
        )

        if (account != null) {
            triggerBackgroundSync(account)
        }
    }

    suspend fun updateInventoryItem(item: InventoryItem, actor: String, account: GoogleSignInAccount?) {
        val now = dateFormat.format(Date())
        val restockNeeded = maxOf(0.0, item.parLevel - item.currentQty)
        val stockStatus = if (item.currentQty < item.parLevel) "LOW" else "OK"
        val totalServings = item.currentQty * item.servingsPerUnit
        val updated = item.copy(
            restockNeeded = restockNeeded,
            stockStatus = stockStatus,
            totalServings = totalServings,
            lastUpdated = now,
            syncPending = true
        )
        inventoryDao.updateItem(updated)

        historyDao.insert(
            HistoryLogEntry(
                timestamp = now,
                action = "EDIT",
                category = updated.category,
                itemName = updated.name,
                change = 0.0,
                newStock = updated.currentQty,
                unit = updated.unit,
                expirationDate = updated.earliestExpiry,
                notes = "Updated settings by $actor (Android)",
                syncPending = true
            )
        )

        syncDao.insertAction(
            PendingSyncAction(
                actionType = "UPDATE_ITEM",
                targetId = updated.id,
                payloadJson = "{\"name\": \"${updated.name}\"}"
            )
        )

        if (account != null) {
            triggerBackgroundSync(account)
        }
    }

    suspend fun deleteInventoryItem(itemId: String, actor: String, account: GoogleSignInAccount?) {
        val existing = inventoryDao.getItemById(itemId)
        val now = dateFormat.format(Date())
        inventoryDao.deleteById(itemId)

        if (existing != null) {
            historyDao.insert(
                HistoryLogEntry(
                    timestamp = now,
                    action = "DELETE",
                    category = existing.category,
                    itemName = existing.name,
                    change = -existing.currentQty,
                    newStock = 0.0,
                    unit = existing.unit,
                    expirationDate = existing.earliestExpiry,
                    notes = "Deleted by $actor (Android)",
                    syncPending = true
                )
            )
        }

        syncDao.insertAction(
            PendingSyncAction(
                actionType = "DELETE_ITEM",
                targetId = itemId,
                payloadJson = "{}"
            )
        )

        if (account != null) {
            triggerBackgroundSync(account)
        }
    }

    suspend fun restockFromGroceryItem(grocery: GroceryItem, actor: String, account: GoogleSignInAccount?) {
        toggleGroceryChecked(grocery.id, true, null)
        val allItems = inventoryDao.getAllItemsOnce()
        val match = allItems.firstOrNull { it.name.equals(grocery.name, ignoreCase = true) }
        if (match != null) {
            restockItem(match.id, grocery.qty, actor, account)
        } else {
            val newItem = InventoryItem(
                id = "RATION-" + java.util.UUID.randomUUID().toString().take(6).uppercase(),
                name = grocery.name,
                category = grocery.aisle.ifBlank { "Canned Goods & Produce" },
                unit = grocery.unit.ifBlank { "packs" },
                location = "Main Pantry",
                initialQty = grocery.qty,
                currentQty = grocery.qty,
                parLevel = maxOf(grocery.qty, 2.0),
                servingsPerUnit = 1.0,
                notes = "Added from Groceries ($actor)"
            )
            addInventoryItem(newItem, actor, account)
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
        val targetId: String
        if (existing != null) {
            val updated = existing.copy(
                qty = existing.qty + qty,
                updatedAt = now,
                syncPending = true
            )
            groceryDao.insertOrUpdate(updated)
            targetId = existing.id
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
            targetId = newItem.id
        }

        syncDao.insertAction(
            PendingSyncAction(
                actionType = "ADD_GROCERY",
                targetId = targetId,
                payloadJson = "{\"name\": \"$name\", \"qty\": $qty}"
            )
        )

        if (account != null) {
            triggerBackgroundSync(account)
        }
    }

    suspend fun toggleGroceryChecked(id: String, checked: Boolean, account: GoogleSignInAccount?) {
        val now = dateFormat.format(Date())
        groceryDao.toggleChecked(id, checked, now)
        syncDao.insertAction(
            PendingSyncAction(
                actionType = "TOGGLE_GROCERY",
                targetId = id,
                payloadJson = "{\"checked\": $checked}"
            )
        )
        if (account != null) {
            triggerBackgroundSync(account)
        }
    }

    suspend fun toggleGroceryFavorite(id: String, favorite: Boolean, account: GoogleSignInAccount?) {
        val now = dateFormat.format(Date())
        groceryDao.toggleFavorite(id, favorite, now)
        syncDao.insertAction(
            PendingSyncAction(
                actionType = "TOGGLE_GROCERY",
                targetId = id,
                payloadJson = "{\"favorite\": $favorite}"
            )
        )
        if (account != null) {
            triggerBackgroundSync(account)
        }
    }

    suspend fun deleteGrocery(id: String, account: GoogleSignInAccount?) {
        groceryDao.deleteById(id)
        syncDao.insertAction(
            PendingSyncAction(
                actionType = "DELETE_GROCERY",
                targetId = id,
                payloadJson = "{}"
            )
        )
        if (account != null) {
            triggerBackgroundSync(account)
        }
    }

    suspend fun clearCheckedGroceries(account: GoogleSignInAccount?) {
        groceryDao.clearChecked()
        syncDao.insertAction(
            PendingSyncAction(
                actionType = "CLEAR_GROCERIES",
                targetId = "all_checked",
                payloadJson = "{}"
            )
        )
        if (account != null) {
            triggerBackgroundSync(account)
        }
    }

    suspend fun syncWithGoogleSheets(account: GoogleSignInAccount) {
        syncMutex.withLock {
            _syncState.value = SyncState.Syncing
            try {
                // 1. Flush any local pending writes first
                pushPendingActionsLocked(account)

                // 2. Single-request batchGet for all 4 sheets (fast ~1s pull!)
                val remainingPending = syncDao.getPendingCount()
                if (remainingPending == 0) {
                    val snapshot = sheetsClient.fetchAllSheetsBatch(account)
                    if (snapshot.inventory.isNotEmpty()) {
                        inventoryDao.insertAll(snapshot.inventory)
                    }
                    groceryDao.clearAll()
                    if (snapshot.groceries.isNotEmpty()) {
                        groceryDao.insertAll(snapshot.groceries)
                    }
                    if (snapshot.history.isNotEmpty()) {
                        historyDao.insertAll(snapshot.history)
                    }
                    settingsDao.saveSettings(snapshot.settings)
                }

                _syncState.value = SyncState.Success("Synced with Google Sheet", System.currentTimeMillis())
            } catch (e: Exception) {
                _syncState.value = SyncState.Error(e.message ?: "Sync failed. Working in offline mode.")
            }
        }
    }

    private suspend fun pushPendingActionsLocked(account: GoogleSignInAccount) {
        val pendingActions = syncDao.getAllPendingActions()
        if (pendingActions.isNotEmpty()) {
            val groceryActionTypes = setOf("ADD_GROCERY", "TOGGLE_GROCERY", "DELETE_GROCERY", "CLEAR_GROCERIES")
            val groceryActions = pendingActions.filter { it.actionType in groceryActionTypes }
            if (groceryActions.isNotEmpty()) {
                val localGroceries = groceryDao.getAllGroceriesOnce()
                sheetsClient.rewriteGroceriesTab(account, localGroceries)
                for (item in localGroceries) {
                    if (item.syncPending) {
                        groceryDao.update(item.copy(syncPending = false))
                    }
                }
                for (action in groceryActions) {
                    syncDao.deleteAction(action.id)
                }
            }

            val inventoryActionTypes = setOf("USE_ITEM", "RESTOCK_ITEM", "ADD_ITEM", "UPDATE_ITEM", "DELETE_ITEM")
            val inventoryActions = pendingActions.filter { it.actionType in inventoryActionTypes }
            val processedTargets = mutableSetOf<String>()
            for (action in inventoryActions) {
                if (action.actionType == "DELETE_ITEM") {
                    sheetsClient.deleteInventoryRow(account, action.targetId)
                } else if (processedTargets.add(action.targetId)) {
                    val item = inventoryDao.getItemById(action.targetId)
                    if (item != null) {
                        sheetsClient.upsertInventoryItem(account, item)
                        inventoryDao.updateItem(item.copy(syncPending = false))
                    }
                }
                syncDao.deleteAction(action.id)
            }
        }

        val remainingHistory = historyDao.getPendingEntries()
        for (entry in remainingHistory) {
            sheetsClient.appendHistoryEntry(account, entry)
            historyDao.markSynced(entry.id)
        }
    }

    private fun triggerBackgroundSync(account: GoogleSignInAccount) {
        externalScope.launch(Dispatchers.IO) {
            syncMutex.withLock {
                try {
                    val pending = syncDao.getAllPendingActions()
                    if (pending.isNotEmpty()) {
                        pushPendingActionsLocked(account)
                        _syncState.value = SyncState.Success("Saved to Google Sheet", System.currentTimeMillis())
                    }
                } catch (_: Exception) {}
            }
        }
    }
}
