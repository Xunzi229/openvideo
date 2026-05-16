package com.example.openvideo.ui.home

import android.content.Context
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import com.example.openvideo.R

object MediaLibraryScanLoadingUi {

    fun bind(
        context: Context,
        loadingContainer: View,
        progressBar: ProgressBar,
        progressLabel: TextView,
        emptyLabel: TextView,
        emptyState: MediaLibraryEmptyState,
        scanProgress: MediaLibraryScanProgress?,
        isContentEmpty: Boolean
    ) {
        if (!isContentEmpty) {
            loadingContainer.visibility = View.GONE
            emptyLabel.visibility = View.GONE
            return
        }

        when (emptyState) {
            MediaLibraryEmptyState.LOADING -> {
                loadingContainer.visibility = View.VISIBLE
                progressBar.visibility = View.VISIBLE
                progressBar.isIndeterminate = true
                emptyLabel.visibility = View.GONE
                val count = scanProgress?.scannedCount ?: 0
                progressLabel.text = if (count > 0) {
                    context.getString(R.string.media_library_scanning_count, count)
                } else {
                    context.getString(R.string.media_library_loading)
                }
            }
            else -> {
                loadingContainer.visibility = View.GONE
                emptyLabel.visibility = View.VISIBLE
            }
        }
    }
}
