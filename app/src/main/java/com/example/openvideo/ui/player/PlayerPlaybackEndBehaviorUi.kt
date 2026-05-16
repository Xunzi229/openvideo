package com.example.openvideo.ui.player

import android.content.Context
import com.example.openvideo.R
import com.example.openvideo.core.prefs.PlaybackEndBehavior

object PlayerPlaybackEndBehaviorUi {

    fun label(context: Context, behavior: PlaybackEndBehavior): String = when (behavior) {
        PlaybackEndBehavior.FOLLOW_SETTINGS ->
            context.getString(R.string.settings_playback_end_follow)
        PlaybackEndBehavior.PLAY_NEXT ->
            context.getString(R.string.settings_playback_end_next)
        PlaybackEndBehavior.REPLAY ->
            context.getString(R.string.settings_playback_end_replay)
        PlaybackEndBehavior.STOP ->
            context.getString(R.string.settings_playback_end_stop)
        PlaybackEndBehavior.RETURN_TO_LIST ->
            context.getString(R.string.settings_playback_end_return)
    }

    fun options(): List<PlaybackEndBehavior> = PlaybackEndBehavior.entries.toList()
}
