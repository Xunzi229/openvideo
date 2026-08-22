package com.example.openvideo.core.ui

import android.app.Dialog
import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

/**
 * Pads overlay windows (dialogs, popups, sheets) so gesture/nav bars and
 * cutouts do not cover actions. Activity is edge-to-edge; these windows
 * must opt in to the same insets.
 */
object OverlayWindowInsets {

    fun edgesFrom(insets: WindowInsetsCompat, includeIme: Boolean = false): SystemBarInsetsPolicy.Edges {
        val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
        val cutout = insets.getInsets(WindowInsetsCompat.Type.displayCutout())
        val union = SystemBarInsetsPolicy.union(
            SystemBarInsetsPolicy.Edges(bars.left, bars.top, bars.right, bars.bottom),
            SystemBarInsetsPolicy.Edges(cutout.left, cutout.top, cutout.right, cutout.bottom)
        )
        if (!includeIme) return union
        val imeBottom = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
        return union.copy(bottom = maxOf(union.bottom, imeBottom))
    }

    fun bind(view: View, extraBottomPx: Int = 0, includeIme: Boolean = false) {
        val baseLeft = view.paddingLeft
        val baseTop = view.paddingTop
        val baseRight = view.paddingRight
        val baseBottom = view.paddingBottom
        ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
            val edges = edgesFrom(insets, includeIme)
            v.setPadding(
                baseLeft,
                baseTop,
                baseRight,
                SystemBarInsetsPolicy.overlayBottomPadding(baseBottom, edges.bottom, extraBottomPx)
            )
            insets
        }
        ViewCompat.requestApplyInsets(view)
    }

    fun bindDialog(dialog: Dialog, extraBottomPx: Int = 0, includeIme: Boolean = false) {
        val content = dialog.findViewById<View>(android.R.id.content) ?: dialog.window?.decorView ?: return
        bind(content, extraBottomPx, includeIme)
    }
}
