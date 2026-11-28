package com.emergencysupply.rations.data.remote

import android.content.Context
import com.emergencysupply.rations.BuildConfig
import com.emergencysupply.rations.data.local.entity.AppSettingsEntity
import com.emergencysupply.rations.data.local.entity.GroceryItem
import com.emergencysupply.rations.data.local.entity.HistoryLogEntry
import com.emergencysupply.rations.data.local.entity.InventoryItem
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.sheets.v4.Sheets
import com.google.api.services.sheets.v4.SheetsScopes
import com.google.api.services.sheets.v4.model.BatchUpdateSpreadsheetRequest
import com.google.api.services.sheets.v4.model.BatchUpdateValuesRequest
import com.google.api.services.sheets.v4.model.ClearValuesRequest
import com.google.api.services.sheets.v4.model.DeleteDimensionRequest
import com.google.api.services.sheets.v4.model.DimensionRange
import com.google.api.services.sheets.v4.model.Request
import com.google.api.services.sheets.v4.model.ValueRange
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GoogleSheetsClient(private val context: Context) {

    private val spreadsheetId = BuildConfig.SPREADSHEET_ID
    private val httpTransport = NetHttpTransport()
    private val jsonFactory = GsonFactory.getDefaultInstance()

    @Volatile
    private var cachedAccountEmail: String? = null
    @Volatile
    private var cachedService: Sheets? = null

    private val cachedInventoryRowById = java.util.concurrent.ConcurrentHashMap<String, Int>()
    @Volatile
    private var cachedMasterSheetId: Int? = null

    fun clearSessionCache() {
        cachedInventoryRowById.clear()
        cachedMasterSheetId = null
    }

    data class BatchSheetSnapshot(
        val inventory: List<InventoryItem>,
        val groceries: List<GroceryItem>,
        val history: List<HistoryLogEntry>,
        val settings: AppSettingsEntity,
        val historyFetched: Boolean = false
    )

    private fun getSheetsService(account: GoogleSignInAccount): Sheets {
        val email = account.email
        val existing = cachedService
        if (existing != null && cachedAccountEmail == email) {
            return existing
        }
        val credential = GoogleAccountCredential.usingOAuth2(
            context,
            listOf(SheetsScopes.SPREADSHEETS)
        ).apply {
            selectedAccount = account.account
        }

        val created = Sheets.Builder(
            httpTransport,
            jsonFactory,
            credential
        ).setApplicationName("EmergencyFoodRations").build()

        cachedAccountEmail = email
        cachedService = created
        return created
    }

    suspend fun fetchAllSheetsBatch(account: GoogleSignInAccount, includeHistory: Boolean = false): BatchSheetSnapshot = withContext(Dispatchers.IO) {
        val service = getSheetsService(account)
        val rangesToFetch = if (includeHistory) {
            listOf("'Master Inventory'!A2:S", "'Groceries'!A2:J", "'History Log'!A2:I", "'Settings'!A2:B")
        } else {
            listOf("'Master Inventory'!A2:S", "'Groceries'!A2:J", "'Settings'!A2:B")
        }
        try {
            val batchResp = service.spreadsheets().values()
                .batchGet(spreadsheetId)
                .setRanges(rangesToFetch)
                .execute()

            val ranges = batchResp.valueRanges ?: emptyList()
            val invRows = ranges.getOrNull(0)?.getValues() ?: emptyList()
            val grocRows = ranges.getOrNull(1)?.getValues() ?: emptyList()
            val histRows = if (includeHistory) ranges.getOrNull(2)?.getValues() ?: emptyList() else emptyList()
            val settingsRows = if (includeHistory) ranges.getOrNull(3)?.getValues() ?: emptyList() else ranges.getOrNull(2)?.getValues() ?: emptyList()

            BatchSheetSnapshot(
                inventory = parseInventoryRows(invRows),
                groceries = parseGroceryRows(grocRows),
                history = if (includeHistory) parseHistoryRows(histRows) else emptyList(),
                settings = parseSettingsRows(settingsRows, account.email ?: ""),
                historyFetched = includeHistory
            )
        } catch (_: Exception) {
            // Fallback gracefully if one sheet tab name differs
            BatchSheetSnapshot(
                inventory = fetchMasterInventory(account),
                groceries = try { fetchGroceries(account) } catch (_: Exception) { emptyList() },
                history = if (includeHistory) { try { fetchHistoryLog(account) } catch (_: Exception) { emptyList() } } else emptyList(),
                settings = fetchSettings(account),
                historyFetched = includeHistory
            )
        }
    }

    suspend fun fetchMasterInventory(account: GoogleSignInAccount): List<InventoryItem> = withContext(Dispatchers.IO) {
        val service = getSheetsService(account)
        val response: ValueRange = service.spreadsheets().values()
            .get(spreadsheetId, "'Master Inventory'!A2:S")
            .execute()
        parseInventoryRows(response.getValues() ?: emptyList())
    }

    private fun parseInventoryRows(rows: List<List<Any>>): List<InventoryItem> {
        val items = mutableListOf<InventoryItem>()

        for ((index, row) in rows.withIndex()) {
            val id = row.getOrNull(0)?.toString()?.trim() ?: continue
            if (id.isEmpty()) continue
            cachedInventoryRowById[id] = index + 2
            val name = row.getOrNull(1)?.toString()?.trim() ?: continue
            if (name.isEmpty()) continue

            val category = row.getOrNull(2)?.toString()?.trim() ?: "General"
            val unit = row.getOrNull(3)?.toString()?.trim() ?: ""
            val location = row.getOrNull(4)?.toString()?.trim() ?: ""
            val initialQty = parseDouble(row.getOrNull(5))
            val stockIn = parseDouble(row.getOrNull(6))
            val stockOut = parseDouble(row.getOrNull(7))
            val currentQty = parseDouble(row.getOrNull(8))
            val parLevel = parseDouble(row.getOrNull(9))
            val restockNeeded = parseDouble(row.getOrNull(10))
            val earliestExpiry = row.getOrNull(11)?.toString()?.trim()
            val daysUntilExpiry = row.getOrNull(12)?.toString()?.toDoubleOrNull()?.toInt()
            val expStatus = row.getOrNull(13)?.toString()?.trim() ?: "UNKNOWN"
            val stockStatus = row.getOrNull(14)?.toString()?.trim() ?: "OK"
            val servingsPerUnit = parseDouble(row.getOrNull(15), default = 1.0)
            val totalServings = parseDouble(row.getOrNull(16))
            val notes = row.getOrNull(17)?.toString()?.trim() ?: ""
            val lastUpdated = row.getOrNull(18)?.toString()?.trim() ?: ""

            items.add(
                InventoryItem(
                    id = id,
                    name = name,
                    category = category,
                    unit = unit,
                    location = location,
                    initialQty = initialQty,
                    stockIn = stockIn,
                    stockOut = stockOut,
                    currentQty = currentQty,
                    parLevel = parLevel,
                    restockNeeded = restockNeeded,
                    earliestExpiry = earliestExpiry,
                    daysUntilExpiry = daysUntilExpiry,
                    expirationStatus = expStatus,
                    stockStatus = stockStatus,
                    servingsPerUnit = servingsPerUnit,
                    totalServings = totalServings,
                    notes = notes,
                    lastUpdated = lastUpdated,
                    syncPending = false
                )
            )
        }
        return items
    }

    suspend fun upsertInventoryItem(account: GoogleSignInAccount, item: InventoryItem) = withContext(Dispatchers.IO) {
        val service = getSheetsService(account)
        var targetRow: Int? = null
        var isColIFormula = false

        val cachedRow = cachedInventoryRowById[item.id]
        if (cachedRow != null) {
            try {
                val checkResp = service.spreadsheets().values()
                    .get(spreadsheetId, "'Master Inventory'!A$cachedRow:I$cachedRow")
                    .setValueRenderOption("FORMULA")
                    .execute()
                val rowValues = checkResp.getValues()?.firstOrNull()
                if (rowValues?.getOrNull(0)?.toString()?.trim() == item.id) {
                    targetRow = cachedRow
                    val colI = rowValues.getOrNull(8)?.toString()?.trim() ?: ""
                    isColIFormula = colI.startsWith("=")
                } else {
                    cachedInventoryRowById.remove(item.id)
                }
            } catch (_: Exception) {
                cachedInventoryRowById.remove(item.id)
            }
        }

        if (targetRow == null) {
            val response: ValueRange = service.spreadsheets().values()
                .get(spreadsheetId, "'Master Inventory'!A2:S")
                .setValueRenderOption("FORMULA")
                .execute()

            val rows = response.getValues() ?: emptyList()
            val index = rows.indexOfFirst { it.getOrNull(0)?.toString()?.trim() == item.id }

            if (index != -1) {
                targetRow = index + 2
                cachedInventoryRowById[item.id] = targetRow
                val existingRow = rows[index]
                val colI = existingRow.getOrNull(8)?.toString()?.trim() ?: ""
                isColIFormula = colI.startsWith("=")
            }
        }

        if (targetRow != null) {
            val rowNum = targetRow
            if (isColIFormula) {
                val batchData = listOf(
                    ValueRange().setRange("'Master Inventory'!B$rowNum:H$rowNum").setValues(
                        listOf(listOf<Any>(item.name, item.category, item.unit, item.location, item.initialQty, item.stockIn, item.stockOut))
                    ),
                    ValueRange().setRange("'Master Inventory'!J$rowNum").setValues(
                        listOf(listOf<Any>(item.parLevel))
                    ),
                    ValueRange().setRange("'Master Inventory'!L$rowNum").setValues(
                        listOf(listOf<Any>(item.earliestExpiry ?: ""))
                    ),
                    ValueRange().setRange("'Master Inventory'!P$rowNum").setValues(
                        listOf(listOf<Any>(item.servingsPerUnit))
                    ),
                    ValueRange().setRange("'Master Inventory'!R$rowNum:S$rowNum").setValues(
                        listOf(listOf<Any>(item.notes, item.lastUpdated))
                    )
                )
                val batchRequest = BatchUpdateValuesRequest()
                    .setValueInputOption("USER_ENTERED")
                    .setData(batchData)
                service.spreadsheets().values()
                    .batchUpdate(spreadsheetId, batchRequest)
                    .execute()
            } else {
                val fullRow = listOf<Any>(
                    item.id,
                    item.name,
                    item.category,
                    item.unit,
                    item.location,
                    item.initialQty,
                    item.stockIn,
                    item.stockOut,
                    item.currentQty,
                    item.parLevel,
                    item.restockNeeded,
                    item.earliestExpiry ?: "",
                    item.daysUntilExpiry ?: "",
                    item.expirationStatus,
                    item.stockStatus,
                    item.servingsPerUnit,
                    item.totalServings,
                    item.notes,
                    item.lastUpdated
                )
                val body = ValueRange().setValues(listOf(fullRow))
                service.spreadsheets().values()
                    .update(spreadsheetId, "'Master Inventory'!A$rowNum:S$rowNum", body)
                    .setValueInputOption("USER_ENTERED")
                    .execute()
            }
        } else {
            val fullRow = listOf<Any>(
                item.id,
                item.name,
                item.category,
                item.unit,
                item.location,
                item.initialQty,
                item.stockIn,
                item.stockOut,
                item.currentQty,
                item.parLevel,
                item.restockNeeded,
                item.earliestExpiry ?: "",
                item.daysUntilExpiry ?: "",
                item.expirationStatus,
                item.stockStatus,
                item.servingsPerUnit,
                item.totalServings,
                item.notes,
                item.lastUpdated
            )
            val body = ValueRange().setValues(listOf(fullRow))
            service.spreadsheets().values()
                .append(spreadsheetId, "'Master Inventory'!A:S", body)
                .setValueInputOption("USER_ENTERED")
                .execute()
        }
    }

    suspend fun upsertInventoryItemsBatch(account: GoogleSignInAccount, items: List<InventoryItem>) = withContext(Dispatchers.IO) {
        if (items.isEmpty()) return@withContext
        val service = getSheetsService(account)
        val response: ValueRange = service.spreadsheets().values()
            .get(spreadsheetId, "'Master Inventory'!A2:S")
            .setValueRenderOption("FORMULA")
            .execute()
        val rows = response.getValues() ?: emptyList()
        for ((idx, r) in rows.withIndex()) {
            val id = r.getOrNull(0)?.toString()?.trim() ?: continue
            if (id.isNotEmpty()) {
                cachedInventoryRowById[id] = idx + 2
            }
        }

        val batchUpdates = mutableListOf<ValueRange>()
        val rowsToAppend = mutableListOf<List<Any>>()

        for (item in items) {
            val index = rows.indexOfFirst { it.getOrNull(0)?.toString()?.trim() == item.id }
            if (index != -1) {
                val rowNum = index + 2
                val existingRow = rows[index]
                val colI = existingRow.getOrNull(8)?.toString()?.trim() ?: ""
                val isFormula = colI.startsWith("=")
                if (isFormula) {
                    batchUpdates.add(ValueRange().setRange("'Master Inventory'!B$rowNum:H$rowNum").setValues(
                        listOf(listOf<Any>(item.name, item.category, item.unit, item.location, item.initialQty, item.stockIn, item.stockOut))
                    ))
                    batchUpdates.add(ValueRange().setRange("'Master Inventory'!J$rowNum").setValues(
                        listOf(listOf<Any>(item.parLevel))
                    ))
                    batchUpdates.add(ValueRange().setRange("'Master Inventory'!L$rowNum").setValues(
                        listOf(listOf<Any>(item.earliestExpiry ?: ""))
                    ))
                    batchUpdates.add(ValueRange().setRange("'Master Inventory'!P$rowNum").setValues(
                        listOf(listOf<Any>(item.servingsPerUnit))
                    ))
                    batchUpdates.add(ValueRange().setRange("'Master Inventory'!R$rowNum:S$rowNum").setValues(
                        listOf(listOf<Any>(item.notes, item.lastUpdated))
                    ))
                } else {
                    val fullRow = listOf<Any>(
                        item.id,
                        item.name,
                        item.category,
                        item.unit,
                        item.location,
                        item.initialQty,
                        item.stockIn,
                        item.stockOut,
                        item.currentQty,
                        item.parLevel,
                        item.restockNeeded,
                        item.earliestExpiry ?: "",
                        item.daysUntilExpiry ?: "",
                        item.expirationStatus,
                        item.stockStatus,
                        item.servingsPerUnit,
                        item.totalServings,
                        item.notes,
                        item.lastUpdated
                    )
                    batchUpdates.add(ValueRange().setRange("'Master Inventory'!A$rowNum:S$rowNum").setValues(listOf(fullRow)))
                }
            } else {
                val fullRow = listOf<Any>(
                    item.id,
                    item.name,
                    item.category,
                    item.unit,
                    item.location,
                    item.initialQty,
                    item.stockIn,
                    item.stockOut,
                    item.currentQty,
                    item.parLevel,
                    item.restockNeeded,
                    item.earliestExpiry ?: "",
                    item.daysUntilExpiry ?: "",
                    item.expirationStatus,
                    item.stockStatus,
                    item.servingsPerUnit,
                    item.totalServings,
                    item.notes,
                    item.lastUpdated
                )
                rowsToAppend.add(fullRow)
            }
        }

        if (batchUpdates.isNotEmpty()) {
            val batchRequest = BatchUpdateValuesRequest()
                .setValueInputOption("USER_ENTERED")
                .setData(batchUpdates)
            service.spreadsheets().values()
                .batchUpdate(spreadsheetId, batchRequest)
                .execute()
        }

        if (rowsToAppend.isNotEmpty()) {
            val body = ValueRange().setValues(rowsToAppend)
            service.spreadsheets().values()
                .append(spreadsheetId, "'Master Inventory'!A:S", body)
                .setValueInputOption("USER_ENTERED")
                .execute()
        }
    }

    suspend fun deleteInventoryRow(account: GoogleSignInAccount, itemId: String) = withContext(Dispatchers.IO) {
        val service = getSheetsService(account)
        cachedInventoryRowById.remove(itemId)
        val response: ValueRange = service.spreadsheets().values()
            .get(spreadsheetId, "'Master Inventory'!A2:A")
            .execute()
        val rows = response.getValues() ?: return@withContext
        val index = rows.indexOfFirst { it.getOrNull(0)?.toString()?.trim() == itemId }
        if (index != -1) {
            val rowNum = index + 2
            var sheetId = cachedMasterSheetId
            if (sheetId == null) {
                val spreadsheet = service.spreadsheets().get(spreadsheetId).execute()
                sheetId = spreadsheet.sheets?.firstOrNull {
                    it.properties?.title == "Master Inventory"
                }?.properties?.sheetId
                if (sheetId != null) {
                    cachedMasterSheetId = sheetId
                }
            }
            if (sheetId != null) {
                val deleteReq = DeleteDimensionRequest().setRange(
                    DimensionRange()
                        .setSheetId(sheetId)
                        .setDimension("ROWS")
                        .setStartIndex(rowNum - 1)
                        .setEndIndex(rowNum)
                )
                service.spreadsheets().batchUpdate(
                    spreadsheetId,
                    BatchUpdateSpreadsheetRequest().setRequests(listOf(Request().setDeleteDimension(deleteReq)))
                ).execute()
            } else {
                service.spreadsheets().values()
                    .clear(spreadsheetId, "'Master Inventory'!A$rowNum:S$rowNum", ClearValuesRequest())
                    .execute()
            }
        }
    }

    suspend fun fetchGroceries(account: GoogleSignInAccount): List<GroceryItem> = withContext(Dispatchers.IO) {
        val service = getSheetsService(account)
        val response: ValueRange = service.spreadsheets().values()
            .get(spreadsheetId, "'Groceries'!A2:J")
            .execute()
        parseGroceryRows(response.getValues() ?: emptyList())
    }

    private fun parseGroceryRows(rows: List<List<Any>>): List<GroceryItem> {
        val items = mutableListOf<GroceryItem>()
        for (row in rows) {
            val id = row.getOrNull(0)?.toString()?.trim() ?: continue
            val name = row.getOrNull(1)?.toString()?.trim() ?: continue
            if (name.isEmpty()) continue

            val qty = parseDouble(row.getOrNull(2), default = 1.0)
            val unit = row.getOrNull(3)?.toString()?.trim() ?: ""
            val aisle = row.getOrNull(4)?.toString()?.trim() ?: ""
            val store = row.getOrNull(5)?.toString()?.trim()?.ifEmpty { "Grocery Store" } ?: "Grocery Store"
            val checked = parseBool(row.getOrNull(6))
            val favorite = parseBool(row.getOrNull(7))
            val addedBy = row.getOrNull(8)?.toString()?.trim() ?: ""
            val updatedAt = row.getOrNull(9)?.toString()?.trim() ?: ""

            items.add(
                GroceryItem(
                    id = id,
                    name = name,
                    qty = qty,
                    unit = unit,
                    aisle = aisle,
                    store = store,
                    checked = checked,
                    favorite = favorite,
                    addedBy = addedBy,
                    updatedAt = updatedAt,
                    syncPending = false
                )
            )
        }
        return items
    }

    suspend fun fetchHistoryLog(account: GoogleSignInAccount): List<HistoryLogEntry> = withContext(Dispatchers.IO) {
        val service = getSheetsService(account)
        val response: ValueRange = service.spreadsheets().values()
            .get(spreadsheetId, "'History Log'!A2:I")
            .execute()
        parseHistoryRows(response.getValues() ?: emptyList())
    }

    private fun parseHistoryRows(rows: List<List<Any>>): List<HistoryLogEntry> {
        val entries = mutableListOf<HistoryLogEntry>()
        for (row in rows) {
            val timestamp = row.getOrNull(0)?.toString()?.trim() ?: continue
            val action = row.getOrNull(1)?.toString()?.trim() ?: ""
            val category = row.getOrNull(2)?.toString()?.trim() ?: ""
            val itemName = row.getOrNull(3)?.toString()?.trim() ?: ""
            val change = parseDouble(row.getOrNull(4))
            val newStock = parseDouble(row.getOrNull(5))
            val unit = row.getOrNull(6)?.toString()?.trim() ?: ""
            val expiry = row.getOrNull(7)?.toString()?.trim()
            val notes = row.getOrNull(8)?.toString()?.trim() ?: ""

            entries.add(
                HistoryLogEntry(
                    timestamp = timestamp,
                    action = action,
                    category = category,
                    itemName = itemName,
                    change = change,
                    newStock = newStock,
                    unit = unit,
                    expirationDate = expiry,
                    notes = notes,
                    syncPending = false
                )
            )
        }
        return entries
    }

    suspend fun fetchSettings(account: GoogleSignInAccount): AppSettingsEntity = withContext(Dispatchers.IO) {
        val service = getSheetsService(account)
        try {
            val response: ValueRange = service.spreadsheets().values()
                .get(spreadsheetId, "'Settings'!A2:B")
                .execute()
            return@withContext parseSettingsRows(response.getValues() ?: emptyList(), account.email ?: "")
        } catch (_: Exception) {}

        AppSettingsEntity(
            lastSyncedAt = System.currentTimeMillis(),
            userEmail = account.email ?: ""
        )
    }

    private fun parseSettingsRows(rows: List<List<Any>>, email: String): AppSettingsEntity {
        try {
            var jsonStr = ""
            for (row in rows) {
                if (row.getOrNull(0)?.toString()?.trim() == "app_settings_json") {
                    jsonStr = row.getOrNull(1)?.toString()?.trim() ?: ""
                    break
                }
            }
            if (jsonStr.isNotEmpty()) {
                val parsed = com.google.gson.JsonParser.parseString(jsonStr).asJsonObject
                val adults = parsed.get("adults")?.asInt ?: 2
                val targetYears = parsed.get("targetYears")?.asDouble ?: 2.0
                val lowStockPct = parsed.get("lowStockPct")?.asInt ?: 40
                val useSoonDays = parsed.get("useSoonDays")?.asInt ?: 90
                val kidsArr = parsed.getAsJsonArray("kidAges")?.toString() ?: "[9, 5, 0]"
                val catsArr = parsed.getAsJsonArray("categories")?.toString() ?: "[]"
                val locsArr = parsed.getAsJsonArray("locations")?.toString() ?: "[]"

                return AppSettingsEntity(
                    key = "app_settings",
                    householdSize = adults + 3,
                    adults = adults,
                    kidAgesJson = kidsArr,
                    categoriesJson = catsArr,
                    locationsJson = locsArr,
                    targetYears = targetYears,
                    lowStockPct = lowStockPct,
                    useSoonDays = useSoonDays,
                    lastSyncedAt = System.currentTimeMillis(),
                    userEmail = email
                )
            }
        } catch (_: Exception) {}

        return AppSettingsEntity(
            lastSyncedAt = System.currentTimeMillis(),
            userEmail = email
        )
    }

    suspend fun saveSettingsJson(account: GoogleSignInAccount, settings: AppSettingsEntity) = withContext(Dispatchers.IO) {
        val service = getSheetsService(account)
        val json = com.google.gson.JsonObject().apply {
            addProperty("adults", settings.adults)
            addProperty("targetYears", settings.targetYears)
            addProperty("lowStockPct", settings.lowStockPct)
            addProperty("useSoonDays", settings.useSoonDays)
            val kidAgesElement = try {
                com.google.gson.JsonParser.parseString(settings.kidAgesJson)
            } catch (_: Exception) {
                com.google.gson.JsonArray()
            }
            add("kidAges", kidAgesElement)
            val catElement = try {
                com.google.gson.JsonParser.parseString(settings.categoriesJson)
            } catch (_: Exception) {
                com.google.gson.JsonArray()
            }
            add("categories", catElement)
            val locElement = try {
                com.google.gson.JsonParser.parseString(settings.locationsJson)
            } catch (_: Exception) {
                com.google.gson.JsonArray()
            }
            add("locations", locElement)
        }
        val jsonStr = json.toString()

        val response: ValueRange = try {
            service.spreadsheets().values()
                .get(spreadsheetId, "'Settings'!A2:B")
                .execute()
        } catch (_: Exception) {
            ValueRange()
        }

        val rows = response.getValues() ?: emptyList()
        val index = rows.indexOfFirst { it.getOrNull(0)?.toString()?.trim() == "app_settings_json" }
        if (index != -1) {
            val rowNum = index + 2
            val body = ValueRange().setValues(listOf(listOf("app_settings_json", jsonStr)))
            service.spreadsheets().values()
                .update(spreadsheetId, "'Settings'!A$rowNum:B$rowNum", body)
                .setValueInputOption("USER_ENTERED")
                .execute()
        } else {
            val body = ValueRange().setValues(listOf(listOf("app_settings_json", jsonStr)))
            service.spreadsheets().values()
                .append(spreadsheetId, "'Settings'!A:B", body)
                .setValueInputOption("USER_ENTERED")
                .execute()
        }
    }

    suspend fun appendHistoryEntry(account: GoogleSignInAccount, entry: HistoryLogEntry) = withContext(Dispatchers.IO) {
        val service = getSheetsService(account)
        val values = listOf(
            listOf(
                entry.timestamp,
                entry.action,
                entry.category,
                entry.itemName,
                entry.change,
                entry.newStock,
                entry.unit,
                entry.expirationDate ?: "",
                entry.notes
            )
        )
        val body = ValueRange().setValues(values)
        service.spreadsheets().values()
            .append(spreadsheetId, "'History Log'!A:I", body)
            .setValueInputOption("USER_ENTERED")
            .execute()
    }

    suspend fun appendHistoryEntriesBatch(account: GoogleSignInAccount, entries: List<HistoryLogEntry>) = withContext(Dispatchers.IO) {
        if (entries.isEmpty()) return@withContext
        val service = getSheetsService(account)
        val rows = entries.map { entry ->
            listOf<Any>(
                entry.timestamp,
                entry.action,
                entry.category,
                entry.itemName,
                entry.change,
                entry.newStock,
                entry.unit,
                entry.expirationDate ?: "",
                entry.notes
            )
        }
        val body = ValueRange().setValues(rows)
        service.spreadsheets().values()
            .append(spreadsheetId, "'History Log'!A:I", body)
            .setValueInputOption("USER_ENTERED")
            .execute()
    }

    suspend fun appendGroceryItem(account: GoogleSignInAccount, item: GroceryItem) = withContext(Dispatchers.IO) {
        val service = getSheetsService(account)
        val values = listOf(
            listOf(
                item.id,
                item.name,
                item.qty,
                item.unit,
                item.aisle,
                item.store,
                item.checked,
                item.favorite,
                item.addedBy,
                item.updatedAt
            )
        )
        val body = ValueRange().setValues(values)
        service.spreadsheets().values()
            .append(spreadsheetId, "'Groceries'!A:J", body)
            .setValueInputOption("USER_ENTERED")
            .execute()
    }

    suspend fun rewriteGroceriesTab(account: GoogleSignInAccount, items: List<GroceryItem>) = withContext(Dispatchers.IO) {
        val service = getSheetsService(account)
        try {
            service.spreadsheets().values()
                .clear(spreadsheetId, "'Groceries'!A2:J", ClearValuesRequest())
                .execute()
        } catch (_: Exception) {}

        val rows = mutableListOf<List<Any>>()
        // Header
        rows.add(listOf("ID", "Name", "Qty", "Unit", "Aisle", "Store", "Checked", "Favorite", "Added By", "Updated At"))
        for (item in items) {
            rows.add(
                listOf(
                    item.id,
                    item.name,
                    item.qty,
                    item.unit,
                    item.aisle,
                    item.store,
                    item.checked,
                    item.favorite,
                    item.addedBy,
                    item.updatedAt
                )
            )
        }
        val body = ValueRange().setValues(rows)
        service.spreadsheets().values()
            .update(spreadsheetId, "'Groceries'!A1:J${rows.size}", body)
            .setValueInputOption("USER_ENTERED")
            .execute()
    }

    private fun parseDouble(value: Any?, default: Double = 0.0): Double {
        if (value == null) return default
        val str = value.toString().replace(Regex("[^0-9.-]"), "").trim()
        return str.toDoubleOrNull() ?: default
    }

    private fun parseBool(value: Any?): Boolean {
        if (value == null) return false
        if (value is Boolean) return value
        val s = value.toString().trim().lowercase()
        return s == "true" || s == "1" || s == "yes"
    }
}
