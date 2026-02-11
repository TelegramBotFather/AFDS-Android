package com.afds.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.afds.app.AFDSApplication
import com.afds.app.R
import kotlinx.coroutines.launch

@Composable
fun SetupScreen(
    onSetupComplete: () -> Unit,
    onRefreshProfile: () -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val apiClient = AFDSApplication.instance.apiClient
    val sessionManager = AFDSApplication.instance.sessionManager

    var telegramUserId by remember { mutableStateOf("") }
    var channelId by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var isRefreshing by remember { mutableStateOf(false) }
    var isCheckingProfile by remember { mutableStateOf(true) }
    var step by remember { mutableIntStateOf(1) } // 1=user_id, 2=channel setup instructions, 3=channel_id, 4=wait
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Fetch profile on load to check if setup is already complete
    LaunchedEffect(Unit) {
        isCheckingProfile = true
        try {
            val token = sessionManager.getToken()
            if (token != null) {
                val profile = apiClient.getProfile(token)
                sessionManager.saveProfileData(profile.email, profile.userId, profile.channelId)
                val hasUserId = !profile.userId.isNullOrBlank()
                val hasChannelId = !profile.channelId.isNullOrBlank()
                if (hasUserId && hasChannelId) {
                    onSetupComplete()
                    return@LaunchedEffect
                } else if (hasUserId) {
                    step = 2
                }
            }
        } catch (_: Exception) { }
        isCheckingProfile = false
    }

    fun refreshProfile() {
        scope.launch {
            isRefreshing = true
            errorMessage = null
            try {
                val token = sessionManager.getToken() ?: return@launch
                val profile = apiClient.getProfile(token)
                sessionManager.saveProfileData(profile.email, profile.userId, profile.channelId)
                val hasUserId = !profile.userId.isNullOrBlank()
                val hasChannelId = !profile.channelId.isNullOrBlank()
                if (hasUserId && hasChannelId) {
                    onSetupComplete()
                } else if (hasUserId && !hasChannelId) {
                    step = 2
                    errorMessage = "Channel ID not set yet. Please complete the setup."
                } else if (!hasUserId && hasChannelId) {
                    step = 1
                    errorMessage = "Telegram User ID not set yet."
                } else {
                    step = 1
                    errorMessage = "Both User ID and Channel ID are missing."
                }
            } catch (e: Exception) {
                errorMessage = "Failed to refresh: ${e.message}"
            } finally {
                isRefreshing = false
            }
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            Icon(
                painter = painterResource(id = R.drawable.ic_logo),
                contentDescription = "AFDS Logo",
                modifier = Modifier.size(80.dp),
                tint = Color.Unspecified
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Account Setup",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                text = "Complete these steps to use AFDS",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Progress indicator
            LinearProgressIndicator(
                progress = { step / 4f },
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                text = "Step $step of 4",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Error
            errorMessage?.let { error ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = error,
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            when (step) {
                1 -> {
                    // Step 1: Set Telegram User ID
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text(
                                "Step 1: Telegram User ID",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Enter your Telegram User ID. You can get it from @userinfobot on Telegram.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            OutlinedTextField(
                                value = telegramUserId,
                                onValueChange = { telegramUserId = it },
                                label = { Text("Telegram User ID") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !isLoading
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = {
                                    if (telegramUserId.isBlank()) {
                                        errorMessage = "Please enter your Telegram User ID"
                                        return@Button
                                    }
                                    scope.launch {
                                        isLoading = true
                                        errorMessage = null
                                        try {
                                            val token = sessionManager.getToken() ?: return@launch
                                            apiClient.setTelegramId(token, telegramUserId)
                                            sessionManager.setUserId(telegramUserId)
                                            step = 2
                                        } catch (e: Exception) {
                                            errorMessage = e.message
                                        } finally {
                                            isLoading = false
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !isLoading && telegramUserId.isNotBlank()
                            ) {
                                if (isLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                                else { Text("Save & Continue") }
                            }
                        }
                    }
                }

                2 -> {
                    // Step 2: Channel Setup Instructions
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text(
                                "Step 2: Setup Your Telegram Channel",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            // Instructions
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("📋 Follow these steps:", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(12.dp))

                                    Text("1️⃣ Create a Telegram channel (or use existing one)", style = MaterialTheme.typography.bodyMedium)
                                    Spacer(modifier = Modifier.height(8.dp))

                                    Text("2️⃣ Add @LinkerXHelperbot to your channel as admin with full permissions", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(8.dp))

                                    Text("3️⃣ Run the /setup command in your channel", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(8.dp))

                                    Text("4️⃣ Get your Channel ID (it looks like -1001234567890)", style = MaterialTheme.typography.bodyMedium)
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("⚠️ Important", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    Text(
                                        "The bot @LinkerXHelperbot MUST be an admin in your channel with full permissions, and /setup must be run BEFORE entering the channel ID.",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { step = 3 },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("I've done this, continue →")
                            }
                        }
                    }
                }

                3 -> {
                    // Step 3: Enter Channel ID
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text(
                                "Step 3: Enter Channel ID",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Enter your Telegram Channel ID where files will be delivered.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            OutlinedTextField(
                                value = channelId,
                                onValueChange = { channelId = it },
                                label = { Text("Channel ID") },
                                placeholder = { Text("-1001234567890") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !isLoading
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = {
                                    if (channelId.isBlank()) {
                                        errorMessage = "Please enter your Channel ID"
                                        return@Button
                                    }
                                    scope.launch {
                                        isLoading = true
                                        errorMessage = null
                                        try {
                                            val token = sessionManager.getToken() ?: return@launch
                                            apiClient.setChannelId(token, channelId)
                                            sessionManager.setChannelId(channelId)
                                            step = 4
                                        } catch (e: Exception) {
                                            errorMessage = e.message
                                        } finally {
                                            isLoading = false
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !isLoading && channelId.isNotBlank()
                            ) {
                                if (isLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                                else { Text("Save Channel ID") }
                            }
                        }
                    }
                }

                4 -> {
                    // Step 4: Wait & Continue
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "Setup Complete! 🎉",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                "Please wait about 2 minutes for the bot to sync with your channel, then tap the button below.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(24.dp))

                            Button(
                                onClick = {
                                    scope.launch {
                                        isRefreshing = true
                                        errorMessage = null
                                        try {
                                            val token = sessionManager.getToken() ?: return@launch
                                            val profile = apiClient.getProfile(token)
                                            sessionManager.saveProfileData(profile.email, profile.userId, profile.channelId)
                                            onSetupComplete()
                                        } catch (e: Exception) {
                                            errorMessage = e.message
                                        } finally {
                                            isRefreshing = false
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !isRefreshing
                            ) {
                                if (isRefreshing) {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Refreshing...")
                                } else {
                                    Icon(Icons.Default.Refresh, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("I've waited, let's go!")
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Refresh Profile button
            OutlinedButton(
                onClick = { refreshProfile() },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isRefreshing && !isLoading
            ) {
                if (isRefreshing) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Checking profile...")
                } else {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Refresh Profile")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Logout option
            TextButton(
                onClick = {
                    scope.launch {
                        sessionManager.clearSession()
                        onLogout()
                    }
                }
            ) {
                Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Logout", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}