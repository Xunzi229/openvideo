package com.example.openvideo.ui.privacy

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.MessageDigest
import java.security.SecureRandom

class PrivacyManager(private val context: Context) {

    private val secureRandom = SecureRandom()

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
        val salt = generateSalt()
        val hash = hashPassword(password, salt)
        prefs.edit().putString(KEY_PASSWORD_HASH, "$salt:$hash").apply()
    }

    fun verifyPassword(password: String): Boolean {
        val stored = prefs.getString(KEY_PASSWORD_HASH, null) ?: return false
        val parts = stored.split(":", limit = 2)
        if (parts.size != 2) return false
        val (salt, expectedHash) = parts
        return hashPassword(password, salt) == expectedHash
    }

    fun hasPassword(): Boolean {
        return prefs.getString(KEY_PASSWORD_HASH, null) != null
    }

    private fun generateSalt(): String {
        val bytes = ByteArray(16)
        secureRandom.nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun hashPassword(password: String, salt: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest((salt + password).toByteArray())
        return hash.joinToString("") { "%02x".format(it) }
    }

    companion object {
        private const val KEY_HIDDEN_FOLDERS = "hidden_folders"
        private const val KEY_PASSWORD_HASH = "password_hash"
    }
}
