package com.afds.app.util

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Environment
import android.widget.Toast

/**
 * Supported downloader apps.
 * "default" = Android's built-in DownloadManager
 * "1dm" = 1DM (idm.internet.download.manager)
 * "1dm_plus" = 1DM+ (idm.internet.download.manager.plus)
 * "1dm_lite" = 1DM Lite (idm.internet.download.manager.lite)
 */
object DownloadHelper {

    enum class Downloader(
        val key: String,
        val displayName: String,
        val packageName: String?
    ) {
        DEFAULT("default", "Built-in (Default)", null),
        IDM("1dm", "1DM", "idm.internet.download.manager"),
        IDM_PLUS("1dm_plus", "1DM+", "idm.internet.download.manager.plus"),
        IDM_LITE("1dm_lite", "1DM Lite", "idm.internet.download.manager.lite");

        companion object {
            fun fromKey(key: String): Downloader =
                entries.find { it.key == key } ?: DEFAULT
        }
    }

    fun isAppInstalled(context: Context, packageName: String): Boolean {
        return try {
            context.packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
            true
        } catch (_: Exception) {
            false
        }
    }

    fun getInstalledDownloaders(context: Context): List<Downloader> {
        val list = mutableListOf(Downloader.DEFAULT)
        Downloader.entries.filter { it.packageName != null }.forEach { dl ->
            if (isAppInstalled(context, dl.packageName!!)) {
                list.add(dl)
            }
        }
        return list
    }

    /**
     * Start download using the selected downloader app.
     * For 1DM apps, sends an ACTION_VIEW intent with the URL.
     * For default, uses Android DownloadManager.
     */
    fun download(context: Context, url: String, fileName: String, downloaderKey: String) {
        val downloader = Downloader.fromKey(downloaderKey)

        if (downloader == Downloader.DEFAULT || downloader.packageName == null) {
            // Use built-in DownloadManager
            downloadWithManager(context, url, fileName)
            return
        }

        // Check if the app is installed
        if (!isAppInstalled(context, downloader.packageName)) {
            Toast.makeText(context, "${downloader.displayName} is not installed. Using default downloader.", Toast.LENGTH_SHORT).show()
            downloadWithManager(context, url, fileName)
            return
        }

        // Open URL in 1DM app
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                setPackage(downloader.packageName)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Toast.makeText(context, "Opening in ${downloader.displayName}...", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to open ${downloader.displayName}: ${e.message}", Toast.LENGTH_SHORT).show()
            downloadWithManager(context, url, fileName)
        }
    }

    private fun downloadWithManager(context: Context, url: String, fileName: String) {
        try {
            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val request = DownloadManager.Request(Uri.parse(url))
                .setTitle(fileName)
                .setDescription("Downloading from AFDS")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
            downloadManager.enqueue(request)
            Toast.makeText(context, "Download started!", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Download failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}