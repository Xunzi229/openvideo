package com.example.openvideo.ui.local

object VideoFolderFilterPolicy {

    fun displayFolders(
        folders: List<VideoFolderSummary>,
        pinnedKeys: Set<String>
    ): List<VideoFolderSummary> {
        return folders
            .filter { it.videoCount > 0 }
            .map { folder ->
                folder.copy(isPinned = folder.key in pinnedKeys)
            }
            .sortedWith(
                compareByDescending<VideoFolderSummary> { it.isPinned }
                    .thenByDescending { it.videoCount }
                    .thenBy { it.name.lowercase() }
                    .thenBy { it.key }
            )
    }

    fun displayFolderList(
        folders: List<VideoFolder>,
        pinnedKeys: Set<String>
    ): List<VideoFolder> {
        return folders
            .filter { it.videoCount > 0 }
            .map { folder ->
                folder.copy(isPinned = folder.key in pinnedKeys)
            }
            .sortedWith(
                compareByDescending<VideoFolder> { it.isPinned }
                    .thenByDescending { it.videoCount }
                    .thenBy { it.name.lowercase() }
                    .thenBy { it.key }
            )
    }

    fun togglePinned(pinnedKeys: Set<String>, folderKey: String): Set<String> {
        if (folderKey.isBlank()) return pinnedKeys
        val mutable = pinnedKeys.toMutableSet()
        if (!mutable.add(folderKey)) {
            mutable.remove(folderKey)
        }
        return mutable
    }

    fun prunePinnedKeys(pinnedKeys: Set<String>, availableKeys: Set<String>): Set<String> =
        pinnedKeys.intersect(availableKeys)
}
