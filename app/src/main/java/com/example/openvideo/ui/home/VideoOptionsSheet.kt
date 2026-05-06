package com.example.openvideo.ui.home

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import com.example.openvideo.R
import com.example.openvideo.data.model.VideoItem
import com.google.android.material.bottomsheet.BottomSheetDialog

class VideoOptionsSheet(
    context: Context,
    private val video: VideoItem,
    isFavorite: Boolean,
    private val onPlay: () -> Unit,
    private val onFavorite: () -> Unit,
    private val onAddToPlaylist: () -> Unit,
    private val onDelete: () -> Unit
) : BottomSheetDialog(context) {

    init {
        val view = LayoutInflater.from(context).inflate(R.layout.sheet_video_options, null)
        setContentView(view)

        view.findViewById<TextView>(R.id.option_play).setOnClickListener {
            dismiss()
            onPlay()
        }

        val favoriteOption = view.findViewById<TextView>(R.id.option_favorite)
        favoriteOption.setText(if (isFavorite) R.string.option_favorited else R.string.option_favorite)
        favoriteOption.setOnClickListener {
            dismiss()
            onFavorite()
        }

        view.findViewById<TextView>(R.id.option_add_to_playlist).setOnClickListener {
            dismiss()
            onAddToPlaylist()
        }

        view.findViewById<TextView>(R.id.option_details).setOnClickListener {
            dismiss()
            showDetails()
        }

        view.findViewById<TextView>(R.id.option_share).setOnClickListener {
            dismiss()
            shareVideo()
        }

        view.findViewById<TextView>(R.id.option_delete).setOnClickListener {
            dismiss()
            onDelete()
        }
    }

    private fun shareVideo() {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "video/*"
            putExtra(Intent.EXTRA_STREAM, video.uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, context.getString(R.string.more_options)))
    }

    private fun showDetails() {
        val details = buildString {
            appendLine("${context.getString(R.string.detail_filename)}: ${video.title}")
            appendLine("${context.getString(R.string.detail_path)}: ${video.path}")
            appendLine("${context.getString(R.string.detail_resolution)}: ${video.width}x${video.height}")
            val durationSec = video.duration / 1000
            val h = durationSec / 3600
            val m = (durationSec % 3600) / 60
            val s = durationSec % 60
            appendLine("${context.getString(R.string.detail_duration)}: %02d:%02d:%02d".format(h, m, s))
            val sizeMB = video.size / (1024.0 * 1024.0)
            appendLine("${context.getString(R.string.detail_size)}: %.1f MB".format(sizeMB))
        }
        com.google.android.material.dialog.MaterialAlertDialogBuilder(context)
            .setTitle(R.string.video_details_title)
            .setMessage(details)
            .setPositiveButton(R.string.action_ok, null)
            .show()
    }
}
