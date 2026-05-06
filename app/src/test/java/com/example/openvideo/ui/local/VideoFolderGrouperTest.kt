package com.example.openvideo.ui.local

import org.junit.Assert.assertEquals
import org.junit.Test

class VideoFolderGrouperTest {

    @Test
    fun groupsVideosByParentDirectory() {
        val folders = VideoFolderGrouper.groupPaths(
            listOf(
                "/storage/emulated/0/DCIM/Camera/a.mp4",
                "/storage/emulated/0/DCIM/Camera/b.mp4",
                "/storage/emulated/0/Movies/English/c.mp4"
            )
        )

        assertEquals(
            listOf(
                VideoFolderSummary(
                    key = "/storage/emulated/0/DCIM/Camera",
                    name = "Camera",
                    videoCount = 2
                ),
                VideoFolderSummary(
                    key = "/storage/emulated/0/Movies/English",
                    name = "English",
                    videoCount = 1
                )
            ),
            folders
        )
    }

    @Test
    fun fallsBackToUnknownFolderForContentUris() {
        val folders = VideoFolderGrouper.groupPaths(
            listOf("content://media/external/video/media/12")
        )

        assertEquals(
            listOf(
                VideoFolderSummary(
                    key = VideoFolderGrouper.UNKNOWN_FOLDER_KEY,
                    name = "未知目录",
                    videoCount = 1
                )
            ),
            folders
        )
    }
}
