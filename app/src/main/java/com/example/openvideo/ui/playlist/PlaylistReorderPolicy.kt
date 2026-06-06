package com.example.openvideo.ui.playlist

import com.example.openvideo.data.local.PlaylistVideoEntity

object PlaylistReorderPolicy {

    fun move(
        videos: List<PlaylistVideoEntity>,
        fromIndex: Int,
        toIndex: Int
    ): List<PlaylistVideoEntity> {
        if (videos.isEmpty()) return emptyList()

        val mutable = videos.sortedBy { it.position }.toMutableList()
        val from = fromIndex.coerceIn(mutable.indices)
        val to = toIndex.coerceIn(mutable.indices)
        val moved = mutable.removeAt(from)
        mutable.add(to, moved)

        return mutable.mapIndexed { index, item -> item.copy(position = index) }
    }
}
