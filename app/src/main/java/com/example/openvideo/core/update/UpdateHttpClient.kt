package com.example.openvideo.core.update

import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

/** Small, redirect-aware HTTP client dedicated to trusted update assets. */
object UpdateHttpClient {
    private const val MAX_REDIRECTS = 5
    const val MAX_CHECKSUM_BYTES = 1024L * 1024L
    const val MAX_APK_BYTES = 1536L * 1024L * 1024L

    fun readText(url: String, userAgent: String, maxBytes: Long = MAX_CHECKSUM_BYTES): String? {
        if (!UpdateUrlPolicy.isTrustedReleaseAsset(url)) return null
        return execute(url, userAgent) { connection ->
            val declaredSize = connection.contentLengthLong
            if (declaredSize > maxBytes) return@execute null
            val output = java.io.ByteArrayOutputStream()
            var total = 0L
            connection.inputStream.use { input ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    total += count
                    if (total > maxBytes) return@execute null
                    output.write(buffer, 0, count)
                }
            }
            if (declaredSize >= 0L && total != declaredSize) return@execute null
            output.toString(Charsets.UTF_8.name())
        }
    }

    fun download(url: String, destination: File, userAgent: String, maxBytes: Long = MAX_APK_BYTES): Boolean {
        if (!UpdateUrlPolicy.isTrustedReleaseAsset(url)) return false
        destination.parentFile?.mkdirs()
        val partial = File(destination.parentFile, "${destination.name}.part")
        partial.delete()

        val downloaded = execute(url, userAgent) { connection ->
            val declaredSize = connection.contentLengthLong
            if (declaredSize > maxBytes) return@execute false
            var total = 0L
            connection.inputStream.use { input ->
                FileOutputStream(partial).use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        total += count
                        if (total > maxBytes) return@execute false
                        output.write(buffer, 0, count)
                    }
                }
            }
            total > 0L && (declaredSize < 0L || total == declaredSize)
        } == true

        if (!downloaded) {
            partial.delete()
            return false
        }
        if (destination.exists() && !destination.delete()) {
            partial.delete()
            return false
        }
        return partial.renameTo(destination).also { moved ->
            if (!moved) partial.delete()
        }
    }

    private fun <T> execute(initialUrl: String, userAgent: String, block: (HttpURLConnection) -> T?): T? {
        var current = URL(initialUrl)
        repeat(MAX_REDIRECTS + 1) { redirectCount ->
            val connection = (current.openConnection() as? HttpURLConnection) ?: return null
            connection.instanceFollowRedirects = false
            connection.requestMethod = "GET"
            connection.connectTimeout = 15_000
            connection.readTimeout = 120_000
            connection.setRequestProperty("User-Agent", userAgent)
            try {
                when (connection.responseCode) {
                    in 200..299 -> return block(connection)
                    in setOf(301, 302, 303, 307, 308) -> {
                        if (redirectCount >= MAX_REDIRECTS) return null
                        val location = connection.getHeaderField("Location") ?: return null
                        val redirected = URL(current, location)
                        if (!UpdateUrlPolicy.isTrustedRedirect(redirected.toString())) return null
                        current = redirected
                    }
                    else -> return null
                }
            } catch (_: Exception) {
                return null
            } finally {
                connection.disconnect()
            }
        }
        return null
    }
}
