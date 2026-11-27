package com.emergencysupply.rations.ui.screens

import android.app.Activity
import android.widget.Toast
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
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emergencysupply.rations.data.local.entity.HistoryLogEntry
import com.emergencysupply.rations.data.repository.SyncState
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
import com.emergencysupply.rations.ui.theme.WarningAmber
import com.emergencysupply.rations.ui.theme.WarningSoft
import com.emergencysupply.rations.updater.UpdateStatus

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
    val clipboardManager = LocalClipboardManager.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Google Account & Cloud Sync Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardSurface),
                border = BorderStroke(1.dp, BorderSlate),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Google Account & Sheets Sync",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MidnightSlate
                        )
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (currentAccount != null) SuccessSoft else Color(0xFFF1F5F9),
                            border = BorderStroke(1.dp, if (currentAccount != null) SuccessGreen.copy(alpha = 0.3f) else BorderSlate)
                        ) {
                            Text(
                                text = if (currentAccount != null) "AUTHORIZED" else "DISCONNECTED",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (currentAccount != null) SuccessGreen else MediumSlate,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (currentAccount != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = TealSoft,
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.AccountCircle,
                                        contentDescription = null,
                                        modifier = Modifier.size(32.dp),
                                        tint = TealPrimary
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = currentAccount?.displayName ?: "Family User",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MidnightSlate
                                )
                                Text(
                                    text = currentAccount?.email ?: "",
                                    fontSize = 12.sp,
                                    color = MediumSlate
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Target Spreadsheet display
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFF8FAFC),
                            border = BorderStroke(1.dp, BorderSlate),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(text = "Target Google Spreadsheet ID", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = LightSlate)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "1xVml4dExwAzDCQGRYrxgGwNQMurEqRTq1Z6wLlLSrM4",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = DarkSlate
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Sync State Banner
                        val statusText = when (syncState) {
                            is SyncState.Idle -> "Status: Offline ready (idle)"
                            is SyncState.Syncing -> "Status: Syncing two-way with Google Sheets..."
                            is SyncState.Success -> "Status: Synced at ${(syncState as SyncState.Success).timestamp}"
                            is SyncState.Error -> "Status: Sync error: ${(syncState as SyncState.Error).message}"
                        }

                        val statusColor = when (syncState) {
                            is SyncState.Idle -> DarkSlate
                            is SyncState.Syncing -> TealPrimary
                            is SyncState.Success -> SuccessGreen
                            is SyncState.Error -> DangerRed
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (syncState is SyncState.Syncing) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = TealPrimary)
                                Spacer(modifier = Modifier.width(8.dp))
                            } else if (syncState is SyncState.Success) {
                                Icon(imageVector = Icons.Default.CloudDone, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                            } else if (syncState is SyncState.Error) {
                                Icon(imageVector = Icons.Default.ErrorOutline, contentDescription = null, tint = DangerRed, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Text(text = statusText, fontSize = 12.sp, color = statusColor, fontWeight = FontWeight.Medium)
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = { viewModel.syncNow() },
                                modifier = Modifier.weight(1.5f),
                                enabled = syncState !is SyncState.Syncing,
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
                            ) {
                                Icon(imageVector = Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Sync Now", fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = onSignOutClick,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, DangerRed.copy(alpha = 0.4f))
                            ) {
                                Text("Sign Out", color = DangerRed, fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        Text(
                            text = "Sign in with an authorized Google account that has Editor access on the family spreadsheet.",
                            fontSize = 13.sp,
                            color = MediumSlate
                        )

                        if (!signInError.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = DangerSoft,
                                border = BorderStroke(1.dp, DangerRed.copy(alpha = 0.3f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ErrorOutline,
                                        contentDescription = null,
                                        tint = DangerRed,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = signInError ?: "",
                                        fontSize = 11.sp,
                                        color = DangerRed,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = onSignInClick,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
                        ) {
                            Icon(imageVector = Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Sign In with Google", fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        Divider(color = BorderSlate)
                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "⚙️ Google Cloud OAuth Setup Helper",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MidnightSlate
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Ensure your package name and SHA-1 certificate are added to Google Cloud Console:",
                            fontSize = 11.sp,
                            color = LightSlate
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        // Package Name row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFF8FAFC), RoundedCornerShape(8.dp))
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
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(16.dp), tint = TealPrimary)
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // SHA-1 Fingerprint row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFF8FAFC), RoundedCornerShape(8.dp))
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
                                Toast.makeText(context, "SHA-1 copied to clipboard!", Toast.LENGTH_SHORT).show()
                            }) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(16.dp), tint = TealPrimary)
                            }
                        }
                    }
                }
            }
        }

        // 2. In-App GitHub Releases Self-Updater Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardSurface),
                border = BorderStroke(1.dp, BorderSlate),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "In-App App Updates",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MidnightSlate
                        )
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = TealSoft,
                            border = BorderStroke(1.dp, TealPrimary.copy(alpha = 0.3f))
                        ) {
                            Text(
                                text = "v1.0.3",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = TealPrimary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Connects directly to GitHub Releases (github.com/fawadmubashir-loonar/Emergency-Supply). Automatically downloads and installs updates.",
                        fontSize = 12.sp,
                        color = MediumSlate
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    when (val state = updateStatus) {
                        is UpdateStatus.Downloading -> {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                LinearProgressIndicator(
                                    progress = { state.progress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp),
                                    color = TealPrimary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Downloading APK: ${(state.progress * 100).toInt()}%",
                                    fontSize = 11.sp,
                                    color = TealPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                        }
                        is UpdateStatus.Available -> {
                            val apkAsset = state.apkAsset
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = TealSoft,
                                border = BorderStroke(1.dp, TealPrimary.copy(alpha = 0.3f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = "Update Ready: ${state.release.tag_name}",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TealPrimary
                                    )
                                    val notes = state.release.body
                                    if (!notes.isNullOrBlank()) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(text = notes, fontSize = 11.sp, color = DarkSlate)
                                    }
                                    Spacer(modifier = Modifier.height(10.dp))
                                    if (context is Activity) {
                                        Button(
                                            onClick = { viewModel.startDownloadUpdate(context, apkAsset) },
                                            colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text("Download & Install Now", fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                        }
                        is UpdateStatus.UpToDate -> {
                            Text(
                                text = "App is running the latest release (v1.0.3)",
                                fontSize = 12.sp,
                                color = SuccessGreen,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                        }
                        is UpdateStatus.Error -> {
                            Text(
                                text = "Update check: ${state.message}",
                                fontSize = 12.sp,
                                color = DangerRed
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                        }
                        else -> {}
                    }

                    OutlinedButton(
                        onClick = { viewModel.checkForUpdates(force = true) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, BorderSlate)
                    ) {
                        Icon(imageVector = Icons.Default.SystemUpdate, contentDescription = null, modifier = Modifier.size(18.dp), tint = DarkSlate)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Check for Updates Now", fontWeight = FontWeight.SemiBold, color = DarkSlate)
                    }
                }
            }
        }

        // 3. Household Rations Planning Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardSurface),
                border = BorderStroke(1.dp, BorderSlate),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Household Planning Rules",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MidnightSlate
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.People, contentDescription = null, tint = TealPrimary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Household Members", fontSize = 13.sp, color = DarkSlate)
                        }
                        Text("5 Persons", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MidnightSlate)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Daily Target Servings", fontSize = 13.sp, color = DarkSlate)
                        Text("15 servings / day (3 / person)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MidnightSlate)
                    }
                }
            }
        }

        // 4. Recent History Audit Log Card
        item {
            Text(
                text = "Recent Activity Log",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MidnightSlate
            )
        }

        if (recentHistory.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = CardSurface),
                    border = BorderStroke(1.dp, BorderSlate)
                ) {
                    Box(modifier = Modifier.padding(20.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(text = "No recent pantry stock changes logged yet.", color = LightSlate, fontSize = 12.sp)
                    }
                }
            }
        } else {
            items(recentHistory, key = { it.id }) { log ->
                ReformedHistoryItemCard(log = log)
            }
        }

        item { Spacer(modifier = Modifier.height(36.dp)) }
    }
}

@Composable
fun ReformedHistoryItemCard(log: HistoryLogEntry) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
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
                    text = log.itemName,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MidnightSlate
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${log.action} • ${log.timestamp}",
                    fontSize = 11.sp,
                    color = LightSlate
                )
                if (log.notes.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = log.notes,
                        fontSize = 11.sp,
                        color = DarkSlate
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
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
