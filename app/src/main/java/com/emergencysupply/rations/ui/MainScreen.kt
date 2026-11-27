package com.emergencysupply.rations.ui

import android.app.Activity
import androidx.compose.foundation.background
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
import com.emergencysupply.rations.ui.theme.BackgroundAlabaster
import com.emergencysupply.rations.ui.theme.CardSurface
import com.emergencysupply.rations.ui.theme.DangerRed
import com.emergencysupply.rations.ui.theme.DarkSlate
import com.emergencysupply.rations.ui.theme.MidnightSlate
import com.emergencysupply.rations.ui.theme.SuccessGreen
import com.emergencysupply.rations.ui.theme.TealAccent
import com.emergencysupply.rations.ui.theme.TealPrimary
import com.emergencysupply.rations.ui.theme.TealSoft
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
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Emergency Rations",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MidnightSlate
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val (dotColor, syncLabel) = when (syncState) {
                                is SyncState.Syncing -> Pair(TealAccent, "Syncing...")
                                is SyncState.Success -> Pair(SuccessGreen, "Synced")
                                is SyncState.Error -> Pair(DangerRed, "Sync error")
                                is SyncState.Idle -> Pair(Color.Gray, "Offline cache ready")
                            }
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(dotColor, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = syncLabel, fontSize = 11.sp, color = Color.Gray)
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.syncNow() },
                        enabled = syncState !is SyncState.Syncing
                    ) {
                        if (syncState is SyncState.Syncing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = TealPrimary
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Sync,
                                contentDescription = "Sync Now",
                                tint = TealPrimary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = CardSurface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = CardSurface,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = currentTab == NavTab.DASHBOARD,
                    onClick = { viewModel.currentTab.value = NavTab.DASHBOARD },
                    icon = { Icon(Icons.Default.Dashboard, contentDescription = "Dashboard") },
                    label = { Text("Dashboard", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = TealPrimary,
                        selectedTextColor = TealPrimary,
                        indicatorColor = TealSoft
                    )
                )

                NavigationBarItem(
                    selected = currentTab == NavTab.INVENTORY,
                    onClick = { viewModel.currentTab.value = NavTab.INVENTORY },
                    icon = { Icon(Icons.Default.Inventory2, contentDescription = "Inventory") },
                    label = { Text("Inventory", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = TealPrimary,
                        selectedTextColor = TealPrimary,
                        indicatorColor = TealSoft
                    )
                )

                NavigationBarItem(
                    selected = currentTab == NavTab.GROCERIES,
                    onClick = { viewModel.currentTab.value = NavTab.GROCERIES },
                    icon = { Icon(Icons.Default.ShoppingCart, contentDescription = "Groceries") },
                    label = { Text("Groceries", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = TealPrimary,
                        selectedTextColor = TealPrimary,
                        indicatorColor = TealSoft
                    )
                )

                NavigationBarItem(
                    selected = currentTab == NavTab.SETTINGS,
                    onClick = { viewModel.currentTab.value = NavTab.SETTINGS },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                    label = { Text("Settings", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = TealPrimary,
                        selectedTextColor = TealPrimary,
                        indicatorColor = TealSoft
                    )
                )
            }
        },
        containerColor = BackgroundAlabaster
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

    // In-App Update Dialog when release is detected
    if (updateStatus is UpdateStatus.Available) {
        val updateAvailable = updateStatus as UpdateStatus.Available
        val release = updateAvailable.release
        val apkAsset = updateAvailable.apkAsset

        AlertDialog(
            onDismissRequest = { viewModel.dismissUpdate() },
            icon = {
                Icon(
                    imageVector = Icons.Default.SystemUpdate,
                    contentDescription = null,
                    tint = TealPrimary,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "Update Available: ${release.tag_name}",
                    fontWeight = FontWeight.Bold,
                    color = MidnightSlate
                )
            },
            text = {
                Column {
                    Text(
                        text = "A new release of Emergency Rations is ready to install.",
                        fontSize = 13.sp,
                        color = DarkSlate
                    )
                    val notes = release.body
                    if (!notes.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = notes,
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
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
                    colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
                ) {
                    Text("Download & Install")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissUpdate() }) {
                    Text("Later", color = Color.Gray)
                }
            }
        )
    }
}
