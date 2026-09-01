package com.example.openvideo.core.update

import android.content.Context
import android.content.Intent
import androidx.core.net.toUri

/** Opens a trusted GitHub Release page in the user's default browser application. */
object ReleasePageLauncher {

    fun open(context: Context, releasePageUrl: String): Boolean {
        if (!UpdateUrlPolicy.isTrustedReleasePage(releasePageUrl)) return false
        val intent = Intent.makeMainSelectorActivity(
            Intent.ACTION_MAIN,
            Intent.CATEGORY_APP_BROWSER
        ).apply {
            data = releasePageUrl.toUri()
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return runCatching {
            context.startActivity(intent)
            true
        }.getOrDefault(false)
    }
}
