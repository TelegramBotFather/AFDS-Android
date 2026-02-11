package com.afds.app.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "afds_prefs")

class SessionManager(private val context: Context) {

    companion object {
        private val AUTH_TOKEN = stringPreferencesKey("auth_token")
        private val TOKEN_TIME = longPreferencesKey("token_time")
        private val SELECTED_CATEGORY = stringPreferencesKey("selected_category")
        private val NSFW_ENABLED = booleanPreferencesKey("nsfw_enabled")
        private val MIX_MEDIA_ENABLED = booleanPreferencesKey("mix_media_enabled")
        private val SHOW_MY_FILES = booleanPreferencesKey("show_my_files")
        private val CHANNEL_ID = stringPreferencesKey("channel_id")
        private const val THIRTY_DAYS_MS = 30L * 24 * 60 * 60 * 1000
    }

    val authToken: Flow<String?> = context.dataStore.data.map { it[AUTH_TOKEN] }
    val isLoggedIn: Flow<Boolean> = context.dataStore.data.map { prefs ->
        val token = prefs[AUTH_TOKEN]
        val tokenTime = prefs[TOKEN_TIME] ?: 0L
        if (token == null) return@map false
        val age = System.currentTimeMillis() - tokenTime
        age < THIRTY_DAYS_MS
    }
    val selectedCategory: Flow<String> = context.dataStore.data.map { it[SELECTED_CATEGORY] ?: "files" }
    val nsfwEnabled: Flow<Boolean> = context.dataStore.data.map { it[NSFW_ENABLED] ?: false }
    val mixMediaEnabled: Flow<Boolean> = context.dataStore.data.map { it[MIX_MEDIA_ENABLED] ?: false }
    val showMyFiles: Flow<Boolean> = context.dataStore.data.map { it[SHOW_MY_FILES] ?: false }
    val channelId: Flow<String?> = context.dataStore.data.map { it[CHANNEL_ID] }

    suspend fun getToken(): String? {
        val prefs = context.dataStore.data.first()
        val token = prefs[AUTH_TOKEN] ?: return null
        val tokenTime = prefs[TOKEN_TIME] ?: 0L
        val age = System.currentTimeMillis() - tokenTime
        if (age > THIRTY_DAYS_MS) {
            clearSession()
            return null
        }
        return token
    }

    suspend fun saveToken(token: String) {
        context.dataStore.edit { prefs ->
            prefs[AUTH_TOKEN] = token
            prefs[TOKEN_TIME] = System.currentTimeMillis()
        }
    }

    suspend fun clearSession() {
        context.dataStore.edit { it.clear() }
    }

    suspend fun setSelectedCategory(category: String) {
        context.dataStore.edit { it[SELECTED_CATEGORY] = category }
    }

    suspend fun setNsfwEnabled(enabled: Boolean) {
        context.dataStore.edit { it[NSFW_ENABLED] = enabled }
    }

    suspend fun setMixMediaEnabled(enabled: Boolean) {
        context.dataStore.edit { it[MIX_MEDIA_ENABLED] = enabled }
    }

    suspend fun setShowMyFiles(enabled: Boolean) {
        context.dataStore.edit { it[SHOW_MY_FILES] = enabled }
    }

    suspend fun getChannelId(): String? {
        return context.dataStore.data.first()[CHANNEL_ID]
    }

    suspend fun setChannelId(channelId: String?) {
        context.dataStore.edit {
            if (channelId != null) {
                it[CHANNEL_ID] = channelId
            } else {
                it.remove(CHANNEL_ID)
            }
        }
    }
}