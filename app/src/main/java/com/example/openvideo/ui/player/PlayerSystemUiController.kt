package com.example.openvideo.ui.player

import android.app.Activity
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

object PlayerSystemUiController {
    fun suppressNotificationOpenTransition(activity: Activity, intent: Intent, extraKey: String) {
        if (!intent.getBooleanExtra(extraKey, false)) return
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> {
                overrideOpenTransitionCompat(activity)
            }
            else -> {
                @Suppress("DEPRECATION")
                activity.overridePendingTransition(0, 0)
            }
        }
    }

    fun enterImmersiveMode(activity: Activity) {
        WindowCompat.setDecorFitsSystemWindows(activity.window, false)
        val controller = WindowInsetsControllerCompat(activity.window, activity.window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    private fun overrideOpenTransitionCompat(activity: Activity) {
        activity.overrideActivityTransition(Activity.OVERRIDE_TRANSITION_OPEN, 0, 0)
    }
}
