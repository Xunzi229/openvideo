package com.example.openvideo.ui.playlist

import com.example.openvideo.data.local.PlaylistDao
import com.example.openvideo.data.local.PlaylistEntity
import com.example.openvideo.data.local.MediaIdentityDao
import com.example.openvideo.data.model.VideoItem
import javax.inject.Inject

class PlaylistEditor @Inject constructor(
    private val playlistDao: PlaylistDao,
    private val mediaIdentityDao: MediaIdentityDao
) {

    suspend fun addToPlaylist(playlistId: Long, video: VideoItem): Boolean {
        val existing = playlistDao.getVideosOnce(playlistId)
        val mediaIdentityId = mediaIdentityDao.getByCurrentVideoId(video.id)?.identityId
        val entry = PlaylistInsertion.createEntry(playlistId, existing, video, mediaIdentityId) ?: return false

        playlistDao.insertVideo(entry)
        touchPlaylist(playlistId)
        return true
    }

    suspend fun createPlaylistWithVideo(name: String, video: VideoItem): Long {
        val playlistId = playlistDao.insert(PlaylistEntity(name = name))
        addToPlaylist(playlistId, video)
        return playlistId
    }

    private suspend fun touchPlaylist(playlistId: Long) {
        playlistDao.getById(playlistId)?.let { playlist ->
            playlistDao.update(playlist.copy(updatedAt = System.currentTimeMillis()))
        }
    }
}
