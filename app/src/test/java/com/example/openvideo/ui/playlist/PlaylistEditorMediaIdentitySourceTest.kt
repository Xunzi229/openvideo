package com.example.openvideo.ui.playlist

import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class PlaylistEditorMediaIdentitySourceTest {

    @Test
    fun playlistEditorWritesMediaIdentityIdForRegularPlaylistAdds() {
        val editorSource = String(Files.readAllBytes(sourceFile("PlaylistEditor.kt")))
        val insertionSource = String(Files.readAllBytes(sourceFile("PlaylistInsertion.kt")))

        assertTrue(editorSource.contains("private val mediaIdentityDao: MediaIdentityDao"))
        assertTrue(editorSource.contains("val mediaIdentityId = mediaIdentityDao.getByCurrentVideoId(video.id)?.identityId"))
        assertTrue(editorSource.contains("PlaylistInsertion.createEntry(playlistId, existing, video, mediaIdentityId)"))
        assertTrue(insertionSource.contains("mediaIdentityId: Long? = null"))
        assertTrue(insertionSource.contains("mediaIdentityId = mediaIdentityId"))
    }

    private fun sourceFile(name: String): Path {
        val relativePath = Paths.get(
            "src",
            "main",
            "java",
            "com",
            "example",
            "openvideo",
            "ui",
            "playlist",
            name
        )
        return sequenceOf(relativePath, Paths.get("app").resolve(relativePath)).first(Files::exists)
    }
}
