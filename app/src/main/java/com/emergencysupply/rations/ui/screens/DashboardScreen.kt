package com.emergencysupply.rations.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emergencysupply.rations.data.PreplannedCatalog
import com.emergencysupply.rations.data.local.entity.InventoryItem
import com.emergencysupply.rations.ui.GroupSummary
import com.emergencysupply.rations.ui.MainViewModel
import com.emergencysupply.rations.ui.NavTab
import com.emergencysupply.rations.ui.components.ReadinessArcGauge
import com.emergencysupply.rations.ui.components.categoryVisualFor
import com.emergencysupply.rations.ui.theme.BorderSlate
import com.emergencysupply.rations.ui.theme.CardSurface
import com.emergencysupply.rations.ui.theme.DangerRed
import com.emergencysupply.rations.ui.theme.DangerSoft
import com.emergencysupply.rations.ui.theme.DarkSlate
import com.emergencysupply.rations.ui.theme.IndigoAccent
import com.emergencysupply.rations.ui.theme.IndigoSoft
import com.emergencysupply.rations.ui.theme.InkBlack
import com.emergencysupply.rations.ui.theme.LightSlate
import com.emergencysupply.rations.ui.theme.MediumSlate
import com.emergencysupply.rations.ui.theme.MidnightSlate
import com.emergencysupply.rations.ui.theme.OrangeAccent
import com.emergencysupply.rations.ui.theme.OrangeSoft
import com.emergencysupply.rations.ui.theme.SoftTileSurface
import com.emergencysupply.rations.ui.theme.SuccessGreen
import com.emergencysupply.rations.ui.theme.SuccessSoft
import com.emergencysupply.rations.ui.theme.TealPrimary
import com.emergencysupply.rations.ui.theme.TealSoft
import com.emergencysupply.rations.ui.theme.WarningAmber
import com.emergencysupply.rations.ui.theme.WarningSoft

@Composable
fun DashboardScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val metrics by viewModel.dashboardMetrics.collectAsState()
    val expiringItems by viewModel.earliestExpiring.collectAsState()
    val restockItems by viewModel.restockNeeded.collectAsState()
    val categorySummaries by viewModel.categorySummaries.collectAsState()
    val locationSummaries by viewModel.locationSummaries.collectAsState()
    val allInventory by viewModel.allInventory.collectAsState()
    val availableCategories by viewModel.availableCategories.collectAsState()
    val availableLocations by viewModel.availableLocations.collectAsState()

    var editingItem by remember { mutableStateOf<InventoryItem?>(null) }
    var showAddSheet by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Personal Finance Arc Gauge Readiness Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = CardSurface),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Pantry Health",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MidnightSlate
                        )
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = SoftTileSurface,
                            border = BorderStroke(1.dp, BorderSlate),
                            modifier = Modifier.clickable { viewModel.openFilteredInventory() }
                        ) {
                            Text(
                                text = "View All",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MediumSlate,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    ReadinessArcGauge(
                        scorePct = metrics.parReadinessScore,
                        daysOfSupply = metrics.estDaysOfSupply
                    )

                    HorizontalDivider(color = BorderSlate.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(12.dp))

                    // 3 Mini Metrics below Arc
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Servings", fontSize = 11.sp, color = LightSlate)
                            Text("%,.0f".format(metrics.totalServings), fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = MidnightSlate)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Household", fontSize = 11.sp, color = LightSlate)
                            Text("5 People", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = MidnightSlate)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Unique Items", fontSize = 11.sp, color = LightSlate)
                            Text("${metrics.totalUniqueItems}", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = TealPrimary)
                        }
                    }
                }
            }
        }

        // 2. Finance Quick Action Circles Strip
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = CardSurface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 14.dp, horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    QuickActionCircle(Icons.Default.Add, "Add Item", OrangeAccent, OrangeSoft) {
                        showAddSheet = true
                    }
                    QuickActionCircle(Icons.Default.Warning, "Low Stock", DangerRed, DangerSoft) {
                        viewModel.openFilteredInventory(lowStockOnly = true)
                    }
                    QuickActionCircle(Icons.Default.ShoppingCart, "Groceries", TealPrimary, TealSoft) {
                        viewModel.currentTab.value = NavTab.GROCERIES
                    }
                    QuickActionCircle(Icons.Default.Sync, "Sync Now", IndigoAccent, IndigoSoft) {
                        viewModel.syncNow()
                        Toast.makeText(context, "Syncing Google Sheets...", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        // 3. Logistics-Style 3-Column Counter Row
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CompactCounterTile(
                    icon = Icons.Default.Inventory2,
                    tint = TealPrimary,
                    softBg = TealSoft,
                    count = "%,.0f".format(metrics.totalUnitsInStock),
                    label = "Total Units",
                    onClick = { viewModel.openFilteredInventory() },
                    modifier = Modifier.weight(1f)
                )
                CompactCounterTile(
                    icon = Icons.Default.HourglassBottom,
                    tint = OrangeAccent,
                    softBg = OrangeSoft,
                    count = "${metrics.expiringSoonCount}",
                    label = "Expiring Soon",
                    onClick = { viewModel.openFilteredInventory(expiringOnly = true) },
                    modifier = Modifier.weight(1f)
                )
                CompactCounterTile(
                    icon = Icons.Default.Warning,
                    tint = DangerRed,
                    softBg = DangerSoft,
                    count = "${metrics.restockNeededCount}",
                    label = "Low Par",
                    onClick = { viewModel.openFilteredInventory(lowStockOnly = true) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // 4. Cloud Storage Inspired: Quick Category Folders (1-Tap Jump)
        if (categorySummaries.isNotEmpty()) {
            item {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Categories", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MidnightSlate)
                        Text(
                            "Browse All >",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TealPrimary,
                            modifier = Modifier.clickable { viewModel.openFilteredInventory() }
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(categorySummaries, key = { "cat_card_${it.name}" }) { summary ->
                            CategoryFolderCard(summary) {
                                viewModel.openFilteredInventory(category = summary.name)
                            }
                        }
                    }
                }
            }
        }

        // 5. Cloud Storage Breakdown List: Storage Locations Reachability
        if (locationSummaries.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = CardSurface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Storage Spots", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MidnightSlate)
                            Text("${locationSummaries.size} zones", fontSize = 11.sp, color = LightSlate)
                        }

                        locationSummaries.forEach { locSummary ->
                            LocationBreakdownRow(locSummary) {
                                viewModel.openFilteredInventory(location = locSummary.name)
                            }
                        }
                    }
                }
            }
        }

        // 6. FIFO Priority Use-First Section
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Use First (FIFO)", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MidnightSlate)
                    Text("Earliest expiring items • Tap to edit", fontSize = 12.sp, color = MediumSlate)
                }
                Surface(shape = RoundedCornerShape(10.dp), color = OrangeSoft) {
                    Text("${expiringItems.size} items", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = OrangeAccent, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                }
            }
        }

        if (expiringItems.isEmpty()) {
            item {
                EmptyStatusCard("All expiration dates are healthy!")
            }
        } else {
            items(expiringItems, key = { "fifo_${it.id}" }) { item ->
                ReformedFifoCard(
                    item = item,
                    onCardClick = { editingItem = item },
                    onUseOne = {
                        viewModel.useItem(item.id, 1.0)
                        Toast.makeText(context, "Used 1 ${item.name}", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }

        // 7. Restock Alerts Section
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Restock Alerts", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MidnightSlate)
                    Text("Below target par • Tap +1 or Groceries", fontSize = 12.sp, color = MediumSlate)
                }
                Surface(shape = RoundedCornerShape(10.dp), color = if (restockItems.isNotEmpty()) DangerSoft else SuccessSoft) {
                    Text("${restockItems.size} alerts", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (restockItems.isNotEmpty()) DangerRed else SuccessGreen, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                }
            }
        }

        if (restockItems.isEmpty()) {
            item {
                EmptyStatusCard("All rations meet target par!")
            }
        } else {
            items(restockItems, key = { "restock_${it.id}" }) { item ->
                ReformedRestockCard(
                    item = item,
                    onCardClick = { editingItem = item },
                    onQuickRestock = {
                        viewModel.restockItem(item.id, 1.0)
                        Toast.makeText(context, "+1 ${item.name} synced to Sheet", Toast.LENGTH_SHORT).show()
                    },
                    onAddToGroceries = {
                        viewModel.addRestockItemToGroceries(item)
                        Toast.makeText(context, "Added ${item.name} to Groceries", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
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
}

@Composable
private fun QuickActionCircle(
    icon: ImageVector,
    label: String,
    tint: Color,
    softBg: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = softBg,
            modifier = Modifier.size(48.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(imageVector = icon, contentDescription = label, tint = tint, modifier = Modifier.size(22.dp))
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(text = label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = DarkSlate)
    }
}

@Composable
private fun CompactCounterTile(
    icon: ImageVector,
    tint: Color,
    softBg: Color,
    count: String,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Surface(shape = CircleShape, color = softBg, modifier = Modifier.size(34.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(text = count, fontSize = 22.sp, fontWeight = FontWeight.Black, color = MidnightSlate)
            Text(text = label, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = MediumSlate)
        }
    }
}

@Composable
private fun CategoryFolderCard(summary: GroupSummary, onClick: () -> Unit) {
    val visual = categoryVisualFor(summary.name)
    Card(
        modifier = Modifier
            .width(136.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(shape = CircleShape, color = visual.softBg, modifier = Modifier.size(36.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(PreplannedCatalog.iconForCategory(summary.name), fontSize = 16.sp)
                    }
                }
                if (summary.lowStockCount > 0) {
                    Surface(shape = RoundedCornerShape(8.dp), color = DangerSoft) {
                        Text(
                            text = "${summary.lowStockCount} low",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = DangerRed,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = summary.name,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MidnightSlate,
                maxLines = 1
            )
            Text(
                text = "${summary.itemCount} items • ${summary.readinessPct}%",
                fontSize = 11.sp,
                color = LightSlate
            )
        }
    }
}

@Composable
private fun LocationBreakdownRow(summary: GroupSummary, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(shape = RoundedCornerShape(12.dp), color = TealSoft, modifier = Modifier.size(38.dp)) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Default.LocationOn, contentDescription = null, tint = TealPrimary, modifier = Modifier.size(18.dp))
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(summary.name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MidnightSlate)
                Text("${summary.itemCount} items", fontSize = 11.sp, color = LightSlate)
            }
            Spacer(modifier = Modifier.height(5.dp))
            val pctFloat = (summary.readinessPct / 100f).coerceIn(0f, 1f)
            LinearProgressIndicator(
                progress = { pctFloat },
                modifier = Modifier.fillMaxWidth().height(5.dp),
                color = if (summary.readinessPct < 50) OrangeAccent else TealPrimary,
                trackColor = Color(0xFFF1F5F9)
            )
        }
    }
}

@Composable
private fun EmptyStatusCard(message: String) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = CardSurface,
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Text(message, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = DarkSlate)
        }
    }
}

@Composable
fun ReformedFifoCard(
    item: InventoryItem,
    onCardClick: () -> Unit,
    onUseOne: () -> Unit
) {
    val visual = categoryVisualFor(item.category)
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onCardClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = CircleShape, color = visual.softBg, modifier = Modifier.size(38.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(PreplannedCatalog.iconForCategory(item.category), fontSize = 16.sp)
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(item.name, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MidnightSlate)
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val expBg = if (item.isExpired) DangerSoft else if (item.isExpiringSoon) WarningSoft else SuccessSoft
                        val expFg = if (item.isExpired) DangerRed else if (item.isExpiringSoon) WarningAmber else SuccessGreen
                        Surface(shape = RoundedCornerShape(6.dp), color = expBg) {
                            Text(
                                text = if (item.isExpired) "EXPIRED" else "Exp: ${item.earliestExpiry ?: "N/A"}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = expFg,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        if (item.location.isNotEmpty()) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("📍 ${item.location}", fontSize = 10.sp, color = LightSlate)
                        }
                    }
                }
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = SoftTileSurface,
                border = BorderStroke(1.dp, BorderSlate),
                modifier = Modifier.clickable(onClick = onUseOne)
            ) {
                Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Remove, contentDescription = null, modifier = Modifier.size(13.dp), tint = DarkSlate)
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("Use 1", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = DarkSlate)
                }
            }
        }
    }
}

@Composable
fun ReformedRestockCard(
    item: InventoryItem,
    onCardClick: () -> Unit,
    onQuickRestock: () -> Unit,
    onAddToGroceries: () -> Unit
) {
    val deficit = if (item.parLevel > item.currentQty) (item.parLevel - item.currentQty) else 1.0
    val visual = categoryVisualFor(item.category)

    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onCardClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = CircleShape, color = visual.softBg, modifier = Modifier.size(34.dp)) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(PreplannedCatalog.iconForCategory(item.category), fontSize = 15.sp)
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(item.name, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MidnightSlate)
                }

                Surface(shape = RoundedCornerShape(8.dp), color = DangerSoft) {
                    Text(
                        text = "Need +%.0f %s".format(deficit, item.unit),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = DangerRed,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            val rawPar = if (item.parLevel > 0) (item.currentQty / item.parLevel).toFloat() else 0f
            val parProgress = if (rawPar.isNaN() || rawPar.isInfinite()) 0f else rawPar.coerceIn(0f, 1f)
            LinearProgressIndicator(
                progress = { parProgress },
                modifier = Modifier.fillMaxWidth().height(6.dp),
                color = OrangeAccent,
                trackColor = Color(0xFFF1F5F9)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "%.0f / %.0f %s".format(item.currentQty, item.parLevel, item.unit),
                    fontSize = 12.sp,
                    color = MediumSlate,
                    fontWeight = FontWeight.SemiBold
                )

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = TealSoft,
                        modifier = Modifier.clickable(onClick = onQuickRestock)
                    ) {
                        Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Add, contentDescription = "Restock +1", modifier = Modifier.size(14.dp), tint = TealPrimary)
                            Spacer(modifier = Modifier.width(2.dp))
                            Text("+1", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TealPrimary)
                        }
                    }

                    Button(
                        onClick = onAddToGroceries,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = InkBlack),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Icon(Icons.Default.AddShoppingCart, contentDescription = null, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Groceries", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
