package com.example.openvideo.ui.player

import android.app.Dialog
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.os.Build
import com.example.openvideo.core.prefs.PlayerPrefs

object PlayerSettingsSheetChrome {

    fun applyWindowLayout(
        window: Window,
        screenWidth: Int,
        screenHeight: Int,
        density: Float
    ) {
        val bounds = PlayerSettingsLayoutPolicy.panelBounds(screenWidth, screenHeight, density)
        window.setLayout(bounds.width, bounds.height)
        window.setGravity(PlayerSettingsLayoutPolicy.panelGravity(screenWidth, screenHeight))
        window.attributes = window.attributes.apply {
            x = PlayerSettingsLayoutPolicy.landscapeMarginPx(screenWidth, screenHeight, density)
        }
        window.setBackgroundDrawableResource(android.R.color.transparent)
        window.decorView.setPadding(0, 0, 0, 0)
        window.decorView.alpha = 1f
        window.decorView.elevation = 20f * density
    }

    fun applyBackdrop(
        window: Window,
        playerPrefs: PlayerPrefs,
        density: Float,
        sdkInt: Int = Build.VERSION.SDK_INT
    ) {
        val style = PlayerSettingsSheetStylePolicy.compute(
            panelOpacityPercent = playerPrefs.settingsPanelOpacity,
            backdropDimPercent = playerPrefs.settingsSheetBackdropDimPercent,
            backdropBlurDp = playerPrefs.settingsSheetBackdropBlurDp,
            density = density
        )
        window.setDimAmount(style.dimAmount)
        window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        if (PlayerSettingsSheetStylePolicy.supportsBackdropBlur(sdkInt)) {
            window.setBackgroundBlurRadius(style.backdropBlurRadiusPx)
        }
    }

    fun applyPanelOpacity(panelRoot: View, playerPrefs: PlayerPrefs) {
        panelRoot.alpha = PlayerChromePolicy.percentToAlpha(playerPrefs.settingsPanelOpacity)
    }

    fun applyDialogChrome(
        dialog: Dialog,
        playerPrefs: PlayerPrefs,
        panelRoot: View? = null
    ) {
        val window = dialog.window ?: return
        val metrics = dialog.context.resources.displayMetrics
        applyWindowLayout(window, metrics.widthPixels, metrics.heightPixels, metrics.density)
        applyBackdrop(window, playerPrefs, metrics.density)
        panelRoot?.let { applyPanelOpacity(it, playerPrefs) }
    }
}
