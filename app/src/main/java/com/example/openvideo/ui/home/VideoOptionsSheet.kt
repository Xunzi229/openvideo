package com.example.openvideo.ui.home

import android.app.Dialog
import android.content.Context
import android.content.Intent
import com.example.openvideo.R
import com.example.openvideo.core.ui.AppleAction
import com.example.openvideo.core.ui.AppleActionSheet
import com.example.openvideo.core.ui.AppleActionStyle
import com.example.openvideo.core.ui.AppleAlertDialog
import com.example.openvideo.data.model.VideoItem

class VideoOptionsSheet(
    private val context: Context,
    private val video: VideoItem,
    isFavorite: Boolean,
    private val onPlay: () -> Unit,
    private val onFavorite: () -> Unit,
    private val onAddToPlaylist: () -> Unit,
    private val onDelete: () -> Unit
) {
    private val favoriteTitle = context.getString(
        if (isFavorite) R.string.option_favorited else R.string.option_favorite
    )

    fun show(): Dialog = AppleActionSheet.show(
        context = context,
        message = video.title,
        actions = listOf(
            AppleAction(context.getString(R.string.option_play), onClick = onPlay),
            AppleAction(favoriteTitle, onClick = onFavorite),
            AppleAction(context.getString(R.string.option_add_to_playlist), onClick = onAddToPlaylist),
            AppleAction(context.getString(R.string.option_details), onClick = { showDetails() }),
            AppleAction(context.getString(R.string.option_share), onClick = { shareVideo() }),
            AppleAction(
                title = context.getString(R.string.action_delete),
                style = AppleActionStyle.DESTRUCTIVE,
                onClick = onDelete
            )
        ),
        defaultFocusCancel = false
    )

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
            appendLine("${context.getString(R.string.detail_path)}: ${video.libraryPath}")
            appendLine("${context.getString(R.string.detail_resolution)}: ${video.width}x${video.height}")
            val durationSec = video.duration / 1000
            val h = durationSec / 3600
            val m = (durationSec % 3600) / 60
            val s = durationSec % 60
            appendLine("${context.getString(R.string.detail_duration)}: %02d:%02d:%02d".format(h, m, s))
            val sizeMB = video.size / (1024.0 * 1024.0)
            appendLine("${context.getString(R.string.detail_size)}: %.1f MB".format(sizeMB))
        }
        AppleAlertDialog.show(
            context = context,
            title = context.getString(R.string.video_details_title),
            message = details.trim(),
            actions = listOf(
                AppleAction(context.getString(R.string.action_ok), AppleActionStyle.CANCEL)
            )
        )
    }
}
