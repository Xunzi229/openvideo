package com.example.openvideo.ui.playlist

import android.app.Dialog
import android.content.Context
import com.example.openvideo.R
import com.example.openvideo.core.ui.AppleAction
import com.example.openvideo.core.ui.AppleActionSheet
import com.example.openvideo.core.ui.AppleActionStyle

object PlaylistOptionsActionSheet {
    fun show(
        context: Context,
        playlistName: String,
        onDismiss: () -> Unit,
        onRename: () -> Unit,
        onDelete: () -> Unit
    ): Dialog = AppleActionSheet.show(
        context = context,
        message = playlistName,
        actions = listOf(
            AppleAction(context.getString(R.string.playlist_option_rename), onClick = onRename),
            AppleAction(
                title = context.getString(R.string.playlist_option_delete),
                style = AppleActionStyle.DESTRUCTIVE,
                onClick = onDelete
            )
        ),
        onDismiss = onDismiss
    )
}
