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

    private fun getSheetsService(account: GoogleSignInAccount): Sheets {
        val credential = GoogleAccountCredential.usingOAuth2(
            context,
            listOf(SheetsScopes.SPREADSHEETS)
        ).apply {
            selectedAccount = account.account
        }

        return Sheets.Builder(
            NetHttpTransport(),
            GsonFactory.getDefaultInstance(),
            credential
        ).setApplicationName("EmergencyFoodRations").build()
    }

    suspend fun fetchMasterInventory(account: GoogleSignInAccount): List<InventoryItem> = withContext(Dispatchers.IO) {
        val service = getSheetsService(account)
        val response: ValueRange = service.spreadsheets().values()
            .get(spreadsheetId, "'Master Inventory'!A2:S")
            .execute()

        val rows = response.getValues() ?: return@withContext emptyList()
        val items = mutableListOf<InventoryItem>()

        for (row in rows) {
            val id = row.getOrNull(0)?.toString()?.trim() ?: continue
            if (id.isEmpty()) continue
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
        items
    }

    suspend fun upsertInventoryItem(account: GoogleSignInAccount, item: InventoryItem) = withContext(Dispatchers.IO) {
        val service = getSheetsService(account)
        val response: ValueRange = service.spreadsheets().values()
            .get(spreadsheetId, "'Master Inventory'!A2:S")
            .setValueRenderOption("FORMULA")
            .execute()

        val rows = response.getValues() ?: emptyList()
        val index = rows.indexOfFirst { it.getOrNull(0)?.toString()?.trim() == item.id }

        if (index != -1) {
            val rowNum = index + 2
            val existingRow = rows[index]
            val colI = existingRow.getOrNull(8)?.toString()?.trim() ?: ""
            val isFormula = colI.startsWith("=")

            if (isFormula) {
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

    suspend fun deleteInventoryRow(account: GoogleSignInAccount, itemId: String) = withContext(Dispatchers.IO) {
        val service = getSheetsService(account)
        val response: ValueRange = service.spreadsheets().values()
            .get(spreadsheetId, "'Master Inventory'!A2:A")
            .execute()
        val rows = response.getValues() ?: return@withContext
        val index = rows.indexOfFirst { it.getOrNull(0)?.toString()?.trim() == itemId }
        if (index != -1) {
            val rowNum = index + 2
            val spreadsheet = service.spreadsheets().get(spreadsheetId).execute()
            val sheetId = spreadsheet.sheets?.firstOrNull {
                it.properties?.title == "Master Inventory"
            }?.properties?.sheetId
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

        val rows = response.getValues() ?: return@withContext emptyList()
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
        items
    }

    suspend fun fetchHistoryLog(account: GoogleSignInAccount): List<HistoryLogEntry> = withContext(Dispatchers.IO) {
        val service = getSheetsService(account)
        val response: ValueRange = service.spreadsheets().values()
            .get(spreadsheetId, "'History Log'!A2:I")
            .execute()

        val rows = response.getValues() ?: return@withContext emptyList()
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
        entries
    }

    suspend fun fetchSettings(account: GoogleSignInAccount): AppSettingsEntity = withContext(Dispatchers.IO) {
        val service = getSheetsService(account)
        try {
            val response: ValueRange = service.spreadsheets().values()
                .get(spreadsheetId, "'Settings'!A2:B")
                .execute()
            val rows = response.getValues() ?: emptyList()
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

                return@withContext AppSettingsEntity(
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
                    userEmail = account.email ?: ""
                )
            }
        } catch (_: Exception) {}

        AppSettingsEntity(
            lastSyncedAt = System.currentTimeMillis(),
            userEmail = account.email ?: ""
        )
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
