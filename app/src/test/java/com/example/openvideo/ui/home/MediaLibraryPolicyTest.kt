package com.example.openvideo.ui.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaLibraryPolicyTest {

    @Test
    fun hiddenFolderMatchesOnlyPathBoundary() {
        assertTrue(
            MediaLibraryPolicy.isHiddenPath(
                path = "/storage/emulated/0/Movies/private/a.mp4",
                hiddenFolders = listOf("/storage/emulated/0/Movies/private")
            )
        )
        assertFalse(
            MediaLibraryPolicy.isHiddenPath(
                path = "/storage/emulated/0/Movies/private2/a.mp4",
                hiddenFolders = listOf("/storage/emulated/0/Movies/private")
            )
        )
    }

    @Test
    fun filtersHiddenPathsBeforeFolderFilter() {
        val paths = listOf(
            "/storage/emulated/0/Movies/public/a.mp4",
            "/storage/emulated/0/Movies/private/b.mp4",
            "/storage/emulated/0/DCIM/c.mp4"
        )

        assertEquals(
            listOf("/storage/emulated/0/Movies/public/a.mp4"),
            MediaLibraryPolicy.visiblePaths(
                paths = paths,
                hiddenFolders = listOf("/storage/emulated/0/Movies/private"),
                folderKey = "/storage/emulated/0/Movies/public"
            )
        )
    }

    @Test
    fun sameScanSignatureDoesNotNeedPublishingAgain() {
        val previous = MediaScanSignature.fromPaths(
            listOf("/a.mp4" to 10L, "/b.mp4" to 20L)
        )
        val same = MediaScanSignature.fromPaths(
            listOf("/b.mp4" to 20L, "/a.mp4" to 10L)
        )
        val changed = MediaScanSignature.fromPaths(
            listOf("/a.mp4" to 10L, "/b.mp4" to 25L)
        )

        assertFalse(MediaLibraryPolicy.shouldPublishScan(previous, same))
        assertTrue(MediaLibraryPolicy.shouldPublishScan(previous, changed))
    }

    @Test
    fun emptyStateDistinguishesNoMediaFromFiltering() {
        assertEquals(
            MediaLibraryEmptyState.LOADING,
            MediaLibraryPolicy.emptyState(isLoading = true, scannedCount = 0, visibleCount = 0)
        )
        assertEquals(
            MediaLibraryEmptyState.NO_MEDIA,
            MediaLibraryPolicy.emptyState(isLoading = false, scannedCount = 0, visibleCount = 0)
        )
        assertEquals(
            MediaLibraryEmptyState.FILTERED_BY_PRIVACY,
            MediaLibraryPolicy.emptyState(
                isLoading = false,
                scannedCount = 3,
                visibleCount = 0,
                hiddenFilteredCount = 3
            )
        )
        assertEquals(
            MediaLibraryEmptyState.FILTERED_BY_QUERY_OR_FOLDER,
            MediaLibraryPolicy.emptyState(
                isLoading = false,
                scannedCount = 3,
                visibleCount = 0,
                hiddenFilteredCount = 1
            )
        )
    }

    @Test
    fun folderFilterKeyFallsBackWhenFolderNoLongerExists() {
        val folders = listOf("/storage/emulated/0/Movies", "/storage/emulated/0/DCIM")

        assertEquals(
            "/storage/emulated/0/Movies",
            MediaLibraryPolicy.validFolderKey(
                selectedFolderKey = "/storage/emulated/0/Movies",
                folderKeys = folders
            )
        )
        assertEquals(
            null,
            MediaLibraryPolicy.validFolderKey(
                selectedFolderKey = "/storage/emulated/0/Downloads",
                folderKeys = folders
            )
        )
    }
}
