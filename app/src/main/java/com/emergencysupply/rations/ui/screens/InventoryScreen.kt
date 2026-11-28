package com.emergencysupply.rations.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.QrCodeScanner
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emergencysupply.rations.data.BundleCatalog
import com.emergencysupply.rations.data.PreplannedCatalog
import com.emergencysupply.rations.data.SupplyBundle
import com.emergencysupply.rations.data.remote.ScannedProductInfo
import com.emergencysupply.rations.data.local.entity.InventoryItem
import com.emergencysupply.rations.ui.GroupSummary
import com.emergencysupply.rations.ui.MainViewModel
import com.emergencysupply.rations.ui.components.BarcodeScannerSheet
import com.emergencysupply.rations.ui.components.ScrollableMultiWheelPicker
import com.emergencysupply.rations.ui.components.ScrollableWheelDatePicker
import com.emergencysupply.rations.ui.components.WheelColumnSpec
import com.emergencysupply.rations.ui.components.categoryVisualFor
import com.emergencysupply.rations.ui.components.dashedBorder
import com.emergencysupply.rations.ui.components.locationVisualFor
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
import com.emergencysupply.rations.ui.theme.OrangeAccent
import com.emergencysupply.rations.ui.theme.PeachSoft
import com.emergencysupply.rations.ui.theme.PeachTint
import com.emergencysupply.rations.ui.theme.SoftTileSurface
import com.emergencysupply.rations.ui.theme.SuccessGreen
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

    val categorySummaries by viewModel.categorySummaries.collectAsState()

    val isCategoryHub = selectedCategory == "All" && searchQuery.isBlank() && !lowStockOnly && !expiringOnly && selectedLocation == "All"

    var showAddSheet by remember { mutableStateOf(false) }
    var showBarcodeScanner by remember { mutableStateOf(false) }
    var scannedPrefill by remember { mutableStateOf<ScannedProductInfo?>(null) }
    var showBundlesSheet by remember { mutableStateOf(false) }
    val isScanningLookup by viewModel.isScanningLookup.collectAsState()
    var editingItem by remember { mutableStateOf<InventoryItem?>(null) }
    var showNewCategoryPrompt by remember { mutableStateOf(false) }
    var showNewLocationPrompt by remember { mutableStateOf(false) }

    val mergedCategories = remember(categorySummaries, availableCategories) {
        val result = categorySummaries.toMutableList()
        val seen = result.map { it.name.trim().lowercase() }.toMutableSet()
        for (cat in availableCategories) {
            val key = cat.trim().lowercase()
            if (key !in seen) {
                seen.add(key)
                result.add(GroupSummary(name = cat, itemCount = 0, lowStockCount = 0, readinessPct = 100))
            }
        }
        result
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // 1. Sleek Logistics-style Search Pill + Vibrant "+ Add" Pill
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = CardSurface,
            shadowElevation = 3.dp,
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
                    tint = CobaltBlue,
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
                IconButton(
                    onClick = { showBarcodeScanner = true },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = "Scan Barcode",
                        tint = EmeraldDeep,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Button(
                    onClick = { showAddSheet = true },
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = InkBlack),
                    modifier = Modifier.height(40.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("Add", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        if (isScanningLookup) {
            Spacer(modifier = Modifier.height(4.dp))
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth().height(3.dp),
                color = EmeraldDeep,
                trackColor = EmeraldDeep.copy(alpha = 0.2f)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (isCategoryHub) {
            // CATEGORIES HUB: 2-column grid inside LazyColumn
            val chunkedRows = remember(mergedCategories) { mergedCategories.chunked(2) }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Pantry Categories",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MidnightSlate
                            )
                            Text(
                                text = "${mergedCategories.size} categories • ${allInventory.size} items",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MediumSlate
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = CobaltSoft,
                            border = BorderStroke(1.dp, CobaltBlue.copy(alpha = 0.3f)),
                            modifier = Modifier.clickable { showBundlesSheet = true }
                        ) {
                            Text(
                                text = "🎒 Kits & Bundles",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = CobaltBlue,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                items(chunkedRows, key = { row -> "cat_row_" + row.joinToString("_") { it.name } }) { pair ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        for (summary in pair) {
                            Box(modifier = Modifier.weight(1f)) {
                                PantryCategoryCard(
                                    summary = summary,
                                    onClick = { viewModel.categoryFilter.value = summary.name }
                                )
                            }
                        }
                        if (pair.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }

                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .dashedBorder(width = 1.5.dp, color = EmeraldVibrant.copy(alpha = 0.5f), cornerRadius = 18.dp)
                            .clickable { showNewCategoryPrompt = true },
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = PeachSoft.copy(alpha = 0.45f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = EmeraldVibrant.copy(alpha = 0.15f),
                                modifier = Modifier.size(32.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Add, contentDescription = null, tint = EmeraldDeep, modifier = Modifier.size(18.dp))
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "+ New Category",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldDeep
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        } else {
            // DRILL-DOWN VIEW (Tapped category, searched, or filter active)
            val activeTitle = when {
                searchQuery.isNotBlank() -> "Search Results"
                selectedCategory != "All" -> selectedCategory
                lowStockOnly -> "Low Par Items"
                expiringOnly -> "Expiring Soon"
                selectedLocation != "All" -> selectedLocation
                else -> "All Rations"
            }
            val activeIcon = when {
                searchQuery.isNotBlank() -> "🔍"
                selectedCategory != "All" -> PreplannedCatalog.iconForCategory(selectedCategory)
                lowStockOnly -> "⚠️"
                expiringOnly -> "⏳"
                selectedLocation != "All" -> "📍"
                else -> "📦"
            }
            val activeVisual = if (selectedCategory != "All") categoryVisualFor(selectedCategory) else null
            val activeColor = activeVisual?.tint ?: CobaltBlue

            // Clean drill-down header bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = CobaltSoft,
                    modifier = Modifier.clickable {
                        viewModel.categoryFilter.value = "All"
                        viewModel.locationFilter.value = "All"
                        viewModel.lowStockOnlyFilter.value = false
                        viewModel.expiringOnlyFilter.value = false
                        viewModel.searchQuery.value = ""
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "← Categories",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = CobaltBlue
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = InkBlack
                ) {
                    Text(
                        text = "${items.size} items",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Active category / filter title banner
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = activeColor,
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(activeIcon, fontSize = 16.sp)
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = activeTitle,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MidnightSlate
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Storage Spot filter chips (All Spots, availableLocations, + Spot) - tinted using locationVisualFor
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                item {
                    val isAll = selectedLocation == "All"
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (isAll) TealPrimary else CardSurface,
                        border = BorderStroke(1.dp, if (isAll) TealPrimary else BorderSlate),
                        shadowElevation = if (isAll) 2.dp else 0.dp,
                        modifier = Modifier.clickable { viewModel.locationFilter.value = "All" }
                    ) {
                        Text(
                            text = "All Spots",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isAll) Color.White else MediumSlate,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }

                items(availableLocations) { loc ->
                    val isSel = selectedLocation.equals(loc, ignoreCase = true)
                    val locVis = locationVisualFor(loc)
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (isSel) locVis.tint else CardSurface,
                        border = BorderStroke(1.dp, if (isSel) locVis.tint else locVis.tint.copy(alpha = 0.35f)),
                        shadowElevation = if (isSel) 2.dp else 0.dp,
                        modifier = Modifier.clickable { viewModel.locationFilter.value = loc }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            if (!isSel) {
                                Surface(
                                    shape = CircleShape,
                                    color = locVis.tint,
                                    modifier = Modifier.size(6.dp)
                                ) {}
                                Spacer(modifier = Modifier.width(5.dp))
                            }
                            Text(
                                text = if (isSel) "📍 $loc" else loc,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSel) Color.White else DarkSlate
                            )
                        }
                    }
                }

                item {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
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

            // Dismissible Low Par ✕ / Expiring Soon ✕ chips if active
            if (lowStockOnly || expiringOnly) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (lowStockOnly) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = DangerRed,
                            modifier = Modifier.clickable { viewModel.lowStockOnlyFilter.value = false }
                        ) {
                            Text(
                                text = "Low Par ✕",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                    if (expiringOnly) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = OrangeAccent,
                            modifier = Modifier.clickable { viewModel.expiringOnlyFilter.value = false }
                        ) {
                            Text(
                                text = "Expiring Soon ✕",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Inventory Item Cards or Empty State
            if (items.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .dashedBorder(width = 1.5.dp, color = EmeraldVibrant.copy(alpha = 0.5f), cornerRadius = 18.dp),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = PeachSoft.copy(alpha = 0.45f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp, horizontal = 20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = CardSurface,
                                shadowElevation = 2.dp,
                                modifier = Modifier.size(60.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.FilterList,
                                        contentDescription = null,
                                        tint = EmeraldDeep,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = if (selectedCategory != "All") "No items in $selectedCategory yet" else "No rations match filters",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = DarkSlate
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = { showAddSheet = true },
                                colors = ButtonDefaults.buttonColors(containerColor = InkBlack),
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (selectedCategory != "All") "Add to $selectedCategory" else "Pick from ${PreplannedCatalog.plannedRations.size}+ Rations",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            } else {
                // Group items by Location inside drill-down view when selectedLocation == "All"
                if (selectedLocation == "All") {
                    val groupedByLocation = remember(items) {
                        items.groupBy { it.location.trim().ifBlank { "Unassigned Spot" } }
                    }
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        groupedByLocation.forEach { (loc, groupItems) ->
                            item(key = "loc_hdr_$loc") {
                                val locVis = locationVisualFor(loc)
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = locVis.softBg,
                                    border = BorderStroke(1.dp, locVis.tint.copy(alpha = 0.35f)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Surface(
                                                shape = CircleShape,
                                                color = locVis.tint,
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        Icons.Default.LocationOn,
                                                        contentDescription = null,
                                                        tint = Color.White,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = loc,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = MidnightSlate
                                            )
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = locVis.tint
                                        ) {
                                            Text(
                                                text = "${groupItems.size} items",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            items(groupItems, key = { "inv_${it.id}" }) { item ->
                                ReformedInventoryCard(
                                    item = item,
                                    onCardClick = { editingItem = item },
                                    onUse = { viewModel.useItem(item.id, 1.0) },
                                    onRestock = { viewModel.restockItem(item.id, 1.0) }
                                )
                            }
                        }
                        item { Spacer(modifier = Modifier.height(16.dp)) }
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
                        item { Spacer(modifier = Modifier.height(16.dp)) }
                    }
                }
            }
        }
    }

    if (showBarcodeScanner) {
        BarcodeScannerSheet(
            onDismiss = { showBarcodeScanner = false },
            onBarcodeDetected = { barcode ->
                showBarcodeScanner = false
                viewModel.lookupBarcode(barcode) { info ->
                    if (info != null) {
                        scannedPrefill = info
                        showAddSheet = true
                    } else {
                        scannedPrefill = ScannedProductInfo(
                            barcode = barcode,
                            name = "",
                            category = PreplannedCatalog.defaultCategoryNames.first(),
                            unit = "packs",
                            notes = "Barcode: $barcode"
                        )
                        showAddSheet = true
                        Toast.makeText(context, "Barcode $barcode scanned — enter details", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }

    if (showBundlesSheet) {
        SupplyBundlesSheet(
            onDismiss = { showBundlesSheet = false },
            onAddToPantry = { bundle ->
                viewModel.addBundleToPantry(bundle)
                Toast.makeText(context, "Added ${bundle.title} items to Pantry", Toast.LENGTH_SHORT).show()
            },
            onAddToGroceries = { bundle ->
                viewModel.addBundleToGroceries(bundle)
                Toast.makeText(context, "Added ${bundle.title} items to Groceries", Toast.LENGTH_SHORT).show()
            }
        )
    }

    if (showAddSheet) {
        val initialCat = if (selectedCategory != "All") selectedCategory else null
        PreplannedAndCustomAddSheet(
            existingItems = allInventory,
            availableCategories = availableCategories,
            availableLocations = availableLocations,
            onCreateCategory = { viewModel.addCustomCategory(it) },
            onCreateLocation = { viewModel.addCustomLocation(it) },
            onDismiss = {
                showAddSheet = false
                scannedPrefill = null
            },
            onConfirm = { newItem ->
                viewModel.addNewItem(newItem)
                Toast.makeText(context, "Added ${newItem.name} to ${newItem.location}", Toast.LENGTH_SHORT).show()
                showAddSheet = false
                scannedPrefill = null
            },
            initialCategory = initialCat,
            initialScannedProduct = scannedPrefill,
            onOpenBundles = {
                showAddSheet = false
                showBundlesSheet = true
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
private fun PantryCategoryCard(
    summary: GroupSummary,
    onClick: () -> Unit
) {
    val visual = categoryVisualFor(summary.name)
    val icon = PreplannedCatalog.iconForCategory(summary.name)
    val isFullyReady = summary.readinessPct >= 100 && summary.itemCount > 0
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        border = BorderStroke(1.dp, BorderSlate),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column {
            if (isFullyReady) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .background(Brush.linearGradient(listOf(EmeraldDeep, EmeraldVibrant, SuccessGreen)))
                )
            }
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = visual.tint,
                        shadowElevation = 2.dp,
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(icon, fontSize = 20.sp)
                        }
                    }
                    if (summary.lowStockCount > 0) {
                        Surface(shape = RoundedCornerShape(8.dp), color = DangerRed) {
                            Text(
                                text = "${summary.lowStockCount} low",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    } else if (isFullyReady) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = EmeraldVibrant.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "100%",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = EmeraldDeep,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = summary.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MidnightSlate,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${summary.itemCount} items",
                        fontSize = 11.sp,
                        color = LightSlate
                    )
                    Text(
                        text = "${summary.readinessPct}%",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            isFullyReady -> EmeraldDeep
                            summary.readinessPct < 70 -> DangerRed
                            else -> visual.tint
                        }
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                if (isFullyReady) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(5.dp)
                            .background(
                                brush = Brush.linearGradient(listOf(EmeraldDeep, EmeraldVibrant, SuccessGreen)),
                                shape = RoundedCornerShape(3.dp)
                            )
                    )
                } else {
                    LinearProgressIndicator(
                        progress = { (summary.readinessPct / 100f).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(5.dp),
                        color = if (summary.readinessPct < 70) DangerRed else visual.tint,
                        trackColor = Color(0xFFF1F5F9)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ReformedInventoryCard(
    item: InventoryItem,
    onCardClick: () -> Unit,
    onUse: () -> Unit,
    onRestock: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val visual = categoryVisualFor(item.category)
    val locVis = locationVisualFor(item.location)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onCardClick,
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onCardClick()
                }
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = visual.tint,
                        shadowElevation = 2.dp,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(PreplannedCatalog.iconForItem(item.name, item.category), fontSize = 20.sp)
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
                                fontWeight = FontWeight.Bold
                            )
                            if (item.location.isNotBlank()) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = locVis.softBg,
                                    border = BorderStroke(1.dp, locVis.tint.copy(alpha = 0.35f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = locVis.tint,
                                            modifier = Modifier.size(5.dp)
                                        ) {}
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = item.location,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = locVis.tint
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                val (badgeBg, badgeText) = when {
                    item.isLowStock -> Pair(DangerRed, "LOW PAR")
                    item.currentQty >= item.parLevel * 1.5 -> Pair(CobaltBlue, "SURPLUS")
                    else -> Pair(SuccessGreen, "HEALTHY")
                }
                Surface(shape = RoundedCornerShape(10.dp), color = badgeBg) {
                    Text(
                        text = badgeText,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
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

                // Vivid Steppers
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = SoftTileSurface,
                        border = BorderStroke(1.dp, BorderSlate),
                        modifier = Modifier.size(40.dp)
                    ) {
                        IconButton(onClick = onUse, enabled = item.currentQty > 0) {
                            Icon(Icons.Default.Remove, contentDescription = "Use 1", tint = if (item.currentQty > 0) DarkSlate else LightSlate, modifier = Modifier.size(18.dp))
                        }
                    }

                    Surface(
                        shape = CircleShape,
                        color = visual.tint,
                        shadowElevation = 2.dp,
                        modifier = Modifier.size(40.dp)
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
                    val expBg = if (item.isExpired) DangerRed else if (item.isExpiringSoon) OrangeAccent else SuccessGreen
                    Surface(shape = RoundedCornerShape(8.dp), color = expBg) {
                        val expText = when {
                            item.isExpired -> "EXPIRED (${item.earliestExpiry})"
                            item.daysUntilExpiry != null -> "Exp: ${item.earliestExpiry} (${item.daysUntilExpiry}d)"
                            else -> "Exp: ${item.earliestExpiry}"
                        }
                        Text(expText, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                    }
                } else {
                    Text(
                        text = if (item.totalServings > 0) "%.0f servings".format(item.totalServings) else "",
                        fontSize = 11.sp,
                        color = LightSlate
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(12.dp), tint = CobaltBlue)
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("Hold to edit", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CobaltBlue)
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
    onConfirm: (InventoryItem) -> Unit,
    initialCategory: String? = null,
    initialScannedProduct: ScannedProductInfo? = null,
    onOpenBundles: (() -> Unit)? = null
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedTab by remember(initialScannedProduct) { mutableStateOf(if (initialScannedProduct != null) 1 else 0) }
    var catalogSearch by remember { mutableStateOf("") }
    var catalogCatFilter by remember(initialCategory) { mutableStateOf(initialCategory ?: "All") }
    var destinationArea by remember { mutableStateOf("Auto") }

    var name by remember(initialScannedProduct) { mutableStateOf(initialScannedProduct?.name ?: "") }
    var category by remember(initialCategory, initialScannedProduct) {
        mutableStateOf(
            initialScannedProduct?.category?.takeIf { it.isNotBlank() }
                ?: initialCategory
                ?: availableCategories.firstOrNull()
                ?: "Grains & Pasta"
        )
    }
    var unit by remember(initialScannedProduct) {
        mutableStateOf(
            initialScannedProduct?.unit?.takeIf { it.isNotBlank() } ?: "lbs"
        )
    }
    var location by remember { mutableStateOf(availableLocations.firstOrNull() ?: "Main Pantry") }
    var initialQtyStr by remember(initialScannedProduct) {
        mutableStateOf(
            initialScannedProduct?.defaultQty?.let {
                if (it % 1.0 == 0.0) "%.0f".format(it) else "$it"
            } ?: "6"
        )
    }
    var parLevelStr by remember { mutableStateOf("12") }
    var earliestExpiry by remember { mutableStateOf(LocalDate.now().plusYears(1).format(DateTimeFormatter.ISO_LOCAL_DATE)) }
    var servingsStr by remember { mutableStateOf("4") }
    var notes by remember(initialScannedProduct) {
        mutableStateOf(
            initialScannedProduct?.notes ?: ""
        )
    }

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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Add Rations & Household Items",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 17.sp,
                    color = MidnightSlate,
                    modifier = Modifier.weight(1f)
                )
                if (onOpenBundles != null) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = PeachSoft,
                        border = BorderStroke(1.dp, OrangeAccent.copy(alpha = 0.4f)),
                        modifier = Modifier.clickable { onOpenBundles() }
                    ) {
                        Text(
                            text = "🎒 Kits & Bundles",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = OrangeAccent,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))

            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = SoftTileSurface,
                contentColor = CobaltBlue
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("All Types (${PreplannedCatalog.plannedRations.size})", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Custom / New", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (selectedTab == 0) {
                val filteredTemplates = PreplannedCatalog.plannedRations.filter { tpl ->
                    val matchSearch = catalogSearch.isBlank() ||
                        tpl.name.contains(catalogSearch, ignoreCase = true) ||
                        tpl.category.contains(catalogSearch, ignoreCase = true)
                    val matchCat = catalogCatFilter == "All" || tpl.category.equals(catalogCatFilter, ignoreCase = true)
                    matchSearch && matchCat
                }

                Column(modifier = Modifier.fillMaxWidth().heightIn(max = 490.dp)) {
                    OutlinedTextField(
                        value = catalogSearch,
                        onValueChange = { catalogSearch = it },
                        placeholder = { Text("Search 52+ household & pantry items...", fontSize = 13.sp) },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // Category filter pills inside catalog
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        item {
                            val isAll = catalogCatFilter == "All"
                            FilterChip(
                                selected = isAll,
                                onClick = { catalogCatFilter = "All" },
                                label = { Text("All", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = CobaltBlue, selectedLabelColor = Color.White)
                            )
                        }
                        items(availableCategories) { cat ->
                            val isSel = catalogCatFilter.equals(cat, ignoreCase = true)
                            val vis = categoryVisualFor(cat)
                            FilterChip(
                                selected = isSel,
                                onClick = { catalogCatFilter = cat },
                                label = { Text("${PreplannedCatalog.iconForCategory(cat)} $cat", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = vis.tint, selectedLabelColor = Color.White)
                            )
                        }
                    }

                    // Destination Area / Storage Spot Selector
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                        Text("Add to Area:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MediumSlate)
                        Spacer(modifier = Modifier.width(8.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            item {
                                val isAuto = destinationArea == "Auto"
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isAuto) TealPrimary else SoftTileSurface,
                                    modifier = Modifier.clickable { destinationArea = "Auto" }
                                ) {
                                    Text("Default Spot", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (isAuto) Color.White else DarkSlate, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                                }
                            }
                            items(availableLocations) { loc ->
                                val isSel = destinationArea == loc
                                val locVis = locationVisualFor(loc)
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSel) locVis.tint else SoftTileSurface,
                                    modifier = Modifier.clickable { destinationArea = loc }
                                ) {
                                    Text("📍 $loc", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (isSel) Color.White else DarkSlate, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(filteredTemplates, key = { it.id }) { tpl ->
                            val alreadyIn = existingNames.contains(tpl.name.lowercase())
                            val visual = categoryVisualFor(tpl.category)
                            val resolvedLoc = if (destinationArea == "Auto") tpl.location else destinationArea
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
                                        location = resolvedLoc
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
                                    Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                                        Surface(shape = RoundedCornerShape(12.dp), color = visual.tint, modifier = Modifier.size(38.dp)) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(tpl.icon, fontSize = 18.sp)
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(tpl.name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MidnightSlate)
                                            Text("${tpl.category} • 📍 $resolvedLoc", fontSize = 10.sp, color = MediumSlate)
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Button(
                                        onClick = { onConfirm(tpl.toInventoryItem(targetLocation = resolvedLoc)) },
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = if (alreadyIn) InkBlack else visual.tint),
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
                // Redesigned Custom Tab with Vertical Rotating Drum Wheels
                val allCategories = remember(availableCategories, category) {
                    (availableCategories + category).distinct().filter { it.isNotBlank() }
                }
                val allLocations = remember(availableLocations, location) {
                    (availableLocations + location).distinct().filter { it.isNotBlank() }
                }

                val wheelGroupA = remember(allCategories, category, allLocations, location) {
                    listOf(
                        WheelColumnSpec(
                            label = "Category",
                            items = allCategories,
                            selected = category,
                            weight = 1.25f,
                            displayTransform = { "${PreplannedCatalog.iconForCategory(it)} $it" },
                            onSelected = { category = it },
                            onAddClick = { showNewCatDialog = true }
                        ),
                        WheelColumnSpec(
                            label = "Storage Spot",
                            items = allLocations,
                            selected = location,
                            weight = 1f,
                            displayTransform = { "📍 $it" },
                            onSelected = { location = it },
                            onAddClick = { showNewLocDialog = true }
                        )
                    )
                }

                val qtyBase = remember { (0..500).map { it.toString() } }
                val qtyItems = remember(initialQtyStr, qtyBase) {
                    if (initialQtyStr.isNotBlank() && initialQtyStr !in qtyBase) {
                        listOf(initialQtyStr) + qtyBase
                    } else {
                        qtyBase
                    }
                }
                val parItems = remember(parLevelStr, qtyBase) {
                    if (parLevelStr.isNotBlank() && parLevelStr !in qtyBase) {
                        listOf(parLevelStr) + qtyBase
                    } else {
                        qtyBase
                    }
                }
                val allUnits = remember(unit) {
                    (PreplannedCatalog.standardUnits + unit).distinct().filter { it.isNotBlank() }
                }

                val wheelGroupB = remember(qtyItems, initialQtyStr, parItems, parLevelStr, allUnits, unit) {
                    listOf(
                        WheelColumnSpec(
                            label = "Initial Qty",
                            items = qtyItems,
                            selected = initialQtyStr,
                            weight = 0.85f,
                            onSelected = { initialQtyStr = it }
                        ),
                        WheelColumnSpec(
                            label = "Par Goal",
                            items = parItems,
                            selected = parLevelStr,
                            weight = 0.85f,
                            onSelected = { parLevelStr = it }
                        ),
                        WheelColumnSpec(
                            label = "Unit",
                            items = allUnits,
                            selected = unit,
                            weight = 1.15f,
                            onSelected = { unit = it }
                        )
                    )
                }

                var showExactInputs by remember { mutableStateOf(false) }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 500.dp)
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

                    if (name.isNotBlank()) {
                        val potentialDupes = remember(name, existingItems) {
                            val q = name.trim()
                            existingItems.filter { it.name.contains(q, ignoreCase = true) }.take(2)
                        }
                        if (potentialDupes.isNotEmpty()) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                potentialDupes.forEach { dup ->
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = WarningSoft,
                                        border = BorderStroke(1.dp, WarningAmber.copy(alpha = 0.5f))
                                    ) {
                                        Text(
                                            text = "Already in Pantry: ${dup.name} (Qty %.0f %s)".format(dup.currentQty, dup.unit),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MidnightSlate,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Wheel Group A: Category + Storage Spot
                    ScrollableMultiWheelPicker(
                        columns = wheelGroupA,
                        accentColor = CobaltBlue
                    )

                    // Header for Wheel Group B with "Type Exact" toggle pill
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Stock & Target Par", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MidnightSlate)
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (showExactInputs) CobaltBlue else SoftTileSurface,
                            border = BorderStroke(1.dp, if (showExactInputs) CobaltBlue else BorderSlate),
                            modifier = Modifier.clickable { showExactInputs = !showExactInputs }
                        ) {
                            Text(
                                text = if (showExactInputs) "✓ Exact Inputs" else "⌨ Type Exact",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (showExactInputs) Color.White else DarkSlate,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    if (showExactInputs) {
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
                    }

                    // Wheel Group B: Initial Qty + Par Goal + Unit
                    ScrollableMultiWheelPicker(
                        columns = wheelGroupB,
                        accentColor = CobaltBlue
                    )

                    // Compact - / + stepper row for Servings/Unit right below Wheel Group B
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Servings / Unit", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MidnightSlate)
                            Text(
                                text = "${servingsStr.toDoubleOrNull()?.let { "%.0f".format(it) } ?: servingsStr} portions per $unit",
                                fontSize = 11.sp,
                                color = MediumSlate
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = SoftTileSurface,
                                border = BorderStroke(1.dp, BorderSlate),
                                modifier = Modifier.size(32.dp)
                            ) {
                                IconButton(
                                    onClick = {
                                        val current = servingsStr.toDoubleOrNull() ?: 1.0
                                        if (current > 1.0) {
                                            servingsStr = "%.0f".format(current - 1.0)
                                        }
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Remove, contentDescription = "Decrease Servings", modifier = Modifier.size(16.dp), tint = DarkSlate)
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = CardSurface,
                                border = BorderStroke(1.dp, BorderSlate)
                            ) {
                                Text(
                                    text = servingsStr,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MidnightSlate,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                )
                            }
                            Surface(
                                shape = CircleShape,
                                color = SoftTileSurface,
                                border = BorderStroke(1.dp, BorderSlate),
                                modifier = Modifier.size(32.dp)
                            ) {
                                IconButton(
                                    onClick = {
                                        val current = servingsStr.toDoubleOrNull() ?: 1.0
                                        servingsStr = "%.0f".format(current + 1.0)
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Increase Servings", modifier = Modifier.size(16.dp), tint = DarkSlate)
                                }
                            }
                        }
                    }

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
                        Text("Save to Pantry & Sync", fontWeight = FontWeight.Bold, color = Color.White)
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

    var showExactInputs by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var showNewCatDialog by remember { mutableStateOf(false) }
    var showNewLocDialog by remember { mutableStateOf(false) }

    val catVisual = categoryVisualFor(category)

    val allCategories = remember(availableCategories, category) {
        (availableCategories + category).distinct().filter { it.isNotBlank() }
    }
    val allLocations = remember(availableLocations, location) {
        (availableLocations + location).distinct().filter { it.isNotBlank() }
    }

    val wheelGroupA = remember(allCategories, category, allLocations, location) {
        listOf(
            WheelColumnSpec(
                label = "Category",
                items = allCategories,
                selected = category,
                weight = 1.25f,
                displayTransform = { "${PreplannedCatalog.iconForCategory(it)} $it" },
                onSelected = { category = it },
                onAddClick = { showNewCatDialog = true }
            ),
            WheelColumnSpec(
                label = "Storage Spot",
                items = allLocations,
                selected = location,
                weight = 1f,
                displayTransform = { "📍 $it" },
                onSelected = { location = it },
                onAddClick = { showNewLocDialog = true }
            )
        )
    }

    val qtyBase = remember { (0..500).map { it.toString() } }
    val qtyItems = remember(currentQtyStr, qtyBase) {
        if (currentQtyStr.isNotBlank() && currentQtyStr !in qtyBase) {
            listOf(currentQtyStr) + qtyBase
        } else {
            qtyBase
        }
    }
    val parItems = remember(parLevelStr, qtyBase) {
        if (parLevelStr.isNotBlank() && parLevelStr !in qtyBase) {
            listOf(parLevelStr) + qtyBase
        } else {
            qtyBase
        }
    }
    val allUnits = remember(unit) {
        (PreplannedCatalog.standardUnits + unit).distinct().filter { it.isNotBlank() }
    }

    val wheelGroupB = remember(qtyItems, currentQtyStr, parItems, parLevelStr, allUnits, unit) {
        listOf(
            WheelColumnSpec(
                label = "Qty",
                items = qtyItems,
                selected = currentQtyStr,
                weight = 0.85f,
                onSelected = { currentQtyStr = it }
            ),
            WheelColumnSpec(
                label = "Par Goal",
                items = parItems,
                selected = parLevelStr,
                weight = 0.85f,
                onSelected = { parLevelStr = it }
            ),
            WheelColumnSpec(
                label = "Unit",
                items = allUnits,
                selected = unit,
                weight = 1.15f,
                onSelected = { unit = it }
            )
        )
    }

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
            // Sleek warm header card with item icon badge, item title + stock summary pill, tactile Quick Adjust pill bar (-5, -1, +1, +5)
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = PeachSoft.copy(alpha = 0.5f)),
                border = BorderStroke(1.dp, PeachTint.copy(alpha = 0.6f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = catVisual.tint,
                                shadowElevation = 2.dp,
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(PreplannedCatalog.iconForItem(name, category), fontSize = 22.sp)
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = name,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 17.sp,
                                    color = MidnightSlate,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                val (summaryColor, summaryText) = when {
                                    item.isLowStock -> DangerRed to "Low Par (${"%.0f".format(item.currentQty)}/${"%.0f".format(item.parLevel)} ${item.unit})"
                                    item.currentQty >= item.parLevel * 1.5 -> CobaltBlue to "Surplus (${"%.0f".format(item.currentQty)} ${item.unit})"
                                    else -> SuccessGreen to "Healthy (${"%.0f".format(item.currentQty)}/${"%.0f".format(item.parLevel)} ${item.unit})"
                                }
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = summaryColor.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = summaryText,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = summaryColor,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        Surface(
                            shape = CircleShape,
                            color = DangerSoft,
                            modifier = Modifier
                                .size(34.dp)
                                .clickable { confirmDelete = !confirmDelete }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = DangerRed, modifier = Modifier.size(16.dp))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Tactile Quick Adjust pill bar (-5, -1, +1, +5)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Quick Adjust:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MediumSlate
                        )
                        listOf(-5.0 to "-5", -1.0 to "-1", 1.0 to "+1", 5.0 to "+5").forEach { (delta, lbl) ->
                            val isPositive = delta > 0
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isPositive) (if (delta >= 5.0) CobaltBlue else SuccessGreen) else CardSurface,
                                border = if (!isPositive) BorderStroke(1.dp, BorderSlate) else null,
                                shadowElevation = if (isPositive) 1.dp else 0.dp,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(34.dp)
                                    .clickable { onQuickStep(delta) }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = lbl,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (isPositive) Color.White else DarkSlate
                                    )
                                }
                            }
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

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Item Name") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )

            // Wheel Group A: Category + Storage Spot
            ScrollableMultiWheelPicker(
                columns = wheelGroupA,
                accentColor = CobaltBlue
            )

            // Header for Wheel Group B with "Type Exact" toggle pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Stock & Target Par", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MidnightSlate)
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (showExactInputs) CobaltBlue else SoftTileSurface,
                    border = BorderStroke(1.dp, if (showExactInputs) CobaltBlue else BorderSlate),
                    modifier = Modifier.clickable { showExactInputs = !showExactInputs }
                ) {
                    Text(
                        text = if (showExactInputs) "✓ Exact Inputs" else "⌨ Type Exact",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (showExactInputs) Color.White else DarkSlate,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            if (showExactInputs) {
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
            }

            // Wheel Group B: Qty + Par Goal + Unit
            ScrollableMultiWheelPicker(
                columns = wheelGroupB,
                accentColor = CobaltBlue
            )

            // Compact - / + stepper row for Servings/Unit right below Wheel Group B
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Servings / Unit", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MidnightSlate)
                    Text(
                        text = "${servingsStr.toDoubleOrNull()?.let { "%.0f".format(it) } ?: servingsStr} portions per $unit",
                        fontSize = 11.sp,
                        color = MediumSlate
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = SoftTileSurface,
                        border = BorderStroke(1.dp, BorderSlate),
                        modifier = Modifier.size(32.dp)
                    ) {
                        IconButton(
                            onClick = {
                                val current = servingsStr.toDoubleOrNull() ?: 1.0
                                if (current > 1.0) {
                                    servingsStr = "%.0f".format(current - 1.0)
                                }
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "Decrease Servings", modifier = Modifier.size(16.dp), tint = DarkSlate)
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = CardSurface,
                        border = BorderStroke(1.dp, BorderSlate)
                    ) {
                        Text(
                            text = servingsStr,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MidnightSlate,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }
                    Surface(
                        shape = CircleShape,
                        color = SoftTileSurface,
                        border = BorderStroke(1.dp, BorderSlate),
                        modifier = Modifier.size(32.dp)
                    ) {
                        IconButton(
                            onClick = {
                                val current = servingsStr.toDoubleOrNull() ?: 1.0
                                servingsStr = "%.0f".format(current + 1.0)
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Increase Servings", modifier = Modifier.size(16.dp), tint = DarkSlate)
                        }
                    }
                }
            }

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

            // Modifier.dashedBorder(...) "Add Deficit to Shopping List" button
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = CobaltSoft.copy(alpha = 0.5f),
                modifier = Modifier
                    .fillMaxWidth()
                    .dashedBorder(width = 1.5.dp, color = CobaltBlue.copy(alpha = 0.5f), cornerRadius = 14.dp)
                    .clickable { onAddToGroceries() }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.AddShoppingCart, contentDescription = null, modifier = Modifier.size(16.dp), tint = CobaltBlue)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Add Deficit to Shopping List", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = CobaltBlue)
                }
            }

            // InkBlack "Save Changes & Sync" primary CTA
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
                Text("Save Changes & Sync", fontWeight = FontWeight.Bold, color = Color.White)
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
                colors = ButtonDefaults.buttonColors(containerColor = CobaltBlue),
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

/**
 * Feature 5: Emergency Kits & Recipe Bundles Sheet
 * Displays pre-assembled survival & family rations packages from BundleCatalog.
 * Allows one-tap adding either directly into the Pantry or into House Groceries checklist.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupplyBundlesSheet(
    onDismiss: () -> Unit,
    onAddToPantry: (SupplyBundle) -> Unit,
    onAddToGroceries: (SupplyBundle) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val bundles = remember { BundleCatalog.allBundles }

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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "🎒 Emergency Kits & Bundles",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 19.sp,
                        color = MidnightSlate
                    )
                    Text(
                        text = "One-tap multi-item ration packages & grocery lists",
                        fontSize = 12.sp,
                        color = MediumSlate
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Clear, contentDescription = "Close", tint = DarkSlate)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 520.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(bundles, key = { it.id }) { bundle ->
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = SoftTileSurface,
                        border = BorderStroke(1.dp, BorderSlate),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp)
                        ) {
                            // Header: Icon + Title + Subtitle
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = PeachSoft,
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(bundle.icon, fontSize = 22.sp)
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = bundle.title,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MidnightSlate
                                    )
                                    Text(
                                        text = bundle.subtitle,
                                        fontSize = 11.sp,
                                        color = MediumSlate,
                                        lineHeight = 15.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Itemized preview chips
                            Text(
                                text = "Included items (${bundle.items.size}):",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = DarkSlate
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(bundle.items) { item ->
                                    val qtyFormatted = if (item.qty % 1.0 == 0.0) "%.0f".format(item.qty) else "${item.qty}"
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = CardSurface,
                                        border = BorderStroke(1.dp, BorderSlate)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "${item.name} ($qtyFormatted ${item.unit})",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = MidnightSlate
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Action buttons: "Add to Pantry" and "+ Groceries"
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { onAddToPantry(bundle) },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = InkBlack),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(38.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = null,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Add to Pantry",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Button(
                                    onClick = { onAddToGroceries(bundle) },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = CobaltBlue),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(38.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AddShoppingCart,
                                        contentDescription = null,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "+ Groceries",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
