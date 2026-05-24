package com.example.openvideo.ui.local

import com.example.openvideo.data.model.VideoItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoFolderFilterPolicyTest {

    @Test
    fun hidesEmptyFoldersAndSortsPinnedFirst() {
        val folders = VideoFolderFilterPolicy.displayFolders(
            folders = listOf(
                summary("b", "B", 2),
                summary("a", "A", 0),
                summary("c", "C", 1)
            ),
            pinnedKeys = setOf("c")
        )

        assertEquals(listOf("c", "b"), folders.map { it.key })
        assertTrue(folders.first().isPinned)
        assertFalse(folders[1].isPinned)
    }

    @Test
    fun sortsSummaryFoldersByVideoCountWithinPinnedGroups() {
        val folders = VideoFolderFilterPolicy.displayFolders(
            folders = listOf(
                summary("unpinned-small", "Zulu", 1),
                summary("pinned-small", "Beta", 1),
                summary("unpinned-large", "Alpha", 4),
                summary("pinned-large", "Gamma", 3),
                summary("pinned-large-name", "Alpha", 3)
            ),
            pinnedKeys = setOf("pinned-small", "pinned-large", "pinned-large-name")
        )

        assertEquals(
            listOf("pinned-large-name", "pinned-large", "pinned-small", "unpinned-large", "unpinned-small"),
            folders.map { it.key }
        )
    }

    @Test
    fun sortsVideoFoldersByVideoCountWithinPinnedGroups() {
        val folders = VideoFolderFilterPolicy.displayFolderList(
            folders = listOf(
                folder("unpinned-small", "Zulu", 1),
                folder("pinned-small", "Beta", 1),
                folder("unpinned-large", "Alpha", 4),
                folder("pinned-large", "Gamma", 3),
                folder("pinned-large-name", "Alpha", 3)
            ),
            pinnedKeys = setOf("pinned-small", "pinned-large", "pinned-large-name")
        )

        assertEquals(
            listOf("pinned-large-name", "pinned-large", "pinned-small", "unpinned-large", "unpinned-small"),
            folders.map { it.key }
        )
    }

    @Test
    fun togglePinnedAddsAndRemovesFolderKey() {
        val added = VideoFolderFilterPolicy.togglePinned(emptySet(), "/Movies/Show")
        assertEquals(setOf("/Movies/Show"), added)

        val removed = VideoFolderFilterPolicy.togglePinned(added, "/Movies/Show")
        assertTrue(removed.isEmpty())
    }

    @Test
    fun prunePinnedKeysDropsUnavailableFolders() {
        val pruned = VideoFolderFilterPolicy.prunePinnedKeys(
            pinnedKeys = setOf("/Movies/Show", "/Downloads"),
            availableKeys = setOf("/Movies/Show")
        )

        assertEquals(setOf("/Movies/Show"), pruned)
    }

    private fun summary(key: String, name: String, count: Int) =
        VideoFolderSummary(key = key, name = name, videoCount = count)

    private fun folder(key: String, name: String, count: Int) =
        VideoFolder(key = key, name = name, videos = videoItems(count))

    @Suppress("UNCHECKED_CAST")
    private fun videoItems(count: Int): List<VideoItem> =
        List(count) { null } as List<VideoItem>
}
