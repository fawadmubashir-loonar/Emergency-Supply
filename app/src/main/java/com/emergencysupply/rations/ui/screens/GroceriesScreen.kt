package com.emergencysupply.rations.ui.screens

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emergencysupply.rations.data.local.entity.GroceryItem
import com.emergencysupply.rations.ui.MainViewModel
import com.emergencysupply.rations.ui.theme.DangerRed
import com.emergencysupply.rations.ui.theme.DarkSlate
import com.emergencysupply.rations.ui.theme.InfoBlue
import com.emergencysupply.rations.ui.theme.MidnightSlate
import com.emergencysupply.rations.ui.theme.SuccessGreen
import com.emergencysupply.rations.ui.theme.TealAccent
import com.emergencysupply.rations.ui.theme.TealPrimary
import com.emergencysupply.rations.ui.theme.TealSoft
import com.emergencysupply.rations.ui.theme.WarningAmber

@Composable
fun GroceriesScreen(viewModel: MainViewModel) {
    val groceries by viewModel.allGroceries.collectAsState()
    val selectedStore by viewModel.groceryStoreFilter.collectAsState()

    var newName by remember { mutableStateOf("") }
    var newQty by remember { mutableStateOf("1") }
    var newUnit by remember { mutableStateOf("packs") }
    var newStore by remember { mutableStateOf("Grocery Store") }
    var newAisle by remember { mutableStateOf("General") }

    val storeOptions = listOf("All Stores", "Costco", "Halal Market", "Grocery Store", "Target", "Other")

    val filteredGroceries = groceries.filter {
        selectedStore == "All Stores" || it.store.equals(selectedStore, ignoreCase = true)
    }

    val checkedCount = filteredGroceries.count { it.checked }
    val uncheckedCount = filteredGroceries.size - checkedCount

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Quick Add Section
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Quick Add to Grocery Checklist",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MidnightSlate
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = newName,
                            onValueChange = { newName = it },
                            placeholder = { Text("Item Name (e.g. Rice, Tuna)...", fontSize = 13.sp) },
                            singleLine = true,
                            modifier = Modifier.weight(1.8f),
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = TealPrimary,
                                unfocusedBorderColor = Color(0xFFCBD5E1)
                            )
                        )

                        OutlinedTextField(
                            value = newQty,
                            onValueChange = { newQty = it },
                            placeholder = { Text("Qty", fontSize = 13.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(0.7f),
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = TealPrimary,
                                unfocusedBorderColor = Color(0xFFCBD5E1)
                            )
                        )

                        Button(
                            onClick = {
                                if (newName.isNotBlank()) {
                                    val qtyVal = newQty.toDoubleOrNull() ?: 1.0
                                    viewModel.addGroceryItem(
                                        name = newName.trim(),
                                        qty = qtyVal,
                                        unit = newUnit.trim(),
                                        aisle = newAisle.trim(),
                                        store = if (selectedStore != "All Stores") selectedStore else newStore
                                    )
                                    newName = ""
                                    newQty = "1"
                                }
                            },
                            modifier = Modifier.height(52.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                            enabled = newName.isNotBlank()
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = "Add")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Store selection for Quick Add
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Target Store:", fontSize = 11.sp, color = Color.Gray)
                        val storeQuickList = listOf("Grocery Store", "Costco", "Halal Market", "Target")
                        storeQuickList.forEach { s ->
                            val isSel = (if (selectedStore != "All Stores") selectedStore else newStore).equals(s, ignoreCase = true)
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (isSel) TealPrimary else Color(0xFFF1F5F9),
                                modifier = Modifier.clickable { newStore = s }
                            ) {
                                Text(
                                    text = s,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (isSel) Color.White else DarkSlate,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 2. Store Filter Chips
        item {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(storeOptions) { store ->
                    FilterChip(
                        selected = selectedStore.equals(store, ignoreCase = true),
                        onClick = { viewModel.groceryStoreFilter.value = store },
                        label = { Text(store, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = DarkSlate,
                            selectedLabelColor = Color.White,
                            containerColor = Color.White,
                            labelColor = DarkSlate
                        )
                    )
                }
            }
        }

        // 3. Header & Clear Action Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$uncheckedCount to buy (${checkedCount} checked)",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MidnightSlate
                )

                if (checkedCount > 0) {
                    OutlinedButton(
                        onClick = { viewModel.clearCheckedGroceries() },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = DangerRed)
                    ) {
                        Icon(imageVector = Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Clear Checked", fontSize = 12.sp)
                    }
                }
            }
        }

        // 4. Grouped Items Checklist
        if (filteredGroceries.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.ShoppingCart,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = Color.LightGray
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Grocery list is empty! Restock alerts appear here.",
                            fontSize = 13.sp,
                            color = Color.Gray
                        )
                    }
                }
            }
        } else {
            // Group by Store then by Aisle
            val byStore = filteredGroceries.groupBy { it.store }
            byStore.forEach { (store, itemsInStore) ->
                item {
                    Text(
                        text = "📍 $store",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TealPrimary,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                items(itemsInStore, key = { it.id }) { item ->
                    GroceryItemCard(
                        item = item,
                        onToggleChecked = { viewModel.toggleGroceryChecked(item.id, !item.checked) },
                        onToggleFavorite = { viewModel.toggleGroceryFavorite(item.id, !item.favorite) },
                        onDelete = { viewModel.deleteGrocery(item.id) }
                    )
                }
            }
        }

        item { Spacer(modifier = Modifier.height(32.dp)) }
    }
}

@Composable
fun GroceryItemCard(
    item: GroceryItem,
    onToggleChecked: () -> Unit,
    onToggleFavorite: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (item.checked) Color(0xFFF8FAFC) else Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (item.checked) 0.dp else 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = item.checked,
                onCheckedChange = { onToggleChecked() },
                colors = CheckboxDefaults.colors(
                    checkedColor = TealPrimary,
                    uncheckedColor = Color.Gray
                )
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onToggleChecked() }
            ) {
                Text(
                    text = item.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (item.checked) Color.Gray else MidnightSlate,
                    textDecoration = if (item.checked) TextDecoration.LineThrough else TextDecoration.None
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Qty: %.0f %s".format(item.qty, item.unit),
                        fontSize = 12.sp,
                        color = if (item.checked) Color.LightGray else DarkSlate
                    )
                    if (item.aisle.isNotEmpty() && item.aisle != "General") {
                        Text(
                            text = " • ${item.aisle}",
                            fontSize = 11.sp,
                            color = TealAccent
                        )
                    }
                }
            }

            // Favorite star
            IconButton(onClick = onToggleFavorite) {
                Icon(
                    imageVector = if (item.favorite) Icons.Default.Star else Icons.Outlined.StarOutline,
                    contentDescription = "Favorite",
                    tint = if (item.favorite) WarningAmber else Color.LightGray,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Delete
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = Color.LightGray,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
