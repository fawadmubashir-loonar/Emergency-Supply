package com.emergencysupply.rations.ui

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.emergencysupply.rations.data.PreplannedCatalog
import com.emergencysupply.rations.data.auth.GoogleAuthManager
import com.emergencysupply.rations.data.local.entity.AppSettingsEntity
import com.emergencysupply.rations.data.local.entity.GroceryItem
import com.emergencysupply.rations.data.local.entity.HistoryLogEntry
import com.emergencysupply.rations.data.local.entity.InventoryItem
import com.emergencysupply.rations.data.repository.RationsRepository
import com.emergencysupply.rations.data.repository.SyncState
import com.emergencysupply.rations.updater.GitHubAsset
import com.emergencysupply.rations.updater.GitHubSelfUpdater
import com.emergencysupply.rations.updater.UpdateStatus
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class NavTab(val title: String) {
    DASHBOARD("Dashboard"),
    INVENTORY("Inventory"),
    GROCERIES("Groceries"),
    SETTINGS("Settings")
}

data class GroupSummary(
    val name: String,
    val itemCount: Int,
    val lowStockCount: Int,
    val readinessPct: Int
)

data class DashboardMetrics(
    val totalUniqueItems: Int = 0,
    val totalUnitsInStock: Double = 0.0,
    val totalServings: Double = 0.0,
    val estDaysOfSupply: Int = 0,
    val expiringSoonCount: Int = 0,
    val restockNeededCount: Int = 0,
    val parReadinessScore: Int = 0
)

class MainViewModel(
    private val repository: RationsRepository,
    private val authManager: GoogleAuthManager,
    private val updater: GitHubSelfUpdater
) : ViewModel() {

    val currentTab = MutableStateFlow(NavTab.DASHBOARD)
    val syncState: StateFlow<SyncState> = repository.syncState
    val updateStatus: StateFlow<UpdateStatus> = updater.status
    val currentAccount: StateFlow<GoogleSignInAccount?> = authManager.currentAccount
    val signInError = MutableStateFlow<String?>(null)

    fun setSignInError(error: String?) {
        signInError.value = error
    }

    val allInventory = repository.allInventory.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val earliestExpiring = repository.earliestExpiring.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val restockNeeded = repository.restockNeeded.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val allGroceries = repository.allGroceries.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val recentHistory = repository.recentHistory.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val settings = repository.settings.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        null
    )

    // Filters
    val searchQuery = MutableStateFlow("")
    val categoryFilter = MutableStateFlow("All")
    val locationFilter = MutableStateFlow("All")
    val lowStockOnlyFilter = MutableStateFlow(false)
    val expiringOnlyFilter = MutableStateFlow(false)
    val groceryStoreFilter = MutableStateFlow("All Stores")
    val favoritesOnlyFilter = MutableStateFlow(false)

    private val isForeground = MutableStateFlow(true)
    private var lastForegroundSyncMs = 0L

    fun onAppForeground() {
        isForeground.value = true
        val account = currentAccount.value
        val now = System.currentTimeMillis()
        if (account != null && now - lastForegroundSyncMs > 15_000L) {
            lastForegroundSyncMs = now
            viewModelScope.launch {
                repository.syncWithGoogleSheets(account)
            }
        }
    }

    fun onAppBackground() {
        isForeground.value = false
    }

    private val customCategories = MutableStateFlow<List<String>>(emptyList())
    private val customLocations = MutableStateFlow<List<String>>(emptyList())

    val availableCategories: StateFlow<List<String>> = combine(
        allInventory,
        customCategories
    ) { items, custom ->
        val seen = LinkedHashMap<String, String>()
        for (c in PreplannedCatalog.defaultCategoryNames) seen.putIfAbsent(c.lowercase(), c)
        for (c in custom) if (c.isNotBlank()) seen.putIfAbsent(c.lowercase(), c.trim())
        for (item in items) {
            val cat = item.category.trim()
            if (cat.isNotEmpty()) seen.putIfAbsent(cat.lowercase(), cat)
        }
        seen.values.toList()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PreplannedCatalog.defaultCategoryNames)

    val availableLocations: StateFlow<List<String>> = combine(
        allInventory,
        customLocations
    ) { items, custom ->
        val seen = LinkedHashMap<String, String>()
        for (l in PreplannedCatalog.standardLocations) seen.putIfAbsent(l.lowercase(), l)
        for (l in custom) if (l.isNotBlank()) seen.putIfAbsent(l.lowercase(), l.trim())
        for (item in items) {
            val loc = item.location.trim()
            if (loc.isNotEmpty()) seen.putIfAbsent(loc.lowercase(), loc)
        }
        seen.values.toList()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PreplannedCatalog.standardLocations)

    val filteredInventory: StateFlow<List<InventoryItem>> = combine(
        allInventory,
        searchQuery,
        categoryFilter,
        locationFilter,
        combine(lowStockOnlyFilter, expiringOnlyFilter) { a, b -> a to b }
    ) { items, query, cat, loc, flags ->
        val (onlyLow, onlyExp) = flags
        items.filter { item ->
            val matchesQuery = query.isEmpty() ||
                    item.name.contains(query, ignoreCase = true) ||
                    item.category.contains(query, ignoreCase = true) ||
                    item.location.contains(query, ignoreCase = true)
            val matchesCat = cat == "All" || item.category.equals(cat, ignoreCase = true)
            val matchesLoc = loc == "All" || item.location.equals(loc, ignoreCase = true)
            val matchesLow = !onlyLow || item.isLowStock
            val matchesExp = !onlyExp || item.isExpiringSoon || item.isExpired
            matchesQuery && matchesCat && matchesLoc && matchesLow && matchesExp
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val dashboardMetrics: StateFlow<DashboardMetrics> = combine(
        allInventory,
        settings
    ) { items, currentSettings ->
        val unique = items.size
        var units = 0.0
        var servings = 0.0
        var expiringSoon = 0
        var restockNeeded = 0
        var parCappedSum = 0.0
        var parTotalSum = 0.0

        for (item in items) {
            units += item.currentQty
            servings += item.totalServings
            if (item.isExpiringSoon) expiringSoon++
            if (item.isLowStock) restockNeeded++
            if (item.parLevel > 0) {
                parCappedSum += minOf(item.currentQty, item.parLevel)
                parTotalSum += item.parLevel
            }
        }

        val household = currentSettings?.householdSize ?: 5
        val estDays = if (household > 0 && servings > 0) {
            (servings / (household * 3.0)).toInt()
        } else 0

        val parScore = if (parTotalSum > 0) {
            ((parCappedSum / parTotalSum) * 100.0).toInt().coerceIn(0, 100)
        } else if (unique > 0) 100 else 0

        DashboardMetrics(
            totalUniqueItems = unique,
            totalUnitsInStock = units,
            totalServings = servings,
            estDaysOfSupply = estDays,
            expiringSoonCount = expiringSoon,
            restockNeededCount = restockNeeded,
            parReadinessScore = parScore
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardMetrics())

    val categorySummaries: StateFlow<List<GroupSummary>> = allInventory.map { items ->
        items.groupBy { it.category.trim().ifEmpty { "Uncategorized" } }
            .map { (cat, list) ->
                val parSum = list.sumOf { if (it.parLevel > 0) it.parLevel else 0.0 }
                val stockCapped = list.sumOf { if (it.parLevel > 0) minOf(it.currentQty, it.parLevel) else 0.0 }
                val pct = if (parSum > 0) ((stockCapped / parSum) * 100.0).toInt().coerceIn(0, 100) else 100
                GroupSummary(
                    name = cat,
                    itemCount = list.size,
                    lowStockCount = list.count { it.isLowStock },
                    readinessPct = pct
                )
            }
            .sortedByDescending { it.itemCount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val locationSummaries: StateFlow<List<GroupSummary>> = allInventory.map { items ->
        items.groupBy { it.location.trim().ifEmpty { "Unassigned" } }
            .map { (loc, list) ->
                val parSum = list.sumOf { if (it.parLevel > 0) it.parLevel else 0.0 }
                val stockCapped = list.sumOf { if (it.parLevel > 0) minOf(it.currentQty, it.parLevel) else 0.0 }
                val pct = if (parSum > 0) ((stockCapped / parSum) * 100.0).toInt().coerceIn(0, 100) else 100
                GroupSummary(
                    name = loc,
                    itemCount = list.size,
                    lowStockCount = list.count { it.isLowStock },
                    readinessPct = pct
                )
            }
            .sortedByDescending { it.itemCount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // Initial check for updates (throttled)
        viewModelScope.launch {
            updater.checkForUpdates(force = false)
        }

        // Auto-sync on startup if signed in
        viewModelScope.launch {
            currentAccount.collect { account ->
                if (account != null) {
                    lastForegroundSyncMs = System.currentTimeMillis()
                    repository.syncWithGoogleSheets(account)
                }
            }
        }

        // Real-time auto-polling every 45s while in foreground and signed in
        viewModelScope.launch {
            while (true) {
                delay(45_000L)
                val account = currentAccount.value
                if (isForeground.value && account != null && syncState.value !is SyncState.Syncing) {
                    lastForegroundSyncMs = System.currentTimeMillis()
                    repository.syncWithGoogleSheets(account)
                }
            }
        }
    }

    fun syncNow() {
        val account = currentAccount.value ?: return
        viewModelScope.launch {
            repository.syncWithGoogleSheets(account)
        }
    }

    fun clearLocalDataOnSignOut() {
        viewModelScope.launch {
            repository.clearLocalCache()
        }
    }

    fun checkForUpdates(force: Boolean = true) {
        viewModelScope.launch {
            updater.checkForUpdates(force = force)
        }
    }

    fun startDownloadUpdate(activity: Activity, asset: GitHubAsset) {
        viewModelScope.launch {
            updater.downloadAndInstallApk(activity, asset)
        }
    }

    fun dismissUpdate() {
        updater.dismissUpdate()
    }

    fun useItem(itemId: String, delta: Double = 1.0) {
        val actor = currentAccount.value?.displayName ?: "Family Member"
        viewModelScope.launch {
            repository.useItem(itemId, delta, actor, currentAccount.value)
        }
    }

    fun restockItem(itemId: String, delta: Double = 1.0) {
        val actor = currentAccount.value?.displayName ?: "Family Member"
        viewModelScope.launch {
            repository.restockItem(itemId, delta, actor, currentAccount.value)
        }
    }

    fun addNewItem(item: InventoryItem) {
        val actor = currentAccount.value?.displayName ?: "Family Member"
        viewModelScope.launch {
            repository.addInventoryItem(item, actor, currentAccount.value)
        }
    }

    fun updateItem(item: InventoryItem) {
        val actor = currentAccount.value?.displayName ?: "Family Member"
        viewModelScope.launch {
            repository.updateInventoryItem(item, actor, currentAccount.value)
        }
    }

    fun deleteItem(itemId: String) {
        val actor = currentAccount.value?.displayName ?: "Family Member"
        viewModelScope.launch {
            repository.deleteInventoryItem(itemId, actor, currentAccount.value)
        }
    }

    fun addCustomCategory(name: String) {
        val trimmed = name.trim()
        if (trimmed.isNotEmpty()) {
            customCategories.value = customCategories.value + trimmed
        }
    }

    fun addCustomLocation(name: String) {
        val trimmed = name.trim()
        if (trimmed.isNotEmpty()) {
            customLocations.value = customLocations.value + trimmed
        }
    }

    fun openFilteredInventory(
        lowStockOnly: Boolean = false,
        expiringOnly: Boolean = false,
        category: String = "All",
        location: String = "All"
    ) {
        searchQuery.value = ""
        categoryFilter.value = category
        locationFilter.value = location
        lowStockOnlyFilter.value = lowStockOnly
        expiringOnlyFilter.value = expiringOnly
        currentTab.value = NavTab.INVENTORY
    }

    fun moveGroceryToPantry(groceryItem: GroceryItem) {
        val actor = currentAccount.value?.displayName ?: "Family Member"
        viewModelScope.launch {
            repository.restockFromGroceryItem(groceryItem, actor, currentAccount.value)
        }
    }

    fun addGroceryItem(name: String, qty: Double, unit: String, aisle: String, store: String) {
        val actor = currentAccount.value?.displayName ?: "Family Member"
        val resolvedAisle = aisle.ifBlank { PreplannedCatalog.aisleForItem(name, "") }
        viewModelScope.launch {
            repository.addGroceryItem(name, qty, unit, resolvedAisle, store, actor, currentAccount.value)
        }
    }

    fun bumpOrUncheckGrocery(existingId: String, addQty: Double = 1.0) {
        viewModelScope.launch {
            repository.bumpOrUncheckGrocery(existingId, addQty, currentAccount.value)
        }
    }

    fun finishShoppingTrip(checkedItems: List<GroceryItem>) {
        val actor = currentAccount.value?.displayName ?: "Family Member"
        viewModelScope.launch {
            repository.finishShoppingTrip(checkedItems, actor, currentAccount.value)
        }
    }

    fun buildShareableGroceryText(listFilter: String, items: List<GroceryItem>): String {
        val unchecked = items.filter { !it.checked }
        if (unchecked.isEmpty()) {
            return "🛒 $listFilter\n\n(No items to buy)"
        }
        val grouped = unchecked.groupBy { item ->
            item.aisle.ifBlank { PreplannedCatalog.aisleForItem(item.name, "") }.ifBlank { "General" }
        }
        val sb = StringBuilder()
        sb.append("🛒 ").append(listFilter)
        for ((aisle, aisleItems) in grouped) {
            sb.append("\n\n")
            val icon = PreplannedCatalog.iconForCategory(aisle)
            val header = if (aisle.startsWith(icon) || aisle.any { Character.isSurrogate(it) }) {
                aisle
            } else {
                "$icon $aisle"
            }
            sb.append(header)
            for (item in aisleItems) {
                val qtyStr = if (item.qty == item.qty.toLong().toDouble()) {
                    item.qty.toLong().toString()
                } else {
                    String.format(java.util.Locale.US, "%.1f", item.qty)
                }
                val unitStr = item.unit.trim()
                val qtyWithUnit = if (unitStr.isNotEmpty()) "$qtyStr $unitStr" else qtyStr
                val detail = if (item.qty > 0) " ($qtyWithUnit)" else ""
                sb.append("\n• ").append(item.name).append(detail)
            }
        }
        return sb.toString()
    }

    fun toggleGroceryChecked(id: String, checked: Boolean) {
        viewModelScope.launch {
            repository.toggleGroceryChecked(id, checked, currentAccount.value)
        }
    }

    fun toggleGroceryFavorite(id: String, favorite: Boolean) {
        viewModelScope.launch {
            repository.toggleGroceryFavorite(id, favorite, currentAccount.value)
        }
    }

    fun deleteGrocery(id: String) {
        viewModelScope.launch {
            repository.deleteGrocery(id, currentAccount.value)
        }
    }

    fun clearCheckedGroceries() {
        viewModelScope.launch {
            repository.clearCheckedGroceries(currentAccount.value)
        }
    }

    fun addRestockItemToGroceries(item: InventoryItem) {
        val actor = currentAccount.value?.displayName ?: "Family Member"
        val needQty = if (item.parLevel > item.currentQty) (item.parLevel - item.currentQty) else 1.0
        val store = guessStoreForCategory(item.category)
        viewModelScope.launch {
            repository.addGroceryItem(
                name = item.name,
                qty = needQty,
                unit = item.unit,
                aisle = item.category,
                store = store,
                actor = actor,
                account = currentAccount.value
            )
        }
    }

    private fun guessStoreForCategory(category: String): String {
        val lower = category.lowercase()
        return when {
            lower.contains("meat") || lower.contains("protein") || lower.contains("halal") -> "Halal Market"
            lower.contains("bulk") || lower.contains("water") || lower.contains("grain") -> "Costco"
            lower.contains("baby") || lower.contains("aid") || lower.contains("soap") -> "Target"
            else -> "Grocery Store"
        }
    }
}

class MainViewModelFactory(
    private val repository: RationsRepository,
    private val authManager: GoogleAuthManager,
    private val updater: GitHubSelfUpdater
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return MainViewModel(repository, authManager, updater) as T
    }
}
