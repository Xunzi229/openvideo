package com.example.openvideo.ui.privacy

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PrivacyManager @Inject constructor(
    @param:ApplicationContext private val context: Context
) {

    private val prefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun getHiddenFolders(): List<String> {
        val set = prefs.getStringSet(KEY_HIDDEN_FOLDERS, emptySet()) ?: emptySet()
        return set.map(PrivacyPathPolicy::canonical).filter(String::isNotBlank).distinct().sorted()
    }

    fun addHiddenFolder(path: String) {
        val current = getHiddenFolders().toMutableSet()
        PrivacyPathPolicy.canonical(path).takeIf(String::isNotBlank)?.let(current::add)
        prefs.edit().putStringSet(KEY_HIDDEN_FOLDERS, current).apply()
    }

    fun removeHiddenFolder(path: String) {
        val current = getHiddenFolders().toMutableSet()
        current.remove(PrivacyPathPolicy.canonical(path))
        prefs.edit().putStringSet(KEY_HIDDEN_FOLDERS, current).apply()
    }

    fun isPathHidden(path: String): Boolean {
        return getHiddenFolders().any { PrivacyPathPolicy.isWithin(path, it) }
    }

    fun setPassword(password: String) {
        prefs.edit().putString(KEY_PASSWORD_HASH, PrivacyPasswordPolicy.encode(password)).apply()
    }

    fun verifyPassword(password: String): Boolean {
        val stored = prefs.getString(KEY_PASSWORD_HASH, null) ?: return false
        val result = PrivacyPasswordPolicy.verify(password, stored)
        if (result.valid && result.needsUpgrade) setPassword(password)
        return result.valid
    }

    fun hasPassword(): Boolean {
        return prefs.getString(KEY_PASSWORD_HASH, null) != null
    }

    companion object {
        private const val PREFS_NAME = "privacy_prefs"
        private const val KEY_HIDDEN_FOLDERS = "hidden_folders"
        private const val KEY_PASSWORD_HASH = "password_hash"
    }
}
