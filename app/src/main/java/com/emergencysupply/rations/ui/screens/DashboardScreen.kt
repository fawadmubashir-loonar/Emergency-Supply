package com.emergencysupply.rations.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emergencysupply.rations.data.local.entity.InventoryItem
import com.emergencysupply.rations.ui.MainViewModel
import com.emergencysupply.rations.ui.theme.BorderSlate
import com.emergencysupply.rations.ui.theme.CardSurface
import com.emergencysupply.rations.ui.theme.DangerRed
import com.emergencysupply.rations.ui.theme.DangerSoft
import com.emergencysupply.rations.ui.theme.DarkSlate
import com.emergencysupply.rations.ui.theme.InfoBlue
import com.emergencysupply.rations.ui.theme.InfoSoft
import com.emergencysupply.rations.ui.theme.LightSlate
import com.emergencysupply.rations.ui.theme.MediumSlate
import com.emergencysupply.rations.ui.theme.MidnightSlate
import com.emergencysupply.rations.ui.theme.SuccessGreen
import com.emergencysupply.rations.ui.theme.SuccessSoft
import com.emergencysupply.rations.ui.theme.TealAccent
import com.emergencysupply.rations.ui.theme.TealPrimary
import com.emergencysupply.rations.ui.theme.TealSoft
import com.emergencysupply.rations.ui.theme.TealVerySoft
import com.emergencysupply.rations.ui.theme.WarningAmber
import com.emergencysupply.rations.ui.theme.WarningSoft

@Composable
fun DashboardScreen(viewModel: MainViewModel) {
    val metrics by viewModel.dashboardMetrics.collectAsState()
    val expiringItems by viewModel.earliestExpiring.collectAsState()
    val restockItems by viewModel.restockNeeded.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Hero Readiness Banner
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = TealPrimary),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "ESTIMATED SUPPLY",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = TealSoft,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    text = "${metrics.estDaysOfSupply}",
                                    fontSize = 42.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White,
                                    lineHeight = 44.sp
                                )
                                Text(
                                    text = " Days of Food",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TealSoft,
                                    modifier = Modifier.padding(bottom = 6.dp, start = 4.dp)
                                )
                            }
                        }

                        // Readiness Badge
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = if (metrics.estDaysOfSupply >= 90) "90D Goal Met" else "Building Par",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Progress toward 90-day readiness goal
                    val supplyProgress = (metrics.estDaysOfSupply.toFloat() / 90f).coerceIn(0f, 1f)
                    LinearProgressIndicator(
                        progress = { supplyProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp),
                        color = Color.White,
                        trackColor = Color.White.copy(alpha = 0.25f)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Calculated for 5 household members at 3 meals/day (~15 daily servings)",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }
            }
        }

        // 2. 2x2 Metric Grid
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricTile(
                        icon = Icons.Default.Fastfood,
                        iconTint = InfoBlue,
                        iconBg = InfoSoft,
                        value = "%,.0f".format(metrics.totalServings),
                        title = "Food Servings",
                        subtitle = "Total pantry calories",
                        modifier = Modifier.weight(1f)
                    )
                    MetricTile(
                        icon = Icons.Default.Inventory,
                        iconTint = TealPrimary,
                        iconBg = TealSoft,
                        value = "%,.0f".format(metrics.totalUnitsInStock),
                        title = "Units in Stock",
                        subtitle = "${metrics.totalUniqueItems} unique items",
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricTile(
                        icon = Icons.Default.HourglassBottom,
                        iconTint = if (metrics.expiringSoonCount > 0) WarningAmber else SuccessGreen,
                        iconBg = if (metrics.expiringSoonCount > 0) WarningSoft else SuccessSoft,
                        value = "${metrics.expiringSoonCount}",
                        title = "Expiring Soon",
                        subtitle = if (metrics.expiringSoonCount > 0) "<6 months left" else "All dates healthy",
                        modifier = Modifier.weight(1f)
                    )
                    MetricTile(
                        icon = Icons.Default.Warning,
                        iconTint = if (metrics.restockNeededCount > 0) DangerRed else SuccessGreen,
                        iconBg = if (metrics.restockNeededCount > 0) DangerSoft else SuccessSoft,
                        value = "${metrics.restockNeededCount}",
                        title = "Restock Needed",
                        subtitle = if (metrics.restockNeededCount > 0) "Below target par" else "All par met",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // 3. FIFO Priority Use-First Section
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Priority Use-First (FIFO)",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MidnightSlate
                    )
                    Text(
                        text = "Earliest expiring rations to rotate first",
                        fontSize = 12.sp,
                        color = MediumSlate
                    )
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFF1F5F9),
                    border = BorderStroke(1.dp, BorderSlate)
                ) {
                    Text(
                        text = "${expiringItems.size} items",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = DarkSlate,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        if (expiringItems.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = CardSurface,
                    border = BorderStroke(1.dp, BorderSlate),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = SuccessGreen,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "No rations expiring within the next 6 months!",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = DarkSlate
                        )
                    }
                }
            }
        } else {
            items(expiringItems, key = { it.id }) { item ->
                ReformedFifoCard(item = item, onUseOne = { viewModel.useItem(item.id, 1.0) })
            }
        }

        // 4. Restock Alerts Section
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Restock Alerts (Below Par)",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MidnightSlate
                    )
                    Text(
                        text = "Pantry items that need replenishment",
                        fontSize = 12.sp,
                        color = MediumSlate
                    )
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (restockItems.isNotEmpty()) DangerSoft else SuccessSoft,
                    border = BorderStroke(1.dp, if (restockItems.isNotEmpty()) DangerRed.copy(alpha = 0.2f) else SuccessGreen.copy(alpha = 0.2f))
                ) {
                    Text(
                        text = "${restockItems.size} alerts",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (restockItems.isNotEmpty()) DangerRed else SuccessGreen,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        if (restockItems.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = CardSurface,
                    border = BorderStroke(1.dp, BorderSlate),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = SuccessGreen,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "All rations are fully stocked at or above target par! 🎉",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = DarkSlate
                        )
                    }
                }
            }
        } else {
            items(restockItems, key = { it.id }) { item ->
                ReformedRestockCard(
                    item = item,
                    onAddToGroceries = { viewModel.addRestockItemToGroceries(item) }
                )
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

@Composable
fun MetricTile(
    icon: ImageVector,
    iconTint: Color,
    iconBg: Color,
    value: String,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        border = BorderStroke(1.dp, BorderSlate),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = iconBg,
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(imageVector = icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(18.dp))
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MediumSlate
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = value,
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MidnightSlate
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = LightSlate
            )
        }
    }
}

@Composable
fun ReformedFifoCard(item: InventoryItem, onUseOne: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        border = BorderStroke(1.dp, BorderSlate),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MidnightSlate
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val expBadgeBg = if (item.isExpired) DangerSoft else if (item.isExpiringSoon) WarningSoft else SuccessSoft
                    val expBadgeColor = if (item.isExpired) DangerRed else if (item.isExpiringSoon) WarningAmber else SuccessGreen
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = expBadgeBg
                    ) {
                        Text(
                            text = if (item.isExpired) "EXPIRED" else "Exp: ${item.earliestExpiry ?: "N/A"}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = expBadgeColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    if (item.location.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "📍 ${item.location}",
                            fontSize = 11.sp,
                            color = LightSlate
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Stock: %.0f %s • %.0f total servings".format(item.currentQty, item.unit, item.totalServings),
                    fontSize = 11.sp,
                    color = MediumSlate
                )
            }

            // Quick Use Button
            OutlinedButton(
                onClick = onUseOne,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, BorderSlate),
                colors = ButtonDefaults.outlinedButtonColors(containerColor = Color(0xFFF8FAFC)),
                modifier = Modifier.height(38.dp)
            ) {
                Icon(imageVector = Icons.Default.Remove, contentDescription = null, modifier = Modifier.size(14.dp), tint = DarkSlate)
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = "Use 1", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = DarkSlate)
            }
        }
    }
}

@Composable
fun ReformedRestockCard(item: InventoryItem, onAddToGroceries: () -> Unit) {
    val deficit = if (item.parLevel > item.currentQty) (item.parLevel - item.currentQty) else 1.0

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        border = BorderStroke(1.dp, DangerRed.copy(alpha = 0.25f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MidnightSlate,
                    modifier = Modifier.weight(1f)
                )

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = DangerSoft
                ) {
                    Text(
                        text = "Need +%.0f %s".format(deficit, item.unit),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = DangerRed,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Stock Bar
            val parProgress = if (item.parLevel > 0) (item.currentQty / item.parLevel).toFloat().coerceIn(0f, 1f) else 0f
            LinearProgressIndicator(
                progress = { parProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp),
                color = WarningAmber,
                trackColor = Color(0xFFE2E8F0)
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Current: %.0f / Par: %.0f %s".format(item.currentQty, item.parLevel, item.unit),
                    fontSize = 12.sp,
                    color = MediumSlate,
                    fontWeight = FontWeight.Medium
                )

                Button(
                    onClick = onAddToGroceries,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                    modifier = Modifier.height(34.dp)
                ) {
                    Icon(imageVector = Icons.Default.AddShoppingCart, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "Add to Groceries", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
