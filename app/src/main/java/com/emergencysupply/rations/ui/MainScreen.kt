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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emergencysupply.rations.BuildConfig
import com.emergencysupply.rations.data.repository.SyncState
import com.emergencysupply.rations.ui.components.FloatingCapsuleBottomBar
import com.emergencysupply.rations.ui.screens.DashboardScreen
import com.emergencysupply.rations.ui.screens.GroceriesScreen
import com.emergencysupply.rations.ui.screens.InventoryScreen
import com.emergencysupply.rations.ui.screens.SettingsScreen
import com.emergencysupply.rations.ui.theme.BackgroundSurface
import com.emergencysupply.rations.ui.theme.BorderSlate
import com.emergencysupply.rations.ui.theme.CardSurface
import com.emergencysupply.rations.ui.theme.CobaltBlue
import com.emergencysupply.rations.ui.theme.DangerRed
import com.emergencysupply.rations.ui.theme.DangerSoft
import com.emergencysupply.rations.ui.theme.DarkSlate
import com.emergencysupply.rations.ui.theme.EmeraldDeep
import com.emergencysupply.rations.ui.theme.EmeraldVibrant
import com.emergencysupply.rations.ui.theme.LightSlate
import com.emergencysupply.rations.ui.theme.MediumSlate
import com.emergencysupply.rations.ui.theme.MidnightSlate
import com.emergencysupply.rations.ui.theme.PeachSoft
import com.emergencysupply.rations.ui.theme.PeachTint
import com.emergencysupply.rations.ui.theme.RoyalBlue
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
    val allInventory by viewModel.allInventory.collectAsState()
    val signInError by viewModel.signInError.collectAsState()
    val context = LocalContext.current

    val uncheckedGroceriesCount = groceries.count { !it.checked }
    val hasAlertDot = updateStatus is UpdateStatus.Available || metrics.restockNeededCount > 0 || metrics.expiringSoonCount > 0

    val syncErrorMessage = (syncState as? SyncState.Error)?.message.orEmpty()
    val isUnauthorized = account != null && allInventory.isEmpty() && (
        syncErrorMessage.contains("403", ignoreCase = true) ||
        syncErrorMessage.contains("PERMISSION_DENIED", ignoreCase = true) ||
        syncErrorMessage.contains("permission", ignoreCase = true) ||
        syncErrorMessage.contains("Forbidden", ignoreCase = true)
    )

    if (account == null) {
        AuthGateScreen(
            signInError = signInError,
            onSignInClick = onSignInClick,
            onCheckForUpdates = { viewModel.checkForUpdates(force = true) }
        )
    } else if (isUnauthorized) {
        UnauthorizedGateScreen(
            accountEmail = account?.email ?: "Unknown Account",
            errorMessage = syncErrorMessage,
            onSignOutClick = onSignOutClick,
            onRetrySync = { viewModel.syncNow() }
        )
    } else {
        Scaffold(
            topBar = {
                // Budgetly warm peach-to-cream header gradient
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Brush.verticalGradient(listOf(PeachSoft, BackgroundSurface)))
                ) {
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

@Composable
fun AuthGateScreen(
    signInError: String?,
    onSignInClick: () -> Unit,
    onCheckForUpdates: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = BackgroundSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Hero Wonder Bites Emerald gradient shield/lock card
            Card(
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(EmeraldDeep, EmeraldVibrant, TealPrimary)
                            )
                        )
                        .padding(horizontal = 24.dp, vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = PeachSoft.copy(alpha = 0.22f),
                            border = BorderStroke(1.5.dp, Color.White.copy(alpha = 0.5f)),
                            modifier = Modifier.size(72.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "Security Lock",
                                    tint = Color.White,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }

                        Text(
                            text = "Emergency Food Rations",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            letterSpacing = (-0.5).sp
                        )

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = PeachSoft.copy(alpha = 0.2f),
                            border = BorderStroke(1.dp, PeachTint.copy(alpha = 0.4f))
                        ) {
                            Text(
                                text = "Authorized Family Access Only",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                            )
                        }

                        Text(
                            text = "Sign in with a Google account shared on your household spreadsheet.",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White.copy(alpha = 0.9f),
                            textAlign = TextAlign.Center,
                            lineHeight = 20.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Display signInError in a crisp red alert box if present
            if (!signInError.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = DangerSoft,
                    border = BorderStroke(1.dp, DangerRed.copy(alpha = 0.4f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ErrorOutline,
                            contentDescription = "Error",
                            tint = DangerRed,
                            modifier = Modifier.size(24.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Authentication Notice",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = DangerRed
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = signInError,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = MidnightSlate,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }

            // Vibrant "Sign In with Google" button invoking onSignInClick
            Button(
                onClick = onSignInClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldDeep),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 3.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = "Sign In with Google",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // App version (v${BuildConfig.VERSION_NAME}) + "Check for Updates" text button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "v${BuildConfig.VERSION_NAME}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MediumSlate
                )
                Text(
                    text = " • ",
                    fontSize = 13.sp,
                    color = LightSlate
                )
                TextButton(
                    onClick = onCheckForUpdates,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "Check for Updates",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TealPrimary
                    )
                }
            }
        }
    }
}

@Composable
fun UnauthorizedGateScreen(
    accountEmail: String,
    errorMessage: String,
    onSignOutClick: () -> Unit,
    onRetrySync: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = BackgroundSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = CardSurface),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                border = BorderStroke(1.dp, BorderSlate),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = DangerSoft,
                        modifier = Modifier.size(64.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Unauthorized",
                                tint = DangerRed,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }

                    Text(
                        text = "Unauthorized Google Account",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MidnightSlate,
                        textAlign = TextAlign.Center
                    )

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = SoftTileSurface,
                        border = BorderStroke(1.dp, BorderSlate),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountCircle,
                                contentDescription = null,
                                tint = MediumSlate,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = accountEmail,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = DarkSlate,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Text(
                        text = "This account does not have access to the household spreadsheet. Please switch to an authorized family Google account that has permission to view and edit the emergency rations sheet.",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Normal,
                        color = MediumSlate,
                        textAlign = TextAlign.Center,
                        lineHeight = 19.sp
                    )

                    if (errorMessage.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = SoftTileSurface,
                            border = BorderStroke(1.dp, BorderSlate),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Sync details: $errorMessage",
                                fontSize = 11.sp,
                                color = LightSlate,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Button(
                        onClick = onSignOutClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DangerRed)
                    ) {
                        Text(
                            text = "Switch Google Account",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    TextButton(
                        onClick = onRetrySync,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                tint = TealPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Retry Sync",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TealPrimary
                            )
                        }
                    }
                }
            }
        }
    }
}
