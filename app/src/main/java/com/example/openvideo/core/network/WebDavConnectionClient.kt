package com.example.openvideo.core.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WebDavConnectionClient @Inject constructor(
    okHttpClient: OkHttpClient,
    private val webDavMemoryCache: WebDavMemoryCache
) {
    private val noRedirectClient = okHttpClient.newBuilder()
        .followRedirects(false)
        .followSslRedirects(false)
        .build()

    sealed class DirectoryResult {
        data class Success(val entries: List<WebDavDirectoryParser.Entry>) : DirectoryResult()
        data class Failure(val error: WebDavConnectionPolicy.Error) : DirectoryResult()
    }

    suspend fun testConnection(
        baseUrl: String,
        username: String,
        password: String,
        userAgent: String
    ): WebDavConnectionPolicy.ConnectionResult = withContext(Dispatchers.IO) {
        val request = WebDavConnectionPolicy.buildPropfindRequest(
            baseUrl = baseUrl,
            username = username,
            password = password,
            userAgent = userAgent
        )
        runCatching {
            noRedirectClient.newCall(request).execute().use { response ->
                WebDavConnectionPolicy.classifyHttpStatus(response.code)
            }
        }.getOrElse { error ->
            WebDavConnectionPolicy.ConnectionResult.Failure(WebDavConnectionPolicy.classifyFailure(error))
        }
    }

    suspend fun listDirectory(
        directoryUrl: String,
        username: String,
        password: String,
        userAgent: String
    ): DirectoryResult = withContext(Dispatchers.IO) {
        val cacheKey = webDavMemoryCache.cacheKey(
            namespace = "directory",
            url = directoryUrl,
            requestHeaders = mapOf("Authorization" to username.trim())
        )
        webDavMemoryCache.getDirectory(cacheKey)?.let { entries ->
            return@withContext DirectoryResult.Success(entries)
        }
        val request = WebDavConnectionPolicy.buildPropfindRequest(
            baseUrl = directoryUrl,
            username = username,
            password = password,
            userAgent = userAgent,
            depth = "1"
        )
        runCatching {
            noRedirectClient.newCall(request).execute().use response@{ response ->
                when (val status = WebDavConnectionPolicy.classifyHttpStatus(response.code)) {
                    WebDavConnectionPolicy.ConnectionResult.Success -> {
                        if (response.body.contentLength() > MAX_DIRECTORY_RESPONSE_BYTES) {
                            return@response DirectoryResult.Failure(WebDavConnectionPolicy.Error.INVALID_RESPONSE)
                        }
                        val xml = response.body.byteStream().use input@{ input ->
                            val output = java.io.ByteArrayOutputStream()
                            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                            var total = 0L
                            while (true) {
                                val count = input.read(buffer)
                                if (count < 0) break
                                total += count
                                if (total > MAX_DIRECTORY_RESPONSE_BYTES) {
                                    return@input null
                                }
                                output.write(buffer, 0, count)
                            }
                            output.toString(Charsets.UTF_8.name())
                        } ?: return@response DirectoryResult.Failure(WebDavConnectionPolicy.Error.INVALID_RESPONSE)
                        val entries = runCatching {
                            WebDavDirectoryParser.parse(baseUrl = directoryUrl, xml = xml)
                        }.getOrElse {
                            return@response DirectoryResult.Failure(WebDavConnectionPolicy.Error.INVALID_RESPONSE)
                        }
                        webDavMemoryCache.putDirectory(cacheKey, entries)
                        DirectoryResult.Success(entries)
                    }
                    is WebDavConnectionPolicy.ConnectionResult.Failure -> DirectoryResult.Failure(status.error)
                }
            }
        }.getOrElse { error ->
            DirectoryResult.Failure(WebDavConnectionPolicy.classifyFailure(error))
        }
    }

    private companion object {
        const val MAX_DIRECTORY_RESPONSE_BYTES = 4L * 1024L * 1024L
    }
}
