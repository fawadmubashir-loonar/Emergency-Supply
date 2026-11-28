package com.emergencysupply.rations.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emergencysupply.rations.data.BundleCatalog
import com.emergencysupply.rations.data.PlannedRationTemplate
import com.emergencysupply.rations.data.PreplannedCatalog
import com.emergencysupply.rations.data.SupplyBundle
import com.emergencysupply.rations.data.local.entity.GroceryItem
import com.emergencysupply.rations.data.local.entity.InventoryItem
import com.emergencysupply.rations.ui.MainViewModel
import com.emergencysupply.rations.ui.components.BarcodeScannerSheet
import com.emergencysupply.rations.ui.components.ScrollableMultiWheelPicker
import com.emergencysupply.rations.ui.components.WheelColumnSpec
import com.emergencysupply.rations.ui.components.categoryVisualFor
import com.emergencysupply.rations.ui.components.dashedBorder
import com.emergencysupply.rations.ui.theme.BorderSlate
import com.emergencysupply.rations.ui.theme.CardSurface
import com.emergencysupply.rations.ui.theme.CobaltBlue
import com.emergencysupply.rations.ui.theme.CobaltSoft
import com.emergencysupply.rations.ui.theme.DangerRed
import com.emergencysupply.rations.ui.theme.DangerSoft
import com.emergencysupply.rations.ui.theme.DarkSlate
import com.emergencysupply.rations.ui.theme.EmeraldDeep
import com.emergencysupply.rations.ui.theme.EmeraldVibrant
import com.emergencysupply.rations.ui.theme.InkBlack
import com.emergencysupply.rations.ui.theme.LightSlate
import com.emergencysupply.rations.ui.theme.MediumSlate
import com.emergencysupply.rations.ui.theme.MidnightSlate
import com.emergencysupply.rations.ui.theme.MutedSlate
import com.emergencysupply.rations.ui.theme.OrangeAccent
import com.emergencysupply.rations.ui.theme.PeachSoft
import com.emergencysupply.rations.ui.theme.PeachTint
import com.emergencysupply.rations.ui.theme.SoftTileSurface
import com.emergencysupply.rations.ui.theme.SuccessGreen
import com.emergencysupply.rations.ui.theme.SuccessSoft
import com.emergencysupply.rations.ui.theme.TealPrimary
import com.emergencysupply.rations.ui.theme.TealSoft
import com.emergencysupply.rations.ui.theme.WarningAmber

data class GrocerySuggestion(
    val name: String,
    val existingGrocery: GroceryItem? = null,
    val pantryItem: InventoryItem? = null,
    val catalogTemplate: PlannedRationTemplate? = null,
    val defaultQty: Double = 1.0,
    val defaultUnit: String = "packs",
    val aisle: String = "General",
    val icon: String = "📦"
)

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun GroceriesScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val groceries by viewModel.allGroceries.collectAsState()
    val allInventory by viewModel.allInventory.collectAsState()
    val selectedStore by viewModel.groceryStoreFilter.collectAsState()
    val favoritesOnly by viewModel.favoritesOnlyFilter.collectAsState()

    var showBarcodeScanner by remember { mutableStateOf(false) }
    var showBundlesSheet by remember { mutableStateOf(false) }

    var newName by remember { mutableStateOf("") }
    var newQty by remember { mutableStateOf("1") }
    var newUnit by remember { mutableStateOf("packs") }
    var newStore by remember { mutableStateOf("Grocery Store") }
    var newAisle by remember { mutableStateOf("General") }
    var cartExpanded by remember { mutableStateOf(true) }
    var editingGrocery by remember { mutableStateOf<GroceryItem?>(null) }

    val storeOptions = PreplannedCatalog.groceryPurposeLists

    val quickPickItems = remember(allInventory) {
        val lowNames = allInventory.filter { it.isLowStock }.map { it.name }
        (lowNames + PreplannedCatalog.groceryQuickPicks).distinct().take(16)
    }

    val filteredGroceries = groceries.filter {
        selectedStore == "All Lists" || selectedStore == "All Stores" || it.store.equals(selectedStore, ignoreCase = true)
    }

    val uncheckedItems = filteredGroceries.filter { !it.checked && (!favoritesOnly || it.favorite) }
    val checkedItems = filteredGroceries.filter { it.checked && (!favoritesOnly || it.favorite) }

    // Smart As-You-Type Autocomplete + Duplicate Guard matches (up to 6)
    val autocompleteSuggestions = remember(newName, groceries, allInventory) {
        val q = newName.trim()
        if (q.isBlank()) {
            emptyList()
        } else {
            val results = mutableListOf<GrocerySuggestion>()
            val seenNames = mutableSetOf<String>()

            // 1. Matches in groceries
            for (g in groceries) {
                if (g.name.contains(q, ignoreCase = true) && seenNames.add(g.name.lowercase())) {
                    val resolvedAisle = g.aisle.ifBlank { PreplannedCatalog.aisleForItem(g.name, "") }
                    results.add(
                        GrocerySuggestion(
                            name = g.name,
                            existingGrocery = g,
                            defaultQty = g.qty,
                            defaultUnit = g.unit,
                            aisle = resolvedAisle,
                            icon = if (g.favorite) "⭐" else PreplannedCatalog.iconForItem(g.name, resolvedAisle)
                        )
                    )
                    if (results.size >= 6) break
                }
            }

            // 2. Matches in pantry inventory
            if (results.size < 6) {
                for (inv in allInventory) {
                    if (inv.name.contains(q, ignoreCase = true) && seenNames.add(inv.name.lowercase())) {
                        val needQty = if (inv.parLevel > inv.currentQty) maxOf(1.0, inv.parLevel - inv.currentQty) else 1.0
                        val resolvedAisle = inv.category.ifBlank { PreplannedCatalog.aisleForItem(inv.name, "") }
                        results.add(
                            GrocerySuggestion(
                                name = inv.name,
                                pantryItem = inv,
                                defaultQty = needQty,
                                defaultUnit = inv.unit.ifBlank { "packs" },
                                aisle = resolvedAisle,
                                icon = "📦"
                            )
                        )
                        if (results.size >= 6) break
                    }
                }
            }

            // 3. Matches in preplanned catalog
            if (results.size < 6) {
                for (tpl in PreplannedCatalog.plannedRations) {
                    if (tpl.name.contains(q, ignoreCase = true) && seenNames.add(tpl.name.lowercase())) {
                        val resolvedAisle = tpl.category.ifBlank { PreplannedCatalog.aisleForItem(tpl.name, "") }
                        results.add(
                            GrocerySuggestion(
                                name = tpl.name,
                                catalogTemplate = tpl,
                                defaultQty = tpl.defaultQty,
                                defaultUnit = tpl.unit.ifBlank { "packs" },
                                aisle = resolvedAisle,
                                icon = tpl.icon
                            )
                        )
                        if (results.size >= 6) break
                    }
                }
            }

            results.take(6)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Top Action & Favorites Bar
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Favorites toggle chip
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = if (favoritesOnly) WarningAmber else CardSurface,
                    border = BorderStroke(1.dp, if (favoritesOnly) WarningAmber else BorderSlate),
                    shadowElevation = if (favoritesOnly) 2.dp else 1.dp,
                    modifier = Modifier.clickable { viewModel.favoritesOnlyFilter.value = !favoritesOnly }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (favoritesOnly) Icons.Default.Star else Icons.Outlined.StarOutline,
                            contentDescription = "Favorites",
                            tint = if (favoritesOnly) Color.White else WarningAmber,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (favoritesOnly) "Favorites Only" else "⭐ Favorites",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (favoritesOnly) Color.White else DarkSlate
                        )
                    }
                }

                // Bundles chip
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = CardSurface,
                    border = BorderStroke(1.dp, BorderSlate),
                    shadowElevation = 1.dp,
                    modifier = Modifier.clickable { showBundlesSheet = true }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🎒 Bundles",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = DarkSlate
                        )
                    }
                }

                // Share button
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = CardSurface,
                    border = BorderStroke(1.dp, BorderSlate),
                    shadowElevation = 1.dp,
                    modifier = Modifier.clickable {
                        val shareText = viewModel.buildShareableGroceryText(selectedStore, filteredGroceries)
                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, shareText)
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "Share Grocery List"))
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share",
                            tint = CobaltBlue,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "📤 Share",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = CobaltBlue
                        )
                    }
                }
            }
        }

        // 2. Quick Add Section + Smart As-You-Type Autocomplete & Duplicate Guard
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = CardSurface),
                border = BorderStroke(1.dp, BorderSlate),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "One-Tap Add",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MidnightSlate
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(quickPickItems) { pick ->
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = TealSoft,
                                modifier = Modifier.clickable {
                                    val targetStore = if (selectedStore != "All Stores" && selectedStore != "All Lists") selectedStore else newStore
                                    val matchInv = allInventory.firstOrNull { it.name.equals(pick, ignoreCase = true) }
                                    val resolvedAisle = matchInv?.category?.ifBlank { PreplannedCatalog.aisleForItem(pick, "") } ?: PreplannedCatalog.aisleForItem(pick, "")
                                    viewModel.addGroceryItem(
                                        name = pick,
                                        qty = matchInv?.let { maxOf(1.0, it.parLevel - it.currentQty) } ?: 1.0,
                                        unit = matchInv?.unit?.ifBlank { "packs" } ?: "packs",
                                        aisle = resolvedAisle,
                                        store = targetStore
                                    )
                                    Toast.makeText(context, "Added $pick to $targetStore", Toast.LENGTH_SHORT).show()
                                }
                            ) {
                                Text(
                                    text = "+ $pick",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TealPrimary,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = newName,
                            onValueChange = { newName = it },
                            placeholder = { Text("Or type item name...", fontSize = 13.sp, color = LightSlate) },
                            singleLine = true,
                            modifier = Modifier.weight(1.5f),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = TealPrimary,
                                unfocusedBorderColor = BorderSlate,
                                focusedContainerColor = SoftTileSurface,
                                unfocusedContainerColor = SoftTileSurface
                            )
                        )

                        OutlinedTextField(
                            value = newQty,
                            onValueChange = { newQty = it },
                            placeholder = { Text("Qty", fontSize = 13.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(0.55f),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = TealPrimary,
                                unfocusedBorderColor = BorderSlate,
                                focusedContainerColor = SoftTileSurface,
                                unfocusedContainerColor = SoftTileSurface
                            )
                        )

                        IconButton(
                            onClick = { showBarcodeScanner = true },
                            modifier = Modifier.size(44.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCodeScanner,
                                contentDescription = "Scan Barcode",
                                tint = EmeraldDeep,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Button(
                            onClick = {
                                if (newName.isNotBlank()) {
                                    val qtyVal = newQty.toDoubleOrNull() ?: 1.0
                                    val targetStore = if (selectedStore != "All Stores" && selectedStore != "All Lists") selectedStore else newStore
                                    val targetAisle = newAisle.ifBlank { PreplannedCatalog.aisleForItem(newName.trim(), "") }
                                    viewModel.addGroceryItem(
                                        name = newName.trim(),
                                        qty = qtyVal,
                                        unit = newUnit.trim(),
                                        aisle = targetAisle,
                                        store = targetStore
                                    )
                                    newName = ""
                                    newQty = "1"
                                }
                            },
                            modifier = Modifier.height(52.dp),
                            shape = RoundedCornerShape(20.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = InkBlack),
                            enabled = newName.isNotBlank()
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = "Add", modifier = Modifier.size(20.dp))
                        }
                    }

                    // Smart As-You-Type Autocomplete + Duplicate Guard (#1)
                    if (newName.isNotBlank() && autocompleteSuggestions.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = SoftTileSurface,
                            border = BorderStroke(1.dp, BorderSlate),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "Smart Suggestions & Duplicate Guard",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MediumSlate,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )

                                autocompleteSuggestions.forEach { suggestion ->
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = CardSurface,
                                        border = BorderStroke(0.5.dp, BorderSlate),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                if (suggestion.existingGrocery != null) {
                                                    viewModel.bumpOrUncheckGrocery(suggestion.existingGrocery.id, 1.0)
                                                    val statusMsg = if (suggestion.existingGrocery.checked) "Restored to list (+1)" else "Bumped qty (+1)"
                                                    Toast.makeText(context, "${suggestion.existingGrocery.name}: $statusMsg", Toast.LENGTH_SHORT).show()
                                                    newName = ""
                                                } else {
                                                    val targetStore = if (selectedStore != "All Stores" && selectedStore != "All Lists") selectedStore else newStore
                                                    val resolvedAisle = suggestion.aisle.ifBlank { PreplannedCatalog.aisleForItem(suggestion.name, "") }
                                                    viewModel.addGroceryItem(
                                                        name = suggestion.name,
                                                        qty = suggestion.defaultQty,
                                                        unit = suggestion.defaultUnit,
                                                        aisle = resolvedAisle,
                                                        store = targetStore
                                                    )
                                                    Toast.makeText(context, "Added ${suggestion.name} to $targetStore", Toast.LENGTH_SHORT).show()
                                                    newName = ""
                                                }
                                            }
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 10.dp, vertical = 8.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                modifier = Modifier.weight(1f),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(suggestion.icon, fontSize = 16.sp)
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Column {
                                                    Text(
                                                        text = suggestion.name,
                                                        fontSize = 13.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MidnightSlate
                                                    )
                                                    if (suggestion.pantryItem != null) {
                                                        Text(
                                                            text = "📦 Qty %.0f %s in Pantry (Par %.0f)".format(
                                                                suggestion.pantryItem.currentQty,
                                                                suggestion.pantryItem.unit,
                                                                suggestion.pantryItem.parLevel
                                                            ),
                                                            fontSize = 10.sp,
                                                            color = MediumSlate
                                                        )
                                                    } else {
                                                        Text(
                                                            text = suggestion.aisle,
                                                            fontSize = 10.sp,
                                                            color = LightSlate
                                                        )
                                                    }
                                                }
                                            }

                                            Spacer(modifier = Modifier.width(6.dp))

                                            if (suggestion.existingGrocery != null) {
                                                if (suggestion.existingGrocery.checked) {
                                                    Surface(shape = RoundedCornerShape(8.dp), color = SuccessSoft) {
                                                        Text(
                                                            text = "✓ In Cart",
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = SuccessGreen,
                                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                        )
                                                    }
                                                } else {
                                                    Surface(shape = RoundedCornerShape(8.dp), color = CobaltSoft) {
                                                        Text(
                                                            text = "On List",
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = CobaltBlue,
                                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                        )
                                                    }
                                                }
                                            } else {
                                                Surface(shape = RoundedCornerShape(8.dp), color = TealSoft) {
                                                    Text(
                                                        text = suggestion.aisle.ifBlank { PreplannedCatalog.aisleForItem(suggestion.name, "") },
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = TealPrimary,
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        item {
                            Text(text = "Store:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MediumSlate)
                        }
                        val storeQuickList = listOf("Weekly Groceries", "Costco", "Halal Market", "Grocery Store", "Target", "Emergency Resupply")
                        items(storeQuickList) { s ->
                            val isSel = (if (selectedStore != "All Stores" && selectedStore != "All Lists") selectedStore else newStore).equals(s, ignoreCase = true)
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSel) InkBlack else SoftTileSurface,
                                border = BorderStroke(1.dp, if (isSel) InkBlack else BorderSlate),
                                modifier = Modifier.clickable { newStore = s }
                            ) {
                                Text(
                                    text = s,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSel) Color.White else DarkSlate,
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. Multiple Purpose / Store Lists (#7)
        item {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(storeOptions) { store ->
                    val isSelected = selectedStore.equals(store, ignoreCase = true) ||
                            ((selectedStore == "All Lists" || selectedStore == "All Stores") && (store == "All Lists" || store == "All Stores"))
                    val unchCount = if (store == "All Lists" || store == "All Stores") {
                        groceries.count { !it.checked }
                    } else {
                        groceries.count { !it.checked && it.store.equals(store, ignoreCase = true) }
                    }
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (isSelected) OrangeAccent else CardSurface,
                        border = BorderStroke(1.dp, if (isSelected) OrangeAccent else BorderSlate),
                        shadowElevation = if (isSelected) 2.dp else 1.dp,
                        modifier = Modifier.clickable { viewModel.groceryStoreFilter.value = store }
                    ) {
                        Text(
                            text = if (unchCount > 0) "$store ($unchCount)" else store,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else DarkSlate,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                        )
                    }
                }
            }
        }

        // 4. Header & Count Status Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${uncheckedItems.size} to buy (${checkedItems.size} in cart)",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MidnightSlate
                )

                if (checkedItems.isNotEmpty()) {
                    OutlinedButton(
                        onClick = { viewModel.clearCheckedGroceries() },
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, DangerRed.copy(alpha = 0.4f)),
                        colors = ButtonDefaults.outlinedButtonColors(containerColor = DangerSoft),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Icon(imageVector = Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(15.dp), tint = DangerRed)
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(text = "Clear Cart (${checkedItems.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = DangerRed)
                    }
                }
            }
        }

        // 5. Aisle-by-Aisle Auto-Grouping (#2) for Unchecked Items
        if (uncheckedItems.isEmpty() && checkedItems.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .dashedBorder(width = 1.5.dp, color = EmeraldVibrant.copy(alpha = 0.5f), cornerRadius = 20.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = PeachSoft.copy(alpha = 0.45f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 36.dp, horizontal = 20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Surface(shape = CircleShape, color = EmeraldVibrant.copy(alpha = 0.15f), modifier = Modifier.size(54.dp)) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.ShoppingCart, contentDescription = null, modifier = Modifier.size(24.dp), tint = EmeraldDeep)
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = if (favoritesOnly) "No favorite grocery items! ⭐" else "Shopping list clear! 🎉",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MidnightSlate
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Tap any quick-pick chip above or type to add items.", fontSize = 12.sp, color = MediumSlate)
                        }
                    }
                }
            }
        } else {
            val itemsByAisle = uncheckedItems.groupBy {
                it.aisle.ifBlank { PreplannedCatalog.aisleForItem(it.name, "") }
            }

            itemsByAisle.forEach { (aisleName, itemsInAisle) ->
                item(key = "aisle_hdr_$aisleName") {
                    val visual = categoryVisualFor(aisleName)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = visual.tint,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(PreplannedCatalog.iconForItem(aisleName, aisleName), fontSize = 14.sp)
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = aisleName,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MidnightSlate
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(shape = RoundedCornerShape(8.dp), color = visual.tint) {
                            Text(
                                text = "${itemsInAisle.size}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                items(itemsInAisle, key = { "groc_${it.id}" }) { item ->
                    ReformedGroceryItemCard(
                        item = item,
                        onToggleChecked = { viewModel.toggleGroceryChecked(item.id, !item.checked) },
                        onToggleFavorite = { viewModel.toggleGroceryFavorite(item.id, !item.favorite) },
                        onMoveToPantry = {
                            viewModel.moveGroceryToPantry(item)
                            Toast.makeText(context, "Restocked +%.0f ${item.name} into Pantry!".format(item.qty), Toast.LENGTH_SHORT).show()
                        },
                        onDelete = { viewModel.deleteGrocery(item.id) },
                        onLongClick = { editingGrocery = item }
                    )
                }
            }
        }

        // 6. Collapsible "✓ In Cart" Sink-to-Bottom Drawer (#4)
        if (checkedItems.isNotEmpty()) {
            item(key = "in_cart_drawer") {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = CardSurface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { cartExpanded = !cartExpanded },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(shape = CircleShape, color = SuccessSoft, modifier = Modifier.size(28.dp)) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text("✓", fontSize = 14.sp, fontWeight = FontWeight.Black, color = SuccessGreen)
                                    }
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "In Cart (${checkedItems.size})",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MidnightSlate
                                )
                            }

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Button(
                                    onClick = {
                                        viewModel.finishShoppingTrip(checkedItems)
                                        Toast.makeText(context, "Finish Trip! Restocked ${checkedItems.size} items to Pantry 🎉", Toast.LENGTH_SHORT).show()
                                    },
                                    shape = RoundedCornerShape(20.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = InkBlack),
                                    modifier = Modifier.height(34.dp)
                                ) {
                                    Text(
                                        text = "Finish Trip → Pantry",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }

                                OutlinedButton(
                                    onClick = { viewModel.clearCheckedGroceries() },
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, DangerRed.copy(alpha = 0.4f)),
                                    colors = ButtonDefaults.outlinedButtonColors(containerColor = DangerSoft),
                                    modifier = Modifier.height(34.dp)
                                ) {
                                    Text(
                                        text = "Clear",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = DangerRed
                                    )
                                }

                                IconButton(
                                    onClick = { cartExpanded = !cartExpanded },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = if (cartExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                        contentDescription = if (cartExpanded) "Collapse" else "Expand",
                                        tint = MediumSlate
                                    )
                                }
                            }
                        }

                        if (cartExpanded) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                checkedItems.forEach { item ->
                                    ReformedGroceryItemCard(
                                        item = item,
                                        onToggleChecked = { viewModel.toggleGroceryChecked(item.id, !item.checked) },
                                        onToggleFavorite = { viewModel.toggleGroceryFavorite(item.id, !item.favorite) },
                                        onMoveToPantry = {
                                            viewModel.moveGroceryToPantry(item)
                                            Toast.makeText(context, "Restocked +%.0f ${item.name} into Pantry!".format(item.qty), Toast.LENGTH_SHORT).show()
                                        },
                                        onDelete = { viewModel.deleteGrocery(item.id) },
                                        onLongClick = { editingGrocery = item }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }

    editingGrocery?.let { itemToEdit ->
        GroceryItemEditorSheet(
            item = itemToEdit,
            storeOptions = storeOptions,
            onDismiss = { editingGrocery = null },
            onSave = { updated ->
                viewModel.updateGroceryItem(updated)
                editingGrocery = null
            },
            onDelete = {
                viewModel.deleteGrocery(itemToEdit.id)
                editingGrocery = null
            }
        )
    }

    if (showBarcodeScanner) {
        BarcodeScannerSheet(
            onBarcodeDetected = { barcode ->
                showBarcodeScanner = false
                viewModel.lookupBarcode(barcode) { info ->
                    if (info != null) {
                        newName = info.name
                        newUnit = info.unit
                        newQty = info.defaultQty.toInt().coerceAtLeast(1).toString()
                        newAisle = PreplannedCatalog.aisleForItem(info.name, info.category)
                        Toast.makeText(context, "Found: ${info.name}", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Barcode $barcode not found — type item name", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            onDismiss = { showBarcodeScanner = false }
        )
    }

    if (showBundlesSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showBundlesSheet = false },
            sheetState = sheetState,
            containerColor = CardSurface,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 24.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = TealSoft,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("🎒", fontSize = 22.sp)
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Emergency & Recipe Bundles",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp,
                            color = MidnightSlate
                        )
                        Text(
                            text = "1-tap add curated kits directly to your grocery list",
                            fontSize = 12.sp,
                            color = MediumSlate
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                BundleCatalog.allBundles.forEach { bundle ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = SoftTileSurface),
                        border = BorderStroke(1.dp, BorderSlate),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = CardSurface,
                                        border = BorderStroke(1.dp, BorderSlate),
                                        modifier = Modifier.size(42.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(bundle.icon, fontSize = 22.sp)
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = bundle.title,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = MidnightSlate
                                        )
                                        Text(
                                            text = bundle.subtitle,
                                            fontSize = 12.sp,
                                            color = DarkSlate,
                                            lineHeight = 16.sp
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = EmeraldDeep.copy(alpha = 0.1f),
                                    border = BorderStroke(1.dp, EmeraldDeep.copy(alpha = 0.3f))
                                ) {
                                    Text(
                                        text = "${bundle.items.size} items",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = EmeraldDeep,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Button(
                                onClick = {
                                    viewModel.addBundleToGroceries(bundle)
                                    showBundlesSheet = false
                                    Toast.makeText(
                                        context,
                                        "Added ${bundle.items.size} items from '${bundle.title}' to Groceries!",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                },
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = InkBlack),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                            ) {
                                Text(
                                    text = "+ Add All to List",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ReformedGroceryItemCard(
    item: GroceryItem,
    onToggleChecked: () -> Unit,
    onToggleFavorite: () -> Unit,
    onMoveToPantry: () -> Unit,
    onDelete: () -> Unit,
    onLongClick: () -> Unit = {}
) {
    val haptic = LocalHapticFeedback.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { onToggleChecked() },
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongClick()
                }
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = if (item.checked) SoftTileSurface else CardSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = if (item.checked) 0.dp else 2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = item.checked,
                onCheckedChange = { onToggleChecked() },
                colors = CheckboxDefaults.colors(checkedColor = TealPrimary, uncheckedColor = MediumSlate),
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            val icon = PreplannedCatalog.iconForItem(item.name, item.aisle)
            Surface(shape = RoundedCornerShape(10.dp), color = if (item.checked) Color(0xFFE2E8F0) else Color(0xFFDBEAFE), modifier = Modifier.size(34.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Text(icon, fontSize = 16.sp)
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .combinedClickable(
                        onClick = { onToggleChecked() },
                        onLongClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onLongClick()
                        }
                    )
            ) {
                Text(
                    text = item.name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (item.checked) LightSlate else MidnightSlate,
                    textDecoration = if (item.checked) TextDecoration.LineThrough else TextDecoration.None
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Qty: %.0f %s".format(item.qty, item.unit),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (item.checked) MutedSlate else DarkSlate
                    )
                    if (item.aisle.isNotEmpty() && item.aisle != "General") {
                        Text(text = " • ${item.aisle}", fontSize = 11.sp, color = TealPrimary)
                    }
                }
            }

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = TealSoft,
                modifier = Modifier.padding(end = 4.dp).clickable(onClick = onMoveToPantry)
            ) {
                Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Inventory, contentDescription = "Move to Pantry", tint = TealPrimary, modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("+ Pantry", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TealPrimary)
                }
            }

            IconButton(onClick = onToggleFavorite, modifier = Modifier.size(30.dp)) {
                Icon(
                    imageVector = if (item.favorite) Icons.Default.Star else Icons.Outlined.StarOutline,
                    contentDescription = "Favorite",
                    tint = if (item.favorite) WarningAmber else LightSlate,
                    modifier = Modifier.size(19.dp)
                )
            }

            IconButton(onClick = onDelete, modifier = Modifier.size(30.dp)) {
                Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = LightSlate, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroceryItemEditorSheet(
    item: GroceryItem,
    storeOptions: List<String>,
    onDismiss: () -> Unit,
    onSave: (GroceryItem) -> Unit,
    onDelete: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var name by remember(item.id) { mutableStateOf(item.name) }
    var isFavorite by remember(item.id) { mutableStateOf(item.favorite) }
    var confirmDelete by remember(item.id) { mutableStateOf(false) }

    // Qty options: 1..50, preserving exact current integer/value
    val currentQtyStr = if (item.qty > 0 && item.qty % 1.0 == 0.0) {
        item.qty.toInt().toString()
    } else if (item.qty > 0) {
        "%.1f".format(item.qty)
    } else {
        "1"
    }
    val qtyList = remember(item.id) {
        val standard = (1..50).map { it.toString() }
        if (standard.contains(currentQtyStr)) standard else (listOf(currentQtyStr) + standard).distinct()
    }
    var selectedQtyStr by remember(item.id) { mutableStateOf(currentQtyStr) }

    // Unit options: (PreplannedCatalog.standardUnits + item.unit).distinct()
    val unitOptions = remember(item.id) {
        (PreplannedCatalog.standardUnits + item.unit).filter { it.isNotBlank() }.distinct()
    }
    var selectedUnit by remember(item.id) {
        mutableStateOf(item.unit.ifBlank { unitOptions.firstOrNull() ?: "packs" })
    }

    // Store/List options: storeOptions.filter { it != "All Lists" && it != "All Stores" }
    val filteredStoreOptions = remember(item.id, storeOptions) {
        val base = storeOptions.filter { it != "All Lists" && it != "All Stores" }
        if (item.store.isNotBlank() && !base.contains(item.store)) {
            (listOf(item.store) + base).distinct()
        } else {
            base
        }
    }
    var selectedStore by remember(item.id) {
        mutableStateOf(if (item.store in filteredStoreOptions) item.store else (filteredStoreOptions.firstOrNull() ?: "Grocery Store"))
    }

    // Aisle options: derived from PreplannedCatalog.aisleForItem common aisles + current item.aisle
    val commonAisles = remember {
        listOf(
            "🛒 General Aisle",
            "🥩 Halal Meat & Seafood",
            "🌾 Bulk Grains & Pasta",
            "🥛 Dairy & Eggs",
            "🥬 Fresh & Frozen",
            "🥫 Canned & Dry Goods",
            "👶 Baby & Kids",
            "💧 Water & Drinks",
            "🫒 Oils & Spices",
            "🧴 Household & First Aid"
        )
    }
    val currentAisle = remember(item.id) {
        item.aisle.ifBlank { PreplannedCatalog.aisleForItem(item.name, "") }
    }
    val aisleOptions = remember(item.id) {
        (listOf(currentAisle) + commonAisles).filter { it.isNotBlank() }.distinct()
    }
    var selectedAisle by remember(item.id) { mutableStateOf(currentAisle) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = CardSurface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header with item icon + "Edit Grocery Item" + favorite/delete toggles
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val icon = PreplannedCatalog.iconForItem(name, selectedAisle)
                    val visual = categoryVisualFor(selectedAisle)
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = visual.tint.copy(alpha = 0.2f),
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(icon, fontSize = 20.sp)
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Edit Grocery Item",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 19.sp,
                            color = MidnightSlate
                        )
                        Text(
                            text = "Hold & spin to tweak list details",
                            fontSize = 11.sp,
                            color = MediumSlate
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = { isFavorite = !isFavorite },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Default.Star else Icons.Outlined.StarOutline,
                            contentDescription = "Toggle Favorite",
                            tint = if (isFavorite) WarningAmber else LightSlate,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Surface(
                        shape = CircleShape,
                        color = DangerSoft,
                        modifier = Modifier
                            .size(36.dp)
                            .clickable { confirmDelete = !confirmDelete }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = DangerRed,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            if (confirmDelete) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = DangerSoft,
                    border = BorderStroke(1.dp, DangerRed.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Remove '${item.name}' from list?",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = DangerRed
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = onDelete,
                                colors = ButtonDefaults.buttonColors(containerColor = DangerRed),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Delete Item", fontSize = 12.sp, color = Color.White)
                            }
                            OutlinedButton(
                                onClick = { confirmDelete = false },
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Cancel", fontSize = 12.sp, color = DarkSlate)
                            }
                        }
                    }
                }
            }

            // Item Name OutlinedTextField
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Item Name") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = EmeraldVibrant,
                    unfocusedBorderColor = BorderSlate,
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = SoftTileSurface
                )
            )

            // ScrollableMultiWheelPicker for Qty and Unit
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = SoftTileSurface,
                border = BorderStroke(1.dp, BorderSlate),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(
                        text = "QUANTITY & UNIT",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MediumSlate,
                        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                    )
                    ScrollableMultiWheelPicker(
                        columns = listOf(
                            WheelColumnSpec(
                                label = "Quantity",
                                items = qtyList,
                                selected = selectedQtyStr,
                                weight = 1f,
                                onSelected = { selectedQtyStr = it }
                            ),
                            WheelColumnSpec(
                                label = "Unit",
                                items = unitOptions,
                                selected = selectedUnit,
                                weight = 1.2f,
                                onSelected = { selectedUnit = it }
                            )
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // ScrollableMultiWheelPicker for Store/List and Aisle
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = SoftTileSurface,
                border = BorderStroke(1.dp, BorderSlate),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(
                        text = "PURPOSE LIST & AISLE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MediumSlate,
                        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                    )
                    ScrollableMultiWheelPicker(
                        columns = listOf(
                            WheelColumnSpec(
                                label = "Store / List",
                                items = filteredStoreOptions,
                                selected = selectedStore,
                                weight = 1.2f,
                                onSelected = { selectedStore = it }
                            ),
                            WheelColumnSpec(
                                label = "Aisle",
                                items = aisleOptions,
                                selected = selectedAisle,
                                weight = 1.6f,
                                onSelected = { selectedAisle = it }
                            )
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // InkBlack "Save & Sync" capsule button
            Button(
                onClick = {
                    val parsedQty = selectedQtyStr.toDoubleOrNull() ?: item.qty
                    val updated = item.copy(
                        name = name.trim().ifBlank { item.name },
                        qty = parsedQty,
                        unit = selectedUnit,
                        store = selectedStore,
                        aisle = selectedAisle,
                        favorite = isFavorite
                    )
                    onSave(updated)
                },
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(containerColor = InkBlack),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text(
                    text = "Save & Sync",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}
