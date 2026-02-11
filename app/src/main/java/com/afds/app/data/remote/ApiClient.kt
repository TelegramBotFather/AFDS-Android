package com.afds.app.data.remote

import com.afds.app.data.model.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.statement.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import android.util.Log
import kotlinx.serialization.json.Json
import java.util.concurrent.TimeUnit

class ApiClient {

    companion object {
        const val BASE_URL = "https://tga-hd.api.hashhackers.com"
        const val GOOGLE_CLIENT_ID = "58094879805-2k4u6f17pfn7fm68kg31fcr4ah7slm0d.apps.googleusercontent.com"
        const val APK_BASE_URL = "https://afds.apks.zindex.eu.org/com.afds.app"
    }

    val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val client = HttpClient(OkHttp) {
        engine {
            config {
                connectTimeout(60, TimeUnit.SECONDS)
                readTimeout(60, TimeUnit.SECONDS)
                writeTimeout(60, TimeUnit.SECONDS)
            }
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 60_000
            connectTimeoutMillis = 60_000
            socketTimeoutMillis = 60_000
        }
        install(ContentNegotiation) {
            json(json)
        }
        install(Logging) {
            level = LogLevel.ALL
            logger = object : Logger {
                override fun log(message: String) {
                    Log.d("AFDS_HTTP", message)
                }
            }
        }
    }

    // ---- Auth ----

    suspend fun requestLoginOtp(email: String): OtpResponse {
        val response = client.post("$BASE_URL/request-login-otp") {
            contentType(ContentType.Application.Json)
            setBody(LoginRequest(email))
        }
        return response.body()
    }

    suspend fun verifyLoginOtp(email: String, code: String): OtpResponse {
        val response = client.post("$BASE_URL/verify-login-otp") {
            contentType(ContentType.Application.Json)
            setBody(OtpRequest(email, code))
        }
        if (response.status == HttpStatusCode.OK) {
            return response.body()
        }
        val error: OtpResponse = response.body()
        throw ApiException(error.error ?: "Verification failed", response.status.value)
    }

    suspend fun requestLoginOtpEmail(email: String): OtpResponse {
        val response = client.post("$BASE_URL/request-login-otp-email") {
            contentType(ContentType.Application.Json)
            setBody(LoginRequest(email))
        }
        return response.body()
    }

    suspend fun googleAuth(credential: String): AuthResponse {
        val response = client.post("$BASE_URL/auth/google") {
            contentType(ContentType.Application.Json)
            setBody(GoogleAuthRequest(credential))
        }
        if (response.status == HttpStatusCode.OK) {
            return response.body()
        }
        if (response.status == HttpStatusCode.NotFound) {
            throw ApiException("No account exists with this Google account. Please sign up first.", 404)
        }
        val error: AuthResponse = response.body()
        throw ApiException(error.error ?: "Google sign-in failed", response.status.value)
    }

    // ---- Search & Browse ----

    suspend fun searchFiles(category: String, query: String, page: Int = 1): SearchResponse {
        Log.d("AFDS_API", "searchFiles: category=$category, query=$query, page=$page")
        val response = client.get("$BASE_URL/$category/search") {
            parameter("q", query)
            parameter("page", page)
        }
        Log.d("AFDS_API", "searchFiles response status: ${response.status}")
        if (response.status == HttpStatusCode.Unauthorized) {
            throw ApiException("Session expired", 401)
        }
        val bodyText = response.bodyAsText()
        Log.d("AFDS_API", "searchFiles raw response (first 500): ${bodyText.take(500)}")
        val result = json.decodeFromString<SearchResponse>(bodyText)
        if (result.files.isNotEmpty()) {
            val f = result.files[0]
            Log.d("AFDS_API", "First file: id=${f.id}, fileId=${f.fileId}, effectiveId=${f.effectiveId}, name=${f.fileName}")
        }
        return result
    }

    suspend fun browseFiles(category: String, page: Int = 1): SearchResponse {
        Log.d("AFDS_API", "browseFiles: category=$category, page=$page")
        val response = client.get("$BASE_URL/$category/index") {
            parameter("page", page)
        }
        Log.d("AFDS_API", "browseFiles response status: ${response.status}")
        if (response.status == HttpStatusCode.Unauthorized) {
            throw ApiException("Session expired", 401)
        }
        val bodyText = response.bodyAsText()
        Log.d("AFDS_API", "browseFiles raw response (first 500): ${bodyText.take(500)}")
        return json.decodeFromString<SearchResponse>(bodyText)
    }

    suspend fun getFileDetails(category: String, fileId: String): FileDetails {
        Log.d("AFDS_API", "getFileDetails: category=$category, fileId=$fileId")
        val response = client.get("$BASE_URL/$category/id") {
            parameter("id", fileId)
        }
        if (response.status == HttpStatusCode.Unauthorized) {
            throw ApiException("Session expired", 401)
        }
        if (!response.status.isSuccess()) {
            throw ApiException("Failed to fetch details", response.status.value)
        }
        val bodyText = response.bodyAsText()
        Log.d("AFDS_API", "getFileDetails raw response (first 500): ${bodyText.take(500)}")
        return json.decodeFromString<FileDetails>(bodyText)
    }

    suspend fun generateDownloadLink(tableName: String, fileId: String): GenLinkResponse {
        val response = client.get("$BASE_URL/genLink") {
            parameter("type", tableName)
            parameter("id", fileId)
        }
        if (response.status == HttpStatusCode.Unauthorized) {
            throw ApiException("Session expired", 401)
        }
        return response.body()
    }

    // ---- Profile ----

    suspend fun getProfile(token: String): ProfileResponse {
        val response = client.get("$BASE_URL/profile") {
            header("Authorization", "Bearer $token")
        }
        if (response.status == HttpStatusCode.Unauthorized) {
            throw ApiException("Session expired", 401)
        }
        return response.body()
    }

    suspend fun changePassword(token: String, currentPassword: String, newPassword: String): MessageResponse {
        val response = client.post("$BASE_URL/profile/change-password") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer $token")
            setBody(ChangePasswordRequest(currentPassword, newPassword))
        }
        if (response.status == HttpStatusCode.Unauthorized) {
            throw ApiException("Session expired", 401)
        }
        if (!response.status.isSuccess()) {
            val error: MessageResponse = response.body()
            throw ApiException(error.error ?: "Failed to change password", response.status.value)
        }
        return response.body()
    }

    suspend fun changeEmail(token: String, newEmail: String, currentPassword: String): MessageResponse {
        val response = client.post("$BASE_URL/profile/change-email") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer $token")
            setBody(ChangeEmailRequest(newEmail, currentPassword))
        }
        if (response.status == HttpStatusCode.Unauthorized) {
            throw ApiException("Session expired", 401)
        }
        if (!response.status.isSuccess()) {
            val error: MessageResponse = response.body()
            throw ApiException(error.error ?: "Failed to change email", response.status.value)
        }
        return response.body()
    }

    suspend fun setTelegramId(token: String, userId: String): MessageResponse {
        val response = client.post("$BASE_URL/profile/set-user-id") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer $token")
            setBody(SetUserIdRequest(userId))
        }
        if (response.status == HttpStatusCode.Unauthorized) {
            throw ApiException("Session expired", 401)
        }
        if (!response.status.isSuccess()) {
            val error: MessageResponse = response.body()
            throw ApiException(error.error ?: "Failed to set Telegram ID", response.status.value)
        }
        return response.body()
    }

    suspend fun updateTelegramId(token: String, userId: String): MessageResponse {
        val response = client.put("$BASE_URL/profile/update-user-id") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer $token")
            setBody(SetUserIdRequest(userId))
        }
        if (response.status == HttpStatusCode.Unauthorized) {
            throw ApiException("Session expired", 401)
        }
        if (!response.status.isSuccess()) {
            val error: MessageResponse = response.body()
            throw ApiException(error.error ?: "Failed to update Telegram ID", response.status.value)
        }
        return response.body()
    }

    // ---- My Files ----

    suspend fun saveFile(token: String, fileId: String, category: String, fileName: String, fileSize: Long): MessageResponse {
        val response = client.post("$BASE_URL/user/save-file") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer $token")
            setBody(SaveFileRequest(fileId, category, fileName, fileSize))
        }
        if (response.status == HttpStatusCode.Unauthorized) {
            throw ApiException("Session expired", 401)
        }
        if (!response.status.isSuccess()) {
            val error: MessageResponse = response.body()
            throw ApiException(error.error ?: "Failed to save file", response.status.value)
        }
        return response.body()
    }

    suspend fun getMyFiles(token: String, page: Int = 1): SearchResponse {
        Log.d("AFDS_API", "getMyFiles: page=$page")
        val response = client.get("$BASE_URL/user/my-files") {
            header("Authorization", "Bearer $token")
            parameter("page", page)
        }
        if (response.status == HttpStatusCode.Unauthorized) {
            throw ApiException("Session expired", 401)
        }
        val bodyText = response.bodyAsText()
        Log.d("AFDS_API", "getMyFiles raw response (first 500): ${bodyText.take(500)}")
        return json.decodeFromString<SearchResponse>(bodyText)
    }

    // ---- Update ----

    suspend fun checkForUpdate(): AppUpdateInfo {
        Log.d("AFDS_UPDATE", "Checking for update from $APK_BASE_URL/app.json")
        val response = client.get("$APK_BASE_URL/app.json")
        val bodyText = response.bodyAsText()
        Log.d("AFDS_UPDATE", "Update response: $bodyText")
        return json.decodeFromString<AppUpdateInfo>(bodyText)
    }

    fun getApkDownloadUrl(version: String): String {
        return "$APK_BASE_URL/$version.apk"
    }

    fun close() {
        client.close()
    }
}

class ApiException(message: String, val statusCode: Int) : Exception(message)