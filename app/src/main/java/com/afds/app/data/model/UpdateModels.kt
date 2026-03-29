package com.afds.app.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AppUpdateInfo(
    val version: String = "1.0.0",
    @SerialName("version_code") val versionCode: Int = 1,
    val changelog: String? = null,
    @SerialName("force_update") val forceUpdate: Boolean = false,
    val downloadUrl: String? = null
)

@Serializable
data class GitHubAsset(
    val name: String,
    @SerialName("browser_download_url") val downloadUrl: String,
    @SerialName("content_type") val contentType: String = ""
)

@Serializable
data class GitHubRelease(
    @SerialName("tag_name") val tagName: String,
    val name: String? = null,
    val body: String? = null,
    val prerelease: Boolean = false,
    val draft: Boolean = false,
    val assets: List<GitHubAsset> = emptyList()
)
