package com.emergencysupply.rations.ui.screens

import android.app.Activity
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emergencysupply.rations.BuildConfig
import com.emergencysupply.rations.data.local.entity.HistoryLogEntry
import com.emergencysupply.rations.data.repository.SyncState
import com.emergencysupply.rations.ui.MainViewModel
import com.emergencysupply.rations.ui.components.dashedBorder
import com.emergencysupply.rations.ui.theme.BorderSlate
import com.emergencysupply.rations.ui.theme.CardSurface
import com.emergencysupply.rations.ui.theme.CobaltBlue
import com.emergencysupply.rations.ui.theme.CobaltSoft
import com.emergencysupply.rations.ui.theme.DangerRed
import com.emergencysupply.rations.ui.theme.DangerSoft
import com.emergencysupply.rations.ui.theme.DarkSlate
import com.emergencysupply.rations.ui.theme.EmeraldVibrant
import com.emergencysupply.rations.ui.theme.InkBlack
import com.emergencysupply.rations.ui.theme.LightSlate
import com.emergencysupply.rations.ui.theme.MediumSlate
import com.emergencysupply.rations.ui.theme.MidnightSlate
import com.emergencysupply.rations.ui.theme.OrangeAccent
import com.emergencysupply.rations.ui.theme.OrangeSoft
import com.emergencysupply.rations.ui.theme.PeachSoft
import com.emergencysupply.rations.ui.theme.SoftTileSurface
import com.emergencysupply.rations.ui.theme.SuccessGreen
import com.emergencysupply.rations.ui.theme.SuccessSoft
import com.emergencysupply.rations.ui.theme.TealPrimary
import com.emergencysupply.rations.ui.theme.TealSoft
import com.emergencysupply.rations.ui.theme.WarningAmber
import com.emergencysupply.rations.ui.theme.WarningSoft
import com.emergencysupply.rations.updater.UpdateStatus

enum class SettingsSubTab {
    SETTINGS, ACTIVITY
}

@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    onSignInClick: () -> Unit,
    onSignOutClick: () -> Unit
) {
    val context = LocalContext.current
    val currentAccount by viewModel.currentAccount.collectAsState()
    val syncState by viewModel.syncState.collectAsState()
    val updateStatus by viewModel.updateStatus.collectAsState()
    val recentHistory by viewModel.recentHistory.collectAsState()
    val signInError by viewModel.signInError.collectAsState()
    val appSettings by viewModel.settings.collectAsState()
    val clipboardManager = LocalClipboardManager.current

    var selectedTab by rememberSaveable { mutableStateOf(SettingsSubTab.SETTINGS) }
    var selectedFilter by rememberSaveable { mutableStateOf("All") }
    var oauthDetailsExpanded by rememberSaveable { mutableStateOf(false) }

    val household = appSettings?.householdSize ?: 5
    val targetYears = appSettings?.targetYears ?: 2.0
    val lowStockPct = appSettings?.lowStockPct ?: 40
    val useSoonDays = appSettings?.useSoonDays ?: 90

    val filteredHistory = remember(recentHistory, selectedFilter) {
        if (selectedFilter == "All") {
            recentHistory
        } else {
            recentHistory.filter { log ->
                when (selectedFilter) {
                    "Restock" -> log.action.contains("restock", ignoreCase = true)
                    "Used" -> log.action.contains("use", ignoreCase = true)
                    "Added" -> log.action.contains("add", ignoreCase = true)
                    else -> log.action.contains(selectedFilter, ignoreCase = true)
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(4.dp))

        // Top Segmented Tab Switcher
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = SoftTileSurface,
            border = BorderStroke(1.dp, BorderSlate),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val isSettings = selectedTab == SettingsSubTab.SETTINGS
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSettings) InkBlack else Color.Transparent,
                    shadowElevation = if (isSettings) 2.dp else 0.dp,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { selectedTab = SettingsSubTab.SETTINGS }
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.padding(vertical = 10.dp)
                    ) {
                        Text(
                            text = "Settings",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSettings) Color.White else MediumSlate
                        )
                    }
                }

                val isActivity = selectedTab == SettingsSubTab.ACTIVITY
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isActivity) InkBlack else Color.Transparent,
                    shadowElevation = if (isActivity) 2.dp else 0.dp,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { selectedTab = SettingsSubTab.ACTIVITY }
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.padding(vertical = 10.dp)
                    ) {
                        Text(
                            text = "Activity (${recentHistory.size})",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isActivity) Color.White else MediumSlate
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        when (selectedTab) {
            SettingsSubTab.SETTINGS -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 1. Google Account & Sync Card
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(22.dp),
                            colors = CardDefaults.cardColors(containerColor = CardSurface),
                            border = BorderStroke(1.dp, BorderSlate),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(18.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = CobaltSoft,
                                            modifier = Modifier.size(34.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(Icons.Default.CloudSync, contentDescription = null, tint = CobaltBlue, modifier = Modifier.size(18.dp))
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text("Google Account & Sync", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MidnightSlate)
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (currentAccount != null) SuccessSoft else PeachSoft
                                    ) {
                                        Text(
                                            text = if (currentAccount != null) "AUTHORIZED" else "OFFLINE",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (currentAccount != null) SuccessGreen else OrangeAccent,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                if (currentAccount != null) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(shape = CircleShape, color = PeachSoft, modifier = Modifier.size(44.dp)) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(Icons.Default.AccountCircle, contentDescription = null, modifier = Modifier.size(32.dp), tint = OrangeAccent)
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(currentAccount?.displayName ?: "Family User", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MidnightSlate)
                                            Text(currentAccount?.email ?: "", fontSize = 12.sp, color = MediumSlate)
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))

                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = SoftTileSurface,
                                        border = BorderStroke(1.dp, BorderSlate),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 12.dp, vertical = 8.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text("Target Google Spreadsheet ID", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = LightSlate)
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text("1xVml4dExwAzDCQGRYrxgGwNQMurEqRTq1Z6wLlLSrM4", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = DarkSlate)
                                            }
                                            IconButton(
                                                onClick = {
                                                    clipboardManager.setText(AnnotatedString("1xVml4dExwAzDCQGRYrxgGwNQMurEqRTq1Z6wLlLSrM4"))
                                                    Toast.makeText(context, "Spreadsheet ID copied!", Toast.LENGTH_SHORT).show()
                                                },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy Spreadsheet ID", modifier = Modifier.size(16.dp), tint = EmeraldVibrant)
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    val statusText = when (syncState) {
                                        is SyncState.Idle -> "Status: Offline ready (idle)"
                                        is SyncState.Syncing -> "Status: Syncing two-way with Google Sheets..."
                                        is SyncState.Success -> "Status: Synced at ${(syncState as SyncState.Success).timestamp}"
                                        is SyncState.Error -> "Status: Sync error: ${(syncState as SyncState.Error).message}"
                                    }
                                    val statusColor = when (syncState) {
                                        is SyncState.Idle -> DarkSlate
                                        is SyncState.Syncing -> EmeraldVibrant
                                        is SyncState.Success -> SuccessGreen
                                        is SyncState.Error -> DangerRed
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (syncState is SyncState.Syncing) {
                                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = EmeraldVibrant)
                                            Spacer(modifier = Modifier.width(8.dp))
                                        } else if (syncState is SyncState.Success) {
                                            Icon(Icons.Default.CloudDone, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                        } else if (syncState is SyncState.Error) {
                                            Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = DangerRed, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                        }
                                        Text(statusText, fontSize = 12.sp, color = statusColor, fontWeight = FontWeight.Medium)
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))

                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Button(
                                            onClick = { viewModel.syncNow() },
                                            modifier = Modifier.weight(1.5f),
                                            enabled = syncState !is SyncState.Syncing,
                                            shape = RoundedCornerShape(12.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = InkBlack)
                                        ) {
                                            Icon(Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Sync Now", fontWeight = FontWeight.Bold)
                                        }

                                        OutlinedButton(
                                            onClick = onSignOutClick,
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(12.dp),
                                            border = BorderStroke(1.dp, DangerRed.copy(alpha = 0.4f))
                                        ) {
                                            Text("Sign Out", color = DangerRed, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                } else {
                                    Text(
                                        text = "Sign in with an authorized Google account with Editor access to sync with your household spreadsheet.",
                                        fontSize = 13.sp,
                                        color = MediumSlate
                                    )

                                    if (!signInError.isNullOrBlank()) {
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = DangerSoft,
                                            border = BorderStroke(1.dp, DangerRed.copy(alpha = 0.3f)),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
                                                Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = DangerRed, modifier = Modifier.size(18.dp))
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(signInError ?: "", fontSize = 11.sp, color = DangerRed, fontWeight = FontWeight.Medium)
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))

                                    Button(
                                        onClick = onSignInClick,
                                        modifier = Modifier.fillMaxWidth().height(48.dp),
                                        shape = RoundedCornerShape(14.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = InkBlack)
                                    ) {
                                        Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Sign In with Google", fontWeight = FontWeight.Bold)
                                    }
                                }

                                // Collapsed OAuth Setup Helper behind toggle
                                Spacer(modifier = Modifier.height(14.dp))
                                HorizontalDivider(color = BorderSlate)
                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { oauthDetailsExpanded = !oauthDetailsExpanded }
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (oauthDetailsExpanded) "OAuth Setup Details ▴" else "OAuth Setup Details ▾",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = CobaltBlue
                                    )
                                    Text(
                                        text = if (oauthDetailsExpanded) "Hide" else "Show",
                                        fontSize = 11.sp,
                                        color = LightSlate
                                    )
                                }

                                if (oauthDetailsExpanded) {
                                    Spacer(modifier = Modifier.height(10.dp))

                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .dashedBorder(width = 1.dp, color = BorderSlate, cornerRadius = 14.dp)
                                            .padding(10.dp)
                                    ) {
                                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .background(SoftTileSurface, RoundedCornerShape(10.dp))
                                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column {
                                                    Text("Package Name", fontSize = 10.sp, color = LightSlate)
                                                    Text("com.emergencysupply.rations", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = DarkSlate)
                                                }
                                                IconButton(onClick = {
                                                    clipboardManager.setText(AnnotatedString("com.emergencysupply.rations"))
                                                    Toast.makeText(context, "Package name copied!", Toast.LENGTH_SHORT).show()
                                                }) {
                                                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(16.dp), tint = EmeraldVibrant)
                                                }
                                            }

                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .background(SoftTileSurface, RoundedCornerShape(10.dp))
                                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text("Release SHA-1 Certificate", fontSize = 10.sp, color = LightSlate)
                                                    Text(
                                                        "4D:C5:68:1B:B8:7A:2F:77:AF:83:FD:BB:3C:7C:90:BF:60:A1:1F:6E",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = DarkSlate
                                                    )
                                                }
                                                IconButton(onClick = {
                                                    clipboardManager.setText(AnnotatedString("4D:C5:68:1B:B8:7A:2F:77:AF:83:FD:BB:3C:7C:90:BF:60:A1:1F:6E"))
                                                    Toast.makeText(context, "SHA-1 copied!", Toast.LENGTH_SHORT).show()
                                                }) {
                                                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(16.dp), tint = EmeraldVibrant)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 2. App Updates Card
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(22.dp),
                            colors = CardDefaults.cardColors(containerColor = CardSurface),
                            border = BorderStroke(1.dp, BorderSlate),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(18.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = TealSoft,
                                            modifier = Modifier.size(34.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(Icons.Default.SystemUpdate, contentDescription = null, tint = EmeraldVibrant, modifier = Modifier.size(18.dp))
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text("App Updates", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MidnightSlate)
                                    }

                                    Surface(shape = RoundedCornerShape(10.dp), color = TealSoft) {
                                        Text(
                                            text = "v${BuildConfig.VERSION_NAME}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = EmeraldVibrant,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Direct updates via GitHub Releases (Emergency-Supply).",
                                    fontSize = 12.sp,
                                    color = MediumSlate
                                )
                                Spacer(modifier = Modifier.height(12.dp))

                                when (val state = updateStatus) {
                                    is UpdateStatus.Downloading -> {
                                        Column(modifier = Modifier.fillMaxWidth()) {
                                            LinearProgressIndicator(progress = { state.progress }, modifier = Modifier.fillMaxWidth().height(8.dp), color = EmeraldVibrant)
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text("Downloading APK: ${(state.progress * 100).toInt()}%", fontSize = 11.sp, color = EmeraldVibrant, fontWeight = FontWeight.Bold)
                                        }
                                        Spacer(modifier = Modifier.height(10.dp))
                                    }
                                    is UpdateStatus.Available -> {
                                        val apkAsset = state.apkAsset
                                        Surface(
                                            shape = RoundedCornerShape(14.dp),
                                            color = TealSoft,
                                            border = BorderStroke(1.dp, EmeraldVibrant.copy(alpha = 0.3f)),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(modifier = Modifier.padding(12.dp)) {
                                                Text("Update Ready: ${state.release.tag_name}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = EmeraldVibrant)
                                                val notes = state.release.body
                                                if (!notes.isNullOrBlank()) {
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Text(notes, fontSize = 11.sp, color = DarkSlate)
                                                }
                                                Spacer(modifier = Modifier.height(10.dp))
                                                if (context is Activity) {
                                                    Button(
                                                        onClick = { viewModel.startDownloadUpdate(context, apkAsset) },
                                                        colors = ButtonDefaults.buttonColors(containerColor = InkBlack),
                                                        shape = RoundedCornerShape(10.dp)
                                                    ) {
                                                        Text("Download & Install Now", fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(10.dp))
                                    }
                                    is UpdateStatus.UpToDate -> {
                                        Text("App is running the latest release (v${BuildConfig.VERSION_NAME})", fontSize = 12.sp, color = SuccessGreen, fontWeight = FontWeight.SemiBold)
                                        Spacer(modifier = Modifier.height(10.dp))
                                    }
                                    is UpdateStatus.Error -> {
                                        Text("Update check: ${state.message}", fontSize = 12.sp, color = DangerRed)
                                        Spacer(modifier = Modifier.height(10.dp))
                                    }
                                    else -> {}
                                }

                                OutlinedButton(
                                    onClick = { viewModel.checkForUpdates(force = true) },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, BorderSlate)
                                ) {
                                    Icon(Icons.Default.SystemUpdate, contentDescription = null, modifier = Modifier.size(18.dp), tint = DarkSlate)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Check for Updates Now", fontWeight = FontWeight.SemiBold, color = DarkSlate)
                                }
                            }
                        }
                    }

                    // 3. Household Targets & Alerts Card
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(22.dp),
                            colors = CardDefaults.cardColors(containerColor = CardSurface),
                            border = BorderStroke(1.dp, BorderSlate),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(18.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = PeachSoft,
                                            modifier = Modifier.size(34.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(Icons.Default.People, contentDescription = null, tint = OrangeAccent, modifier = Modifier.size(18.dp))
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text("Household Targets & Alerts", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MidnightSlate)
                                    }

                                    Surface(shape = RoundedCornerShape(10.dp), color = PeachSoft) {
                                        Text(
                                            text = "$household Persons",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = OrangeAccent,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    HouseholdSettingRow(
                                        icon = Icons.Default.People,
                                        iconTint = OrangeAccent,
                                        iconBg = PeachSoft,
                                        label = "Household Size",
                                        value = "$household Persons"
                                    )
                                    HouseholdSettingRow(
                                        icon = Icons.Default.Restaurant,
                                        iconTint = EmeraldVibrant,
                                        iconBg = TealSoft,
                                        label = "Daily Target",
                                        value = "${household * 3} servings / day"
                                    )
                                    HouseholdSettingRow(
                                        icon = Icons.Default.TrackChanges,
                                        iconTint = CobaltBlue,
                                        iconBg = CobaltSoft,
                                        label = "Reserve Goal",
                                        value = "${targetYears} yrs"
                                    )
                                    HouseholdSettingRow(
                                        icon = Icons.Default.Warning,
                                        iconTint = WarningAmber,
                                        iconBg = WarningSoft,
                                        label = "Low Stock Alert",
                                        value = "<${lowStockPct}%"
                                    )
                                    HouseholdSettingRow(
                                        icon = Icons.Default.AccessTime,
                                        iconTint = DangerRed,
                                        iconBg = DangerSoft,
                                        label = "Expiry Alert",
                                        value = "<${useSoonDays} days"
                                    )
                                }
                            }
                        }
                    }

                    item { Spacer(modifier = Modifier.height(24.dp)) }
                }
            }

            SettingsSubTab.ACTIVITY -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = CobaltSoft,
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.History, contentDescription = null, tint = CobaltBlue, modifier = Modifier.size(18.dp))
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Recent Activity", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MidnightSlate)
                            }

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = SoftTileSurface,
                                border = BorderStroke(1.dp, BorderSlate)
                            ) {
                                Text(
                                    text = "${filteredHistory.size} events",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MediumSlate,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    // Quick Filter Chips (All, Restock, Used, Added)
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("All", "Restock", "Used", "Added").forEach { filter ->
                                val isSelected = selectedFilter == filter
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSelected) InkBlack else CardSurface,
                                    border = BorderStroke(1.dp, if (isSelected) InkBlack else BorderSlate),
                                    shadowElevation = if (isSelected) 2.dp else 0.dp,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { selectedFilter = filter }
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.padding(vertical = 8.dp)
                                    ) {
                                        Text(
                                            text = filter,
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) Color.White else DarkSlate
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (filteredHistory.isEmpty()) {
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .dashedBorder(width = 1.dp, color = BorderSlate, cornerRadius = 18.dp),
                                shape = RoundedCornerShape(18.dp),
                                colors = CardDefaults.cardColors(containerColor = CardSurface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                            ) {
                                Box(modifier = Modifier.padding(24.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                    Text(
                                        text = if (recentHistory.isEmpty()) "No pantry changes logged yet." else "No activity matching \"$selectedFilter\".",
                                        color = LightSlate,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    } else {
                        items(filteredHistory, key = { it.id }) { log ->
                            ReformedHistoryItemCard(log = log)
                        }
                    }

                    item { Spacer(modifier = Modifier.height(24.dp)) }
                }
            }
        }
    }
}

@Composable
private fun HouseholdSettingRow(
    icon: ImageVector,
    iconTint: Color,
    iconBg: Color,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SoftTileSurface, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = iconBg,
                modifier = Modifier.size(28.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(16.dp))
                }
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(label, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = DarkSlate)
        }
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = CardSurface,
            border = BorderStroke(1.dp, BorderSlate.copy(alpha = 0.8f))
        ) {
            Text(
                text = value,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MidnightSlate,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
fun ReformedHistoryItemCard(log: HistoryLogEntry) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        border = BorderStroke(1.dp, BorderSlate),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(log.itemName, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MidnightSlate)
                Spacer(modifier = Modifier.height(2.dp))
                Text("${log.action} • ${log.timestamp}", fontSize = 11.sp, color = LightSlate)
                if (log.notes.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(log.notes, fontSize = 11.sp, color = DarkSlate)
                }
            }

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (log.change >= 0) SuccessSoft else DangerSoft
            ) {
                Text(
                    text = (if (log.change > 0) "+%.0f" else "%.0f").format(log.change),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (log.change >= 0) SuccessGreen else DangerRed,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}
