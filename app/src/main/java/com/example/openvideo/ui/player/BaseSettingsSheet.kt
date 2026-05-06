package com.example.openvideo.ui.player

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

/**
 * Base class for settings BottomSheets to reduce boilerplate.
 * Subclasses should override layoutResId to provide their layout.
 */
abstract class BaseSettingsSheet : BottomSheetDialogFragment() {
    protected abstract val layoutResId: Int

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(layoutResId, container, false)
    }
}
