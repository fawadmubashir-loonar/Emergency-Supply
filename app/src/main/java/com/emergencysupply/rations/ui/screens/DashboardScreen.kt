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
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.graphics.Brush
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
import com.emergencysupply.rations.ui.components.categoryVisualFor
import com.emergencysupply.rations.ui.theme.BorderSlate
import com.emergencysupply.rations.ui.theme.CardSurface
import com.emergencysupply.rations.ui.theme.CobaltBlue
import com.emergencysupply.rations.ui.theme.DangerRed
import com.emergencysupply.rations.ui.theme.DangerSoft
import com.emergencysupply.rations.ui.theme.DarkSlate
import com.emergencysupply.rations.ui.theme.InkBlack
import com.emergencysupply.rations.ui.theme.LightSlate
import com.emergencysupply.rations.ui.theme.MediumSlate
import com.emergencysupply.rations.ui.theme.MidnightSlate
import com.emergencysupply.rations.ui.theme.OrangeAccent
import com.emergencysupply.rations.ui.theme.RoyalBlue
import com.emergencysupply.rations.ui.theme.SoftTileSurface
import com.emergencysupply.rations.ui.theme.SuccessGreen
import com.emergencysupply.rations.ui.theme.SuccessSoft
import com.emergencysupply.rations.ui.theme.TealPrimary
import com.emergencysupply.rations.ui.theme.TealSoft
import com.emergencysupply.rations.ui.theme.WarningAmber
import com.emergencysupply.rations.ui.theme.WarningSoft

private enum class HomeActionTab { RESTOCK, EXPIRING }

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

    var activeFocusTab by remember { mutableStateOf(HomeActionTab.RESTOCK) }
    var editingItem by remember { mutableStateOf<InventoryItem?>(null) }
    var showAddSheet by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Vibrant Gradient Readiness Hero Banner (Inspired by AgroPulse & Finance Hero)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.openFilteredInventory() },
                shape = RoundedCornerShape(26.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(RoyalBlue, CobaltBlue, TealPrimary)
                            )
                        )
                        .padding(20.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "HOUSEHOLD SUPPLY",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White.copy(alpha = 0.8f),
                                    letterSpacing = 1.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.Bottom) {
                                    Text(
                                        text = "${metrics.estDaysOfSupply}",
                                        fontSize = 42.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color.White,
                                        lineHeight = 42.sp
                                    )
                                    Text(
                                        text = " Days",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White.copy(alpha = 0.9f),
                                        modifier = Modifier.padding(bottom = 5.dp, start = 4.dp)
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = Color.White.copy(alpha = 0.2f)
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        text = "${metrics.parReadinessScore}%",
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "Par Goal",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White.copy(alpha = 0.9f)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        val progress = (metrics.parReadinessScore / 100f).coerceIn(0f, 1f)
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxWidth().height(7.dp),
                            color = Color.White,
                            trackColor = Color.White.copy(alpha = 0.25f)
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Crisp translucent summary strip
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color.White.copy(alpha = 0.14f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                HeroMiniStat("%,.0f".format(metrics.totalServings), "Servings")
                                HeroMiniStat("%,.0f".format(metrics.totalUnitsInStock), "Units")
                                HeroMiniStat("${metrics.totalUniqueItems}", "Rations")
                            }
                        }
                    }
                }
            }
        }

        // 2. Vibrant Quick Action Tiles (Solid Saturated Colors)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                QuickSolidActionTile(Icons.Default.Add, "Add Item", OrangeAccent) {
                    showAddSheet = true
                }
                QuickSolidActionTile(Icons.Default.Warning, "Low (${metrics.restockNeededCount})", DangerRed) {
                    viewModel.openFilteredInventory(lowStockOnly = true)
                }
                QuickSolidActionTile(Icons.Default.ShoppingCart, "Shop", SuccessGreen) {
                    viewModel.currentTab.value = NavTab.GROCERIES
                }
                QuickSolidActionTile(Icons.Default.Sync, "Sync", CobaltBlue) {
                    viewModel.syncNow()
                    Toast.makeText(context, "Syncing Google Sheets...", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // 3. Browse Categories (Colorful Solid Icon Tiles - Cloud Storage Style)
        if (categorySummaries.isNotEmpty()) {
            item {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Categories", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = MidnightSlate)
                        Text(
                            "View All >",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = CobaltBlue,
                            modifier = Modifier.clickable { viewModel.openFilteredInventory() }
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(categorySummaries, key = { "cat_card_${it.name}" }) { summary ->
                            VibrantCategoryCard(summary) {
                                viewModel.openFilteredInventory(category = summary.name)
                            }
                        }
                    }
                }
            }
        }

        // 4. Storage Zones Jump Pills
        if (locationSummaries.isNotEmpty()) {
            item {
                Column {
                    Text("Storage Spots", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = MidnightSlate)
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(locationSummaries, key = { "loc_pill_${it.name}" }) { loc ->
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = CardSurface,
                                shadowElevation = 2.dp,
                                modifier = Modifier.clickable { viewModel.openFilteredInventory(location = loc.name) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(shape = CircleShape, color = CobaltBlue, modifier = Modifier.size(26.dp)) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(loc.name, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MidnightSlate)
                                        Text("${loc.itemCount} items • ${loc.readinessPct}%", fontSize = 10.sp, color = LightSlate)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 5. Tidy Tabbed Focus Toggle (Keeps Home Clean & Concise!)
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val isRestock = activeFocusTab == HomeActionTab.RESTOCK
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (isRestock) DangerRed else CardSurface,
                    shadowElevation = if (isRestock) 3.dp else 1.dp,
                    modifier = Modifier.weight(1f).clickable { activeFocusTab = HomeActionTab.RESTOCK }
                ) {
                    Text(
                        text = "⚠️ Low Stock (${restockItems.size})",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isRestock) Color.White else DarkSlate,
                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 12.dp)
                    )
                }

                val isExp = activeFocusTab == HomeActionTab.EXPIRING
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (isExp) OrangeAccent else CardSurface,
                    shadowElevation = if (isExp) 3.dp else 1.dp,
                    modifier = Modifier.weight(1f).clickable { activeFocusTab = HomeActionTab.EXPIRING }
                ) {
                    Text(
                        text = "⏳ Expiring (${expiringItems.size})",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isExp) Color.White else DarkSlate,
                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 12.dp)
                    )
                }
            }
        }

        // 6. Active Focus List
        if (activeFocusTab == HomeActionTab.RESTOCK) {
            if (restockItems.isEmpty()) {
                item { EmptyStatusCard("All rations meet target par!") }
            } else {
                items(restockItems, key = { "restock_${it.id}" }) { item ->
                    ReformedRestockCard(
                        item = item,
                        onCardClick = { editingItem = item },
                        onQuickRestock = {
                            viewModel.restockItem(item.id, 1.0)
                            Toast.makeText(context, "+1 ${item.name} synced", Toast.LENGTH_SHORT).show()
                        },
                        onAddToGroceries = {
                            viewModel.addRestockItemToGroceries(item)
                            Toast.makeText(context, "Added ${item.name} to Groceries", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        } else {
            if (expiringItems.isEmpty()) {
                item { EmptyStatusCard("All expiration dates are healthy!") }
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
private fun HeroMiniStat(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 16.sp, fontWeight = FontWeight.Black, color = Color.White)
        Text(label, fontSize = 11.sp, color = Color.White.copy(alpha = 0.8f))
    }
}

@Composable
private fun QuickSolidActionTile(
    icon: ImageVector,
    label: String,
    bgColor: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = bgColor,
            shadowElevation = 4.dp,
            modifier = Modifier.size(52.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(imageVector = icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(24.dp))
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(text = label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = DarkSlate)
    }
}

@Composable
private fun VibrantCategoryCard(summary: GroupSummary, onClick: () -> Unit) {
    val visual = categoryVisualFor(summary.name)
    Card(
        modifier = Modifier
            .width(142.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(shape = RoundedCornerShape(12.dp), color = visual.tint, modifier = Modifier.size(38.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(PreplannedCatalog.iconForCategory(summary.name), fontSize = 18.sp)
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
                text = "${summary.itemCount} items",
                fontSize = 11.sp,
                color = LightSlate
            )
            Spacer(modifier = Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { (summary.readinessPct / 100f).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(4.dp),
                color = visual.tint,
                trackColor = Color(0xFFF1F5F9)
            )
        }
    }
}

@Composable
private fun EmptyStatusCard(message: String) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = CardSurface,
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Text(message, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = DarkSlate)
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
                Surface(shape = RoundedCornerShape(12.dp), color = visual.tint, modifier = Modifier.size(40.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(PreplannedCatalog.iconForItem(item.name, item.category), fontSize = 18.sp)
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(item.name, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MidnightSlate)
                    Spacer(modifier = Modifier.height(3.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val expBg = if (item.isExpired) DangerRed else if (item.isExpiringSoon) OrangeAccent else SuccessGreen
                        Surface(shape = RoundedCornerShape(6.dp), color = expBg) {
                            Text(
                                text = if (item.isExpired) "EXPIRED" else "Exp: ${item.earliestExpiry ?: "N/A"}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        if (item.location.isNotEmpty()) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("📍 ${item.location}", fontSize = 10.sp, color = MediumSlate)
                        }
                    }
                }
            }

            Surface(
                shape = RoundedCornerShape(10.dp),
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
                    Surface(shape = RoundedCornerShape(12.dp), color = visual.tint, modifier = Modifier.size(38.dp)) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(PreplannedCatalog.iconForItem(item.name, item.category), fontSize = 18.sp)
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(item.name, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MidnightSlate)
                }

                Surface(shape = RoundedCornerShape(8.dp), color = DangerRed) {
                    Text(
                        text = "Need +%.0f %s".format(deficit, item.unit),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
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
                color = visual.tint,
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
                    fontWeight = FontWeight.Bold
                )

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = SuccessGreen,
                        modifier = Modifier.clickable(onClick = onQuickRestock)
                    ) {
                        Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Add, contentDescription = "Restock +1", modifier = Modifier.size(14.dp), tint = Color.White)
                            Spacer(modifier = Modifier.width(2.dp))
                            Text("+1", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }

                    Button(
                        onClick = onAddToGroceries,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CobaltBlue),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Icon(Icons.Default.AddShoppingCart, contentDescription = null, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Shop", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
