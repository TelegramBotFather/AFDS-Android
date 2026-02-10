package com.afds.app.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Expected JSON at: https://afds.apks.zindex.eu.org/com.afds.app/app.json
 *
 * {
 *   "version": "1.0.0",
 *   "version_code": 1,
 *   "changelog": "Initial release",
 *   "force_update": false
 * }
 *
 * When you release 1.1.0, update this to:
 * {
 *   "version": "1.1.0",
 *   "version_code": 2,
 *   "changelog": "Bug fixes and new features",
 *   "force_update": false
 * }
 *
 * APK download URL is auto-constructed:
 * https://afds.apks.zindex.eu.org/com.afds.app/{version}.apk
 */
@Serializable
data class AppUpdateInfo(
    val version: String = "1.0.0",
    @SerialName("version_code") val versionCode: Int = 1,
    val changelog: String? = null,
    @SerialName("force_update") val forceUpdate: Boolean = false
)