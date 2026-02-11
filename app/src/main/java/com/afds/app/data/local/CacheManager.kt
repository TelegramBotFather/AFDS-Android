package com.afds.app.data.local

import android.util.Log

/**
 * Simple in-memory cache with 60-second TTL.
 * Used for search results, browse results, profile data.
 * NOT used for file sending or link generation.
 */
object CacheManager {
    private const val TAG = "AFDS_CACHE"
    private const val TTL_MS = 60_000L // 60 seconds

    private data class CacheEntry(
        val data: Any,
        val timestamp: Long = System.currentTimeMillis()
    ) {
        fun isExpired(): Boolean = System.currentTimeMillis() - timestamp > TTL_MS
    }

    private val cache = mutableMapOf<String, CacheEntry>()

    fun <T> get(key: String): T? {
        val entry = cache[key] ?: return null
        if (entry.isExpired()) {
            cache.remove(key)
            Log.d(TAG, "Cache expired: $key")
            return null
        }
        Log.d(TAG, "Cache hit: $key")
        @Suppress("UNCHECKED_CAST")
        return entry.data as? T
    }

    fun put(key: String, data: Any) {
        cache[key] = CacheEntry(data)
        Log.d(TAG, "Cache set: $key")
    }

    fun invalidate(key: String) {
        cache.remove(key)
    }

    fun invalidateAll() {
        cache.clear()
        Log.d(TAG, "Cache cleared")
    }

    // Key builders
    fun searchKey(category: String, query: String, page: Int) = "search:$category:$query:$page"
    fun browseKey(category: String, page: Int) = "browse:$category:$page"
    fun profileKey() = "profile"
    fun myFilesKey(page: Int) = "myfiles:$page"
}