package com.example.openvideo.core.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/**
 * Downloads release APK to app cache, verifies SHA-256, triggers package installer via [FileProvider].
 */
object UpdateApkInstaller {

    private const val APK_SUBDIR = "updates"
    private const val APK_NAME = "openvideo-update.apk"

    fun cacheApkFile(context: Context): File {
        val dir = File(context.cacheDir, APK_SUBDIR).apply { mkdirs() }
        return File(dir, APK_NAME)
    }

    fun downloadApk(url: String, dest: File, userAgent: String): Boolean {
        dest.parentFile?.mkdirs()
        if (dest.exists()) dest.delete()

        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 30_000
            readTimeout = 120_000
            setRequestProperty("User-Agent", userAgent)
        }
        return try {
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            if (code !in 200..299) return false
            FileOutputStream(dest).use { out ->
                stream.copyTo(out)
            }
            dest.length() > 0L
        } catch (_: Exception) {
            false
        } finally {
            conn.disconnect()
        }
    }

    fun sha256Hex(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buf = ByteArray(8192)
            while (true) {
                val n = input.read(buf)
                if (n <= 0) break
                digest.update(buf, 0, n)
            }
        }
        return digest.digest().joinToString("") { b -> "%02x".format(b) }
    }

    fun shaMatches(file: File, expectedHex: String): Boolean {
        val actual = sha256Hex(file)
        val exp = expectedHex.trim().lowercase()
        return actual.equals(exp, ignoreCase = false)
    }

    fun buildInstallIntent(context: Context, apk: File): Intent {
        val authority = "${context.packageName}.fileprovider"
        val uri = FileProvider.getUriForFile(context, authority, apk)
        return Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}
