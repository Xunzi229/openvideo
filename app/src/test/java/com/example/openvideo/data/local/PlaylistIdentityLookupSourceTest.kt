package com.example.openvideo.data.local

import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class PlaylistIdentityLookupSourceTest {

    @Test
    fun playlistDaoListsCurrentIdentityRowsWhenVideoIdChanges() {
        val source = String(Files.readAllBytes(sourceFile()))

        assertTrue(source.contains("fun getVideos(playlistId: Long): Flow<List<PlaylistVideoEntity>>"))
        assertTrue(source.contains("suspend fun getVideosOnce(playlistId: Long): List<PlaylistVideoEntity>"))
        assertTrue(source.contains("LEFT JOIN media_identity"))
        assertTrue(source.contains("playlist_videos.mediaIdentityId = media_identity.identityId"))
        assertTrue(source.contains("media_identity.currentVideoId"))
        assertTrue(source.contains("media_identity.currentPath"))
        assertTrue(source.contains("media_identity.durationMs"))
        assertTrue(source.contains("playlist_videos.position AS position"))
        assertTrue(source.contains("ORDER BY playlist_videos.position"))
    }

    @Test
    fun playlistDaoRemovesOldIdentityRowsWhenCurrentVideoIdIsRemoved() {
        val source = String(Files.readAllBytes(sourceFile()))

        assertTrue(source.contains("DELETE FROM playlist_videos"))
        assertTrue(source.contains("videoId = :videoId"))
        assertTrue(source.contains("mediaIdentityId IN"))
        assertTrue(source.contains("SELECT identityId FROM media_identity WHERE currentVideoId = :videoId"))
    }

    private fun sourceFile(): Path {
        val relativePath = Paths.get(
            "src",
            "main",
            "java",
            "com",
            "example",
            "openvideo",
            "data",
            "local",
            "PlaylistDao.kt"
        )
        return sequenceOf(relativePath, Paths.get("app").resolve(relativePath)).first(Files::exists)
    }
}
