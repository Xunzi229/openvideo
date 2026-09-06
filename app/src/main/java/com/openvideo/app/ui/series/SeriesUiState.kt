package com.openvideo.app.ui.series

import com.openvideo.app.data.local.SeriesEntity

data class SeriesUiState(
    val seriesId: Long,
    val title: String,
    val folderPath: String,
    val posterPath: String?,
    val updatedAt: Long
) {
    companion object {
        fun from(entity: SeriesEntity): SeriesUiState =
            SeriesUiState(
                seriesId = entity.seriesId,
                title = entity.title.takeIf { it.isNotBlank() } ?: entity.normalizedTitleKey,
                folderPath = entity.folderPath,
                posterPath = entity.posterPath,
                updatedAt = entity.updatedAt
            )
    }
}
