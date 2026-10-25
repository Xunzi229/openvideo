package com.openvideo.app.ui.settings

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.openvideo.app.R
import com.openvideo.app.core.prefs.LocalContinuePlaybackPosition
import com.openvideo.app.core.prefs.PlaybackOrientation
import com.openvideo.app.core.ui.AppleAction
import com.openvideo.app.core.ui.AppleActionSheet
import com.openvideo.app.core.ui.LibraryNavigator
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class GeneralSettingsFragment : Fragment() {
    private val viewModel: SettingsViewModel by activityViewModels()
    private var activeDialog: Dialog? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_general_settings, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.findViewById<TextView>(R.id.tv_back_title).setText(R.string.settings_page_title)
        view.findViewById<View>(R.id.btn_back).setOnClickListener { LibraryNavigator.pop(this) }
        val value = view.findViewById<TextView>(R.id.tv_continue_playback_position_value)
        updatePositionLabel(value)
        view.findViewById<View>(R.id.row_continue_playback_position).setOnClickListener {
            if (activeDialog?.isShowing == true) return@setOnClickListener
            activeDialog = AppleActionSheet.show(
                context = requireContext(),
                title = getString(R.string.settings_continue_playback_position),
                actions = LocalContinuePlaybackPosition.entries.map { position ->
                    AppleAction(
                        title = positionLabel(position),
                        selected = position == viewModel.localContinuePlaybackPosition,
                        onClick = {
                            viewModel.setLocalContinuePlaybackPosition(position)
                            updatePositionLabel(value)
                        }
                    )
                },
                defaultFocusCancel = false,
                onDismiss = { activeDialog = null }
            )
        }
        val orientationValue = view.findViewById<TextView>(R.id.tv_playback_orientation_value)
        updateOrientationLabel(orientationValue)
        view.findViewById<View>(R.id.row_playback_orientation).setOnClickListener {
            if (activeDialog?.isShowing == true) return@setOnClickListener
            activeDialog = AppleActionSheet.show(
                context = requireContext(),
                title = getString(R.string.settings_playback_orientation),
                actions = PlaybackOrientation.entries.map { orientation ->
                    AppleAction(
                        title = orientationLabel(orientation),
                        selected = orientation == viewModel.playbackOrientation,
                        onClick = {
                            viewModel.setPlaybackOrientation(orientation)
                            updateOrientationLabel(orientationValue)
                        }
                    )
                },
                defaultFocusCancel = false,
                onDismiss = { activeDialog = null }
            )
        }
    }

    private fun positionLabel(position: LocalContinuePlaybackPosition): String = getString(
        when (position) {
            LocalContinuePlaybackPosition.TOP -> R.string.settings_continue_playback_position_top
            LocalContinuePlaybackPosition.BOTTOM -> R.string.settings_continue_playback_position_bottom
        }
    )

    private fun updatePositionLabel(value: TextView) {
        value.text = positionLabel(viewModel.localContinuePlaybackPosition)
        (value.parent as View).contentDescription =
            "${getString(R.string.settings_continue_playback_position)} ${value.text}"
    }

    private fun orientationLabel(orientation: PlaybackOrientation): String = getString(
        when (orientation) {
            PlaybackOrientation.DEFAULT -> R.string.settings_playback_orientation_default
            PlaybackOrientation.PORTRAIT -> R.string.settings_playback_orientation_portrait
            PlaybackOrientation.LANDSCAPE -> R.string.settings_playback_orientation_landscape
        }
    )

    private fun updateOrientationLabel(value: TextView) {
        value.text = orientationLabel(viewModel.playbackOrientation)
        (value.parent as View).contentDescription =
            "${getString(R.string.settings_playback_orientation)} ${value.text}"
    }

    override fun onDestroyView() {
        activeDialog?.dismiss()
        activeDialog = null
        super.onDestroyView()
    }
}
