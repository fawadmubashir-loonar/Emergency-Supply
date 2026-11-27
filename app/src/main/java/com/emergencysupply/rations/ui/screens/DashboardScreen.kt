package com.emergencysupply.rations.ui.screens

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emergencysupply.rations.data.local.entity.InventoryItem
import com.emergencysupply.rations.ui.MainViewModel
import com.emergencysupply.rations.ui.theme.DangerRed
import com.emergencysupply.rations.ui.theme.DarkSlate
import com.emergencysupply.rations.ui.theme.InfoBlue
import com.emergencysupply.rations.ui.theme.MidnightSlate
import com.emergencysupply.rations.ui.theme.SuccessGreen
import com.emergencysupply.rations.ui.theme.TealAccent
import com.emergencysupply.rations.ui.theme.TealPrimary
import com.emergencysupply.rations.ui.theme.WarningAmber

@Composable
fun DashboardScreen(viewModel: MainViewModel) {
    val metrics by viewModel.dashboardMetrics.collectAsState()
    val expiringItems by viewModel.earliestExpiring.collectAsState()
    val restockItems by viewModel.restockNeeded.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. KPI Cards Section
        item {
            Text(
                text = "Pantry Readiness & Health",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MidnightSlate
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Row 1: Days of Supply & Total Servings
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                KpiCard(
                    title = "Days of Supply",
                    value = "${metrics.estDaysOfSupply}",
                    subtitle = "Based on family of 5",
                    accentColor = TealPrimary,
                    modifier = Modifier.weight(1f)
                )
                KpiCard(
                    title = "Total Servings",
                    value = "%,.0f".format(metrics.totalServings),
                    subtitle = "Available food calories",
                    accentColor = InfoBlue,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Row 2: In Stock & Unique Items
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                KpiCard(
                    title = "Units in Stock",
                    value = "%,.0f".format(metrics.totalUnitsInStock),
                    subtitle = "Total physical packs",
                    accentColor = SuccessGreen,
                    modifier = Modifier.weight(1f)
                )
                KpiCard(
                    title = "Unique Items",
                    value = "${metrics.totalUniqueItems}",
                    subtitle = "Pantry rations tracked",
                    accentColor = DarkSlate,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Row 3: Expiring Soon & Restock Needed
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                KpiCard(
                    title = "Expiring (<6 mos)",
                    value = "${metrics.expiringSoonCount}",
                    subtitle = "Priority use-first",
                    accentColor = if (metrics.expiringSoonCount > 0) WarningAmber else SuccessGreen,
                    modifier = Modifier.weight(1f)
                )
                KpiCard(
                    title = "Restock Needed",
                    value = "${metrics.restockNeededCount}",
                    subtitle = "Below par level",
                    accentColor = if (metrics.restockNeededCount > 0) DangerRed else SuccessGreen,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // 2. Priority Use-First (FIFO) Section
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Priority Use-First (Earliest Expiry)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MidnightSlate
                )
                Text(
                    text = "${expiringItems.size} items",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        }

        if (expiringItems.isEmpty()) {
            item {
                EmptySectionCard("All expiration dates are healthy!")
            }
        } else {
            items(expiringItems) { item ->
                FifoItemCard(item = item, onUseOne = { viewModel.useItem(item.id, 1.0) })
            }
        }

        // 3. Restock Alerts Section
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Restock Alerts (Below Par Level)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MidnightSlate
                )
                Text(
                    text = "${restockItems.size} needed",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        }

        if (restockItems.isEmpty()) {
            item {
                EmptySectionCard("All rations are fully stocked at or above par level! 🎉")
            }
        } else {
            items(restockItems) { item ->
                RestockAlertCard(
                    item = item,
                    onAddToGroceries = { viewModel.addRestockItemToGroceries(item) }
                )
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

@Composable
fun KpiCard(
    title: String,
    value: String,
    subtitle: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(text = title, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color.Gray)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = value, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = accentColor)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = subtitle, fontSize = 10.sp, color = Color.DarkGray)
        }
    }
}

@Composable
fun FifoItemCard(item: InventoryItem, onUseOne: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = item.name, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MidnightSlate)
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Exp: ${item.earliestExpiry ?: "N/A"}",
                        fontSize = 12.sp,
                        color = if (item.isExpired) DangerRed else if (item.isExpiringSoon) WarningAmber else SuccessGreen,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (item.daysUntilExpiry != null) {
                        Text(
                            text = " (${item.daysUntilExpiry}d left)",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                }
                Text(
                    text = "Stock: ${item.currentQty} ${item.unit} · Location: ${item.location}",
                    fontSize = 11.sp,
                    color = Color.Gray
                )
            }

            OutlinedButton(
                onClick = onUseOne,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TealPrimary)
            ) {
                Text("-1 Use", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun RestockAlertCard(item: InventoryItem, onAddToGroceries: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = item.name, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MidnightSlate)
                Spacer(modifier = Modifier.height(2.dp))
                val needed = (item.parLevel - item.currentQty).coerceAtLeast(0.0)
                Text(
                    text = "Have: ${item.currentQty} / Par: ${item.parLevel} ${item.unit}",
                    fontSize = 12.sp,
                    color = DangerRed,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Need +$needed ${item.unit} · ${item.category}",
                    fontSize = 11.sp,
                    color = Color.Gray
                )
            }

            Button(
                onClick = onAddToGroceries,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
            ) {
                Icon(Icons.Default.AddShoppingCart, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Grocery", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun EmptySectionCard(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(16.dp),
            fontSize = 13.sp,
            color = Color.Gray
        )
    }
}
