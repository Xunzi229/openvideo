package com.example.openvideo.ui.local

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
}
