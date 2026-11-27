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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.emergencysupply.rations.data.PlannedRationTemplate
import com.emergencysupply.rations.data.PreplannedCatalog
import com.emergencysupply.rations.data.local.entity.InventoryItem
import com.emergencysupply.rations.ui.MainViewModel
import com.emergencysupply.rations.ui.theme.BackgroundSurface
import com.emergencysupply.rations.ui.theme.BorderSlate
import com.emergencysupply.rations.ui.theme.CardSurface
import com.emergencysupply.rations.ui.theme.DangerRed
import com.emergencysupply.rations.ui.theme.DangerSoft
import com.emergencysupply.rations.ui.theme.DarkSlate
import com.emergencysupply.rations.ui.theme.LightSlate
import com.emergencysupply.rations.ui.theme.MediumSlate
import com.emergencysupply.rations.ui.theme.MidnightSlate
import com.emergencysupply.rations.ui.theme.SuccessGreen
import com.emergencysupply.rations.ui.theme.SuccessSoft
import com.emergencysupply.rations.ui.theme.TealPrimary
import com.emergencysupply.rations.ui.theme.TealSoft
import com.emergencysupply.rations.ui.theme.WarningAmber
import com.emergencysupply.rations.ui.theme.WarningSoft
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID

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

    var showAddDialog by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<InventoryItem?>(null) }
    var showNewCategoryPrompt by remember { mutableStateOf(false) }
    var showNewLocationPrompt by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = TealPrimary,
                contentColor = Color.White,
                shape = CircleShape,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp)
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Ration", modifier = Modifier.size(24.dp))
            }
        },
        containerColor = BackgroundSurface
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // 1. Clean Rounded Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.searchQuery.value = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search rations, category, or location...", fontSize = 13.sp, color = LightSlate) },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = MediumSlate, modifier = Modifier.size(20.dp))
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.searchQuery.value = "" }) {
                            Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear", tint = MediumSlate, modifier = Modifier.size(18.dp))
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = TealPrimary,
                    unfocusedBorderColor = BorderSlate,
                    focusedContainerColor = CardSurface,
                    unfocusedContainerColor = CardSurface
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            // 2. Dynamic Category Filter Row + Add Category Chip
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                item {
                    val isAll = selectedCategory == "All"
                    FilterChip(
                        selected = isAll,
                        onClick = { viewModel.categoryFilter.value = "All" },
                        label = { Text("All", fontSize = 12.sp, fontWeight = if (isAll) FontWeight.Bold else FontWeight.Medium) },
                        shape = RoundedCornerShape(20.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = TealPrimary,
                            selectedLabelColor = Color.White,
                            containerColor = CardSurface,
                            labelColor = DarkSlate
                        ),
                        border = BorderStroke(1.dp, if (isAll) TealPrimary else BorderSlate)
                    )
                }

                items(availableCategories) { cat ->
                    val isSelected = selectedCategory.equals(cat, ignoreCase = true)
                    val icon = PreplannedCatalog.iconForCategory(cat)
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.categoryFilter.value = cat },
                        label = { Text("$icon $cat", fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium) },
                        shape = RoundedCornerShape(20.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = TealPrimary,
                            selectedLabelColor = Color.White,
                            containerColor = CardSurface,
                            labelColor = DarkSlate
                        ),
                        border = BorderStroke(1.dp, if (isSelected) TealPrimary else BorderSlate)
                    )
                }

                item {
                    FilterChip(
                        selected = false,
                        onClick = { showNewCategoryPrompt = true },
                        label = { Text("+ New Category", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TealPrimary) },
                        shape = RoundedCornerShape(20.dp),
                        colors = FilterChipDefaults.filterChipColors(containerColor = TealSoft),
                        border = BorderStroke(1.dp, TealPrimary.copy(alpha = 0.35f))
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // 3. Dynamic Location Filter Row + Add Location Chip
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                item {
                    val isAll = selectedLocation == "All"
                    FilterChip(
                        selected = isAll,
                        onClick = { viewModel.locationFilter.value = "All" },
                        label = { Text("All Locations", fontSize = 11.sp, fontWeight = if (isAll) FontWeight.Bold else FontWeight.Normal) },
                        shape = RoundedCornerShape(20.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = DarkSlate,
                            selectedLabelColor = Color.White,
                            containerColor = CardSurface,
                            labelColor = MediumSlate
                        ),
                        border = BorderStroke(1.dp, if (isAll) DarkSlate else BorderSlate)
                    )
                }

                items(availableLocations) { loc ->
                    val isSelected = selectedLocation.equals(loc, ignoreCase = true)
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.locationFilter.value = loc },
                        label = { Text("📍 $loc", fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                        shape = RoundedCornerShape(20.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = DarkSlate,
                            selectedLabelColor = Color.White,
                            containerColor = CardSurface,
                            labelColor = MediumSlate
                        ),
                        border = BorderStroke(1.dp, if (isSelected) DarkSlate else BorderSlate)
                    )
                }

                item {
                    FilterChip(
                        selected = false,
                        onClick = { showNewLocationPrompt = true },
                        label = { Text("+ Location", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = DarkSlate) },
                        shape = RoundedCornerShape(20.dp),
                        colors = FilterChipDefaults.filterChipColors(containerColor = Color(0xFFF1F5F9)),
                        border = BorderStroke(1.dp, BorderSlate)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Active status pills + Reset row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "${items.size} rations",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MediumSlate
                    )
                    if (lowStockOnly) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = DangerSoft,
                            modifier = Modifier.clickable { viewModel.lowStockOnlyFilter.value = false }
                        ) {
                            Text("Low Stock Only ✕", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = DangerRed, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    }
                    if (expiringOnly) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = WarningSoft,
                            modifier = Modifier.clickable { viewModel.expiringOnlyFilter.value = false }
                        ) {
                            Text("Expiring Soon ✕", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = WarningAmber, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    }
                }

                if (searchQuery.isNotEmpty() || selectedCategory != "All" || selectedLocation != "All" || lowStockOnly || expiringOnly) {
                    Text(
                        text = "Reset Filters",
                        fontSize = 12.sp,
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

            // 4. Inventory List
            if (items.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 40.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFF1F5F9),
                            modifier = Modifier.size(56.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(imageVector = Icons.Default.FilterList, contentDescription = null, tint = LightSlate, modifier = Modifier.size(28.dp))
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No rations match your filters.",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = DarkSlate
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = { showAddDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Add From Pre-Planned List")
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(items, key = { "inv_${it.id}" }) { item ->
                        ReformedInventoryCard(
                            item = item,
                            onCardClick = { editingItem = item },
                            onUse = { viewModel.useItem(item.id, 1.0) },
                            onRestock = { viewModel.restockItem(item.id, 1.0) }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(72.dp)) }
                }
            }
        }
    }

    if (showAddDialog) {
        PreplannedAndCustomAddDialog(
            existingItems = allInventory,
            availableCategories = availableCategories,
            availableLocations = availableLocations,
            onCreateCategory = { viewModel.addCustomCategory(it) },
            onCreateLocation = { viewModel.addCustomLocation(it) },
            onDismiss = { showAddDialog = false },
            onConfirm = { newItem ->
                viewModel.addNewItem(newItem)
                Toast.makeText(context, "Added ${newItem.name} to Pantry", Toast.LENGTH_SHORT).show()
                showAddDialog = false
            }
        )
    }

    editingItem?.let { current ->
        InventoryItemEditorDialog(
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
            title = "Add Brand New Category",
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
            title = "Add Storage Location",
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
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onCardClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        border = BorderStroke(1.dp, BorderSlate),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = PreplannedCatalog.iconForCategory(item.category),
                        fontSize = 18.sp,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(
                        text = item.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MidnightSlate
                    )
                }

                val (badgeBg, badgeText, badgeColor) = when {
                    item.isLowStock -> Triple(DangerSoft, "LOW STOCK", DangerRed)
                    item.currentQty >= item.parLevel * 1.5 -> Triple(TealSoft, "SURPLUS", TealPrimary)
                    else -> Triple(SuccessSoft, "IN STOCK", SuccessGreen)
                }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = badgeBg,
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Text(
                        text = badgeText,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = badgeColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.category,
                        fontSize = 12.sp,
                        color = TealPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (item.location.isNotEmpty()) {
                        Text(text = " • ", fontSize = 12.sp, color = LightSlate)
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = LightSlate
                        )
                        Text(text = item.location, fontSize = 12.sp, color = MediumSlate)
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(12.dp), tint = LightSlate)
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("Tap to edit", fontSize = 10.sp, color = LightSlate)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

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
                    val progressColor = if (item.isLowStock) WarningAmber else SuccessGreen
                    LinearProgressIndicator(
                        progress = { progressRatio },
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .height(5.dp),
                        color = progressColor,
                        trackColor = Color(0xFFF1F5F9)
                    )

                    if (item.totalServings > 0) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "%.0f total servings (%.0f/unit)".format(item.totalServings, item.servingsPerUnit),
                            fontSize = 11.sp,
                            color = LightSlate
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (item.currentQty > 0) Color(0xFFF1F5F9) else Color(0xFFF8FAFC),
                        border = BorderStroke(1.dp, BorderSlate),
                        modifier = Modifier.size(40.dp)
                    ) {
                        IconButton(
                            onClick = onUse,
                            enabled = item.currentQty > 0,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Remove,
                                contentDescription = "Use 1",
                                tint = if (item.currentQty > 0) DarkSlate else LightSlate,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = TealSoft,
                        border = BorderStroke(1.dp, TealPrimary.copy(alpha = 0.3f)),
                        modifier = Modifier.size(40.dp)
                    ) {
                        IconButton(
                            onClick = onRestock,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Restock 1",
                                tint = TealPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            if (!item.earliestExpiry.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                val expColor = if (item.isExpired) DangerRed else if (item.isExpiringSoon) WarningAmber else SuccessGreen
                val expBadgeBg = if (item.isExpired) DangerSoft else if (item.isExpiringSoon) WarningSoft else SuccessSoft
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = expBadgeBg
                ) {
                    val expText = when {
                        item.isExpired -> "EXPIRED: ${item.earliestExpiry}"
                        item.daysUntilExpiry != null -> "Expires: ${item.earliestExpiry} (${item.daysUntilExpiry} days left)"
                        else -> "Expires: ${item.earliestExpiry}"
                    }
                    Text(
                        text = expText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = expColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun PreplannedAndCustomAddDialog(
    existingItems: List<InventoryItem>,
    availableCategories: List<String>,
    availableLocations: List<String>,
    onCreateCategory: (String) -> Unit,
    onCreateLocation: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: (InventoryItem) -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) } // 0 = Pre-Planned Catalog, 1 = Custom Item
    var catalogSearch by remember { mutableStateOf("") }

    // Custom Form State
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

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(18.dp),
        containerColor = Color.White,
        title = {
            Column {
                Text("Add Pantry Ration", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MidnightSlate)
                Spacer(modifier = Modifier.height(8.dp))
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color(0xFFF8FAFC),
                    contentColor = TealPrimary
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
            }
        },
        text = {
            if (selectedTab == 0) {
                // Scrollable Pre-Planned Family Items List
                val filteredTemplates = PreplannedCatalog.plannedRations.filter {
                    catalogSearch.isBlank() ||
                        it.name.contains(catalogSearch, ignoreCase = true) ||
                        it.category.contains(catalogSearch, ignoreCase = true)
                }
                Column(modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp)) {
                    OutlinedTextField(
                        value = catalogSearch,
                        onValueChange = { catalogSearch = it },
                        placeholder = { Text("Filter pre-planned foods...", fontSize = 12.sp) },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(filteredTemplates, key = { it.id }) { tpl ->
                            val alreadyIn = existingNames.contains(tpl.name.lowercase())
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (alreadyIn) Color(0xFFF8FAFC) else CardSurface,
                                border = BorderStroke(1.dp, BorderSlate),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        // Pre-fill Custom tab so user can tweak before adding if desired
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
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${PreplannedCatalog.iconForCategory(tpl.category)} ${tpl.name}",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MidnightSlate
                                        )
                                        Text(
                                            text = "${tpl.category} • Par: %.0f ${tpl.unit} • 📍 ${tpl.location}".format(tpl.parLevel),
                                            fontSize = 11.sp,
                                            color = MediumSlate
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Button(
                                        onClick = { onConfirm(tpl.toInventoryItem()) },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (alreadyIn) DarkSlate else TealPrimary
                                        ),
                                        modifier = Modifier.height(34.dp)
                                    ) {
                                        Text(if (alreadyIn) "+ Add" else "Quick Add", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // Scrollable Picker-Driven Custom Item Form
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Item Name *") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Category Chip Picker + Create New
                    Text("Category", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MidnightSlate)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(availableCategories) { cat ->
                            val isSel = category.equals(cat, ignoreCase = true)
                            FilterChip(
                                selected = isSel,
                                onClick = { category = cat },
                                label = { Text("${PreplannedCatalog.iconForCategory(cat)} $cat", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = TealPrimary,
                                    selectedLabelColor = Color.White
                                )
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

                    // Location Chip Picker + Create New
                    Text("Storage Location", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MidnightSlate)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(availableLocations) { loc ->
                            val isSel = location.equals(loc, ignoreCase = true)
                            FilterChip(
                                selected = isSel,
                                onClick = { location = loc },
                                label = { Text("📍 $loc", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = DarkSlate,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                        item {
                            FilterChip(
                                selected = false,
                                onClick = { showNewLocDialog = true },
                                label = { Text("+ Location", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                colors = FilterChipDefaults.filterChipColors(containerColor = Color(0xFFF1F5F9))
                            )
                        }
                    }

                    // Unit Chip Picker
                    Text("Unit / Pack Size", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MidnightSlate)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(PreplannedCatalog.standardUnits) { u ->
                            val isSel = unit.equals(u, ignoreCase = true)
                            FilterChip(
                                selected = isSel,
                                onClick = { unit = u },
                                label = { Text(u, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = TealPrimary,
                                    selectedLabelColor = Color.White
                                )
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
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = parLevelStr,
                            onValueChange = { parLevelStr = it },
                            label = { Text("Par Goal") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Quick Expiry Chips + Date Input
                    Text("Expiration Date", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MidnightSlate)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        val presets = listOf(
                            "+3 mo" to 3L,
                            "+6 mo" to 6L,
                            "+1 yr" to 12L,
                            "+2 yrs" to 24L
                        )
                        items(presets) { (label, mos) ->
                            FilterChip(
                                selected = false,
                                onClick = {
                                    earliestExpiry = LocalDate.now().plusMonths(mos).format(DateTimeFormatter.ISO_LOCAL_DATE)
                                },
                                label = { Text(label, fontSize = 11.sp) }
                            )
                        }
                        item {
                            FilterChip(
                                selected = earliestExpiry.isEmpty(),
                                onClick = { earliestExpiry = "" },
                                label = { Text("No Expiry", fontSize = 11.sp) }
                            )
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = earliestExpiry,
                            onValueChange = { earliestExpiry = it },
                            label = { Text("YYYY-MM-DD") },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1.3f)
                        )
                        OutlinedTextField(
                            value = servingsStr,
                            onValueChange = { servingsStr = it },
                            label = { Text("Servings/Unit") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Notes (optional)") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            if (selectedTab == 1) {
                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            val initQty = initialQtyStr.toDoubleOrNull() ?: 0.0
                            val par = parLevelStr.toDoubleOrNull() ?: 0.0
                            val serv = servingsStr.toDoubleOrNull() ?: 1.0
                            val item = InventoryItem(
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
                            onConfirm(item)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                    shape = RoundedCornerShape(10.dp),
                    enabled = name.isNotBlank()
                ) {
                    Text("Save Ration", fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = MediumSlate)
            }
        }
    )

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
            title = "New Storage Location",
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

@Composable
fun InventoryItemEditorDialog(
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

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(18.dp),
        containerColor = Color.White,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Edit Ration Settings", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = MidnightSlate)
                IconButton(onClick = { confirmDelete = true }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = DangerRed, modifier = Modifier.size(20.dp))
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 440.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (confirmDelete) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = DangerSoft,
                        border = BorderStroke(1.dp, DangerRed.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Delete '${item.name}' from Google Sheet & Phone?", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = DangerRed)
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

                // Quick Bulk Stepper Row
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF8FAFC),
                    border = BorderStroke(1.dp, BorderSlate),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Quick Stock Adjust", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MediumSlate)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            OutlinedButton(onClick = { onQuickStep(-5.0) }, modifier = Modifier.height(34.dp)) {
                                Text("-5", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            OutlinedButton(onClick = { onQuickStep(-1.0) }, modifier = Modifier.height(34.dp)) {
                                Text("-1", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            Button(onClick = { onQuickStep(1.0) }, colors = ButtonDefaults.buttonColors(containerColor = TealPrimary), modifier = Modifier.height(34.dp)) {
                                Text("+1", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            Button(onClick = { onQuickStep(5.0) }, colors = ButtonDefaults.buttonColors(containerColor = TealPrimary), modifier = Modifier.height(34.dp)) {
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
                    shape = RoundedCornerShape(10.dp),
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
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = TealPrimary, selectedLabelColor = Color.White)
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

                Text("Storage Location", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MidnightSlate)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(availableLocations) { loc ->
                        val isSel = location.equals(loc, ignoreCase = true)
                        FilterChip(
                            selected = isSel,
                            onClick = { location = loc },
                            label = { Text("📍 $loc", fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = DarkSlate, selectedLabelColor = Color.White)
                        )
                    }
                    item {
                        FilterChip(
                            selected = false,
                            onClick = { showNewLocDialog = true },
                            label = { Text("+ New", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            colors = FilterChipDefaults.filterChipColors(containerColor = Color(0xFFF1F5F9))
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
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = parLevelStr,
                        onValueChange = { parLevelStr = it },
                        label = { Text("Par Goal") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = earliestExpiry,
                        onValueChange = { earliestExpiry = it },
                        label = { Text("Expiry (YYYY-MM-DD)") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1.3f)
                    )
                    OutlinedTextField(
                        value = servingsStr,
                        onValueChange = { servingsStr = it },
                        label = { Text("Servings/Unit") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedButton(
                    onClick = onAddToGroceries,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.AddShoppingCart, contentDescription = null, modifier = Modifier.size(16.dp), tint = TealPrimary)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Add Deficit to Groceries List", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TealPrimary)
                }
            }
        },
        confirmButton = {
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
                colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Save Changes", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = MediumSlate)
            }
        }
    )

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
            title = "New Storage Location",
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
        shape = RoundedCornerShape(14.dp),
        containerColor = Color.White,
        title = { Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MidnightSlate) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                placeholder = { Text(placeholder) },
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = { if (value.isNotBlank()) onConfirm(value.trim()) },
                colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                enabled = value.isNotBlank()
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
