package com.example.openvideo.ui.player

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.fragment.app.DialogFragment
import com.example.openvideo.core.prefs.PlayerPrefs

/**
 * Player overlay settings hosted in a plain dialog — same window chrome as [PlayerSettingsDialog].
 */
abstract class BaseSettingsSheet : DialogFragment() {
    protected abstract val layoutResId: Int

    protected open fun settingsSheetPanelRootId(): Int? = null

    protected open fun settingsSheetPlayerPrefs(): PlayerPrefs? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_FRAME, 0)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog =
        Dialog(requireContext()).apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            setCanceledOnTouchOutside(true)
        }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(layoutResId, container, false)
    }

    override fun onStart() {
        super.onStart()
        val prefs = settingsSheetPlayerPrefs() ?: return
        val panelRootId = settingsSheetPanelRootId()
        val panelRoot = panelRootId?.let { id -> view?.findViewById<View>(id) }
        dialog?.let { PlayerSettingsSheetChrome.applyDialogChrome(it, prefs, panelRoot) }
    }
}
