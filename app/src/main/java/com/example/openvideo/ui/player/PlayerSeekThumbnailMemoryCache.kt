package com.example.openvideo.ui.player

import android.graphics.Bitmap

object PlayerSeekThumbnailMemoryCache {

    private const val MAX_ENTRIES = 24

    private val entries = object : LinkedHashMap<String, Bitmap>(MAX_ENTRIES, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Bitmap>?): Boolean {
            return size > MAX_ENTRIES
        }
    }

    @Synchronized
    fun get(key: String): Bitmap? = entries[key]

    @Synchronized
    fun put(key: String, bitmap: Bitmap) {
        entries[key] = bitmap
    }

    @Synchronized
    fun clear() {
        entries.clear()
    }
}
