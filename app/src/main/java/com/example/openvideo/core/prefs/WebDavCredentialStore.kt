package com.example.openvideo.core.prefs

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WebDavCredentialStore @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs = EncryptedSharedPreferences.create(
        context,
        PREFS_NAME,
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun save(sourceId: Long, username: String, password: String) {
        prefs.edit()
            .putString(usernameKey(sourceId), username.trim())
            .putString(passwordKey(sourceId), password)
            .apply()
    }

    fun read(sourceId: Long): Credentials? {
        val username = prefs.getString(usernameKey(sourceId), null)?.takeIf { it.isNotBlank() }
        val password = prefs.getString(passwordKey(sourceId), null)?.takeIf { it.isNotBlank() }
        return if (username != null && password != null) Credentials(username, password) else null
    }

    fun delete(sourceId: Long) {
        prefs.edit()
            .remove(usernameKey(sourceId))
            .remove(passwordKey(sourceId))
            .apply()
    }

    data class Credentials(val username: String, val password: String)

    companion object {
        private const val PREFS_NAME = "webdav_credentials"

        fun credentialAlias(sourceId: Long): String = "webdav:$sourceId"

        private fun usernameKey(sourceId: Long): String = "${credentialAlias(sourceId)}:username"

        private fun passwordKey(sourceId: Long): String = "${credentialAlias(sourceId)}:password"
    }
}
