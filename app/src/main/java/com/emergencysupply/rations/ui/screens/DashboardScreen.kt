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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.CheckCircle
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emergencysupply.rations.data.PreplannedCatalog
import com.emergencysupply.rations.data.local.entity.InventoryItem
import com.emergencysupply.rations.ui.GroupSummary
import com.emergencysupply.rations.ui.MainViewModel
import com.emergencysupply.rations.ui.NavTab
import com.emergencysupply.rations.ui.components.categoryVisualFor
import com.emergencysupply.rations.ui.components.dashedBorder
import com.emergencysupply.rations.ui.components.locationVisualFor
import com.emergencysupply.rations.ui.theme.BorderSlate
import com.emergencysupply.rations.ui.theme.CardSurface
import com.emergencysupply.rations.ui.theme.CobaltBlue
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
import com.emergencysupply.rations.ui.theme.RoyalBlue
import com.emergencysupply.rations.ui.theme.SoftTileSurface
import com.emergencysupply.rations.ui.theme.SuccessGreen
import com.emergencysupply.rations.ui.theme.SuccessSoft
import com.emergencysupply.rations.ui.theme.TealPrimary
import com.emergencysupply.rations.ui.theme.WarningAmber
import com.emergencysupply.rations.ui.theme.WarningSoft

private enum class HomeActionTab { RESTOCK, EXPIRING }

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DashboardScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val metrics by viewModel.dashboardMetrics.collectAsState()
    val expiringItems by viewModel.earliestExpiring.collectAsState()
    val restockItems by viewModel.restockNeeded.collectAsState()
    val locationSummaries by viewModel.locationSummaries.collectAsState()
    val allInventory by viewModel.allInventory.collectAsState()
    val allGroceries by viewModel.allGroceries.collectAsState()
    val availableCategories by viewModel.availableCategories.collectAsState()
    val availableLocations by viewModel.availableLocations.collectAsState()

    val criticalItems = remember(expiringItems) {
        expiringItems.filter { it.isExpired || (it.daysUntilExpiry != null && it.daysUntilExpiry <= 30) }
    }

    var activeFocusTab by remember { mutableStateOf(HomeActionTab.RESTOCK) }
    var editingItem by remember { mutableStateOf<InventoryItem?>(null) }
    var showAddSheet by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Vibrant Gradient Readiness Hero Banner (with Smarter Translucent Alert Pills)
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
                                colors = listOf(EmeraldDeep, EmeraldVibrant, TealPrimary)
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

                        Spacer(modifier = Modifier.height(10.dp))

                        // Smarter Hero Banner: Two clickable translucent alert pills
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color.White.copy(alpha = 0.18f),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { viewModel.openFilteredInventory(lowStockOnly = true) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = "Low Stock (${metrics.restockNeededCount})",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color.White.copy(alpha = 0.18f),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { viewModel.openFilteredInventory(expiringOnly = true) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = "Expiring (${metrics.expiringSoonCount})",
                                        fontSize = 11.sp,
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

        // 2. Vibrant Quick Action Tiles (Solid Saturated Colors)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                QuickSolidActionTile(Icons.Default.Add, "Add Item", InkBlack) {
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

        // 3. Live Groceries Card (Replaces redundant Categories row)
        item {
            val pendingCount = allGroceries.count { !it.checked }
            val checkedCount = allGroceries.count { it.checked }
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.currentTab.value = NavTab.GROCERIES },
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = CardSurface),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = SuccessSoft,
                            modifier = Modifier.size(46.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.ShoppingCart,
                                    contentDescription = "Groceries",
                                    tint = SuccessGreen,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Shopping List",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MidnightSlate
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "$pendingCount to buy • $checkedCount in cart",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = MediumSlate
                            )
                        }
                    }
                    Button(
                        onClick = { viewModel.currentTab.value = NavTab.GROCERIES },
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = InkBlack),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "Start Trip",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }

        // 4. Compact Storage Zone Health (2-column Grid)
        if (locationSummaries.isNotEmpty()) {
            item {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Storage Spots", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = MidnightSlate)
                        Text(
                            "View All >",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = CobaltBlue,
                            modifier = Modifier.clickable { viewModel.openFilteredInventory() }
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        locationSummaries.chunked(2).forEach { rowPair ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                rowPair.forEach { loc ->
                                    StorageZoneCard(
                                        loc = loc,
                                        modifier = Modifier.weight(1f),
                                        onClick = { viewModel.openFilteredInventory(location = loc.name) }
                                    )
                                }
                                if (rowPair.size == 1) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        }

        // 5. Action-Needed / All-Clear Strip
        if (criticalItems.isNotEmpty()) {
            item {
                val firstCritical = criticalItems.first()
                val isExpired = firstCritical.isExpired
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .dashedBorder(
                            width = 1.5.dp,
                            color = if (isExpired) DangerRed else OrangeAccent,
                            cornerRadius = 16.dp
                        )
                        .combinedClickable(
                            onClick = { editingItem = firstCritical },
                            onLongClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                editingItem = firstCritical
                            }
                        ),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isExpired) DangerSoft else PeachSoft
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = if (isExpired) DangerRed else OrangeAccent,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "${criticalItems.size} Action Needed: ${firstCritical.name}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MidnightSlate,
                                    maxLines = 1
                                )
                                val expiryDetail = if (isExpired) "Already expired!" else "Expires in ${firstCritical.daysUntilExpiry ?: 0} days"
                                Text(
                                    text = "$expiryDetail • Tap to inspect",
                                    fontSize = 11.sp,
                                    color = if (isExpired) DangerRed else OrangeAccent,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isExpired) DangerRed else OrangeAccent
                        ) {
                            Text(
                                text = "Inspect",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        } else if (restockItems.isEmpty() && expiringItems.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = SuccessSoft,
                    border = BorderStroke(1.dp, SuccessGreen.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = SuccessGreen,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "All systems clear! Pantry is fully stocked & fresh.",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = DarkSlate
                        )
                    }
                }
            }
        }

        // 6. Tidy Tabbed Focus Toggle (Keeps Home Clean & Concise!)
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

        // 7. Active Focus List (Top-5 Urgent Focus)
        val isRestock = activeFocusTab == HomeActionTab.RESTOCK
        if (isRestock) {
            if (restockItems.isEmpty()) {
                item { EmptyStatusCard("All rations meet target par!") }
            } else {
                val displayItems = restockItems.take(5)
                items(displayItems, key = { "restock_${it.id}" }) { item ->
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
                if (restockItems.size > 5) {
                    item {
                        Button(
                            onClick = { viewModel.openFilteredInventory(lowStockOnly = isRestock, expiringOnly = !isRestock) },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CardSurface),
                            border = BorderStroke(1.dp, BorderSlate),
                            modifier = Modifier.fillMaxWidth().height(44.dp)
                        ) {
                            Text(
                                text = "See all ${restockItems.size} in Pantry >",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = CobaltBlue
                            )
                        }
                    }
                }
            }
        } else {
            if (expiringItems.isEmpty()) {
                item { EmptyStatusCard("All expiration dates are healthy!") }
            } else {
                val displayItems = expiringItems.take(5)
                items(displayItems, key = { "fifo_${it.id}" }) { item ->
                    ReformedFifoCard(
                        item = item,
                        onCardClick = { editingItem = item },
                        onUseOne = {
                            viewModel.useItem(item.id, 1.0)
                            Toast.makeText(context, "Used 1 ${item.name}", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
                if (expiringItems.size > 5) {
                    item {
                        Button(
                            onClick = { viewModel.openFilteredInventory(lowStockOnly = isRestock, expiringOnly = !isRestock) },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CardSurface),
                            border = BorderStroke(1.dp, BorderSlate),
                            modifier = Modifier.fillMaxWidth().height(44.dp)
                        ) {
                            Text(
                                text = "See all ${expiringItems.size} in Pantry >",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = CobaltBlue
                            )
                        }
                    }
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
private fun StorageZoneCard(
    loc: GroupSummary,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val is100Ready = loc.readinessPct >= 100
    val readinessColor = when {
        loc.readinessPct >= 80 -> SuccessGreen
        loc.readinessPct >= 50 -> OrangeAccent
        else -> DangerRed
    }
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (is100Ready) Color.Transparent else CardSurface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = if (is100Ready) null else BorderStroke(1.dp, BorderSlate)
    ) {
        val contentModifier = if (is100Ready) {
            Modifier
                .fillMaxWidth()
                .background(Brush.linearGradient(listOf(EmeraldDeep, EmeraldVibrant, SuccessGreen)))
                .padding(12.dp)
        } else {
            Modifier
                .fillMaxWidth()
                .padding(12.dp)
        }

        Column(modifier = contentModifier) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = loc.name,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (is100Ready) Color.White else MidnightSlate,
                    maxLines = 1,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (is100Ready) Color.White.copy(alpha = 0.22f) else readinessColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "${loc.readinessPct}%",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (is100Ready) Color.White else readinessColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${loc.itemCount} items",
                fontSize = 11.sp,
                fontWeight = if (is100Ready) FontWeight.Medium else FontWeight.Normal,
                color = if (is100Ready) Color.White.copy(alpha = 0.9f) else MediumSlate
            )
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { (loc.readinessPct / 100f).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp),
                color = if (is100Ready) Color.White else readinessColor,
                trackColor = if (is100Ready) Color.White.copy(alpha = 0.25f) else Color(0xFFF1F5F9)
            )
        }
    }
}

@Composable
private fun EmptyStatusCard(message: String) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = CardSurface,
        modifier = Modifier
            .fillMaxWidth()
            .dashedBorder(1.dp, BorderSlate, 18.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Text(message, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = DarkSlate)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ReformedFifoCard(
    item: InventoryItem,
    onCardClick: () -> Unit,
    onUseOne: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val visual = categoryVisualFor(item.category)
    val locVisual = locationVisualFor(item.location)
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
                            Surface(shape = RoundedCornerShape(6.dp), color = locVisual.softBg) {
                                Text(
                                    text = "📍 ${item.location}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = locVisual.tint,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ReformedRestockCard(
    item: InventoryItem,
    onCardClick: () -> Unit,
    onQuickRestock: () -> Unit,
    onAddToGroceries: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val deficit = if (item.parLevel > item.currentQty) (item.parLevel - item.currentQty) else 1.0
    val visual = categoryVisualFor(item.category)

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
