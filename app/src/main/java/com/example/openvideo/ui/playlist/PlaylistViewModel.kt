package com.example.openvideo.ui.playlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.openvideo.data.local.PlaylistDao
import com.example.openvideo.data.local.PlaylistEntity
import com.example.openvideo.data.local.PlaylistVideoEntity
import com.example.openvideo.data.model.VideoItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
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

    fun removeStalePlaylistVideos(playlistId: Long, videoIds: List<Long>) {
        if (videoIds.isEmpty()) return
        viewModelScope.launch {
            videoIds.forEach { videoId ->
                playlistDao.removeVideo(playlistId, videoId)
            }
        }
    }

    fun pruneMissingFilesFromPlaylist(playlistId: Long) {
        viewModelScope.launch {
            val list = playlistDao.getVideosOnce(playlistId)
            val stale = PlaylistVideoAvailabilityPolicy.staleVideoIds(list)
            removeStalePlaylistVideos(playlistId, stale)
        }
    }

    fun clearPlaylist(playlistId: Long) {
        viewModelScope.launch {
            playlistDao.clearVideos(playlistId)
        }
    }

    fun selectPlaylist(playlist: PlaylistEntity?) {
        _selectedPlaylist.value = playlist
    }
}
