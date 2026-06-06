package com.example.openvideo.ui.playlist

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.openvideo.data.local.PlaylistDao
import com.example.openvideo.data.local.PlaylistEntity
import com.example.openvideo.data.local.PlaylistVideoEntity
import com.example.openvideo.data.model.VideoItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class PlaylistViewModel @Inject constructor(
    private val playlistDao: PlaylistDao,
    private val playlistEditor: PlaylistEditor
) : ViewModel() {

    val playlists: Flow<List<PlaylistEntity>> = playlistDao.getAll()

    private val _selectedPlaylist = MutableStateFlow<PlaylistEntity?>(null)
    val selectedPlaylist: StateFlow<PlaylistEntity?> = _selectedPlaylist

    fun getPlaylistVideos(playlistId: Long): Flow<List<PlaylistVideoEntity>> {
        return playlistDao.getVideos(playlistId)
    }

    fun createPlaylist(name: String) {
        viewModelScope.launch {
            playlistDao.insert(PlaylistEntity(name = name))
        }
    }

    fun renamePlaylist(id: Long, newName: String) {
        viewModelScope.launch {
            playlistDao.getById(id)?.let { playlist ->
                playlistDao.update(playlist.copy(name = newName, updatedAt = System.currentTimeMillis()))
            }
        }
    }

    fun deletePlaylist(id: Long) {
        viewModelScope.launch {
            playlistDao.delete(id)
        }
    }

    fun addToPlaylist(playlistId: Long, video: VideoItem) {
        viewModelScope.launch {
            playlistEditor.addToPlaylist(playlistId, video)
        }
    }

    fun removeFromPlaylist(playlistId: Long, videoId: Long) {
        viewModelScope.launch {
            playlistDao.removeVideo(playlistId, videoId)
        }
    }

    fun reorderPlaylistVideos(playlistId: Long, videos: List<PlaylistVideoEntity>) {
        viewModelScope.launch {
            playlistDao.updatePositions(
                videos
                    .filter { it.playlistId == playlistId }
                    .sortedBy { it.position }
                    .mapIndexed { index, video -> video.copy(position = index) }
            )
        }
    }

    fun removeStalePlaylistVideos(playlistId: Long, videoIds: List<Long>) {
        if (videoIds.isEmpty()) return
        viewModelScope.launch {
            removePlaylistVideosNow(playlistId, videoIds)
        }
    }

    private suspend fun removePlaylistVideosNow(playlistId: Long, videoIds: List<Long>) {
        videoIds.forEach { videoId ->
            playlistDao.removeVideo(playlistId, videoId)
        }
    }

    fun pruneMissingFilesFromPlaylist(playlistId: Long) {
        viewModelScope.launch {
            val list = playlistDao.getVideosOnce(playlistId)
            val stale = PlaylistVideoAvailabilityPolicy.staleVideoIds(list)
            removeStalePlaylistVideos(playlistId, stale)
        }
    }

    fun cleanupPlaylistVideos(playlistId: Long) {
        viewModelScope.launch {
            cleanupPlaylistVideosForUndo(playlistId)
        }
    }

    suspend fun cleanupPlaylistVideosForUndo(playlistId: Long): List<PlaylistVideoEntity> {
        val list = playlistDao.getVideosOnce(playlistId)
        val plan = PlaylistCleanupPolicy.plan(list)
        val removableIds = plan.removableVideoIds.toSet()
        val removedVideos = list.filter { video -> video.videoId in removableIds }
        removePlaylistVideosNow(playlistId, plan.removableVideoIds)
        return removedVideos
    }

    fun restorePlaylistVideos(videos: List<PlaylistVideoEntity>) {
        if (videos.isEmpty()) return
        viewModelScope.launch {
            videos.forEach { video -> playlistDao.insertVideo(video) }
        }
    }

    fun clearPlaylist(playlistId: Long) {
        viewModelScope.launch {
            playlistDao.clearVideos(playlistId)
        }
    }

    sealed class TransferResult {
        data object Success : TransferResult()
        data object ReadWriteFailure : TransferResult()
        data class ParseFailure(val reason: PlaylistTransferFormat.FailureReason) : TransferResult()
    }

    suspend fun writePlaylistExportTo(
        context: Context,
        uri: Uri,
        playlistId: Long,
        playlistName: String
    ): TransferResult = withContext(Dispatchers.IO) {
        try {
            val json = PlaylistTransferFormat.exportJson(
                playlistName = playlistName,
                videos = playlistDao.getVideosOnce(playlistId)
            )
            val stream = context.contentResolver.openOutputStream(uri)
                ?: return@withContext TransferResult.ReadWriteFailure
            stream.use { it.write(json.toByteArray(Charsets.UTF_8)) }
            TransferResult.Success
        } catch (_: Exception) {
            TransferResult.ReadWriteFailure
        }
    }

    suspend fun readAndImportPlaylist(
        context: Context,
        uri: Uri,
        playlistId: Long
    ): TransferResult = withContext(Dispatchers.IO) {
        val text = try {
            context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.readText()
                ?: return@withContext TransferResult.ReadWriteFailure
        } catch (_: Exception) {
            return@withContext TransferResult.ReadWriteFailure
        }

        val parseResult = if (text.trimStart().startsWith("{")) {
            PlaylistTransferFormat.parseJson(text)
        } else {
            PlaylistTransferFormat.parseM3u(text)
        }
        when (parseResult) {
            is PlaylistTransferFormat.ParseResult.Failure -> TransferResult.ParseFailure(parseResult.reason)
            is PlaylistTransferFormat.ParseResult.Success -> {
                val existing = playlistDao.getVideosOnce(playlistId)
                val rows = PlaylistImportPolicy.createRows(
                    playlistId = playlistId,
                    existing = existing,
                    candidates = parseResult.items
                )
                if (rows.isEmpty()) {
                    TransferResult.ParseFailure(PlaylistTransferFormat.FailureReason.EMPTY_PLAYLIST)
                } else {
                    rows.forEach { row -> playlistDao.insertVideo(row) }
                    TransferResult.Success
                }
            }
        }
    }

    fun selectPlaylist(playlist: PlaylistEntity?) {
        _selectedPlaylist.value = playlist
    }
}
