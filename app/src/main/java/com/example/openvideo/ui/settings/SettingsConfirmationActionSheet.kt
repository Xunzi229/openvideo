package com.example.openvideo.ui.settings

import android.app.Dialog
import android.content.Context
import androidx.annotation.StringRes
import com.example.openvideo.core.ui.AppleAction
import com.example.openvideo.core.ui.AppleActionSheet
import com.example.openvideo.core.ui.AppleActionStyle

object SettingsConfirmationActionSheet {
    fun show(
        context: Context,
        @StringRes titleRes: Int,
        @StringRes messageRes: Int,
        @StringRes confirmRes: Int,
        @StringRes cancelRes: Int,
        onDismiss: () -> Unit,
        onConfirm: () -> Unit
    ): Dialog = show(
        context = context,
        title = context.getString(titleRes),
        message = context.getString(messageRes),
        confirmRes = confirmRes,
        cancelRes = cancelRes,
        onDismiss = onDismiss,
        onConfirm = onConfirm
    )

    fun show(
        context: Context,
        @StringRes titleRes: Int,
        message: CharSequence,
        @StringRes confirmRes: Int,
        @StringRes cancelRes: Int,
        onDismiss: () -> Unit,
        onConfirm: () -> Unit
    ): Dialog = show(
        context = context,
        title = context.getString(titleRes),
        message = message,
        confirmRes = confirmRes,
        cancelRes = cancelRes,
        onDismiss = onDismiss,
        onConfirm = onConfirm
    )

    private fun show(
        context: Context,
        title: CharSequence,
        message: CharSequence,
        @StringRes confirmRes: Int,
        @StringRes cancelRes: Int,
        onDismiss: () -> Unit,
        onConfirm: () -> Unit
    ): Dialog = AppleActionSheet.show(
        context = context,
        title = title,
        message = message,
        actions = listOf(
            AppleAction(
                title = context.getString(confirmRes),
                style = AppleActionStyle.DESTRUCTIVE,
                onClick = onConfirm
            )
        ),
        cancelTitle = context.getString(cancelRes),
        onDismiss = onDismiss
    )
}
