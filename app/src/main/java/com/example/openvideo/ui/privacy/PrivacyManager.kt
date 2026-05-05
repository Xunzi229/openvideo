package com.example.openvideo.ui.privacy

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class PrivacyManager(private val context: Context) {

    private val prefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context,
            "privacy_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun getHiddenFolders(): List<String> {
        val set = prefs.getStringSet(KEY_HIDDEN_FOLDERS, emptySet()) ?: emptySet()
        return set.toList()
    }

    fun addHiddenFolder(path: String) {
        val current = getHiddenFolders().toMutableSet()
        current.add(path)
        prefs.edit().putStringSet(KEY_HIDDEN_FOLDERS, current).apply()
    }

    fun removeHiddenFolder(path: String) {
        val current = getHiddenFolders().toMutableSet()
        current.remove(path)
        prefs.edit().putStringSet(KEY_HIDDEN_FOLDERS, current).apply()
    }

    fun isPathHidden(path: String): Boolean {
        return getHiddenFolders().any { path.startsWith(it) }
    }

    fun setPassword(password: String) {
        prefs.edit().putString(KEY_PASSWORD, password).apply()
    }

    fun verifyPassword(password: String): Boolean {
        val stored = prefs.getString(KEY_PASSWORD, null) ?: return true
        return stored == password
    }

    fun hasPassword(): Boolean {
        return prefs.getString(KEY_PASSWORD, null) != null
    }

    companion object {
        private const val KEY_HIDDEN_FOLDERS = "hidden_folders"
        private const val KEY_PASSWORD = "password"
    }
}
