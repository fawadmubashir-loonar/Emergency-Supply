package com.emergencysupply.rations.ui.screens

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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import com.emergencysupply.rations.ui.theme.TealAccent
import com.emergencysupply.rations.ui.theme.TealPrimary
import com.emergencysupply.rations.ui.theme.TealSoft
import com.emergencysupply.rations.ui.theme.WarningAmber
import com.emergencysupply.rations.ui.theme.WarningSoft
import java.util.UUID

@Composable
fun InventoryScreen(viewModel: MainViewModel) {
    val items by viewModel.filteredInventory.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCategory by viewModel.categoryFilter.collectAsState()
    val selectedLocation by viewModel.locationFilter.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }

    val categories = listOf(
        "All" to "All",
        "Grains & Rice" to "🌾 Grains",
        "Halal Meats & Proteins" to "🥩 Halal Meats",
        "Canned Goods" to "🥫 Canned",
        "Water & Drinks" to "💧 Water",
        "Dairy & Powders" to "🥛 Dairy",
        "Snacks & Bars" to "🍫 Snacks",
        "First Aid & Medical" to "🩹 First Aid"
    )

    val locations = listOf(
        "All", "Kitchen Pantry", "Basement Shelf A", "Deep Freezer", "Garage Storage", "Bug Out Bag"
    )

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = TealPrimary,
                contentColor = Color.White,
                shape = CircleShape,
                elevation = androidx.compose.material3.FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp)
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
                placeholder = { Text("Search by name, category, or location...", fontSize = 13.sp, color = LightSlate) },
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

            // 2. Category Filter Row
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(categories) { (catKey, catLabel) ->
                    val isSelected = selectedCategory.equals(catKey, ignoreCase = true)
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.categoryFilter.value = catKey },
                        label = { Text(catLabel, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium) },
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
            }

            Spacer(modifier = Modifier.height(6.dp))

            // 3. Location Filter Row
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(locations) { loc ->
                    val isSelected = selectedLocation.equals(loc, ignoreCase = true)
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.locationFilter.value = loc },
                        label = { Text(if (loc == "All") "All Locations" else "📍 $loc", fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
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
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Rations count label
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${items.size} rations tracked",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MediumSlate
                )
                if (searchQuery.isNotEmpty() || selectedCategory != "All" || selectedLocation != "All") {
                    Text(
                        text = "Reset Filters",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TealPrimary,
                        modifier = Modifier.clickable {
                            viewModel.searchQuery.value = ""
                            viewModel.categoryFilter.value = "All"
                            viewModel.locationFilter.value = "All"
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
                            text = "No rations match your filter criteria.",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = DarkSlate
                        )
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
        ReformedAddRationDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { newItem ->
                viewModel.addNewItem(newItem)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun ReformedInventoryCard(
    item: InventoryItem,
    onUse: () -> Unit,
    onRestock: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        border = BorderStroke(1.dp, BorderSlate),
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
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MidnightSlate,
                    modifier = Modifier.weight(1f)
                )

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

            // Category & Location Tag
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

            Spacer(modifier = Modifier.height(10.dp))

            // Stock Bar & Steppers Row
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

                    // Visual Progress Bar towards Par
                    val rawProgressRatio = if (item.parLevel > 0) (item.currentQty / item.parLevel).toFloat() else 1f
                    val progressRatio = if (rawProgressRatio.isNaN() || rawProgressRatio.isInfinite()) 0f else rawProgressRatio.coerceIn(0f, 1f)
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

                // Wide touch steppers
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Use -1 Button
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

                    // Restock +1 Button
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

            // Expiry Row if provided
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
fun ReformedAddRationDialog(
    onDismiss: () -> Unit,
    onConfirm: (InventoryItem) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Canned Goods") }
    var unit by remember { mutableStateOf("cans") }
    var location by remember { mutableStateOf("Kitchen Pantry") }
    var initialQtyStr by remember { mutableStateOf("6") }
    var parLevelStr by remember { mutableStateOf("12") }
    var earliestExpiry by remember { mutableStateOf("2027-06-30") }
    var servingsStr by remember { mutableStateOf("4") }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(18.dp),
        containerColor = Color.White,
        title = {
            Text("Add Emergency Ration", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = MidnightSlate)
        },
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
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Category (e.g. Canned Goods, Grains)") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = unit,
                        onValueChange = { unit = it },
                        label = { Text("Unit (cans, lbs)") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = location,
                        onValueChange = { location = it },
                        label = { Text("Location") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = initialQtyStr,
                        onValueChange = { initialQtyStr = it },
                        label = { Text("Initial Stock") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = parLevelStr,
                        onValueChange = { parLevelStr = it },
                        label = { Text("Par Level") },
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
                    label = { Text("Notes (optional)") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
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
                shape = RoundedCornerShape(10.dp),
                enabled = name.isNotBlank()
            ) {
                Text("Save Ration", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = MediumSlate)
            }
        }
    )
}
