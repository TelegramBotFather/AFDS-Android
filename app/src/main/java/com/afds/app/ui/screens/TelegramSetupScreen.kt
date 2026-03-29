package com.afds.app.ui.screens

import android.annotation.SuppressLint
import android.util.Log
import android.webkit.ConsoleMessage
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.webkit.WebViewAssetLoader
import com.afds.app.AFDSApplication
import com.afds.app.data.remote.ApiException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TelegramSetupScreen(
    onSetupComplete: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val apiClient = AFDSApplication.instance.apiClient
    val sessionManager = AFDSApplication.instance.sessionManager
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isProcessing by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Auto Telegram Setup") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    WebView(ctx).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true

                        // Serve assets from a trusted https:// origin to avoid ES module CORS issues.
                        // Path "/" maps the full URL path to assets root, so
                        // appassets.androidplatform.net/tg-webapp/afds-setup.html → assets/tg-webapp/afds-setup.html
                        val assetLoader = WebViewAssetLoader.Builder()
                            .addPathHandler("/", WebViewAssetLoader.AssetsPathHandler(ctx))
                            .build()

                        val bridge = object : Any() {
                            @JavascriptInterface
                            fun onChannelCreated(channelId: String, telegramUserId: String) {
                                CoroutineScope(Dispatchers.Main).launch {
                                    isProcessing = true
                                    statusMessage = "Saving to AFDS..."
                                    try {
                                        val token = sessionManager.getToken() ?: run {
                                            statusMessage = "Session expired. Please log in again."
                                            isProcessing = false
                                            return@launch
                                        }
                                        // Save channel ID and Telegram user ID locally and to API
                                        sessionManager.setChannelId(channelId)
                                        if (telegramUserId.isNotEmpty()) {
                                            sessionManager.setUserId(telegramUserId)
                                        }
                                        apiClient.setChannelId(token, channelId)
                                        // Save Telegram user ID to API (set or update)
                                        if (telegramUserId.isNotEmpty()) {
                                            try {
                                                apiClient.setTelegramId(token, telegramUserId)
                                            } catch (_: ApiException) {
                                                // Already set — try update instead
                                                try { apiClient.updateTelegramId(token, telegramUserId) } catch (_: Exception) {}
                                            }
                                        }
                                        statusMessage = "Setup complete!"
                                        Toast.makeText(ctx, "Channel ready: $channelId", Toast.LENGTH_SHORT).show()
                                        onSetupComplete()
                                    } catch (e: ApiException) {
                                        if (e.statusCode == 401) sessionManager.clearSession()
                                        statusMessage = "Failed to save: ${e.message}"
                                        isProcessing = false
                                    } catch (e: Exception) {
                                        statusMessage = "Failed to save: ${e.message}"
                                        isProcessing = false
                                    }
                                }
                            }

                            @JavascriptInterface
                            fun onSetupComplete() {
                                CoroutineScope(Dispatchers.Main).launch {
                                    onSetupComplete()
                                }
                            }

                            @JavascriptInterface
                            fun onError(message: String) {
                                CoroutineScope(Dispatchers.Main).launch {
                                    Toast.makeText(ctx, "Setup error: $message", Toast.LENGTH_LONG).show()
                                    onBack()
                                }
                            }
                        }

                        addJavascriptInterface(bridge, "AndroidBridge")

                        webChromeClient = object : WebChromeClient() {
                            override fun onConsoleMessage(msg: ConsoleMessage): Boolean {
                                val level = when (msg.messageLevel()) {
                                    ConsoleMessage.MessageLevel.ERROR -> Log.ERROR
                                    ConsoleMessage.MessageLevel.WARNING -> Log.WARN
                                    else -> Log.DEBUG
                                }
                                Log.println(level, "TGSetup-JS", msg.message())
                                return true
                            }
                        }

                        webViewClient = object : WebViewClient() {
                            override fun shouldInterceptRequest(
                                view: WebView,
                                request: WebResourceRequest
                            ): WebResourceResponse? {
                                return assetLoader.shouldInterceptRequest(request.url)
                            }

                            override fun shouldOverrideUrlLoading(
                                view: WebView,
                                request: WebResourceRequest
                            ): Boolean {
                                return false
                            }
                        }

                        loadUrl("https://appassets.androidplatform.net/tg-webapp/afds-setup.html")
                    }
                }
            )

            // Processing overlay
            if (isProcessing) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = statusMessage ?: "Processing...",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }
    }
}
