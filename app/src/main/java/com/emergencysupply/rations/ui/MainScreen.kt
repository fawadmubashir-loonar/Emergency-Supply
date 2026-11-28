package com.emergencysupply.rations.ui

import android.app.Activity
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.emergencysupply.rations.ui.components.FloatingCapsuleBottomBar
import com.emergencysupply.rations.ui.screens.DashboardScreen
import com.emergencysupply.rations.ui.screens.GroceriesScreen
import com.emergencysupply.rations.ui.screens.InventoryScreen
import com.emergencysupply.rations.ui.screens.SettingsScreen
import com.emergencysupply.rations.ui.theme.BackgroundSurface
import com.emergencysupply.rations.ui.theme.BorderSlate
import com.emergencysupply.rations.ui.theme.CardSurface
import com.emergencysupply.rations.ui.theme.DangerRed
import com.emergencysupply.rations.ui.theme.DarkSlate
import com.emergencysupply.rations.ui.theme.LightSlate
import com.emergencysupply.rations.ui.theme.MediumSlate
import com.emergencysupply.rations.ui.theme.MidnightSlate
import com.emergencysupply.rations.ui.theme.SoftTileSurface
import com.emergencysupply.rations.ui.theme.SuccessGreen
import com.emergencysupply.rations.ui.theme.TealAccent
import com.emergencysupply.rations.ui.theme.TealPrimary
import com.emergencysupply.rations.ui.theme.TealSoft
import com.emergencysupply.rations.updater.UpdateStatus

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
    val account by viewModel.currentAccount.collectAsState()
    val context = LocalContext.current

    val uncheckedGroceriesCount = groceries.count { !it.checked }
    val hasAlertDot = updateStatus is UpdateStatus.Available || metrics.restockNeededCount > 0 || metrics.expiringSoonCount > 0

    Scaffold(
        topBar = {
            // Frosted clean Dribbble/Finance style header
            Surface(color = BackgroundSurface) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        val firstName = account?.givenName ?: account?.displayName?.split(" ")?.firstOrNull() ?: "Family"
                        Text(
                            text = if (currentTab == NavTab.DASHBOARD) "Hello, $firstName" else currentTab.title,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MidnightSlate,
                            letterSpacing = (-0.4).sp
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            val (dotColor, syncLabel) = when (syncState) {
                                is SyncState.Syncing -> Pair(TealAccent, "Syncing Google Sheets...")
                                is SyncState.Success -> Pair(SuccessGreen, "Synced • Live Sheet")
                                is SyncState.Error -> Pair(DangerRed, "Sync Warning")
                                is SyncState.Idle -> Pair(LightSlate, "Offline Cache Active")
                            }
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .background(dotColor, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = syncLabel,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MediumSlate
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        // Alerts / Updates Bell Circle Button
                        Surface(
                            shape = CircleShape,
                            color = CardSurface,
                            shadowElevation = 2.dp,
                            border = BorderStroke(1.dp, BorderSlate.copy(alpha = 0.6f)),
                            modifier = Modifier
                                .size(42.dp)
                                .clickable {
                                    if (updateStatus is UpdateStatus.Available) {
                                        viewModel.currentTab.value = NavTab.SETTINGS
                                    } else {
                                        viewModel.openFilteredInventory(lowStockOnly = true)
                                    }
                                }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Notifications,
                                    contentDescription = "Alerts",
                                    tint = DarkSlate,
                                    modifier = Modifier.size(20.dp)
                                )
                                if (hasAlertDot) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(9.dp)
                                            .size(8.dp)
                                            .background(DangerRed, CircleShape)
                                    )
                                }
                            }
                        }

                        // Sync Pill Circle Button
                        Surface(
                            shape = CircleShape,
                            color = if (syncState is SyncState.Syncing) TealSoft else CardSurface,
                            shadowElevation = 2.dp,
                            border = BorderStroke(1.dp, BorderSlate.copy(alpha = 0.6f)),
                            modifier = Modifier
                                .size(42.dp)
                                .clickable(enabled = syncState !is SyncState.Syncing) { viewModel.syncNow() }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                if (syncState is SyncState.Syncing) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp,
                                        color = TealPrimary
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Sync,
                                        contentDescription = "Sync",
                                        tint = TealPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        bottomBar = {
            Box(modifier = Modifier.navigationBarsPadding()) {
                FloatingCapsuleBottomBar(
                    currentTab = currentTab,
                    restockBadgeCount = metrics.restockNeededCount,
                    groceriesBadgeCount = uncheckedGroceriesCount,
                    onSelectTab = { viewModel.currentTab.value = it }
                )
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

    if (updateStatus is UpdateStatus.Available) {
        val updateAvailable = updateStatus as UpdateStatus.Available
        val release = updateAvailable.release
        val apkAsset = updateAvailable.apkAsset

        AlertDialog(
            onDismissRequest = { viewModel.dismissUpdate() },
            shape = RoundedCornerShape(22.dp),
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
                    text = "Update Ready (${release.tag_name})",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MidnightSlate
                )
            },
            text = {
                Column {
                    Text(
                        text = "A new version of Emergency Rations is ready to install.",
                        fontSize = 13.sp,
                        color = DarkSlate
                    )
                    val notes = release.body
                    if (!notes.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = SoftTileSurface,
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
                    shape = RoundedCornerShape(12.dp)
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
