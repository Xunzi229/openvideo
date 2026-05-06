package com.example.openvideo.data.scanner

import android.app.PendingIntent
import android.app.RecoverableSecurityException
import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import com.example.openvideo.data.model.VideoItem
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject
import javax.inject.Singleton

sealed class VideoDeleteResult {
    data class Deleted(val uris: Set<Uri>) : VideoDeleteResult()
    data class RequiresUserAction(val pendingIntent: PendingIntent) : VideoDeleteResult()
    object Failed : VideoDeleteResult()
}

@Singleton
class VideoScanner @Inject constructor(
    @ApplicationContext private val context: Context
) {

    fun scanVideos(): Flow<List<VideoItem>> = flow {
        val videos = mutableListOf<VideoItem>()

        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        }

        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DATA,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.WIDTH,
            MediaStore.Video.Media.HEIGHT,
            MediaStore.Video.Media.DATE_ADDED
        )

        val sortOrder = "${MediaStore.Video.Media.DATE_ADDED} DESC"

        context.contentResolver.query(
            collection, projection, null, null, sortOrder
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
            val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)
            val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
            val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
            val widthCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.WIDTH)
            val heightCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.HEIGHT)
            val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val name = cursor.getString(nameCol) ?: "Unknown"
                val path = cursor.getString(dataCol) ?: ""
                val duration = cursor.getLong(durationCol)
                val size = cursor.getLong(sizeCol)
                val width = cursor.getInt(widthCol)
                val height = cursor.getInt(heightCol)
                val dateAdded = cursor.getLong(dateCol)

                val contentUri = ContentUris.withAppendedId(
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id
                )

                val thumbnailUri = Uri.withAppendedPath(
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI, "$id"
                )

                videos.add(
                    VideoItem(
                        id = id,
                        title = name,
                        path = path,
                        uri = contentUri,
                        duration = duration,
                        size = size,
                        width = width,
                        height = height,
                        dateAdded = dateAdded,
                        thumbnailUri = thumbnailUri
                    )
                )
            }
        }

        emit(videos)
    }.flowOn(Dispatchers.IO)

    fun deleteVideo(uri: Uri): Boolean {
        return deleteVideoWithResult(uri) is VideoDeleteResult.Deleted
    }

    fun deleteVideos(uris: List<Uri>): VideoDeleteResult {
        if (uris.isEmpty()) return VideoDeleteResult.Deleted(emptySet())

        createDeleteRequest(uris)?.let { request ->
            return VideoDeleteResult.RequiresUserAction(request)
        }

        val deleted = mutableSetOf<Uri>()
        for (uri in uris) {
            when (val result = deleteVideoWithResult(uri)) {
                is VideoDeleteResult.Deleted -> deleted += result.uris
                is VideoDeleteResult.RequiresUserAction -> return result
                VideoDeleteResult.Failed -> Unit
            }
        }

        return if (deleted.isNotEmpty()) {
            VideoDeleteResult.Deleted(deleted)
        } else {
            VideoDeleteResult.Failed
        }
    }

    private fun deleteVideoWithResult(uri: Uri): VideoDeleteResult {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            deleteVideoWithScopedStorageFallback(uri)
        } else {
            deleteVideoLegacy(uri)
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun deleteVideoWithScopedStorageFallback(uri: Uri): VideoDeleteResult {
        return try {
            if (context.contentResolver.delete(uri, null, null) > 0) {
                VideoDeleteResult.Deleted(setOf(uri))
            } else {
                VideoDeleteResult.Failed
            }
        } catch (e: RecoverableSecurityException) {
            VideoDeleteResult.RequiresUserAction(e.userAction.actionIntent)
        } catch (e: Exception) {
            VideoDeleteResult.Failed
        }
    }

    private fun deleteVideoLegacy(uri: Uri): VideoDeleteResult {
        return try {
            if (context.contentResolver.delete(uri, null, null) > 0) {
                VideoDeleteResult.Deleted(setOf(uri))
            } else {
                VideoDeleteResult.Failed
            }
        } catch (e: Exception) {
            VideoDeleteResult.Failed
        }
    }

    fun createDeleteRequest(uris: List<Uri>): PendingIntent? {
        if (uris.isEmpty()) return null
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            MediaStore.createDeleteRequest(context.contentResolver, uris)
        } else {
            null
        }
    }

}
