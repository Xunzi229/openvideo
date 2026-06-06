package com.example.openvideo.ui.home

import com.example.openvideo.core.metadata.MediaSmartListItem
import com.example.openvideo.core.metadata.MediaSmartListPolicy
import com.example.openvideo.core.metadata.MediaSmartListType
import com.example.openvideo.data.local.HistoryEntity
import com.example.openvideo.data.model.VideoItem

data class HomeSmartListSection(
    val type: MediaSmartListType,
    val videos: List<VideoItem>
)

data class HomeSmartListVideo(
    val id: Long,
    val title: String,
    val path: String,
    val durationMs: Long,
    val sizeBytes: Long,
    val width: Int,
    val height: Int,
    val dateAdded: Long
)

data class HomeSmartListVideoSection(
    val type: MediaSmartListType,
    val videos: List<HomeSmartListVideo>
)

object HomeSmartListBuilder {
    private val sectionOrder = listOf(
        MediaSmartListType.RECENTLY_ADDED,
        MediaSmartListType.IN_PROGRESS,
        MediaSmartListType.COMPLETED,
        MediaSmartListType.LARGE_FILES,
        MediaSmartListType.UHD,
        MediaSmartListType.HDR,
        MediaSmartListType.WITH_SUBTITLES
    )

    fun build(
        videos: List<VideoItem>,
        history: List<HistoryEntity>,
        limit: Int = 20,
        largeFileThresholdBytes: Long = 1_073_741_824L
    ): List<HomeSmartListSection> {
        val videosById = videos.associateBy { it.id }
        return buildVideoSections(
            videos = videos.map { it.toSmartListVideo() },
            history = history,
            limit = limit,
            largeFileThresholdBytes = largeFileThresholdBytes
        ).mapNotNull { section ->
            val sectionVideos = section.videos.mapNotNull { videosById[it.id] }
            sectionVideos.takeIf { it.isNotEmpty() }?.let { videosForSection ->
                HomeSmartListSection(type = section.type, videos = videosForSection)
            }
        }
    }

    fun buildVideoSections(
        videos: List<HomeSmartListVideo>,
        history: List<HistoryEntity>,
        limit: Int = 20,
        largeFileThresholdBytes: Long = 1_073_741_824L
    ): List<HomeSmartListVideoSection> {
        if (videos.isEmpty()) return emptyList()
        val videosById = videos.associateBy { it.id }
        val latestHistoryByVideoId = history
            .groupBy { it.videoId }
            .mapValues { (_, entries) -> entries.maxBy { it.timestamp } }

        val items = videos.map { video ->
            val latestHistory = latestHistoryByVideoId[video.id]
            MediaSmartListItem(
                id = video.id,
                title = video.title,
                path = video.path,
                durationMs = video.durationMs,
                sizeBytes = video.sizeBytes,
                width = video.width,
                height = video.height,
                dateAdded = video.dateAdded,
                lastPositionMs = latestHistory?.lastPosition,
                hasExternalSubtitle = latestHistory?.externalSubtitleUri?.isNotBlank() == true,
                isHdr = false
            )
        }

        return sectionOrder.mapNotNull { type ->
            val sectionVideos = MediaSmartListPolicy.itemsFor(
                type = type,
                items = items,
                limit = limit,
                largeFileThresholdBytes = largeFileThresholdBytes
            ).mapNotNull { item -> videosById[item.id] }
            sectionVideos.takeIf { it.isNotEmpty() }?.let { videosForSection ->
                HomeSmartListVideoSection(type = type, videos = videosForSection)
            }
        }
    }

    private fun VideoItem.toSmartListVideo(): HomeSmartListVideo =
        HomeSmartListVideo(
            id = id,
            title = title,
            path = path,
            durationMs = duration,
            sizeBytes = size,
            width = width,
            height = height,
            dateAdded = dateAdded
        )
}
