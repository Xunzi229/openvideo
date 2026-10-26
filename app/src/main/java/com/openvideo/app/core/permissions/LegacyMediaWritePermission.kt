package com.openvideo.app.core.permissions

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.openvideo.app.R
import com.openvideo.app.core.ui.AppleHud

/** Register with the owner before STARTED; request legacy access only for a write operation. */
class LegacyMediaWritePermission(
    caller: ActivityResultCaller,
    private val contextProvider: () -> Context?
) {
    private var pendingAction: (() -> Unit)? = null
    private val launcher = caller.registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        val action = pendingAction ?: return@registerForActivityResult
        pendingAction = null
        val context = contextProvider() ?: return@registerForActivityResult
        if (granted) action()
        else AppleHud.show(context, R.string.media_write_permission_denied)
    }

    fun runWhenGranted(action: () -> Unit) {
        val context = contextProvider() ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            action()
            return
        }
        if (pendingAction != null) return
        pendingAction = action
        try {
            launcher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        } catch (error: RuntimeException) {
            pendingAction = null
            throw error
        }
    }
}
