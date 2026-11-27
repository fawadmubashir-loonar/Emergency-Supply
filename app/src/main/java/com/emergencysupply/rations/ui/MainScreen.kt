package com.emergencysupply.rations.ui

import android.app.Activity
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emergencysupply.rations.data.repository.SyncState
import com.emergencysupply.rations.ui.screens.DashboardScreen
import com.emergencysupply.rations.ui.screens.GroceriesScreen
import com.emergencysupply.rations.ui.screens.InventoryScreen
import com.emergencysupply.rations.ui.screens.SettingsScreen
import com.emergencysupply.rations.ui.theme.BackgroundSurface
import com.emergencysupply.rations.ui.theme.BorderSlate
import com.emergencysupply.rations.ui.theme.CardSurface
import com.emergencysupply.rations.ui.theme.DangerRed
import com.emergencysupply.rations.ui.theme.DarkSlate
import com.emergencysupply.rations.ui.theme.MediumSlate
import com.emergencysupply.rations.ui.theme.MidnightSlate
import com.emergencysupply.rations.ui.theme.SuccessGreen
import com.emergencysupply.rations.ui.theme.SuccessSoft
import com.emergencysupply.rations.ui.theme.TealAccent
import com.emergencysupply.rations.ui.theme.TealPrimary
import com.emergencysupply.rations.ui.theme.TealSoft
import com.emergencysupply.rations.ui.theme.WarningAmber
import com.emergencysupply.rations.updater.UpdateStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    onSignInClick: () -> Unit,
    onSignOutClick: () -> Unit
) {
    val currentTab by viewModel.currentTab.collectAsState()
    val syncState by viewModel.syncState.collectAsState()
    val updateStatus by viewModel.updateStatus.collectAsState()
    val groceries by viewModel.allGroceries.collectAsState()
    val metrics by viewModel.dashboardMetrics.collectAsState()
    val context = LocalContext.current

    val uncheckedGroceriesCount = groceries.count { !it.checked }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Emergency Rations",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MidnightSlate,
                                letterSpacing = (-0.3).sp
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(top = 1.dp)
                            ) {
                                val (dotColor, syncLabel) = when (syncState) {
                                    is SyncState.Syncing -> Pair(TealAccent, "Syncing Sheets...")
                                    is SyncState.Success -> Pair(SuccessGreen, "Live Synced")
                                    is SyncState.Error -> Pair(DangerRed, "Sync Warning")
                                    is SyncState.Idle -> Pair(MediumSlate, "Offline Ready")
                                }
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .background(dotColor, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = syncLabel,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MediumSlate
                                )
                            }
                        }

                        // Right Action: Quick Sync Button with pill outline
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (syncState is SyncState.Syncing) TealSoft else Color(0xFFF1F5F9),
                            border = BorderStroke(1.dp, BorderSlate)
                        ) {
                            IconButton(
                                onClick = { viewModel.syncNow() },
                                enabled = syncState !is SyncState.Syncing,
                                modifier = Modifier.size(36.dp)
                            ) {
                                if (syncState is SyncState.Syncing) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = TealPrimary
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Sync,
                                        contentDescription = "Sync",
                                        tint = TealPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = CardSurface
                )
            )
        },
        bottomBar = {
            Surface(
                color = CardSurface,
                border = BorderStroke(1.dp, BorderSlate),
                tonalElevation = 2.dp
            ) {
                NavigationBar(
                    containerColor = CardSurface,
                    tonalElevation = 0.dp,
                    modifier = Modifier.height(68.dp)
                ) {
                    // 1. Dashboard Tab
                    NavigationBarItem(
                        selected = currentTab == NavTab.DASHBOARD,
                        onClick = { viewModel.currentTab.value = NavTab.DASHBOARD },
                        icon = {
                            if (metrics.restockNeededCount > 0) {
                                BadgedBox(badge = {
                                    Badge(containerColor = DangerRed) {
                                        Text("${metrics.restockNeededCount}", fontSize = 10.sp, color = Color.White)
                                    }
                                }) {
                                    Icon(Icons.Default.Dashboard, contentDescription = "Dashboard", modifier = Modifier.size(22.dp))
                                }
                            } else {
                                Icon(Icons.Default.Dashboard, contentDescription = "Dashboard", modifier = Modifier.size(22.dp))
                            }
                        },
                        label = { Text("Dashboard", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = TealPrimary,
                            selectedTextColor = TealPrimary,
                            indicatorColor = TealSoft,
                            unselectedIconColor = MediumSlate,
                            unselectedTextColor = MediumSlate
                        )
                    )

                    // 2. Inventory Tab
                    NavigationBarItem(
                        selected = currentTab == NavTab.INVENTORY,
                        onClick = { viewModel.currentTab.value = NavTab.INVENTORY },
                        icon = { Icon(Icons.Default.Inventory2, contentDescription = "Inventory", modifier = Modifier.size(22.dp)) },
                        label = { Text("Inventory", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = TealPrimary,
                            selectedTextColor = TealPrimary,
                            indicatorColor = TealSoft,
                            unselectedIconColor = MediumSlate,
                            unselectedTextColor = MediumSlate
                        )
                    )

                    // 3. Groceries Tab with Unchecked Badge
                    NavigationBarItem(
                        selected = currentTab == NavTab.GROCERIES,
                        onClick = { viewModel.currentTab.value = NavTab.GROCERIES },
                        icon = {
                            if (uncheckedGroceriesCount > 0) {
                                BadgedBox(badge = {
                                    Badge(containerColor = TealPrimary) {
                                        Text("$uncheckedGroceriesCount", fontSize = 10.sp, color = Color.White)
                                    }
                                }) {
                                    Icon(Icons.Default.ShoppingCart, contentDescription = "Groceries", modifier = Modifier.size(22.dp))
                                }
                            } else {
                                Icon(Icons.Default.ShoppingCart, contentDescription = "Groceries", modifier = Modifier.size(22.dp))
                            }
                        },
                        label = { Text("Groceries", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = TealPrimary,
                            selectedTextColor = TealPrimary,
                            indicatorColor = TealSoft,
                            unselectedIconColor = MediumSlate,
                            unselectedTextColor = MediumSlate
                        )
                    )

                    // 4. Settings Tab
                    NavigationBarItem(
                        selected = currentTab == NavTab.SETTINGS,
                        onClick = { viewModel.currentTab.value = NavTab.SETTINGS },
                        icon = { Icon(Icons.Default.Settings, contentDescription = "Settings", modifier = Modifier.size(22.dp)) },
                        label = { Text("Settings", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = TealPrimary,
                            selectedTextColor = TealPrimary,
                            indicatorColor = TealSoft,
                            unselectedIconColor = MediumSlate,
                            unselectedTextColor = MediumSlate
                        )
                    )
                }
            }
        },
        containerColor = BackgroundSurface
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                NavTab.DASHBOARD -> DashboardScreen(viewModel = viewModel)
                NavTab.INVENTORY -> InventoryScreen(viewModel = viewModel)
                NavTab.GROCERIES -> GroceriesScreen(viewModel = viewModel)
                NavTab.SETTINGS -> SettingsScreen(
                    viewModel = viewModel,
                    onSignInClick = onSignInClick,
                    onSignOutClick = onSignOutClick
                )
            }
        }
    }

    // In-App Update Prompt Dialog
    if (updateStatus is UpdateStatus.Available) {
        val updateAvailable = updateStatus as UpdateStatus.Available
        val release = updateAvailable.release
        val apkAsset = updateAvailable.apkAsset

        AlertDialog(
            onDismissRequest = { viewModel.dismissUpdate() },
            shape = RoundedCornerShape(16.dp),
            containerColor = Color.White,
            icon = {
                Surface(
                    shape = CircleShape,
                    color = TealSoft,
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.SystemUpdate,
                            contentDescription = null,
                            tint = TealPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            },
            title = {
                Text(
                    text = "Update Available (${release.tag_name})",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = MidnightSlate
                )
            },
            text = {
                Column {
                    Text(
                        text = "A new improved version of Emergency Rations is ready to install.",
                        fontSize = 13.sp,
                        color = DarkSlate
                    )
                    val notes = release.body
                    if (!notes.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFF8FAFC),
                            border = BorderStroke(1.dp, BorderSlate),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = notes,
                                fontSize = 11.sp,
                                color = MediumSlate,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (context is Activity) {
                            viewModel.startDownloadUpdate(context, apkAsset)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Download & Install", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissUpdate() }) {
                    Text("Later", color = MediumSlate)
                }
            }
        )
    }
}
