package com.example.openvideo.data.scanner

import org.junit.Assert.assertEquals
import org.junit.Test

class MediaStorePathPolicyTest {

    @Test
    fun scopedStorageUsesContentUriForPlaybackAndRelativePathForLibraryDisplay() {
        val contentUri = "content://media/external/video/media/42"

        assertEquals(
            contentUri,
            MediaStorePathPolicy.playbackSource(29, "/storage/emulated/0/Movies/a.mp4", contentUri)
        )
        assertEquals(
            "/storage/emulated/0/Movies/Private/a.mp4",
            MediaStorePathPolicy.libraryPath(
                dataPath = "",
                relativePath = "Movies/Private/",
                displayName = "a.mp4",
                externalStorageRoot = "/storage/emulated/0",
                contentUri = contentUri
            )
        )
    }

    @Test
    fun legacyStorageKeepsFilesystemPathAndMissingMetadataFallsBackToContentUri() {
        val contentUri = "content://media/external/video/media/7"

        assertEquals(
            "/sdcard/Movies/a.mp4",
            MediaStorePathPolicy.playbackSource(28, "/sdcard/Movies/a.mp4", contentUri)
        )
        assertEquals(
            contentUri,
            MediaStorePathPolicy.libraryPath("", "", "a.mp4", "/sdcard", contentUri)
        )
    }
}
