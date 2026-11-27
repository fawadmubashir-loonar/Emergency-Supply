package com.emergencysupply.rations.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.filled.Clear
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emergencysupply.rations.data.local.entity.InventoryItem
import com.emergencysupply.rations.ui.MainViewModel
import com.emergencysupply.rations.ui.theme.DangerRed
import com.emergencysupply.rations.ui.theme.DarkSlate
import com.emergencysupply.rations.ui.theme.MidnightSlate
import com.emergencysupply.rations.ui.theme.SuccessGreen
import com.emergencysupply.rations.ui.theme.TealAccent
import com.emergencysupply.rations.ui.theme.TealPrimary
import com.emergencysupply.rations.ui.theme.TealSoft
import com.emergencysupply.rations.ui.theme.WarningAmber
import java.util.UUID

@Composable
fun InventoryScreen(viewModel: MainViewModel) {
    val items by viewModel.filteredInventory.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCategory by viewModel.categoryFilter.collectAsState()
    val selectedLocation by viewModel.locationFilter.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }

    val categories = listOf(
        "All", "Water & Drinks", "Grains & Rice", "Canned Goods",
        "Halal Meats & Proteins", "Dairy & Powders", "Snacks & Bars", "First Aid & Medical"
    )

    val locations = listOf(
        "All", "Basement Shelf A", "Kitchen Pantry", "Deep Freezer", "Garage Storage", "Bug Out Bag"
    )

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = TealPrimary,
                contentColor = Color.White,
                shape = CircleShape
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Ration")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // 1. Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.searchQuery.value = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search rations by name, category, or location...", fontSize = 13.sp) },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Search, contentDescription = "Search", tint = Color.Gray)
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.searchQuery.value = "" }) {
                            Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = TealPrimary,
                    unfocusedBorderColor = Color(0xFFCBD5E1),
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 2. Category Filter Chips
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(categories) { cat ->
                    FilterChip(
                        selected = selectedCategory.equals(cat, ignoreCase = true),
                        onClick = { viewModel.categoryFilter.value = cat },
                        label = { Text(cat, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = TealPrimary,
                            selectedLabelColor = Color.White,
                            containerColor = Color.White,
                            labelColor = DarkSlate
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // 3. Location Filter Chips
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(locations) { loc ->
                    FilterChip(
                        selected = selectedLocation.equals(loc, ignoreCase = true),
                        onClick = { viewModel.locationFilter.value = loc },
                        label = { Text(loc, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = DarkSlate,
                            selectedLabelColor = Color.White,
                            containerColor = Color.White,
                            labelColor = DarkSlate
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Item count indicator
            Text(
                text = "${items.size} rations found",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(6.dp))

            // 4. Inventory List
            if (items.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 40.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "No rations match your filter.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = {
                                viewModel.searchQuery.value = ""
                                viewModel.categoryFilter.value = "All"
                                viewModel.locationFilter.value = "All"
                            }
                        ) {
                            Text("Reset Filters")
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(items, key = { it.id }) { item ->
                        InventoryCard(
                            item = item,
                            onUse = { viewModel.useItem(item.id, 1.0) },
                            onRestock = { viewModel.restockItem(item.id, 1.0) }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(64.dp)) }
                }
            }
        }
    }

    if (showAddDialog) {
        AddRationDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { newItem ->
                viewModel.addNewItem(newItem)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun InventoryCard(
    item: InventoryItem,
    onUse: () -> Unit,
    onRestock: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header: Name and Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MidnightSlate,
                    modifier = Modifier.weight(1f)
                )

                // Stock status badge
                val (badgeBg, badgeText, badgeColor) = when {
                    item.isLowStock -> Triple(DangerRed.copy(alpha = 0.12f), "LOW STOCK", DangerRed)
                    item.currentQty >= item.parLevel * 1.5 -> Triple(TealSoft, "SURPLUS", TealPrimary)
                    else -> Triple(SuccessGreen.copy(alpha = 0.12f), "IN STOCK", SuccessGreen)
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

            // Subtitle: Category & Storage Location
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = item.category, fontSize = 12.sp, color = TealAccent, fontWeight = FontWeight.SemiBold)
                if (item.location.isNotEmpty()) {
                    Text(text = " • ", fontSize = 12.sp, color = Color.Gray)
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = Color.Gray
                    )
                    Text(text = " ${item.location}", fontSize = 12.sp, color = Color.Gray)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Quantities & Expiry Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "%.0f".format(item.currentQty),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MidnightSlate
                        )
                        Text(
                            text = " / Par %.0f ${item.unit}".format(item.parLevel),
                            fontSize = 13.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
                        )
                    }
                    if (item.totalServings > 0) {
                        Text(
                            text = "%.0f total servings (%.0f/unit)".format(item.totalServings, item.servingsPerUnit),
                            fontSize = 11.sp,
                            color = Color.DarkGray
                        )
                    }
                }

                // Steppers: Use (-1) and Restock (+1)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onUse,
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color(0xFFF1F5F9), RoundedCornerShape(8.dp)),
                        enabled = item.currentQty > 0
                    ) {
                        Icon(
                            imageVector = Icons.Default.Remove,
                            contentDescription = "Use 1",
                            tint = if (item.currentQty > 0) DarkSlate else Color.LightGray
                        )
                    }

                    IconButton(
                        onClick = onRestock,
                        modifier = Modifier
                            .size(36.dp)
                            .background(TealPrimary.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Restock 1",
                            tint = TealPrimary
                        )
                    }
                }
            }

            // Expiry info badge
            if (!item.earliestExpiry.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                val expColor = if (item.isExpired) DangerRed else if (item.isExpiringSoon) WarningAmber else SuccessGreen
                val expText = when {
                    item.isExpired -> "EXPIRED (${item.earliestExpiry})"
                    item.daysUntilExpiry != null -> "Expires: ${item.earliestExpiry} (${item.daysUntilExpiry} days left)"
                    else -> "Expires: ${item.earliestExpiry}"
                }
                Text(
                    text = expText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = expColor
                )
            }
        }
    }
}

@Composable
fun AddRationDialog(
    onDismiss: () -> Unit,
    onConfirm: (InventoryItem) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Canned Goods") }
    var unit by remember { mutableStateOf("cans") }
    var location by remember { mutableStateOf("Kitchen Pantry") }
    var initialQtyStr by remember { mutableStateOf("5") }
    var parLevelStr by remember { mutableStateOf("10") }
    var earliestExpiry by remember { mutableStateOf("2026-12-31") }
    var servingsStr by remember { mutableStateOf("4") }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add New Emergency Ration", fontWeight = FontWeight.Bold, color = MidnightSlate) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Item Name *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Category (e.g. Canned Goods, Water)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = unit,
                        onValueChange = { unit = it },
                        label = { Text("Unit / Pack") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = location,
                        onValueChange = { location = it },
                        label = { Text("Location") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = initialQtyStr,
                        onValueChange = { initialQtyStr = it },
                        label = { Text("Initial Qty") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = parLevelStr,
                        onValueChange = { parLevelStr = it },
                        label = { Text("Par Level") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = earliestExpiry,
                        onValueChange = { earliestExpiry = it },
                        label = { Text("Expiry (YYYY-MM-DD)") },
                        singleLine = true,
                        modifier = Modifier.weight(1.2f)
                    )
                    OutlinedTextField(
                        value = servingsStr,
                        onValueChange = { servingsStr = it },
                        label = { Text("Servings/Unit") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
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
                            restockNeeded = if (par > initQty) par - initQty else 0.0,
                            earliestExpiry = earliestExpiry.trim().ifEmpty { null },
                            expirationStatus = "GOOD",
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
                enabled = name.isNotBlank()
            ) {
                Text("Save Ration")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.Gray)
            }
        }
    )
}
