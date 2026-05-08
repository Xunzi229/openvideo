package com.example.openvideo.core.prefs

import android.content.Context
import android.content.SharedPreferences

abstract class PrefsManager(context: Context, prefsName: String) {

    protected val prefs: SharedPreferences =
        context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)

    protected fun getString(key: String, default: String): String =
        prefs.getString(key, default) ?: default

    protected fun putString(key: String, value: String) {
        prefs.edit().putString(key, value).apply()
    }

    protected fun getInt(key: String, default: Int): Int =
        prefs.getInt(key, default)

    protected fun putInt(key: String, value: Int) {
        prefs.edit().putInt(key, value).apply()
    }

    protected fun getLong(key: String, default: Long): Long =
        prefs.getLong(key, default)

    protected fun putLong(key: String, value: Long) {
        prefs.edit().putLong(key, value).apply()
    }

    protected fun getFloat(key: String, default: Float): Float =
        prefs.getFloat(key, default)

    protected fun putFloat(key: String, value: Float) {
        prefs.edit().putFloat(key, value).apply()
    }

    protected fun getBoolean(key: String, default: Boolean): Boolean =
        prefs.getBoolean(key, default)

    protected fun putBoolean(key: String, value: Boolean) {
        prefs.edit().putBoolean(key, value).apply()
    }
}
