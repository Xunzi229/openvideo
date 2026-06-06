package com.example.openvideo.core.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WebDavConnectionClient @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val webDavMemoryCache: WebDavMemoryCache
) {
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
            okHttpClient.newCall(request).execute().use { response ->
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
            okHttpClient.newCall(request).execute().use { response ->
                when (val status = WebDavConnectionPolicy.classifyHttpStatus(response.code)) {
                    WebDavConnectionPolicy.ConnectionResult.Success -> {
                        val entries = WebDavDirectoryParser.parse(
                            baseUrl = directoryUrl,
                            xml = response.body.string()
                        )
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
}
