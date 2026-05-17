package com.example.openvideo.ui.home

/**
 * Draft selections shown in the filter popover before the user taps Apply.
 */
data class VideoLibraryFilterUiState(
    val duration: DurationFilter = DurationFilter.ANY,
    val formatExtension: String? = null,
    val date: DateFilter = DateFilter.ANY
) {
    fun toAdvancedFilters(): MediaLibraryAdvancedFilters =
        MediaLibraryAdvancedFilters(
            durationFilter = duration,
            formatExtension = formatExtension,
            dateFilter = date
        )

    companion object {
        fun from(filters: MediaLibraryAdvancedFilters): VideoLibraryFilterUiState =
            VideoLibraryFilterUiState(
                duration = filters.durationFilter,
                formatExtension = filters.formatExtension,
                date = filters.dateFilter
            )

        fun default(): VideoLibraryFilterUiState = VideoLibraryFilterUiState()
    }
}
