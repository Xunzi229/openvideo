package com.example.openvideo.ui.history

import android.content.Context
import com.example.openvideo.R

data class HistoryContinueWatchingLabels(
    val missingFile: String,
    val completed: String,
    val justNow: String,
    val minutesAgo: (Long) -> String,
    val hoursAgo: (Long) -> String,
    val daysAgo: (Long) -> String,
    val progressPercent: (Int) -> String
) {
    companion object {
        fun from(context: Context): HistoryContinueWatchingLabels {
            val resources = context.resources
            return HistoryContinueWatchingLabels(
                missingFile = resources.getString(R.string.history_continue_missing_file),
                completed = resources.getString(R.string.history_continue_completed),
                justNow = resources.getString(R.string.history_continue_just_now),
                minutesAgo = { count ->
                    resources.getString(R.string.history_continue_minutes_ago, count)
                },
                hoursAgo = { count ->
                    resources.getString(R.string.history_continue_hours_ago, count)
                },
                daysAgo = { count ->
                    resources.getString(R.string.history_continue_days_ago, count)
                },
                progressPercent = { percent ->
                    resources.getString(R.string.history_continue_progress_percent, percent)
                }
            )
        }

        fun englishDefaults(): HistoryContinueWatchingLabels = HistoryContinueWatchingLabels(
            missingFile = "Missing file",
            completed = "Completed",
            justNow = "Just now",
            minutesAgo = { count -> "$count min ago" },
            hoursAgo = { count -> "$count hr ago" },
            daysAgo = { count -> "$count d ago" },
            progressPercent = { percent -> "$percent%" }
        )
    }
}
