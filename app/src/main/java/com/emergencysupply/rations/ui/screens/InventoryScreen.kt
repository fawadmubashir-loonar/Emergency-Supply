package com.emergencysupply.rations.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emergencysupply.rations.data.PreplannedCatalog
import com.emergencysupply.rations.data.local.entity.InventoryItem
import com.emergencysupply.rations.ui.MainViewModel
import com.emergencysupply.rations.ui.components.ScrollableWheelDatePicker
import com.emergencysupply.rations.ui.components.categoryVisualFor
import com.emergencysupply.rations.ui.theme.BorderSlate
import com.emergencysupply.rations.ui.theme.CardSurface
import com.emergencysupply.rations.ui.theme.DangerRed
import com.emergencysupply.rations.ui.theme.DangerSoft
import com.emergencysupply.rations.ui.theme.DarkSlate
import com.emergencysupply.rations.ui.theme.InkBlack
import com.emergencysupply.rations.ui.theme.LightSlate
import com.emergencysupply.rations.ui.theme.MediumSlate
import com.emergencysupply.rations.ui.theme.MidnightSlate
import com.emergencysupply.rations.ui.theme.OrangeAccent
import com.emergencysupply.rations.ui.theme.SoftTileSurface
import com.emergencysupply.rations.ui.theme.SuccessGreen
import com.emergencysupply.rations.ui.theme.SuccessSoft
import com.emergencysupply.rations.ui.theme.TealPrimary
import com.emergencysupply.rations.ui.theme.TealSoft
import com.emergencysupply.rations.ui.theme.WarningAmber
import com.emergencysupply.rations.ui.theme.WarningSoft
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID

enum class InventoryGroupMode(val label: String) {
    LIST("All"),
    BY_CATEGORY("By Category"),
    BY_LOCATION("By Spot")
}

@Composable
fun InventoryScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val items by viewModel.filteredInventory.collectAsState()
    val allInventory by viewModel.allInventory.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCategory by viewModel.categoryFilter.collectAsState()
    val selectedLocation by viewModel.locationFilter.collectAsState()
    val lowStockOnly by viewModel.lowStockOnlyFilter.collectAsState()
    val expiringOnly by viewModel.expiringOnlyFilter.collectAsState()
    val availableCategories by viewModel.availableCategories.collectAsState()
    val availableLocations by viewModel.availableLocations.collectAsState()

    var groupMode by remember { mutableStateOf(InventoryGroupMode.LIST) }
    var showAddSheet by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<InventoryItem?>(null) }
    var showNewCategoryPrompt by remember { mutableStateOf(false) }
    var showNewLocationPrompt by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // 1. Sleek Logistics-style Search Pill + Inline "+ Add" Pill Button
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = CardSurface,
            shadowElevation = 2.dp,
            border = BorderStroke(1.dp, BorderSlate.copy(alpha = 0.6f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = LightSlate,
                    modifier = Modifier.padding(start = 6.dp).size(20.dp)
                )
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.searchQuery.value = it },
                    placeholder = { Text("Search rations...", fontSize = 13.sp, color = LightSlate) },
                    singleLine = true,
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.searchQuery.value = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                            }
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent
                    ),
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = { showAddSheet = true },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = OrangeAccent),
                    modifier = Modifier.height(40.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("Add", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 2. View Group Switcher (All / By Category / By Spot) + Item Count
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                InventoryGroupMode.values().forEach { mode ->
                    val isSel = groupMode == mode
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSel) InkBlack else CardSurface,
                        border = BorderStroke(1.dp, if (isSel) InkBlack else BorderSlate),
                        modifier = Modifier.clickable { groupMode = mode }
                    ) {
                        Text(
                            text = mode.label,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSel) Color.White else MediumSlate,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            Text(
                text = "${items.size} items",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MediumSlate
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 3. AgroPulse-style Pill Category Tabs + "+ Category"
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            item {
                val isAll = selectedCategory == "All"
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (isAll) OrangeAccent else CardSurface,
                    border = BorderStroke(1.dp, if (isAll) OrangeAccent else BorderSlate),
                    modifier = Modifier.clickable { viewModel.categoryFilter.value = "All" }
                ) {
                    Text(
                        text = "All Categories",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isAll) Color.White else DarkSlate,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                    )
                }
            }

            items(availableCategories) { cat ->
                val isSel = selectedCategory.equals(cat, ignoreCase = true)
                val icon = PreplannedCatalog.iconForCategory(cat)
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (isSel) OrangeAccent else CardSurface,
                    border = BorderStroke(1.dp, if (isSel) OrangeAccent else BorderSlate),
                    modifier = Modifier.clickable { viewModel.categoryFilter.value = cat }
                ) {
                    Text(
                        text = "$icon $cat",
                        fontSize = 12.sp,
                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSel) Color.White else DarkSlate,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                    )
                }
            }

            item {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = TealSoft,
                    border = BorderStroke(1.dp, TealPrimary.copy(alpha = 0.35f)),
                    modifier = Modifier.clickable { showNewCategoryPrompt = true }
                ) {
                    Text(
                        text = "+ New Category",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TealPrimary,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // 4. Scrollable Storage Spot Pills + "+ Spot"
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            item {
                val isAll = selectedLocation == "All"
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = if (isAll) TealPrimary else CardSurface,
                    border = BorderStroke(1.dp, if (isAll) TealPrimary else BorderSlate),
                    modifier = Modifier.clickable { viewModel.locationFilter.value = "All" }
                ) {
                    Text(
                        text = "All Spots",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isAll) Color.White else MediumSlate,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            items(availableLocations) { loc ->
                val isSel = selectedLocation.equals(loc, ignoreCase = true)
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = if (isSel) TealPrimary else CardSurface,
                    border = BorderStroke(1.dp, if (isSel) TealPrimary else BorderSlate),
                    modifier = Modifier.clickable { viewModel.locationFilter.value = loc }
                ) {
                    Text(
                        text = "📍 $loc",
                        fontSize = 11.sp,
                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSel) Color.White else MediumSlate,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            item {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = SoftTileSurface,
                    border = BorderStroke(1.dp, BorderSlate),
                    modifier = Modifier.clickable { showNewLocationPrompt = true }
                ) {
                    Text(
                        text = "+ Spot",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = DarkSlate,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }
        }

        if (lowStockOnly || expiringOnly || searchQuery.isNotEmpty() || selectedCategory != "All" || selectedLocation != "All") {
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (lowStockOnly) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = DangerSoft,
                            modifier = Modifier.clickable { viewModel.lowStockOnlyFilter.value = false }
                        ) {
                            Text("Low Par ✕", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = DangerRed, modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp))
                        }
                    }
                    if (expiringOnly) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = WarningSoft,
                            modifier = Modifier.clickable { viewModel.expiringOnlyFilter.value = false }
                        ) {
                            Text("Expiring Soon ✕", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = WarningAmber, modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp))
                        }
                    }
                }
                Text(
                    text = "Reset All",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TealPrimary,
                    modifier = Modifier.clickable {
                        viewModel.searchQuery.value = ""
                        viewModel.categoryFilter.value = "All"
                        viewModel.locationFilter.value = "All"
                        viewModel.lowStockOnlyFilter.value = false
                        viewModel.expiringOnlyFilter.value = false
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 5. Inventory List (Flat or Grouped)
        if (items.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(top = 36.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(shape = CircleShape, color = CardSurface, shadowElevation = 2.dp, modifier = Modifier.size(60.dp)) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.FilterList, contentDescription = null, tint = LightSlate, modifier = Modifier.size(26.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("No rations match these filters", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = DarkSlate)
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = { showAddSheet = true },
                        colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Pick from Pre-Planned Catalog")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                when (groupMode) {
                    InventoryGroupMode.LIST -> {
                        items(items, key = { "inv_${it.id}" }) { item ->
                            ReformedInventoryCard(
                                item = item,
                                onCardClick = { editingItem = item },
                                onUse = { viewModel.useItem(item.id, 1.0) },
                                onRestock = { viewModel.restockItem(item.id, 1.0) }
                            )
                        }
                    }
                    InventoryGroupMode.BY_CATEGORY -> {
                        val grouped = items.groupBy { it.category.ifBlank { "Uncategorized" } }
                        grouped.forEach { (cat, list) ->
                            item(key = "hdr_cat_$cat") {
                                val visual = categoryVisualFor(cat)
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(top = 6.dp, bottom = 2.dp)
                                ) {
                                    Surface(shape = CircleShape, color = visual.softBg, modifier = Modifier.size(28.dp)) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(PreplannedCatalog.iconForCategory(cat), fontSize = 14.sp)
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(cat, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = MidnightSlate)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(shape = RoundedCornerShape(8.dp), color = visual.softBg) {
                                        Text("${list.size}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = visual.tint, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                    }
                                }
                            }
                            items(list, key = { "inv_cat_${cat}_${it.id}" }) { item ->
                                ReformedInventoryCard(
                                    item = item,
                                    onCardClick = { editingItem = item },
                                    onUse = { viewModel.useItem(item.id, 1.0) },
                                    onRestock = { viewModel.restockItem(item.id, 1.0) }
                                )
                            }
                        }
                    }
                    InventoryGroupMode.BY_LOCATION -> {
                        val grouped = items.groupBy { it.location.ifBlank { "Unassigned Spot" } }
                        grouped.forEach { (loc, list) ->
                            item(key = "hdr_loc_$loc") {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(top = 6.dp, bottom = 2.dp)
                                ) {
                                    Surface(shape = CircleShape, color = TealSoft, modifier = Modifier.size(28.dp)) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(Icons.Default.LocationOn, contentDescription = null, tint = TealPrimary, modifier = Modifier.size(15.dp))
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(loc, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = MidnightSlate)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(shape = RoundedCornerShape(8.dp), color = TealSoft) {
                                        Text("${list.size}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TealPrimary, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                    }
                                }
                            }
                            items(list, key = { "inv_loc_${loc}_${it.id}" }) { item ->
                                ReformedInventoryCard(
                                    item = item,
                                    onCardClick = { editingItem = item },
                                    onUse = { viewModel.useItem(item.id, 1.0) },
                                    onRestock = { viewModel.restockItem(item.id, 1.0) }
                                )
                            }
                        }
                    }
                }
                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }

    if (showAddSheet) {
        PreplannedAndCustomAddSheet(
            existingItems = allInventory,
            availableCategories = availableCategories,
            availableLocations = availableLocations,
            onCreateCategory = { viewModel.addCustomCategory(it) },
            onCreateLocation = { viewModel.addCustomLocation(it) },
            onDismiss = { showAddSheet = false },
            onConfirm = { newItem ->
                viewModel.addNewItem(newItem)
                Toast.makeText(context, "Added ${newItem.name} to Pantry", Toast.LENGTH_SHORT).show()
                showAddSheet = false
            }
        )
    }

    editingItem?.let { current ->
        InventoryItemEditorSheet(
            item = current,
            availableCategories = availableCategories,
            availableLocations = availableLocations,
            onCreateCategory = { viewModel.addCustomCategory(it) },
            onCreateLocation = { viewModel.addCustomLocation(it) },
            onDismiss = { editingItem = null },
            onSave = { updated ->
                viewModel.updateItem(updated)
                Toast.makeText(context, "Saved ${updated.name}", Toast.LENGTH_SHORT).show()
                editingItem = null
            },
            onQuickStep = { delta ->
                if (delta > 0) viewModel.restockItem(current.id, delta)
                else viewModel.useItem(current.id, -delta)
                editingItem = null
            },
            onAddToGroceries = {
                viewModel.addRestockItemToGroceries(current)
                Toast.makeText(context, "Added ${current.name} to Groceries", Toast.LENGTH_SHORT).show()
                editingItem = null
            },
            onDelete = {
                viewModel.deleteItem(current.id)
                Toast.makeText(context, "Deleted ${current.name}", Toast.LENGTH_SHORT).show()
                editingItem = null
            }
        )
    }

    if (showNewCategoryPrompt) {
        QuickNameDialog(
            title = "New Category",
            placeholder = "e.g. Halal Frozen Meats",
            onDismiss = { showNewCategoryPrompt = false },
            onConfirm = { catName ->
                viewModel.addCustomCategory(catName)
                viewModel.categoryFilter.value = catName
                showNewCategoryPrompt = false
            }
        )
    }

    if (showNewLocationPrompt) {
        QuickNameDialog(
            title = "New Storage Spot",
            placeholder = "e.g. Basement Freezer #2",
            onDismiss = { showNewLocationPrompt = false },
            onConfirm = { locName ->
                viewModel.addCustomLocation(locName)
                viewModel.locationFilter.value = locName
                showNewLocationPrompt = false
            }
        )
    }
}

@Composable
fun ReformedInventoryCard(
    item: InventoryItem,
    onCardClick: () -> Unit,
    onUse: () -> Unit,
    onRestock: () -> Unit
) {
    val visual = categoryVisualFor(item.category)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onCardClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = visual.softBg,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(PreplannedCatalog.iconForCategory(item.category), fontSize = 18.sp)
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = item.name,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MidnightSlate
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = item.category,
                                fontSize = 11.sp,
                                color = visual.tint,
                                fontWeight = FontWeight.SemiBold
                            )
                            if (item.location.isNotEmpty()) {
                                Text(text = " • ", fontSize = 11.sp, color = LightSlate)
                                Text(text = "📍 ${item.location}", fontSize = 11.sp, color = MediumSlate)
                            }
                        }
                    }
                }

                val (badgeBg, badgeText, badgeColor) = when {
                    item.isLowStock -> Triple(DangerSoft, "LOW PAR", DangerRed)
                    item.currentQty >= item.parLevel * 1.5 -> Triple(TealSoft, "SURPLUS", TealPrimary)
                    else -> Triple(SuccessSoft, "HEALTHY", SuccessGreen)
                }
                Surface(shape = RoundedCornerShape(10.dp), color = badgeBg) {
                    Text(
                        text = badgeText,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = badgeColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "%.0f".format(item.currentQty),
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Black,
                            color = MidnightSlate
                        )
                        Text(
                            text = " / %.0f %s par".format(item.parLevel, item.unit),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MediumSlate,
                            modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                        )
                    }

                    val rawRatio = if (item.parLevel > 0) (item.currentQty / item.parLevel).toFloat() else 1f
                    val progressRatio = if (rawRatio.isNaN() || rawRatio.isInfinite()) 0f else rawRatio.coerceIn(0f, 1f)
                    LinearProgressIndicator(
                        progress = { progressRatio },
                        modifier = Modifier.fillMaxWidth(0.88f).height(6.dp),
                        color = if (item.isLowStock) OrangeAccent else visual.tint,
                        trackColor = Color(0xFFF1F5F9)
                    )
                }

                // Tactile Pill Steppers
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = SoftTileSurface,
                        border = BorderStroke(1.dp, BorderSlate),
                        modifier = Modifier.size(38.dp)
                    ) {
                        IconButton(onClick = onUse, enabled = item.currentQty > 0) {
                            Icon(Icons.Default.Remove, contentDescription = "Use 1", tint = if (item.currentQty > 0) DarkSlate else LightSlate, modifier = Modifier.size(18.dp))
                        }
                    }

                    Surface(
                        shape = CircleShape,
                        color = InkBlack,
                        modifier = Modifier.size(38.dp)
                    ) {
                        IconButton(onClick = onRestock) {
                            Icon(Icons.Default.Add, contentDescription = "Restock 1", tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!item.earliestExpiry.isNullOrEmpty()) {
                    val expColor = if (item.isExpired) DangerRed else if (item.isExpiringSoon) WarningAmber else SuccessGreen
                    val expBg = if (item.isExpired) DangerSoft else if (item.isExpiringSoon) WarningSoft else SuccessSoft
                    Surface(shape = RoundedCornerShape(8.dp), color = expBg) {
                        val expText = when {
                            item.isExpired -> "EXPIRED (${item.earliestExpiry})"
                            item.daysUntilExpiry != null -> "Exp: ${item.earliestExpiry} (${item.daysUntilExpiry}d)"
                            else -> "Exp: ${item.earliestExpiry}"
                        }
                        Text(expText, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = expColor, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                    }
                } else {
                    Text(
                        text = if (item.totalServings > 0) "%.0f servings".format(item.totalServings) else "",
                        fontSize = 11.sp,
                        color = LightSlate
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(12.dp), tint = LightSlate)
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("Tap to edit", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = LightSlate)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreplannedAndCustomAddSheet(
    existingItems: List<InventoryItem>,
    availableCategories: List<String>,
    availableLocations: List<String>,
    onCreateCategory: (String) -> Unit,
    onCreateLocation: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: (InventoryItem) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedTab by remember { mutableStateOf(0) }
    var catalogSearch by remember { mutableStateOf("") }

    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(availableCategories.firstOrNull() ?: "Grains & Pasta") }
    var unit by remember { mutableStateOf("lbs") }
    var location by remember { mutableStateOf(availableLocations.firstOrNull() ?: "Main Pantry") }
    var initialQtyStr by remember { mutableStateOf("6") }
    var parLevelStr by remember { mutableStateOf("12") }
    var earliestExpiry by remember { mutableStateOf(LocalDate.now().plusYears(1).format(DateTimeFormatter.ISO_LOCAL_DATE)) }
    var servingsStr by remember { mutableStateOf("4") }
    var notes by remember { mutableStateOf("") }

    var showNewCatDialog by remember { mutableStateOf(false) }
    var showNewLocDialog by remember { mutableStateOf(false) }

    val existingNames = remember(existingItems) { existingItems.map { it.name.trim().lowercase() }.toSet() }

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
        ) {
            Text("Add Pantry Ration", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = MidnightSlate)
            Spacer(modifier = Modifier.height(10.dp))

            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = SoftTileSurface,
                contentColor = InkBlack
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Pre-Planned (${PreplannedCatalog.plannedRations.size})", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Custom / New", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (selectedTab == 0) {
                val filteredTemplates = PreplannedCatalog.plannedRations.filter {
                    catalogSearch.isBlank() ||
                        it.name.contains(catalogSearch, ignoreCase = true) ||
                        it.category.contains(catalogSearch, ignoreCase = true)
                }
                Column(modifier = Modifier.fillMaxWidth().heightIn(max = 460.dp)) {
                    OutlinedTextField(
                        value = catalogSearch,
                        onValueChange = { catalogSearch = it },
                        placeholder = { Text("Filter pre-planned items...", fontSize = 13.sp) },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(filteredTemplates, key = { it.id }) { tpl ->
                            val alreadyIn = existingNames.contains(tpl.name.lowercase())
                            val visual = categoryVisualFor(tpl.category)
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = if (alreadyIn) SoftTileSurface else CardSurface,
                                border = BorderStroke(1.dp, BorderSlate),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        name = tpl.name
                                        category = tpl.category
                                        unit = tpl.unit
                                        location = tpl.location
                                        initialQtyStr = "%.0f".format(tpl.defaultQty)
                                        parLevelStr = "%.0f".format(tpl.parLevel)
                                        earliestExpiry = tpl.defaultMonthsExpiry?.let {
                                            LocalDate.now().plusMonths(it).format(DateTimeFormatter.ISO_LOCAL_DATE)
                                        } ?: ""
                                        servingsStr = "%.0f".format(tpl.servingsPerUnit)
                                        notes = tpl.notes
                                        selectedTab = 1
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                                        Surface(shape = CircleShape, color = visual.softBg, modifier = Modifier.size(36.dp)) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(PreplannedCatalog.iconForCategory(tpl.category), fontSize = 16.sp)
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(tpl.name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MidnightSlate)
                                            Text("${tpl.category} • Par: %.0f ${tpl.unit}".format(tpl.parLevel), fontSize = 11.sp, color = MediumSlate)
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Button(
                                        onClick = { onConfirm(tpl.toInventoryItem()) },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = if (alreadyIn) InkBlack else TealPrimary),
                                        modifier = Modifier.height(36.dp)
                                    ) {
                                        Text(if (alreadyIn) "+ Add" else "Quick Add", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 460.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Item Name *") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("Category", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MidnightSlate)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(availableCategories) { cat ->
                            val isSel = category.equals(cat, ignoreCase = true)
                            FilterChip(
                                selected = isSel,
                                onClick = { category = cat },
                                label = { Text("${PreplannedCatalog.iconForCategory(cat)} $cat", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = OrangeAccent, selectedLabelColor = Color.White)
                            )
                        }
                        item {
                            FilterChip(
                                selected = false,
                                onClick = { showNewCatDialog = true },
                                label = { Text("+ New Category", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TealPrimary) },
                                colors = FilterChipDefaults.filterChipColors(containerColor = TealSoft)
                            )
                        }
                    }

                    Text("Storage Spot", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MidnightSlate)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(availableLocations) { loc ->
                            val isSel = location.equals(loc, ignoreCase = true)
                            FilterChip(
                                selected = isSel,
                                onClick = { location = loc },
                                label = { Text("📍 $loc", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = InkBlack, selectedLabelColor = Color.White)
                            )
                        }
                        item {
                            FilterChip(
                                selected = false,
                                onClick = { showNewLocDialog = true },
                                label = { Text("+ Spot", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                colors = FilterChipDefaults.filterChipColors(containerColor = SoftTileSurface)
                            )
                        }
                    }

                    Text("Unit / Pack Size", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MidnightSlate)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(PreplannedCatalog.standardUnits) { u ->
                            val isSel = unit.equals(u, ignoreCase = true)
                            FilterChip(
                                selected = isSel,
                                onClick = { unit = u },
                                label = { Text(u, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = TealPrimary, selectedLabelColor = Color.White)
                            )
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = initialQtyStr,
                            onValueChange = { initialQtyStr = it },
                            label = { Text("Initial Qty") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = parLevelStr,
                            onValueChange = { parLevelStr = it },
                            label = { Text("Par Goal") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = servingsStr,
                            onValueChange = { servingsStr = it },
                            label = { Text("Serv/Unit") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(0.9f)
                        )
                    }

                    // Scrollable Wheel Date Picker!
                    Text("Expiration Date (Scrollable)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MidnightSlate)
                    ScrollableWheelDatePicker(
                        initialDate = earliestExpiry,
                        onDateSelected = { earliestExpiry = it }
                    )

                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Notes (optional)") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = {
                            if (name.isNotBlank()) {
                                val initQty = initialQtyStr.toDoubleOrNull() ?: 0.0
                                val par = parLevelStr.toDoubleOrNull() ?: 0.0
                                val serv = servingsStr.toDoubleOrNull() ?: 1.0
                                val newItem = InventoryItem(
                                    id = "RATION-" + UUID.randomUUID().toString().take(6).uppercase(),
                                    name = name.trim(),
                                    category = category.trim(),
                                    unit = unit.trim(),
                                    location = location.trim(),
                                    initialQty = initQty,
                                    stockIn = 0.0,
                                    stockOut = 0.0,
                                    currentQty = initQty,
                                    parLevel = par,
                                    restockNeeded = maxOf(0.0, par - initQty),
                                    earliestExpiry = earliestExpiry.trim().ifEmpty { null },
                                    expirationStatus = if (earliestExpiry.isBlank()) "UNKNOWN" else "GOOD",
                                    stockStatus = if (initQty < par) "LOW" else "OK",
                                    servingsPerUnit = serv,
                                    totalServings = initQty * serv,
                                    notes = notes.trim(),
                                    syncPending = true
                                )
                                onConfirm(newItem)
                            }
                        },
                        enabled = name.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = InkBlack),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Text("Save Ration to Pantry & Sync", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (showNewCatDialog) {
        QuickNameDialog(
            title = "New Category",
            placeholder = "e.g. Halal Frozen Meats",
            onDismiss = { showNewCatDialog = false },
            onConfirm = {
                onCreateCategory(it)
                category = it
                showNewCatDialog = false
            }
        )
    }

    if (showNewLocDialog) {
        QuickNameDialog(
            title = "New Storage Spot",
            placeholder = "e.g. Garage Shelf B",
            onDismiss = { showNewLocDialog = false },
            onConfirm = {
                onCreateLocation(it)
                location = it
                showNewLocDialog = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryItemEditorSheet(
    item: InventoryItem,
    availableCategories: List<String>,
    availableLocations: List<String>,
    onCreateCategory: (String) -> Unit,
    onCreateLocation: (String) -> Unit,
    onDismiss: () -> Unit,
    onSave: (InventoryItem) -> Unit,
    onQuickStep: (Double) -> Unit,
    onAddToGroceries: () -> Unit,
    onDelete: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var name by remember { mutableStateOf(item.name) }
    var category by remember { mutableStateOf(item.category) }
    var unit by remember { mutableStateOf(item.unit.ifBlank { "lbs" }) }
    var location by remember { mutableStateOf(item.location.ifBlank { "Main Pantry" }) }
    var currentQtyStr by remember { mutableStateOf("%.0f".format(item.currentQty)) }
    var parLevelStr by remember { mutableStateOf("%.0f".format(item.parLevel)) }
    var earliestExpiry by remember { mutableStateOf(item.earliestExpiry ?: "") }
    var servingsStr by remember { mutableStateOf("%.0f".format(item.servingsPerUnit)) }
    var notes by remember { mutableStateOf(item.notes) }

    var confirmDelete by remember { mutableStateOf(false) }
    var showNewCatDialog by remember { mutableStateOf(false) }
    var showNewLocDialog by remember { mutableStateOf(false) }

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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Edit Ration Settings", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = MidnightSlate)
                Surface(
                    shape = CircleShape,
                    color = DangerSoft,
                    modifier = Modifier.size(36.dp).clickable { confirmDelete = !confirmDelete }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = DangerRed, modifier = Modifier.size(18.dp))
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
                        Text("Remove '${item.name}' permanently?", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = DangerRed)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = onDelete, colors = ButtonDefaults.buttonColors(containerColor = DangerRed)) {
                                Text("Confirm Delete")
                            }
                            TextButton(onClick = { confirmDelete = false }) {
                                Text("Cancel", color = DarkSlate)
                            }
                        }
                    }
                }
            }

            // Quick Stock Adjust Bar
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = SoftTileSurface,
                border = BorderStroke(1.dp, BorderSlate),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Quick Adjust (${item.unit})", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MediumSlate)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedButton(onClick = { onQuickStep(-5.0) }, shape = RoundedCornerShape(10.dp), modifier = Modifier.height(36.dp)) {
                            Text("-5", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = DarkSlate)
                        }
                        OutlinedButton(onClick = { onQuickStep(-1.0) }, shape = RoundedCornerShape(10.dp), modifier = Modifier.height(36.dp)) {
                            Text("-1", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = DarkSlate)
                        }
                        Button(onClick = { onQuickStep(1.0) }, shape = RoundedCornerShape(10.dp), colors = ButtonDefaults.buttonColors(containerColor = TealPrimary), modifier = Modifier.height(36.dp)) {
                            Text("+1", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Button(onClick = { onQuickStep(5.0) }, shape = RoundedCornerShape(10.dp), colors = ButtonDefaults.buttonColors(containerColor = TealPrimary), modifier = Modifier.height(36.dp)) {
                            Text("+5", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Item Name") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Text("Category", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MidnightSlate)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(availableCategories) { cat ->
                    val isSel = category.equals(cat, ignoreCase = true)
                    FilterChip(
                        selected = isSel,
                        onClick = { category = cat },
                        label = { Text("${PreplannedCatalog.iconForCategory(cat)} $cat", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = OrangeAccent, selectedLabelColor = Color.White)
                    )
                }
                item {
                    FilterChip(
                        selected = false,
                        onClick = { showNewCatDialog = true },
                        label = { Text("+ New", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TealPrimary) },
                        colors = FilterChipDefaults.filterChipColors(containerColor = TealSoft)
                    )
                }
            }

            Text("Storage Spot", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MidnightSlate)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(availableLocations) { loc ->
                    val isSel = location.equals(loc, ignoreCase = true)
                    FilterChip(
                        selected = isSel,
                        onClick = { location = loc },
                        label = { Text("📍 $loc", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = InkBlack, selectedLabelColor = Color.White)
                    )
                }
                item {
                    FilterChip(
                        selected = false,
                        onClick = { showNewLocDialog = true },
                        label = { Text("+ New", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(containerColor = SoftTileSurface)
                    )
                }
            }

            Text("Unit", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MidnightSlate)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(PreplannedCatalog.standardUnits) { u ->
                    val isSel = unit.equals(u, ignoreCase = true)
                    FilterChip(
                        selected = isSel,
                        onClick = { unit = u },
                        label = { Text(u, fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = TealPrimary, selectedLabelColor = Color.White)
                    )
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = currentQtyStr,
                    onValueChange = { currentQtyStr = it },
                    label = { Text("Current Qty") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = parLevelStr,
                    onValueChange = { parLevelStr = it },
                    label = { Text("Par Goal") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = servingsStr,
                    onValueChange = { servingsStr = it },
                    label = { Text("Serv/Unit") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(0.9f)
                )
            }

            // Scrollable Drum Wheel Date Picker
            Text("Expiration Date (Scrollable)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MidnightSlate)
            ScrollableWheelDatePicker(
                initialDate = earliestExpiry,
                onDateSelected = { earliestExpiry = it }
            )

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedButton(
                onClick = onAddToGroceries,
                modifier = Modifier.fillMaxWidth().height(42.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, TealPrimary.copy(alpha = 0.4f))
            ) {
                Icon(Icons.Default.AddShoppingCart, contentDescription = null, modifier = Modifier.size(16.dp), tint = TealPrimary)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Add Deficit to Shopping List", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TealPrimary)
            }

            Button(
                onClick = {
                    val newQty = currentQtyStr.toDoubleOrNull() ?: item.currentQty
                    val newPar = parLevelStr.toDoubleOrNull() ?: item.parLevel
                    val newServ = servingsStr.toDoubleOrNull() ?: item.servingsPerUnit
                    val delta = newQty - item.currentQty
                    val newStockIn = if (delta > 0) item.stockIn + delta else item.stockIn
                    val newStockOut = if (delta < 0) item.stockOut + (-delta) else item.stockOut

                    onSave(
                        item.copy(
                            name = name.trim().ifEmpty { item.name },
                            category = category.trim(),
                            unit = unit.trim(),
                            location = location.trim(),
                            currentQty = newQty,
                            stockIn = newStockIn,
                            stockOut = newStockOut,
                            parLevel = newPar,
                            restockNeeded = maxOf(0.0, newPar - newQty),
                            earliestExpiry = earliestExpiry.trim().ifEmpty { null },
                            servingsPerUnit = newServ,
                            totalServings = newQty * newServ,
                            stockStatus = if (newQty < newPar) "LOW" else "OK",
                            notes = notes.trim()
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = InkBlack),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Text("Save Changes & Sync", fontWeight = FontWeight.Bold)
            }
        }
    }

    if (showNewCatDialog) {
        QuickNameDialog(
            title = "New Category",
            placeholder = "e.g. Halal Meats",
            onDismiss = { showNewCatDialog = false },
            onConfirm = {
                onCreateCategory(it)
                category = it
                showNewCatDialog = false
            }
        )
    }

    if (showNewLocDialog) {
        QuickNameDialog(
            title = "New Storage Spot",
            placeholder = "e.g. Garage Freezer",
            onDismiss = { showNewLocDialog = false },
            onConfirm = {
                onCreateLocation(it)
                location = it
                showNewLocDialog = false
            }
        )
    }
}

@Composable
fun QuickNameDialog(
    title: String,
    placeholder: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var value by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(18.dp),
        containerColor = Color.White,
        title = { Text(title, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = MidnightSlate) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                placeholder = { Text(placeholder) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = { if (value.isNotBlank()) onConfirm(value.trim()) },
                colors = ButtonDefaults.buttonColors(containerColor = InkBlack),
                enabled = value.isNotBlank(),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
