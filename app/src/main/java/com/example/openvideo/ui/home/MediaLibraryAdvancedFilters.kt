package com.example.openvideo.ui.home

enum class DurationFilter {
    ANY,
    SHORT,
    MEDIUM,
    LONG
}

enum class DateFilter {
    ANY,
    LAST_7_DAYS,
    LAST_30_DAYS,
    OLDER_THAN_30_DAYS
}

data class MediaLibraryAdvancedFilters(
    val durationFilter: DurationFilter = DurationFilter.ANY,
    val formatExtension: String? = null,
    val dateFilter: DateFilter = DateFilter.ANY
) {
    fun isActive(): Boolean =
        durationFilter != DurationFilter.ANY ||
            !formatExtension.isNullOrBlank() ||
            dateFilter != DateFilter.ANY
}
