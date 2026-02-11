package com.afds.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.rememberNavController
import com.afds.app.data.model.AppUpdateInfo
import com.afds.app.ui.navigation.AFDSNavHost
import com.afds.app.ui.theme.AFDSTheme
import com.afds.app.util.NetworkObserver
import com.afds.app.util.UpdateManager

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AFDSTheme {
                val context = LocalContext.current
                val isOnline by NetworkObserver.observe(context)
                    .collectAsState(initial = NetworkObserver.isOnline(context))

                // Global update check — runs on app launch regardless of login state
                var updateInfo by remember { mutableStateOf<AppUpdateInfo?>(null) }
                var showUpdateDialog by remember { mutableStateOf(false) }
                var isDownloadingUpdate by remember { mutableStateOf(false) }
                val apiClient = AFDSApplication.instance.apiClient

                LaunchedEffect(isOnline) {
                    if (isOnline) {
                        try {
                            val currentVersionCode = UpdateManager.getVersionCode(context)
                            val remoteInfo = apiClient.checkForUpdate()
                            if (remoteInfo.versionCode > currentVersionCode) {
                                updateInfo = remoteInfo
                                showUpdateDialog = true
                            }
                        } catch (_: Exception) { }
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (isOnline) {
                        val navController = rememberNavController()
                        AFDSNavHost(navController = navController)
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Default.WifiOff,
                                contentDescription = null,
                                modifier = Modifier.size(80.dp),
                                tint = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Text(
                                text = "No Internet Available",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Please check your internet connection and try again.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                // Global Update Dialog — shows even on login screen
                if (showUpdateDialog && updateInfo != null) {
                    val update = updateInfo!!
                    AlertDialog(
                        onDismissRequest = {
                            if (!update.forceUpdate) showUpdateDialog = false
                        },
                        icon = {
                            Icon(
                                Icons.Default.SystemUpdate,
                                contentDescription = null,
                                modifier = Modifier.size(40.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        title = { Text("Update Available") },
                        text = {
                            Column {
                                Text(
                                    "Version ${update.version} is available!",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                if (update.changelog != null && update.changelog.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        update.changelog,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (update.forceUpdate) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        "This update is required.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.error,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                val manualUrl = apiClient.getApkDownloadUrl(update.version)
                                TextButton(onClick = {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(manualUrl)))
                                }) {
                                    Text("📥 Manual download: ${update.version}.apk", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    isDownloadingUpdate = true
                                    UpdateManager.downloadAndInstallUpdate(
                                        context,
                                        apiClient.getApkDownloadUrl(update.version),
                                        update.version
                                    )
                                },
                                enabled = !isDownloadingUpdate
                            ) {
                                if (isDownloadingUpdate) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Downloading...")
                                } else {
                                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Update Now")
                                }
                            }
                        },
                        dismissButton = {
                            if (!update.forceUpdate) {
                                TextButton(onClick = { showUpdateDialog = false }) {
                                    Text("Later")
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}