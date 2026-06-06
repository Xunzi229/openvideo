package com.example.openvideo.ui.playlist

import com.example.openvideo.data.local.PlaylistVideoEntity

object PlaylistImportPolicy {

    fun createRows(
        playlistId: Long,
        existing: List<PlaylistVideoEntity>,
        candidates: List<PlaylistTransferFormat.ImportCandidate>
    ): List<PlaylistVideoEntity> {
        val existingPaths = existing.mapTo(mutableSetOf()) { it.videoPath.trim() }
        val importedPaths = mutableSetOf<String>()
        val startPosition = existing.maxOfOrNull { it.position }?.plus(1) ?: 0
        val firstTemporaryId = existing.minOfOrNull { it.videoId }?.takeIf { it < 0L }?.minus(1L) ?: -1L

        return candidates
            .sortedBy { it.position }
            .mapNotNull { candidate ->
                val path = candidate.path.trim()
                if (path.isBlank()) return@mapNotNull null
                if (path in existingPaths || !importedPaths.add(path)) return@mapNotNull null
                candidate.copy(path = path)
            }
            .mapIndexed { index, candidate ->
                PlaylistVideoEntity(
                    playlistId = playlistId,
                    videoId = firstTemporaryId - index,
                    videoTitle = candidate.title.ifBlank { candidate.path.substringAfterLast('/') },
                    videoPath = candidate.path,
                    videoDuration = candidate.durationMs.coerceAtLeast(0L),
                    position = startPosition + index
                )
            }
    }
}
