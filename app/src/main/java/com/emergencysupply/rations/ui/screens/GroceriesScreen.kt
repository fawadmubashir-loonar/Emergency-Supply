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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Inventory
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emergencysupply.rations.data.PreplannedCatalog
import com.emergencysupply.rations.data.local.entity.GroceryItem
import com.emergencysupply.rations.ui.MainViewModel
import com.emergencysupply.rations.ui.theme.BorderSlate
import com.emergencysupply.rations.ui.theme.CardSurface
import com.emergencysupply.rations.ui.theme.DangerRed
import com.emergencysupply.rations.ui.theme.DangerSoft
import com.emergencysupply.rations.ui.theme.DarkSlate
import com.emergencysupply.rations.ui.theme.LightSlate
import com.emergencysupply.rations.ui.theme.MediumSlate
import com.emergencysupply.rations.ui.theme.MidnightSlate
import com.emergencysupply.rations.ui.theme.MutedSlate
import com.emergencysupply.rations.ui.theme.TealPrimary
import com.emergencysupply.rations.ui.theme.TealSoft
import com.emergencysupply.rations.ui.theme.WarningAmber

@Composable
fun GroceriesScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val groceries by viewModel.allGroceries.collectAsState()
    val allInventory by viewModel.allInventory.collectAsState()
    val selectedStore by viewModel.groceryStoreFilter.collectAsState()

    var newName by remember { mutableStateOf("") }
    var newQty by remember { mutableStateOf("1") }
    var newUnit by remember { mutableStateOf("packs") }
    var newStore by remember { mutableStateOf("Grocery Store") }
    var newAisle by remember { mutableStateOf("General") }

    val storeOptions = listOf("All Stores", "Costco", "Halal Market", "Grocery Store", "Target", "Other")

    val quickPickItems = remember(allInventory) {
        val lowNames = allInventory.filter { it.isLowStock }.map { it.name }
        (lowNames + PreplannedCatalog.groceryQuickPicks).distinct().take(16)
    }

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
        // 1. Quick Add Section with One-Tap Pantry/Staple Chips
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardSurface),
                border = BorderStroke(1.dp, BorderSlate),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Quick Add to Shopping List",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MidnightSlate
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    // Scrollable One-Tap Quick-Pick Chips (no typing required!)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(quickPickItems) { pick ->
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = TealSoft,
                                border = BorderStroke(1.dp, TealPrimary.copy(alpha = 0.3f)),
                                modifier = Modifier.clickable {
                                    val targetStore = if (selectedStore != "All Stores") selectedStore else newStore
                                    val matchInv = allInventory.firstOrNull { it.name.equals(pick, ignoreCase = true) }
                                    viewModel.addGroceryItem(
                                        name = pick,
                                        qty = matchInv?.let { maxOf(1.0, it.parLevel - it.currentQty) } ?: 1.0,
                                        unit = matchInv?.unit?.ifBlank { "packs" } ?: "packs",
                                        aisle = matchInv?.category ?: "General",
                                        store = targetStore
                                    )
                                    Toast.makeText(context, "Added $pick to $targetStore", Toast.LENGTH_SHORT).show()
                                }
                            ) {
                                Text(
                                    text = "+ $pick",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TealPrimary,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = newName,
                            onValueChange = { newName = it },
                            placeholder = { Text("Or type custom item...", fontSize = 13.sp, color = LightSlate) },
                            singleLine = true,
                            modifier = Modifier.weight(1.8f),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = TealPrimary,
                                unfocusedBorderColor = BorderSlate,
                                focusedContainerColor = Color(0xFFF8FAFC),
                                unfocusedContainerColor = Color(0xFFF8FAFC)
                            )
                        )

                        OutlinedTextField(
                            value = newQty,
                            onValueChange = { newQty = it },
                            placeholder = { Text("Qty", fontSize = 13.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(0.7f),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = TealPrimary,
                                unfocusedBorderColor = BorderSlate,
                                focusedContainerColor = Color(0xFFF8FAFC),
                                unfocusedContainerColor = Color(0xFFF8FAFC)
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
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                            enabled = newName.isNotBlank()
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = "Add", modifier = Modifier.size(20.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Store:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MediumSlate)
                        val storeQuickList = listOf("Grocery Store", "Costco", "Halal Market", "Target")
                        storeQuickList.forEach { s ->
                            val isSel = (if (selectedStore != "All Stores") selectedStore else newStore).equals(s, ignoreCase = true)
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSel) TealSoft else Color(0xFFF1F5F9),
                                border = BorderStroke(1.dp, if (isSel) TealPrimary else BorderSlate),
                                modifier = Modifier.clickable { newStore = s }
                            ) {
                                Text(
                                    text = s,
                                    fontSize = 10.sp,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSel) TealPrimary else DarkSlate,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
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
                    val isSelected = selectedStore.equals(store, ignoreCase = true)
                    val storeItemCount = if (store == "All Stores") groceries.size else groceries.count { it.store.equals(store, ignoreCase = true) }
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.groceryStoreFilter.value = store },
                        label = {
                            Text(
                                text = if (storeItemCount > 0) "$store ($storeItemCount)" else store,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        shape = RoundedCornerShape(20.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = DarkSlate,
                            selectedLabelColor = Color.White,
                            containerColor = CardSurface,
                            labelColor = DarkSlate
                        ),
                        border = BorderStroke(1.dp, if (isSelected) DarkSlate else BorderSlate)
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
                    text = "$uncheckedCount remaining ($checkedCount checked)",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MidnightSlate
                )

                if (checkedCount > 0) {
                    OutlinedButton(
                        onClick = { viewModel.clearCheckedGroceries() },
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, DangerRed.copy(alpha = 0.4f)),
                        colors = ButtonDefaults.outlinedButtonColors(containerColor = DangerSoft)
                    ) {
                        Icon(imageVector = Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(16.dp), tint = DangerRed)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Clear Checked ($checkedCount)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = DangerRed)
                    }
                }
            }
        }

        // 4. Grouped Items Checklist
        if (filteredGroceries.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = CardSurface,
                    border = BorderStroke(1.dp, BorderSlate),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 36.dp, horizontal = 20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Surface(
                                shape = CircleShape,
                                color = TealSoft,
                                modifier = Modifier.size(52.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.ShoppingCart,
                                        contentDescription = null,
                                        modifier = Modifier.size(24.dp),
                                        tint = TealPrimary
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Shopping list is all clear! 🎉",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MidnightSlate
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Tap any quick-pick chip above to add items instantly.",
                                fontSize = 12.sp,
                                color = MediumSlate
                            )
                        }
                    }
                }
            }
        } else {
            val byStore = filteredGroceries.groupBy { it.store }
            byStore.forEach { (store, itemsInStore) ->
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = TealSoft,
                            border = BorderStroke(1.dp, TealPrimary.copy(alpha = 0.3f))
                        ) {
                            Text(
                                text = "📍 $store",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = TealPrimary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${itemsInStore.size} items",
                            fontSize = 11.sp,
                            color = LightSlate
                        )
                    }
                }

                items(itemsInStore, key = { "groc_${store}_${it.id}" }) { item ->
                    ReformedGroceryItemCard(
                        item = item,
                        onToggleChecked = { viewModel.toggleGroceryChecked(item.id, !item.checked) },
                        onToggleFavorite = { viewModel.toggleGroceryFavorite(item.id, !item.favorite) },
                        onMoveToPantry = {
                            viewModel.moveGroceryToPantry(item)
                            Toast.makeText(context, "Restocked +%.0f ${item.name} into Pantry!".format(item.qty), Toast.LENGTH_SHORT).show()
                        },
                        onDelete = { viewModel.deleteGrocery(item.id) }
                    )
                }
            }
        }

        item { Spacer(modifier = Modifier.height(36.dp)) }
    }
}

@Composable
fun ReformedGroceryItemCard(
    item: GroceryItem,
    onToggleChecked: () -> Unit,
    onToggleFavorite: () -> Unit,
    onMoveToPantry: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (item.checked) Color(0xFFF8FAFC) else CardSurface
        ),
        border = BorderStroke(1.dp, BorderSlate),
        elevation = CardDefaults.cardElevation(defaultElevation = if (item.checked) 0.dp else 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = item.checked,
                onCheckedChange = { onToggleChecked() },
                colors = CheckboxDefaults.colors(
                    checkedColor = TealPrimary,
                    uncheckedColor = MediumSlate
                ),
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(10.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onToggleChecked() }
            ) {
                Text(
                    text = item.name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (item.checked) LightSlate else MidnightSlate,
                    textDecoration = if (item.checked) TextDecoration.LineThrough else TextDecoration.None
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Qty: %.0f %s".format(item.qty, item.unit),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (item.checked) MutedSlate else DarkSlate
                    )
                    if (item.aisle.isNotEmpty() && item.aisle != "General") {
                        Text(
                            text = " • ${item.aisle}",
                            fontSize = 11.sp,
                            color = TealPrimary
                        )
                    }
                }
            }

            // 1-tap Move to Pantry Stock button
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = TealSoft,
                border = BorderStroke(1.dp, TealPrimary.copy(alpha = 0.25f)),
                modifier = Modifier
                    .padding(end = 4.dp)
                    .clickable(onClick = onMoveToPantry)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Inventory, contentDescription = "Move to Pantry", tint = TealPrimary, modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("+ Pantry", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TealPrimary)
                }
            }

            IconButton(onClick = onToggleFavorite, modifier = Modifier.size(30.dp)) {
                Icon(
                    imageVector = if (item.favorite) Icons.Default.Star else Icons.Outlined.StarOutline,
                    contentDescription = "Favorite",
                    tint = if (item.favorite) WarningAmber else LightSlate,
                    modifier = Modifier.size(19.dp)
                )
            }

            IconButton(onClick = onDelete, modifier = Modifier.size(30.dp)) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = LightSlate,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
