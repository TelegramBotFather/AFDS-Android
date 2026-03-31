package com.afds.app.ui.screens

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.webkit.ConsoleMessage
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.ComponentActivity
import com.afds.app.AFDSApplication
import com.afds.app.data.remote.ApiException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TelegramSetupActivity : ComponentActivity() {

    private fun mimeTypeFor(path: String): String = when {
        path.endsWith(".html") -> "text/html"
        path.endsWith(".js")   -> "application/javascript"
        path.endsWith(".css")  -> "text/css"
        path.endsWith(".png")  -> "image/png"
        path.endsWith(".svg")  -> "image/svg+xml"
        else                   -> "application/octet-stream"
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val webView = WebView(this)
        setContentView(webView)

        val apiClient = AFDSApplication.instance.apiClient
        val sessionManager = AFDSApplication.instance.sessionManager

        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true

        val bridge = object : Any() {
            @JavascriptInterface
            fun onChannelCreated(channelId: String, telegramUserId: String) {
                CoroutineScope(Dispatchers.Main).launch {
                    try {
                        val token = sessionManager.getToken() ?: run {
                            Toast.makeText(this@TelegramSetupActivity, "Session expired. Please log in again.", Toast.LENGTH_SHORT).show()
                            setResult(Activity.RESULT_CANCELED)
                            finish()
                            return@launch
                        }
                        sessionManager.setChannelId(channelId)
                        if (telegramUserId.isNotEmpty()) {
                            sessionManager.setUserId(telegramUserId)
                        }
                        apiClient.setChannelId(token, channelId)
                        if (telegramUserId.isNotEmpty()) {
                            try {
                                apiClient.setTelegramId(token, telegramUserId)
                            } catch (_: ApiException) {
                                try { apiClient.updateTelegramId(token, telegramUserId) } catch (_: Exception) {}
                            }
                        }
                        Toast.makeText(this@TelegramSetupActivity, "Channel ready: $channelId", Toast.LENGTH_SHORT).show()
                        setResult(Activity.RESULT_OK)
                        finish()
                    } catch (e: ApiException) {
                        if (e.statusCode == 401) sessionManager.clearSession()
                        Toast.makeText(this@TelegramSetupActivity, "Failed to save: ${e.message}", Toast.LENGTH_LONG).show()
                    } catch (e: Exception) {
                        Toast.makeText(this@TelegramSetupActivity, "Failed to save: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }

            @JavascriptInterface
            fun onSetupComplete() {
                CoroutineScope(Dispatchers.Main).launch {
                    setResult(Activity.RESULT_OK)
                    finish()
                }
            }

            @JavascriptInterface
            fun onError(message: String) {
                CoroutineScope(Dispatchers.Main).launch {
                    Toast.makeText(this@TelegramSetupActivity, "Setup error: $message", Toast.LENGTH_LONG).show()
                    setResult(Activity.RESULT_CANCELED)
                    finish()
                }
            }
        }

        webView.addJavascriptInterface(bridge, "AndroidBridge")

        webView.webChromeClient = object : WebChromeClient() {
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

        webView.webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(
                view: WebView,
                request: WebResourceRequest
            ): WebResourceResponse? {
                val url = request.url
                if (url.host != "appassets.androidplatform.net") return null
                val path = url.path ?: return null
                // Strip /assets/ prefix → maps to assets/ directory
                val assetPath = path.removePrefix("/assets/")
                return try {
                    val stream = assets.open(assetPath)
                    WebResourceResponse(mimeTypeFor(assetPath), "UTF-8", stream)
                } catch (e: Exception) {
                    Log.e("TGSetup", "Asset not found: $assetPath")
                    null
                }
            }

            override fun onReceivedError(
                view: WebView,
                request: WebResourceRequest,
                error: WebResourceError
            ) {
                if (request.isForMainFrame) {
                    Log.e("TGSetup", "WebView error: ${error.errorCode} — ${error.description}")
                    Toast.makeText(
                        this@TelegramSetupActivity,
                        "Could not load setup page.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }

        // Load HTML inline — avoids a real HTTP request to appassets.androidplatform.net
        // which fails in release builds. Sub-resources (JS) are still served via shouldInterceptRequest.
        val html = assets.open("tg-webapp/afds-setup.html").bufferedReader().readText()
        webView.loadDataWithBaseURL(
            "https://appassets.androidplatform.net/assets/tg-webapp/",
            html,
            "text/html",
            "UTF-8",
            null
        )
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        setResult(Activity.RESULT_CANCELED)
        @Suppress("DEPRECATION")
        super.onBackPressed()
    }
}
